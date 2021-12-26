package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class FurranoRush extends CareerEvent
{
	int laps = 1;
	float time = 215.0;

	float[] times = new float[3];

	public FurranoRush();

	public void init()
	{
		track_data_id = multibot.maps.UKRallyStage2.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000067r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000002cr;
		eventName = "Furrano Rush";

		raceTime = 5;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(15000);
		rating = 45;

		carClass = CAR_CLASS_A;

		carModel = "Furrano GT54";

		useAwards = 1;

		fee = 300;

		times[0] = time;
		times[1] = 221.0;
		times[2] = 226.333;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Car model: " + carModel;
		reqText[1] = "No nitrous";
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
			if(checkModel(carModel)) return 1;
			break;

			case 1:
			if(!checkNitrous()) return 1;
			break;

			case 2:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
