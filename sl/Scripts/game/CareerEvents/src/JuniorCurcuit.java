package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class JuniorCurcuit extends CareerEvent
{
	int laps = 2;

	public JuniorCurcuit();

	public void init()
	{
		track_data_id = multibot.maps.Norisring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000003er);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000003r;
		eventName = "Junior Curcuit";

		raceTime = 16;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(0));	//EVO
		botData.addElement(new Integer(1));	//DomenicAkab
		botData.addElement(new Integer(2));	//Smertokog
		botData.addElement(new Integer(3));	//HKS

		setPrize(500);

		maxPower = 100;

		useFines = 0;
		useAwards = 1;

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
