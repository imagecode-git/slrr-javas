package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;

public class TimeAttackTest extends CareerEvent
{
	int laps = 1;
	float time = 200.0;

	float[] times = new float[3];

	public TimeAttackTest(){}

	public void init()
	{
//		track_data_id = multibot.maps.GatewayRoad.t_data:0x0102r;
		track_data_id = multibot.maps.UKRallyStage2.t_data:0x0102r;
		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.old_stdpack_231:0x0009r);
		event_id = sl.Scripts.game.CareerEvents.old_stdpack_231:0x0005r;
		eventName = "TimeAttack Test";

		gamemode_id = GAMEMODE_TIMEATTACK;
		carClass = CAR_CLASS_C;

//		setPrize(new Vehicle( GameLogic.player, cars.racers.Furrano:0x00000006r, 1.0, 1.0, 1.0, 1.0, 1.0 ));
		setPrize(5000);
		rating = 35;

		useFines = 0;
		useAwards = 1;
		cleanDriving = 0;
		useCamAnimation = 0;

		raceTime = 9;

		times[0] = time;
		times[1] = 85.0;
		times[2] = 90.0;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();

		reqText[0] = "No nitrous";
		reqText[1] = "Club rating: " + rating + " or higher";

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
			if(!checkNitrous()) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}