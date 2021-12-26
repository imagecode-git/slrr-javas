package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.sound.*;

public class TimeAttack extends Gamemode
{
	int laps, current_lap, flags;
	float targetTime;
	float[] targetTimes; //if awards are being used

	Text lap_txt, besttime_txt, laptime_txt, racetime_txt, targettime_txt;

	finishWindowTimeAttack fWindow;

	public TimeAttack() {}

	public void init()
	{
		name = "Time Attack";
		shieldIcon = new ResourceRef(sl.Scripts.game.Gamemodes.stdpack_231:0x1001r);
	}

	public void launch()
	{
		useTimers = 1;

		super.init(); //setup fines stuff

		if(gmcEvent && gmcEvent.useFines && res_fineFlags)
		{
			//active						//disabled
			res_fineFlags[0] = new ResourceRef(frontend:0xD126r);	res_fineFlags[3] = new ResourceRef(frontend:0xD139r); //fine_penalty
			res_fineFlags[2] = new ResourceRef(frontend:0xD128r);	res_fineFlags[5] = new ResourceRef(frontend:0xD13Br); //fine_runout
		}

		res_riSplitter = new ResourceRef(frontend:0xD125r);

		getOSD(); //get instance of OSD from Track()
		if(gmcEvent)
		{
			laps = gmcEvent.specialSlots[0];
			targetTime = gmcEvent.specialSlots[1];
			
			if(gmcEvent.useAwards)
			{
				//this is needed to call fWindow
				targetTimes = new float[3];
				for(int i=0; i<targetTimes.length; i++) targetTimes[i] = gmcEvent.specialSlots[i+1];
			}
			else
			{
				targetTimes = new float[1];
				targetTimes[0] = targetTime;
			}
		}

		current_lap = 1;
		applyTriggers();

		gmThread = new Thread( this, "circuit racing GM animation thread", 1 ); //extended! thread-oriented methodology (THOR), no injections
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
		Navigator nav = gmcEvent.track.nav;
		if(!nav || (nav && nav.type>Navigator.TYPE_CLASSIC))
		{
			if(GameLogic.player)
			{
				GameLogic.player.gpsState = 0;
				GameLogic.player.handleGPSframe(GameLogic.player.gpsState);
			}
		}
		
		lockPlayerCar(); //patch to prevent engine start on player's car before the 3..2..1..GO countdown

		super.launch();
	}

	public void prepareOSD()
	{
		flags = 0;

		if(osd)
		{
			if(gmcEvent.useFines && GameLogic.player.car && gmcEvent.cleanDriving)
			{
				//player.car _must_ exist before calling this!
				fineFlags[0] = osd.createRectangle( 2.68, 0.45, 0.121, 0.216, 1, res_fineFlags[0+3], 0 ); //fine_penalty
				fineFlags[1] = osd.createRectangle( 2.95, 0.45, 0.121, 0.216, 1, res_fineFlags[2+3], 0 ); //fine_runout
				flags=2;
				
				for(int i=0; i<flags; i++)
				{
					fineFlags[i].setupAnimation(0.1, 10, -1, "X"); //direction: left (-1)
					fineFlags[i].runThread(); //begin the animation
				}
			}

			riSplitter = osd.createRectangle( 2.67, 0.61, 0.32, 0.036, 1, res_riSplitter, 0 );
			riSplitter.setupAnimation(0.1, 10, -1, "X"); //direction: left (-1)
			riSplitter.runThread(); //begin the animation

			lap_txt = osd.createText( "LAP: " + current_lap + "/" + laps, Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.92 );
			targettime_txt = osd.createText( "TARGET TIME: " + String.timeToString( targetTime, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.835 );

			racetime_txt = osd.createText( "RACE TIME: " + String.timeToString( 0.0f, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.75 );
			laptime_txt = osd.createText( "LAP TIME: " + String.timeToString( 0.0f, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.665 );

			bestTime = GameLogic.player.getTrackData(gmcEvent.track.name);
			besttime_txt = osd.createText( "BEST LAP: " + String.timeToString( bestTime, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.603, -0.580 );

			lap_txt.changeColor(0x00000000);
			besttime_txt.changeColor(0x00000000);
			laptime_txt.changeColor(0x00000000);
			racetime_txt.changeColor(0x00000000);
			targettime_txt.changeColor(0x00000000);

			lap_txt.fadeIn();
			targettime_txt.fadeIn();
			besttime_txt.setupAnimation(0x0FFFFFFF, 8, 1);	besttime_txt.runThread();
			laptime_txt.setupAnimation(0x0FFFFFFF, 8, 1);	laptime_txt.runThread();
			racetime_txt.setupAnimation(0x0FFFFFFF, 8, 1);	racetime_txt.runThread();

			osd.show();
		}
	}

	public void hideOSD()
	{
		if(fineFlags)
		{
			for(int i=0; i<flags; i++)
			{
				fineFlags[i].restartAnimation("X");
				fineFlags[i].setupAnimation(0.1, 10, 1, "X"); //direction: opposite, right (1)
				fineFlags[i].runThread(); //begin the animation
			}
		}

		riSplitter.restartAnimation("X");
		riSplitter.setupAnimation(0.1, 10, 1, "X"); //direction: opposite, right (1)
		riSplitter.runThread(); //begin the animation

		lap_txt.restartAnimation();
		lap_txt.fadeOut();

		besttime_txt.restartAnimation();
		besttime_txt.fadeOut();

		laptime_txt.restartAnimation();
		laptime_txt.fadeOut();

		racetime_txt.restartAnimation();
		racetime_txt.fadeOut();

		targettime_txt.restartAnimation();
		targettime_txt.fadeOut();
	}

	public void clearScreen()
	{
		gTrack.clearMsgBoxes();
		gTrack.lockMsgBoxes();
		gTrack.enableOsd(0);
		hideOSD();
	}

	public void applyTriggers()
	{
		setupCheckpoints(); //step 1: scan for v3's in track data and create triggers from them

		if(cpTrigger) //step 2: make notifications from the created trigger instances
		{
			for(int j=0; j<cpTrigger.length; j++)
			{
			        addNotification( cpTrigger[j].trigger, EVENT_TRIGGER_ON, EVENT_SAME, null, "handleCheckpoint" );
			}
		}
	}

	public void beginRace() //here the race actually begins
	{
		unlockCars();

		startTimers();
		raceActive = 1;
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
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
					fWindow = new finishWindowTimeAttack(this, targetTimes);
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
			if( param == 8 ) //update flag: runout
			{
				if(bTimer[2] < 7)
				{
					updateFines(2, 1);
					addTimer(0.5, 8);
				}
				else	fixFineStatus(2, 1);
			}
			else
			if( param == 6 ) //update flag: penalty
			{
				if(bTimer[0] < 7)
				{
					updateFines(0, 0);
					addTimer(0.5, 6);
				}
				else	fixFineStatus(0, 0);
			}
			else
			if( param == 5 ) //offroad timer
			{
				gmThread.switchStatus(0); //regenerate THOR
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

	public void handleCheckpoint( GameRef obj_ref, int event, String param )
	{
		int id = param.token(0).intValue();

		if( id == GameLogic.player.car.id() ) //player checkpoint handling
		{
			if(cpTrigger && cpTrigger[nextTrigger[0]].trigger.id() == obj_ref.id())
			{
				if(nextTrigger[0] < cpTrigger.length-1)
				{
					gTrack.showMsgBox("CHECKPOINT: " + String.timeToString(time[0], String.TCF_NOHOURS), Track.MBOX_GREEN, Track.MBOX_MID );
					nextTrigger[0]++;
				}
				else
				{
					if((current_lap+1) <= gmcEvent.specialSlots[0]) //slot 0: laps count
					{
						gTrack.showMsgBox("LAP " + current_lap + ": " + String.timeToString(time[0], String.TCF_NOHOURS), Track.MBOX_GREEN, Track.MBOX_SHORT );
						current_lap++;
						if(time[0] < bestTime || !bestTime)
						{
							bestTime = time[0];
							GameLogic.player.setTrackData(gmcEvent.track.name, bestTime);
							String bestLapTime = String.timeToString( GameLogic.player.getTrackData(gmcEvent.track.name), String.TCF_NOHOURS );

							besttime_txt.changeText("BEST LAP: " + bestLapTime);
							gTrack.showMsgBox("NEW BEST LAP TIME: " + bestLapTime, Track.MBOX_YELLOW, Track.MBOX_MID );
						}
						lap_txt.changeText("LAP: " + current_lap + "/" + laps);
						time[0] = 0.0f;
						startTimer(0);
						nextTrigger[0] = 0;
					}
					else //player ends race
					{
						if(time[0] < bestTime || !bestTime)
						{
							bestTime = time[0];
							GameLogic.player.setTrackData(gmcEvent.track.name, bestTime);
							String bestLapTime = String.timeToString( GameLogic.player.getTrackData(gmcEvent.track.name), String.TCF_NOHOURS );
							besttime_txt.changeText("BEST LAP: " + bestLapTime);
						}
						stopTimer(0);
						nextTrigger[0] = 0;
						gmThread.execute(7); //show up finish window
					}
				}
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

				showMinimap();
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

			if(raceActive)
			{
				updateTimers();
				laptime_txt.changeText( "LAP TIME: " + String.timeToString( time[0], String.TCF_NOHOURS ) );
				racetime_txt.changeText( "RACE TIME: " + String.timeToString( raceTime[0], String.TCF_NOHOURS ) );

				if(fines)
				{
					if(gmcEvent.cleanDriving)
					{
						if(isOffroad()) gmThread.execute(0);
					}
				}

				//THOR methods (in-race)
				if(gmThread.methodStatus(0) == 1) //fine for runout
				{
					if(fines)
					{
						if(!fines[0]) //if no penalties got yet
						{
							if(fines[2] < 2)
							{
								if(gTrack) gTrack.showMsgBox("GET BACK TO THE ROAD!", Track.MBOX_RED, Track.MBOX_MID );
								fines[2]++;
								updateFines(2, 1);
								addTimer(0.5, 8); //set blinking animation
							}
							else
							{
								if(gTrack) gTrack.showMsgBox("YOU'VE GOT A 5 SECONDS PENALTY!", Track.MBOX_RED, Track.MBOX_LONG );
								fines[2]++;
								updateFines(2, 1);
								addTimer(0.5, 8); //set blinking animation

								fines[0]++; //give a penalty
								updateFines(0, 0);
								addTimer(0.5, 6); //set blinking animation
							}
						}
						else	gmThread.execute(1);
					}

					gmThread.controlMethod(0,-1);
					addTimer(4, 5); //4 secs to get back
				}

				if(gmThread.methodStatus(1) == 1) //disqualification
				{
					Sound.changeMusicSet(Sound.MUSIC_SET_NONE);
					raceActive = 0;
					clearScreen(); //hide all msgBoxes, OSD stuff

					finishRace(2); //now show up race end message (disqualification)
					if(GameLogic.player.car) GameLogic.player.car.command("brake");
					giveUp(); //mark career event as failed
					addTimer(4, 10); //4 sec timer for frWindow
					gmThread.controlMethod(1,-1);
				}

				if(gmThread.methodStatus(7) == 1) //finish race, show up erWindow
				{
					Sound.changeMusicSet(Sound.MUSIC_SET_NONE);
					raceActive = 0;
					clearScreen(); //hide all msgBoxes, OSD stuff

					finishRace(0); //show up race end message (race finished)
					addTimer(2, 12); //2 sec timer for braking down; better to replace this with the follow spline
					addTimer(4, 11); //4 sec timer for frWindow
					gmThread.controlMethod(7,-1);
				}
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
		stopTimer(0);
		nextTrigger[0] = 0;
		raceActive = 0;
		clearScreen(); //hide all msgBoxes, OSD stuff
		finishRace(0); //show up race end message (race finished)
		addTimer(1, 12);
		addTimer(2, 11);
	}
}

//individual window for showing up race results
public class finishWindowTimeAttack extends GameType
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
	Text[]	f_times; //finish time, target time

	//for animation; describes distances to travel in the animation sequence
	float	deltaDist_frame	= 2.0;
	float	deltaDist	= 1.0;

	int	windowStatus = 0;

	ResourceRef res_holder 	     = new ResourceRef(frontend:0xD142r); //holder_frame.png
	ResourceRef res_bckFlags     = new ResourceRef(frontend:0xD143r); //holder_flags_bck.png
	ResourceRef res_splitter     = new ResourceRef(frontend:0xD144r);
	ResourceRef res_separator    = new ResourceRef(frontend:0xC0A1r); //long separator fron EventList

	ResourceRef res_titles;
	ResourceRef res_cups;
	ResourceRef res_titles_awards;

	int titles_resid	     = frontend:0xD145r; //title_lose.png
	int cups_resid	 	     = frontend:0xD147r; //cup_gold.png
	int titles_awards_resid	     = frontend:0xD160r; //award_title_gold.png

	Rectangle   holder, bckFlags, splitter, titles, cups, titles_awards;
	Rectangle[] separator;

	float targetTime, raceTime;
	int finishPos; //like 1st/2nd/3rd etc.
	int highlight;
	String cupPostfix;

	SfxRef bckLoop;

	int debugMode = 0;

	public finishWindowTimeAttack(Gamemode gm, float[] tTime)
	{
		game = gm;

//--------------begin collect GM data

		raceTime = game.raceTime[0];
		if(game.fines && game.fines[0]) raceTime += 5;
		finishPos = 0; //initial pos

		if(game.gmcEvent.useAwards)
		{
			for(int i=0; i<tTime.length; i++)
			{
				targetTime = tTime[i];

				if(raceTime <= targetTime) i = tTime.length; //break
				else finishPos++;
			}

			switch(finishPos)
			{
				case 0: cupPostfix = "Gold"; break;
				case 1: cupPostfix = "Silver"; break;
				case 2: cupPostfix = "Bronze"; break;
				
				default: cupPostfix = "Bronze"; break;
			}
		}
		else
		{
			targetTime = tTime[0];

			if(raceTime <= targetTime) finishPos = 0;
			else finishPos = 3;
		}

//--------------GM data collected

		if(debugMode) traceDebug();

		osd = new Osd(1.0, 0.0, 20);
		osd.show();
		osd.createHotkey( Input.RCDIK_ENTER, Input.KEY|Osd.HK_STATIC, 0x01, this );
		f_times = new Text[maxlines];
		separator = new Rectangle[maxlines];

		int checkWin;
		if(game.gmcEvent.useAwards)
		{
			if(finishPos < 3) checkWin = 1; //win
			else checkWin = 0; //lose

			if(checkWin == 1) res_cups = new ResourceRef(cups_resid+finishPos);
		}
		else if(finishPos == 0) checkWin = 1;
		res_titles = new ResourceRef(titles_resid+checkWin);
		res_titles_awards = new ResourceRef(titles_awards_resid+finishPos);

		holder = osd.createRectangle( 0.0-deltaDist_frame, 0.0, 2.0, 0.960, 1, res_holder, 0 );
		bckFlags = osd.createRectangle( 0.835, -0.045, 0.635, 0.425, 2, res_bckFlags, 0 );
		results_title_txt = osd.createText("RACE RESULTS", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.475 );
		results_title_txt.changeColor(0x00000000);
		splitter = osd.createRectangle( 0.0-deltaDist, 0.345, 0.25, 0.0325, 2, res_splitter, 0 );
		titles = osd.createRectangle( 0.63+deltaDist, 0.020, 0.28, 0.120, 3, res_titles, 0 );

		if(game.gmcEvent.useAwards && checkWin == 1)
		{
			cups = osd.createRectangle( 0.0, 0.070, 0.10, 0.200, 3, res_cups, 0 );
			titles_awards = osd.createRectangle( 0.0, -0.165, 0.15, 0.150, 3, res_titles_awards, 0 );
		}

		if(checkWin == 1) //race won
		{
			prize_txt = osd.createText("", Frontend.largeFont_strong, Text.ALIGN_CENTER, 0.5, 0.375 );
			prize_txt.changeColor(0x00000000);

			if(game.gmcEvent.useAwards) prize_txt.changeText("YOUR PRIZE: " + game.gmcEvent.getPrizeCash(finishPos)); //returns safe results anyway; see CareerEvent.class for more details
			else prize_txt.changeText("YOUR PRIZE: " + game.gmcEvent.getPrizeName());
		}

		pressenter_txt = osd.createText("", Frontend.largeFont_strong, Text.ALIGN_CENTER, 0.0, 0.40 );
		pressenter_txt.changeColor(0x00000000);
		pressenter_txt.changeText("PRESS ENTER");

		//create infolines
		String[] prepareNames = new String[2];

		prepareNames[0] = "YOUR RACE TIME: " + String.timeToString(raceTime, String.TCF_NOHOURS);
		if(game.gmcEvent.useAwards) prepareNames[1] = "GOAL (" + cupPostfix + "): " + String.timeToString(targetTime, String.TCF_NOHOURS);
		else prepareNames[1] = "TARGET TIME: " + String.timeToString(targetTime, String.TCF_NOHOURS);
		
		highlight = 0; //line 0 is gonna be in a red color
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

		game.updateCareerStats(checkWin, finishPos); //update player's achievements in his career
		game.updateEventStatus(checkWin, finishPos); //update status of event for displaying in EventList (event failed/succeded or passed with an award)

		//finally give a prize to player for winning a race
		if(checkWin == 1)
		{
			if(game.gmcEvent.useAwards) game.gmcEvent.aquirePrize(finishPos);
			else game.gmcEvent.aquirePrize();
		}
		show();
	}

	public void createLine(int line, String data)
	{
		f_times[line] = osd.createText(data, Frontend.largeFont_strong, Text.ALIGN_LEFT, xpos, ypos );
		separator[line] = osd.createRectangle( xpos_ls-deltaDist, ypos_ls, 0.395, 0.004, 2, res_separator, 0 );
		f_times[line].changeColor(0x00000000);
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
			for(int i=0; i<f_times.length; i++)
			{
				if(f_times[i])
				{
					f_times[i].restartAnimation();
					f_times[i].a_speed = 1;
					f_times[i].fadeOut(0xF0000000); //from black to 100% transparent
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

			if(cups) cups.finalize();
			if(titles_awards) titles_awards.finalize();
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
		System.log("TimeAttack: no debug data provided");
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
		if(cups) cups.finalize();
		if(titles_awards) titles_awards.finalize();
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
				for(int i=0; i<f_times.length; i++)
				{
					if(f_times[i])
					{
						if(i != highlight) f_times[i].fadeIn(0xFF000000); //to solid black
						else f_times[i].fadeIn(0xFFFF0000); //to solid red
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