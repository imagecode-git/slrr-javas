package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TheSpeedCyndicate extends CareerEvent
{
	int laps = 1;
	float time = 162.0;
	int mass = 1500;

	float[] times = new float[3];

	public TheSpeedCyndicate();

	public void init()
	{
		track_data_id = multibot.maps.SPAFrankoohapsGPCircuit.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000060r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000025r;
		eventName = "The Speed Cyndicate";

		raceTime = 16;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(3000);
		rating = 35;

		drivetype = DRIVETYPE_AWD;

		minPower = 150;
		maxPower = 210;

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;

		times[0] = time;
		times[1] = 167.5;
		times[2] = 174.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();

		reqText[0] = powerToString();
		reqText[1] = "Vehicle mass: " + mass + "kg max";
		reqText[2] = "Club rating: " + rating + " or higher";

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
			if(checkPower()) return 1;
			break;

			case 1:
			if(checkMass(mass)) return 1;
			break;

			case 2:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
