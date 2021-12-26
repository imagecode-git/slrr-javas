package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class NostalgiaRun extends CareerEvent
{
	int laps = 4;
	float time = 381.275;

	public NostalgiaRun();

	public void init()
	{
		track_data_id = multibot.maps.LagunaSeca.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000006dr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000032r;
		eventName = "Nostalgia Run";

		raceTime = 4;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(13000);
		rating = 20;

		carVendor = "Prime";

		carClass = CAR_CLASS_C;

		useFines = 1;
		cleanDriving = 1;

		fee = 1000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();
		conditionText[4] = getCleanDriving();

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[0] = laps;
		specialSlots[1] = time;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkVendor(carVendor)) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
