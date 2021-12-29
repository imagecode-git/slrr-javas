package java.game.parts.rgearpart.suspension;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class FLDoubleWishbone extends Suspension
{
	public FLDoubleWishbone( int id )
	{
		super( id );

		name = "Front left double wishbone suspension";

		prestige_calc_weight = 30.0;
	}
}

