package java.game.parts.enginepart;

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

public class ExhaustPipe extends EnginePart
{
	float	efficiency = 1.0;
	Vector	mufflerSlotIDList = null;

	public ExhaustPipe(){}

	public ExhaustPipe( int id )
	{
		super( id );

		name = "Exhaust pipe";

		prestige_calc_weight = 22.5;
	}

	//RAXAT: build 932, refactored muffler collection code
	public Vector getMufflers()
	{
		Vector result = null;
		
		if(mufflerSlotIDList && mufflerSlotIDList.size())
		{
			result = new Vector();
			
			for(int i=0; i<mufflerSlotIDList.size(); i++)
			{
				Part p = partOnSlot(mufflerSlotIDList.elementAt(i).intValue());
				if(p && p instanceof ExhaustTip) result.addElement((ExhaustTip)p);
			}
		}

		return result;
	}
}
