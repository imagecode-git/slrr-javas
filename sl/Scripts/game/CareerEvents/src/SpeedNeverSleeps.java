package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class SpeedNeverSleeps extends CareerEvent
{
	int laps = 3;
	float time = 141.200;

	public SpeedNeverSleeps();

	public void init()
	{
		track_data_id = multibot.maps.Norisring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000005cr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000021r;
		eventName = "Speed Never Sleeps";

		raceTime = 15;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(createSet(parts.engines.Dodge_BMW_Racing_V10:0x0000A3B9r)); //V10 605CID stroker kit

		carClass = CAR_CLASS_A;

		useFines = 1;
		cleanDriving = 1;

		fee = 100;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		reqText[0] = "No nitrous";

		super.init();

		specialSlots[0] = laps;
		specialSlots[1] = time;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(!checkNitrous()) return 1;
			break;
		}

		return 0;
	}
}
