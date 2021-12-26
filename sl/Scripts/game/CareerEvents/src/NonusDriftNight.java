package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class NonusDriftNight extends CareerEvent
{
	int laps = 3;
	int score = 30000;

	int[] scores = new int[3];

	public NonusDriftNight();

	public void init()
	{
		track_data_id = multibot.maps.Nurburgring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000052r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000017r;
		eventName = "Nonus Drift Night";

		raceTime = 3;

		gamemode_id = GAMEMODE_DRIFT;

		setPrize(4000);
		rating = 10;

		drivetype = DRIVETYPE_RWD;
		
		carVendor = "Nonus";

		minPower = 200;
		maxPower = 350;

		useAwards = 1;

		scores[0] = score;
		scores[1] = 20000;
		scores[2] = 15000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target score: " + score;
		conditionText[2] = prizeToString(); //prize description

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = powerToString();
		reqText[2] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[0] = laps;
		specialSlots[4] = scores[0];
		specialSlots[5] = scores[1];
		specialSlots[6] = scores[2];
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
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
