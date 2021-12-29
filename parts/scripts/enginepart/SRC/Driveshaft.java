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

public class Driveshaft extends EnginePart
{
	public Driveshaft( int id )
	{
		super( id );

		name = "Driveshaft";

		prestige_calc_weight = 12.5;
	}
}

