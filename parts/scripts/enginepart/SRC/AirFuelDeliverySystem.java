package java.game.parts.enginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class AirFuelDeliverySystem extends EnginePart
{
	// constants //
	static int	FT_GAS_95	= 0;
	static int	FT_GAS_98	= 1;
	static int	FT_GAS_100	= 2;
	static int	FT_METHANOL	= 3;
	static int	FT_DIESEL	= 4;

	// input //
	float	mixture_ratio		= 1.0;
	float	optimal_mixture_ratio	= 1.0;
	float	max_fuel_consumption	= 0.000001;
	float	max_air_consumption	= 0.00001;

	// selectables //
	int	fuel_type		= FT_GAS_95;

	// output //
	float	mixture_H		= 1.0;
	float	time_burn		= 0.1;
	float	nitro_H			= 1.0;
	float	nitro_cooling		= 1.0;
	float	nitro_consumption	= 1.0;
	float	nitro_minRPM		= 0.0;
	float	nitro_maxRPM		= -1.0;
	float	nitro_minThrottle	= 0.0;

	float	optimal_burn_time_base	= 0.003;
	float	optimal_burn_time_offset= 0.001;

	float	minComp			= 0.0;
	float	maxComp			= 0.0;

	public AirFuelDeliverySystem(){}

	public AirFuelDeliverySystem( int id )
	{
		super( id );

		name = "Air/Fuel delivery system";
	}

	public void calcStuffs()
	{
		if (fuel_type == FT_GAS_98)
			optimal_mixture_ratio = 13.5/1.0;
		else
		if (fuel_type == FT_GAS_100)
			optimal_mixture_ratio = 14.0/1.0;
		else
		if (fuel_type == FT_METHANOL)
			optimal_mixture_ratio = 16.0/1.0;
		else
		if (fuel_type == FT_DIESEL)
			optimal_mixture_ratio = 11.0/1.0;
		else
			optimal_mixture_ratio = 12.5/1.0; // FT_GAS_95 //
	}

	public void updatevariables()
	{
//		System.log("----->>>>> updating "+name+":");
		float H; // J/kg //
		int ft = getFuelType();

		if (ft == FT_METHANOL)
			police_check_fine_value = 200;
		else
			police_check_fine_value = 0;

		if (ft == FT_GAS_95)
//			System.log("  fuel type: 95 octane pump gas");
			H = 1099500.0;
			optimal_mixture_ratio = 12.5/1.0;
			optimal_burn_time_base	= 0.00250;
			optimal_burn_time_offset= 0.00080;

		if (ft == FT_GAS_98)
		{
//			System.log("  fuel type: 98 octane pump gas");
			H = 1100000.0;
			optimal_mixture_ratio = 13.5/1.0;
			optimal_burn_time_base	= 0.00247;
			optimal_burn_time_offset= 0.00075;
		}
		else
		if (ft == FT_GAS_100)
		{
//			System.log("  fuel type: 100 octane premium gas");
			H = 1100250.0;
			optimal_mixture_ratio = 14.0/1.0;
			optimal_burn_time_base	= 0.00242;
			optimal_burn_time_offset= 0.00065;
		}
		else
		if (ft == FT_METHANOL)
		{
//			System.log("  fuel type: racing methanol");
			H = 1400000.0;
			optimal_mixture_ratio = 16.0/1.0;
			optimal_burn_time_base	= 0.00225;
			optimal_burn_time_offset= 0.00040;
		}
		else
		if (ft == FT_DIESEL)
		{
//			System.log("  fuel type: diesel oil");
			H = 1600000.0;
			optimal_mixture_ratio = 11.0/1.0;
			optimal_burn_time_base	= 0.00340;
			optimal_burn_time_offset= 0.00030;
		}
		
		float ratio = clampTo(getMixtureRatio(), 8.0, 20.0);
		float ratio_detuning_ratio;

//		System.log("     current air:fuel ratio: "+ratio+":1.0");
//		System.log("     optimal air:fuel ratio: "+optimal_mixture_ratio+":1.0");

		if (ratio < optimal_mixture_ratio)
			ratio_detuning_ratio = ratio/optimal_mixture_ratio;
		else
			ratio_detuning_ratio = optimal_mixture_ratio/ratio;

		ratio_detuning_ratio = 0.45*ratio_detuning_ratio+0.55;
		ratio_detuning_ratio = ratio_detuning_ratio*ratio_detuning_ratio;
		ratio_detuning_ratio = ratio_detuning_ratio*ratio_detuning_ratio*ratio_detuning_ratio;
		ratio_detuning_ratio = 1.0 - ratio_detuning_ratio;

 		mixture_H = H*(0.8+0.2*(1.0-ratio_detuning_ratio))*1.55;
		time_burn = (optimal_burn_time_base+optimal_burn_time_offset*ratio_detuning_ratio)*0.25;

		minComp = mixture_H/250000.0;
		maxComp = mixture_H/100000.0;

//		System.log("     minComp = "+minComp);
//		System.log("     maxComp = "+maxComp);

//		System.log("     detuning_coeff.: "+(ratio_detuning_ratio*100.0)+"%");
//		System.log("     burn time: "+(time_burn*1000.0)+" ms");
//		System.log("     heat value: "+mixture_H+" J/kg");
//		System.log("----->>>>> end of updating "+name);
	}

	public int getFuelType()
	{
		return fuel_type;
	}

	public String getFuelTypeName()
	{
		if (fuel_type == FT_GAS_95)
			return "95 octane pump gas";
		else
		if (fuel_type == FT_GAS_98)
			return "98 octane pump gas";
		else
		if (fuel_type == FT_GAS_100)
			return "100 octane premium gas";
		else
		if (fuel_type == FT_METHANOL)
			return "racing methanol";
		else
		if (fuel_type == FT_DIESEL)
			return "diesel oil";

		return "unknown fuel type";
	}

	public float getMixtureRatio()
	{
		return mixture_ratio;
	}

	public float getMaxFuelConsumption()
	{
		return max_fuel_consumption;
	}

	public float getMaxAirConsumption()
	{
		return max_air_consumption;
	}
}
