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

public class DoubleAirOutPipe extends EnginePart
{
	float	efficiency = 1.0;

	public DoubleAirOutPipe(){}

	public DoubleAirOutPipe( int id )
	{
		super( id );

		name = "Double air out pipe";

		prestige_calc_weight = 22.5;
	}
}
