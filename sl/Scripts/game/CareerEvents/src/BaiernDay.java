package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class BaiernDay extends CareerEvent
{
	int laps = 1;
	float time = 112.0;

	public BaiernDay();

	public void init()
	{
		track_data_id = multibot.maps.Nurburgring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000004er);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000013r;
		eventName = "Baiern Day";

		raceTime = 15;

		gamemode_id = GAMEMODE_TIMEATTACK;

		setPrize(createPart(parts.engines.Baiern_Emer:0x00000051r)); //SL Tuners 6 channel N2O System
		rating = 15;

		carClass = CAR_CLASS_D;

		useFines = 1;
		cleanDriving = 1;

		carVendors = new String[2];
		carVendors[0] = "CoupeSport";
		carVendors[1] = "DevilSport";

		fee = 300;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = "Target time: " + String.timeToString(time, String.TCF_NOHOURS);
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		reqText[0] = "Brands: " + carVendors[0] + ", " + carVendors[1];
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
			if(checkVendor(carVendors)) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
