package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class OffroadTimeChallenge extends CareerEvent
{
	int laps = 1;
	float time = 247.0;

	float[] times = new float[3];

	public OffroadTimeChallenge();

	public void init()
	{
		track_data_id = multibot.maps.UKRallyStage3.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000069r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000002er;
		eventName = "Offroad Time Challenge";

		raceTime = 17;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(8000);
		rating = 15;

		carVendors = new String[2];
		carVendors[0] = "Enula";
		carVendors[1] = "Focer";

		carClass = CAR_CLASS_B;

		useAwards = 1;

		fee = 100;

		times[0] = time;
		times[1] = 255.0;
		times[2] = 267.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Brands: " + carVendors[0] + ", " + carVendors[1];
		reqText[1] = "Club rating: " + rating + " or higher";
		reqText[2] = "Transmission: manual";

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
			if(checkVendor(carVendors)) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;

			case 2:
			if(checkTransmission(TRANSMISSION_MANUAL) || checkTransmission(TRANSMISSION_MANUAL_C)) return 1;
			break;
		}

		return 0;
	}
}
