package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class RemoTestDrive extends CareerEvent
{
	int laps = 2;
	float time = 210.0;
	int mass = 1950;

	float[] times = new float[3];

	public RemoTestDrive();

	public void init()
	{
		track_data_id = multibot.maps.A1Ring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000044r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000009r;
		eventName = "Remo Test Drive";

		raceTime = 18;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(4000);

		carModel = "Remo GTi";

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;

		times[0] = time;
		times[1] = 215.0;
		times[2] = 227.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();

		reqText[0] = "Car model: " + carModel;
		reqText[1] = "Vehicle mass: " + mass + "kg max";

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

			case 1:
			if(checkMass(mass)) return 1;
			break;
		}

		return 0;
	}
}
