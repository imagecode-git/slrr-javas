package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class Round
{
	final static int GREEN_ARROW = frontend:0x00000070r;

	Vector		tempObj = new Vector();
	Vector		trigger = new Vector();
	Vector		positions = new Vector();

	Track		track;
	String		messageA, messageB;
	int		osdGroup; //RAXAT: v2.3.1, allows to show complete interfaces, not just messages
	int		type;
	int		noAuto;

	Vector3		startPos;
	Ypr		startOri;

	//RAXAT: new in 2.3.1
	public Round( Track tt, int og )
	{
		this( tt, null, null );
		osdGroup = og;
	}

	public Round( Track tt, String msgA, String msgB, int nA )
	{
		this( tt, msgA, msgB );
		noAuto = nA;
	}

	public Round( Track tt, String msgA, String msgB )
	{
		track = tt;
		messageA = msgA;
		messageB = msgB;
		type = 0;
	}

	public void startdir( float yaw )
	{
		startOri = new Ypr( yaw, 0.0, 0.0 );
	}

	public void point( float x, float y, float z, int obj )
	{
		point( x, y, z, obj, 30.0 );
	}

	public void point( float x, float y, float z, int obj, float r )
	{
		RenderRef	marker;

		Vector3	v3 = new Vector3( x, y, z );

		if( trigger.size() == 0 )
		{
			marker = Marker.RR_START;
			startPos = v3;
		}
		else
			marker = Marker.RR_FINISH;

		positions.addElement( v3 );
		trigger.addElement( track.addTrigger( v3, null, marker, "event_handlerTrigger", r, "testrack trigger" ) );

		if( obj )
		{
			RenderRef	finishObject;

			finishObject = new RenderRef( track.map, GREEN_ARROW, "finishObject" );
			finishObject.setMatrix( new Vector3( x, y + 3.0, z ), null );
			tempObj.addElement( finishObject );
		}
	}

	public void loop()
	{
		trigger.addElement( trigger.elementAt( 0 ) );
		type = 1;
	}

	public Vector3 getPoint( int n )
	{
		return positions.elementAt(n);
	}

	public int size()
	{
		return positions.size();
	}

	public void destroy()
	{
		for( int i = 0; i < trigger.size(); i++ )
		{
			Trigger	tr = trigger.elementAt( i );
			if( tr )
				track.removeTrigger( tr );
		}
		trigger.removeAllElements();

		for( int i = 0; i < tempObj.size(); i++ )
		{
			RenderRef rr = tempObj.elementAt( i );
			if( rr )
				rr.destroy();
		}
		tempObj.removeAllElements();

		positions = null;
	}
}
