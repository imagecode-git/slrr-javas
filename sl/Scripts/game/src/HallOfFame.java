package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

public class HallOfFame extends Dialog
{
	final static int  CMD_EXIT		= 1;
	final static int  CMD_PREV		= 2;
	final static int  CMD_NEXT		= 3;

	Player	player;
	Text	nameTxt;
	Text[]	descTxt;

	ResourceRef res_upper_frame = new ResourceRef(frontend:0x93E2r); //upper_frame.png
	ResourceRef res_lower_frame = new ResourceRef(frontend:0x93E3r); //lower_frame.png
	ResourceRef res_hof_title = new ResourceRef(frontend:0x93E4r); //hof_title.png
	ResourceRef res_bck = new ResourceRef(frontend:0x93E5r); //bck.jpg
	ResourceRef res_text = Frontend.largeFont_strong;

	Rectangle upper_frame, lower_frame, hof_title;

	Menu arrow_left, arrow_right, button_close;
	Style arrow_style, button_style;

	SlidingMenu cardSlider;
	CardBase base;

	int descLines = 2;
	int anim_text_speed = 1;
	int descColor = 0xFFC0C0C0;
	int nameColor = 0xFF2C2C2C;

 	Thread	hofThread;
	int	tMethods = 4; //amount of THOR methods

	public HallOfFame()
	{
		super(GameLogic.player.controller, DF_FULLSCREEN|DF_HIGHPRI|DF_HIDEPOINTER, null, null);
		player=GameLogic.player;
		osd.globalHandler = this;

		base = new CardBase(descLines);
	}

	public void show()
	{
		osd.darkStatus = 1; //force fade-IN
		osd.darken(16,1);

		arrow_style  = new Style(0.16, 0.16, Frontend.mediumFont, Text.ALIGN_LEFT, null);
		button_style = new Style(0.14, 0.14, Frontend.mediumFont, Text.ALIGN_LEFT, null);

		hofThread = new Thread(this, "hall of fame animation thread", 1); //extended! thread-oriented methodology (THOR), no injections
		hofThread.setPriority(Thread.MAX_PRIORITY);
		hofThread.start();

		//initialize THOR methods
		for(int i=0; i<(tMethods+1); i++) hofThread.addMethod(i);

		//hofThread.controlMethod(0,1); //this will prevent execution of THOR auto-fade for text on the window open. WARNING! freezes the game on build 900!!

		osd.createBG(res_bck);

		upper_frame = osd.createRectangle(0.0, 1.05, 2.0, 0.625, 0, res_upper_frame, 0);
		lower_frame = osd.createRectangle(0.0, -1.1, 2.0, 0.575, 0, res_lower_frame, 0);
		hof_title = osd.createRectangle(0.0, 0.975, 0.725, 0.215, 1, res_hof_title, 0);

		//width/height of each card
		float cardW = 0.55;
		float cardH = 1.2;

		cardSlider = osd.createSlidingMenu(-1.4, 0, 1.4, 1.0, 1, 1, SlidingMenu.STYLE_HORIZONTAL); //single sliding menu! + adjustable animation + styling, see Osd.class
		cardSlider.setSpeed(0.5);

		for(int i=0; i<base.totalCards(); i++) cardSlider.addItem(cardW, cardH, 1, base.getCard(i).id, base.getCard(i).id+1, 0, base.getCard(i).name);

		cardSlider.activate(1);
		osd.activeSlider=null; //to prevent text update glitches

		Gadget g;
		int pri = 2;

		arrow_left = osd.createMenu(arrow_style, 0.0, 0.0, 0, Osd.MD_HORIZONTAL);
		g = arrow_left.addItem(new ResourceRef(frontend:0xD15Dr), CMD_PREV, null, null, 0); //"<" button
		g.rect.setPos(new Vector3(-1.495, 0, pri)); //we manually set higher priority
		g.phy.setMatrix(new Vector3(-1.495, 0, pri), null); //same for hotspot

		arrow_right = osd.createMenu(arrow_style, 0.0, 0.0, 0, Osd.MD_HORIZONTAL);
		g = arrow_right.addItem(new ResourceRef(frontend:0xD15Er), CMD_NEXT, null, null, 0); //">" button
		g.rect.setPos(new Vector3(1.495, 0, pri));
		g.phy.setMatrix(new Vector3(1.495, 0, pri), null);

		button_close = osd.createMenu(button_style, 0.9, -0.9, 0, Osd.MD_HORIZONTAL);
		button_close.addItem(new ResourceRef(frontend:0xD15Fr), CMD_EXIT, null, null, 1); //"X" button

		float delta = Text.getLineSpacing(res_text, osd.vp);
		float y = 0.91 - (delta*3.5)+delta;

		descTxt = new Text[descLines];
		descTxt[0] = osd.createText(null, res_text, Text.ALIGN_CENTER, 0.0, y);
		descTxt[1] = osd.createText(null, res_text, Text.ALIGN_CENTER, 0.0, y+delta);

		for(int i=0; i<descTxt.length; i++)
		{
			descTxt[i].setColor(0);
			descTxt[i].fadeIn(descColor);
			descTxt[i].a_speed = anim_text_speed;
		}

		nameTxt = osd.createText("<no name>", res_text, Text.ALIGN_CENTER, 0.0, y-0.315+delta);
		nameTxt.setColor(0);
		nameTxt.fadeIn(nameColor);
		nameTxt.a_speed = anim_text_speed;
		
		updateText();

		super.show();

		if(!System.nextGen())
		{
			Sound.changeMusicSet(Sound.MUSIC_SET_GARAGE);
		}
		else
		{
			//build 900, direct music play, track#2: Hall Of Fame
			Sound.changeMusicSet(Sound.MUSIC_SET_MISC);
			Sound.playTrack(1); //set 2!!
		}

		Input.cursor.enable(1);
	}

	public void hide()
	{
		hofThread.stop();
		hofThread=null;

		Input.cursor.enable(0);
		Frontend.loadingScreen.show(Osd.RRT_GHOST, 1);
		osd.darken(16,1);
		Frontend.loadingScreen.userWait(1.0);

		super.hide();
	}

	public void animateText(int dir)
	{
		int ready;
		if(dir == 1) //left
		{
			if(cardSlider.activeItem > 0) ready++;
		}
		else //right
		{
			if(cardSlider.activeItem < cardSlider.items.size()-1) ready++;
		}

		if(ready) //text will not be animated if we attempt to break slider's bounds
		{
			if(hofThread.methodStatus(0) == -1) hofThread.switchStatus(0); //reset THOR for text animations

			nameTxt.restartAnimation();
			nameTxt.fadeOut(nameColor);

			for(int i=0; i<descTxt.length; i++)
			{
				descTxt[i].restartAnimation();
				descTxt[i].fadeOut(descColor);
			}
		}
	}

	public void updateText()
	{
		CardData cd = base.getCard(cardSlider.activeItem);
		for(int i=0; i<descTxt.length; i++) descTxt[i].changeText(cd.desc[i]); //fill in description texts
		if(nameTxt) nameTxt.changeText(cd.name); //pick name from database
	}

	public void run()
	{
		for(;;)
		{
			//THOR methods
			if(hofThread.methodStatus(0) == 1) //fadeIn all animated texts
			{
				updateText(); //update data in each text instance

				if(nameTxt)
				{
					nameTxt.restartAnimation();
					nameTxt.fadeIn(nameColor);
				}

				for(int i=0; i<descTxt.length; i++)
				{
					descTxt[i].restartAnimation();
					descTxt[i].fadeIn(descColor);
				}

				hofThread.controlMethod(0,-1);
			}
			//end of THOR

			if(nameTxt && nameTxt.a_finished) hofThread.execute(0); //check params just for one text, since theese params are the same for all text animations in this class

			hofThread.sleep(10);
		}
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_PREV):
					animateText(1);
					cardSlider.execute(Osd.CMD_MENU_LF);
					break;

			case(CMD_NEXT):
					animateText(-1);
					cardSlider.execute(Osd.CMD_MENU_RG);
					break;

			case(CMD_EXIT):
					super.osdCommand(0);
					break;
		}
	}
}

class CardData
{
	String	 name;
	String[] desc;
	int	 id;
	
	public CardData(String n, int l, int i)
	{
		name = n;
		desc = new String[l];
		id = i;
	}
}

class CardBase
{
	CardData[] card;
	String[] desc;

	public CardBase(int len)
	{
		card = new CardData[6];
		desc = new String[card.length];

		card[0] = new CardData("Furball", len, frontend:0x000093E6r);
		desc[0] = "One of the first community members that gained access to game sources. \n Co-developer of official SLRR patches.";

		card[1] = new CardData("Singh", len, frontend:0x000093EAr);
		desc[1] = "Leader of the world's biggest SLRR community, founder and owner of GOM-TEAM website. \n Very significant figure in SLRR history.";

		card[2] = new CardData("miran", len, frontend:0x000093E6r);
		desc[2] = "The developer of 2.2.1MWM patch and variety of powerful modding tools. \n Executed the most advanced discover of the game engine.";

		card[3] = new CardData("Wichur", len, frontend:0x000093EEr);
		desc[3] = "Co-developer of 2.2.1MWM patch, the first modder who discovered making add-on maps. \n Composer, modeller, game developer, highly talented person.";

		card[4] = new CardData("h00die", len, frontend:0x000093ECr);
		desc[4] = "First person discovered making the most beatiful paintjobs and interface designs. \n One of the greatest graphic artists in the SLRR community.";

		card[5] = new CardData("mihon", len, frontend:0x000093E8r);
		desc[5] = "Built the best add-on engines for the game with perfect materials and high detail. \n Modeller, 2D artist, mechanic.";

		for(int i=0; i<card.length; i++)
		{
			StringTokenizer tr = new StringTokenizer(desc[i], "\n");
			for(int j=0; j<len; j++) card[i].desc[j] = tr.nextToken();
		}
	}

	public int totalCards()
	{
		return card.length;
		return 0;
	}

	public CardData getCard(int id)
	{
		return card[id];
		return null;
	}
}