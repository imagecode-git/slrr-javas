package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class SlowMotionStorm extends CareerEvent
{
	public SlowMotionStorm();

	public void init()
	{
		track_data_id = multibot.maps.DragStrip.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000041r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000006r;
		eventName = "Slow Motion Storm";

		raceTime = 4;

		gamemode_id = GAMEMODE_DRAG;

		botData = new Vector();
		botData.addElement(new Integer(8));	//Yjin

		setPrize(250);

		carClass = CAR_CLASS_E;

		conditionText[0] = prizeToString(); //prize description

		super.init();
	}

	public int reqCheck(int reqId)
	{
		return 1;
	}
}
