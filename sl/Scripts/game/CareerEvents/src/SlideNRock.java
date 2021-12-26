package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class SlideNRock extends CareerEvent
{
	int laps = 2;
	int score = 30000;
	int mass = 1300;

	int[] scores = new int[3];

	public SlideNRock();

	public void init()
	{
		track_data_id = multibot.maps.Vallelunga.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000056r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000001br;
		eventName = "Slide N Rock";

		raceTime = 11;

		gamemode_id = GAMEMODE_DRIFT;

		setPrize(2500);

		useAwards = 1;

		fee = 500;

		scores[0] = score;
		scores[1] = 20000;
		scores[2] = 15000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target score: " + score;
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Vehicle mass: " + mass + "kg max";

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
		}

		return 0;
	}
}
