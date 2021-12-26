package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class SupersonicBlast extends CareerEvent
{
	int laps = 5;
	float time = 381.333;

	float[] times = new float[3];

	public SupersonicBlast();

	public void init()
	{
		track_data_id = multibot.maps.Vallelunga.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000073r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000038r;
		eventName = "Supersonic Blast";

		raceTime = 18;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(25000);
		rocWins = 2;

		carClass = CAR_CLASS_S;

		carModel = "Whisper Q1000XL";

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;

		fee = 1000;

		times[0] = time;
		times[1] = 386.5;
		times[2] = 393.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();
		conditionText[4] = getCleanDriving();

		reqText[0] = "Car model: " + carModel;
		reqText[1] = "Wins in ROC: " + rocWins;

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
			if(checkROC()) return 1;
			break;
		}

		return 0;
	}
}
