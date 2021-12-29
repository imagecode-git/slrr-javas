package java.game.parts.rgearpart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;

public class StrutBrace extends RGearPart
{
	public StrutBrace( int id )
	{
		super( id );

		name = "Strut brace";

		prestige_calc_weight = 30.0;
	}
}

