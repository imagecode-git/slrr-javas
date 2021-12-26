package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class BurningTyres extends CareerEvent
{
	public BurningTyres();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000062r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000027r;
		eventName = "Burning Tyres";

		raceTime = 3;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(38));	//h00die

		setPrize(6000);
		rating = 40;

		carClass = CAR_CLASS_A;
		drivetype = DRIVETYPE_RWD;

		conditionText[0] = prizeToString(); //prize description

		reqText[0] = "Transmission: manual";
		reqText[1] = "Club rating: " + rating + " or higher";

		super.init();
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkTransmission(TRANSMISSION_MANUAL) || checkTransmission(TRANSMISSION_MANUAL_C)) return 1;
			break;

			case 1:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
