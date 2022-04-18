package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.game.parts.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;

//RAXAT: v2.3.1, refactored code, painter is moved to paint booth, moveable parts interface
//todo: print car class somewhere and NOT print it in CarInfo
public class Garage extends Scene implements GameState
{
	final static int  RID_BTN_L			= frontend:0x0098r;
	
	final static int  RID_GARAGE1		= misc.garage:0x0000007ar;
	final static int  RID_GARAGE1_STUFF	= misc.garage:0x00000042r;
	final static int  RID_GARAGE2		= misc.garage:0x00000006r;
	final static int  RID_GARAGE2_STUFF	= misc.garage:0x00000018r;
	final static int  RID_GARAGE3		= misc.garage:0x000000c7r;
	final static int  RID_GARAGE3_STUFF	= misc.garage:0x0000003cr;

	//roc "garage"
	final static int  RID_GARAGE4		= misc.garage:0x0000002Fr;
	final static int  RID_GARAGE4_STUFF	= misc.garage:0x00000051r;
	
	//RAXAT: build 931, garage lift
	final static int  RID_GARAGE_LIFT	= misc.garage_objects:0x00000013r;
	
	//RAXAT: build 931, new time button
	final static float	TIME_BTN_HOLD_TIME			= 0.3f; //for time button
	final static int	TIME_BTN_INTERVAL_NORMAL	= 3600; //1 hour in one click
	final static int	TIME_BTN_INTERVAL_FAST		= 300; //fast forward time skip
	
	final static float	MOVE_PARTS_LIMIT			= 1.0;
	
	//RAXAT: build 931, constants for vehicle lift
	final static float	LIFT_ARM_AMPLITUDE_DEFAULT	= 1.5;
	final static float	LIFT_ARM_AMPLITUDE_MAX		= 1.5;
	final static float	LIFT_ARM_AMPLITUDE_STEP		= 0.0075;

	int		garageIndex;
	int[]	garageID, garageStuffID;
	ResourceRef[]	garageEnvMaps;

	RenderRef	light, lighttype;

	PhysicsRef	aknafedel = new PhysicsRef(); //RAXAT: garage bounding box

	//color constants
	final static int  BUTTON_DEFAULT_STATE_COLOR	= 0xFFC0C0C0;
	final static int  BUTTON_ACTIVE_STATE_COLOR	= 0xFFFFFFFF;

	//editing modes
	final static int  MODE_NONE	= 0;
	final static int  MODE_MECH	= 1;
	final static int  MODE_TEST	= 2;
	final static int  MODE_TUNE	= 3;
	final static int  MODE_MOVE	= 4;

	final static int  CMD_NONE			= 100;
	final static int  CMD_MAINMENU		= 101;
	final static int  CMD_MENU			= 107;
	final static int  CMD_ROC			= 108;
	final static int  CMD_HITTHESTREET	= 109;
	final static int  CMD_TESTTRACK		= 110;
	final static int  CMD_CARLOT		= 111;
	final static int  CMD_BUYCARS		= 112;
	final static int  CMD_CATALOG		= 113;
	final static int  CMD_CLUBINFO		= 114;
	final static int  CMD_CARINFO		= 115;
	final static int  CMD_TIME			= 116;
	final static int  CMD_MECHANIC		= 117;
	final static int  CMD_PAINT			= 118;
	final static int  CMD_ESCAPE		= 119;
	final static int  CMD_ROCRACE		= 120;
	final static int  CMD_ROCTEST		= 121;
	final static int  CMD_BUYCARSUSED	= 122;
	final static int  CMD_TEST			= 123;
	final static int  CMD_TUNE			= 124;
	final static int  CMD_ROCINFO		= 125;
	final static int  CMD_ROCQUIT		= 126;
	final static int  CMD_CHEATMONEY	= 127;
	final static int  CMD_BEGIN_ROC		= 128;
	final static int  CMD_LIFT			= 129;
	final static int  CMD_LIFT_ARM_MOVE	= 130;
	final static int  CMD_LIFT_DONE		= 131; //exit from lift
	final static int  CMD_TRADEIN		= 132;
	final static int  CMD_EVENTLIST		= 133;
	final static int  CMD_TESTDRIVE		= 134;
	final static int  CMD_DEBUG_1		= 140;
	final static int  CMD_DEBUG_2		= 141;
	final static int  CMD_DEBUG_3		= 142;
	final static int  CMD_DEBUG_4		= 143;
	final static int  CMD_DEBUG_5		= 144;
	
	final static int  CMD_MOVE_PARTS		= 145;
	final static int  CMD_MOVE_PARTS_DONE	= 146;
	final static int  CMD_MOVE_PARTS_RESET	= 147;
	final static int  CMD_MOVE_PARTS_SETPOS	= 148;
	final static int  CMD_MOVE_PARTS_SETORI	= 151;

	final static Vector3 defCarPos = new Vector3(0.0, 0.0, -0.5);
	final static Vector3 defLookPos = new Vector3(0.0, 0.0, -0.5);
	
	final static int DRIVEABLE_GARAGE = true; //RAXAT: now the garage could be driveable

	Multiplayer			multiplayer; //RAXAT: multiplayer static instance
	Player				player;
	ControlSetState		css;

	Roc			roc;

	Osd			osd;
	GameRef     camera;
	Vector3		cameraPos = new Vector3(-3.0, 1.5,-2.0);
	Ypr			cameraOri = new Ypr(-2.05, -0.25, 0);

	GameState	parentState;

	Mechanic	mechanic;
	int			filterEngine, filterBody, filterRGear; 

	int	mode, drag, move, mode_memory=MODE_MECH;
	int	prevMode;
	int	menuGroup, menuGroup2, tuneGroup, movePartsGroup, liftGroup;
	int	menuVisible;
	
	//RAXAT: build 931, vehicle lift stuff
	int carLifted = false;
	RenderRef stuff;
	Part lift;
	PhysicsRef liftPhys; //to collide with the lift in MODE_TEST
	Pori[] liftPori;
	float liftVerticalOffset = -0.815;
	float liftArmAmplitude = LIFT_ARM_AMPLITUDE_DEFAULT; //vertical distance travelled by lift arm
	Slider liftArmAmplitudeSlider;

	Gadget	timeBtn;
	float	timeBtnHoldTick = -1.0f;
	int		timeBtnInterval = TIME_BTN_INTERVAL_NORMAL;
	Text moneytxt, daytxt, timeTitleTxt, timeTxt, invLineTxt, infoline, prestigeTxt, carClassTxt;

	//RAXAT: moveable parts stuff
	Slider[] partPosSlider;
	Slider[] partOriSlider;
	
	Text	targetPartTxt;
	Part	targetPart;
	Vector3	partPos;
	Ypr		partOri;
	
	//RAXAT: the second car that appears in Red Flames garage only
	Vehicle	redFlamesVhc;
	Vector3	redFlamesVhcPos = new Vector3(9.6, 0.0, -3.5);
	Ypr		redFlamesVhcOri = new Ypr(0.0, 0.0, 0.0);

	public Garage()
	{
		createNativeInstance();

		//patch
		InventoryItem x = new InventoryItem();

		garageID=new int[GameLogic.CLUBS+1]; garageStuffID = new int[GameLogic.CLUBS+1];
		garageID[0] = RID_GARAGE1; garageStuffID[0] = RID_GARAGE1_STUFF;
		garageID[1] = RID_GARAGE2; garageStuffID[1] = RID_GARAGE2_STUFF;
		garageID[2] = RID_GARAGE3; garageStuffID[2] = RID_GARAGE3_STUFF;
		garageID[3] = RID_GARAGE4; garageStuffID[3] = RID_GARAGE4_STUFF;

		garageEnvMaps	 = new ResourceRef[GameLogic.CLUBS+1];
		garageEnvMaps[0] = new ResourceRef(maps.skydome:0x0036r);
		garageEnvMaps[1] = new ResourceRef(maps.skydome:0x0037r);
		garageEnvMaps[2] = new ResourceRef(maps.skydome:0x0038r);
		garageEnvMaps[3] = new ResourceRef(maps.skydome:0x0039r);
		
		liftPori = new Pori[GameLogic.CLUBS+1];
		liftPori[0] = new Pori(new Vector3(11.0, 0.8, 1.0), new Ypr(0.0));
		liftPori[1] = new Pori(new Vector3(-9.0, 0.8, 4.0), new Ypr(0.0));
		liftPori[2] = new Pori(new Vector3(-7.0, 0.8, 1.5), new Ypr(0.0));
		liftPori[3] = new Pori(new Vector3(3.5, 1.0, 10.0), new Ypr(0.0));

		internalScene = 1;
	}
	
	public void init()
	{
		//RAXAT: new in build 928
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
		{
			multiplayer = new Multiplayer();
			multiplayer.connect();
		}
		else multiplayer = null;
	}

	public void enter( GameState prevState )
	{
		if(mode_memory == MODE_TEST) mode_memory = MODE_MECH; //switching to default mode if we're back from a test drive
		
		if(camera) camera.command("render 0");
		Integrator.isCity = 0;

		if(!parentState) parentState = prevState;
		player = GameLogic.player;
		
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) player.setMoney(multiplayer.RPC("getMoney", null, true).intValue());

		//RAXAT: refactored code for loading images
		//add some smart and smooth fade-in/fade-out
		int pic;
		if(!roc)
		{
			switch(player.club)
			{
				case 0:
					pic = frontend:0x000002E5r; //green slip
					break;

				case 1:
					pic = frontend:0x000002EAr; //blue cheetah
					break;

				case 2:
					pic = frontend:0x000002EBr; //red flames
					break;
			}
		}
		else	pic = frontend:0x000002ECr; //roc
		Frontend.loadingScreen.show(new ResourceRef(pic));

		if(roc) garageIndex = GameLogic.CLUBS;
		else garageIndex = player.club;

		map = new GroundRef(garageID[garageIndex]);
		stuff = new RenderRef(map, garageStuffID[garageIndex], null);
		
		//vehicle lift
		lift = new GameRef().create(map, new GameRef(RID_GARAGE_LIFT),	"100,0,0,0,0,0", "garage_lift");
		lift.setMatrix(liftPori[garageIndex].pos, liftPori[garageIndex].ori);
		
		liftPhys = new PhysicsRef();
		liftPhys.createBox(map, 3.0, 5.0, 2.0, "garage_lift_phys");
		liftPhys.setMatrix(liftPori[garageIndex]);
		
		osd = new Osd();
		osd.globalHandler = this;
		osd.defSelection = 5;
		osd.orientation = 1;
		createOSDObjects();
		
		aknafedel.createBox(map, 2.0, 0.05, 5.0, "aknafedel");
		aknafedel.setMatrix(new Vector3(0.0,-0.035,0.0), null); //RAXAT: we decided to put it a bit lower, so the car won't get stuck in MODE_TEST

		if(player.car)
		{
			player.car.setDamageMultiplier(0.0);
			player.car.setCruiseControl(0);
		}

		//---------------------------------time of day dependent stuff:
		addSceneElements(GameLogic.getTime());
		GfxEngine.setGlobalEnvmap(garageEnvMaps[garageIndex]);
		//-----------------------------------------------------------------------

		camera = new GameRef(map, GameRef.RID_CAMERA, cameraPos.toString() + "," + cameraOri.toString() + ", 0x13, 1.0,1.0, 0.05", "Internal camera for garage");
		cameraSetup(camera);

		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
		{
			multiplayer.map = map;
			multiplayer.cam = camera;
			multiplayer.player = player;
		}
		else
		{
			if(GameLogic.player.club == GameLogic.CLUBS-1 && !roc) //RAXAT: Red Flames garage
			{
				int botIndex = GameLogic.player.club*GameLogic.CLUBMEMBERS-1 + GameLogic.day%(GameLogic.CLUBMEMBERS-1); //we pick someone from the Red Flames club and then spawn his car
				Bot b = new Bot(botIndex);
				redFlamesVhc = b.getCar(map);
			}
		}
		
		osd.endGroup();
		lockCar(); //RAXAT: this will place both player car and RedFlame car on the map in a right position

		mechanic = new Mechanic(player, osd, moneytxt, infoline, 0, multiplayer);
		mechanic.camera = camera;
		mechanic.map = map;

		if(roc) mechanic.flags|=Mechanic.MF_FREE_REPAIRS;
		else mechanic.flags&=~Mechanic.MF_FREE_REPAIRS;

		lighttype = new RenderRef();
		lighttype.duplicate( new RenderRef(misc.garage:0x001Dr) );
		light = new RenderRef( map, lighttype, "neon" );

		osd.show();
		GameLogic.setTimerText( daytxt, timeTxt );

		mechanic.filterInventory(filterEngine, filterBody, filterRGear);
		mechanic.filterEngine=filterEngine;
		mechanic.filterBody=filterBody;
		mechanic.filterRGear=filterRGear;
		if(player.car)
		{
			player.car.command("filter 1 " + filterEngine);
			player.car.command("filter 2 " + filterBody);
			player.car.command("filter 3 " + filterRGear);
			player.car.wakeUp();
			player.car.command("suspend");
		}
		
		//patch
		Frontend.loadingScreen.show();
		if(!roc) Frontend.loadingScreen.display(); //RAXAT: the game still could freeze here!
		Frontend.loadingScreen.hide();
		setEventMask(EVENT_CURSOR|EVENT_COMMAND|EVENT_TIME);
		
		/*
		osd.darkStatus = 1; //force fade-IN
		osd.darken(16,1);
		*/

		addTimer(1, 2);	//RAXAT: timer ticks to avoid using threads

		//special request: reset mouse and set sensitivity to 0
		Input.getAxis (1, -1);
		Input.cursor.enable(1);

		Input.cursor.addHandler(this);	//kivancsiak vagyunk ra, mit csinal az eger
		Input.cursor.enableCameraControl(camera);

		changeMode(mode_memory);

		if(!(prevState instanceof ClubInfo || prevState instanceof CarInfo || prevState instanceof Catalog || prevState instanceof Garage || prevState instanceof RocInfo))
		{
			Sound.changeMusicSet( Sound.MUSIC_SET_GARAGE );
			new SfxRef( GameLogic.SFX_ENTERGARAGE ).play();
		}

		if(timeTxt && timeTitleTxt)
		{
			float xpos;
			float ypos = timeTxt.getPos().y;

			if(roc) xpos = 0.86;
			else xpos = 0.46;

			timeTitleTxt.setPos(xpos, ypos);
			timeTxt.setPos(xpos, ypos);
		}

		//display welcome dialog
		if(roc)
		{
			if(roc.init)
			{
				giveWarning( "ROC", "Welcome to the Race of Champions! \n \n The world's top 16 street racers are ready to fight for the prize and pride this rare event brings for one of them. \n There's 4 rounds, each has 3 runs - only the thoughest drivers and cars can make it through. Go for it!");
				roc.init = 0;
			}
		}
		else
		{
			if(GameLogic.gameMode != GameLogic.GM_MULTIPLAYER)
			{
				if(GameLogic.gameMode != GameLogic.GM_SINGLECAR)
				{
					if(player.checkHint(Player.H_GARAGE))
					{
						giveWarning("WELCOME!", "Welcome to Street Legal Racing! \n \n Your Garage is empty. You can buy cars at the Car Dealers', now visit the Used Car Dealer and choose an affordable car to start with!");
						player.hints |= Player.H_NEWCARS; //RAXAT: we auto-add this hint to preserve it showing up when beginning new career

					}

					//RAXAT: this may work incorrectly after getting 2nd car, check CareerEvent
					if(player.checkHint(Player.H_NEWCARS)) giveWarning("DELIVERY MESSAGE", "Someone has delivered a vehicle for you, check your car lot!");
				}
			}
			else
			{
				if(multiplayer.enterMessage)
				{
					giveWarning("MULTIPLAYER", "Meet other racers online! \n \n Get a car, tune it and find someone in the city to play with.");
					multiplayer.enterMessage = 0;
				}
			}
		}

		GameLogic.played=1;
		GameLogic.careerComplete(); //RAXAT: v2.3.1, game now always check player's achievements
		
		//RAXAT: additional multiplayer init
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
		{
			multiplayer.osd = osd;
			multiplayer.RPC("inGarage", null);
			enableAnimateHook();
		}
		
		setTargetPart(null); //selecting nothing in moving parts interface
		enableControlHook();
		
		System.setLdPriority(System.LD_HIGH); //RAXAT: lagging loading fix
	}
	
	public void control(float t)
	{
		if(carLifted)
		{
			if(player.car) player.car.command("stop"); //RAXAT: to prevent car wakeup from collision with the camera
		}
		
		//time button stuff
		if(timeBtn && timeBtn.pressed)
		{
			if(timeBtnHoldTick < 0.0f)
			{
				//single press of the time button, normal mode
				timeBtnInterval = TIME_BTN_INTERVAL_NORMAL;
				timeBtnHoldTick = System.currentTime();
			}
			else
			{
				//holding the time button, fast forward mode
				if(System.currentTime() - timeBtnHoldTick > TIME_BTN_HOLD_TIME)
				{
					timeBtnInterval = TIME_BTN_INTERVAL_FAST;
					osdCommand(CMD_TIME);
				}
			}
		}
		else timeBtnHoldTick = -1.0f;
	}
	
	//RAXAT: used only in multiplayer
    public void animate()
	{
		runGarageRPCScript();
    }
	
    public void runGarageRPCScript()
	{
       multiplayer.runRPCScript();
    }

	//RAXAT: we run this every second in multiplayer
    public void everySecond()
	{
		refreshMoneyString(); //todo: make async http request with JVM callback, so the game won't freeze every second
		lockCar(); //lockCar is only for placing vehicle after loading from server here, we don't really need it!
    }

	public void exit(GameState nextState)
	{
		disableControlHook();
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) disableAnimateHook();
		
		clearEventMask(EVENT_ANY);
		
		camera.setMatrix(cameraPos, cameraOri); //to restore default camera position after driving in the garage

		if(!(nextState instanceof ClubInfo || nextState instanceof CarInfo || nextState instanceof Catalog || nextState instanceof Garage || nextState instanceof RocInfo))
		{
			new SfxRef(GameLogic.SFX_LEAVEGARAGE).play();
		}

		aknafedel.destroy();
		
		lift.destroy();
		liftPhys.destroy();
		lift = null;

		mode_memory = mode;
		changeMode(MODE_NONE);

		Input.cursor.enable(0);
		Input.cursor.remHandler(this);
		Input.cursor.disableCameraControl();

		cameraPos = camera.getPos();
		cameraOri = camera.getOri();
		camera.destroy();

		osd.hide();
		osd=null;

		mechanic.flushInventory();
		filterEngine=mechanic.filterEngine;
		filterBody=mechanic.filterBody;
		filterRGear=mechanic.filterRGear;
		mechanic = null;

		if(player.car)
		{
			player.car.command("filter 1 0");
			player.car.command("filter 2 0");
			player.car.command("filter 3 0");
			player.car.wakeUp();
			player.car.command("suspend");
		}

		if(player.car) player.car.setDamageMultiplier(Config.player_damage_multiplier);

		releaseCar();

		light.destroy();
		lighttype.destroy();

		//----------------------------------------
		remSceneElements();
		//----------------------------------------

		stuff.destroy();
		map.unload();

		player.controller.reset();
		player.controller.activateState(ControlSet.MENUSET);
		
		System.setLdPriority(System.LD_NORM); //RAXAT: reverting back loading priority, see enter()
	}

	public void cameraSetup(GameRef cam)
	{
		cam.command("render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)); //vp id, cam id, flags
		cam.command("dist 2.5 4.6");
		cam.command("smooth 0.05 0.5");
		cam.command("zoom 60 5");
		cam.command("force 0.3 0.5 -0.7");	//defaults are in config.java
		cam.command("torque 0.08");

		if (player.car)
		{
			cam.command("move " + player.car.id() + " 0,0,0 3.5");
			cam.command("look " + player.car.id() + " 0,0,0 0,0,0");
		} else
		{
			cam.command("move " + map.id() + " " + defLookPos.toString() + " 3.5");
			cam.command("look " + map.id() + " " + defLookPos.toString() + " 0,0,0");
		}

		player.controller.reset();
		player.controller.activateState(ControlSet.CAMTURNSET);

	}

	public void createOSDObjects()
	{
		Style buttonStyle;
		Menu m;

		buttonStyle = new Style(0.1175, 0.1175, Frontend.mediumFont, Text.ALIGN_LEFT, null);
		m = osd.createMenu(buttonStyle, -0.98, -0.85, 0, Osd.MD_HORIZONTAL);
		Gadget g;

		if(GameLogic.gameMode != GameLogic.GM_MULTIPLAYER)
		{
			if(!roc)
			{
				g = m.addItem( new ResourceRef( frontend:0x9C07r ), CMD_HITTHESTREET, "Go driving in the city", null, 1 );
				g = m.addItem( new ResourceRef( frontend:0x9C1Er ), CMD_TESTTRACK, "Test Track", null, 1 );
				g = m.addItem( new ResourceRef( frontend:0x9C08r ), CMD_EVENTLIST, "Browse racing events", null, 1 ); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
				g = m.addItem( new ResourceRef( frontend:0x9C1Ar ), CMD_ROC, "Go to the Race Of Champions", null, 1 ); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
				m.addSeparator();
				m.addSeparator();
				g = m.addItem( new ResourceRef( frontend:0x9C03r ), CMD_CARLOT, "Go to the Car Lot", null, 1 ); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
				g = m.addItem( new ResourceRef( frontend:0x9C15r ), CMD_BUYCARS, "Buy new cars", null, 1 ); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
				g = m.addItem( new ResourceRef( frontend:0x9C22r ), CMD_BUYCARSUSED, "Buy used cars or sell your car", null, 1 ); if( GameLogic.gameMode == GameLogic.GM_SINGLECAR ) g.disable();
				g = m.addItem( new ResourceRef( frontend:0x9C20r ), CMD_TRADEIN, "Trade-in cars", null, 1); if( GameLogic.gameMode == GameLogic.GM_SINGLECAR ) g.disable();
				m.addSeparator();
				m.addSeparator();
				g = m.addItem( new ResourceRef( frontend:0x9C05r ), CMD_CATALOG, "Browse the Catalog", null, 1); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
				g = m.addItem( new ResourceRef( frontend:0x9C01r ), CMD_CLUBINFO, "Check your ranking here", null, 1 ); if( GameLogic.gameMode == GameLogic.GM_SINGLECAR ) g.disable();
				g = m.addItem( new ResourceRef( frontend:0x9C11r ), CMD_CARINFO, "Details of this car", null, 1 );
				m.addSeparator();
				m.addSeparator();
				g = m.addItem( new ResourceRef( frontend:0x92E2r ), CMD_MECHANIC, "Install/Remove parts", null, 1 );
				g = m.addItem( new ResourceRef( frontend:0x9C1Cr ), CMD_TUNE, "Fine tune specific parts", null, 1 );
				g = m.addItem( new ResourceRef( frontend:0x9C0Cr ), CMD_LIFT, "Lift the car", null, 1 ); //RAXAT: build 931, vehicle lift
				g = m.addItem( new ResourceRef( frontend:0x9C16r ), CMD_PAINT,"Go to paint booth", null, 1 );
				m.addSeparator();
				m.addSeparator();
				g = m.addItem( new ResourceRef( frontend:0x9C12r ), CMD_TEST, "Test engine and steering", null, 1 );
				timeBtn = m.addItem( new ResourceRef( frontend:0x9C1Fr ), CMD_TIME, "Advance time", null, 1 );
				//g = m.addItem( new ResourceRef( frontend:0x92E7r ), CMD_DEBUG_3, "Dialog box debug", null, 1 );
				//g = m.addItem( new ResourceRef( frontend:0x92E7r ), CMD_DEBUG_2, "Set money", null, 1 );
				m.addSeparator();
				g = m.addItem( new ResourceRef( frontend:0x9C02r ), CMD_MAINMENU, "Go back to Main Menu", null, 1 );
			}
			else
			{
				m.addItem( new ResourceRef( frontend:0x9C08r ), CMD_ROCRACE, "Go on the next race", null, 1 );
				m.addItem( new ResourceRef( frontend:0x9C1Fr ), CMD_ROCTEST, "Try the race track", null, 1 );
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addItem( new ResourceRef( frontend:0x9C1Ar ), CMD_ROCINFO, "Check the race status", null, 1 );
				m.addItem( new ResourceRef( frontend:0x9C11r ), CMD_CARINFO, "Details of your car", null, 1 );
				m.addSeparator();
				m.addItem( new ResourceRef( frontend:0x92E2r ), CMD_MECHANIC, "Install/Remove parts", null, 1 );
				m.addItem( new ResourceRef( frontend:0x9C1Cr ), CMD_TUNE, "Fine tune specific parts", null, 1 );
				m.addItem( new ResourceRef( frontend:0x9C0Cr ), CMD_LIFT, "Lift the car", null, 1 ); //RAXAT: build 931, vehicle lift
				m.addSeparator();
				m.addItem( new ResourceRef( frontend:0x9C12r ), CMD_TEST, "Test engine and steering", null, 1 );
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addSeparator();
				m.addItem( new ResourceRef( frontend:0x9C02r ), CMD_ROCQUIT, "Quit the Race Of Champions", null, 1 );
			}
		}
		else
		{
			g = m.addItem( new ResourceRef( frontend:0x9C07r ), CMD_HITTHESTREET, "Go driving in the city", null, 1 );

			m.addSeparator();
			m.addSeparator();
			//g = m.addItem( new ResourceRef( frontend:0x9C08r ), CMD_EVENTLIST, "Browse racing events", null, 1 ); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
			m.addSeparator();
			m.addSeparator();
			m.addSeparator();
			m.addSeparator();
			g = m.addItem( new ResourceRef( frontend:0x9C03r ), CMD_CARLOT, "Go to the Car Lot", null, 1 ); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
			g = m.addItem( new ResourceRef( frontend:0x9C15r ), CMD_BUYCARS, "Buy new cars", null, 1 ); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
			g = m.addItem( new ResourceRef( frontend:0x9C22r ), CMD_BUYCARSUSED, "Buy used cars or sell your car", null, 1 ); if( GameLogic.gameMode == GameLogic.GM_SINGLECAR ) g.disable();
			g = m.addItem( new ResourceRef( frontend:0x9C20r ), CMD_TRADEIN, "Trade-in cars", null, 1); if( GameLogic.gameMode == GameLogic.GM_SINGLECAR ) g.disable();
			m.addSeparator();
			m.addSeparator();
			m.addSeparator();
			m.addSeparator();
			g = m.addItem( new ResourceRef( frontend:0x9C05r ), CMD_CATALOG, "Browse the Catalog", null, 1); if(GameLogic.gameMode == GameLogic.GM_SINGLECAR) g.disable();
			g = m.addItem( new ResourceRef( frontend:0x9C11r ), CMD_CARINFO, "Details of this car", null, 1 );
			m.addSeparator();
			m.addSeparator();
			m.addSeparator();
			g = m.addItem( new ResourceRef( frontend:0x92E2r ), CMD_MECHANIC, "Install/Remove parts", null, 1 );
			g = m.addItem( new ResourceRef( frontend:0x9C1Cr ), CMD_TUNE, "Fine tune specific parts", null, 1 );
			g = m.addItem( new ResourceRef( frontend:0x9C16r ), CMD_PAINT,"Go to paint booth", null, 1 );
			g = m.addItem( new ResourceRef( frontend:0x9C0Cr ), CMD_LIFT, "Lift the car", null, 1 ); //RAXAT: build 931, vehicle lift
			m.addSeparator();
			m.addSeparator();
			m.addSeparator();
			g = m.addItem( new ResourceRef( frontend:0x9C12r ), CMD_TEST, "Test engine and steering", null, 1 );
			timeBtn = m.addItem( new ResourceRef( frontend:0x9C1Fr ), CMD_TIME, "Advance time", null, 1 );
			m.addSeparator();
			g = m.addItem( new ResourceRef( frontend:0x9C02r ), CMD_MAINMENU, "Exit from multiplayer", null, 1 );
		}
		
		menuGroup = osd.endGroup();
		menuVisible=1;

		m = osd.createMenu(buttonStyle, 0.915, -0.85, 0, Osd.MD_HORIZONTAL);
		m.addItem(new ResourceRef(frontend:0x9C02r), CMD_TEST, "Exit test car mode", null, 1 );
		
		if(!roc)
		{
			m = osd.createMenu(buttonStyle, -0.98, -0.85, 0, Osd.MD_HORIZONTAL);
			m.addItem(new ResourceRef(frontend:0x9C1Er), CMD_TESTDRIVE, "Test drive this car", null, 1 );
		}
		osd.hideGroup(menuGroup2 = osd.endGroup());

		//---------------------------------------------------------------------------------
		osd.createRectangle(0.0, -0.885, 2.0, 0.24, -1, new ResourceRef(frontend:0x000092EAr)); //RAXAT: upper frame, in v2.2.1 - tile image for wide headers
		infoline = osd.createText("Welcome!", Frontend.mediumFont, Text.ALIGN_RIGHT, 0.98, 0.4875);

		//global status
		String titleString = "";
		if(roc) titleString = player.name + " - " + Roc.roundNames[roc.numRounds()-roc.getCurrentRound()-1] + " of R.O.C.";
		else
		{
			switch(GameLogic.gameMode)
			{
				case(GameLogic.GM_SINGLECAR):
					titleString = "Single car mode";
					break;
					
				case(GameLogic.GM_MULTIPLAYER):
					titleString = "Multiplayer mode";
					break;
					
				default:
					//RAXAT: global club rating is displayed now (local club rating can be checked in ClubInfo anyway)
					int maxlen = 12;
					String name = player.name;
					if(name && name.length() > maxlen) name = name.chop(name.length()-maxlen) + "..."; //RAXAT: long player names are chopped
					titleString = name + " (" + (int)(GameLogic.findRacer(GameLogic.player)+1) + "/" + GameLogic.CLUBMEMBERS*GameLogic.CLUBS + ")";
					break;
			}
		}
		osd.createText(titleString, Frontend.smallFont, Text.ALIGN_LEFT,	-0.975, -0.98);
		
		//RAXAT: build 933, corrupted ROC garage interface patch
		float xpos = 0.0;

		//prestige status
		if(roc) xpos = 0.0;
		else xpos = -0.25;
		prestigeTxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT,		xpos, -0.98);
		
		//RAXAT: build 932, car class info
		xpos -= 0.40;
		carClassTxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT,		xpos, -0.98);
		
		if(GameLogic.gameMode != GameLogic.GM_SINGLECAR)
		{
			if(roc) xpos = 0.485;
			else xpos = 0.155;
			
			osd.createText("DAY ",	Frontend.smallFont, Text.ALIGN_RIGHT,		xpos, -0.98);
			daytxt=osd.createText( null, Frontend.smallFont, Text.ALIGN_LEFT,	xpos, -0.98);
		}

		timeTitleTxt = osd.createText("TIME: ",	Frontend.smallFont, Text.ALIGN_RIGHT,	0.86, -0.98);
		timeTxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT,		0.86, -0.98);

		if(roc) moneytxt = null;
		else
		{
			osd.createText("MONEY: ", Frontend.smallFont, Text.ALIGN_RIGHT,		0.86, -0.98);
			moneytxt=osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT,	0.86, -0.98);
		}

		refreshMoneyString(1);
		refreshPrestigeString();
		refreshCarClassString();
		
		osd.createHotkey(Input.AXIS_MENU, Input.VIRTUAL|Osd.HK_STATIC, CMD_ESCAPE, this);
		osd.endGroup();
		
		Style sty = new Style(0.33, 0.12, Frontend.smallFont, Text.ALIGN_LEFT, new ResourceRef(RID_BTN_L));
		m = osd.createMenu(sty, -1.01, 0.525, -0.125, Osd.MD_VERTICAL);
		m.addItem("MOVE PARTS", CMD_MOVE_PARTS, "Move and rotate the parts of your car");
		osd.hideGroup(tuneGroup = osd.endGroup());
		
		Style	sld_k	= new Style(0.038, 0.05, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB));
		Style	sld_lh	= new Style(0.55, 0.03,   Frontend.mediumFont, Text.ALIGN_LEFT,  new ResourceRef(Osd.RID_SLD_BACK));

		m = osd.createMenu(buttonStyle, -0.98, -0.85, 0, Osd.MD_HORIZONTAL);
		for(int i=0; i<38; i++) m.addSeparator();
		g = m.addItem( new ResourceRef( frontend:0x92E2r ), CMD_MECHANIC, "Install/Remove parts", null, 1 );
		g = m.addItem( new ResourceRef( frontend:0x9C1Cr ), CMD_TUNE, "Fine tune specific parts", null, 1 );
		m.addSeparator();
		g = m.addItem( new ResourceRef( frontend:0x9C02r ), CMD_LIFT_DONE, "Leave from the lift", null, 1 );
		
		m = osd.createMenu(buttonStyle, -0.75, -0.8575, 0, Osd.MD_HORIZONTAL);
		m.setSliderStyle(sld_lh, sld_k);
		osd.createText("Lift arm amplitude", Frontend.largeFont, Text.ALIGN_LEFT,	-0.975, -0.89);
		liftArmAmplitudeSlider = m.addItem(null, CMD_LIFT_ARM_MOVE, liftArmAmplitude, 0.0, LIFT_ARM_AMPLITUDE_MAX, LIFT_ARM_AMPLITUDE_MAX/LIFT_ARM_AMPLITUDE_STEP, null);
		osd.hideGroup(liftGroup = osd.endGroup());
		
		osd.createRectangle(0.0, -1.4, 2.5, 0.5, -1, new ResourceRef(frontend:0xC0A0r), 0);
		
		partPosSlider = new Slider[3];
		partOriSlider = new Slider[3];
		
		sty = new Style(0.13, 0.13, Frontend.mediumFont, Text.ALIGN_LEFT, null);
		m = osd.createMenu(sty, -0.78, 0.7, 0.105, Osd.MD_VERTICAL);
		m.setSliderStyle(sld_lh, sld_k);
		
		float max_value = MOVE_PARTS_LIMIT;
		int steps = (max_value*1000)*4;
		partPosSlider[0] = m.addItem("X", CMD_MOVE_PARTS_SETPOS+0, 0.0, -max_value, max_value, steps, null);
		partPosSlider[1] = m.addItem("Y", CMD_MOVE_PARTS_SETPOS+1, 0.0, -max_value, max_value, steps, null);
		partPosSlider[2] = m.addItem("Z", CMD_MOVE_PARTS_SETPOS+2, 0.0, -max_value, max_value, steps, null);
		
		for(int i=0; i<partPosSlider.length; i++) partPosSlider[i].printValue(partPosSlider[i].value);
		
		m = osd.createMenu(sty, 0.25, 0.7, 0.105, Osd.MD_VERTICAL);
		m.setSliderStyle(sld_lh, sld_k);
		
		max_value = Math.PI;
		steps = (max_value*1000)/4;
		partOriSlider[0] = m.addItem("Yaw",   CMD_MOVE_PARTS_SETORI+0, 0.0, -max_value, max_value, steps, null);
		partOriSlider[1] = m.addItem("Pitch", CMD_MOVE_PARTS_SETORI+1, 0.0, -max_value, max_value, steps, null);
		partOriSlider[2] = m.addItem("Roll",  CMD_MOVE_PARTS_SETORI+2, 0.0, -max_value, max_value, steps, null);
		
		for(int i=0; i<partOriSlider.length; i++) partOriSlider[i].printValue(partOriSlider[i].value);
		
		Style buttonStyleRed = new Style(0.4, 0.15, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef(Dialog.RID_BUTTON_RED_GL));
		osd.createButton(buttonStyleRed, 0.0, 0.815, "RESET", CMD_MOVE_PARTS_RESET).label.txt.setColor(Palette.RGB_WHITE);
		
		m = osd.createMenu(buttonStyle, 0.915, -0.85, 0, Osd.MD_HORIZONTAL);
		m.addItem(new ResourceRef(frontend:0x9C02r), CMD_MOVE_PARTS_DONE, "Stop moving parts", null, 1 );
		targetPartTxt = osd.createText(null, Frontend.largeFont, Text.ALIGN_LEFT,	-0.975, -0.89);
			
		osd.hideGroup(movePartsGroup = osd.endGroup());
	}

	public void lockCar()
	{
		if(player.car)
		{
			player.car.setParent(map);

			Vector3 ppp = new Vector3(defCarPos);
			player.car.setPos(ppp);

			player.car.command("reset");
			player.car.command("setsteer -0.7");
			player.car.command("stop");
		}
		
		if(redFlamesVhc)
		{
			redFlamesVhc.setMatrix(redFlamesVhcPos, redFlamesVhcOri);
			redFlamesVhc.command("setsteer -0.7");
		}
	}

	public void releaseCar()
	{
		if(player.car)
		{
			player.car.command("reset");
			player.car.command("start");	//release

			player.car.setParent(player);
		}
		
		if(redFlamesVhc)
		{
			redFlamesVhc.destroy();
			redFlamesVhc = null;
		}
	}

	public void refreshMoneyString()
	{
		refreshMoneyString(0);
	}

	public void refreshMoneyString(int init)
	{
		if(moneytxt)
		{
			int money = 0;

			if(moneytxt.text) money = moneytxt.text.scanf("$").lastElement().intValue();
			if(money != player.getMoney())
			{
				if(!init) new SfxRef(Frontend.SFX_MONEY).play();
				money = player.getMoney();
			}
			
			moneytxt.changeText("$" + Integer.toString(money));
			if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) player.setMoney(multiplayer.RPC("getMoney", null, true).intValue());
		}
	}

	public void refreshPrestigeString()
	{
		if(GameLogic.gameMode != GameLogic.GM_SINGLECAR) prestigeTxt.changeText("Prestige: " + player.getPrestigeString());
	}
	
	public void refreshCarClassString()
	{
		String str = "";
		if(player.car)
		{
			str = "Vehicle " + CareerEvent.getClass(CareerEvent.checkClass(player.car));
			
			if(player.car.chassis && !player.car.canTakeSeat())
			{
				int power = 0;
				Block engine = player.car.chassis.getBlock();

				if(engine)
				{
					power = engine.dynodata.maxHP;
					if(power && power > 0) str += " (" + power + "HP)";
					else str = ""; //display nothing if the engine is not in a working condition
				}
				else str = ""; //display nothing if there's no engine as well
			}
			else str = "";
		}
		
		carClassTxt.changeText(str);
	}

	public void changeMode(int newMode)
	{
		if(mode != newMode)
		{
			//if GPS frame has been previously killed, we're restoring it here
			if(mode != MODE_TEST)
			{
				if(player.gpsState != 1)
				{
					player.gpsState = 1;
					player.handleGPSframe(1);
					//lockCar(); //RAXAT: car could return to def pos, we don't want that since the garage is driveable now
				}
			}

			//-------------mode OFFs
			if(mode == MODE_MECH || mode == MODE_TUNE)
			{
				if(newMode != MODE_MECH && newMode != MODE_TUNE) mechanic.hide();
				if(newMode != MODE_TUNE) osd.hideGroup(tuneGroup);
			}
			else
			if(mode == MODE_TEST)
			{
				if(player.render) player.render.destroy();

				player.controller.command("leave " + player.car.id());
				player.controller.command("camera "+ camera.id());

				player.hideOsd();

				osdCommand(CMD_MENU);

				player.car.command("reset");
				player.car.command("stop");

				player.controller.reset();
				player.controller.activateState(ControlSet.CAMTURNSET);
			}
			else
			if(mode == MODE_MOVE)
			{
				osd.hideGroup(movePartsGroup);
				if(!carLifted) osd.showGroup(menuGroup);
				if(carLifted) osd.showGroup(liftGroup);
			}

			int oldMode = mode;
			mode = newMode;

			changePointer();

			//-----------mode ONs
			if(mode == MODE_MECH || mode == MODE_TUNE)
			{
				if(oldMode != MODE_MECH && oldMode != MODE_TUNE) mechanic.show();
				if(oldMode == MODE_MOVE) setTargetPart(null);

				if(mode == MODE_MECH) mechanic.mode = 0;
				else
				{
					mechanic.mode = 1;
					if(player.car) osd.showGroup(tuneGroup); //we show "move parts" only if player has got a car
					else osd.hideGroup(tuneGroup);
				}
			}
			else
			if(mode == MODE_TEST)
			{
				osdCommand(CMD_MENU);

				if(!mechanic.filterBody)
				{
					player.render = new RenderRef(map, player.driverID, "player");
					player.controller.command("renderinstance " + player.render.id());
				}

				player.controller.command("camera 0");
				player.controller.command("controllable " + player.car.id());
				player.controller.reset();
				player.controller.activateState(ControlSet.DRIVERSET);
				
				//destroying GPS frame, dangerous!
				player.gpsState = 0;
				player.handleGPSframe(0);

				player.showOsd();
				if(DRIVEABLE_GARAGE) player.car.command("start");
			}
			else
			if(mode == MODE_MOVE)
			{
				osd.hideGroup(tuneGroup);
				if(carLifted) osd.hideGroup(liftGroup);
				osd.showGroup(movePartsGroup);
				if(!carLifted) osd.hideGroup(menuGroup);
				mechanic.hide();
			}
		}
	}

	public void changePointer()
	{
		if(move) Input.cursor.setPointer(Frontend.pointers, "M");
		else
		{
			switch(mode)
			{
				case (MODE_MECH):
					if(drag)
					{
						//RAXAT: we don't change cursor if lift arm amplitude slider is pressed in a car lifting mode
						if(!liftArmAmplitudeSlider.pressed) Input.cursor.setPointer(Frontend.pointers, "A");
					}
					else Input.cursor.setPointer(Frontend.pointers, "G");
					break;

				case (MODE_TUNE):
					if(drag)
					{
						if(!liftArmAmplitudeSlider.pressed) Input.cursor.setPointer(Frontend.pointers, "A"); //RAXAT: build 928, drag cursor fix in tuning mode
					}
					else Input.cursor.setPointer(Frontend.pointers, "B");
					break;

				default:
					Input.cursor.setPointer(Frontend.pointers, "J");
			}
		}
	}

	public void handleEvent(GameRef obj_ref, int event, int param)
	{
		if(event == EVENT_TIME)
		{
			if(param == 2)
			{
				addTimer(1, 2); //ten sec tick
				super.refresh(GameLogic.getTime());
				refreshPrestigeString();
				refreshCarClassString();

				//RAXAT: transmission type refresher for older builds
				//we need to update this even faster! maybe via transmission itself?
				if(!System.nextGen())
				{
					if(GameLogic.player.car)
					{
						Block engine = player.car.chassis.partOnSlot(401);
						if(engine)
						{
							Transmission t = engine.getTransmission();
							if(t) t.updateType();
						}
					}
				}
				
				if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
				{
					if(mode != MODE_TEST) everySecond();
				}
			}
		}
	}

	public void handleEvent(GameRef obj_ref, int event, String param)
	{
		int	tok = -1;

		if(event == EVENT_CURSOR)
		{
			int ec = param.token(++tok).intValue();
			int cursor_id = param.token(++tok).intValue();

			if(ec == GameType.EC_LDRAGBEGIN)
			{
				if (mode==MODE_MECH || mode==MODE_TUNE) //RAXAT: build 928, drag cursor fix in tuning mode
				{
					if(!obj_ref.getInfo(GameType.GII_GETOUT_OK)) //RAXAT: obj_ref is a dragged item
					{
						if(!Integrator.frozen) drag=1; //RAXAT: when tuning menu is displayed, Mechanic is switching Integrator.frozen to true, so here in garage we know that we don't need to change cursor
						changePointer();
					}
				}
			} 
			else
			if (ec == GameType.EC_LDRAGEND)
			{
				if (mode==MODE_MECH || mode==MODE_TUNE) //RAXAT: build 928, drag cursor fix in tuning mode
				{
					drag=0;
					changePointer();
				}
			} 
			else
			if(ec == GameType.EC_LDROP)
			{
				if(mode==MODE_MECH || mode==MODE_TUNE) //RAXAT: build 928, drag cursor fix in tuning mode
				{
					GameRef draggedOnto = new GameRef(param.token( ++tok ).intValue());
					GameRef draggedItem = new GameRef(param.token( ++tok ).intValue());
					Vector3 droppos = new Vector3(param.token(++tok).floatValue(), param.token(++tok).floatValue(),	param.token(++tok).floatValue());

					int phy_id = param.token(++tok).intValue();
					Object o_onto = draggedOnto.getScriptInstance();
					
					//todo: prevent dragging from another car into inventory
					if(draggedOnto.id() == map.id() || o_onto instanceof Part)
					{
						//attemping to to put-in
						int cat = draggedItem.getInfo(GameType.GII_CATEGORY);

						if(cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE)
						{
							GameRef xpart = new GameRef();
							Part part = draggedItem.getScriptInstance();
							
							if(GameLogic.player.car && GameLogic.player.car.chassis && part.getCarRef().id() == GameLogic.player.car.chassis.getCarRef().id() ) //RAXAT: we check if it's a player's car, so other cars in the garage won't be affected
							{
								int[] slotId = part.install_OK(player.car, 0, xpart, 0, droppos);
								if(slotId)
								{
									String error;
									if(!(error = part.installCheck(xpart.getScriptInstance(), slotId)))
									{
										GameLogic.spendTime(GameLogic.mechTime(part, 0));
										draggedItem.command("remove 0 " + map.id());

										GameLogic.spendTime(GameLogic.mechTime(part, 1));
										draggedItem.command("install 0 " + player.car.id() + " 0 0 0 "+droppos.x + " " + droppos.y + " "+droppos.z);

										new SfxRef(GameLogic.SFX_WRENCH).play();
										if(!carLifted) player.car.wakeUp();
									}
									else giveWarning(error);
								}
							}
						}
						else
						{
							int item = mechanic.inventory.getItemIDbyButtonPhyId(phy_id);
							String error;
							if((error = mechanic.inventory.installToCar(item, player.car, droppos)) && error!="") giveWarning(error);
							else mechanic.updateLineIndex();
						}
					}
				}
			}
			else
			if(ec == GameType.EC_RCLICK)
			{
				GameRef dest = new GameRef(param.token(++tok).intValue());
				int cat = dest.getInfo(GameType.GII_CATEGORY);

				Object part = dest.getScriptInstance();
				if(part instanceof Part)
				{
					if(GameLogic.player.car && GameLogic.player.car.chassis && part.getCarRef().id() == GameLogic.player.car.chassis.getCarRef().id() ) //RAXAT: we check if it's a player's car, so other cars in the garage won't be affected
					{
						if(cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE)
						{
							mechanic.lastLookDestination = dest;
							camera.command("look " + dest.id() + " " + param.token(++tok) + "," + param.token(++tok) + "," + param.token(++tok));
							
							if(mode == MODE_MOVE) setTargetPart(part);
						}
						else
						{
							if(mode == MODE_MOVE) setTargetPart(null);
						}
					}
					else
					{
						if(mode == MODE_MOVE) new SfxRef(Frontend.SFX_WARNING).play();
					}
				}
				else
				{
					if(mode == MODE_MOVE) new SfxRef(Frontend.SFX_WARNING).play();
				}
			}
			else
			if(ec == GameType.EC_RDRAGBEGIN)
			{
				move=1;
				changePointer();

				//enable camera control with mouse
				player.controller.user_Add( Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);
				player.controller.user_Add( Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);

				//disable cursor movement
				player.controller.user_Del( Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0 );
				player.controller.user_Del( Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1 );

				Input.cursor.cursor.command( "lock" );
			} 
			else
			if(ec == GameType.EC_RDRAGEND)
			{
				move=0;
				changePointer();

				//disable camera control with mouse
				player.controller.user_Del( Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1 );
				player.controller.user_Del( Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0 );

				//enable cursor movement
				player.controller.user_Add( Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);
				player.controller.user_Add( Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);

				Input.cursor.cursor.command( "unlock" );
			}
		}
	}
	
	public void setTargetPart(GameRef part)
	{
		String str = "No item selected";
		targetPart = null;
		
		if(part)
		{
			targetPart = (Part)part;
			Pori pori = targetPart.drag_matrix;
			partPos = pori.pos;
			partOri = pori.ori;
			
			str = "Selected: " + targetPart.name;
		}
		else
		{
			partPos = new Vector3(0);
			partOri = new Ypr(0);
		}
		
		if(partPos && partOri)
		{
			partPosSlider[0].setValue(partPos.x);
			partPosSlider[1].setValue(partPos.y);
			partPosSlider[2].setValue(partPos.z);
			
			partPosSlider[0].printValue(partPos.x);
			partPosSlider[1].printValue(partPos.y);
			partPosSlider[2].printValue(partPos.z);
			
			partOriSlider[0].setValue(partOri.y);
			partOriSlider[1].setValue(partOri.p);
			partOriSlider[2].setValue(partOri.r);
			
			partOriSlider[0].printValue(partOri.y);
			partOriSlider[1].printValue(partOri.p);
			partOriSlider[2].printValue(partOri.r);
		}

		if(mode == MODE_MOVE) new SfxRef(Frontend.SFX_MENU_SELECT).play();
		targetPartTxt.changeText(str);
	}

	public void osdCommand(int cmd)
	{
		if(cmd == CMD_DEBUG_1)
		{
			GameRef xa = new GameRef();
			Part part = xa.create( map, new GameRef(cars.racers.Naxas:0x000000E9r), "100,0,0,0,0,0", "part" );
			mechanic.inventory.addItem(part); //right to visual inventory
		}
		else
		if(cmd == CMD_DEBUG_2)
		{
/*
			giveWarning("DEBUG BOX", "This is the first line of the debug dialog box \n Here goes the second line. Attempting to test some very big and long text. Drop parts here from the Parts Bin to sell them. \n \n To sell you entire car go to the Used Car Dealer. \n If you want to sell all parts from parts bin press this button when you will have at least 1 part there. Always remember, filtered parts will not be sold, you must unfilter them and press sell button again. \n\n Here goes the THIRD line. Attempting to test some very big and long text. Drop parts here from the Parts Bin to sell them. \n \n To sell you entire car go to the Used Car Dealer. \n If you want to sell all parts from parts bin press this button when you will have at least 1 part there. Always remember, filtered parts will not be sold, you must unfilter them and press sell button again. n\n Here goes the FOURTH line. Attempting to test some very big and long text. Drop parts here from the Parts Bin to sell them. \n \n To sell you entire car go to the Used Car Dealer. \n If you want to sell all parts from parts bin press this button when you will have at least 1 part there. Always remember, filtered parts will not be sold, you must unfilter them and press sell button again.");
*/
			StringRequesterDialog d = new StringRequesterDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY|Dialog.SIF_MINI, "STR REQ DEBUG MINI", "enter text");
			int r = d.display();
			player.setMoney(d.input.intValue());
			System.print(d.input.intValue());
			refreshMoneyString();
			new SfxRef( GameLogic.SFX_ENTERGARAGE ).play(); 
			System.print("d.display(): " + r, System.PF_WARNING);
			/*
			System.print("d.input: " + d.input);
			StringRequesterDialog d2 = new StringRequesterDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY|Dialog.SIF_SLIM, "STR REQ DEBUG SLIM", "enter text");
			int r2 = d2.display();
			System.print("d2.input: " + d2.input);
			System.print("d2.display(): " + r2, System.PF_WARNING);
			*/
//			FileRequesterDialog d3 = new FileRequesterDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY, "FILE REQ DEBUG", "LOAD", GameLogic.skinSaveDir, "*").display();
/*
			YesNoDialog d4 = new YesNoDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_WIDE, "WIDE DIALOG DBG", "This is the first line of the debug dialog box \n Here goes the second line. Attempting to test some very big and long text. Drop parts here from the Parts Bin to sell them. \n \n To sell you entire car go to the Used Car Dealer. \n If you want to sell all parts from parts bin press this button when you will have at least 1 part there. Always remember, filtered parts will not be sold, you must unfilter them and press sell button again. \n\n Here goes the THIRD line. Attempting to test some very big and long text. Drop parts here from the Parts Bin to sell them. \n \n To sell you entire car go to the Used Car Dealer. \n If you want to sell all parts from parts bin press this button when you will have at least 1 part there. Always remember, filtered parts will not be sold, you must unfilter them and press sell button again. n\n Here goes the FOURTH line. Attempting to test some very big and long text. Drop parts here from the Parts Bin to sell them. \n \n To sell you entire car go to the Used Car Dealer. \n If you want to sell all parts from parts bin press this button when you will have at least 1 part there. Always remember, filtered parts will not be sold, you must unfilter them and press sell button again.").display();
*/
		}
		else
		if(cmd == CMD_DEBUG_3)
		{
//			OptionsDialog od = new OptionsDialog(Dialog.DF_DARKEN);
//			od.display();
		}
		else
		if(cmd == CMD_TEST)
		{
			if(mode == MODE_TEST) changeMode(prevMode);
			else
			{
				if(player.car)
				{
					String e = player.car.canTakeSeat();
					if(!e)
					{
						prevMode = mode;
						player.car.command("reload");
						changeMode(MODE_TEST);
					}
					else giveWarning("You can't test the car because " + e);
				}
				else giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
			}
		}
		else
		if(cmd == CMD_ESCAPE)
		{
			if(mode == MODE_TEST) osdCommand(CMD_TEST);
		}
		else
		if(cmd == CMD_MENU)
		{
			if(menuVisible)
			{
				osd.hideGroup(menuGroup);
				osd.showGroup(menuGroup2);
			}
			else
			{
				osd.hideGroup(menuGroup2);
				osd.showGroup(menuGroup);
			}

			menuVisible = 1-menuVisible;
		}
		else
		if(cmd == CMD_MAINMENU)
		{
			GameLogic.changeActiveSection(new MainMenu());
		}
		else
		if(cmd == CMD_CHEATMONEY)
		{
			player.addMoney(100000);
			refreshMoneyString();
		}
		else
		if(cmd == CMD_EVENTLIST)
		{
			if(player.car)
			{
				String problem = player.car.isDriveable();
				if(!problem) GameLogic.changeActiveSection(new EventList(EventList.MODE_AMATEUR));
				else giveWarning(problem);
			}
			else giveWarning("You need a car to do this! \n Buy a car or get one from the car lot."); //RAXAT: or show up this message in eventlist?
		}
		else
		if(cmd == CMD_TESTDRIVE)
		{
			GameLogic.changeActiveSection(new EventList(EventList.MODE_FREERIDE));
		}
		else
		if(cmd == CMD_ROC)
		{
			int entryFee, minRanking;
			float maxPartsWeight;
			float minCarPrestige;

			entryFee = GameLogic.ROC_ENTRYFEE;
			minRanking = 5;
			minCarPrestige = 7.5;
			maxPartsWeight = 1000;

			float curCarPrestige;
			if(player.car) curCarPrestige = player.car.getPrestige();

			//RAXAT: v2.3.1, spare parts calculation patch
			float partsWeight;
			for(int i=0; i<mechanic.inventory.items.size(); i++) //taking info from mechanic, not from player's inventory
			{
				int t = mechanic.inventory.items.elementAt(i).type;
				switch(t)
				{
					case(InventoryItem.IIT_PART): //normal part
						partsWeight += mechanic.inventory.items.elementAt(i).getPart().getMass(); //taking info from mechanic, not from player's inventory
						break;

					case(InventoryItem.IIT_SET): //set (engine kit, suspension kit, body kit, etc.)
						partsWeight += mechanic.inventory.items.elementAt(i).getItem().getMass();
						break;
				}
			}

			int requirements;

			if(player.getMoney() >= entryFee) requirements|=0x01;
			if(GameLogic.findRacer(player) >= GameLogic.speedymen.length-minRanking) requirements|=0x02;
			if(curCarPrestige >= minCarPrestige) requirements|=0x04;
			if(partsWeight <= maxPartsWeight) requirements|=0x08;

			int rocIntervalDays = 30*6;
			if(new ROCEntryDialog(player.controller, requirements, rocIntervalDays-GameLogic.day%(rocIntervalDays+1)).display())
			{
				GameLogic.setTime(8*3600);
				osdCommand(CMD_BEGIN_ROC);
			}
		}
		else
		if(cmd == CMD_BEGIN_ROC)
		{
			roc = new Roc(player);
			GameLogic.changeActiveSection(this);
		}
		else
		if(cmd == CMD_ROCINFO)
		{
			GameLogic.changeActiveSection(new RocInfo());
		}
		else
		if(cmd == CMD_ROCRACE)
		{
			String problem = player.car.isDriveable();
			if(!problem) GameLogic.changeActiveSection(new ROCTrack());
			else giveWarning( problem );
		}
		else
		if(cmd == CMD_ROCTEST)
		{
			String problem = player.car.isDriveable();
			if(!problem)
			{
				ROCTrack rt = new ROCTrack();
				rt.testMode = 1;

				GameLogic.changeActiveSection(rt);
			}
			else giveWarning(problem);
		}
		else
		if(cmd == CMD_ROCQUIT)
		{
			if(0 == new NoYesDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "ARE YOU SURE?", "Think twice: You can only resubmit to the next (6 months away) Race Of Champions, if you quit now! Quitting also seriously degrades your prestige." ).display())
			{
				GameLogic.updateCodeROC(); //RAXAT: v2.3.1, generate link to the next prize vehicle
				GameLogic.spendTime(8*3600+24*3600-GameLogic.getTime());
				player.takeMoney(GameLogic.ROC_ENTRYFEE);
				roc = null;
				player.prestige*=0.2;
				GameLogic.changeActiveSection(this);
			}
		}
		else
		if(cmd == CMD_HITTHESTREET)
		{
			if(player.car)
			{
				String problem = player.car.isDriveable();
				if(!problem) GameLogic.changeActiveSection(new Valocity(multiplayer));
				else giveWarning(problem);
			}
			else giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
		}
		else
		if(cmd == CMD_TESTTRACK)
		{
			if(player.car)
			{
				String problem = player.car.isDriveable();
				if(!problem)
				{
					GameLogic.spendTime(1800);
					GameLogic.changeActiveSection(new TestTrack());
				}
				else giveWarning(problem);
			}
			else giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
		}
		else
		if(cmd == CMD_CARLOT)
		{
			GameLogic.changeActiveSection(player.carlot);
		}
		else
		if(cmd == CMD_BUYCARS || cmd == CMD_BUYCARSUSED || cmd == CMD_TRADEIN)
		{
			if(cmd == CMD_TRADEIN && !player.car) giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
			else
			{
				float hour = GameLogic.getTime()/3600;

				int pass = 0;
				if(cmd == CMD_TRADEIN)
				{
					if(hour > 7 && hour < 19) pass++; //RAXAT: v2.3.1, trade-in is opened from 7am to 7pm, unlike other dealerships
					else pass = -7;
				}
				else
				{
					if(hour > 7 && hour < 17) pass++;
					else pass = -5;
				}

				if(pass > 0)
				{
					GameLogic.spendTime(1800);

					int used;
					float visitTimeStamp = GameLogic.day*24 + GameLogic.getTime()/3600;
					float hoursPassed;
					VehicleDescriptor[] vds;

					if(cmd == CMD_BUYCARSUSED)
					{
						used=1;
						vds = GameLogic.carDescriptors_Used;

						if(!GameLogic.dealerVisitTimeStamp_Used) GameLogic.dealerVisitTimeStamp_Used = visitTimeStamp;
						hoursPassed = visitTimeStamp-GameLogic.dealerVisitTimeStamp_Used;
						GameLogic.dealerVisitTimeStamp_Used = visitTimeStamp;
					}
					else
					{
						if(cmd == CMD_TRADEIN)
						{
							if(!GameLogic.dealerVisitTimeStamp_TradeIn) GameLogic.dealerVisitTimeStamp_TradeIn = visitTimeStamp;
							vds = GameLogic.carDescriptors_TradeIn;
							hoursPassed = visitTimeStamp-GameLogic.dealerVisitTimeStamp_TradeIn;
							GameLogic.dealerVisitTimeStamp_TradeIn = visitTimeStamp;
						}
						else
						{
							if(!GameLogic.dealerVisitTimeStamp_New) GameLogic.dealerVisitTimeStamp_New = visitTimeStamp;
							vds = GameLogic.carDescriptors_New;
							hoursPassed = visitTimeStamp-GameLogic.dealerVisitTimeStamp_New;
							GameLogic.dealerVisitTimeStamp_New = visitTimeStamp;
						}
					}

					CarMarket.alterCars(used, vds, hoursPassed);

					int cheap; //this will turn on cheap cars in dealers (but not for trade-in dealership)

					if(!used && player.getMoney() < GameLogic.INITIAL_PLAYER_MONEY && !player.car && player.carlot.isEmpty()) cheap++;
					if(player.getMoney() < GameLogic.INITIAL_PLAYER_MONEY) cheap++;
					if(cheap) GameLogic.carDescriptors_New = CarMarket.getInitialCars(0);

					if(cmd == CMD_TRADEIN) used = -1;
					GameLogic.changeActiveSection(new CarMarket(used, vds, multiplayer));
				}
				else giveWarning("The car dealer is closed now! \n Opening hours: 7am to " + pass*(-1) + "pm");
			}
		}
		else
		if(cmd == CMD_CATALOG)
		{
			GameLogic.changeActiveSection(new Catalog(multiplayer));
		}
		else
		if(cmd == CMD_CLUBINFO)
		{
			GameLogic.changeActiveSection(new ClubInfo());
		}
		else
		if(cmd == CMD_CARINFO)
		{
			if(player.car) GameLogic.changeActiveSection(new CarInfo(GameLogic.player.car));
			else giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
		}
		else
		if(cmd == CMD_TIME)
		{
			GameLogic.spendTime(timeBtnInterval);
			
			//RAXAT: build 932, prestige decrease step fix
			float delta = (float)timeBtnInterval;
			if(timeBtnInterval == TIME_BTN_INTERVAL_NORMAL) delta = delta/150.0f;
			
			player.prestige -= 0.00333/delta; //RAXAT: total ~1 prestige point decrease every day
			if(player.prestige < 0) player.prestige = 0; //RAXAT: build 931, negative prestige fix
		}
		else
		if(cmd == CMD_LIFT)
		{
			if(player.car)
			{
				carLifted = true;
				mechanic.lift = carLifted;
				mechanic.inventory.wakeUpOnInstall = !carLifted;
				
				//step 1: show up loading screen
				Input.cursor.enable(0);
				Frontend.loadingScreen.show();
				
				//step 2: prepare the lift arm
				liftArmAmplitude = LIFT_ARM_AMPLITUDE_DEFAULT;
				liftArmAmplitudeSlider.setValue(liftArmAmplitude); //CMD_LIFT_ARM_MOVE will try to take its value, so we need to reset this slider
				lift.setSlotPos(2, new Vector3(0, liftArmAmplitude, 0), new Ypr(0));
				liftPhys.setMatrix(new Vector3(0), new Ypr(0)); //moving physical body off to prevent interference when clicking on car
				
				//step 3: move the car to lift
				osdCommand(CMD_LIFT_ARM_MOVE);
				player.car.command("start");
				player.car.command("stop");
				
				//step 4: kill old camera, make the new one and move it to the right place
				Vector3 pos = player.car.getPos();
				Ypr ori = player.car.getOri();
				pos.add(new Vector3(0.0, 1.5, -7.0)); //closer to front bumper
				ori.add(new Ypr(-Math.deg2rad(180), 0.0, 0.0)); //rotation makes the camera directed to the front side
				
				camera.destroy();
				camera = new GameRef(map, GameRef.RID_CAMERA, pos.toString() + "," + ori.toString() + ", 0x13, 1.0,1.0, 0.05", "Internal camera for garage");
				cameraSetup(camera);
				player.controller.command("camera "+ camera.id());
				
				//step 5: do the interface work
				osd.hideGroup(menuGroup);
				osd.showGroup(liftGroup);
				
				//step 6: release loading screen
				Frontend.loadingScreen.userWait(1.0);
				Input.cursor.enable(1);
				
				System.setLdPriority(System.LD_HIGH); //RAXAT: loading screen overrides loading priority

				if(player.checkHint(Player.H_VEHICLELIFT)) giveWarning("VEHICLE LIFT", "Use the lift to disassemble suspension parts of your car! \n \n Drag amplitude slider knob to adjust vertical position of the lift arm. Doors, trunks and other similar parts can't be opened when the car is on lift.");
			}
			else giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
		}
		else if(cmd == CMD_LIFT_ARM_MOVE)
		{
			liftArmAmplitude = liftArmAmplitudeSlider.value;
			
			Vector3 pos = new Vector3(liftPori[garageIndex].pos);
			Ypr ori = new Ypr(liftPori[garageIndex].ori);
			
			pos.y += liftVerticalOffset + liftArmAmplitude;
			player.car.setMatrix(pos, ori);
			
			lift.setSlotPos(2, new Vector3(0, liftArmAmplitude, 0), new Ypr(0));
		}
		else if(cmd == CMD_LIFT_DONE)
		{
			//todo: return the car and the camera to their default positions
			carLifted = false;
			mechanic.lift = carLifted;
			mechanic.inventory.wakeUpOnInstall = !carLifted;
			
			//step 1: show up loading screen
			Input.cursor.enable(0);
			Frontend.loadingScreen.show();
			
			//step 2: prepare the lift arm
			liftArmAmplitude = 0.0;
			lift.setSlotPos(2, new Vector3(0, liftArmAmplitude, 0), new Ypr(0));
			liftPhys.setMatrix(liftPori[garageIndex]); //moving back the physical body of lift
			
			//step 3: move the car to default position
			lockCar();
			
			//step 4: kill old camera, make the new one and move it to the right place
			camera.destroy();
			camera = new GameRef(map, GameRef.RID_CAMERA, cameraPos.toString() + "," + cameraOri.toString() + ", 0x13, 1.0,1.0, 0.05", "Internal camera for garage");
			cameraSetup(camera);
			player.controller.command("camera "+ camera.id());
			
			//step 5: do the interface work
			osd.hideGroup(liftGroup);
			osd.showGroup(menuGroup);
			
			//step 6: release loading screen
			Frontend.loadingScreen.userWait(1.0);
			Input.cursor.enable(1);
			
			System.setLdPriority(System.LD_HIGH); //RAXAT: loading screen overrides loading priority
		}
		else
		if(cmd == CMD_MECHANIC)
		{
			changeMode(MODE_MECH);
		}
		else
		if(cmd == CMD_TUNE)
		{
			Input.flushKeys();
			int code = GameLogic.kismajomCheck(GameLogic.kismajom);
			if(code >=0 && ((Player.c_garage && GameLogic.carrerInProgress) || GameLogic.DEBUG_MODE)) //RAXAT: build 931, patch for key codes
			{
				if(!roc)
				{
					player.setSteamAchievement(Steam.ACHIEVEMENT_CHEATER);
					switch(code)
					{
						case 0:	//letmeroc
							osdCommand(CMD_BEGIN_ROC);
							break;

						case 1:	//begformoney
							osdCommand(CMD_CHEATMONEY);
							break;

						case 2:	//reddevil
							int globalrank = GameLogic.findRacer(GameLogic.player);
							int maxrank = (GameLogic.CLUBS*GameLogic.CLUBMEMBERS)-1;
							
							if(globalrank < maxrank)
							{
								new SfxRef(GameLogic.SFX_CHEAT_MONEY).play();
								GameLogic.challenge(globalrank, maxrank, 0, 1, 1);
								player.club = GameLogic.CLUBS-1;
								GameLogic.changeActiveSection(this);
							}
							break;

						case 3:	//testme
							CarRequesterDialog d = new CarRequesterDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY, "Select car", "OK");
							if(d.display() == 0)
							{
								new SfxRef(GameLogic.SFX_CHEAT_MONEY).play();

								player.carlot.lockPlayerCar();
								player.carlot.saveCar(player.carlot.curcar);
								VehicleDescriptor vd = GameLogic.getVehicleDescriptor(VehicleType.VS_STOCK);
								player.car = new Vehicle(player, d.selectedCarID, vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear);
								player.carlot.flushCars();
								GameLogic.changeActiveSection(player.carlot);
							}
							break;

						case 4:	//fillmygarage
							new SfxRef(GameLogic.SFX_CHEAT_MONEY).play();

							Vector		stack = new Vector();
							Vector		chassisIDlist = new Vector();
							GameRef parts;

							stack.addElement(new GameRef(cars:0x0101r));
							if(stack)
							{
								while(!stack.isEmpty())
								{
									parts=stack.removeLastElement();
									if(parts.isScripted("java.game.parts.Part")) chassisIDlist.addElement(new Integer(parts.id()));
									else
									{
										if(parts=parts.getFirstChild())
										{
											while(parts)
											{
												if(parts.isScripted()) stack.addElement(parts);
												parts=parts.getNextChild();
											}
										}
									}
								}
							}

							for(int i=0; i<chassisIDlist.size(); i++)
							{
								player.carlot.lockPlayerCar();
								player.carlot.saveCar( player.carlot.curcar );
								VehicleDescriptor vd = GameLogic.getVehicleDescriptor( VehicleType.VS_STOCK );
								player.car = new Vehicle( player, chassisIDlist.elementAt(i).intValue(), vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear );
								player.carlot.flushCars();
							}

							GameLogic.changeActiveSection( player.carlot );
							break;
					}
				}
			}
			else changeMode(MODE_TUNE);
		}
		else
		if(cmd == CMD_PAINT)
		{
			if(player.car)
			{
				GameLogic.spendTime(1200);
				GameLogic.changeActiveSection(new PaintBooth(multiplayer));
			}
			else giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
		}
		else
		if(cmd == CMD_MOVE_PARTS) //todo: save/load adjusted position
		{
			changeMode(MODE_MOVE);
			if(player.checkHint(Player.H_MOVEPARTS)) giveWarning("MOVE PARTS", "You can move and rotate the parts of your car! \n \n Right-click the part to select it, then drag slider knobs to adjust its position.");
		}
		else
		if(cmd == CMD_MOVE_PARTS_DONE)
		{
			changeMode(MODE_TUNE);
		}
		else
		if(cmd == CMD_MOVE_PARTS_RESET) //will reset position and rotation of part to zero on call
		{
			Pori p = new Pori(new Vector3(0.0001), new Ypr(0)); //with 0.0001 slider will print a positive value
			partPos = p.pos;
			partOri = p.ori;
			
			partPosSlider[0].setValue(partPos.x);
			partPosSlider[1].setValue(partPos.y);
			partPosSlider[2].setValue(partPos.z);
			
			partPosSlider[0].printValue(partPos.x);
			partPosSlider[1].printValue(partPos.y);
			partPosSlider[2].printValue(partPos.z);
			
			partOriSlider[0].setValue(partOri.y);
			partOriSlider[1].setValue(partOri.p);
			partOriSlider[2].setValue(partOri.r);
			
			partOriSlider[0].printValue(partOri.y);
			partOriSlider[1].printValue(partOri.p);
			partOriSlider[2].printValue(partOri.r);

			if(targetPart)
			{
				targetPart.drag_matrix = p;
				osdCommand(CMD_MOVE_PARTS_SETPOS);
				osdCommand(CMD_MOVE_PARTS_SETORI);
			}
		}
		else
		if(cmd >= CMD_MOVE_PARTS_SETPOS && cmd < CMD_MOVE_PARTS_SETPOS+partPosSlider.length)
		{
			if(targetPart)
			{
				Vector3 pos = new Vector3(partPosSlider[0].value, partPosSlider[1].value, partPosSlider[2].value);
				
				partPosSlider[0].printValue(pos.x);
				partPosSlider[1].printValue(pos.y);
				partPosSlider[2].printValue(pos.z);
				
				partPos = pos;
				targetPart.drag_matrix.pos = pos;
				targetPart.updateDraggable(partPos, partOri);
			}
		}
		else
		if(cmd >= CMD_MOVE_PARTS_SETORI && cmd < CMD_MOVE_PARTS_SETORI+partOriSlider.length)
		{
			if(targetPart)
			{
				Ypr ori = new Ypr(partOriSlider[0].value, partOriSlider[1].value, partOriSlider[2].value);
				
				partOriSlider[0].printValue(ori.y);
				partOriSlider[1].printValue(ori.p);
				partOriSlider[2].printValue(ori.r);
				
				partOri = ori;
				targetPart.drag_matrix.ori = ori;
				targetPart.updateDraggable(partPos, partOri);
			}
		}
	}

	//easy dialog handling:
	public void giveWarning(String title, String text)
	{
		new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, title, text).display();
	}

	public void giveWarning(String text)
	{
		giveWarning("WARNING", text);
	}
}

//-----------------------------------------------------------------------
//-----------------------------------------------------------------------

//RAXAT: v2.3.1, extended and redesigned dialog
public class ROCEntryDialog extends Dialog
{
	final static int CMD_INFO0 = 0;
	final static int CMD_INFO1 = 1;
	final static int CMD_INFO2 = 2;
	final static int CMD_INFO3 = 3;
	final static int CMD_ENTER = 4;
	final static int CMD_CANCEL= 5;

	final static int RID_VALID	=  frontend:0xD100r;
	final static int RID_DENIED	=  frontend:0xD101r;
	final static int RID_INFO	=  frontend:0x92EEr;

	int	reqs, daysleft;
	Controller ctrl;

	public ROCEntryDialog( Controller ctrl, int reqs, int daysleft )
	{
		super(ctrl, DF_FULLSCREEN|DF_MODAL|DF_SUPERDARKEN|DF_FREEZE, null, null); //RAXAT: v2.3.1, darken 2X (DF_SUPERDARKEN)
		this.reqs = reqs;
		this.daysleft = daysleft;
		this.ctrl = ctrl;
	}

	public void show()
	{
		float top=-0.58, row0=-0.51, row1 = 0.51, step=0.14, x, y, size=0.11;

		//osd.createRectangle(0.0, 0.0, 2.0, 2.0, -2, Osd.RRT_DARKEN); //RAXAT: DF_SUPERDARKEN is used instead to achieve this effect
		osd.createRectangle(0.0, 0.0, 1.12, 2.0, -1, new ResourceRef(GameLogic.getBannerROC())); //RAXAT: banner texture itself, generated by GameLogic in v2.3.1

		Style buttonStyle = new Style( 0.11, 0.11, Frontend.mediumFont, Text.ALIGN_CENTER, null );
		Menu m = osd.createMenu( buttonStyle, row0, top, step, Osd.MD_VERTICAL );

		//RAXAT: info buttons
		for(int i=0; i<4; i++) m.addItem( new ResourceRef( RID_INFO ), CMD_INFO0+i, null, null, 1 );

		x=row1; y=top;
		ResourceRef icon;

		if(reqs & 0x02) icon = new ResourceRef(RID_VALID); else icon = new ResourceRef(RID_DENIED);
		osd.createRectangle( x, y, size, size, 1.0, 0.0, 0.0, 0, icon );    y+=step;

		if(reqs & 0x04) icon = new ResourceRef(RID_VALID); else icon = new ResourceRef(RID_DENIED);
		osd.createRectangle( x, y, size, size, 1.0, 0.0, 0.0, 0, icon );    y+=step;

		if(reqs & 0x01) icon = new ResourceRef(RID_VALID); else icon = new ResourceRef(RID_DENIED);
		osd.createRectangle( x, y, size, size, 1.0, 0.0, 0.0, 0, icon );    y+=step;

		if(reqs & 0x08) icon = new ResourceRef(RID_VALID); else icon = new ResourceRef(RID_DENIED);
		osd.createRectangle( x, y, size, size, 1.0, 0.0, 0.0, 0, icon );    y+=step;

		m = osd.createMenu(buttonStyle, -0.3275, 0.825, 0.65, Osd.MD_HORIZONTAL);
		Gadget g = m.addItem(new ResourceRef( Osd.RID_OK ), CMD_ENTER, "ENTER ROC", null, 1 );
		m.addItem(new ResourceRef(Osd.RID_CANCEL), CMD_CANCEL, "GO BACK TO GARAGE", null, 1 );
	
		String msg = "DAY OF ROC! ENTER NOW!";
		if(daysleft) msg = "ONLY " + daysleft + " DAYS LEFT!";
		osd.createText(msg, Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 0.775);

		if(reqs != 0x0F) g.disable();

		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, CMD_CANCEL, this);

		super.show();
	}

	public void osdCommand( int cmd )
	{
		if(cmd == CMD_INFO0) info("You must be among the top ranked players. \n Be within the top 5 of the Red Flame Racing Club and you can enter the R.O.C.");
	        else
		if(cmd == CMD_INFO1) info("Bring a highly tuned & respected car. Your car must have at least 750 prestige point.");
		else
		if(cmd == CMD_INFO2) info("Collect the entry fee: $100000. You'll get free repair and tuning services for this fee.");
		else
		if(cmd == CMD_INFO3) info("Total weight of your parts-bin is limited to 2000 lbs. \n Don't forget to bring enough spare tyres and N2O.");
		else
		if(cmd == CMD_CANCEL)
		{
			result = 0;
			notify();
		}
		else
		if(cmd == CMD_ENTER)
		{
			result = 1;
			if(daysleft != 0)
			{
				if(new YesNoDialog(ctrl, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_HIGHPRI, "QUESTION", "There's " + daysleft + " days left till the following Race of Champions. \n Do you want to skip the remainig days?").display())
					result = 0;
				else	GameLogic.spendTime( daysleft*24*3600-GameLogic.getTime()+8*3600 );
			}
			notify();
		}
	}

	public void info(String i)
	{
		new WarningDialog(controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_HIGHPRI, "INFO", i).display();
	}
}