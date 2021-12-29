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

public class Battery extends EnginePart
{
	float	power = 1.0;

	public Battery(){}

	public Battery( int id )
	{
		super( id );

		name = "Battery";

		prestige_calc_weight = 2.5;
	}
}

