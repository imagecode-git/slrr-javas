package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class KingsVersusKings extends CareerEvent
{
	int laps = 20;

	public KingsVersusKings();

	public void init()
	{
		track_data_id = multibot.maps.Zandvoort.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000078r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000003dr;
		eventName = "Kings Versus Kings";

		raceTime = 1;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = findRacers(1,10,5); //TOP10

		specialVehiclePath = "save/cars/special/DTM/";

		setPrize(500000);
		rocWins = 3;

		drivetype = DRIVETYPE_RWD;

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;
		randomStartGrid = 1;

		fee = 20000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		reqText[0] = "Wins in ROC: " + rocWins;
		reqText[1] = "Transmission: manual";
		reqText[2] = "No nitrous";

		super.init();

		specialSlots[0] = laps;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkROC()) return 1;
			break;

			case 1:
			if(checkTransmission(TRANSMISSION_MANUAL) || checkTransmission(TRANSMISSION_MANUAL_C)) return 1;
			break;

			case 2:
			if(!checkNitrous()) return 1;
			break;
		}

		return 0;
	}
}
