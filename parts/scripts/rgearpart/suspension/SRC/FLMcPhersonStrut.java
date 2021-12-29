package java.game.parts.rgearpart.suspension;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class FLMcPhersonStrut extends Suspension
{
	public FLMcPhersonStrut( int id )
	{
		super( id );

		name = "Front left McPherson strut";

		prestige_calc_weight = 30.0;
	}
}

