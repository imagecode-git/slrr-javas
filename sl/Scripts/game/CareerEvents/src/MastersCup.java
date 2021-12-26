package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class MastersCup extends CareerEvent
{
	int laps = 7;

	public MastersCup();

	public void init()
	{
		track_data_id = multibot.maps.A1Ring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000006br);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000030r;
		eventName = "Masters Cup";

		raceTime = 15;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(53));	//Draeghonov
		botData.addElement(new Integer(56));	//Singh
		botData.addElement(new Integer(54));	//Fuzzy

		setPrize(30000);
		rocWins = 1;

		carClass = CAR_CLASS_S;

		useFines = 1;
		useAwards = 1;
		cleanDriving = 1;
		randomStartGrid = 1;

		fee = 2000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		reqText[0] = "Wins in ROC: " + rocWins;

		super.init();

		specialSlots[0] = laps;
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
