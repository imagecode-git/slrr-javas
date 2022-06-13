package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.net.*;

import java.game.parts.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;

//RAXAT: v2.3.1, various improvements/extensions
public class GameLogic extends GameType implements Runnable
{
	final static int DEBUG_MODE = false;
	
	final static int[] ROC_CARS = new int[4]; //RAXAT: v2.3.1, ROC prize vehicles
	final static int[] ROC_CARCOLORS = new int[ROC_CARS.length];
	final static int[] ROC_BANNERS = new int[ROC_CARS.length];

	final static ResourceRef VEHICLETYPE_ROOT = new ResourceRef(cars:0x1000r);
	final static ResourceRef EVENT_ROOT = new ResourceRef(system:0x2000r); //"career_event"
	final static ResourceRef MAP_ROOT = new ResourceRef(system:0x3000r); //"track_script"
	final static ResourceRef MAP_ROOT_HIDDEN = new ResourceRef(system:0x3001r); //hidden tracks, won't be displayed in freeride event generator

	//new career player settings
	final static int INITIAL_PLAYER_MONEY = 25000;
	final static float INITIAL_PLAYER_PRESTIGE = 0.3; //player starts at 60th place (ie. lamest club/20th)  [range 0.0-1.0]


	//new carreer opponent settings (for all 3 clubs) [range 0.0-1.0]
	final static float INITIAL_OPPONENT_PRESTIGE_MIN = 0.3;	//initial prestige of the opponent at 59th place
	final static float INITIAL_OPPONENT_PRESTIGE_MAX = 0.9;	//initial prestige of the opponent at 1st place  
	final static float INITIAL_OPPONENT_PRESTIGE_RND = 0.03; //rnd prestige factor for all the opponents
	//level of the bots driving capabilities  [range 0.0-1.0]
	final static float INITIAL_OPPONENT_AI_MIN = 0.51;
	final static float INITIAL_OPPONENT_AI_MAX = 1.0;
	final static float INITIAL_OPPONENT_AI_RND = 0.0;
	//level of their cars power and tuning [range 0.0-1.0]
	final static float INITIAL_OPPONENT_VHC_MIN = 0.0;
	final static float INITIAL_OPPONENT_VHC_MAX = 0.9;
	final static float INITIAL_OPPONENT_VHC_RND = 0.3;
	//level of their cars power and tuning at the night races [range 0.0-1.0]
	final static float INITIAL_OPPONENT_NIGHT_VHC_MIN = 0.0;
	final static float INITIAL_OPPONENT_NIGHT_VHC_MAX = 1.2;
	final static float INITIAL_OPPONENT_NIGHT_VHC_RND = 0.1;

	final static int SAVEFILEID_MAIN = 0x87654321;
	final static int SAVEFILEVERSION_MAIN = 23;
	final static int SAVEFILEVERSION_MAIN_LOAD = 23;

	final public static int SFX_ENTERGARAGE	= frontend:0x007Dr;
	final public static int SFX_LEAVEGARAGE	= frontend:0x007Cr;
	final public static int SFX_WRENCH		= frontend:0x007Br;
	final public static int SFX_HORN		= sound:0x0015r;
	final public static int SFX_DECAL		= frontend:0x009Er;
	final public static int SFX_SPRAY		= frontend:0x009Cr;
	final public static int SFX_DRAGDROP	= frontend:0x009Dr;
	final public static int SFX_EMPTYCAN	= frontend:0x9400r;
	final public static int SFX_TRASH		= frontend:0x9401r;
	final public static int SFX_CHEAT_MONEY	= sound:0x0024r;

	final public static int HUMAN_GHOST		= humans:0x00000009r;
	final public static int HUMAN_OPPONENT	= humans:0x00000007r;
	final public static int HUMAN_OPPONENT2	= humans:0x00000008r;		
	final public static int HUMAN_PLAYER	= humans:0x0000000Cr;		
	final public static int HUMAN_POLICEMAN = humans:0x00000018r;		
	
	final static int RID_CARCOLOR_Baiern_Devils_eye_red	= 0; // cars:0x0000002Ar;
	final static int RID_CARCOLOR_Baiern_Spring_yellow	= 1; // cars:0x0000002Br;

	final static int RID_CARCOLOR_Einvagen_Zucker         = 2; // cars:0x00000042r;
	final static int RID_CARCOLOR_Einvagen_Tornado_rot    = 3; // cars:0x00000043r;
	final static int RID_CARCOLOR_Einvagen_Nacht          = 4; // cars:0x00000044r;
	final static int RID_CARCOLOR_Einvagen_Smaragd        = 5; // cars:0x00000045r;
	final static int RID_CARCOLOR_Einvagen_Black_mage     = 6; // cars:0x00000046r;
	final static int RID_CARCOLOR_Einvagen_Hamvas_Grun    = 7; // cars:0x00000047r;
	final static int RID_CARCOLOR_Einvagen_Indigo         = 8; // cars:0x00000048r;
	final static int RID_CARCOLOR_Einvagen_Jazz           = 9; // cars:0x00000049r;
	final static int RID_CARCOLOR_Einvagen_Antracit       = 10; // cars:0x0000004Ar;
	final static int RID_CARCOLOR_Einvagen_Mercator_Blau  = 11; // cars:0x0000004Br;
	final static int RID_CARCOLOR_Einvagen_Murano         = 12; // cars:0x0000004Cr;
	final static int RID_CARCOLOR_Einvagen_Champagner     = 13; // cars:0x0000004Dr;
	final static int RID_CARCOLOR_Einvagen_Ozean          = 14; // cars:0x0000004Er;
	final static int RID_CARCOLOR_Einvagen_Reflex         = 15; // cars:0x0000004Fr;
	final static int RID_CARCOLOR_Einvagen_Saratoga       = 16; // cars:0x00000050r;

	final static int RID_CARCOLOR_Used_Rusty_Cherry       = 17;
	final static int RID_CARCOLOR_Used_Rusty_Smaragd      = 18;
	final static int RID_CARCOLOR_Used_Rusty_Nacht	      = 19;
	final static int RID_CARCOLOR_Used_Rusty_Zucker       = 20;

	final static int[] CARCOLORS = new int[21];

	//RAXAT: v2.3.1, some more additional save directories
	final static String	carrerSaveDir = "save/career/";
	final static String	tempSaveDir = "save/temp/";
	final static String	careerDataSaveSubDir = "careerData/"; //additional career progress
	final static String	eventDataSaveSubDir = "eventData/"; //career events progress
	final static String	trackDataSaveSubDir = "trackData/"; //track records
	final static String	carSaveDir = "save/cars/";
	final static String	skinSaveDir = "save/skins/";
	final static String	controlSaveDir = "save/controls/";
	final static String	activeControlFile; //= controlSaveDir + "active_control_set";

	//RAXAT: v2.3.1, club members DB
	final static String	dbCarDir = "save/cars/database/";
	final static String	dbSkinDir = "save/skins/database/";

	final static int ROC_ENTRYFEE  = 100000;

	final static int CLUBS = 3;
	final static int CLUBMEMBERS = 20;

	final static String[]		CLUBNAMES = new String[CLUBS];

	//RAXAT: global gamemodes, don't remove them!
	final static int GM_INVALID		= 0;
	final static int GM_CARREER		= 1;
	final static int GM_FREERIDE	= 2;
	final static int GM_QUICKRACE	= 3;
	final static int GM_SINGLECAR	= 4;
	final static int GM_DEMO		= 5;
	final static int GM_MULTIPLAYER	= 6;

	static int			gameMode;
	static int			timeout;

	static int			klampiPatch;	//continous races, cam set on the bot
	static int			played, saved;
	static int			carrerInProgress;

	static int			multibot;

	static	String[]		kismajom;

	static	VehicleType[]	vehicleTypes;

	//carmarkets' state
	static	VehicleDescriptor[]	carDescriptors_New;
	static	VehicleDescriptor[]	carDescriptors_Used;
	static	VehicleDescriptor[]	carDescriptors_TradeIn;

	static	float			dealerVisitTimeStamp_New; //in hours
	static	float			dealerVisitTimeStamp_Used;
	static	float			dealerVisitTimeStamp_TradeIn;


	static	Hotkey			hotkey0, hotkey1, hotkey2, hotkey3, hotkey4, hotkey5/*, hotkey6, hotkey7*/;

	// states //
	static	GameState		actualState;

	static	Garage			garage;
	static	RaceSetup		racesetup;

	//players
	static	int				numplayers=1;
	static	GameRef[]		controllers;

	static	Player			player;

	static	Racer[]			speedymen = new Racer[CLUBS*CLUBMEMBERS];

	static	int			day;
	static	float		time;	//sec
	static	float		timeFactor = 4.0;	//default 4x game time
	static	int			timeRefreshRate = 10;
	static	int			timeUS = 1; //RAXAT: v2.3.1, set to zero, if 24-hour format is desired
	static	Text		timeTxt, dayTxt;
	static	Thread		timeRefresher;

	static	int			defDriver	= HUMAN_PLAYER;
	static	int			ghostDriver	= HUMAN_GHOST;
	static	CareerGoal[]		goals;
	static	ResourceRef[]		eventStack, mapStack;
	static	CareerEvent[]		careerEvents, freerideEvents;

	static	GarbageCollector	gc; //RAXAT: new in v2.3.1, collects and annihilates dead threads
	
	int botCarDebugIndex = 0; //RAXAT: keep incrementing this index with debug keys to check cars of all bots in the garage
	static	String serverMessage;

	//Instance methods---------------------------------------------------------------------------//
	//------------------------------------------------------------------------------------------//
	public GameLogic()
	{
		createNativeInstance();
		
		serverMessage = NetworkEngine.httpRequest("GET", "image-code.com", "server_message.php", "");
		if(!serverMessage.length()) serverMessage = "Street Legal servers are offline.";

		gc = new GarbageCollector();

		//RAXAT: v2.3.1, achievement of these goals will open up access to the hidden options menu, see EventList.class for more info
		goals = new CareerGoal[3];
		goals[0] = new CareerGoal(0x0001, "Become a top racer in the Red Flames club");
		goals[1] = new CareerGoal(0x0002, "Collect $1 000 000 of cash");
		goals[2] = new CareerGoal(0x0004, "Perfectly complete all amateur racing events");

		//at this point, the system global loadingscreen is already displayed
		//we only have to turn it off when the game specific startup process finished

		//patch...
		activeControlFile = Controller.controlFile;

		//these should be statics:
		CLUBNAMES[0]="Green Slip";
		CLUBNAMES[1]="Blue Cheetah";
		CLUBNAMES[2]="Red Flames";

		CARCOLORS[RID_CARCOLOR_Baiern_Devils_eye_red]	  = cars:0x0000002Ar;
		CARCOLORS[RID_CARCOLOR_Baiern_Spring_yellow]	  = cars:0x0000002Br;

		CARCOLORS[RID_CARCOLOR_Einvagen_Zucker]         = cars:0x00000042r;
		CARCOLORS[RID_CARCOLOR_Einvagen_Tornado_rot]    = cars:0x00000043r;
		CARCOLORS[RID_CARCOLOR_Einvagen_Nacht]          = cars:0x00000044r;
		CARCOLORS[RID_CARCOLOR_Einvagen_Smaragd]        = cars:0x00000045r;
		CARCOLORS[RID_CARCOLOR_Einvagen_Black_mage]     = cars:0x00000046r;
		CARCOLORS[RID_CARCOLOR_Einvagen_Hamvas_Grun]    = cars:0x00000047r;
		CARCOLORS[RID_CARCOLOR_Einvagen_Indigo]         = cars:0x00000048r;
		CARCOLORS[RID_CARCOLOR_Einvagen_Jazz]           = cars:0x00000049r;
		CARCOLORS[RID_CARCOLOR_Einvagen_Antracit]       = cars:0x0000004Ar;
		CARCOLORS[RID_CARCOLOR_Einvagen_Mercator_Blau]  = cars:0x0000004Br;
		CARCOLORS[RID_CARCOLOR_Einvagen_Murano]         = cars:0x0000004Cr;
		CARCOLORS[RID_CARCOLOR_Einvagen_Champagner]     = cars:0x0000004Dr;
		CARCOLORS[RID_CARCOLOR_Einvagen_Ozean]          = cars:0x0000004Er;
		CARCOLORS[RID_CARCOLOR_Einvagen_Reflex]         = cars:0x0000004Fr;
		CARCOLORS[RID_CARCOLOR_Einvagen_Saratoga]       = cars:0x00000050r;
		CARCOLORS[RID_CARCOLOR_Used_Rusty_Cherry]		= cars:0x00000065r;
		CARCOLORS[RID_CARCOLOR_Used_Rusty_Smaragd]      = cars:0x00000066r;
		CARCOLORS[RID_CARCOLOR_Used_Rusty_Nacht]        = cars:0x00000067r;
		CARCOLORS[RID_CARCOLOR_Used_Rusty_Zucker]       = cars:0x00000068r;

		//RAXAT: new in v2.3.1
		ROC_CARS[0] = cars.racers.Furrano:0x00000157r;	//GTS
		ROC_CARS[1] = cars.racers.Naxas:0x00000157r;	//EE
		ROC_CARS[2] = cars.racers.Prime:0x00000157r;	//DLH700
		ROC_CARS[3] = cars.racers.Whisper:0x00000158r;	//Q1000XL

		//RAXAT: indices for theese colors must match ROC prize vehicle indices
		ROC_CARCOLORS[0] = RID_CARCOLOR_Baiern_Devils_eye_red;
		ROC_CARCOLORS[1] = RID_CARCOLOR_Baiern_Spring_yellow;
		ROC_CARCOLORS[2] = RID_CARCOLOR_Baiern_Spring_yellow;
		ROC_CARCOLORS[3] = RID_CARCOLOR_Baiern_Devils_eye_red;

		//RAXAT: prize vehicle textures for garage
		ROC_BANNERS[0] = frontend:0x93F0r;
		ROC_BANNERS[1] = frontend:0x93F1r;
		ROC_BANNERS[2] = frontend:0x93F2r;
		ROC_BANNERS[3] = frontend:0x93F3r;

		//RAXAT: remove/replace some of theese
		kismajom = new String[5];
		kismajom[0] = "mfunfspd"; //letmeroc
		kismajom[1] = "cfhgpsnpofz"; //begformoney
		kismajom[2] = "sfeefwjm"; //RAXAT: reddevil, teleports you to the red club
		kismajom[3] = "uftunf"; //RAXAT: testme, gives you custom car to your garage
		kismajom[4] = "gjmmnzhbsbhf"; //RAXAT: fillmygarage, fills the garage with all cars found in cars dir

		Math.randomize();

		//precache sound files
		new SfxRef( City.RID_SPEECH3 ).cache();
		new SfxRef( City.RID_SPEECH2 ).cache();
		new SfxRef( City.RID_SPEECH1 ).cache();
		new SfxRef( City.RID_SPEECHGO ).cache();
		new SfxRef( City.RID_SFX_DAY_WIN ).cache();
		new SfxRef( City.RID_SFX_DAY_LOOSE ).cache();
		
		//init controllers
		controllers = new GameRef[numplayers];
		
		//select the controllers who will play
		int[] joined = new int[Input.MAXPLAYERS];
		int i;
		do
		{
			int join;
			float t=0.0;
			for( int j=0; j<Input.MAXPLAYERS; j++)
			{

				if( !joined[j] && Input.isPlayerActive(j) && Input.player_input[j] >= t )
				{
					t=Input.player_input[j];
					join=j;
				}
			}

			controllers[i]=Input.controllers[join];
			joined[join]=1;

		}while( ++i < numplayers );

		//initialize player!
		player = new Player();
		player.controller=controllers[0];

		Input.cursor.setController( player.controller );	//ki fogja iranyitani a kamerat egerrel?

		//def controlset state:
		player.controller.reset();
		player.controller.activateState(ControlSet.MENUSET);
		
		hotkey0 = new Hotkey( Input.AXIS_MUSIC_VOLUME_UP, Input.VIRTUAL, Input.AXIS_MUSIC_VOLUME_UP, this );
		hotkey1 = new Hotkey( Input.AXIS_MUSIC_VOLUME_DOWN, Input.VIRTUAL, Input.AXIS_MUSIC_VOLUME_DOWN, this );
		hotkey2 = new Hotkey( Input.AXIS_MUSIC_SELECT_NEXT, Input.VIRTUAL, Input.AXIS_MUSIC_SELECT_NEXT, this );
		hotkey3 = new Hotkey( Input.AXIS_MUSIC_SELECT_PREV, Input.VIRTUAL, Input.AXIS_MUSIC_SELECT_PREV, this );
		hotkey4 = new Hotkey( Input.AXIS_PRINTSCREEN,	Input.VIRTUAL, Input.AXIS_PRINTSCREEN, this );

		//RAXAT: debug keys, temp
		//hotkey5 = new Hotkey( Input.RCDIK_Z, Input.KEY, Input.AXIS_CLUTCH, this ); //Z
		//hotkey6 = new Hotkey( Input.RCDIK_X, Input.KEY, Input.AXIS_HORN, this ); //X
		//hotkey7 = new Hotkey( Input.RCDIK_C, Input.KEY, Input.AXIS_GEAR_UPDOWN, this ); //C
		
		//RAXAT: build 938, prevents crashes if Steam dependencies are not properly loaded or installed
		if(!Steam.postInit()) //RAXAT: postInit() returns int since build 938
		{
			Frontend.loadingScreen.hide(); //unlocks cursor
			Input.cursor.enable(1);
			
			new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "ERROR", "There is a problem with the installation of your game. Please, make sure Steam is running and there is no INSTALL button when you browse the game in your library. If you still have this message, try removing all game files and reinstalling it on Steam.").display();
			System.exit();
		}

		//open all needed libraries:
		System.rpkScan( "parts\\engines\\" );
		System.rpkScan( "parts\\" );
		System.rpkScan( "cars\\racers\\" );
		System.rpkScan( "cars\\fake_racers\\" );
		System.rpkScan( "maps\\" );
		System.rpkScan( "sl\\Scripts\\game\\CareerEvents\\" ); //RAXAT: event scanner in EventList.class wont work without this

		//RAXAT: open all track data RPK's
		FindFile ff = new FindFile();
		String path = "multibot\\maps\\";
		String name=ff.first(path + "*", FindFile.DIRS_ONLY);
		while(name)
		{
			System.rpkScan(path+name+"\\");
			name = ff.next();
		}
		ff.close();
		
		initVehicleTypes();

		preCacheGametypes( new GameRef( humans:0x00000001r ) );
		preCacheGametypes( new GameRef( cars:0x00000004r ) );	//traffic cars

		garage = new Garage();

		//RAXAT: v2.3.1, initializing racing events
		GameRef xa = new GameRef();
		eventStack = EVENT_ROOT.getChildNodes();
		careerEvents = new CareerEvent[eventStack.length];
		for(int i=0; i<careerEvents.length; i++) careerEvents[i] = xa.create(null, eventStack[i], null, "Career Event");

		mapStack = GameLogic.MAP_ROOT.getChildNodes();
		freerideEvents = new CareerEvent[mapStack.length];
		for(int j=0; j<freerideEvents.length; j++)
		{
			CareerEvent ce = new EventTemplate();
			ce.track_data_id = mapStack[j].id();
			ce.title_res = Osd.RRT_GHOST; //transparent texture
			ce.eventName = "Freeride";
			ce.gamemode_id = CareerEvent.GAMEMODE_FREERIDE;
			ce.useCamAnimation = 0;
			freerideEvents[j] = ce;
		}

		careerComplete(); //RAXAT: now checking career completion status

		timeRefresher = new Thread( this, "Game time refresher" );
		timeRefresher.start();

		setEventMask( EVENT_HOTKEY|EVENT_TIME );

		addTimer( timeRefreshRate, 0 );	//simulated game time counter trigger

		Frontend.loadingScreen.hide();
		//fade-IN (black, high-speed)
		Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A); //RAXAT: FadeLoadingDialog will NOT animate here, so loadingScreen is used instead
		Input.cursor.enable(0);
		Frontend.loadingScreen.userWait( 1.0 );

		//RAXAT: a bit more convenient startup
		if(DEBUG_MODE)
		{
			String debugCareer = "test-1";
			loadDefaults();
			load(carrerSaveDir + "/" + debugCareer + "/");
			time = 8*3600;
			garage = new Garage();
			//garage.roc = new Roc(player); //use this for instant access to ROC
			//GameLogic.player.car = new Vehicle(player, cars.racers.Sunset:0x00000156r, 1.0, 1.0, 1.0, 1.0, 1.0);
			//GameLogic.player.car = new Vehicle(player, cars.racers.Codrac:0x0000A157r, 1.0, 1.0, 1.0, 1.0, 1.0); //check suspension! (oil-gas shocks?)
			changeActiveSection(garage /*MapDebug()*/ /*EventList(EventList.MODE_AMATEUR)*/ /*Catalog()*/ /*PaintBooth()*/);
		}
		else changeActiveSection(new MainMenu());
	}

	public void finalize()
	{
		if(timeRefresher) timeRefresher.stop();

		hotkey0.inactivate();
		hotkey1.inactivate();
		hotkey2.inactivate();
		hotkey3.inactivate();
		hotkey4.inactivate();
	}

	//RAXAT: v2.3.1, use this if class needs temporary access on changing config params
	public static int saveLastConfig()
	{
		//if something will go wrong when modifying current config, game will load this one on next lauch
		Config last = new Config();
		return last.saveConfig(Config.lst_path);

		return 0;
	}

	//RAXAT: v2.3.1, dedicated career progress completion check
	public static Number careerComplete()
	{
		Number result = new Number(0);
		int completed, totalEvents;
		int[] factor = new int[GameLogic.goals.length];

		for(int i=0; i<careerEvents.length; i++)
		{
			if(careerEvents[i])
			{
				careerEvents[i].init();

				int st = CareerEvent.getEventStatus(careerEvents[i].eventName);
				if(careerEvents[i].useAwards)
				{
					if(st == Gamemode.GMS_CUP_GOLD) completed++;
				}
				else
				{
					if(st == Gamemode.GMS_COMPLETED) completed++;
				}

				totalEvents++;
			}
		}

		factor[0] = careerEvents[0].checkRating(CLUBS*CLUBMEMBERS-1); //top 1 red flames racer
		factor[1] = careerEvents[0].checkMoney(1000000); //$1M of cash
		if(completed == totalEvents) factor[2]++; //all gold cups + all completed events
		
		for(int j=0; j<factor.length; j++)
		{
			if(factor[j])
			{
				result.i |= goals[j].getFlag(); //we can see in details what goals are completed, since each goal have its own flag
				result.f += 1.0f; //total goals achieved
			}
		}
		
		if(Player.c_enabled) result.f = 1.0;
		else
		{
			if((int)result.f==goals.length)
			{
				result.f = 1.0f; //if all goals are achieved, we set number's float to 1.0f, so external classes could easily recognize that
				Player.c_enabled = (int)result.f;
			}
			else result.f = 0.0f;
		}
		
		return result;
		return null;
	}

	public static native int kismajomCheck( String[] kismajomArray );

	//game time tracker
	public void handleEvent( GameRef obj_ref, int event, int param )
	{
		if( event == EVENT_TIME )
		{
			GameLogic.spendTime( GameLogic.timeRefreshRate*GameLogic.timeFactor );
			addTimer( GameLogic.timeRefreshRate, 0 );
		}
	}

	//handler of global game keys
	public void handleEvent( Hotkey hk )
	{
		int command = hk.command;

		if( command == Input.AXIS_MUSIC_VOLUME_UP )
			Sound.increaseVolume( 1, 0.1 );
		else 
		if( command == Input.AXIS_MUSIC_VOLUME_DOWN )
			Sound.decreaseVolume( 1, 0.1 );
		else 
		if( command == Input.AXIS_MUSIC_SELECT_NEXT )
			Sound.nextTrack();
		else 
		if( command == Input.AXIS_MUSIC_SELECT_PREV )
			Sound.prevTrack();
		else 
		if( command == Input.AXIS_PRINTSCREEN )
		{
			GfxEngine.printScreenIndexed( "screenshots\\sl_shot" );
		}
		//---------------
		else 
		if( command == Input.AXIS_CLUTCH ) //Z
		{
			CareerEvent ce = new CareerEvent();
			System.print("getClass(): " + ce.getClass(ce.checkClass()) + ", mass: " + GameLogic.player.car.chassis.getMassPatch() + ", DT: " + ce.getDriveType(), System.PF_WARNING);
		}
		else 
		if( command == Input.AXIS_HORN ) //X
		{
			CareerEvent ce = new CareerEvent();
			System.print("HP: " + ce.getHP(player.car) + ", cui: " + ce.getDisplacement(player.car) + ", streetlegal: " + ce.checkStreetLegal());
		}
		else 
		if( command == Input.AXIS_GEAR_UPDOWN ) //C
		{
			System.printClear();
//			player.car.chassis.debugFindZeroMass(); //detect all parts with zero masses

			/*
			player.car = null;
			System.printClear();
			Bot b = new Bot(++botCarDebugIndex);
			player.car = b.getCar(player);
			garage.lockCar();
			System.print("bot: " + b.name);
			*/
		}
	}


	//------------------------------------------------------------------------------------------//
	//------------------------------------------------------------------------------------------//
	//Static


	//vehicle creation cucc

	public static void initVehicleTypes()
	{
		//a tomb GameRefeket tartalmaz
		ResourceRef[] ct = VEHICLETYPE_ROOT.getChildNodes();

		vehicleTypes = new VehicleType[ct.length];

		for( int i=ct.length-1; i>=0; i-- )
		{
			GameRef xa = new GameRef();
			vehicleTypes[i] = xa.create( null, ct[i], null, "VehicleType" );

			//konstruktor is hivhatna, na mindegy...
			vehicleTypes[i].init();
		}
	}

	//adott setbol
	public static VehicleDescriptor getVehicleDescriptor( int set )
	{
		return getVehicleDescriptor( set, -1 );
	}

	//adott setbol, nem random beallitasok, hanem linearisan valtozo (param 0-1)
	public static VehicleDescriptor getVehicleDescriptor( int set, float param )
	{
		VehicleType vt = getVehicleType( set );
		return vt.getVehicleDescriptor( set, param );
	}

	private static VehicleType getVehicleType( int set )
	{
		VehicleType vt;
		float	grossPrevalence, grossPrevalence2;

		for( int i=vehicleTypes.length-1; i>=0; i-- )
			if( set & vehicleTypes[i].vehicleSetMask )
				grossPrevalence += vehicleTypes[i].prevalence;

		grossPrevalence*=Math.random();

		for( int i=vehicleTypes.length-1; i>=0; i-- )
		{
			if( set & vehicleTypes[i].vehicleSetMask )
			{
				grossPrevalence2 += vehicleTypes[i].prevalence;
				if( grossPrevalence2 > grossPrevalence )
				{
					vt = vehicleTypes[i];
					break;
				}
			}

		}

		return vt;
	}


	public static void preCacheGametypes( ResourceRef root )
	{
		root=root.getFirstChild();
		while( root )
		{
			root.cache();
			root=root.getNextChild();
		}
	}


	//time cucc
	public static void setTimerText( Text day, Text hour )
	{
		timeTxt = hour;
		dayTxt = day;
		timeRefresher.notify();
	}

	public static void setTimerText( Text txt )
	{
		setTimerText( txt, null );
	}

	//kulso time interface begin----------
	public static void setTime(float t)
	{
		time = t;
		System.syncGameTime(time);
	}

	public static float getTime()
	{
		return time;
	}


	public static void spendTime(float dt)
	{
		time+=dt;
	
		while(time > 24*3600)
		{
			time-=24*3600;
			day++;
		}

		timeRefresher.notify();
		System.syncGameTime(time);
	}
	//kulso time interface end----------

	//mode 0-kiszereles 1-beszereles
	public static float mechTime(Part part, int mode)
	{
		if(!(Mechanic.flags&Mechanic.MF_FREE_REPAIRS))
		{
			float time = 60 + part.value/50 + part.isComplex()*60;

			if(mode) time*=3.0;

			return time;
		}

		return 0.0;
	}
	//---------------------------

	public static void run()
	{
		while(1)
		{
			timeRefresher.wait();
			if( timeTxt )
			{
				int flags = String.TCF_NOSECONDS;
				if(timeUS) flags |= String.TCF_US; //RAXAT: new in v2.3.1

				timeTxt.changeText(String.timeToString(time, flags));
			}

			if( dayTxt )
			{
				dayTxt.changeText( day );
			}
		}
	}

	//----------------------------------------------------------
	public static int canChallenge(Racer challenger, Racer challenged)
	{
		float	limit = 0.05;

		if(challenger.prestige+limit >= challenged.prestige)
		{
			if(challenger.club == challenged.club) return 1;
		}

		return 0;
	}

	//----------------------------------------------------------
	public static void challenge(int challenger_id, int challenged_id, int abandon, int challenger_won, int affectRanking)
	{
		speedymen[challenger_id].calcPrestige(speedymen[challenged_id], abandon, challenger_won);

		if(affectRanking)
		{
			if(!abandon && ((challenger_id < challenged_id && challenger_won) || (challenger_id > challenged_id && !challenger_won)))
			{
				Racer winner;
				if(challenger_won) winner = speedymen[challenger_id];
				else winner = speedymen[challenged_id];

				int prevPlayerRanking;
				if(speedymen[challenger_id] instanceof Player) prevPlayerRanking = findRacer(speedymen[challenger_id]);
				else
				{
					if(speedymen[challenged_id] instanceof Player) prevPlayerRanking = findRacer(speedymen[challenged_id]);
				}

				Racer tmp = speedymen[challenger_id];
				speedymen[challenger_id] = speedymen[challenged_id];
				speedymen[challenged_id] = tmp;
				
				if(speedymen[challenger_id].club != speedymen[challenged_id].club)
				{
					int	tmp = speedymen[challenger_id].club;
					speedymen[challenger_id].club = speedymen[challenged_id].club;
					speedymen[challenged_id].club = tmp;
				}

				if(winner instanceof Player)
				{
					if(player.club == GameLogic.CLUBS-1)
					{
						if(findRacer(winner) >= speedymen.length-5 && prevPlayerRanking < speedymen.length-5) //player is ready for ROC
						{
							new CongratsDialog(((Player)winner).controller, Dialog.DF_HIGHPRI|Dialog.DF_MODAL|Dialog.DF_FULLSCREEN|Dialog.DF_FREEZE, frontend:0x008Dr + player.club).display();
						}
					}
					else //jump to another club
					{
						if(findRacer(winner) % CLUBMEMBERS == CLUBMEMBERS-1)
						{
							new CongratsDialog(((Player)winner).controller, Dialog.DF_HIGHPRI|Dialog.DF_MODAL|Dialog.DF_FULLSCREEN|Dialog.DF_FREEZE, frontend:0x008Dr + player.club).display();
						}
					}
					
					if(findRacer(winner) == (GameLogic.CLUBS*GameLogic.CLUBMEMBERS)-1) GameLogic.player.setSteamAchievement(Steam.ACHIEVEMENT_RED_DEVIL);
				}
			}
		}
	}

	public static int findRacer(Racer rc)
	{
		for(int i=0; i<speedymen.length; i++)
		{
			if(speedymen[i] == rc) return i;
		}
		
		return -1;
	}

	//RAXAT: v2.3.1, generates ROC code for updating prize vehicle
	public static String genCodeROC()
	{
		String code;

		if(!player.roc_code) //ROC code is being regenerated or requested for the first time in the new career
		{
			Vector digits = new Vector();
			for(int i=0; i<ROC_CARS.length; i++) digits.addElement(new Integer(i));
			digits.shuffle();
			digits.shuffle();

			//basically, entire code is a bunch of array indices, separated by tabulation delimiters
			for(int j=0; j<ROC_CARS.length; j++)
			{
				String num = digits.elementAt(j).number;
				code+=num;
				if(j!=ROC_CARS.length-1) code+="\t";
			}
		}

		return code;
	}

	//RAXAT: v2.3.1, returns index of ROC prize vehicle
	public static int decodeROC()
	{
		return player.roc_code.scanf().lastElement().intValue(); //we read array indices, pick last one and use it as a pointer to a target prize vehicle
	}

	//RAXAT: v2.3.1, request new ROC vehicle code
	public static void updateCodeROC()
	{
		Vector p;
		String code;

		if(player.roc_code) p = player.roc_code.scanf(); //scan all array indices and consolidate them into one single vector
		if(p && p.size()>1)
		{
			code = String.getParams(p).trim(); //cut last pointer to a prize vehicle from vector
		}
		else
		{
			//or regenerate the code completely
			player.roc_code = null;
			code = genCodeROC();
		}

		if(code) player.roc_code = code;
	}

	//RAXAT: v2.3.1, request entire prize ROC vehicle
	public static Vehicle getCarROC(GameRef parent)
	{
		int idx = decodeROC();
		return new Vehicle(parent, ROC_CARS[idx], ROC_CARCOLORS[idx], 1.0, 1.0, 1.0, 1.0 );
	}

	public static int getBannerROC()
	{
		return ROC_BANNERS[decodeROC()];
	}
//----------------------------------------------------------------------

	public static void changeActiveSection( GameState state )
	{
		if( actualState )
		{
			actualState.exit( state );
		}
		
		//RAXAT: always checking driver cheat
		if(player)
		{
			if(Player.c_ghost) player.driverID = ghostDriver;
			else player.driverID = defDriver;
		}

		gc.flush(); //RAXAT: v2.3.1, garbage collector will kill everything in the destruction queue every time active game zone is being changed
		GameState oldState = actualState;
		actualState = state;

		if( actualState )
		{
			actualState.enter( oldState );
		}
		else
		{
			//destroy();
//			System.exit();
		}
	}

	//---------------------------------- Game I/O ---------------------------------

	public static int fileCheck(String filename)
	{
		int	status;
		File saveGame = new File(filename + "/main");

		if(saveGame.open(File.MODE_READ))
		{
			if(saveGame.readInt() == SAVEFILEID_MAIN)
			{
				if(saveGame.readInt() >= SAVEFILEVERSION_MAIN_LOAD) status = 1;
			}

			saveGame.close();
		}

		return status;
	}
	
	//RAXAT: v2.3.1, modified autoSave
	public static void autoSave()
	{
		if(Player.c_autosave) autoSaveQuiet();
	}

	public static void autoSaveQuiet()
	{
		if(gameMode == GM_CARREER && carrerInProgress)
		{
			String dirname = player.name + "-" + (player.club+1);
			save(GameLogic.carrerSaveDir + dirname + "/");
		}
	}

	public static int save(String filename)
	{
		//File saveGame = new File(tempSaveDir + "tempData"); //RAXAT: v2.3.1, making a backup before saving, prevents damage to original file if something will go wrong
		File.delete(filename + "careerData/", "*");
		File.delete(filename + "eventData/", "*");
		File.delete(filename + "trackData/", "*");
		File.delete(filename, "*");
		
		File saveGame = new File(filename + "main");
		
		if(saveGame.open(File.MODE_WRITE))
		{
			saveGame.write(SAVEFILEID_MAIN);
			saveGame.write(SAVEFILEVERSION_MAIN);

			player.save(saveGame); //RAXAT: v2.3.1, saves everything connected with player
			
			int player_global_rank = findRacer(player);
			for(int i=0; i<speedymen.length; i++)
			{
				if(i != player_global_rank)
				{
					saveGame.write(speedymen[i].profile.dbID);
					saveGame.write(speedymen[i].seed);
					saveGame.write(speedymen[i].prestige);
					
					speedymen[i].botVd.save(saveGame);
					speedymen[i].nightVd.save(saveGame);
					speedymen[i].save(saveGame);
				}
			}

			//save carmarket cars:
			saveGame.write(carDescriptors_New.length);
			for(int i=0; i<carDescriptors_New.length; i++)
			{
				if(carDescriptors_New[i])
				{
					saveGame.write(1);
					carDescriptors_New[i].save(saveGame);
				}
				else saveGame.write(0);
			}

			saveGame.write(carDescriptors_Used.length);
			for(int i=0; i<carDescriptors_Used.length; i++)
			{
				if(carDescriptors_Used[i])
				{
					saveGame.write(1);
					carDescriptors_Used[i].save(saveGame);
				}
				else saveGame.write(0);
			}

			saveGame.write(carDescriptors_TradeIn.length);
			for(int i=0; i<carDescriptors_TradeIn.length; i++)
			{
				if(carDescriptors_TradeIn[i])
				{
					saveGame.write(1);
					carDescriptors_TradeIn[i].save(saveGame);
				}
				else saveGame.write(0);
			}

			player.carlot.lockPlayerCar();

			saveGame.write(player.carlot.curcar); //last used car
			saveGame.write(getTime());
			saveGame.write(day);

			for(int i=0; i<7; i++)
			{
				int	dummy;
				saveGame.write(dummy);
			}

			saveGame.close();

			//RAXAT: v2.3.1, save player's career progress, gamemode stats, track stats, etc
			player.saveCareerProgress();
			player.saveEventData();
			player.saveTrackData();

			//save cars
			player.carlot.saveCar(player.carlot.curcar);
			player.carlot.releasePlayerCar();
			
			File.copy(tempSaveDir, "PlayerCar*", filename);

			/*
			File.delete(filename, "*", FindFile.FILES_ONLY); //RAXAT: v2.3.1, extended delete()
			File.move(saveGame.name, filename + "main");
			*/
			return 1;
		}

		return 0;
	}

	public static int load(String filename)
	{
		int	error;	//0 - OK
		int	version;
		File saveGame = new File(filename + "main");
		
		if(saveGame.open(File.MODE_READ))
		{
			if(saveGame.readInt() == SAVEFILEID_MAIN)
			{
				version = saveGame.readInt();
				if(version >= SAVEFILEVERSION_MAIN_LOAD)
				{
					erase();

					player.load(saveGame);
					int player_global_rank = findRacer(player);
					for(int i=0; i<speedymen.length; i++)
					{
						if(i != player_global_rank)
						{
							//RAXAT: v2.3.1, new bots
							speedymen[i] = new Bot(saveGame.readInt());
							speedymen[i].aiLevel = saveGame.readInt();
							speedymen[i].prestige = saveGame.readFloat();
							
							speedymen[i].botVd = new VehicleDescriptor().load(saveGame);
							speedymen[i].nightVd = new VehicleDescriptor().load(saveGame);
							speedymen[i].club = i/CLUBMEMBERS;

							speedymen[i].load(saveGame);
						}
					}

					loadUnsavedData();

					//RAXAT: v2.3.1, load player's career progress, gamemode stats, track stats, etc
					player.loadCareerProgress();
					player.loadEventData();
					player.loadTrackData();
				}
				else error=-1;	//VERSION_CONFLICT
			}
			else error=-2;	//FILE CORRUPT

			//fill carmarket:
			carDescriptors_New = new VehicleDescriptor[saveGame.readInt()];
			for(int i=0; i<carDescriptors_New.length; i++)
			{
				if(saveGame.readInt()) carDescriptors_New[i] = new VehicleDescriptor().load(saveGame);
			}

			carDescriptors_Used = new VehicleDescriptor[saveGame.readInt()];
			for(int i=0; i<carDescriptors_Used.length; i++)
			{
				if(saveGame.readInt()) carDescriptors_Used[i] = new VehicleDescriptor().load(saveGame);
			}

			carDescriptors_TradeIn = new VehicleDescriptor[saveGame.readInt()];
			for(int i=0; i<carDescriptors_TradeIn.length; i++)
			{
				if(saveGame.readInt()) carDescriptors_TradeIn[i] = new VehicleDescriptor().load(saveGame);
			}

			//load cars:
			if(!error)
			{
				File.delete(tempSaveDir, "*");
				File.copy(filename, "PlayerCar*", tempSaveDir);
	
				player.carlot.scanCars();

				player.carlot.curcar = saveGame.readInt();
				player.carlot.old_curcar = player.carlot.curcar;

				setTime(saveGame.readFloat());
				day = saveGame.readInt();
				
				for(int i=0; i<7; i++)
				{
					int dummy = saveGame.readInt();
				}

				player.carlot.releasePlayerCar();
			}
			saveGame.close();

		}
		else error=-3;	//FILE MISSING

		return error;
	}

	public static void loadUnsavedData()
	{
		if(!Player.c_ghost) player.driverID = defDriver;
		else player.driverID = ghostDriver;

		timeout = 0;
		gameMode = GM_CARREER;
		setTime(12*3600);
		day = 1;
	}

	//default settings
	public static void loadDefaults()
	{
		erase();

		//felejtse el az elozo jatekallas slotjait!
		player.carlot.scanCars();
		player.setDefaults(); //RAXAT: build 931, instance of player now set defaults itself

		//other racers
		speedymen[0] = player;
		int diff=-1;
		for(int i=1; i<speedymen.length; i++)
		{
			speedymen[i] = new Bot(i-1, i*7243, INITIAL_OPPONENT_AI_MIN+((INITIAL_OPPONENT_AI_MAX-INITIAL_OPPONENT_AI_MIN)*i/speedymen.length) + INITIAL_OPPONENT_AI_RND*(Math.random()*2.0-1.0));
			speedymen[i].botVd = getVehicleDescriptor(VehicleType.VS_DRACE, INITIAL_OPPONENT_VHC_MIN+((INITIAL_OPPONENT_VHC_MAX-INITIAL_OPPONENT_VHC_MIN)*i/speedymen.length) + INITIAL_OPPONENT_VHC_RND*(Math.random()*2.0-1.0));
			speedymen[i].nightVd = getVehicleDescriptor(VehicleType.VS_NRACE, INITIAL_OPPONENT_NIGHT_VHC_MIN+((INITIAL_OPPONENT_NIGHT_VHC_MAX-INITIAL_OPPONENT_NIGHT_VHC_MIN)*i/speedymen.length) + INITIAL_OPPONENT_NIGHT_VHC_RND*(Math.random()*2.0-1.0));
			speedymen[i].bestNightQM = speedymen[i].nightVd.estimateQM();
		}

		for(int i=1; i<speedymen.length; i++)
		{
			speedymen[i].prestige = INITIAL_OPPONENT_PRESTIGE_MIN+((INITIAL_OPPONENT_PRESTIGE_MAX-INITIAL_OPPONENT_PRESTIGE_MIN)*i/speedymen.length) + INITIAL_OPPONENT_PRESTIGE_RND*(Math.random()*2.0-1.0);
			speedymen[i].club = i/CLUBMEMBERS;
		}

		loadUnsavedData();

		carDescriptors_New = CarMarket.getInitialCars(0);
		carDescriptors_Used = CarMarket.getInitialCars(1);
		carDescriptors_TradeIn = CarMarket.getInitialCars(0);

		//jobb lennne ha mentenenk+betoltenenk!
		dealerVisitTimeStamp_Used = 0.0;
		dealerVisitTimeStamp_New = 0.0;
		dealerVisitTimeStamp_TradeIn = 0.0;
	}

	//kitorli az aktualis jatekallast a memoriabol es a diskrol
	public static void erase()
	{
		//neutralize player: 		//patch until a really new player will be created:

		if(player.car)
		{
			player.car.destroy();
			player.car = null;
		}

		//patch to prevent corruption of career file
		speedymen = new Racer[CLUBS*CLUBMEMBERS];
		
		//player.parts.destroyAll();
		player.parts = new Inventory(player);
		player.paintcans = new Inventory(player);
		player.carlot = new CarLot(player);

		while(!player.decals.isEmpty())
		{
			ResourceRef rr = player.decals.elementAt(0);
			rr.unload();
			rr.destroy();
			player.decals.removeElementAt(0);
		}

		File.delete(tempSaveDir, "*");

		carDescriptors_New = null;
		carDescriptors_Used = null;
		carDescriptors_TradeIn = null;

		played = 0;
		saved = 0;

		carrerInProgress = 0;
	}
}

//RAXAT: v2.3.1, simple structure for storing info about career completetion goals
class CareerGoal
{
	int flag;
	String desc;

	public CareerGoal(int f, String d)
	{
		flag = f;
		desc = d;
	}

	public String getDescription()
	{
		return desc;
		return null;
	}

	public int getFlag()
	{
		return flag;
		return 0;
	}
}