package java.game.parts.enginepart.slidingenginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class ReciprocatingEnginePart extends SlidingEnginePart
{
	float	inertia = 0.0;
	float	maxRPM = -1.0;

	public ReciprocatingEnginePart(){}

	public ReciprocatingEnginePart( int id )
	{
		super( id );

		name = "Reciprocating engine part";
	}

	public float getInertia()
	{
//		System.log(name+"->maxRPM = "+maxRPM);
		return inertia;
	}

	public float kgToInertia(float kg)
	{
		return kg*0.09;//*0.233;
	}

	public void fillDynoData( DynoData dd, int parentSlot )
	{
		super.fillDynoData(dd,parentSlot);
		if (maxRPM >= 0.0 && (dd.maxRPM < 0 || maxRPM < dd.maxRPM))
			dd.maxRPM = maxRPM;
	}
}
