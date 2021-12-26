package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class HotCityNights extends CareerEvent
{
	int laps = 2;
	float time = 105.5;

	float[] times = new float[3];

	public HotCityNights();

	public void init()
	{
		track_data_id = multibot.maps.Norisring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000048r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000000dr;
		eventName = "Hot City Nights";

		raceTime = 3;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(1500);
		rating = 20;

		useAwards = 1;

		times[0] = time;
		times[1] = 110.0;
		times[2] = 118.0;

		carClass = CAR_CLASS_D;
		drivetype = DRIVETYPE_AWD;

		fee = 500;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Only street legal vehicles";
		reqText[1] = "Club rating: " + rating + " or higher";

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
			if(checkStreetLegal()) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
