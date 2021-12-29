package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.cylinderhead;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.block.block_vee.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.camshaft.*;
import java.game.parts.rgearpart.*;

public class OHV_CylinderHead extends CylinderHead
{
	int	engine_block_slot_ID = 0;

	// misc //
	float	intake_rocker_ratio = 1.0;
	float	exhaust_rocker_ratio = 1.0;

	public OHV_CylinderHead(){}

	public OHV_CylinderHead( int id )
	{
		super( id );

		name = "OHV cylinder head";
	}

	public void updatevariables()
	{
		super.updatevariables();

		OHV_Camshaft cam = getCamshaft();
		if (cam)
		{
			cam.updatevariables();

			in_min = intake_tunnel_area*cam.lift_in_close*0.1; // should raise as the valve springs get older
			in_max = intake_tunnel_area*cam.lift_in_open*0.1;
			time_in_open = cam.time_in_open;
			time_in_close = cam.time_in_close;

			out_min = exhaust_tunnel_area*cam.lift_out_close*0.1; // should raise as the valve springs get older
			out_max = exhaust_tunnel_area*cam.lift_out_open*0.1;
			time_out_open = cam.time_out_open;
			time_out_close = cam.time_out_close;
		}
	}

	public Part getCamshaft()
	{
		if (engine_block_slot_ID <= 0)
			return null;

		Part res = partOnSlot(engine_block_slot_ID);

		if (res && res instanceof Block_Vee_OHV)
			return ((Block_Vee_OHV)res).getCamshaft();
//		else
//			System.log("!!!OHV Vee block required on slot!!!");

		return null;
	}

	public float getSlictionLoss()
	{
		float res = super.getSlictionLoss();
		OHV_Camshaft cam = getCamshaft();

		if (cam)
			res *= cam.getSlictionLoss();

		return res;
	}

	public void check4warnings()
	{
/*		super.check4warnings();
		if (!engine_block_slot_ID)
			System.log("   engine_block_slot_ID is 0");
*/	}
}
