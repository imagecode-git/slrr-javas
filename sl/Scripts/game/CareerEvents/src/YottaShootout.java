package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class YottaShootout extends CareerEvent
{
	public YottaShootout();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000004dr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000012r;
		eventName = "Yotta Shootout";

		raceTime = 3;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(13));	//Gangstaog

		setPrize(1250);

		carVendor = "Yotta";

		carClass = CAR_CLASS_D;

		fee = 150;

		conditionText[0] = prizeToString(); //prize description
		conditionText[1] = feeToString();

		reqText[0] = "Brand: " + carVendor;

		fogData[0] = -100.0;
		fogData[1] = 125.0;

		super.init();
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkVendor(carVendor)) return 1;
			break;
		}

		return 0;
	}
}
