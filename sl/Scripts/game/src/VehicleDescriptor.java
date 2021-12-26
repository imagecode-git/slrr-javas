package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

//describes a single car
public class VehicleDescriptor
{
	int		id;
	int		colorIndex;
	float	valid = 2.3;
	float	power = 1.0;
	float	optical = 1.0;
	float	tear = 1.0;
	float	wear = 1.0;
	float	stockPrestige, fullPrestige;
	float	stockQM = 16.000;
	float	fullQM = 9.000;
	String	vehicleName = "unknown";

	public void save( File saveGame )
	{
		int save_ver = 3;
		saveGame.write(save_ver);
		if (save_ver >= 1)
		{
			saveGame.write( new GameRef( id ) );
			saveGame.write( colorIndex );
			saveGame.write( power );
			saveGame.write( optical );
			saveGame.write( tear );
			saveGame.write( wear );
			saveGame.write( stockPrestige );
			saveGame.write( fullPrestige );
		}
		if (save_ver >= 2)
		{
			saveGame.write( vehicleName );
		}
		if (save_ver >= 3)
		{
			saveGame.write( stockQM );
			saveGame.write( fullQM );
			saveGame.write( valid );
		}
	}

	public VehicleDescriptor load( File saveGame )
	{
		int save_ver;
		save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			id = saveGame.readResID();
			colorIndex = saveGame.readInt();
			power = saveGame.readFloat();
			optical = saveGame.readFloat();
			tear = saveGame.readFloat();
			wear = saveGame.readFloat();
			stockPrestige = saveGame.readFloat();
			fullPrestige = saveGame.readFloat();
		}
		if (save_ver >= 2)
		{
			vehicleName = saveGame.readString();
		}
		if (save_ver >= 3)
		{
			stockQM = saveGame.readFloat();
			fullQM = saveGame.readFloat();
			valid = saveGame.readFloat();
		}

		return this;
	}

	public float estimatePrestige()
	{
		float	result = ((power - 1.0) + (optical - 1.0)) * 0.5;
		if (result < 0.0)
			result = stockPrestige * (1.0 - result);
		else
			result = stockPrestige + (fullPrestige - stockPrestige) * result;
		result *= 0.01;
//		System.log("estimated prestige: "+result);
		return result;
	}

	public float estimateQM()
	{
		float	result = 1.0;

		if (power>=1.0)
			result *= stockQM + (fullQM - stockQM) * (power-1.0);
		else
			result *= stockQM;
//		System.log("estimated quarter mile time: "+result);
		return result;
	}
}
