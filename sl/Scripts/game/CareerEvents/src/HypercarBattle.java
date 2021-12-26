package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class HypercarBattle extends CareerEvent
{
	public HypercarBattle();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000074r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000039r;
		eventName = "Hypercar Battle";

		raceTime = 5;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(46));	//De3aP

		setPrize(14500);
		rating = 50;

		drivetype = DRIVETYPE_RWD;

		minPower = 1000;
		maxPower = 1500;

		fee = 3500;

		conditionText[0] = prizeToString(); //prize description
		conditionText[1] = feeToString();

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
