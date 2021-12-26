package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class OldSchoolChallenge extends CareerEvent
{
	int laps = 5;

	public OldSchoolChallenge();

	public void init()
	{
		track_data_id = multibot.maps.Zandvoort.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000005br);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000020r;
		eventName = "Old School Challenge";

		raceTime = 10;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(44));	//SL1NE
		botData.addElement(new Integer(42));	//KilRoy
		botData.addElement(new Integer(35));	//Coffeecat

		setPrize(createPart(parts.engines.MC_Prime_SuperDuty:0x00001085r)); //Draggster GT Tuning Supercharger
		rating = 15;

		carVendors = new String[3];
		carVendors[0] = "Badge";
		carVendors[1] = "MC";
		carVendors[2] = "Prime";

		useFines = 0;
		useAwards = 0;
		randomStartGrid = 1;

		fee = 800;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Brands: " + carVendors[0] + ", " + carVendors[1] + ", " + carVendors[2];
		reqText[1] = "Club rating: " + rating + " or higher";
		reqText[2] = "Naturally aspirated engines only";
		reqText[3] = "No nitrous";

		super.init();

		specialSlots[0] = laps;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkVendor(carVendors)) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;

			case 2:
			if(!checkForcedInduction()) return 1;
			break;

			case 3:
			if(!checkNitrous()) return 1;
			break;
		}

		return 0;
	}
}
