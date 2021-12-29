package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.cylinderhead;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.block.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.camshaft.*;
import java.game.parts.rgearpart.*;

public class SOHC_CylinderHead extends CylinderHead
{
	int	camshaft_slot_ID = 0;

	public SOHC_CylinderHead(){}

	public SOHC_CylinderHead( int id )
	{
		super( id );

		name = "SOHC cylinder head";
	}

	public void updatevariables()
	{
		super.updatevariables();

		if (camshaft_bearing_slot_ID && getCamshaftBearing())
		{
			if (camshaft_slot_ID)
				disableSlot(camshaft_slot_ID,1);
		}
		else
		{
			if (camshaft_slot_ID)
				disableSlot(camshaft_slot_ID,0);
		}

		SOHC_Camshaft cam = getCamshaft();
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
		if (camshaft_slot_ID <= 0)
			return null;

		Part res = partOnSlot(camshaft_slot_ID);

		if (res && res instanceof SOHC_Camshaft)
			return res;
//		else
//			System.log("!!!SOHC camshaft required on slot!!!");

		return null;
	}

	public float getInertia()
	{
		float res = super.getInertia();
		SOHC_Camshaft cam = getCamshaft();

		if (cam)
			res += 2.0*cam.getInertia();

		return res;
	}

	public float getSlictionLoss()
	{
		float res = super.getSlictionLoss();
		SOHC_Camshaft cam = getCamshaft();

		if (cam)
			res *= cam.getSlictionLoss();

		return res;
	}

	public void check4warnings()
	{
/*		super.check4warnings();
		if (!camshaft_slot_ID)
			System.log("   camshaft_slot_ID is 0");
*/	}

	public String isDynoable()
	{
		Part p;

		p = getCamshaft();
		if (!p && camshaft_slot_ID)
			return "It's missing the camshaft.";

		return super.isDynoable();
	}
}
