package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class EClassCup extends CareerEvent
{
	int laps = 2;

	public EClassCup();

	public void init()
	{
		track_data_id = multibot.maps.HockenheimGP.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000003fr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000004r;
		eventName = "E Class Cup";

		raceTime = 11;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(8));	//Yjin
		botData.addElement(new Integer(3));	//HKS
		botData.addElement(new Integer(2));	//Smertokog
		botData.addElement(new Integer(1));	//DomenicAkab
		botData.addElement(new Integer(0));	//EVO

		setPrize(500);

		carClass = CAR_CLASS_E;

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;

		fee = 100;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		super.init();

		specialSlots[0] = laps;
	}

	public int reqCheck(int reqId)
	{
		return 1;
	}
}
