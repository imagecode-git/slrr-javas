package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.sound.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;

public class Drifting extends Gamemode
{
	int laps, current_lap;
	int targetScore;
	int[] targetScores; //if awards are being used

	//following data is required to estimate what % of lap distance player did pass through
	int rpValidTrigger = -1; //last rpTrigger passed by player
	int validScoreZone = -1;

	//damage analyzer stuff
	int[] damageList;
	int chassisDamage;
	int scratches, hits, falloffs, crashes;

	int isDrifting, driftDirection, scoreGain, scoreGainCombined;
	int offroadState = 0;
	float singleDriftTime, combinedDriftTime;
	Timer driftTimer, idleTimer, breakTimer;

	//impression meter stuff
	SlidingMenu iMeter;
	float time_i;
	float speed_i;
	int angle_i, sAngle_i;
	int brokenDrifts, calcCycles, spinouts, runouts, slowspeed;
	int iMeter_state = -1; //current "position" of impression meter

	Slider angleMeter;
	int angleMeterGroup;

	String scoreGainString = "";
	String impressionString;
	int angle, impression;

	Text lap_txt, score_txt, targetscore_txt, imeter_txt;

	finishWindowDrifting fWindow;

	public Drifting() {}

	public void init()
	{
		name = "Drifting";
		shieldIcon = new ResourceRef(sl.Scripts.game.Gamemodes.stdpack_231:0x1004r);
	}

	public void launch()
	{
		useTimers = 0;
		handleCollisions = 1;

		res_riSplitter = new ResourceRef(frontend:0xD125r);

		getOSD(); //get instance of OSD from Track()
		if(gmcEvent)
		{
			laps = gmcEvent.specialSlots[0];
			targetScore = gmcEvent.specialSlots[4];
			
			if(gmcEvent.useAwards)
			{
				//this is needed to call fWindow
				targetScores = new int[3];
				for(int i=0; i<targetScores.length; i++) targetScores[i] = gmcEvent.specialSlots[i+4];
			}
			else
			{
				targetScores = new int[1];
				targetScores[0] = targetScore;
			}
		}

		current_lap = 1;
		rpTriggerSize = gmcEvent.track.rpTriggerSizeCustom;
		setupRacePosCalc(); //for operations with rpTrigger[]
		setupDamageAnalyzer();
		applyTriggers();

		driftTimer = new Timer();
		idleTimer = new Timer();
		breakTimer = new Timer();

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
		if(osd)
		{
			buildAngleMeter(); //must be created first! since it's confined inside the dedicated OSD group

			iMeter = osd.createSlidingMenu(-0.37, 1.315, 0.185, 0.35, 2, SlidingMenu.STYLE_HORIZONTAL); //single sliding menu + adjustable animation (horizontal lock) + styling
			for(int i=0; i<5; i++) iMeter.addItem(0.2, 0.35, 2, frontend:0x0000C10Dr, frontend:0x0000C20Dr, 0, null);
			iMeter.teleport(0.05,10,1,"Y"); //slide down impression meter to show it up

			riSplitter = osd.createRectangle( 2.67, 0.81, 0.32, 0.036, 1, res_riSplitter, 0 );
			riSplitter.setupAnimation(0.1, 10, -1, "X"); //direction: left (-1)
			riSplitter.runThread(); //begin the animation

			lap_txt = osd.createText( "LAP: " + current_lap + "/" + laps, Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.92 );
			targetscore_txt = osd.createText( "TARGET SCORE: " + targetScore, Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.835 );
			score_txt = osd.createText( "SCORE: " + score, Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.75 );

			imeter_txt = osd.createText( "IMPRESSION LEVEL", Frontend.smallFont, Text.ALIGN_LEFT, -0.14, -0.75 );

			lap_txt.changeColor(0x00000000);
			score_txt.changeColor(0x00000000);
			targetscore_txt.changeColor(0x00000000);
			imeter_txt.changeColor(0x00000000);

			lap_txt.fadeIn();
			targetscore_txt.fadeIn();
			imeter_txt.fadeIn();

			score_txt.setupAnimation(0x0FFFFFFF, 8, 1);
			score_txt.runThread();

			osd.show();
		}
	}

	public void buildAngleMeter()
	{
		Style sld_bar = new Style(0.5, 0.09, Frontend.mediumFont, Text.ALIGN_LEFT, new ResourceRef(frontend:0x0000D165r));
		Style sld_knob =  new Style(0.04, 0.06, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(frontend:0x0000D166r));
		Style menuStyle = new Style(0.45, 0.12, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);

		Menu m = osd.createMenu( menuStyle, 0.596, -0.600, 0 );
		m.setSliderStyle( sld_bar, sld_knob );

		angleMeter = m.addItem(null, 0, 0, -134.0, 134.0, 0, null);

		angleMeterGroup = osd.endGroup();
		osd.showGroup (angleMeterGroup);
	}

	public void hideOSD()
	{
		riSplitter.restartAnimation("X");
		riSplitter.setupAnimation(0.1, 10, 1, "X"); //direction: opposite, right (1)
		riSplitter.runThread(); //begin the animation

		lap_txt.restartAnimation();
		lap_txt.fadeOut();

		score_txt.restartAnimation();
		score_txt.fadeOut();

		targetscore_txt.restartAnimation();
		targetscore_txt.fadeOut();

		imeter_txt.restartAnimation();
		imeter_txt.fadeOut();

		iMeter.hide();
		osd.hideGroup (angleMeterGroup);
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

		if(rpTrigger)
		{
			for(int l=0; l<rpTrigger.length; l++)
			{
			        addNotification( rpTrigger[l].trigger, EVENT_TRIGGER_ON, EVENT_SAME, null, "handleRacePos" );
			}
		}
	}

	public void setupDamageAnalyzer()
	{
		chassisDamage = GameLogic.player.car.chassis.getDamage();
		damageList = getBodyPartDamage();
	}

	//damage analyzer: get damage for each bodypart
	public int[] getBodyPartDamage()
	{
		Vehicle car = GameLogic.player.car;

		int[] list = new int[countBodyParts()];
		int j;
		for(int i=car.chassis.attachedParts.size()-1; i>=0; i--)
		{
			Part p = car.chassis.attachedParts.elementAt(i);
			if(p && p instanceof BodyPart)
			{
				list[j] = p.getDamage();
				j++;
			}
		}
		return list;

		return null;
	}

	//damage analyzer: get amount of bodyparts installed on chassis
	public int countBodyParts()
	{
		Vehicle car = GameLogic.player.car;

		int count;
		if(car.chassis)
		{
			for(int i=car.chassis.attachedParts.size()-1; i>=0; i--)
			{
				Part p = car.chassis.attachedParts.elementAt(i);
				if(p && p instanceof BodyPart) count++;
			}

			return count;
		}

		return 0;
	}

	public void beginRace() //here the race actually begins
	{
		unlockCars();
		setEventMask( EVENT_CURSOR|EVENT_COMMAND|EVENT_HOTKEY|EVENT_TRIGGER_ON|EVENT_TRIGGER_OFF|EVENT_TIME|EVENT_COLLISION );
		addNotification(GameLogic.player.car, EVENT_COLLISION, EVENT_SAME, null, "handleCollision");

		addTimer(4.0, 5); //launch automatic updater for impression meter
		raceActive = 1;
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	//if player has "legally" interrupted his single drift (i.e. didn't hit/crash/runout)
	public void seekDrift()
	{
		if(isDrifting > 0)
		{
			isDrifting = -1;
			scoreGain = 0;
//			debug("seeking...");

			if(driftTimer.activated)
			{
				combinedDriftTime += singleDriftTime;
				driftTimer.stop();
				singleDriftTime = 0;
			}

			if(!idleTimer.activated)
			{
				idleTimer = new Timer();
				idleTimer.start();
			}
		}
	}

	//4sec timeout for all "broken" drifts
	public void breakDrift(int valid)
	{
		isDrifting = 0;

		if(!breakTimer.activated)
		{
			breakTimer = new Timer(4.0f);
			breakTimer.start();

			if(valid && combinedDriftTime > 1.0) //"good" drifts
			{
				time_i += combinedDriftTime;
				brokenDrifts++;
			}
		}
	}

	public void breakDrift() {breakDrift(0);} //"bad" drifts

	//discard all gained score and stop drift timer
	public void discardDrift()
	{
		scoreGain = 0;
		scoreGainCombined = 0;
		singleDriftTime = 0.0f;
		combinedDriftTime = 0.0f;
		driftTimer.stop();
	}

	public void updateOffroad()
	{
		int assigned;

		if(isDrifting != 0) //drifting was in progress (single drift/seeking)
		{
			if(isOffroad() && combinedDriftTime > 0.25)
			{
				if(gTrack) gTrack.showMsgBox("OFFROAD DRIVING! SCORE LOST", Track.MBOX_RED, Track.MBOX_MID );
				breakDrift(); //this drift will not be scored
				discardDrift();
				runouts++;

				offroadState = 1;
				assigned++;
			}
		}
		else
		{
			//drifing ended, now check again where is player
			if(!isOffroad())
			{
				offroadState = 0; //player returned back to the track, now we can score his drifting again
				assigned++;
			}
		}

		if(!assigned)
		{
			if(isOffroad()) offroadState = -1; //player was not drifting yet, but still stuck somewhere out of track, so we cannot score him
			else offroadState = 0;
		}
	}

	public void calcAngle()
	{
		float cc = GameLogic.player.car.getOri().y;
		float hb = gTrack.hBone.getOri().y;

		float result = Math.rad2deg((cc-hb)*1.35);
		if(Math.abs(result) > 360) result = Math.rad2deg((cc+hb)*1.35); //abs value may be 400+ degrees, so there is also a special fix included

		angle = result;
//		gTrack.setMessage2("car.ori.y: " + Math.rad2deg(GameLogic.player.car.getOri().y) + ", hBone.ori.y: " + Math.rad2deg(gTrack.hBone.getOri().y) + ", angle: " + angle);
	}

	public int getSteerFactor()
	{
		if(isDrifting < 0) return 30; //a bit less agressive, if drift is in progress

		return 40;
	}

	public int spinOut()
	{
		if(Math.abs(angle) >= 115) return 1;

		return 0;
	}

	//process parameters for impression meter
	public void captureStats()
	{
		//time_i is being retrieved in breakDrift()
		speed_i += GameLogic.player.car.getSpeed();
		angle_i += Math.abs(angle);
		sAngle_i += Math.abs(GameLogic.player.car.chassis.getSteer());
		calcCycles++;

		//gTrack.setMessage2("CS: " + "time_i " + time_i + ", angle_i " + angle_i + ", sAngle_i " + sAngle_i + ", speed_i" + speed_i);
		//gTrack.setMessage2("brokenDrifts: " + brokenDrifts + ", crashes: " + crashes + ", runouts: " + runouts + ", spinouts: " + spinouts + ", falloffs: " + falloffs + ", hits: " + hits + ", slowspeed: " + slowspeed);
	}

	//transfer all points for combined drift to player's score and update related text
	public void updateScore()
	{
		score += scoreGainCombined;
		score_txt.changeText("SCORE: " + score);
	}

	//handle animated impression meter
	public void updateImpMeter()
	{
		int imp_mul = impression/20;

		if(imp_mul > iMeter_state)
		{
			if(iMeter_state == 0) iMeter.activate(0);
			else iMeter.execute(Osd.CMD_MENU_RG);
			iMeter_state++;
		}

		if(imp_mul < iMeter_state)
		{
			if(iMeter_state > 1) iMeter.execute(Osd.CMD_MENU_LF);
			else iMeter.deactivate();
			iMeter_state--;
		}
	}

	public void handleCollision( GameRef obj_ref, int event, String param )
	{
		if(!destroy)
		{
			int h,s,f,c;
			int damageFactor = 3.39*Config.player_damage_multiplier; //this value is used to estimate severity of damage

			int[] compareList; //bodypart damage _after_ crash
			compareList = getBodyPartDamage();

			if(compareList.length != damageList.length) falloffs++; //if player has lost some parts on his car
			else
			{
				for(int i=0; i<damageList.length; i++)
				{
					int diff = damageList[i] - compareList[i];
					if(diff > damageFactor) h++; //typical damage caused by hitting something - registering a hit
					else s=1; //otherwise it's just a scratch
				}
			}

			//now update bodypart damage list
			damageList = new int[compareList.length];
			for(int i=0; i<compareList.length; i++) {damageList[i] = compareList[i];}

			//if even a chassis got involved in a collision, we register a crash
			int diff = chassisDamage - GameLogic.player.car.chassis.getDamage(); //compare chassis damage before and after collision
			if(diff > damageFactor) c++;
			chassisDamage = GameLogic.player.car.chassis.getDamage(); //update stored damage for chassis

			//finally, update damage statistics for this gamemode
			if(s) scratches++;
			hits += h;
			falloffs += f;
			crashes += c;

			if(isDrifting !=0)
			{
				if(h || c || f)
				{
					if(gTrack) gTrack.showMsgBox("CAR HIT! SCORE LOST", Track.MBOX_RED, Track.MBOX_MID );
					breakDrift(); //this drift will not be scored
					discardDrift();
				}
			}

//			debug("scratches: " + scratches + ", hits: " + hits + ", falloffs: " + falloffs + ", crashes: " + crashes);
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
					fWindow = new finishWindowDrifting(this, targetScores);
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
			if( param == 5 ) //update impression meter
			{
				updateImpMeter();
				if(isDrifting == 0) impression -= (impression/100)*5; //also decrease impression level by 5% if player does no drifts
				if(raceActive) addTimer(4.0, 5); //4sec tick
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
							gTrack.createHelperBone(); //this object is used to calculate vehicle's angle when drifting
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
					gTrack.showMsgBox("CHECKPOINT: " + score + " PTS", Track.MBOX_GREEN, Track.MBOX_MID ); //more info?
					nextTrigger[0]++;
				}
				else
				{
					if((current_lap+1) <= gmcEvent.specialSlots[0]) //slot 0: laps count
					{
						gTrack.showMsgBox("LAP " + current_lap + ": " + score + " PTS", Track.MBOX_GREEN, Track.MBOX_SHORT ); //more info?
						current_lap++;

						lap_txt.changeText("LAP: " + current_lap + "/" + laps);
						nextTrigger[0] = 0;
					}
					else //player ends race
					{
						nextTrigger[0] = 0;
						gmThread.execute(7); //show up finish window
					}

					//finally, reset valid trigger data
					rpValidTrigger = -1;
					validScoreZone = -1;
				}
			}
		}
	}

	public void handleRacePos( GameRef obj_ref, int event, String param )
	{
		int id = param.token(0).intValue();

		if(raceActive && rpTrigger)
		{
			int temp = 0.85*cpTrigger.length; //detect end of round with checkpoints

			if(id == GameLogic.player.car.id()) //player pass racepos trigger
			{
				for(int i=0; i<rpTrigger.length; i++)
				{
					if(rpTrigger[i].trigger.id() == obj_ref.id())
					{
						//gTrack.setMessage2("rpValidTrigger: " + rpValidTrigger);

						//also checking difference between current trigger and a valid one, if it's too high, then the player is passing through the start line
						if((i > rpValidTrigger && (Math.abs(i-rpValidTrigger) < rpTrigger.length/2)))
						{
							validScoreZone = 1;
							rpValidTrigger = i;
						}
						else
						{
							//in this case valid trigger is not assigned yet, so it has default -1 value (player just started the race or passed a lap)
							if(rpValidTrigger == -1 || (i == 0 && rpValidTrigger == 0)) //finish line patch, may cause bugs on some tracks!!
							{
								validScoreZone = 1;
								rpValidTrigger = 0;
							}
							else validScoreZone = 0;
						}

						//all the general rpTrigger stuff goes below
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
				//THOR methods (in-race)
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

				calcAngle();
				if(angleMeter) angleMeter.setValue(angle);

				//primary check: if player is in a valid zone to be scored and if he does the drifting maneuvers
				if(validScoreZone)
				{
					if(offroadState == 0)
					{
						if(GameLogic.player.car.getSpeed() > 8.333) //8.333m/s ~ 30KPH
						{
							int detect;

							if(!spinOut() && !breakTimer.activated)
							{
								if(angle > 0)
								{
									if(GameLogic.player.car.chassis.getSteer() < getSteerFactor()*(-1))
									{
										isDrifting = 1;
										driftDirection = 1;
										detect++;
									}
								}
								else
								{
									if(GameLogic.player.car.chassis.getSteer() > (getSteerFactor()))
									{
										isDrifting = 1;
										driftDirection = -1;
										detect++;
									}
								}
							}

							if(!detect) seekDrift();
						}
					}
				}

				//secondary check: if gaining score is in progress
				if(isDrifting > 0 && offroadState == 0)
				{
					if(!driftTimer.activated)
					{
						driftTimer = new Timer();
						driftTimer.start();
					}

					if(idleTimer.activated) idleTimer.stop();

					singleDriftTime = driftTimer.time;
//					debug("drift time: " + singleDriftTime + ", angle: " + angle);
//					debug("scoreGain: " + scoreGain + ", combined: " + scoreGainCombined);

					//calculating score now
					if(!Integrator.frozen && !Integrator.IngameMenuActive)
					{
						//first of all, process impression meter calculations
						captureStats(); //collect drift info

						int imp_old, imp_calc;
						int cc, delta, style, accuracy;
						imp_old = impression;

						//coeffs represent weight of each value in the entire calculation
						if(calcCycles) cc = calcCycles; else cc = 1;
						float calc_time = 0.0; if(brokenDrifts) calc_time = time_i/brokenDrifts;
						style = (crashes*2.0) + (runouts*1.5) + (spinouts*2.0) + (hits*1.0) + (falloffs*3.5) + (scratches*0.5) + (slowspeed*0.25);
						accuracy = ((calc_time*2.5) * ((angle_i/cc)*4.0) + ((sAngle_i/cc)*2.0) + ((speed_i/cc)*3.0))/150;
						imp_calc = (accuracy - style)*2.5;

						delta = imp_calc - imp_old;
						impression += delta;

						if(impression < 0) impression = 0;
						if(impression > 100) impression = 100;

						//gTrack.setMessage2("style: " + style + ", accuracy: " + accuracy + ", impression: " + impression + ", delta: " + delta);
						//gTrack.setMessage2("brokenDrifts: " + brokenDrifts + ", crashes: " + crashes + ", runouts: " + runouts + ", spinouts: " + spinouts + ", falloffs: " + falloffs + ", hits: " + hits + ", slowspeed: " + slowspeed);

						//now process score calculation and involve impression in it
						int af = Math.abs(angle);
						int as = Math.abs(GameLogic.player.car.chassis.getSteer());
						float ts = driftTimer.time;
						float tc = combinedDriftTime;
						float s = GameLogic.player.car.getSpeed();
						float h = (Config.player_steeringhelp + ((Config.player_abs+Config.player_asr)/2))/2;

						int result = ((ts*(7*(1-h))) * ((s/5)*af/50)) * (as/60) + tc/4;
						int imp_mul = impression/20; //impression multiplier
						result = (result/10) * imp_mul;
						if(result < 1) result++;
						scoreGain += result;
						scoreGainCombined += result;

						String i;
						if(!i)
						{
							int sgc = scoreGainCombined;
							if(sgc >= 250) i = "NOT BAD!";
							if(sgc >= 500) i = "GOOD DRIFTING!";
							if(sgc >= 1000) i = "GREAT DRIFTING!";
							if(sgc >= 5000) i = "IMPRESSIVE!";
							if(sgc >= 10000) i = "BADASS!";
							if(sgc >= 20000) i = "INSANE!";
							if(sgc >= 50000) i = "IMPOSSIBLE!";
							if(sgc >= 100000) i = "MANIAC!";
							if(sgc >= 250000) i = "HOLY SHIT!";
							if(sgc >= 500000) i = "HOW IN HELL??!!!";
							if(sgc >= 1000000) i = "DAMN IT!!";
						}

						if(!i) i = "NICE TRY!";
						impressionString = i;
					}

					if(scoreGainString != "") imeter_txt.changeText(scoreGainString);
					scoreGainString =  impressionString + " " + scoreGainCombined + "PTS";
				}

				//extended check: include seeking
				if(isDrifting !=0)
				{
					if(GameLogic.player.car.getSpeed() <= 11.111) //11.111m/s ~ 40KPH
					{
						if(combinedDriftTime > 1.0) slowspeed++;
						updateScore();
						breakDrift(1); //drift is accepted, but slow speed will decrease impression level, so less score will be gained
						discardDrift();
						if(gTrack) gTrack.showMsgBox("DRIVE FASTER!", Track.MBOX_RED, Track.MBOX_SHORT );
					}

					if(spinOut())
					{
						spinouts++;
						updateScore();
						breakDrift(1); //this drift is also scored, but it's ended up with spinout and this will dramatically decrease impression level
						discardDrift();
					}
				}

				//if seeking timer is up
				if(idleTimer.activated)
				{
					if(idleTimer.time >= 1.5)
					{
						updateScore();
						breakDrift(1); //drift ended perfectly, full score is gained
						discardDrift();
//						gTrack.setMessage2("TIMEOUT!");
					}
				}

				//drift is ended, performing final actions
				if(isDrifting == 0)
				{
					if(idleTimer.activated) idleTimer.stop();
					if(scoreGainString != "")
					{
						impressionString = "";
						scoreGainString = "";
						scoreGainCombined = 0;

						imeter_txt.changeText("IMPRESSION LEVEL");
					}

//					debug("drift ended");
				}

				//and also check where is the player
				updateOffroad();

//				gTrack.setMessage2("iMeter_state: " + iMeter_state + "; impression: " + impression);
//				gTrack.setMessage2("driftTimer.time: " + driftTimer.time);
//				gTrack.setMessage2("combined drift time: " + combinedDriftTime);
//				gTrack.setMessage2("rpValidTrigger: " + rpValidTrigger + ", rpNextTrigger[0]: " + rpNextTrigger[0]);
//				gTrack.setMessage2("car angle: " + angle);
//				gTrack.setMessage2("steer: " + GameLogic.player.car.chassis.getSteer()); //steering direction
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
		nextTrigger[0] = 0;
		raceActive = 0;
		clearScreen(); //hide all msgBoxes, OSD stuff
		finishRace(0); //show up race end message (race finished)
		addTimer(1, 12);
		addTimer(2, 11);
	}
}

//individual window for showing up race results
public class finishWindowDrifting extends GameType
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
	Text[]	f_scores; //score, target score

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

	int targetScore, score;
	int finishPos; //like 1st/2nd/3rd etc.
	int highlight;
	String cupPostfix;

	SfxRef bckLoop;

	int debugMode = 0;

	public finishWindowDrifting(Gamemode gm, int[] tScore)
	{
		game = gm;

//--------------begin collect GM data

		score = game.score;
		finishPos = 0; //initial pos

		if(game.gmcEvent.useAwards)
		{
			for(int i=0; i<tScore.length; i++)
			{
				targetScore = tScore[i];

				if(score >= targetScore) i = tScore.length; //break
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
			targetScore = tScore[0];

			if(score >= targetScore) finishPos = 0;
			else finishPos = 3;
		}

//--------------GM data collected

		if(debugMode) traceDebug();

		osd = new Osd(1.0, 0.0, 20);
		osd.show();
		osd.createHotkey( Input.RCDIK_ENTER, Input.KEY|Osd.HK_STATIC, 0x01, this );
		f_scores = new Text[maxlines];
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

		prepareNames[0] = "YOUR SCORE: " + score + "PTS";
		if(game.gmcEvent.useAwards) prepareNames[1] = "GOAL (" + cupPostfix + "): " + targetScore + "PTS";
		else prepareNames[1] = "TARGET SCORE: " + targetScore + "PTS";
		
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
		f_scores[line] = osd.createText(data, Frontend.largeFont_strong, Text.ALIGN_LEFT, xpos, ypos );
		separator[line] = osd.createRectangle( xpos_ls-deltaDist, ypos_ls, 0.395, 0.004, 2, res_separator, 0 );
		f_scores[line].changeColor(0x00000000);
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
			for(int i=0; i<f_scores.length; i++)
			{
				if(f_scores[i])
				{
					f_scores[i].restartAnimation();
					f_scores[i].a_speed = 1;
					f_scores[i].fadeOut(0xF0000000); //from black to 100% transparent
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
		System.log("Drifting: no debug data provided");
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
				for(int i=0; i<f_scores.length; i++)
				{
					if(f_scores[i])
					{
						if(i != highlight) f_scores[i].fadeIn(0xFF000000); //to solid black
						else f_scores[i].fadeIn(0xFFFF0000); //to solid red
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