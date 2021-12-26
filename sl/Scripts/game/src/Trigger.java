package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.sound.*;

//RAXAT: v2.3.1, pos, alias added
public class Trigger
{
	final static int DEFAULT = system:0x00000034r;
	
	GameRef		trigger;

	Navigator	nav;
	Marker		marker;

	//v2.3.1
	Vector3		pos;
	String		alias;

	public Trigger( GameRef parent, GameRef type, Vector3 pos, String alias)
	{
		this( parent, type, pos, 20.0, alias );
		this.pos = pos;
		this.alias = alias;
	}

	public Trigger( GameRef parent, GameRef type, Vector3 pos, float r, String alias)
	{
		if( type == null )
			type = new GameRef( DEFAULT );

		trigger = new GameRef(parent, type.id(), pos.toString() + ",0,0,0,sphere," + r, alias);
		this.pos = pos;
		this.alias = alias;
	}

	public Trigger( GameRef parent, GameRef type, Vector3 pos, float x, float y, float z, String alias)
	{
		if( type == null )
			type = new GameRef( DEFAULT );

		trigger = new GameRef(parent, type.id(), pos.toString() +",0,0,0,box,"+ x +","+ y +","+ z, alias);
		this.pos = pos;
		this.alias = alias;
	}

	public void finalize()
	{
		if( trigger )
		{
			trigger.destroy();
			trigger=null;
		}

		if( nav )
			nav.remMarker( marker );
	}
}
