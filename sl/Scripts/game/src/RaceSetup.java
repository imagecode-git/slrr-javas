package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.sound.*;

public class RaceSetup extends GameType implements GameState
{
	final static int[] prizeMax;
	final static int[] prizeStep;
	String[]	prizeValues;
	int		prizeMultiplier;
	int		forMoney;
	Text		prizeText;

	float		timewarp;

	Osd		osd;
	Thread		scrollerThread;
	Text		chooseTxt;

	//common
	GameRef		cursor;
	GameRef		localroot;
	GameRef		click;

	Vector3		pStart, pFinish;
	Marker		mStart, mFinish;

	Racer opponent;	//vs player

	GameState	lastState;

	City		track;

	final static int CMD_RACE = 0;
	final static int CMD_ABANDON = 1;
	final static int CMD_ZOOM_IN = 2;
	final static int CMD_ZOOM_OUT = 3;
	final static int CMD_LESS_MONEY = 4;
	final static int CMD_MORE_MONEY = 5;
	final static int CMD_PRIZE = 6;

	public RaceSetup()
	{
		createNativeInstance();

		prizeMax = new int[GameLogic.CLUBS];
		prizeStep= new int[GameLogic.CLUBS];

		prizeMax[0] = 1000;
		prizeStep[0] = 100;
		prizeMax[1] = 2000;
		prizeStep[1] = 250;
		prizeMax[2] = 4000;
		prizeStep[2] = 500;
	}

	public void run()
	{
		for(;;)
		{
			float	area = 0.1;
			float	min =-0.9;
			float	max = 0.9;
			float	step=10.0;

			Vector3 v = Input.cursor.getPos();
			if(v.x <= min)
			{
				track.nav.offsetX += step*(v.x - min)/area;
				track.nav.updateNavigator(track.player.car);
			}
			else
			if(v.x >= max)
			{
				if(track.nav.offsetX < Config.video_x)
				{
					track.nav.offsetX += step*(v.x - max)/area;
					track.nav.updateNavigator(track.player.car);
				}
			}

			if(v.y <= min)
			{
				track.nav.offsetZ += step*(v.y - min)/area;
				track.nav.updateNavigator(track.player.car);
			}
			else
			if(v.y >= max)
			{
				track.nav.offsetZ += step*(v.y - max)/area;
				track.nav.updateNavigator(track.player.car);
			}

			//if(chooseTxt) chooseTxt.changeText(track.nav.offsetX);

			scrollerThread.sleep(20);
		}
	}

	public void enter(GameState prev_state)
	{
		lastState = prev_state;
		track = lastState;

		if(Frontend.loadingScreen.loadingDialog)
		{
//			Frontend.loadingScreen.hide();
			timewarp = Frontend.loadingScreen.loadingDialog.timewarp;
			Frontend.loadingScreen.loadingDialog.timewarp = 0.0;
			System.timeWarp(0.0);
		}
		else timewarp = System.timeWarp(0.0);

		int prizeTicks = prizeMax[GameLogic.player.club]/prizeStep[GameLogic.player.club];
		prizeValues = new String[prizeTicks];
		for(int i=0; i<prizeTicks; i++)
		{
			if(i==0) prizeValues[i] = "Prestige only";
			else prizeValues[i] = "$" + i*prizeStep[GameLogic.player.club];
		}

		//ha elfogyott a penze, vagy klub csokkenes (uj jatek) akkor nullazzuk a kezdo tetet
		if(forMoney > GameLogic.player.getMoney() || forMoney > prizeMax[GameLogic.player.club] || GameLogic.gameMode == GameLogic.GM_QUICKRACE)
			forMoney=0;

		if(forMoney) prizeMultiplier = forMoney/prizeStep[GameLogic.player.club];
		else prizeMultiplier = 0;

		if(track.challenger == GameLogic.player) opponent=track.challenged;
		else opponent=track.challenger;

		pStart = track.map.getNearestCross(track.player.car.getPos());
		do
		{
			pFinish = track.map.getNearestCross(pStart, 500+Math.random()*300);
		}
		while(!track.map.getStartDirection(pStart, pFinish));

		//display route
		float distance = track.map.getRouteLength(pStart, pFinish);
		RenderRef ltype = new RenderRef(particles:0x00000017r);
		ltype.cache();

		RenderRef line = track.nav.route;
		if(line) line.destroy();
		line = new RenderRef();
		line.plotRoute(track.nav.localroot, ltype, 0xFFFF0000, 10.0, new Vector3(0.01,0,0.01));
		track.nav.route = line;

		mStart = track.nav.addMarker(Marker.RR_START, pStart, 3);
		mFinish = track.nav.addMarker(Marker.RR_FINISH, pFinish, 3);

		osd = new Osd(1.0, 0.0, 15);
		osd.globalHandler = this;
		osd.defSelection=1;
		createOSDObjects();

		updatePrize();

		osd.show();

		if(GameLogic.player.prestige >= opponent.prestige) addNotification(click, GameType.EVENT_CURSOR, GameType.EVENT_SAME, null, "event_handlerClick");

		GameLogic.player.hideOsd();

		setEventMask(EVENT_CURSOR);

		scrollerThread = new Thread(this, "Racesetup map scroller");
		scrollerThread.start();

		if(Frontend.loadingScreen.loadingDialog)
		{
			Frontend.loadingScreen.hide();
			timewarp = 1.0;
		}
		Input.cursor.enable(1);
	}

	public void exit(GameState next_state)
	{
		System.timeWarp(timewarp);

		clearEventMask(EVENT_ANY);

		scrollerThread.stop();

		Input.cursor.enable(0);
		osd.hide();

		deleteOSDObjects();

		track.nav.remMarker(mStart);
		track.nav.remMarker(mFinish);

		GameLogic.player.showOsd();

		lastState = null;
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_RACE):
				track.startRace(pStart, pFinish, forMoney);
				GameLogic.changeActiveSection(track);
				break;

			case(CMD_ABANDON):
				if(GameLogic.gameMode == GameLogic.GM_QUICKRACE) track.raceState = 0;
				else track.abandonRace();
				GameLogic.changeActiveSection(track);
				break;

			case(CMD_ZOOM_IN):
				if(track.nav.zoom > 10)
				{
					track.nav.changeZoom(track.nav.zoom-2);
					track.nav.updateNavigator(track.player.car);
				}
				break;

			case(CMD_ZOOM_OUT):
				if(track.nav.zoom < 30)
				{
					track.nav.changeZoom(track.nav.zoom+2);
					track.nav.updateNavigator(track.player.car);
				}
				break;

			case(CMD_PRIZE):
				prizeMultiplier++;
				updatePrize();
				break;
		}
	}

	public void updatePrize()
	{
		if(prizeMultiplier >= prizeValues.length) prizeMultiplier = 0;

		forMoney = prizeMultiplier*prizeStep[GameLogic.player.club];
		if(forMoney > GameLogic.player.getMoney())
		{
			forMoney = 0;
			prizeMultiplier = 0;
		}

		if(prizeText) prizeText.changeText("Prize: " + prizeValues[prizeMultiplier]);
	}

	public void createOSDObjects()
	{
		osd.createStrongHeader("RACE SETUP");
		chooseTxt=osd.createFooter("CHOOSE TRACK");

		osd.createText(track.challenger.name + " vs " + track.challenged.name , Frontend.mediumFont, Text.ALIGN_RIGHT,	0.98, -0.98);

		if(GameLogic.player.prestige >= opponent.prestige) chooseTxt.changeText("YOU choose the track.");
		else chooseTxt.changeText(opponent.name + " chooses the track.");

		Style buttonStyle = new Style(0.1, 0.1, Frontend.mediumFont, Text.ALIGN_LEFT, null);

		Menu m;
		m = osd.createMenu(buttonStyle, -0.98, 0.92, 0, Osd.MD_HORIZONTAL);
		m.addItem(new ResourceRef(Osd.RID_OK), CMD_RACE, "START RACE", null, 1);
		m.addItem(new ResourceRef(Osd.RID_CANCEL), CMD_ABANDON, "ABANDON RACE", null, 1);
		m.addSeparator();
		m.addItem(new ResourceRef(frontend:0x92E3r), CMD_ZOOM_IN, "ZOOM IN", null, 1);
		m.addItem(new ResourceRef(frontend:0x92E4r), CMD_ZOOM_OUT, "ZOOM OUT", null, 1);
		if(GameLogic.gameMode != GameLogic.GM_QUICKRACE)
		{
			m.addSeparator();
			Gadget g = m.addItem(new ResourceRef(frontend:0x9C0Ar), CMD_PRIZE, "CHANGE THE PRIZE", null, 1);
			prizeText = osd.createText(null, Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 0.87);
		}

		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL|Osd.HK_STATIC, CMD_ABANDON, this);
		osd.createHotkey(Input.RCDIK_PGUP, Input.KEY|Osd.HK_STATIC, CMD_ZOOM_IN, this);
		osd.createHotkey(Input.RCDIK_PGDN, Input.KEY|Osd.HK_STATIC, CMD_ZOOM_OUT, this);

		track.nav.offsetX=track.nav.offsetZ=0.0;
		track.nav.changeSize(0.0, 0.0, 1.0, 0.93);
		track.nav.changeMode(0);
		track.nav.changeZoom(20.0);
		track.nav.updateNavigator(track.player.car);

		click = new GameRef(track.nav.localroot, frontend:0x0065r, "0,0,0 0,0,0", "navigator click-object");
	}

	public void deleteOSDObjects()
	{
		click.destroy();

		track.nav.changeSize(0.02, 0.78, 0.2, 0.18);
		track.nav.changeMode(Config.gpsMode);
		track.nav.offsetX=track.nav.offsetZ=0.0;
		track.nav.changeZoom(Navigator.DEF_ZOOM);
		track.nav.updateNavigator(track.player.car);
	}

	public void event_handlerClick(GameRef obj_ref, int event, String param)
	{
		int ec = param.token(0).intValue();

		if(ec == GameType.EC_LCLICK)
		{
			Vector3 v = Input.cursor.getPickedPos();
			v.mul(100);

			Vector3 temp = track.map.getNearestCross(v);

			if(track.map.getStartDirection(pStart, temp))
			{
				new SfxRef(Frontend.SFX_MENU_SELECT).play();

				pFinish = temp;

				float distance = track.map.getRouteLength(pStart, pFinish);

				RenderRef ltype = new RenderRef(particles:0x00000017r);
				ltype.cache();

				RenderRef line = track.nav.route;
				if(line) line.destroy();
				line = new RenderRef();
				line.plotRoute(track.nav.localroot, ltype, 0xFFFF0000, 10.0, new Vector3(0.01,0,0.01));
				track.nav.route = line;

				track.nav.remMarker(mFinish);
				mFinish = track.nav.addMarker(Marker.RR_FINISH, pFinish, 3);
				track.nav.updateNavigator(track.player.car);
			}
			else new SfxRef(Frontend.SFX_WARNING).play();
		}
	}
}
