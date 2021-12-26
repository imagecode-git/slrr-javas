package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.sound.*;

public class DestructionDerby extends Gamemode
{
	Rectangle timerBar, timerPin;
	ResourceRef res_timerBar, res_timerPin;

	Text racetime_txt, carsleft_txt;

	Vector3 mapCenter;

	int carsLeft;
	int AI_tactic = -1;
	int target = -1; //current idx of AI target (from targetList)
	int[] eliminated, eliminatedOld, targetList;
	Timer[] actTimer; //activity timer for each racer

	float inactivityLimit = 30.0f; //30 seconds inactivity period by default

	finishWindowDestructionDerby fWindow;

	public DestructionDerby(){}

	public void init()
	{
		name = "Destruction Derby";
		shieldIcon = new ResourceRef(sl.Scripts.game.Gamemodes.stdpack_231:0x1005r);
	}

	public void launch()
	{
		useTimers = 1;
		handleCollisions = 1;
		res_timerBar = new ResourceRef(frontend:0x0000D16Cr); //timer_bar.png
		res_timerPin = new ResourceRef(frontend:0x0000D16Dr); //timer_pin.png
		res_riSplitter = new ResourceRef(frontend:0xD125r);

		getOSD(); //get instance of OSD from Track()

		if(gmcEvent.racers && gmcEvent.botData.size()) //find opponents
		{
			gBot = new Bot[gmcEvent.racers];
			for(int i=0; i<gmcEvent.racers; i++) gBot[i] = new Bot(gmcEvent.botData.elementAt(i).intValue()); //pick bots from the database
		}

		if(gBot && gBot.length)
		{
			createBotCars(); //prepare cars, track will find them and place on the start grid
			joinBotCars(); //connect bots with the cars
			carsLeft = gBot.length;
		}

		if(gmcEvent && gmcEvent.track)
		{
			mapCenter = gmcEvent.track.checkpoints.elementAt(0); //center place on the derby map
			if(gmcEvent.specialSlots[0]) inactivityLimit = gmcEvent.specialSlots[0];
		}

		eliminatedOld = new int[calcArray()];
		eliminated = new int[calcArray()];
		targetList = new int[calcArray()];
		actTimer = new Timer[calcArray()];

		//generate list of AI targets
		if(gBot)
		{
			for(int j=0; j<calcArray(); j++)
			{
				if(j==0)
				{
					if(GameLogic.player.car) targetList[j] = GameLogic.player.car.id();
				}
				else
				{
					if(gBot[j-1].car) targetList[j] = gBot[j-1].car.id();
				}
			}
		}

		gmThread = new Thread( this, "destruction derby GM animation thread", 1 ); //extended! thread-oriented methodology (THOR), no injections
		gmThread.setPriority(Thread.MAX_PRIORITY);
		gmThread.start();

		tMethods = 8; //amount of THOR methods

		for(int i=0; i<(tMethods+1); i++)
		{
			gmThread.addMethod(i);
		}

		//camera animation stuff
		if(gmcEvent && gmcEvent.useCamAnimation) aCamPathCreate();

		//override GPS frame created by Track()
		if(GameLogic.player)
		{
			GameLogic.player.gpsState = 0;
			GameLogic.player.handleGPSframe(GameLogic.player.gpsState);
		}

		lockPlayerCar(); //patch to prevent engine start on player's car before the 3..2..1..GO countdown

		super.launch();
	}

	public void prepareOSD()
	{
		if(osd)
		{
			timerBar = osd.createRectangle( -2.665, -0.9, 0.28, 0.65, 0, res_timerBar, 0 );
			timerPin = osd.createRectangle( -2.665, -1.02, 0.27, 0.45, 0, res_timerPin, 0 );

			timerBar.runAnimation(0.05, 10, 1, "X"); //direction: right (1)
			timerPin.runAnimation(0.05, 10, 1, "X"); //direction: right (1)

			riSplitter = osd.createRectangle( 2.67, 0.91, 0.32, 0.036, 1, res_riSplitter, 0 );
			riSplitter.runAnimation(0.1, 10, -1, "X"); //direction: left (-1)

			racetime_txt = osd.createText( "RACE TIME: " + String.timeToString( 0.0f, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.92 );
			racetime_txt.changeColor(0x00000000);
			racetime_txt.setupAnimation(0x0FFFFFFF, 8, 1);	racetime_txt.runThread();

			carsleft_txt = osd.createText( "VEHICLES LEFT: " + carsLeft, Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.835 );
			carsleft_txt.changeColor(0x00000000);
			carsleft_txt.fadeIn();

			osd.show();
		}
	}

	public void hideOSD()
	{
		timerBar.runAnimation(0.05, 15, -1, "X"); //direction: opposite, left (-1)
		timerPin.runAnimation(0.05, 15, -1, "X"); //direction: opposite, left (-1)

		riSplitter.restartAnimation("X");
		riSplitter.runAnimation(0.1, 10, 1, "X"); //direction: opposite, right (1)

		racetime_txt.restartAnimation();
		racetime_txt.fadeOut();

		carsleft_txt.restartAnimation();
		carsleft_txt.fadeOut();
	}

	public void clearScreen()
	{
		gTrack.clearMsgBoxes();
		gTrack.lockMsgBoxes();
		gTrack.enableOsd(0);
		hideOSD();
	}

	//regular check of chassis and engine condition
	public void checkCondition()
	{
		Vehicle check;

		for(int i=0; i<calcArray(); i++)
		{
			if(!eliminated[i])
			{
				int dead;
				
				if(i==0)
				{
					if(GameLogic.player && GameLogic.player.car)
					{
						check = GameLogic.player.car;
						if(check.chassis.getDamage() == 0) dead++; //chassis is totally damaged
					}
				}
				else
				{
					if(gBot && gBot.length && gBot[i-1].car) check = gBot[i-1].car;
				}

				if(check.chassis.partOnSlot(401))
				{
					if(check.chassis.partOnSlot(401).getInfo(GII_DAMAGE) == 0) dead++; //engine is in bad condition or not working
				}
				else dead++; //engine is lost, destroyed or not installed

				if(dead) eliminateRacer(i);
			}
		}
	}

	public void checkInactivity()
	{
		if(calcArray() > 1)
		{
			for(int i=0; i<actTimer.length; i++)
			{
				if(actTimer[i].stopped) eliminateRacer(i);
			}
		}
	}

	public void eliminateRacer(int idx)
	{
		if(!eliminated[idx])
		{
			eliminated[idx]++;
			carsLeft--;

			if(idx == 0) gmThread.execute(1); //disqualify player
			else
			{
				if(carsLeft == 0) gmThread.execute(7); //show up finish window
				else
				{
					gTrack.showMsgBox(gBot[idx-1].profile.name_f + " " + gBot[idx-1].profile.name_s + " IS ELIMINATED!", Track.MBOX_YELLOW, Track.MBOX_LONG);
					carsleft_txt.changeText("VEHICLES LEFT: " + carsLeft);
				}
			}

		}
	}

	public void findMarginals(int param)
	{
		Vehicle check;
		Vector damageList = new Vector();

		if(param > 0) param = Rank.MAXMIN; //higher values have better rating
		else param = Rank.MINMAX; //lowest values have better rating

		//first of all, collect information about engine damage for all alive racers
		for(int i=0; i<calcArray(); i++)
		{
			if(!eliminated[i])
			{
				if(i==0)
				{
					if(GameLogic.player && GameLogic.player.car) check = GameLogic.player.car;
				}
				else
				{
					if(gBot && gBot.length && gBot[i-1].car) check = gBot[i-1].car;
				}

				int engineDamage = 0;
				if(check.chassis.partOnSlot(401)) engineDamage = check.chassis.partOnSlot(401).getWear()*100;	
				damageList.addElement(new Integer(engineDamage));
			}
		}

		//now arrange collected information
		rankings = new Rank();
		Vector sortedDamage = rankings.getRanks(damageList, param);
		target = sortedDamage.elementAt(0).number; //marginal found, now assign the target
	}

	//OSD timer pictogram refresher
	public void updateCountdown()
	{
		if(!Integrator.frozen && !Integrator.IngameMenuActive)
		{
			float tick = 360/inactivityLimit;
			float angle = Math.deg2rad(actTimer[0].time*tick);
			timerPin.setOri(new Ypr(0, 0, angle));
		}
	}

	public void beginRace() //here the race actually begins
	{
		unlockCars();
		startTimers();

		setEventMask( EVENT_CURSOR|EVENT_COMMAND|EVENT_HOTKEY|EVENT_TRIGGER_ON|EVENT_TRIGGER_OFF|EVENT_TIME|EVENT_COLLISION );
		for(int i=0; i<calcArray(); i++)
		{
			Vehicle vhc;

			if(i==0) vhc = GameLogic.player.car;
			else vhc = gBot[i-1].car;

			addNotification(vhc, EVENT_COLLISION, EVENT_SAME, null, "handleCollision"); //all vehicles will handle the same collision event
		}

		for(int i=0; i<actTimer.length; i++)
		{
			actTimer[i] = new Timer(inactivityLimit);
			actTimer[i].start();
		}

		if(gBot) AI_chooseDestiny(); //engage the AI

		raceActive = 1;
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	public void AI_chooseDestiny() //instant change of AI tactics
	{
		addTimer(0.1, 5);
	}

	public void AI_killMarginals() //attack the strongest or the weakest racers
	{
		int param = Math.siRandom();
		findMarginals(param); //scan damage and pick new target
	}

	public void AI_scanTarget() //switch between racers and attempt to attack them
	{
		Vector aliveTargets = new Vector();
		int idx = -1;

		for(int i=0; i<eliminated.length; i++)
		{
			if(!eliminated[i]) //check who is not eliminated and add them to the list
			{
				if(i != 0) //exclude player
				{
					aliveTargets.addElement(new Integer(i));
					if(target == i) idx = aliveTargets.size()-1; //memorize current target
				}
			}
		}

		if(idx < 0) idx = Math.random()*aliveTargets.size()-1; //pick random target if current one is player or it's eliminated

		float rand = Math.random();
		int delta = -1;
		if(rand > 0.5) delta *= (-1);
		
		idx += delta; //pick next or previous alive target
		if(idx >= aliveTargets.size()) idx = aliveTargets.size()-1;
		if(idx < 0) idx = 0;		

		target = aliveTargets.elementAt(idx).number; //finally, assign the target
	}

	public void AI_killPlayer() //engage all AI power against the player
	{
		target = 0; //player's idx in target list
	}

	public void AI_safeMode() //drive to a more safe area
	{
		AI_tactic = 4;
		addTimer(0.1, 7); //check who is where
	}

	public void AI_attackTarget()
	{
		if(AI_tactic >= 0 && target >= 0 && raceActive)
		{
			for(int i=0; i<gBot.length; i++)
			{
				Vector3 pos;

				if(AI_tactic != 4)
				{
					if(eliminated[target]) //target is killed, turn into safe mode immediately
					{
						AI_safeMode();
						break;
					}
					else
					{
						//targeted bot does attack player, while others will attack _that_ bot
						if(target != i) pos = getTargetPos(targetList[target]);
						else pos = getTargetPos(targetList[0]);
					}
				}
				else pos = mapCenter;
				if(pos)
				{
					if(!eliminated[i+1]) gBot[i].driveStraightTo(new Vector3(pos.x+Math.random()*2, pos.y, pos.z+Math.random()*3)); //active bots continue race
					else gBot[i].beStupid(); //eliminated do nothing and just wait until the race will end
				}
			}
		}

		addTimer(1.0, 9); //auto-update info about all AI targets
	}

	//idx in eliminated[] for target vehicle
	public int getTargetIdx(int id)
	{
		if(id == GameLogic.player.car.id()) return 0;
		else
		{
			if(gBot && gBot.length)
			{
				for(int i=0; i<gBot.length; i++)
				{
					if(id == gBot[i].car.id()) return i+1;
				}
			}
		}

		return -1;
	}

	//idx in eliminated[] for target vehicle
	public float getTargetSpeed(int id)
	{
		return getTargetVehicle(id).getSpeed();
		return 0.0f;
	}

	public Vector3 getTargetPos(int id)
	{
		return getTargetVehicle(id).getPos();
		return null;
	}

	public Vehicle getTargetVehicle(int id)
	{
		Vehicle check;

		if(id == GameLogic.player.car.id()) check = GameLogic.player.car;
		else
		{
			if(gBot && gBot.length)
			{
				for(int i=0; i<gBot.length; i++)
				{
					if(id == gBot[i].car.id()) check = gBot[i].car;
				}
			}
		}

		return check;
		return null;
	}

	public void stopActTimers()
	{
		for(int i=0; i<actTimer.length; i++)
		{
			actTimer[i].stop();
			actTimer[i].finalize();
		}
	}

	public void handleCollision( GameRef obj_ref, int event, String param )
	{
		if(!destroy && raceActive)
		{
			int colliderID = param.token(0).intValue();
			GameRef obj = new GameRef(colliderID);
			
			int cat = obj.getInfo(GII_CATEGORY);
			int carID = obj_ref.id(); //id of vehicle that is hitting the collider

			if(cat == GIR_CAT_VEHICLE)
			{
				int carIdx = getTargetIdx(carID);
				int colliderIdx = getTargetIdx(colliderID);
				
				if(carIdx >= 0 && colliderIdx >= 0)
				{
					if(!eliminated[carIdx] && getTargetSpeed(carID) >= 5.5 && actTimer[carIdx]) //racer is not eliminated and his car does drive faster than 20KPH
					{
						if(!eliminated[colliderIdx])
						{
							//now reset activity timer
							actTimer[carIdx].reset();
							actTimer[carIdx].start();
						}
					}
				}
			}
		}
	}

	public void handleEvent( GameRef obj_ref, int event, int param )
	{
		if( event == EVENT_TIME && !destroy )
		{
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
					fWindow = new finishWindowDestructionDerby(this);
					fWindow.show();
				}
			}
			else
			if( param == 10 ) //race end window destruction timer (player's disqualification)
			{
				if(erWindow.windowStatus != -1)
				{
					erWindow.hide();
					addTimer(1.0, 10); //repeat this check until erWindow will return a 'destruction-ready' state
				}
				else unlaunch(); //exit from gamemode
			}
			else
			if( param == 9 ) //repeat attacks on the target vehicle
			{
				AI_attackTarget();
			}
			else
			if( param == 7 ) //check distance to the center of map in safe mode
			{
				for(int i=0; i<gBot.length; i++)
				{
					if(gBot[i].car)
					{
						if(gBot[i].car.getPos().distance(mapCenter) <= 5)
						{
							AI_tactic = -1;
							AI_chooseDestiny(); //some bot reached center of map, now AI can pick new tactics
						}
					}
				}

				if(AI_tactic == 4) addTimer(0.5, 7); //self-check every 0.5sec
			}
			else
			if( param == 6 ) //prepare changing tactics
			{
				AI_safeMode();
			}
			else
			if( param == 5 ) //pick target for AI
			{
				if(raceActive)
				{
					int firstRun;
					if(AI_tactic < 0) firstRun++;

					if(carsLeft > 1) AI_tactic = Math.chaos()*2.4; //using "ultra random"
					else AI_tactic = 1;

					switch(AI_tactic)
					{
						case 0:
							AI_scanTarget();
							break;
						case 1:
							AI_killPlayer();
							break;
						case 2:
							AI_killMarginals();
							break;
					}

					if(firstRun) AI_attackTarget(); //initiate first attack
					if(AI_tactic >= 0 && AI_tactic != 4) addTimer(15+Math.random()*5, 6); //run safe mode each 15~20 seconds
				}
			}
			else
			if( param == 4 ) //a 3..2..1 event
			{
				if(gTrack)
				{
					if( countDown )
					{
						if( countDown == 3 )
						{
							lockCars();
							prepareOSD(); //show OSD stuff

							new SfxRef(Gamemode.SFX_HORN_1).play();
							addTimer(1, 4);
						}
						else 
						if( countDown == 2 )
						{
							new SfxRef(Gamemode.SFX_HORN_1).play();
							addTimer(1, 4);
						}
						else 
						if( countDown == 1 )
						{
							if(gBot) prepareBots(1); //rev up
							new SfxRef(Gamemode.SFX_HORN_1).play();
							addTimer(1, 4);
						}
						gTrack.showMsgBox(countDown, Track.MBOX_RED, Track.MBOX_SHORT );
						countDown--;
					}
					else
					{
						new SfxRef(Gamemode.SFX_HORN_2).play();
						gTrack.clearMsgBoxes();
						gTrack.showMsgBox("GO!", Track.MBOX_GREEN, Track.MBOX_SHORT );
						beginRace();
					}
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

				hideGPSFrame();
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

			if(gmThread.methodStatus(1) == 1) //player's disqualification
			{
				Sound.changeMusicSet(Sound.MUSIC_SET_NONE);
				clearScreen(); //hide all msgBoxes, OSD stuff

				raceActive = 0;
				stopTimer(0);
				stopActTimers();
				stopCars();
				AI_tactic = -1;
				finishRace(1); //now show up race end message (race failed)
				giveUp(); //mark career event as failed
				addTimer(4, 10); //4 sec timer for frWindow
				gmThread.controlMethod(1,-1);
			}

			if(gmThread.methodStatus(7) == 1) //finish race, show up erWindow
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
				stopTimer(0);
				stopActTimers();
				AI_tactic = -1;
				finishRace(0); //show up race end message (race finished)
				addTimer(2, 12); //2 sec timer for braking down; better to replace this with the follow spline
				addTimer(4, 11); //4 sec timer for fWindow
				gmThread.controlMethod(7,-1);
			}

			if(raceActive)
			{
				updateTimers();
				updateCountdown();
				checkCondition();
				checkInactivity();
				racetime_txt.changeText( "RACE TIME: " + String.timeToString( raceTime[0], String.TCF_NOHOURS ) );

			}
			gmThread.sleep(10);
		}
	}

	public void finalize()
	{
		if(fWindow) fWindow.finalize();
		super.finalize();
	}

	public void cheatWin()
	{
		raceActive = 0;
		stopTimer(0);
		stopActTimers();
		AI_tactic = -1;
		clearScreen(); //hide all msgBoxes, OSD stuff
		finishRace(0); //show up race end message (race finished)
		addTimer(1, 12);
		addTimer(2, 11);
	}
}

public class finishWindowDestructionDerby extends GameType
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
	Text[]	f_text; //race time, vehicles destroyed

	//for animation; describes distances to travel in the animation sequence
	float	deltaDist_frame	= 2.0;
	float	deltaDist	= 1.0;

	int	windowStatus = 0;

	ResourceRef res_holder 	     = new ResourceRef(frontend:0xD142r); //holder_frame.png
	ResourceRef res_bckFlags     = new ResourceRef(frontend:0xD143r); //holder_flags_bck.png
	ResourceRef res_splitter     = new ResourceRef(frontend:0xD144r);
	ResourceRef res_separator    = new ResourceRef(frontend:0xC0A1r); //long separator fron EventList
	ResourceRef res_titles;

	int titles_resid	     = frontend:0xD145r; //title_lose.png

	Rectangle   holder, bckFlags, splitter, titles;
	Rectangle[] separator;

	int vDestroyed;
	int finishPos; //like 1st/2nd/3rd etc.
	int highlight;

	SfxRef bckLoop;

	int debugMode = 0;

	public finishWindowDestructionDerby(Gamemode gm)
	{
		game = gm;

//--------------begin collect GM data

		if(game.gBot && game.gBot.length) vDestroyed = game.gBot.length; //how much bots were killed (if gamemode did use them)
		finishPos = 0; //player is the winner by default, since he survived in derby

//--------------GM data collected

		if(debugMode) traceDebug();

		osd = new Osd(1.0, 0.0, 20);
		osd.show();
		osd.createHotkey( Input.RCDIK_ENTER, Input.KEY|Osd.HK_STATIC, 0x01, this );
		f_text = new Text[maxlines];
		separator = new Rectangle[maxlines];

		int checkWin;
		if(finishPos == 0) checkWin = 1;
		res_titles = new ResourceRef(titles_resid+checkWin);

		holder = osd.createRectangle( 0.0-deltaDist_frame, 0.0, 2.0, 0.960, 1, res_holder, 0 );
		bckFlags = osd.createRectangle( 0.835, -0.045, 0.635, 0.425, 2, res_bckFlags, 0 );
		results_title_txt = osd.createText("RACE RESULTS", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.475 );
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
		prepareNames[0] = "RACE TIME: " + String.timeToString(game.raceTime[0], String.TCF_NOHOURS);
		prepareNames[1] = "VEHICLES DESTROYED: " + vDestroyed;
		
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

		for(int i=0; i<(tMethods+1); i++) fwThread.addMethod(i);

		float vol = Sound.getVolume(Sound.CHANNEL_MUSIC);
		if(checkWin == 1)
			bckLoop = new SfxRef("frontend\\sounds\\gamemodes\\race_win.wav", vol, 15.4);
		else
			bckLoop = new SfxRef("frontend\\sounds\\gamemodes\\race_lose.wav", vol, 13.55);
		bckLoop.loopPlay();

		game.updateCareerStats(checkWin, finishPos); //update player's achievements in his career
		game.updateEventStatus(checkWin, finishPos); //update status of event for displaying in EventList (event failed/succeded or passed with an award)

		//finally give a prize to player for winning a race
		if(checkWin == 1) game.gmcEvent.aquirePrize();
		show();
	}

	public void createLine(int line, String data)
	{
		f_text[line] = osd.createText(data, Frontend.largeFont_strong, Text.ALIGN_LEFT, xpos, ypos );
		separator[line] = osd.createRectangle( xpos_ls-deltaDist, ypos_ls, 0.395, 0.004, 2, res_separator, 0 );
		f_text[line].changeColor(0x00000000);
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
			for(int i=0; i<f_text.length; i++)
			{
				if(f_text[i])
				{
					f_text[i].restartAnimation();
					f_text[i].a_speed = 1;
					f_text[i].fadeOut(0xF0000000); //from black to 100% transparent
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
		System.log("Destruction Derby: no debug data provided");
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
				for(int i=0; i<f_text.length; i++)
				{
					if(f_text[i])
					{
						if(i != highlight) f_text[i].fadeIn(0xFF000000); //to solid black
						else f_text[i].fadeIn(0xFFFF0000); //to solid red
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