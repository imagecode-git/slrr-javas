package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.sound.*;

//RAXAT: v2.3.1, see changes below
public class Init extends GameType /*implements Runnable*/
{
	final static int STEAM_THREAD_CBK_LATENCY = 100;
	
	GameLogic gameLogic;
	Debug debug;
	/*
	Vector workshopItems;
	Thread steamThread;
	*/

	public Init(int obsolete_id)
	{
		Frontend.init();
		Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr),1);
		
		/*
		steamThread = new Thread(this, "Steam initialization thread");
		steamThread.start();
		Steam.init();
		wait();
		*/
		
		//RAXAT: cache all sounds BEFORE initializing the sound engine!
		new SfxRef(Frontend.SFX_MENU_MOVE).precache();
		new SfxRef(Frontend.SFX_MENU_SELECT).precache();
		new SfxRef(Frontend.SFX_QUESTION).precache();

		Sound.init();

		Input.initControllers(this, "save/controls/active_control_set");
		Input.enablePointingDevice();
		
		//RAXAT: if there are some settings that have been applied after restarting the game, they'll be copied to Config here
		if(Config.restart_apply)
		{
			Config.video_x = Config.restart_video_x;
			Config.video_y = Config.restart_video_y;
			Config.video_depth = Config.restart_video_depth;
			Config.video_windowed = Config.restart_video_windowed;

			Config.restart_apply = 0;
			Config options = new Config();
			options.saveConfig(Config.def_path);
		}

		debug = new Debug(Debug.DM_PRINTLOG); //for launching static debug methods
		gameLogic = new GameLogic(); //will load new MainMenu.class
		
		//RAXAT: auto-updater for saved cars
		/*
		FindFile ff = new FindFile();
		String[] list = ff.listFiles("save/cars/");
		
		for(int i=0; i<list.length; i++)
		{
			Vector vec = list[i].scanf(".");
			if(vec.size() == 1)
			{
				String fn = vec.elementAt(0);
				if(fn.length())
				{
					System.trace(fn); //lists vehicles found in trace.log
					//String path = GameLogic.carSaveDir+fn;
					String path = GameLogic.carSaveDir+"database/"+fn;
					
					Vehicle vhc = Vehicle.load(path, GameLogic.player);
					if(vhc) vhc.save(path);
					else System.trace("failed to load vehicle: " + fn);
				}
			}
		}*/
		
		//toDo: scan all cars and create list of VehicleDescriptor's, that will contain id's and names of vehicles for increasing game performance
		//also force updating this list at every game init
		
		//simple RPK generator. maybe we should add this as a standard utility?
		/*
		String[] names = new String[22];
		names[0] = "kit_Wolley_17_wheel";
		names[1] = "kit_Baiern_19_DTM_wheel";
		names[2] = "kit_Star_II_21_wheel";
		names[3] = "kit_Star_II_20_wheel";
		names[4] = "kit_Star_II_19_wheel";
		names[5] = "kit_Star_II_17_wheel";
		names[6] = "kit_Prime_DLH_17_wheel";
		names[7] = "kit_MT_Mescaline_15_wheel";
		names[8] = "kit_MT_Mescaline_15_drag_wheel";
		names[9] = "kit_Einvagen_GT_16_wheel";
		names[10] = "kit_DevilSport_5_21_wheel";
		names[11] = "kit_DevilSport_5_19_wheel";
		names[12] = "kit_DevilSport_5_17_wheel";
		names[13] = "kit_Blossom_19_wheel";
		names[14] = "kit_Blossom_17_wheel";
		names[15] = "kit_Dish_19_wheel";
		names[16] = "kit_Web_II_18_wheel";
		names[17] = "kit_Vortex_18_wheel";
		names[18] = "kit_Grinder_18_wheel";
		names[19] = "kit_Chariot_18_wheel";
		names[20] = "kit_Slider_17_wheel";
		names[21] = "kit_DS1_17_wheel";
		
		int hexlen = 8;
		int typeof = 8;
		int rid = 2185; //initial node number, 889 hex
		int superid = 62025; //f249 hex
		String path = "parts\\running_gear\\scripts\\";
		
		for(int i=0; i<names.length; i++)
		{
			System.trace("");
			String str = ""+Integer.getHex(rid);
			str = str.cut("0x");
			while(str.length()<hexlen) str = "0"+str; //zerofill
			
			System.trace("<FILE "+str+".res >");
			System.trace("typeof	"+typeof);
			System.trace("superid	"+Integer.getHex(superid));
			System.trace("typeid	"+Integer.getHex(rid));
			System.trace("alias	"+names[i]);
			System.trace("isparentcompatible	1.00");
			System.trace("</FILE>");
			System.trace("<FILE "+str+".rsd >");
			System.trace("script "+path+names[i]+".class");
			System.trace("</FILE>");
			
			rid++;
		}
		*/
	}

	/*
	public void run()
	{
		for(;;)
		{
			if(Steam.isReady())
			{
				workshopItems = new Vector();
				String[] param = new String[1];
				
				for(int i=0; i<Steam.getWorkshopNumSubscribedItems(); i++)
				{
					SteamWorkshopItem item = new SteamWorkshopItem(i);
					
					param[0] = i;
					item.title = runDLL(Steam.STEAM_MODULE, "GetWorkshopItemTitle", "string:int", param);
					item.description = runDLL(Steam.STEAM_MODULE, "GetWorkshopItemDescription", "string:int", param);
					item.author = runDLL(Steam.STEAM_MODULE, "GetWorkshopItemAuthor", "string:int", param);
					item.publishedfileid = runDLL(Steam.STEAM_MODULE, "GetWorkshopItemPublishedFileId", "string:int", param);
					item.visibility = runDLL(Steam.STEAM_MODULE, "GetWorkshopItemVisibility", "string:int", param);
					
					workshopItems.addElement(item);
				}
				Steam.setWorkshopItems(workshopItems);

				notify(); //after Steam is being initialized, the execution continues and this thread dies
				
				steamThread.stop();
				steamThread = null;
			}
			
			steamThread.sleep(STEAM_THREAD_CBK_LATENCY);
		}
	}
	*/
	
	public void finalize()
	{
		if(gameLogic) gameLogic.finalize(); //v2.3.1, a gamelogic check added, fixes errors after pressing Alt+F4

		Input.disablePointingDevice();
		Input.destroyControllers();
		Frontend.destroy();
		
		//Steam.finalize(); //RAXAT: careful, may crash!

		super.finalize();
	}
}