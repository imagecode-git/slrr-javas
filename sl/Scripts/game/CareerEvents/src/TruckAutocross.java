package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TruckAutocross extends CareerEvent
{
	int laps = 1;
	float time = 130.0;
	int mass = 2500;

	float[] times = new float[3];

	public TruckAutocross();

	public void init()
	{
		track_data_id = multibot.maps.NationalDirtRaceway.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000051r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000016r;
		eventName = "Truck Autocross";

		raceTime = 4;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(5000);
		rating = 40;

		carVendor = "SuperDuty";

		useAwards = 1;

		times[0] = time;
		times[1] = 133.0;
		times[2] = 140.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = "Vehicle mass: " + mass + "kg max";
		reqText[2] = "Club rating: " + rating + " or higher";

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
			if(checkMass(mass)) return 1;
			break;

			case 2:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
