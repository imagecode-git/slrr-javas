package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TheInvasion extends CareerEvent
{
	int laps = 3;
	int score = 30000;

	int[] scores = new int[3];

	public TheInvasion();

	public void init()
	{
		track_data_id = multibot.maps.Nurburgring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000063r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000028r;
		eventName = "The Invasion";

		raceTime = 14;

		gamemode_id = GAMEMODE_DRIFT;

		setPrize(9000);
		rating = 45;

		carClass = CAR_CLASS_B;

		useAwards = 1;

		scores[0] = score;
		scores[1] = 20000;
		scores[2] = 15000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target score: " + score;
		conditionText[2] = prizeToString(); //prize description

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