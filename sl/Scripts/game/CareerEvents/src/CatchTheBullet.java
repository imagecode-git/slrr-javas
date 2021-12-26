package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class CatchTheBullet extends CareerEvent
{
	public CatchTheBullet();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000075r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000003ar;
		eventName = "Catch The Bullet";

		raceTime = 1;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(56));	//Singh

		setPrize(30000);
		rocWins = 2;

		carClass = CAR_CLASS_S;

		fee = 2000;

		conditionText[0] = prizeToString(); //prize description
		conditionText[1] = feeToString();

		reqText[0] = "Transmission: manual";
		reqText[1] = "Wins in ROC: " + rocWins;
		reqText[2] = "Naturally aspirated engines only";

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
			if(checkROC()) return 1;
			break;

			case 2:
			if(!checkForcedInduction()) return 1;
			break;
		}

		return 0;
	}
}