package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class LongShot extends CareerEvent
{
	int maxScouts = 6;

	public LongShot();

	public void init()
	{
		track_data_id = multibot.maps.ValoCity.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000059r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000001er;
		eventName = "Long Shot";

		raceTime = 17;

		gamemode_id = GAMEMODE_COP;

		setPrize(5000);
		rating = 20;

		copDensity = 5.0;

		conditionText[0] = prizeToString(); //prize description

		reqText[0] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[4] = maxScouts;

		camPathCustom = new Pori[4];
		camPathCustom[0] = new Pori(new Vector3(-865.484,16.176,-400.118), new Ypr(-2.348,-0.036,0.001));
		camPathCustom[1] = new Pori(new Vector3(-840.805,14.612,-370.624), new Ypr(-2.353,-0.025,0.002));
		camPathCustom[2] = new Pori(new Vector3(-824.081,14.565,-330.331), new Ypr(-1.965,-0.081,0.003));
		camPathCustom[3] = new Pori(new Vector3(-796.151,13.546,-317.379), new Ypr(-2.053,-0.133,0.001));
		camSpeedMulCustom = 6.5;

		posStartCustom = new Vector3( -782.237,11.169,-305.802);
		oriStartCustom = new Ypr(-2.598,-0.033,-0.000);
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}