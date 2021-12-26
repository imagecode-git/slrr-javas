package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class SafetyDirect extends CareerEvent
{
	int laps = 1;
	float time = 150.0;

	float[] times = new float[3];

	public SafetyDirect();

	public void init()
	{
		track_data_id = multibot.maps.Zandvoort.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000040r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000005r;
		eventName = "Safety Direct";

		raceTime = 11;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(600);
		rating = 5;

		carClass = CAR_CLASS_E;

		useFines = 0;
		useAwards = 1;

		times[0] = time;
		times[1] = 155.0;
		times[2] = 165.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description

		reqText[0] = "No nitrous";
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
			if(!checkNitrous()) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
