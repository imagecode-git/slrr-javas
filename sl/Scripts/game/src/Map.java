package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class Map extends GameType
{
	Thread		mThread;
	Track		track;
	GameRef		cursor;
	Text		frameText, infoText;

	RenderRef	line;// = new RenderRef();

	Text		debugText;
	int		debugMode;
	int		xlimit;
	int		maxzoom;
	int		planRoute;
	int		teleport;
	int		tcshow;
	int		osdStatus; //RAXAT: status of track OSD
	String		cMode;

	Osd		mosd;

	GameRef		localroot;
	GameRef		click;

	Vector3		pStart, pFinish;
	Marker		mStart, mFinish, mTeleport;
	Menu		m, m2;

	final static int CMD_EXIT		= 1;
	final static int CMD_ZOOM_IN		= 2;
	final static int CMD_ZOOM_OUT		= 3;
	final static int CMD_MODE_TELEPORT	= 4;
	final static int CMD_CMODE		= 5;
	final static int CMD_CSHOW		= 6;
	final static int CMD_PLAN_ROUTE		= 7;

	public Map(Track trk)
	{
		debugMode = 0;

		if(debugMode)
		{
			teleport = 1;
			tcshow = 1;
		}

		if(Integrator.isCity) cMode = "GNC positioning";
		else cMode = "Direct positioning";

		track = trk;
	}

	public void init()
	{
		createNativeInstance();

		osdStatus = track.osdEnabled ? 1 : 0; //RAXAT: also, check if JVM supports this: osdStatus == (track.osdEnabled = 1) ? 1 : 0
		GameLogic.player.hideOsd(); //RAXAT: but not navigator!

		if(!osdStatus) track.nav.show();

		track.nav.offsetX=track.nav.offsetZ=0.0;
		track.nav.changeSize( 0.0, 0.12, 1.0, 0.78 );
		track.nav.changeMode( 0 );

		if(!Integrator.isCity) track.nav.changeZoom(6.0);
		else track.nav.changeZoom(14.0);

		track.nav.updateNavigator( GameLogic.player.car );

		mThread = new Thread( this, "Map watcher" );
		mThread.start();

		Input.cursor.enable(1);

		mosd = new Osd( 1.0, 0.0, 15 );
		mosd.iLevel=Osd.IL_KEYS;
		mosd.globalHandler = this; //RAXAT: AND ALL GLOBAL KEYS MUST BE ASSIGNED TO LOCAL OSD TO WORK
		mosd.defSelection=1;
		mosd.show();

		debugText=mosd.createText( null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.5 );

		mosd.createStrongHeader("GPS MANAGER");
		mosd.createFooter("");

		infoText=mosd.createText( null, Frontend.mediumFont, Text.ALIGN_LEFT,	-0.95, -0.875);

		Style buttonStyle = new Style( 0.12, 0.12, Frontend.mediumFont, Text.ALIGN_RIGHT, null );
		Style buttonStyle2 = new Style( 0.12, 0.12, Frontend.mediumFont, Text.ALIGN_LEFT, null );

		m = mosd.createMenu( buttonStyle, 0.94, 0.86, 0, Osd.MD_HORIZONTAL );
		m2 = mosd.createMenu( buttonStyle2, -0.94, 0.86, 0, Osd.MD_HORIZONTAL );

		m2.addItem( new ResourceRef( Osd.RID_CANCEL ), CMD_EXIT, "CLOSE MAP VIEWER" );

		if( Integrator.isCity )
			m2.addItem( new ResourceRef( frontend:0xF0FAr ), CMD_PLAN_ROUTE, "PLAN ROUTE" );

		if( debugMode )
			m2.addItem( new ResourceRef( frontend:0xF0F5r ), CMD_MODE_TELEPORT, "TELEPORTER" );

		m.addItem( new ResourceRef( frontend:0x006Ar ), CMD_ZOOM_OUT, "ZOOM OUT" );
		m.addItem( new ResourceRef( frontend:0x0061r ), CMD_ZOOM_IN, "ZOOM IN" );

		if( teleport )
		{
			m.addSeparator();
			m.addItem( new ResourceRef( frontend:0xF0F5r ), CMD_MODE_TELEPORT, "TELEPORT!" );
			m.addItem( new ResourceRef( frontend:0xF0F7r ), CMD_CMODE, "CMODE" );
			m.addItem( new ResourceRef( frontend:0xF0F6r ), CMD_CSHOW, "SHOW COORDINATES" );
		}

		pStart = track.map.getNearestCross( track.player.car.getPos() );

		click = new GameRef( track.nav.localroot, frontend:0x0065r, "0,0,0 0,0,0", "navigator click-object" );
		addNotification( click, GameType.EVENT_CURSOR, GameType.EVENT_SAME, null, "event_handlerClick" );
		setEventMask( EVENT_CURSOR );

		if( teleport ) infoText.changeText( "Debug teleporter tool is active. Browsing area: " + track.name + ". " + cMode );
	}

	public void run()
	{
		for(;;)
		{
			if( track.nav )
			{

				if( track.nav.zoom == 6 )
					xlimit = 830;
				else
				if( track.nav.zoom == 8 )
					xlimit = 720;
				else
				if( track.nav.zoom == 10 )
					xlimit = 600;
				else
				if( track.nav.zoom == 12 )
					xlimit = 470;
				else
				if( track.nav.zoom == 14 )
					xlimit = 350;
				else
				if( track.nav.zoom == 16 )
					xlimit = 210;
				else
				{
					if( !Integrator.isCity )
					{
						xlimit = 0;
					}

					if( Integrator.isCity )
					{
						xlimit = 2000;
					}
				}

				if( Integrator.isCity )
					maxzoom = 31;

				if( !Integrator.isCity )
				{
					if( track )
					{
						if( track instanceof TestTrack )
							maxzoom = 12;
						else
							maxzoom = 17;
					}
				}

//				if( debugMode )
//					debugText.changeText( track.nav.offsetX + " and zoom: " + track.nav.zoom );

				float	area = 0.1;
				float	min =-0.9;
				float	max = 0.9;
				float	step=10.0;

				Vector3 v = Input.cursor.getPos();
				if( v.x <= min )
				{
					track.nav.offsetX += step*(v.x - min)/area;
					track.nav.updateNavigator( track.player.car );
				}
				else
				if( v.x >= max )
				{
					if( track.nav.offsetX < xlimit )
					{
						track.nav.offsetX += step*(v.x - max)/area;
						track.nav.updateNavigator( track.player.car );
					}
					else
					if( !xlimit ) //RAXAT: CITY..
					{
						track.nav.offsetX += step*(v.x - max)/area;
						track.nav.updateNavigator( track.player.car );
					}
				}

				if( v.y <= min )
				{
					track.nav.offsetZ += step*(v.y - min)/area;
					track.nav.updateNavigator( track.player.car );
				}
				else
				if( v.y >= max )
				{
					track.nav.offsetZ += step*(v.y - max)/area;
					track.nav.updateNavigator( track.player.car );
				}

				if( track.nav.offsetX > xlimit )
					track.nav.offsetX = xlimit;
			}
			mThread.sleep(20);
		}
	}

	public void osdCommand( int cmd )
	{
		if( cmd==CMD_EXIT )
		{
			finalize();
		}
		else
		if( cmd==CMD_ZOOM_IN )
		{
			if( track.nav.zoom > 5 )
			{
				track.nav.changeZoom( track.nav.zoom-2 );
				track.nav.updateNavigator( track.player.car );
			}
		}
		else
		if( cmd==CMD_ZOOM_OUT )
		{
			if( track.nav.zoom < maxzoom )
			{
				track.nav.changeZoom( track.nav.zoom+2 );
				track.nav.updateNavigator( track.player.car );
			}
		}
		else
		if( cmd==CMD_MODE_TELEPORT )
		{
			if( pStart == pFinish )
				track.player.car.setPos( pFinish );

			if( mTeleport )
				track.nav.remMarker( mTeleport );

			new SfxRef( GameLogic.SFX_CHEAT_MONEY ).play();

			track.nav.updateNavigator( track.player.car );
			track.nav.offsetX = xlimit;
			track.teleported = 1;
		}
		else
		if( cmd==CMD_CMODE )
		{
			if( cMode == "GNC positioning" )
				cMode = "Direct positioning";
			else
				cMode = "GNC positioning";

			infoText.changeText( "Debug teleporter tool is active. Browsing area: " + track.name + ". " + cMode );
		}
		else
		if( cmd==CMD_CSHOW )
		{
			if(!tcshow) 
				tcshow = 1;
			else
			{
				tcshow = 0;
				debugText.changeText( "" );
			}
		}
		if( cmd==CMD_PLAN_ROUTE )
		{
			if( !planRoute )
			{
				planRoute = 1;
				infoText.changeText( "Planning new route - destination point not defined" );
			}
			else
			{
				planRoute = 0;
				infoText.changeText( "" );

				if (track.nav.route)
					track.nav.route.destroy();

				if (track.mRouteS)
					track.nav.remMarker( track.mRouteS );

				if (track.mRouteF)
					track.nav.remMarker( track.mRouteF );

				if(track.routeDestTriggerAdded)
				{
					track.routeTrigger.finalize();
					track.routeDestTriggerAdded = 0;
				}

				if( mStart )
					track.nav.remMarker( mStart );

				if( mFinish )
					track.nav.remMarker( mFinish );
			}
		}
	}

	public void finalize()
	{
		mThread.stop();

		if(mosd) mosd.finalize();
		if(Input.cursor) Input.cursor.enable(0);

		click.destroy();

		if(frameText) frameText.finalize();
		if(infoText) infoText.finalize();

		if(track.nav)
		{
			if(mStart)
			{
				if(planRoute && pStart) track.mRouteS = track.nav.addMarker(Marker.RR_TARGET_BLACK, pStart, 3);
				track.nav.remMarker(mStart);
			}

			if(mFinish)
			{
				if(planRoute && pFinish) track.mRouteF = track.nav.addMarker(Marker.RR_TARGET, pFinish, 3);
				track.nav.remMarker( mFinish );
			}

			if(mTeleport) track.nav.remMarker(mTeleport);

			track.nav.changeSize( 0.02, 0.78, 0.2, 0.18 );
			track.nav.changeMode( Config.gpsMode );
			track.nav.offsetX=track.nav.offsetZ=0.0;
			track.nav.changeZoom( Navigator.DEF_ZOOM );
			track.nav.updateNavigator( GameLogic.player.car );
		}

		if(track.osd)
		{
			//RAXAT: manual methods to keep track.osdEnabled without changes
			if(osdStatus)
			{
				GameLogic.player.showOsd();
				track.nav.show();
			}
			else track.nav.hide();

			track.osdCommand(Track.CMD_GPSMAP);
		}
	}

	public void event_handlerClick( GameRef obj_ref, int event, String param )
	{
		int	ec = param.token( 0 ).intValue();

		if (ec == GameType.EC_LCLICK)
		{
			Vector3 v = Input.cursor.getPickedPos();
			v.mul( 100 );

			Vector3 temp = track.map.getNearestCross( v );

			if( debugMode )
			{
				if( temp )
				{
					if( tcshow )
						debugText.changeText( "actual " + v.toString() + " ;GNC: " + temp.toString() );
				}

				if( !temp )
				{
					if( tcshow )
						debugText.changeText( v.toString() );
				}
			}

			if( teleport && !planRoute )
			{
				if( mStart )
					track.nav.remMarker( mStart );

				if( mTeleport )
					track.nav.remMarker( mTeleport );

				if( cMode == "GNC positioning" )
				{
					if( temp )
						pFinish = temp;

					if( !temp )
						pFinish = v;
				}

				if( cMode == "Direct positioning" )
				{
					pFinish = v;
				}

				mTeleport = track.nav.addMarker( Marker.RR_TELEPORT, pFinish, 3 );
				pStart = pFinish;

				new SfxRef( Frontend.SFX_MENU_SELECT ).play();
			}

			if( planRoute && !teleport )
			{
				if( track.mRouteS )
					track.nav.remMarker( track.mRouteS );

				if( track.mRouteF )
					track.nav.remMarker( track.mRouteF );

				pStart = GameLogic.player.car.getPos();

				if( mStart )
					track.nav.remMarker( mStart );

				mStart = track.nav.addMarker( Marker.RR_TARGET_BLACK, pStart, 3 );
				pFinish = temp;

				float distance = track.map.getRouteLength( pStart, pFinish );

				RenderRef ltype = new RenderRef(particles:0x00000017r);
				ltype.cache();
				if (line)
					line.destroy();
				line = track.nav.route;
				line = new RenderRef();
				line.plotRoute( track.nav.localroot, ltype, 0xFFFF0000, 10.0, new Vector3(0.01,0,0.01) );
				track.nav.route = line;

				if( mFinish )
					track.nav.remMarker( mFinish );

				if(track.routeDestTriggerAdded)
				{
					track.routeTrigger.finalize();
					track.routeDestTriggerAdded = 0;
				}

				mFinish = track.nav.addMarker( Marker.RR_TARGET, pFinish, 3 );

				track.routeDestPos = pFinish;

				infoText.changeText( "Planning new route - total length: " + track.map.getRouteLength( pStart, pFinish ) + "m" );
				new SfxRef( Frontend.SFX_MENU_SELECT ).play();
			}

			track.nav.updateNavigator( track.player.car );
		}
	}
}
