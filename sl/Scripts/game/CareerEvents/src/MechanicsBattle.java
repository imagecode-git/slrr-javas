package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class MechanicsBattle extends CareerEvent
{
	int laps = 10;
	int mass = 1800;

	public MechanicsBattle();

	public void init()
	{
		track_data_id = multibot.maps.A1Ring.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000057r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000001cr;
		eventName = "Mechanics Battle";

		raceTime = 13;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(43));	//Drifter
		botData.addElement(new Integer(40));	//Skon
		botData.addElement(new Integer(27));	//VOVA

		setPrize(createSet(parts.engines.MC_Prime_SuperDuty:0x000060B9r)); //DLH engine kit

		drivetype = DRIVETYPE_RWD;

		maxPower = 500;

		useFines = 1;
		useAwards = 0;
		randomStartGrid = 1;

		fee = 600;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = feeToString();

		reqText[0] = powerToString();
		reqText[1] = "Vehicle mass: " + mass + "kg max";
		reqText[2] = "Transmission: manual";
		reqText[3] = "No nitrous";

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

			case 1:
			if(checkMass(mass)) return 1;
			break;

			case 2:
			if(checkTransmission(TRANSMISSION_MANUAL) || checkTransmission(TRANSMISSION_MANUAL_C)) return 1;
			break;

			case 3:
			if(!checkNitrous()) return 1;
			break;
		}

		return 0;
	}
}
