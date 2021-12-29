package java.game.parts;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.bodypart.*;

public class Descriptor
{
	public Descriptor( int col, float opt, float pow, float w, float t)
	{
		color = col;
		optical = opt;
		power = pow;
		wear = w;
		tear = t;
	}

	public Descriptor( Descriptor src)
	{
		color = src.color;
		optical = src.optical;
		power = src.power;
		wear = src.wear;
		tear = src.tear;
	}

	int		color;		// texture ID
	float	optical;
	float	power;
	float	wear;
	float	tear;
}
