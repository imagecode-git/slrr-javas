package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;

public class Bot extends Racer
{
	int	seed;
	float   aiLevel;
	int	debugid;

	VehicleDescriptor botVd;
	VehicleDescriptor nightVd;

	GameRef	dummycar; //dublor, native-only instance

	float	color, optical, engine; //randomly generated data

	int	imaPoliceDriver;
	int	nightWins = 0;
	int	nightLoses = 0;
        float	bestNightQM = 0.0;

	static	String[] botNames;

	GameRef world;

	GameRef brain;          //ha ai-kent mukodik
	GameRef controller;	// ==brain :o), patch
	int	traffic_id;     //ha a forgalomban van
	int	horn;

	static database db = new database();
	botCard profile;

	//RAXAT: v2.3.1, simplified, mostly used by new gamemodes
	public Bot(int dbId)
	{
		type = 1; //RAXAT: v2.3.1, see Racer.class

		int rnd;

		profile = db.getProfile(dbId);
		name = profile.getName();
		character = new RenderRef(RID_FEJ + dbId);

		if(name.length() % 2) driverID = GameLogic.HUMAN_OPPONENT;
		else driverID = GameLogic.HUMAN_OPPONENT2;

	        setEventMask(EVENT_COMMAND);
	}

	public Bot(int dbId, int rndseed, float level)
	{
		this(dbId);

        	seed = rndseed;

		int tmp;

		color = rndseed*1.36785;	tmp = color; color -= tmp;
		optical = rndseed*3.13771;	tmp = optical; optical -= tmp;
		engine = rndseed*4.75835;	tmp = engine; engine -= tmp;

		engine = 1.0 + engine*level;
		optical += 1.0;

		aiLevel = level;

	        setEventMask(EVENT_COMMAND);
	}

	public Bot(int dbId, int rndseed, float col, float opti, float eng, float ai)
	{
		this(dbId);

        	seed = rndseed;
	        name = constructName(seed);

		color = col;
		optical = opti;
		engine = eng;
		aiLevel = ai;

		setEventMask(EVENT_COMMAND);
	}

	//RAXAT: some more helpful stuff for bots
	public Vehicle getCar(GameRef map)
	{
		Vehicle result;

		if(profile)
		{
			result = Vehicle.load( profile.vhcPath, map );
			//result.loadSkin( profile.vhcSkin );
			result.chassis.AI_steerhelp = 1; //to stabilize bot vehicles

			return result;
		}

		return null;
	}

	//RAXAT: including improved version of createCar
	public void createCar(GameRef map, int fictive)
	{
		Vehicle vhc;

		if(profile)
		{
			vhc = Vehicle.load(profile.vhcPath, map);
			vhc.loadSkin(profile.vhcSkin);

			createCar(map, vhc);
		}
	}

	public RenderRef getMarker()
	{
		if(club == 2)	return Marker.RR_CAR3;
		if(club == 1)	return Marker.RR_CAR2;

		return Marker.RR_CAR1;
	}

	public RenderRef getMarker(int idx)
	{
		switch(idx)
		{
			case 0:
				return Marker.RR_CAR1;
				break;
			case 1:
				return Marker.RR_V4_BOT;
				break;
		}

		return Marker.RR_CAR1;
	}

	public float getCarPrestige()
	{
		if(!car && botVd) return botVd.estimatePrestige();

		return super.getCarPrestige();
	}				

	public void setDriverObject(int id)
	{
		driverID=id;		
	}

	//letrehozas, megszuntetes:
	//fajlbol:
	public void createCar(GameRef map, String filename)
	{
		Vehicle vhc = Vehicle.load(filename, this);

		if(!vhc) System.exit("Fatal: Cannot create car using file " + filename);

		createCar(map, vhc);
	}

	//automatikusan:
	public void createCar( GameRef map )
	{
		Vehicle vhc;

		if( botVd )
		{
			vhc = new Vehicle( this, botVd.id,  botVd.colorIndex, botVd.optical, botVd.power, botVd.wear, botVd.tear );
		}

		//RAXAT: remarked, for safety
		/*
		else
		{
			System.exit( "Bot.createCar(): VehicleDescriptor null" );
		}
		*/

		//RAXAT: and additional safety patch
		if(vhc)	createCar( map, vhc );
	}

	//atadjuk neki direktbe:
	public void createCar( GameRef map, Vehicle c )
	{
        	world=map;
		deleteCar();

		car = c;
		debugid = car.id();

		enterCar( car );
	}


	public void deleteCar()
	{
		if( car )
		{
			leaveCar(0);

			//kocsi bezuzva
			if( car.id() )
//			System.log( "deleted: " + car.id() );
			car.destroy();
			car=null;       //eleg lenne csak ez, majd teszteljuk le!
		}
	}

	public void enterCar( Vehicle c )
	{
		if (!c)	return;

		c.setTransmission( Vehicle.TRANSMISSION_SEMIAUTO );
        
        	//megcsinaljuk az agyat
		//if( brain )	//pl volt mar ha epp forgalomba maszott vissza a dummycar, es kihivtuk
		//	brain.destroy();


		brain = new GameRef( world, sl:0x0000006Er, "", "BOTBRAIN");
		controller = brain;

		//RAXAT: v2.3.1, AI Level patch
		if(aiLevel) brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_level " + aiLevel );
		if(profile) brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_level " + profile.skill );

		//ultetunk bele emberket is:
		c.command( "corpse 0 0" );	//kiszedjuk ha mar volt hullaja :)

		int did;
		if(!Player.c_ghost) did = driverID;
		render = new RenderRef( world, did, "botfigura" );
		brain.queueEvent( null, GameType.EVENT_COMMAND, "renderinstance " + render.id() );

		//ezt a kocsit vezessed ni:
		brain.queueEvent( null, GameType.EVENT_COMMAND, "controllable " + c.id() );
		brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_suspend" );

		//addNotification( c, EVENT_COMMAND, EVENT_SAME, null );
		//setEventMask( EVENT_COMMAND );
	}

	public void leaveCar( int leaveInTraffic )
	{
		//clearEventMask( EVENT_COMMAND );

		if( !leaveInTraffic )
		{
			leaveTraffic();	//kiszedi a forgalombol, ha ott volt
		}
	
		//sofor killed
		if( render )
			render.destroy();

		//agya is killed
		if (brain)
		{
			brain.queueEvent( null, GameType.EVENT_COMMAND, "leave " + car.id() );
			brain.destroy();
			brain = null;
			controller = null;
		}

		//remNotification( car, EVENT_COMMAND );

		if( leaveInTraffic )
		{
			//native gamerefbol keszult vehicles:
			//ha forgalomba engedjuk vissza, ki kell torolnunk a Vehicle-bol, kulonben torolni fogja a gc!
			if( car )	//ez miert kelll???!!!
			{
				car.release();
				car=null;
			}
		}
		
	}

	public void handleEvent( GameRef obj_ref, int event, int param )
	{
		if( event == EVENT_TIME )
		{
			clearEventMask( EVENT_TIME );

			reJoinTraffic();
		}
	}

	//itt szinkronizalunk az automatikus forgalomkezeles esemenyeihez:
	//ha karambolozik az auto, automatikusan kikerul a forgalombol, (denes kuld infot)
	//visszakuldom a forgalomba, (AI_GoToTraffic)
	//majd visszakerul a forgalomba. (klampi csinalja)
	public void handleEvent( GameRef obj_ref, int event, String param )
	{
		int did;
		if(!Player.c_ghost) did = driverID;

		if( event == EVENT_COMMAND )
		{
			String t0 = param.token(0);
			if( t0 == "ai_info_entertraffic" )
			{
				traffic_id = param.token(1).intValue();

				if( imaPoliceDriver )
				{
					remNotification( car, EVENT_COMMAND );
					leaveCar(1); //lassu! de ez nincs olyan gyakran...
					//majd a traffic id miatt eszreveszi a city!
				}
				else
				{
					//leaveCar(1); lassu!

					brain.destroy();
					brain = null;
					controller = null;

					RenderRef render = new RenderRef( world, did, "botfigura-leavetraffic" );
					car.command( "corpse 0 " + render.id() );

					car.release();
					car = null;

				}
			}
			else
			if( t0 == "ai_info_leavetraffic" )
			{
				traffic_id = 0;

				car = new Vehicle(dummycar);
				//enterCar( car ); lassu!

				brain = new GameRef( world, sl:0x0000006Er, "", "BOTBRAIN");
				controller = brain;

				dummycar.command( "corpse 0 0" );
				RenderRef render = new RenderRef( world, did, "botfigura-leavetraffic" );
				brain.command( "renderinstance " + render.id() );

				brain.queueEvent( null, GameType.EVENT_COMMAND, "controllable " + car.id() );


				stop();

				setEventMask( EVENT_TIME );
				addTimer( 3.0, 0 );
			}
		}
	}


	//parancsok:
	public void setTrafficBehaviour( int mode )
	{
		if( traffic_id && world instanceof GroundRef )
		{
			((GroundRef)world).setTrafficCarBehaviour( traffic_id, mode );
		}
	}

	//az adott poziciohoz legkozelebbi keresztezodesnel csatlakozik a forgalomhoz
	public void joinTraffic( Vector3 pos )
	{
		if( !traffic_id )
		{
			beStupid();
			traffic_id = world.addTrafficCar( car, pos );
	        }
	}

	public void reJoinTraffic()
	{
		if( !traffic_id )
		{
			//leaveTraffic(); elvarjuk, hogy hivaskor mar ne legyen benne
			if (brain)
				brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_GoToTraffic" );
			//ha odaert, onmukododen traficce valik... ekkor notificationt kapok rola (az ai lesuspendelodik, ha en csinaltam)
		}
	}

	public void leaveTraffic()
	{
        	if( traffic_id )
        	{
			world.remTrafficCar( traffic_id );
			traffic_id = 0;
		}
	}

	public void beStupid()
	{
		brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_suspend" );
	}

	public void followCar( GameRef playercar, float dest )
	{
		leaveTraffic();
		brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_follow 0,0," + dest + " " + playercar.id() );
	}

	public void stopCar( GameRef playercar )
	{
		leaveTraffic();
		brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_follow 0,0,-2 " + playercar.id() );
	}

	public void startRace( Vector3 destination, Racer opponent )
	{
		leaveTraffic();
		brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_race " + destination.toString() + " " + opponent.car.id() );
	}

	public void driveStraightTo( Vector3 destination )
	{
		leaveTraffic();
		brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_GoToTarget " + destination.toString() );
	}

	public void pressHorn()
	{
		if (horn) return;
//              brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_horn 1" );
		if (car)
		{
			car.queueEvent( null, GameType.EVENT_COMMAND, "sethorn 1" );
			horn = 1;
		} else
		if (dummycar)
		{
			dummycar.queueEvent( null, GameType.EVENT_COMMAND, "sethorn 1" );
			horn = 1;
		}
	}

	public void releaseHorn()
	{
		if (!horn) return;
//              brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_horn 0" );
		if (car)
		{
			car.queueEvent( null, GameType.EVENT_COMMAND, "sethorn 0" );
			horn = 0;
		} 
		else
		if (dummycar)
		{
			dummycar.queueEvent( null, GameType.EVENT_COMMAND, "sethorn 0" );
			horn = 0;
		}
	}

	public void stop()
	{
		leaveTraffic();
		if (brain)
			brain.queueEvent( null, GameType.EVENT_COMMAND, "AI_stop" );
		//beStupid();
	}

	public void followSplineTrack( float width, String splineFile, int oppCarId )
	{
		brain.queueEvent( null, EVENT_COMMAND, "AI_spline " + width + " " + splineFile + " " + oppCarId );
	}

	public String constructName( int seed )
	{
		String[] pre = new String[20];
		pre[0]="Dr. ";
		pre[1]="Old ";

		String[] first = new String[12];
		first[0]="John ";
		first[1]="Mike ";
		first[2]="Bill ";
		first[3]="Stewart ";
		first[4]="Joe ";
		first[5]="Sam ";
		first[6]="Alan ";
		first[7]="Marc ";
		first[8]="Jason ";
		first[9]="Sean ";
		first[10]="Jim ";
		first[11]="Leo ";

		String[] mid = new String[40];
		mid[0]="'Lucky' ";
		mid[1]="'Speedy' ";
		mid[2]="'Swifty' ";
		mid[3]="'Ugly' ";
		mid[4]="'Bighead' ";
		mid[5]="'Bugsy' ";
		mid[6]="'Hammerhead' ";
		mid[7]="'Russian' ";
		mid[8]="'Scottish' ";
		mid[9]="'Danish' ";
		mid[10]="'Looser' ";
		mid[11]="'GearHead' ";
		mid[12]="'Genius' ";

		String[] last = new String[22];
		last[0]="Galahad";
		last[1]="Lloyd";
		last[2]="Robertson";
		last[3]="Cocker";
		last[4]="Johnson";
		last[5]="Livingstone";
		last[6]="Dunnigan";
		last[7]="Little";
		last[8]="Lembcke";
		last[9]="Evans";
		last[10]="Murphy";
		last[11]="Speaker";
		last[12]="Sterkovic";
		last[13]="Scott";
		last[14]="McDonell";
		last[15]="Bonnett";
		last[16]="Bakers";
		last[17]="Perkins";
		last[18]="Lennon";
		last[19]="Polansky";
		last[20]="O'Connor";
		last[21]="Kozak";

		String[] post = new String[20];
		post[0]=" jr.";

		return //pre[(seed*3)%pre.length] +
		first[(seed*19)%first.length] +
		mid[(seed*31)%mid.length] +
		last[(seed*23)%last.length] +
		post[(seed*17)%post.length];
	}

	public String getPrestigeString()
        {
               	return getPrestigeString(-1);
        }

	public String getPrestigeString(int racemode)
	{
		int pprestige, cprestige, aprestige;
		pprestige = prestige*PRESTIGE_SCALE;

		if( car )
			cprestige = car.getPrestige() * VHC_PRESTIGE_SCALE;
                else
                if (racemode == 0 || racemode == 1)
                  cprestige = nightVd.estimatePrestige() * VHC_PRESTIGE_SCALE;
                else
//                if (racemode == 4 || racemode == 5)
                  cprestige = botVd.estimatePrestige() * VHC_PRESTIGE_SCALE;

		aprestige = pprestige*0.5 + cprestige*0.5;

		return aprestige + " (" + pprestige +  ":" + cprestige + ")";
//		return pprestige +  "/" + cprestige;
	}

	public void save( File saveGame )
	{
               super.save(saveGame);

		int save_ver = 1;
		saveGame.write(save_ver);
		if (save_ver >= 1)
		{
			saveGame.write(nightWins);
			saveGame.write(nightLoses);
			saveGame.write(bestNightQM);
		}
	}

	public void load( File saveGame )
	{
               super.load(saveGame);

		int save_ver;
		save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			nightWins = saveGame.readInt();
			nightLoses = saveGame.readInt();
			bestNightQM = saveGame.readFloat();
		}
	}
}

//RAXAT: v2.3.1, personal data card of each racer
public class botCard
{
	String name_f, name_s, name_n; //first, second, nick
	String vhcPath, vhcSkin;
	int rating;
	int dbID;
	String skill;

	public botCard( String nf, String ns, String nn, String vhcp, String vhcs, int r, String s )
	{
		name_f = nf;	name_s = ns;	name_n = nn;
		vhcPath = vhcp;	vhcSkin = vhcs;
		rating = r;	skill = s;
		dbID = r;
	}

	public String getName()
	{
		return name_f + " " + name_s;
		return null;
	}

	public String getNick()
	{
		return name_n;
		return null;
	}

	public String getFullName()
	{
		return name_f + " " + name_n + " " + name_s;
		return null;
	}

	public int getPhoto()
	{
		return Racer.RID_FEJ + dbID;
		return 0;
	}

	public int getRating()
	{
		return rating;
		return 0;
	}
}

//RAXAT: v2.3.1, database for all game opponents
public class database
{
	botCard[] card;

	public database()
	{
		card = new botCard[59];

		//green slip
		card[18] = new botCard( "Matt", "Wiley", "'RallyBally'", GameLogic.dbCarDir + "RallyBally", GameLogic.dbSkinDir + "RallyBally",				 18, " 7.5 0.42" ); //enula WRST 665HP, class A
		card[17] = new botCard( "Hans-Peter", "Groeneboer", "'Xenu'", GameLogic.dbCarDir + "Xenu", GameLogic.dbSkinDir + "Xenu",				 	 17, " 7.2 0.41" ); //baiern GTIII 621HP, class A
		card[16] = new botCard( "Chad", "Alan", "'King'", GameLogic.dbCarDir + "King", GameLogic.dbSkinDir + "King",								 16, " 7.2 0.31" ); //coyot TX2200GT 514HP class C
		card[15] = new botCard( "Daniel", "Pudoff", "'Grizzly'", GameLogic.dbCarDir + "Grizzly", GameLogic.dbSkinDir + "Grizzly",					 15, " 7.1 2.36" ); //MC GT 360HP
		card[14] = new botCard( "Tee Jay", "Aneim", "'Cecil'", GameLogic.dbCarDir + "Cecil", GameLogic.dbSkinDir + "Cecil",						 	 14, " 6.9 0.35" ); //coyot C1500 389HP, class C
		card[13] = new botCard( "Mark", "Heiland", "'Gangstaog'", GameLogic.dbCarDir + "Gangstaog", GameLogic.dbSkinDir + "Gangstaog",				 13, " 6.7 0.34" ); //yotta 3.6TT 346HP, class D
		card[12] = new botCard( "Garri", "Agaian", "'HellBoy'", GameLogic.dbCarDir + "HellBoy", GameLogic.dbSkinDir + "HellBoy",					 12, " 6.5 0.3"  ); //nonus GT2 291HP, class C
		card[11] = new botCard( "Ken", "Van Nieuwamerongen", "'Hard'", GameLogic.dbCarDir + "Hard", GameLogic.dbSkinDir + "Hard",					 11, " 6.2 0.31" ); //ninja thatch 298HP, class D
		card[10] = new botCard( "Ruben", "Gundersen", "'Koda'", GameLogic.dbCarDir + "Koda", GameLogic.dbSkinDir + "Koda",							 10, " 5.9 0.33" ); //SD500 419HP
		card[9]  = new botCard( "Einimas", "Juravicius", "'einimas'", GameLogic.dbCarDir + "Einimas", GameLogic.dbSkinDir + "Einimas",				 9,  " 5.9 0.32" ); //remo 1.8 127HP, class E
		card[8]  = new botCard( "Odd", "Mathisen", "'Nixxen'", GameLogic.dbCarDir + "Nixxen", GameLogic.dbSkinDir + "Nixxen",						 8,  " 5.2 0.32" ); //remo 1.8 137HP, class E
		card[7]  = new botCard( "Marton", "Kovacs", "'Maresz'", GameLogic.dbCarDir + "Maresz", GameLogic.dbSkinDir + "Maresz",						 7,  " 5.1 0.34" ); //ninja pl XT 251HP, class D
		card[6]  = new botCard( "Kamil", "Kubera", "'Racko'", GameLogic.dbCarDir + "Racko", GameLogic.dbSkinDir + "Racko",							 6,  " 5.0 0.34" ); //SD500 203HP //add more power!
		card[5]	 = new botCard( "Dale", "Simpson", "'Boost'", GameLogic.dbCarDir + "Boost", GameLogic.dbSkinDir + "Boost",							 5,  " 4.6 0.33" ); //zed RC 137HP, class E
		card[4]  = new botCard( "Martin", "Piek", "'TyreShredder'", GameLogic.dbCarDir + "TyreShredder", GameLogic.dbSkinDir + "TyreShredder",		 4,  " 4.4 0.33" ); //csport 2.5 164HP, class D
		card[3]  = new botCard( "Joe", "BZ", "'RedlineCodrac'", GameLogic.dbCarDir + "RedlineCodrac", GameLogic.dbSkinDir + "RedlineCodrac",		 3,  " 4.2 0.33" ); //Remo
		card[2]  = new botCard( "Nikolajs", "Trubachovs", "'Smertokog'", GameLogic.dbCarDir + "Smertokog", GameLogic.dbSkinDir + "Smertokog",		 2,  " 4.1 0.33" ); //sunset
		card[1]  = new botCard( "Rick", "Lane", "'Warhead'", GameLogic.dbCarDir + "Warhead", GameLogic.dbSkinDir + "Warhead",						 1,  " 4.0 0.33" ); //codrac
		card[0]  = new botCard( "Cerhi", "Kazama", "'cerhi'", GameLogic.dbCarDir + "cerhi", GameLogic.dbSkinDir + "cerhi",					 		 0,  " 4.0 0.32" ); //ST9

		//blue cheetah
		card[38] = new botCard( "Kevin", "Walker", "'Scarface'", GameLogic.dbCarDir + "Scarface", GameLogic.dbSkinDir + "Scarface",					 38, " 9.9 0.45" ); //nonus GT2 819HP, class A
		card[37] = new botCard( "Olegas", "Bilyk", "'Dogbrain'", GameLogic.dbCarDir + "Dogbrain", GameLogic.dbSkinDir + "Dogbrain",					 37, " 8.6 0.52" ); //naxas EE 753HP, class A
		card[36] = new botCard( "Kevin", "Oxford", "'TunerBoy'", GameLogic.dbCarDir + "TunerBoy", GameLogic.dbSkinDir + "TunerBoy",						 36, " 8.6 0.43" ); //Focer WRC 626HP, class A
		card[35] = new botCard( "Aaditya", "Nair", "'Coffeecat'", GameLogic.dbCarDir + "Coffeecat", GameLogic.dbSkinDir + "Coffeecat",				 35, " 8.5 0.45" ); //prime DLH500 562HP, class B
		card[34] = new botCard( "Thomas ", "Csaba", "'Tamas'", GameLogic.dbCarDir + "Tamas", GameLogic.dbSkinDir + "Tamas",		 					 34, " 8.1 0.42" ); //kurumma Z3 518HP, class C
		card[33] = new botCard( "Caleb", "Goley", "'skullcrusher'", GameLogic.dbCarDir + "skullcrusher", GameLogic.dbSkinDir + "skullcrusher",		 33, " 7.5 0.43" ); //stallion 3.2 568HP, class B
		card[32] = new botCard( "Charlie", "Harper", "'BloodRed'", GameLogic.dbCarDir + "BloodRed", GameLogic.dbSkinDir + "BloodRed",				 32, " 7.4 0.47" ); //furrano GT54 587HP, class B
		card[31] = new botCard( "Roman", "Abadilla", "'NFKRZ'", GameLogic.dbCarDir + "NFKRZ", GameLogic.dbSkinDir + "NFKRZ",				 		 31, " 7.8 0.48" ); //codrac GT 519HP, class B
		card[30] = new botCard( "Felipper", "Massa", "'Supercar'", GameLogic.dbCarDir + "Supercar", GameLogic.dbSkinDir + "Supercar",		 		 30, " 7.5 0.42" ); //zed RC 583HP
		card[29] = new botCard( "Oskars", "Gravelsins", "'EvilMcSheep'", GameLogic.dbCarDir + "EvilMcSheep", GameLogic.dbSkinDir + "EvilMcSheep",	 29, " 7.3 0.39" ); //coyot T1800S 485HP, class C
		card[28] = new botCard( "Akos", "Divianszky", "'diviaki'", GameLogic.dbCarDir + "diviaki", GameLogic.dbSkinDir + "diviaki",					 28, " 6.8 0.37" ); //stallion 3.2 289HP, class C
		card[27] = new botCard( "Bartosz", "Bieszka", "'Wichur'", GameLogic.dbCarDir + "Wichur", GameLogic.dbSkinDir + "Wichur",					 27, " 6.4 0.32" ); //kurumma Z3 346HP, class C
		card[26] = new botCard( "Joao", "Mendes", "'Gorgoil'", GameLogic.dbCarDir + "Gorgoil", GameLogic.dbSkinDir + "Gorgoil",						 26, " 6.5 0.33" ); //ST9 fusion 328HP, class D
		card[25] = new botCard( "Gabor", "Simon", "'sala'", GameLogic.dbCarDir + "sala", GameLogic.dbSkinDir + "sala",								 25, " 5.8 0.39" ); //einvagen 100GTK 168HP, class D
		card[24] = new botCard( "Cody", "Clarkie", "'Canadia'", GameLogic.dbCarDir + "Canadia", GameLogic.dbSkinDir + "Canadia",					 24, " 6.9 0.31" ); //focer RC200, 187HP, class D
		card[23] = new botCard( "Adrian", "Zdanowicz", "'Silent'", GameLogic.dbCarDir + "Silent", GameLogic.dbSkinDir + "Silent",					 23, " 6.4 0.32" ); //SD750 263HP //add more power!
		card[22] = new botCard( "Renan", "Gallas", "'EQUAL2'", GameLogic.dbCarDir + "EQUAL2", GameLogic.dbSkinDir + "EQUAL2",						 22, " 6.2 0.28" ); //csport GTIII 295HP, class C
		card[21] = new botCard( "Brandon", "Moffatt", "'Vellon'", GameLogic.dbCarDir + "Vellon", GameLogic.dbSkinDir + "Vellon",					 21, " 6.2 0.33" ); //nonus SGT 219HP, class D
		card[20] = new botCard( "Charles", "Linck", "'OwnerKen'", GameLogic.dbCarDir + "OwnerKen", GameLogic.dbSkinDir + "OwnerKen",				 20, " 5.3 0.31" ); //ninja tourer 283HP, class D
		card[19] = new botCard( "Marc-Antoine", "Daniel", "'MadSlipknot'", GameLogic.dbCarDir + "MadSlipknot", GameLogic.dbSkinDir + "MadSlipknot",	 19, " 5.1 0.33" ); //coyot C1500 142HP, class E

		//red flames
		card[58] = new botCard( "Daniel", "Alabaster", "'Furball'", GameLogic.dbCarDir + "Furball", GameLogic.dbSkinDir + "Furball",				 58, " 10.0 1.0" ); //Baiern DTM 668HP, class A
		card[57] = new botCard( "Isaac", "Conrad", "'baker219'", GameLogic.dbCarDir + "baker219", GameLogic.dbSkinDir + "baker219",					 57, " 10.5 0.6" ); //whisper Q1000XL 2707HP, class S
		card[56] = new botCard( "David", "Singh", "'KINGH'", GameLogic.dbCarDir + "Singh", GameLogic.dbSkinDir + "Singh",						 	 56, " 10.0 1.0" ); //whisper Q1000XL 2707HP, class S
		card[55] = new botCard( "James", "Morris", "'Forester'", GameLogic.dbCarDir + "Forester", GameLogic.dbSkinDir + "Forester",					 55, " 12.0 1.0" ); //duhen CDVC 492HP
		card[54] = new botCard( "Eddy", "Lucas", "'gomeS'", GameLogic.dbCarDir + "gomeS", GameLogic.dbSkinDir + "gomeS",							 54, " 11.0 1.0" ); //furrano GTS 1629HP, class S
		card[53] = new botCard( "David", "Donkel", "'Draeghonov'", GameLogic.dbCarDir + "Draeghonov", GameLogic.dbSkinDir + "Draeghonov",			 53, " 11.0 1.0" ); //SD750 1243HP, class S
		card[52] = new botCard( "Mike", "Bones", "'Kens'", GameLogic.dbCarDir + "Kens", GameLogic.dbSkinDir + "Kens",								 52, " 8.8 0.41" ); //whisper D8800 1170HP, class S
		card[51] = new botCard( "Luiz", "Fernando", "'Maverick'", GameLogic.dbCarDir + "Maverick", GameLogic.dbSkinDir + "Maverick",			 	 51, " 8.7 0.43" ); //prime DLH700 1501HP, class S
		card[50] = new botCard( "Hector", "Toledo", "'NepTune'", GameLogic.dbCarDir + "NepTune", GameLogic.dbSkinDir + "NepTune",				 	 50, " 8.7 0.45" ); //naxas LX4000 1064HP, class S
		card[49] = new botCard( "Dennis", "Smith", "'Ghost Rider'", GameLogic.dbCarDir + "GhostRider", GameLogic.dbSkinDir + "GhostRider",			 49, " 8.5 0.41" ); //naxas tornado 723HP, class A
		card[48] = new botCard( "Emanuel", "Dell Ascenza", "'loryo'", GameLogic.dbCarDir + "loryo", GameLogic.dbSkinDir + "loryo",					 48, " 8.5 0.36" ); //prime DLH500 700HP, class A
		card[47] = new botCard( "Alan", "Vispo", "'Phantom'", GameLogic.dbCarDir + "Phantom", GameLogic.dbSkinDir + "Phantom",						 47, " 8.3 0.36" ); //badge GTO 736HP, class A
		card[46] = new botCard( "Ricardo", "Louros", "'PrimoShot'", GameLogic.dbCarDir + "PrimoShot", GameLogic.dbSkinDir + "PrimoShot",		 	 46, " 8.5 0.45" ); //MC GTLE 1086HP, class S
		card[45] = new botCard( "Marcus", "Lam", "'Random41'", GameLogic.dbCarDir + "Random41", GameLogic.dbSkinDir + "Random41",				 	 45, " 7.7 0.33" ); //nonus SGT 723HP, class A
		card[44] = new botCard( "Nate", "Ryan", "'Redux'", GameLogic.dbCarDir + "Redux", GameLogic.dbSkinDir + "Redux",								 44, " 7.5 0.34" ); //badge 67 662HP, class B
		card[43] = new botCard( "Eric", "Palmer", "'Drifter'", GameLogic.dbCarDir + "Drifter", GameLogic.dbSkinDir + "Drifter",				 		 43, " 7.5 0.32" ); //whisper R1 497HP, class C
		card[42] = new botCard( "Michael", "McCoy", "'SharkBull'", GameLogic.dbCarDir + "SharkBull", GameLogic.dbSkinDir + "SharkBull",			 	 42, " 7.2 0.32" ); //MC GTLE 588HP, class B
		card[41] = new botCard( "Mikey", "Casorelli", "'Soulkast'", GameLogic.dbCarDir + "Soulkast", GameLogic.dbSkinDir + "Soulkast",				 41, " 7.1 0.31" ); //badge 67 419HP
		card[40] = new botCard( "Tommy", "Clark", "'Wypple'", GameLogic.dbCarDir + "Wypple", GameLogic.dbSkinDir + "Wypple",					 	 40, " 6.8 0.35" ); //yotta 3.6 366HP, class C
		card[39] = new botCard( "Bill", "Vogel", "'Charger'", GameLogic.dbCarDir + "Charger", GameLogic.dbSkinDir + "Charger",					 	 39, " 6.5 0.36" ); //SD750 271HP //add more power!
	}

	public botCard getProfile(int id)
	{
		return card[id];

		return null;
	}

	public int getPhoto(int id)
	{
		int result;
		result = Racer.RID_FEJ+id;
		return result;

		return 0;
	}

	public int getRating(int id)
	{
		int result;
		result = card[id].rating;
		return result;

		return 0;
	}
}