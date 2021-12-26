package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;

//RAXAT: external storage for all map info
public class TrackData extends GameType
{
	final static String TYPE_CITY 		= "city";
	final static String TYPE_OPEN		= "open location";
	final static String TYPE_CIRCUIT	= "circuit";
	final static String TYPE_OFFROAD	= "offroad";
	final static String TYPE_SPECIAL	= "special";

	Track	dTrack; //for grid data

	String	name; //for Track()

	//EventList relative stuff
	String	e_name;
	int	minimap, bg_pic, envmap; //minimap was recently used for GPS V3, but now it's just kind a logo of track, since minimaps are now automatically generated in GPS V4

	int map_id; //for GroundRef
	Navigator nav;
	Vector3 posStart;
	Ypr oriStart;

	//for freeride interface
	String author, location, type; //general info
	String[] description;


	Vector splinesAI;
	Vector checkpoints; //finish line must be _the last_ checkpoint in this vector!
	Vector safeSurfaces; //list of surface id's that are not considered offroad
	Vector pits;
	Vector raceposData; //for race position calculator and some other modules (must be very precise!)
	Vector3[] routeData; //data for GPS v4

	//startline camera animation stuff
	Pori[] camPath;
	float  camSpeedMul;

	//traffic light setup
	Pori[]  lightPori;
	Light[] light;

	//array to recieve some extra data from map
	float[] specialData;

	//for small maps
	float  rpTriggerSizeCustom;

	float  falseStartDist; //when racer does pass this distance from his start grid position, he will be disqualified from race because of false start

	public TrackData() {}

	public void getGrid() {}
	public void setStartGrid( float x, float y, float z, float yy, float yp, float yr ) //this is left only for compatibility with the debugger
	{
		if(dTrack) dTrack.setStartGrid(x,y,z,yy,yp,yr);
	}

	public void setTrafficLight(ResourceRef parent, int status) {}

	public void getData() {} //data will be loaded only when requested (i.e. when Track will initialize in a specific mode)

	public void finalize()
	{
		//destroy all traffic lights
		if(light && light.length)
		{
			for(int i=0; i<light.length; i++)
			{
				if(light[i]) light[i].finalize();
			}
		}
	}
}