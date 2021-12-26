package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class NightShift extends CareerEvent
{
	int laps = 3;
	int score = 30000;

	int[] scores = new int[3];

	public NightShift();

	public void init()
	{
		track_data_id = multibot.maps.Norisring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000055r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000001ar;
		eventName = "Night Shift";

		raceTime = 3;

		gamemode_id = GAMEMODE_DRIFT;

		setPrize(3000);
		rating = 5;

		carClass = CAR_CLASS_C;
		drivetype = DRIVETYPE_RWD;

		useAwards = 1;

		fee = 500;

		scores[0] = score;
		scores[1] = 20000;
		scores[2] = 15000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target score: " + score;
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Club rating: " + rating + " or higher";

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
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
