package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class MayhemAtSandown extends CareerEvent
{
	int laps = 1;
	float time = 65.0;

	float[] times = new float[3];

	public MayhemAtSandown();

	public void init()
	{
		track_data_id = multibot.maps.Sandown.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000054r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000019r;
		eventName = "Mayhem At Sandown";

		raceTime = 5;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(3500);

		carClass = CAR_CLASS_C;
		drivetype = DRIVETYPE_FWD;

		useAwards = 1;

		times[0] = time;
		times[1] = 71.0;
		times[2] = 18.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description

		super.init();

		specialSlots[0] = laps;
		specialSlots[1] = times[0];
		specialSlots[2] = times[1];
		specialSlots[3] = times[2];
	}

	public int reqCheck(int reqId)
	{
		return 1;
	}
}
