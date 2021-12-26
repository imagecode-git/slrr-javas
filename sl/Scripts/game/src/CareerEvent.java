package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.lang.*;
import java.render.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.airfueldeliverysystem.charger.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;

//RAXAT: parent class, declares principals of storing data for all player's career events
public class CareerEvent extends GameType
{
	//standard gamemode pack, v2.3.1
	final static int GAMEMODE_CIRCUIT	= sl.Scripts.game.Gamemodes.stdpack_231:0x0001r;
	final static int GAMEMODE_TIMEATTACK	= sl.Scripts.game.Gamemodes.stdpack_231:0x0002r;
	final static int GAMEMODE_DRIFT		= sl.Scripts.game.Gamemodes.stdpack_231:0x0003r;
	final static int GAMEMODE_DRAG		= sl.Scripts.game.Gamemodes.stdpack_231:0x0004r;
	final static int GAMEMODE_COP		= sl.Scripts.game.Gamemodes.stdpack_231:0x0005r;
	final static int GAMEMODE_DERBY		= sl.Scripts.game.Gamemodes.stdpack_231:0x0006r;
	final static int GAMEMODE_FREERIDE	= sl.Scripts.game.Gamemodes.stdpack_231:0x0008r;
	final static int GAMEMODE_DEBUG		= sl.Scripts.game.Gamemodes.stdpack_231:0x0009r;

/*
	final static int GAMEMODE_CROSSRACING	= sl.Scripts.game.Gamemodes.stdpack_231:0x000Ar;
	final static int GAMEMODE_DTM		= sl.Scripts.game.Gamemodes.stdpack_231:0x000Br;
*/

	final static int CAR_CLASS_E = 0x201;
	final static int CAR_CLASS_D = 0x202;
	final static int CAR_CLASS_C = 0x203;
	final static int CAR_CLASS_B = 0x204;
	final static int CAR_CLASS_A = 0x205;
	final static int CAR_CLASS_S = 0x206;

	final static int DRIVETYPE_FWD = 0x301;
	final static int DRIVETYPE_RWD = 0x302;
	final static int DRIVETYPE_AWD = 0x303;

	//not used in shield data
	final static int TRANSMISSION_MANUAL	= 3;
	final static int TRANSMISSION_AUTO	= 1;
	final static int TRANSMISSION_SEMIAUTO	= 5;
	final static int TRANSMISSION_MANUAL_C	= 2; //manual+clutch
	final static int TRANSMISSION_OLDSCHOOL = 0; //compatibility
	
	String eventName; //must be same as name at title pic
	String e_trackName;
	String[] conditionText = new String[5];
	String[] reqText = new String[5];
	int track_data_id;
	int gamemode_id, carClass, drivetype; //for shields
	int introFMV, outroFMV; //videos

	int useFines; //penalties, disqualifications
	int cleanDriving; //fines for runout
	int customRequirements; //amount of lines to be displayed in the requirements block

	Gamemode careerGM;
	Vector gamemodeData; //event calls gamemode with given gamemode_id and put params into method instance data
	ResourceRef title_res, e_bck, e_minimap;
	TrackData track;
	int event_id; //id of every single event (not the gamemode id!!)

	float[] fogData = new float[2];
	float	copDensity; //"density" of police vehicles in the entire traffic
	int	raceTime = -1;

	int	minPower = -1;
	int	maxPower = -1;
	int	minDisplacement = -1;
	int	maxDisplacement = -1;

	String prizeTitle; //overrides generated prize string
	Object prize;
	int prizeCash;
	int fee;
	int rocWins; //for checking wins in ROC
	int rating; //player's club rating check
	String carMaker;
	String carVendor, carModel;
	String[] carVendors, carModels;
	int useAwards = 0; //0 - no awards, 1 - gold/silver/bronze cups

	int racers = 0;
	Vector botData; //id's of racers from the database
	int randomStartGrid = 0; //if enabled, start grid will be randomized; turned off by default

	//specific gamemode data, max 10 slots
	//WARNING!! slot 0 must be ALWAYS occupied by laps count! otherwise game will crash!
	int[] specialSlots;

	//alternative path/speed for camera animation for specific racing event
	Pori[]	camPathCustom;
	float	camSpeedMulCustom;
	int	useCamAnimation; //adjustable, but by default is switched on (see Config.class)

	//custom camera animation trajectories, start positions
	Vector3	posStartCustom;
	Ypr	oriStartCustom;

	//override bot vehicles
	int[]	specialVehicles;
	String	specialVehiclePath;
	String	specialSkinPath;

	static String	noReqText = "No special requirements for this event";

	public CareerEvent()
	{
		useCamAnimation = Config.gm_cam_anim;
	}

	public void init()
	{
		if(!Config.gm_cam_anim) useCamAnimation = 0; //this prevents overriding global switch by child classes
		else useCamAnimation = Config.gm_cam_anim;

		specialSlots = new int[10];
		racers = getRacers();
		GameRef xa = new GameRef();
		track = xa.create(null, new GameRef(track_data_id), null, "track data" );
		if(track)
		{
			track.init();
			e_trackName = track.e_name;
			e_bck = new ResourceRef(track.bg_pic);
			e_minimap = new ResourceRef(track.minimap);
		}

		careerGM = xa.create(null, new GameRef(gamemode_id), null, "gamemode" );
		if(careerGM) careerGM.init();
	}

	public int getRacers()
	{
		if(botData){return botData.size();}

		return 0;
	}

	public void setRacers(int count) {racers = count;} //for debug purposes only!!

	//pick random opponents by rating
	public Vector findRacers(int minrating, int maxrating, int count)
	{
		RandomBox data;
		Vector indices = new Vector();
		Vector result = new Vector();

		for(int k=0; k<GameLogic.CLUBMEMBERS*GameLogic.CLUBS-1; k++)
		{
			if(Math.section(minrating, maxrating, Bot.db.getRating(k))) indices.addElement(new Integer(k)); //find all racers that match rating criteria
		}

		data = new RandomBox(indices);
		for(int j=0; j<count; j++) result.addElement(data.pick()); //now pick some random bots
		return result;

		return null;
	}

	//pick one racer by rating
	public int findRacer(int rating)
	{
		for(int k=0; k<GameLogic.CLUBMEMBERS*GameLogic.CLUBS-1; k++) if(rating == getRating(k)) return k;

		return 0;
	}

	public void syncRaceTime()
	{
		if(raceTime > 0) GameLogic.setTime(raceTime*3600); //jump to time, defined in the event
		else GameLogic.spendTime((Math.chaos()*4)*3600); //or just increase current time
	}

	public int countRequirements()
	{
		int result;

		if(gamemode_id != GAMEMODE_FREERIDE)
		{
			for(int i=0; i<reqText.length; i++)
			{
				if(reqText[i] && reqText[i].length()) result++;
			}

			if(!result)
			{
				reqText[0] = noReqText;
				return 1;
			}
		}
		else
		{
			//data collector for special EventList mode
			if(track.location) conditionText[0] = "Location: " + track.location;
			if(track.type) conditionText[1] = "Type: " + track.type;
			if(track.author) conditionText[2] = "Author: " + track.author;

			for(int i=0; i<reqText.length; i++)
			{
				if(track.description && track.description.length)
				{
					if(i<track.description.length)
					{
						if(track.description[i] && track.description[i].length()) reqText[i] = track.description[i];
						result++;
					}
				}
				else
				{
					reqText[0] = "No description";
					return 1;
				}
			}
		}

		return result;
		return 0;
	}

	public String racersToString()
	{
		return "Participants: " + getRacers();

		return null;
	}

	public String getCleanDriving()
	{
		if(cleanDriving) return "Clean driving";

		return "";
	}

	public String feeToString()
	{
		return "Entry fee: $" + fee;

		return null;
	}

	public String powerToString()
	{
		String result = "Engine power: ";

		if(minPower >= 0 && maxPower > 0) result += minPower + "~" + maxPower + "HP";
		else
		{
			if(minPower >= 0) result += minPower + "HP min";
			if(maxPower > 0) result += maxPower + "HP max";
		}
		return result;

		return null;
	}

	public String displacementToString()
	{
		String result = "Engine displacement: ";

		if(minDisplacement >= 0 && maxDisplacement > 0) result += minDisplacement + "~" + maxDisplacement + "cc";
		else
		{
			if(minDisplacement >= 0) result += minDisplacement + "cc min";
			if(maxDisplacement > 0) result += maxDisplacement + "cc max";
		}
		return result;

		return null;
	}

	public Part createPart(int id)
	{
		GameRef xa = new GameRef();
		Part part = xa.create( GameLogic.player, new GameRef(id), "100,0,0,0,0,0", "CareerEvent generated part" );
		return part;

		return null;
	}

	public Set createSet(int id)
	{
		GameRef xa = new GameRef();
		Set set = xa.create( GameLogic.player, new GameRef(id), "100,0,0,0,0,0", "CareerEvent generated set" );
		return set;

		return null;
	}

	public void setPrize(int amount) {prize = new Integer(amount);}
	public void setPrize(Object o)	 {prize = o;}

	//for EventList, returns "name" of prize
	public String prizeToString()
	{
		String prefix; if(!useAwards) prefix = "Prize: "; else prefix = "Prize (Gold cup): ";
		if(!prizeTitle) return prefix+getPrizeName();
		else return prizeTitle;

		return null;
	}

	//for other classes
	public String getPrizeName()
	{
		int max = 20; //should have charset size dependancy, something like (Text.getCharWidth(sty.charset,vp)*vp.getWidth()
		String result;

		if(prize instanceof Integer)
		{
			Integer value = (Integer)prize;
			result = result + "$" + value.number;
		}
		else
		{
			if(prize instanceof Vehicle)
			{
				Vehicle vhc = (Vehicle)prize;
				result += vhc.chassis.vehicleName;
			}

			if(prize instanceof Part)
			{
				Part p = (Part)prize;
				result += p.name;
			}

			if(prize instanceof Set)
			{
				Set s = (Set)prize;
				result += s.name;
			}
		}

		if(result.length()>max) result = result.chop(result.length()-max) + "..."; //a long name will be chopped for a more smart visual look
		return result;

		return null;
	}

	//to get "ripped" prize value in external classes
	public String getPrizeCash(int place)
	{
		String result;

		if(prize)
		{
			if(prize instanceof Integer)
			{
				Integer value = (Integer)prize;
				int cash = value.number;
				switch(place) //decrease prize if player didn't finish at 1st place
				{
					case 1: //silver cup, 2nd place
					cash = cash/2;
					break;

					case 2: //bronze cup, 3rd place
					cash = cash/5;
					break;
				}
				result = result + "$" + cash;
			}
			else result = getPrizeName(); //for safety
		}
		return result;

		return null;
	}

	//for races without award supply
	public void aquirePrize()
	{
		if(prize)
		{
			if (prize instanceof Integer)
			{
				Integer value = (Integer)prize;
				GameLogic.player.addMoney(value.number);
			}
			else
			{
				if(prize instanceof Vehicle)
				{
					//leave a new hint to tell player that he got a new car
					GameLogic.player.addHint(Player.H_NEWCARS);
					GameLogic.player.carlot.addCar((Vehicle)prize, GameLogic.player.carlot.curcar);
				}

				if(prize instanceof Part)
				{
					Part p = (Part)prize;
					GameLogic.player.parts.addItem(p);
				}

				if(prize instanceof Set)
				{
					Set s = (Set)prize;
					InventoryItem_Folder tmp = new InventoryItem_Folder(GameLogic.player.parts);
					tmp.set = s;
					s.build(tmp.inv);
					GameLogic.player.parts.items.addElement(tmp);
				}
			}
		}
	}

	//for award-supplied races
	public void aquirePrize(int place) //gold is 0, not 1!
	{
		if(prize)
		{
			if(prize instanceof Integer)
			{
				Integer value = (Integer)prize;
				int cash = value.number;
				switch(place) //decrease prize if player didn't finish at 1st place
				{
					case 1: //silver cup, 2nd place
					cash = cash/2;
					break;

					case 2: //bronze cup, 3rd place
					cash = cash/5;
					break;
				}
				GameLogic.player.addMoney(cash);
			}
		}
	}

	public int reqCheck( int reqId ) {return 0;} //reqId: actually id's of lines in each req column of EventList
	public static String getClass( int classId )
	{
		String result = "class ";
		String cID;

		switch(classId)
		{
			case (CAR_CLASS_E): cID = "E"; break;
			case (CAR_CLASS_D): cID = "D"; break;
			case (CAR_CLASS_C): cID = "C"; break;
			case (CAR_CLASS_B): cID = "B"; break;
			case (CAR_CLASS_A): cID = "A"; break;
			case (CAR_CLASS_S): cID = "S"; break;
		}

		if(!cID) cID = "unknown";

		return result+cID;
		return null;
	}

	public int checkClass()
	{
		return checkClass(GameLogic.player.car);

		return 0;
	}

	//get engine horsepower
	public int getHP(Vehicle car)
	{
		int cID;

		Block engine;
		DynoData dyno;

		if(car)
		{
			if( car.iteratePartsInit() )
			{
				Part part;
				while( part = car.iterateParts() )
				{
					if ( part instanceof Block )
						engine = part;
				}
			}
		}

		if(engine) dyno = engine.dynodata;
		if(dyno) return dyno.maxHP;

		return 0;
	}

	public int getDisplacement(Vehicle car)
	{
		int cID;

		Block engine;
		DynoData dyno;

		if(car)
		{
			if( car.iteratePartsInit() )
			{
				Part part;
				while( part = car.iterateParts() )
				{
					if ( part instanceof Block )
						engine = part;
				}
			}
		}

		if(engine) dyno = engine.dynodata;
		if(dyno) return dyno.Displacement*1000000.0; //cc

		return 0;
	}


	public int checkPower()
	{
		int check, target;
		float power;
		Vehicle vhc = GameLogic.player.car;
		if(vhc) power = getHP(vhc);

		if(power)
		{
			if(minPower>=0)
			{
				target++;
				if(power>=minPower) check++;
			}

			if(maxPower>=0)
			{
				target++;
				if(power<=maxPower) check++;
			}

			if(check==target) return 1;
		}

		return 0;
	}

	public int checkDisplacement()
	{
		int check, target;
		float displ;
		Vehicle vhc = GameLogic.player.car;
		if(vhc) displ = getDisplacement(vhc);

		if(displ)
		{
			if(minDisplacement>=0)
			{
				target++;
				if(displ>=minDisplacement) check++;
			}

			if(maxDisplacement>=0)
			{
				target++;
				if(displ<=maxDisplacement) check++;
			}

			if(check==target) return 1;
		}

		return 0;
	}

	//RAXAT: static in build 932
	public static int checkClass(Vehicle car)
	{
		int cID;
		Block engine;
		DynoData dyno;

		if(car)
		{
			if( car.iteratePartsInit() )
			{
				Part part;
				while( part = car.iterateParts() )
				{
					if ( part instanceof Block ) engine = part;
				}
			}
		}

		//OLD, more complicated
		/*
		if(engine) dyno = engine.dynodata;
		float mass;	if(car.chassis) mass = car.chassis.getMassPatch(); //kg
		float power;	if(dyno) power = dyno.maxHP; //bhp
		float displ;	if(dyno) displ = dyno.Displacement*1000000.0; //cc
		float ratio = 0.0f;
		if(power != 0) ratio = mass/power;

		System.trace("CareerEvent: checkClass()");
		System.trace("--------------------------");
		System.trace("stats for this car:");
		System.trace("engine power: " + power + "HP");
		System.trace("total mass: " + mass + "kg");
		System.trace("engine displacement: " + displ + "cc");
		System.trace("ratio: " + ratio);

		if(Math.section(0,160,power)) return CareerEvent.CAR_CLASS_E;
		if(Math.section(161,350,power) && displ <= 3000 && mass <= 2000) return CareerEvent.CAR_CLASS_D;
		if(ratio > 3.75 && ratio <= 6.5 && displ <= 4000) return CareerEvent.CAR_CLASS_C;
		if(ratio > 3.25 && ratio <= 4.5) return CareerEvent.CAR_CLASS_B;
		if(Math.section(600,900,power)) return CareerEvent.CAR_CLASS_A;
		if(power > 900 && displ >= 5000) return CareerEvent.CAR_CLASS_S;
		System.trace(getClass(cID));
		*/
		
		//NEW, horsepower-based
		//todo: put this table somewhere, so player will be aware of this system
		if(engine)
		{
			dyno = engine.dynodata;
			if(dyno)
			{
				float power = dyno.maxHP;
				if(power < 160)					cID = CareerEvent.CAR_CLASS_E;
				if(Math.section(160,299,power))	cID = CareerEvent.CAR_CLASS_D;
				if(Math.section(300,499,power))	cID = CareerEvent.CAR_CLASS_C;
				if(Math.section(500,699,power))	cID = CareerEvent.CAR_CLASS_B;
				if(Math.section(700,999,power))	cID = CareerEvent.CAR_CLASS_A;
				if(power > 999)					cID = CareerEvent.CAR_CLASS_S;
			}
		}

		engine = null;
		dyno = null;

		return cID;
	}

	public int checkDriveType(int base)
	{
		int dt = GameLogic.player.car.getInfo(52);

		switch(dt)
		{
			case 1:
			base = CareerEvent.DRIVETYPE_AWD;
			break;

			case 2:
			base = CareerEvent.DRIVETYPE_FWD;
			break;

			case 3:
			base = CareerEvent.DRIVETYPE_RWD;
			break;

			case 4:
			base = CareerEvent.DRIVETYPE_AWD; //4x4
			break;
		}
		if(base ==  drivetype) return 1;

		return 0;
	}

	public String getDriveType()
	{
		int dt = GameLogic.player.car.getInfo(52);
		switch(dt)
		{
			case(1): return "AWD"; break;
			case(2): return "FWD"; break;
			case(3): return "RWD"; break;
			case(4): return "4WD"; break;
		}

		return "N/A";
	}

	//basic req check implementations:
	public int checkMaker( String base )
	{
		if(GameLogic.player.car)
		{
			if(GameLogic.player.car.chassis.makerName == base)
				return 1;
		}

		return 0;
	}

	//one vendor
	public int checkVendor( String base )
	{
		String[] b = new String[1];
		b[0] = base;

		return checkVendor(b);
		return 0;
	}

	//multiple vendors
	public int checkVendor( String[] base )
	{
		if(GameLogic.player.car)
		{
			for(int i=0; i<base.length; i++)
			{
				if(GameLogic.player.car.chassis.vendorName == base[i]) return 1;
			}
		}

		return 0;
	}

	//one model
	public int checkModel( String base )
	{
		String[] b = new String[1];
		b[0] = base;

		return checkModel(b);
		return 0;
	}

	//multiple models
	public int checkModel( String[] base )
	{
		if(GameLogic.player.car)
		{
			for(int i=0; i<base.length; i++)
			{
				if((GameLogic.player.car.chassis.vendorName + " " + GameLogic.player.car.chassis.modelName) == base[i]) return 1;
			}
		}

		return 0;
	}

	public int checkMoney( int base )
	{
		if(GameLogic.player.getMoney() >= base) return 1;

		return 0;
	}
	
	//RAXAT: build 932, checking if the engine is naturally aspirated
	public int checkForcedInduction()
	{
		Block engine = null;

		if(GameLogic.player.car.iteratePartsInit())
		{
			Part part;
			while(part = GameLogic.player.car.iterateParts())
			{
				if (part instanceof Block)
				{
					engine = part;
					if(engine.naturallyAspirated()) return 0;
					else return 1;
				}
			}
		}
		
		return 0;
	}

	public int checkNitrous()
	{
		if(GameLogic.player.car)
		{
			if( GameLogic.player.car.iteratePartsInit() )
			{
				Part part;
				while( part = GameLogic.player.car.iterateParts() ) {if ( part instanceof Canister ) return 1;}
			}
		}

		return 0;
	}

	public int checkROC()
	{
		if(GameLogic.player.ROC_wins >= rocWins) return 1;

		return 0;
	}

	public int checkRating( int base )
	{
		if(GameLogic.findRacer(GameLogic.player) >= base) return 1;

		return 0;
	}

	public int checkStreetLegal()
	{
		if(GameLogic.player.car)
		{
			if( GameLogic.player.car.iteratePartsInit() )
			{
				Part part;
				while( part = GameLogic.player.car.iterateParts() ) {if ( part.police_check_fine_value ) return 0;} //illegal parts found, so car is no more street legal
			}
		}

		return 1;
	}

	public int checkTransmission( int base )
	{
		if(GameLogic.player.car)
		{
			if( GameLogic.player.car.iteratePartsInit() )
			{
				Part part;
				Transmission t;
				while( part = GameLogic.player.car.iterateParts() )
				{
					if( part instanceof Transmission )
					{
						t = part;
						if(t.type == base)
						return 1;
					}
				}
			}
		}

		return 0;
	}

	public int checkMass( float base )
	{
		if(GameLogic.player.car)
		{
			if(GameLogic.player.car.chassis.getMass() <= base)
			return 1;
		}

		return 0;
	}

	public int checkPrestige( int base )
	{
		if(GameLogic.player.prestige >= base) return 1;

		return 0;
	}

	//recieves status of career event from player
	public static int getEventStatus(String name)
	{
//		System.trace("getEventStatus(" + name + ")");
		if(GameLogic.player.eventInfo.size())
		{
			for(int i=0; i<GameLogic.player.eventInfo.size(); i++)
			{
//				System.trace("scan element: " + i);
				if(name == GameLogic.player.eventInfo.elementAt(i+1))
				{
//					System.trace("return " + GameLogic.player.eventInfo.elementAt(i).number);
					return GameLogic.player.eventInfo.elementAt(i).number;
				}

				i++;
			}
		}

		return 0;
	}

	//set status of career event back to player; the data will be kept unsaved until player.saveEventData() will be called
	public static void setEventStatus(String name, int value)
	{
//		System.trace("setEventStatus(" + name + ", " + value + ")");
		int statusCheck = 0;
		if(GameLogic.player.eventInfo.size())
		{
			for(int i=0; i<GameLogic.player.eventInfo.size(); i++)
			{
//				System.trace("scan element: " + i);
				if(name == GameLogic.player.eventInfo.elementAt(i+1)) //checking only name yet
				{
//					System.trace("writing value: " + GameLogic.player.eventInfo.elementAt(i).number);
					GameLogic.player.eventInfo.elementAt(i).number = value;
					statusCheck = 1;
					break;
				}

				i++;
			}
		}

		if(!GameLogic.player.eventInfo.size() || !statusCheck)
		{
//			System.trace("!eventInfo! ADDING value: " + value);
			GameLogic.player.eventInfo.addElement(new Integer(value));
			GameLogic.player.eventInfo.addElement(name);
		}
	}

	public void finalize()
	{
		if(!(prize instanceof Integer)) prize = null;
	}

	//RDB and java generator for career events, makes work faster, but you still need to edit some stuff by hands
	public void generateCareerPackage(String rdb_name, String path)
	{
		//usage:
		//CareerEvent ce = new CareerEvent();
		//String path = "sl/Scripts/game/CareerEvents/titles_dev/";
		//ce.generateCareerPackage("stdpack_231", path);

		int traceDebug = 0; //switch on to log all the generation process
		if(traceDebug)
		{
			System.trace("CareerEvent: begin generating career package " + rdb_name);
			System.trace("CareerEvent: building RPK...");
		}

		int c = 1;
		FindFile ff = new FindFile();
		Vector fileNameList = new Vector();
		fileNameList.setSize(ff.countFiles(path + "*", FindFile.FILES_ONLY));
		Debug d = new Debug("multibot\\debugger\\" + rdb_name + ".rdb");
		String name=ff.first(path + "*", FindFile.FILES_ONLY); //scan for filenames, they must have format: DebugMode_3.png or FurranoRush_6.png or DayOfTheDrags_22.png
		while(name)
		{
			fileNameList.setElementAt(name.cut("_"), name.cut("_", ".").intValue()-1); //produce "clean" event name and set it inside the vector under index of event from filename

			StringTokenizer t = new StringTokenizer(name, ".");
			t.nextToken();
			String extension = t.nextToken();
			File.copy(path+name, "multibot\\debugger\\titles\\" + name.cut("_") + "." + extension);

			name = ff.next();
		}
		ff.close();

		//header of RDB
		d.write( "<FILE external_links >" );
		d.write( "system\\" );
		d.write( "</FILE>" );
		d.write( "" );
		d.write( "<FILE 0000000" + c + ".res >" );
		d.write( "typeof	7" );
		d.write( "superid	0x00010008" );
		d.write( "typeid	0x00000002" );
		d.write( "alias	texture_root" );
		d.write( "isparentcompatible	1.00" );
		d.write( "</FILE>" );
		d.write( "<FILE 0000000" + c + ".rsd >" );
		d.write( "</FILE>" );
		d.write( "" );
		d.write( "//=============================" );
		d.write( "" );
		c+=2;

		if(traceDebug)
		{
			System.trace("CareerEvent: RPK header processed");
			System.trace("CareerEvent: attempting to build child nodes...");
		}

		//class nodes
		for(int i=0; i<fileNameList.size(); i++)
		{
			String n = fileNameList.elementAt(i);
			if(traceDebug) System.trace("CareerEvent: processing class: " + n + ".class");

			d.write( "<FILE 0000000" + c + ".res >" );
			d.write( "typeof	8" );
			d.write( "superid	0x00012000" );
			d.write( "typeid	" + Integer.getHex(c));
			d.write( "alias	" + n + ".class" );
			d.write( "isparentcompatible	1.00" );
			d.write( "</FILE>" );
			d.write( "<FILE 0000000" + c + ".rsd >" );
			d.write( "script sl\\Scripts\\game\\CareerEvents\\" + n + ".class" );
			d.write( "</FILE>" );
			d.write( "" );
			c++;
		}

		d.write( "//=============================" );
		d.write( "" );

		//image res nodes (event titles)
		for(int j=0; j<fileNameList.size(); j++)
		{
			String n = fileNameList.elementAt(j);
			if(traceDebug) System.trace("CareerEvent: processing image: " + n + ".png");

			d.write( "<FILE 0000000" + c + ".res >" );
			d.write( "typeof	7" );
			d.write( "superid	0x00000002" );
			d.write( "typeid	" + Integer.getHex(c));
			d.write( "alias	" + n + ".png" );
			d.write( "isparentcompatible	1.00" );
			d.write( "</FILE>" );
			d.write( "<FILE 0000000" + c + ".rsd >" );
			d.write( "sourcefile sl\\Scripts\\game\\CareerEvents\\titles\\" + n + ".png" );
			d.write( "</FILE>" );
			d.write( "" );
			c++;
		}

		if(traceDebug)
		{
			System.trace("Total nodes processed: " + fileNameList.size());
			System.trace("CareerEvent: RPK building done!");
			System.trace("");
			System.trace("CareerEvent: Begin generating base source files...");
		}

		//base javas, you need to further edit them in order to get a complete career event sourcefile
		c = 3;
		for(int k=0; k<fileNameList.size(); k++)
		{
			String n = fileNameList.elementAt(k);
			Debug d = new Debug("multibot\\debugger\\src\\" + n + ".java");

			if(traceDebug) System.trace("CareerEvent: generating sourcefile: " + n + ".java");

			d.write( "package java.game;" );
			d.write( "" );
			d.write( "import java.io.*;" );
			d.write( "import java.util.*;" );
			d.write( "import java.util.resource.*;" );
			d.write( "" );
			d.write( "public class " + n + " extends CareerEvent" );
			d.write( "{" );
			d.write( "	public " + n + "(){}" );
			d.write( "" );
			d.write( "	public void init()" );
			d.write( "	{" );
			d.write( "		track_data_id = multibot.maps.TestTrack.t_data:0x0102r;" );
			d.write( "" );
			d.write( "		title_res = new ResourceRef(sl.Scripts.game.CareerEvents.stdpack_231:" + Integer.getHex(c+fileNameList.size()) + "r);" );
			d.write( "		event_id = sl.Scripts.game.CareerEvents.stdpack_231:" + Integer.getHex(c) + "r;" );

			String s = "		eventName = ";
			d.write( s.append(34) +  n.append(34) + ";"); //append(34) will surrond text with the quotes
			d.write( "" );
			d.write( "		raceTime = 8;" );
			d.write( "" );
			d.write( "		gamemode_id = GAMEMODE_FREERIDE;" );
			d.write( "" );
			d.write( "		super.init();" );
			d.write( "	}" );
			d.write( "" );
			d.write( "	public int reqCheck(int reqId)" );
			d.write( "	{" );
			d.write( "		return 1;" );
			d.write( "	}" );
			d.write( "}" );
			c++;
		}

		if(traceDebug) System.trace("CareerEvent: all sources generated!");
	}

	//trace complete vehicle class map
	public void printVehicleClasses()
	{
		GameRef parts;
		Vector vehicles = new Vector();
		VehicleDescriptor vd = GameLogic.getVehicleDescriptor(VehicleType.VS_STOCK);

		Vector stack = new Vector();
		stack.addElement(new GameRef(cars:0x0101r));

		System.trace("---------beginning vehicle class scan---------");

		if(stack)
		{
			while(!stack.isEmpty())
			{
				parts=stack.removeLastElement();

				if(parts.isScripted("java.game.parts.Part"))
				{
					Vehicle v =  new Vehicle(GameLogic.player, parts.id(), vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear);
					System.trace("VehicleName: " + v.chassis.vehicleName);
					System.trace("Class: " + getClass(checkClass(v)));
					System.trace("displ: " + getDisplacement(v));
					System.trace("HP: " + getHP(v));
					System.trace("mass: " + v.chassis.getMass());
					System.trace("mass/HP: " + v.chassis.getMass()/getHP(v));
					System.trace("real mass: " + v.chassis.getMassPatch());
					System.trace("real mass/HP: " + v.chassis.getMassPatch()/getHP(v));
					System.trace("");

					v.destroy();
					v = null;
				}
				else
				{
					if(parts=parts.getFirstChild())
					{
						while(parts)
						{
							if(parts.isScripted()) stack.addElement(parts);
							parts=parts.getNextChild();
						}
					}
				}
			}
		}

		System.trace("---------vehicle class scan done---------");
	}
}