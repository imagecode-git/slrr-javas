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

public class SlidingEnginePart extends EnginePart
{
	float	sliction_loss_multiplier = 1.0;
	float	amount_of_oil_needed = 0.0;

	public SlidingEnginePart(){}

	public SlidingEnginePart( int id )
	{
		super( id );

		name = "Sliding engine part";
	}

	public float getSlictionLoss()
	{
		return sliction_loss_multiplier;
	}
}
