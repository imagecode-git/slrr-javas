package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.cylinderhead;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.camshaft.*;
import java.game.parts.rgearpart.*;

public class DOHC_CylinderHead extends CylinderHead
{
	int	exhaust_camshaft_slot_ID = 0;
	int	intake_camshaft_slot_ID = 0;

	public DOHC_CylinderHead(){}

	public DOHC_CylinderHead( int id )
	{
		super( id );

		name = "DOHC cylinder head";
	}

	public Part getExhaustCamshaft()
	{
		if (exhaust_camshaft_slot_ID <= 0)
			return null;

		Part res = partOnSlot(exhaust_camshaft_slot_ID);

		if (res && res instanceof DOHC_Camshaft)
			return res;
//		else
//			System.log("!!!DOHC camshaft required on slot!!!");

		return null;
	}

	public Part getIntakeCamshaft()
	{
		if (intake_camshaft_slot_ID <= 0)
			return null;

		Part res = partOnSlot(intake_camshaft_slot_ID);

		if (res && res instanceof DOHC_Camshaft)
			return res;
//		else
//			System.log("!!!DOHC camshaft required on slot!!!");

		return null;
	}

	public void updatevariables()
	{
		super.updatevariables();

		if (camshaft_bearing_slot_ID && getCamshaftBearing())
		{
			if (exhaust_camshaft_slot_ID)
				disableSlot(exhaust_camshaft_slot_ID,1);

			if (intake_camshaft_slot_ID)
				disableSlot(intake_camshaft_slot_ID,1);
		}
		else
		{
			if (exhaust_camshaft_slot_ID)
				disableSlot(exhaust_camshaft_slot_ID,0);

			if (intake_camshaft_slot_ID)
				disableSlot(intake_camshaft_slot_ID,0);
		}

		DOHC_Camshaft in_cam = getIntakeCamshaft();
		DOHC_Camshaft ex_cam = getExhaustCamshaft();

		if (in_cam)
		{
			in_cam.updatevariables();
			in_min = intake_tunnel_area*in_cam.lift_in_close*0.1; // should raise as the valve springs get older
			in_max = intake_tunnel_area*in_cam.lift_in_open*0.1;
			time_in_open = in_cam.time_in_open;
			time_in_close = in_cam.time_in_close;
		}

		if (ex_cam)
		{
			ex_cam.updatevariables();
			out_min = exhaust_tunnel_area*ex_cam.lift_out_close*0.1; // should raise as the valve springs get older
			out_max = exhaust_tunnel_area*ex_cam.lift_out_open*0.1;
			time_out_open = ex_cam.time_out_open;
			time_out_close = ex_cam.time_out_close;
		}
	}

	public float getInertia()
	{
		float res = super.getInertia();
		DOHC_Camshaft in_cam = getIntakeCamshaft();
		DOHC_Camshaft ex_cam = getExhaustCamshaft();

		if (in_cam)
			res += 2.0*in_cam.getInertia();
		if (ex_cam)
			res += 2.0*ex_cam.getInertia();

		return res;
	}

	public float getSlictionLoss()
	{
		float res = super.getSlictionLoss();
		DOHC_Camshaft in_cam = getIntakeCamshaft();
		DOHC_Camshaft ex_cam = getExhaustCamshaft();

		if (in_cam)
			res *= in_cam.getSlictionLoss();
		if (ex_cam)
			res *= ex_cam.getSlictionLoss();

		return res;
	}

	public void check4warnings()
	{
/*		super.check4warnings();
		if (!intake_camshaft_slot_ID)
			System.log("   intake_camshaft_slot_ID is 0");
		if (!exhaust_camshaft_slot_ID)
			System.log("   exhaust_camshaft_slot_ID is 0");
*/	}

	public String isDynoable()
	{
		Part p;

		p = getExhaustCamshaft();
		if (!p && exhaust_camshaft_slot_ID)
			return "It's missing the exhaust camshaft.";

		p = getIntakeCamshaft();
		if (!p && intake_camshaft_slot_ID)
			return "It's missing the intake camshaft.";

		return super.isDynoable();
	}
}
