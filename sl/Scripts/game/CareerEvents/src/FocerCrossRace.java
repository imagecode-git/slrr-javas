package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class FocerCrossRace extends CareerEvent
{
	int laps = 1;
	float time = 247.335;

	float[] times = new float[3];

	public FocerCrossRace();

	public void init()
	{
		track_data_id = multibot.maps.UKRallyStage1.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000065r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000002ar;
		eventName = "Focer Cross Race";

		raceTime = 11;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(7000);

		carClass = CAR_CLASS_A;

		carModel = "Focer WRC";

		useAwards = 1;

		fee = 500;

		times[0] = time;
		times[1] = 262.5;
		times[2] = 268.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Car model: " + carModel;

		fogData[0] = -100.0;
		fogData[1] = 125.0;

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
		}

		return 0;
	}
}
