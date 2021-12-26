package java.game;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.sound.*;

//for prize type check
import java.game.parts.*;
import java.game.parts.enginepart.*;

//RAXAT: global class for all gamemodes
//special dev note: finishWindow class must have different name in each child of Gamemode, otherwise compile errors will occur (game will just init the very first compiled finishWindow instead of recompiling it)

//toDo: add global OSD group (for all child GM's), add all OSD stuff there and then just show/hide it via some switching method
public class Gamemode extends GameType
{
	ResourceRef shieldIcon;
	String name;

	CareerEvent gmcEvent;
	Track gTrack;
	Bot[] gBot;
	Marker[] mBot;
	float[] nitroTanks;

	float[] time; //actual lap time
	float[] raceTime; //full race time
	float[] startTime, startTimeRace; //2nd timer is for raceTime
	int[]	calcTime; //returns non-zero value if the time is being updated
	float	bestTime; //player's best lap time
	Rank	rankings; //rank calculator instance

	int	score;
	int	failable = 1; //defines if event can have a "failed" status, enabled by default

	Thread gmThread;
	int tMethods;

	Osd osd;
	int countDown;

	Rectangle[] fineFlags;
	ResourceRef[] res_fineFlags;
	int[] fines; //begins from [0] - penalty; [1] - runout; [2] - rude driving
	int[] blink;
	int[] bTimer;

	int rdTimeout = 0; //timeout for rude driving fine

	//raceinfo_splitter
	Rectangle riSplitter;
	ResourceRef res_riSplitter;

	Rectangle[] posFont;
	ResourceRef[] res_posFont;
	int[] posFont_resid;

	int[] position; //position of player or bot in the race
	Trigger[] cpTrigger, pitTrigger;
	int[] nextTrigger;

	int raceActive = 0; //for thread, indicates that the race was started and is now active
	int destroy = 0; //to disable all regular method calls after leaving the gamemode
	int botsOnly = 0; //special
	int useTimers;
	int handleCollisions;

	int usePolice = 0;
	float policeDensity = 1.0f; //density of police cars in city traffic

	int reverse = 0; //support for reversed tracks

	//camera animation stuff
	Pori	aCamPori, aCamSpeed, aCamSpeedDest, aCamInitPoint;
	Pori[]	aCamPath;
	float	aCamDistance, aCamDestDistance, aCamRealDistance, aCamRevCheck;
	float	aCamSpeedMul = 2.0; //adjustable speed multiplier for camera animation
	int	aCamPathIndex;
	int	aCamActive = 0;
	int	aCamFadeReady = 0;

	//camera animation modes
	final static int AC_CYCLED	= 0x001;
	final static int AC_TERMINATED	= 0x002;

	//special flags to enable/disable fade FX in transitions in camera animation
	final static int ACF_FADE_ON	= 0x101;
	final static int ACF_FADE_OFF	= 0x102;

	//gamemode status flags
	final static int GMS_FAILED	= 0x201;
	final static int GMS_COMPLETED	= 0x202;
	final static int GMS_CUP_BRONZE	= 0x203;
	final static int GMS_CUP_SILVER	= 0x204;
	final static int GMS_CUP_GOLD	= 0x205;

	//sound FX
	final static int SFX_RACE_WIN	= frontend:0x0000D157r;
	final static int SFX_RACE_LOSE	= frontend:0x0000D158r;
	final static int SFX_RACE_FNSH	= frontend:0x0000D159r;
	final static int SFX_RACE_FAIL	= frontend:0x0000D15Ar;
	final static int SFX_HORN_1	= frontend:0x0000D15Br;
	final static int SFX_HORN_2	= frontend:0x0000D15Cr;

	//internal fade FX for changing camera views and other purposes
	Text	fader;
	int	fadeStatus = -1;
	int	fadeStage = 0; //to connect fade with cam animation

	//race position calculator stuff
	Trigger[] rpTrigger;
	int[] rpNextTrigger;
	int[] rpTotal;
	float rpTriggerSize = 40.0;

	Vector objects = new Vector();

	endRaceWindow erWindow;

	int useDebug = 0; //override some switches for debugging

	public Gamemode() {}

	public void init()
	{
		if(gmcEvent)
		{
			if(gmcEvent.useFines)
			{
				fineFlags = new Rectangle[3];
				res_fineFlags = new ResourceRef[6];
				fines = new int[3];

				blink = new int[3];
				bTimer = new int[3];

				//setup blink/fine integers
				for(int i=0; i<3; i++)
				{
					blink[i] = 1;	bTimer[i] = 0;	fines[i] = 0;
				}
			}
		}
	}

	public void launch()
	{
		if(gmcEvent && gmcEvent.fee)
		{
			int bucks = GameLogic.player.getMoney();

			bucks -= gmcEvent.fee; //discount the entry fee, if detected in the loaded career event
			GameLogic.player.setMoney(bucks);
		}
		countDown = 3;

		if(useTimers)
		{
			setupTimers();
			bestTime = 0.0f;
		}

		if(gmcEvent)
		{
			if(gmcEvent.useCamAnimation && gmcEvent.track)
			{
				//set speed of camera animation
				if(gmcEvent.camSpeedMulCustom) aCamSpeedMul = gmcEvent.camSpeedMulCustom; //event data has higher priority
				else
				{
					if(gmcEvent.track.camSpeedMul) aCamSpeedMul = gmcEvent.track.camSpeedMul; //if no custom data found in career event
				}
			}
		}
		addTimer(0,3); //initialize the first action in the gamemode

		if(gmcEvent && gmcEvent.useFines) handleCollisions = 1;

		if(handleCollisions)
		{
			setEventMask( EVENT_CURSOR|EVENT_COMMAND|EVENT_HOTKEY|EVENT_TRIGGER_ON|EVENT_TRIGGER_OFF|EVENT_TIME|EVENT_COLLISION );
			if(GameLogic.player.car) addNotification(GameLogic.player.car, EVENT_COLLISION, EVENT_SAME, null, "handleCollision"); //gamemode must have a fines support for this!!
		}
		else	setEventMask( EVENT_CURSOR|EVENT_COMMAND|EVENT_HOTKEY|EVENT_TRIGGER_ON|EVENT_TRIGGER_OFF|EVENT_TIME );
	}

	public void unlaunch()
	{
		//every child class must have a proper finalize() to exit normally
		if(mBot && gTrack.nav)
		{
			for(int i=0; i<mBot.length; i++) gTrack.nav.remMarker(mBot[i]);
		}

		if(gTrack) gTrack.getOut();
	}

	public void finalize()
	{
		if(gmThread)
		{
			gmThread.stop();
			gmThread = null;
		}

		if(fader)
		{
			fader.finalize();
			fader = null;
		}

		if(erWindow)
		{
			erWindow.finalize();
			erWindow = null;
		}

		if(gmcEvent && gmcEvent.track) gmcEvent.track.finalize();
		if(objects) objects = null;

		clearEventMask(EVENT_ANY);
		removeAllTimers();
		countDown = 0;

		//check for darken state and revert it back if needed
		if(osd && osd.darkStatus > 1) osd.darken();

		raceActive = 0;
		destroy = 1; //kill the gamemode
	}

	//for service purposes; returns total amount of GM racers, including player
	public int calcArray()
	{
		int gm_racers;

		if(gmcEvent.racers) gm_racers = gmcEvent.racers;
		if(!botsOnly) gm_racers += 1; //add player
		return gm_racers;

		return 0;
	}

	//service message
	public void debug(String msg)
	{
		if(gTrack) gTrack.setMessage(msg);
	}

	//abstract method for debug gamemode
	public void handleDebug();

	//error report handling
	public void error(String msg)
	{
		if(System.nextGen())
		{
			errorVM(msg); //dump to error.log for build 900
		}
		else	System.log(msg); //compatibility mode
	}

	//trigger setup for race position calculator
	public void setupRacePosCalc()
	{
		if(gmcEvent.track.raceposData)
		{
			rpTrigger = new Trigger[gmcEvent.track.raceposData.size()];
			for(int k=0; k<gmcEvent.track.raceposData.size(); k++)
			{
				//notification setup goes in child classes! keep that in mind
				rpTrigger[k] = new Trigger(gTrack.map, null, gmcEvent.track.raceposData.elementAt(k), rpTriggerSize, rpTriggerSize, 5, "GM race position trigger #" + k);
			}

			rpNextTrigger = new int[calcArray()];
			rpTotal = new int[calcArray()];
			for(int l=0; l<rpNextTrigger.length; l++)
			{
				rpNextTrigger[l] = 1;
			}
		}
	}

	//setup traffic and pedestrians for city maps
	public void setupCity()
	{
		GroundRef map = gTrack.map;
		float hour = GameLogic.getTime() / 3600;

		if( hour > 4 && hour < 22 )
		{
			map.addTraffic(new GameRef(cars.traffic.Taxi:0x00000006r), 80, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Ambulance:0x00000006r), 20, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.FireEngine:0x00000006r), 12, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Coach:0x00000006r), 30, 2, 10,2);

			map.addTraffic(new GameRef(cars.traffic.Schoolbus:0x00000006r), 55, 2, 10, 2);
			map.addTraffic(new GameRef(cars.traffic.ArmoredVan:0x00000006r), 20, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Wagon:0x00000006r), 200, 2, 5, 2);
			
			map.addTraffic(new GameRef(cars.traffic.Erbilac:0x00000006r), 100, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Erbilac_2:0x00000006r), 100, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Erbilac_3:0x00000006r), 100, 2, 5, 2);
			
			map.addTraffic(new GameRef(cars.traffic.CivilVan:0x00000006r), 100, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.CivilVan_2:0x00000006r), 100, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.CivilVan_3:0x00000006r), 100, 2, 5, 2);
			
			map.addTraffic(new GameRef(cars.traffic.Peterbild:0x00000006r), 50, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Peterbild_2:0x00000006r), 50, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Peterbild_3:0x00000006r), 50, 2, 5, 2);
			
			map.addTraffic(new GameRef(cars.traffic.Pickup:0x00000006r), 75, 2, 7, 2);
			map.addTraffic(new GameRef(cars.traffic.Pickup_2:0x00000006r), 75, 2, 7, 2);
			map.addTraffic(new GameRef(cars.traffic.Pickup_3:0x00000006r), 75, 2, 7, 2);

			map.setPedestrianDensity( 0.003 );
		}
		else
		{
			map.addTraffic(new GameRef(cars.traffic.Erbilac:0x00000006r), 20, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Peterbild:0x00000006r), 20, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Pickup:0x00000006r), 20, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.CivilVan:0x00000006r), 10, 2, 5, 2);
			map.addTraffic(new GameRef(cars.traffic.Taxi:0x00000006r), 150, 2, 5, 2);

			map.setPedestrianDensity( 0.0005 );
		}

		if(usePolice)
		{
			if(gmcEvent.copDensity) policeDensity = gmcEvent.copDensity;
			map.addTraffic(new GameRef(cars.misc.Police:0x0006r), 30*policeDensity, 2, 5, 2);
		}

		map.addPedestrianType(new GameRef(humans:0x0057r));
		map.addPedestrianType(new GameRef(humans:0x0058r));
		map.addPedestrianType(new GameRef(humans:0x0059r));
		map.addPedestrianType(new GameRef(humans:0x005Ar));
		map.addPedestrianType(new GameRef(humans:0x005Br));
		map.addPedestrianType(new GameRef(humans:0x0022r));

		map.setWater(new Vector3(0.0,-8.0,-1500.0), new Vector3(0.0,1.0,0.0), 300.0, 50.0);
		map.addWaterLimit(new Vector3(0.0,0.0,-500.0), new Vector3(0.0,0.0,1.0));
	}

	public void setupCheckpoints()
	{
		if(gmcEvent)
		{
			if(gmcEvent.track.checkpoints && gmcEvent.track.checkpoints.size() > 0) //track checkpoints
			{
				cpTrigger = new Trigger[gmcEvent.track.checkpoints.size()];
				nextTrigger = new int[calcArray()];

				for(int i=0; i<gmcEvent.track.checkpoints.size(); i++)
				{
					cpTrigger[i] = new Trigger( gTrack.map, null, gmcEvent.track.checkpoints.elementAt(i), 30, 30, 5, "GM trigger #" + i );
				}

				if(cpTrigger)
				{
					for(int k=1; k<calcArray(); k++) {nextTrigger[k] = 0;} //if will cause bugs, set nextTrigger[k] = cpTrigger.length-2
					if(!botsOnly) nextTrigger[0] = 0; //player
						else nextTrigger[0] = cpTrigger.length-2; //bot
				}
			}

			if(gmcEvent.track.pits && gmcEvent.track.pits.size() > 0) //pitstop stuff
			{
				pitTrigger = new Trigger[gmcEvent.track.pits.size()];

				int n, m; //for reverse related calculations
				if(!reverse) {n = 0; m = 1;}
				else {n = gmcEvent.track.pits.size()-1; m = -1;}

				for(int k=0; k<gmcEvent.track.pits.size(); k++)
				{
					pitTrigger[k] = new Trigger( gTrack.map, null, gmcEvent.track.pits.elementAt(n), 2, 2, 5, "pitstop trigger #" + k );
					n += m;
				}
			}
		}
	}

	//direct setup: both numbers can be changed manually
	public void setupPosFont(int num)
	{
		posFont[0].changeTexture(new ResourceRef(posFont_resid[0]+num-1));
		posFont[1].changeTexture(new ResourceRef(posFont_resid[1]+calcArray()-1));
	}

	//connected with auto update
	public void updatePosFont()
	{
		posFont[0].changeTexture(new ResourceRef(posFont_resid[0]+position[0]-1));
	}

	public void updateFines(int rr, int ff) //res_fineFlags idx, fineFlags idx; rr: {0 - penalty; 2 - runout; 1 - rude driving}
	{
		int status = fines[rr]; //status: {1...3}
		int rID  = res_fineFlags[rr+3].typeID;
		int rID2 = res_fineFlags[rr].typeID;
		
		//will cause bugs if use {if...else}
		if(ff != 4) if(blink[ff] == -1) status -= 1;		if(ff == 4) {if(blink[0] == -1) status -= 1;}

		if(ff != 4)
		{
			if(status == 1 || status == 0)
			{
				if(status == 1) fineFlags[ff].changeTexture(new ResourceRef(rID2)); //active
					else fineFlags[ff].changeTexture(new ResourceRef(rID)); //disabled
			}
			else
			{
				if(ff != 0) fineFlags[ff].changeTexture(new ResourceRef(rID+3-ff+status-1)); //yellow or red flag
				else 	    fineFlags[ff].changeTexture(new ResourceRef(rID+3-ff+status-3)); //special
			}

			blink[ff] *= (-1);
			bTimer[rr]++;
		}
		else //very special case: actual ff is still 1 when using 2 flags instead of 3, so we define special fine updates by setting ff as 4
		{
			if(status == 1 || status == 0)
			{
				if(status == 1) fineFlags[0].changeTexture(new ResourceRef(rID2)); //active
					else fineFlags[0].changeTexture(new ResourceRef(rID)); //disabled
			}
			else	fineFlags[0].changeTexture(new ResourceRef(rID+1+status-1)); //yellow or red flag

			blink[0] *= (-1);
			bTimer[rr]++;
		}
	}

	//ripped version of updateFines; just removes artefacts after blinking animation
	public void fixFineStatus(int rr, int ff)
	{
		if(ff != 4)
		{
			if(fines[rr] == 1 || fines[rr] == 0)
			{
				if(fines[rr] == 1) fineFlags[ff].changeTexture(new ResourceRef(res_fineFlags[rr].typeID));
					else fineFlags[ff].changeTexture(new ResourceRef(res_fineFlags[rr+3].typeID));
			}
			else
			{
				if(ff != 0)	fineFlags[ff].changeTexture(new ResourceRef(res_fineFlags[rr+3].typeID+3-ff+fines[rr]-1));
				else		fineFlags[ff].changeTexture(new ResourceRef(res_fineFlags[rr+3].typeID+3-ff+fines[rr]-3));
			}

			blink[ff] = 1;
			bTimer[rr] = 0;
		}
		else
		{
			if(fines[rr] == 1 || fines[rr] == 0)
			{
				if(fines[rr] == 1) fineFlags[0].changeTexture(new ResourceRef(res_fineFlags[rr].typeID));
					else fineFlags[0].changeTexture(new ResourceRef(res_fineFlags[rr+3].typeID));
			}
			else	fineFlags[0].changeTexture(new ResourceRef(res_fineFlags[rr+3].typeID+1+fines[rr]-1));

			blink[0] = 1;
			bTimer[rr] = 0;
		}
	}

	//set texture to inactive for a defined flag
	public void resetFines(int rr, int ff)
	{
		fineFlags[ff].changeTexture(new ResourceRef(res_fineFlags[rr+3].typeID));
	}

	public void setupTimers() //NOT the handleEvent timers!
	{
		int gm_racers; //to calculate length for each array

		if(gmcEvent.racers) gm_racers = gmcEvent.racers;
		if(!botsOnly) gm_racers += 1; //add player

		time = new float[calcArray()];
		raceTime = new float[calcArray()];
		startTime = new float[calcArray()];
		startTimeRace = new float[calcArray()];
		calcTime = new int[calcArray()];

		for(int i=0; i<calcArray(); i++)
		{
			startTime[i] = 0.0f;
			startTimeRace[i] = 0.0f;
			calcTime[i] = 1;
		}
	}

	public void startTimers()
	{
		for(int i=0; i<startTime.length; i++)
		{
			startTime[i] = System.simTime();
			startTimeRace[i] = System.simTime();
		}
	}

	public void startTimer(int timerID)
	{
		startTime[timerID] = System.simTime();
	}

	public void stopTimers()
	{
		for(int i=0; i<calcTime.length; i++)
		{
			calcTime[i] = 0;
		}
	}

	public void stopTimer(int timerID)
	{
		calcTime[timerID] = 0;
	}

	public void updateTimers()
	{
		for(int i=0; i<time.length; i++)
		{
			if(calcTime[i])
			{
				time[i] = System.simTime() - startTime[i];
				raceTime[i] = System.simTime() - startTimeRace[i];
			}
		}
	}

	public void hideGPSFrame()
	{
		if(GameLogic.player)
		{
			GameLogic.player.gpsState = 0;
			GameLogic.player.handleGPSframe(GameLogic.player.gpsState);
		}
	}

	public void showGPSFrame()
	{
		if(GameLogic.player)
		{
			GameLogic.player.gpsState = 1;
			GameLogic.player.handleGPSframe(GameLogic.player.gpsState);
		}
	}

	public void showMinimap()
	{
		if(gmcEvent.track.nav)
		{
			//we only need to show it, track will hide nav itself
			gTrack.nav = gmcEvent.track.nav;
			gTrack.nav.show();

			if(!gTrack.mPlayer) gTrack.mPlayer = gTrack.nav.addMarker(GameLogic.player);
			if(gBot)
			{
				mBot = new Marker[gBot.length];
				for(int i=0; i<gBot.length; i++) mBot[i] = gTrack.nav.addMarker(gBot[i]);
			}
		}
	}

	public void getOSD()
	{
		//keep in mind, that OSD of track must be already created before you call this method
		if(gTrack.osd) osd = gTrack.osd;
	}

	public void createBotCars() //step 1: create the vehicles
	{
		if(gTrack && gBot && gmcEvent)
		{
			Vector vehicleNames, vehicleTypes;
			RandomBox pandora; //if amount of bots will be less than amount of vehicles in custom database

			if(gmcEvent.specialVehiclePath) //filenames, if loaded vehicles are used
			{
				vehicleNames = new Vector();
				FindFile ff = new FindFile();
				String name=ff.first(gmcEvent.specialVehiclePath + "*", FindFile.FILES_ONLY);
				while(name)
				{
					if(Vehicle.fileCheck(gmcEvent.specialVehiclePath + name)) vehicleNames.addElement(name); //sort out only main vehicle savefiles and collect them
					name = ff.next();
				}
				ff.close();

				if(gBot.length <= vehicleNames.size()) pandora = new RandomBox(vehicleNames);
			}
			else
			{
				if(gmcEvent.specialVehicles) //chassis typeid list, if generating is preferred
				{
					vehicleTypes = new Vector();
					for(int j=0; j<gmcEvent.specialVehicles.length; j++) vehicleTypes.addElement(new Integer(gmcEvent.specialVehicles[j]));
					if(gBot.length <= vehicleTypes.size()) pandora = new RandomBox(vehicleTypes);
				}
			}

			gTrack.maxAIracers = gmcEvent.racers;
			gTrack.mapVehicle = new Vehicle[gTrack.maxAIracers];
			nitroTanks = new float[gBot.length];

			//all required data is processed, now creating vehicles
			for(int i=0; i<gBot.length; i++)
			{
				if(gmcEvent.specialVehicles && gmcEvent.specialVehicles.length) //option #1: load vehicles by names from custom database
				{
					int color = (int)(Math.random()*GameLogic.CARCOLORS.length);
					VehicleDescriptor vd = GameLogic.getVehicleDescriptor(VehicleType.VS_NRACE);

					if(pandora) gTrack.mapVehicle[i] = new Vehicle(gTrack.map, gmcEvent.specialVehicles[pandora.pick().intValue()], color, vd.optical, vd.power, vd.wear, vd.tear);
					else gTrack.mapVehicle[i] = new Vehicle(gTrack.map, gmcEvent.specialVehicles[(int)(Math.chaos()*gmcEvent.specialVehicles.length)], color, vd.optical, vd.power, vd.wear, vd.tear);
				}
				else
				{
					if(vehicleNames && vehicleNames.size()) //option #2: generate vehicles from custom typeid array
					{
						if(pandora) gTrack.mapVehicle[i] = Vehicle.load(gmcEvent.specialVehiclePath + pandora.pick(), gTrack.map);
						else gTrack.mapVehicle[i] = Vehicle.load(gmcEvent.specialVehiclePath + vehicleNames.elementAt((int)(Math.chaos()*vehicleNames.size())), gTrack.map);
					}
					else gTrack.mapVehicle[i] = gBot[i].getCar(gTrack.map); //default option: pick vehicle from bot database
				}

				nitroTanks[i] = gTrack.mapVehicle[i].chassis.tank_nitro;
			}
		}
	}

	public void joinBotCars() //step 2: place bots into created vehicles
	{
		if(gTrack && gBot && gmcEvent)
		{
			for(int i=0; i<gBot.length; i++)
			{
				gBot[i].createCar(gTrack.map, gTrack.mapVehicle[i]);
				gBot[i].car.setDamageMultiplier(0.25+Math.chaos()/3); //vehicles of bots are always stronger
				gBot[i].command("ai_cheat Math.chaos()*20, 15, 15, 15, 15, 7, Math.random()*8"); //ultra-difficulty
			}
		}
	}

	public void prepareBots(int stage) //step 3: force bots to start the engines
	{
		if(gBot)
		{
			for(int i=0; i<gBot.length; i++)
			{
				if(stage == 0) gBot[i].brain.command( "AI_BeginRace 0.5" );
					else gBot[i].brain.command( "AI_BeginRace 1.0" );
			}
		}
	}

	public void destroyBots()
	{
		if(gBot)
		{
			for(int i=0; i<gBot.length; i++)
			{
				if(gBot[i])
				{
					gBot[i].deleteCar();
					gBot[i] = null;

					if(i == gBot.length) gBot = null;
				}
			}
		}

		if(gTrack)
		{
			gTrack.maxAIracers = 0;

			if(gBot)
			{
				for(int i=0; i<gBot.length; i++)
				{
					if(gTrack.mapVehicle[i])
					{
						gTrack.mapVehicle[i].destroy();
						gTrack.mapVehicle[i] = null;
					}
				}
			}
		}
	}

	public void stopCars()
	{
		if(gBot)
		{
			for(int i=0; i<gBot.length; i++)
			{
				if(gBot[i].car) gBot[i].car.queueEvent(null, EVENT_COMMAND, "brake");
			}
		}

		if(GameLogic.player)
		{
			if(GameLogic.player.car) GameLogic.player.car.queueEvent(null, EVENT_COMMAND, "brake");
		}

	}

	public void lockPlayerCar()
	{
		if(GameLogic.player)
		{
			if(GameLogic.player.car)
			{
				GameLogic.player.car.queueEvent( null, EVENT_COMMAND, "brake" );
				GameLogic.player.car.queueEvent( null, EVENT_COMMAND, "idle" );
			}
		}
	}

	public void unlockPlayerCar()
	{
		if(GameLogic.player)
		{
			if(GameLogic.player.car) GameLogic.player.car.queueEvent( null, EVENT_COMMAND, "start" );
		}
	}

	public void lockCars()
	{
		if(gBot)
		{
			for(int i=0; i<gBot.length; i++)
			{
				if(gBot[i].car)
				{
					gBot[i].car.queueEvent( null, EVENT_COMMAND, "stop" );
					gBot[i].car.queueEvent( null, EVENT_COMMAND, "idle" );
				}
			}
		}

		if(GameLogic.player)
		{
			if(GameLogic.player.car)
			{
				GameLogic.player.car.queueEvent( null, EVENT_COMMAND, "start" ); //to revert previously called command "brake"
				GameLogic.player.car.queueEvent( null, EVENT_COMMAND, "stop" );
				GameLogic.player.car.queueEvent( null, EVENT_COMMAND, "idle" );
			}
		}
	}

	public void unlockCars()
	{
		if(gBot)
		{
			for(int i=0; i<gBot.length; i++)
			{
				if(gBot[i].car) gBot[i].car.queueEvent( null, EVENT_COMMAND, "start" );
			}
		}

		unlockPlayerCar();
	}

	public void wakeUpCars()
	{
		if(gBot)
		{
			for(int i=0; i<gBot.length; i++)
			{
				if(gBot[i].car) gBot[i].car.queueEvent( null, EVENT_COMMAND, "wakeup" );
			}
		}

		if(GameLogic.player)
		{
			if(GameLogic.player.car) GameLogic.player.car.queueEvent( null, EVENT_COMMAND, "wakeup" );
		}
	}

	//runout check
	public int isOffroad()
	{
		if(gmcEvent.track && gmcEvent.track.safeSurfaces)
		{
			for(int i=0; i<gmcEvent.track.safeSurfaces.size(); i++)
			{
				if(getSurface() == gmcEvent.track.safeSurfaces.elementAt(i).intValue() || getSurface() < 0)
				return 0;
			}
		}
		return 1;

		//how to debug a "safe surface":
		//	if(GameLogic.player.car.chassis.getMaterialIndex() > 0)
		//		debug("matIndex: " + GameLogic.player.car.chassis.getMaterialIndex() + ", name: " + gTrack.map.getMaterialName(GameLogic.player.car.chassis.getMaterialIndex()));
		//after that just save asphalt/rumble material indexes to trackData
	}

	//build 900 only
	public int getSurface()
	{
		if(System.nextGen())
		{
			if(GameLogic.player.car.chassis) return GameLogic.player.car.chassis.getMaterialIndex();
		}
		else return -1;

		return 0;
	}

	//returns distance, travelled by object in point C; distance is being projected into a spline AB
	//gives most accurate results in curcuit racing race position calculator
	public float getSplineDistance(Vector3 iA, Vector3 iB, Vector3 iC)
	{
		//A, B and C combine into a triangle ABC, which height _h_ is an orthogonal projection of point C into a base AB of this triangle
		//the task is to find length of AH, which is our travelled distance inside a part of spline AB
		//below is a scheme of triangles, that was used in this method:
		//---------------------
		//           C
		//           .
		//          ...
		//         . . .
		//        .  .  .
		//       .   .   .
		//      ...........
		//     A     H     B
		//---------------------
		//for instance, a car[0] does travel lengthwise the curved path AB, from point a into point B; an another car[1] is travelling trough the same path with a speed, that is higher than speed of car[0]
		//the question is, which of two cars is closer to the point B, considering fact, that the AB path is so curved, that simple 2D distance comparison will not work

		Vector3 A,B,C;
		float AC,AB,CB,AH;
		float h,p,S;

		//all calculations will pass in 2D, so we push off depth from all input V3's
		A = iA.to2D();
		B = iB.to2D();
		C = iC.to2D();

		//define length of each side of triangle using V3 coordinates of all its vertices
		AC = A.distance(C);
		AB = A.distance(B);
		CB = C.distance(B);

		//check if additional calculations are not needed
		if(iC.equals(iA)) return 0;
		if(iC.equals(iB)) return AB;

		//now find area of triangle ABC, that is needed for calculation of it's height HC
		p = (AC+CB+AB)/2; //Heron's formula is used, since we know length for all triangle's sides
		S = Math.sqrt(p*(p-AC)*(p-CB)*(p-AB));
		h = (2*S)/AB; //HC side, that forms two new rectangular triangles inside of ABC, but we need only AHC

		AH = Math.sqrt(Math.sqr(AC)-Math.sqr(h)); //since AHC is rectangular, we utilize a Pythagorean theorem to find AH with a modified formula

    		return AH;
		return 0.0f;
	}

	//more advanced variant of getSplineDistance, but has another field of usage
	//the difference is, that getSplineDistanceSegment returns projected distance only for point that _does belong_ to the segment BC
	float getSplineDistanceSegment(Vector3 iB, Vector3 iC, Vector3 iA)
	{
		//scheme is the same, just another letters of triangle vertices
		//---------------------
		//           A
		//           .
		//          ...
		//         . . .
		//        .  .  .
		//       .   .   .
		//      ...........
		//     B     D     C
		//---------------------
		//B is the beginning of spline and C is its end; A is some object or vehicle that is passing through that spline path
		//our goal is to understand when point A is travelling trough spline BC and what is the distance travelled

		Vector3 A = iA.to2D();
		Vector3 B = iB.to2D();
		Vector3 C = iC.to2D();
		Vector3 D;

		//check if additional calculations are not needed
		if(iA.equals(iB)) return 0.0f;
		if(iA.equals(iC)) return A.distance(C);

		float kAD, kBC;

		//step 1: build equation of line BC
		float x,x0,x1,x2,y,y0,y1,y2,pA,pB,pC;
		float k1, k2; //angular coeffs, kBC and kAD respectively

		x0 = A.x;	y0 = A.z;
		x1 = B.x;	y1 = B.z;
		x2 = C.x;	y2 = C.z;

		//coeffs for Ax+By+C equation
		pA = y1-y2;
		pB = x2-x1;
		pC = (x1*y2)-(x2*y1);

		//step 2: find angular coeff for BC
		k1 = (pA/pB)*(-1);

		//step 3: find angular coeff for AD
		k2 = (1/k1)*(-1);

		//now find XY coords for D, solving system of two equations: 1. AD passing through point A with angular coeff k2; 2. BC with pA, pB and pC coeffs
		//AD: y-y0 = k(x-x0)
		//BC: Ax+By+C = 0

		//step 4.1: find Y
		y = (pA*((y0/k2)-x0)-pC)/(pB+(pA/k2));

		//step 4.2: find X
		x = ((y-y0)/k2)+x0;

		//step 5: D point is found, now check if it belongs to the line segment BC
		float c1,c2,d1,d2; //theese values are needed to guess the combination of signs in y1 and y2
		if(y1>y2) {c1 = x1; d1 = y1;	c2 = x2; d2 = y2;}
		if(y1<y2) {c1 = x2; d1 = y2;	c2 = x1; d2 = y1;}

		//we are sure that d1 is the max value and d2 is min value, so we compare Y to theese two abstract units
		if(y>d1) {x=c1;	y=d1;}
		if(y<d2) {x=c2; y=d2;}

		//step 6: finally, build two-dimensional point D
		D = new Vector3(x,0,y);

		return B.distance(D);
		return 0.0f;
	}

	//camera animation with fade support
	public void animateCam(int mode, int fade)
	{
		if(gTrack && gmcEvent && gmcEvent.track && aCamActive == 1 && !Integrator.frozen && !Integrator.IngameMenuActive)
		{
			if(aCamPori && aCamPathIndex >=0)
			{
				aCamDistance = new Vector3().diff( aCamPath[aCamPathIndex].pos, aCamPori.pos ).length();
			}
			else
			{
				aCamPathIndex = -1;
				aCamPori = new Pori( aCamPath[0] );
				aCamSpeed = new Pori();
			}

			if(aCamDestDistance <= aCamDistance)
			{
				if(fade == ACF_FADE_ON)
				{
					if(aCamPathIndex > 0) aCamFadeReady = 1;
					else aCamChangeView(mode); //doesn't actually change cam view, just initializes a cam movement
				}
				else aCamChangeView(mode);
			}
			else
			{
				aCamDestDistance = aCamDistance;
				if(!aCamRevCheck || aCamRevCheck == 1000000.0) aCamRevCheck = aCamDestDistance;
			}

			if(aCamPathIndex >=0)
			{
				aCamSpeed = aCamSpeed.mul(0.97).add( new Pori(aCamSpeedDest).mul(0.03*aCamSpeedMul) );
				aCamPori.add( aCamSpeed );
				if(gTrack.cam) gTrack.cam.setMatrix( aCamPori.pos, aCamPori.ori );
				aCamRealDistance = new Vector3().diff( aCamPath[aCamPathIndex].pos, aCamPori.pos ).length();

				//special check if cam will change direction of its movement
				if(aCamRevCheck != 1000000.0 && aCamDestDistance != 1000000.0)
				{
					if(aCamRevCheck < aCamRealDistance)
					{
						aCamDestDistance += ((aCamRealDistance-aCamRevCheck)*2); //this fixes distance value if a reverse direction is detected
						aCamRevCheck = 0;
					}
				}
			}
		}
	}

	//service method, applies "hard" view change, without fade effect; can be assigned to hotkey and used for debug
	public void aCamChangeView(int cmode)
	{
		if(aCamPathIndex+2 >= aCamPath.length)
		{
			aCamPathIndex = -1;
			aCamPori = new Pori( aCamPath[0] );
			aCamSpeed = new Pori();

			//no actions for mode AC_CYCLED, it's ok
			if(cmode == AC_TERMINATED)
				aCamActive = -1;
		}
		else
		{
			aCamPathIndex +=2;
			if(gTrack.cam) gTrack.cam.setMatrix( aCamPath[aCamPathIndex-1].pos, aCamPath[aCamPathIndex-1].ori );
			aCamPori = new Pori( aCamPath[aCamPathIndex-1] );
			aCamSpeedDest = new Pori().diff( aCamPath[aCamPathIndex], aCamPori ).mul( 0.001 );
			aCamDestDistance = 1000000.0;
			aCamRevCheck = 1000000.0;
		}
	}

	public void aCamPathCreate()
	{
		if(gmcEvent && gmcEvent.track)
		{
			if(gmcEvent.camPathCustom)
			{
				aCamPath = new Pori[gmcEvent.camPathCustom.length];
				for(int i=0; i<gmcEvent.camPathCustom.length; i++) aCamPath[i] = new Pori(gmcEvent.camPathCustom[i]);
			}
			else
			{
				aCamPath = new Pori[gmcEvent.track.camPath.length];
				for(int i=0; i<gmcEvent.track.camPath.length; i++) aCamPath[i] = new Pori(gmcEvent.track.camPath[i]);
			}
		}
	}

	//default fade to black
	public void createFade()
	{
		initFade();

		if(fader)
		{
			if(!fader.animationActive()) //to prevent bugs in extreme environments
			{
				if(fadeStatus == -1) fader.fadeIn(0xFF000000); //to solid black
				else fader.fadeOut(0xFF000000); //to 100% transparent

				fadeStatus *= (-1);
			}
		}
	}

	//fade with adjustable color (black/white)
	public void createFade(String color)
	{
		initFade();

		if(fader)
		{
			if(!fader.animationActive()) //to prevent bugs in extreme environments
			{
				if(color == "black")
				{
					if(fadeStatus == -1) fader.fadeIn(0xFF000000); //to solid
					else fader.fadeOut(0xFF000000); //to 100% transparent
				}

				if(color == "white")
				{
					if(fadeStatus == -1) fader.fadeIn(); //to solid
					else fader.fadeOut(); //to 100% transparent
				}

				fadeStatus *= (-1);
			}
		}
	}

	//service method
	public void initFade()
	{
		if(!fader)
		{
			if( !Config.HD_quality() )
				fader =  osd.createText( "a", Frontend.fadeRes, Text.ALIGN_CENTER,  0.0, 0.7, 0 );
			else
				fader =  osd.createText( "a", Frontend.fadeRes_HD, Text.ALIGN_CENTER,  0.0, 0.7, 0 );

		}
	}

	//empty default method, child classes MUST override it (if require any collision handling)
	public void handleCollision( GameRef obj_ref, int event, String param );

	public void hideOSD(); //default method to be implemented by the child classes

	public void updateEventStatus(int winCheck, int finPos)
	{
		int status;
		if(gmcEvent.useAwards)
		{
			if(winCheck)
			{
				switch(finPos)
				{
					case 0:
						status = GMS_CUP_GOLD;
						break;
					case 1:
						status = GMS_CUP_SILVER;
						break;
					case 2:
						status = GMS_CUP_BRONZE;
						break;
				}
			}
			else status = GMS_FAILED;
		}
		else
		{
			if(finPos == 0) status = GMS_COMPLETED; //1st place
			else status = GMS_FAILED; //2nd/3rd/etc place
		}

		if(CareerEvent.getEventStatus(gmcEvent.eventName) < status) //check current status
			CareerEvent.setEventStatus(gmcEvent.eventName, status); //set new
	}

	public void updateCareerStats(int winCheck, int finPos)
	{
		if(winCheck)
		{
			if
			(
			 gmcEvent.prize instanceof Integer ||
			 gmcEvent.prize instanceof Vehicle ||
			 gmcEvent.prize instanceof Part
			)
				{
					if(gmcEvent.prize instanceof Integer)
					{
						int result;
						Integer cash = (Integer)gmcEvent.prize;
						if(gmcEvent.useAwards)
						{
							switch(finPos) //same prize cash dividers as in CareerEvent.aquirePrize(int place)
							{
								case 0:
									result = cash.number;
									break;
								case 1:
									result = cash.number/2;
									break;
								case 2:
									result = cash.number/5;
									break;
							}
						}
						GameLogic.player.cash_earned += result;
					}
					if(gmcEvent.prize instanceof Vehicle) GameLogic.player.cars_won++;
					if(gmcEvent.prize instanceof Part) GameLogic.player.parts_won++;
				}
				else GameLogic.player.misc_won++;

			if(gmcEvent.useAwards)
			{
				switch(finPos)
				{
					case 0:
						GameLogic.player.gold_cups++;
						break;
					case 1:
						GameLogic.player.silver_cups++;
						break;
					case 2:
						GameLogic.player.bronze_cups++;
						break;
				}
			}

			GameLogic.player.races_won++;
		}
		else GameLogic.player.races_lost++;
	}

	public static void updatePoliceStats(int state)
	{
		if(state) GameLogic.player.police_escaped++;
		else GameLogic.player.police_busted++;
	}

	//if player exit race without finishing it
	public void giveUp() //connect this with ingame menu!
	{
		//save stats for a failed race
		if(failable)
		{
			updateEventStatus(0,-1);
			updateCareerStats(0,-1);
		}
	}

	//just shows up the window, not actually finishes race!
	public void finishRace(int mode)
	{
		erWindow = new endRaceWindow(osd, mode);
		erWindow.show();
	}

	//mostly used for debug purposes; normally ends up the race in any suitable moment
	public void cheatWin();

	//---begin map object creation stuff:
	public Spectator createSpectator(int o_rid, int o_bid, Vector3 o_pos, Ypr o_ori, String o_alias) //render ID, bones ID (matrixdata), pos/ori, system name (alias)
	{
		Spectator s;

		if(gTrack)
		{
			s = new Spectator(gTrack.map, o_rid, o_bid, o_pos, o_ori, o_alias);

			if(s)
			{
				objects.addElement(s);
				s.beginAnim();
				return s;
			}
		}

		return null;

		//example usage:
		//createSpectator(humans:0x002fr, humans:0x0014r, startGridData.elementAt(2), startGridData.elementAt(3), "spectator");
	}

	//vehicles, simple
	public TrafficVehicle createTraffic(int o_id, Vector3 o_pos, Ypr o_ori) //render ID, pos/ori
	{
		TrafficVehicle t;

		if(gTrack)
		{
			t = new TrafficVehicle(gTrack.map, o_id);

			if(t)
			{
				objects.addElement(t);
				t.setMatrix(o_pos, o_ori);
				return t;
			}
		}

		return null;
	}

	//vehicles, extended
	public TrafficVehicle createTraffic(int o_id, Vector3 o_pos, Ypr o_ori, String p, String a) //gametype ID, pos/ori, low-level parameters, system name (alias)
	{
		TrafficVehicle t;

		if(gTrack)
		{
			t = new TrafficVehicle(gTrack.map, o_id, p, a);

			if(t)
			{
				objects.addElement(t);
				t.setMatrix(o_pos, o_ori);
				return t;
			}
		}

		return null;
	}

	//objects, simple
	public MapObject createObject(int o_id, Vector3 o_pos, Ypr o_ori) //gametype ID, pos/ori
	{
		MapObject mo;

		if(gTrack)
		{
			mo = new MapObject(gTrack.map, o_id);

			if(mo)
			{
				objects.addElement(mo);
				mo.setMatrix(o_pos, o_ori);
				return mo;
			}
		}

		return null;
	}

	//objects, extended
	public MapObject createObject(int o_id, Vector3 o_pos, Ypr o_ori, String p, String a, int m) //gametype ID, pos/ori, low-level parameters, system name (alias), movable state
	{
		MapObject mo;

		if(gTrack)
		{
			mo = new MapObject(gTrack.map, o_id, p, a, m);

			if(mo)
			{
				objects.addElement(mo);
				mo.setMatrix(o_pos, o_ori);
				return mo;
			}
		}

		return null;
	}

	public GameType getObject(int idx)
	{
		return objects.elementAt(idx).type;
		return null;
	}
	//---end of map object creation stuff
}

//this window is designed for a moment when the race ends
public class endRaceWindow extends GameType
{
	Osd osd; //the osd of this window to let it have its own graphic layer
	Osd osd_ext; //osd of class/object that called this window

	Rectangle[] pictogram;
	int pictogram_resid;

	float deltaDist = 3.0;

	int	windowStatus = 0;
	Thread	erwThread;
	int	tMethods = 2; //amount of THOR methods

	Text	flash;

	int	mode = 0; //0 - default, finish; 1 - disqualification; 2 - race failed; 3 - chase finished; 4 - busted; 5 - false start

	public endRaceWindow(Osd o, int m)
	{
		osd_ext = o;
		osd = new Osd(1.0, 0.0, 20);
		osd.show();

		mode = m;

		pictogram = new Rectangle[3];
		switch(mode)
		{
			case 0: pictogram_resid = frontend:0xD150r; break; //race_finished_i.png
			case 1: pictogram_resid = frontend:0xD152r; break; //disqualification_i.png
			case 2: pictogram_resid = frontend:0xD167r; break; //race_failed_i.png
			case 3: pictogram_resid = frontend:0xD16Er; break; //chase_finished_i.png
			case 4: pictogram_resid = frontend:0xD170r; break; //busted_i.png
			case 5: pictogram_resid = frontend:0xD17Br; break; //false_start_i.png
		}

		windowStatus = 1; //to notify watcher thread

		erwThread = new Thread( this, "GM race end window watcher thread", 1 ); //extended! thread-oriented methodology (THOR), no injections
	}

	public void show()
	{
		erwThread.setPriority(Thread.MAX_PRIORITY);
		erwThread.start();

		for(int i=0; i<(tMethods+1); i++)
		{
			erwThread.addMethod(i);
		}

		osd_ext.darken(); //make a dark screen

		if(!Config.HD_quality())
			flash =  osd.createText( "a", Frontend.fadeRes, Text.ALIGN_CENTER,  0.0, 0.7, 0 );
		else
			flash =  osd.createText( "a", Frontend.fadeRes_HD, Text.ALIGN_CENTER,  0.0, 0.7, 0 );

		flash.changeColor(0x00000000);

		pictogram[0] = osd.createRectangle( deltaDist, 0.0, 0.6, 0.61, 1, new ResourceRef(pictogram_resid), 0 );
		pictogram[1] = osd.createRectangle(-deltaDist, 0.0, 0.6, 0.61, 1, new ResourceRef(pictogram_resid), 0 );

		pictogram[0].setupAnimation(deltaDist/20, 20, -1, "X"); //direction: left (-1)
		pictogram[0].runThread(); //begin the animation

		pictogram[1].setupAnimation(deltaDist/20, 20, 1, "X"); //direction: right (1)
		pictogram[1].runThread(); //begin the animation

		if(mode == 1 || mode == 2 || mode == 4 || mode == 5) new SfxRef(Gamemode.SFX_RACE_FAIL).play();
	}

	public void hide()
	{
		if(windowStatus == 2) //to protect window objects in destruction check calls
		{
			pictogram[2].finalize();

			pictogram[0].restartAnimation("X");
			pictogram[0].setupAnimation(deltaDist/20, 20, 1, "X"); //direction: opposite, right (1)
			pictogram[0].runThread();

			pictogram[1].restartAnimation("X");
			pictogram[1].setupAnimation(deltaDist/20, 20, -1, "X"); //direction: opposite, left (-1)
			pictogram[1].runThread();

			windowStatus = 3;
		}
	}

	public void finalize()
	{
		for(int i=0; i<pictogram.length; i++)
		{
			if(pictogram[i])
			{
				pictogram[i].finalize();
				pictogram[i] = null;
			}
		}
		erwThread.stop();
		flash.finalize();
	}

	public void run()
	{
		for(;;)
		{
			if(windowStatus == 1)
			{
				if(pictogram[0].a_finished_x) erwThread.execute(0); //call flashlight
			}

			if(windowStatus == 3)
			{
				if(pictogram[1].a_finished_x) erwThread.execute(1); //notify thread that window is ready for destruction
			}

			//THOR methods
			if(erwThread.methodStatus(0) == 1) //flashlight
			{
				if(mode == 0 || mode == 3) new SfxRef(Gamemode.SFX_RACE_FNSH).play();
				pictogram[2] = osd.createRectangle(0.0, 0.0, 0.6, 0.61, 1, new ResourceRef(pictogram_resid+1), 0 );
				flash.setupAnimation( 0x00FFFFFF, 16, -1, 2 ); //light-off
				flash.runThread();
				windowStatus = 2;

				erwThread.controlMethod(0,-1);
			}

			if(erwThread.methodStatus(1) == 1) //destruction mark
			{
				windowStatus = -1;
				osd.hide();

				erwThread.controlMethod(1,-1);
			}

			erwThread.sleep(10);
		}
	}
}