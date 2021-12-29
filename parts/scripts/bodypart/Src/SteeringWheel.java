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

public class SteeringWheel extends BodyPart
{
	float	diameter = 380.0; // in millimeters //
	float	hub_depth = 0.0; // in centimeters //

	public SteeringWheel( int id )
	{
		super( id );

		name = "Steering wheel";

		prestige_calc_weight = 20.0;

		catalog_view_ypr = new Ypr( 0.0, -0.7, 0.0 );
	}

	public void updatevariables()
	{
		Part the_car = getCarRef();

		if (the_car && the_car instanceof Chassis)
		{
//			the_car.setSteerWheelRadius((diameter-66.0)/2.0/1000.0);
			((Chassis)the_car).setSteerWheel((diameter-66.0)/2.0/1000.0, hub_depth/100.0);
		}
	}
}

