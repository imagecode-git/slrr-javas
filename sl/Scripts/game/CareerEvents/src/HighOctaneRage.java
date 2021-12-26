package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class HighOctaneRage extends CareerEvent
{
	public HighOctaneRage();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000072r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000037r;
		eventName = "High Octane Rage";

		raceTime = 4;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(57));	//Lenipenb

		setPrize(15000);
		rating = 40;

		carClass = CAR_CLASS_S;

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