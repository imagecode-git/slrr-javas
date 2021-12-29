package java.game.parts.enginepart.airfueldeliverysystem.charger;

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
import java.game.parts.enginepart.airfueldeliverysystem.*;

public class TurboCharger extends Charger
{
	float	default_P_turbo_waste	= 0.0;
	float	max_waste = 0.0;
	float	min_waste = 0.0;

	public TurboCharger( int id )
	{
		super( id );

		name = "turbocharger";
	}

	public void updatevariables()
	{
	}

	public float kmToMaxWear(float km)
	{
		if (P_turbo_waste>0.0)
			return super.kmToMaxWear(km)/((default_P_turbo_waste/P_turbo_waste)*(default_P_turbo_waste/P_turbo_waste));
		return super.kmToMaxWear(km);
	}

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			P_turbo_waste = saveGame.readFloat();
		}
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 2;

		saveGame.write( save_ver );
		if (save_ver >= 1)
		{
			saveGame.write(P_turbo_waste);
		}
	}
	
	//RAXAT: build 933, support of legacy turbo mods
	public void fillDynoData( DynoData dd, int parentSlot )
	{
		updatevariables();
		
		super.fillDynoData(dd, parentSlot);
		
		Part p = partOnSlot(getSlotID(-1));
		if(p && p instanceof ExhaustHeader)
		{
			ExhaustHeader eh = (ExhaustHeader)p;
			if(!eh.getTurbochargers().size()) //legacy exhaust header detected, applying old formula for power calculation
			{
				dd.rpm_turbo_mul = 1.0;
				dd.rpm_turbo_opt = rpm_turbo_opt;
				dd.rpm_turbo_range = rpm_turbo_range;
				dd.P_turbo_max = P_turbo_max;
				if (P_turbo_waste > 0.0)
					dd.P_turbo_waste = P_turbo_waste;
				else
					dd.P_turbo_max = 0.0;
			}
		}
		
	}

	//---------tuning
	public int isTuneable()
	{
		return (max_waste>0.0 && min_waste>0.0);
	}

	// backup values //
	float	old_waste;

	public void buildTuningMenu( Menu m )
	{
		old_waste = P_turbo_waste;

		m.addItem( "Wastegate pressure", 1, P_turbo_waste, min_waste, max_waste, (max_waste-min_waste)/0.05+1, null ).changeVLabelText( Float.toString(P_turbo_waste, " %1.2f bar") + Float.toString(P_turbo_waste*14.7, " %1.2f psi") );
	}
	public void endTuningSession( int cancelled )
	{
		if( cancelled )
		{
			P_turbo_waste = old_waste;
		}
		else
		{
			if (old_waste != P_turbo_waste)
				GameLogic.spendTime(4*60);
			getCar_LocalVersion();
			if (the_car)
				the_car.forceUpdate();
		}
	}

	public void handleMessage( Event m )
	{
		if( m.cmd == 1 )
		{
			P_turbo_waste = ((Slider)m.gadget).value;
			((Slider)m.gadget).changeVLabelText( Float.toString(P_turbo_waste, " %1.2f bar") + Float.toString(P_turbo_waste*14.7, " %1.2f psi") );
		}
	}
	//---------tuning
}
