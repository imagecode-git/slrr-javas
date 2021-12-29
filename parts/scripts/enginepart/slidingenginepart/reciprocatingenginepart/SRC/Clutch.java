package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.rgearpart.*;

public class Clutch extends ReciprocatingEnginePart
{
	float	maxF = 500.0;

	public Clutch( int id )
	{
		super( id );

		name = "Clutch";

		prestige_calc_weight = 10.0;
	}
}
