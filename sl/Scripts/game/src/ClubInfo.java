package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

public class ClubInfo extends GameType implements GameState
{
	final static int  RID_BG = frontend:0x009Fr;

	GameState	parentState;
	Osd		osd;

	final static int  CMD_BACK = 0;
	final static int  CMD_INFO = 1;
	final static int  CMD_ACHIEVEMENTS = 2;


	public ClubInfo()
	{
		createNativeInstance();
	}

	public void enter( GameState prevState )
	{
		if(!parentState) parentState=prevState;

		osd = new Osd();
		osd.globalHandler = this;

		createOSDObjects();
		osd.show();

		Input.cursor.enable(1);

		setEventMask( EVENT_CURSOR );
	}

	public void exit( GameState nextState )
	{
		clearEventMask( EVENT_ANY );
		Input.cursor.enable(0);
		osd.hide();
	}

	public void osdCommand( int cmd )
	{
		switch(cmd)
		{
			case(CMD_BACK):
				GameLogic.changeActiveSection(parentState);
				break;

			case(CMD_INFO):
				new WarningDialog(GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "ADDITIONAL INFO", "Your name and position is marked RED. Club members marked YELLOW are ready to be challenged at daytime. \n The higher prestige you have, the more club members will like to race with you, day or night. Prestige grows as you won races and lose the police in a chase, but decreases if you loose a race or the police fines you. \n Night races affect your ranking besides your prestige but prepare, night race prizes are very high.").display();
				break;

			case(CMD_ACHIEVEMENTS):
				new CareerGoalsDialog().display();
				break;
		}
	}

	//RAXAT: v2.3.1, table with racers is more efficiently aligned now
	public void createOSDObjects()
	{
		osd.createBG(new ResourceRef(RID_BG+GameLogic.player.club));

		osd.createStrongHeader("CLUB INFORMATION");
		osd.createFooter(GameLogic.CLUBNAMES[GameLogic.player.club]);

		Style buttonStyle = new Style( 0.11, 0.11, Frontend.mediumFont, Text.ALIGN_RIGHT, null );
		Menu m = osd.createMenu(buttonStyle, 0.975, -0.93, 0, Osd.MD_HORIZONTAL);

		m.addItem( new ResourceRef( frontend:0x00009C0Er ), CMD_BACK, "GO BACK TO GARAGE", null, 1 );
		m.addItem( new ResourceRef( frontend:0x00009C08r ), CMD_ACHIEVEMENTS, "ACHIEVEMENTS", null, 1 );
		m.addItem( new ResourceRef( frontend:0x00009C11r ), CMD_INFO, "ADDITIONAL INFO", null, 1 );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL|Osd.HK_STATIC, CMD_BACK, this );

		ResourceRef charset = Frontend.mediumFont;
		float spacing = Text.getLineSpacing(charset, osd.vp); //RAXAT: we also try to measure width of each text symbol with this
		float ypos = GameLogic.CLUBMEMBERS*spacing*0.43; //RAXAT: pre-calculated, centered position of entire table

		int color;
		int base=GameLogic.player.club*GameLogic.CLUBMEMBERS;
		String name;
		for(int i=0; i<GameLogic.CLUBMEMBERS; i++)
		{
			if(GameLogic.speedymen[base+i] == GameLogic.player)
			{
				color=0xFFFF5555;
				name = GameLogic.speedymen[base+i].getFullName();
			}
			else
			{
				if(GameLogic.canChallenge(GameLogic.player, GameLogic.speedymen[base+i])) color=0xFFFFFF55;
				else color=0xFFFFFFFF;

				name = GameLogic.speedymen[base+i].profile.getFullName();
			}

			//RAXAT: spacing is used here mostly as an alignment basic unit
			osd.createText( (GameLogic.CLUBMEMBERS-i) + ". ", charset, Text.ALIGN_RIGHT, -1+(4.75*spacing),  ypos-(i*spacing)).changeColor( color );
			osd.createText( name, charset, Text.ALIGN_LEFT, -1+(4.75*spacing),  ypos-(i*spacing)).changeColor( color );

			int prestige = GameLogic.speedymen[base+i].prestige*Racer.PRESTIGE_SCALE;
			osd.createText( "prestige: " + prestige, charset, Text.ALIGN_LEFT,	 1-(7*spacing),  ypos-(i*spacing)).changeColor( color );
		}
	}	
}
