package java.game.parts.enginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class AirCooler extends EnginePart
{

	float mincooling = 10;
	float maxcooling = 50;
	float airflowspeed = 0.1;

	float intercooling = 1.0;

	public AirCooler(){}

	public AirCooler( int id )
	{
		super( id );

		name = "Air coolers";
	}

	public void calcAirCooling()
	{
		/*
		getCar_LocalVersion();
		if (the_car)
			the_car.setCooling( mincooling, maxcooling, airflowspeed );
		*/
	}

/*
	public void fillDynoData( DynoData dd, int parentSlot )
	{
		super.fillDynoData(dd,parentSlot);
		dd.intercooling = intercooling;
	}
*/
}

