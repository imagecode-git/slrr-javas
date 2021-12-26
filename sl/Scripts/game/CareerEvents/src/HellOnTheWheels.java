package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class HellOnTheWheels extends CareerEvent
{
	int sandbagTime = 45;

	public HellOnTheWheels();

	public void init()
	{
		track_data_id = multibot.maps.DerbyArena.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000047r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000000cr;
		eventName = "Hell On The Wheels";

		raceTime = 16;

		gamemode_id = GAMEMODE_DERBY;

		botData = new Vector();
		botData.addElement(new Integer(9));	//Einimas
		botData.addElement(new Integer(24));	//Canadia
		botData.addElement(new Integer(25));	//cheekone
		botData.addElement(new Integer(19));	//MadSlipknot

		setPrize(3500);
		rating = 10;

		randomStartGrid = 1;

		fee = 500;

		conditionText[0] = "Sandbag time: " + sandbagTime + "seconds"; //no activity withing this period of time leads to disqualification
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[0] = sandbagTime;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
