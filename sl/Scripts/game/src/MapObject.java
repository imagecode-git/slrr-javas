package java.game;

import java.util.*;
import java.util.resource.*;
import java.render.*;

//RAXAT: v2.3.1, general map object
public class MapObject
{
	GameType type;
	int typeID;
	Vector3 pos;
	Ypr ori;
	String alias, params;
	int movable; //for wakeUp() on init

	//basic creation (objects are NOT marked as movable by default)
	public MapObject(ResourceRef parent, int tID)
	{
		this(parent, tID, "0,0,0,0,0,0", "mapObject", 0);
	}

	public MapObject(ResourceRef parent, int tID, String a)
	{
		this(parent, tID, "0,0,0,0,0,0", a, 0);
	}

	//advanced creation
	public MapObject(ResourceRef parent, int tID, String pr, String a, int m)
	{
		typeID = tID;
		params = pr;
		alias = a;
		movable = m;

		type = new GameType();
		type.create_native(parent, new GameRef(typeID), params, alias);
		type.cache();
		type.load();
	}

	public void setMatrix(Vector3 p, Ypr o)
	{
		pos = p;
		ori = o;
		if(type) type.setMatrix(pos, ori);
		if(movable) wakeUp();
	}

	public void wakeUp()
	{
		if(type)
		{
			type.setState
			(
				new Vector3(pos.x, pos.y+2, pos.z), ori,
				new Vector3(0,2,0), new Vector3(0,2,0)
			);
		}
	}

	public void finalize()
	{
		if(type)
		{
			type.unload();
			type.release();
			type.deleteNative();
			type = null;
		}
	}
}