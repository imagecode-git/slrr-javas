package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TheDarkRing extends CareerEvent
{
	int laps = 7;

	public TheDarkRing(){}

	public void init()
	{
		track_data_id = multibot.maps.GatewayRoad.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000005ar);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000001fr;
		eventName = "The Dark Ring";

		raceTime = 2;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(17));	//TulskyPryanik
		botData.addElement(new Integer(45));	//CHOOVAK
		botData.addElement(new Integer(36));	//ACrow
		botData.addElement(new Integer(31));	//PuPs
		botData.addElement(new Integer(30));	//Igorr
		botData.addElement(new Integer(55));	//Dee_Key

		setPrize(8000);
		rating = 35;

		drivetype = DRIVETYPE_RWD;

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;

		fee = 400;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		reqText[0] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[0] = laps;
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
