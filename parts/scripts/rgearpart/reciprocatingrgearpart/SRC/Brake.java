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

public class Brake extends ReciprocatingRGearPart
{
	float	friction = 1.0;
	float	radius = 0.18;
	float	force = 1.0;
	float	torque = force*friction*radius;
	int	renderID_FL = parts:0x000000A1r;
	int	renderID_FR = parts:0x000000C1r;
	int	renderID_RL = parts:0x000000C3r;
	int	renderID_RR = parts:0x000000C5r;

	public Brake( int id )
	{
		super( id );

		name = "Brake";

		prestige_calc_weight = 20.0;
	}

	public void updatevariables()
	{
		WheelRef whl = getWheel();
		int whlID = getWheelID();
		int rID = 0;

		getCar_LocalVersion();
		if (the_car)
		{
			if (whl)
			{
				whl.setRadius(radius+0.02); // "radius" in the brake class defines the distance of braking point from the center of the rotor. Sala always sets it to 2 cms (~ 4cm calipers). It should be increased with this amount to get correct results. //
				if (whlID>1)
				{
					whl.setBrake(torque*the_car.brake_balance);
					whl.setHBrake(torque);
				}
				else
				{
					whl.setBrake(torque*(1.0-the_car.brake_balance));
				}
				switch (whlID)
				{
					case 0:
						rID = renderID_FL;
						break;
					case 1:
						rID = renderID_FR;
						break;
					case 2:
						rID = renderID_RL;
						break;
					case 3:
						rID = renderID_RR;
						break;
					default:
						rID = parts:0x000000A1r;
						break;
				}
				if (rID)
					setRenderType(rID);
			}
		}
	}
}
