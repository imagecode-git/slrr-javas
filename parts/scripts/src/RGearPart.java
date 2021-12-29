package java.game.parts;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.bodypart.*;

public class RGearPart extends Part
{
	Chassis the_car = null;
	String	name_prefix = "SL Tuners";
	float	brand_prestige_factor = 1.00;

	public RGearPart( int id )
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

	public void fillDynoData( DynoData dd, int parentSlot )
	{
		Part p = partOnSlot( parentSlot ); // reference to parent
		if (p instanceof RGearPart)
			the_car = ((RGearPart)p).the_car;
		else
			getCar_LocalVersion();

		super.fillDynoData( dd, parentSlot );
	}
}

