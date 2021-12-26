package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class AmateurDriftCup extends CareerEvent
{
	int laps = 1;
	int score = 30000;
	int mass = 1400;

	int[] scores = new int[3];

	public AmateurDriftCup();

	public void init()
	{
		track_data_id = multibot.maps.A1Ring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000004cr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000011r;
		eventName = "Amateur Drift Cup";

		raceTime = 15;

		gamemode_id = GAMEMODE_DRIFT;

		setPrize(1000);
		rating = 15;

		carClass = CAR_CLASS_C;
		drivetype = DRIVETYPE_RWD;

		useAwards = 1;

		scores[0] = score;
		scores[1] = 20000;
		scores[2] = 15000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target score: " + score;
		conditionText[2] = prizeToString(); //prize description

		reqText[0] = "Vehicle mass: " + mass + "kg max";
		reqText[1] = "Club rating: " + rating + " or higher";

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
			if(checkMass(mass)) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
