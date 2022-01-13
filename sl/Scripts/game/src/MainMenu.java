package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

//RAXAT: v2.3.1, total remake of main menu
public class MainMenu implements GameState
{
	Gates	window;

	public void enter( GameState prevState )
	{
		window = new Gates();
		window.myPrevState = prevState;
		window.show();
	}

	public void exit( GameState nextState )
	{
		window.hide();
		window = null;
	}
}

public class Gates extends Dialog
{
	final static int  CMD_LOAD_CAR			= 0xA001;
	final static int  CMD_SAVE_CAR			= 0xA002;
	final static int  CMD_LOAD_CAREER		= 0XA003;
	final static int  CMD_SAVE_CAREER		= 0XA004;
	final static int  CMD_DELETE_CAREER		= 0XA005;
	final static int  CMD_NEW_CAREER		= 0XA006;
	final static int  CMD_BACK_TO_GARAGE	= 0XA007;
	final static int  CMD_FREERIDE			= 0XA008;
	final static int  CMD_QUICKRACE			= 0XA009;
	final static int  CMD_MULTIPLAYER		= 0XB001;
	final static int  CMD_EXTRAS			= 0XB002;
	final static int  CMD_OPTIONS			= 0XB003;
	final static int  CMD_EXIT_GAME			= 0XB004;
	final static int  CMD_MENU_LEFT			= 0XB005;
	final static int  CMD_MENU_RIGHT		= 0XB006;
	final static int  CMD_MENU_SELECT		= 0XB007;
	
	final static int MULTIPLAYER_ENABLED	= false; //launcher should tell us that we're running the game in multiplayer mode

	Player	player;

	static	GameState myPrevState;
	ColorBank bank = new ColorBank();

	int	bckVideo;
	String	videoData;

	Thread	mmaThread;

	//red pulsing message text
	Text	message;
	int	m_idx;
	int	m_dir = 1;
	int	m_iterator = 3;

	int	tMethods = 7; //amount of THOR methods

	int	modeInit; //'modes' of main menu
	int	messageSeek = 0;
	int	showMenu = 0; //describes different states of displaying the menu elements

	//these listed only to be precached
	ResourceRef res_center_line = new ResourceRef(frontend:0xC00Fr);
	ResourceRef res_logo_streetlegal = new ResourceRef(frontend:0xC00Dr);
	ResourceRef res_logo_redline = new ResourceRef(frontend:0xC00Br);
	ResourceRef res_safeBck = new ResourceRef(frontend:0xC00Ar); //source: frontend\textures\interface\sections\main_menu\bck.png

	//rectangle animation stuff
	Rectangle safeBck, center_line;
	Rectangle logo_streetlegal, logo_redline, logo_streetlegal_resized, logo_redline_resized;

	Text	versionTxt;
	String tempData;

	SlidingMenu slider;

	//arrows menu for navigating with mouse
	int arrowGroup;
	Menu arrow_left, arrow_right, ghost_button;
	Style arrow_style, ghost_style;

	public Gates()
	{
		super(GameLogic.player.controller, DF_FULLSCREEN|DF_LOWPRI|DF_HIDEPOINTER, null, null);
		player=GameLogic.player;
		osd.globalHandler = this;
	}

	public void show()
	{
		GameLogic.careerComplete(); //RAXAT: v2.3.1, game now always check player's achievements

		if(!myPrevState) modeInit++; //include splashscreen
		else modeInit = 0; //or instantly show up sliding menu and resized logos

		//game will often check if career is completed, this makes appearance of main menu a little bit slower
		int careerMode = (GameLogic.gameMode == GameLogic.GM_CARREER && GameLogic.carrerInProgress);
		if(!modeInit && careerMode)
		{
			if(!Player.c_enabled)
			{
				int cc = GameLogic.careerComplete().f;
				if(cc) Player.c_enabled = 1;
			}
		}

		res_logo_streetlegal.cache();
		res_logo_redline.cache();
		res_safeBck.cache();

		Sound.changeMusicSet( Sound.MUSIC_SET_MENU );

		//---arrows begin
		Gadget g;
		int pri = 3;
		arrow_style  = new Style(0.11, 0.11, Frontend.mediumFont, Text.ALIGN_LEFT, null);
		ghost_style  = new Style(1.15, 0.21, Frontend.mediumFont, Text.ALIGN_CENTER, null);

		ResourceRef res_a_lf = new ResourceRef(frontend:0xD15Dr);
		res_a_lf.precache();

		arrow_left = osd.createMenu(arrow_style, 0.0, 0.0, 0, Osd.MD_HORIZONTAL);
		g = arrow_left.addItem(res_a_lf, CMD_MENU_LEFT, null, null, 0); //"<" button
		g.rect.setPos(new Vector3(-1.06, 0, pri)); //we manually set higher priority
		g.phy.setMatrix(new Vector3(-1.06, 0, pri), null); //same for hotspot

		ResourceRef res_a_rg = new ResourceRef(frontend:0xD15Er);
		res_a_rg.precache();

		arrow_right = osd.createMenu(arrow_style, 0.0, 0.0, 0, Osd.MD_HORIZONTAL);
		g = arrow_right.addItem(res_a_rg, CMD_MENU_RIGHT, null, null, 0); //">" button
		g.rect.setPos(new Vector3(1.06, 0, pri));
		g.phy.setMatrix(new Vector3(1.06, 0, pri), null);

		ghost_button = osd.createMenu(ghost_style, 0.0, 0.0, 0, Osd.MD_HORIZONTAL);
		g = ghost_button.addItem(Osd.RRT_GHOST, CMD_MENU_SELECT, null, null, 0); //hidden clickable area between nav buttons
		g.rect.setPos(new Vector3(0.0, 0, pri));
		g.phy.setMatrix(new Vector3(0.0, 0, pri), null);

		osd.hideGroup(arrowGroup = osd.endGroup());
		//---arrows end

		//begin fade BEFORE creating the video stream to avoid artefacts on the screen
		Frontend.loadingScreen.show(new ResourceRef(frontend:0x00A4r), 1, 0x00C, 1); //no-bck!

		if(Config.HD_quality()) videoData =  "data\\fmv\\background_motion_HD.avi";
			else 		videoData =  "data\\fmv\\background_motion.avi";

		if(bckVideo = (GfxEngine.openVideo(videoData, 1, 1)!=0)) safeBck = osd.createRectangle(0.0, -0.54, 2.20, 3.45, 0, res_safeBck);

		String ver = Config.version + " (build " + System.buildNumber() + ") ";
		if(!Steam.ready) ver += " [WARNING: FAILED TO INITIALIZE STEAM API]";
		
		versionTxt = osd.createText(ver, Frontend.smallFont_strong, Text.ALIGN_LEFT, -0.977, 0.925 );
		if(versionTxt) versionTxt.changeColor(0xFF000000);
		
		/*
		Vector items = Steam.getWorkshopItems();
		for(int i=0; i<items.size(); i++)
		{
			System.trace("item " + i + ", title: " + items.elementAt(i).title + ", description: " + items.elementAt(i).description + ", author: " + items.elementAt(i).author + ", publishedfileid: " + items.elementAt(i).publishedfileid + ", visibility: " + items.elementAt(i).visibility);
		}*/

		message=osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_CENTER, 0.0, 0.7 );
		message.setColor(0xFF000000);

		buildSlidingMenu();

		osd.createHotkey( Input.AXIS_SELECT, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_SELECT, this );
		super.show(); //this will activate and show osd

		if(modeInit) //including splashscreen stuff
		{
			logo_streetlegal = osd.createRectangle( 1.955, -0.10, 1.7, 0.29, 1, res_logo_streetlegal );
			logo_redline = osd.createRectangle( -2.54, 0.10, 0.5, 0.19, 1, res_logo_redline );
		}

		logo_streetlegal_resized = osd.createRectangle( 3.6, 0.87, 1.0, 0.18, 1, res_logo_streetlegal, 0 );
		logo_redline_resized = osd.createRectangle( 2.675, 0.69, 0.28, 0.11, 1, res_logo_redline, 0 );
		center_line = osd.createRectangle( -5.0, 0.0, 2.0, 0.65, 1, res_center_line, 0 );

		if(modeInit) Frontend.loadingScreen.userWait(2.5); //init: wait for fade-OUT

		//thread & THOR setup
		mmaThread = new Thread( this, "main menu animation thread", 1 ); //extended! thread-oriented methodology (THOR), no injections
		mmaThread.setPriority(Thread.MAX_PRIORITY);
		mmaThread.start();
		for(int i=0; i<(tMethods+1); i++) {mmaThread.addMethod(i);}

		if(modeInit) //begin animation for splash logos
		{
			logo_streetlegal.runAnimation(0.1, 50, -1, "X"); //direction: left
			logo_redline.runAnimation(0.15, 50, 1, "X"); //direction: right
			messageSeek = 1;
		}
		else //..or skip it (if main menu was launched at least once)
		{
			Frontend.loadingScreen.userWait( 2.0 ); //wait for fade-OUT
			showPrimaryVisuals();
		}

		//logo_redline_resized.watch(); //debug!
	}

	public void hide()
	{
		super.hide();

		mmaThread.stop();

		if(!bckVideo) GfxEngine.closeVideo();

		if(safeBck) safeBck.finalize();
	}

	//show center line, sliding menu, resized logos
	public void showPrimaryVisuals()
	{
		center_line.runAnimation(0.2, 25, 1, "X"); //direction: right
		slider.teleport(0.1,50,1,"X");

		//direction: left
		logo_streetlegal_resized.runAnimation(0.1, 30, -1, "X");
		logo_redline_resized.runAnimation(0.115, 15, -1, "X");

		messageSeek = 2;
		showMenu = 2;
	}

	//quickly hide main interface (slider, center line, logos, message)
	public void cleanup()
	{
		message.changeText("");
		osd.hideGroup(arrowGroup);
		slider.deactivate();
		slider.teleport(0.25,30,1,"X");
		center_line.runAnimation(0.2, 25, -1, "X"); //direction: left
		logo_streetlegal_resized.reverseAnimation("X");
		logo_redline_resized.reverseAnimation("X");

		showMenu = 3;
	}

	//reverse menu elements back from cleanup()
	public void reverse()
	{
		message.changeText(slider.getLabel());
		osd.showGroup(arrowGroup);
		slider.teleport();
		center_line.reverseAnimation("X");
		logo_streetlegal_resized.reverseAnimation("X");
		logo_redline_resized.reverseAnimation("X");
		slider.activate();
	}

	public void buildSlidingMenu()
	{
		slider = osd.createSlidingMenu(3.57, 0, 0.475, 1.0, 1, 1, SlidingMenu.STYLE_HORIZONTAL); //single sliding menu! + adjustable animation + styling, see Osd.class

		slider.addItem(0.2, 0.35, 2, frontend:0x0000C105r, frontend:0x0000C205r, CMD_LOAD_CAR, "LOAD CAR");

		if((GameLogic.gameMode == GameLogic.GM_CARREER || GameLogic.gameMode == GameLogic.GM_SINGLECAR) && player.car) slider.addItem(0.2, 0.35, 2, frontend:0x0000C109r, frontend:0x0000C209r, CMD_SAVE_CAR, "SAVE CAR");

		slider.addItem(0.2, 0.35, 2, frontend:0x0000C106r, frontend:0x0000C206r, CMD_LOAD_CAREER, "LOAD CAREER");

		if(GameLogic.gameMode == GameLogic.GM_CARREER) slider.addItem(0.2, 0.35, 2, frontend:0x0000C10Ar, frontend:0x0000C20Ar, CMD_SAVE_CAREER, "SAVE CAREER");

		slider.addItem(0.2, 0.35, 2, frontend:0x0000C101r, frontend:0x0000C201r, CMD_DELETE_CAREER, "DELETE CAREER");
		slider.addItem(0.2, 0.35, 2, frontend:0x0000C107r, frontend:0x0000C207r, CMD_NEW_CAREER, "NEW CAREER");

		if(GameLogic.played) slider.addItem(0.2, 0.35, 2, frontend:0x0000C100r, frontend:0x0000C200r, CMD_BACK_TO_GARAGE, "BACK TO GARAGE");

		slider.addItem(0.2, 0.35, 2, frontend:0x0000C104r, frontend:0x0000C204r, CMD_FREERIDE, "FREERIDE");
		slider.addItem(0.2, 0.35, 2, frontend:0x0000C10Br, frontend:0x0000C20Br, CMD_QUICKRACE, "QUICK RACE");
		slider.addItem(0.2, 0.35, 2, frontend:0x0000C10Cr, frontend:0x0000C20Cr, CMD_MULTIPLAYER, "MULTIPLAYER");
		slider.addItem(0.2, 0.35, 2, frontend:0x0000C103r, frontend:0x0000C203r, CMD_EXTRAS, "EXTRAS");
		slider.addItem(0.2, 0.35, 2, frontend:0x0000C108r, frontend:0x0000C208r, CMD_OPTIONS, "OPTIONS");
		slider.addItem(0.2, 0.35, 2, frontend:0x0000C102r, frontend:0x0000C202r, CMD_EXIT_GAME, "EXIT GAME");

		slider.activate(3);
		slider.setSpeed(0.5);
		slider.active = 0;
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(Input.AXIS_SELECT):
						if(message.text && message.text != "" && modeInit > 0)
						{
							logo_streetlegal.runAnimation(0.2, 25, -1, "X"); //direction: left
							logo_redline.runAnimation(0.2, 15, 1, "X"); //direction: right

							message.changeText("");
							showMenu = 1;
							modeInit = -1;
						}
						break;

			case(CMD_LOAD_CAR):
						VehicleFileReqDialog d = new VehicleFileReqDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "LOAD CAR", "LOAD", GameLogic.carSaveDir, "*" );
						if(d.display() == 0)
						{
							GameLogic.autoSave();

							cleanup();
							Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A, 1); //fade-IN (black, high-speed), no-bck
							Frontend.loadingScreen.userWait(1.5);

							String filename = GameLogic.carSaveDir + d.fileName;

							//debug quick paint save
							//Vehicle v = Vehicle.load(filename,null);
							//v.saveSkin(GameLogic.dbSkinDir + d.fileName);
							//System.print("PJ saved: " + filename);

							Integrator.flushPainterData(); //reset info for paintbooth
							Integrator.transitString = filename;
							GameLogic.changeActiveSection(new Transit(0x1001));
						}
						break;

			case(CMD_SAVE_CAR):
						VehicleFileReqDialog d = new VehicleFileReqDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.FRF_SAVE, "SAVE CAR", "SAVE", GameLogic.carSaveDir, "*" );
						if(d.display() == 0)
						{
							//fixed errors when attempting to overwrite existing files
							String filename = GameLogic.carSaveDir + d.fileName;
							if (File.exists(filename))
							{
								File.delete(filename);
								File.delete(GameLogic.carSaveDir, d.fileName+".*");
							}

							player.car.save(filename);
							new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "SAVE CAR", "Your current car has been saved." ).display();
						}
						break;

			case(CMD_LOAD_CAREER):
						CarrerFileReqDialog d = new CarrerFileReqDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "LOAD CAREER", "LOAD", GameLogic.carrerSaveDir, "*", FindFile.DIRS_ONLY);

						if( d.display() == 0 )
						{
							String filename = GameLogic.carrerSaveDir + d.fileName;

							int currentCarreer = 0;
							if(currentCarreer) new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "Load career", "This is your current career file, you cannot reload it." ).display();
							else
							{
								GameLogic.autoSave();
								int error = GameLogic.load( filename + "/" );
								if(!error)
								{
									cleanup();
									Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A, 1); //fade-IN (black, high-speed), no-bck
									Frontend.loadingScreen.userWait(1.5);
									Integrator.flushPainterData(); //reset info for paintbooth
									GameLogic.changeActiveSection(new Transit(0x0001));
								}
								else
								{
									new SfxRef( Frontend.SFX_WARNING ).play(); 
									new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", "Corrupt savegame file. \n Loading terminated." ).display();
								}
							}
						}
						break;

			case(CMD_SAVE_CAREER):
						GameLogic.autoSaveQuiet();
						new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "CAREER INFO", "Your current career progress was succesfully saved" ).display();
						break;


			case(CMD_DELETE_CAREER):
						CarrerFileReqDialog d = new CarrerFileReqDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "DELETE CAREER", "DELETE", GameLogic.carrerSaveDir, "*", FindFile.DIRS_ONLY);

						if(d.display() == 0)
						{
							String filename = GameLogic.carrerSaveDir + d.fileName;

							int currentCarreer = 0;
							int reallyDelete;

							//but we can do that in 2.2.1! a disadvantage for players, fix that!
							if(currentCarreer) new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", "You cannot delete your ongoing career.").display();
							else reallyDelete = (0 == new NoYesDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "Delete career", "Do you really want to continue?" ).display());

							if(reallyDelete)
							{
								//killing all folders first, otherwise game will throw IO error
								File.delete(filename + "/" + GameLogic.careerDataSaveSubDir, "*");
								File.delete(filename + "/" + GameLogic.eventDataSaveSubDir, "*");
								File.delete(filename + "/" + GameLogic.trackDataSaveSubDir, "*");

								File.delete(filename + "/", "*");
								File.delete(filename);
							}
						}
						break;


			case(CMD_NEW_CAREER):
						StringRequesterDialog d = new StringRequesterDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.SIF_NOEMPTY|Dialog.SIF_MINI|Dialog.SIF_FILES, "Enter player name", "" );
						if(d.display() == 0)
						{
							GameLogic.autoSave();

							cleanup();
							Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A, 1); //fade-IN (black, high-speed), no-bck
							Frontend.loadingScreen.userWait(1.5);
							Sound.changeMusicSet(Sound.MUSIC_SET_NONE);

							player.name = d.input; //name required only to define name of savegame
							player.character = new ResourceRef(Racer.RID_FEJ+(int)(Math.random()*(60-1))); //must be Vince Polansky pic!!
							GameLogic.loadDefaults();
							GameLogic.carrerInProgress = 1;
							GameLogic.updateCodeROC();  //generate link to the first prize vehicle
							GameLogic.autoSaveQuiet();
							Integrator.flushPainterData(); //reset info for paintbooth

							GameLogic.changeActiveSection(GameLogic.garage);
						}
						break;


			case(CMD_BACK_TO_GARAGE):
						cleanup();
						Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A, 1); //fade-IN (black, high-speed), no-bck
						Frontend.loadingScreen.userWait(1.5);
						Sound.changeMusicSet(Sound.MUSIC_SET_NONE);

						GameLogic.changeActiveSection (GameLogic.garage); //not a transit since the garage is already loaded
						break;

			case(CMD_FREERIDE):
						int reallyExit = 1;
						
						if(GameLogic.gameMode == GameLogic.GM_CARREER && GameLogic.carrerInProgress)
						{
							reallyExit = (0 == new NoYesDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "FREERIDE", "Are you sure to leave career mode? \n \n Don't forget to save your current progress.").display());
						}
						
						if(reallyExit)
						{
							GameLogic.autoSave();
							
							cleanup();
							Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A, 1); //fade-IN (black, high-speed), no-bck
							Frontend.loadingScreen.userWait(1.5);
							Sound.changeMusicSet(Sound.MUSIC_SET_NONE);

							GameLogic.changeActiveSection(new EventList(EventList.MODE_FREERIDE));
						}
						break;

			case(CMD_QUICKRACE):
						int reallyExit = 1;
						
						if(GameLogic.gameMode == GameLogic.GM_CARREER && GameLogic.carrerInProgress)
						{
							reallyExit = (0 == new NoYesDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "QUICKRACE", "Are you sure to leave career mode? \n \n Don't forget to save your current progress.").display());
						}
						
						if(reallyExit)
						{
							GameLogic.autoSave();

							cleanup();
							Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A, 1); //fade-IN (black, high-speed), no-bck
							Frontend.loadingScreen.userWait(1.5);
							Sound.changeMusicSet(Sound.MUSIC_SET_NONE);

							GameLogic.loadDefaults();
							GameLogic.gameMode = GameLogic.GM_QUICKRACE;
							GameLogic.setTime((21+Math.random()*8)*3600);
							if(!player.car)
							{
								VehicleDescriptor vd = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
								player.car = new Vehicle(player, vd.id, vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear);
							}

							GameLogic.changeActiveSection(new Valocity(null));
						}
						break;

			case(CMD_MULTIPLAYER):
						if(MULTIPLAYER_ENABLED)
						{
							GameLogic.autoSave();
							
							cleanup();
							Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A, 1); //fade-IN (black, high-speed), no-bck
							Frontend.loadingScreen.userWait(1.5);
							Sound.changeMusicSet(Sound.MUSIC_SET_NONE);
							
							GameLogic.loadDefaults();
							GameLogic.gameMode = GameLogic.GM_MULTIPLAYER;
							
							GameLogic.garage.init();
							GameLogic.changeActiveSection(GameLogic.garage);
						}
						else new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "MULTIPLAYER", GameLogic.serverMessage).display();
						break;

			case(CMD_EXTRAS):
						ExtrasDialog ed =  new ExtrasDialog();
						ed.display();

						int r = ed.result;

						if(r == ExtrasDialog.CMD_CREDITS)
						{
							cleanup();
							Frontend.loadingScreen.show(Osd.RRT_GHOST, 1);
							osd.darken(16,1);
							Frontend.loadingScreen.userWait(1.0);
							if(GfxEngine.isPlayingVideo()) GfxEngine.closeVideo();

							if(!System.nextGen())
							{
								Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
							}
							else
							{
								//build 900, direct music play, track#1: Ending Theme
								Sound.changeMusicSet(Sound.MUSIC_SET_MISC);
								Sound.playTrack(1);
							}

							CreditsDialog cd = new CreditsDialog();
							cd.display();

							if(!safeBck) GfxEngine.openVideo(videoData, 1, 1);
							Sound.changeMusicSet(Sound.MUSIC_SET_MENU);
							reverse();
							osd.darken(16, 1);
						}

						if(r == ExtrasDialog.CMD_HOF)
						{
							Input.cursor.enable(0);
							cleanup();
							Frontend.loadingScreen.show(Osd.RRT_GHOST, 1);
							osd.darken(16,1);
							Frontend.loadingScreen.userWait(1.0);
							if(GfxEngine.isPlayingVideo()) GfxEngine.closeVideo();

							osd.activeSlider = null;
							HallOfFame hof = new HallOfFame();
							hof.display();

							osd.activeSlider = slider;
							if(!safeBck) GfxEngine.openVideo(videoData, 1, 1);
							Sound.changeMusicSet(Sound.MUSIC_SET_MENU);
							reverse();
							osd.darken(16, 1);
							Input.cursor.enable(1);
						}

						break;

			case(CMD_OPTIONS):
						OptionsDialog od = new OptionsDialog(Dialog.DF_DARKEN|Dialog.DF_HIGHPRI);
						od.display();

						break;

			case(CMD_EXIT_GAME):
						int reallyExit;
						int careerMode = (GameLogic.gameMode == GameLogic.GM_CARREER && GameLogic.carrerInProgress);

						if(careerMode)
						{
							reallyExit = (0 == new NoYesDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "EXIT", "Do you really want to Exit? \n \n Don't forget to save your current career.").display());
						}
						else reallyExit = (0 == new NoYesDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "EXIT", "Do you really want to Exit?" ).display());

						if(reallyExit)
						{
							GameLogic.autoSave();
							message.changeText("");
							osd.hideGroup(arrowGroup);
							slider.deactivate();
							slider.teleport(0.2,40,1,"X");
							showMenu = 5; //initialize destruction process
						}
						break;

			case(CMD_MENU_LEFT):
						if(slider) slider.execute(Osd.CMD_MENU_LF);
						break;

			case(CMD_MENU_RIGHT):
						if(slider) slider.execute(Osd.CMD_MENU_RG);
						break;

			case(CMD_MENU_SELECT):
						if(slider) slider.execute(Osd.CMD_MENU_SEL);
						break;
		}
	}

	public void run()
	{
		for(;;)
		{
			//THOR methods
			if(mmaThread.methodStatus(0) == 1) //splash logo animation slow down 15X
			{
				Rectangle[] rect = new Rectangle[2];
				rect[0] = logo_streetlegal;
				rect[1] = logo_redline;

				for(int i=0; i<2; i++)
				{
					rect[i].a_steps_x += (rect[i].a_steps_x - rect[i].a_iterator_x)*15; //1st action - extend total amount of animation steps
					rect[i].a_steps_x -= 7; //2nd action - cut unnecessary animation steps
					rect[i].a_step_x = rect[i].a_step_x/15; //2nd action - shrink step size to the same value
				}

				mmaThread.controlMethod(0,-1);
			}

			if(mmaThread.methodStatus(1) == 1) //run animation for primary controls
			{
				showPrimaryVisuals();
				mmaThread.controlMethod(1,-1);
			}

			if(mmaThread.methodStatus(2) == 1) //fade-IN and exit game
			{
				Frontend.loadingScreen.show(new ResourceRef(frontend:0x000003EBr), 1, 0x10A, 1); //fade-IN (black, high-speed), no-bck
				Frontend.loadingScreen.userWait( 1.5 );
				System.exit();

			}

			if(message && message.text && message.text != "") //message animation stuff (native text animation is not used because of using special colors)
			{
				if(m_iterator == 3) //message animation is 3 times slower than thread
				{
					m_idx += m_dir;
					message.setColor(bank.getColor(m_idx));

					if(m_idx == 37 || m_idx == 0) {m_dir *= (-1);}
					m_iterator = 0;
				}
				else m_iterator++;
			}

			switch(messageSeek)
			{
				case 1:
					if(logo_streetlegal.a_iterator_x > logo_streetlegal.a_steps_x/1.2) mmaThread.execute(0); //run THOR

					if(logo_streetlegal.a_finished_x && logo_redline.a_finished_x)
					{
						message.changeText("PRESS ENTER");
						messageSeek = 0;
					}
					break;

				case 2: //showPrimaryVisuals() was called, waiting for finishing the animation of menu elements
					if(((modeInit && logo_streetlegal.a_finished_x && logo_redline.a_finished_x) || !modeInit) && slider.getItem().rect.a_finished_x) 
					{
						Input.cursor.enable(1);
						osd.showGroup(arrowGroup);
						slider.active = 1;
						message.changeText(slider.getLabel());
						messageSeek = 0;
					}
					break;
			}

			switch(showMenu)
			{
				case 1:
					if(logo_streetlegal.a_finished_x && logo_redline.a_finished_x)
					{
						message.changeText("");
						mmaThread.execute(1); //utilize THOR to show primary controls (slider, center line, etc.)
					}
					break;

				case 5: //destruction - stage 1
					if(slider.getItem().rect.a_finished_x)
					{
						center_line.runAnimation(0.2, 25, -1, "X"); //direction: left
						logo_streetlegal_resized.reverseAnimation("X");
						logo_redline_resized.reverseAnimation("X");

						showMenu = 6;
					}
					break;
				case 6: //destruction - stage 2
					if(logo_streetlegal_resized.a_finished_x && logo_redline_resized.a_finished_x) mmaThread.execute(2);
					break;
			}

			if(slider && slider.active && message.text != "" && showMenu > 0)
			{
				String label = slider.getLabel();
				if(message.text != label) message.changeText(label);
			}

			mmaThread.sleep(10);
		}
	}
}

class CarrerFileReqDialog extends FileRequesterDialog
{
	public CarrerFileReqDialog( Controller ctrl, int myflags, String mytitle, String OKButtonText, String path, String mask, int ffFlags )
	{
		super( ctrl, myflags, mytitle, OKButtonText, path, mask, ffFlags );
	}

	public int validator( String filename )
	{
		return GameLogic.fileCheck( filename );
	}
}

class VehicleFileReqDialog extends FileRequesterDialog
{
	public VehicleFileReqDialog( Controller ctrl, int myflags, String mytitle, String OKButtonText, String path, String mask )
	{
		super( ctrl, myflags, mytitle, OKButtonText, path, mask );
		if( myflags & FRF_SAVE )
			osd.defSelection = 5;
	}

	public int validator( String filename )
	{
		return Vehicle.fileCheck( filename );
	}
}

class ColorBank
{
	int[] color = new int[38];
	
	public int getColor(int id) {return color[id]; return 0;}
	public ColorBank()
	{
		color[0]  = 0x40000000;
		color[1]  = 0x50110000;
		color[2]  = 0x60220000;
		color[3]  = 0x70330000;
		color[4]  = 0x80440000;
		color[5]  = 0x90550000;
		color[6]  = 0xA0660000;
		color[7]  = 0xB0770000;
		color[8]  = 0xC0880000;
		color[9]  = 0xD0990000;
		color[10] = 0xE0AA0000;
		color[11] = 0xF0BB0000;
		color[12] = 0xF0CC0000;
		color[13] = 0xF0DD0000;
		color[14] = 0xF0EE0000;
		color[15] = 0xF0FF0000;
		color[16] = 0xF0FF0000;
		color[17] = 0xF0FF0000;
		color[18] = 0xF0FF0000;
		color[19] = 0xF0FF0000;
		color[20] = 0xF0FF0000;
		color[21] = 0xF0FF0000;
		color[22] = 0xF0FF0000;
		color[23] = 0xF0EE0000;
		color[24] = 0xF0DD0000;
		color[25] = 0xF0CC0000;
		color[26] = 0xF0BB0000;
		color[27] = 0xE0AA0000;
		color[28] = 0xD0990000;
		color[29] = 0xC0880000;
		color[30] = 0xB0770000;
		color[31] = 0xA0660000;
		color[32] = 0x90550000;
		color[33] = 0x80440000;
		color[34] = 0x70330000;
		color[35] = 0x60220000;
		color[36] = 0x50110000;
		color[37] = 0x40000000;
	}
}

class ExtrasDialog extends Dialog
{
	Menu	m;
	Style	menuStyle = new Style(0.45, 0.12, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
	Player	player;

	float	spacing;

	final static int	CMD_BACK	= 0;
	final static int	CMD_CREDITS	= 1;
	final static int	CMD_HOF		= 2;
	final static int	CMD_HIDDEN	= 3;

	public ExtrasDialog()
	{
		super(GameLogic.player.controller, Dialog.DF_HIGHPRI|Dialog.DF_TUNING|Dialog.DF_DARKEN|Dialog.DF_DEFAULTBG|DF_MODAL|DF_FREEZE, "EXTRAS", "BACK");
		player = GameLogic.player;
		osd.globalHandler = this;
		spacing = Text.getLineSpacing(menuStyle.charset, osd.vp);
	}

	public void show()
	{
		super.show();

		m = osd.createMenu(menuStyle, 0.0, spacing*(3-0.75)*(-1), 0);
		m.addItem("CREDITS", CMD_CREDITS);
		m.addItem("HALL OF FAME", CMD_HOF);

		Gadget g = m.addItem("HIDDEN OPTIONS", CMD_HIDDEN);
		if(!Player.c_enabled) g.disable();
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_BACK):
				super.osdCommand(0);
				break;

			case(CMD_CREDITS):
				result = cmd;
				notify();
				break;

			case(CMD_HOF):
				result = cmd;
				notify();
				break;

			case(CMD_HIDDEN):
				HiddenOptionsDialog hod = new HiddenOptionsDialog(Dialog.DF_DARKEN|Dialog.DF_TOPPRI); //warning! top priority!
				hod.display();
				break;
		}
	}
}

public class HiddenOptionsDialog extends Dialog
{
	Player	player;
	Menu	m;

	Style	sld_bh		= new Style(0.13, 0.0625, Frontend.mediumFont, Text.ALIGN_LEFT,   new ResourceRef(Osd.RID_SW_BACK));
	Style	sld_bk		= new Style(0.06, 0.07875,Frontend.mediumFont, Text.ALIGN_RIGHT,  new ResourceRef(Osd.RID_SW_KNOB));
	Style	menuStyle	= new Style(0.45, 0.12,   Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);

	String[] onOffText, disEnaText;
	int c_garage, c_track, c_eventlist, c_catalog, c_autosave, c_ghost;

	String	defTitle = "HIDDEN OPTIONS";

	final static int	CMD_ESC				= 700;
	final static int	CMD_SAVE			= 0;
	final static int	CMD_DISCARD			= 1;

	final static int	CMD_GARAGE			= 2;
	final static int	CMD_TRACK			= 3;
	final static int	CMD_EVENTLIST			= 4;
	final static int	CMD_CATALOG			= 5;
	final static int	CMD_AUTOSAVE			= 6;
	final static int	CMD_GHOST			= 7;

	public HiddenOptionsDialog(int additionalFlags)
	{
		super(GameLogic.player.controller, additionalFlags|DF_WIDE|Dialog.DF_DEFAULTBG|DF_MODAL, defTitle, "SAVE; DISCARD");

		escapeCmd = CMD_ESC;
		player = GameLogic.player;
		osd.globalHandler = this;
	}

	public void show()
	{
		onOffText = new String[2];
		onOffText[0] = "OFF";
		onOffText[1] = "ON";

		disEnaText = new String[2];
		disEnaText[0] = "DISABLED";
		disEnaText[1] = "ENABLED";

		super.show();
		
		int count = 6;
		m = osd.createMenu(menuStyle, 0.0, 0-((sld_bk.rt.height/2.25)*(count+1)), 0);
		m.setSliderStyle(sld_bh, sld_bk);
		
		c_garage = Player.c_garage;
		c_track = Player.c_track;
		c_eventlist = Player.c_eventlist;
		c_catalog = Player.c_catalog;
		c_autosave = Player.c_autosave;
		c_ghost = Player.c_ghost;

		m.addItem("GARAGE CODES", CMD_GARAGE, c_garage, 0, 1, 2, null).setValues(disEnaText);
		m.addItem("TRACK CHEATS", CMD_TRACK, c_track, 0, 1, 2, null).setValues(disEnaText);
		m.addItem("EVENT LIST CHEATS", CMD_EVENTLIST, c_eventlist, 0, 1, 2, null).setValues(disEnaText);
		m.addItem("KITS IN CATALOG", CMD_CATALOG, c_catalog, 0, 1, 2, null).setValues(disEnaText);
		m.addItem("AUTO SAVING", CMD_AUTOSAVE, c_autosave, 0, 1, 2, null).setValues(onOffText);
		m.addItem("GHOST DRIVER", CMD_GHOST, c_ghost, 0, 1, 2, null).setValues(onOffText);
	}

	public void hide()
	{
		//kill or restore driver
		if(Player.c_ghost) player.driverID = 0;
		else player.driverID = GameLogic.defDriver;

		super.hide();
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_GARAGE):
				c_garage = osd.sliderValue;
				break;

			case(CMD_TRACK):
				c_track = osd.sliderValue;
				break;

			case(CMD_EVENTLIST):
				c_eventlist = osd.sliderValue;
				break;

			case(CMD_CATALOG):
				c_catalog = osd.sliderValue;
				break;

			case(CMD_AUTOSAVE):
				c_autosave = osd.sliderValue;
				break;

			case(CMD_GHOST):
				c_ghost = osd.sliderValue;
				break;

			case(CMD_ESC): //cmd=escapeCmd
				osdCommand(CMD_DISCARD);
				break;

			case(CMD_SAVE): //cmd=0
				Player.c_garage		= c_garage;
				Player.c_track		= c_track;
				Player.c_eventlist	= c_eventlist;
				Player.c_catalog	= c_catalog;
				Player.c_autosave	= c_autosave;
				Player.c_ghost		= c_ghost;
				
				super.osdCommand(cmd);
				break;

			case(CMD_DISCARD): //cmd=1
				super.osdCommand(0);
				break;
		}
	}
}

class CreditsDialog extends Dialog
{
	Menu	m;
	Style	sty = new Style(0.45, 0.12, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
	Player	player;

	Thread	cdThread;
	Vector	texts;
	Text[]	lineTxt;

	float	spacing;
	float	move_speed = 0.005;
	Vector3 defPos = new Vector3(0.0, 1.0, 0.0);

	final static int CMD_BACK	= 0;

	public CreditsDialog()
	{
		super(GameLogic.player.controller, Dialog.DF_HIGHPRI|Dialog.DF_FULLSCREEN|DF_MODAL|DF_FREEZE, null, null);
		texts = new Vector();
		player = GameLogic.player;
		osd.globalHandler = this;
		osd.forceNoInitFocus = 1;
		spacing = Text.getLineSpacing(Frontend.largeFont, osd.vp);
	}

	public void show()
	{
		addLine(1, "STREET LEGAL RACING REDLINE CREDITS");
		addLine(1, "");
		addLine(1, "IMAGECODE LLC.");
		addLine(1, "");
		addLine(0, "Lead Programmer");
		addLine(1, "Yaroslav \"RAXAT\" Monakhov");
		addSeparator();
		addLine(0, "Game Code Programming");
		addLine(1, "Alexey \"Grim Maple\" Drozhzhin");
		addLine(1, "Andrey \"jhonyxakep\" Nedobylsky");
		addSeparator();
		addLine(0, "Graphic Design and Interface Programming");
		addLine(1, "Yaroslav \"RAXAT\" Monakhov");
		addSeparator();
		addLine(0, "Map and Vehicle Scripting");
		addLine(1, "Yaroslav \"RAXAT\" Monakhov");
		addLine(1, "Igor \"Green2989\" Mironenko");
		addSeparator();
		addLine(0, "Map Modelling");
		addLine(1, "smouk2");
		addLine(1, "GTR-X-ITE");
		addLine(1, "mihon");
		addSeparator();
		addLine(0, "AI Spline Design");
		addLine(1, "GTR-X-ITE");
		addSeparator();
		addLine(0, "Extra Vehicle Parts");
		addLine(1, "Draeghonov");
		addLine(1, "JAG");
		addSeparator();
		addLine(0, "Technical Consulting");
		addLine(1, "GTR-X-ITE");
		addLine(1, "miran");
		addLine(1, "mihon");
		addSeparator();
		addLine(0, "Vehicle Modelling");
		addLine(1, "Igor \"Green2989\" Mironenko");
		addLine(1, "123sw");
		addSeparator();
		addLine(0, "Additonal 3D Modelling");
		addLine(1, "Yaroslav \"RAXAT\" Monakhov");
		addLine(1, "mihon");
		addLine(1, "ramrom");
		addSeparator();
		addLine(0, "Texture Artwork");
		addLine(1, "Albert");
		addLine(1, "meskis");
		addLine(1, "VellonGamer");
		addLine(1, "Nikita Sirman");
		addSeparator();
		addLine(0, "Sound Effects");
		addLine(1, "Akvalang");
		addLine(1, "user12");
		addLine(1, "REDLINE_RACER");
		addLine(1, "mihon");
		addLine(1, "straight-north");
		addLine(1, "9K RPM and Brap Team");
		addSeparator();
		addLine(0, "Ingame Music");
		addLine(1, "Hotel Sinus");
		addLine(1, "JOZ");
		addLine(1, "karpej");
		addLine(1, "CJYoung");
		addLine(1, "Terroristik Beatz");
		addLine(1, "Solar Fields");
		addLine(1, "Yaroslav \"RAXAT\" Monakhov");
		addSeparator();
		addLine(1, "GREAT THANKS TO RASTA-BEAT");
		addLine(1, "for his unique and very important");
		addLine(1, "investigation into the project");
		addSeparator();
		addLine(1, "SPECIAL THANKS TO THE GOM-TEAM.COM COMMUNITY");
		addLine(1, "and David Singh for his help");
		addLine(1, "we would never finish this project without this guy!");
		addSeparator();
		addLine(1, "SPECIAL THANKS TO <<<ST@LK3R>>> A.K.A FRIENDKILLER, FORTUNATO");
		addLine(1, "RAZEIL, INVICTUS, MINTACID, JIM, SNAKER, GTR-X-ITE");
		addLine(1, "ILLKING, AKVALANG AND JIM");
		addLine(1, "for supporting development by donations");
		addSeparator();
		addLine(1, "TONS OF RESPECT TO MINTACID AND A-CROW");
		addLine(1, "for supporting the project from the day one");
		addSeparator();
		addLine(0, "List of Actors");
		for(int i=0; i<GameLogic.CLUBS*GameLogic.CLUBMEMBERS-1; i++)
		{
			Bot b = new Bot(i);
			addLine(1, b.profile.getFullName());
		}
		addSeparator();
		addSeparator();
		addLine(1, "ACTIVISION VALUE INC.");
		addSeparator();
		addLine(0, "General Manager");
		addLine(1, "Dave Oxford");
		addSeparator();
		addLine(0, "Senior Vice President");
		addLine(1, "Chad Koehler");
		addSeparator();
		addLine(0, "Vice President of Studios");
		addLine(1, "Patrick Kelly");
		addSeparator();
		addLine(0, "Vice President of Marketing and Creative Services");
		addLine(1, "Mark Meadows");
		addSeparator();
		addLine(0, "Vice President of Sales");
		addLine(1, "Tim Flaherty");
		addSeparator();
		addLine(0, "Senior Counsel");
		addLine(1, "Joe Hedges");
		addSeparator();
		addLine(0, "Producer");
		addLine(1, "Sean Dunnigan");
		addSeparator();
		addLine(0, "Assistant Producer");
		addLine(1, "Jason Lembcke");
		addSeparator();
		addLine(0, "Manager of Quality Assurance");
		addLine(1, "Chris Arends");
		addSeparator();
		addLine(0, "QA Lead");
		addLine(1, "Chad Schilling");
		addSeparator();
		addLine(0, "QA Team");
		addLine(1, "Bob Viau");
		addLine(1, "Isham Ashour");
		addLine(1, "Travis Clarke");
		addLine(1, "Bill Hart");
		addLine(1, "Donna Johnston");
		addLine(1, "Ryan Lee");
		addLine(1, "Kris Young");
		addSeparator();
		addLine(0, "Brand and Licencing Manager");
		addLine(1, "Andy Koehler");
		addSeparator();
		addLine(0, "Trade Marketing Manager");
		addLine(1, "Robbin Livernois");
		addSeparator();
		addLine(0, "Senior Graphic Artist");
		addLine(1, "Trevor Harveaux");
		addSeparator();
		addLine(0, "Graphic Artist");
		addLine(1, "Sean James");
		addSeparator();
		addLine(0, "Video Production Coordinator");
		addLine(1, "Skye Thomas");
		addSeparator();
		addLine(0, "Information Systems Administrator");
		addLine(1, "Josh Miedema");
		addSeparator();
		addLine(0, "Regional Sales Directors");
		addLine(1, "Dan Matschina");
		addLine(1, "Jim Holland");
		addLine(1, "Jennifer Mirabelli");
		addSeparator();
		addLine(0, "Director of OEM & Alternative Channels");
		addLine(1, "Brian Johnson");
		addSeparator();
		addLine(0, "Special Thanks");
		addLine(1, "Anne Beggs");
		addLine(1, "Don Borchers");
		addLine(1, "Mike Dalton");
		addLine(1, "Nicole Bement");
		addLine(1, "Brian Kirkvold");
		addLine(1, "Keri Gross");
		addLine(1, "Mike Groshens");
		addLine(1, "Kurt Niederloh");
		addLine(1, "Chris Owen");
		addLine(1, "Laura Saugen");
		addSeparator();
		addLine(0, "Additional Thanks");
		addLine(1, "Emily Bauman");
		addSeparator();
		addSeparator();
		addLine(1, "INVICTUS GAMES LTD.");
		addSeparator();
		addLine(0, "Producer");
		addLine(1, "Tamas Kozak");
		addSeparator();
		addLine(0, "Lead Programmer");
		addLine(1, "Denes Nagymathe");
		addSeparator();
		addLine(0, "Programming");
		addLine(1, "Akos Divianszky");
		addLine(1, "Laszlo Javorszky");
		addLine(1, "Zsolt Klampeczki");
		addLine(1, "Attila Kocsis");
		addLine(1, "Jozsef Punk");
		addSeparator();
		addLine(0, "Tool Programming");
		addLine(1, "Gabor Simon");
		addLine(1, "Jozsef Punk");
		addLine(1, "Zsolt Klampeczki");
		addSeparator();
		addLine(0, "Artwork");
		addLine(1, "Tibor Mester");
		addLine(1, "Csaba Csukas");
		addLine(1, "Viktor Sass");
		addLine(1, "Jozsef Bakos");
		addLine(1, "David Szabo");
		addLine(1, "Otto Feldmajer");
		addLine(1, "Janos Varadi");
		addLine(1, "Gabor Simon");
		addLine(1, "Laszlo Fesus");
		addLine(1, "Peter Gazso");
		addLine(1, "Tibor Valyi");
		addLine(1, "Zoltan Saghy");
		addLine(1, "Balazs Borsos");
		addSeparator();
		addLine(0, "Music by");
		addLine(1, "HOTEL SINUS www.hotelsinus.com");
		addLine(1, "Laszlo Zizics");
		addLine(1, "Gergo Ergelyi");
		addSeparator();
		addLine(0, "SFX");
		addLine(1, "Tibor Simon");
		addSeparator();
		addLine(0, "Additional Testing");
		addLine(1, "Gabor Szappanos");
		addLine(1, "Laszlo Zizics");
		addSeparator();
		addLine(0, "Special Thanks To");
		addLine(1, "Daniel 'Furball' Alabaster");
		addLine(1, "and the www.buildersedge.org community");
		addSeparator();
		addSeparator();
		addLine(0, "INVICTUS GAMES LTD. 2003");
		addLine(0, "RE-EDITION BY IMAGECODE LLC. 2016");
		addLine(0, "www.image-code.com");
		addLine(1, "All rights reserved");
		addSeparator();
		addSeparator();

		processLines();

		disableAnimateHook();

		cdThread = new Thread(this, "CreditsDialog watcher");
		cdThread.start();

		super.show();

		m = osd.createMenu(sty, -0.91, 0.85, 0, Osd.MD_HORIZONTAL);
		m.addItem("BACK", CMD_BACK);
	}

	public void hide()
	{
		if(cdThread)
		{
			cdThread.stop();
			cdThread = null;
		}

		super.hide();
	}

	public void addSeparator()
	{
		addLine(1, "");
		addLine(1, "");
	}

	public void addLine(int type, String str)
	{
		texts.addElement(new TextLine(type, str));
	}

	public void processLines()
	{
		lineTxt = new Text[texts.size()];

		for(int i=0; i<texts.size(); i++)
		{
			TextLine tl = texts.elementAt(i);

			if(!tl.type)
			{
				lineTxt[i] = osd.createText(tl.str, Frontend.smallFont, Text.ALIGN_CENTER, 0.0, 1.0);
				lineTxt[i].setColor(Palette.setAlpha(Palette.RGB_RED,0));
				tl.setText(lineTxt[i]);
			}
			else
			{
				lineTxt[i] = osd.createText(tl.str, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, 1.0);
				lineTxt[i].setColor(Palette.setAlpha(Palette.RGB_WHITE,0));
				tl.setText(lineTxt[i]);
			}
		}
	}

	public void run()
	{
		while(1)
		{
			int locked;

			for(int i=0; i<texts.size(); i++)
			{
				int move;
				TextLine txt = texts.elementAt(i);

				if(i==0) move++;
				else
				{
					TextLine txt0 = texts.elementAt(i-1);
					if(!txt.active)
					{
						if((txt.pos.y-txt0.pos.y)>=spacing) txt.activate();
					}

					if(txt.active>0) move++;
					if(txt.active<0) locked++;
				}

				if(move)
				{
					txt.setPos(txt.pos.x, txt.pos.y-move_speed);
					txt.update();
				}
			}

			if(locked==texts.size()-1)
			{
				for(int j=0; j<texts.size(); j++) texts.elementAt(j).reset();
			}

			Thread.sleep(30);
		}
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(0):
				super.osdCommand(0);
				break;
		}
	}
}

class TextLine
{
	int type, active, fade;
	String str;
	Text txt;
	Vector3 pos;

	final static float SCREEN_BOTTOM = 1.1;
	final static float SCREEN_TOP = -1.1;

	public TextLine() {}

	public TextLine(int t, String s)
	{
		type = t;
		str = s;
	}

	public void setText(Text t)
	{
		txt = t;
		txt.a_speed = 5; //slower speed for fade
		reset();
	}

	public void setPos(float x, float y)
	{
		pos.x = x; pos.y = y;
		txt.renderinst.setMatrix(pos, null);
	}

	public void update()
	{
		if(pos.y<=SCREEN_TOP*0.75)
		{
			if(fade==1)
			{
				txt.restartAnimation();
				txt.fadeOut(txt.getColor());
				fade++;
			}
		}

		if(pos.y<=SCREEN_TOP)
		{
			if(active > 0) active = -1;
		}

		if(pos.y<SCREEN_BOTTOM*0.75)
		{
			if(!fade)
			{
				txt.restartAnimation();
				txt.fadeIn(Palette.setAlpha(txt.getColor(), 255));
				fade++;
			}
		}
	}

	public void activate()
	{
		if(!active) active = 1;
	}

	public void reset()
	{
		fade = 0;
		active = 0;
		pos = new Vector3(0);
		setPos(0, SCREEN_BOTTOM);
	}
}