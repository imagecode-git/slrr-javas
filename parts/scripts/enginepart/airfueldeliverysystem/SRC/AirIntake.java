package java.game.parts.enginepart.airfueldeliverysystem;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class AirIntake extends AirFuelDeliverySystem
{
	public AirIntake( int id )
	{
		super( id );

		name = "Air intake part";

		prestige_calc_weight = 10.0;
	}
}

