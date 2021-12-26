package java.game;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import parts:part.root.BodyPart.*;

public class Multiplayer extends Track
{
	final static String RELAY_ADRESS = "localhost";
	final static String MP_DIR = "mp/"; //for storing cars

	//static Vehicle[] Ghost = new Vehicle[12];
	Vector	Bots = new Vector();
	Vector	BotsMarkers = new Vector();
	
	Marker[]	Markers = new Marker[2001];
	RenderRef[]	Objects = new RenderRef[2001];
	Text[]	Texts = new Text[2001];
	Vehicle Ghosty;

	int enterMessage = 1;
	int frames = 0;
	int framesToGo = 1;
	float fps = 0;

	Text tempText, debugText;

	static int connected = false;
	static int KEYPRESS_OFFSET = 2000;
	static int FPS_TIMER = 2000;
	static float FPS_TICK = 500.0;
	
	public static int connect()
	{
	    if(connected) return true;
		if(NetworkEngine.httpRequest("POST", RELAY_ADRESS, "initConnection", "") != "OK") return false;

		connected = true;
		return true;
	}

	public static String RPC(String function, String[] params, int async)
	{
	    String endpoint = "RPC";
	    if(async) endpoint = "RPCasync";

		String callStack = "function="+function +"";
		if(params != null)
		{
			for(int i = 0; i < params.length; i++)
			{
				callStack = callStack + "&p[]="+params[i];
			}
		}
		
		return NetworkEngine.httpRequest("POST", RELAY_ADRESS, endpoint, callStack);
	}

	public static String RPC(String function, String[] params)
	{
		return RPC(function, params, false);
	}

	public void enter(GameState prev_state)
	{
		super.enter(prev_state);
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) init();
	}

	public void init()
	{
		//keyboard interception setup
        for(int i = 0; i<= 255; i++)
		{
			osd.createHotkey(i, Input.KEY|Osd.HK_STATIC, i + KEYPRESS_OFFSET, this);
        }
		
		if(Multiplayer.connect())
		{
    		//Multiplayer.setCity(this, player); //don't miss this line!
    	}
		
		//Bots.addElement(new Vehicle(map, cars.racers.duhen:0x00000006r, 1.0, 1.0, 1.0, 1.0, 1.0));
		//Bots.addElement(new Vehicle(map, cars.racers.duhen:0x00000006r, 1.0, 1.0, 1.0, 1.0, 1.0));
		//System.log(frontend:0x00000070r);
		//tempText = osd.createText("test", Frontend.smallFont, Text.ALIGN_CENTER, 0, 0);

        //System.log(debugText.renderinst.toString());
		//oppStatusTxt.changeText("follow the white rabbit");

		debugText = new Text(map,  Frontend.smallFont, "debug", 0, 0 ); //osd.createText( null, Frontend.smallFont, Text.ALIGN_LEFT, -0.98, -0.89 );
		addTimer(FPS_TICK/1000.0, FPS_TIMER);
	}

	public void animate()
	{
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) update();
		super.animate();
	}

	public void update()
	{
        frames++;
		//debugText.changeText(fps);
		//super.nightTime = true;

		if (player.car && player.car.chassis)
		{
			nav.updateNavigator(player.car);
			Multiplayer.setPos(player.car.getPos(), player.car.chassis.getOri(), player.car.getVel(), player.car.chassis.getAngvel(),(Input.getInput(Input.AXIS_TURN_LEFTRIGHT)/100.0), player.car.chassis.getInfo(GII_TYPE));
		}

		runRPCScript();
		nav.updateNavigator(player.car);

		//Vehicle current = Bots.elementAt(0);
		Vector3 carpos = player.car.getPos();
		carpos.x = carpos.x-5;
		/*
		if(carpos.distance(current.getPos()) > 0.5)
		{
			Ghosts[0].setMatrix(carpos, Ghosts[0].chassis.getOri());
			current.setMatrix(carpos, player.car.chassis.getOri());
		}

		if(player.car.chassis.getOri().diff(Ghost.chassis.getOri()) > 0.1) Ghosts[0].setMatrix(Ghosts[0].getPos(), player.car.chassis.getOri() );

		Ghosts[0].command("reset");
		current.command("setsteer "+(Input.getInput(Input.AXIS_TURN_LEFTRIGHT)/100.0));
		demoBot.car.command("reload");
		current.command("setvel "+player.car.getVel().toString());
		current.command("start");
		current.command("wakeup");
		*/
	}

	//execute RPC script from server
	public void runRPCScript()
	{
        int i = 0;
		
        String script = Multiplayer.RPC("getRpcScript", null);
        String method = "temp";
        String methodName = "";
		
        while(true)
		{
			method = script.token(i, "¤");
            if(method == null) break;
			i++;

            methodName = method.token(0, " ");
            if(methodName != "player" && methodName != "Error")
			{
				//System.log(methodName);
				runMethod(methodName, method);
			}
		}
	}

    public void handleEvent(GameRef obj_ref, int event, int param)
	{
		super.handleEvent(obj_ref, event, param);
        
		if(event == EVENT_TIME)
		{
			if(param == FPS_TIMER)
			{
				addTimer(FPS_TICK/1000.0, FPS_TIMER);
				fps = frames*(1000.0/FPS_TICK);
				frames = 0;
			}
		}
	}

	//wrapper methods for gameplay
	public static void setPos(Vector3 pos, Ypr ori, Vector3 vel, Vector3 angvel, float steer, int carType)
	{
		String[] params  = new String[6];
		params[0] = pos.toString();
		params[1] = ori.toString(); 
		params[2] = vel.toString();
		params[3] = steer;
		params[4] = carType;
		params[5] = angvel.toString();
		
		Multiplayer.RPC("setPos", params);
	}

	public void spendMoney(int money)
	{
        String[] params = new String[1];
        params[0] = money;
		
        RPC("spendMoney", params);
	}

	//RPC methods
	public String createBot(String method)
	{
	    int carType = method.token(2, " ").intValue();
	    Vehicle bot = new Vehicle(map, carType, 1.0, 1.0, 1.0, 1.0, 1.0);
	    BotsMarkers.addElement(nav.addMarker(Marker.RR_CAR3, bot));
        Bots.addElement(bot);
		
        return Bots.size();
	}

	//player has disconnected, removing him from the world
    public String deleteBot(String method)
	{
        int slot = method.token(1, " ").intValue();
        nav.remMarker(BotsMarkers.elementAt(slot));
        BotsMarkers.setElementAt(null, slot);
        Bots.setElementAt(null, slot);
		
        return "";
    }

	//updating vehicle positions of players
    public String botPos(String method)
	{
		int slot = method.token(1, " ").intValue();
		int ping = method.token(7, " ").intValue();
        
		framesToGo = ((fps/1000.0)*(float)ping); //how much frames we have until recieve new data packet
        if(framesToGo < 1) framesToGo = 1;

        Vehicle current = Bots.elementAt(slot);
        if(current == null || current.chassis == null) return "";

		String pos = method.token(2, " ");
		String ori = method.token(3, " ");
		Vector3 newPos = strV3(pos);
		float distance = Math.abs(newPos.distance(current.getPos()));

		Vector3 yprVector = new Vector3(strYpr(ori));
		Vector3 currentYprVector = new Vector3(current.chassis.getOri());
		float distance2 = (yprVector.distance(currentYprVector));

		Vector3 velocity = (strV3(method.token(4, " ")));
        //debugText.changeText(distance);

		float dist2ratio = 0.5;
		float distRatio = 0.5;
		
		//drop sync threshhold if speed is below 50kph
        if(current.getSpeedSquare() < (14*14)) dist2ratio = 0.05;

		//drop sync threshold if speed is below 20kph
        if(current.getSpeedSquare() < (5.5*5.5)) dist2ratio = 0.0;

		//if stuck, don't do anything
        if(current.getSpeedSquare() <= 0)
		{
			dist2ratio = 5;
			distRatio = 1;
        }

        if(distance > 10) current.setMatrix(newPos, strYpr(ori));
		else
		{
			//soft sync
            if(distance >= distRatio || distance2 > dist2ratio)
			{
				//if(Math.abs(velocity.x) >= 0.1)
				//{
					Vector3 currPos = current.getPos();
					newPos.x = ((newPos.x + currPos.x)/2);
					newPos.y = ((newPos.y + currPos.y)/2);
					newPos.z = ((newPos.z + currPos.z)/2);

					current.setMatrix(newPos, strYpr(ori));
				//}
            }
        }

		current.command("setsteer "+method.token(5, " "));
        //if(distance > 0.1)
		//{
			current.command("setvel "+velocity.toString()+" "+method.token(6, " "));
        //}
		current.command("start");
		current.command("wakeup");
		
		return "";
    }

	//loading vehicle of some player
	public String loadBot(String method)
	{
		int slot = method.token(1, " ").intValue();

		//delete old model from the world
		Vehicle current = Bots.elementAt(slot);
		if(current != null)
		{
			//debugText.changeText(current.getPos().distance(player.car.getPos()));
			if(current.getPos())
			{
				if(current.getPos().distance(player.car.getPos()) <= 10) return "";
				if(current.getPos().distance(player.car.getPos()) >= 40) return "";
                
				current.setPos(new Vector3(0,0,0));
            }
			
			nav.remMarker(BotsMarkers.elementAt(slot));
			BotsMarkers.setElementAt(null, slot);
			Bots.setElementAt(null,slot);
        }

		//load car by alias
        current = Vehicle.load(MP_DIR + "cars/car_"+method.token(2, " "), map);
        if(current != null) current.loadSkin(MP_DIR + "cars/skin_"+method.token(2, " "));
		else
		{
			System.log("Car "+method.token(2, " ")+" is null");
			File.delete(MP_DIR + "cars/car_"+method.token(2, " "));
			File.delete(MP_DIR + "cars/skin_"+method.token(2, " "));
			return "";
		}
		
		//current.wait();
        
		File.delete(MP_DIR + "cars/car_"+method.token(2, " "));
		File.delete(MP_DIR + "cars/skin_"+method.token(2, " "));

		Bots.setElementAt(current, slot);
		BotsMarkers.setElementAt(nav.addMarker(Marker.RR_CAR3, current), slot);

		return "";
	}

	//initializes loading vehicle to external target
	public String uploadCar(String method)
	{
		if(player.car && Integrator.isCity)
		{
			//System.log(method);
			File.delete(MP_DIR + "cars/car_"+method.token(1," "));
			File.delete(MP_DIR + "cars/skin_"+method.token(1," "));
			player.car.save(MP_DIR + "cars/car_"+method.token(1," "));
			player.car.saveSkin(MP_DIR + "cars/skin_"+method.token(1," "));
			RPC("updateCar", null);
		}
		
		return "";
	}

	//change player's pos
    public String playerPos(String method)
	{
		if(player.car != null)
		{
			String pos = method.token(1, " ");
			String ori = method.token(2, " ");
			player.car.setMatrix(strV3(pos), strYpr(ori));
			player.car.command("setsteer "+method.token(4, " "));
			player.car.command("setvel "+method.token(3, " "));
		}
		
		return "";
	}

	//change player's velocity
    public String playerVel(String method)
	{
		if(player.car != null)
		{
			player.car.command("setvel "+method.token(1, " "));
		}
		
		return "";
	}

	//send cmd to car
	public String carCommand(String method)
	{
		if(player.car != null) player.car.command(glueFromToken(1, method, " "));
		return "";
	}

	//repair car
    public String carRepair(String method)
	{
		if(player.car != null) player.car.repair();
		return "";
    }

	//set or modify player's minimap marker
    public String createMarker(String method)
	{
        if(nav == null) return "";

		int slot = method.token(1, " ").intValue();
		int markerType = method.token(2, " ").intValue();
		String pos = method.token(3, " ");
		
		if(Markers[slot]) nav.remMarker(Markers[slot]);
		Markers[slot] = nav.addMarker(new RenderRef(markerType), strV3(pos), frames );
		
		return "";
	}

	//set/change render ref on a map
    public String createObject(String method)
	{
        if(map == null) return "";

        int slot = method.token(1, " ").intValue();
        int objectType = method.token(2, " ").intValue();
        String pos = method.token(3, " ");
        String ori = method.token(4, " ");

		int color = method.token(5, " ").intValue();
		if(Objects[slot]) Objects[slot].destroy();
		Objects[slot] =  new RenderRef(map, objectType, method.token(1, " "));
		Objects[slot].setMatrix(strV3(pos), strYpr(ori));
		Objects[slot].setColor(color);

		return "";
	}

	//send cmd to the camera
    public String camCommand(String method)
	{
		if(cam == null) return "";
        cam.command(glueFromToken(1, method, " "));
        //cam.command("move "+map.id()+" "+glueFromToken(1, method, " "));
		
		return "";
	}

	//set/modify player's OSD text
    public String createText(String method)
	{
		int slot = method.token(1, " ").intValue();
		int font = method.token(2, " ").intValue();
		Vector3 pos = strV3(method.token(3, " "));
		int color = method.token(4, " ").intValue();
		String text = glueFromToken(5, method, " ");

		ResourceRef fontSize = Frontend.smallFont;
		if(font == 1) fontSize = Frontend.mediumFont;
		if(font == 2) fontSize = Frontend.largeFont;

        if(Texts[slot]) Texts[slot].finalize();
        if(text.length()>0)
		{
			Texts[slot] =  osd.createText( text, fontSize, Text.ALIGN_CENTER, pos.x, pos.y );
			Texts[slot].changeColor(color);
		}
		//Texts[slot].update();
        return "";
	}

	//JVM methodcall
    public String invoke(String method)
	{
		runMethod(method.token(1, " "));
		return "";
	}

	public String changeCamPoint(String method)
	{
		changeCamPoint(strV3(method.token(1, " ")));
		return "";
    }

	//set player's time
    public String setTime(String method)
	{
		float timet = method.token(1," ").floatValue();
		GameLogic.setTime(timet);
		super.refresh(timet);
		
		return "";
    }

	//will initialize uploading a car
    public String saveCar(String method)
	{
		System.log(method);
		if(player.car)
		{
			File.delete(MP_DIR + "saved/"+method.token(1," "));
			//File.delete(MP_DIR + "saved/skin_"+method.token(1," "));
			player.car.save(MP_DIR + "saved/"+method.token(1," "));
			player.car.saveSkin(MP_DIR + "saved/skin_"+method.token(1," "));
			RPC("saveCar", null);
		}
		
		return "";
	}

	//load player's car
    public String loadCar(String method)
	{
		System.log(method);
		String name = method.token(1, " ");

		player.car = Vehicle.load(MP_DIR + "saved/"+name, map);
		player.car.wait();
		player.car.loadSkin(MP_DIR + "saved/skin_"+name);
		File.delete(MP_DIR + "saved/"+name);
		File.delete(MP_DIR + "saved/skin_"+name);

		return "";
	}

	//will initialize uploading a car
    public String alert(String method)
	{
		String message = glueFromToken(1, method, " ");
		giveWarning(message.token(0,"~"), message.token(1,"~"));
		return "";
    }

	//set current session to garage
    public String gotoGarage(String method)
	{
        if(player.car) GameLogic.changeActiveSection(GameLogic.garage);
        return "";
    }

    public String gotoCity(String method)
	{
        if(player.car) GameLogic.changeActiveSection(new City());
        return "";
    }

	//utility
    public static Vector3 strV3(String str)
	{
		return new Vector3(str.token(0,",").floatValue(), str.token(1,",").floatValue(), str.token(2,",").floatValue());
    }

    public static Ypr strYpr(String str)
	{
		return  new Ypr(str.token(0,",").floatValue(), str.token(1,",").floatValue(), str.token(2,",").floatValue());
    }

    public static String glueFromToken(int from,String str, String token)
	{
        int i = from;
        String glued = "";
        String temp = "";
		
        while(true)
		{
            temp = str.token(i, token);
            if(temp == null) break;
            i++;
            glued += temp + token;
        }
		
        return glued;
    }

    public void handleMessage(Message m)
	{
		super.handleMessage(m);
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER && m.type == Message.MT_EVENT)
		{
			int	key = m.cmd;
			String[] params  = new String[1];
			params[0] = key - KEYPRESS_OFFSET;
			RPC("keyPress", params);
		}
    }

	public static float stabilize (float f)
	{
		if(f < 0.1) return 0;
		return f;
	}

	public static Vector3 v3Stabilize(Vector3 v)
	{
		v.x = stabilize(v.x);
		v.y = stabilize(v.y);
		v.z = stabilize(v.z);
		
		return v;
	}

	public void giveWarning(String title, String text)
	{
		new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, title, text ).display();
	}
}