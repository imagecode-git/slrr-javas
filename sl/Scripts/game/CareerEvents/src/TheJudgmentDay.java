package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TheJudgmentDay extends CareerEvent
{
	int sandbagTime = 35;
	int mass = 2500;

	public TheJudgmentDay();

	public void init()
	{
		track_data_id = multibot.maps.DerbyArena.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000061r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000026r;
		eventName = "The JudgmentDay";

		raceTime = 5;

		gamemode_id = GAMEMODE_DERBY;

		botData = new Vector();
		botData.addElement(new Integer(48));	//loryo
		botData.addElement(new Integer(10));	//Nomad
		botData.addElement(new Integer(41));	//FSp
		botData.addElement(new Integer(15));	//MrSmart
		botData.addElement(new Integer(28));	//Bruce

		setPrize(9000);
		rating = 35;

		randomStartGrid = 1;

		fee = 500;

		conditionText[0] = "Sandbag time: " + sandbagTime + "seconds"; //no activity withing this period of time leads to disqualification
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Vehicle mass: " + mass + "kg max";
		reqText[1] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[0] = sandbagTime;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkMass(mass)) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
