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

public class Canister extends EnginePart
{
	float	capacity = 0.0;
	float	actual = 0.0;

	public Canister( int id )
	{
		super( id );

		name = "Canister";

		prestige_calc_weight = 10.0;
	}

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			actual = saveGame.readFloat();
		}
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 1;

		saveGame.write( save_ver );
		if (save_ver >= 1)
		{
			saveGame.write(actual);
		}
	}
}
