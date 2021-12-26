package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class VintageDragStars extends CareerEvent
{
	public VintageDragStars();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x0000005dr);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000022r;
		eventName = "Vintage Drag Stars";

		raceTime = 2;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(47));	//Sannex

		setPrize(15000);
		rocWins = 1;

		carVendors = new String[2];
		carVendors[0] = "Badge";
		carVendors[1] = "MC";

		carClass = CAR_CLASS_A;

		conditionText[0] = prizeToString(); //prize description

		reqText[0] = "Brands: " + carVendors[0] + ", " + carVendors[1];
		reqText[1] = "Wins in ROC: " + rocWins;
		reqText[2] = "No nitrous";

		super.init();
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkVendor(carVendors)) return 1;
			break;

			case 1:
			if(checkROC()) return 1;
			break;

			case 2:
			if(!checkNitrous()) return 1;
			break;
		}

		return 0;
	}
}
