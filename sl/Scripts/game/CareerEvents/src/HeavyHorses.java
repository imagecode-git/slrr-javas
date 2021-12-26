package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class HeavyHorses extends CareerEvent
{
	public HeavyHorses();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000058r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000001dr;
		eventName = "Heavy Horses";

		raceTime = 5;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(33));	//Scorpion

		setPrize(createPart(parts.engines.GMC_Nissan_Honda_Hyundai_Opel:0x0000D085r)); //V6 turbocharger

		carVendor = "Stallion";

		carClass = CAR_CLASS_B;

		conditionText[0] = prizeToString(); //prize description

		reqText[0] = "Brand: " + carVendor;

		fogData[0] = -100.0;
		fogData[1] = 125.0;

		super.init();
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkVendor(carVendor)) return 1;
			break;
		}

		return 0;
	}
}
