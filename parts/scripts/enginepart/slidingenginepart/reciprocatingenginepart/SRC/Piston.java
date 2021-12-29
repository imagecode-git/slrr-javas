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

public class Piston extends ReciprocatingEnginePart
{
	float	bore = 0.0;						// in milimeters //
	float	length_from_wrist_pin_center_to_crown_base = 25.0;	// in milimeters //
	float	crown_height = 0.0;					// in milimeters //
	float	crown_volume = 0.0;					// in cc //
	float	length_from_lowest_safe_belt_to_crown_base = 0.0;					// in milimeters //

	public Piston(){}

	public Piston( int id )
	{
		super( id );

		name = "Piston";

		prestige_calc_weight = 15.0;
	}
}
