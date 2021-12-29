package java.game.parts.rgearpart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;

//RAXAT: bug!! chassis will always return rollbar count as 0! does add_rollbar/rem_rollbar work?
public class Swaybar extends RGearPart
{
	float force;
	float damping;

	public Swaybar( int id )
	{
		super( id );

		name = "Anti-roll bar";

		prestige_calc_weight = 20.0;
	}

	public void calcStuffs()
	{
		name = name_prefix + Float.toString(force,"\n %1.0f N/m ")+"\n anti-roll bar";
		value = tHUF2USD(brand_prestige_factor * (force/2333.0 + 0.100/(damping)*13.0)*4);
	}

	public void updatevariables()
	{
		getCar_LocalVersion();

		int parentSlotID = slotIDOnSlot(1);

		if (parentSlotID == 701)
		{
			the_car.queueEvent( null, EVENT_COMMAND, "addrollbar 0 1 "+force+" "+damping );
		}
		else
		if (parentSlotID == 702)
		{
			the_car.queueEvent( null, EVENT_COMMAND, "addrollbar 2 3 "+force+" "+damping );
		}
		else
			return;
//		System.log("anti-roll bar damping = " + damping);
//		System.log("anti-roll bar force   = " + force );
	}
}
