package java.game.parts.enginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class OilPan extends EnginePart
{
	float	friction_fwd	= 1.0;
	float	friction_rev	= 1.0;
	float	capacity	= 0.0;

	public OilPan( int id )
	{
		super( id );

		name = "Oil pan";

		prestige_calc_weight = 7.5;
	}
}
