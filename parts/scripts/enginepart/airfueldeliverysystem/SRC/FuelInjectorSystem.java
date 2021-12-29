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

public class FuelInjectorSystem extends AirFuelDeliverySystem
{
	// input //
	float	default_mixture_ratio	= 1.0;

	// selectables //
	int	default_fuel_type	= FT_GAS_95;

	public FuelInjectorSystem(){}

	public FuelInjectorSystem( int id )
	{
		super( id );

		name = "Fuel injector system";

		prestige_calc_weight = 25.0;

	}

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			mixture_ratio = saveGame.readFloat();
			fuel_type = saveGame.readInt();
		}
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 1;

		saveGame.write( save_ver );
		if (save_ver >= 1)
		{
			saveGame.write(mixture_ratio);
			saveGame.write(fuel_type);
		}
	}

	public void randomizeMixtureRatio( float power )
	{
		if (power<1.0 && power>0.0)
		{
			float v = 0.5+power*0.5;
			float v2 = 1.0-v;
			mixture_ratio = (mixture_ratio*v)+(random()*mixture_ratio*v2)-(0.5*mixture_ratio*v2);
		}
	}

	//---------tuning
	public int isTuneable()
	{
		return 1;
	}

	// backup values //
	float	old_mixture_ratio;
	int	old_fuel_type;

	public void buildTuningMenu( Menu m )
	{
		String[] fuelTypes = new String[4];

		fuelTypes[0] = "95 octane pump gas";
		fuelTypes[1] = "98 octane pump gas";
		fuelTypes[2] = "100 octane premium pump gas";
		fuelTypes[3] = "112 octane racing methanol";

		old_mixture_ratio = mixture_ratio;
		m.addItem( "Mixture ratio",		1, mixture_ratio, 8.0, 20.0, 49, null ).printValue("   %1.2f:1 A/F");
		old_fuel_type = fuel_type;
//		m.addItem( "Fuel type",			2, fuel_type, fuelTypes, null );
	}

	public void endTuningSession( int cancelled )
	{
		if( cancelled )
		{
			mixture_ratio = old_mixture_ratio;
			fuel_type = old_fuel_type;
		}
		else
		{
			getCar_LocalVersion();
			if (the_car)
				the_car.forceUpdate();
			if (old_mixture_ratio != mixture_ratio)
				GameLogic.spendTime(5*60);
		}
	}

	public void handleMessage( Event m )
	{
		if( m.cmd == 1 )
		{
			mixture_ratio = ((Slider)m.gadget).value;
		}
		else
		if( m.cmd == 2 )
		{
			fuel_type = ((MultiChoice)m.gadget).value;
		}
	}
	//---------tuning

	public void fillDynoData( DynoData dd, int parentSlot )
	{
//		System.log("---------------------!!!!!!!!!!!!!!!!!_-------------------------");
		updatevariables(); // to get current fuel values //
		super.fillDynoData(dd,parentSlot);

		dd.minCompression = minComp;
		dd.maxCompression = maxComp;
		dd.fuelType = getFuelTypeName();
		dd.mixture_ratio = mixture_ratio;

		if (dd.max_fuel_consumption <= 0.0 || dd.max_fuel_consumption > max_fuel_consumption)
			dd.max_fuel_consumption = max_fuel_consumption; //
		if (dd.max_air_consumption <= 0.0 || dd.max_air_consumption > max_air_consumption)
			dd.max_air_consumption = max_air_consumption; //

		dd.mixture_H = mixture_H;
		dd.time_burn = time_burn;
	}
}
