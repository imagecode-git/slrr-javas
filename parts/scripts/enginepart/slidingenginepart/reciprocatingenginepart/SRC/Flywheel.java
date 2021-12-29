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

public class Flywheel extends ReciprocatingEnginePart
{
	float	contact_surface_inner_radius = 80.0; // in milimeters //
	float	contact_surface_outer_radius = 125.0; // in milimeters //
	float	surface_roughness = 1.0; // friction multiplier //
	float	outer_radius = 140.0; // in milimeters //
	float	lug_diam = 0.0; // diameter of the bolting //
	int	lug_count = 0; // number of bolts //

	int	clutch_slot_ID = 0;

	public Flywheel(){}

	public Flywheel( int id )
	{
		super( id );

		name = "Flywheel";

		prestige_calc_weight = 10.0;
	}

	public Part getClutch()
	{
		if (clutch_slot_ID <= 0)
			return null;

		Part res = partOnSlot(clutch_slot_ID);

		if (res && res instanceof Clutch)
			return res;
//		else
//			System.log("!!!Clutch required on slot!!!");

		return null;
	}

	public void fillDynoData( DynoData dd, int parentSlot )
	{
		Clutch cl = getClutch();

		super.fillDynoData(dd,parentSlot);

		if (the_car)
		{
			float res = 0.0;
			if (cl)
				res = cl.maxF*surface_roughness;
//			the_car.maxF = res;
		}
	}

	public float getInertia()
	{
		Part cl = getClutch();
		float res = inertia;

		if (cl)
			res += cl.getInertia();

		return res;
	}

	public void check4warnings()
	{
/*		super.check4warnings();
		if (!clutch_slot_ID)
			System.log("   clutch_slot_ID is 0");
*/	}

	public String isDynoable()
	{
		Part p;

		p = getClutch();
		if (!p && clutch_slot_ID)
			return "It's missing the clutch.";

		return super.isDynoable();
	}
}
