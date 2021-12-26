package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

//RAXAT: v2.3.1, GPS frame handlers, additional I/O, Steam achievements
public class Player extends Racer
{
	final static int	H_GARAGE		= 0x00000001;
	final static int	H_DAYCITY		= 0x00000002;
	final static int	H_NIGHTCITY		= 0x00000004;
	final static int	H_PAINTBOOTH	= 0x00000008;
	final static int	H_NEWCARS		= 0x00000010;
	final static int	H_MOVEPARTS		= 0x00000020;
	final static int	H_VEHICLELIFT	= 0x00000040;
	final static int	H_EVENTLIST		= 0x00000080;
	
	final static String	DEFAULT_PLAYER_NAME = "Player";

	int		hints;
	int		winPinkSlips;
	
	//RAXAT: v2.3.1, all switches for cheats are now collected right here
	static int		c_enabled	= 1; //defines if player has access to cheats menu
	static int		c_garage	= 0; //garage codes: money, car selector, roc teleporter, etc.
	static int		c_track		= 0; //track cheats: slowmotion, osd on/off, freeze, etc.
	static int		c_eventlist	= 0; //eventlist stuff: join any race, one-click prize
	static int		c_catalog	= 0; //show/hide kits in catalog
	static int		c_autosave	= 1; //automatic career progress save
	static int		c_ghost		= 0; //no drivers in vehicles
	
	static String	achievements = ""; //Steam achievements, current value, we store it to keep Steam from overloading by game requests

	Osd		osd;

	Controller	controller;
	Inventory	parts, paintcans;
	Vector		decals = new Vector();

	int			flags;
	float		record_acceleration, record_speed;
	int			races_won, races_lost;

	CarLot		carlot;

	Rectangle	gpsFrame;
	int			gpsState = 1; //GPS frame is switched on by default
	int			gpsTextureID = frontend:0x0083r;

	String	name_f = "Vince";
	String	name_s = "Polansky";

	//RAXAT: v2.3.1, new savegame data; always sync this with careerProgress I/O!
	int		ROC_wins; //total wins in Race Of Champions
	int		cash_earned, cars_won, parts_won, misc_won; //all prize items won by the player
	int		police_busted, police_escaped;
	int		gold_cups, silver_cups, bronze_cups;
	int		lastPlayedEvent;

	//gamemode specific data
	Vector	eventInfo; //stores states for all career events
	Vector	bestTimes; //best lap times on all tracks

	//for the future updates
	int		parts_repaired;
	int		money_spent;
	float	distance_travelled, time_played;

	String	roc_code; //RAXAT: identification code for ROC prize vehicles

	int		dumpDebug = 0;

	public Player()
	{
		parts = new Inventory(this);
		paintcans = new Inventory(this);
		carlot = new CarLot(this);
		achievements = "";
		eventInfo = new Vector();
		bestTimes = new Vector();

		//pri=18, hogy a loading dialog eltakarja a megjeleno muszereket!
		osd = new Osd( 1.0, 0.0, 18 );
		osd.iLevel=Osd.IL_NONE;

		//RAXAT: v2.3.1, new initialization of GPS frame
		handleGPSframe(gpsState);

		type = 0; //RAXAT: v2.3.1, see Racer.class
	}
	
	//RAXAT: build 931, externally called by GameLogic
	public void setDefaults()
	{
		setMoney(GameLogic.INITIAL_PLAYER_MONEY);
		prestige = GameLogic.INITIAL_PLAYER_PRESTIGE;
		club = 0;
		flags = 0;
		hints = 0;
		winPinkSlips = 0;
		ROC_wins = 0;
		roc_code = GameLogic.genCodeROC();
		
		//RAXAT: build 932, more defaults
		lastPlayedEvent = 0;
		police_escaped = 0;
		police_busted = 0;
		gold_cups = 0;
		silver_cups = 0;
		bronze_cups = 0;
		cash_earned = 0;
		cars_won = 0;
		parts_won = 0;
		misc_won = 0;
		parts_repaired = 0;
		money_spent = 0;
		distance_travelled = 0;
		time_played = 0;
		
		eventInfo = new Vector();
		bestTimes = new Vector();

		if(!character)
		{
			character = new ResourceRef(Racer.RID_FEJ);
			name = DEFAULT_PLAYER_NAME;
		}
	}
	
	//RAXAT: Steam achievements
	public void setSteamAchievement(int typeid)
	{
		String token = "\t";
		
		if(!getSteamAchievement(typeid))
		{
			achievements += Integer.toString(typeid);
			achievements += token;
			
			Frontend.steam.setAchievement(typeid); //frontend stores static instance of Steam, so we send request there first
		}
	}
	
	public int getSteamAchievement(int typeid)
	{
		Vector vec = achievements.scanf();
		if(vec)
		{
			for(int i=0; i<vec.size(); i++)
			{
				int val = vec.elementAt(i).intValue();
				if(val == typeid) return 1;
			}
		}
		
		return 0;
	}
	
	//RAXAT: this will reset static local achievement string and send request to Steam
	public void resetSteamAchievements()
	{
		achievements = "";
		Frontend.steam.resetAchievements();
	}

	public int setMoney(int amount)
	{
		super.setMoney(amount);
		
		if(GameLogic.gameMode == GameLogic.GM_CARREER && money > 1000000) setSteamAchievement(Steam.ACHIEVEMENT_MILLIONARE);
		return money;
	}

	//RAXAT: Track() creates this automatically, but in other gamezones you need to create it manually
	public void handleGPSframe( int action )
	{
		if(action == 1)
		{
			if(gpsState)
			{
				if(!gpsFrame) gpsFrame = osd.createRectangle( -0.74, 0.74, 0.48, 0.53, -2, new ResourceRef( gpsTextureID ) );
				gpsState = 2;
			}
		}
		else
		{
			if(gpsState < 1)
			{
				if(gpsFrame)
				{
					gpsFrame.finalize(); //RAXAT: if handle 0
					gpsFrame = null;
					gpsState = -1;
				}
			}
		}
	}

	//RAXAT: if you need a new texture for GPS
	public void setGPSframeTexture( int tID )
	{
		gpsTextureID = tID;
		gpsFrame.setTexture( tID );
	}

	public String getName()
	{
		String result;

		result = name_f + " " + name_s;
		return result;

		return null;
	}

	public String getFullName()
	{
		String result;

		if(name) result = name_f + " '" + name + "' " + name_s;
		else result = getName();
		return result;

		return null;
	}

	//RAXAT: static, since we usually don't even need player's instance for that
	public static int getPhoto()
	{
		return RID_VINCE;
	}

	public int checkHint(int hintMask)
	{
		int shown = hints & hintMask;
		hints |= hintMask;
		return !shown;
	}

	public void addHint(int hintMask)
	{
		if(hints & hintMask) hints ^= hintMask;
	}

	public RenderRef getMarker()
	{
		return Marker.RR_PLAYER;
	}

	public RenderRef getMarker(int idx)
	{
		switch(idx)
		{
			case 0:
				return Marker.RR_PLAYER;
				break;
			case 1:
				return Marker.RR_V4_PLAYER;
				break;
		}

		return Marker.RR_PLAYER;
	}

	public void showOsd()
	{
		osd.show();
		controller.queueEvent( null, GameType.EVENT_COMMAND, "osd " + osd.id() );
	}

	public void hideOsd()
	{
		controller.queueEvent( null, GameType.EVENT_COMMAND, "osd 0" );
		osd.hide();
	}

	//RAXAT: this should save all player-related stuff from GameLogic! but savegame file must be closed before that. maybe save to another one? like "playerData"
	public void save(File saveGame)
	{
		super.save(saveGame);

		saveGame.write(character);
		saveGame.write(money);
		saveGame.write(prestige);
		saveGame.write(GameLogic.findRacer(this));
		saveGame.write(flags);
		saveGame.write(name);
		saveGame.write(races_won);
		saveGame.write(races_lost);
		saveGame.write(hints);
		saveGame.write(winPinkSlips);
		saveGame.write(roc_code);
		//saveGame.write(bet);
		
		saveGame.write(c_enabled);
		saveGame.write(c_garage);
		saveGame.write(c_track);
		saveGame.write(c_eventlist);
		saveGame.write(c_catalog);
		saveGame.write(c_autosave);
		saveGame.write(c_ghost);
		
		parts.save(saveGame);
		paintcans.save(saveGame);

		int num_decals = decals.size();
		saveGame.write(num_decals);
		for(int i=0; i<num_decals; i++)
		{
			Decal d = decals.elementAt(i);
			saveGame.write(d);
			saveGame.write(d.stickies);
		}
		
		if(dumpDebug)
		{
			System.trace("Player.save(): " + saveGame.name);
			
			System.trace("money: " + money);
			System.trace("character: " + Integer.getHex(character.type()));
			System.trace("prestige: " + prestige);
			System.trace("global_rank: " + GameLogic.findRacer(this));
			System.trace("flags: " + Integer.getHex(flags));
			System.trace("name: " + name);
			System.trace("races_won: " + races_won);
			System.trace("races_lost: " + races_lost);
			System.trace("hints: " + Integer.getHex(hints));
			System.trace("winPinkSlips: " + winPinkSlips);
			System.trace("roc_code: " + roc_code);
			//System.trace("bet: " + bet);
			
			System.trace("c_enabled: " + c_enabled);
			System.trace("c_garage: " + c_garage);
			System.trace("c_track: " + c_track);
			System.trace("c_eventlist: " + c_eventlist);
			System.trace("c_catalog: " + c_catalog);
			System.trace("c_autosave: " + c_autosave);
			System.trace("c_ghost: " + c_ghost);
		}
	}

	public void load(File saveGame)
	{
		super.load(saveGame);

		int character_id = saveGame.readResID();
		character = new ResourceRef(character_id);
		
		money = saveGame.readInt();
		prestige = saveGame.readFloat(); 
		
		int global_rank = saveGame.readInt();
		club = global_rank/GameLogic.CLUBMEMBERS;
		GameLogic.speedymen[global_rank] = this;
		
		flags = saveGame.readInt();		
		name = saveGame.readString();	
		races_won = saveGame.readInt();	
		races_lost = saveGame.readInt();
		hints = saveGame.readInt();
		winPinkSlips = saveGame.readInt();
		roc_code = saveGame.readString();
		//bet = saveGame.readInt();
		
		c_enabled	= saveGame.readInt();
		c_garage	= saveGame.readInt();
		c_track		= saveGame.readInt();
		c_eventlist = saveGame.readInt();
		c_catalog	= saveGame.readInt();
		c_autosave	= saveGame.readInt();
		c_ghost		= saveGame.readInt();

		club = global_rank/GameLogic.CLUBMEMBERS;
		GameLogic.speedymen[global_rank] = this;

		parts.load(saveGame);
		paintcans.load(saveGame);

		int num_decals = saveGame.readInt();
		for(int i=0; i<num_decals; i++)
		{
			int id = saveGame.readResID();
			Decal rd = new Decal(id);
			rd.stickies = saveGame.readInt();

			decals.addElement(rd);
		}
		
		if(dumpDebug)
		{
			System.trace("Player.load(): " + saveGame.name);
			System.trace("character: " + Integer.getHex(character_id));
			System.trace("money: " + money);
			System.trace("prestige: " + prestige);
			System.trace("global_rank: " + global_rank);
			System.trace("flags: " + Integer.getHex(flags));
			System.trace("name: " + name);
			System.trace("races_won: " + races_won);
			System.trace("races_lost: " + races_lost);
			System.trace("hints: " + Integer.getHex(hints));
			System.trace("winPinkSlips: " + winPinkSlips);
			System.trace("roc_code: " + roc_code);
			//System.trace("bet: " + bet);
			
			System.trace("c_enabled: " + c_enabled);
			System.trace("c_garage: " + c_garage);
			System.trace("c_track: " + c_track);
			System.trace("c_eventlist: " + c_eventlist);
			System.trace("c_catalog: " + c_catalog);
			System.trace("c_autosave: " + c_autosave);
			System.trace("c_ghost: " + c_ghost);
		}
	}

	//RAXAT: v2.3.1, new I/O, read the descriptions below
	public void saveTrackData() //save/load track best lap times; each index represents one single track data slot, divided into 2 elements
	{
		if(dumpDebug) System.trace("Player::saveTrackData()");

		int Club = club+1;
		if(bestTimes)
		{
			if(dumpDebug) System.trace("bestTimes found, begin preparing data...");

			String trackDataDirname = name + "-" + Club + "/" + GameLogic.trackDataSaveSubDir;
			String filename = GameLogic.carrerSaveDir + trackDataDirname;

			//File.delete( filename, "*" );

			File[] saveData = new File[2];
			saveData[0] = new File(filename + "data_v_F");
			saveData[1] = new File(filename + "data_v_S");

			Vector[] data_write = new Vector[2];
			data_write[0] = new Vector();
			data_write[1] = new Vector();

			//scan player's data in vector, and split it into 2 datablocks
			for(int i=0; i<bestTimes.size(); i++) //size must be >= 2!
			{
				if(dumpDebug) System.trace("processing element " + i);

				data_write[0].addElement(bestTimes.elementAt(i)); i++;  //best time
				data_write[1].addElement(bestTimes.elementAt(i));	//map name

				if(dumpDebug)
				{
					System.trace("added: best time " + bestTimes.elementAt(i-1));
					System.trace("added: map name " + String.timeToString(bestTimes.elementAt(i).number, String.TCF_NOHOURS));
				}
			}

			if(dumpDebug) System.trace("begin writing track data to file...");

			//now write each datablock to a relative save file
			for(int j=0; j<2; j++)
			{
				if(saveData[j].open(File.MODE_WRITE))
				{
					saveData[j].write(data_write[j]);
					saveData[j].close();
				}
			}

			if(dumpDebug) System.trace("track data saved successfully!");
		}
	}

	public void loadTrackData()
	{
		if(dumpDebug) System.trace("Player::loadTrackData()");

		int Club = club+1;
		String trackDataDirname = name + "-" + Club + "/trackData";
		String filename = GameLogic.carrerSaveDir + trackDataDirname + "/";

		File[] saveData = new File[2];
		saveData[0] = new File(filename + "data_v_F");
		saveData[1] = new File(filename + "data_v_S");

		Vector[] data_read = new Vector[2];
		data_read[0] = new Vector();
		data_read[1] = new Vector();

		if(dumpDebug) System.trace("begin vector-to-vector data read...");
		
		for(int i=0; i<2; i++)
		{
			if(saveData[i].open(File.MODE_READ))
			{
				switch(i)
				{
					case 0:
						data_read[i] = saveData[i].readVector(new Float()); //best time
						break;
					case 1:
						data_read[i] = saveData[i].readVector(new String()); //map name
						break;
				}
				saveData[i].close();
			}
		}

		if(dumpDebug) System.trace("read successful! attempting to recreate track data...");

		bestTimes = new Vector();
		if(data_read[0].size())
		{
			for(int j=0; j<data_read[0].size(); j++)
			{
				bestTimes.addElement(data_read[0].elementAt(j)); //best time
				bestTimes.addElement(data_read[1].elementAt(j)); //map name
			}
		}

		if(dumpDebug)
		{
			System.trace("track data is recreated, beginning data scan...");
			if(bestTimes && bestTimes.size())
			{
				System.trace("total bestTimes size: " + bestTimes.size());
				if((Math.remDiv(bestTimes.size(), 2)).f) System.trace("!FATAL ERROR: track data is corrupted!");

				for(int k=0; k<bestTimes.size(); k++)
				{
					System.trace("processing element " + k);

					String mapName = bestTimes.elementAt(k+1);
					if(mapName instanceof String) System.trace("map name: " + mapName);
					else System.trace("!ERROR: map name not set or corrupted!");
					System.trace("time: " + String.timeToString(bestTimes.elementAt(k).number, String.TCF_NOHOURS));
					k++;
				}
			}
			else System.trace("no data is present");
		}

		if(dumpDebug) System.trace("track data loaded successfully!");
	}

	//save/load player's career achievements (from/to a separate file)
	public void saveCareerProgress()
	{
		int Club = club+1;
		String dirname = name + "-" + Club + "/" + GameLogic.careerDataSaveSubDir;
		String filename = GameLogic.carrerSaveDir + dirname;

		//File.delete( filename, "*" );

		File saveData = new File(filename + "data");
		if(saveData.open(File.MODE_WRITE))
		{
			saveData.write(ROC_wins);
			saveData.write(cash_earned);
			saveData.write(cars_won);
			saveData.write(parts_won);
			saveData.write(misc_won);
			saveData.write(police_busted);
			saveData.write(police_escaped);
			saveData.write(gold_cups);
			saveData.write(silver_cups);
			saveData.write(bronze_cups);
			saveData.write(lastPlayedEvent);

			saveData.write(parts_repaired);
			saveData.write(money_spent);
			saveData.write(distance_travelled);
			saveData.write(time_played);

			if(dumpDebug)
			{
				System.trace("Player::saveCareerProgress()");
				System.trace("SAVE: cash_earned " + cash_earned);
				System.trace("SAVE: cars_won " + cars_won);
				System.trace("SAVE: parts_won " + parts_won);
				System.trace("SAVE: misc_won " + misc_won);
				System.trace("SAVE: police_busted " + police_busted);
				System.trace("SAVE: police_escaped " + police_escaped);
				System.trace("SAVE: gold_cups " + gold_cups);
				System.trace("SAVE: silver_cups " + silver_cups);
				System.trace("SAVE: bronze_cups " + bronze_cups);
				System.trace("SAVE: lastPlayedEvent " + lastPlayedEvent);

				System.trace("SAVE: parts_repaired " + parts_repaired);
				System.trace("SAVE: money_spent " + money_spent);
				System.trace("SAVE: distance_travelled " + distance_travelled);
				System.trace("SAVE: time_played " + time_played);
			}
			saveData.close();
		}
	}

	public void loadCareerProgress()
	{
		int Club = club+1;
		String dirname = name + "-" + Club + "/careerData";
		String filename = GameLogic.carrerSaveDir + dirname + "/";

		File saveData = new File(filename + "data");
		if(saveData.open(File.MODE_READ))
		{
			ROC_wins = saveData.readInt();
			cash_earned = saveData.readInt();
			cars_won = saveData.readInt();
			parts_won = saveData.readInt();
			misc_won = saveData.readInt();
			police_busted = saveData.readInt();
			police_escaped = saveData.readInt();
			gold_cups = saveData.readInt();
			silver_cups = saveData.readInt();
			bronze_cups = saveData.readInt();
			lastPlayedEvent = saveData.readInt();

			parts_repaired = saveData.readInt();
			money_spent = saveData.readInt();
			distance_travelled = saveData.readFloat();
			time_played = saveData.readFloat();

			if(dumpDebug)
			{
				System.trace("Player::loadCareerProgress()");
				System.trace("LOAD: cash_earned " + cash_earned);
				System.trace("LOAD: cars_won " + cars_won);
				System.trace("LOAD: parts_won " + parts_won);
				System.trace("LOAD: misc_won " + misc_won);
				System.trace("LOAD: police_busted " + police_busted);
				System.trace("LOAD: police_escaped " + police_escaped);
				System.trace("LOAD: gold_cups " + gold_cups);
				System.trace("LOAD: silver_cups " + silver_cups);
				System.trace("LOAD: bronze_cups " + bronze_cups);
				System.trace("LOAD: lastPlayedEvent " + lastPlayedEvent);

				System.trace("LOAD: parts_repaired " + parts_repaired);
				System.trace("LOAD: money_spent " + money_spent);
				System.trace("LOAD: distance_travelled " + distance_travelled);
				System.trace("LOAD: time_played " + time_played);
			}
			saveData.close();
		}
	}

	//save/load info about all career events that player participated in
	public void saveEventData()
	{
		int Club = club+1;
		if(eventInfo)
		{
			String eventDirname = name + "-" + Club + "/" + GameLogic.eventDataSaveSubDir;
			String filename = GameLogic.carrerSaveDir + eventDirname;

			//File.delete( filename, "*" );

			File[] saveData = new File[2];
			saveData[0] = new File(filename + "data_v_0"); //status
			saveData[1] = new File(filename + "data_v_1"); //name

			Vector[] data_write = new Vector[2];
			data_write[0] = new Vector();
			data_write[1] = new Vector();

			if(dumpDebug)
			{
				System.trace("Player::saveEventData(): attempting vector-to-vector write...");
				System.trace("Player::saveEventData(): eventInfo size: " + eventInfo.size());
			}

			for(int i=0; i<eventInfo.size(); i++) //size must be >= 2!
			{
				if(dumpDebug) System.trace("Player::loadEventData(): processing node " + i);

				data_write[0].addElement(eventInfo.elementAt(i)); i++;  //event status
				data_write[1].addElement(eventInfo.elementAt(i));	//event name
			}

			if(dumpDebug)
			{
				System.trace("Player::saveEventData(): vector-to-vector write successful!");
				System.trace("Player::saveEventData(): beginning writing datablocks...");
			}

			//now write each datablock to a relative save file
			for(int j=0; j<2; j++)
			{
				if(dumpDebug) System.trace("Player::saveEventData(): processing datablock " + j);

				if(saveData[j].open(File.MODE_WRITE))
				{
					saveData[j].write(data_write[j]);
					saveData[j].close();
				}
			}

			if(dumpDebug) System.trace("Player::saveEventData(): event data saved successfully!");
		}
	}

	public void loadEventData()
	{
		int Club = club+1;
		String eventDirname = name + "-" + Club + "/eventData";
		String filename = GameLogic.carrerSaveDir + eventDirname + "/";

		File[] saveData = new File[2];
		saveData[0] = new File(filename + "data_v_0"); //status
		saveData[1] = new File(filename + "data_v_1"); //name

		Vector[] data_read = new Vector[2]; //vector-to-vector read mode
		data_read[0] = new Vector();
		data_read[1] = new Vector();

		for(int i=0; i<2; i++)
		{
			if(dumpDebug) System.trace("Player::loadEventData(): attempting vector-to-vector read...");

			if(saveData[i].open(File.MODE_READ))
			{
				switch(i)
				{
					case 0:
						data_read[i] = saveData[i].readVector(new Integer()); //event status
						break;
					case 1:
						data_read[i] = saveData[i].readVector(new String()); //event name
						break;
				}
				saveData[i].close();

				if(dumpDebug) System.trace("Player::loadEventData(): vector-to-vector read successful!");
			}
		}

		if(dumpDebug) System.trace("Player::loadEventData(): begin processing data vector...");

		eventInfo = new Vector(); //now transfer loaded data to player
		if(data_read[0].size())
		{
			if(dumpDebug) System.trace("Player::loadEventData(): data size for eventInfo is " + data_read[0].size());

			for(int j=0; j<data_read[0].size(); j++)
			{
				if(dumpDebug) System.trace("Player::loadEventData(): processing data element " + j);

				eventInfo.addElement(data_read[0].elementAt(j));
				eventInfo.addElement(data_read[1].elementAt(j));
			}

			if(dumpDebug) System.trace("Player::loadEventData(): event data collected successfully!");
		}

		if(dumpDebug)
		{
			System.trace("Player::loadEventData(): begin tracing eventInfo...");
			for(int i=0; i<eventInfo.size(); i++)
			{
				if(eventInfo.elementAt(i) instanceof Integer) System.trace("eventInfo.elementAt(" + i + ") data: " + Integer.getHex(eventInfo.elementAt(i).number));
				else System.trace("eventInfo.elementAt(" + i + ") data: " + eventInfo.elementAt(i));
			}
		}
	}

	//this method is called to retrieve best lap time from the _existing_ data (i.e. it's already created or loaded)
	public float getTrackData(String mapName)
	{
		if(bestTimes.size())
		{
			for(int i=0; i<bestTimes.size(); i++)
			{
				if(mapName == bestTimes.elementAt(i+1)) //checking only by name yet
				{
					return bestTimes.elementAt(i).number;
					break;
				}
				i++;
			}
		}

		return 0;
	}

	//this method does set best lap time for some specific track; keep in mind, you need to run saveTrackData() after calling this method in order to save best lap times to file!
	public void setTrackData(String mapName, float value)
	{
		int statusCheck = 0;
		if(bestTimes.size())
		{
			for(int i=0; i<bestTimes.size(); i++)
			{
				//data is being identified by track name, if it does match, system will recognize the track and set data for it
				if(mapName == bestTimes.elementAt(i+1))
				{
					bestTimes.elementAt(i).number = value;
					statusCheck = 1;
					break;
				}
				i++;
			}
		}

		//create new entry in vector if track or its best lap time data was not found
		if(!bestTimes.size() || !statusCheck)
		{
			bestTimes.addElement(new Float(value));
			bestTimes.addElement(mapName);
		}
	}
}