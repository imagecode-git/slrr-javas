package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.differential;

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
import java.game.parts.rgearpart.*;

public class Center_Differential extends Differential
{
	public Center_Differential();

	public Center_Differential(int id)
	{
		super(id);

		name = "Center differential";

		drive_base_title = "F";
		drive_comp_title = "R";

		type = TYPE_LIMITED_SLIP;

		applyTuning();
	}

	public void applyTuning()
	{
		getCar_LocalVersion();
		if(the_car)
		{
			the_car.diff_lock = diff_lock;
			the_car.updateDifflock();

			Transmission t = the_car.getBlock().getTransmission();
			if(t) t.drive_front = drive_base;
		}
	}
}
