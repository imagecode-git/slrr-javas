package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TruckDemolition extends CareerEvent
{
	int laps = 3;
	int score = 30000;

	int[] scores = new int[3];

	public TruckDemolition();

	public void init()
	{
		track_data_id = multibot.maps.HockenheimGP.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000005fr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000024r;
		eventName = "Truck Demolition";

		raceTime = 13;

		gamemode_id = GAMEMODE_DRIFT;

		setPrize(6000);

		carVendor = "SuperDuty";

		drivetype = DRIVETYPE_RWD;

		minPower = 250;
		maxPower = 400;

		useAwards = 1;

		fee = 1000;

		scores[0] = score;
		scores[1] = 20000;
		scores[2] = 15000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target score: " + score;
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = powerToString();

		fogData[0] = -100.0;
		fogData[1] = 125.0;

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
			if(checkVendor(carVendor)) return 1;
			break;

			case 1:
			if(checkPower()) return 1;
			break;
		}

		return 0;
	}
}