package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.rgearpart.*;

public class Crankshaft extends ReciprocatingEnginePart
{
	float	stroke = 0.0; // in milimeters //
	float	main_bearing_diam = 0.0; // in milimeters //
	float	con_bearing_diam = 0.0; // in milimeters //
	float	cylinder_spacing = 0.0; // distance between cylinder centerlines //

	int	stroker_count = 0; // it's not named correctly, this reflects how many cylinders are in a bank in this configuration - in hungarian: a kihajtotengelyek szama (i4 es v8-nal pl. 4-4) //

	int	con_rod_slot_ID = 0;
	int	flywheel_slot_ID = 0;

	public Crankshaft(){}

	public Crankshaft( int id )
	{
		super( id );

		name = "Crankshaft";

		prestige_calc_weight = 15.0;
	}

	public Part getConnectingRod()
	{
		if (con_rod_slot_ID <= 0)
			return null;

		Part res = partOnSlot(con_rod_slot_ID);

		if (res && res instanceof ConnectingRod)
			return res;
//		else
//			System.log("!!!Crankshaft required on slot!!!");

		return null;
	}

	public Part getFlywheel()
	{
		if (flywheel_slot_ID <= 0)
			return null;

		Part res = partOnSlot(flywheel_slot_ID);

		if (res && res instanceof Flywheel)
			return res;
//		else
//			System.log("!!!Flywheel required on slot!!!");

		return null;
	}

	public float getInertia()
	{
		Part fw = getFlywheel();
		Part cr = getConnectingRod();
		float res = super.getInertia();

		if (fw)
			res += fw.getInertia();
		if (cr)
			res += cr.getInertia();

		return res;
	}

	public float getSlictionLoss()
	{
		Part fw = getFlywheel();
		Part cr = getConnectingRod();
		float res = super.getSlictionLoss();

		if (fw)
			res *= fw.getSlictionLoss();
		if (cr)
			res *= cr.getSlictionLoss();

		return res;
	}

	public void check4warnings()
	{
/*		super.check4warnings();

		if (!con_rod_slot_ID)
			System.log("   con_rod_slot_ID is 0");
		if (!flywheel_slot_ID)
			System.log("   flywheel_slot_ID is 0");
*/	}

	public String isDynoable()
	{
		Part p;

		p = getConnectingRod();
		if (!p && con_rod_slot_ID)
			return "It's missing the connecting rods.";

		p = getFlywheel();
		if (!p && flywheel_slot_ID)
			return "It's missing the flywheel.";

		return super.isDynoable();
	}
}
