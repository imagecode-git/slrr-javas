package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class GatewayRoadEndurance extends CareerEvent
{
	int laps = 20;

	public GatewayRoadEndurance();

	public void init()
	{
		track_data_id = multibot.maps.GatewayRoad.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000077r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000003cr;
		eventName = "Gateway Road Endurance";

		raceTime = 17;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = findRacers(1,10,6); //TOP10

		specialVehiclePath = "save/cars/special/DTM/";

		setPrize(35000);

		drivetype = DRIVETYPE_RWD;

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;

		fee = 1250;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		reqText[0] = "No nitrous";

		super.init();

		specialSlots[0] = laps;
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
