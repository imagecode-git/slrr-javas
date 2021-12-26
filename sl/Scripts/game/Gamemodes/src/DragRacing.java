package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.sound.*;

public class DragRacing extends Gamemode
{
	Rectangle posBar;
	Rectangle[] posMarker;

	ResourceRef res_posBar;
	ResourceRef[] res_posMarker;

	Text racetime_txt, besttime_txt;

	DragLane[] lane;
	int[] finished;
	int detectFalseStart = 0; //pre-race detection

	finishWindowDragRacing fWindow;

	public DragRacing(){}

	public void init()
	{
		name = "Drag Racing";
		shieldIcon = new ResourceRef(sl.Scripts.game.Gamemodes.stdpack_231:0x1002r);
	}

	public void launch()
	{
		useTimers = 1;

		res_posMarker = new ResourceRef[2];
		res_posMarker[0] = new ResourceRef(frontend:0x0000D16Ar); //player, non-transparent
		res_posMarker[1] = new ResourceRef(frontend:0x0000D16Br); //bot, semi-transparent
		res_posBar = new ResourceRef(frontend:0x0000D169r); //long vertical holder for markers
		res_riSplitter = new ResourceRef(frontend:0xD125r);

		getOSD(); //get instance of OSD from Track()

		if(gmcEvent.racers && gmcEvent.botData.size()) //find opponents
		{
			gBot = new Bot[gmcEvent.racers];
			for(int i=0; i<gmcEvent.racers; i++) gBot[i] = new Bot(gmcEvent.botData.elementAt(i).intValue()); //pick bots from the database
		}

		generateLanes(); //generate drag racing paths ('lanes') for all racers before placing bots on the map

		if(gBot && gBot.length)
		{
			createBotCars(); //prepare cars, track will find them and place on the start grid
			joinBotCars(); //connect bots with the cars
		}

		applyTriggers();
		finished = new int[calcArray()];

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
			posMarker = new Rectangle[2];
			posBar = osd.createRectangle( -2.225, 0.0, 0.07, 1.625, 1, res_posBar, 0 );
			for(int i=0; i<2; i++) posMarker[i] = osd.createRectangle( -1.631, -0.8, 0.03515, 0.06152, 2, res_posMarker[i], 0 );

			posBar.runAnimation(0.025, 10, 1, "X"); //direction: right (1)
			for(int i=0; i<posMarker.length; i++) posMarker[i].runAnimation(0.01, 10, 1, "X"); //direction: right (1)

			riSplitter = osd.createRectangle( 2.67, 0.91, 0.32, 0.036, 1, res_riSplitter, 0 );
			riSplitter.runAnimation(0.1, 10, -1, "X"); //direction: left (-1)

			racetime_txt = osd.createText( "RACE TIME: " + String.timeToString( 0.0f, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.92 );
			racetime_txt.changeColor(0x00000000);
			racetime_txt.fadeIn();

			bestTime = GameLogic.player.getTrackData(gmcEvent.track.name);
			besttime_txt = osd.createText( "BEST TIME: " + String.timeToString( bestTime, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_LEFT, 0.605, -0.835 );
			besttime_txt.changeColor(0x00000000);
			besttime_txt.setupAnimation(0x0FFFFFFF, 8, 1);	besttime_txt.runThread();

			osd.show();
		}
	}

	public void hideOSD()
	{
		posBar.runAnimation(0.025, 10, -1, "X"); //direction: opposite, left (-1)
		for(int i=0; i<2; i++) posMarker[i].runAnimation(0.01, 20, -1, "X"); //direction: opposite, left (-1)

		riSplitter.restartAnimation("X");
		riSplitter.runAnimation(0.1, 10, 1, "X"); //direction: opposite, right (1)

		racetime_txt.restartAnimation();
		racetime_txt.fadeOut();

		besttime_txt.restartAnimation();
		besttime_txt.fadeOut();
	}

	public void clearScreen()
	{
		gTrack.clearMsgBoxes();
		gTrack.lockMsgBoxes();
		gTrack.enableOsd(0);
		hideOSD();
	}

	//drag racing track must have enough lanes to pass generation successfully!
	public void generateLanes()
	{
		Vector temp = new Vector();
		Vector pori = new Vector();
		lane = new DragLane[calcArray()];

		for(int i=0; i<calcArray()*2; i++)
		{
			pori.addElement(new Pori(gTrack.startGridData.elementAt(i), gTrack.startGridData.elementAt(i+1)));
			i+=1;
		}

		TrackData td = gmcEvent.track;
		for(int i=0; i<calcArray(); i++) temp.addElement(new DragLane(td.splinesAI.elementAt(i), i, pori.elementAt(i), td.checkpoints.elementAt(i)));

		//attempting to swap lanes
		temp.shuffle();
		temp.shuffle();
		temp.shuffle();

		//regenerating lanes and updating start grid
		for(int j=0; j<temp.size(); j++)
		{
			lane[j] = temp.elementAt(j);
			gTrack.startGridData.setElementAt(lane[j].start.pos, j*2);
			gTrack.startGridData.setElementAt(lane[j].start.ori, (j*2)+1);
		}
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

	//only one player VS one bot supported
	public void applySplines()
	{
		if(gBot && gBot[0] && GameLogic.player.car)
			gBot[0].followSplineTrack( Math.random()*3, lane[1].spline, GameLogic.player.car.id() );
	}

	//false start detection
	public int checkFalseStart()
	{
		//step 1: collect all start grid 2D pos
		int idx;
		int l1 = gTrack.startGridData.size();
		float[] ax = new float[l1/2];
		float[] az = new float[l1/2];
		for(int i=0; i<l1; i++)
		{
			ax[idx] = gTrack.startGridData.elementAt(i).x;
			az[idx] = gTrack.startGridData.elementAt(i).z;
			idx++;
			i++;
		}

		//step 2: collect all finish line 2D pos
		int l2 = gmcEvent.track.checkpoints.size();
		float[] bx = new float[l2];
		float[] bz = new float[l2];
		for(int j=0; j<l2; j++)
		{
			bx[j] = gmcEvent.track.checkpoints.elementAt(j).x;
			bz[j] = gmcEvent.track.checkpoints.elementAt(j).z;
		}

		//step 3: calculate middle line between all drag lanes
		Vector3 p0 = new Vector3(Math.avg(ax), 0, Math.avg(az));
		Vector3 p1 = new Vector3(Math.avg(bx), 0, Math.avg(bz));

		//step 4: check distance travelled by player along that middle line
		if(GameLogic.player.car)
		{
			if(getSplineDistanceSegment(p0, p1, GameLogic.player.car.getPos()) > gmcEvent.track.falseStartDist) return 1; //travelled distance is too long, false start detected
		}

		return 0;
	}

	//utilize traffic lights on track
	public void updateLightStand(int status)
	{
		gmcEvent.track.setTrafficLight(gTrack.map, status);
	}

	//2D position marker refresher
	public void updatePosition()
	{
		float[] d_total = new float[calcArray()]; //total distance from start to finish
		float[] d_est = new float[calcArray()]; //distance left to pass through
		float[] ratio = new float[calcArray()]; //2D/3D distance coefficient
		float barLength = 1.6; //max length of 2D position slider bar, from 0.8 to -0.8

		for(int i=0; i<calcArray(); i++)
		{
			if(!finished[i]) //position will be updated only for those, who didn't finish yet
			{
				d_total[i] = lane[i].start.pos.distance(lane[i].finishPos);
				ratio[i] = barLength/d_total[i];

				Vector3 comp; //3D position of racer
				if(i==0) comp = GameLogic.player.car.getPos();
				else
				{
					if(gBot && gBot[0]) comp = gBot[0].car.getPos();
					else comp = new Vector3(0);
				}
				d_est[i] = comp.distance(lane[i].finishPos);

				Vector3 mpos = posMarker[i].pos;
				float addpos = barLength - d_est[i]*ratio[i];

				//fixing addpos to prevent visual bugs
				if(addpos > barLength) addpos = barLength;
				if(addpos < 0) addpos = 0;
				
				posMarker[i].setPos(new Vector3(mpos.x, -0.8+addpos, mpos.z)); //finally, update position for each marker
			}
		}
	}

	public void beginRace() //here the race actually begins
	{
		unlockCars();
		applySplines();
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
					raceActive = 0; //let bots try to pass finish line before the race results will show up
					fWindow = new finishWindowDragRacing(this, finished);
					fWindow.show();
				}
			}
			else
			if( param == 10 ) //race end window destruction timer (false start)
			{
				if(erWindow.windowStatus != -1)
				{
					erWindow.hide();
					addTimer(1.0, 10); //repeat this check until erWindow will return a 'destruction-ready' state
				}
				else unlaunch(); //exit from gamemode
			}
			else
			if( param == 4 ) //a 3..2..1 event
			{
				if(gTrack && detectFalseStart >= 0)
				{
					if( countDown )
					{
						if( countDown == 3 )
						{
							detectFalseStart = 1;
							lockCars();
							unlockPlayerCar(); //player's vehicle will stay unlocked
							prepareOSD(); //show OSD stuff
							updateLightStand(1); //1st orange lane

							new SfxRef(Gamemode.SFX_HORN_1).play();
							addTimer(1, 4);
						}
						else 
						if( countDown == 2 )
						{
							new SfxRef(Gamemode.SFX_HORN_1).play();
							updateLightStand(2); //2nd orange lane
							addTimer(1, 4);
						}
						else 
						if( countDown == 1 )
						{
							if(gBot) prepareBots(1); //rev up
							new SfxRef(Gamemode.SFX_HORN_1).play();
							updateLightStand(3); //3rd orange lane + red lights
							addTimer(1, 4);
						}
						gTrack.showMsgBox(countDown, Track.MBOX_RED, Track.MBOX_SHORT );
						countDown--;
					}
					else
					{
						new SfxRef(Gamemode.SFX_HORN_2).play();
						updateLightStand(4); //light up green lights and switch orange lights off
						detectFalseStart = 0;
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

		if(raceActive)
		{
			//player checkpoint handling
			if( id == GameLogic.player.car.id() )
			{
				if(GameLogic.player.car && !finished[0])
				{
					//update best time if needed
					if(time[0] < bestTime || !bestTime)
					{
						bestTime = time[0];
						GameLogic.player.setTrackData(gmcEvent.track.name, bestTime);
					}

					stopTimer(0); //stop timer for player
					GameLogic.player.car.command("brake");
					gmThread.execute(7); //show up finish window
					finished[0]++;
				}
			}

			if(gBot && gBot.length)
			{
				for(int i=0; i<gBot.length; i++)
				{
					if( id == gBot[i].car.id() && !finished[i+dlt]) //bot checkpoint handling
					{
						if(gBot[i].car)
						{
							stopTimer(i+dlt);
							gBot[i].car.command("brake");
							gTrack.showMsgBox( gBot[i].profile.name_f + " " + gBot[i].profile.name_s + " HAS FINISHED RACE!", Track.MBOX_YELLOW, Track.MBOX_LONG );
							finished[i+dlt]++;
						}
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

				hideGPSFrame();
				updateLightStand(0); //show up STAGE lights
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

			if(gmThread.methodStatus(1) == 1) //false start
			{
				Sound.changeMusicSet(Sound.MUSIC_SET_NONE);
				raceActive = 0;
				detectFalseStart = -1;
				clearScreen(); //hide all msgBoxes, OSD stuff

				finishRace(5); //now show up race end message (false start)
				stopCars();
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

				finishRace(0); //show up race end message (race finished)
				addTimer(2, 12); //2 sec timer for braking down; better to replace this with the follow spline
				addTimer(4, 11); //4 sec timer for fWindow
				gmThread.controlMethod(7,-1);
			}

			if(detectFalseStart > 0)
			{
				if(checkFalseStart()) gmThread.execute(1);
				updatePosition(); //player can drive before the race begins, so we need to update pos marker for him
			}

			if(raceActive)
			{
				updateTimers();
				updatePosition(); //pos markers
				racetime_txt.changeText( "RACE TIME: " + String.timeToString( raceTime[0], String.TCF_NOHOURS ) );

				for(int i=0; i<gBot.length; i++)
				{
					if(gBot[i].car) gBot[i].car.chassis.setTorque(2); //increases engine power for bots
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
		clearScreen(); //hide all msgBoxes, OSD stuff
		finishRace(0); //show up race end message (race finished)
		addTimer(1, 12);
		addTimer(2, 11);
	}
}

//special service object
public class DragLane
{
	String spline;
	Pori start;
	Vector3 finishPos;
	int gridID;

	public DragLane();

	public DragLane(String spl, int gID, Pori pStart, Vector3 posFinish)
	{
		if(spl) spline = spl;
		start = pStart;
		finishPos = posFinish;
		gridID = gID;
	}
}

//individual window for showing up race results
public class finishWindowDragRacing extends GameType
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
	ResourceRef res_titlePosFont;

	int titles_resid	     = frontend:0xD145r; //title_lose.png
	int titlePosFont_resid	     = frontend:0x10C2r; //title_pos_1st.png

	Rectangle   holder, bckFlags, splitter, titles, titlePosFont;
	Rectangle[] separator;

	Vector finishedNames = new Vector(); //vector instead of array to force rank indices match the indices from elements of finishedNames
	String[] notFinished;

	Rank rating;
	Vector timesRaw, timesArranged, rankIndices;

	int[] finished;
	int finishPos; //like 1st/2nd/3rd etc.
	int highlight;

	SfxRef bckLoop;

	int debugMode = 0;

	public finishWindowDragRacing(Gamemode gm, int[] fn)
	{
		game = gm;
		finished = fn;

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
				if(finished[i+dlt])
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
		if(finishPos == 0) checkWin = 1;

		res_titles = new ResourceRef(titles_resid+checkWin);
		res_titlePosFont = new ResourceRef(titlePosFont_resid+finishPos);

		holder = osd.createRectangle( 0.0-deltaDist_frame, 0.0, 2.0, 0.960, 1, res_holder, 0 );
		bckFlags = osd.createRectangle( 0.835, -0.045, 0.635, 0.425, 2, res_bckFlags, 0 );
		results_title_txt = osd.createText("RACE RESULTS", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.475 );
		results_title_txt.changeColor(0x00000000);
		splitter = osd.createRectangle( 0.0-deltaDist, 0.345, 0.25, 0.0325, 2, res_splitter, 0 );
		titles = osd.createRectangle( 0.63+deltaDist, 0.020, 0.28, 0.120, 3, res_titles, 0 );
		titlePosFont = osd.createRectangle( 0.0, 0.020, 0.15, 0.150, 3, res_titlePosFont, 0 );

		if(checkWin == 1) //race won
		{
			prize_txt = osd.createText("", Frontend.largeFont_strong, Text.ALIGN_CENTER, 0.5, 0.375 );
			prize_txt.changeColor(0x00000000);
			prize_txt.changeText("YOUR PRIZE: " + game.gmcEvent.getPrizeName());
		}

		pressenter_txt = osd.createText("", Frontend.largeFont_strong, Text.ALIGN_CENTER, 0.0, 0.40 );
		pressenter_txt.changeColor(0x00000000);
		pressenter_txt.changeText("PRESS ENTER");

		int line;
		String[] prepareNames = new String[maxlines]; //array for preprocessing line strings

		for(int i=0; i<rankIndices.size(); i++)
		{
			if(line < maxlines)
			{
				prepareNames[line] = (line+1) + ". " + finishedNames.elementAt(rankIndices.elementAt(i).number) + " " + String.timeToString(timesArranged.elementAt(i).number, String.TCF_NOHOURS);
				if(rankIndices.elementAt(i).number == 0) highlight = line;
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