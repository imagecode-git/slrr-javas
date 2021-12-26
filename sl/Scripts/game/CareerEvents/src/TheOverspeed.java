package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TheOverspeed extends CareerEvent
{
	int laps = 2;
	float time = 187.680;

	float[] times = new float[3];

	public TheOverspeed();

	public void init()
	{
		track_data_id = multibot.maps.HockenheimGP.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000006fr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000034r;
		eventName = "The Overspeed";

		raceTime = 13;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(20000);
		rocWins = 1;

		carClass = CAR_CLASS_S;
		drivetype = DRIVETYPE_RWD;

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;

		fee = 2000;

		times[0] = time;
		times[1] = 193.5;
		times[2] = 200.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();
		conditionText[4] = getCleanDriving();

		reqText[0] = "Wins in ROC: " + rocWins;

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
			if(checkROC()) return 1;
			break;
		}

		return 0;
	}
}
