package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;

//RAXAT: good class for testing new locations in game instantly
public class MapDebug extends Scene implements GameState
{
	Vehicle		standVhc;
	MapObject	standObj;
	GameState	parentState;

	Player		player;
	Osd		osd;
	ControlSetState	css;

	RenderRef	stuff;
	RenderRef	light, lighttype;

	GameRef         camera;
	Vector3		cameraPos = new Vector3(-2.2, 0.7, -3.5);
	Ypr		cameraOri = new Ypr(-2.5, -0.15, 0);

	Painter		painter;

	int		move;

	ResourceRef loadedPartSkin;
	Part	loadedPart, attachedPart;
	Chassis loadedChassis;
	Vehicle loadedCar;
	Text		debugTxt;
	int		mode;

	final static int CMD_LOADPART	= 0;
	final static int CMD_LOADCAR	= 1;
	final static int CMD_DYNATEX	= 2; //dynamic texture stability test
	
	final static int MODE_BROWSE = 0;
	final static int MODE_DRIVE = 1;
	
	final static int MAX_DYNATEX_X = 3600;

	final static Vector3 defCarPos = new Vector3(0.0, -0.535, 0.0);
	final static Vector3 defLookPos = new Vector3(0, 0.0, -0.5);

	Pori[]	slots = new Pori[6];
	Vehicle[] vhc = new Vehicle[6];

	RenderRef stand;
	float rot;
	
	Rectangle rectDynaTex;
	ResourceRef resDynaTex;
	int rectDynaTex_x = 1;

	public MapDebug()
	{
		createNativeInstance();
		internalScene = 0;
	}

	public void enter(GameState prevState)
	{
		mode = MODE_BROWSE;

		enableAnimateHook();

		map = new GroundRef(misc.dealer3:0x00000001r);
		stuff = new RenderRef(map, misc.dealer3:0x00000103r, null);
/*
		standVhc = new Vehicle(map, cars.racers.Furrano:0x00000006r, 0.5, 1.0, 1.0, 1.0, 1.0); //GT54
		standVhc.setMatrix(new Vector3(15.9, 0.5, 8.3), new Ypr(0,0,0));
		standVhc.command("reset");
		standVhc.command("stop");
*/
		standObj = new MapObject(map, misc.dealer3:0x00000032r); //stand.cfg
		standObj.setMatrix(new Vector3(15.9, 0.5, 8.3), new Ypr(0,0,0));

		//stand = new RenderRef(map, misc.dealer3:0x00000105r, null);
		//stand.setMatrix(new Vector3(15.9, -0.1, 8.3), new Ypr(0,0,0));

		slots[0] = new Pori(new Vector3(-5.588, -0.069, -14.380),	new Ypr( 2.359, 0.002,-0.004));
		slots[1] = new Pori(new Vector3(-10.342, -0.075, -15.170),	new Ypr(-2.748, 0.004, 0.000));
		slots[2] = new Pori(new Vector3(-9.728, -0.030, -5.897),	new Ypr(-2.329, 0.003, 0.002));
		slots[3] = new Pori(new Vector3(-14.284, 0.021, 5.487),		new Ypr(-1.931, 0.001, 0.003));
		slots[4] = new Pori(new Vector3(0.660, 0.011, 2.239),		new Ypr(2.086, 0.001, -0.005));
		slots[5] = new Pori(new Vector3(11.050, 0.012, 1.724),		new Ypr(2.135, 0.001, -0.005));

/*
		for(int i=0; i<slots.length; i++)
		{
			vhc[i] = new Vehicle(map, cars.racers.Furrano:0x00000006r, 0.5, 1.0, 1.0, 1.0, 1.0);
			vhc[i].setMatrix(slots[i].pos, slots[i].ori);
			vhc[i].command("reset");
			vhc[i].command("stop");
		}
*/

		debugTxt = new Text(Input.cursor, new ResourceRef(Text.RID_CONSOLE_RU), null, -0.95, -0.95);

		if(camera) camera.command("render 0");
		Integrator.isCity = 0;

		parentState = prevState;
		player = GameLogic.player;

		Frontend.loadingScreen.show();

		osd = new Osd();
		osd.globalHandler = this;
		osd.orientation = 1;

		lighttype = new RenderRef();
		lighttype.duplicate(new RenderRef(misc.dealer3:0x0030r));
		light = new RenderRef(map, lighttype, "neon");

		addSceneElements(9);

		if(mode == MODE_DRIVE) player.car = new Vehicle(player, cars.racers.Focer:0x00000108r, 0.5, 1.0, 1.0, 1.0, 1.0); //debug! Focer WRC
		camera = new GameRef(map, GameRef.RID_CAMERA, cameraPos.toString() + "," + cameraOri.toString() + ", 0x13, 1.0,1.0, 0.05", "Internal camera for paint booth (with collider)");
		cameraSetup(camera);

		lockCar();

		if(player.car)
		{
			player.car.setDamageMultiplier(0.0);
			player.car.setCruiseControl(0);
			player.car.setPos(defCarPos);
			player.car.wakeUp();

			for(int i=0; i<3; i++) player.car.command("filter " + i + " 0");
		}
		enterAsyncMode_Script();
		osd.menuKeysCreated = 1;
		osd.alienMp = mp;
		osd.createHotkey(Input.RCDIK_F, Input.KEY|Osd.HK_STATIC, CMD_LOADPART, Event.F_KEY_PRESS);
		osd.createHotkey(Input.RCDIK_G, Input.KEY|Osd.HK_STATIC, CMD_LOADCAR, Event.F_KEY_PRESS);
		osd.createHotkey(Input.RCDIK_H, Input.KEY|Osd.HK_STATIC, CMD_DYNATEX, Event.F_KEY_PRESS);
		osd.show();

		changePointer();
		Frontend.loadingScreen.display();

		setEventMask(EVENT_CURSOR|EVENT_COMMAND|EVENT_HOTKEY);

		//special request: reset mouse and set sensitivity to 0
		Input.getAxis (1, -1);
		Input.cursor.enable(1);

		Input.cursor.addHandler(this);
		Input.cursor.enableCameraControl(camera);
	}

	public void exit(GameState nextState)
	{
		clearEventMask(EVENT_ANY);
		leaveAsyncMode_Script();

		Input.cursor.enable(0);
		Input.cursor.remHandler(this);
		Input.cursor.disableCameraControl();

		camera.destroy();

		osd.hide();
		osd=null;

		if(player.car)
		{
			player.car.setDamageMultiplier(Config.player_damage_multiplier);
			player.car.setCruiseControl(0);
			player.car.setPos(defCarPos);
			player.car.wakeUp();
			player.car.command("suspend");

			for(int i=0; i<3; i++) player.car.command("filter " + i + " 0");
		}

		releaseCar();

		light.destroy();
		lighttype.destroy();

		remSceneElements();

		changePointer();

		if(stuff) stuff.destroy();
		map.unload();

		player.controller.reset();
		player.controller.activateState(ControlSet.MENUSET);
	}

	public void animate()
	{
		if(standVhc && standObj)
		{
			Ypr ro = new Ypr(0,0,0);
			rot+=0.005;
			ro.y = rot;
			standVhc.setMatrix(new Vector3(15.9, 0.5, 8.3), ro);
			//stand.setMatrix(new Vector3(15.9, -0.1, 8.3), ro);
			standObj.setMatrix(new Vector3(15.9, 0.5, 8.3), ro);
		}
		
		if(rectDynaTex)
		{
			rectDynaTex_x = (rectDynaTex_x%MAX_DYNATEX_X)+1;
			rectDynaTex.setPos(new Vector3(Math.sin(rectDynaTex_x),0,0));
			debugTxt.changeText("sin(rectDynaTex_X): " + Math.sin(rectDynaTex_x));
		}
		else
		{
			if(player && player.car) debugTxt.changeText("vhc pos: " + player.car.getPos().toString() + " vhc ori: " + player.car.getOri().toString());
			else
			{
				if(camera) debugTxt.changeText("cam pos: " + camera.getPos().toString() + " cam ori: " + camera.getOri().toString());
			}
		}

		super.animate();
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
			if(mode != MODE_DRIVE) player.car.command("stop"); //grab
		}
	}

	public void releaseCar()
	{
		if(player.car)
		{
			player.car.command("reset");
			player.car.command("start"); //release

			player.car.setParent(player);
		}
	}

	public void cameraSetup(GameRef cam)
	{
		cam.command("render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)); //vp id, cam id, flags
		cam.command("dist 2.7 3.3");
		cam.command("smooth 0.15 0.5");
		cam.command("zoom 70 3");
		cam.command("force 3.1 0.5 -0.2"); //with inertial movement support
		cam.command("torque 0.12");

		if(player.car)
		{
			cam.command("move " + player.car.id() + " 0,0,0 4.5");
			cam.command("look " + player.car.id() + " 0,0.0,0 0,0,0");
		}
		else
		{
			if(map)
			{
				cam.command("move " + map.id() + " " + defLookPos.toString() + " 3.0");
				cam.command("look " + map.id() + " " + defLookPos.toString() + " 0,0,0");
			}
		}

		if(mode == MODE_BROWSE)
		{
			player.controller.reset();
			player.controller.activateState(ControlSet.CAMTURNSET);
			player.controller.command("controllable " + cam.id());
			player.controller.activateState(5, 1);
		}
		else
		{
			releaseCar();
			player.controller.command("controllable " + player.car.id());
			player.controller.activateState(ControlSet.DRIVERSET);
		}

	}

	public void changePointer()
	{
		if(move) Input.cursor.setPointer(Frontend.pointers, "A");
		else Input.cursor.setPointer(Frontend.pointers, "J");
	}
	
	public void handleMessage(Message m)
	{
		if(m.type == Message.MT_EVENT)
		{
			int cmd = m.cmd;
			
			switch(cmd)
			{
				case(CMD_LOADPART): //load/save part test
					String fname = "testpart";
					if(!File.exists(fname))
					{
						int rnd = (int)(Math.random()*GameLogic.CARCOLORS.length);
						
						GameRef xa = new GameRef();
						loadedPart = xa.create(map, new GameRef(cars.racers.Baiern:0x00000121r),	"100,0,0,0,0,0", "loaded carpart"); //FL door
						loadedPart.setTexture(GameLogic.CARCOLORS[rnd]);
						
						attachedPart = xa.create(map, new GameRef(cars.racers.Baiern:0x00000104r),	"100,0,0,0,0,0", "loaded carpart"); //L mirror
						attachedPart.setTexture(GameLogic.CARCOLORS[rnd]);
						attachedPart.command( "install " + 0 + " " + loadedPart.id() + " " + 5 + " " + attachedPart.id() + " " + 24 );
						
						File f = new File(fname);
						f.open(File.MODE_WRITE);
						loadedPart.save(f);
						f.close();
						
						//todo: save paintjob here
					}
					else
					{
						File f = new File(fname);
						f.open(File.MODE_READ);
						loadedPart = Part.createFromFile(f, map);
						f.close();
					}
					
					if(loadedPart) //always random color to check stability
					{
						loadedPart.setMatrix(camera.getPos(), camera.getOri());
						loadedPart.command("reset");
						//todo: load paintjob here
					}
 					
					break;
					
				case(CMD_LOADCAR):
					//String fname = "test/ROC_GT3_1";
					String fname = "save/cars/database/ACrow";
					
					/*
					GameRef xa = new GameRef();
					loadedChassis = xa.create(map, new GameRef(cars.racers.Baiern:0x000000FDr),	"100,0,0,0,0,0", "loaded carpart");

					Chassis chassis = loadedChassis;
					chassis.suspend_update = 1;
					chassis.addStockParts(new Descriptor(GameLogic.CARCOLORS[(int)(Math.random()*GameLogic.CARCOLORS.length)], 1.0f, 1.0f, 1.0f, 1.0f));
					chassis.suspend_update = 0;
					chassis.forceUpdate();
					
					loadedCar = new Vehicle(chassis);
					loadedCar.setParent(map);
					loadedCar.setMatrix(camera.getPos(), camera.getOri());
					loadedCar.command("reset");
					*/
					
					loadedCar = Vehicle.load(fname, map);
					
					//loading ROC cars wont crash, seems like bot vehicles are the problem
					/*
					int rnd = (int)(Math.random()*(GameLogic.speedymen.length-1));
					Bot b = new Bot(rnd);
					loadedCar = b.getCar(map);
					*/
					loadedCar.setMatrix(camera.getPos(), camera.getOri());
					loadedCar.command("reset");
					
					break;
					
				case(CMD_DYNATEX):
					if(!rectDynaTex)
					{
						float len = 0.6;
						float aspect = (float)Config.video_x/(float)Config.video_y;
						resDynaTex = new ResourceRef(cars:0x0000002Ar);
						rectDynaTex = osd.createRectangle(0, 0, len, len*aspect, 0, resDynaTex, 0);
					}
					else
					{
						//this way of making dynamic texture seems to be absolutely correct
						resDynaTex.finalize();
						resDynaTex = null;
						
						resDynaTex = new ResourceRef();
						resDynaTex.makeTexture(new ResourceRef(system:0x0008r), "imagecode.png");
						//rectDynaTex.changeTexture(resDynaTex);
						
						/*
						//even dynamic sounds work fine
						SfxRef res = new SfxRef();
						res.makeSound(new ResourceRef(system:0x0009r), "sound/wav/misc/HornLp2.WAV");
						res.play();
						res.finalize();
						res=null;
						*/
						
						if(loadedPart)
						{
							if(loadedPartSkin)
							{
								loadedPartSkin.finalize();
								loadedPartSkin = null;
							}
							
							loadedPartSkin = new ResourceRef(loadedPart.getTexture());
							rectDynaTex.changeTexture(loadedPartSkin);
							loadedPart.setTexture(resDynaTex.id()); //this will damage the texture pointer!! getTexture() will return garbage!
							//loadedPart.setTexture(loadedPart.getTexture());
						}
					}
					break;
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
			int pick_ref = param.token(2).intValue();

			if(ec == GameType.EC_RCLICK)
			{
				GameRef dest = new GameRef(param.token(++tok).intValue());
				int cat = dest.getInfo(GameType.GII_CATEGORY);

				if(cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE)
				{
					camera.command("look " + dest.id() + " " + param.token(++tok) + "," + param.token(++tok) + "," + param.token(++tok));
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
}