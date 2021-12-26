package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

import parts:part.root.BodyPart.*;

//BUG: race results dialog may not appear while watching AI race
//BUG: camera could stuck targeted on the opponents while watching AI race
//todo: let player to gain pink slip tokens, so he could select pink slip option himself in bet dialog
public class City extends Multiplayer //RAXAT: all multiplayer maps must extend Multiplayer, NOT Track
{
	final static int		DEBUG_MODE = false;
	
	final static float		SPEED_LIMIT	= 18.0; // 18m/s = 66KPH
	final static float		SPEED_LIMIT_SQ	= SPEED_LIMIT*SPEED_LIMIT;

	final static float		TINY_SPEED_SQ	= 9.0;

	final static int		GRI_POLICECAR	= cars.misc.Police:0x0006r;
	final static GameRef	GRT_POLICECAR	= new GameRef( GRI_POLICECAR );

	final static int		RID_BUNTESS	= frontend:0x0094r;
	final static int		RID_NRSTARTBG	= frontend:0x00b6r;
	final static int		RID_NRFIN_WIN	= frontend:0x00a7r;
	final static int		RID_NRFIN_LOSE	= frontend:0x00c9r;
	final static int		RID_NRFIN_AI	= frontend:0x00ccr;
	final static int		RID_SPEECH3	= sound:0x001dr;
	final static int		RID_SPEECH2	= sound:0x001cr;
	final static int		RID_SPEECH1	= sound:0x001br;
	final static int		RID_SPEECHGO	= sound:0x001er;
	final static int		RID_SPEECHYOUWIN = sound:0x001fr;
	final static int		RID_APPLAUSE1	= sound:0x0021r;
	final static int		RID_APPLAUSE2	= sound:0x0022r;
	final static int		GREEN_ARROW	= frontend:0x0070r;

	final static float		ODDS_CASH_1 = 1.00;
	final static float		ODDS_CASH_2 = 0.97;
	final static float		ODDS_PINKS  = 0.94;

	final static int		RID_SFX_DAY_WIN = sound:0x0023r;
	final static int		RID_SFX_DAY_LOOSE = sound:0x0024r;

	final static int		RID_DAY_WIN = frontend:0x00CFr;
	final static int		RID_DAY_LOOSE = frontend:0x00D0r;

	final static int		RID_DAY_CHALLENGE = frontend:0x00A5r;

	final static ResourceRef	RRT_FRAME = new ResourceRef(frontend:0x00CBr);
	
	final static int		NR_INVALID	=-1;
	final static int		NR_IDLE		= 0;
	final static int		NR_SHOWSTART	= 1;
	final static int		NR_3		= 2;
	final static int		NR_2		= 3;
	final static int		NR_1		= 4;
	final static int		NR_START	= 5;
	final static int		NR_RACE		= 6;
	final static int		NR_FINISH	= 7;
	final static int		NR_SHOWFINISH	= 8;

	//RAXAT: these cmd's must not interfere with Track.class ones!
	final static int		CMD_PARTICIPATE		= 1001;
	final static int		CMD_WATCH_RACE		= 1002;
	final static int		CMD_STOP_WATCHING	= 1003;
	final static int		CMD_DEBUG		= 1004; //RAXAT: debug!!


	Vector3[]	posGarage = new Vector3[GameLogic.CLUBS];
	Ypr[]		oriGarage = new Ypr[GameLogic.CLUBS];


	Text		statusTxt, oppStatusTxt;
	int			oppStatusDisplayed, collision, time, prize;

	//day race
	Vector3		raceStart, raceFinish;
	Trigger		trRaceFinish;
	RenderRef	finishObject;
	RaceDialog	raceDialog;

	Racer		challenger, challenged;
	Marker		mStart, mFinish;

	Vehicle		ghostVhc;
	Bot			raceBot, demoBot;
	Marker		mRaceBot;

	int			raceState; //0-nop 1-race 2-race after the winner crossed the finish line
	int			aiChallengeState, abandoned, abandoned2;

	Vector		opponentCars = new Vector(); //TrafficTracker

	//night race
	GameRef		nrcameraTarget;
	int			nightTime;
	RenderRef	nrStarterLady;
	Animation	nrStarterLadyAnim;
	int			nrStat;
	int			nrDelay = 0;
	int			nrBotID1, nrBotID2;
	Vector3		pS, pF, dirS, dirF;
	Ypr			oriS, oriF;
	Bot			nrBot1, nrBot2;
	Trigger		nrFinishTrigger;
	Trigger		nrFinishSoundTrigger;

	int			nrPlayerRace; //0-semmi; 1-varakozas; 2-van
	int			nrNear, nrWatching, nrPlayerPaused, nrShowRaceStart, nrShowRaceFinish, backupnrWatching, nrPrize;

	ResourceRef[]	fakers;
	ParkingCar[]	parkingCars;
	RenderRef[]		nrMen;
	Animation[]		nrMenAnim;

	float		yawNudge = 0.2;
	float		posNudge = 0.3;

	float		nrStartTime, nrTime1, nrTime2;

	int			nightRaceGroup;	// osd group
	int			nrWatchingGroup;
	int			nrLookAt;
	int			nrFinished1, nrFinished2, nrQuit;

	ResourceRef	nrHead1, nrHead2;
	String		nrName1, nrName2;
	Racer		pl1, pl2;

	Vector		nrOpponents = new Vector();
	NightracesData	nrData = new NightracesData();

	int[]		nrPrizeList;
	int			nrLastPlaceDay = -1;
	int			nrcameraMode_before;
	int			applauseSfxID, applauseSfxOn;

	SfxRef		applause1 = new SfxRef(RID_APPLAUSE1);
	SfxRef		applause2 = new SfxRef(RID_APPLAUSE2);

	SfxRef		speech3 = new SfxRef(RID_SPEECH3);
	SfxRef		speech2 = new SfxRef(RID_SPEECH2);
	SfxRef		speech1 = new SfxRef(RID_SPEECH1);

	SfxRef		speechGO = new SfxRef(RID_SPEECHGO);
	SfxRef		speechYOUWIN = new SfxRef(RID_SPEECHYOUWIN);

	SfxRef		speechDAYYOUWIN = new SfxRef(RID_SFX_DAY_WIN);
	SfxRef		speechDAYYOULOOSE = new SfxRef(RID_SFX_DAY_LOOSE);
	
	//police stuff
	int			maxScouts = 2;

	Vector		policeCars = new Vector();	//TrafficTracker
	Vector		alertedScouts = new Vector();	//PoliceScout

	float		overSpeed, crashes, fleedAway;
	float		lastCollisionTime, lastAlertTime, firstAlertTime, pullOverTime;
	int			policeState, roamFree, backupCam;
	
	Multiplayer multiplayer; //RAXAT: we leave it here for static calls, even despite this track is a Multiplayer instance itself

	public void enter(GameState prev_state)
	{
		if(DEBUG_MODE)
		{
			//RAXAT: traffic vehicle quick tester
			//player.car = new Vehicle();
			//player.car.create_native(map, new GameRef(cars.traffic.CivilVan_2:0x00000006r), "0,-10000,0,0,0,0", "my_ride" );
			
			posStart = pS;
			oriStart = oriS;
		}
		
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
		{
			map = new GroundRef( maps.city:0x00000001r );
			nav = new Navigator( -23.482, -24.45, 5.828, maps.city.smallmap:0x00000001r, maps.city.smallmap:0x00000002r, maps.city.smallmap:0x00000005r, 8, 8, 8 );
			map.setWater(new Vector3(0.0,-8.0,-1500.0), new Vector3(0.0,1.0,0.0), 300.0, 50.0);
			map.addWaterLimit(new Vector3(0.0,0.0,-500.0), new Vector3(0.0,0.0,1.0));
		}
		
		Integrator.isCity = 1;

        if(prev_state instanceof RaceSetup)
		{
			Frontend.loadingScreen.show();
			osd.show();
		}
		else
		{
			activeTrigger=-1;

			abandoned=raceState = 0;
			aiChallengeState = 0;
			policeState = 0;
			roamFree = 0;

			overSpeed = 0;
			crashes = 0;
			fleedAway = 0;

			nightTime = 0;

			if(GameLogic.gameMode == GameLogic.GM_FREERIDE || GameLogic.gameMode == GameLogic.GM_SINGLECAR)
			{
				//NOP
			} 
			else
			if(GameLogic.gameMode == GameLogic.GM_QUICKRACE)
			{
				createQuickRaceBot();
				changeCamTarget2(raceBot.car);
			} 
			else
			if(GameLogic.gameMode == GameLogic.GM_DEMO)
			{
				osdEnabled = 0;
				enableOsd(osdEnabled);

				if(1)
				{
					if(player.car)
					{
						player.car.destroy();
						player.controller.command("leave " + player.car.id());
						player.car = null;
					}

					raceBot = new Bot(Math.random()*59, Math.random()*1234, Math.random(), 2.0, 2.0, 1.0);
					demoBot = new Bot(Math.random()*59, Math.random()*1234, Math.random(), 2.0, 2.0, 1.0);

					raceBot.command("osd 0");
					demoBot.command("osd 0");

					VehicleDescriptor vd = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
					raceBot.createCar(map, new Vehicle(map, vd.id, vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear));

					vd = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
					demoBot.createCar(map, new Vehicle(map, vd.id, vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear));

					if(raceBot.car)
					{
						raceBot.car.setParent(map);
						raceBot.car.setMatrix(posStart, oriStart);
						raceBot.car.command("reset");
						raceBot.car.command("reload"); //RAXAT: remove this!! gives infinite NOS
					}

					if(demoBot.car)
					{
						demoBot.car.setParent(map);
						demoBot.car.setMatrix(posStart, oriStart);
						demoBot.car.command("reset");
						demoBot.car.command("reload"); //RAXAT: remove this!! gives infinite NOS
					}

					player.car = demoBot.car; //RAXAT: since player is not actually racing in demo mode, but GameLogic requires its vehicle instance

					changeCamTarget(demoBot.car);
					changeCamTarget2(raceBot.car);
				}
			}
			else
			{
				float hour = GameLogic.getTime()/3600;

				if(hour < 4 || hour > 22) //night race time
				{
					nightTime = 1;

					nrStat = NR_IDLE;
					fakers = new ResourceRef(cars:0x0027r).getChildNodes();

					parkingCars = new ParkingCar[20];
					int np = 0;

					int nm = 0;
					nrMen = new RenderRef[20];
					nrMenAnim = new Animation[20];

					float halfStreetWidth = 8;
					float parkingYaw = 0.7;
					
					Vector3[] alignedPos;
					
					alignedPos = map.alignToRoad(pS);
					pS = new Vector3(alignedPos[0]);

					alignedPos = map.alignToRoad(pF);
					pF = new Vector3(alignedPos[0]);

					int[] manID = new int[6];
					manID[0] = humans:0x002fr;
					manID[1] = humans:0x0030r;
					manID[2] = humans:0x0031r;
					manID[3] = humans:0x0032r;
					manID[4] = humans:0x0033r;
					manID[5] = humans:0x0034r;

					int[] manAnimID = new int[6];
					manAnimID[0] = humans:0x0014r;
					manAnimID[1] = humans:0x001br;
					manAnimID[2] = humans:0x007dr;
					manAnimID[3] = humans:0x007er;
					manAnimID[4] = humans:0x0017r;
					manAnimID[5] = humans:0x007fr;

					Vector3 spacin, sideoffs, botPos;
					Ypr oriCars;
					RenderRef rr;
					
					//start pos
					alignedPos = map.alignToRoad(pS);
					alignedPos[1].normalize();

					spacin = new Vector3(alignedPos[1]);
					spacin.mul(2.5);

					sideoffs = new Vector3(alignedPos[1]);
					sideoffs.y = 0;
					sideoffs.mul(halfStreetWidth);
					sideoffs.rotate(new Ypr(1.57, 0.0, 0.0));

					for(int i=0; i<5; i++)
					{
						float	pNudge, yNudge;
						int	idx, colorSeed, man;

						pNudge = (Math.random()*2-1)*posNudge;
						yNudge = (Math.random()*2-1)*yawNudge;
						idx = Math.random()*fakers.length;
						colorSeed = Math.random()*12345;

						botPos = new Vector3(alignedPos[0]);
						botPos.add(sideoffs);

						oriCars = new Ypr(alignedPos[1]);
						oriCars.y -= parkingYaw + yNudge;
						parkingCars[np++] = new ParkingCar(map, fakers[idx], botPos, oriCars, colorSeed);

						botPos.add(spacin);
						man = Math.random()*manID.length;

						nrMen[nm] = new RenderRef(map, manID[man], "tesztmen");
						nrMenAnim[nm] = new Animation(nrMen[nm], new ResourceRef(manAnimID[man]));
						nrMenAnim[nm].setSpeed(0.5+Math.random());
						nrMenAnim[nm].loopPlay();

						oriCars.y+=3.14;
						nrMen [nm].setMatrix(botPos, oriCars);
						oriCars.y-=3.14;
						nm++;

						pNudge = (Math.random()*2-1)*posNudge;
						yNudge = (Math.random()*2-1)*yawNudge;
						idx = Math.random()*fakers.length;
						colorSeed = Math.random()*12345;

						botPos = new Vector3(alignedPos[0]);
						botPos.sub(sideoffs);

						oriCars = new Ypr(alignedPos[1]);
						oriCars.y += parkingYaw + yNudge;
						parkingCars[np++] = new ParkingCar(map, fakers[idx], botPos, oriCars, colorSeed);

						botPos.add(spacin);
						man = Math.random()*manID.length;

						nrMen [nm] = new RenderRef(map, manID[man], "tesztmen");
						nrMenAnim[nm] = new Animation(nrMen[nm], new ResourceRef(manAnimID[man]));
						nrMenAnim[nm].setSpeed(0.5+Math.random());
						nrMenAnim[nm].loopPlay();

						oriCars.y+=3.14;
						nrMen [nm].setMatrix(botPos, oriCars);
						oriCars.y-=3.14;
						nm++;

						alignedPos[0].add(spacin);
						alignedPos[0].add(spacin);
						alignedPos = map.alignToRoad(alignedPos[0]);
						alignedPos[1].normalize();
					}


					//finish pos
					alignedPos = map.alignToRoad(pF);
					alignedPos[1].normalize();

					spacin = new Vector3(alignedPos[1]);
					spacin.mul(2.5);

					sideoffs = new Vector3(alignedPos[1]);
					sideoffs.y = 0;
					sideoffs.mul(halfStreetWidth);
					sideoffs.rotate(new Ypr(1.57,0.0,0.0));

					for(int i=0; i<5; i++)
					{
						float	pNudge, yNudge;
						int	idx, colorSeed, man;

						pNudge = (Math.random()*2-1)*posNudge;
						yNudge = (Math.random()*2-1)*yawNudge;
						idx = Math.random()*fakers.length;
						colorSeed = Math.random()*12345;

						botPos = new Vector3(alignedPos[0]);
						botPos.add(sideoffs);
						oriCars = new Ypr(alignedPos[1]);
						oriCars.y -= parkingYaw + yNudge;
						parkingCars[ np++ ] = new ParkingCar(map, fakers[idx], botPos, oriCars, colorSeed);

						botPos.add(spacin);
						man = Math.random()*4;

						nrMen [nm] = new RenderRef(map, manID[man], "tesztmen");
						nrMenAnim[nm] = new Animation(nrMen[nm], new ResourceRef(manAnimID[man]));
						nrMenAnim[nm].setSpeed(0.5+Math.random());
						nrMenAnim[nm].loopPlay();

						oriCars.y+=3.14;
						nrMen [nm].setMatrix(botPos, oriCars);
						oriCars.y-=3.14;
						nm++;

						pNudge = (Math.random()*2-1)*posNudge;
						yNudge = (Math.random()*2-1)*yawNudge;
						idx = Math.random()*fakers.length;
						colorSeed = Math.random()*12345;

						botPos = new Vector3(alignedPos[0]);
						botPos.sub(sideoffs);
						oriCars = new Ypr(alignedPos[1]);
						oriCars.y += parkingYaw + yNudge;
						parkingCars[np++] = new ParkingCar(map, fakers[idx], botPos, oriCars, colorSeed);

						botPos.add(spacin);
						man = Math.random()*4;
						nrMen [nm] = new RenderRef(map, manID[man], "tesztmen");
						nrMenAnim[nm] = new Animation(nrMen[nm], new ResourceRef(manAnimID[ man ]));
						nrMenAnim[nm].setSpeed(0.5+Math.random());
						nrMenAnim[nm].loopPlay();

						oriCars.y+=3.14;
						nrMen [nm].setMatrix(botPos, oriCars);
						oriCars.y-=3.14;
						nm++;

						alignedPos[0].add(spacin);
						alignedPos[0].add(spacin);
						alignedPos = map.alignToRoad(alignedPos[0]);
						alignedPos[1].normalize();
					}

					//RAXAT: replace this ugly arrow with some nice semi-transparent 2D texture polygon? (with lens effect camera angle follow, so it will be always in front of player's view point)
					finishObject = new RenderRef(map, GREEN_ARROW, "finishObject");
					finishObject.setMatrix(new Vector3(pF.x, pF.y + 3.0, pF.z), null);

					addTrigger(pS, null, Marker.RR_START, "event_handlerNrStart", 13, "night race start trigger");
				}
			}
		}

		new SfxRef(sound:0x0001r).cache();	//def idle
		new SfxRef(sound:0x0002r).cache();	//def down
		new SfxRef(sound:0x0003r).cache();	//def up
		new SfxRef(sound:0x0016r).precache();	//police siren

		super.enter(prev_state); //waits for loading to finish

		osd.createHotkey(Input.RCDIK_9, Input.KEY|Osd.HK_STATIC, CMD_DEBUG, Event.F_KEY_PRESS); //RAXAT: debug!!
		if(prev_state instanceof RaceSetup)
		{
			//NOP
		}
		else
		{
			setEventMask(EVENT_COLLISION);
			addNotification(GameLogic.player.car, EVENT_COLLISION, EVENT_SAME, null);

			if(GameLogic.gameMode == GameLogic.GM_QUICKRACE) changeCamTarget2(raceBot.car);
			if(GameLogic.gameMode != GameLogic.GM_DEMO && GameLogic.gameMode != GameLogic.GM_SINGLECAR && GameLogic.gameMode != GameLogic.GM_MULTIPLAYER)
			{
				statusTxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT, -0.98, -0.97);
				statusTxt.setColor(0x90FFFFFF);

				if(GameLogic.gameMode != GameLogic.GM_FREERIDE)
				{
					oppStatusTxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT, -0.98, -0.89);

					if(GameLogic.gameMode == GameLogic.GM_QUICKRACE)
					{
						String txt = raceBot.name;
						if(raceBot.car.chassis) txt += " (" + raceBot.car.chassis.vehicleName + ")";

						oppStatusTxt.changeText(txt);
						oppStatusTxt.setColor(0x90FFFFFF);
					}
				}
			}

			if(GameLogic.gameMode == GameLogic.GM_CARREER)
			{
				String tip;

				if(nightTime)
				{
					if(player.checkHint(Player.H_NIGHTCITY))
					{
						tip = "At night the racing community gathers to arrange drag races for high prizes even for pink slips! \n You can find them at the location marked with the green flag, to race or just to watch others race.";
					}
				}
				else
				{
					if(player.checkHint(Player.H_DAYCITY)) tip = "Find opponents from your club to race, for money prizes or just for prestige. \n Get to know them so you'll know who to race at the night races! (Night races take place around midnight) \n Beware of the cops, pulling over is often easier than trying to get away.";
				}

				if(tip) new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "Welcome to ValoCity!", tip).display();
			}

			osd.endGroup();
			osd.globalHandler = this;

			Style butt0 = new Style(0.3, 0.175, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);

			osd.createRectangle(-0.825, -0.4, 0.425, 0.175, -1, new ResourceRef(frontend:0x0000019Cr));
			osd.createRectangle(-0.825, -0.225, 0.425, 0.175, -1, new ResourceRef(frontend:0x0000019Cr));

			Menu m;

			m = osd.createMenu(butt0, -1.0075, -0.465, 0);
			m.addItem("PARTICIPATE", CMD_PARTICIPATE);
			m.addItem("WATCH RACE", CMD_WATCH_RACE);
			osd.hideGroup(nightRaceGroup = osd.endGroup());

			osd.createRectangle(-0.825, -0.4, 0.425, 0.175, -1, new ResourceRef(frontend:0x0000019Cr));
			osd.createRectangle(-0.825, -0.225, 0.425, 0.175, -1, new ResourceRef(frontend:0x0000019Cr));

			m = osd.createMenu(butt0, -1.0075, -0.465, 0);
			m.addItem("STOP WATCHING", CMD_STOP_WATCHING);
			m.addItem("PARTICIPATE", CMD_PARTICIPATE);
			osd.hideGroup(nrWatchingGroup = osd.endGroup());
		}
		refreshStatus();
		
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.RPC("inCity", null);
	}

	public void exit(GameState next_state)
	{
        if(next_state instanceof RaceSetup) osd.hide();
		else
		{
			int i;

			clearEventMask(EVENT_COLLISION);

			if(raceState) cleanupRace();
			removeAllTimers();
			destroyRaceBot();

			if(demoBot)
			{
				demoBot.deleteCar();
				demoBot=null;
			}

			if(GameLogic.gameMode == GameLogic.GM_DEMO || GameLogic.gameMode == GameLogic.GM_QUICKRACE || GameLogic.gameMode == GameLogic.GM_FREERIDE)
			{
				killCar = 1;

				for(i=alertedScouts.size()-1; i>=0; i--)
				{
					PoliceScout pc = alertedScouts.removeLastElement();
					pc.bot.deleteCar();
					nav.remMarker(pc.tracker.m);
				}

				for(i=policeCars.size()-1; i>=0; i--)
				{
					TrafficTracker tt = policeCars.removeLastElement();
					tt.car.release();
					nav.remMarker(tt.m);
				}

				for(i=opponentCars.size()-1; i>=0; i--)
				{
					TrafficTracker tt = opponentCars.removeLastElement();
					tt.car.release();
					nav.remMarker(tt.m);
				}

				cleanupNightRace();

				parkingCars = null;
				nrMen = null;
				fakers = null;
	        }
		}


		Integrator.isCity = 0; //patch to fix all city detection bugs
		super.exit(next_state);
	}
	
	public void prepareNightRace()
	{
		int nrPlace;
		
		if(nrLastPlaceDay != GameLogic.day)
		{
			nrLastPlaceDay = GameLogic.day;
			nrPlace = Math.random()*5 + GameLogic.player.club*6;
		}
		
		if(DEBUG_MODE) nrPlace = 5;

		if(1)
		{
			pS = nrData.startPos[nrPlace];
			pF = nrData.finishPos[nrPlace];

			oriS = nrData.startOri[nrPlace];
			oriF = nrData.finishOri[nrPlace];

			dirS = new Vector3(oriS);
			dirF = new Vector3(oriF);
		}
		else
		{
			pS = map.getNearestCross(posStart);
			pF = map.getNearestCross(pS, 150);

			//normalized vectors
			dirS = map.getStartDirection(pS, pF);
			dirF = map.getStartDirection(pF, pS);

			oriS = new Ypr(dirS);
			oriF = new Ypr(dirF);

			Vector3 tmp = new Vector3(dirS);
			tmp.mul(20.0);
			pS.add(tmp);

			tmp = new Vector3(dirF);
			tmp.mul(20.0);
			pF.add(tmp);
		}
		
		map.haltTrafficPath(pS, pF);

		nrNear = 0;
		nrWatching = 0;

		nrPlayerPaused = 0;
		nrPlayerRace = 0;

		nrShowRaceStart = 0;
		nrShowRaceFinish = 0;
	}

	public void refreshStatus()
	{
		if(statusTxt)
		{
			String txt;
			if(player.car)
			{
				if(GameLogic.gameMode == GameLogic.GM_QUICKRACE || GameLogic.gameMode == GameLogic.GM_FREERIDE) txt = player.name;
				else
				{
					int ranking = (GameLogic.CLUBMEMBERS-(GameLogic.findRacer(player)-GameLogic.CLUBMEMBERS*player.club));
					txt = player.name + "  " + player.club + "/" + ranking + "  $" + player.getMoney() + " > " + player.getPrestigeString();
				}

				if(player.car && player.car.chassis)
				txt = txt + " (" + player.car.chassis.vehicleName + ")";
			}
			statusTxt.changeText(txt);
		}
	}

	public void alertPolice()
	{
		float time = System.simTime();
		if(time-lastAlertTime < 3.0) return;

		lastAlertTime = time;
	        if(roamFree) return;

		int chasingScouts;
		float maxdst;

		PoliceScout pc, maxpc;
		for(int i=alertedScouts.size()-1; i>=0; i--)
		{
			if(!(pc=alertedScouts.elementAt(i)).returningTraffic)
			{
				chasingScouts++;
				if(pc.distance > maxdst)
				{
					maxdst = pc.distance;
					maxpc = pc;
				}
			}
		}

		float	distance=50.0+25*player.club;
		float	d;
		int	j;

		TrafficTracker theOne;

		for(int i=0; i<policeCars.size(); i++)
		{
			TrafficTracker tt = policeCars.elementAt(i);
			GameRef pc = tt.car;

			if(pc.id())
			{
				tt.trafficId = pc.getInfo(GII_CAR_TRAFFICPTR);
				if(tt.trafficId)
				{
					d = map.getRouteLength(pc.getPos(), player.car.getPos());
					if(d>0 && d<distance)
					{
						distance = d;
						theOne = tt;
						j=i;
					}
				}
			}
			else
			{
				policeCars.removeElementAt(i);
				nav.remMarker(tt.m);
			}
		}

		if(theOne)
		{
			if(distance < 80.0)
			{
				int killTheLamest; 

				if(chasingScouts>=maxScouts)
				{
					if(distance*3 < maxdst)
					{
						killTheLamest = 1;
						chasingScouts--;
					}
				}

				if(chasingScouts<maxScouts)
				{
					policeCars.removeElementAt(j);

					PoliceScout pc = new PoliceScout();
					pc.distance = distance;
					pc.tracker = theOne;

					wakePoliceScout(pc);
					alertedScouts.addElement(pc);
				}

				if(killTheLamest) sleepPoliceScoutQuick(maxpc);
			}
		}
	}

	public void wakePoliceScout(PoliceScout pc)
	{
		if(!policeState) firstAlertTime=System.simTime();

		pc.bot = new Bot(0, 12345, 0.0+player.club*0.25); //0~0.75
		pc.bot.setDriverObject(GameLogic.HUMAN_POLICEMAN);

		pc.bot.createCar(map, new Vehicle(pc.tracker.car));
		pc.bot.traffic_id = pc.tracker.trafficId;
		pc.bot.imaPoliceDriver=1;

		policeState = 10;
		pc.bot.followCar(player.car, 10);
		pc.bot.pressHorn();

		if(GameLogic.gameMode != GameLogic.GM_DEMO) changeCamTarget2(pc.bot.car);
	}

	public void sleepPoliceScout(PoliceScout pc)
	{
		pc.bot.releaseHorn();

		pc.returningTraffic = 1;
		pc.bot.addNotification(pc.bot.car, EVENT_COMMAND, EVENT_SAME, null);
		pc.bot.reJoinTraffic();
	}

	public void sleepPoliceScoutQuick(PoliceScout pc)
	{
		Vector3 pos = pc.bot.car.getPos();
		nav.remMarker(pc.tracker.m);
		pc.bot.releaseHorn();
		pc.bot.deleteCar();

		alertedScouts.removeElement(pc);
		map.addTrafficP(GRT_POLICECAR, pos, 1, 2, 5, 2);
	}

	//RAXAT: v2.3.1, quickrace bots are being picked from the database
	public void createQuickRaceBot()
	{
		int characterIndex = Math.random()*59;

		raceBot = new Bot(characterIndex, Math.random()*1234, Math.random(), 2.0, 2.0, 1.0);
		raceBot.botVd = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);

		Vector3 pos = player.car.getPos();
		Vector3 vel = player.car.getVel();

		vel.normalize();
		vel.mul(500.0f);

		pos.add(vel);
		raceBot.createCar(map);
	}


	public void destroyRaceBot()
	{
		if(raceBot)
		{
            		if(mRaceBot)
			{
				nav.remMarker(mRaceBot);
				mRaceBot=null;
			}

			raceBot.deleteCar();
			raceBot=null;
		}
	}

	public void startRace(Vector3 pStart, Vector3 pFinish, int moneyprize)
	{
        	raceStart = pStart;
			raceFinish = pFinish;

	        prize = moneyprize;

		Vector3 startDir = map.getStartDirection(pStart, pFinish);
		Ypr startOri = new Ypr(startDir);

		if(raceBot.dummycar) //kill & replace dummycar
		{
			Vector3	pos = raceBot.dummycar.getPos();
			Ypr	ori = raceBot.dummycar.getOri();

			if(raceBot.brain)
			{
				raceBot.brain.destroy();
				raceBot.brain = null;

				raceBot.car.release();
				raceBot.car = null;
			}
			else
			{
				map.remTrafficCar(raceBot.traffic_id);
				raceBot.traffic_id = 0;
			}

			raceBot.dummycar.command("reset");
			raceBot.dummycar.setMatrix(new Vector3(0.0, -10000.0, 0.0), ori);
			raceBot.dummycar.setParent(raceBot);

			raceBot.createCar(map, 0); //RAXAT: v2.3.1, loading bot's database vehicle here
			raceBot.car.command("reset");
			raceBot.car.setMatrix(pos, ori);
			raceBot.car.setParent(map);
		}
		
		raceBot.stop();
		mRaceBot = nav.addMarker(raceBot);

		map.haltTrafficCross(raceStart, 15.0);

		Vector3 camPos = new Vector3(startDir);
		camPos.mul(-7.0);
		camPos.y += 3;
		camPos.add(raceStart);

		if(cam)
		{
			Ypr ypr = new Ypr(startOri);
			ypr.p -= 0.3;
			cam.setMatrix(camPos, startOri);
		}

		Vector3 posLeft = new Vector3(startDir);
		posLeft.mul(1.75);
		posLeft.rotate(new Ypr(1.57, 0.0, 0.0));
		posLeft.add(raceStart);

		Vector3 posRight = new Vector3(startDir);
		posRight.mul(1.75);
		posRight.rotate(new Ypr(-1.57, 0.0, 0.0));
		posRight.add(raceStart);

		if(Math.random() > 0.5)
		{
			Vector3	tmp = posLeft;
			posLeft = posRight;
			posRight = tmp;
		}

		mStart = nav.addMarker(Marker.RR_START, pStart, 3);
		mFinish = nav.addMarker(Marker.RR_FINISH, pFinish, 3);

		trRaceFinish = new Trigger(map, null, raceFinish, "dayrace_finish_trigger");
		addNotification(trRaceFinish.trigger, EVENT_TRIGGER_ON, EVENT_SAME, null, "event_handlerRaceFinish");

		finishObject = new RenderRef(map, frontend:0x00000070r, "finishObject");
		Vector3 tmp = new Vector3(raceFinish);
		tmp.y += 3;
		finishObject.setMatrix(tmp, null);

		if(raceBot)
		{
			raceBot.car.command("reset");
			raceBot.car.setMatrix(posLeft, startOri);
			raceBot.car.setParent(map);

			raceBot.car.command("stop");
			raceBot.car.command("idle");

			raceBot.brain.command("AI_BeginRace 0.5");
		}

		if(demoBot)
		{
			demoBot.car.command("reset");
			demoBot.car.setMatrix(posRight, startOri);
			demoBot.car.setParent(map);

			demoBot.car.command("stop");
			demoBot.car.command("idle");

			demoBot.brain.command("AI_BeginRace 0.5");
		}
		else
		{
			if (player.car)
			{
				player.car.command("reset");
				player.car.setMatrix(posRight, startOri);

				player.car.command("stop"); 
				player.car.command("idle");
			}
		}

		setEventMask(EVENT_TIME);
		abandoned = 0;
		addTimer(1, 9);
	}

	public void startRace2()
	{
		if(player.car)
		{
			player.car.setCruiseControl(0);
			player.car.command("start");
		}

		if(raceBot)
		{
			if(demoBot)
			{
				raceBot.car.command("start");
				demoBot.car.command("start");

				raceBot.startRace(raceFinish, demoBot);
				demoBot.startRace(raceFinish, raceBot);
			}
			else
			{
				raceBot.car.command("start");
				raceBot.startRace(raceFinish, player);
			}
		}

		if(GameLogic.klampiPatch >= 2)
		{
	        	if(GameLogic.klampiPatch >= 3)
			{
				player.controller.command("viewport 0");
				player.controller.command("osd 0");
			}

			raceBot.brain.command("camera " + cam.id());

			if(GameLogic.klampiPatch >= 3)
			{
				if(osdEnabled) raceBot.brain.command("osd " + osd.id());
				raceBot.brain.command("viewport " + osd.getViewport().id());
			}
		}

		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	public void lookBot(Bot bot, int init)
	{
		if(init) changeCamNone();

		if(bot)
		{
			changeCamTarget(bot.car);
			changeCamChase();
		} 
		else
		{
			changeCamNone();

			Vector3 camPos = new Vector3(dirS);
			camPos.mul(-6.0);
			camPos.y += 1.5;
			camPos.add(pS);

			cam = new GameRef( map, GameRef.RID_CAMERA, camPos.toString() + "," + oriS.y + ",0,0, 0x02, 1.0,0.0,0.1", "no_bot_cam");
			cam.command("look " + map.id() + " " + pS.x + "," + (pS.y+1.0) + "," + pS.z + " 0,0,0");
			cam.command("move " + map.id() + " " + camPos.toString() + " 8");
			cam.command("render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
		}
	}

	//RAXAT: this code needs some additional work
	public int selectNrOpponent()
	{
		if(nrOpponents.size() == 0)
		{
			int clubMin = player.club * GameLogic.CLUBMEMBERS;
			int clubMax = clubMin + GameLogic.CLUBMEMBERS;
			int playerID = GameLogic.findRacer(player);

			if(player.club < 2 && playerID == clubMax-1) clubMax++;
			int extra = -1;

			float psr = (player.winPinkSlips+1)*(player.winPinkSlips+1)+0.25; //pink slips ratio
			int id;

			for(id = playerID+2; id >= playerID+1; id--)
			{
				if (id < clubMax && id >= clubMin && id != playerID && GameLogic.speedymen[id].lastRaceDay < GameLogic.day)
				{
					nrOpponents.addElement(new Integer(id));

					GameLogic.speedymen[id].enabledPinkSlips = 0;
					if(id >= 50 && Math.random() < 1.0/psr) GameLogic.speedymen[id].enabledPinkSlips = 1;
				}
			}

			for (id = playerID-1; id >= playerID-1; id--)
			{
				if(id < clubMax && id >= clubMin && id != playerID && GameLogic.speedymen[id].lastRaceDay < GameLogic.day)
				{
					nrOpponents.addElement(new Integer(id));
					GameLogic.speedymen[id].enabledPinkSlips = 0;
				}
			}
		}

		if(nrOpponents.size() != 0)
		{
			int n = nrOpponents.size();
			if (n>3) n = 3;

			String[] s = new String[n];
			int[] botID = new int[n];
			int[] pict = new int[n];

			int[] cash = new int[4];
			int clubIndex = GameLogic.findRacer(player)/GameLogic.CLUBMEMBERS+1;

			cash[0] = 1000*clubIndex;
			cash[1] = 2000*clubIndex;
			cash[2] = 4000*clubIndex;
			cash[3] = 0;

			nrPrizeList = new int[n];

                        float pstrippenalty = 1.0;
                        if (player.car && player.car.chassis) pstrippenalty -= player.car.chassis.C_drag;
			pstrippenalty *= pstrippenalty;

			for(int i=0; i<n; i++)
			{
				botID[i] = nrOpponents.elementAt(i).intValue();
                                Bot b = (Bot)GameLogic.speedymen[botID[i]];
				pict[i] = b.profile.getPhoto();
				int rank = GameLogic.CLUBMEMBERS-(botID[i]%GameLogic.CLUBMEMBERS);	// hanyadik a rangsorban

				float odds = 1.000; // the factor of chance of your winning - the higher, the more sure you win //
				float pt, bt;

				if (player.car.bestNightQM < 0.100 || b.bestNightQM < 0.100)
					odds *= 3.000;
				else
					odds *= b.bestNightQM / player.car.bestNightQM;
				odds /= 1.0-(1.0-pstrippenalty)*0.5;
				odds -= Math.chaos()*0.4;

				if (odds <= ODDS_PINKS && GameLogic.speedymen[botID[i]].enabledPinkSlips) nrPrizeList[i] = cash[3];
				else
				{
					if (odds <= ODDS_CASH_2) nrPrizeList[i] = cash[2];
					else
					{
						if (odds <= ODDS_CASH_1) nrPrizeList[i] = cash[1];
						else nrPrizeList[i] = cash[0];
					}
				}

				String tmp;
				if((rank % 10) == 1) tmp = "st - ";
				else
				{
					if((rank % 10) == 2) tmp = "nd - ";
					else
					{
						if((rank % 10) == 3) tmp = "rd - ";
						else tmp = "th - ";
					}
				}

				if((rank % 100) >= 11 && (rank % 100) <= 13) tmp = "th - ";

				//RAXAT: old
				//if(nrPrizeList[i] == 0) s[i] = rank + tmp + b.profile.getName() + " - PINK SLIPS";
				//else s[i] = rank + tmp + b.profile.getName() + " - $" + nrPrizeList[i];
				
				//RAXAT: new, with adjustable night race stakes
				s[i] = rank + tmp + b.profile.getName();

				//s[i] = s[i] + " - "+b.getPrestigeString(0)+" - "+b.nightVd.vehicleName; //RAXAT: old
				s[i] = s[i] + " - "+b.getPrestigeString(0)+" - "+Vehicle.loadName(b.profile.vhcPath) /*+ " (" + b.bestNightQM + "/" + player.car.bestNightQM + ")"*/; //RAXAT: v2.3.1, bot vehicle name is now loaded from the vehicle save file, not from VehicleDescriptor
			}

			if(player.getMoney() < player.getMinBet())
			{
				new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "NOT ENOUGH MONEY", "Hey dude, we're not a homeless assistance fund here! \n Get at least $" + player.getMinBet() + " and then we could talk about racing.").display();
			}
			else
			{
				int p = new NightRaceDialog(player.controller, pict, s, nrPrizeList).display();
				p--; //RAXAT: to distinguish pressing 'cancel' button from selecting first racer in opponents list
				if(p < n && p >= 0)
				{
					nrBotID1 = botID[p];
					int bet = new BetDialog(player.controller, GameLogic.speedymen[nrBotID1]).display();
					if(bet)
					{
						nrOpponents.removeAllElements();

						//nrPrize = nrPrizeList[p]; //old
						nrPrize = bet; //new

						GameLogic.speedymen[nrBotID1].lastRaceDay = GameLogic.day;
						
						return 1;
					}
				}
			}
		}
		else new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "NO OPPONENTS", "Sorry, there are no opponents to race against. \n Come back later!").display();

		return 0;
	} //todo: show bet in loading dialog

	public void startNightRace(int playerRace)
	{
		int[] pic = new int[2];
		int backState = nrStat;
		nrStat = NR_INVALID;
		nrPlayerRace = playerRace;

		Vector3 v = new Vector3();
		v.diff(player.car.getPos(), pS);

		float dist = v.length();
		nrShowRaceStart = nrPlayerRace || nrWatching;

		if(nrShowRaceStart)
		{
			Input.cursor.enable(0);
			osd.hideGroup(nrWatchingGroup);
			osd.hideGroup(nightRaceGroup);

			if(!(nrWatching || nrPlayerRace)) enableOsd(0);
		}

		pl1 = null;
		pl2 = null;

		if(nrPlayerRace)
		{
			if(!selectNrOpponent())
			{
				nrWatching = backupnrWatching;
				Input.cursor.enable(1);

				if(nrWatching)
				{
					osd.showGroup(nrWatchingGroup);
					enableOsd(0);
				}
				else osd.showGroup(nightRaceGroup);

				nrStat  = backState;
				nrPlayerRace = 0;
				return;
			}

			pic[0] = ((Bot)GameLogic.speedymen[nrBotID1]).profile.getPhoto();
			pic[1] = player.getPhoto();

			nrName1 = GameLogic.speedymen[nrBotID1].name;
			nrName2 = player.getName(); //RAXAT: will be always displayed as "Vince Polansky" since v2.3.1

                        pl1 = GameLogic.speedymen[nrBotID1];
                        pl2 = player;
		}
		else
		{
			int clubMin = player.club * GameLogic.CLUBMEMBERS;
			int clubMax = clubMin + GameLogic.CLUBMEMBERS - 1;
			int playerID = GameLogic.findRacer( player );

			nrBotID1 = Math.random() * GameLogic.CLUBMEMBERS;
			nrBotID1 = nrBotID1 + clubMin;

			if(nrBotID1 == playerID)
			{
				if(playerID == clubMax) nrBotID1 = playerID-1;
				else nrBotID1 = playerID+1;
			}

			int d = 1;
			if(nrBotID1 >= (clubMin + clubMax)/2) d = -1;

			nrBotID2 = nrBotID1+d;
			if(nrBotID2 == playerID) nrBotID2 += d;

			nrHead1 = GameLogic.speedymen[nrBotID1].character;
			nrHead2 = GameLogic.speedymen[nrBotID2].character;
			nrName1 = GameLogic.speedymen[nrBotID1].name;
			nrName2 = GameLogic.speedymen[nrBotID2].name;

                        pl1 = GameLogic.speedymen[nrBotID1];
                        pl2 = GameLogic.speedymen[nrBotID2];
		}

		RaceDialog nrDialog;

		if(nrShowRaceStart)
		{
			applauseSfxID = applause1.play(pS, 150.0, 1.0, 1.0, SfxRef.SFX_LOOP | SfxRef.SFX_NOAUTOSTOP);  //start looped sfx
			applauseSfxOn = 1;
			
			int pict = Math.random()*5.0;
			nrDialog = new RaceDialog(player.controller, new ResourceRef(RID_NRSTARTBG + pict), pic[1], pic[0], nrName2, nrName1, 0, 0, nrPrize, 0);
			Frontend.loadingScreen.show(nrDialog);
		}

		cleanupNightRace();

		if(!nrPlayerPaused && !nrPlayerRace && dist <= 10.0)
		{
			nrPlayerPaused = 1;

			Vector3 newPos = new Vector3(dirS);
			newPos.mul(-10.0);
			newPos.add(pS);

			Vector3[] alignedPos = map.alignToRoad(newPos);
			newPos.y = alignedPos[0].y;

			oriS.y += 0.5;
			player.car.setMatrix(newPos, oriS);

			oriS.y -= 0.5;
			player.car.command("stop");
		}

		Vector3[] alignedPos = map.alignToRoad(pS);
		Vector3 botPos, tmp;

		if(!nrStarterLady)
		{
			botPos = new Vector3(alignedPos[0]);

			tmp = new Vector3(alignedPos[1]);
			tmp.mul(5.0);

			botPos.add(tmp);

			nrStarterLady=new RenderRef(map, humans:0x0070r, "gizi");
			nrStarterLady.setMatrix(botPos, new Ypr(tmp));
			nrStarterLadyAnim = new Animation(nrStarterLady, new ResourceRef(humans:0x007cr));
		}

		botPos = new Vector3(alignedPos[0]);

		tmp = new Vector3(alignedPos[1]);
		tmp.mul(2.0);
		tmp.rotate(new Ypr(1.57, 0.0, 0.0));

		botPos.add(tmp);

		float yNudge;
		int idx, colorSeed;

		if(nrPlayerRace)
		{
			nrBot1 = GameLogic.speedymen[nrBotID1];
			nrBot1.setDriverObject(GameLogic.HUMAN_OPPONENT);

			int botIndex = GameLogic.findRacer(nrBot1);

			//Vehicle nightVhc = new Vehicle(this, nrBot1.nightVd.id,  nrBot1.nightVd.colorIndex, nrBot1.nightVd.optical, nrBot1.nightVd.power, nrBot1.nightVd.wear, nrBot1.nightVd.tear);
			Vehicle nightVhc = nrBot1.getCar(map);
			nightVhc.races_won = nrBot1.nightWins;
                        nightVhc.races_lost = nrBot1.nightLoses;

			nrBot1.createCar(map, nightVhc);

			nrBot1.car.setMatrix(botPos, oriS);
			nrBot1.car.setParent(map);

			nrBot1.car.command("reload");
			nrBot1.car.command("stop");
			nrBot1.car.command("idle");

			mRaceBot = nav.addMarker(nrBot1);
		}
		else
		{
			yNudge = (Math.random()*2-1)*yawNudge;
			idx = Math.random()*fakers.length;
			colorSeed = Math.random()*12345;

			oriS.y += yNudge;

			GameRef car1 = new GameRef(map, fakers[idx], botPos.toString() + ","+oriS.y+",0,0," + colorSeed, "nightracer1");
			car1.setMatrix(botPos, oriS);
			oriS.y-=yNudge;

			nrBot1 = GameLogic.speedymen[nrBotID1];
			nrBot1.setDriverObject(GameLogic.HUMAN_OPPONENT);
			nrBot1.createCar(map, new Vehicle(car1));

			nrBot1.car.command("stop");
			nrBot1.car.command("idle");
		}

		botPos.sub(tmp.mul(2.0));

		if(nrPlayerRace)
		{
			player.car.setMatrix(botPos, oriS);
			player.car.command("stop");
			player.car.command("idle");

			nrPlayerRace = 2;
		}
		else
		{
			yNudge = (Math.random()*2-1)*yawNudge;

			idx = Math.random()*fakers.length;
			colorSeed = Math.random()*12345;

			oriS.y+=yNudge;

			GameRef car2 = new GameRef(map, fakers[idx], botPos.toString() + ","+oriS.y+",0,0," + colorSeed, "nightracer1");
			car2.setMatrix(botPos, oriS);
			oriS.y -= yNudge;

			nrBot2 = GameLogic.speedymen[nrBotID2];
			nrBot2.setDriverObject(GameLogic.HUMAN_OPPONENT);
			nrBot2.createCar(map, new Vehicle(car2));

			nrBot2.car.command("stop");
			nrBot2.car.command("idle");
		}

		nrFinishTrigger = addTrigger(pF, null, Marker.RR_FINISH, "event_handlerNrFinish", 8, "night race finish trigger");
		nrFinishSoundTrigger = addTrigger(pF, null, Marker.RR_FINISH, "event_handlerNrFinishSound", 30, "night race finish sound trigger");

		if(nrShowRaceStart)
		{
			backupCamera();

			if(nrPlayerRace)
			{
				changeCamTarget(player.car);
				changeCamTarget2(nrBot1.car);
				changeCamFollow();
			}
			else
			if(nrWatching)
			{
				lookBot(null, 1);
				cameraTarget = nrBot1.car;
				cameraTarget2 = nrBot2.car;
				nrLookAt = 0;
			}
			else
			{
				changeCamTarget2(nrBot1.car);
				changeCamChase();
			}

			Frontend.loadingScreen.display(nrDialog, 10.0);

			nrStat = NR_3;
			if(nrPlayerRace)
			{
				if(cam)
				{
					changeCamTarget2(nrBot1.car);
					cam.command("angle 0 4.0 0.7853");
				}

				nrDelay = 7;
			} 
			else
			{
				Input.cursor.enable(1);

				if(nrWatching) osd.showGroup(nrWatchingGroup);
				nrDelay = 1;
			}

		}
		else
		{
			nrStat = NR_START;
			nrDelay = 0;
		}
	}

	public void startNightRace2()
	{
		nrFinished1 = 0;
		nrFinished2 = 0;

		if(nrShowRaceStart)
		{
			clearMsgBoxes();
			showMsgBox("GO!", Track.MBOX_GREEN, Track.MBOX_SHORT ); //RAXAT: v2.3.1, new messages
			speechGO.play();
		}

		nrStartTime = System.simTime();

		if(nrPlayerRace)
		{
			nrBot1.car.command("start");
			nrBot1.startRace(pF, player);
			nrBot1.brain.command("AI_NightRace");

			player.car.setCruiseControl(0);
			player.car.command("start");

			cam.command("angle 0 0");
		} 
		else
		{
			nrBot1.car.command("start");
			nrBot2.car.command("start");

			nrBot1.startRace(pF, nrBot2);
			nrBot2.startRace(pF, nrBot1);
		}

		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	public void finishNightRace()
	{
		nrStat = NR_INVALID;

		RaceDialog nrDialog;
		int background;
		nrQuit = 0;

		if(nrPlayerRace == 2)
		{
			if(nrTime2 < nrTime1) background = RID_NRFIN_WIN;
			else background = RID_NRFIN_LOSE;
		} 
		else background = RID_NRFIN_AI;

		if(nrShowRaceFinish)
		{
			if(nrPlayerRace == 2 || nrWatching)
			{
				cam.command("angle 0 0");
				cam.command("dist 8.0 16.0");
			}

			if(nrWatching)
			{
				nrBot1.brain.command("camera 0");
				nrBot2.brain.command("camera 0");
			}
			else enableOsd(0);

			int[]	 srcId = new int[2];
			String[] srcName = new String[2];

			if(nrPlayerRace == 2)
			{
				srcId[0]   = player.getPhoto();
				srcName[0] = player.getName();
			}
			else
			{
				srcId[0]   = nrBot2.profile.getPhoto();
				srcName[0] = nrBot2.profile.getName();
			}
			
			srcId[1]   = nrBot1.profile.getPhoto();
			srcName[1] = nrBot1.profile.getName();

			int[]	 idx = new int[2];
			for(int i=0; i<idx.length; i++)
			{
				int bool = nrTime2 < nrTime1 ? 0 : 1;
				
				if(bool) idx[i] = 1-i;
				else idx[i] = i;
			}

			nrDialog = new RaceDialog(player.controller, new ResourceRef(background), srcId[idx[0]], srcId[idx[1]], srcName[idx[0]], srcName[idx[1]], nrTime2, nrTime1, nrPrize, 1);
			Frontend.loadingScreen.show(nrDialog);
		} 

		if(nrPlayerRace == 2)
		{
			if(nrBot1.bestNightQM == 0.0 || nrTime1 < nrBot1.bestNightQM) nrBot1.bestNightQM = nrTime1;
			if(nrTime2 < nrTime1)
			{
				//player wins
                	        if (player.car.bestNightQM == 0.0 || nrTime2 < player.car.bestNightQM) player.car.bestNightQM = nrTime2;
				speechYOUWIN.play();

				player.car.races_won++;
				player.races_won++;

                                nrBot1.nightLoses++;
                                nrBot1.car.races_lost++;
				nrBot1.brain.command("camera 0");

				if(nrPrize > 0) player.addMoney(nrPrize);
				else
				{
					//pinks won, gaining opponent's vehicle
					player.winPinkSlips++;
					nrBot1.leaveCar(0);

					player.carlot.addCar(nrBot1.car);
					nrBot1.car = null;
					player.carlot.saveCar(player.carlot.curcar);
					player.carlot.flushCars();
				}
			}
			else
			{
				//player lose
				player.car.races_lost++;
				player.races_lost++;
                                nrBot1.nightWins++;
                                nrBot1.car.races_won++;

				if(nrPrize > 0) player.takeMoney(nrPrize);
				else nrQuit = 1; //player losing vehicle in pinks, back to garage!
			}

			GameLogic.challenge(GameLogic.findRacer(player), GameLogic.findRacer(nrBot1), 0, (nrTime2 < nrTime1), 1 );
			refreshStatus();

		}
		else GameLogic.challenge(GameLogic.findRacer(nrBot2), GameLogic.findRacer(nrBot1), 0, (nrTime2 < nrTime1), 1);

		Vector3 playerPos = player.car.getPos();
		for(int i = 0; i < parkingCars.length; i++)
		{
			if(parkingCars[i]) parkingCars[i].reset(playerPos);
		}

		cleanupNightRace(); //kills bots, their cars and the trigger areas

		if(nrShowRaceFinish)
		{
			if(nrPlayerRace==2)
			{
				player.car.command("start");
				nrPlayerRace = 0;
			}

			if(nrWatching)
			{
				player.controller.command("camera " + cam.id());
				restoreCamera();
			} 
			else enableOsd(1);

			Frontend.loadingScreen.display(nrDialog, 10.0);
		}

		if(nrQuit)
		{
			Thread.sleep(1000);
			killCar = 1;
			GameLogic.changeActiveSection(GameLogic.garage);
		}
		else
		{
			nrDelay = 15;
			nrStat = NR_IDLE;
		}
	}

	public void cleanupNightRace()
	{
		if(nrBot1) nrBot1.deleteCar();

		if(mRaceBot)
		{
			nav.remMarker(mRaceBot);
			mRaceBot=null;
		}

		if(nrBot2) nrBot2.deleteCar();

		if(nrFinishTrigger) removeTrigger(nrFinishTrigger);
		if(nrFinishSoundTrigger) removeTrigger(nrFinishSoundTrigger);

		nrBot1 = nrBot2 = nrFinishTrigger = nrFinishSoundTrigger = null;
	}


	public void nightRaceStep()
	{
		if(nrDelay > 0)
		{
			nrDelay--;
			return;
		}

		if(nrStat == NR_IDLE)
		{
			if(player.car.getPos().sub(pS).length() < 500.0f) startNightRace(0);
			else nrDelay = 10;
		}
		else
		if(nrStat == NR_3)
		{
			if(nrPlayerRace) cam.command("angle 0 3.0");
			showMsgBox("3", Track.MBOX_RED, Track.MBOX_SHORT); //RAXAT: v2.3.1, new messages
			speech3.play();

			if(nrBot1) nrBot1.brain.command("AI_BeginRace 0.5");
			if(nrBot2) nrBot2.brain.command("AI_BeginRace 0.5");

			nrStat = NR_2;
			nrDelay = 0;
		}
		else
		if(nrStat == NR_2)
		{
			showMsgBox("2", Track.MBOX_RED, Track.MBOX_SHORT); //RAXAT: v2.3.1, new messages
			speech2.play();

			nrStarterLadyAnim.setSpeed(2.0);
			nrStarterLadyAnim.play();
			
			nrStat = NR_1;
			nrDelay = 0;
		}
		else
		if( nrStat == NR_1 )
		{
			showMsgBox("1", Track.MBOX_RED, Track.MBOX_SHORT); //RAXAT: v2.3.1, new messages
			speech1.play();

			if(nrBot1) nrBot1.brain.command("AI_BeginRace 1.0");
			if(nrBot2) nrBot2.brain.command("AI_BeginRace 1.0");

			nrStat = NR_START;
			nrDelay = 0;
		}
		else
		if(nrStat == NR_START)
		{
			startNightRace2();

			nrStat = NR_RACE;
			nrDelay = 1;
		}
		else
		if(nrStat == NR_RACE)
		{
			if(nrStarterLady)
			{
				nrStarterLady.destroy();
				nrStarterLady = null;
				nrStarterLadyAnim = null;
			}

			if(nrPlayerPaused && !nrWatching)
			{
				player.car.command("start");
				nrPlayerPaused = 0;
			}

			if(nrWatching)
			{
				if(nrBot1 && nrBot2 && !nrFinished1 && !nrFinished2)
				{
					Vector3 v1 = new Vector3();
					Vector3 v2 = new Vector3();

					v1.diff(nrBot1.car.getPos(), pF);
					v2.diff(nrBot2.car.getPos(), pF);

					float d1 = v1.length();
					float d2 = v2.length();

					if(d1 > d2)
					{
						if(nrLookAt != 1)
						{
							if(cameraTarget) changeCamTarget2(nrBot2.car);

							lookBot(nrBot1, 0);
							nrLookAt = 1;
						}
					}
					else
					{
						if(nrLookAt != 2)
						{
							if(cameraTarget) changeCamTarget2(nrBot1.car);

							lookBot(nrBot2, 0);
							nrLookAt = 2;
						}
					}
				}
			}
		}
		else
		if(nrStat == NR_FINISH) finishNightRace();
	}

	public void event_handlerNrFinishSound( GameRef obj_ref, int event, String param )
	{
		int id = param.token(0).intValue();

		if(event == EVENT_TRIGGER_ON)
		{
			if(nrStat == NR_RACE)
			{
				int startSound;
				int id = param.token(0).intValue();

				if (id == nrBot1.car.id()) startSound = 1;

				if (nrPlayerRace == 2)
				{
					if(id == player.car.id()) startSound = 1;
				}
				else
				{
					if(id == nrBot2.car.id()) startSound = 1;
				}

				if(startSound) applause2.play(pF, 150.0, 1.0, 1.0, 0);
			}
		}
	}

	public void event_handlerNrFinish(GameRef obj_ref, int event, String param)
	{
		int id = param.token(0).intValue();

		if(event == EVENT_TRIGGER_ON)
		{
			if(nrStat == NR_RACE)
			{
				int	id = param.token(0).intValue();

				if(!nrFinished1)
				{
					if(id == nrBot1.car.id())
					{
						nrTime1 = System.simTime() - nrStartTime;
						nrFinished1 = 1;
						
						addTimer(10, 7);
						nrBot1.stop();
						
						if(nrPlayerRace) showMsgBox(nrBot1.profile.name_f + " " + nrBot1.profile.name_s + " HAS FINISHED RACE!", Track.MBOX_YELLOW, Track.MBOX_LONG); //this message appears only if player takes part in a race
					}
				}

				if(!nrFinished2)
				{
					if(nrPlayerRace == 2)
					{
						if(id == player.car.id())
						{
							nrTime2 = System.simTime() - nrStartTime;
							nrFinished2 = 1;
							player.car.command("brake");

							if(!nrFinished1)
							{
								//RAXAT: finish race safety patch
								nrBot1.stop();
								addTimer(1, 7);
							}
						}
					}
					else
					{
						if(id == nrBot2.car.id())
						{
							nrTime2 = System.simTime() - nrStartTime;
							nrFinished2 = 1;
							nrBot2.stop();

							if(!nrFinished1) addTimer(10, 7);
						}
					}
				}

				if(nrFinished1 && nrFinished2)
				{
					nrStat = NR_INVALID;

					Vector3 v = new Vector3();
					v.diff(player.car.getPos(), pF);
					float dist = v.length();

					nrShowRaceFinish = (nrPlayerRace == 2 || nrWatching || ((dist <= 25.0) && player.car.getSpeedSquare() < TINY_SPEED_SQ));

					if(nrShowRaceFinish)
					{
						if(nrPlayerRace == 2 || nrWatching)
						{
							changeCamFollow();

							cam.command("dist 2.5 10.0");
							cam.command("smooth 0.5 0.5");
							cam.command("force 1.6 0.5 -0.7");
							cam.command("torque 0.05" );
							cam.command("angle 0 4.0 0.7853");
							cam.command("dist 5.5 6.5");

							nrDelay = 8;
						}
					}
					nrStat = NR_FINISH;
				}
			}
		}
	}

	public void event_handlerNrStart(GameRef obj_ref, int event, String param)
	{
		int id = param.token(0).intValue();

		if(event == EVENT_TRIGGER_ON)
		{
			if(player.car && (id == player.car.id()))
			{
				if(!nrPlayerRace)
				{
					nrNear = 1;

					osd.showGroup(nightRaceGroup);
					Input.cursor.enable(1);
				}
			}
		}
		else
		{
			if(event == EVENT_TRIGGER_OFF)
			{
				if (player.car && (id == player.car.id()))
				{
					if(!nrPlayerRace)
					{
						if(nrWatching)
						{
							if(nrBot1 && nrBot2)
							{
								nrBot1.brain.command("camera 0");
								nrBot2.brain.command("camera 0");
							}

							restoreCamera();
			
							osd.hideGroup(nrWatchingGroup);
							nrWatching = 0;
							enableOsd(1);
						}
						else osd.hideGroup(nightRaceGroup);

						Input.cursor.enable(0);

						nrShowRaceStart = 0;
						nrShowRaceFinish = 0;
						nrNear = 0;
					}
				}

				int applauseOff;
				if(nrStat == NR_RACE && applauseSfxOn)
				{
					if(nrBot1 && id == nrBot1.car.id()) applauseOff = 1;
				}

				if(nrPlayerRace == 2)
				{
					if(player.car && id == player.car.id()) applauseOff = 1;
				}
				else
				{
					if(nrBot2 && id == nrBot2.car.id()) applauseOff = 1;
				}

				if(applauseOff)
				{
					applause1.stop(applauseSfxID);
					applauseSfxOn = 0;
				}
			}
		}
	}

	public void event_handlerRaceFinish(GameRef obj_ref, int event, String param)
	{
		if(raceState == 1)
		{
			int id = param.token(0).intValue();
			int rank;

			if(player.car && (id == player.car.id())) rank = 1;
			else
			{
				if(id == raceBot.car.id()) rank = 2;
				else
				{
					if (demoBot && (id == demoBot.car.id())) rank = 1;
				}
			}

			if(rank)
			{
				raceState = 2;

				System.timeWarp(0.1);
				addTimer(0.1*10.0, 14);

				if(player && (id == player.car.id()))
				{
					player.car.command("brake");
					changeCamTarget(player.car);
					changeCamTarget2(raceBot.car);
				}
				else
				{
					if(demoBot && (id == demoBot.car.id()))
					{
						demoBot.car.command("brake");
						demoBot.stop();
						changeCamTarget(demoBot.car);
						changeCamTarget2(raceBot.car);
					}
					else
					{
						if(raceBot && (id == raceBot.car.id()))
						{
							raceBot.car.command("brake");
							raceBot.stop();
							changeCamTarget(raceBot.car);

							if(demoBot && demoBot.car) changeCamTarget2(demoBot.car);
							else
							{
								if(player.car) changeCamTarget2(player.car);
							}
						}
					}
				}

				changeCamNone();

				lastCamPos = obj_ref.getPos();
				lastCamPos.y += 2.0f;

				changeCamChase();
				if (cam) cam.command("simulate 1");

				if(GameLogic.gameMode == GameLogic.GM_DEMO) return;
				else
				{
					player.car.command("brake");

					int challenger_won = !(challenger == player ^ rank == 1);
					if(rank==1)
					{
						speechDAYYOUWIN.play();
						raceDialog = new RaceDialog(player.controller, new ResourceRef(RID_DAY_WIN), player.getPhoto(), raceBot.profile.getPhoto(), player.getName(), raceBot.profile.getName(), 0.0, 0.0, prize, 5);
					}
					else
					{
						speechDAYYOULOOSE.play();
						raceDialog = new RaceDialog(player.controller, new ResourceRef(RID_DAY_LOOSE), raceBot.profile.getPhoto(), player.getPhoto(), raceBot.profile.getName(), player.getName(), 0.0, 0.0, prize, 5);
					}

					if(GameLogic.gameMode == GameLogic.GM_CARREER)
					{
						if(rank == 1)
						{
							player.addMoney(prize);
							player.car.races_won++;
							player.races_won++;
						}
						else
						{
							player.takeMoney(prize);
							player.car.races_lost++;
							player.races_lost++;
						}

						GameLogic.challenge(GameLogic.findRacer(challenger), GameLogic.findRacer(challenged), abandoned, challenger_won, 0);
						refreshStatus();
					}
				}
			}
		}
	}

	public void startQuickRace()
	{
		Frontend.loadingScreen.show();
		createQuickRaceBot();

		raceState = 1;

		if(!GameLogic.racesetup) GameLogic.racesetup = new RaceSetup();
		GameLogic.changeActiveSection(GameLogic.racesetup);
	}

	public void cleanupRace()
	{
		abandoned = 1;

		if(!raceState) return;
		raceState=0;

		if(raceBot)
		{
			if(raceBot.dummycar)
			{
				if (raceBot.dummycar.id() != raceBot.car.id())
				{
					Vector3	pos = raceBot.car.getPos();
					Ypr ori = raceBot.car.getOri();

					raceBot.deleteCar();

					raceBot.dummycar.command("reset");
					raceBot.dummycar.setMatrix(pos, ori);
					raceBot.dummycar.setParent(map);

					raceBot.car = new Vehicle(raceBot.dummycar);
					raceBot.brain = new GameRef(map, sl:0x0000006Er, "", "BOTBRAIN");

				        RenderRef render = new RenderRef(map, raceBot.driverID, "botfigura-afterrace");

					raceBot.brain.command("renderinstance " + render.id());
					raceBot.brain.command("controllable " + raceBot.dummycar.id());
					raceBot.traffic_id = 0;
				}
			}
		}
		else destroyRaceBot();

		if(mRaceBot)
		{
			nav.remMarker(mRaceBot);
			mRaceBot=null;
		}

		raceBot.reJoinTraffic();
		raceBot.setTrafficBehaviour(GameRef.TC_PASSIVE);

		if (trRaceFinish)
		{
			trRaceFinish.finalize();
			trRaceFinish = null;
		}

		nav.remMarker(mStart);
		nav.remMarker(mFinish);

		if(nav.route)
		{
			nav.route.destroy();
			nav.route = null;
		}

		finishObject.destroy();
	}

	public void abandonRace()
	{
		GameLogic.challenge(GameLogic.findRacer(challenger), GameLogic.findRacer(challenged), 1, 0, 0);
		refreshStatus();
		raceState=0;
		raceBot.setTrafficBehaviour(GameRef.TC_PASSIVE);
		abandoned = 1;

		if(nav.route)
		{
			nav.route.destroy();
			nav.route = null;
		}
	}

	public void backupCamera()
	{
		if(!backupCam)
		{
			backupCam = 1;

			nrcameraMode_before = cameraMode;
			nrcameraTarget = cameraTarget;
		}
	}

	public void restoreCamera()
	{
		if(backupCam)
		{
			backupCam = 0;

			cameraTarget = nrcameraTarget;
			cameraMode = nrcameraMode_before;

			switch(cameraMode)
			{
				case(CAMMODE_INTERNAL):	changeCamInternal(); break;
				case(CAMMODE_TV):	changeCamTV(); break;

				default:		changeCamFollow(); break;
			}
		}
	}

	public int[] calculateFineSum(int clear)
	{
		int[] fine = new int[5];

		if(overSpeed) fine[1] = overSpeed*3.6*5 + 30; //$5 USD for every KPH exceed
		fine[2] = crashes*100;

		if(fleedAway)
		{
			float tm;

			if(pullOverTime) tm = pullOverTime;
			else tm = System.simTime();

			fine[3] = 200 + (tm-firstAlertTime)*10;
		}

		fine[4] = 0.0;

		if(System.simTime()-firstAlertTime > 1.0*60.0)
		{
			if(GameLogic.player && GameLogic.player.car) fine[4] = GameLogic.player.car.calcPoliceFine((System.simTime() - firstAlertTime - 1.0*60.0)/30.0);
		}

		fine[0] = fine[1]+fine[2]+fine[3]+fine[4];

		if(clear)
		{
			overSpeed = 0;
			crashes = 0;
			fleedAway = 0;
		}

		return fine;
	}
    
	int timeLock;

	public void handleEvent(GameRef obj_ref, int event, int param)
	{
        	super.handleEvent(obj_ref, event, param);

		if(event == EVENT_TIME)
		{
			switch(param)
			{
				case 2:
				if(timeLock) {}//NOP
				else
				{
					timeLock = 1;
					time++;

					if(GameLogic.actualState == this)
					{
						if(nightTime)
						{
							nightRaceStep();

							if(nrQuit)
							{
								timeLock = 0;
								break;
							}
						}

						float playerSpeedSq = player.car.getSpeedSquare();
						int actScout;

						if(policeState || playerSpeedSq > SPEED_LIMIT_SQ) alertPolice();

						for(int i=alertedScouts.size()-1; i>=0; i--)
						{
							PoliceScout pc = alertedScouts.elementAt(i);
							if(pc.returningTraffic)
							{
								if(pc.bot.traffic_id)
								{
									alertedScouts.removeElementAt(i);
									policeCars.addElement(pc.tracker);
								}
								else
								{
									Vector3 v = pc.bot.car.getPos();
									v.sub(player.car.getPos());

									float distance = v.length();
									if(distance > 600) sleepPoliceScoutQuick(pc);
								}
							}
							else
							{
								pc.distance = map.getRouteLength(pc.bot.car.getPos(), player.car.getPos());
								actScout++;

								if(pc.distance >= 0)
								{
									if(pc.distance < 300.0)
									{
										float maxSpeed = player.car.hasCrime()*1.1;
										if(maxSpeed >= 0)
										{
											maxSpeed = Math.sqrt(playerSpeedSq) - maxSpeed;
											if(maxSpeed > overSpeed) overSpeed = maxSpeed;
										}
									}

									if(fleedAway)
									{
										if(!pullOverTime)
										{
											if(playerSpeedSq < TINY_SPEED_SQ) pullOverTime = System.simTime();
										}
										else
										{
											if(playerSpeedSq >= TINY_SPEED_SQ) pullOverTime = 0.0;
										}
									}
									else
									if(playerSpeedSq >= TINY_SPEED_SQ)
									{
										if(System.simTime() - firstAlertTime > 10.0)
										{
											fleedAway = 1;
											pullOverTime = 0.0;
										}
									}

									if(pc.distance < 10.0)
									{
										if(playerSpeedSq < TINY_SPEED_SQ)
										{
											if(pc.bot.car.getSpeedSquare() < TINY_SPEED_SQ)
											{
												policeState = 0;
												roamFree = 1;
												addTimer(5, 13);

												if(GameLogic.gameMode != GameLogic.GM_DEMO)
												{
													int[] fine = calculateFineSum(1);

													int osdState = osdEnabled;
													enableOsd(0);

													new PoliceDialog(player.controller, PoliceDialog.MODE_CAPTURE, fine).display();
													enableOsd(1);

													player.takeMoney(fine[0]);
													player.decreasePrestige(Racer.PRESTIGE_STEP);
													refreshStatus();
												}

												for(int i=alertedScouts.size()-1; i>=0; i--)
												{
													PoliceScout pc = alertedScouts.elementAt(i);
													sleepPoliceScout(pc);
												}
												break;
											}
										}
									}
									else
									if(pc.distance < 100.0)
									{
										if(GameLogic.gameMode != GameLogic.GM_DEMO)
										{
											if(policeState == 10)
											{
												//RAXAT: v2.3.1, new messages
												String loseTxt = "POLICE: PULL OVER NOW!";
												if(findBusyBox() < 0) showMsgBox(loseTxt, Track.MBOX_RED, Track.MBOX_MID);
												else holdMsgBox(loseTxt);
											}
										}
									}
									else
									if(pc.distance > 400.0 + 100.0*player.club)
									{
										Vector3 v = pc.bot.car.getPos();
										v.sub(player.car.getPos());

										float distance = v.length();
										if(distance > 600) sleepPoliceScoutQuick(pc);
										else sleepPoliceScout(pc);
									}
								}
							}
						}

						if(policeState && !actScout)
						{
							if(!(--policeState))
							{
								clearMsgBoxes();
								showMsgBox("COPS LOST", Track.MBOX_GREEN, Track.MBOX_SHORT); //RAXAT: v2.3.1, new messages

								Gamemode.updatePoliceStats(1); //RAXAT: v2.3.1, update police chase stats (escaped++)
								player.increasePrestige(2*Racer.PRESTIGE_STEP);
								calculateFineSum(1);
								refreshStatus();
							}
						}

						//RAXAT: v2.3.1, player can invoke a police chase by a rude behavior
						if(!policeState)
						{
							for(int i=0; i<policeCars.size(); i++)
							{
								TrafficTracker tt = policeCars.elementAt(i);
								GameRef pc = tt.car;

								if(pc.id())
								{
									tt.trafficId = pc.getInfo(GII_CAR_TRAFFICPTR);
									if(tt.trafficId)
									{
										Vector3 v = pc.getPos();
										v.sub(player.car.getPos());

										if(v.length() < 20.0)
										{
											if(player.car.getHorn())
											{
												int offense = new PoliceDialog(player.controller, PoliceDialog.MODE_TALK, null).display();
												if(offense)
												{
													alertPolice();
													fleedAway = 1;
													overSpeed = 100+Math.random()*200;
												}
											}
										}
									}
								}
							}
						}

						if(!raceState)
						{
							if((GameLogic.gameMode == GameLogic.GM_CARREER) && (GameLogic.carrerInProgress))
							{
								TrafficTracker nearest_tt = null;
								float racerDistance = 100000.0;

								for(int i=opponentCars.size()-1; i>=0; i--)
								{
									TrafficTracker tt = opponentCars.elementAt(i);
									GameRef pc = tt.car;

									if(pc.id())
									{
										Vector3 v = pc.getPos();
										v.sub(player.car.getPos());

										float distance = v.length();
										if(distance < racerDistance)
										{
											racerDistance = distance;
											nearest_tt = tt;
										}
									}
								}

								int keepoppstatus = 0;
								if(nearest_tt)
								{
									if(racerDistance < 40.0)
									{
										if(raceBot)
										{
											if(raceBot != nearest_tt.bot)
											{
												raceBot.releaseHorn();
												raceBot = null;

												oppStatusTxt.changeText(null);
												oppStatusDisplayed = 0;

												aiChallengeState = 0;
												abandoned2 = 0;
											}
										}

										if(!raceBot) raceBot = nearest_tt.bot;

										if(!oppStatusDisplayed)
										{
											int ranking = (GameLogic.CLUBMEMBERS-(GameLogic.findRacer(raceBot)-GameLogic.CLUBMEMBERS*raceBot.club));
											oppStatusTxt.changeText(raceBot.name + "  " + GameLogic.CLUBNAMES[raceBot.club] + "/" + ranking + " > " + raceBot.getPrestigeString());

											if(GameLogic.canChallenge(player, raceBot)) oppStatusDisplayed = 1;
											else oppStatusDisplayed = 2;

											if(!policeState) changeCamTarget2(raceBot.dummycar);

										}

										int hex;
										switch(oppStatusDisplayed)
										{
											case 1:  hex = 0x90FFFFFF; break;
											case 2:  hex = 0x90FF5555; break;
											default: hex = 0x90FFFFFF; break;
										}

										if(hex > 0) oppStatusTxt.setColor(hex);

										if(!policeState && racerDistance < 20.0)
										{
											if(!abandoned2)
											{
												if(aiChallengeState)
												{
													if(Math.random() > 0.4) raceBot.releaseHorn();
													else raceBot.pressHorn();
												}
												else
												{
													if(Math.random() < 0.4)
													{
														if(GameLogic.canChallenge(raceBot, player))
														{
															raceBot.pressHorn();
															aiChallengeState = 1;
														}
														else abandoned2 = 1;

													}
													else abandoned2 = 1;
												}
											}
										}

										if(racerDistance < 10.0)
										{
											if(player.car.getHorn())
											{
												raceBot.releaseHorn();
												player.car.command("sethorn 0");

												int shallWeRace = aiChallengeState;
												if(!shallWeRace) shallWeRace = new DayRaceDialog(player.controller, raceBot, abandoned, policeState).display();
												if(shallWeRace)
												{
													raceState = 1;

													if(aiChallengeState)
													{
														aiChallengeState = 0;

														challenger = raceBot;
														challenged = player;
													}
													else
													{
														challenger = player;
														challenged = raceBot;
													}

													if(!GameLogic.racesetup) GameLogic.racesetup = new RaceSetup();
													GameLogic.changeActiveSection(GameLogic.racesetup);
												}
												else abandoned2 = 1;
											}
										}

										keepoppstatus = 1;
									}
								}

								if(!keepoppstatus)
								{
									if(oppStatusDisplayed)
									{
										if(raceBot)
										{
											raceBot.releaseHorn();
											raceBot = null;
										}

										oppStatusTxt.changeText(null);
										oppStatusDisplayed = 0;

										if(!policeState) changeCamTarget2(null);

										abandoned = 0;
										abandoned2 = 0;
									}
								}
							}
						}
					}

					timeLock = 0;
				}
				break;

				case 7:
				if(nrStat == NR_RACE)
				{
					if(nrFinished1 != nrFinished2)
					{
						if(nrFinished1 || nrFinished2)
						{
							int lamerPlayer;

							if(nrFinished1)
							{
								nrFinished2 = 1;
								nrTime2 = 1000.0;

								if(nrPlayerRace == 2)
								{
									player.car.command("brake");
									lamerPlayer = 1;
								}
								else nrBot2.stop();
							}
							else
							{
								nrFinished1 = 1;
								nrTime1 = 1000.0;

								nrBot1.stop();
							}

							Vector3 v = new Vector3();
							v.diff(player.car.getPos(), pF);
							float dist = v.length();

							nrShowRaceFinish = (nrPlayerRace == 2 || nrWatching || ((dist <= 25.0) && player.car.getSpeedSquare() < TINY_SPEED_SQ));
							if(nrShowRaceFinish)
							{
								if(nrPlayerRace == 2 || nrWatching)
								{
									changeCamFollow();

									cam.command("dist 2.5 10.0");
									cam.command("smooth 0.5 0.5");
									cam.command("force 1.6 0.5 -0.7");
									cam.command("torque 0.05" );
									cam.command("angle 0 4.0 0.7853");
									cam.command("dist 5.5 6.5");

									if(lamerPlayer) nrDelay = 3;
									else nrDelay = 8;
								}
							}
							nrStat = NR_FINISH;
						}
					}
				}
				break;

				case 8:
				if(GameLogic.gameMode == GameLogic.GM_QUICKRACE)
				{
					if(!(new YesNoDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_FREEZE, "QUICKRACE", "Want another Race?").display()))
					{
						//RAXAT: opp name fix
						String txt = raceBot.name;
						if(raceBot.car.chassis) txt = txt + " riding a " + raceBot.car.chassis.vehicleName;

						startQuickRace();
						break;
					}

					GameLogic.changeActiveSection(parentState);
				}
				break;

				case 9:
				showMsgBox("3", Track.MBOX_RED, Track.MBOX_SHORT); //RAXAT: v2.3.1, new messages
				speech3.play();
				addTimer(1, 10);
				break;

				case 10:
				showMsgBox("2", Track.MBOX_RED, Track.MBOX_SHORT); //RAXAT: v2.3.1, new messages
				speech2.play();
				addTimer(1, 11);
				break;

				case 11:
				showMsgBox("1", Track.MBOX_RED, Track.MBOX_SHORT); //RAXAT: v2.3.1, new messages
				speech1.play();

				raceBot.brain.command("AI_BeginRace 1.0");

				addTimer(1, 12);
				break;

				case 12:
				clearMsgBoxes();
				showMsgBox("GO!", Track.MBOX_GREEN, Track.MBOX_SHORT );
				speechGO.play();
				startRace2();
				break;

				case 13:
				roamFree = 0;
				break;

				case 14:
				if(GameLogic.gameMode == GameLogic.GM_DEMO)
				{
					System.timeWarp(1.0);
					if(cam) cam.command("simulate 0");

					cleanupRace();
					GameLogic.changeActiveSection(parentState);
				}
				else
				{
					System.timeWarp(1.0);
					if(cam) cam.command("simulate 0");

					if(raceDialog)
					{
						Frontend.loadingScreen.show(raceDialog);
						Frontend.loadingScreen.userWait();
					}

					cleanupRace();

					if(player.car)
					{
						player.car.command("reset");
						player.car.command("start");

						changeCamTarget(player.car);
						changeCamFollow();
					}

					if(GameLogic.gameMode == GameLogic.GM_QUICKRACE) addTimer(3, 8);
					else Sound.changeMusicSet(Sound.MUSIC_SET_DRIVING);
				}
				break;
			}
		}
	}

	public void handleEvent(GameRef obj_ref, int event, String param)
	{
        if(event == EVENT_COLLISION)
		{
			collision = 1;

			float time = System.simTime();
			if(time-lastCollisionTime > 1.0)
			{
				lastCollisionTime = time;

				GameRef obj = new GameRef(param.token(0).intValue());

				int cat = obj.getInfo(GII_CATEGORY);
				if(cat == GIR_CAT_VEHICLE)
				{
					if(GameLogic.gameMode != GameLogic.GM_DEMO) changeCamTarget2(obj);

					alertPolice();
					if(policeState) crashes++;
				}
			}
		}
		else
		if(event == EVENT_COMMAND)
		{
			String cmd = param.token(0);
			if(cmd == "car_add")
			{
				TrafficTracker tt = new TrafficTracker();

				tt.id = param.token(1).intValue();
				tt.trafficId = param.token(2).intValue();
				tt.car = new GameRef(tt.id);

				int typeId = tt.car.getInfo(GII_TYPE);
				if(typeId == GRI_POLICECAR)
				{
					tt.m = nav.addMarker(Marker.RR_POLICE, tt.car);
					policeCars.addElement(tt);
				}
				else
				{
					for(int i=0; i<GameLogic.speedymen.length; i++)
					{
						Racer opp = GameLogic.speedymen[i];
						if(opp instanceof Bot)
						{
							if(((Bot)opp).dummycar && ((Bot)opp).dummycar.id() == tt.id)
							{
								tt.m = nav.addMarker(opp.getMarker(), ((Bot)opp).dummycar);
								tt.bot = opp;
								opponentCars.addElement(tt);

							        RenderRef render = new RenderRef(map, ((Bot)opp).driverID, "botfigura-corpse");
								tt.car.command("corpse 0 " + render.id());

								((Bot)opp).world=map;
								opp.addNotification(((Bot)opp).dummycar, EVENT_COMMAND, EVENT_SAME, null);

								break;
							}
						}
					}
				}
			}
			else
			{
				if(cmd == "car_rem")
				{
					int id = param.token(1).intValue();
					int i;

					for(i=policeCars.size()-1; i>=0; i--)
					{
						if(policeCars.elementAt(i).id == id)
						{
							TrafficTracker tt = policeCars.removeElementAt(i);
							nav.remMarker(tt.m);
							return;
						}
					}

					for(i=opponentCars.size()-1; i>=0; i--)
					{
						if(opponentCars.elementAt(i).id == id)
						{
							TrafficTracker tt = opponentCars.removeElementAt(i);

							nav.remMarker(tt.m);
							tt.car.command("corpse 0 0");
							tt.bot.remNotification(tt.bot.dummycar, EVENT_COMMAND);

							return;
						}
					}
				}
			}
		}
	}

	public void handleMessage(Message m)
	{
		int handled = 0;

		if(m.type == Message.MT_EVENT)
		{
			int cmd = m.cmd;

			switch(cmd)
			{
				case(CMD_PARTICIPATE):
					Input.cursor.enable(0);

					backupnrWatching = nrWatching;
					if(nrWatching)
					{
						osd.hideGroup(nrWatchingGroup);
						nrWatching = 0;
					}
					else osd.hideGroup(nightRaceGroup);

					startNightRace(1);
					handled = 1;
				break;

				case(CMD_WATCH_RACE):
					osd.hideGroup(nightRaceGroup);
					osd.showGroup(nrWatchingGroup);

					enableOsd(0);
					if(nrBot2)
					{
						backupCamera();
						cameraTarget = nrBot2.car;
						cameraTarget2 = nrBot1.car;
						lookBot(nrBot2, 1);
					}
					else startNightRace(0);

					if(!nrPlayerPaused)
					{
						Vector3 newPos = new Vector3(dirS);

						newPos.mul(-10.0);
						newPos.add(pS);

						Vector3[] alignedPos = map.alignToRoad(newPos);
						newPos.y = alignedPos[0].y;

						player.car.setMatrix(newPos, oriS);
						player.car.command("stop");

						nrPlayerPaused = 1;
					}

					nrWatching = 1;
				break;

				case(CMD_STOP_WATCHING):
					osd.hideGroup(nrWatchingGroup);
					osd.showGroup(nightRaceGroup);

					clearMsgBoxes();

					nrWatching = 0;
					nrShowRaceFinish = 0;

					if(nrBot1 && nrBot2)
					{
						nrBot1.brain.command("camera 0");
						nrBot2.brain.command("camera 0");
					}
				
					restoreCamera();
	
					enableOsd(1);

					if(nrPlayerPaused)
					{
						player.car.command("start");
						nrPlayerPaused = 0;
					}
				break;

				case(CMD_DEBUG):
					//new DayRaceDialog(player.controller, new Bot(32), abandoned, policeState).display();
					//Frontend.loadingScreen.show(new RaceDialog(player.controller, new ResourceRef(RID_NRSTARTBG), frontend:0x9901r, frontend:0x9902r, "David Singh", "Kirill Padalko", 0, 0, 3000, 0));
					
					//--------
					/*
					int[] profileID = new int[3];
					profileID[0] = frontend:0x9901r;
					profileID[1] = frontend:0x9902r;
					profileID[2] = frontend:0x9900r;

					//debug!
					String[] texts = new String[3];
					texts[0] = "17th David Singh - $5000 - 104/0 - PFAA Whisper Q1000XL";
					texts[1] = "18th - Kirill Padalko - PINK SLIPS - 94/0 - Emer Nonus Street GT";
					texts[2] = "19th - Mikhail Kirillov - $3000 - 90/220 - Baiern Yotta 2.5L";

					int[] prizeList = new int[3];
					prizeList[0] = 3000;
					prizeList[1] = 2000;
					prizeList[2] = 1500;

					new NightRaceDialog(player.controller, profileID, texts, prizeList).display(); //status: 100% done
					*/
					break;
			}
		}

		if(!handled) super.handleMessage(m);
	}
}

public class RacerTalkDialog extends Dialog
{
	final static int CMD_RACE = 0;
	final static int CMD_EXIT = 1;
	final static int CMD_EXITNRACE = 2;

	static	String[] Text_honkin;
	static	String[] Text_letsRace;
	static	String[] Text_goAway;
	static	String[] Text_noWay;
	static	String[] Text_seeYou;
	static	String[] Text_right;

	Player	player;
	Racer	bot;

	int     mainGroup, raceGroup1, raceGroup2, raceGroup3, raceGroup4;
	int     canChallenge, justRaced, policeStateCopy;

	public void init()
	{
		if(Text_honkin == null)
		{
			Text_honkin = new String[2];
			Text_honkin[0] = "\"You honkin' at me punk?\"";
			Text_honkin[1] = "\"What do ya want?\"";

			Text_letsRace = new String[1];
			Text_letsRace[0] = "OK!";

			Text_goAway = new String[4];
			Text_goAway[0] = "\"Go away, kiddy!\"";
			Text_goAway[1] = "\"Don't make me laugh!\"";
			Text_goAway[2] = "\"Not now.\"";
			Text_goAway[3] = "\"Forget me.\"";

			Text_noWay = new String[2];
			Text_noWay[0] = "\"No way, my ride needs some tuning!\"";
			Text_noWay[1] = "\"Maybe next time, Pal.\"";

			Text_seeYou = new String[2];
			Text_seeYou[0] = "\"See ya next time!\"";
			Text_seeYou[1] = "\"Bye for now!\"";

			Text_right = new String[4];
			Text_right[0] = "\"Right.\"";
			Text_right[1] = "\"Fine.\"";
			Text_right[2] = "\"Cool.\"";
			Text_right[3] = "\"OK\"";
		}
	}
}

//RAXAT: v2.3.1, new dialog for day races
public class DayRaceDialog extends Dialog
{
	final int CMD_OPTION = 300;

	Player	player;
	Bot	bot;

	Menu	m;
	int     canChallenge, justRaced, policeStateCopy;

	Rectangle[] profileRect = new Rectangle[2];
	int[] profileID = new int[2];

	int mainGroup;

	ResourceRef charset = Frontend.mediumFont;
	float spacing;

	String[] str_honkin, str_goAway, str_noWay, str_seeYou, str_right, str_goRace, str_looseCops;

	public void init()
	{
		if(str_honkin == null)
		{
			str_goAway = new String[4];
			str_goAway[0] = "Go away, kiddy!";
			str_goAway[1] = "Don't make me laugh!";
			str_goAway[2] = "Not now.";
			str_goAway[3] = "Forget me.";

			str_noWay = new String[2];
			str_noWay[0] = "No way, my ride needs some tuning!";
			str_noWay[1] = "Maybe next time, Pal.";

			str_seeYou = new String[2];
			str_seeYou[0] = "See ya next time!";
			str_seeYou[1] = "Bye for now!";

			str_right = new String[4];
			str_right[0] = "Right.";
			str_right[1] = "Fine.";
			str_right[2] = "Cool.";
			str_right[3] = "Got it.";

			str_goRace = new String[2];
			str_goRace[0] = "So you wanna play some? Come on!";
			str_goRace[1] = "Catch my wheels, lagger!";

			str_looseCops = new String[2];
			str_looseCops[0] = "Loose your tail first buddy!";
			str_looseCops[1] = "Man, this is illegal! I'm outta there!";
		}
	}

	public DayRaceDialog(Controller ctrl, Bot b, int abandoned, int policeState)
	{
		super(ctrl, Dialog.DF_DEFAULTBG|Dialog.DF_WIDEMINI|Dialog.DF_DARKEN|Dialog.DF_MODAL|Dialog.DF_FREEZE, getTitle(), null);

		this.player = GameLogic.player;
		bot = b;
		
		this.policeStateCopy = policeStateCopy;
		this.justRaced = justRaced;
		canChallenge = GameLogic.canChallenge(player, bot);

		init();

		profileID[0] = player.getPhoto();
		profileID[1] = bot.profile.getPhoto();

		spacing = Text.getLineSpacing(charset, osd.vp);

		float xpos = -0.7;
		float ypos = 0.09;

		for(int i=0; i<profileRect.length; i++)
		{
			profileRect[i] = osd.createRectangle(xpos, ypos, 0.3525, 1, -1, new ResourceRef(profileID[i]));
			xpos *= (-1);
		}

		osd.endGroup();

		buildGroup(0);

		osd.globalHandler = this;
	}

	public String getTitle()
	{
		String[] titles = new String[2];
		titles[0] = "You honkin' at me punk?";
		titles[1] = "What do ya want?";

		return titles[(int)(Math.random()*titles.length)];
	}

	public void buildGroup(int id)
	{
		Style	menuStyle = new Style(0.45, 0.12, charset, Text.ALIGN_CENTER, Osd.RRT_TEST);

		float ypos = -spacing;
		if(id>0) ypos = -spacing/2;

		m = osd.createMenu(menuStyle, 0.0, ypos, 0);

		int cmd = CMD_OPTION+1;
		String[] str, titleStr;

		switch(id)
		{
			case 0:
				m.addItem("Wanna race with you!", CMD_OPTION);
				m.addItem("Nah, never mind.", CMD_OPTION+1);
				mainGroup = osd.endGroup();
				break;

			case 1: //ready to race
				str = str_right;
				titleStr = str_goRace;
				cmd = CMD_OPTION+2;
				break;

			case 2: //player's car is too slow or not prestigeous enough
				titleStr = str_goAway;
				str = str_right;
				break;

			case 3: //already raced
				titleStr = str_noWay;
				str = str_seeYou;
				break;

			case 4: //got some cops in pursuit
				titleStr = str_looseCops;
				str = str_seeYou;
				break;
		}

		if(id>0)
		{
			m.addItem(str[(int)(Math.random()*str.length)], cmd);
			setTitle(titleStr[(int)(Math.random()*titleStr.length)]);
		}

		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, CMD_OPTION+1, this); //exit cmd
	}

	public void osdCommand(int cmd)
	{
		int gid;

		switch(cmd)
		{
			case(CMD_OPTION):
				osd.hideGroup(mainGroup);

				if(policeStateCopy) gid = 4;
				else
				{
					if(canChallenge)
					{
						if(justRaced) gid = 3;
						else gid = 1;
					}
					else gid = 2;
				}

				buildGroup(gid);
				break;

			case(CMD_OPTION+1): //exit
				result = 0;
				notify();

				break;

			case(CMD_OPTION+2): //exit and race
				result = 1;
				notify();

				break;
		}
	}
}

public class NightRaceDialog extends Dialog
{
	Player	player;
	Menu	m;
	int		nItems;

	Rectangle[]	profileRect;

	public NightRaceDialog(Controller ctrl, int[] rid, String[] texts, int[] prizeSums)
	{
		super(ctrl, DF_DEFAULTBG|DF_LARGE|DF_DARKEN|DF_MODAL|DF_FREEZE, "The following racers are willing to race with you:", "CANCEL");
		inverseButtons = 1;

		nItems = rid.length;
		profileRect = new Rectangle[nItems];

		Style	menuStyle = new Style(1.8, 0.12, Frontend.smallFont, Text.ALIGN_LEFT, Osd.RRT_TEST);

		float xpos = -0.75;
		float ypos = 0.15-(0.15*nItems);

		for(int i=0; i<nItems; i++)
		{
			profileRect[i] = osd.createRectangle(xpos, ypos, 0.175, 0.385, -1, new ResourceRef(rid[i]));

			m = osd.createMenu(menuStyle, xpos+0.1, ypos-0.035, 0);
			Gadget g = m.addItem(texts[i], i+1);

			if(prizeSums[i] > 0 && prizeSums[i] > GameLogic.player.getMoney()) g.disable();
			ypos += 0.4;
		}
		
		osd.globalHandler = this;
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, nItems, this);
	}
	
	public void osdCommand(int cmd)
	{
		result = cmd;
		notify();
	}
}

//RAXAT: v2.3.1, entirely new dialog for interaction with cops
public class PoliceDialog extends Dialog
{
	final static int POLICE_VOICES = 6;

	final static int MODE_CAPTURE	= 0;
	final static int MODE_TALK	= 1;

	final String[] btnText = new String[2];
	final String[] titleText = new String[2];

	final int CMD_OK	= 0;
	final int CMD_OFFENSE	= 1;

	Text[]	finesTxt;
	SfxRef[]	speechPOLICE = new SfxRef[POLICE_VOICES];

	Player	player;
	Text	testTxt;

	Menu	m;

	Rectangle copRect;

	int lines = 0;
	ResourceRef charset = Frontend.largeFont_strong;
	float spacing;
	float xpos, ypos;
	
	public PoliceDialog(Controller ctrl, int mode, int[] fine)
	{
		super(ctrl, DF_DEFAULTBG|DF_MEDIUM|DF_DARKEN|DF_MODAL|DF_FREEZE, getTitle(mode), getButtonTitle(mode));

		copRect = osd.createRectangle(-0.6, 0.125, 0.525, 1, -1, new ResourceRef(Racer.RID_COP));

		if(mode==MODE_TALK) charset = Frontend.mediumFont;
		spacing = Text.getLineSpacing(charset, osd.vp);
		
		player = GameLogic.player;

		switch(mode)
		{
			case(MODE_TALK):
				xpos = 0.2;
				ypos = 0.0;

				Style	menuStyle = new Style(0.45, 0.12, charset, Text.ALIGN_CENTER, Osd.RRT_TEST);
				m = osd.createMenu(menuStyle, xpos, ypos, 0);

				m.addItem("1. Everything is fine, officer.", CMD_OK);
				m.addItem("2. Yea, I hate fat cops!", CMD_OFFENSE);

				break;

			case(MODE_CAPTURE):
				String[] fStr = new String[5];

				fStr[0] = "TOTAL:";
				fStr[1] = "SPEEDING:";
				fStr[2] = "CRASHES:";
				fStr[3] = "CHASE:";
				fStr[4] = "ILLEGAL PARTS:";

				for(int i=0; i<fine.length; i++)
				{
					if(i==0) lines += 2;
					else
					{
						if(fine[i] > 0) lines++;
					}
				}

				xpos = -0.3;
				ypos = -((lines*spacing)/2)+(Config.video_y*0.0021*spacing)*0.7;

				int curLine = 0;
				for(int j=1; j<fine.length; j++)
				{
					if(fine[j] > 0)
					{
						fStr[j] += " $" + fine[j];
						osd.createText(fStr[j], charset, Text.ALIGN_LEFT, xpos, ypos+curLine*spacing).changeColor(Palette.RGB_GREY);

						curLine++;
					}
				}

				osd.createText(fStr[0] + " $" + fine[0], charset, Text.ALIGN_LEFT, xpos, ypos+(curLine+1)*spacing).changeColor(Palette.RGB_GREY);
				
				Gamemode.updatePoliceStats(0); //RAXAT: v2.3.1, update police chase stats (busted++)
				
				for(int i=0; i<POLICE_VOICES; i++) speechPOLICE[i] = new SfxRef(sound:0x00000026r+i);
				speechPOLICE[(int)(Math.random()*POLICE_VOICES)].play();
				player.setSteamAchievement(Steam.ACHIEVEMENT_BUSTED);

				break;
		}
		
		osd.globalHandler = this;
	}

	public String getTitle(int m)
	{
		switch(m)
		{
			case 0:
				return "You are busted!";
				break;

			case 1:
				return "Problems, sir?";
				break;
		}

		return null;
	}

	public String getButtonTitle(int m)
	{
		switch(m)
		{
			case 0:
				return "GOT IT";
				notify();
				break;
		}

		return null;
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_OK):
				notify();
				break;

			case(CMD_OFFENSE):
				result = 1;
				notify();
				break;
		}
	}
}

public class TrafficTracker
{
	int	id;
	GameRef	car;
	int	trafficId;
	Marker	m;
	Bot	bot;
}

public class PoliceScout
{
	Bot	bot;
	int	returningTraffic;
	float	distance;

	TrafficTracker	tracker;
}

public class ParkingCar
{
	GameRef	gr;
	Vector3	origPos;
	Ypr	origOri;

	public ParkingCar(GameRef parent, GameRef type, Vector3 pos, Ypr ori, int colorSeed)
	{
		origPos = new Vector3(pos);
		origOri = new Ypr(ori);
		gr = new GameRef(parent, type, origPos.toString() + ","+origOri.y+",0,0," + colorSeed, "_.*0*._");
		gr.setMatrix(origPos, origOri);
	}

	public void finalize()
	{
		if(gr) gr.destroy();
	}

	public void reset(Vector3 playerPos)
	{
		Vector3 d = new Vector3(playerPos);
		d.sub(origPos);

		if(d.length() > 10) gr.setMatrix(origPos, origOri);
	}
}


public class NightracesData
{
	final Vector3[] startPos = new Vector3[18];
	final Ypr[] startOri = new Ypr[18];
	final Vector3[] finishPos = new Vector3[18];
	final Ypr[] finishOri = new Ypr[18];

	public NightracesData()
	{
		startPos[0] = new Vector3( -296.606, 7.603, 1356.304 );
		startOri[0] = new Ypr( -1.639, 0.000, 0.000 );
		startPos[1] = new Vector3( -360.481, 12.457, 663.527 );
		startOri[1] = new Ypr( -2.999, 0.000, 0.000 );
		startPos[2] = new Vector3( 149.582, 13.039, 1066.371 );
		startOri[2] = new Ypr( 0.211, 0.000, 0.000 );
		startPos[3] = new Vector3( -523.833, 10.102, 1447.879 );
		startOri[3] = new Ypr( 1.253, 0.000, 0.000 );
		startPos[4] = new Vector3( -827.339, 7.823, 940.699 );
		startOri[4] = new Ypr( -1.868, 0.000, 0.000 );
		startPos[5] = new Vector3( 595.824, 0.784, 1297.508 );
		startOri[5] = new Ypr( 0.480, 0.000, 0.000 );

		startPos[6] = new Vector3( 226.576, 9.498, 464.792 );
		startOri[6] = new Ypr( -1.909, 0.000, 0.000 );
		startPos[7] = new Vector3( 328.415, 2.361, -99.669 );
		startOri[7] = new Ypr( 1.843, 0.000, 0.000 );
		startPos[8] = new Vector3( 1039.097, 14.434, 385.346 );
		startOri[8] = new Ypr( -3.036, 0.000, 0.000 );
		startPos[9] = new Vector3( 1323.804, 10.885, 184.227 );
		startOri[9] = new Ypr( 3.140, 0.000, 0.000 );
		startPos[10] = new Vector3( 1324.637, 10.885, 133.709 );
		startOri[10] = new Ypr( 0.037, 0.000, 0.000 );
		startPos[11] = new Vector3( 1025.139, 3.226, -506.960 );
		startOri[11] = new Ypr( 2.124, 0.000, 0.000 );

		startPos[12] = new Vector3( -597.025, 6.195, -79.347 );
		startOri[12] = new Ypr( 0.605, 0.000, 0.000 );
		startPos[13] = new Vector3( -323.593, 9.188, -310.091 );
		startOri[13] = new Ypr( -1.303, 0.000, 0.000 );
		startPos[14] = new Vector3( -309.928, 2.396, 295.465 );
		startOri[14] = new Ypr( -1.546, 0.000, 0.000 );
		startPos[15] = new Vector3( -633.293, 9.092, 394.970 );
		startOri[15] = new Ypr( 2.483, 0.000, 0.000 );
		startPos[16] = new Vector3( 109.644, 11.987, -263.815 );
		startOri[16] = new Ypr( 1.692, 0.000, 0.000 );
		startPos[17] = new Vector3( 350.465, 4.643, -160.232 );
		startOri[17] = new Ypr( 0.479, 0.000, 0.000 );

		finishPos[0] = new Vector3( 102.489, 6.309, 1383.424 );
		finishOri[0] = new Ypr( -1.639, 0.000, 0.000 );
		finishPos[1] = new Vector3( -303.727, 11.174, 1059.489 );
		finishOri[1] = new Ypr( -2.999, 0.000, 0.000 );
		finishPos[2] = new Vector3( 65.93901571, 14.23259833, 675.215754);//Vector3( 50.424, 14.454, 602.660 );
		finishOri[2] = new Ypr( 0.211, 0.000, 0.000 );
		finishPos[3] = new Vector3( -904.633, 9.929, 1322.552 );
		finishOri[3] = new Ypr( 1.253, 0.000, 0.000 );
		finishPos[4] = new Vector3( -444.301, 8.676, 1057.920 );
		finishOri[4] = new Ypr( -1.868, 0.000, 0.000 );
		finishPos[5] = new Vector3( 411.031, 3.435, 942.638 );
		finishOri[5] = new Ypr( 0.480, 0.000, 0.000 );

		finishPos[6] = new Vector3( 604.144, 5.633, 597.412 );
		finishOri[6] = new Ypr( -1.909, 0.000, 0.000 );
		finishPos[7] = new Vector3( -57.186, 0.000, 8.065 );
		finishOri[7] = new Ypr( 1.843, 0.000, 0.000 );
		finishPos[8] = new Vector3( 1081.286, 8.047, 783.100 );
		finishOri[8] = new Ypr( -3.036, 0.000, 0.000 );
		finishPos[9] = new Vector3( 1323.130, 11.527, 584.675 );
		finishOri[9] = new Ypr( 3.140, 0.000, 0.000 );
		finishPos[10] = new Vector3( 1309.875, 12.770, -266.077 );
		finishOri[10] = new Ypr( 0.037, 0.000, 0.000 );
		finishPos[11] = new Vector3( 684.285, 6.642, -296.689 );
		finishOri[11] = new Ypr( 2.124, 0.000, 0.000 );

		finishPos[12] = new Vector3( -824.928, 11.272, -408.966 );
		finishOri[12] = new Ypr( 0.605, 0.000, 0.000 );
		finishPos[13] = new Vector3( 61.955, 15.959, -415.879 );
		finishOri[13] = new Ypr( -1.303, 0.000, 0.000 );
		finishPos[14] = new Vector3( 90.156, 2.641, 285.623 );
		finishOri[14] = new Ypr( -1.546, 0.000, 0.000 );
		finishPos[15] = new Vector3( -878.330, 12.977, 711.550 );
		finishOri[15] = new Ypr( 2.483, 0.000, 0.000 );
		finishPos[16] = new Vector3( -288.414, 11.409, -215.491 );
		finishOri[16] = new Ypr( 1.692, 0.000, 0.000 );
		finishPos[17] = new Vector3( 165.907, 17.337, -515.365 );
		finishOri[17] = new Ypr( 0.479, 0.000, 0.000 );
	}
}