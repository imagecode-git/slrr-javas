package java.game.parts.enginepart.airfueldeliverysystem;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class Charger extends AirFuelDeliverySystem
{
	float	rpm_turbo_mul	= 1.0;
	float	rpm_turbo_opt	= 2.0;
	float	rpm_turbo_range = 1.0;
	float	P_turbo_max		= 0.0;
	float	P_turbo_waste	= 0.0;

	public Charger( int id )
	{
		super( id );

		name = "air charger";

		prestige_calc_weight = 90.0;
	}

	//RAXAT: this was missing for some reason
	public void updatevariables()
	{
	}
	
	public void calculateOptandRange(float work_from, float work_to)
	{
		rpm_turbo_range = (work_to-work_from)*0.5;
		rpm_turbo_opt = work_from+rpm_turbo_range;
	}
}
