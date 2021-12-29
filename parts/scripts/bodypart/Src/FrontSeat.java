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

public class FrontSeat extends BodyPart
{
	public FrontSeat( int id )
	{
		super( id );

		name = "FrontSeat";

		prestige_calc_weight = 25.0;

		catalog_view_ypr = new Ypr( 2.3565, -0.7, 0.0 );
	}
}
