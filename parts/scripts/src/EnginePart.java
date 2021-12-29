package java.game.parts;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.bodypart.*;

public class EnginePart extends Part
{
	Chassis the_car = null;

	public EnginePart( int id )
	{
		super( id );
	}

	public void getCar_LocalVersion()
	{
		Part part = getCarRef();

		if( part && part instanceof Chassis )
			the_car = part;
		else
			the_car = null;
	}

	public float sparkAngleTo4cycleTime( float ang )
	{
		return ang/720.0; // convert angle to 4-cycle time - Sala
	}

	public void fillDynoData( DynoData dd, int parentSlot )
	{
		Part p = partOnSlot( parentSlot ); // reference to parent
		if (p instanceof EnginePart)
			the_car = ((EnginePart)p).the_car;
		else
			getCar_LocalVersion();

		super.fillDynoData( dd, parentSlot );
	}

	public String isDynoable()
	{
		return null;
	}

	public String isDriveable()
	{
		String result=isDynoable();
		if (result)
			return "the engine is not in working condition. "+result;

		return super.isDriveable();
	}
}
