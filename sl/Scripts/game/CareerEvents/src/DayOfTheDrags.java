package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class DayOfTheDrags extends CareerEvent
{
	public DayOfTheDrags();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000053r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000018r;
		eventName = "Day Of The Drags";

		raceTime = 7;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(26));	//Shv3d

		setPrize(2000);
		rating = 10;

		drivetype = DRIVETYPE_AWD;

		minPower = 210;
		maxPower = 300;

		conditionText[0] = prizeToString(); //prize description

		reqText[0] = powerToString();
		reqText[1] = "Club rating: " + rating + " or higher";

		super.init();
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkPower()) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
