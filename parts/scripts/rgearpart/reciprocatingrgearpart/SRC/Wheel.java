package java.game.parts.rgearpart.reciprocatingrgearpart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class Wheel extends ReciprocatingRGearPart
{
	float	offset = 0.0;

	// Sala: the following variables are used for scripted calculations //

	float	CPatchPatch = 2.0;

	float	wheel_radius;
	float	rim_width;

	static int	rtFACTORY = 0;
	static int	rtALLOY = 1;
	static int	rtRACE = 2;

	int	rim_type = rtFACTORY;

	public Wheel()
	{
		prestige_calc_weight = 20.0;

//		catalog_view_ypr = new Ypr( 1.571, -0.7, 0.0 );
		catalog_view_ypr = new Ypr( -1.35, -0.7, 0.0 );
//		catalog_view_ypr = new Ypr( -1.35, -0.85, 1.0 );
	}

	public void SetupWheel( float R, float W, float ET )
	{
		wheel_radius = R*25.4/2.0/1000.0;
		rim_width = W;
		offset = ET/1000.0;
		setMaxWear(1000000000.0);

		value = HUF2USD((R-13.0)*3500.0+W*1750.0);

		if (rim_type == rtALLOY)
			value *= 1.5;
		else
		if (rim_type == rtRACE)
			value *= 5.0;

		brand_new_prestige_value = 0.4*value/5.0 + 0.6*20.0;
	}

	public void updatevariables()
	{
		Part tyrex=partOnSlot(2);

		WheelRef this_wheel = getWheel();

		if (this_wheel)
		{
			if (tyrex instanceof Tyre)
			{
				Tyre tyre=(Tyre)tyrex;
				tyre.updatevariables();
//				float side = (getWheelID()%2==0) ? (-1.0):(1.0);
				float side;
				if (getWheelID()%2==0)
					side = 1.0;
				else
					side = -1.0;

				/*
				System.log(name+":");
				System.log("  tyre = "+tyre.name);
				System.log("  half width = "+(tyre.tyre_width/2.0/1000.0));
				System.log("  CPatchMaximumAngle = "+(tyre.CPatchMaximumAngle*57.2957795130823208767981548141052));
				System.log("  offset*side = "+(offset*side));
				*/
				this_wheel.setCPatch( tyre.tyre_width/2.0/1000.0*CPatchPatch, tyre.CPatchMaximumAngle, offset*side );
			}
			else
			{
				this_wheel.setPacejka ( 0, 1.4 );
				this_wheel.setPacejka ( 1, 0.0 );
				this_wheel.setPacejka ( 2, 1.49 );
				this_wheel.setPacejka ( 3, 0.0 );
				this_wheel.setPacejka ( 4, 15.20 );
				this_wheel.setPacejka ( 5, 0.0 );
				this_wheel.setPacejka ( 6, -0.00 );
				this_wheel.setPacejka ( 7, 0.0 );
				this_wheel.setPacejka ( 8, -1.00 );
				this_wheel.setPacejka ( 9, 0.0 );
				this_wheel.setPacejka ( 10, 0.0 );
				this_wheel.setPacejka ( 11, 8000.0 );
				this_wheel.setPacejka ( 12, 1.0 );
				this_wheel.setPacejka ( 13, 0.015 );
				this_wheel.setPacejka ( 14, 0.4 );

				this_wheel.setRadius( wheel_radius );
				this_wheel.setWidth( rim_width );
				this_wheel.setFriction( 1.0 );
				this_wheel.setFrictn_x( 1.0 );
				this_wheel.setSliction( 1.0 );
				this_wheel.setStiffness( 100.0 );
				this_wheel.setRollRes( 0.001 );
				this_wheel.setBearing( 20.0 );
				this_wheel.setMaxLoad( 1000.0 );
				this_wheel.setLoadSmooth( 0.0 );
			}
		}
	}

	public String isDriveable()
	{
		Part p;

		p = partOnSlot(2);
		if (!p)
			return "there's no tyre on some wheels.";
		else
		p = partOnSlot(1);
		if (!p)
			return "some wheels are missing the brake rotors.";

		return null;
	}
}