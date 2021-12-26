package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class TurtleRace extends CareerEvent
{
	int laps = 2;

	public TurtleRace();

	public void init()
	{
		track_data_id = multibot.maps.OultonPark.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000042r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000007r;
		eventName = "Turtle Race";

		raceTime = 17;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(0));	//EVO
		botData.addElement(new Integer(2));	//Smertokog

		setPrize(400);

		carClass = CAR_CLASS_E;

		useFines = 0;
		useAwards = 1;

		minPower = 75;
		maxPower = 120;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description

		reqText[0] = powerToString();

		super.init();

		specialSlots[0] = laps;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkPower()) return 1;
			break;
		}

		return 0;
	}
}
