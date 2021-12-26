package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

public class MeetTheMillionaires extends CareerEvent
{
	int laps = 6;

	public MeetTheMillionaires();

	public void init()
	{
		track_data_id = multibot.maps.SPAFrankoohapsGPCircuit.t_data:0x0102r;

		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:0x00000070r);
		event_id = sl.Scripts.game.CareerEvents.stdpack_231:0x00000035r;
		eventName = "Meet The Millionaires";

		raceTime = 13;

		gamemode_id = GAMEMODE_CIRCUIT;

		botData = new Vector();
		botData.addElement(new Integer(49));	//GhostRider
		botData.addElement(new Integer(37));	//Den_nvkz
		botData.addElement(new Integer(52));	//Albert
		botData.addElement(new Integer(32));	//Coyote
		botData.addElement(new Integer(54));	//Fuzzy

		setPrize(new Vehicle(GameLogic.player, cars.racers.Whisper:0x00000158r, 1.0, 1.0, 1.0, 1.0, 1.0 )); //Whisper Q1000XL
		rating = 40;

		carVendors = new String[3];
		carVendors[0] = "Whisper";
		carVendors[1] = "Naxas";
		carVendors[2] = "Furrano";

		useFines = 1;
		useAwards = 0;
		cleanDriving = 1;
		randomStartGrid = 1;

		fee = 10000;

		conditionText[0] = "Laps: " + laps;
		conditionText[1] = racersToString(); //participants
		conditionText[2] = prizeToString(); //prize description
		conditionText[3] = getCleanDriving();
		conditionText[4] = feeToString();

		reqText[0] = "Brands: " + carVendors[0] + ", " + carVendors[1] + ", " + carVendors[2];
		reqText[1] = "No nitrous";
		reqText[2] = "Club rating: " + rating + " or higher";

		fogData[0] = -100.0;
		fogData[1] = 125.0;

		super.init();

		specialSlots[0] = laps;
	}

	public int reqCheck(int reqId)
	{
		switch(reqId)
		{
			case 0:
			if(checkVendor(carVendors)) return 1;
			break;

			case 1:
			if(!checkNitrous()) return 1;
			break;

			case 2:
			if(checkRating(rating)) return 1;
			break;
		}

		return 0;
	}
}
