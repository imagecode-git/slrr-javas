package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class DutchMastersRace extends CareerEvent
{
	int laps = 5;

	public DutchMastersRace();

	public void init()
	{
		track_data_id = multibot.maps.Zandvoort.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000004br);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000010r;
		eventName = "Dutch Masters Race";

		raceTime = 10;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(4));	//JackSIM
		botData.addElement(new Integer(22));	//Dron92
		botData.addElement(new Integer(21));	//Bimmer8704
		botData.addElement(new Integer(12));	//HellBoy

		setPrize(5000);
		rating = 20;

		carVendors = new String[2];
		carVendors[0] = "CoupeSport";
		carVendors[1] = "Nonus";

		maxPower = 300;

		useFines = 1;
		useAwards = 1;
		randomStartGrid = 1;

		fee = 300;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Brands: " + carVendors[0] + ", " + carVendors[1];
		reqText[1] = powerToString();
		reqText[2] = "Club rating: " + rating + " or higher";
		reqText[3] = "Transmission: manual";
		reqText[4] = "Naturally aspirated engines only";

		fogData[0] = -100.0;
		fogData[1] = 125.0;

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
			if(checkPower()) return 1;
			break;

			case 2:
			if(checkRating(rating)) return 1;
			break;

			case 3:
			if(checkTransmission(TRANSMISSION_MANUAL) || checkTransmission(TRANSMISSION_MANUAL_C)) return 1;
			break;

			case 4:
			if(!checkForcedInduction()) return 1;
			break;
		}

		return 0;
	}
}
