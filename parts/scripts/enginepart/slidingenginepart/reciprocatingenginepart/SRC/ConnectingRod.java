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

public class ConnectingRod extends ReciprocatingEnginePart
{
	float	length = 0.0; // in milimeters //
	float	crank_bearing_diam = 0.0;
	float	wrist_bearing_diam = 0.0;

	int	piston_slot_ID = 0;

	public ConnectingRod(){}

	public ConnectingRod( int id )
	{
		super( id );

		name = "Connecting rod";

		prestige_calc_weight = 5.0;
	}

	public Part getPiston()
	{
		if (piston_slot_ID <= 0)
			return null;

		Part res = partOnSlot(piston_slot_ID);

		if (res && res instanceof Piston)
			return res;
//		else
//			System.log("!!!Connecting rod required on slot!!!");

		return null;
	}

	public float getInertia()
	{
		Part p = getPiston();
		float res = super.getInertia();

		if (p)
			res += p.getInertia();

		return res;
	}

	public float getSlictionLoss()
	{
		Part p = getPiston();
		float res = super.getSlictionLoss();

		if (p)
			res *= p.getSlictionLoss();

		return res;
	}

	public void check4warnings()
	{
/*		super.check4warnings();
		if (!piston_slot_ID)
			System.log("   piston_slot_ID is 0");
*/	}

	public String isDynoable()
	{
		Part p;

		p = getPiston();
		if (!p && piston_slot_ID)
			return "It's missing the pistons.";

		return super.isDynoable();
	}
}
