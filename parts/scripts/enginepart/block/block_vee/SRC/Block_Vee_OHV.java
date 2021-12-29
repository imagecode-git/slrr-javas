package java.game.parts.enginepart.block.block_vee;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.block.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.camshaft.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.cylinderhead.*;
import java.game.parts.rgearpart.*;


public class Block_Vee_OHV extends Block_Vee
{
	int	camshaft_slot_ID = 0;
	int	camshaft_bearing_slot_ID = 0;

	public Block_Vee_OHV(){}

	public Block_Vee_OHV( int id )
	{
		super( id );
	}

	public Part getCamshaft()
	{
		if (camshaft_slot_ID <= 0)
			return null;

		Part res = partOnSlot(camshaft_slot_ID);

		if (res)
		{
			if (res instanceof OHV_Camshaft)
				return res;
		}
//		else
//			System.log("!!!OHV camshaft required on slot!!!");

		return null;
	}

	public Part getCamshaftBearing()
	{
		if (camshaft_bearing_slot_ID <= 0)
			return null;

		Part res = partOnSlot(camshaft_bearing_slot_ID);

		if (res)
		{
			if (res instanceof SlidingEnginePart)
				return res;
		}
//		else
//			System.log("!!!Camshaft bearing bridge required on slot!!!");

		return null;
	}

	public Part getLeftCylinderHead()
	{
		if (L_cylinder_head_slot_ID <= 0)
			return null;

		Part res = partOnSlot(L_cylinder_head_slot_ID);

		if (res)
		{
			if (res instanceof OHV_CylinderHead)
				return res;
		}
//		else
//			System.log("!!!OHV cylinder head required on slot!!!");

		return null;
	}

	public Part getRightCylinderHead()
	{
		if (R_cylinder_head_slot_ID <= 0)
			return null;

		Part res = partOnSlot(R_cylinder_head_slot_ID);

		if (res)
		{
			if (res instanceof OHV_CylinderHead)
				return res;
		}
//		else
//			System.log("!!!OHV cylinder head required on slot!!!");

		return null;
	}

	public void updatevariables()
	{
//		System.log("--------------------------------------");
//		System.log("Block_Vee_OHV updatevariables() called");
//		System.log("--------------------------------------");

		skip_end_of_update += 1;
		super.updatevariables();

		Camshaft cam=getCamshaft();
		if (cam)
		{
			cam.updatevariables();
			if (cam.maxRPM < dynodata.maxRPM)
				dynodata.maxRPM = cam.maxRPM;

			inertia += 2.0*cam.getInertia(); // because it rotates twice faster than the crankshaft //
			total_sliction_loss *= cam.getSlictionLoss();
		}

		SlidingEnginePart cam_bearing=getCamshaftBearing();
		if (cam_bearing)
			total_sliction_loss *= cam_bearing.getSlictionLoss();

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

//			System.log(">>"+name+", BLOCK_VEE_OHV.JAVA<<:");
//			System.log("  friction_fwd = "+friction_fwd);
//			System.log("  friction_rev = "+friction_rev);
//			System.log("  inertia = "+inertia);
//			System.log("-------------------------------");

		}
	}

	public void check4warnings()
	{
/*		super.check4warnings();
		if (!camshaft_slot_ID)
			System.log("   camshaft_slot_ID is 0");
		if (!camshaft_bearing_slot_ID)
			System.log("   camshaft_bearing_slot_ID is 0");
*/	}
}
