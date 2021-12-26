package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TheInfiniteLoop extends CareerEvent
{
	int laps = 5;
	float time = 187.0;

	float[] times = new float[3];

	public TheInfiniteLoop();

	public void init()
	{
		track_data_id = multibot.maps.GatewayRoad.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000006ar);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000002fr;
		eventName = "TheInfiniteLoop";

		raceTime = 14;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(11500);
		rating = 45;

		carVendor = "Naxas";

		carClass = CAR_CLASS_A;
		drivetype = DRIVETYPE_RWD;

		useAwards = 1;

		fee = 2000;

		times[0] = time;
		times[1] = 192.3;
		times[2] = 205.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = "Club rating: " + rating + " or higher";
		reqText[2] = "Naturally aspirated engines only";

		fogData[0] = -100.0;
		fogData[1] = 125.0;

		super.init();

		specialSlots[0] = laps;
		specialSlots[1] = times[0];
		specialSlots[2] = times[1];
		specialSlots[3] = times[2];
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkVendor(carVendor)) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;

			case 3:
			if(!checkForcedInduction()) return 1;
			break;
		}

		return 0;
	}
}
