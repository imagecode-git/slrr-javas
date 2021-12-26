package java.game;

import java.render.osd.*;
import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.sound.*;

//RAXAT: dedicated test area
public class GameDebug extends Gamemode implements Callback //callbacks are finally supported
{
	Spectator roadGuy;
	PhysicsRef phy;

	TrafficVehicle tVhc;
	Vector gVhc;
	RandomBox box;

	int dist_init;

	Light[] light;

	GameType doll;
	Vector3 dollPos;

	Gauge gauge;
	Slider[] palette;
	Slider[] designer, dollDesigner;
	Rectangle designDummy;

	SlidingMenu slider;
	Text slider_txt;

	int test; //id of test

	Rectangle rect, rect2;

	ParticleSystem PS;
	RenderRef PSres;
	Vector3 PSvelo;
	int ps;

	int showMyPos = 0;

	Timer callbackTimer;

	public GameDebug(){}

	public void init()
	{
		name = "Debug";
		shieldIcon = new ResourceRef(sl.Scripts.game.Gamemodes.stdpack_231:0x1007r);
		useDebug = 1; //prevents using fog in Track
	}

	public void launch()
	{
		light = new Light[14];
		PSvelo = new Vector3(0,1,5);

		roadGuy = new Spectator();
		tVhc = new TrafficVehicle();
		gVhc = new Vector();
		box = new RandomBox();

		failable = 0; //events for this gamemode cannot have a "failed" status when exit

		//debug switches for Track
		if(gTrack)
		{
			gTrack.debugSFX_enabled = 0; //map SFX debug
			gTrack.debugGRD_enabled = 0; //start grid debug
			gTrack.debugOSD_enabled = 0; //debug OSD setup
			gTrack.debugKey_enabled = 1; //quick feature test with a special debug key
			gTrack.debugMap_enabled = 1; //debug tools for minimaps
			gTrack.debugRC_enabled  = 0; //remote control debug

			PSres = new RenderRef(multibot.scripts:0x00001021r);
			PS = new ParticleSystem(gTrack.cam, PSres);
			PS.init(gTrack.map, PSres, null);
			PS.modePermanent(1);
		}

		gmcEvent.useCamAnimation = 0;
		getOSD(); //get instance of OSD from Track()

		gmThread = new Thread( this, "debug mode GM animation thread", 1 ); //extended! thread-oriented methodology (THOR), no injections
		gmThread.setPriority(Thread.MAX_PRIORITY);
		gmThread.start();

		tMethods = 8; //amount of THOR methods

		for(int i=0; i<(tMethods+1); i++)
		{
			gmThread.addMethod(i);
		}

		//override GPS frame created by Track()
		if(GameLogic.player)
		{
			GameLogic.player.gpsState = 0;
			GameLogic.player.handleGPSframe(GameLogic.player.gpsState);
		}

		osd.menuKeysCreated = 1;
		osd.alienMp = mp;
		super.launch();

		hideGPSFrame();

//		einstein();
//		spawnPalette();
//		spawnDesigner();

//		GameLogic.player.car = new Vehicle( GameLogic.player, cars.racers.Furrano:0x00000006r, 1.0, 1.0, 1.0, 1.0, 1.0 );
	}

	public void cheatWin()
	{
		handleDebug();
	}

	//gTrack calls this when pressing DKEY; put all debug stuff for testing here
	public void handleDebug()
	{
		callbackTest();

//		dist_init = GameLogic.player.car.chassis.getMileage(); //to measure travelled distances
//		debug("test");
//		detectCarClass();
//		spawnObjects();
//		relativeMatrix();

/*
		if(!doll) ruleMyDolls();
		else
		{
			System.trace("");
			System.trace("doll #" + objects.size());
			System.trace("doll XYZ: " + doll.getPos().toString() + ", doll YPR: " + doll.getOri().toString());
			new SfxRef(Frontend.SFX_MONEY).play();
			newDoll(); //spawn clone of doll
		}
*/

//		msgBoxHoldTest();

//		if(!slider) makeSlidingMenu();
//		else testSlidingMenu();

//		lightTest();

//		rescaleRect();
//		if(!gauge) makeGauges();

//		randomBoxTest();

//		soundTest();
//		PStest();

//		tVhc = createTraffic(cars.traffic.Ambulance:0x00000006r, gTrack.startGridData.elementAt(4), gTrack.startGridData.elementAt(5));
//		graphTest();
//		projLine2DTest();
//		GameLogic.player.car.chassis.setMaxSteering(0.95);
	}

	public void hideOSD()
	{
	}

	public void clearScreen()
	{
		gTrack.clearMsgBoxes();
		gTrack.lockMsgBoxes();
		gTrack.enableOsd(0);
		hideOSD();
	}

	public void handleEvent( GameRef obj_ref, int event, int param )
	{
		if( event == EVENT_TIME && !destroy )
		{
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
			if(showMyPos) debug("pos: " + GameLogic.player.car.getPos().toString() + ", ori: " + GameLogic.player.car.getOri().toString());

			if(ps)
			{
//=====================================
				//snow in the game!
				PSres = new RenderRef(multibot.scripts:0x00001021r);
				PS = new ParticleSystem(gTrack.cam, PSres);

				switch(ps)
				{
					case 1:
						PS.setDirectSource("source", gTrack.cam.getPos(), 50, 100, PSvelo, 75, 50, 200, gTrack.cam.getInfo(GameType.GII_BONE)); //light snow
						break;
					case 2:
						PS.setDirectSource("source", gTrack.cam.getPos(), 100, 300, PSvelo, 15, 50, 300, gTrack.cam.getInfo(GameType.GII_BONE)); //light snow 2
						break;
					case 3:
						PS.setDirectSource("source", gTrack.cam.getPos(), 100, 30, PSvelo, 15, 50, 300, gTrack.cam.getInfo(GameType.GII_BONE)); //heavy snow
						break;
				}
//=====================================
			}

			if(dist_init)
			{
				int dist = GameLogic.player.car.chassis.getMileage() - dist_init;
				debug("distance travelled: " + dist);
			}

			//debug(getCarMatrix());

			Vector3 cpos;
			float cang;

			if(gauge) //display info on the analog gauge
			{
				/*
				//display the direction pointing to cursor
				if(Input.cursor)
				{
					cpos = Input.cursor.getPos();
					float x = cpos.y*(-1);
					float y = cpos.x*(-1);
					cang = Math.atan2(x,y);
					debug("ATAN2: " + cang + ", X: " + x + ", Y: " + y);

					gauge.setValue(Math.deg2rad(cang));
				}
				*/

				//display direction angle of car on gauge
				//float dir = Math.atan2(GameLogic.player.car.getVel().z, GameLogic.player.car.getVel().x);
				//gauge.setValue(Math.deg2rad(dir));

				//debug(Math.rad2deg(GameLogic.player.car.getOri().y));

				Ypr ori = GameLogic.player.car.getOri();
//				gauge.setValue((ori.y-gTrack.cam.getOri().y)*1.25);
				gauge.setValue((Math.abs(ori.y)-Math.abs(gTrack.cam.getOri().y))*1.35);

				int y1 = ori.y;
				int y2 = gTrack.cam.getOri().y;
				int sign;

//				if(y1<0 && y2<0) sign = -1; else sign = 1;
//				anglemeter.setValue(sign*Math.rad2deg((Math.abs(ori.y)-Math.abs(gTrack.cam.getOri().y))*1.35));

				debug("vhc: " + Math.rad2deg(GameLogic.player.car.getOri().y) + ", cam: " + Math.rad2deg(gTrack.cam.getOri().y));
			}

			if(palette)
			{
				for(int i=0; i<4; i++) palette[i].changeVLabelText(palette[i].value);

				int A = palette[0].value*16777216;
				int R = palette[1].value*65536;
				int G = palette[2].value*256;
				int B = palette[3].value;

				palette[0].changeVLabelColor(A+R+G+B);
			}

			if(designer)
			{
				//simple visual editor for interface
				//if(gauge) gauge.setPos(designer[1].value, designer[2].value);

				float mul = designer[0].value;
				for(int i=0; i<4; i++) designer[i].changeVLabelText(designer[i].value);
				designDummy.setPos(new Vector3(designer[1].value*mul, designer[2].value*mul, (int)designer[3].value));
				//debug(designDummy.pos.toString());
			}

			if(dollDesigner)
			{
				doll.setMatrix(new Vector3(dollPos.x+dollDesigner[0].value, dollPos.y+dollDesigner[1].value, dollPos.z+dollDesigner[2].value), new Ypr(Math.deg2rad(dollDesigner[3].value),0,0));
				gTrack.setMessage2("doll ori: " + doll.getOri().toString());
				debug("doll pos: " + doll.getPos().toString());
			}

			if(gmThread.methodStatus(0) == 1) //THOR method #0
			{
				debug("test");
				gmThread.controlMethod(0,-1);
			}

			//surface test
//			if(GameLogic.player.car.chassis.getMaterialIndex() > 0)
//				debug("matIndex: " + GameLogic.player.car.chassis.getMaterialIndex() + ", name: " + gTrack.map.getMaterialName(GameLogic.player.car.chassis.getMaterialIndex()));

			gmThread.sleep(10);
		}
	}

	public void finalize()
	{
		super.finalize();
	}

	//----------debug tests:
	//new F3 camera change
	void F3cam()
	{
		if(GameLogic.player.car && gTrack.cam)
		{
			//new F3 camera change
			gTrack.cam.setPos(gTrack.lastCamPos);
			gTrack.changeCamFollow();
		}
	}

	//pic-in-pic VP, renders F1 camera
	void vpPIP()
	{
		gTrack.vport2 = new Viewport( 12, 0.99, 0.01, 0.3, 4.0/3.0, -1.0, 0.0 );
		gTrack.vport2.activate( Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET );
		if(gTrack.cameraTarget)
		{
			GameRef con = new GameRef(gTrack.cameraTarget.getInfo(GII_OWNER));
			gTrack.cameraTarget.command( "render " + gTrack.vport2.id() +" "+ con.id() +" "+ gTrack.cameraNum);
		}
	}

	//saving vector to file
	void fileSave()
	{
		Vector x = new Vector();
		File save_test = new File("save_test");
		x.addElement("FUUUU 1");
		x.addElement("FUUUU 2");
		x.addElement("FUUUU 3");
		x.addElement("FUUUU 4");
		if(!File.exists("save_test"))
		{
			save_test.open(File.MODE_WRITE);
			save_test.write(x);
			save_test.close();
			debug("file write OK!");
		}
		else
		{
			save_test.open(File.MODE_READ);
			Vector result = save_test.readVector(new String());
			debug(result.elementAt(2));
			save_test.close();
		}
	}

	//show up finish window with a given mode
	void fWin(int mode)
	{
		clearScreen();
		finishRace(mode); //try 0 or 1, more values in Gamemode.class
	}

	//thread-based looping SFX
	void loopSFX()
	{
		SfxRef xxx = new SfxRef("frontend\\sounds\\money.wav", 1.0, 3.0);
		if(!xxx.looping) xxx.loopPlay();
	}

	//basic savegame stuff
	void saveGameTest()
	{
		if(!bestTime)
		{
			bestTime = 50.50;
			GameLogic.player.bestTimes.addElement(new Integer(gmcEvent.track.map_id));
			GameLogic.player.bestTimes.addElement(new Float(bestTime));
			GameLogic.player.bestTimes.addElement(gmcEvent.track.name);
			debug("vectors are created");

			GameLogic.player.loadTrackData();
			debug("data loaded: " + GameLogic.player.bestTimes.elementAt(2));
		}
		else
		{
			GameLogic.player.saveTrackData();
			debug("data saved");
		}
	}

	//spectators, trees, vehicles, traffic, whatsoever
	void spawnObjects()
	{
//		createSpectator(humans:0x002fr, humans:0x0014r, gTrack.startGridData.elementAt(2), gTrack.startGridData.elementAt(3), "spectator");
//		createTraffic(cars.traffic.Ambulance:0x00000006r, gTrack.startGridData.elementAt(4), gTrack.startGridData.elementAt(5));
//		createTraffic(cars.traffic.Schoolbus:0x00000006r, gTrack.startGridData.elementAt(6), gTrack.startGridData.elementAt(7), "1,1,1,1,1,1", "trafficVehicle");
//		createObject(objects:0x00000172r, gTrack.startGridData.elementAt(8), gTrack.startGridData.elementAt(9));
//		createObject(objects:0x00000546r, gTrack.startGridData.elementAt(10), gTrack.startGridData.elementAt(11), "0,0,0,0,0,0", "mapObject", 1);
//		createObject(objects:0x000006CDr, gTrack.startGridData.elementAt(6), gTrack.startGridData.elementAt(7)); //high street lamp
//		createObject(objects:0x0000018Er, gTrack.startGridData.elementAt(2), gTrack.startGridData.elementAt(3)); //high street lamp 2 (any difference?)
//		createObject(objects:0x0000018Cr, gTrack.startGridData.elementAt(2), gTrack.startGridData.elementAt(3)); //park lamp
	}

	//science!
	void einstein()
	{
//		System.trace("abs(-33) = " + Math.abs(-33));
//		System.trace("abs(-2.4) = " + Math.abs(-2.4));
//		System.trace("getPeriods(50, 8) = " + Math.getPeriods(50,8));

//		Number result = Math.remDiv(50,8);
//		System.trace("remDiv(50, 8) = N: " + result.i + ", " + result.f); //6 with remainder 2

//		System.trace("atan(0.57735) = " + (int)Math.atan(0.57735)); //30
//		System.trace("atan(0.83909) = " + (int)Math.atan(0.83909)); //40
//		System.trace("atan(1.0) = " + (int)Math.atan(1.0)); //45
//		System.trace("atan(1.19175) = " + (int)Math.atan(1.19175)); //50
//		System.trace("atan(1.73205) = " + (int)Math.atan(1.73205)); //60
//		System.trace("atan(3.73205) = " + (int)Math.atan(3.73205)); //75
//		System.trace("atan(2) = " + (int)Math.atan(2)); //63.43494
//		System.trace("atan(0.875) = " + (int)Math.atan(0.875)); //41.1859

//		System.trace("atan2(40.0, 35.0) = " + Math.atan2(40.0, 35.0)); //41.1859deg
//		System.trace("atan2(0.0, -20.0) = " + Math.atan2(0.0, -20.0)); //-90deg, -1.5707rad (-PI/2)
//		System.trace("atan2(120.0, -50.0) = " + Math.atan2(120.0, -50.0)); //-22.62deg, -0.39479rad
//		System.trace("atan2(-140.0, -36.0) = " + Math.atan2(140.0, -36.0)); //-14.42deg, -0.251689rad
//		System.trace("atan2(267.0, 0.0) = " + Math.atan2(267.0, 0.0)); //0.0f
//		System.trace("atan2(-90.0, 0.0) = " + Math.atan2(-90.0, 0.0)); //3.14159265 (PI)
//		System.trace("atan2(0.0, 0.0) = " + Math.atan2(0.0, 0.0)); //0.0f (undefined)

//		System.trace("fact(4,2) = " + Math.fact(4,2)); //1*3*5*7 = 105

//		System.trace("asin(0.5) = " + Math.asin(0.5)); //30
//		System.trace("asin(0.3) = " + Math.asin(0.3)); //17
//		System.trace("asin(0.707) = " + Math.asin(0.707)); //45

//		System.trace("acos(0.25) = " + Math.acos(0.25)); //75.5224
//		System.trace("acos(0.707) = " + Math.acos(0.707)); //45
//		System.trace("acos(0.965) = " + Math.acos(0.965)); //15

//		System.trace("cotan(30) = " + Math.cotan(30)); //1.73205
//		System.trace("atan(1.73205) = " + Math.atan(1.73205)); //59.99998
//		System.trace("acotan(1.73205) = " + Math.acotan(1.73205)); //30deg, 0.52335rad

/*
		float[] arr = new float[10];
		for(int i=0; i<arr.length; i++)
		{
			arr[i] = 2*(i+1);
			System.log("arr[" + i + "]: " + arr[i]);
		}
		System.trace("total(arr[10]) = " + Math.total(arr)); //110
		System.trace("avg(arr[10]) = " + Math.avg(arr)); //11.0
*/
	}

	//operations with matrix object
	void matrix()
	{
		Matrix mtx = new Matrix(4,5);
		mtx.setElementAt(new Integer(8), 2, 2);
		mtx.setElementAt(9, 3, 3);
		mtx.setElementAt(5.4, 0, 1);
		if(mtx)
		{
			System.trace("Matrix " + mtx.i + "x" + mtx.j); //valid: 4x5
			System.trace("Matrix Integer.number at (2,2): " + mtx.elementAt(2,2).number ); //valid: 8
			System.trace("Matrix getValue(3,3): " + mtx.getValue(3,3)); //valid: 9.0
			System.trace("Matrix getValue(0,1): " + mtx.getValue(0,1)); //valid: 5.4
			System.trace("Matrix total columns: " + mtx.getColumns()); //valid: 4 (i)
			System.trace("Matrix total rows: " + mtx.getRows()); //valid: 5 (j)
			mtx.setValue(new Integer(7));
			System.trace("Matrix value: " + mtx.getValue(2,2)); //valid: 7.0
			mtx.setValue(new Float(6.2));
			System.trace("Matrix value: " + mtx.getValue(2,2)); //valid: 6.2
		}
	}

	//randomizer for Vector
	void vectorShuffle()
	{
		Vector tt = new Vector();
		for(int i=0; i<10; i++)	tt.addElement(new Integer(i));
		tt.shuffle();
		tt.shuffle();
		tt.shuffle();
		tt.shuffle();
		debug("shuffle completed");
	}

	//vehicle/chassis creation benchmark
	void vhcBench(int cars)
	{
		for(int i=0; i<cars; i++)
		{
			GameRef xa = new GameRef();
			debug("creating chassis #" + i);
			Chassis chassis = xa.create( gTrack.map, new GameRef(cars.racers.Furrano:0x00000006r),	"0,0,0,0,0,0", "Vehicle" );
			System.log("chassis #" + i + " created: " + chassis.name);

			debug("creating Vehicle #" + i);
			Vehicle vhc = new Vehicle(chassis);
			System.log("Vehicle #" + i + " created: " + vhc.chassis.name);
		}
	}

	//execute THOR
	void runTHOR(int id)
	{
		debug("runTHOR: id = " + id);
		gmThread.execute(id);
	}

	//check status for THOR method
	void checkTHOR(int id)
	{
		debug("THOR method id " + id + " status: " + gmThread.methodStatus(id));
	}

	//show up msgBox
	void msgBox(String text)
	{
//		gTrack.showMsgBox(text, Track.MBOX_BLUE, Track.MBOX_SHORT );
		gTrack.showMsgBox(text, Track.MBOX_GREEN, Track.MBOX_MID );
//		gTrack.showMsgBox(text, Track.MBOX_RED, Track.MBOX_LONG );
	}

	//debug a holding msgBox
	void msgBoxHoldTest()
	{
		String holdMessage = gTrack.busyBox[2];

		msgBox("test 1");
		msgBox("test 2");
		msgBox("test 3");
		gTrack.holdMsgBox(holdMessage); //BUG! 2nd box is being hold! must be 3rd
		//when 3 msgBoxes does appear, track says that 3rd box is not busy (busyBox[2] = 0), try to fix that in order to remove holdMsgBox bug
		//probably, msgBox constructor is just buggy/unfinished, try to find some vulnerable places in it
		//clearMsgBoxes() also seems to be buggy
	}

	//trace all the contents of eventInfo (it stores player's career data)
	void dumpEventInfo()
	{
		if(GameLogic.player.eventInfo.size())
		{
			for(int i=0; i<GameLogic.player.eventInfo.size(); i++)
			{
				if(GameLogic.player.eventInfo.elementAt(i) instanceof Integer) System.trace(GameLogic.player.eventInfo.elementAt(i).number);
				else System.trace(GameLogic.player.eventInfo.elementAt(i));
			}
		}
		debug("eventInfo dumped!");
	}

	//get name of class that player's car does belong to
	void detectCarClass()
	{
		String cl = gmcEvent.getClass(gmcEvent.checkClass());

		if(cl) debug("getClass(): " + cl);
		else debug("getClass(): UNDEFINED");
	}

	//return player.car matrix
	String getCarMatrix()
	{
		if(GameLogic.player.car) return "pos: " + GameLogic.player.car.getPos().toString() + "; ori: " + GameLogic.player.car.getOri().toString();

		return null;
	}

	//rescale animation for rectangles
	void rescaleRect()
	{
		if(!rect && !rect2)
		{
			rect2 = osd.createRectangle( 0.0, 0.0, 0.26, 0.47, 1, gmcEvent.e_minimap, 0 );
			rect = osd.createRectangle( 0.0, 0.0, 0.26, 0.47, 1, gmcEvent.e_minimap, 0 );
		}
		else
		{
			switch(test)
			{
				case 0:
//					rect.rescale(0.1);
					rect.runAnimation(0.05, 10, 1, "S"); //bigger (1)
					break;

				case 1:
//					rect.rescale(-0.1);
					rect.runAnimation(0.05, 10, -1, "S"); //smaller (-1)
					break;
			}
			test++;
		}
	}

	//JVM-based analog gauges
	void makeGauges()
	{
//		gauge = new Gauge(osd, 2.075, 0.5);
		gauge = new Gauge(osd, 0, 0);
		gauge.setPos(0.65,-1.15);
		gauge.rescale(-0.25);
	}

	//ARGB palette with 4 sliders
	void spawnPalette()
	{
		Style sld1 = new Style( 0.45, 0.06, Frontend.mediumFont, Text.ALIGN_LEFT, new ResourceRef(Osd.RID_SLD_BACK) );
		Style sldk =  new Style( 0.04, 0.05, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB) );
		Style butt0 = new Style( 0.45, 0.12, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST );

		Menu m = osd.createMenu( butt0, -0.400, 0.550, 0 );
		m.setSliderStyle( sld1, sldk );

		palette = new Slider[4];
		String[] name = new String[4];
		name[0] = "A"; name[1] = "R"; name[2] = "G"; name[3] = "B";

		for(int i=0; i<4; i++)
		{
			palette[i] = m.addItem(name[i], 0, 0, 0.0, 255.0, 0, null);
			palette[i].setTicks(256);
		}
	}

	//mini-editor for interface rectangles
	void spawnDesigner()
	{
		Style sld1 = new Style( 0.45, 0.06, Frontend.mediumFont, Text.ALIGN_LEFT, new ResourceRef(Osd.RID_SLD_BACK) );
		Style sldk =  new Style( 0.04, 0.05, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB) );
		Style butt0 = new Style( 0.45, 0.12, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST );

		Menu m = osd.createMenu( butt0, -0.400, 0.550, 0 );
		m.setSliderStyle( sld1, sldk );

		designer = new Slider[4];
		String[] name = new String[4];
		name[0] = "M"; name[1] = "X"; name[2] = "Y"; name[3] = "Z"; //M - multiplier for XYZ

		designer[0] = m.addItem(name[0], 0, 0, 0.0, 2.0, 0, null);
		designer[0].setTicks(1000);
		designer[0].setValue(1.0);

		for(int i=1; i<3; i++)
		{
			designer[i] = m.addItem(name[i], 0, 0, -4.0, 4.0, 0, null);
			designer[i].setTicks(10000);
		}

		designer[3] = m.addItem(name[3], 0, 0, -1.0, 6, 0, null);
		designer[3].setTicks(7);

		designDummy = osd.createRectangle( 0.0, 0.0, 0.26, 0.47, 0, gmcEvent.e_minimap, 0 );
	}

	//build sliding menu
	void makeSlidingMenu()
	{
		int big;

		if(big)
		{
			slider = osd.createSlidingMenu(-0.5, 0.75, 0.255, 0.5, 0, SlidingMenu.STYLE_HORIZONTAL); //single sliding menu, no animation
			for(int i=0; i<7; i++) slider.addItem(0.2, 0.35, 2, frontend:0x0000C10Dr, frontend:0x0000C20Dr, 0, "TEST " + i);
		}
		else
		{
			slider = osd.createSlidingMenu(-0.365, 0.75, 0.185, 0.35, 2, SlidingMenu.STYLE_HORIZONTAL); //single sliding menu, animated rescale
			for(int i=0; i<7; i++) slider.addItem(0.2, 0.35, 2, frontend:0x0000C10Dr, frontend:0x0000C20Dr, 0, "TEST " + i);
		}
	}

	//test modes for sliding menu
	void testSlidingMenu()
	{
		if(!test) slider.activate(2);
		else
		{
			switch(test)
			{
				case 1:  slider.execute(Osd.CMD_MENU_RG); break;
				case 2:  slider.execute(Osd.CMD_MENU_RG); break;
				case 3:  slider.execute(Osd.CMD_MENU_LF); break;
				case 4:  slider.execute(Osd.CMD_MENU_LF); break;
				case 5:  slider.execute(Osd.CMD_MENU_LF); break;
				case 6:  slider.execute(Osd.CMD_MENU_LF); break;

				case 7:  slider.deactivate(); break;
				case 8:  slider.activate(); break;
				case 9:  slider.execute(Osd.CMD_MENU_RG); break;
				case 10: slider.execute(Osd.CMD_MENU_RG); break;
				case 11: slider.execute(Osd.CMD_MENU_LF); break;
				case 12: slider.execute(Osd.CMD_MENU_LF); break;
				case 13: slider.execute(Osd.CMD_MENU_RG); break;

//				case 7:  slider.execute(Osd.CMD_MENU_LF); break;
//				case 8:  slider.execute(Osd.CMD_MENU_LF); break;
//				case 9:  slider.execute(Osd.CMD_MENU_RG); break;
//				case 10:  slider.execute(Osd.CMD_MENU_RG); break;
//				case 12:  slider.execute(Osd.CMD_MENU_RG); break;
//				case 13:  slider.execute(Osd.CMD_MENU_RG); break;

				//for multiple items enabled:
				//case 0:	slider.activate(0); break;
				//case 1:	slider.activate(1); break;
				//case 2:	slider.activate(2); break;

				//show/hide tests:
				//case 1:  slider.teleport(0.1, 5, 1, "Y"); break;
				//case 2:  slider.teleport(0.1, 5, 1, "Y"); break;
			}
		}
		test++;
	}

	//handle map objects
	void ruleMyDolls()
	{
		dollPos = gTrack.startGridData.elementAt(2);
		createObject(objects:0x000006CDr, dollPos, gTrack.startGridData.elementAt(3)); //high street lamp
//		createObject(objects:0x0000053Dr, dollPos, gTrack.startGridData.elementAt(3)); //side table (!!)
//		createObject(objects:0x00000546r, dollPos, gTrack.startGridData.elementAt(3)); //red cone
//		createObject(objects:0x000000D3r, dollPos, gTrack.startGridData.elementAt(3)); //yellow cone
//		createObject(objects:0x00000171r, dollPos, gTrack.startGridData.elementAt(3)); //'no parking' sign
//		createObject(objects:0x00000172r, dollPos, gTrack.startGridData.elementAt(3)); //ad banner
//		createObject(objects:0x00000173r, dollPos, gTrack.startGridData.elementAt(3)); //cylindric ad
//		createObject(objects:0x00000176r, dollPos, gTrack.startGridData.elementAt(3)); //trash can
		doll = getObject(0);

		Style sld1 = new Style( 0.45, 0.06, Frontend.mediumFont, Text.ALIGN_LEFT, new ResourceRef(Osd.RID_SLD_BACK) );
		Style sldk =  new Style( 0.04, 0.05, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB) );
		Style butt0 = new Style( 0.45, 0.12, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST );

		Menu m = osd.createMenu( butt0, -0.400, 0.550, 0 );
		m.setSliderStyle( sld1, sldk );

		dollDesigner = new Slider[4];
		String[] name = new String[3];
		name[0] = "X"; name[1] = "Y"; name[2] = "Z";

		for(int i=0; i<3; i++)
		{
			dollDesigner[i] = m.addItem(name[i], 0, 0, -25.0, 25.0, 0, null);
			dollDesigner[i].setTicks(1000);
		}

		dollDesigner[3] = m.addItem("R", 0, 0, -360.0, 360.0, 0, null);
		dollDesigner[3].setTicks(360);
	}

	void newDoll()
	{
		//for CFG's: x+=1 and y-=1 to each XYZ
		dollPos = doll.getPos();
		for(int i=0; i<3; i++) dollDesigner[i].setValue(0);
		Ypr ori = doll.getOri();

		createObject(objects.elementAt(objects.size()-1).typeID, dollPos, ori); //clone of doll
		doll = getObject(objects.size()-1); //gametype of cloned doll
	}

	void lightTest()
	{
		/*
		//'manual' test, positions work on gateway dragstrip track
		Vector3[] lightPos = new Vector3[14];

		lightPos[0] = new Vector3(-134.8, 4.85, -237.45); //yellow light L1
		lightPos[1] = new Vector3(-134.9, 4.85, -237.45); //yellow light L2
		lightPos[2] = new Vector3(-135.245, 4.85, -237.47); //yellow light R1
		lightPos[3] = new Vector3(-135.34, 4.85, -237.5); //yellow light R2

		lightPos[4] = new Vector3(-134.835, 4.68, -237.413); //orange light L1
		lightPos[5] = new Vector3(-134.835, 4.475, -237.413); //orange light L2
		lightPos[6] = new Vector3(-135.345, 4.68, -237.513); //orange light R1
		lightPos[7] = new Vector3(-135.345, 4.475, -237.513); //orange light R2
		lightPos[8] = new Vector3(-134.825, 4.27, -237.413); //orange light L3
		lightPos[9] = new Vector3(-135.335, 4.27, -237.513); //orange light R3

		lightPos[10] = new Vector3(-134.825, 4.05, -237.5); //green light L
		lightPos[11] = new Vector3(-135.35, 4.05, -237.5); //green light R

		lightPos[12] = new Vector3(-134.815, 3.845, -237.45); //red light L
		lightPos[13] = new Vector3(-135.35, 3.845, -237.5); //red light R

		for(int i=0; i<4; i++)   light[i] = new Light(gTrack.map, cars.racers:0x0000119Fr, 0, lightPos[i], null, "yellow");
		for(int i=4; i<10; i++)  light[i] = new Light(gTrack.map, cars.racers:0x0000119Dr, cars.racers:0x0000119Er, lightPos[i], null, "orange");
		for(int i=10; i<12; i++) light[i] = new Light(gTrack.map, cars.racers:0x0000119Br, cars.racers:0x0000119Cr, lightPos[i], null, "green");
		for(int i=12; i<14; i++) light[i] = new Light(gTrack.map, cars.racers:0x00001199r, cars.racers:0x0000119Ar, lightPos[i], null, "red");
		*/
		
		//'automatic' light generation, track _must_ support traffic lights! (check trackdata of gateway dragstrip to find example)
		gmcEvent.track.setTrafficLight(gTrack.map, test);
		test++;
	}

	//test features of RandomBox.class
	void randomBoxTest()
	{
		int area = 15;
		int dbg = -1; //for tracking box.debug
		System.trace("randomBoxTest(): begin debug for area = " + area);

		for(int i=0; i<area; i++)
		{
			System.trace("adding element #"  + i);
			box.add("test " + i);
		}
		box.close();

		System.trace("adding OK!");

		for(int j=0; j<area*2; j++)
		{
			System.trace("picking random element, attempt #" + j);
			System.trace("contents of random element are: " + box.pick());

			if(box.debug)
			{
				if(box.debug != dbg)
				{
					dbg = box.debug;
					System.trace("WARNING! RandomBox has reported a loop with debug value: " + box.debug);
				}
			}
		}

		System.trace("RandomBox debug session finished! box will now reset");
		System.trace("");
		System.trace("");

		box.reset();
	}

	//test sound/music features for build 900
	void soundTest()
	{
		switch(test)
		{
			case 0:
				debug("total tracks: " + Sound.getTotalTracks());
				break;

			case 1:
				debug("now playing: " + Sound.getNowPlaying());
				break;
			case 2:
				debug("switching to track #2");
				Sound.playTrack(2);
				break;
			case 3:
				debug("switching to track #3");
				Sound.playTrack(3);
				break;
			case 4:
				debug("switching to track #0");
				Sound.playTrack(0);
				break;
			case 5:
				debug("switching to track #1");
				Sound.playTrack(1);
				break;
			case 6:
				debug("utilizing firstTrack()");
				Sound.firstTrack();
				break;
			case 7:
				debug("utilizing randomTrack(), attempt #1");
				Sound.randomTrack();
				break;
			case 8:
				debug("utilizing randomTrack(), attempt #2");
				Sound.randomTrack();
				break;
			case 9:
				debug("utilizing randomTrack(), attempt #3");
				Sound.randomTrack();
				break;
			case 10:
				debug("utilizing stopMusic()");
				Sound.stopMusic();
				break;
			case 11:
				debug("direct play: Music\\Main_Menu\\maintheme final - hotel sinus feat antal csilla.mp3");
				Sound.playTrack("Music\\Main_Menu\\maintheme final - hotel sinus feat antal csilla.mp3");
				break;
		}
		test++;
	}

	//particle system tests
	void PStest()
	{
		ps++;
	}

	//draw 3D graphs from game objects
	void graphTest()
	{
		int depth = -270; //Z depth
		int bounds = 100;
		int delta = 3; //distance between objects

		//XY basis
		for(int i=0; i<bounds/2; i++)
		{
			//build XY basis
			TrafficVehicle tx = createTraffic(cars.traffic.Ambulance:0x00000006r, new Vector3(i*delta,depth,0), null);
			gVhc.addElement(tx);

			TrafficVehicle tx2 = createTraffic(cars.traffic.Ambulance:0x00000006r, new Vector3(i*delta*(-1),depth,0), null);
			gVhc.addElement(tx2);

			TrafficVehicle ty = createTraffic(cars.traffic.Ambulance:0x00000006r, new Vector3(0,depth,i*delta), null);
			gVhc.addElement(ty);

			TrafficVehicle ty2 = createTraffic(cars.traffic.Ambulance:0x00000006r, new Vector3(0,depth,i*delta*(-1)), null);
			gVhc.addElement(ty2);
		}

		//graph, cos(x)
		for(int i=-bounds*2; i<bounds*4; i++)
		{
			TrafficVehicle tg = createTraffic(cars.traffic.Ambulance:0x00000006r, new Vector3(i,depth,Math.cos(Math.deg2rad(i))*delta*50), null);
			gVhc.addElement(tg);
		}

		debug(GameLogic.player.car.getPos().toString());
	}

	//projection of 2D line from 2 points
	void projLine2DTest()
	{
		//f(x): ((x-x0)*((y1-y0)/(x1-x0)))+y0;
		Vector3 from = new Vector3(83.206, GameLogic.player.car.getPos().y, -145.773);
		Vector3 to = new Vector3(-136.262, GameLogic.player.car.getPos().y, -18.587);
		Vector3 pos = from.projLine2D(to, -20);

		tVhc = createTraffic(cars.traffic.Ambulance:0x00000006r, pos, GameLogic.player.car.getOri()); //placing some debug object into the projected point
		System.trace("projLine2D: " + pos.toString());
	}

	//relative pos and render getPos()
	void relativeMatrix()
	{
		roadGuy = createSpectator(humans:0x002fr, humans:0x0014r, gTrack.startGridData.elementAt(2), gTrack.startGridData.elementAt(3), "spectator");
		phy = new PhysicsRef();
		phy.createBox(gTrack.map, 10,10,10, "phys");
		phy.setMatrix(roadGuy.pos, null);
		System.trace("testRef V3 before: " + roadGuy.render.getPos().toString());
		Pori p = new Pori(new Vector3(roadGuy.pos.x+5, roadGuy.pos.y, roadGuy.pos.z+4), null);
		roadGuy.render.setMatrix(roadGuy.render.getBoneId("bone00"), phy, new Vector3(0,3,3), null);
		System.trace("testRef V3 after: " + roadGuy.render.getPos().toString());
	}

	void callbackTest()
	{
		callbackTimer = new Timer(3, this);
		callbackTimer.start();
		debug("callback timer launched");
	}

	//this is called by other classes
	public void callback(Object message)
	{
		String m = "";
		if(message instanceof String) m=message;
		System.stop("callback(Object) worked! " + m);
	}

	public void callback()
	{
		System.stop("callback() worked!");
	}
}