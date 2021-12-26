package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TotalDomination extends CareerEvent
{
	public TotalDomination();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000071r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000036r;
		eventName = "Total Domination";

		raceTime = 3;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(50));	//MobyDik

		setPrize(10000);
		rating = 45;

		carClass = CAR_CLASS_S;
		drivetype = DRIVETYPE_RWD;

		conditionText[0] = prizeToString(); //prize description

		reqText[0] = "Club rating: " + rating + " or higher";
		reqText[1] = "No nitrous";

		fogData[0] = -100.0;
		fogData[1] = 125.0;

		super.init();
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{

			case 0:
			if(checkRating(rating)) return 1;
			break;

			case 1:
			if(!checkNitrous()) return 1;
			break;
		}

		return 0;
	}
}
