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

public class SingleAirOutPipe extends EnginePart
{
	float	efficiency = 1.0;

	public SingleAirOutPipe(){}

	public SingleAirOutPipe( int id )
	{
		super( id );

		name = "Single air out pipe";

		prestige_calc_weight = 22.5;
	}
}
