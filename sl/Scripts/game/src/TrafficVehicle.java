package java.game;

import java.util.*;
import java.util.resource.*;
import java.render.*;

//RAXAT: v2.3.1, traffic vehicle as map object
public class TrafficVehicle
{
	GameType type;
	int typeID;
	Vector3 pos;
	Ypr ori;
	String alias, params;

	//basic traffic (ambulance/taxi/bus/etc.)
	public TrafficVehicle(ResourceRef parent, int tID)
	{
		this(parent, tID, "1,1,1,1,1,1", "trafficVehicle");
	}

	public TrafficVehicle(ResourceRef parent, int tID, String a)
	{
		this(parent, tID, "1,1,1,1,1,1", a);
	}

	//advanced traffic (f/ex. with paintable parts)
	public TrafficVehicle(ResourceRef parent, int tID, String pr, String a)
	{
		typeID = tID;
		params = pr;
		alias = a;

		type = new GameType();
		type.create_native(parent, new GameRef(typeID), params, alias);
	}

	public void setMatrix(Vector3 p, Ypr o)
	{
		pos = p;
		ori = o;
		type.setMatrix(pos, ori);
	}

}