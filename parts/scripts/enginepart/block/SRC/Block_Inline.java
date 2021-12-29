package java.game.parts.enginepart.block;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.game.parts.enginepart.airfueldeliverysystem.*;

public class Block_Inline extends Block
{
	int	cylinder_head_slot_ID = 0;

	public Block_Inline(){}

	public Block_Inline( int id )
	{
		super( id );
	}

	public Part getCylinderHead()
	{
		if (cylinder_head_slot_ID <= 0)
			return null;

		Part res = partOnSlot(cylinder_head_slot_ID);

		if (res)
		{
			if (res instanceof CylinderHead)
				return res;
		}
//		else
//			System.log("!!!Cylinder head required on slot!!!");

		return null;
	}

	public void updatevariables()
	{
//		System.log("-------------------------------------");
//		System.log("Block_Inline updatevariables() called");
//		System.log("-------------------------------------");

		skip_end_of_update += 1;
		super.updatevariables();

		float exhaust_efficiency = 1.0;
		float intake_efficiency = 0.0;

		float bore_area = bore*bore*0.01*0.25*3.1416;
		float cylinder_volume = 1000000000000000.0;

		Piston piston=null;
		ConnectingRod connecting_rod=null;
		Crankshaft crank=getCrankshaft();
		if (crank)
		{
			connecting_rod=crank.getConnectingRod();
			if (connecting_rod)
				piston = connecting_rod.getPiston();
			cylinder_volume = bore_area*crank.stroke*0.1;
		}

		CylinderHead ch=getCylinderHead();
		if (ch)
		{
			ch.updatevariables();
			if (ch.maxRPM < dynodata.maxRPM)
				dynodata.maxRPM = ch.maxRPM;

			dynodata.T_loss=ch.T_loss;
			total_sliction_loss *= ch.getSlictionLoss();
			inertia += ch.getInertia();

			if (piston)
			{
				float TDC_height_offset = ((crank_center_to_cylinder_top-crank.stroke*0.5-connecting_rod.length-piston.length_from_wrist_pin_center_to_crown_base)*0.1);

				float tvm2=bore_area*TDC_height_offset;
				float temp_vmin=(ch.Vmin-piston.crown_volume+tvm2);

				dynodata.Vmin = temp_vmin*0.000001;
			}
			else
			{
				dynodata.Vmin = 1000000000000000.0;
			}

			float waste = 0.0;
			ExhaustHeader eh = ch.getExhaustHeader();
			if (eh)
			{
				eh.updatevariables();
				exhaust_efficiency = eh.efficiency*ch.exhaust_efficiency_tuning;
			
			}

			IntakeManifold im = ch.getIntakeManifold();
			if (im)
			{
				im.updatevariables();
				intake_efficiency = im.efficiency*ch.intake_efficiency_tuning;
				dynodata.mixture_ratio = im.mixture_ratio;
				dynodata.mixture_H = im.mixture_H;
				dynodata.max_fuel_consumption = im.max_fuel_consumption;
				dynodata.max_air_consumption = im.max_air_consumption;
				dynodata.time_burn = im.time_burn;
			}
			else
			{
				dynodata.mixture_ratio = 1.0;
				dynodata.mixture_H = 1.0;
				dynodata.max_fuel_consumption = 0.000001;
				dynodata.max_air_consumption = 0.000001;
				dynodata.time_burn = 0.1;
			}

			if( cylinder_volume && intake_efficiency )
			{
				dynodata.in_min = ((ch.in_min*ch.in_min)/cylinder_volume)/intake_efficiency*in_breather_magic;
				dynodata.in_max = ((ch.in_max*ch.in_max)/cylinder_volume)*intake_efficiency*in_breather_magic;
				dynodata.out_min = ((ch.out_min*ch.out_min)/cylinder_volume)/exhaust_efficiency*ex_breather_magic;
				dynodata.out_max = ((ch.out_max*ch.out_max)/cylinder_volume)*exhaust_efficiency*ex_breather_magic;

				dynodata.in_min = clampTo( dynodata.in_min, 0.0, 1.0 );
				dynodata.in_max = clampTo( dynodata.in_max, 0.0, 1.0 );
				dynodata.out_min = clampTo( dynodata.out_min, 0.0, 1.0 );
				dynodata.out_max = clampTo( dynodata.out_max, 0.0, 1.0 );
			}
			else
			{
				dynodata.in_min = 0.0;
				dynodata.in_max = 0.0;
				dynodata.out_min = 0.0;
				dynodata.out_max = 0.0;
			}

			dynodata.time_in_open = ch.time_in_open;
			dynodata.time_in_close = ch.time_in_close;
			dynodata.time_out_open = ch.time_out_open;
			dynodata.time_out_close = ch.time_out_close;
		}
		else
			dynodata.Vmin = 1000000000000000.0;

		if (skip_end_of_update)
			skip_end_of_update -= 1;
		else
		{
			if (total_sliction_loss < 0.001)
				total_sliction_loss = 0.001;

			if (total_sliction_loss > 1.0)
				total_sliction_loss = 1.0;

			// lubrication //
			OilPan oilpan = getOilPan();
			if (oilpan)
			{
				oilpan.updatevariables();

				friction_fwd = oilpan.friction_fwd/total_sliction_loss;
				friction_rev = oilpan.friction_rev/total_sliction_loss;
			}
			else
			{
				friction_fwd = 1.0;
				friction_rev = 1.0;
			}

//			System.log(">>"+name+", BLOCK_INLINE.JAVA<<:");
//			System.log("  friction_fwd = "+friction_fwd);
//			System.log("  friction_rev = "+friction_rev);
//			System.log("  inertia = "+inertia);
//			System.log("-------------------------------");
		}
	}

	public void check4warnings()
	{
/*		super.check4warnings();
		if (!cylinder_head_slot_ID)
			System.log("   cylinder_head_slot_ID is 0");
*/	}

	public String isDynoable()
	{
		Part p;

		p = getCylinderHead();
		if (!p && cylinder_head_slot_ID)
			return "It's missing the cylinder head.";

		return super.isDynoable();
	}
}
