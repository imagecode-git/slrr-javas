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
import java.game.parts.enginepart.airfueldeliverysystem.*;

public class ExhaustHeader extends EnginePart
{
	float	efficiency = 1.0;
	
	//RAXAT: build 932, turbo lists
	Vector	turboSlotIDList = null;

	public ExhaustHeader(){}

	public ExhaustHeader( int id )
	{
		super( id );
		
		name = "Exhaust header";

		prestige_calc_weight = 20.0;
	}
	
	public Vector getTurbochargers()
	{
		Vector result = new Vector();
		if(turboSlotIDList)
		{
			int slots = turboSlotIDList.size();
			for(int i=0; i<slots; i++)
			{
				Part p = partOnSlot(turboSlotIDList.elementAt(i).intValue());
				if(p && p instanceof Charger)
				{
					Charger tc = (Charger)p;
					result.addElement(tc);
				}
			}
		}
		
		return result;
	}
	
	public void updatevariables()
	{
	}
	
	//todo: fillDynoData for legacy turbos (if no turbo slots, but there's a turbo installed, use legacy formula, otherwise use the new one)
	
	//RAXAT: build 932, multiple turbos
	public void fillDynoData( DynoData dd, int parentSlot )
	{
		updatevariables();

		super.fillDynoData(dd,parentSlot);
		
		Vector turbos = getTurbochargers();
		float air_efficiency		= 1.0; //air flow efficiency of all turbochargers in a sequence
		float wheel_acceleration	= 1.0; //acceleration of turbine wheel
		float rpm_turbo_mul_total	= 0.0;
		float rpm_turbo_opt_total	= 0.0;
		float rpm_turbo_mul_max		= 0.0;
		float rpm_turbo_opt_max		= 0.0;
		float rpm_turbo_range_max	= 0.0;
		float P_turbo_max			= 0.0;
		float P_turbo_waste			= 0.0;
		
		if(turbos && turbos.size())
		{
			rpm_turbo_mul_total	= 0.0;
			rpm_turbo_opt_total	= 0.0;
			
			for(int i=0; i<turbos.size(); i++)
			{
				Charger tc = turbos.elementAt(i);
				if(i>0) air_efficiency += 1.0/(2*(i+1)); //more sequential turbos - less additional air efficiency
				wheel_acceleration += i*((tc.P_turbo_waste/tc.P_turbo_max)/turbos.size());
				
				rpm_turbo_range_max += tc.rpm_turbo_range*wheel_acceleration;
				rpm_turbo_opt_total += tc.rpm_turbo_opt*wheel_acceleration;
				P_turbo_max += tc.P_turbo_max*air_efficiency;
				P_turbo_waste += tc.P_turbo_waste*air_efficiency;
				rpm_turbo_mul_total += tc.rpm_turbo_mul*air_efficiency;
			}
			
			dd.rpm_turbo_mul = rpm_turbo_mul_total;
			dd.rpm_turbo_opt = rpm_turbo_opt_total;
			dd.rpm_turbo_range = rpm_turbo_range_max;
			dd.P_turbo_max = P_turbo_max;
			
			if (P_turbo_waste > 0.0) dd.P_turbo_waste = P_turbo_waste;
			else dd.P_turbo_max = 0.0;
		}
	}
	
	//RAXAT: build 932, checking all turbos now
	public String isDynoable()
	{
		if(turboSlotIDList)
		{
			Vector turbos = getTurbochargers();
			if(turbos)
			{
				int turbos_installed = turbos.size();
				int turbo_slots = turboSlotIDList.size();
				
				if(turbos_installed < turbo_slots && turbo_slots > 1) return this.name + " is missing a turbocharger.";
			}
		}
		
		return super.isDynoable();
	}
}

