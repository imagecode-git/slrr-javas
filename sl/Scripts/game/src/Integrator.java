package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

//RAXAT: the most efficient storage class for any kind of data
//remove legacy stuff asap!!!
public class Integrator
{
	static int	IngameMenuActive;
	static int	frozen;
	static int	disableTrackHotkeys;
	static int	isCity;

	static int	clubInfoCheat;

	static String	transitString; //temp string for transit class

	static int	debugLoadCareer; //TEMP!!

	static int	lastPaintCanId, lastPainterLine, showDecals, lastPaintMode = Painter.MODE_PAINTPART;

	public Integrator();

	//this data is used to restore UI state when player returns to paintbooth
	public static void flushPainterData()
	{
		lastPaintCanId = 0;
		lastPainterLine = 0;
		showDecals = 0;
		lastPaintMode = Painter.MODE_PAINTPART;
	}

	public static int write_CFG( int esl_shader )
	{
		File config = new File( "Data/config/config.class" );

		File.delete( "Data/config/config.class" );
		if( config.open( File.MODE_WRITE ) )
		{
			config.write( esl_shader );

			int dummy;
			config.write( dummy );
			config.write( dummy );
			config.write( dummy );
			config.write( dummy );
			config.write( dummy );
			config.write( dummy );
			config.write( dummy );

			config.close();

			return 1;
		}
		return 0;
	}

	public static int write_CFG3( int reflection_detail )
	{
		File config3 = new File( "Data/config/config3.class" );

		File.delete( "Data/config/config3.class" );
		if( config3.open( File.MODE_WRITE ) )
		{
			config3.write( reflection_detail );

			int dummy;
			config3.write( dummy );
			config3.write( dummy );
			config3.write( dummy );
			config3.write( dummy );
			config3.write( dummy );
			config3.write( dummy );
			config3.write( dummy );

			config3.close();

			return 1;
		}
		return 0;
	}

	public static int write_CFG2( int smart_vrc )
	{
		File conf2 = new File( "Data/config/config2.class" );

		File.delete( "Data/config/config2.class" );
		if( conf2.open( File.MODE_WRITE ) )
		{
			conf2.write( smart_vrc );

			int dummy;
			conf2.write( dummy );
			conf2.write( dummy );
			conf2.write( dummy );
			conf2.write( dummy );
			conf2.write( dummy );
			conf2.write( dummy );
			conf2.write( dummy );

			conf2.close();

			return 1;
		}
		return 0;
	}

	public static int write_CFG4( int retro_style )
	{
		File config4 = new File( "Data/config/config4.class" );

		File.delete( "Data/config/config4.class" );
		if( config4.open( File.MODE_WRITE ) )
		{
			config4.write( retro_style );

			int dummy;
			config4.write( dummy );
			config4.write( dummy );
			config4.write( dummy );
			config4.write( dummy );
			config4.write( dummy );
			config4.write( dummy );
			config4.write( dummy );

			config4.close();

			return 1;
		}
		return 0;
	}

	public static Vector3 deltaVector3( Vector3 input, Vector3 delta, String action )
	{
		if( action == "summarize" )
		{
			Vector3 tempV3 = new Vector3( input.x + delta.x, input.y + delta.y, input.z + delta.z );
			return tempV3;
		}

		if( action == "substract" )
		{
			Vector3 tempV3 = new Vector3( input.x - delta.x, input.y - delta.y, input.z - delta.z );
			return tempV3;
		}

		if( action == "multiply" )
		{
			Vector3 tempV3 = new Vector3( input.x * delta.x, input.y * delta.y, input.z * delta.z );
			return tempV3;
		}

		if( action == "divide" )
		{
			Vector3 tempV3 = new Vector3( input.x / delta.x, input.y / delta.y, input.z / delta.z );
			return tempV3;
		}

		if( !action )
			return null;

		return null;
	}

	public static Ypr deltaYpr( Ypr input, Ypr delta, String action )
	{
		if( action == "summarize" )
		{
			Ypr tempYpr = new Ypr( input.y + delta.y, input.p + delta.p, input.r + delta.r );
			return tempYpr;
		}

		if( action == "substract" )
		{
			Ypr tempYpr = new Ypr( input.y - delta.y, input.p - delta.p, input.r - delta.r );
			return tempYpr;
		}

		if( action == "multiply" )
		{
			Ypr tempYpr = new Ypr( input.y * delta.y, input.p * delta.p, input.r * delta.r );
			return tempYpr;
		}

		if( action == "divide" )
		{
			Ypr tempYpr = new Ypr( input.y / delta.y, input.p / delta.p, input.r / delta.r );
			return tempYpr;
		}

		if( !action )
			return null;

		return null;
	}

	public static Vector3 floatToVector( float vx, float vy, float vz )
	{
		Vector3 tempV3 = new Vector3();
		tempV3.x = vx;
		tempV3.y = vy;
		tempV3.z = vz;
		return tempV3;

		return null;
	}

	public static Vector3 intToVector( int vx, int vy, int vz )
	{
		Vector3 tempV3 = new Vector3();
		tempV3.x = vx;
		tempV3.y = vy;
		tempV3.z = vz;
		return tempV3;

		return null;
	}

	public static Ypr floatToYpr( float vy, float vp, float vr )
	{
		Ypr tempYpr = new Ypr();
		tempYpr.y = vy;
		tempYpr.p = vp;
		tempYpr.r = vr;
		return tempYpr;

		return null;
	}

	public static Ypr intToYpr( int vy, int vp, int vr )
	{
		Ypr tempYpr = new Ypr();
		tempYpr.y = vy;
		tempYpr.p = vp;
		tempYpr.r = vr;
		return tempYpr;

		return null;
	}
}
