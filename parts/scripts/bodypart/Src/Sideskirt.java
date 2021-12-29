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

public class Sideskirt extends BodyPart
{
	public Sideskirt( int id )
	{
		super( id );

		name = "Sideskirt";

		prestige_calc_weight = 12.5;
		drag_reduction = 0.0025;
	}
}

