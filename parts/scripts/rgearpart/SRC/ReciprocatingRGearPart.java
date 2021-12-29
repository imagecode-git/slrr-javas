package java.game.parts.rgearpart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;

public class ReciprocatingRGearPart extends RGearPart
{
	float	inertia = 0.0;
	float	maxRPM = -1.0;

	public ReciprocatingRGearPart(){}

	public ReciprocatingRGearPart( int id )
	{
		super( id );

		name = "Reciprocating running gear part";
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
}
