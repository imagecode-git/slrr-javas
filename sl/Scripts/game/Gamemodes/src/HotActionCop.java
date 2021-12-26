package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.sound.*;

public class HotActionCop extends Gamemode
{
	final static int POLICECAR_ID = cars.misc.Police:0x0006r;

	int copCount;
	int maxScouts = 6; //six scouts by default
	int copsKicked; //total hits into cop cars
	int copKickLimit = 15; //if player will crash more cop vehicles than this amount, he will get a speedy cop on tail immediately

	float engageDistance = 300; //radius used to engage new cops
	float criticalDistance = 500; //every cop behind this distance will be destroyed
	int supportTimeout = -1; //this does force support scouts _not_ to regenerate
	int supportDelay = 0;
	int copVoiceEnabled = 1;
	int copVoiceTimeout = 10; //timeout for "regenerating" cop voices

	int AI_strategy = -2; //-1: picking cops from traffic; 1: generating support scouts; 2: engaging speedy cop
	int lastStrategy; //AI strategy that was used before turning into "passive patrol" mode
	float[] AI_timelimits;

	//passive patrol stuff
	Vector3 PP_fakeTarget; //where police does expect to find player
	Timer 	PP_chaseTimer; //describes total time available for police to catch player in passive patrol mode
	float 	PP_safeDistance = 75; //this is how far the player should stay away from cops when they are in passive patrol mode

	Text chasetime_txt, copcount_txt;
	Vector alertedScouts; //cops from traffic
	Vector supportScouts; //cops from generator
	Vector policeCars; //traffic trackers

	Timer bustedTimer;
	int safeStart = 1; //protects player from police for the first 10 seconds since the beginning of the chase

	String carPath = "multibot/objects/police_cars/";
	String speedyCopCarFilename; //full path to speedyCop's vehicle
	SupportScout speedyCop;

	//banks of random strings, sounds
	RandomBox rHideout, rCaught, rNearSFX;
	SfxRef copVoiceSFX;

	int debugNoCops = 0; //set 1 to disable police in the gamemode
	int debugUseMarkers = 0;

	finishWindowHotActionCop fWindow;

	public HotActionCop(){}

	public void init()
	{
		name = "Hot Action Cop";
		shieldIcon = new ResourceRef(sl.Scripts.game.Gamemodes.stdpack_231:0x1003r);
	}

	public void launch()
	{
		if(!debugNoCops)
		{
			findSpeedyCopCar(); //scan all vehicles in the database before the gamemode will be launched
			usePolice = 1;
		}

		useTimers = 1;
		handleCollisions = 1;

		res_riSplitter = new ResourceRef(frontend:0xD125r);

		getOSD(); //get instance of OSD from Track()

		if(gmcEvent)
		{
			int ms = gmcEvent.specialSlots[4];
			if(ms) maxScouts = ms;
		}
		alertedScouts = new Vector();
		policeCars = new Vector();
		supportScouts = new Vector();

		//fill banks of random resources, all elements are _strings_!
		rHideout = new RandomBox();
		rHideout.add("FIND SOME HIDEOUT!");
		rHideout.add("COPS LOST YOU, KEEP SILENT!");
		rHideout.add("LET THE COPS DROP THE PURSUIT!");
		rHideout.add("LOOK FOR A SAFE PLACE!");
		rHideout.add("NOW LEAVE THE PUBLIC PLACES!");
		rHideout.close();

		rCaught = new RandomBox();
		rCaught.add("YOU GOT CAUGHT! RUN AWAY!");
		rCaught.add("COPS REVEALED YOU!");
		rCaught.add("POLICE IS TRACKING YOU AGAIN!");
		rCaught.add("COPS ARE BACK IN PURSUIT, KEEP OUT!");
		rCaught.add("NOW YOU CAN BEGIN PANIC! COPS ARE BACK!");
		rCaught.add("YOU GOT SOME GUESTS ON YOUR TAIL!");
		rCaught.add("WATCH YOUR BACK!");
		rCaught.close();

		//also generate and precache cop sounds
		rNearSFX = new RandomBox();
		int sfxCount = 16;
		int sfxID = multibot.sounds:0x00002053r;

		for(int i=0; i<sfxCount; i++)
		{
			SfxRef sref = new SfxRef(sfxID+i);
			sref.precache();
			rNearSFX.add(sref);
		}
		rNearSFX.close();

		//override GPS frame created by Track()
		if(GameLogic.player)
		{
			GameLogic.player.gpsState = 0;
			GameLogic.player.handleGPSframe(GameLogic.player.gpsState);
		}

		//this gamemode usually use custom start pos and camera animation trajectories
		if(gmcEvent)
		{
			//camera animation stuff will be assigned automatically in the parent class, but race position will be adjusted right in this place (via start grid array)
			if(gmcEvent.posStartCustom) gTrack.startGridData.setElementAt(gmcEvent.posStartCustom, 0);
			if(gmcEvent.oriStartCustom) gTrack.startGridData.setElementAt(gmcEvent.oriStartCustom, 1);
		}

		gmThread = new Thread( this, "circuit racing GM animation thread", 1 ); //extended! thread-oriented methodology (THOR), no injections
		gmThread.setPriority(Thread.MAX_PRIORITY);
		gmThread.start();

		tMethods = 12; //amount of THOR methods
		for(int i=0; i<(tMethods+1); i++) gmThread.addMethod(i);

		//camera animation stuff
		if(gmcEvent && gmcEvent.useCamAnimation) aCamPathCreate();

		//now define periods of time (in seconds), that are used to switch between different AI strategies
		AI_timelimits = new float[3];
		float base = 60;
		for(int j=0; j<AI_timelimits.length; j++)
		{
			AI_timelimits[j] = base + (Math.siRandom()*55); //longer pursuit - harder pressure upon player
			base += (55*j) + (Math.siRandom()*60);
		}

		lockPlayerCar(); //patch to prevent engine start on player's car before the 3..2..1..GO countdown
		gTrack.map.haltTrafficCross(gTrack.startGridData.elementAt(0), 30.0); //clear start pos (grid slot 0) from traffic

		AI_strategy = -1; //pick the weakest strategy - engaging traffic cops

		super.launch();

		hideGPSFrame();
	}

	public void prepareOSD()
	{
		if(osd)
		{
			riSplitter = osd.createRectangle( 2.67, 0.91, 0.32, 0.036, 1, res_riSplitter, 0 );
			riSplitter.runAnimation(0.1, 10, -1, "X"); //direction: left (-1)

			chasetime_txt = osd.createText( "RACE TIME: " + String.timeToString( 0.0f, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.92 );
			chasetime_txt.changeColor(0x00000000);
			chasetime_txt.fadeIn();

			copcount_txt = osd.createText( "COPS ON TAIL: " + copCount, Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.835 );
			copcount_txt.changeColor(0x00000000);
			copcount_txt.setupAnimation(0x0FFFFFFF, 8, 1);	copcount_txt.runThread();

			showGPSFrame();
			if(gmcEvent.track.nav)
			{
				gTrack.nav = gmcEvent.track.nav;
				gTrack.nav.show();
				gTrack.mPlayer = gTrack.nav.addMarker(GameLogic.player);
			}

			osd.show();
		}
	}

	public void hideOSD()
	{
		riSplitter.restartAnimation("X");
		riSplitter.runAnimation(0.1, 10, 1, "X"); //direction: opposite, right (1)

		chasetime_txt.restartAnimation();
		chasetime_txt.fadeOut();

		copcount_txt.restartAnimation();
		copcount_txt.fadeOut();
	}

	public void clearScreen()
	{
		gTrack.clearMsgBoxes();
		gTrack.lockMsgBoxes();
		gTrack.enableOsd(0);
		hideOSD();
	}

	public void beginRace() //here the race actually begins
	{
		if(!debugNoCops)
		{
			//generate basic chase group
			engageSupport(200, 900);
			engageSupport(200, 800);
			engageSupport(200, 700);
		}

		addTimer(3.0, 14); //3sec initial cop squad check
		addTimer(10.0, 9); //10sec initial "invulnerability" (safeStart)

		unlockCars();
		startTimers();

		addNotification(gTrack.map, EVENT_COMMAND, EVENT_SAME, null, "handleTraffic");
		addNotification(GameLogic.player.car, EVENT_COLLISION, EVENT_SAME, null, "handleCollision");

		raceActive = 1;
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	//involve scout from traffic into the chase
	public void engageScout(HAC_PoliceScout pc)
	{
		pc.map = gTrack.map;
		pc.dropDistance = 500 + 100*Math.siRandom();
		pc.bot = new Bot(20.0, 20.0, 20.0, 20.0, 20.0, 10.0);
		pc.bot.setDriverObject(GameLogic.HUMAN_POLICEMAN);
		pc.bot.createCar(gTrack.map, new Vehicle(pc.tracker.car));
		pc.bot.traffic_id = pc.tracker.trafficId;
		pc.bot.imaPoliceDriver=1;

		if(AI_strategy != 3) AI_decide_normalChase(pc.bot);
		else AI_decide_passivePatrol(pc.bot);

		pc.bot.pressHorn();
		pc.tracker.onMission = 1;
		if(debugUseMarkers) pc.tracker.m = gTrack.nav.addMarker(Marker.RR_POLICE, pc.tracker.car);
	}
	
	//generate nearest scout
	public void engageSupport(float radius, float minDrop)
	{
		SupportScout s = new SupportScout();
		s.map = gTrack.map;
		s.dropDistance = minDrop + 150*Math.siRandom();
		s.bot = new Bot(20.0, 20.0, 20.0, 20.0, 20.0, 10.0);
		s.car = new Vehicle(s.bot, POLICECAR_ID, 8.0, 1.0, 1.0, 1.0, 1.0);
		if(debugUseMarkers) s.m = gTrack.nav.addMarker(Marker.RR_POLICE, s.car);
		setupScout(s, radius);
		supportScouts.addElement(s);
	}

	//generate special police scout with a high-speed vehicle
	public void engageSpeedyCop(float radius)
	{
		if(speedyCopCarFilename != "") //if database is OK or if the vehicle can be created
		{
			SupportScout s = new SupportScout();
			s.map = gTrack.map;
			s.dropDistance = 100 + 100*Math.siRandom(); //debug 100 + 100r
			s.bot = new Bot(20.0, 20.0, 20.0, 20.0, 20.0, 10.0);
			s.car = Vehicle.load(speedyCopCarFilename, gTrack.map); //random vehicle, but should match class of player's car instead
			s.speedyCop++;
			setupScout(s, radius);
			supportScouts.addElement(s);
			speedyCop = s; //for easier AI handling
		}
	}

	//service method
	public void setupScout(SupportScout pc, float r) //r is a distance, desired for creating police scout
	{
		Vector3 pos;

		if(supportScouts.size())
		{
			SupportScout spc = supportScouts.lastElement();
			if(spc && spc.car)
			{
				pos = gTrack.map.getNearestCross(spc.car.getPos(), r+Math.random()*700);
				if(AI_strategy < 2) pos = fixCross(pos);
			}
		}

		if(!pos) pos = gTrack.map.getNearestCross(GameLogic.player.car.getPos(), r+Math.random()*400);

		pc.map = gTrack.map;
		pc.bot.setDriverObject(GameLogic.HUMAN_POLICEMAN);
		pc.bot.createCar(gTrack.map, pc.car);
		pc.car.setPos(pos);
		pc.car.setParent(gTrack.map);
		pc.car.queueEvent(null, EVENT_COMMAND, "reset");
		pc.car.queueEvent(null, EVENT_COMMAND, "reload");
		AI_decide_normalChase(pc.bot);
		pc.bot.pressHorn();
		pc.bot.imaPoliceDriver=1;
	}

	//destroy police scout and regenerate traffic
	public void killScout(int idx)
	{
		HAC_PoliceScout hpc = alertedScouts.elementAt(idx);

		if(debugUseMarkers) gTrack.nav.remMarker(hpc.tracker.m);
		hpc.bot.releaseHorn();
		alertedScouts.removeElementAt(idx);
		if(hpc.bot.traffic_id) policeCars.addElement(hpc.tracker);
		hpc.bot.deleteCar();
		hpc.bot = null;
		hpc = null;
	}

	//destroy support scout
	public void killSupport(int idx)
	{
		SupportScout spc = supportScouts.elementAt(idx);

		if(debugUseMarkers) gTrack.nav.remMarker(spc.m);
		spc.bot.releaseHorn();
		supportScouts.removeElementAt(idx);
		spc.bot.deleteCar();
		spc.bot = null;
		spc = null;
	}

	public void stopScouts()
	{
		//variant #1: neutralize all active cop vehicles
/*
		for(int i=0; i<alertedScouts.size(); i++)
		{
			HAC_PoliceScout hpc = alertedScouts.elementAt(i);
			if(hpc)
			{
				hpc.bot.releaseHorn();
				hpc.bot.beStupid();
				hpc.tracker.car.queueEvent(null, EVENT_COMMAND, "brake");
			}

			for(int j=0; j<supportScouts.size(); j++)
			{
				SupportScout spc = supportScouts.elementAt(j);
				if(spc)
				{
					if(!spc.speedyCop) spc.bot.releaseHorn();
					spc.bot.beStupid();
					spc.car.queueEvent(null, EVENT_COMMAND, "brake");
				}
			}
		}
*/
		//variant #2:  just kill all cops
		for(int i=0; i<alertedScouts.size(); i++) killScout(i);
		for(int j=0; j<supportScouts.size(); j++) killSupport(j);
	}

	public Vector3 fixCross(Vector3 input)
	{
		Vector3 result;
		Vector3 tPos;
		float minDist = 150;
		float maxDist = 700;

		if(GameLogic.player.car)
		{
			tPos = GameLogic.player.car.getPos();
			Vector3 r = input;

			if(input.distance(tPos) < minDist)
			{
				r.x += minDist;
				r.z += minDist;
			}

			if(input.distance(tPos) > maxDist)
			{
				r.x -= maxDist/2;
				r.z -= minDist/2;
			}

			if(r) result = gTrack.map.alignToRoad(r)[0];
		}

		if(result) return result;
		return null;
	}

	public int findWeakestHPC()
	{
		float weakDist; //distance to the weakest police scout
		int weakest;
		HAC_PoliceScout hpc;

		for(int i=0; i<alertedScouts.size(); i++)
		{
			hpc = alertedScouts.elementAt(i);

			float hDist = hpc.getDistance();
			int found;

			if(!weakDist)
			{
				weakDist = hDist;
				found++;
			}
			else
			{
				if(hDist > weakDist)
				{
					weakDist = hDist;
					found++;
				}
			}

			if(found) weakest = i;
		}

		return weakest;
		return -1;
	}

	//get car for speedyCop
	public void findSpeedyCopCar()
	{
		String reserveCarFilename, fastestCarFilename;
		Vector fileNameList = new Vector();
		FindFile ff = new FindFile();
		String name=ff.first(carPath + "*", FindFile.FILES_ONLY);
		while(name)
		{
			if(Vehicle.fileCheck(carPath + name)) fileNameList.addElement(name); //sort out only main vehicle savefiles and collect them
			name = ff.next();
		}
		ff.close();

		int pClass = gmcEvent.checkClass(); //class of player's vehicle
		int cClass; //of speedyCop's vehicle
		int fClass, maxHP; //supposed to be the fastest vehicle

		for(int i=0; i<fileNameList.size(); i++)
		{
			String fullpath = carPath + fileNameList.elementAt(i);
			Vehicle car = Vehicle.load(fullpath, gTrack.map);
			cClass = gmcEvent.checkClass(car);

			if(cClass >= fClass)
			{
				int found;
				int hp = gmcEvent.getHP(car);
				fClass = cClass;

				if(!fastestCarFilename)
				{
					maxHP = hp;
					found++;
				}
				else
				{
					if(hp > maxHP)
					{
						maxHP = hp;
						found++;
					}
				}

				if(found) fastestCarFilename = fullpath;
			}

			if(cClass > pClass)
			{
				if(!reserveCarFilename) reserveCarFilename = fullpath; //saving results for a faster vehicle
			}

			if(cClass == pClass) //attempting to find a vehicle of a same class as a player's one
			{
				if(!speedyCopCarFilename) speedyCopCarFilename = fullpath;
			}

			if(car)
			{
				car.destroy();
				car=null; //destroying temp vehicle, if it's still alive
			}
		}

		if(reserveCarFilename) speedyCopCarFilename = reserveCarFilename;
		else
		{
			if(!speedyCopCarFilename) speedyCopCarFilename = fastestCarFilename; //picking fastest vehicle if no matching car was found in database
		}
	}

	public void AI_decide_passivePatrol(Bot brain)
	{
		if(!PP_fakeTarget) brain.startRace(GameLogic.player.car.getPos(), GameLogic.player); //for safety
		else brain.startRace(PP_fakeTarget, GameLogic.player);
	}

	public void AI_decide_normalChase(Bot brain)
	{
		brain.stopCar(GameLogic.player.car);
	}

	public void AI_decide_attackPlayer(Bot brain)
	{
		//bump player car if it's near cop!
	}

	//this method completely initiates passive patrol
	public void AI_goPassive()
	{
		generateFakeTarget(); //attempt to reveal player's current location and force AI to drive there
		PP_chaseTimer = new Timer(20+Math.random()*25); //define how long the chase will be in a passive patrol mode
		PP_chaseTimer.start();
		addTimer(Math.random()*5, 7); //begin regenerating fake target

		if(AI_strategy != 3) //for safety
		{
			lastStrategy = AI_strategy; //memorize recently used AI strategy
			AI_strategy = 3;
		}
		gmThread.execute(9); //show up msgBox
	}

	//passive patrol: cops are attempting to detect approximated player's position
	public void generateFakeTarget()
	{
		PP_fakeTarget = gTrack.map.getNearestCross(GameLogic.player.car.getPos(), Math.random()*350);
	}

	//get info about total amount of cops chasing player and update OSD stuff
	public void updateTailCount()
	{
		if(AI_strategy < 3) //cop count will be displayed only for scouts, that are _not_ in a "passive patrol" mode
		{
			String dbg_SC;
			if(speedyCop) dbg_SC = "SC";
			else dbg_SC = "";

			copCount = alertedScouts.size() + supportScouts.size();
			copcount_txt.changeText("COPS ON TAIL: " + copCount);
			//copcount_txt.changeText("CC: " + copCount + "; HPC: " + alertedScouts.size() + "; SPC: " + supportScouts.size() + " " + dbg_SC); //debug!
		}
		else copcount_txt.changeText("CONNECTION LOST"); //passive patrol (but SPC count is still tracked!)
	}

	//realtime AI strategy update
	public void updateAIStrategy()
	{
		if(AI_strategy < 3)
		{
			if(AI_strategy !=2)
			{
				for(int i=0; i<AI_timelimits.length; i++)
				{
					if(raceTime[0] >= AI_timelimits[i])
					{
						int u;
						if(copsKicked >= copKickLimit) u=2; //immediately switch to the most efficient strategy (i.e. call speedy cop)
						else u = i+1;

						if(u > AI_strategy && u!=3) AI_strategy = u;
					}
				}
			}

			int goPassive;
			if(AI_strategy == 2) //speedyCop is on tail?
			{
				if(copCount == 1) //in theory, this _supposed_ to be speedyCop (since we can't kill him, but he is counted in statistics)
				{
					if(speedyCop)
					{
						if(speedyCop.getDistance() >= speedyCop.dropDistance) goPassive++; //we got out of ALL cops!
					}
					else
					{
						//debug("ERROR! NO SC TO UPDATE AI STRATEGY!");
						engageSpeedyCop(150+(Math.siRandom()*50)); //temp patch, ~200 meters out from player
					}
				}
			}
			else
			{
				if(copCount == 0) goPassive++;
			}

			if(goPassive && !safeStart) AI_goPassive(); //player can't escape from cops if the pursuit has just began
		}
	}

	//police scout manager
	public void updateScouts()
	{
		Vector3 target; //to push off weak scouts

		if(AI_strategy == 3) //"passive patrol" mode
		{
			updatePassivePatrol();
			target = PP_fakeTarget;
		}

		//-----------ADDING COPS
		if(AI_strategy > -2 || AI_strategy == 3) //involving scouts from traffic
		{
			int weakpc; //idx of weakest police scout
			HAC_PoliceScout pc;
			HAC_TrafficTracker chaser;

			for(int i=0; i<policeCars.size(); i++)
			{
				if(!target) target = GameLogic.player.car.getPos();
				float distance = target.distance(policeCars.elementAt(i).car.getPos());
				if(distance <= engageDistance) //nearest cop found in traffic, now check if he can be envolved into a chase
				{
					int engage;
					if(!maxCops()) engage = 1; //engage any nearest HPC scout
					else
					{
						int find = findWeakestHPC(); //or replace the weakest ones
						if(find > 0)
						{
							engage = 2;
							weakpc = find;
						}
					}

					if(engage)
					{
						if(engage == 2) killScout(weakpc);

						chaser = policeCars.elementAt(i);
						pc = new HAC_PoliceScout();
						pc.map = gTrack.map;
						pc.tracker = chaser;
						engageScout(pc);
						alertedScouts.addElement(pc);
						policeCars.removeElementAt(i);
					}
				}
			}

			if(AI_strategy > 0 && AI_strategy < 3) //generating support scouts
			{
				if(!maxCops() && supportTimeout < 1)
				{
					if(supportScouts.size() <= maxScouts/2) //if we don't have anough HPC scouts, we engage support
					{
						if(!supportDelay) engageSupport(100+Math.random()*50, 600); //min. 100 meters out from player, min 600 meters drop distance
					}

					if(supportTimeout < 0)
					{
						addTimer(Math.random()*75, 5); //max 75 seconds to begin regerating support squad
						supportTimeout = 0;
					}

					if(!supportDelay)
					{
						addTimer(4.5, 6); //delay between respawns of SPC's, minimize time value for total hardcore
						supportDelay = 1;
					}
					
				}
			}

			if(AI_strategy == 2) //engaging speedy cop
			{
				if(!speedyCop) engageSpeedyCop(150+(Math.siRandom()*50)); //~200 meters out from player
			}

			//-----------REMOVING COPS
			for(int i=0; i<alertedScouts.size(); i++)
			{
				HAC_PoliceScout hpc = alertedScouts.elementAt(i);
				if(hpc)
				{
					if(hpc.getDistance() >= hpc.dropDistance || hpc.criticalState(criticalDistance)) killScout(i);
				}
			}

			for(int j=0; j<supportScouts.size(); j++)
			{
				SupportScout spc = supportScouts.elementAt(j);
				if(spc)
				{
					if(spc.getDistance() >= spc.dropDistance || spc.criticalState(criticalDistance))
					{
						if(!spc.speedyCop) killSupport(j); //speedyCop will be _always_ on mission since the first time he was engaged
					}
				}
			}
		}
	}

	public void updatePassivePatrol()
	{
		if(PP_chaseTimer && !PP_chaseTimer.stopped)
		{
			if(speedyCop)
			{
				if(speedyCop.car.getPos().distance(GameLogic.player.car.getPos()) < PP_safeDistance/2) resumePursuit(); //player unveiled himself
			}

			for(int i=0; i<alertedScouts.size(); i++)
			{
				HAC_PoliceScout hpc = alertedScouts.elementAt(i);
				GameRef car;
				if(hpc.tracker) car = hpc.tracker.car;

				if(hpc && car && car.getPos())
				{
					if(car.getPos().distance(GameLogic.player.car.getPos()) < PP_safeDistance) resumePursuit(); //player unveiled himself
					else
					{
						if(hpc.getDistance(PP_fakeTarget) < 100) //works well, 100m distance seems to be OK
						{
							generateFakeTarget();
							AI_decide_passivePatrol(hpc.bot); //redirecting AI to the new fake target
							if(speedyCop) AI_decide_passivePatrol(speedyCop.bot); //same for speedyCop
						}
					}
				}
			}
		}
		else gmThread.execute(7); //player escaped, chase ends up here
	}
	
	public void checkBusted()
	{
		if(AI_strategy < 3)
		{
			for(int i=0; i<alertedScouts.size(); i++)
			{
				HAC_PoliceScout hpc = alertedScouts.elementAt(i);
				if(hpc)
				{
					if(hpc.getDistance() <= 25) //use the voice, if player is ~50m near the cop
					{
						//force HPC cops to use the "voice"
						if(copVoiceEnabled && hpc.getDistance() > 0)
						{
							copVoiceSFX = new SfxRef(rNearSFX.pick());
							if(hpc.tracker.car) copVoiceSFX.play(hpc.tracker.car.getPos(), 100, 1.0, 35.0, 0);
							copVoiceSFX.play(); //additional SFX play for a more realistic sound
							addTimer(copVoiceTimeout, 8); //reset copVoiceEnabled in a predefined timeout
							copVoiceEnabled = 0;
						}

						if(hpc.getDistance() <= 20)
						{
							if(!bustedTimer) spawnBustedTimer(6); //player is about to get busted! he got 6 seconds to escape
							else
							{
								if(bustedTimer.stopped)
								{
									//5.555m/s ~ 20KPH
									if(GameLogic.player.car.getSpeed() <= 5.555) gmThread.execute(1); //player is now busted!
								}
							}
						}
						else killBustedTimer();
					}
				}
			}

			for(int j=0; j<supportScouts.size(); j++)
			{
				SupportScout spc = supportScouts.elementAt(j);
				if(spc)
				{
					if(spc.getDistance() <= 50)
					{
						//force support scouts or speedyCop to use the "voice"
						if(copVoiceEnabled && spc.getDistance() > 0)
						{
							copVoiceSFX = new SfxRef(rNearSFX.pick());
							if(spc.car) copVoiceSFX.play(spc.car.getPos(), 125, 1.0, 50.0, 0);
							copVoiceSFX.play();
							addTimer(copVoiceTimeout, 8); //reset copVoiceEnabled in a predefined timeout
							copVoiceEnabled = 0;
						}

						if(spc.getDistance() <= 30)
						{
							if(!spc.speedyCop) //player is caught by support scout
							{
								if(!bustedTimer) spawnBustedTimer(5); //5 seconds for support scouts
								else
								{
									if(bustedTimer.stopped)
									{
										//5.555m/s ~ 20KPH
										if(GameLogic.player.car.getSpeed() <= 5.555) gmThread.execute(1);
									}
								}
							}
							else //player is caught by speedy cop
							{
								if(!bustedTimer) spawnBustedTimer(2); //only 2 seconds to escape if speedyCop is near the player
								else
								{
									if(bustedTimer.stopped)
									{
										//8.333m/s~30KPH
										if(GameLogic.player.car.getSpeed() <= 8.333) gmThread.execute(1);
									}
								}
							}
						}
						else killBustedTimer();
					}
				}
			}
		}
	}

	public void forceInitialSquad()
	{
		if(safeStart)
		{
			if(!copCount) engageSupport(200, 800);
			else addTimer(0.5, 14); //generate SPC again if previous generation attempt was unsuccessful
		}
	}

	public void spawnBustedTimer(int time)
	{
		bustedTimer = new Timer(time);
		bustedTimer.start();
	}

	public void killBustedTimer()
	{
		if(bustedTimer)
		{
			bustedTimer.stop();
			bustedTimer.finalize();
			bustedTimer = null;
		}
	}

	//cops are back on mission
	public void resumePursuit()
	{
		AI_strategy = lastStrategy;
		supportTimeout = -1; //cops are getting back to agressive state, so we reset this timeout and continue generating support scouts

		PP_chaseTimer.stop();
		PP_chaseTimer.finalize();
		PP_chaseTimer = null;

		for(int i=0; i<alertedScouts.size(); i++) AI_decide_normalChase(alertedScouts.elementAt(i).bot); //all HPC's from passive patrol squad now get involved into a normal chase
		if(speedyCop) AI_decide_normalChase(speedyCop.bot);
		gmThread.execute(8); //show up msgBox
	}

	public int maxCops()
	{
		int max;
		if(AI_strategy == 3) max = maxScouts/2; //less scouts in passive patrol mode
		else max = maxScouts-1;

		if(copCount >= max) return 1;
		return 0;
	}

	public void handleTraffic(GameRef obj_ref, int event, String param)
	{
		String cmd = param.token(0);

		if(cmd == "car_add")
		{
			HAC_TrafficTracker tt = new HAC_TrafficTracker();
			tt.id = param.token(1).intValue();
			tt.trafficId = param.token(2).intValue();
			tt.car = new GameRef(tt.id);

			int typeId = tt.car.getInfo(GII_TYPE);
			if(typeId == POLICECAR_ID) policeCars.addElement(tt);
		}

		if(cmd == "car_rem")
		{
			int id = param.token(1).intValue();

			for(int i=policeCars.size()-1; i>=0; i--)
			{
				HAC_TrafficTracker tt = policeCars.elementAt(i);
				if(tt.id == id)
				{
					if(!tt.onMission)
					{
						policeCars.removeElementAt(i);
						return;
					}
				}
			}
		}
	}

	public void handleCollision( GameRef obj_ref, int event, String param )
	{
		if(!destroy && raceActive)
		{
			int colliderID = param.token(0).intValue();
			GameRef obj = new GameRef(colliderID);
			int cat = obj.getInfo(GII_CATEGORY);

			if(cat == GIR_CAT_VEHICLE)
			{
				if(obj.getInfo(GII_TYPE) == POLICECAR_ID)
				{
					copsKicked++;
					if(AI_strategy == 3) resumePursuit(); //player unveiled himself, cops get back on mission
				}
			}
		}
	}

	public void handleEvent( GameRef obj_ref, int event, int param )
	{
		if( event == EVENT_TIME && !destroy )
		{
			if( param == 14 ) //ensure non-zero cop count in the first seconds of pursuit
			{
				forceInitialSquad();
			}
			else
			if( param == 13 ) //finish window destruction timer
			{
				if(fWindow.windowStatus != -1)
				{
					addTimer(1.0, 13); //destruction check
				}
				else unlaunch(); //exit from gamemode
			}
			else
			if( param == 12 ) //actions for player car when the 'race finished' window does appear
			{
				stopCars();
			}
			else
			if( param == 11 ) //race end window destruction timer (normal finish of race)
			{
				if(erWindow.windowStatus != -1)
				{
					erWindow.hide();
					addTimer(1.0, 11); //destruction check
				}
				else
				{
					//call fWindow again
					fWindow = new finishWindowHotActionCop(this);
					fWindow.show();
				}
			}
			else
			if( param == 10 ) //race end window destruction timer (disqualification)
			{
				if(erWindow.windowStatus != -1)
				{
					erWindow.hide();
					addTimer(1.0, 10); //repeat this check until erWindow will return a 'destruction-ready' state
				}
				else unlaunch(); //exit from gamemode
			}
			else
			if( param == 9 ) //disable initial "invulnerable" mode for player
			{
				safeStart = 0;
			}
			else
			if( param == 8 ) //re-enable voice for cops
			{
				copVoiceEnabled = 1;
			}
			else
			if( param == 7 ) //regenerate fake target, if there are no cops near the current one
			{
				if(AI_strategy == 3) //only for passive patrol mode
				{
					int gen;
					if(speedyCop) gen++;
					else
					{
						if(copCount == 0) gen++;
					}
					if(gen) generateFakeTarget();
					addTimer(Math.random()*30, 7);
				}
			}
			else
			if( param == 6 ) //reset delay for regenerating support scouts (not timeout!!)
			{
				if(supportDelay == 1) supportDelay = 0;
			}
			else
			if( param == 5 ) //reset timeout for support scout generator
			{
				switch(supportTimeout)
				{
					case 0:
						addTimer(Math.random()*75, 5); //police will regenerate support squad in ~75 seconds
						supportTimeout = 1;
						break;

					case 1:
						supportTimeout = -1;
						break;
				}
			}
			else
			if( param == 4 ) //a 3..2..1 event
			{
				if(gTrack) //immediate start, without countdown
				{
					prepareOSD(); //show OSD stuff
					gTrack.clearMsgBoxes();
					gTrack.showMsgBox("GET OUT OF COPS!", Track.MBOX_RED, Track.MBOX_MID );
					beginRace();
				}
			}
			else
			if( param == 3 ) //super.launch() initializes this
			{
				gmThread.execute(4);
			}
		}
	}

	public void run()
	{
		for(;;)
		{
			//pre-race actions
			if(aCamActive > 0)
			{
				animateCam(Gamemode.AC_TERMINATED, Gamemode.ACF_FADE_ON);

				if(aCamFadeReady && !fadeStage)
				{
					if(fader) fader.restartAnimation();
					fadeStage = 1;
					gmThread.execute(5); //fade-IN
				}

				//fade animation monitoring
				if(fader && fader.a_finished)
				{
					if(fadeStage == 1) //fade-IN is finished
					{
						gmThread.switchStatus(5); //regenerate THOR for fade-IN
						aCamChangeView(Gamemode.AC_TERMINATED);
						fadeStage = 2; //camera is ready to fade-OUT
					}

					if(fadeStage == 2) gmThread.execute(6); //fade-OUT

					if(fadeStage == 3) //fade-OUT is finished
					{
						//reset all params for next animation
						if(gmThread.methodStatus(6) == -1) gmThread.switchStatus(6); //regenerate THOR for fade-OUT, if needed
						fader.restartAnimation();
						aCamFadeReady = 0;
						fadeStage = 0;
					}
				}
			}

			if(aCamActive == -1)
			{
				if(gTrack)
				{
					gTrack.lockOSD = 0;
					gTrack.lockCam = 0;

					if(gTrack.cam)
					{
						gTrack.lastCamPos = new Vector3(0.0, 3.0, 10.0);
						if(GameLogic.player.car)
						{
							gTrack.lastCamPos.rotate(GameLogic.player.car.getOri());
							gTrack.lastCamPos.add(GameLogic.player.car.getPos() );
							gTrack.changeCamTarget(GameLogic.player.car);
						}

						gTrack.cam.setPos(gTrack.lastCamPos);
						gTrack.changeCamFollow();
					}

					if(gmcEvent.useCamAnimation) gTrack.osdCommand(gTrack.CMD_OSDONOFF);
				}

				if(gmcEvent.useCamAnimation)
				{
					createFade("white");
					fader.restartAnimation();
					createFade("white");
				}

				addTimer(1, 4); //run countdown timer
				aCamActive = 0;
			}

			//THOR methods (off-race)
			if(gmThread.methodStatus(4) == 1) //begin startline camera animation
			{
				if(gTrack)
				{
					gTrack.lockOSD = 1;
					gTrack.lockCam = 1;
				}
				if(!gmcEvent.useCamAnimation)
					aCamActive = -1;
				else
					aCamActive = 1;
				gmThread.controlMethod(4,-1);
			}

			if(gmThread.methodStatus(5) == 1) //fade-IN on changing camera view
			{
				createFade("white");
				gmThread.controlMethod(5,-1);
			}

			if(gmThread.methodStatus(6) == 1) //fade-OUT on changing camera view
			{
				if(fadeStage == 2)
				{
					createFade("white");
					gmThread.controlMethod(6,-1);
					fadeStage = 3;
				}
			}

			if(gmThread.methodStatus(1) == 1) //chase is finishing by the police capturing player
			{
				endRace();
				finishRace(4); //now show up race end message (busted)
				
				GameLogic.player.setSteamAchievement(Steam.ACHIEVEMENT_BUSTED);

				giveUp(); //mark career event as failed
				addTimer(4, 10); //4 sec timer for frWindow
				gmThread.controlMethod(1,-1);
			}

			if(gmThread.methodStatus(7) == 1) //finish chase, show up erWindow
			{
				endRace();
				finishRace(3); //show up race end message (chase finished)

				addTimer(2, 12); //2 sec timer for braking down; better to replace this with the follow spline
				addTimer(4, 11); //4 sec timer for fWindow
				gmThread.controlMethod(7,-1);
			}

			if(gmThread.methodStatus(8) == 1) //resume pursuit msgbox
			{
				gTrack.showMsgBox(rCaught.pick(), Track.MBOX_RED, Track.MBOX_LONG);
				gmThread.resetTHOR(9); //reset THOR for hideout msgbox
				gmThread.controlMethod(8,-1);
			}

			if(gmThread.methodStatus(9) == 1) //hideout msgbox
			{
				gTrack.showMsgBox(rHideout.pick(), Track.MBOX_GREEN, Track.MBOX_LONG);
				killBustedTimer(); //for safety
				gmThread.resetTHOR(8); //reset THOR for "resume pursuit" msgBox
				gmThread.controlMethod(9,-1);
			}

			if(raceActive)
			{
				updateTimers();
				if(!debugNoCops)
				{
					updateTailCount();
					updateAIStrategy();
					updateScouts();
					if(!safeStart) checkBusted();
				}

				chasetime_txt.changeText( "CHASE TIME: " + String.timeToString( raceTime[0], String.TCF_NOHOURS ) );
				if(speedyCop && speedyCop.bot) speedyCop.bot.brain.queueEvent(null, GameType.EVENT_COMMAND, "AI_horn 1"); //control siren on speedyCop's car
			}
			gmThread.sleep(10);
		}
	}

	//override method
	public void giveUp()
	{
		Gamemode.updatePoliceStats(0); //update police chase stats (busted++)
		super.giveUp();
	}

	public void endRace()
	{
		Sound.changeMusicSet(Sound.MUSIC_SET_NONE);
		clearScreen(); //hide all msgBoxes, OSD stuff

		//lock controls (doesn't work?)
		if(GameLogic.player && GameLogic.player.car)
		{
			GameLogic.player.controller.command( "controllable 0" );
			GameLogic.player.controller.activateState( 5, 1 );
		}

		raceActive = 0;
		killBustedTimer();
		stopTimer(0);
		stopScouts();
		stopCars(); //stop player car
		AI_strategy = -2;
	}

	public void finalize()
	{
		killBustedTimer();

		if(fWindow) fWindow.finalize();
		super.finalize();
	}

	public void cheatWin()
	{
		gmThread.execute(7);
	}
}

public class HAC_TrafficTracker
{
	int		id, trafficId;
	GameRef		car;
	Bot		bot;
	int		onMission;
	Marker		m; //for debug purposes
	public		HAC_TrafficTracker();
}

public class HAC_PoliceScout
{
	GroundRef	map;
	Bot		bot;
	GameRef		car;
	float		dropDistance; //if scout reaches this gap between himself and player, he does stop the pursuit
	HAC_TrafficTracker	tracker;

	public HAC_PoliceScout();
	public float getDistance()
	{
		return getDistance(GameLogic.player.car.getPos());
		return 0.0f;
	}

	public float getDistance(Vector3 target)
	{
		if(tracker && tracker.car)
		{
			float d = map.getRouteLength(tracker.car.getPos(), target);
			if(d>0) return d;
			else return tracker.car.getPos().distance(target);
		}
		return 0.0f;
	}
	
	//for eliminating extremely weak cops
	public int criticalState(float cDist)
	{
		if(tracker && tracker.car)
		{
			if(getDistance() < 0) return 1;
			if(tracker.car.getPos().distance(GameLogic.player.car.getPos()) >= cDist) return 1;
		}
		return 0;
	}
}

public class SupportScout
{
	GroundRef	map;
	Bot		bot;
	Vehicle		car;
	float		dropDistance;
	int		speedyCop; //special identity
	Marker		m; //for debug purposes

	public SupportScout();
	public float getDistance()
	{
		if(car && car.getPos())
		{
			float d  = map.getRouteLength(car.getPos(), GameLogic.player.car.getPos());
			if(d>0) return d;
			else return car.getPos().distance(GameLogic.player.car.getPos());
		}
		return 0.0f;
	}

	public int criticalState(float cDist)
	{
		if(car && car.getPos())
		{
			if(getDistance() < 0) return 1;
			if(car.getPos().distance(GameLogic.player.car.getPos()) >= cDist) return 1;
		}
		return 0;
	}
}

//individual window for showing up race results
public class finishWindowHotActionCop extends GameType
{
	Gamemode game; //to recieve external info from it
	Osd osd; //the osd of this window to let it have its own graphic layer

	//line separator XY
	float	xpos_ls = -0.950;
	float	ypos_ls = 0.2;

	//line text XY
	float	xpos = -0.875;
	float	ypos = -0.325;

	float	spacing = -0.25; //for text
	float	spacing_ls = 0.235; //for separator
	int  	maxlines = 2;

	Thread	fwThread;
	int	tMethods = 4; //amount of THOR methods

	Text	results_title_txt, prize_txt, pressenter_txt;
	Text[]	f_racers; //names and results for each finished racer

	//for animation; describes distances to travel in the animation sequence
	float	deltaDist_frame	= 2.0;
	float	deltaDist	= 1.0;

	int	windowStatus = 0;

	ResourceRef res_holder 	     = new ResourceRef(frontend:0xD142r); //holder_frame.png
	ResourceRef res_bckFlags     = new ResourceRef(frontend:0xD143r); //holder_flags_bck.png
	ResourceRef res_splitter     = new ResourceRef(frontend:0xD144r);
	ResourceRef res_separator    = new ResourceRef(frontend:0xC0A1r); //long separator fron EventList

	ResourceRef res_titles;
	int titles_resid	     = frontend:0xD172r; //title_escaped.png

	Rectangle   holder, bckFlags, splitter, titles;
	Rectangle[] separator;

	Vector finishedNames = new Vector(); //vector instead of array to force rank indices match the indices from elements of finishedNames
	Vector timesArranged;

	int[] finished;
	int finishPos; //like 1st/2nd/3rd etc.
	int highlight;

	SfxRef bckLoop;

	int debugMode = 0;

	public finishWindowHotActionCop(Gamemode gm)
	{
		game = gm;

		timesArranged = new Vector();

//--------------begin collect GM data

		if(GameLogic.player)
		{
			finishedNames.addElement(GameLogic.player.getName());
			timesArranged.addElement(new Float(game.raceTime[0]));
		}
		finishPos = 0; //player is the winner by default

//--------------GM data collected

		if(debugMode) traceDebug();

		osd = new Osd(1.0, 0.0, 20);
		osd.show();
		osd.createHotkey( Input.RCDIK_ENTER, Input.KEY|Osd.HK_STATIC, 0x01, this );
		f_racers = new Text[maxlines];
		separator = new Rectangle[maxlines];

		int checkWin;
		if(finishPos == 0) checkWin = 1;

		res_titles = new ResourceRef(titles_resid);

		holder = osd.createRectangle( 0.0-deltaDist_frame, 0.0, 2.0, 0.960, 1, res_holder, 0 );
		bckFlags = osd.createRectangle( 0.835, -0.045, 0.635, 0.425, 2, res_bckFlags, 0 );
		results_title_txt = osd.createText("CHASE RESULTS", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.475 );
		results_title_txt.changeColor(0x00000000);
		splitter = osd.createRectangle( 0.0-deltaDist, 0.345, 0.25, 0.0325, 2, res_splitter, 0 );
		titles = osd.createRectangle( 0.63+deltaDist, 0.020, 0.28, 0.120, 3, res_titles, 0 );

		if(checkWin == 1) //race won
		{
			prize_txt = osd.createText("", Frontend.largeFont_strong, Text.ALIGN_CENTER, 0.5, 0.375 );
			prize_txt.changeColor(0x00000000);
			prize_txt.changeText("YOUR PRIZE: " + game.gmcEvent.getPrizeName());
		}

		pressenter_txt = osd.createText("", Frontend.largeFont_strong, Text.ALIGN_CENTER, 0.0, 0.40 );
		pressenter_txt.changeColor(0x00000000);
		pressenter_txt.changeText("PRESS ENTER");

		//create infolines
		String[] prepareNames = new String[2];
		prepareNames[0] = "SUSPECT ESCAPED: " + GameLogic.player.getName();
		prepareNames[1] = "CHASE TIME: " + String.timeToString(game.raceTime[0], String.TCF_NOHOURS);
		
		highlight = 1; //line 0 is gonna be in a red color
		addSeparator(); //move down the infolines

		//now build all the lines using the data from prepareNames[]
		for(int k=0; k<prepareNames.length; k++)
		{
			if(prepareNames[k] && prepareNames[k].length())
			{
				createLine(k, prepareNames[k]);
				addSeparator();
			}
		}

		fwThread = new Thread( this, "GM finish window watcher thread", 1 ); //extended! thread-oriented methodology (THOR), no injections
		fwThread.setPriority(Thread.MAX_PRIORITY);
		fwThread.start();

		for(int i=0; i<(tMethods+1); i++)
		{
			fwThread.addMethod(i);
		}

		float vol = Sound.getVolume(Sound.CHANNEL_MUSIC);
		if(checkWin == 1)
			bckLoop = new SfxRef("frontend\\sounds\\gamemodes\\race_win.wav", vol, 15.4);
		else
			bckLoop = new SfxRef("frontend\\sounds\\gamemodes\\race_lose.wav", vol, 13.55);
		bckLoop.loopPlay();

		Gamemode.updatePoliceStats(1); //update police chase stats
		game.updateCareerStats(checkWin, finishPos); //update player's achievements in his career
		game.updateEventStatus(checkWin, finishPos); //update status of event for displaying in EventList (event failed/succeded or passed with an award)

		//finally give a prize to player for winning a race
		if(checkWin == 1) game.gmcEvent.aquirePrize();
		show();
	}

	public void createLine(int line, String data)
	{
		f_racers[line] = osd.createText(data, Frontend.largeFont_strong, Text.ALIGN_LEFT, xpos, ypos );
		separator[line] = osd.createRectangle( xpos_ls-deltaDist, ypos_ls, 0.395, 0.004, 2, res_separator, 0 );
		f_racers[line].changeColor(0x00000000);
	}

	public void addSeparator()
	{
		ypos-=spacing/2/osd.vpHeight;
		ypos_ls-=spacing_ls/2/osd.vpHeight;
	}

	public void show()
	{
		holder.setupAnimation(deltaDist_frame/20, 20, 1, "X"); //direction: right (1)
		holder.runThread(); //begin the animation

		splitter.setupAnimation(deltaDist/10, 10, 1, "X"); //direction: right (1)
		splitter.runThread();

		titles.setupAnimation(deltaDist/15, 15, -1, "X"); //direction: left (-1)
		titles.runThread();

		for(int i=0; i<separator.length; i++)
		{
			if(separator[i])
			{
				separator[i].setupAnimation(deltaDist/10, 10, 1, "X"); //direction: right (1)
				separator[i].runThread();
			}
		}

		windowStatus = 1; //to notify watcher thread
	}

	public void hide()
	{
		if(windowStatus == 3)
		{
		//------text
			for(int i=0; i<f_racers.length; i++)
			{
				if(f_racers[i])
				{
					f_racers[i].restartAnimation();
					f_racers[i].a_speed = 1;
					f_racers[i].fadeOut(0xF0000000); //from black to 100% transparent
				}
			}

			if(prize_txt)
			{
				prize_txt.restartAnimation();
				prize_txt.a_speed = 1;
				prize_txt.fadeOut(0xF0000000); //from black to 100% transparent
			}
			pressenter_txt.restartAnimation();
			pressenter_txt.a_speed = 1;
			pressenter_txt.fadeOut(0xF0FF0000); //from red to 100% transparent
			results_title_txt.restartAnimation();
			results_title_txt.a_speed = 1;
			results_title_txt.fadeOut(); //from white to 100% transparent

		//------rectangles
			holder.restartAnimation("X");
			holder.setupAnimation(deltaDist_frame/2, 10, -1, "X"); //direction: left, opposite (-1)
			holder.runThread();

			splitter.restartAnimation("X");
			splitter.setupAnimation(deltaDist/2, 10, -1, "X"); //direction: left, opposite (-1)
			splitter.runThread();

			titles.restartAnimation("X");
			titles.setupAnimation(deltaDist/15, 15, 1, "X"); //direction: right, opposite (1)
			titles.runThread();

			for(int i=0; i<separator.length; i++)
			{
				if(separator[i])
				{
					separator[i].restartAnimation("X");
					separator[i].setupAnimation(deltaDist/2, 10, -1, "X"); //direction: left, opposite (-1)
					separator[i].runThread();
				}
			}

			if(bckFlags) bckFlags.finalize();
		//------
			bckLoop.loopStop();
			bckLoop=null;
			game.addTimer(1.0, 13); //to exit from gamemode
			windowStatus = 4;
		}
	}

	public static void osdCommand(int cmd)
	{
		if(cmd == 0x01) hide();
	}

	public void traceDebug()
	{
		System.log("-------begin GM debug session-------");
		System.log("");
		System.log("Hot Action Cop: no debug data provided");
		System.log("");
		System.log("-------end of GM debug-------");
	}

	public void finalize()
	{
		fwThread.stop();

		holder.finalize();
		bckFlags.finalize();
		splitter.finalize();
		titles.finalize();
		for(int i=0; i<separator.length; i++) if(separator[i]) separator[i].finalize();
	}

	public void run()
	{
		for(;;)
		{
			switch(windowStatus)
			{
				case 1: //if rectangle animation is finished after show()
					if(holder.a_finished_x) fwThread.execute(0);
					break;

				case 2:
					if(results_title_txt.a_finished) fwThread.execute(1); //if text animation is finished after show()
					break;

				case 3:
					if(pressenter_txt.a_finished) //pulse fade animation for 'press enter' text when the window is shown
					{
						if(fwThread.methodStatus(3) == -1) fwThread.switchStatus(3); //reset THOR for this method
						fwThread.execute(3);
					}
					break;

				case 4:
					if(holder.a_finished_x) fwThread.execute(2); //if hiding animation is complete
					break;
			}

			//THOR methods
			if(fwThread.methodStatus(0) == 1) //show up text
			{
				for(int i=0; i<f_racers.length; i++)
				{
					if(f_racers[i])
					{
						if(i != highlight) f_racers[i].fadeIn(0xFF000000); //to solid black
						else f_racers[i].fadeIn(0xFFFF0000); //to solid red
					}
				}
				if(prize_txt) prize_txt.fadeIn(0xFF000000); //to solid black
				pressenter_txt.fadeIn(0xFFFF0000); //to solid red
				results_title_txt.fadeIn(); //to solid white
				windowStatus = 2;

				fwThread.controlMethod(0,-1);
			}

			if(fwThread.methodStatus(1) == 1) //notify thread that animation for show() is completely done
			{
				windowStatus = 3;

				fwThread.controlMethod(1,-1);
			}

			if(fwThread.methodStatus(2) == 1) //notify external class that the window is ready to be destroyed
			{
				windowStatus = -1;
				osd.hide();

				fwThread.controlMethod(2,-1);
			}

			if(fwThread.methodStatus(3) == 1) //restart animation for 'press enter' text
			{
				if(pressenter_txt.a_dir == 1)
					pressenter_txt.fadeOut(0xF0FF0000); //to transparent
				else
					pressenter_txt.fadeIn(0xFFFF0000); //to solid red
				pressenter_txt.restartAnimation();

				fwThread.controlMethod(3,-1);
			}

			fwThread.sleep(10);
		}
	}
}