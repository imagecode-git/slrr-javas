package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

//RAXAT: v2.3.1, dedicated area for paint
public class PaintBooth extends Scene implements GameState, Callback
{
	GameState	parentState;

	Player		player;
	Osd			osd;
	ControlSetState	css;

	RenderRef	stuff;
	RenderRef	light, lighttype;

	GameRef		camera;
	Vector3		cameraPos = new Vector3(-2.2, 0.7, -3.5);
	Ypr			cameraOri = new Ypr(-2.5, -0.15, 0);

	Painter		painter;
	Multiplayer multiplayer;

	int			move, drag, shop;

	Text		moneyTxt, infoTxt;

	final static Vector3 defCarPos = new Vector3(0.0, -0.535, 0.0);
	final static Vector3 defLookPos = new Vector3(0, 0.0, -0.5);

	final static int  CMD_BACK = 0;
	final static int  CMD_COLORCATALOG = 1;

	public PaintBooth(Multiplayer mp)
	{
		createNativeInstance();
		internalScene = 1;
		multiplayer = mp;
	}

	public void enter(GameState prevState)
	{
		map = new GroundRef(misc.paintbooth:0x00000001r);
		stuff = new RenderRef(map, misc.paintbooth:0x00000003r, null); //sidelight glow

		if(camera) camera.command("render 0");
		Integrator.isCity = 0;

		parentState = prevState;
		player = GameLogic.player;

		Frontend.loadingScreen.show(new ResourceRef(frontend:0x00009407r));

		osd = new Osd();
		osd.globalHandler = this;
		osd.defSelection = 5;
		osd.orientation = 1;

		osd.createStrongHeader("PAINT BOOTH");
		moneyTxt = osd.createText("$" + player.getMoney(), Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, -0.98);
		infoTxt = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.98, 0.4875);

		Style buttonStyle = new Style(0.1, 0.1, Frontend.mediumFont, Text.ALIGN_RIGHT, null);
		Menu m = osd.createMenu(buttonStyle, 0.98, -0.93, 0, Osd.MD_HORIZONTAL);

		m.addItem(new ResourceRef(frontend:0x9C0Er), CMD_BACK, "Back to garage", null, 1);
		m.addItem(new ResourceRef(frontend:0x9C05r), CMD_COLORCATALOG, "Buy colors", null, 1);

		//osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, CMD_BACK, this); //only for MENUSET
		osd.createHotkey(Input.RCDIK_ESCAPE, Input.KEY|Osd.HK_STATIC, CMD_BACK, this);

		osd.endGroup();
		
		/*
		osd.darkStatus = 1; //force fade-IN
		osd.darken(16,1);
		*/

		painter = new Painter(player, multiplayer, osd, this, moneyTxt, infoTxt, 0);
		painter.paintCans.lastCanId = Integrator.lastPaintCanId; //RAXAT: build 934, previously loaded paintcan id could be incorrect
		painter.filterInventory(); //load paintcans from player's inventory into painter
		painter.showDecals = Integrator.showDecals;
		painter.lastPaintMode = Integrator.lastPaintMode;
		painter.show();
		painter.paintCans.scrollToLine(Integrator.lastPainterLine);
		painter.updateLineIndex(); //painter will always show us 1st page on enter so we manually fix that here
		
		lighttype = new RenderRef();
		lighttype.duplicate(new RenderRef(misc.paintbooth:0x000Cr));
		light = new RenderRef(map, lighttype, "neon");

		addSceneElements(0);
		GfxEngine.setGlobalEnvmap(new ResourceRef(misc.paintbooth:0x0000000Dr));

		camera = new GameRef(map, GameRef.RID_CAMERA, cameraPos.toString() + "," + cameraOri.toString() + ", 0x13, 1.0,1.0, 0.05", "Internal camera for paint booth (with collider)");
		cameraSetup(camera);

		lockCar();

		if(player.car)
		{
			player.car.setDamageMultiplier(0.0);
			player.car.setCruiseControl(0);
			player.car.setPos(defCarPos);
			player.car.wakeUp();
			player.car.command("suspend");

			for(int i=0; i<3; i++) player.car.command("filter " + i + " 0");
		}

		osd.show();
		changePointer();
		Frontend.loadingScreen.display();

		setEventMask(EVENT_CURSOR|EVENT_COMMAND);

		//special request: reset mouse and set sensitivity to 0
		Input.getAxis (1, -1);
		Input.cursor.enable(1);

		Input.cursor.addHandler(this);
		Input.cursor.enableCameraControl(camera);

		Sound.changeMusicSet(Sound.MUSIC_SET_GARAGE);
		
		if(player.checkHint(Player.H_PAINTBOOTH))
		{
			new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "PAINT BOOTH", "This is your first time exploring the paint booth! \n \n Buy cans with color pigment in the paint shop to begin painting. Also, all decals purchased in the catalog can be found and utilized here.").display();
		}
	}

	public void exit(GameState nextState)
	{
		clearEventMask(EVENT_ANY);

		Input.cursor.enable(0);
		Input.cursor.remHandler(this);
		Input.cursor.disableCameraControl();

		camera.destroy();

		osd.hide();
		osd=null;

		if(player.car)
		{
			player.car.setDamageMultiplier(Config.player_damage_multiplier);
			player.car.setCruiseControl(0);
			player.car.setPos(defCarPos);
			player.car.wakeUp();
			player.car.command("suspend");

			for(int i=0; i<3; i++) player.car.command("filter " + i + " 0");
		}

		releaseCar();

		Integrator.lastPaintCanId = painter.paintCans.lastCanId;
		Integrator.lastPainterLine = painter.paintCans.currentLine();
		Integrator.showDecals = painter.showDecals;
		Integrator.lastPaintMode = painter.lastPaintMode;
		painter.flushInventory(); //unload paintcans from painter back to player's inventory
		painter.hide();
		painter=null;

		light.destroy();
		lighttype.destroy();

		remSceneElements();

		shop=1;
		changePointer();

		stuff.destroy();
		map.unload();
	}

	public void lockCar()
	{
		if(player.car)
		{
			player.car.setParent(map);

			Vector3 ppp = new Vector3(defCarPos);
			player.car.setPos(ppp);

			player.car.command("reset");
			player.car.command("setsteer -0.7");
			player.car.command("stop"); //grab
		}
	}

	public void releaseCar()
	{
		if(player.car)
		{
			player.car.command("reset");
			player.car.command("start"); //release

			player.car.setParent(player);
		}
	}

	public void cameraSetup(GameRef cam)
	{
		cam.command("render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)); //vp id, cam id, flags
		cam.command("dist 2.7 3.3");
		cam.command("smooth 0.15 0.5");
		cam.command("zoom 70 3");
		cam.command("force 3.1 0.5 -0.2"); //with inertial movement support
		cam.command("torque 0.12");

		if(player.car)
		{
			cam.command("move " + player.car.id() + " 0,0,0 4.5");
			cam.command("look " + player.car.id() + " 0,0.0,0 0,0,0");
		}
		else
		{
			if(map)
			{
				cam.command("move " + map.id() + " " + defLookPos.toString() + " 3.0");
				cam.command("look " + map.id() + " " + defLookPos.toString() + " 0,0,0");
			}
		}

		player.controller.reset();
		player.controller.activateState(ControlSet.CAMTURNSET);

	}

	public void changePointer()
	{
		if(move) Input.cursor.setPointer(Frontend.pointers, "M"); //cross-arrow icon
		else
		{
			if(shop) Input.cursor.setPointer(Frontend.pointers, "B"); //normal cursor
			else
			{
				if(drag) Input.cursor.setPointer(Frontend.pointers, "A");
				else Input.cursor.setPointer(Frontend.pointers, "D"); //painter icon
			}
		}
	}

	public void refreshMoneyTxt()
	{
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) player.setMoney(multiplayer.RPC("getMoney", null, true).intValue());
		if(moneyTxt)
		{
			String diff = "";
			if(moneyTxt && moneyTxt.text) diff = moneyTxt.text.cut("$");
			if(Integer.toString(player.getMoney()) != diff)
			{
				new SfxRef(Frontend.SFX_MONEY).play();
				moneyTxt.changeText("$" + Integer.toString(player.getMoney()));
			}
		}
	}
	
	public static int calcPaintCanPrice(float capacity_, int quantity_)
	{
		return (capacity_/25)*quantity_*3;
	}

	//this will emulate drag cursor events for decals
	public void callback(int param)
	{
		if(painter.mode == Painter.MODE_PAINTDECAL)
		{
			if(player.decals.size())
			{
				drag = param;
				changePointer();
			}
		}
		
		refreshMoneyTxt(); //painter needs this to tell us that paintcan has been refilled and some money have been spent by player
	}

	public void handleEvent(GameRef obj_ref, int event, String param)
	{
		int	tok = -1;

		if(event == EVENT_CURSOR)
		{
			int ec = param.token(++tok).intValue();
			int cursor_id = param.token(++tok).intValue();
			int pick_ref = param.token(2).intValue();

			if(ec == GameType.EC_LDRAGBEGIN)
			{
				if(painter)
				{
					if(painter.mode != Painter.MODE_PAINTDECAL)
					{
						if(painter.paintCans && painter.paintCans.panels.length)
						{
							for(int i=0; i<painter.paintCans.panels.length; i++)
							{
								if(pick_ref == painter.paintCans.panels[i].button.phy.id())
								{
									int idx = painter.paintCans.getItemIDbyButtonPhyId(pick_ref);
									PaintCan p = painter.paintCans.getCanbyIndex(idx);
									if(p)
									{
										drag=1;
										changePointer();
									}
								}
							}
						}
					}
				}
			}
			else
			if(ec == GameType.EC_LDRAGEND)
			{
				drag=0;
				changePointer();
			} 
			else
			if(ec == GameType.EC_LDROP)
			{
			}
			else
			if(ec == GameType.EC_RCLICK)
			{
				GameRef dest = new GameRef(param.token(++tok).intValue());
				int cat = dest.getInfo(GameType.GII_CATEGORY);

				if(cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE)
				{
					camera.command("look " + dest.id() + " " + param.token(++tok) + "," + param.token(++tok) + "," + param.token(++tok));
				}
			} 
			else
			if(ec == GameType.EC_RDRAGBEGIN)
			{
				move=1;
				changePointer();

				//enable camera control with mouse
				player.controller.user_Add( Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);
				player.controller.user_Add( Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);

				//disable cursor movement
				player.controller.user_Del( Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0 );
				player.controller.user_Del( Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1 );

				Input.cursor.cursor.command( "lock" );
			} 
			else
			if(ec == GameType.EC_RDRAGEND)
			{
				move=0;
				changePointer();

				//disable camera control with mouse
				player.controller.user_Del( Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1 );
				player.controller.user_Del( Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0 );

				//enable cursor movement
				player.controller.user_Add( Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);
				player.controller.user_Add( Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);

				Input.cursor.cursor.command( "unlock" );
			}
		}
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_BACK):
				GameLogic.changeActiveSection(GameLogic.garage);
				break;

			case(CMD_COLORCATALOG):
				shop=1;
				changePointer();

				ColorShop cs = new ColorShop(painter, multiplayer);
				if(cs.display()) //if something was bought
				{
					painter.paintCans.scrollTo(painter.paintCans.size());
					painter.updateLineIndex();
					painter.updateCanCapacity(0.0f); //this will refresh infoline text
				}
				
				refreshMoneyTxt();
				
				shop=0;
				changePointer();
				break;
		}
	}
}

class ColorShop extends Dialog
{
	Painter		painter;
	Player		player;
	Multiplayer	multiplayer;
	Menu		m;
	int		actGroup, mainGroup, rgbGroup, hsbGroup;

	RenderRef	can;
	RenderRef	light; //backlight

	int color = 0xFF0000;
	int[] hsbValues;
	int quantity, capacity, price;

	Style	textStyle	= new Style(0.55, 0.12,   Frontend.mediumFont, Text.ALIGN_LEFT,  Palette.RGB_GREY, Osd.RRT_TEST);
	Style	textStyle2	= new Style(1.05, 0.12,   Frontend.mediumFont, Text.ALIGN_LEFT,  Palette.RGB_GREY, Osd.RRT_TEST);
	Style	menuStyle	= new Style(0.45, 0.12,   Frontend.mediumFont, Text.ALIGN_CENTER,Osd.RRT_TEST);
	Style	menuStyle2	= new Style(0.45, 0.12,   Frontend.mediumFont, Text.ALIGN_LEFT,  Osd.RRT_TEST);
	Style	sld_h		= new Style(0.43, 0.03,   Frontend.mediumFont, Text.ALIGN_LEFT,  new ResourceRef(Osd.RID_SLD_BACK));
	Style	sld_k		= new Style(0.04, 0.0525, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB));
	Style	sld_bk		= new Style(0.06, 0.07875,Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SW_KNOB));
	Style	sld_bh		= new Style(0.13, 0.0625, Frontend.mediumFont, Text.ALIGN_LEFT,  new ResourceRef(Osd.RID_SW_BACK)); //switch
	Style	sld_lbh		= new Style(0.26, 0.0625, Frontend.mediumFont, Text.ALIGN_LEFT,  new ResourceRef(Osd.RID_SWL_BACK)); //long switch

	Slider[] rgbSlider, hsbSlider;
	Slider	 cpcSlider, qntSlider, mixSwitch;

	Text	priceTxt;

	String	defTitle = "PAINT SHOP";

	final static int	CMD_ESC		= 700;
	final static int	CMD_BUY		= 0;
	final static int	CMD_CANCEL	= 1;
	final static int	CMD_PICK	= 2;
	final static int	CMD_RANDOM	= 3;
	final static int	CMD_CAPACITY	= 4;
	final static int	CMD_QUANTITY	= 5;
	final static int	CMD_MIXER	= 6;

	int			mixer;
	String[]		mixers;
	final static int	MIX_RGB		= 0;
	final static int	MIX_HSB		= 1;

	public ColorShop(Painter p, Multiplayer mp)
	{
		super(GameLogic.player.controller, DF_WIDE|DF_DEFAULTBG|DF_MODAL|DF_DARKEN, defTitle, "BUY; CANCEL");

		escapeCmd = CMD_ESC;
		player = GameLogic.player;
		multiplayer = mp;
		osd.globalHandler = this;
		actGroup = -1;

		rgbSlider = new Slider[3];
		hsbSlider = new Slider[3];
		painter = p;

		mixer = MIX_RGB;
	}

	public void show()
	{
		super.show();

		hsbValues = new int[3];

		String[] sw = new String[2];	sw[0] = "RGB";	sw[1] = "HSB";
		m = osd.createMenu(menuStyle2, -0.565, -0.405, 0, Osd.MD_VERTICAL);
		m.setSliderStyle(sld_bh, sld_bk);
		mixSwitch = m.addItem(null, CMD_MIXER, mixer, 0, sw.length-1, sw.length, null); //switch
		mixSwitch.setValues(sw);

		m = osd.createMenu(menuStyle2, 0.35, -0.125, 0, Osd.MD_VERTICAL);
		m.setSliderStyle(sld_lbh, sld_bk);

		String[] str = new String[5];
		int cpc = PaintCan.MIN_CAPACITY;
		for(int k=0; k<str.length; k++) str[k] = cpc*(k+1) + " ml";
		cpcSlider = m.addItem("Capacity", CMD_CAPACITY, cpc/str.length, 0, str.length-1, str.length, null); //long switch
		cpcSlider.setValues(str);

		str = new String[10];
		int qnt = 1;
		for(int l=0; l<str.length; l++) str[l] = qnt + l + " pcs";
		qntSlider = m.addItem("Quantity", CMD_QUANTITY, qnt-str.length, 0, str.length-1, str.length, null); //long switch
		qntSlider.setValues(str);

		m.setStyle(textStyle2);
		priceTxt = m.addItem("PRICE:");
		updatePrice();

		light = new RenderRef(osd, misc.paintbooth:0x000Er, null);
		can = new RenderRef(osd.id(), frontend:0x0021r, null);
		can.setColor(color);
		Vector3 pos = can.getPos();
		can.setMatrix(new Vector3(pos.x, pos.y, pos.z+3.7), new Ypr(0.0, 0.25, 0.0));

		mainGroup = osd.endGroup();

		m = osd.createMenu(menuStyle, -0.65, -0.225, 0, Osd.MD_VERTICAL);
		m.setSliderStyle(sld_h, sld_k);
		rgbSlider[0] = m.addItem("Red", CMD_PICK, Palette.getChannel(color, Palette.CH_RED)/Palette.MUL_R, 0, 255, 0, null);
		rgbSlider[1] = m.addItem("Green", CMD_PICK, Palette.getChannel(color, Palette.CH_GREEN)/Palette.MUL_G, 0, 255, 0, null);
		rgbSlider[2] = m.addItem("Blue", CMD_PICK, Palette.getChannel(color, Palette.CH_BLUE), 0, 255, 0, null);
		for(int i=0; i<rgbSlider.length; i++) rgbSlider[i].printValue("%.0f");
		m.setStyle(textStyle);
		m.addItem("RANDOM COLOR", CMD_RANDOM);
		actGroup = rgbGroup = osd.endGroup();

		m = osd.createMenu(menuStyle, -0.65, -0.225, 0, Osd.MD_VERTICAL);
		m.setSliderStyle(sld_h, sld_k);
		hsbSlider[0] = m.addItem("Hue", CMD_PICK, hsbValues[0], 0, 359, 0, null);
		hsbSlider[1] = m.addItem("Saturation", CMD_PICK, hsbValues[1], 0, 100, 0, null);
		hsbSlider[2] = m.addItem("Brightness", CMD_PICK, hsbValues[2], 0, 100, 0, null);
		for(int i=0; i<hsbSlider.length; i++) hsbSlider[i].printValue("%.0f");
		m.setStyle(textStyle);
		m.addItem("RANDOM COLOR", CMD_RANDOM);
		hsbGroup = osd.endGroup();
		osd.hideGroup(hsbGroup);

		osdCommand(CMD_RANDOM);

		osdCommand(CMD_CAPACITY); //update capacity
		osdCommand(CMD_QUANTITY); //update quantity
		osdCommand(CMD_PICK); //paint the can

		osd.showGroup(actGroup);
	}

	public void hide()
	{
		light.destroy();
		can.destroy();
		can = null;
		light = null;

		super.hide();
	}

	public void updatePrice()
	{
		price = PaintBooth.calcPaintCanPrice(capacity, quantity);
		priceTxt.changeText("PRICE: $" + price);
	}

	//this moves all sliders to match color value
	public void updateMixers()
	{
		if(mixer == MIX_RGB)
		{
			if(rgbSlider[0]) rgbSlider[0].setValue(Palette.getChannel(color, 1)/Palette.MUL_R);
			if(rgbSlider[1]) rgbSlider[1].setValue(Palette.getChannel(color, 2)/Palette.MUL_G);
			if(rgbSlider[2]) rgbSlider[2].setValue(Palette.getChannel(color, 3));
		}
		else
		{
			for(int i=0; i<hsbSlider.length; i++) if(hsbSlider[i]) hsbSlider[i].setValue(hsbValues[i]);
		}

		can.setColor(color); //finally, color is being applied to paint can, so all changes to color become completely obvious
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_ESC): //cmd=escapeCmd
				osdCommand(CMD_CANCEL);
				break;

			case(CMD_BUY):
				if(player.getMoney() >= price)
				{
					if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.spendMoney(price);
					player.takeMoney(price);
					for(int i=0; i<quantity; i++) painter.paintCans.addItem(new PaintCan(color, capacity));
					super.osdCommand(cmd);
					result = 1;
				}
				else	new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_HIGHPRI, "NOT ENOUGH MONEY", "You don't have $" + price + " to buy this color!").display();
				break;

			case(CMD_CANCEL): //cmd=1
				super.osdCommand(0);
				break;

			case(CMD_PICK):
				if(mixer == MIX_RGB)
				{
					color = Palette.getColor(rgbSlider[0].value, rgbSlider[1].value, rgbSlider[2].value);
					hsbValues = Palette.RGB2HSB(color);
				}
				else
				{
					color = Palette.HSB2RGB(hsbSlider[0].value, hsbSlider[1].value, hsbSlider[2].value);
					for(int k=0; k<hsbValues.length; k++) hsbValues[k] = hsbSlider[k].value;
				}

				updateMixers();
				break;

			case(CMD_RANDOM):
				color = Palette.randomColor();
				hsbValues = Palette.RGB2HSB(color);
				updateMixers();
				break;

			case(CMD_CAPACITY):
				capacity = cpcSlider.getVLabelText().intValue();
				updatePrice();
				break;

			case(CMD_QUANTITY):
				quantity = qntSlider.getVLabelText().intValue();
				updatePrice();
				break;

			case(CMD_MIXER):
				mixer = osd.sliderValue;
				osd.hideGroup(actGroup);

				updateMixers();
				if(mixer == MIX_RGB) actGroup = rgbGroup;
				else actGroup = hsbGroup;
				osd.showGroup(actGroup);
				break;
		}
	}
}