package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class Assassination extends CareerEvent
{
	int laps = 5;
	int mass = 1700;

	public Assassination(){}

	public void init()
	{
		track_data_id = multibot.maps.OranPark.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000004ar);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000000fr;
		eventName = "Assassination";

		raceTime = 10;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(7));	//rudvil
		botData.addElement(new Integer(20));	//LokoFun
		botData.addElement(new Integer(11));	//Hard

		setPrize(4000);
		rating = 15;

		drivetype = DRIVETYPE_FWD;

		carVendor = "Ninja";

		useFines = 1;
		useAwards = 1;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = "Vehicle mass: " + mass + "kg max";
		reqText[2] = "Club rating: " + rating + " or higher";

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
			if(checkMass(mass)) return 1;
			break;

			case 2:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}