package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class LockedAndLoaded extends CareerEvent
{
	int laps = 2;
	int score = 30000;

	int[] scores = new int[3];

	public LockedAndLoaded();

	public void init()
	{
		track_data_id = multibot.maps.SPAFrankoohapsGPCircuit.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000006cr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000031r;
		eventName = "Locked And Loaded";

		raceTime = 9;

		gamemode_id = GAMEMODE_DRIFT;

		setPrize(15000);
		rocWins = 1;

		drivetype = DRIVETYPE_RWD;

		useAwards = 1;

		fee = 2500;

		scores[0] = score;
		scores[1] = 20000;
		scores[2] = 15000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target score: " + score;
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Transmission: manual";
		reqText[1] = "Wins in ROC: " + rocWins;

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
			if(checkTransmission(TRANSMISSION_MANUAL) || checkTransmission(TRANSMISSION_MANUAL_C)) return 1;
			break;

			case 1:
			if(checkROC()) return 1;
			break;
		}

		return 0;
	}
}