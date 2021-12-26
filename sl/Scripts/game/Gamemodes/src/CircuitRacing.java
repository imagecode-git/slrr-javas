//v 1.07
package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.sound.*;

public class CircuitRacing extends Gamemode
{
	int laps, current_lap, flags;
	int[] bot_laps;

	Text lap_txt, besttime_txt, laptime_txt;

	int pitStage = 0; //monitoring player's activity inside the pits
	int pitPenalty = 0;
	float penaltyTime = 8.0;
	float penaltyStartTime = 0.0f;
	float penaltyTimer;

	int rankTimer = 0;
	int racePosUpdateLatency = 10;

	finishWindowCircuitRacing fWindow;

	public CircuitRacing() {}

	public void init()
	{
		name = "Circuit Racing";
		shieldIcon = new ResourceRef(sl.Scripts.game.Gamemodes.stdpack_231:0x1000r);
	}

	public void launch()
	{
		useTimers = 1;

		posFont = new Rectangle[3];
		res_posFont = new ResourceRef[3];
		posFont_resid = new int[3];

		super.init(); //setup fines stuff

		if(gmcEvent && gmcEvent.useFines && res_fineFlags)
		{
			//active						//disabled
			res_fineFlags[0] = new ResourceRef(frontend:0xD126r);	res_fineFlags[3] = new ResourceRef(frontend:0xD139r); //fine_penalty
			res_fineFlags[1] = new ResourceRef(frontend:0xD127r);	res_fineFlags[4] = new ResourceRef(frontend:0xD13Ar); //fine_rudedriving
			res_fineFlags[2] = new ResourceRef(frontend:0xD128r);	res_fineFlags[5] = new ResourceRef(frontend:0xD13Br); //fine_runout
		}

		res_riSplitter = new ResourceRef(frontend:0xD125r);

		posFont_resid[0] = frontend:0x10A1r; //acitve
		posFont_resid[1] = frontend:0x10B1r; //disabled
		posFont_resid[2] = frontend:0x10C1r; //splitter

		res_posFont[0] = new ResourceRef(posFont_resid[0]);
		res_posFont[1] = new ResourceRef(posFont_resid[1]);
		res_posFont[2] = new ResourceRef(posFont_resid[2]);

		getOSD(); //get instance of OSD from Track()
		if(gmcEvent)
		{
			laps = gmcEvent.specialSlots[0];

			if(gmcEvent.racers && gmcEvent.botData.size()) //find opponents
			{
				gBot = new Bot[gmcEvent.racers];
				for(int i=0; i<gmcEvent.racers; i++)
				{
					gBot[i] = new Bot(gmcEvent.botData.elementAt(i).intValue()); //pick bots from the database
				}

				createBotCars(); //prepare cars, track will find them and place on the start grid
				joinBotCars(); //connect bots with the cars
			}
		}

		current_lap = 1;
		setupRacePosCalc();
		applyTriggers();
		position = new int[calcArray()]; //get array length to calculate data for posFont
		rankings = new Rank();

		if(calcArray() > 1) bot_laps = new int[calcArray()-1];
		if(bot_laps) for(int k=0; k<bot_laps.length; k++) {bot_laps[k] = 0;}

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
			if(gmcEvent.useFines && GameLogic.player.car) //warning! fine stuff won't be created if only bots are driving
			{
				//player.car _must_ exist before calling this!
				fineFlags[0] = osd.createRectangle( 2.95, 0.65, 0.121, 0.216, 1, res_fineFlags[1+3], 0 ); //fine_rudedriving
				if(gmcEvent.cleanDriving)
				{
					flags++;
					fineFlags[flags] = osd.createRectangle( 2.68, 0.65, 0.121, 0.216, 1, res_fineFlags[2+3], 0 ); //fine_runout
					fineFlags[flags+1] = osd.createRectangle( 2.41, 0.65, 0.121, 0.216, 1, res_fineFlags[0+3], 0 ); //fine_penalty
				}
				else
				{
					fineFlags[flags+1] = osd.createRectangle( 2.68, 0.65, 0.121, 0.216, 1, res_fineFlags[0+3], 0 ); //fine_penalty
				}
				flags+=2;

				for(int i=0; i<flags; i++)
				{
					fineFlags[i].setupAnimation(0.1, 10, -1, "X"); //direction: left (-1)
					fineFlags[i].runThread(); //begin the animation
				}
			}

			riSplitter = osd.createRectangle( 2.67, 0.81, 0.32, 0.036, 1, res_riSplitter, 0 );
			riSplitter.setupAnimation(0.1, 10, -1, "X"); //direction: left (-1)
			riSplitter.runThread(); //begin the animation

			lap_txt = osd.createText( "LAP: " + current_lap + "/" + laps, Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.92 );

			bestTime = GameLogic.player.getTrackData(gmcEvent.track.name);
			besttime_txt = osd.createText( "BEST TIME: " + String.timeToString( bestTime, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.603, -0.835 );

			laptime_txt = osd.createText( "LAP TIME: " + String.timeToString( 0.0f, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.75 );

			lap_txt.changeColor(0x00000000);
			besttime_txt.changeColor(0x00000000);
			laptime_txt.changeColor(0x00000000);

			lap_txt.fadeIn();
			besttime_txt.setupAnimation(0x0FFFFFFF, 8, 1);	besttime_txt.runThread();
			laptime_txt.setupAnimation(0x0FFFFFFF, 8, 1);	laptime_txt.runThread();

			if(gBot)
			{
				//position indicator
				posFont[0] = osd.createRectangle( -3.0,  1.02, 0.077, 0.132, 1, res_posFont[0], 0 ); //player's index
				posFont[1] = osd.createRectangle( -2.75, 0.95, 0.077, 0.132, 1, res_posFont[1], 0 ); //total racers
				posFont[2] = osd.createRectangle( -2.88, 0.97, 0.058, 0.255, 1, res_posFont[2], 0 ); //splitter

				//update textures in posFont
				position[0] = 1; //initial data
				getInitialPosition(); //properly calculated initial race position
				setupPosFont(position[0]);

				for(int j=0; j<3; j++)
				{
					posFont[j].setupAnimation(0.1, 10, 1, "X"); //direction: right (1)
					posFont[j].runThread(); //begin the animation
				}
			}

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

		if(gBot && posFont)
		{
			for(int j=0; j<3; j++)
			{
				posFont[j].restartAnimation("X");
				posFont[j].setupAnimation(0.1, 10, -1, "X"); //direction: opposite, left (-1)
				posFont[j].runThread(); //begin the animation
			}
		}
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

		if(pitTrigger)
		{
			for(int k=0; k<pitTrigger.length; k++)
			{
			        addNotification( pitTrigger[k].trigger, EVENT_TRIGGER_ON, EVENT_SAME, null, "handlePitStop" );
			}
		}

		if(rpTrigger)
		{
			for(int l=0; l<rpTrigger.length; l++)
			{
			        addNotification( rpTrigger[l].trigger, EVENT_TRIGGER_ON, EVENT_SAME, null, "handleRacePos" );
			}
		}
	}

	public void applySplines()
	{
		int vi;

		if(gmcEvent.track.splinesAI.size() > 0) vi = 1; //kind of randomization if number of AI splines is smaller, than total amount of opponents

		for(int i=0; i<gBot.length; i++)
		{
			if(i==0) //spline for the first bot
			{
				if(gBot[i] && GameLogic.player.car)
					gBot[i].followSplineTrack( Math.random()*3, gmcEvent.track.splinesAI.elementAt(i), GameLogic.player.car.id() );
				else
				{
					//if the player is not participating a race (only bots are driving)
					if(gBot[i] && gBot[i+1])
						gBot[i].followSplineTrack( Math.random()*3, gmcEvent.track.splinesAI.elementAt(i), gBot[i+1].car.id() );

					//a ghost car can be used instead of 2nd bot as well (if it's a circuit race with the only one bot driving on track)
				}
			}
			else //for all other bots
			{
				if(gBot[i] && gBot[i-1])
				{
					if(gmcEvent.track.splinesAI.size() > 1)
					{
						if(i <= (gmcEvent.track.splinesAI.size()-1))
						{
							gBot[i].followSplineTrack( Math.random()*10, gmcEvent.track.splinesAI.elementAt(vi), gBot[i-1].car.id() );
							vi++;
						}
						else
						{
							vi = 0;
							gBot[i].followSplineTrack( Math.random()*10, gmcEvent.track.splinesAI.elementAt(vi), gBot[i-1].car.id() );
							vi++;
						}
					}
					else
						gBot[i].followSplineTrack( Math.random()*3, gmcEvent.track.splinesAI.elementAt(0), gBot[i-1].car.id() );
				}
			}
		}
	}

	public void beginRace() //here the race actually begins
	{
		unlockCars();

		if(gmcEvent.track.splinesAI && gBot)
		{
			applySplines(); //find splines and force bots to drive
		}

		startTimers();
		raceActive = 1;
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	public void handleCollision( GameRef obj_ref, int event, String param )
	{
		if(!destroy)
		{
			GameRef obj = new GameRef(param.token(0).intValue());
			int cat = obj.getInfo(GII_CATEGORY);
			if(cat == GIR_CAT_VEHICLE)
			{
				int collider = obj.getInfo(GII_TYPE);
				if(gBot)
				{
					for(int i=0; i<gBot.length; i++)
					{
						if(fines && fines.length)
						{
							if(collider == gBot[i].car.chassis.getInfo(GII_TYPE) && !rdTimeout)
							{
								if(!fines[0]) //if no penalties got yet
								{
									if(fines[1] < 2)
									{
										if(gTrack) gTrack.showMsgBox("DRIVE SAFELY!", Track.MBOX_RED, Track.MBOX_SHORT );
										fines[1]++;
										if(flags == 3) updateFines(1, 0); else updateFines(1, 4);
										rdTimeout = 1;
										addTimer(0.5, 7); //set blinking animation
										addTimer(4, 9); //4 secs timeout for this fine
									}
									else
									{
										if(gTrack) gTrack.showMsgBox("YOU'VE GOT A PENALTY! GO TO THE PITS", Track.MBOX_RED, Track.MBOX_LONG );
										fines[1]++;
										if(flags == 3) updateFines(1, 0); else updateFines(1, 4);
										rdTimeout = 1;
										addTimer(0.5, 7); //set blinking animation
										addTimer(4, 9); //4 secs timeout for this fine

										fines[0]++; //give a penalty
										if(flags == 3) updateFines(0, 2); else updateFines(0, 1);
										addTimer(0.5, 6); //set blinking animation
									}
								}
								else gmThread.execute(1); //disqualify
							}
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
					fWindow = new finishWindowCircuitRacing(this, bot_laps);
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
			if( param == 9 ) //rude driving timeout handler
			{
				rdTimeout = 0;
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
			if( param == 7 ) //update flag: rude driving
			{
				if(bTimer[1] < 7)
				{
					if(flags == 3) updateFines(1, 0); else updateFines(1, 4);
					addTimer(0.5, 7);
				}
				else	{if(flags == 3) fixFineStatus(1, 0); else fixFineStatus(1, 4);}
			}
			else
			if( param == 6 ) //update flag: penalty
			{
				if(bTimer[0] < 7)
				{
					if(flags == 3) updateFines(0, 2); else updateFines(0, 1);
					addTimer(0.5, 6);
				}
				else	{if(flags == 3) fixFineStatus(0, 2); else fixFineStatus(0, 1);}
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
							if(gBot) prepareBots(0); //start the engines
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
		int dlt;
		if(!botsOnly) dlt = 1; else dlt = 0;

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
					//check if player has penalty at the end of lap
					int pass = 1;
					if(gmcEvent.useFines)
					{
						if(fines[0]) pass = 0;
					}

					if(pass) //no penalties or fines are not used
					{
						if((current_lap+1) <= gmcEvent.specialSlots[0]) //slot 0: laps count
						{
							gTrack.showMsgBox("LAP " + current_lap + ": " + String.timeToString(time[0], String.TCF_NOHOURS), Track.MBOX_GREEN, Track.MBOX_SHORT );
							current_lap++;
							String bestLapTime = checkBestTime();
							if(bestLapTime) gTrack.showMsgBox("NEW BEST LAP TIME: " + bestLapTime, Track.MBOX_YELLOW, Track.MBOX_MID );
							lap_txt.changeText("LAP: " + current_lap + "/" + laps);
							time[0] = 0.0f;
							startTimer(0);
							nextTrigger[0] = 0;
						}
						else //player ends race
						{
							checkBestTime();
							stopTimer(0);
							nextTrigger[0] = 0;
							gmThread.execute(7); //show up finish window
						}
					}
					else //player has a penalty, so he must drive to the pits
					{
						if(!pitStage) gTrack.showMsgBox("GET TO THE PITS!", Track.MBOX_RED, Track.MBOX_SHORT );
					}
				}
			}
		}
		else
		{
			if(gBot)
			{
				for(int i=0; i<gBot.length; i++)
				{
					if( id == gBot[i].car.id() ) //bot checkpoint handling
					{
						if(!(bot_laps[i] == 0 && nextTrigger[i+dlt] == 0 && cpTrigger[cpTrigger.length-1].trigger.id() == obj_ref.id())) //filtering out start line trigger on the first lap
						{
							if(cpTrigger[nextTrigger[i+dlt]].trigger.id() == obj_ref.id()) //player usually occupies slot 0
							{
								if(nextTrigger[i+dlt] < cpTrigger.length-1) //bot passed checkpoint ('common' trigger)
								{
									nextTrigger[i+dlt]++;
								}
								else //bot pass last trigger (start line)
								{
									bot_laps[i]++;
									if((bot_laps[i]) < gmcEvent.specialSlots[0]) //slot 0: laps count
									{
										time[i+dlt] = 0.0f;
										startTimer(i+dlt);
										gBot[i].car.chassis.tank_nitro = nitroTanks[i]; //reset nitrous
										nextTrigger[i+dlt] = 0;
									}
									else //bot finishes race
									{
										if((bot_laps[i]) == gmcEvent.specialSlots[0]) //stop timer and show up the message only once in the race
										{
											stopTimer(i+dlt);
											nextTrigger[i+dlt] = 0;
											gTrack.showMsgBox( gBot[i].profile.name_f + " " + gBot[i].profile.name_s + " HAS FINISHED RACE!", Track.MBOX_YELLOW, Track.MBOX_LONG );
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void handlePitStop( GameRef obj_ref, int event, String param )
	{
		int id = param.token(0).intValue();

		if( id == GameLogic.player.car.id() && pitTrigger )
		{
			if(pitTrigger[0].trigger.id() == obj_ref.id()) //entering pit lane
			{
				if(!cpTrigger || (cpTrigger && nextTrigger[0] == cpTrigger.length-1) ) //all CP's must be passed
				{
					if(!pitStage) //first attempt
					{
						if(fines && fines.length)
						{
							for(int l=0; l<fines.length; l++)
							{
								if(fines[l] == 3) //player got a penalty
								{
									pitStage = 1;
									if(gmThread.methodStatus(3) == -1) gmThread.switchStatus(3); //regenerate THOR for 3rd pit stage

									String spd = "50MPH";
									if(Config.metricSystem) spd = "80KPH";
									gTrack.showMsgBox("ENTERING PITS, SPEED LIMIT: " + spd, Track.MBOX_YELLOW, Track.MBOX_LONG);

									break;
								}
							}
						}
					}
				}
			}

			if(pitTrigger[1].trigger.id() == obj_ref.id()) //the pit 
			{
				if(pitStage == 1)
				{
					for(int l=0; l<fines.length; l++)
					{
						if(fines[l] == 3)
						{
							if(GameLogic.player.car) GameLogic.player.car.command("brake");
							gmThread.execute(2); //begin penalty timer stuff
						}
					}
				}
			}

			if(pitTrigger[2].trigger.id() == obj_ref.id()) //return to track
			{
				if(pitStage == 2)
				{
					if(cpTrigger)
					{
						if((current_lap+1) <= gmcEvent.specialSlots[0]) //slot 0: laps count
						{
							//general next lap stuff start
							gTrack.showMsgBox("LAP " + current_lap + ": " + String.timeToString(time[0], String.TCF_NOHOURS), Track.MBOX_GREEN, Track.MBOX_SHORT );
							current_lap++;
							if(time[0] < bestTime || !bestTime)
							{
								bestTime = time[0];
								GameLogic.player.setTrackData(gmcEvent.track.name, bestTime);
								String bestLapTime = String.timeToString( GameLogic.player.getTrackData(gmcEvent.track.name), String.TCF_NOHOURS );

								besttime_txt.changeText("BEST TIME: " + bestLapTime);
								gTrack.showMsgBox("NEW BEST LAP TIME: " + bestLapTime, Track.MBOX_YELLOW, Track.MBOX_MID );
							}
							lap_txt.changeText("LAP: " + current_lap + "/" + laps);
							time[0] = 0.0f;
							startTimer(0);
							nextTrigger[0] = 0;
							//general stuff end
						}
						else //player ends race
						{
							if(time[0] < bestTime || !bestTime)
							{
								bestTime = time[0];
								GameLogic.player.setTrackData(gmcEvent.track.name, bestTime);
								String bestLapTime = String.timeToString( GameLogic.player.getTrackData(gmcEvent.track.name), String.TCF_NOHOURS );
								besttime_txt.changeText("BEST TIME: " + bestLapTime);
							}
							stopTimer(0);
							nextTrigger[0] = 0;
							gmThread.execute(7); //show up finish window
						}
					}
					else gTrack.showMsgBox("PENALTIES RESET. CONTINUE RACE", Track.MBOX_GREEN, Track.MBOX_MID );

					//"forgive" the player
					for(int ii=0; ii<fines.length; ii++)
					{
						if(fines[ii] == 3)
						{
							fines[ii] = 0;
							fines[0] = 0; //reset penalty status

							//update flags:
							if(ii == 1) //rude driving
								resetFines(1, 0);

							if(ii == 2) //runout
								resetFines(2, 1);

							//penalty flag
							if(flags == 3) resetFines(0, 2);
							else resetFines(0, 1);
						}
					}

					pitStage = 0;
				}
			}
		}
	}

	public void handleRacePos( GameRef obj_ref, int event, String param )
	{
		int id = param.token(0).intValue();
		int dlt;
		if(!botsOnly) dlt = 1; else dlt = 0;

		if(raceActive && rpTrigger)
		{
			int temp = 0.85*cpTrigger.length; //detect end of round with checkpoints

			if(id == GameLogic.player.car.id()) //player pass racepos trigger
			{
				for(int i=0; i<rpTrigger.length; i++)
				{
					if(rpTrigger[i].trigger.id() == obj_ref.id())
					{
						if(i<rpTrigger.length-1) rpNextTrigger[0] = i+1;
						else rpNextTrigger[0] = 0;

						//patch: position of last rpTrigger may be different from the position of finish line and that may cause bugs while calculating rpTotal
						if((rpNextTrigger[0] > rpTrigger.length-1 || rpNextTrigger[0] < (int)(0.2*rpTrigger.length)) && (nextTrigger[0] >= temp))
							rpTotal[0] = rpTrigger.length*(current_lap);
						else rpTotal[0] = (rpTrigger.length*(current_lap-1)) + i;
/*
						System.trace("temp: " + temp);
						System.trace("cpTrigger.length: " + cpTrigger.length);
						System.trace("nextTrigger[0]: " + nextTrigger[0]);
						System.trace("player pass through rpTrigger[" + i + "]");
						System.trace("rpNextTrigger[0]: " + rpNextTrigger[0]);
						System.trace("rpTrigger.length: " + rpTrigger.length);
						System.trace("rpTotal[0]: " + rpTotal[0]);
						System.trace("current_lap: " + current_lap);
*/
					}
				}
			}

			if(gBot)
			{
				for(int i=0; i<gBot.length; i++)
				{
					if(id == gBot[i].car.id()) //bot racepos trigger handling
					{
						for(int j=0; j<rpTrigger.length; j++)
						{
							if(rpTrigger[j].trigger.id() == obj_ref.id())
							{
								if(j<rpTrigger.length-1) rpNextTrigger[i+dlt] = j+1;
								else rpNextTrigger[i+dlt] = 0;

								//same patch for bots
								if((rpNextTrigger[i+dlt] > rpTrigger.length-1 || rpNextTrigger[i+dlt] < (int)(0.2*rpTrigger.length)) && (nextTrigger[i+dlt] >= temp))
									rpTotal[i+dlt] = rpTrigger.length*(bot_laps[i]+1);
								else rpTotal[i+dlt] = (rpTrigger.length*bot_laps[i]) + j;
/*
								System.trace("              ---");
								System.trace("bot[0] pass through rpTrigger[" + j + "]");
								System.trace("nextTrigger[1]: " + nextTrigger[1]);
								System.trace("rpNextTrigger[1]: " + rpNextTrigger[1]);
								System.trace("rpTrigger.length: " + rpTrigger.length);
								System.trace("rpTotal[1]: " + rpTotal[1]);
								System.trace("bot_laps[i]: " + bot_laps[i]);
								System.trace("EOF---");
								System.trace("");
*/
							}
						}
					}
				}
			}
		}
	}

	public String checkBestTime()
	{
		if(!botsOnly)
		{
			if(time[0] < bestTime || !bestTime)
			{
				bestTime = time[0];
				GameLogic.player.setTrackData(gmcEvent.track.name, bestTime);
				String bestLapTime = String.timeToString(GameLogic.player.getTrackData(gmcEvent.track.name), String.TCF_NOHOURS);
				besttime_txt.changeText("BEST TIME: " + bestLapTime);

				return bestLapTime;
			}
		}

		return null;
	}

	public void getInitialPosition()
	{
		Vector3[] gridPos = new Vector3[calcArray()];
		Vector gridBackup = new Vector();
		Vector stack = new Vector();
		float[] dist = new float[calcArray()];
		int idx;

		//step 1: memorize old configuration of start grid
		for(int l=0; l<calcArray()*2; l++)
		{
			gridBackup.addElement(new Pori(gTrack.startGridData.elementAt(l), gTrack.startGridData.elementAt(l+1)));
			l++;
		}

		//step 2: we suppose that start grid is always randomized, so we revert it back to normal state
		gTrack.startGridData = new Vector();
		gmcEvent.track.getGrid();

		//step 3: collect all start grid V3's, occupied by racers
		for(int i=0; i<gridPos.length; i++)
		{
			Vector3 g = gTrack.startGridData.elementAt(idx);
			gridPos[i] = new Vector3(g.x,g.y,g.z);
			idx+=2; //jump to next V3 in grid data
		}


		//step 4: collect distances between racers, standing on the start grid
		for(int j=0; j<dist.length; j++)
		{
			//we use "normal" grid as boundary indicator and randomized grid as a reference point
			dist[j] = getSplineDistance(gridPos[gridPos.length-1], gridPos[0], gridBackup.elementAt(j).pos);
			stack.addElement(new Float(dist[j]));
		}

		//step 5: arrange collected distances using rank calculator
		Vector ranks = rankings.getRanks(stack, Rank.MAXMIN); //MAXMIN, higher values have better rating
		for(int k=0; k<ranks.size(); k++) position[ranks.elementAt(k).intValue()] = k+1;

		//step 6: update visuals for player
		updatePosFont();

		//step 7: finally, restore randomized start grid from gridBackup
		for(int m=0; m<gridBackup.size(); m++)
		{
			Pori po = gridBackup.elementAt(m);
			Vector3 p = po.pos;
			Ypr o = po.ori;
		}
	}

	public void updatePosition()
	{
		int dlt, interpolate, prevTriggerIdx;
		float[] dist = new float[calcArray()];
		Vector cmp = new Vector();
		Vector base;

		if(!botsOnly) dlt = 1; else dlt = 0;

		int iCheck;
		for(int j=0; j<rpTotal.length; j++)
		{
			if(rpTotal[0] == rpTotal[j] && rpNextTrigger[0] == rpNextTrigger[j]) iCheck++;
		}

		if(iCheck) interpolate = 1;
		else interpolate = 0;

		if(interpolate == 0) //collect data to arrange by total amount of rpTriggers passed; less CPU usage
		{
			for(int k=0; k<rpTotal.length; k++)
			{
				cmp.addElement(new Integer(rpTotal[k]));
			}
		}
		else //collect data to arrange by the distance from the passed rpTrigger to next rpTrigger, calculated over vector projection; greater CPU usage
		{
			base = new Vector();
			for(int j=0; j<rpTotal.length; j++) base.addElement(new Integer(rpTotal[j]));

			if(GameLogic.player.car)
			{
				if(rpNextTrigger[0] > 0) prevTriggerIdx = rpNextTrigger[0]-1;
				else prevTriggerIdx = rpTrigger.length-1;

				dist[0] = getSplineDistance(rpTrigger[rpNextTrigger[0]].pos, rpTrigger[prevTriggerIdx].pos, GameLogic.player.car.getPos());
				cmp.addElement(new Float(dist[0]));
			}

			if(gBot)
			{
				for(int i=0; i<gBot.length; i++)
				{
					if(gBot[i].car)
					{
						if(rpTotal[0] == rpTotal[i+dlt])
						{
							if(rpNextTrigger[i+dlt] > 0) prevTriggerIdx = rpNextTrigger[i+dlt]-1;
							else prevTriggerIdx = rpTrigger.length-1;

							dist[i+dlt] = getSplineDistance(rpTrigger[rpNextTrigger[i+dlt]].pos, rpTrigger[prevTriggerIdx].pos, gBot[i].car.getPos());
							cmp.addElement(new Float(dist[i+dlt]));
						}
						else cmp.addElement(new Object());
					}
				}
			}
		}

		if(cmp.size())
		{
			if(base && base.size()) doRanking(base,cmp);
			else doRanking(cmp,null);
		}
	}

	public void doRanking(Vector rdata, Vector precision)
	{
		int oldpos = position[0]; //memorize position of player;
		int addpos;
		Vector source = rankings.getRanks(rdata, Rank.MAXMIN); //MAXMIN, higher values have better rating
		Vector sortPrecision, addRank, precisionIndices;

		if(precision && precision.size()) //interpolated ranking
		{
			for(int i=1; i<rdata.size(); i++)
			{
				if(rdata.elementAt(i).number > rdata.elementAt(0).number) addpos++; //first of all, find out, how the player lagged out from others with higher rpTotal
			}

			sortPrecision = new Vector();
			precisionIndices = new Vector();
			for(int j=0; j<precision.size(); j++)
			{
				if(precision.elementAt(j) instanceof Float) //now collect precision values of racers, that have same rpTotal as player
				{
					sortPrecision.addElement(precision.elementAt(j));
					precisionIndices.addElement(new Integer(j)); //also memorize indices of theese racers
				}
			}

			//then rank all racers by the highest precision values
			if(sortPrecision && sortPrecision.size()) addRank = rankings.getRanks(sortPrecision, Rank.MINMAX); //MINMAX, higher values have better rating

			if(addRank && addRank.size())
			{
				for(int l=0; l<addRank.size(); l++) //finally, match ranks with indices of racers to define proper positions
				{
					for(int i=0; i<precisionIndices.size(); i++)
					{
						if(l == precisionIndices.elementAt(i).number)
						{
							position[precisionIndices.elementAt(i).number] = addRank.elementAt(l).number+1;
							i = precisionIndices.size(); //end up cycle, next l
						}
					}
				}
				position[0] += addpos; //add player's lag to his precision rank
			}
		}
		else //simple ranking
		{
			for(int i=0; i<source.size(); i++) position[source.elementAt(i).intValue()] = i+1;
		}

		if(oldpos != position[0]) updatePosFont(); //update if needed
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
						else
						{
							gTrack.lastCamPos.rotate(gBot[0].car.getOri());
							gTrack.lastCamPos.add(gBot[0].player.car.getPos() );
							gTrack.changeCamTarget(gBot[0].player.car);
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

				if(!Integrator.frozen && !Integrator.IngameMenuActive && gBot)
				{
					rankTimer++;
					if(rankTimer == racePosUpdateLatency) //theese calculations are processed in a slower rate to boost FPS
					{
						updatePosition();
						rankTimer = 0;
					}
				}

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
								if(gTrack) gTrack.showMsgBox("YOU'VE GOT A PENALTY! GO TO THE PITS", Track.MBOX_RED, Track.MBOX_LONG );
								fines[2]++;
								updateFines(2, 1);
								addTimer(0.5, 8); //set blinking animation

								fines[0]++; //give a penalty
								if(flags == 3) updateFines(0, 2); else updateFines(0, 1);
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

					finishRace(1); //now show up race end message (disqualification)
					if(GameLogic.player.car) GameLogic.player.car.command("brake");
					giveUp(); //mark career event as failed
					addTimer(4, 10); //4 sec timer for frWindow
					gmThread.controlMethod(1,-1);
				}

				if(gmThread.methodStatus(2) == 1) //call msgBox with penalty timer
				{
					penaltyTimer = 0.0f;
					penaltyTime = 8.0;
					penaltyStartTime = System.simTime();
					if(gTrack) gTrack.showMsgBox( String.timeToString( penaltyTime-penaltyTimer, String.TCF_NOHOURS ) + "s LEFT TO WAIT", Track.MBOX_RED, Track.MBOX_MID );
					if(gTrack) gTrack.holdMsgBox( String.timeToString( penaltyTime-penaltyTimer, String.TCF_NOHOURS ) + "s LEFT TO WAIT" ); //begin hold
					pitPenalty = 1;
					gmThread.switchStatus(2); //reset THOR for this method
				}

				if(gmThread.methodStatus(3) == 1) //releasing the car from pits, getting to the exit of pit lane
				{
					pitPenalty = 0;
					pitStage = 2;
					if(GameLogic.player.car) GameLogic.player.car.command("start");
					gTrack.unholdMsgBox(); //end of hold
					gTrack.showMsgBox("DRIVE TO THE END OF PIT LANE", Track.MBOX_YELLOW, Track.MBOX_MID );
					gmThread.controlMethod(3,-1);
				}

				if(gmThread.methodStatus(7) == 1) //finish race, show up erWindow
				{
					Sound.changeMusicSet(Sound.MUSIC_SET_NONE);
					raceActive = 0;
					clearScreen(); //hide all msgBoxes, OSD stuff

					//lock controls (doesn't work?)
					if(GameLogic.player && GameLogic.player.car)
					{
						GameLogic.player.controller.command( "controllable 0" );
						GameLogic.player.controller.activateState( 5, 1 );
					}

					finishRace(0); //show up race end message (race finished)
					addTimer(2, 12); //2 sec timer for braking down; better to replace this with the follow spline
					addTimer(4, 11); //4 sec timer for frWindow
					gmThread.controlMethod(7,-1);
				}

				if(pitStage)
				{
					if(GameLogic.player.car)
					{
						if(GameLogic.player.car.getSpeed() > 22.0 ) //80kph
							gmThread.execute(1); //disqualification if speed limit is broken inside the pits
					}
				}

				if(pitPenalty)
				{
					if(penaltyTime-penaltyTimer > 0)
					{
						if(!Integrator.frozen && !Integrator.IngameMenuActive) penaltyTimer = System.simTime() - penaltyStartTime;
						if(penaltyTime-penaltyTimer<0) {penaltyTime = 0.0f; penaltyTimer = 0.0f;} //to prevent incorrect values on display
						if(gTrack) gTrack.holdMsgBox( String.timeToString( penaltyTime-penaltyTimer, String.TCF_NOHOURS ) + "s LEFT TO WAIT" ); //continue hold
					}
					else	gmThread.execute(3);
				}
			}
			gmThread.sleep(10);
		}
	}

	public void endRace()
	{
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
public class finishWindowCircuitRacing extends GameType
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
	int  	maxlines = 6;

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
	ResourceRef res_cups;
	ResourceRef res_titlePosFont;

	int titles_resid	     = frontend:0xD145r; //title_lose.png
	int cups_resid	 	     = frontend:0xD147r; //cup_gold.png
	int titlePosFont_resid	     = frontend:0x10C2r; //title_pos_1st.png

	Rectangle   holder, bckFlags, splitter, titles, cups, titlePosFont;
	Rectangle[] separator;

	Vector finishedNames = new Vector(); //vector instead of array to force rank indices match the indices from elements of finishedNames
	String[] notFinished;

	Rank rating;
	Vector timesRaw, timesArranged, rankIndices;

	int[] botLaps;
	int finishPos; //like 1st/2nd/3rd etc.
	int highlight;

	SfxRef bckLoop;

	int debugMode = 0;

	public finishWindowCircuitRacing(Gamemode gm, int[] bl)
	{
		game = gm;
		botLaps = bl;

		rating = new Rank();
		timesRaw = new Vector();
		timesArranged = new Vector();

//--------------begin collect GM data
		notFinished = new String[game.calcArray()];

		if(GameLogic.player)
		{
			finishedNames.addElement(GameLogic.player.getName());
			timesRaw.addElement(new Float(game.raceTime[0]));
		}

		int dlt, loosers;
		if(!game.botsOnly) dlt = 1; else dlt = 0;
		loosers = 0;

		if(game.gBot)
		{
			for(int i=0; i<game.gBot.length; i++)
			{
				if(botLaps[i] >= game.gmcEvent.specialSlots[0])
				{
					finishedNames.addElement(game.gBot[i].profile.getName());
					timesRaw.addElement(new Float(game.raceTime[i+dlt]));
				}
				else
				{
					notFinished[loosers] = game.gBot[i].profile.getName();
					loosers++;
				}
			}
		}
//--------------GM data collected

		//generate arranged list of race times
		if(timesRaw.size() > 1) timesArranged = rating.getData(timesRaw, Rank.MINMAX); //MINMAX, lowest values have higher rating
		else timesArranged = Vector.copy(timesRaw);

		//get id of each timelist entry to arrange names
		rankIndices = rating.getRanks(timesRaw, Rank.MINMAX);

		for(int i=0; i<rankIndices.size(); i++)
		{
			if(rankIndices.elementAt(i).number == 0) finishPos = i;
		}

		if(debugMode) traceDebug();

		osd = new Osd(1.0, 0.0, 20);
		osd.show();
		osd.createHotkey( Input.RCDIK_ENTER, Input.KEY|Osd.HK_STATIC, 0x01, this );
		f_racers = new Text[maxlines];
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
		res_titlePosFont = new ResourceRef(titlePosFont_resid+finishPos);

		holder = osd.createRectangle( 0.0-deltaDist_frame, 0.0, 2.0, 0.960, 1, res_holder, 0 );
		bckFlags = osd.createRectangle( 0.835, -0.045, 0.635, 0.425, 2, res_bckFlags, 0 );
		results_title_txt = osd.createText("RACE RESULTS", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.475 );
		results_title_txt.changeColor(0x00000000);
		splitter = osd.createRectangle( 0.0-deltaDist, 0.345, 0.25, 0.0325, 2, res_splitter, 0 );
		titles = osd.createRectangle( 0.63+deltaDist, 0.020, 0.28, 0.120, 3, res_titles, 0 );

		float yCoord;
		if(game.gmcEvent.useAwards && checkWin == 1)
		{
			cups = osd.createRectangle( 0.0, 0.070, 0.10, 0.200, 3, res_cups, 0 );
			yCoord = -0.165; //under the cup
		}
		else yCoord = 0.020; //no cups, centered pos
		titlePosFont = osd.createRectangle( 0.0, yCoord, 0.15, 0.150, 3, res_titlePosFont, 0 );

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

		int line;
		int maxlines = 6;
		String[] prepareNames = new String[maxlines]; //array for preprocessing line strings

		for(int i=0; i<rankIndices.size(); i++)
		{
			if(line == (maxlines-1) && finishPos > (maxlines-2)) //player is out of the last line
			{
				if(line < maxlines)
				{
					prepareNames[line] = (finishPos+1) + ". " + finishedNames.elementAt(0) + " " + String.timeToString(timesArranged.elementAt(finishPos).number, String.TCF_NOHOURS);
					highlight = line; //mark line as red highlighted (for player)
				}
			}
			else
			{
				if(line < maxlines)
				{
					prepareNames[line] = (line+1) + ". " + finishedNames.elementAt(rankIndices.elementAt(i).number) + " " + String.timeToString(timesArranged.elementAt(i).number, String.TCF_NOHOURS);
					if(rankIndices.elementAt(i).number == 0) highlight = line;
				}
			}
			line++;
		}
		
		//data for racers who didn't finish the race
		for(int j=0; j<notFinished.length; j++)
		{
			if(notFinished[j] && notFinished[j].length())
			{
				if(line < maxlines)
				{
					prepareNames[line] = (line+1) + ". " + notFinished[j] + " not finished";
					line++;
				}
			}
		}

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

			if(cups) cups.finalize();
			if(titlePosFont) titlePosFont.finalize();
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
		if(game.gBot) System.log(game.gBot.length + " bots participate in race event"); else System.log("no bots detected in race event (!game.gBot)");
		System.log("");
		System.log("List of all participants (including player): ");
		int idx;
		for(int i=0; i<finishedNames.size(); i++)
		{
			if(finishedNames.elementAt(i)) System.log((idx+1) + ". " + finishedNames.elementAt(i));
			idx++;
		}
		System.log("");
		System.log("raw finish times (timesRaw): ");
		System.log("");
		for(int i=0; i<timesRaw.size(); i++)
		{
			System.log(String.timeToString(timesRaw.elementAt(i).number, String.TCF_NOHOURS)); //number!!! not Float!!

		}
		System.log("");
		System.log("arranged finish times (timesArranged): ");
		System.log("");
		for(int i=0; i<timesArranged.size(); i++)
		{
			System.log(String.timeToString(timesArranged.elementAt(i).number, String.TCF_NOHOURS)); //number!!! not Float!!
		}
		System.log("");
		if(!rating){System.log("rank calculator was not used for this call"); System.log("");}
		for(int i=0; i<botLaps.length; i++)
		{
			System.log("botLaps[" + i + "] = " + botLaps[i]);
		}
		System.log("");
		System.log("Ready output data: ");
		for(int i=0; i<timesArranged.size(); i++)
		{
			if(finishedNames.elementAt(rankIndices.elementAt(i).number))
				System.log((i+1) + ". " + finishedNames.elementAt(rankIndices.elementAt(i).number) + " " + String.timeToString(timesArranged.elementAt(i).number, String.TCF_NOHOURS));
		}
		if(notFinished)
		{
			System.log("");
			System.log("List of racers who didn't finish: ");
		}
		for(int i=0; i<notFinished.length; i++)
		{
			if(notFinished[i]) System.log((i+1) + ". " + notFinished[i]);
		}
		System.log("");
		System.log("Player/gBot[0] finished as: (finishPos) " + finishPos);
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
		titlePosFont.finalize();
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