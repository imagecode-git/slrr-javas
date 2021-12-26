package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.sound.*;

//RAXAT: v2.3.1, route-line based GPS 4.0
public class Navigator implements Runnable
{
	//RAXAT: v2.3.1, generated route lines
	SkidMark routeSkid;
	RenderRef routeLine;
	Timer routeTimer;
	Thread routeWatcher;
	int routeUpdateTime = 13; //20 will force line to disappear for some time at low FPS
	int routeRegen = 0;
	float routerAOV = 130.0; //kind of zoom

	//RAXAT: only for "still" minimaps
	float adjustA;
	float adjustB;
	float rotation;

	int useRouteMarkers = 0;
	Marker[] routeMarker; //start/finish for router

	int useMinimapMarkers = 1;
	Marker[] minimapMarker; //finish or any other marker for minimap

	final static int lda;

	final static float DEF_ZOOM = 4.5;	//tavolsag a terkep plane-tol

	final static int TYPE_CLASSIC		= 0x001;
	final static int TYPE_ROUTER		= 0x002;
	final static int TYPE_MINIMAP		= 0x003;
	final static int TYPE_MINIMAP_STILL	= 0x004;

	int defType = TYPE_CLASSIC;


	Viewport	vp;
	Dummy		localroot;
	Camera		cam;
	RenderRef[]	tiles;		//ground tile, instances
	RenderRef[]	tiletypes;	//ground tiles, types
	RenderRef	route;		//instance
	ResourceRef[]	meshes;		//duplicated meshes and types
	GameRef		button;


	Vector		marker = new Vector();	//markers for static objects: known shops,garages, etc.
	Vector		dynamarker = new Vector();	//markers for dynamic object: player/opponent car, etc..

	float		vp_l=0.03, vp_t=0.78, vp_w=0.2, vp_h=0.18;
	float		zoom=DEF_ZOOM;

	//RAXAT: used by cam handler in EXE
	float		offsetX, offsetZ;
	float		top, left, size;

	int		mode;
	int		type;

	Vector3[]	routeData;

	//a render resourceid-k sorfolytonosan legyenek letarolva!
	public Navigator( float left, float top, float size, int ridtype, int ridmsh, int ridtex, int x, int z, int modulo )
	{
		type = defType;

		localroot = new Dummy( GameRef.WORLDTREEROOT );

		RenderRef	base = new RenderRef( ridtype );
		ResourceRef	baseTex = new ResourceRef(ridtex);//def texture
		ResourceRef	baseMsh = new ResourceRef(ridmsh);//def mesh

		int	numTiles=x*z;

		tiles=new RenderRef[numTiles];
		tiletypes=new RenderRef[numTiles];

		meshes=new ResourceRef[numTiles];

		this.top=top;
		this.left=left;
		this.size=size;

		int	xi, zi;
		float offset_x=left, offset_z=top;

		for( int i=0; i<numTiles; i++ )
		{
			tiletypes[i] = new RenderRef();
			tiletypes[i].duplicate( base );
			tiletypes[i].changeResource( baseTex, new ResourceRef( ridtex ) );

			meshes[i] = new ResourceRef();
			meshes[i].duplicate( baseMsh );

			tiletypes[i].changeResource( baseMsh, meshes[i] );

			tiles[i]= new RenderRef( localroot, tiletypes[i], "navigator_segment");
			tiles[i].setMatrix( new Vector3(offset_x+xi*size, -0.001, offset_z+zi*size), null );
			ridtex++;

			if( ++xi >= x )
			{ 
				xi=0; zi++; 
				ridtex+=modulo;
			}
		}
	}

	//RAXAT: v2.3.1, GPS V4
	public Navigator(float zoom,  Vector3[] data)
	{
		type = TYPE_MINIMAP;
		this.zoom=zoom;

		setupV4(data);

	}

	public Navigator(float left, float top, float zoom,  Vector3[] data)
	{
		type = TYPE_MINIMAP;
		this.left = left;
		this.top = top;
		this.zoom=zoom;

		setupV4(data);

	}

	//RAXAT: V4 for "still" minimaps
	public Navigator(float zoom, float adjA, float adjB, float rotation,  Vector3[] data)
	{
		type = TYPE_MINIMAP_STILL;
		this.zoom=zoom;
		this.adjustA = adjA;
		this.adjustB = adjB;
		this.rotation = rotation;

		setupV4(data);

	}

	//RAXAT: V4 for routeline-based navigation
	public Navigator(Vector3[] data)
	{
		type = TYPE_ROUTER;
		setupV4(data);
	}

	public void setupV4(Vector3[] d)
	{
		localroot = new Dummy( GameRef.WORLDTREEROOT );

		left = -17.1137;
		top = -26.6;
		size = 4.9404;

		vp_l=0.01;
		vp_t=0.625;
		vp_w=0.2;
		vp_h=0.36;

		routeData = new Vector3[d.length];
		for(int i=0; i<d.length; i++) routeData[i] = new Vector3(d[i].x, d[i].y, d[i].z);

		if(type == TYPE_MINIMAP || type == TYPE_MINIMAP_STILL)
		{
			if(useMinimapMarkers)
			{
				minimapMarker = new Marker[1];
				minimapMarker[0] = addMarker(Marker.RR_V4_FINISH, d[0], 0);
			}
		}

		if(type == TYPE_ROUTER)
		{
			if(useRouteMarkers)
			{
				routeMarker = new Marker[2];
				routeMarker[0] = addMarker(Marker.RR_V4_START, d[0], 0);
				routeMarker[1] = addMarker(Marker.RR_V4_FINISH, d[d.length-1], 0);
			}
		}
	}

	public void finalize()
	{
		if(type = TYPE_CLASSIC)
		{
			if(tiletypes)
			{
				for( int i=0; i<tiletypes.length; i++ )
				{
					tiles[i].destroy();
					meshes[i].destroy();
					tiletypes[i].destroy();
				}
			}
		}

		if(type == TYPE_ROUTER || type == TYPE_MINIMAP || type == TYPE_MINIMAP_STILL)
		{
			if(routeWatcher)
			{
				routeWatcher.stop();
				routeWatcher = null;
			}

			if(routeTimer)
			{
				routeTimer.stop();
				routeTimer = null;
			}

			if(routeMarker && routeMarker.length)
			{
				if(useRouteMarkers)
				{
					for(int i=0; i<routeMarker.length; i++) remMarker(routeMarker[i]);
				}
			}

			if(routeMarker && minimapMarker.length)
			{
				if(useMinimapMarkers)
				{
					for(int i=0; i<minimapMarker.length; i++) remMarker(minimapMarker[i]);
				}
			}

			if(routeSkid) routeSkid.finalize();
			routeLine.finalize();
		}

		if(type>defType) type = defType;
	}

	public void setInteractive()
	{
	}

	public void show()
	{
		if(!routeRegen)
		{
			if(!vp) vp = new Viewport( 12, vp_l, vp_t, vp_w, vp_h );

			//oc=0, planetest=1
			float aov = 90.0;
			if(type == TYPE_ROUTER) aov = routerAOV;
			cam = new Camera( localroot, vp, 1, aov, 1.0, 100.0, 0.2, 2.0, 0, 1 );

			//updateNavigator() will set the camera's matrix, except its height
			changeMode( Config.gpsMode );

			vp.activate( Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET );

			if(type == TYPE_ROUTER || type == TYPE_MINIMAP || type == TYPE_MINIMAP_STILL)
			{
				if(type == TYPE_ROUTER) routeLine = new RenderRef(particles:0xF019r); //graph.cfg, solid line
				if(type == TYPE_MINIMAP || type == TYPE_MINIMAP_STILL) routeLine = new RenderRef(particles:0x0019r); //graph_fade.cfg, line with fade-in/fade-out
				routeLine.cache();

				routeWatcher = new Thread(this, "navigator route watcher thread");
				routeWatcher.start();
			}
		}

		if(type == TYPE_ROUTER || type == TYPE_MINIMAP || type == TYPE_MINIMAP_STILL) regenerateRoute();
	}

	public void hide()
	{
		cam.destroy(); cam=null;
		vp.destroy(); vp=null;
	}

	public void changeMode( int newMode )
	{
		mode = newMode;
		if(cam) cam.setMatrix( new Vector3(0.0, zoom, 0.0),  new Ypr(0.0, -1.57, 0.0) );
	}

	public void updateNavigator( GameRef car )
	{
		switch(type)
		{
			case(TYPE_CLASSIC):
				updateNavigator(car, mode);
				break;
			case(TYPE_ROUTER):
				updateNavigator(car, 1);
				break;
			case(TYPE_MINIMAP):
				updateNavigator(car, 0);
				break;
			case(TYPE_MINIMAP_STILL):
				updateNavigator(car, 1);
				if(cam) cam.setMatrix(new Vector3(adjustB, zoom, adjustA), new Ypr(rotation, -1.57, 0.0));
		}
	}

	public native void updateNavigator( GameRef car, int mode );


	public void changeSize( float l, float t, float w, float h )
	{
		vp_l=l; vp_t=t; vp_w=w; vp_h=h;

		if(vp) vp.resize(l,t,w,h);
	}

	public void changeZoom( float z )
	{
		zoom=z;
		if(cam) cam.setMatrix(new Vector3(0.0, zoom, 0.0),  new Ypr(0.0, -1.57, 0.0));
	}

	public Marker addMarker( Racer rc )
	{
		RenderRef marker;
		if(type == TYPE_CLASSIC) marker=rc.getMarker();
		if(type == TYPE_ROUTER || type == TYPE_MINIMAP || type == TYPE_MINIMAP_STILL) marker=rc.getMarker(1);

		GameRef car=rc.car;

		return addMarker( marker, car );
	}

	public Marker addMarker( RenderRef rtype, GameRef obj )	//creates dynamic marker
	{
		RenderRef sym = new RenderRef( localroot, rtype, "marker" );
		Marker m = new DMarker( sym, obj );
		dynamarker.addElement( m );

		return m;
	}

	public Marker addMarker( RenderRef rtype, Vector3 pos, int pri )	//creates static marker
	{
		RenderRef sym = new RenderRef( localroot, rtype, "marker" );
		Vector3 mypos = new Vector3( pos );

		//majd a klampelo elvegzi...
		//mypos.mul( 0.01 );
		mypos.y=pri/100.0+0.001f;
		//sym.setMatrix( mypos, null );


		Marker m = new SMarker( sym, mypos );
		marker.addElement( m );

		return m;
	}

	public void remMarker( Marker m )
	{
		if( m )
		{
			if( m instanceof DMarker )
			{	//dynamic!
				if( dynamarker )
				{
					dynamarker.removeElement( m );
					if( m.symbol )
						m.symbol.destroy();
				}
			}
			else
			{	//static
				if( marker )
				{
					marker.removeElement( m );
					if( m.symbol )
						m.symbol.destroy();
				}
			}
		}
	}

	public void run()
	{
		for(;;)
		{
			if(routeTimer.stopped) regenerateRoute(); //RAXAT: this will rebuild route skidmark and flush viewport to prevent visual artefacts

			routeWatcher.sleep(10);
		}
	}

	public void regenerateRoute()
	{
		if(cam && vp)
		{
			routeTimer = new Timer(routeUpdateTime); //regenerate route skidmark every 15 seconds, we need to begin that before everything, otherwise skidmark will be rendered multiple times and cause artefacts
			routeTimer.start();

			int color = 0xCCFFFFFF; //~80% transparency
			if(type == TYPE_ROUTER) color = 0x77FFFFFF; //semi-transparent

			if(routeSkid) routeSkid.finalize();

			routeSkid  = new SkidMark(localroot, routeLine);

			//RAXAT: for some unknown reason routeData array comes broken (prints valid values, but game can't build line with them), so we always need to reassemble it
			Vector3[] convert = new Vector3[routeData.length];
			for(int i=0; i<routeData.length; i++) convert[i] = new Vector3(routeData[i].x, routeData[i].y, routeData[i].z);

			for(int j=0; j<convert.length-1; j++)
			{
				Vector3 nvec = convert[j].add(convert[j+1]);
				routeSkid.add(nvec.mul(0.5*0.01), new Vector3(0,1,0), color, 1.0);
			}

			if(!routeRegen) routeRegen = 1;
		}
	}
}