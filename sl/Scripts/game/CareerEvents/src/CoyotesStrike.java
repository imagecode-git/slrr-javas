package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class CoyotesStrike extends CareerEvent
{
	int laps = 3;

	public CoyotesStrike();

	public void init()
	{
		track_data_id = multibot.maps.Sandown.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000046r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000000br;
		eventName = "Coyotes Strike";

		raceTime = 23;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(16));	//OSA
		botData.addElement(new Integer(29));	//Balaperdida
		botData.addElement(new Integer(14));	//MoRoZ

		setPrize(1500);
		rating = 10;

		drivetype = DRIVETYPE_RWD;

		minPower = 60;
		maxPower = 90;

		carVendor = "Coyot";

		useFines = 1;
		useAwards = 1;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = powerToString();
		reqText[2] = "Only street legal vehicles";
		reqText[3] = "Club rating: " + rating + " or higher";

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
			if(checkStreetLegal()) return 1;
			break;

			case 3:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
