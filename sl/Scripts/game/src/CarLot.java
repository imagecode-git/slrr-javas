package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class CarLot extends GameType implements GameState
{
	//resource ID constants
	final static int  	RID_MAP_CARLOT = misc.carlot:0x00000001r;

	final static float	FLOOR_HEIGHT =3.3;

	final static int 	PARKS = 10;
	final static int 	FLOORS =10;

	GameState		parentState;
	Osd			osd;
	int			loadingGroup;

	//owner
	Player			player;

	//lot
	GameRef			map;
	CarLotData		data = new CarLotData();
	RenderRef		sun, suntype;

	Vehicle[]		cars = new Vehicle[PARKS*FLOORS];	//tobbdimenzios tombok meg nincsenek!
	int[]			carSlots = new int[PARKS*FLOORS];


	int			curcar, old_curcar;
	int			cancelled;
	int			cacheSection, initSection;
	Thread			loadTextThread;
	
	Text			carName;

	GameRef			cam;

	public CarLot( Player owner )
	{
		createNativeInstance();

		player = owner;
		map = new GroundRef( RID_MAP_CARLOT );
	}

	public Vector3 getCarPos( int n )
	{
		Vector3 pos = new Vector3( data.kocsidummyPos[n%PARKS] );
		pos.y+=0.0+FLOOR_HEIGHT*(n/PARKS);
		return pos;
	}

	public Vector3 getCameraPos( int n )
	{
		//Vector3 pos = new Vector3( data.CameraPos[n%PARKS] );
		Vector3 pos = new Vector3( n%PARKS*2.75-12.25, 1.3, 8.0 );
		pos.y+=FLOOR_HEIGHT*(n/PARKS);
		return pos;
	}
	public Vector3 getCameraOri( int n )
	{
		//return data.CameraOri[n%PARKS];
		return new Vector3( 0.0, -0.2, 0.0 );
	}


	public void moveCamera()
	{

		if( carSlots[curcar] )
		{
			if( !cars[curcar] )
			{
				loadCar( curcar );
			}
		}

		if( !cacheSection )
		{
			loadVisibleCars();

			cam.queueEvent( null, GameType.EVENT_COMMAND, "move " + map.id() + " " + getCameraPos(curcar).toString() +" 0.01");
			////cam.queueEvent( null, GameType.EVENT_COMMAND, "look " + map.id() + " " + getCarPos(curcar).toString());

			if( carSlots[curcar] )
				carName.changeText( cars[curcar].toString() );
			else
				carName.changeText( null );
		}
	}


	public void enter( GameState prevState )
	{
		GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x00000125r)); //RAXAT: v2.3.1, envmap patch

		enterAsyncMode();
		t.setPriority( Thread.MAX_PRIORITY );

		GameLogic.autoSave();

		parentState=prevState;

		Frontend.loadingScreen.show();

		cacheSection=0;

		//lighting
		suntype = new RenderRef();
		suntype.duplicate( new RenderRef(misc.carlot:0x0025r) );
		sun = new RenderRef( map, suntype, "carlot light" );

		Style butt2 = new Style( 0.45, 0.12, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST );

		osd = new Osd();
		osd.globalHandler = this;

		osd.createStrongHeader( "CAR LOT" );
		carName=osd.createText( null, Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, -0.98 );

		//osd.createRectangle( 0.94, -0.82, 1.2, 0.22, -1, new ResourceRef(frontend:0x0024r) ); //RAXAT: icon background stripe (long solid header is used instead in v2.3.1)

		Style buttonStyle = new Style( 0.1, 0.1, Frontend.mediumFont, Text.ALIGN_RIGHT, null );
		Menu m = osd.createMenu( buttonStyle, 0.98, -0.93, 0, Osd.MD_HORIZONTAL );

		m.addItem( new ResourceRef( Osd.RID_CANCEL ), Input.AXIS_CANCEL, null, null );
		m.addSeparator();
		m.addSeparator();
		m.addItem( new ResourceRef( Osd.RID_OK ), Input.AXIS_SELECT, null, null, 1 );
		m.addItem( new ResourceRef( Osd.RID_ARROWRG ), Input.AXIS_MENU_RIGHT, null, null, 1 );
		m.addItem( new ResourceRef( Osd.RID_ARROWLF ), Input.AXIS_MENU_LEFT, null, null, 1 );
		m.addItem( new ResourceRef( Osd.RID_ARROWDN ), Input.AXIS_MENU_DOWN, null, null, 1 );
		m.addItem( new ResourceRef( Osd.RID_ARROWUP ), Input.AXIS_MENU_UP, null, null, 1 );

		osd.createHotkey( Input.AXIS_MENU_LEFT, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_MENU_LEFT, this );
		osd.createHotkey( Input.AXIS_MENU_RIGHT, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_MENU_RIGHT, this );
		osd.createHotkey( Input.AXIS_MENU_UP, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_MENU_UP, this );
		osd.createHotkey( Input.AXIS_MENU_DOWN, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_MENU_DOWN, this );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_CANCEL, this );
		//osd.createHotkey( Input.AXIS_SELECT, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_SELECT, this );

		osd.endGroup();
		osd.createText( "LOADING...",	Frontend.mediumFont, Text.ALIGN_LEFT, -0.95, 0.90 );
		osd.hideGroup( loadingGroup = osd.endGroup() );

		lockPlayerCar();

		cam= new GameRef( map, GameRef.RID_CAMERA, getCameraPos(curcar).toString() +","+ getCameraOri(curcar).toString()+", 0x01,1.5,0.0", "s_cam" );
		cam.command( "zoom 60 5");
		cam.queueEvent( null, GameType.EVENT_COMMAND, "render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET) );
		//cam.queueEvent( null, GameType.EVENT_COMMAND, "look "+ cars[curcar].id() +" 0.0, 0.0, 0.0");
		//cam.queueEvent( null, GameType.EVENT_COMMAND, "look "+ map.id() +" 0.0, 0.0, -1000.0");

		//Input.cursor.cursor.queueEvent(null, GameType.EVENT_COMMAND, "move 0.0,0.0" );
		Input.cursor.enable(1);
		osd.show();
		setEventMask( EVENT_CURSOR );

		initSection=1;
		moveCamera();//loadVisibleCars();
		initSection=0;

		Frontend.loadingScreen.display();
	}

	public void exit( GameState nextState )
	{
		clearEventMask( EVENT_ANY );
		Input.cursor.enable(0);

		cam.destroy();

		//lighting
		sun.destroy();
		suntype.destroy();
		//

		releasePlayerCar();

		flushCars();

		if( loadTextThread )
			loadTextThread.stop();

 		osd.hide();
		osd=null;

		cancelled=0;	

		parentState=null;
		leaveAsyncMode();
	}

	public void lockPlayerCar()
	{
		if( player.car )
		{
			addCar( player.car );
			player.car=null;
		}

		//ide kell: ha pl a carmarket hiv lock/release
		cancelled=0;
		old_curcar=curcar;
	}

	public void releasePlayerCar()
	{
		if( cancelled )
		{
			curcar=old_curcar;
		}

		if( carSlots[curcar] )
		{
			if( !cars[curcar] )	//nincs meg kesz vele az async tolto?
			{	//nem kellene leallitani most rogton?

				loadCar( curcar );
			}

			cars[curcar].queueEvent( null, GameType.EVENT_COMMAND, "start" );	//release
			player.car = cars[curcar];
			player.car.setParent( player );

			cars[curcar] = null;
			carSlots[curcar]=0;
		}

		//ha uj kocsit/ures helyet valaszt, akkor ha hozott kocsit a garazsbol, azt ki kell menteni!!
		if( curcar!=old_curcar )
		{
			//save old_curcar
			saveCar(old_curcar);
		}

	}

	//writes out car to disk (call this before saving)
	public void saveCar( int slot )
	{
		if(carSlots[slot] && cars[slot])
		{
			File.delete( GameLogic.tempSaveDir + "PlayerCar" + slot );
			File.delete( GameLogic.tempSaveDir, "PlayerCar" + slot + ".*" );
			cars[slot].save( GameLogic.tempSaveDir + "PlayerCar" + slot );
		}
	}

	public int isEmpty()
	{
		for( int i=0; i<carSlots.length; i++ )
		{
			if( carSlots[i] )
				return 0;
		}
		return 1;
	}

	public int getFreeSlot()
	{
		for( int i=0; i<carSlots.length; i++ )
		{
			if( !carSlots[i] )
				return i;
		}

		//System.log( "No more free carlot slots!" );
		return -1;
	}
	
	public void loadCar( int slot )
	{
		if( carSlots[slot] && !cars[slot] && File.exists(GameLogic.tempSaveDir + "PlayerCar" + slot) )
		{
			addCar( Vehicle.load( GameLogic.tempSaveDir + "PlayerCar" + slot, map ), slot );
		}
	}

	public void loadVisibleCars()
	{
		cacheSection = 1;

		if( !initSection )
		{
			loadTextThread = new LoadingText( osd, loadingGroup );
			loadTextThread.start();
			loadTextThread.setPriority( Thread.MAX_PRIORITY );
		}

		if( goLeft() )
			goRight();
		if( goRight() )
			goLeft();

		if( !initSection )
			loadTextThread.stop();

		cacheSection = 0;
	}

	//changes curcar!
	public void addCar( Vehicle car )
	{
		if(curcar >= 0 && curcar < carSlots.length) //RAXAT: build 934, negative array index patch
		{
			if( carSlots[curcar] )	//ha foglalt a hely (pl vasarlasok miatt) keresunk egy ureset!
				curcar = getFreeSlot();

			addCar( car, curcar );
		}
	}


	public void addCar( Vehicle car, int slot )
	{
		if( car )
		{
			if(slot >= 0 && slot < carSlots.length) //RAXAT: build 934, negative array index patch
			{
				if( !cars[slot] )
				{
					cars[slot] = car;
					carSlots[slot]=1;

					cars[slot].setParent( map );
					cars[slot].setMatrix( getCarPos(slot), new Ypr( 3.14, 0, 0) );
					cars[slot].command( "reset" );
					cars[slot].command( "stop" );	//grab
				}
			}
		}
	}

	public void flushCars()
	{
		for( int i=0; i<cars.length; i++ )
		{
			if( cars[i] )
			{
				cars[i].destroy();
				cars[i]=null;		//eleg lenne ez is...
			}
		}
	}

	public void scanCars()
	{
		curcar=0;
		old_curcar=0;

		for( int i=0; i<carSlots.length; i++ )
		{
			if( File.exists( GameLogic.tempSaveDir + "PlayerCar" + i) )
				carSlots[i]=1;
			else
				carSlots[i]=0;
		}
	}

//----------------------------------------------------------------------
	public void osdCommand( int cmd )
	{
		mp.putMessage( new Event( cmd ) );
	}

	public int goLeft()
	{
		if( curcar%PARKS != 0 )
		{
			curcar--;
			moveCamera();
			return 1;
		}
		return 0;
	}
	public int goRight()
	{
		if( curcar%PARKS != PARKS-1 )
		{
			curcar++;
			moveCamera();
			return 1;
		}
		return 0;
	}
	public int goUp()
	{
		if( curcar/PARKS != FLOORS-1 )
		{
			curcar+=PARKS;
			moveCamera();
			return 1;
		}
		return 0;
	}
	public int goDown()
	{
		if( curcar/PARKS != 0 )
		{
			curcar-=PARKS;
			moveCamera();
			return 1;
		}
		return 0;
	}

//----------------------------------------------------------------------
	public void event_handlerMoveUp( GameRef obj_ref, int event, String param )
	{
		if (param.token(0).intValue() == GameType.EC_LCLICK)
			osdCommand( Input.AXIS_MENU_UP );
	}
	public void event_handlerMoveDown( GameRef obj_ref, int event, String param )
	{
		if (param.token(0).intValue() == GameType.EC_LCLICK)
			osdCommand( Input.AXIS_MENU_DOWN );
	}
	public void event_handlerMoveLeft( GameRef obj_ref, int event, String param )
	{
		if (param.token(0).intValue() == GameType.EC_LCLICK)
			osdCommand( Input.AXIS_MENU_LEFT );
	}
	public void event_handlerMoveRight( GameRef obj_ref, int event, String param )
	{
		if (param.token(0).intValue() == GameType.EC_LCLICK)
			osdCommand( Input.AXIS_MENU_RIGHT );
	}

	public void handleMessage( Message m )
	{
		if( m.type == Message.MT_EVENT )
		{
			Event oe = m;

			if( oe.cmd == Input.AXIS_MENU_LEFT )
			{
				goLeft();
			}
			else
			if( oe.cmd == Input.AXIS_MENU_RIGHT )
			{
				goRight();
			}
			else
			if( oe.cmd == Input.AXIS_MENU_UP )
			{
				goUp();
			}
			else
			if( oe.cmd == Input.AXIS_MENU_DOWN )
			{
				goDown();
			}
			else
			if( oe.cmd == Input.AXIS_CANCEL )
			{
				cancelled=1;
				GameLogic.changeActiveSection( parentState );
			}
			else
			if( oe.cmd == Input.AXIS_SELECT )
			{
				cancelled=0;
				GameLogic.changeActiveSection( parentState );
			}
		}
	}
}

//----------------------------------------------------------------------------------------
public class LoadingText extends Thread
{
	Osd osd;
	int	group;

	public LoadingText( Osd target, int group_ )
	{
		osd = target;
		group=group_;
	}

	public void stop()
	{
		super.stop();

		osd.hideGroup( group );
//		GfxEngine.forceRendering();
		//System.setLdPriority( System.LD_NORM );
	}

	public void run()
	{
		//System.setLdPriority( System.LD_HIGH );
//		while(1)
		{
			osd.showGroup( group );
//			GfxEngine.forceRendering();
			sleep( 100 );

			osd.hideGroup( group );
//			GfxEngine.forceRendering();
			sleep( 300 );
		}
	}
}

//----------------------------------------------------------------------------------------------------

public class CarLotData
{
	final Vector3[] CameraPos = new Vector3[10];
	final Ypr[] CameraOri = new Ypr[10];
	final Vector3[] kocsidummyPos = new Vector3[10];
	final Ypr[] kocsidummyOri = new Ypr[10];

	public CarLotData()
	{
		CameraPos[0] = new Vector3( -10.975, 1.900, 7.905 );
		CameraOri[0] = new Ypr( 0.284, -0.253, 0.000 );
		CameraPos[1] = new Vector3( -8.207, 1.900, 7.905 );
		CameraOri[1] = new Ypr( 0.284, -0.253, 0.000 );
		CameraPos[2] = new Vector3( -5.486, 1.900, 7.905 );
		CameraOri[2] = new Ypr( 0.284, -0.253, 0.000 );
		CameraPos[3] = new Vector3( -2.767, 1.900, 7.905 );
		CameraOri[3] = new Ypr( 0.284, -0.253, 0.000 );
		CameraPos[4] = new Vector3( -0.047, 1.900, 7.905 );
		CameraOri[4] = new Ypr( 0.284, -0.253, 0.000 );
		CameraPos[5] = new Vector3( 0.020, 1.900, 7.905 );
		CameraOri[5] = new Ypr( -0.278, -0.253, 0.000 );
		CameraPos[6] = new Vector3( 2.751, 1.900, 7.905 );
		CameraOri[6] = new Ypr( -0.278, -0.253, 0.000 );
		CameraPos[7] = new Vector3( 5.470, 1.900, 7.905 );
		CameraOri[7] = new Ypr( -0.278, -0.253, 0.000 );
		CameraPos[8] = new Vector3( 8.193, 1.900, 7.905 );
		CameraOri[8] = new Ypr( -0.278, -0.253, 0.000 );
		CameraPos[9] = new Vector3( 10.911, 1.900, 7.905 );
		CameraOri[9] = new Ypr( -0.278, -0.253, 0.000 );
		kocsidummyPos[0] = new Vector3( -12.318, 0.000, 3.305 );
		kocsidummyOri[0] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[1] = new Vector3( -9.549, 0.000, 3.305 );
		kocsidummyOri[1] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[2] = new Vector3( -6.829, 0.000, 3.305 );
		kocsidummyOri[2] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[3] = new Vector3( -4.110, 0.000, 3.305 );
		kocsidummyOri[3] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[4] = new Vector3( -1.389, 0.000, 3.305 );
		kocsidummyOri[4] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[5] = new Vector3( 1.332, 0.000, 3.305 );
		kocsidummyOri[5] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[6] = new Vector3( 4.063, 0.000, 3.305 );
		kocsidummyOri[6] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[7] = new Vector3( 6.782, 0.000, 3.305 );
		kocsidummyOri[7] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[8] = new Vector3( 9.505, 0.000, 3.305 );
		kocsidummyOri[8] = new Ypr( 0.000, -3.142, -1.571 );
		kocsidummyPos[9] = new Vector3( 12.223, 0.000, 3.305 );
		kocsidummyOri[9] = new Ypr( 0.000, -3.142, -1.571 );
	}
}
