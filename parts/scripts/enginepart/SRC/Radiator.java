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

public class Radiator extends EnginePart
{
	public Radiator( int id )
	{
		super( id );

		name = "Radiator";

		prestige_calc_weight = 10.0;
	}
}

