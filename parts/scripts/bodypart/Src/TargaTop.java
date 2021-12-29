package java.game.parts.bodypart;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.osd.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;
import java.game.parts.rgearpart.reciprocatingrgearpart.*;

public class TargaTop extends BodyPart
{
	public TargaTop( int id )
	{
		super( id );

		name = "TargaTop";

		prestige_calc_weight = 30.0;
		drag_reduction = 0.0025;
	}
}

