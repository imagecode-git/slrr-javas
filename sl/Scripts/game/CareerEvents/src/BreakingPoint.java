package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class BreakingPoint extends CareerEvent
{
	int laps = 2;
	int score = 30000;
	int mass = 1500;

	int[] scores = new int[3];

	public BreakingPoint();

	public void init()
	{
		track_data_id = multibot.maps.Zandvoort.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000005er);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000023r;
		eventName = "Breaking Point";

		raceTime = 5;

		gamemode_id = GAMEMODE_DRIFT;

		setPrize(4000);
		rating = 20;

		carClass = CAR_CLASS_B;
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
		reqText[2] = "Transmission: manual";

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

			case 2:
			if(checkTransmission(TRANSMISSION_MANUAL) || checkTransmission(TRANSMISSION_MANUAL_C)) return 1;
			break;
		}

		return 0;
	}
}
