package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class ProfessionalsCup extends CareerEvent
{
	int laps = 10;

	public ProfessionalsCup();

	public void init()
	{
		track_data_id = multibot.maps.A1Ring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000076r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000003br;
		eventName = "Professionals Cup";

		raceTime = 18;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = findRacers(1,10,5); //TOP10

		/*
		specialVehicles = new int[3];
		specialVehicles[0] = cars.racers.Baiern:0x000000F8r;
		specialVehicles[1] = cars.racers.Nonus:0x00000157r;
		specialVehicles[2] = cars.racers.Einvagen:0x00000157r;
		*/

		specialVehiclePath = "save/cars/special/DTM/";

		setPrize(30000);
		rocWins = 1;

		drivetype = DRIVETYPE_RWD;

		minPower = 400;
		maxPower = 650;

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;
		randomStartGrid = 1;

		fee = 2500;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		reqText[0] = powerToString();
		reqText[1] = "Wins in ROC: " + rocWins;
		reqText[2] = "Transmission: manual";
		reqText[3] = "No nitrous";

		super.init();

		specialSlots[0] = laps;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkPower()) return 1;
			break;

			case 1:
			if(checkROC()) return 1;
			break;

			case 2:
			if(checkTransmission(TRANSMISSION_MANUAL) || checkTransmission(TRANSMISSION_MANUAL_C)) return 1;
			break;

			case 3:
			if(!checkNitrous()) return 1;
			break;
		}

		return 0;
	}
}
