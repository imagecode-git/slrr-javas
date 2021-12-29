package java.game.parts.rgearpart.reciprocatingrgearpart.brake;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;
import java.game.parts.rgearpart.reciprocatingrgearpart.*;

public class DrumBrake extends Brake
{
	public DrumBrake( int id )
	{
		super( id );

		name = "Drum brake";

		prestige_calc_weight = 20.0;
	}
}

