package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class QuatermastersChallenge extends CareerEvent
{
	public QuatermastersChallenge();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000064r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000029r;
		eventName = "Quatermasters Challenge";

		raceTime = 4;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(18));	//Hmmm

		setPrize(createPart(parts.engines.Lama_Performance_V12:0x00000BD0r)); //Lama V12 N2O system
		rating = 30;

		carClass = CAR_CLASS_A;
		drivetype = DRIVETYPE_RWD;

		fee = 1000;

		conditionText[0] = prizeToString(); //prize description
		conditionText[1] = feeToString();

		reqText[0] = "Club rating: " + rating + " or higher";
		reqText[1] = "No nitrous";

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
