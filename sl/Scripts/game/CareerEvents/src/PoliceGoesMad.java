package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class PoliceGoesMad extends CareerEvent
{
	int maxScouts = 5;

	public PoliceGoesMad();

	public void init()
	{
		track_data_id = multibot.maps.ValoCity.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000045r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x0000000ar;
		eventName = "Police Goes Mad";

		raceTime = 14;

		gamemode_id = GAMEMODE_COP;

		setPrize(2000);
		rating = 5;

		copDensity = 4.0;

		conditionText[0] = prizeToString(); //prize description

		reqText[0] = "Club rating: " + rating + " or higher";

		super.init();

		specialSlots[4] = maxScouts;

		camPathCustom = new Pori[4];
		camPathCustom[0] = new Pori(new Vector3(605.006,11.863,-540.584), new Ypr( 0.351,-0.018,0.000));
		camPathCustom[1] = new Pori(new Vector3(612.756,11.838,-546.575), new Ypr( 0.849,-0.026,0.000));
		camPathCustom[2] = new Pori(new Vector3(590.446,12.420,-565.167), new Ypr(-2.456,-0.066,0.002));
		camPathCustom[3] = new Pori(new Vector3(597.074,11.736,-558.049), new Ypr(-2.445,-0.079,0.001));
		camSpeedMulCustom = 6.0;

		posStartCustom = new Vector3(603.546,10.233,-552.857);
		oriStartCustom = new Ypr(-2.444,-0.010,0.000);
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