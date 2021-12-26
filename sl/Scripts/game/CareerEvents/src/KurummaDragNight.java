package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class KurummaDragNight extends CareerEvent
{
	public KurummaDragNight();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000004fr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000014r;
		eventName = "Kurumma Drag Night";

		raceTime = 1;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(34));	//4lligator

		setPrize(createPart(parts.engines.GMC_Nissan_Honda_Hyundai_Opel:0x0000AE95r)); //SL Tuners 6ch methanol fuel rail (V6)

		carVendor = "Kurumma";

		carClass = CAR_CLASS_C;

		fee = 500;

		conditionText[0] = prizeToString(); //prize description
		conditionText[1] = feeToString();

		reqText[0] = "Brand: " + carVendor;
		reqText[1] = "No nitrous";

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

			case 1:
			if(!checkNitrous()) return 1;
			break;
		}

		return 0;
	}
}
