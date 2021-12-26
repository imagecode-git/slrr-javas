//v 1.04
package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.render.osd.dialog.*;	//Text
import java.sound.*;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;

//RAXAT: v2.3.1, lots of improvements

//BUG: if no fog data, no fog will be created!
abstract public class Track extends Scene implements GameState
{
	//ezeket a leszarmazott allitsa be!

	abstract Vector3	posStart;
	abstract Ypr		oriStart;

	//hold the id of the currently active trigger (shop, garage, etc)
	int			activeTrigger;
	Vector		trigger = new Vector();

	GameState	parentState;

	Osd		osd;
	int		osdCounter;

	final public static int	SFX_SMSTART		= frontend:0x0A29r;
	final public static int	SFX_SMLOOP		= frontend:0x0B29r;
	final public static int	SFX_SMSTOP		= frontend:0x0C29r;
	final public static int	SFX_FREEZE		= frontend:0x01B1r;
	final public static int	SFX_UNFREEZE	= frontend:0x01C1r;
	final public static int	SFX_QREPAIR		= frontend:0x01A1r;
	final public static int	SFX_FLIP		= frontend:0x02F1r;

	float	viewRange;
		
	int	sm_enabled;
	int	gpsmap_enabled;

	int	camRotate;

	int	killCar;
	int	musicPlay = 0;
	int	cameraNumWatcher; //RAXAT: it will always tell the system which cam is in use
	GameRef activeCamera; //RAXAT: camera patch, see end section of class code

	String	comments; //RAXAT: for debugSFX

	final static int RID_APPLAUSE1 = sound:0x0021r;

	final static int CMD_CHANGECAM_TV	= 0;
	final static int CMD_INGAMEMENU		= 1;
	final static int CMD_CAMMOVE		= 3;
	final static int CMD_OPPCAM		= 4;
	final static int CMD_OSDONOFF		= 6;
	final static int CMD_CHANGECAM_INT	= 7;
	final static int CMD_CHANGECAM_EXT	= 8;
	final static int CMD_CRUISECONTROL	= 9;
	final static int CMD_CHANGECAM_CHASE	= 10;
	final static int CMD_CHANGECAM_INVCH	= 11;
	final static int CMD_CHANGECAM_FREE	= 12;
	final static int CMD_SIMSPEEDINC	= 13;
	final static int CMD_SIMSPEEDDEC	= 14;
	final static int CMD_SIMPAUSE		= 15;
	final static int CMD_CAMROTATE		= 16;
	final static int CMD_QUICKREPAIR	= 17;
	final static int CMD_CHANGECAMTARGET	= 18;
	final static int CMD_EXIT		= 19;
	final static int CMD_SM_SWITCH		= 20;
	final static int CMD_FLIP		= 21;

	//RAXAT: for debugging env SFX
	final static int CMD_DSFX_INFO		= 22;
	final static int CMD_DSFX_COMMENT	= 23;
	final static int CMD_DSFX_NIGHTSETUP	= 24;
	
	final static int CMD_CHANGECAM_REARVIEW = 31;

	//RAXAT: debug OSD stuff
	final static int CMD_DOSD_PREV		= 32;
	final static int CMD_DOSD_NEXT		= 33;
	final static int CMD_DOSD_INC		= 34;
	final static int CMD_DOSD_DEC		= 35;
	final static int CMD_DOSD_SETINT	= 36; //changes delta
	final static int CMD_DOSD_SETVAL	= 37;
	final static int CMD_DOSD_GETVAL	= 38;
	final static int CMD_DOSD_INFO		= 39;

	//RAXAT: grid debug
	final static int CMD_DGRD_PREV		= 40;
	final static int CMD_DGRD_NEXT		= 41;
	final static int CMD_DGRD_WRITE		= 42;
	final static int CMD_DGRD_SPAWN		= 43;
	final static int CMD_DGRD_MAXRACERS	= 44;
	final static int CMD_DGRD_SAVE		= 45;

	//RAXAT: minimap debug
	final static int CMD_DMAP_INIT		= 46;
	final static int CMD_DMAP_PRINT		= 47;
	final static int CMD_DMAP_STOP		= 48;
	final static int CMD_DMAP_TURNLEFT	= 49;
	final static int CMD_DMAP_TURNRIGHT	= 50;
	final static int CMD_DMAP_ADJUST_A_INC	= 51;
	final static int CMD_DMAP_ADJUST_A_DEC	= 52;
	final static int CMD_DMAP_ADJUST_B_INC	= 53;
	final static int CMD_DMAP_ADJUST_B_DEC	= 54;

	//RAXAT: remote control debug
	final static int CMD_RC_THROTTLE	= 55;
	final static int CMD_RC_BRAKE		= 56;
	final static int CMD_RC_HANDBRAKE	= 57;
	final static int CMD_RC_CLUTCH		= 58;
	final static int CMD_RC_TURN_LEFT	= 59;
	final static int CMD_RC_TURN_RIGHT	= 60;
	final static int CMD_RC_GEAR_UP		= 61;
	final static int CMD_RC_GEAR_DOWN	= 62;

	//RAXAT: debug hotkey for quick tests
	final static int CMD_DKEY_ACTIVATE	= 63;

	final static int CMD_GPSMAP		= 64;

	Navigator		nav;
	Marker			mPlayer, mCamera;
	static Marker		mRouteS, mRouteF; //RAXAT: transfer markers for route manager
	static Vector3		routeDestPos;
	static Trigger		routeTrigger;
	static int		routeDestTriggerAdded;

	Player			player;
	GameRef			cursor;

	//message display
	Text			messages;
	Text			messages2;
	Text			infoline;
	int			msgtimers;
	int			osdEnabled;

	//cam
	GameRef			cam_external;	//external camera instance
	GameRef			cam;		//currently active camera instance
	GameRef			renderer;
	GameRef			cameraTarget;	//car of the player (usually) or another bot
	GameRef			cameraTarget2;	//another car or object (police, racer, etc.)
	int			cameraMode;
	int			cameraNum;	//ha pl. tobb camera van az adott cameraMode-hoz (pl. belso, de lehet TV, ext, stb. is)

	Vector3			lastCamPos;
	Vector3			lastCrossPos;	//for TV-camera
	
	Viewport		vport2;

	final static int CAMMODE_NONE		= 0;
	final static int CAMMODE_FOLLOW		= 1;
	final static int CAMMODE_TV		= 2;
	final static int CAMMODE_CHASE		= 3;
	final static int CAMMODE_INVCH		= 4;
	final static int CAMMODE_INTERNAL	= 5;
	final static int CAMMODE_FREE		= 6;
	final static int CAMMODE_POINT		= 7;

	Mechanic	mechanic;
	Painter		painter;

	//RAXAT: skydome animation stuff
	float		skydomeRot;
	Vector3		customSkydomePos;

	Map		mapViewer;
	static int	teleported;

	//RAXAT: v2.3.1, start grid integration
	Vector		startGridData = new Vector(); //set order: 3
	Vector3[]	startGrid_XYZ;
	Ypr[]		startGrid_YPR;
	int		maxAIracers; //set order: 1
	Vehicle[]	mapVehicle; //set order: 2

	String		name, e_name;

	//RAXAT: debug switches
	int		debugSFX_enabled = 0; //enables map SFX debug
	int		debugGRD_enabled = 0; //enables start grid debug
	int		debugOSD_enabled = 0; //enables debug OSD setup
	int		debugKey_enabled = 0; //enables quick feature test with a special debug key
	int		debugMap_enabled = 0; //enables debug tools for minimap object
	int		debugRC_enabled  = 0; //enables remote control debug

	int		debugOSD_index = 0;
	float		debugOSD_curval = 0.00000f;
	float		debugOSD_delta = 0.00000f;
	int		debugOSD_maxparams = 10;
	String[]	debugOSD_titles = new String[10]; //RAXAT: same count as maxparams

	Debug		debugSFX_outFile = new Debug("multibot\\debugger\\SFX_info.rdb");
	Vector		debugSFX_nightNodes = new Vector(); //to write night SFX matrices for further transmutation
	int		debugSFX_headerProcessed = 0;
	int		debugSFX_rsdIndex = 0;
	int		debugSFX_maxparams = 14; //max sounds used, apply to all lib arrays
	int[]		debugSFX_markerColors = new int[14]; //to identify the SFX placed
	String[]	debugSFX_resLib = new String[14]; //without extensions!
	String[]	debugSFX_resIdParents = new String[14];
	String[]	debugSFX_resIdChilds = new String[14];

	Debug		debugGRD_outFile = new Debug("multibot\\debugger\\GRD_info.java");
	int		debugGRD_index = 0;
	int		debugGRD_maxAIracersOK = 0; //maxracers can be set only once
	Vector		debugGRD_markers;
	Vector		debugGRD_gridData; //double index: {xyz, ypr}
	Vehicle[]	debugGRD_vehicles; //being initialized if maxracers is set

	int		debugMap_idx = 0;
	float		debugMap_dist = 0.0;
	float		debugMap_dist_init = 0.0;
	float		debugMap_dist_print = -1.0;
	float		debugMap_mode = 0; //0: GPS V4 XYZ vectors; 1: racepos vector elements
	RenderRef	debugMap_startMarker;
	float		debugMap_rotationStep = 0.02; //only for "still" minimaps!
	float		debugMap_adjustStep = 0.25; //only for "still" minimaps!

	Gamemode	game;
	Bot[]		rBot;
	GameRef		dummy;

	//RAXAT: v2.3.1, message box support
	Rectangle[]	msgBox;
	Text[]		boxTxt;
	int[]		boxTimer;
	float[]		boxStatus;
	int[]		busyBox;
	Vector		boxQueue;
	int		holdID = -1;

	//RAXAT: v2.3.1, disable OSD/cam change hotkeys
	int		lockOSD = 0;
	int		lockCam = 0;

	final public static int	MSGBOX_SMALL	= frontend:0xD116r;
	final public static int	MSGBOX_MEDIUM	= frontend:0xD119r;
	final public static int	MSGBOX_LONG	= frontend:0xD11Cr;

	//msgBox length flags
	final public static int	MBOX_SHORT	= 0x0A;
	final public static int	MBOX_MID	= 0x0B;
	final public static int	MBOX_LONG	= 0x0C;

	//msgBox color flags
	final public static int	MBOX_GREEN	= 0x1A;
	final public static int	MBOX_RED	= 0x1B;
	final public static int	MBOX_YELLOW	= 0x1C;

	//RAXAT: for remote control debug
	int	ai_throttle	= -1;
	int	ai_brake	= -1;
	int	ai_handbrake	= -1;
	int	ai_turn		= -1;
	int	ai_gear_up	= -1;
	int	ai_gear_down	= -1;
	int	ai_clutch	= -1;

	int	ai_rc_enabled	= 0;

	//RAXAT: v2.3.1, helper bone, an invisible clone of F3 cam, mostly used by drifting gamemode
	GameRef	hBone;

	public Track()
	{
		enableFog = 1;
		customFog = 0;

		stdInit();
		initDebug();
	}

	//RAXAT: v2.3.1, gamemode support
	public Track( Gamemode gm, CareerEvent ce, TrackData td )
	{
		stdInit();

		game = gm;
		td.getData(); //loading _all_ data since that point!
		map = new GroundRef(td.map_id);
		game.gTrack = this;
		td.dTrack = this;
		if(nav && player.car) nav.updateNavigator(player.car);

		//if no grid will be provided
		posStart = td.posStart;
		oriStart = td.oriStart;

		game.gmcEvent = ce;
		td.getGrid(); //this looks for the grid info in track data
		if(ce.randomStartGrid) randomizeStartGrid(game.calcArray()); //randomize grid if it's required by career event, otherwise gamemode will use normal grid
		game.launch(); //create osd, bots, cars, prepare objects, initialize threads and THOR, etc.

		//weather stuff
		if(game.gmcEvent.fogData[0] && game.gmcEvent.fogData[1])
		{
			enableFog = 1;
			customFog = 1;
			fogVars[0] = game.gmcEvent.fogData[0];
			fogVars[1] = game.gmcEvent.fogData[1];
		}
		else
		{
			if(game && game.useDebug) enableFog = 0;
			customFog = 0;
		}

		initDebug();
	}

	public void stdInit()
	{
		createNativeInstance();
		viewRange = Config.camera_ext_viewrange;

		name = "<unknown track>";
		e_name = "UNKNOWN TRACK";

		player=GameLogic.player;
		if(player) player.handleGPSframe(player.gpsState); //RAXAT: v2.3.1, show/hide GPS frame. Gamemodes may override it when the track is being loaded!

		//RAXAT: v2.3.1, OSD patch
		osd = new Osd();
		osd.menuKeysCreated = 1;

		setupMsgBoxes();
	}

	public void initDebug()
	{
		if( debugOSD_enabled )
		{
			debugOSD_titles[0] = "spd NULL";
			debugOSD_titles[1] = "spd MUL";
			debugOSD_titles[2] = "spd MIN";
			debugOSD_titles[3] = "spd MAX";
			debugOSD_titles[4] = "spd RAD";

			debugOSD_titles[5] = "rpm NULL";
			debugOSD_titles[6] = "rpm MUL";
			debugOSD_titles[7] = "rpm MIN";
			debugOSD_titles[8] = "rpm MAX";
			debugOSD_titles[9] = "rpm RAD";
		}

		if( debugSFX_enabled )
		{
			debugSFX_resLib[0] = "pitstop";
			debugSFX_resLib[1] = "birds_1";
			debugSFX_resLib[2] = "birds_2";
			debugSFX_resLib[3] = "crows_1";
			debugSFX_resLib[4] = "crows_2";
			debugSFX_resLib[5] = "forest";
			debugSFX_resLib[6] = "water_heavy";
			debugSFX_resLib[7] = "water_light";
			debugSFX_resLib[8] = "wind_deep";
			debugSFX_resLib[9] = "wind_extreme";
			debugSFX_resLib[10] = "wind_heavy";
			debugSFX_resLib[11] = "wind_light";
			debugSFX_resLib[12] = "night";
			debugSFX_resLib[13] = "bees";

			debugSFX_resIdParents[0] = "00100A";
			debugSFX_resIdParents[1] = "00100B";
			debugSFX_resIdParents[2] = "00100C";
			debugSFX_resIdParents[3] = "00100D";
			debugSFX_resIdParents[4] = "00100E";
			debugSFX_resIdParents[5] = "00100F";
			debugSFX_resIdParents[6] = "00101A";
			debugSFX_resIdParents[7] = "00101B";
			debugSFX_resIdParents[8] = "00101C";
			debugSFX_resIdParents[9] = "00101D";
			debugSFX_resIdParents[10] = "00101E";
			debugSFX_resIdParents[11] = "00101F";
			debugSFX_resIdParents[12] = "00102A";
			debugSFX_resIdParents[13] = "00102B";

			debugSFX_resIdChilds[0]	= "062012";
			debugSFX_resIdChilds[1] = "062014";
			debugSFX_resIdChilds[2] = "062015";
			debugSFX_resIdChilds[3] = "062016";
			debugSFX_resIdChilds[4] = "062020";
			debugSFX_resIdChilds[5] = "062021";
			debugSFX_resIdChilds[6] = "062022";
			debugSFX_resIdChilds[7] = "062023";
			debugSFX_resIdChilds[8] = "062024";
			debugSFX_resIdChilds[9] = "062025";
			debugSFX_resIdChilds[10] = "062026";
			debugSFX_resIdChilds[11] = "062027";
			debugSFX_resIdChilds[12] = "062028";
			debugSFX_resIdChilds[13] = "062029";

			debugSFX_markerColors[0] = 0xFF000000;  //pitstop, black
			debugSFX_markerColors[1] = 0xFF174000;  //birds_1, dark green
			debugSFX_markerColors[2] = 0xFF3E8100;  //birds_2, green
			debugSFX_markerColors[3] = 0xFFCB4D00;  //crows_1, dark orange
			debugSFX_markerColors[4] = 0xFFFF7817;  //crows_2, heavy orange
			debugSFX_markerColors[5] = 0xFF35D500;  //forest, nuclear green
			debugSFX_markerColors[6] = 0xFF002481;  //water_heavy, dark blue
			debugSFX_markerColors[7] = 0xFF002EC2;  //water_light, sea blue
			debugSFX_markerColors[8] = 0xFF8A00C2;  //wind_deep, violet
			debugSFX_markerColors[9] = 0xFFFA001C;  //wind_extreme, bright red
			debugSFX_markerColors[10] = 0xFFFF2993; //wind_heavy, super pink
			debugSFX_markerColors[11] = 0xFF45FFFF; //wind_light, nuclear cyan
			debugSFX_markerColors[12] = 0xFF414141; //night, dark grey
			debugSFX_markerColors[13] = 0xFFFFF429; //bees, nuclear yellow
		}

		if( debugGRD_enabled )
		{
			debugGRD_markers = new Vector();
			debugGRD_gridData = new Vector();
		}
	}

	//RAXAT: v2.3.1, message box builders
	public void setupMsgBoxes()
	{
		int lines = 3;
		int variants = 3;

		float x = 0.53;
		boxQueue = new Vector();

		//cache all needed resources
		for(int i=0; i<9; i++) {new ResourceRef( MSGBOX_SMALL+i ).cache();}

		boxTimer = new int[3]; boxTimer[0]=0;
		busyBox = new int[3];

		boxStatus = new float[3];
		boxStatus[0] = -4.0; //left side (hidden)
		boxStatus[1] =  0.0; //centered (shown)
		boxStatus[2] =  4.0; //right side (hidden)

		msgBox = new Rectangle[lines*variants];
		msgBox[0] = osd.createRectangle( boxStatus[0], 0.53, 0.44, 0.198, 1, new ResourceRef(MSGBOX_SMALL), 0 );
		msgBox[1] = osd.createRectangle( boxStatus[0], 0.53, 0.66, 0.198, 1, new ResourceRef(MSGBOX_MEDIUM), 0 );
		msgBox[2] = osd.createRectangle( boxStatus[0], 0.53, 0.88, 0.198, 1, new ResourceRef(MSGBOX_LONG), 0 );

		msgBox[3] = osd.createRectangle( boxStatus[0], 0.33, 0.44, 0.198, 1, new ResourceRef(MSGBOX_SMALL), 0 );
		msgBox[4] = osd.createRectangle( boxStatus[0], 0.33, 0.66, 0.198, 1, new ResourceRef(MSGBOX_MEDIUM), 0 );
		msgBox[5] = osd.createRectangle( boxStatus[0], 0.33, 0.88, 0.198, 1, new ResourceRef(MSGBOX_LONG), 0 );

		msgBox[6] = osd.createRectangle( boxStatus[0], 0.13, 0.44, 0.198, 1, new ResourceRef(MSGBOX_SMALL), 0 );
		msgBox[7] = osd.createRectangle( boxStatus[0], 0.13, 0.66, 0.198, 1, new ResourceRef(MSGBOX_MEDIUM), 0 );
		msgBox[8] = osd.createRectangle( boxStatus[0], 0.13, 0.88, 0.198, 1, new ResourceRef(MSGBOX_LONG), 0 );

		boxTxt = new Text[3];
		boxTxt[0] = osd.createText( null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.5 );
		boxTxt[1] = osd.createText( null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.33 );
		boxTxt[2] = osd.createText( null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.16 );
	}

	public void showMsgBox( String text, int color, int length )
	{
		//example of use:
		//showMsgBox("Test msgBox", MBOX_GREEN, MBOX_SHORT );

		int cFactor, lFactor;
		int findBox = 0; //line id
		int boxID; //rect id
		int textureID;

		switch(color)
		{
			case(MBOX_GREEN):
			cFactor = 0;
			break;

			case(MBOX_YELLOW):
			cFactor = 1;
			break;

			case(MBOX_RED):
			cFactor = 2;
			break;
		}

		switch(length)
		{
			case(MBOX_SHORT):
			lFactor = 0;
			textureID = MSGBOX_SMALL;
			break;

			case(MBOX_MID):
			lFactor = 1;
			textureID = MSGBOX_MEDIUM;
			break;

			case(MBOX_LONG):
			lFactor = 2;
			textureID = MSGBOX_LONG;
			break;
		}

		if( (busyBox[0]==1) && (busyBox[1]==1) && (busyBox[2]==1) ) boxQueue.addElement(new msgBoxQueue(text, color, length));
		else
		{
			if(busyBox[0] == 0 && busyBox[1] == 1 && busyBox[2] == 1) findBox = 0; //a very special case
			else
			{
				//look for free msgBox slot
				if(busyBox[0]) findBox = 1; if(busyBox[1]) findBox = 2;
			}

			if( !( (busyBox[0]==1) && (busyBox[1]==1) && (busyBox[2]==1)) ) //if not all 3 boxes are busy
			{
				//which box to show from the selected line
				boxID = (findBox*3)+lFactor;

				//now show up the box
				msgBox[boxID].changeTexture(new ResourceRef(MSGBOX_SMALL+cFactor+(lFactor*3)));
				msgBox[boxID].setPos(new Vector3( boxStatus[0], msgBox[boxID].pos.y, msgBox[boxID].pos.z ));
				msgBox[boxID].restartAnimation("X");
				msgBox[boxID].setupAnimation(0.2, 20, 1, "X"); //direction: right (1)
				msgBox[boxID].runThread(); //this begins the animation

				boxTxt[findBox].changeText(text);
				addTimer( 3, 5+findBox );
				++boxTimer[findBox];
				busyBox[findBox] = 1;
			}
		}
	}

	//hold a dedicated msgBox line and update its message
	//usage: first of all, call box with THOR, then apply hold; after that, unhold with THOR when the box is no more needed and stop holding destroyed box
	public void holdMsgBox( String txt )
	{
		if(holdID != -2)
		{
			if(holdID == -1)
			{
				if(findBusyBox() != -1)
				{
					holdID = findBusyBox(); //autodetect line
					holdMsgBox(txt,0);
				}
			}
			else holdMsgBox(txt,0);
		}
	}

	//this method is used only by the code
	public void holdMsgBox(String input, int fictive)
	{
		if(holdID != -2)
		{
			boxTxt[holdID].changeText(input); //!WARNING: supports hold of only one line
			addTimer( 3, 5+holdID );
			++boxTimer[holdID];
		}
	}

	//each box that is hold, must be unhold!
	public void unholdMsgBox()
	{
		holdID = -2; //intermediate condition, disables holding
		addTimer( 3, 8 );
	}

	//find id of _last_ called msgBox
	public int findBusyBox()
	{
		int find = -1;

		for(int i=0; i<2; i++)
		{
			if(busyBox[i])	find++;
		}
		if(find != -1) return find;

		return find;
	}

	public void hideMsgBox( int lineID )
	{
		for(int i=0; i<3; i++)
		{
			if( (msgBox[ (lineID*3)+i].pos.x > boxStatus[0]) && (msgBox[ (lineID*3)+i].pos.x < boxStatus[2]) ) //if the box is actually visible
			{
				msgBox[(lineID*3)+i].setPos( new Vector3(boxStatus[1], msgBox[(lineID*3)+i].pos.y, msgBox[(lineID*3)+i].pos.z) ); //move from center
				msgBox[(lineID*3)+i].restartAnimation("X"); //unlock movement
				busyBox[lineID] = 0;
			}
		}

		boxTxt[lineID].changeText("");
	}

	//hide all boxes
	public void clearMsgBoxes()
	{
		for(int i=0; i<3; i++)
		{
			if(busyBox[i]) hideMsgBox(i);
		}
	}

	//force boxes not to appear
	public void lockMsgBoxes()
	{
		for(int i=0; i<busyBox.length; i++){busyBox[i] = 1;}
	}
	public void unlockMsgBoxes()
	{
		for(int i=0; i<busyBox.length; i++){busyBox[i] = 0;}
	}

	public void animate()
	{
		//if(player.car) setMessage2("engine RPM: " + player.car.chassis.getRPM()); //RAXAT: useful for debugging OSD
		//if(player.car) setMessage2("pos: " + player.car.getPos().toString() + ", ori: " + player.car.getOri().toString());

		if(player.car && player.car.chassis)
		{
			if(player.car.chassis.getInfo(GameType.GII_DAMAGE) == 0) player.setSteamAchievement(Steam.ACHIEVEMENT_WASTED);
		}

		if(ai_rc_enabled == 1)
		{
			game.gBot[0].brain.command("AI_throttle " + Input.getInput(Input.AXIS_THROTTLE));
			game.gBot[0].brain.command("AI_brake " + Input.getInput(Input.AXIS_BRAKE));
			game.gBot[0].brain.command("AI_handbrake " + Input.getInput(Input.AXIS_HANDBRAKE));
			game.gBot[0].brain.command("AI_clutch " + Input.getInput(Input.AXIS_CLUTCH));
			game.gBot[0].brain.command("AI_gear " + Input.getInput(Input.AXIS_GEAR_UPDOWN));
			game.gBot[0].brain.command("AI_turn " + Input.getInput(Input.AXIS_TURN_LEFTRIGHT));
//			setMessage("Throttle power: " + Input.getInput(Input.AXIS_THROTTLE));
		}

		//RAXAT: v2.3.1, msgBox queue watcher
		if(boxQueue.size())
		{
			for(int i=0; i<2; i++)
			{
				if(!busyBox[i])
				{
					msgBoxQueue bq = boxQueue.firstElement();
					showMsgBox(bq.boxTxt, bq.boxColor, bq.boxLength);
					boxQueue.removeElementAt(0);
				}
			}
		}

		cameraNumWatcher = cameraNum;

		//RAXAT: v2.3.1, GPS route trigger stuff
		if( routeDestPos && mRouteS && mRouteF && !mapViewer && !routeDestTriggerAdded )
		{
			if( routeTrigger )
				routeTrigger.finalize();

			routeTrigger = new Trigger( map, null, routeDestPos, 10, 10, 1, "route destination trigger" );
		        addNotification( routeTrigger.trigger, EVENT_TRIGGER_ON, EVENT_SAME, null, "event_handlerRouteDest" );
			routeDestTriggerAdded = 1;
		}

		//RAXAT: v2.3.1, camera shaking
		if( cameraMode == CAMMODE_FOLLOW )
		{
			if( player.car && cam )
			{
				float speedDep = ((Math.sqrt(player.car.getSpeedSquare())*3600)/1000);
//				setMessage( speedDep/1000 ); //debug
				if( speedDep <= 150 || Integrator.frozen )
					cam.command( "torque 0.35" );
				if( !Integrator.frozen )
				{
					if( speedDep > 150 && speedDep <= 250 )
						cam.command( "torque " + speedDep/400 );
					if( speedDep > 250 && speedDep <= 350 )
						cam.command( "torque " + speedDep/900 );
					if( speedDep > 350 && speedDep <= 450 )
						cam.command( "torque " + speedDep/1200 );
					if( speedDep > 450 )
						cam.command( "torque " + speedDep/1300 );
				}
			}
		}

		//RAXAT: part of minimap 2.0 debug stuff
		if(debugMap_dist_print > 0)
		{
			debugMap_dist = GameLogic.player.car.chassis.getMileage() - debugMap_dist_init;
			setMessage2("debugMap next print: " + (debugMap_dist_print - debugMap_dist));
			if(debugMap_dist >= debugMap_dist_print)
			{
				String prefix = "		"; //tabulation for better text alignment
				String postfix = ")";
				if(debugMap_mode)
				{
					prefix += "raceposData.addElement(new Vector3("; //race position calculator triggers
					postfix += ")";
				}
				else prefix += "routeData[" + debugMap_idx + "] = new Vector3("; //GPS V4 minimap projection points
				postfix += ";";

				debugMap_dist = 0.0;
				debugMap_dist_init = GameLogic.player.car.chassis.getMileage();
				System.trace(prefix + GameLogic.player.car.getPos().toString() + postfix);
				debugMap_idx++;
				setMessage("debugMap: node " + debugMap_idx + " printed in auto mode" , "green", 5);
			}
		}

		if(!musicPlay)
		{
			Sound.changeMusicSet( Sound.MUSIC_SET_DRIVING );
			Sound.nextTrack();

			musicPlay = 1;
		}

		//RAXAT: realtime transmission type refresher for older builds
		if( !System.nextGen() )
		{
			if( player.car && player.car.chassis)
			{
				Block engine = player.car.chassis.partOnSlot(401);
				if( engine )
				{
					Transmission t = engine.getTransmission();
					if(t) t.updateType();
				}
			}
		}

		if( skydome )
		{
			if(!Integrator.frozen)
			{
				if(!Integrator.IngameMenuActive)
				{
					//RAXAT: animated skydome
					skydomeRot += 0.00004;
					if(!customSkydomePos) skydome.setMatrix(new Vector3(0,0.5,0), new Ypr(skydomeRot, 0.0, 0.0));
					else skydome.setMatrix(customSkydomePos, new Ypr(skydomeRot, 0.0, 0.0));
				}
			}
		}

		if( nav && player.car && cameraMode != CAMMODE_FREE && !mCamera )
		{
			nav.updateNavigator( player.car );
		}

		//RAXAT: v2.3.1, navigator cam marker watcher
		if( cameraMode == CAMMODE_FREE && cam && nav )
			nav.updateNavigator( cam );

		if( cameraMode != 6 && mCamera && nav )
		{
			nav.remMarker( mCamera );	mCamera=null;
		}

		if(Integrator.isCity == 1)
		{
			//RAXAT: v2.3.1, city water limit controller, fixes camera bug in Old Riverbed
			if(player && player.car)
			{
				if(player.car.getPos().y > -10.0)
				{
					map.setWater(new Vector3(0.0,-8.0,-1500.0), new Vector3(0.0,1.0,0.0), 300.0, 50.0);
					map.addWaterLimit(new Vector3(0.0,0.0,-500.0), new Vector3(0.0,0.0,1.0));
//					setMessage("Upper water limit");
				}

				if(player.car.getPos().y < -10.0)
				{
					map.setWater(new Vector3(0.0,-80.0,-1500.0), new Vector3(0.0,1.0,0.0), 300.0, 50.0);
					map.addWaterLimit(new Vector3(0.0,0.0,-500.0), new Vector3(0.0,0.0,1.0));
//					setMessage("Lower water limit");
				}
			}
		}
		else
		{
			//RAXAT: water issue fix for other tracks
			map.setWater(new Vector3(0.0,0,0.0), new Vector3(0.0,0.0,0.0), 0.0, 0.0);
			map.addWaterLimit(new Vector3(0.0,0.0,0.0), new Vector3(0.0,0.0,0.0));
		}
	}
//----------------------------------------------------------------------

	public void enter( GameState prev_state )
	{
		if( prev_state instanceof RaceSetup )
		{
		}
		else
		{
			GameLogic.autoSave();

			routeDestTriggerAdded = 0;

			teleported	= 0;
			sm_enabled	= 2;
			gpsmap_enabled	= 2;

			//RAXAT: check if there is any grid data and apply it if found
			if(startGridData.size()) applyStartGrid();

			enterAsyncMode_Script();
			t.setPriority( Thread.MAX_PRIORITY );

			//a gyerekosztalyok implementaljak a kov ket sort, az o enter() metodusukban!
			Frontend.loadingScreen.show();
			GfxEngine.flush();

			parentState=prev_state;

			//RAXAT: v2.3.1, OSD patch
			osd.menuKeysCreated = 1;
			osd.alienMp = mp;

			//---------------------------------time of day dependent stuff:
			addSceneElements( GameLogic.getTime() ); 
			//-----------------------------------------------------------------------

			msgtimers=0;
			messages=osd.createText( null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.5 );
			messages2=osd.createText( null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.6 );

			setEventMask( EVENT_CURSOR|EVENT_COMMAND|EVENT_HOTKEY|EVENT_TRIGGER_ON|EVENT_TRIGGER_OFF|EVENT_TIME );

			//player
			player.render = new RenderRef( map, player.driverID, "player" );
			player.controller.command( "renderinstance " + player.render.id() );

			if (player.car)
			{
				player.car.command( "filter 3 0" ); //RAXAT: patch, fixing car filter after gamemode reset
				
				if( GameLogic.gameMode != GameLogic.GM_DEMO )
				{
					lockCar();
					player.car.command( "setsteer 0" );

					//igy ha valtoztak a config beallitasok, az is szamit neki!
					player.car.setDefaultTransmission();
					player.car.setDefaultSteeringHelp();
					player.car.command("reload");
					player.controller.command( "controllable " + player.car.id() );
				}

				//RAXAT: v2.3.1, camera setup is optimized for launching gamemodes
				if(!game || (game && !game.gmcEvent.useCamAnimation))
				{
					lastCamPos = new Vector3(0.0, 3.0, 10.0);
					lastCamPos.rotate(player.car.getOri()); //RAXAT: patch! this does fix wrong camera rotation when calling changeCamFollow()
					lastCamPos.add(player.car.getPos() );
					changeCamTarget(player.car);
					changeCamFollow();
				}

				map.command( "obs_add " + player.car.id() );
			}

			//RAXAT: v2.3.1, this will use another cam if gamemode is active
			if(game && game.aCamPath)
			{
				dummy = GameRef.spawnDummy(map);
				lastCamPos = new Vector3(0, 3, 6);
				lastCamPos.add( game.aCamPath[0].pos ); //primary artefact filter
				changeCamTarget(dummy);
				changeCamFollow();
				cam.setMatrix(game.aCamPath[0].pos, game.aCamPath[0].ori);  //secondary artefact filter
				changeCamFreeLock();
			}

			infoline=osd.createText( null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.97, 0.57);

			osd.endGroup();

			//---------RAXAT: begin custom debug modes
			//adjusting OSD speed and RPM pins for vehicles
			if( debugOSD_enabled )
			{
				osd.createHotkey( Input.RCDIK_NUMPAD4, Input.KEY|Osd.HK_STATIC, CMD_DOSD_PREV, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD6, Input.KEY|Osd.HK_STATIC, CMD_DOSD_NEXT, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD8, Input.KEY|Osd.HK_STATIC, CMD_DOSD_INC, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD2, Input.KEY|Osd.HK_STATIC, CMD_DOSD_DEC, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD1, Input.KEY|Osd.HK_STATIC, CMD_DOSD_SETINT, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD3, Input.KEY|Osd.HK_STATIC, CMD_DOSD_SETVAL, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD9, Input.KEY|Osd.HK_STATIC, CMD_DOSD_GETVAL, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD5, Input.KEY|Osd.HK_STATIC, CMD_DOSD_INFO, Event.F_KEY_PRESS );
			}

			//placing sound instances on the map
			if( debugSFX_enabled )
			{
				osd.createHotkey( Input.RCDIK_NUMPAD8, Input.KEY|Osd.HK_STATIC, CMD_DSFX_INFO, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD2, Input.KEY|Osd.HK_STATIC, CMD_DSFX_COMMENT, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD5, Input.KEY|Osd.HK_STATIC, CMD_DSFX_NIGHTSETUP, Event.F_KEY_PRESS );
			}

			//start grid placement or any kind of positions you desire (checkpoint, pitstop, camera animation targets, etc.)
			if( debugGRD_enabled )
			{
				osd.createHotkey( Input.RCDIK_NUMPAD4, Input.KEY|Osd.HK_STATIC, CMD_DGRD_PREV, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD6, Input.KEY|Osd.HK_STATIC, CMD_DGRD_NEXT, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD8, Input.KEY|Osd.HK_STATIC, CMD_DGRD_WRITE, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD2, Input.KEY|Osd.HK_STATIC, CMD_DGRD_MAXRACERS, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD5, Input.KEY|Osd.HK_STATIC, CMD_DGRD_SPAWN, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD9, Input.KEY|Osd.HK_STATIC, CMD_DGRD_SAVE, Event.F_KEY_PRESS );
			}

			//making route line points for minimap (GPS V4)
			if( debugMap_enabled )
			{
				osd.createHotkey( Input.RCDIK_NUMPAD1, Input.KEY|Osd.HK_STATIC, CMD_DMAP_INIT, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD5, Input.KEY|Osd.HK_STATIC, CMD_DMAP_PRINT, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD3, Input.KEY|Osd.HK_STATIC, CMD_DMAP_STOP, Event.F_KEY_PRESS );

				//CMD_DMAP_TURNLEFT and CMD_DMAP_TURNRIGHT are only for "still" minimap! (a minimap with a still standing camera)
				//theese CMD's are adjusting rotation angle of a still standing camera, so the entire minimap itself can be rotated as well
				//to use this, follow theese steps:
				//1. trace routeData in a debug gamemode (or in any other gamemode), then apply it to your map with a complete setup (you can also set some approximate rotation value)
				//2. turn minimap debug ON in Track (i.e. in this file), then launch the gamemode that uses GPS V4 with "still" minimap and press NUM6 or NUM4 to adjust camera rotation
				//3. when rotation value will become precise enough, update it in the track data of your map and you're done
				osd.createHotkey( Input.RCDIK_NUMPAD7, Input.KEY|Osd.HK_STATIC, CMD_DMAP_TURNLEFT, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD9, Input.KEY|Osd.HK_STATIC, CMD_DMAP_TURNRIGHT, Event.F_KEY_PRESS );

				osd.createHotkey( Input.RCDIK_NUMPAD4, Input.KEY|Osd.HK_STATIC, CMD_DMAP_ADJUST_A_INC, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD6, Input.KEY|Osd.HK_STATIC, CMD_DMAP_ADJUST_A_DEC, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD8, Input.KEY|Osd.HK_STATIC, CMD_DMAP_ADJUST_B_INC, Event.F_KEY_PRESS );
				osd.createHotkey( Input.RCDIK_NUMPAD2, Input.KEY|Osd.HK_STATIC, CMD_DMAP_ADJUST_B_DEC, Event.F_KEY_PRESS );
			}

			if( debugKey_enabled )
				osd.createHotkey( Input.RCDIK_D, Input.KEY|Osd.HK_STATIC, CMD_DKEY_ACTIVATE, Event.F_KEY_PRESS );

			if( debugRC_enabled )
			{
				osd.createHotkey( Input.RCDIK_NUMPAD8, Input.KEY|Osd.HK_STATIC, CMD_RC_THROTTLE, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
				osd.createHotkey( Input.RCDIK_NUMPAD2, Input.KEY|Osd.HK_STATIC, CMD_RC_BRAKE, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
				osd.createHotkey( Input.RCDIK_NUMPAD5, Input.KEY|Osd.HK_STATIC, CMD_RC_HANDBRAKE, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
				osd.createHotkey( Input.RCDIK_NUMPAD1, Input.KEY|Osd.HK_STATIC, CMD_RC_CLUTCH, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
				osd.createHotkey( Input.RCDIK_NUMPAD4, Input.KEY|Osd.HK_STATIC, CMD_RC_TURN_LEFT, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
				osd.createHotkey( Input.RCDIK_NUMPAD5, Input.KEY|Osd.HK_STATIC, CMD_RC_TURN_RIGHT, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
				osd.createHotkey( Input.RCDIK_NUMPAD9, Input.KEY|Osd.HK_STATIC, CMD_RC_GEAR_UP, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
				osd.createHotkey( Input.RCDIK_NUMPAD7, Input.KEY|Osd.HK_STATIC, CMD_RC_GEAR_DOWN, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
			}
			//---------RAXAT: end of custom debug modes

			osd.createHotkey( Input.RCDIK_F4, Input.KEY|Osd.HK_STATIC, CMD_CHANGECAM_REARVIEW, this );
			//osd.createHotkey( Input.RCDIK_M, Input.KEY|Osd.HK_STATIC, CMD_GPSMAP, this );

			if(Player.c_track) //RAXAT: cheat features in v2.3.1
			{
				osd.createHotkey( Input.RCDIK_F9, Input.KEY|Osd.HK_STATIC, CMD_CHANGECAM_FREE, this );
				osd.createHotkey( Input.RCDIK_2, Input.KEY|Osd.HK_STATIC, CMD_FLIP, this );
				osd.createHotkey( Input.RCDIK_NUMPAD0, Input.KEY|Osd.HK_STATIC, CMD_SIMPAUSE, this );
				osd.createHotkey( Input.RCDIK_1, Input.KEY|Osd.HK_STATIC, CMD_QUICKREPAIR, this );
				osd.createHotkey( Input.RCDIK_ENTER, Input.KEY|Osd.HK_STATIC, CMD_SM_SWITCH, this );
				osd.createHotkey( Input.RCDIK_TAB, Input.KEY|Osd.HK_STATIC, CMD_OSDONOFF, this ); //, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );
			}

			osd.createHotkey( Input.RCDIK_F1, Input.KEY|Osd.HK_STATIC, CMD_CHANGECAM_INT, this );
			osd.createHotkey( Input.RCDIK_F2, Input.KEY|Osd.HK_STATIC, CMD_CHANGECAM_TV, this );
			osd.createHotkey( Input.RCDIK_F3, Input.KEY|Osd.HK_STATIC, CMD_CHANGECAM_EXT, this );
			osd.createHotkey( Input.RCDIK_F5, Input.KEY|Osd.HK_STATIC, CMD_CHANGECAM_CHASE, this );
			osd.createHotkey( Input.RCDIK_F6, Input.KEY|Osd.HK_STATIC, CMD_CHANGECAM_INVCH, this );


			//osd.createHotkey( Input.RCDIK_S, Input.KEY|Osd.HK_STATIC, CMD_CRUISECONTROL, this );
			osd.createHotkey( Input.AXIS_SPECIAL_1, Input.VIRTUAL|Osd.HK_STATIC, CMD_CRUISECONTROL, this ); //RAXAT: new axis, added to default control set
			osd.createHotkey( Input.AXIS_MENU, Input.VIRTUAL|Osd.HK_STATIC, CMD_INGAMEMENU, this );
			osd.createHotkey( Input.AXIS_CURSOR_BUTTON2, Input.VIRTUAL|Osd.HK_STATIC, CMD_CAMMOVE, Event.F_KEY_PRESS|Event.F_KEY_RELEASE );

			osd.show();

			if(!game || (game && !game.gmcEvent.useCamAnimation)) //RAXAT: v2.3.1, force to hide OSD if the gamemode is being run
			{
				osdEnabled = 1;
				enableOsd( osdEnabled );	//player osd, navigator
			}

			if(nav) mPlayer = nav.addMarker( player );
			
			// special request: reset mouse and set sensitivity
			Input.getAxis (1, -1 - (Config.mouseSensitivity * 100.0f));

			addTimer( 1, 2 );	//one sec tick starter

			//start automatic camerachanges
			if (1) //RAXAT: WTF IS THIS?!
			if( GameLogic.gameMode == GameLogic.GM_DEMO )
				addTimer (5, 4 );
		}

		new ResourceRef( particles:0x0106r ).cache(); //def spark
		new ResourceRef( particles:0x0002r ).cache(); //def smoke
		new ResourceRef( particles:0x000Ar ).cache(); //def skidmark

		new SfxRef( sound:0x000Br ).cache(); //def collision
		new SfxRef( sound:0x0006r ).cache(); //def skid
		new SfxRef( sound:0x0015r ).cache(); //def horn
		new SfxRef( sound:0x000Ar ).cache(); //def ignition
		new SfxRef( sound:0x0017r ).cache(); //def gear up
		new SfxRef( sound:0x0018r ).cache(); //def gear down
		new SfxRef( sound:0x001Ar ).precache(); //def air
		new SfxRef( sound:0x000Dr ).precache(); //tyre out


		if( GameLogic.gameMode == GameLogic.GM_QUICKRACE && !(prev_state instanceof RaceSetup) )
		{
		}
		else
		{
			Frontend.loadingScreen.display();

			//RAXAT: WARNING!!!!
			//Frontend.killShutter(); //destroy previously created fade screen, if it exists
			//FadeLoadingDialog fDialog = new FadeLoadingDialog(0x00D); //show up new fade screen
//			if(game && game.minimap) game.minimap.show();
		}

		player.controller.reset();
		player.controller.activateState( ControlSet.DRIVERSET );

		enableAnimateHook();

		if(game)
		{
			//shall we always need auto-wakeup?
			//game.wakeUpCars(); //RAXAT: v2.3.1, auto-wakeup for gamemodes
			if(Integrator.isCity) game.setupCity(); //place traffic and pedestrians
		}
	}

	public void exit( GameState next_state )
	{
		disableAnimateHook();

		if( next_state instanceof RaceSetup )
		{
		}
		else
		{
			GameLogic.autoSave();

			if(mRouteS) nav.remMarker(mRouteS);
			if(mRouteF) nav.remMarker(mRouteF);
			if(mapViewer) mapViewer.finalize();

			if(routeTrigger) routeTrigger.finalize();
			if(routeDestPos) routeDestPos = null;
			if(routeDestTriggerAdded) routeDestTriggerAdded = 0;

			name = "<unknown track>";

			//RAXAT: reset GPS frame
			player.gpsState = 1;
			player.handleGPSframe(player.gpsState);

			if(game) //RAXAT: destroy stuff, created by gamemodes
			{
				if(game.gmcEvent.racers) game.destroyBots(); //destroy cars and bots
				game.finalize();
			}

			//RAXAT: discard locks
			if(lockCam) lockCam = 0;	if(lockOSD) lockOSD = 0;

			if(dummy) dummy = null;

			if(mCamera)
			{
				nav.remMarker(mCamera);
				mCamera=null;
			}

			removeAllTimers();
			trigger.removeAllElements();

//			changeCamNone();

			if (player.car)
			{
				map.command( "obs_rem " + player.car.id() );

				posStart = player.car.getPos();
				oriStart = player.car.getOri();
				player.controller.command( "leave " + player.car.id() );
			}

			player.render.destroy();
			releaseCar();

			if( killCar )
			{
				player.car.destroy();
				player.car=null;
				killCar = 0;
			}

			//kill sky and sun
			remSceneElements();

			map.delTraffic();
			map.setPedestrianDensity( 0.0 );
			map.unload();

			Integrator.isCity = 0;
			Integrator.IngameMenuActive = 0;
			Integrator.frozen = 0;

			osdEnabled = 0;
			enableOsd( osdEnabled );

			if(nav)
			{
				nav.finalize(); //RAXAT: v2.3.1, nav destruction patch
				nav.remMarker( mPlayer );	mPlayer=null;
			}

			osd.hide();
			osd=null;

			clearEventMask( EVENT_ANY );

			leaveAsyncMode_Script();

			GfxEngine.flush();
			Sound.changeMusicSet( Sound.MUSIC_SET_NONE );

		}

		//allitsa vissza az 'idoszamitast' ha pl verseny vegen a lassitott mod kozben lepteti ki!
		System.timeWarp(1.0);


		player.controller.reset();
		player.controller.activateState( ControlSet.MENUSET );
	}

	//call this after camera settings have been changed
	public void resetCamera()
	{
		if( cameraMode == CAMMODE_TV || cameraMode == CAMMODE_FOLLOW )
		{
			cam.command( "render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET) );
			renderer = cam;
		}
//		else
//			System.log( "cannot reset unsupported cam type" );
	}

	public void changeCamTV( )//Vector3 destPos )
	{
		if ( !cameraTarget ) return;
		if ( !cameraTarget.id() ) return;
		Vector3	destPos = cameraTarget.getPos();
		if ( !destPos ) return;
		Vector3 actdestPos = new Vector3(destPos);
		Vector3 vel = cameraTarget.getVel();
		if (vel)
		{
			vel.mul(3.0f);
			destPos.add(vel);
		}

		if( cameraMode == CAMMODE_INTERNAL && !cameraNum )
			player.car.command( "filter 3 0" );	//running geart kifilterezzuk

		Vector3 camPos;

		if (cameraMode == CAMMODE_FREE)
		{
			player.controller.command( "leave " + cam.id() );
			player.controller.command( "controllable " + player.car.id() );
			player.controller.activateState( 5, 0 );

			lastCrossPos = map.getNearestCross( destPos, 0 );	//ToDo: getnearestCamera
			if (cam)
				camPos = cam.getPos();
			else
				camPos = destPos;
		} else
		{
			camPos = map.getNearestCross( destPos, 0 );	//ToDo: getnearestCamera
			if (camPos)
			{
				if (cameraMode == CAMMODE_TV)
				if (lastCrossPos)
				if (camPos.distance( lastCrossPos ) < 0.1)
				{
					//ha esetleg valtozott a cameratarget...
					ResourceRef bone = new ResourceRef(	cameraTarget.getInfo(GameType.GII_BONE) );
					cam.command( "look " + bone.id() + " 0,0,-1" );

					return;
				}
				lastCrossPos = new Vector3(camPos);
				camPos.y += 0.2 + 3.0*Math.random();
			} else
			{
				if (cam)
					camPos = cam.getPos();
				else
					camPos = destPos;
				if (!camPos)
					camPos = lastCamPos;
			}

			if (cameraMode == CAMMODE_TV)
			{
				if (lastCamPos)
				{
					if (camPos.distance( lastCamPos ) < 0.1)
						return;
					if (lastCamPos.distance(actdestPos) < camPos.distance(actdestPos))
						return;	//jobb a mostani
				}
			} else
				addTimer( 3,3 );	//start changing cameras every 3 seconds
		}

		changeCamNone();
		if (!camPos)	return;

		Vector3 pTemp = new Vector3(actdestPos);
		pTemp.sub( camPos );
		Ypr yTemp = new Ypr( pTemp );

//		cam = new GameRef( map, GameRef.RID_CAMERA, camPos.toString() + "," + yTemp.toString() + ",0x06", "track tv cam" );
		cam = new GameRef( map, GameRef.RID_CAMERA, camPos.toString() + "," + yTemp.toString() + ",0x02", "track tv cam" );
		cam.command( "autozoom 1 0 " + (0.2+Math.random()*0.8) + " " + (Math.random()*0.2) );
		cam.command( "render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET) );
		renderer = cam;
		cam.command( "torque 0.1" );
		cam.command( "roll " + (Math.random()-0.5) );

		lastCamPos = new Vector3(camPos);
		cameraMode = CAMMODE_TV;

		ResourceRef bone = new ResourceRef(	cameraTarget.getInfo(GameType.GII_BONE) );
		cam.command( "look " + bone.id() + " 0,0,-1" );
	}

	public void changeCamInternal( )//Vector3 pos )
	{
		if (!cameraTarget)	return;
		if (!cameraTarget.id())	return;

		if (cameraMode == CAMMODE_FREE)
		{
			player.controller.command( "leave " + cam.id() );
			player.controller.command( "controllable " + player.car.id() );
			player.controller.activateState( 5, 0 );
		}

//		changeCamNone();

		int cameras = cameraTarget.getInfo( GameType.GII_RENDER );
		if (cameras == 0)	return;

		if (cameraMode == CAMMODE_INTERNAL)
		{//change to _next_ int.cam.
//			setMessage( cameraNum + " " + cameras );
			if (++cameraNum >= cameras )
					cameraNum = 0;

			GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
			cameraTarget.command( "render " + osd.getViewport().id() +" "+ con.id() +" " + cameraNum);
			renderer = cameraTarget;
		} else
		{
			GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
			if (cam)
			{
				lastCamPos = cam.getPos();
				cam.command( "hide " + osd.getViewport().id());
				if (cam != cam_external)
					cam.destroy();	//TV
			}

			if (cameraNum < 0)
				cameraNum = (-cameraNum)%cameras;
			else
				cameraNum = 0;

			cameraTarget.command( "render " + osd.getViewport().id() +" "+ con.id() +" "+ cameraNum);
			renderer = cameraTarget;
//			if ( osdEnabled )
//				cameraTarget.command( "osd 0 " + con.id());	//disable osd
		}
		cameraMode = CAMMODE_INTERNAL;

		if( !cameraNum )
			player.car.command( "filter 3 2" );	//running geart kifilterezzuk
		else
			player.car.command( "filter 3 0" );	//running geart kifilterezzuk

	}

	public void changeCamInternal( int num )
	{
		if (cameraMode == CAMMODE_INTERNAL)
			cameraNum = num-1;
		else
			cameraNum = -num;
		changeCamInternal();
	}

	//RAXAT: v2.3.1, clone of cam to be used as a dummy bone; create it only when F3 cam is active and ready to work!!
	void createHelperBone()
	{
		if(!hBone)
		{
			//remove remarks to visualize/debug this cam
			hBone = new GameRef( map, GameRef.RID_CAMERA, cam.getPos().toString() + "," + cam.getOri().toString() + ",0x02", "helper bone" );

			hBone.command( "dist 2.5 10.0");
			hBone.command( "smooth 0.5 0.5");
			hBone.command( "force 1.6 0.5 -0.7" );	//defaults are in config.java
			hBone.command( "torque 0.1" );
			hBone.command( "roll 0" );
//			hBone.command( "render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET) );

			GameRef con = new GameRef(GameLogic.player.car.getInfo(GII_OWNER));
//			con.command( "viewport " + osd.getViewport().id() );
			con.command( "camera " + hBone.id() );
		}
	}

	void makeCamExternal()	//ennek ki kellene kapcsolnia a renderert, ha az nem az ext. cam.
	{
		Vector3	pos;

		if (cameraMode == CAMMODE_FREE)
		{
			player.controller.command( "leave " + cam.id() );
			player.controller.command( "controllable " + player.car.id() );
			player.controller.activateState( 5, 0 );
		}

		if( cameraMode == CAMMODE_INTERNAL && !cameraNum )
			player.car.command( "filter 3 0" );	//running geart kifilterezzuk


		if (cam_external)
			if (!cam_external.id())
				cam_external = null;	//drop

		if( cam_external )
		{
			if (renderer)
			if (renderer != cam_external)
			{
				renderer.command( "hide " + osd.getViewport().id());
				renderer = null;
			}
/*			if (cameraMode == CAMMODE_INTERNAL)
			{
//				GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
				cameraTarget.command( "hide " + osd.getViewport().id());// +" "+ con.id());
//				if ( osdEnabled )
//					cameraTarget.command( "osd "+ osd.id() +" "+ con.id());	//enable osd
			}
*/			if (cam && cam != cam_external)
			{
				cam.destroy();	//TV
				cam = null;
			}

			cam = cam_external;
		}

		if (!cam_external)
		{
			changeCamNone();
			
			if (lastCamPos)
				pos = lastCamPos;
			else
				pos = new Vector3(0,-10000,0);	//patch

			Vector3 pTemp = cameraTarget.getPos();
			if (!pTemp)
			{
//				System.log("Missing cameratarget! Ext.camera not created!");
				return;
			}
			Ypr yTemp;
			pTemp.sub( pos );

			//ha tul mesze ment (tvben), lerakjuk moge
			if( pTemp.length() > 70.0 )
			{
				pos = cameraTarget.getPos();
				if (!pos)	return;
				Vector3 displ = new Vector3( pTemp );
				displ.normalize();
				displ.mul( 5.0 );

				pTemp = new Vector3( pos );
				pos.sub( displ );
				pos.y+=2.0;

				pTemp.sub( pos );

				yTemp = new Ypr( pTemp );
			}
			else
			{
				yTemp = new Ypr( pTemp );
			}

			//RAXAT: old
//			cam = new GameRef( map, GameRef.RID_CAMERA, pos.toString() + "," + yTemp.toString() + ",0x02, 1.0,0, 0.01", "external track cam" );
			//RAXAT: camera patch, now it will NOT render backfacing
			cam = new GameRef( map, GameRef.RID_CAMERA, pos.toString() + "," + yTemp.toString() + ",0x02", "external track cam" );

			cam_external = cam;

			lastCamPos = new Vector3(pos);
		} 
	}

	public void changeCamFollow( )//Vector3 pos )
	{
		makeCamExternal();

		cam.command( "dist 2.5 10.0");
		cam.command( "smooth 0.5 0.5");
		cam.command( "force 1.6 0.5 -0.7" );	//defaults are in config.java
		cam.command( "torque 0.1" );
		cam.command( "roll 0" );
		cam.command( "render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET) );
		renderer = cam;

		GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
		con.command( "viewport " + osd.getViewport().id() );
		con.command( "camera " + cam.id() );

		cameraMode = CAMMODE_FOLLOW;
	}

	public void changeCamPoint( Vector3 pos )
	{
		makeCamExternal();

		GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
		con.command( "viewport 0");// + osd.getViewport().id() );
		ResourceRef bone = new ResourceRef(	cameraTarget.getInfo(GameType.GII_BONE) );

		Vector3 pTemp = cameraTarget.getPos();
		if (!pTemp)	return;
		pTemp.sub( pos );
		Ypr yTemp = new Ypr( pTemp );

		cam.command( "render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET) );
		renderer = cam;
		cam.command( "move " + bone.id() + " 0,1,0 0.1 0,-0.2,-3" );
		cam.command( "look " + map.id() + " "+pos.x+","+pos.y+","+pos.z );
		cam.command( "roll " + (Math.random()-0.5) );
		cameraMode = CAMMODE_POINT;
	}

	public void changeCamChase()
	{
		if (!cameraTarget2)
		{
			changeCamFollow();
		} else
		if (!cameraTarget2.id())
		{
			changeCamFollow();
		} else
		{
			makeCamExternal();

			GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
			con.command( "viewport 0");// + osd.getViewport().id() );
			ResourceRef bone = new ResourceRef(	cameraTarget.getInfo(GameType.GII_BONE) );
			cam.command( "render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET) );
			renderer = cam;
			float roll = Math.random()-0.5;
			cam.command( "move " + bone.id() + " 0,"+(Math.random())+",0 0.01 " + roll*-3.0 + ",-0.2," + (-3-5*Math.random()) );
			cam.command( "look " + cameraTarget2.id() + " 0,0,-1 " + roll*0.6 + ",0,-1" );
			cam.command( "roll " + roll );
			cam.command( "zoom " + (20.0+70.0*Math.random()) +" 5" );
		}
		cameraMode = CAMMODE_CHASE;
	}

	public void changeCamInvChase()
	{
		if (!cameraTarget2)
		{
			changeCamFollow();
		} else
		if (!cameraTarget2.id())
		{
			changeCamFollow();
		} else
		{
			makeCamExternal();

			GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
			con.command( "viewport 0");// + osd.getViewport().id() );
			ResourceRef bone = new ResourceRef(	cameraTarget2.getInfo(GameType.GII_BONE) );
			cam.command( "render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET) );
			renderer = cam;
//			cam.command( "move " + bone.id() + " 0,1,0 0.1 0,-0.2,-8" );
//			cam.command( "look " + cameraTarget.id() + " 0,0,-1" );
			float roll = Math.random()-0.5;
			cam.command( "move " + bone.id() + " 0,"+(Math.random())+",0 0.01 " + roll*-3.0 + ",-0.2," + (-3-5*Math.random()) );
			cam.command( "look " + cameraTarget.id() + " 0,0,-1 " + roll*0.6 + ",0,-1" );
			cam.command( "roll " + roll );
			cam.command( "zoom " + (20.0+70.0*Math.random()) +" 5" );
		}
		cameraMode = CAMMODE_INVCH;
	}

	public void changeCamFree()
	{
		if( !mCamera && nav )
			mCamera = nav.addMarker( Marker.RR_CAMERA, cam );

		makeCamExternal();
		player.controller.command( "leave " + player.car.id() );
		player.controller.command( "controllable " + cam.id() );
		player.controller.activateState( 5, 1 );
		cameraMode = CAMMODE_FREE;
	}

	//RAXAT: v2.3.1, free cam with locked controls, used by gamemodes
	public void changeCamFreeLock()
	{
		makeCamExternal();
		player.controller.command( "leave " + player.car.id() );
		player.controller.command( "controllable 0" );
		player.controller.activateState( 5, 1 );
		cameraMode = CAMMODE_FREE;
	}

	public void enablePicInPic()
	{
//debug test:
//		vport2 = new Viewport( 12, 0.1, 0.1, 0.3, 0.3 );
		vport2 = new Viewport( 12, 0.99, 0.01, 0.3, 4.0/3.0, -1.0, 0.0 );
		vport2.activate( Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET );
		GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
		cameraTarget.command( "render " + vport2.id() +" "+ con.id() +" "+ cameraNum);
//		renderer = cameraTarget;
	}

	public void changeCamNone()
	{
		if (renderer)
		{
			renderer.command( "hide " + osd.getViewport().id());
			renderer = null;
		}

		if( cameraMode == CAMMODE_INTERNAL && !cameraNum )
			player.car.command( "filter 3 0" );	//running geart kifilterezzuk

/*		if (cameraMode == CAMMODE_INTERNAL)
		{
//			GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
			cameraTarget.command( "hide " + osd.getViewport().id());// +" "+ con.id());
		}
*/		if( cam )
		{
			cam.destroy();
			if (cam_external == cam)
				cam_external = null;
			if (renderer == cam)
				renderer = null;
			cam = null;
		}
	}

	public void refreshCamera()
	{
		if (cameraMode == CAMMODE_FOLLOW)
			changeCamFollow();
		else
		if (cameraMode == CAMMODE_INTERNAL)
			changeCamInternal();
		else
		if (cameraMode == CAMMODE_TV)
			changeCamTV();
		else
		if (cameraMode == CAMMODE_CHASE)
			changeCamChase();
		else
		if (cameraMode == CAMMODE_INVCH)
			changeCamInvChase();
	}

	public void changeCamTarget( GameRef obj )
	{
		if (cameraTarget == obj)
			return;
//		if (cameraMode == CAMMODE_INTERNAL)
		if (renderer && cameraTarget)
		if (renderer.id() == cameraTarget.id())
		{
			renderer.command( "hide " + osd.getViewport().id());
			renderer = null;
		}
		cameraTarget = obj;
		refreshCamera();
	}

	public void changeCamTarget2( GameRef obj )
	{
		if (cameraTarget2 == obj)
			return;
		if (renderer && cameraTarget2)
		if (renderer.id() == cameraTarget2.id())
		{
			renderer.command( "hide " + osd.getViewport().id());
			renderer = null;
		}
		cameraTarget2 = obj;
		if ((cameraMode == CAMMODE_CHASE)
		 || (cameraMode == CAMMODE_INVCH))
			refreshCamera();
	}

	public Trigger addTrigger( Trigger t, Vector3 pos, RenderRef marker, String handler )
	{
		trigger.addElement(t);
		addNotification( t.trigger, EVENT_TRIGGER_ON, EVENT_SAME, null, handler );
		addNotification( t.trigger, EVENT_TRIGGER_OFF, EVENT_SAME, null, handler );
		
		if( marker )
		{
			t.marker = nav.addMarker( marker, pos, 0 );
			t.nav = nav;
		}

		return t;
	}


	public Trigger addTrigger( Vector3 pos, GameRef type, RenderRef marker, String handler, float r, String alias )
	{
		return addTrigger( new Trigger( map, null, pos, r, alias ), pos, marker, handler );
	}

	public void removeTrigger( Trigger t )
	{
		if( t )
		{
			remNotification( t.trigger, EVENT_TRIGGER_ON|EVENT_TRIGGER_OFF );
			trigger.removeElement(t);
			t.finalize();
		}
	}

	//el/visszaveszi a kocsit a playertol
	public void lockCar()
	{
		if (player.car)
		{
			player.car.setParent( map );

			if( startGridData.size() == 0 ) //RAXAT: if there is no grid data even for player
				player.car.setMatrix( posStart, oriStart );

			player.car.command( "reset" );
		}
	}

	public void releaseCar()
	{
		if (player.car)
		{
			player.car.setParent( player );
			player.car.command( "reset" );
		}
	}

	//RAXAT: simple text
	public void setMessage( String str )
	{
		messages.changeText( str );
		messages.changeColor( 0xFFFFFFFF ); //RAXAT: changed color will stay after previous message; this will set it back to white
		addTimer( 2, 0 );
		++msgtimers;
	}

	//RAXAT: extended text
	public void setMessage( String str, int color, int lifeTime )
	{
		messages.changeText( str );
		messages.changeColor( color );
		addTimer( lifeTime, 0 );
		++msgtimers;
	}

	//RAXAT: with simple colors
	public void setMessage( String str, String color, int lifeTime )
	{
		messages.changeText( str );

		if( color )
		{
			if( color == "red" )
				messages.changeColor( 0xFFFF2020 );

			if( color == "blue" )
				messages.changeColor( 0xFC1783FF );

			if( color == "green" )
				messages.changeColor( 0xFC06FA00 );

			if( color == "yellow" )
				messages.changeColor( 0xFCF3FF45 );

			if( color == "pink" )
				messages.changeColor( 0xFCF100C0 );

			if( color == "purple" )
				messages.changeColor( 0xFCAC45FF );

			if( color == "black" )
				messages.changeColor( 0xFF000000 );

			if( color == "white" )
				messages.changeColor( 0xFFFFFFFF );
		}

		addTimer( lifeTime, 0 );
		++msgtimers;
	}

	//RAXAT: additional line, use it for debug purposes only
	public void setMessage2( String str )
	{
		messages2.changeText( str );
		messages2.changeColor( 0xFFFFFFFF );
		addTimer( 2, 0 );
		++msgtimers;
	}

	public void handleEvent( GameRef obj_ref, int event, int param )
	{
		//intet kellene visszadnia, jelezve, hogy feldolgozta-e az uzenetet!
		if( event == EVENT_TIME )
		{
			if( param == 0 )
			{
				if( !--msgtimers )
					messages.changeText( "" );
			}
			else
			if( param == 2 )
			{
				addTimer( 1, 2 );	//one sec tick

				if( GameLogic.timeout )
				{
					if( !--GameLogic.timeout )
						GameLogic.changeActiveSection( parentState );
				}
			}
			else
			if( param == 3 )
			{
				if( cameraMode == CAMMODE_TV )
				{
//					if (player.car)
						changeCamTV();
					addTimer( 3, 3 ); //3mp-kent keresunk neki uj post
				}
			}
			else
			if( param == 4 )
			{
				if ( GameLogic.gameMode == GameLogic.GM_DEMO )
				{//random camera changes
					int	changed = 0;
					if (Math.random() > 0.3f)
					{
						int cam = 2.0*Math.random();
						cameraMode = 2+cam%3;
						changed++;
					} else
					{
						int	cam = 3.0*Math.random();
						changeCamInternal(1+cam%3);
					}

					if (Math.random() > 0.7f)
						if (cameraTarget2)
						{
							GameRef			t2 = cameraTarget2;
							
							if (renderer && cameraTarget)
							if (renderer.id() == cameraTarget.id())
							{
								renderer.command( "hide " + osd.getViewport().id());
								renderer = null;
							}
							cameraTarget2 = cameraTarget;
							cameraTarget = t2;
							changed++;
						}
					if (changed)
					{
						refreshCamera();
						addTimer (5, 4 );
					} else
					{
						addTimer (2, 4 );
					}
				}
			}
			else
			if( param == 5 )
			{
				if( !--boxTimer[0] ) hideMsgBox(0);
			}
			else
			if( param == 6 )
			{
				if( !--boxTimer[1] ) hideMsgBox(1);
			}
			else
			if( param == 7 )
			{
				if( !--boxTimer[2] ) hideMsgBox(2);
			}
			else
			if( param == 8 )
			{
				holdID = -1; //final step of unholdMsgBox()
			}
		}
	}


	public void handleEvent( GameRef obj_ref, int event, String param )
	{
		int	tok = -1;

		if( event == EVENT_CURSOR )
		{
			int	ec = param.token( ++tok ).intValue();

			int	cursor_id = param.token( ++tok ).intValue();
			if (ec == GameType.EC_RCLICK)
			{
				GameRef dest = new GameRef(param.token( ++tok ).intValue());
				int cat = dest.getInfo(GameType.GII_CATEGORY);
				if( cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE )
//					cam.command( "look " + dest.id() + " 0,0,0" );
					cam.command( "look " + dest.id() + " " + param.token( ++tok ) + "," + param.token( ++tok ) + "," + param.token( ++tok ) );
			}
			else
			{
				if(ec == GameType.EC_LCLICK)
				{
					Object part = obj_ref.getScriptInstance();
					if( part instanceof Part )
					{
						/*
						int texID = part.getTexture();
						ResourceRef texture = new ResourceRef(texID);
						System.print("saved part " + part.name + ", texture: " + Integer.getHex(texID));
						
						String fname = "brokenpart_texture";
						File f = new File(fname);
						
						f.open(File.MODE_WRITE);
						f.write(texture);
						f.close();
						*/
						/*
						String fname = "brokenpart_texture";
						File f = new File(fname);
						
						f.open(File.MODE_READ);
						int texID = f.readResID();
						f.close();
						
						part.setTexture(texID);
						*/
					}
				}
			}
		}
	}

	public void handleMessage( Message m )
	{
		if( m.type == Message.MT_EVENT )
		{
			int	cmd=m.cmd;

			if( cmd == CMD_DOSD_PREV )
			{
				if(debugOSD_index > 0)
				{
					debugOSD_index -=1;
					setMessage("debugging OSD: " + debugOSD_titles[debugOSD_index], "yellow", 3);
				}
			}
			else
			if( cmd == CMD_DOSD_NEXT )
			{
				if(debugOSD_index < debugOSD_maxparams-1)
				{
					debugOSD_index +=1;
					setMessage("debugging OSD: " + debugOSD_titles[debugOSD_index], "yellow", 3);
				}
			}
			else
			if( cmd == CMD_DOSD_INC )
			{
				if(player.car)
				{
					debugOSD_curval += debugOSD_delta;
					GameLogic.player.car.chassis.setOSD(debugOSD_index,debugOSD_curval);
	setMessage("OSD param [" + debugOSD_titles[debugOSD_index] + "] value: " + Float.toString(debugOSD_curval, "%4f"), "green", 5);
				}
			}
			else
			if( cmd == CMD_DOSD_DEC )
			{
				if(player.car)
				{
					debugOSD_curval -= debugOSD_delta;
					GameLogic.player.car.chassis.setOSD(debugOSD_index,debugOSD_curval);
	setMessage("OSD param [" + debugOSD_titles[debugOSD_index] + "] value: " + Float.toString(debugOSD_curval, "%4f"), "red", 5);
				}
			}
			else
			if( cmd == CMD_DOSD_SETINT )
			{
StringRequesterDialog d = new StringRequesterDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY, "debugOSD: set delta", "" );
				if( d.display() == 0 )
				{
					debugOSD_delta = d.input.floatValue();
					setMessage("OSD delta value: " + Float.toString(debugOSD_delta, "%4f"), "yellow", 5);
				}
			}
			else
			if( cmd == CMD_DOSD_SETVAL ) //RAXAT: also can be used to override curval from getOSD() method as extra interpolation
			{
StringRequesterDialog d = new StringRequesterDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY, "debugOSD: set direct value", "" );
				if( d.display() == 0 )
				{
					debugOSD_curval = d.input.floatValue();
					GameLogic.player.car.chassis.setOSD(debugOSD_index,debugOSD_curval);
	setMessage("OSD param [" + debugOSD_titles[debugOSD_index] + "] value: " + Float.toString(debugOSD_curval, "%4f"), "yellow", 5);
				}
			}
			else
			if( cmd == CMD_DOSD_GETVAL )
			{
				debugOSD_curval = GameLogic.player.car.chassis.getOSD(debugOSD_index);
	setMessage("OSD param [" + debugOSD_titles[debugOSD_index] + "] value: " + Float.toString(debugOSD_curval, "%4f") + " GET OK", "white", 5);
			}
			else
			if( cmd == CMD_DOSD_INFO )
			{
	setMessage("INFO: OSD param [" + debugOSD_titles[debugOSD_index] + "] value: " + Float.toString(debugOSD_curval, "%4f"), "white", 5);
				new SfxRef( frontend:0x02F1r ).play();
			}
			else
			if( cmd == CMD_SM_SWITCH )
			{
				sm_enabled = 1-sm_enabled;
				SlowMotionSwitcher();
			}
			else
			if( cmd == CMD_GPSMAP )
			{
				if( !Integrator.frozen )
				{
					gpsmap_enabled = 1-gpsmap_enabled;

					if( gpsmap_enabled < 0 )
					{
						System.timeWarp( 0.0 );

						mapViewer = new Map(this);
						mapViewer.init();
					}

					if( gpsmap_enabled > 0 )
					{
						mapViewer = null;
						System.timeWarp( 1.0 );
						osd.rebuild();

						if( cam && teleported )
						{
							cam.setPos( new Vector3( player.car.getPos().x, player.car.getPos().y+5, player.car.getPos().z+5) );
							teleported = 0;
						}

						changeCamFollow();
					}
				}
			}
			else
			if( cmd == CMD_CRUISECONTROL )
			{
				//RAXAT: work more on this
				if(!game)
				{
					int newCruise = 1-player.car.getCruiseControl();
					player.car.setCruiseControl( newCruise );
					if( newCruise )
						setMessage( "Cruise control on" );
					else
						setMessage( "Cruise control off" );
				}
			}
			else
			if( cmd == CMD_DSFX_INFO )
			{
				if(!debugSFX_headerProcessed) //write parents, including unused nodes!
				{
					debugSFX_outFile.write( "//-----------------------PARENTS:" );

					for( int i=0; i<debugSFX_maxparams; i++ )
					{
						debugSFX_outFile.write( "<FILE 00" + debugSFX_resIdParents[i] + ".res >" );
						debugSFX_outFile.write( "typeof	1" );
						debugSFX_outFile.write( "superid	0x00000001" );
						debugSFX_outFile.write( "typeid	0x00" + debugSFX_resIdParents[i] );
						debugSFX_outFile.write( "alias	" + debugSFX_resLib[i] );
						debugSFX_outFile.write( "isparentcompatible	1.00" );
						debugSFX_outFile.write( "</FILE>" );
						debugSFX_outFile.write( "<FILE 00" + debugSFX_resIdParents[i] + ".rsd >" );
						debugSFX_outFile.write( "</FILE>" );
						debugSFX_outFile.write( "" );
					}

					debugSFX_outFile.write( "//-----------------------CHILDS:" );
					debugSFX_headerProcessed = 1;
				}

				//now write child SFX nodes, max ~200, should be enough
				String resId;
				if( debugSFX_rsdIndex < 10 )
					resId = "001F" + 0 + debugSFX_rsdIndex;
				else
				{
					if( debugSFX_rsdIndex < 100 )
						resId = "001F" + debugSFX_rsdIndex;
					else
					{
						if( (debugSFX_rsdIndex-100) < 10 )
							resId = "002F" + "0" + (debugSFX_rsdIndex-100);
						else
							resId = "002F" + (debugSFX_rsdIndex-100);
					}
				}

				for( int i=0; i<debugSFX_maxparams; i++ )
				{
					if( comments == debugSFX_resLib[i] )
					{
						debugSFX_outFile.write( "<FILE 00" + resId + ".res >" );
						debugSFX_outFile.write( "typeof	1" );
						debugSFX_outFile.write( "superid	0x00" + debugSFX_resIdParents[i] );
						debugSFX_outFile.write( "typeid	0x00" + resId );
						debugSFX_outFile.write( "alias	" + comments + ".cfg" );
						debugSFX_outFile.write( "isparentcompatible	1.00" );
						debugSFX_outFile.write( "</FILE>" );
						debugSFX_outFile.write( "<FILE 00" + resId + ".rsd >" );
						debugSFX_outFile.write( "gametype 0x00" + debugSFX_resIdChilds[i] );
						debugSFX_outFile.write( "params " + cam.getPos().toString() + ",0.000,0.000,0.000" );
						debugSFX_outFile.write( "</FILE>" );
						debugSFX_outFile.write( "" );

						setMessage( "debug SFX node written: " + comments + "; idx: " + debugSFX_rsdIndex );

						debugSFX_rsdIndex++;

						if( i==8 || i==9 || i==10 || i==11 ) //all wind SFX
							debugSFX_nightNodes.addElement( new Vector3(cam.getPos().x+1, cam.getPos().y, cam.getPos().z) );

						RenderRef	marker = new RenderRef( map, frontend:0x00000070r, "debugSFX_marker" );
						marker.setMatrix( cam.getPos(), cam.getOri() );
						marker.setColor( debugSFX_markerColors[i] );
					}
				}

				new SfxRef( Frontend.SFX_MONEY ).play();
			}
			else
			if( cmd == CMD_DSFX_COMMENT )
			{
StringRequesterDialog d = new StringRequesterDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY, "Console", "" );
			if( d.display() == 0 )
				comments = d.input;
			}
			else
			if( cmd == CMD_DSFX_NIGHTSETUP ) //RAXAT: write all collected debug night SFX nodes
			{

				String resId; //names in this method are being refreshed at EVERY single iteration
				for( int i=0; i<debugSFX_nightNodes.size(); i++ )
				{
					if( debugSFX_rsdIndex < 10 )
						resId = "001F" + 0 + debugSFX_rsdIndex;
					else
					{
						if( debugSFX_rsdIndex < 100 )
							resId = "001F" + debugSFX_rsdIndex;
						else
						{
							if( (debugSFX_rsdIndex-100) < 10 )
								resId = "002F" + "0" + (debugSFX_rsdIndex-100);
							else
								resId = "002F" + (debugSFX_rsdIndex-100);
						}
					}

					debugSFX_outFile.write( "<FILE 00" + resId + ".res >" );
					debugSFX_outFile.write( "typeof	1" );
					debugSFX_outFile.write( "superid	0x00" + debugSFX_resIdParents[12] );
					debugSFX_outFile.write( "typeid	0x00" + resId );
					debugSFX_outFile.write( "alias	" + debugSFX_resLib[12] + ".cfg" );
					debugSFX_outFile.write( "isparentcompatible	1.00" );
					debugSFX_outFile.write( "</FILE>" );
					debugSFX_outFile.write( "<FILE 00" + resId + ".rsd >" );
					debugSFX_outFile.write( "gametype 0x00" + debugSFX_resIdChilds[12] );
					debugSFX_outFile.write( "params " + debugSFX_nightNodes.elementAt(i).toString() + ",0.000,0.000,0.000" );
					debugSFX_outFile.write( "</FILE>" );
					debugSFX_outFile.write( "" );

					debugSFX_rsdIndex++;
				}

				setMessage( "debug night SFX array size: " + debugSFX_nightNodes.size() );
				new SfxRef( Frontend.SFX_MONEY ).play();
			}
			else
			if( cmd == CMD_DGRD_PREV )
			{
				if(debugGRD_maxAIracersOK)
				{
					if(debugGRD_index > 0)
						debugGRD_index --;

					if(debugGRD_index<maxAIracers)
						debugGRD_markers.elementAt(debugGRD_index+1).setColor(0xFF35D500); //nuclear green

					debugGRD_markers.elementAt(debugGRD_index).setColor(0xFFFA001C); //bright red

					setMessage("debug grid slot " + debugGRD_index + " selected", "yellow", 5 );
				}
				else
					setMessage("debug grid: maxAIracers is not set! switch terminated", "white", 5 );
			}
			else
			if( cmd == CMD_DGRD_NEXT )
			{
				if(debugGRD_maxAIracersOK)
				{
					if(debugGRD_index < (maxAIracers-1))
						debugGRD_index ++;

					if(debugGRD_index>0)
						debugGRD_markers.elementAt(debugGRD_index-1).setColor(0xFF35D500); //nuclear green

					debugGRD_markers.elementAt(debugGRD_index).setColor(0xFFFA001C); //bright red

					setMessage("debug grid slot " + debugGRD_index + " selected", "yellow", 5 );
				}
				else
					setMessage("debug grid: maxAIracers is not set! switch terminated", "white", 5 );
			}
			else
			if( cmd == CMD_DGRD_MAXRACERS )
			{
				if(!debugGRD_maxAIracersOK)
				{
StringRequesterDialog d = new StringRequesterDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY, "Console", "" );
			if( d.display() == 0 )
					maxAIracers = d.input.intValue();
					debugGRD_vehicles = new Vehicle[maxAIracers];

					for(int i=0; i<maxAIracers; i++)
					{
						RenderRef	marker = new RenderRef( map, frontend:0x00000070r, "debugGRD_marker" );
						marker.setMatrix(new Vector3(0,0,0), new Ypr(0,0,0));
						marker.setColor(0xFF35D500); //nuclear green
						debugGRD_markers.addElement(marker);

						debugGRD_gridData.addElement(new Vector3(0,0,0));
						debugGRD_gridData.addElement(new Ypr(0,0,0));
					}

					debugGRD_maxAIracersOK = 1;
					setMessage("debug grid: maxAIracers " + maxAIracers + ", all vectors initialized, NO UNDO!", "red", 5 );
				}
			}
			else
			if( cmd == CMD_DGRD_WRITE )
			{
				if(cameraMode == CAMMODE_FREE) //writing cam.pos
				{
					if(cam)
					{
						debugGRD_gridData.setElementAt(cam.getPos(), debugGRD_index*2);
						debugGRD_gridData.setElementAt(cam.getOri(), (debugGRD_index*2)+1);

						debugGRD_markers.elementAt(debugGRD_index).setMatrix
							(new Vector3( cam.getPos().x, cam.getPos().y+3, cam.getPos().z ), cam.getOri());
					}
					
				}
				else //writing player.car.pos
				{
					debugGRD_gridData.setElementAt(player.car.getPos(), debugGRD_index*2);
					debugGRD_gridData.setElementAt(player.car.getOri(), (debugGRD_index*2)+1);

					debugGRD_markers.elementAt(debugGRD_index).setMatrix
						(new Vector3( player.car.getPos().x, player.car.getPos().y+3, player.car.getPos().z ), player.car.getOri());
				}

				setMessage("debug grid data is set for slot " + debugGRD_index, "red", 5 );
			}
			else
			if( cmd == CMD_DGRD_SPAWN )
			{
CarRequesterDialog d = new CarRequesterDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY, "Select car to spawn", "OK" );
				if( d.display() == 0 )
				{
					VehicleDescriptor vd = GameLogic.getVehicleDescriptor( VehicleType.VS_STOCK );
					Vehicle spawned = new Vehicle( map, d.selectedCarID, vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear );

					if(debugGRD_vehicles[debugGRD_index]) //respawn or just a change in grid slot
					{
						debugGRD_vehicles[debugGRD_index].destroy();
						debugGRD_vehicles[debugGRD_index] = null;
					}

					debugGRD_vehicles[debugGRD_index] = spawned;
					debugGRD_vehicles[debugGRD_index].setMatrix
						(debugGRD_gridData.elementAt(debugGRD_index*2), debugGRD_gridData.elementAt((debugGRD_index*2)+1));
					debugGRD_vehicles[debugGRD_index].queueEvent( null, EVENT_COMMAND, "reset" );
					debugGRD_vehicles[debugGRD_index].wakeUp();
					debugGRD_vehicles[debugGRD_index].queueEvent( null, GameType.EVENT_COMMAND, "start" );

					setMessage("debug grid vehicle spawned in slot " + debugGRD_index, "green", 5 );
					new SfxRef( Frontend.SFX_MONEY ).play();
				}
			}
			else
			if( cmd == CMD_DGRD_SAVE )
			{
				debugGRD_outFile.write("		/* debug start grid generated data */");

				for(int i=0; i<maxAIracers; i++)
				{
					debugGRD_outFile.write
					(
						 "		setStartGrid( " + debugGRD_gridData.elementAt(i*2).toString() + ", " + 
								  + debugGRD_gridData.elementAt((i*2)+1).toString() + " );"
								  + "	//start grid slot " + i
					);
				}

				debugGRD_outFile.write("		/* end of generated data */");

				setMessage("debug start grid file write OK!", "green", 5 );
				new SfxRef( Frontend.SFX_MONEY ).play();
			}
			else
			if( cmd == CMD_DKEY_ACTIVATE )
			{
/*
				//savegame debug tests:
				if(!game.bestTime)
				{
					game.bestTime = 50.50;
					player.bestTimes.addElement(new Integer(game.gmcEvent.track.map_id));
					player.bestTimes.addElement(new Float(game.bestTime));
					player.bestTimes.addElement(game.gmcEvent.track.name);
					setMessage("vectors are created");

					player.loadTrackData();
					setMessage("data loaded: " + player.bestTimes.elementAt(2));
				}
				else
				{
					player.saveTrackData();
					setMessage("data saved");
				}
*/
//				CareerEvent.setEventStatus(game.gmcEvent.event_id, game.gmcEvent.eventName, Gamemode.GMS_CUP_GOLD);
//				player.saveEventData();
//				setMessage("event status is set");

//				player.setTrackData(game.gmcEvent.track.map_id, game.gmcEvent.track.name, 30.2);
//				player.loadTrackData();
//				setMessage("track data is set: " + player.getTrackData(game.gmcEvent.track.map_id, game.gmcEvent.track.name));

//				setMessage(String.timeToString( player.getTrackData(game.gmcEvent.track.map_id, game.gmcEvent.track.name), String.TCF_NOHOURS ));

/*
				//remote control test:
				ai_rc_enabled = 1;
				changeCamTarget(game.gBot[0].car);
				game.gBot[0].brain.command("AI_suspend" );
//				game.gBot[0].car.command("esp 1" ); //tweak ESP?
				game.gBot[0].car.command("abs " + Config.player_abs );
				game.gBot[0].car.command("asr " + Config.player_asr );
				game.gBot[0].car.command("steerhelp " + Config.player_steeringhelp );
				setMessage("debug remote control ON");
*/

//				if(game) game.unlaunch(); //remove when finish with fade FX

/*
				Frontend.killShutter();
				if(ai_throttle == -1)
				{
					FadeLoadingDialog fDialog = new FadeLoadingDialog(0x10D);
				}
				else
				{
					FadeLoadingDialog fDialog = new FadeLoadingDialog(0x00D); //to transparent
				}

				ai_throttle *= (-1);
*/

//				game.cheatWin();
//				Sound.changeMusicSet( Sound.MUSIC_SET_NONE );

//				if(game) game.handleDebug();
			}
			else
			if( cmd == CMD_RC_THROTTLE )
			{
				ai_throttle *= (-1);
			}
			else
			if( cmd == CMD_RC_BRAKE )
			{
				ai_brake *= (-1);
			}
			else
			if( cmd == CMD_RC_HANDBRAKE )
			{
				ai_handbrake *= (-1);
			}
			else
			if( cmd == CMD_RC_CLUTCH )
			{
				ai_clutch *= (-1);
			}
			else
			if( cmd == CMD_RC_TURN_LEFT )
			{
				ai_turn *= (-1);
			}
			else
			if( cmd == CMD_RC_TURN_RIGHT )
			{
				ai_turn *= (-1);
			}
			else
			if( cmd == CMD_RC_GEAR_UP )
			{
				ai_gear_up *= (-1);
			}
			else
			if( cmd == CMD_RC_GEAR_DOWN )
			{
				ai_gear_down *= (-1);
			}
			else
			if( cmd == CMD_FLIP )
			{
			player.car.queueEvent( null, EVENT_COMMAND, "reset" );
			Ypr	playerOri = new Ypr( player.car.getOri().y, 0, 0 );
			player.car.queueEvent( null, EVENT_COMMAND, "reset" );
			player.car.setMatrix( player.car.getPos(), playerOri );
			new SfxRef( frontend:0x02F1r ).play();
			}
			else
			if( cmd == CMD_CAMMOVE )
			{
				if( ((Event)m).flags )
				{	//press
					player.controller.user_Add( Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);
					player.controller.user_Add( Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);
				}
				else
				{	//release
					player.controller.user_Del( Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1 );
					player.controller.user_Del( Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0 );
				}
			} else

			if( cmd == CMD_CHANGECAM_TV )
			{
				if(!lockCam) changeCamTV();
			} else
			if( cmd == CMD_CHANGECAM_INT )
			{
				if(!lockCam) changeCamInternal();
			} else
			if( cmd == CMD_CHANGECAM_EXT )
			{
				if (cameraMode != CAMMODE_FOLLOW)
				{
					if(!lockCam) changeCamFollow();
				}
			} else
			if( cmd == CMD_CHANGECAM_CHASE )
			{
				if(!lockCam) changeCamChase();
			} else
			if( cmd == CMD_CHANGECAM_INVCH )
			{
				if(!lockCam) changeCamInvChase();
			} else
			if( cmd == CMD_CHANGECAM_FREE )
			{
				if( !Integrator.disableTrackHotkeys )
				{
					if(!lockCam)
					{
						changeCamFree();
//						enablePicInPic();
					}
				}
			} else
			if( cmd == CMD_CHANGECAM_REARVIEW )
			{
				if(!lockCam)
				{
					int cameras = cameraTarget.getInfo( GameType.GII_RENDER );
					changeCamInternal(cameras-1); //RAXAT: get last cam
				}
			}
			if( cmd == CMD_CHANGECAMTARGET )
			{
				if(!lockCam)
				{
					if (cameraTarget2)
					{
						GameRef			t2 = cameraTarget2;

						if (renderer && cameraTarget)
						if (renderer.id() == cameraTarget.id())
						{
							renderer.command( "hide " + osd.getViewport().id());
							renderer = null;
						}
						if (renderer && cameraTarget2)
						if (renderer.id() == cameraTarget2.id())
						{
							renderer.command( "hide " + osd.getViewport().id());
							renderer = null;
						}

						{
							GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
							con.command( "viewport 0");
							con.command( "osd 0" );
							cameraTarget.command( "osd 0 " + con.id());//disable osd
						}

						cameraTarget2 = cameraTarget;
						cameraTarget = t2;

						{
							GameRef con = new GameRef(cameraTarget.getInfo(GII_OWNER));
							con.command( "viewport " + osd.getViewport().id() );
							con.command( "osd " + osd.id() );
							cameraTarget.command( "osd "+ osd.id() +" "+ con.id());	//enable osd
						}

						refreshCamera();
					}
				}
			}
			else
			if( cmd == CMD_DMAP_INIT )
			{
				StringRequesterDialog d = new StringRequesterDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY, "debugMap: set print distance", "" );
				if(d.display() == 0)
				{
					if(d.input)
					{
						String md = " GPS V4";
						if(debugMap_mode) md = "racepos";

						debugMap_dist_print = d.input.intValue();

						debugMap_dist_init = GameLogic.player.car.chassis.getMileage();
						debugMap_dist = 0.0;
						debugMap_startMarker = new RenderRef(map, frontend:0x00000070r, "debugMap_marker");
						Vector3 pos = GameLogic.player.car.getPos();
						debugMap_startMarker.setMatrix(new Vector3(pos.x, pos.y+2, pos.z), GameLogic.player.car.getOri());
						setMessage("debugMap initialized! mode: " + md, "yellow", 5);
					}
				}
			}
			else
			if( cmd == CMD_DMAP_PRINT )
			{
				if(debugMap_dist_print == 0)
				{
					System.trace("		routeData[" + debugMap_idx + "] = new Vector3(" + GameLogic.player.car.getPos().toString() + ");");
					debugMap_idx++;
					setMessage("debugMap: node " + debugMap_idx + " printed in manual mode" , "green", 5);
				}
			}
			else
			if( cmd == CMD_DMAP_STOP )
			{
				if(debugMap_dist_print >= 0)
				{
					if(debugMap_startMarker)
					{
						debugMap_startMarker.destroy();
						debugMap_startMarker = null;
					}

					setMessage("debugMap stopped, total nodes printed: " + debugMap_idx, "red", 5);

					debugMap_dist_print = -1;
					debugMap_idx = 0;
				}
			}
			else
			if( cmd == CMD_DMAP_TURNRIGHT )
			{
				if(nav && nav.type == Navigator.TYPE_MINIMAP_STILL)
				{
					nav.rotation += debugMap_rotationStep;
					setMessage("debugMap: current nav stillcam rotation: " + nav.rotation + ", TURNRIGHT", "green", 5);
				}
			}
			else
			if( cmd == CMD_DMAP_TURNLEFT )
			{
				if(nav && nav.type == Navigator.TYPE_MINIMAP_STILL)
				{
					nav.rotation -= debugMap_rotationStep;
					setMessage("debugMap: current nav stillcam rotation: " + nav.rotation + ", TURNLEFT", "green", 5);
				}
			}
			else
			if( cmd == CMD_DMAP_ADJUST_A_INC )
			{
				if(nav && nav.type == Navigator.TYPE_MINIMAP_STILL)
				{
					nav.adjustA += debugMap_adjustStep;
					setMessage("debugMap: nav adjustA: " + nav.adjustA + ", ADJUST_A_INC", "yellow", 5);
				}
			}
			else
			if( cmd == CMD_DMAP_ADJUST_A_DEC )
			{
				if(nav && nav.type == Navigator.TYPE_MINIMAP_STILL)
				{
					nav.adjustA -= debugMap_adjustStep;
					setMessage("debugMap: nav adjustA: " + nav.adjustA + ", ADJUST_A_DEC", "yellow", 5);
				}
			}
			else
			if( cmd == CMD_DMAP_ADJUST_B_INC )
			{
				if(nav && nav.type == Navigator.TYPE_MINIMAP_STILL)
				{
					nav.adjustB += debugMap_adjustStep;
					setMessage("debugMap: nav adjustB: " + nav.adjustB + ", ADJUST_B_INC", "yellow", 5);
				}
			}
			else
			if( cmd == CMD_DMAP_ADJUST_B_DEC )
			{
				if(nav && nav.type == Navigator.TYPE_MINIMAP_STILL)
				{
					nav.adjustB -= debugMap_adjustStep;
					setMessage("debugMap: nav adjustB: " + nav.adjustB + ", ADJUST_B_DEC", "yellow", 5);
				}
			}
			else
			if( cmd == CMD_OSDONOFF )
			{
				if( !Integrator.disableTrackHotkeys )
				{
					if(!lockOSD)
					{
						osdEnabled = 1-osdEnabled;
						enableOsd( osdEnabled );
					}
				}
			} 
			else
			if( cmd == CMD_CAMROTATE )
			{
				if( camRotate )
				{
					cam.command( "angle 0 0" );		//0.7853 = (2*pi)/8.0
				}
				else
				{
					cam.command( "angle 0 8.0 0.7853" );		//0.7853 = (2*pi)/8.0
				}

				camRotate = 1-camRotate;
			}
			else
			if( cmd == CMD_QUICKREPAIR )
			{
				if( !Integrator.disableTrackHotkeys )
				{
					if (player.car)
					{
						player.car.repair();
						new SfxRef( frontend:0x01B1r ).play();
					}
				}
			}
			else
			if( cmd == CMD_SIMPAUSE )
			{	// !=0.0 <-> 0.0
				if ( System.timeWarp(-1.0) > 0.0 )
				{
					if( !Integrator.disableTrackHotkeys )
					{
						System.timeWarp(0.0);
						cam.command( "simulate 1" );
						Integrator.frozen = 1;
						new SfxRef( frontend:0x01A1r ).play();
					}
				}
				else
				{
					if( !Integrator.disableTrackHotkeys )
					{
						cam.command( "simulate 0" );
						System.timeWarp( 1.0 );
						if( sm_enabled <0 )
						{
						sm_enabled = 1-sm_enabled;
						SlowMotionSwitcher();
						}
						Integrator.frozen = 0;
						new SfxRef( frontend:0x01C1r ).play();
						if( camRotate )
							cam.command( "angle 0 10.0 0.7853" );		//0.7853 = (2*pi)/8.0
					}
				}
			}
			else
			if( cmd == CMD_SIMSPEEDINC )
			{
				float t = 2.0*System.timeWarp(-1.0);
				if( t < 1.0 )
				{
					if( t == 0.0 )
						t = 1.0/64;
					System.timeWarp( t );
					cam.command( "simulate 1" );
					if( camRotate )
						cam.command( "angle 0 10.0 0.7853" );		//0.7853 = (2*pi)/8.0
				}
				else
				{
					System.timeWarp(1.0);
					cam.command( "simulate 0" );
					if( camRotate )
						cam.command( "angle 0 10.0 0.7853" );		//0.7853 = (2*pi)/8.0
				}
			} 
			else
			if( cmd == CMD_SIMSPEEDDEC )
			{
				float t = 0.5*System.timeWarp(-1.0);
				if (t < 1.0/64)
					t = 0.0;
				System.timeWarp(t);
				if( camRotate )
					cam.command( "angle 0 10.0 0.7853" );		//0.7853 = (2*pi)/8.0
				if( t < 1.0 )
					cam.command( "simulate 1" );
				else
					cam.command( "simulate 0" );
			} 
			else
			if( cmd == CMD_INGAMEMENU ) //RAXAT: v2.3.1, more simplier and more efficient interaction with ingame menu
			{
				//this part code is not completely tested!!!! may be buggy!!

				int gpsMode = Config.gpsMode;
				IngameMenu im = new IngameMenu(this, Dialog.DF_DARKEN);
				if(cam && cam.id()) im.setActiveCamera(cam);
				im.display();

				/*
				player.car.setDefaultTransmission();
				player.car.setDefaultSteeringHelp();
				player.car.setDefaultASR();
				player.car.setDefaultABS();
				*/

				if(nav)
				{
					if(gpsMode != Config.gpsMode) nav.changeMode(Config.gpsMode);
				}

				if(im.reqTrackExit)
				{
					int spec;
					if(this instanceof ROCTrack)
					{
						if(!this.testMode) spec++;
					}
					if(spec) this.giveUpRace();
					else
					{
						if(!game) GameLogic.changeActiveSection(parentState);
							else tempGMexit(); //RAXAT: TEMP!!!!!!
					}
				}
			}
			else
			if( cmd == CMD_EXIT )
			{
				GameLogic.changeActiveSection( parentState );
			}
		}
	}

	public void osdCommand( int cmd )
	{
		mp.putMessage( new Event( cmd ) );
	}

	public void SlowMotionSwitcher(	)
	{
		if( sm_enabled <0 )
		{
			System.timeWarp( 0.3 );
//			osd.darken(); //DEBUG ONLY! since there is no darken check yet
			Integrator.frozen = 0;
			new SfxRef( frontend:0x0A29r ).play();
		}

		if( sm_enabled >0 )
		{
			System.timeWarp( 1.0 );
//			osd.darken(); //DEBUG ONLY! since there is no darken check yet
			Integrator.frozen = 0;
			new SfxRef( frontend:0x0C29r ).play();
		}
	}

	public void enableOsd( int enable )
	{
		if( enable )
		{
			osdCounter++;
			if( osdCounter == 1 )	// 0 -> 1
			{
				player.showOsd();

				if( !mapViewer )
				{
					if(nav)
					{
						nav.routeRegen = 0;
						nav.show();
					}
				}

				//osd.show(0);	//csak screenshotozashoz!!
			}
		}
		else
		{
			osdCounter--;
			if( osdCounter == 0 )	// 1 -> 0
			{
				player.hideOsd();

				if( !mapViewer )
				{
					if(nav && nav.cam) nav.hide();
				}
				//osd.hide(0);	//csak screenshotozashoz!!
			}
		}
	}

	//RAXAT: v2.3.1, exit method for gamemodes
	public void getOut()
	{
		if(GameLogic.gameMode == GameLogic.GM_FREERIDE) GameLogic.changeActiveSection(new EventList(EventList.MODE_FREERIDE));
		else
		{
			if(!game.failable) GameLogic.changeActiveSection(new Garage());
			else GameLogic.changeActiveSection(new EventList(EventList.MODE_AMATEUR));
		}
	}

	//RAXAT: temp method for exiting GM via ingame menu (this does fail the GM event)
	public void tempGMexit()
	{
		game.giveUp();
		getOut();
	}

	public void event_handlerRouteDest( GameRef obj_ref, int event, String param )
	{
		int	id = param.token(0).intValue();

		if( id == player.car.id() )
		{
			if ( nav.route )
				nav.route.destroy();

			if( mRouteS )
				nav.remMarker( mRouteS );

			if( mRouteF )
				nav.remMarker( mRouteF );

			new SfxRef( frontend:0x02F1r ).play();
			setMessage( "GPS info: destination point is reached", "yellow", 3 );
			routeDestTriggerAdded = 0;
			routeDestPos = null;
			routeTrigger.finalize();
		}
	}

	public void setStartGrid( float px, float py, float pz, float oy, float op, float or )
	{
		startGridData.addElement(new Vector3(px,py,pz));
		startGridData.addElement(new Ypr(oy,op,or));
	}

	public void randomizeStartGrid(int gridSize) //gridSize does limit randomization area (to remove possible gaps between start positions in randomized grid)
	{
		Vector source = new Vector();

		for(int i=0; i<gridSize*2; i++)
		{
			source.addElement(new Pori(startGridData.elementAt(i), startGridData.elementAt(i+1)));
			i+=1;
		}

		//4 iterations of shuffle for ultimate randomization
		source.shuffle();
		source.shuffle();
		source.shuffle();
		source.shuffle();
		startGridData = new Vector();

		for(int j=0; j<source.size(); j++)
		{
			startGridData.addElement(source.elementAt(j).pos);
			startGridData.addElement(source.elementAt(j).ori);
		}
	}

	public void applyStartGrid()
	{
		//RAXAT: let the player take the first element of grid data
		player.car.setMatrix(startGridData.elementAt(0), startGridData.elementAt(1));
		player.car.queueEvent( null, EVENT_COMMAND, "reset" );

		if(!game)
		{
			player.car.wakeUp();
			player.car.queueEvent( null, GameType.EVENT_COMMAND, "start" );
		}

		if( maxAIracers > 0 )
		{
			int cid = 0;
			int cdlt = 2;

			for( int i=2; i<((maxAIracers*2)+2); i++ )
			{
				if( i-cdlt == cid )
				{
					if(mapVehicle[cid])
					{
						mapVehicle[cid].setMatrix(startGridData.elementAt(i), startGridData.elementAt(i+1));
						mapVehicle[cid].queueEvent( null, EVENT_COMMAND, "reset" );

						if(!game)
						{
							mapVehicle[cid].wakeUp();
							mapVehicle[cid].queueEvent( null, GameType.EVENT_COMMAND, "start" );
						}
					}

					cid++;
					cdlt++;
				}
			}
		}

		if(nav && player.car) nav.updateNavigator(player.car); //little patch for navigator
	}
}

//RAXAT: v2.3.1, queue object for msgBox
public class msgBoxQueue
{
	String	boxTxt;
	int	boxColor, boxLength;

	public	msgBoxQueue(String t, int c, int l)
	{
		boxTxt = t;
		boxColor = c;
		boxLength = l;
	}
}