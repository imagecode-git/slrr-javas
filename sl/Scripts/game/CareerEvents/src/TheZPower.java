package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TheZPower extends CareerEvent
{
	public TheZPower();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000049r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000000er;
		eventName = "The Z Power";

		raceTime = 2;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(5));	//Boost

		setPrize(1500);
		rating = 5;

		carVendor = "Zed";

		carClass = CAR_CLASS_D;

		fee = 100;

		conditionText[0] = prizeToString(); //prize description
		conditionText[1] = feeToString();

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = "Club rating: " + rating + " or higher";

		super.init();
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
