package java.game.parts;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.bodypart.*;

//RAXAT: v2.3.1, getMass(), abstract build(inv)
public class Set extends GameType
{
	String	name;
	String	description;
	int	logo;

	public void save( File saveGame )
	{
		saveGame.write( name );
		saveGame.write( description );
	}

	public static Set createFromFile( File saveGame )
	{
		Set result = new Set();

		result.name = saveGame.readString();
		result.description = saveGame.readString();

		return result;
	}

	//RAXAT: v2.3.1, mass calculation patch
	public float getMass()
	{
		float mass;
		GameType gt = new GameType(); //some abstract gametype to place inventory into
		Inventory inv = new Inventory(gt);

		InventoryItem_Folder tmp = new InventoryItem_Folder(inv);
		tmp.set = this;
		build(tmp.inv);

		if(tmp.inv.items.size())
		{
			for(int i=0; i<tmp.inv.items.size(); i++)
			{
				Part p = tmp.inv.items.elementAt(i).getPart();
				if(p) mass += p.getMass();
			}
		}

		return mass;
		return 0.0;
	}

	//RAXAT: this should stay abstract
	public void build(Inventory inv);
}

