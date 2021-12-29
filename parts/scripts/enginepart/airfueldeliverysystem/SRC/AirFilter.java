package java.game.parts.enginepart.airfueldeliverysystem;

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

public class AirFilter extends AirFuelDeliverySystem
{
	float	stock_max_air_consumption = 0.00001;
	float	dusted_efficiency = 0.0;

	public AirFilter( int id )
	{
		super( id );

		stock_max_air_consumption = 0.00001;
		max_air_consumption = 0.00001;

		name = "Air Filter";

		prestige_calc_weight = 5.0;
	}

	public void updatevariables()
	{
		max_air_consumption = (stock_max_air_consumption*getWear()*dusted_efficiency)+(stock_max_air_consumption*(1.0-dusted_efficiency))+0.00001;
	}
	//---------tuning
	public int isTuneable()
	{
		return 0;
	}

	public void buildTuningMenu( Menu m )
	{
		m.addItem( "Change filter",	1, "Cleanness: "+(getWear()*100.0)+"%");
	}

	public void endTuningSession( int cancelled )
	{
		if( cancelled )
		{
		}
		else
		{
			getCar_LocalVersion();
			if (the_car)
				the_car.forceUpdate();
			GameLogic.spendTime(1*60);
		}
	}

	public void handleMessage( Event m )
	{
		if( m.cmd == 1 )
		{
			setWear(repair_max_wear);
		}
	}
	//---------tuning
}

