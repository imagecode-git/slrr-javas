package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class OranParkSuperlap extends CareerEvent
{
	int laps = 1;
	float time = 92.0;

	public OranParkSuperlap();

	public void init()
	{
		track_data_id = multibot.maps.OranPark.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000043r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000008r;
		eventName = "OranPark Superlap";

		raceTime = 14;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(createPart(parts.interior:0x0000004Dr)); //SL Tuners Torino front seat
		rating = 5;

		carClass = CAR_CLASS_E;

		useFines = 0;
		useAwards = 0;

		fee = 150;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		super.init();

		specialSlots[0] = laps;
		specialSlots[1] = time;
	}

	public int reqCheck(int reqId)
	{
		return 1;
	}
}
