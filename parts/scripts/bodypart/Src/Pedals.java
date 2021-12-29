package java.game.parts.bodypart;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.osd.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;
import java.game.parts.rgearpart.reciprocatingrgearpart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;

public class Pedals extends BodyPart
{
	int	transmissionType;

	public Pedals( int id )
	{
		super( id );

		name = "Pedals";

		int transmissionType;

		prestige_calc_weight = 15.0;
		drag_reduction = 0.0015;
	}

	public String isDriveable()
	{
		getCar_LocalVersion();

		int error;

		if (the_car && transmissionType)
		{
			Block engine = the_car.partOnSlot(401);

			if( engine )
			{
				Transmission t = the_car.partOnSlot(401).getTransmission();

				if(t)
				{
					if( t.type )
					{
						if( transmissionType == 1 ) //auto tranny set for pedals
						{
							if( t.type != 1 && t.type != 5 ) //a manual one detected
								error = 1;
						}

						if( transmissionType != 1 && transmissionType != -1 )
						{
							if( t.type == 1 || t.type == 5 ) //some auto tranny detected
								error = 2;
						}
					}
					else
					{
						if( !t.type )
							transmissionType = -1;
					}
				}
			}
		}


		if( error == 1 )
			return "the pedals you've installed on a car are not for manual transmission.";

		if( error == 2 )
			return "the pedals you've installed on a car are not for automatic transmission.";

		return super.isDriveable();
	}

}

