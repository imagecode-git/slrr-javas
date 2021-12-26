package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class PowerInTheDark extends CareerEvent
{
	public PowerInTheDark();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000068r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000002dr;
		eventName = "Power In The Dark";

		raceTime = 4;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(51));	//SnakeR

		setPrize(10000);
		rocWins = 1;

		carClass = CAR_CLASS_S;
		drivetype = DRIVETYPE_AWD;

		fee = 2000;

		conditionText[0] = prizeToString(); //prize description
		conditionText[1] = feeToString();

		reqText[0] = "Wins in ROC: " + rocWins;

		super.init();
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkROC()) return 1;
			break;
		}

		return 0;
	}
}
