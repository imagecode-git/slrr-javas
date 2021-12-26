package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.game.parts.*;
import java.game.parts.enginepart.*;

//RAXAT: next-generation game module, collects and describes all existing racing events
public class EventList implements GameState
{
	final static int MODE_AMATEUR	= 0x001;
	final static int MODE_SPORT		= 0x002;
	final static int MODE_FREERIDE	= 0x004;
	final static int MODE_DEBUG		= 0x008;

	int mode;
	Generator eGen;

	public EventList(int m)
	{
		mode = m;
	}

	public void enter( GameState prevState )
	{
		eGen = new Generator(mode);
		eGen.myPrevState = prevState;
		eGen.show();
	}

	public void exit( GameState nextState )
	{
		Sound.changeMusicSet( Sound.MUSIC_SET_NONE );
		eGen.hide();
		eGen = null;
	}
}

public class Generator extends Dialog
{
	static	GameState myPrevState;

	final static int CMD_PREV  = 0x10A;
	final static int CMD_NEXT  = 0x10B;
	final static int CMD_EXIT  = 0x10C;
	final static int CMD_INFO  = 0x10D;
	final static int CMD_RACE  = 0x10E;
	final static int CMD_PRIZE = 0x10F;

	Thread	evgThread;
	int	tMethods = 10; //amount of THOR methods

	float	s_spacing = 0.625; //for shield generator
	float	s_x; //shield initial x

	ResourceRef res_upper_frame = new ResourceRef(frontend:0xC0A3r);
	ResourceRef res_lower_frame = new ResourceRef(frontend:0xC0A0r);
	ResourceRef[] res_shields = new ResourceRef[5];
	ResourceRef[] res_shields_clone = new ResourceRef[5];
	ResourceRef res_track_minimap;
	ResourceRef res_stdBck = new ResourceRef(frontend:0xC00Ar);

	Rectangle stdBck;
	Rectangle upper_frame, lower_frame, trackPic, track_minimap, raceTitle;
	Rectangle trackPic_clone, track_minimap_clone, raceTitle_clone;
	Rectangle[] shields = new Rectangle[5];
	Rectangle[] shields_clone = new Rectangle[5];

	float anim_rect_delta = 6.7; //distance that travels each rectangle while being animated left/right
	float anim_rect_speed = 0.5;
	int anim_rect_steps = 20; //BASE steps!! they will be automatically recalculated and corrected using anim_rect_speed in this code
	int anim_text_speed = 1; //must be the same in reData

	int mode;
	int anim_rect_status = -1; //describes which rectangle bundle is in use right now (-1: clone, 1: normal rect)
	int firstRun = 1; //patch to fix errors connected with launching career events

	Text	headerTxt, eventNumTxt, trackNameTxt;

	//left/right buttons
	Menu arrow_left, arrow_right, button_close, button_gorace;
	Style arrow_style, button_style;
	Gadget button_race;

	Requirements reData;
	CareerEvent cEvent, cEvent_clone;

	int goRaceStatus;
	int currentEvent = 1;
	CareerEvent[] events;

	Player player;
	
	public Generator(int m)
	{
		super(GameLogic.player.controller, DF_FULLSCREEN|DF_LOWPRI|DF_HIDEPOINTER, null, null);
		player = GameLogic.player;
		osd.globalHandler = this;
		mode = m;
		Input.cursor.enable(1);
	}

	public void show()
	{
		osd.darkStatus = 1; //force fade-IN
		osd.darken(16,1);
		
		//--------------------------
		if(!Integrator.debugLoadCareer)
		{
			//career event pack generation
			/*
			CareerEvent ce = new CareerEvent();
			String path = "sl/Scripts/game/CareerEvents/titles_dev/";
			ce.generateCareerPackage("stdpack_231", path);
			*/

			Integrator.debugLoadCareer++;
		}
		//--------------------------

		if(mode != EventList.MODE_FREERIDE)
		{
			if(GameLogic.player.lastPlayedEvent) currentEvent = GameLogic.player.lastPlayedEvent; //buggy last event jump! game simply can't jump at last event index
		}

		collectEvents();
		if(!cEvent_clone) cEvent_clone = cEvent; //patch: fictive cEvent for special cases, when currentEvent = events.length

		trackPic = osd.createRectangle( 0.0, 0.6, 2.6, 1.43, -1, cEvent.e_bck, 0 );
		trackPic_clone = osd.createRectangle( 0.0+anim_rect_delta, 0.6, 2.6, 1.43, -1, cEvent_clone.e_bck, 0 );

		raceTitle = osd.createRectangle( 0.065, -0.0825, 1.65, 0.17, 1, cEvent.title_res, 0 );
		raceTitle_clone = osd.createRectangle( 0.065+anim_rect_delta, -0.0825, 1.65, 0.17, 1, cEvent_clone.title_res, 0 );

		upper_frame = osd.createRectangle( 0.0, 1.325, 2.0, 0.22, 0, res_upper_frame, 0 );
		lower_frame = osd.createRectangle( 0.0, -0.588, 1.488, 0.7, 2, res_lower_frame, 0 );

		track_minimap = osd.createRectangle( 1.5, 0.52, 0.26, 0.47, 1, cEvent.e_minimap, 0 );
		track_minimap_clone = osd.createRectangle( 1.5+anim_rect_delta, 0.52, 0.26, 0.47, 1, cEvent_clone.e_minimap, 0 );

		addShields();
		addButtons();

		String title = "EVENTS - AMATEUR RACING";
		if(mode == EventList.MODE_FREERIDE) title = "CHOOSE MAP";
		headerTxt = osd.createText( title, Frontend.largeFont, Text.ALIGN_LEFT, -0.98, -0.98 );
		eventNumTxt = osd.createText( currentEvent + "/" + events.length, Frontend.largeFont, Text.ALIGN_LEFT, -0.98, -0.81 );
		trackNameTxt = osd.createText( cEvent.e_trackName, Frontend.largeFont_strong, Text.ALIGN_RIGHT, 0.83, -0.81 );

		//animation speed for animated text instances
		trackNameTxt.a_speed = anim_text_speed;
		eventNumTxt.a_speed = anim_text_speed;

		reData = new Requirements(osd, cEvent, mode);

		if(!System.nextGen())
		{
			Sound.changeMusicSet( Sound.MUSIC_SET_GARAGE );
		}
		else
		{
			//build 900, direct music play, track#0: EventList
			Sound.changeMusicSet( Sound.MUSIC_SET_MISC );
			Sound.firstTrack();
		}

		osd.forceNoInitFocus = 1;
		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_CANCEL, this );
		super.show(); //this will activate and show osd
		osd.forceNoInitFocus = 0; //just to build a menu without any focused button at show up

		if(Player.c_eventlist) //cheat keys
		{
			osd.createHotkey( Input.RCDIK_R, Input.KEY|Osd.HK_STATIC, CMD_RACE, this ); //bypass requirements and join any desired race
			osd.createHotkey( Input.RCDIK_P, Input.KEY|Osd.HK_STATIC, CMD_PRIZE, this ); //aquire prize without race
		}

		evgThread = new Thread( this, "event list animation thread", 1 ); //extended! thread-oriented methodology (THOR), no injections
		evgThread.setPriority(Thread.MAX_PRIORITY);
		evgThread.start();

		//initialize THOR methods
		for(int i=0; i<(tMethods+1); i++)
		{
			evgThread.addMethod(i);
		}

		anim_rect_status *= (-1); //reverting status to -1 for operating with cloned rectangles

		//debug:
		//CareerEvent c = new CareerEvent();
		//c.printVehicleClasses();
		
		if(mode != EventList.MODE_FREERIDE && player.checkHint(Player.H_EVENTLIST)) new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "RACING EVENTS", "Win gold cups and earn special rewards in racing events to become the ultimate racer! Many events have special requirements to participate in: \n \n Wins in ROC - win the Race Of Champions once or multiple times \n \n Car model - specific car models required \n \n Brands - you need to have a car of some particular brand \n \n Club rating - beat other club racers in the city night races to increase it \n \n Transmission - install different gearbox onto your engine to meet this requirement \n \n No nitrous - remove N2O canisters from your car \n \n Vehicle mass - disassemble some parts from your car to decrease its weight \n \n Naturally aspirated engines only - no boost, remove turbochargers and superchargers from the engine \n \n Engine power - fine-tune your engine to increase the power or install cheaper parts to decrease it \n \n Only street legal vehicles - check car information, find parts of your car that are not street legal and remove them \n \n Clean driving - no offroad driving in the event").display();
		
		Input.cursor.enable(1);
	}

	public void hide()
	{
		evgThread.stop();
		evgThread=null;
		
		super.hide();
		Input.cursor.enable(0);
	}

	//this creates all 5 shields, but each of them will have transparent texture
	public void addShields()
	{
		s_x = 1.925;

		ResourceRef def = Osd.RRT_GHOST; //default transparent image

		getShieldData(cEvent, res_shields);
		getShieldData(cEvent_clone, res_shields_clone);

		shields[0] = osd.createRectangle( s_x, 0.0, 0.12, 0.22, 1, def, 0 );
		shields_clone[0] = osd.createRectangle( s_x+anim_rect_delta, 0.0, 0.12, 0.22, 1, def, 0 );

		for(int j=1; j<5; j++)
		{
			shields[j] = osd.createRectangle( addSeparator(), 0.0, 0.12, 0.22, 1, def, 0 );
			shields_clone[j] = osd.createRectangle( s_x+anim_rect_delta, 0.0, 0.12, 0.22, 1, def, 0 ); //s_x is used since separator is already added to it
		}

		updateShields();
	}

	public void destroyShields()
	{
		for( int i=0; i<shields.length; i++ )
		{
			if(shields[i]) shields[i].finalize();
		}

		for( int i=0; i<shields_clone.length; i++ )
		{
			if(shields_clone[i]) shields_clone[i].finalize();
		}
	}

	//this does the texture update for all shields
	public void updateShields()
	{
		getShieldData(cEvent, res_shields);
		getShieldData(cEvent_clone, res_shields_clone);

		//now scan for shieldData: if it returns null, assign default transparent image res; otherwise - assign a normal image res (retrieved from shieldData)
		for(int i=0; i<5; i++)
		{
			if(anim_rect_status == -1)
			{
				if(shields[i])
				{
					if(res_shields[i]) shields[i].changeTexture(res_shields[i]);
						else shields[i].changeTexture(Osd.RRT_GHOST); //default_transparent.png
				}
			}
			else
			{
				if(shields_clone[i])
				{
					if(res_shields_clone[i]) shields_clone[i].changeTexture(res_shields_clone[i]);
						else shields_clone[i].changeTexture(Osd.RRT_GHOST);
				}
			}
		}
	}

	public void getShieldData(CareerEvent event, ResourceRef[] shieldData)
	{
		if(event.fogData[0] && event.fogData[1]) shieldData[0] = new ResourceRef(frontend:0xD10Ar); //fog
		else
		{
			if(event.raceTime > 0)
			{
				if(event.raceTime >= 8 && event.raceTime <= 19) shieldData[0] = new ResourceRef(frontend:0xD108r); //day time
				else shieldData[0] = new ResourceRef(frontend:0xD109r); //night
			}
			else shieldData[0] = null;
		}

		switch(event.drivetype)
		{
			case (CareerEvent.DRIVETYPE_FWD):
			shieldData[1] = new ResourceRef(frontend:0xD10Cr);
			break;

			case (CareerEvent.DRIVETYPE_RWD):
			shieldData[1] = new ResourceRef(frontend:0xD10Br);
			break;

			case (CareerEvent.DRIVETYPE_AWD):
			shieldData[1] = new ResourceRef(frontend:0xD10Dr);
			break;

			case 0:
			shieldData[1] = null;
			break;
		}

		switch(event.carClass)
		{
			case (CareerEvent.CAR_CLASS_E):
			shieldData[2] = new ResourceRef(frontend:0xD112r);
			break;

			case (CareerEvent.CAR_CLASS_D):
			shieldData[2] = new ResourceRef(frontend:0xD111r);
			break;

			case (CareerEvent.CAR_CLASS_C):
			shieldData[2] = new ResourceRef(frontend:0xD110r);
			break;

			case (CareerEvent.CAR_CLASS_B):
			shieldData[2] = new ResourceRef(frontend:0xD10Fr);
			break;

			case (CareerEvent.CAR_CLASS_A):
			shieldData[2] = new ResourceRef(frontend:0xD10Er);
			break;

			case (CareerEvent.CAR_CLASS_S):
			shieldData[2] = new ResourceRef(frontend:0xD113r);
			break;

			case 0:
			shieldData[2] = null;
			break;
		}

		if(event.careerGM.shieldIcon) shieldData[3] = event.careerGM.shieldIcon;
			else shieldData[3] = null;

		switch(CareerEvent.getEventStatus(event.eventName))
		{
			case (Gamemode.GMS_COMPLETED):
			shieldData[4] = new ResourceRef(frontend:0xD114r);
			break;

			case (Gamemode.GMS_FAILED):
			shieldData[4] = new ResourceRef(frontend:0xD115r);
			break;

			case (Gamemode.GMS_CUP_GOLD):
			shieldData[4] = new ResourceRef(frontend:0xD154r);
			break;

			case (Gamemode.GMS_CUP_SILVER):
			shieldData[4] = new ResourceRef(frontend:0xD155r);
			break;

			case (Gamemode.GMS_CUP_BRONZE):
			shieldData[4] = new ResourceRef(frontend:0xD156r);
			break;

			case 0: //not registered
			shieldData[4] = null;
			break;
		}
		
		//raw shieldData collected, now rearrange it (remove the gaps inside it, if found any)
		ResourceRef[] ripped = new ResourceRef[5];
		int idx = 0;
		for(int i=0; i<shieldData.length; i++) //scan array for gaps
		{
			if(shieldData[i])
			{
				ripped[idx] = shieldData[i]; //fill the new solid array
				idx++;
			}
		}
		
		for(int j=0; j<shieldData.length; j++) shieldData[j] = ripped[j]; //copy all elements of "solid" array back into shieldData
	}

	public float addSeparator()
	{
		s_x-=s_spacing/2/osd.vpHeight;

		return s_x;
	}

	public void addButtons()
	{
		arrow_style = new Style( 0.18, 0.18, Frontend.mediumFont, Text.ALIGN_LEFT, null );
		arrow_left = osd.createMenu( arrow_style, -0.995, -0.325, 0, Osd.MD_HORIZONTAL );
		arrow_left.addItem( new ResourceRef( frontend:0xD15Dr ), CMD_PREV, null, null, 1 ); //"<" button
		arrow_right = osd.createMenu( arrow_style, 0.89, -0.325, 0, Osd.MD_HORIZONTAL );
		arrow_right.addItem( new ResourceRef( frontend:0xD15Er ), CMD_NEXT, null, null, 1 ); //">" button

		float xpos = 0.91;
		if(mode != EventList.MODE_FREERIDE) xpos = 0.82;
		
		button_style = new Style( 0.12, 0.12, Frontend.mediumFont, Text.ALIGN_LEFT, null );
		button_close = osd.createMenu( button_style, xpos, -0.915, 0, Osd.MD_HORIZONTAL );
		if(mode != EventList.MODE_FREERIDE) button_close.addItem( new ResourceRef( frontend:0x9C11r ), CMD_INFO, "Additional information", null, 4 ); //"i" button
		button_close.addItem( new ResourceRef( frontend:0xD15Fr ), CMD_EXIT, "Close this menu", null, 4 ); //"X" button

		button_gorace = osd.createMenu( button_style, -0.005, 0.9, 0, Osd.MD_HORIZONTAL );
		button_race = button_gorace.addItem("", CMD_RACE);
		updateGoRace();
	}

	public void run()
	{
		for(;;)
		{
			//THOR methods
			if(evgThread.methodStatus(0) == 1) //fadeIn all animated texts
			{
				updateText(); //update data in each text instance

				eventNumTxt.restartAnimation();
				eventNumTxt.fadeIn();

				trackNameTxt.restartAnimation();
				trackNameTxt.fadeIn();

				for(int i=0; i<reData.maxlines; i++)
				{
					reData.conditions[i].restartAnimation();
					reData.conditions[i].fadeIn();
					reData.requirements[i].restartAnimation();
					reData.requirements[i].fadeIn();
				}

				evgThread.controlMethod(0,-1);
			}
			//end of THOR

			if(eventNumTxt.a_finished) evgThread.execute(0); //check params just for one text, since theese params are the same for all text animations in this class

			evgThread.sleep(10);
		}
	}

	public void prevEvent()
	{
		if( currentEvent > 1 )
		{
			currentEvent--;
			GameRef xa = new GameRef();

			cEvent = events[currentEvent-1];
			cEvent_clone = events[currentEvent-1];

			updateShields();
			updateData(-1);

			//---animation stuff
			animateRect(-1); //rectangles
			animateText(); //texts

			anim_rect_status *= (-1);

			if(firstRun) firstRun = 0;
		}
	}

	public void nextEvent()
	{
		if( currentEvent < events.length )
		{
			currentEvent++;
			GameRef xa = new GameRef();

			cEvent = events[currentEvent-1];
			if(cEvent) cEvent.init();

			cEvent_clone = events[currentEvent-1];
			if(cEvent_clone) cEvent_clone.init();

			updateData(1);

			//---animation stuff
			animateRect(1); //rectangles
			animateText(); //texts

			anim_rect_status *= (-1);

			if(firstRun) firstRun = 0;
		}
	}

	public void updateData(int dir)
	{
		if(anim_rect_status == -1) raceTitle.changeTexture(cEvent.title_res);
		else raceTitle_clone.changeTexture(cEvent_clone.title_res);

		reData.c_event = cEvent;

		updateTrack(1);
		updateShields();
		updateBadges();
		updateTrack(2);
	}

	public void updateText() //dedicated text update
	{
		eventNumTxt.changeText(currentEvent + "/" + events.length);
		trackNameTxt.changeText(cEvent.e_trackName);
		updateLine_L();
		updateLine_R();
	}

	public void updateTrack(int stage)
	{
		//special texture update order
		if(stage == 1)
		{
			if(anim_rect_status == -1) track_minimap.changeTexture(cEvent.e_minimap);
			else track_minimap_clone.changeTexture(cEvent_clone.e_minimap);
		}

		if(stage == 2)
		{
			if(anim_rect_status == -1) trackPic.changeTexture(cEvent.e_bck);
			else trackPic_clone.changeTexture(cEvent_clone.e_bck);
		}
	}

	public void updateLine_L()
	{
		for(int i=0; i<reData.maxlines; i++) reData.conditions[i].changeText(cEvent.conditionText[i]);
	}

	public void updateLine_R()
	{
		for(int i=0; i<reData.maxlines; i++) reData.requirements[i].changeText(cEvent.reqText[i]);
	}

	public void updateBadges()
	{
		for(int i=0; i<reData.maxlines; i++) {if(reData.complyBadge[i]) reData.complyBadge[i].finalize();}

		//reset XY:
		reData.xpos_cb = reData.xpos_cb_def;
		reData.ypos_cb = reData.ypos_cb_def;

		//reconstruct
		for(int j=0; j<cEvent.countRequirements(); j++)
		{
			reData.createBadge(j);
			reData.addSeparator(1);
		}
	}

	public void collectEvents()
	{
		switch(mode)
		{
			case(EventList.MODE_FREERIDE):
				events = GameLogic.freerideEvents;
				break;

			case(EventList.MODE_AMATEUR):
				events = GameLogic.careerEvents;
				break;
		}

		//debug stuff, list of all nodes inside stack
		/*for(int i=0; i<events.length; i++) System.trace("event stack node collected: " + Integer.getHex(events[i].id()));*/

		cEvent = events[currentEvent-1];
		if(cEvent) cEvent.init(); //only selected cEvent must init

		if(currentEvent < events.length) cEvent_clone = events[currentEvent]; //RAXAT: patch for last event
		else cEvent_clone = cEvent;
		if(cEvent_clone) cEvent_clone.init();
	}

	public void updateGoRace()
	{
		String joinFail = complyCheck();

		if(joinFail)
		{
			button_race.getLabel().changeText(joinFail);

			if(!goRaceStatus || goRaceStatus == 2)
			{
				button_race.disable();
				button_race.getLabel().setColor(0xFFFF3333);
				goRaceStatus = 1;
			}
		}
		else
		{
			String title = "GO RACE";
			if(mode == EventList.MODE_FREERIDE) title = "TAKE A RIDE";

			button_race.getLabel().changeText(title);

			if(!goRaceStatus || goRaceStatus == 1)
			{
				button_race.enable();
				goRaceStatus = 2;
			}
		}

	}

	public String complyCheck()
	{
		if(cEvent)
		{
			for(int i=0; i<cEvent.countRequirements(); i++)		{if(!cEvent.reqCheck(i)) 				return "You cannot participate in this event";		}
			if(cEvent.fee)						{if(!cEvent.checkMoney(cEvent.fee)) 			return "Not enough cash to pay entry fee";		}
			if(cEvent.carClass)					{if(cEvent.checkClass() != cEvent.carClass) 		return "You must pick a car of another class";		}
			if(cEvent.drivetype)					{if(!cEvent.checkDriveType(cEvent.drivetype)) 		return "You need a car with another drivetype";		}
			
			if(cEvent.raceTime > 0)
			{
				int interval = 8; //player can access time-limited event within this interval
				int rt = cEvent.raceTime;
				int hour = String.timeToString(GameLogic.getTime(), String.TCF_NOSECONDS|String.TCF_NOMINUTES).intValue();
				int min = hour-interval/2;	if(min<0) min += 24;
				int max = hour+interval/2;	if(min>23) max -= 24;

				if(rt < min && rt > max) return "This event is closed, come another time";
			}
		}

		return null;
	}

	//animations for moving left/right (universal method)
	public void animateRect(Rectangle target, int direction) //inversed direction for easier use
	{
		int steps = anim_rect_steps*(1/anim_rect_speed);
		float delta = anim_rect_delta/steps;
		direction *= (-1);

		target.restartAnimation("X");
		target.setupAnimation(delta, steps, direction, "X");
		target.a_speed = anim_rect_speed;
		target.runThread(); //this begins the animation
	}

	//'unified' method for rectangle animation inside EventList
	public void animateRect(int dir)
	{
		s_x = 1.925; //same as in addShields()

		//prepare rectangles for animation
		if(anim_rect_status == -1)
		{
			placeRect(trackPic, 0.0, dir);
			placeRect(raceTitle, 0.065, dir);
			placeRect(track_minimap, 1.5, dir);

			if(shields[0]) placeRect(shields[0], s_x, dir);
			for(int i=1; i<5; i++) if(shields[i]) placeRect(shields[i], addSeparator(), dir);
		}
		else
		{
			placeRect(trackPic_clone, 0.0, dir);
			placeRect(raceTitle_clone, 0.065, dir);
			placeRect(track_minimap_clone, 1.5, dir);

			if(shields_clone[0]) placeRect(shields_clone[0], s_x, dir);
			for(int i=1; i<5; i++) if(shields_clone[i]) placeRect(shields_clone[i], addSeparator(), dir);
		}

		//now move all rectangles
		animateRect(trackPic, dir);
		animateRect(trackPic_clone, dir);

		animateRect(raceTitle, dir);
		animateRect(raceTitle_clone, dir);

		animateRect(track_minimap, dir);
		animateRect(track_minimap_clone, dir);

		for(int i=0; i<5; i++)
		{
			if(shields[i]) animateRect(shields[i], dir);
			if(shields_clone[i]) animateRect(shields_clone[i], dir);
		}
	}

	//more compact method for preparing objects for animation
	public void placeRect(Rectangle target, float initX, int dir)
	{
		target.setPos(new Vector3( initX+(anim_rect_delta*dir), target.pos.y, target.pos.z ));
	}

	//fadeOut only! see method run() for text fadeIn
	public void animateText()
	{
		if(evgThread.methodStatus(0) == -1) evgThread.switchStatus(0); //reset THOR for text animations

		eventNumTxt.restartAnimation();
		eventNumTxt.fadeOut();

		trackNameTxt.restartAnimation();
		trackNameTxt.fadeOut();

		for(int i=0; i<reData.maxlines; i++)
		{
			reData.conditions[i].restartAnimation();
			reData.conditions[i].fadeOut();

			reData.requirements[i].restartAnimation();
			reData.requirements[i].fadeOut();
		}
	}

	public void freerideSetup()
	{
		if(!(myPrevState instanceof Garage))
		{
			GameLogic.autoSaveQuiet();
			GameLogic.loadDefaults();
			GameLogic.gameMode = GameLogic.GM_FREERIDE;
			GameLogic.setTime((Math.random()*14+4)*3600);
			
			if(!GameLogic.player.car)
			{
				VehicleDescriptor vd = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
				GameLogic.player.car = new Vehicle(GameLogic.player, vd.id, vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear);
			}
		}
	}

	public void osdCommand( int cmd )
	{
		if(!eventNumTxt.animationActive()) //kind of limiter, prevents visual bugs when attempting to interrupt interface animation
		{
			if( cmd == CMD_PREV )
			{
				if(events.length > 1)
				{
					prevEvent();
					updateGoRace();
				}
			}

			if( cmd == CMD_NEXT )
			{
				if(events.length > 1)
				{
					nextEvent();
					updateGoRace();
				}
			}
		}

		if( cmd == CMD_PRIZE )
		{
			cEvent.aquirePrize();
			new SfxRef(Frontend.SFX_NOTIF).play();
		}

		if( cmd == CMD_RACE )
		{
			int debugOption = 0;

			if(mode != EventList.MODE_FREERIDE) GameLogic.player.lastPlayedEvent = currentEvent; //we don't save last played event in freeride mode
			if(anim_rect_status == -1 || firstRun)
			{
				if(!debugOption)
				{
					if(mode == EventList.MODE_FREERIDE) freerideSetup();
					else cEvent.syncRaceTime(); //apply time in the race event to the global time
					GameLogic.changeActiveSection(new Track(cEvent.careerGM, cEvent, cEvent.track));
				}
				else
				{
					switch(debugOption)
					{
						case 1:
							System.print("EventList: CMD_RACE is called for cEvent", System.PF_CRITICAL);
							break;
					}
				}
			}
			else
			{
				if(!debugOption)
				{
					if(mode == EventList.MODE_FREERIDE) freerideSetup();
					else cEvent_clone.syncRaceTime(); //apply time in the race event to the global time
					GameLogic.changeActiveSection(new Track(cEvent_clone.careerGM, cEvent_clone, cEvent_clone.track));
				}
				else
				{
					switch(debugOption)
					{
						case 1:
							System.print("EventList: CMD_RACE is called for cEvent_clone", System.PF_CRITICAL);
							break;
					}
				}
			}
		}

		if( cmd == CMD_EXIT )
		{
			if(myPrevState instanceof MainMenu) GameLogic.changeActiveSection(myPrevState);
			else if(myPrevState instanceof Track && !GameLogic.carrerInProgress) GameLogic.changeActiveSection(new MainMenu());
			else GameLogic.changeActiveSection(GameLogic.garage);
		}
		
		if( cmd == CMD_INFO )
		{
			new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFORMATION", "You may need to build a car with specific engine power to match one of these classes and participate in racing events: (sorted by engine power) \n \n Class E: 0-160HP \n Class D: 160-300HP \n Class C: 300-500HP \n Class B: 500-700HP \n Class A: 700-1000HP \n Class S: Over 1000HP \n").display();
		}

		if( cmd == Input.AXIS_CANCEL ) osdCommand(CMD_EXIT);
	}
}

class Requirements
{
	Osd osd;

	//columns XY
	float ypos = 0.25;
	float xleft = -0.98;
	float xright = 0.0;

	//line separator XY
	float xleft_ls = -0.6745; //X pos for separators from left column
	float xright_ls = 0.4875; //X pos for separators from right column
	float ypos_ls = -0.3065;

	//comply badge XY
	float xpos_cb;
	float ypos_cb;
	float xpos_cb_def = 1.0075;
	float ypos_cb_def = -0.2775;

	float spacing = -0.2; //for text
	float spacing_ls = 0.12975; //for rectangles
	int   maxlines = 5;

	Text	c_label_txt, r_label_txt;
	Text[]  conditions, requirements;

	Rectangle[] req_badge, separator_a, separator_b, complyBadge;
	ResourceRef res_separator_a = new ResourceRef(frontend:0xC0A2r);
	ResourceRef res_separator_b = new ResourceRef(frontend:0xC0A1r);

	ResourceRef res_complyBadgeOK = new ResourceRef(frontend:0xD100r);
	ResourceRef res_complyBadgeDN = new ResourceRef(frontend:0xD101r);

	CareerEvent c_event;
	int anim_text_speed = 1; //EventList set this up itself
	int e_mode; //mode of EventList

	public Requirements( Osd o, CareerEvent ce, int mode )
	{
		osd = o;
		c_event = ce;
		e_mode = mode;
		xpos_cb = xpos_cb_def;
		ypos_cb = ypos_cb_def;
		for(int i=0; i<maxlines-2; i++) {createColumns(i);}
	}

	public void createColumns( int stage )
	{
		switch( stage )
		{
			case 0:
				conditions = new Text[maxlines];
				requirements = new Text[maxlines];
				req_badge = new Rectangle[maxlines];
				complyBadge = new Rectangle[maxlines];
				separator_a = new Rectangle[2]; //short
				separator_b = new Rectangle[maxlines*2]; //long
				break;

			case 1:
				String c_title = "CONDITIONS";
				String r_title = "REQUIREMENTS";

				if(e_mode == EventList.MODE_FREERIDE)
				{
					c_title = "GENERAL INFO";
					r_title = "DESCRIPTION";
				}

				c_label_txt = osd.createText( c_title, Frontend.largeFont, Text.ALIGN_LEFT, xleft, ypos );
				r_label_txt = osd.createText( r_title, Frontend.largeFont, Text.ALIGN_LEFT, xright, ypos );
				separator_a[0] = osd.createRectangle( -0.91, -0.237, 0.195, 0.008, 3, res_separator_a, 0 );
				separator_a[1] = osd.createRectangle( 0.25, -0.237, 0.195, 0.008, 3, res_separator_a, 0 );
				addSeparator(0);
				break;

			case 2:
				for(int s=0; s<maxlines; s++)
				{
					createLine_L(s);
					createLine_R(s);
					if(s < c_event.countRequirements()) createBadge(s);
					addSeparator(1);
				}
				break;
		}
	}

	public void createLine_L( int line )
	{
		conditions[line] = osd.createText( c_event.conditionText[line], Frontend.mediumFont, Text.ALIGN_LEFT, xleft, ypos );
		conditions[line].a_speed = anim_text_speed;
		separator_b[line] = osd.createRectangle( xleft_ls, ypos_ls, 0.376, 0.008, 3, res_separator_b, 0 );
	}

	public void createLine_R( int line )
	{
		requirements[line] = osd.createText( c_event.reqText[line], Frontend.mediumFont, Text.ALIGN_LEFT, xright, ypos );
		requirements[line].a_speed = anim_text_speed;
		separator_b[line+maxlines] = osd.createRectangle( xright_ls, ypos_ls, 0.376, 0.008, 3, res_separator_b, 0 );
	}

	public void createBadge( int line )
	{
		if(e_mode != EventList.MODE_FREERIDE)
		{
			if(c_event.reqText[line] != CareerEvent.noReqText)
			{
				if(c_event.reqCheck(line))
					complyBadge[line] = osd.createRectangle( xpos_cb, ypos_cb, 0.025, 0.0453, 3, res_complyBadgeOK, 0 );
				else	complyBadge[line] = osd.createRectangle( xpos_cb, ypos_cb, 0.025, 0.0453, 3, res_complyBadgeDN, 0 );
			}
		}
	}

	public void addSeparator( int lineSeparator )
	{
		ypos-=spacing/2/osd.vpHeight;

		if(lineSeparator)
		{
			ypos_ls-=spacing_ls/2/osd.vpHeight;
			ypos_cb-=spacing_ls/2/osd.vpHeight;
		}
	}
}