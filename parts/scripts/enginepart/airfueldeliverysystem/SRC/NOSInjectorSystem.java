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

public class NOSInjectorSystem extends AirFuelDeliverySystem
{
	float	maxconsumption = 0.0;
	float	minconsumption = 0.0;

	public NOSInjectorSystem(){}

	public NOSInjectorSystem( int id )
	{
		super( id );

		name = "N2O injector system";

		prestige_calc_weight = 30.0;

		nitro_H = 1.0;
		nitro_cooling = 1.0;
		nitro_consumption = 0.0;
	}

	public void fillDynoData( DynoData dd, int parentSlot )
	{
		updatevariables(); // to get current N2O values //
		super.fillDynoData(dd,parentSlot);

		if (maxconsumption>15.5)
			maxconsumption = 15.5; //RAXAT: extended

		if (minconsumption<0.001)
			minconsumption = 0.001;

		nitro_H = 1.0 + nitro_consumption*0.0666;
		nitro_cooling = 1.0 - nitro_consumption*0.0666;

		dd.nitro_H = nitro_H;
		dd.nitro_cooling = nitro_cooling;
		dd.nitro_consumption = nitro_consumption*0.333;

//		dd.nitro_minRPM = nitro_minRPM;
//		dd.nitro_maxRPM = nitro_maxRPM;
//		dd.nitro_minThrottle = nitro_minThrottle;
	}

	public float FromLbsHr(float value)
	{
		return value/132.352941176470588235294117647059;
	}

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			nitro_consumption = clampTo(saveGame.readFloat(), minconsumption, maxconsumption);
		}
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 1;

		saveGame.write( save_ver );
		if (save_ver >= 1)
		{
			saveGame.write(nitro_consumption);
		}
	}

	//---------tuning
	public int isTuneable()
	{
		return 1;
	}

	// backup values //
	float	old_nitro_consumption;

	public void buildTuningMenu( Menu m )
	{
		old_nitro_consumption = nitro_consumption;

		m.addItem( "Amount injected", 1, nitro_consumption, minconsumption, maxconsumption, 0, null ).changeVLabelText( Float.toString(nitro_consumption*132.277, "%1.0f lbs/hr"));
	}

	public void endTuningSession( int cancelled )
	{
		if( cancelled )
		{
			nitro_consumption = old_nitro_consumption;
		}
		else
		{
			getCar_LocalVersion();
			if (the_car)
				the_car.forceUpdate();
			if (old_nitro_consumption != nitro_consumption)
				GameLogic.spendTime(3*60);
		}
	}

	public void handleMessage( Event m )
	{
		if( m.cmd == 1 )
		{
			nitro_consumption = ((Slider)m.gadget).value;
			((Slider)m.gadget).changeVLabelText( Float.toString(nitro_consumption*132.277, "%1.0f lbs/hr"));
		}
	}
	//---------tuning
}
