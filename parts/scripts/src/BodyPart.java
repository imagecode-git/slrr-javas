package java.game.parts;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.bodypart.*;

public class BodyPart extends Part
{
	Chassis the_car = null;
	float	drag_reduction = 0.0;

	public BodyPart( int id )
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
		if (p instanceof BodyPart)
			the_car = ((BodyPart)p).the_car;
		else
			getCar_LocalVersion();

		super.fillDynoData( dd, parentSlot );
	}
}

