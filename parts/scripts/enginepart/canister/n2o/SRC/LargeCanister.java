package java.game.parts.enginepart.canister.n2o;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

public class LargeCanister extends Canister
{
	public LargeCanister( int id )
	{
		super( id );
		name = "Large gas canister";

		prestige_calc_weight = 10.0;
	}
}

