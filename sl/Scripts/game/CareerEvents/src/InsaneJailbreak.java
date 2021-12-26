package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class InsaneJailbreak extends CareerEvent
{
	int maxScouts = 8;

	public InsaneJailbreak();

	public void init()
	{
		track_data_id = multibot.maps.ValoCity.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000066r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000002br;
		eventName = "Insane Jailbreak";

		raceTime = 11;

		gamemode_id = GAMEMODE_COP;

		setPrize(9000);
		rating = 40;

		copDensity = 6.0;

		conditionText[0] = prizeToString(); //prize description

		reqText[0] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[4] = maxScouts;

		camPathCustom = new Pori[4];
		camPathCustom[0] = new Pori(new Vector3(-306.894,23.480,609.052), new Ypr(-1.713,-0.043,-0.001));
		camPathCustom[1] = new Pori(new Vector3(-276.436,22.130,613.439), new Ypr(-1.713,-0.043,-0.001));
		camPathCustom[2] = new Pori(new Vector3(-247.628,12.778,613.750), new Ypr(-1.974,-0.075,0.002));
		camPathCustom[3] = new Pori(new Vector3(-227.529,11.088,623.803), new Ypr(-1.974,-0.075,0.002));
		camSpeedMulCustom = 7.0;

		posStartCustom = new Vector3(-216.070,9.698,627.419);
		oriStartCustom = new Ypr(-1.561,-0.007,-0.000);
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