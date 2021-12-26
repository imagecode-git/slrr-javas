package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class DirtyActions extends CareerEvent
{
	int laps = 3;

	public DirtyActions();

	public void init()
	{
		track_data_id = multibot.maps.NationalDirtRaceway.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000050r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000015r;
		eventName = "Dirty Actions";

		raceTime = 17;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(6));	//NOFEAR
		botData.addElement(new Integer(23));	//konner
		botData.addElement(new Integer(39));	//Charger

		setPrize(3000);
		rating = 25;

		drivetype = DRIVETYPE_AWD;

		carVendor = "SuperDuty";

		maxPower = 275;

		useFines = 0;
		useAwards = 0;
		randomStartGrid = 1;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = powerToString();
		reqText[2] = "Naturally aspirated engines only";
		reqText[3] = "No nitrous";
		reqText[4] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[0] = laps;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkVendor(carVendor)) return 1;
			break;

			case 1:
			if(checkPower()) return 1;
			break;

			case 2:
			if(!checkForcedInduction()) return 1;
			break;

			case 3:
			if(!checkNitrous()) return 1;
			break;

			case 4:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
