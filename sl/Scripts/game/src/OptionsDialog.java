package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

//RAXAT: v2.3.1, dramatical changes for the options menu
public class OptionsDialog extends Dialog
{
	Player	player;
	int	actGroup, mainGroup, navGroup;

	Vector 	blocks;
	Config	options;

	float	spacing; //used to define when menu blocks should be divided into local groups

	//menu styles
	Style	menuStyle_1	= new Style(0.45, 0.12, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
	Style	menuStyle_2	= new Style(0.45, 0.12, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
	Style	navbtnStyle	= new Style(0.13, 0.13, Frontend.mediumFont, Text.ALIGN_RIGHT, null); //navigation buttons

	//slider styles
	Style	sld_h		= new Style(0.4,  0.03,   Frontend.mediumFont, Text.ALIGN_LEFT,  new ResourceRef(Osd.RID_SLD_BACK));
	Style	sld_k		= new Style(0.04, 0.0525, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB));
	Style	sld_bh		= new Style(0.13, 0.0625, Frontend.mediumFont, Text.ALIGN_LEFT,  new ResourceRef(Osd.RID_SW_BACK));
	Style	sld_bk		= new Style(0.06, 0.07875,Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SW_KNOB));
	Style	sld_lh		= new Style(0.55, 0.03,   Frontend.mediumFont, Text.ALIGN_LEFT,  new ResourceRef(Osd.RID_SLD_BACK)); //longer slider
	Style	sld_lbh		= new Style(0.26, 0.0625, Frontend.mediumFont, Text.ALIGN_LEFT,  new ResourceRef(Osd.RID_SWL_BACK)); //long switch

	String	defTitle = "OPTIONS";

	GameRef			activeCamera;

	MenuBlock		actBlock, prevBlock;

	int			getKey = -1;
	int			getName = -1;
	int			getKeyStat = 0;

	//video settings patch
	int			videoWindowed = -1;
	int			videoModeIndex = -1; //for slider only
	VideoMode	videoMode;
	Vector		videoModes_FS, videoModes_WND;

	ControlSet		controlSet;

	MultiChoice		soundHWMulti;

	int			textureDetail;
	int			shadowDetail;
	int			colorDepth;
	
	Text[]			keyText = new Text[NCONTROLS];

	Slider[]		powerSlider, dzSlider, ffbSlider, acSlider;
	Slider			vmSlider;

	String[]		detailTextLMH, detailTextOLMH, onoffText, depthText, videoModesFSText, videoModesWNDText, soundHWText, metricSystemText, gpsText;

	static int		cmd = 0;

	final static int	CMD_ESC				= 700;
	final static int	CMD_SAVE_ALL			= cmd;
	final static int	CMD_DISCARD			= ++cmd;

	final static int	CMD_VIDEO_OPTIONS		= ++cmd;
	final static int	CMD_SOUND_OPTIONS		= ++cmd;
	final static int	CMD_CONTROL_OPTIONS		= ++cmd;
	final static int	CMD_GAME_OPTIONS		= ++cmd;
	final static int	CMD_ADV_GAME_OPTIONS		= ++cmd;

	//video
	final static int	CMD_RESOLUTION			= ++cmd;
	final static int	CMD_DEPTH			= ++cmd;
	final static int	CMD_WINDOWED_MODE		= ++cmd;
	final static int	CMD_DYNAMIC_LIGHTS		= ++cmd;
	final static int	CMD_LIGHT_FLARES		= ++cmd;
	final static int	CMD_TEXTURE_DETAIL		= ++cmd;
	final static int	CMD_SHADOW_DETAIL		= ++cmd;
	final static int	CMD_VIEW_RANGE			= ++cmd;
	final static int	CMD_GAMMA			= ++cmd;
	final static int	CMD_OBJECT_DETAIL		= ++cmd;
	final static int	CMD_LOD_DETAIL			= ++cmd;
	final static int	CMD_PARTICLE			= ++cmd;

	//sound
	final static int	CMD_SOUND_HW			= ++cmd;
	final static int	CMD_EFFECTS_VOL			= ++cmd;
	final static int	CMD_MUSIC_VOL			= ++cmd;
	final static int	CMD_ENGINE_VOL			= ++cmd;

	//controls
	final static int	CMD_REDEFINE_CONTROLS		= ++cmd;
	final static int	CMD_REDEFINE_CONTROLS2		= ++cmd;
	final static int	CMD_REDEFINE_CONTROLS3		= ++cmd;
	final static int	CMD_AXISCHECK			= ++cmd;
	final static int	CMD_UNUSED			= ++cmd;
	final static int	CMD_RESET_CONTROLS		= ++cmd;
	final static int	CMD_LOAD_CONTROLS		= ++cmd;
	final static int	CMD_SAVE_CONTROLS		= ++cmd;
	final static int	CMD_MOUSE_SENS			= ++cmd;

	//game
	final static int	CMD_METRIC			= ++cmd;
	final static int	CMD_GPSMODE			= ++cmd;
	final static int	CMD_DEFORMATION			= ++cmd;
	final static int	CMD_STEERHELP			= ++cmd;
	final static int	CMD_ABS_SLIDER			= ++cmd;
	final static int	CMD_ASR_SLIDER			= ++cmd;
	final static int	CMD_ACH_RESET			= ++cmd;

	//game advanced
	final static int	CMD_HMF_1			= ++cmd;
	final static int	CMD_HMF_2			= ++cmd;
	final static int	CMD_HMF_3			= ++cmd;
	final static int	CMD_TRAFFICDENSITY		= ++cmd;
	final static int	CMD_PEDESTRIANDENSITY		= ++cmd;
	final static int	CMD_RACE_INTROS			= ++cmd;

	final static int	CMD_GETKEY			= 300;
	final static int	CMD_DEAD_ZONE			= 400;
	final static int	CMD_POWER			= 500;
	final static int	CMD_FFB				= 600;

	final static int	CMD_SCROLL_UP			= 201;
	final static int	CMD_SCROLL_DN			= 202;

	final static int	BLK_MAIN_PAGE			= 0;
	final static int	BLK_VIDEO_OPTIONS		= 1;
	final static int	BLK_SOUND_OPTIONS		= 2;
	final static int	BLK_CONTROL_OPTIONS		= 3;
	final static int	BLK_REDEFINE_CAR_CONTROLS	= 4;
	final static int	BLK_REDEFINE_GAME_CONTROLS	= 5;
	final static int	BLK_ADVANCED_CONTROLS		= 6;
	final static int	BLK_AXISCHECK			= 7;
	final static int	BLK_GAME_OPTIONS		= 8;
	final static int	BLK_ADV_GAME_OPTIONS		= 9;

	final static int	NCONTROLS			= ControlSet.NCONTROLS;

	public OptionsDialog(int additionalFlags)
	{
		super(GameLogic.player.controller, additionalFlags|DF_WIDE|Dialog.DF_DEFAULTBG|DF_MODAL|DF_LOWPRI, defTitle, "SAVE; DISCARD");

		escapeCmd = CMD_ESC;
		player = GameLogic.player;
		osd.globalHandler = this;
		spacing = Text.getLineSpacing(Frontend.mediumFont, osd.vp);
		actGroup = -1;

		controlSet = new ControlSet();
		controlSet.load(GameLogic.activeControlFile);
	}

	public void show()
	{
		//backing up previous config, more safe than generating new one
		String backup = Config.bkp_path;
		if(File.exists(backup)) File.delete(backup);
		
		FindFile ff = new FindFile();
		String name = ff.first(Config.def_path, FindFile.FILES_ONLY);
		if(name) File.copy(Config.def_path, Config.bkp_path); //this configuration will be loaded after pressing 'discard'
		ff.close();

		videoWindowed = Config.video_windowed;
		colorDepth = Config.video_depth;

		//searching and collecting fullscreen resolutions; only valid screen XY are supported for fullscreen mode! otherwise game will crash at initialization		
		videoModes_FS = new Vector();
		for(int i=0; i<GfxEngine.numDisplayModes(); i++)
		{
			String vmt = GfxEngine.displayModeName(i);

			float w = vmt.token(0).intValue();
			float h = vmt.token(1).intValue();
			float d = vmt.token(2).intValue();

			int wide;
			float aspect = w/h;
			if(aspect>1.7 && aspect<1.8) wide=1;

			if(d != 16) videoModes_FS.addElement(new VideoMode(w, h, colorDepth, 0)); //patch
		}

		//now collecting screen resolutions for windowed mode, we can use any XY we want		
		videoModes_WND = new Vector();
		videoModes_WND.addElement(new VideoMode(1366, 768,  colorDepth, 1));
		videoModes_WND.addElement(new VideoMode(1440, 810,  colorDepth, 1));
		videoModes_WND.addElement(new VideoMode(1536, 864,  colorDepth, 1));
		videoModes_WND.addElement(new VideoMode(1600, 900,  colorDepth, 1));
		videoModes_WND.addElement(new VideoMode(1920, 1080, colorDepth, 1));

		videoModeIndex = getVideoMode(GfxEngine.displayModeName(GfxEngine.currDisplayMode()));

		detailTextLMH = new String[3];
		detailTextLMH[0] = "LOW";
		detailTextLMH[1] = "MEDIUM";
		detailTextLMH[2] = "HIGH";

		detailTextOLMH = new String[4];
		detailTextOLMH[0] = "OFF";
		detailTextOLMH[1] = "LOW";
		detailTextOLMH[2] = "MEDIUM";
		detailTextOLMH[3] = "HIGH";

		onoffText = new String[2];
		onoffText[0] = "OFF";
		onoffText[1] = "ON";

		depthText = new String[2];
		depthText[0] = "16";
		depthText[1] = "32";

		videoModesFSText = new String[videoModes_FS.size()];
		for(int i=0; i<videoModesFSText.length; i++)
		{
			VideoMode vm = videoModes_FS.elementAt(i);
			videoModesFSText[i] = (int)vm.width + "x" + (int)vm.height;
		}

		videoModesWNDText = new String[videoModes_WND.size()];
		for(int i=0; i<videoModesWNDText.length; i++)
		{
			VideoMode vm = videoModes_WND.elementAt(i);
			videoModesWNDText[i] = (int)vm.width + "x" + (int)vm.height;
		}

		soundHWText = new String[4];
		soundHWText[0] = "SOFTWARE";
		soundHWText[1] = "HARDWARE (2D)";
		soundHWText[2] = "HARDWARE (FULL 3D)";
		soundHWText[3] = "AUTO";

		metricSystemText = new String[2];
		metricSystemText[0] = "MILES";
		metricSystemText[1] = "KILOMETERS";

		gpsText = new String[2];
		gpsText[0] = "MAP RELATIVE";
		gpsText[1] = "CAR RELATIVE";

		powerSlider	= new Slider[4];
		dzSlider	= new Slider[4];
		ffbSlider	= new Slider[2];
		acSlider	= new Slider[4];

		if(Config.texture_size <= Config.texture_size_high) textureDetail = Config.HIGH;
		else
		if(Config.texture_size <= Config.texture_size_mid)  textureDetail = Config.MID;
		else textureDetail = Config.LOW;

		if(Config.shadow_detail >= Config.shadow_detail_off) shadowDetail = Config.OFF;
		else
		if(Config.shadow_detail >= Config.shadow_detail_low) shadowDetail = Config.LOW;
		else
		if(Config.shadow_detail >= Config.shadow_detail_mid) shadowDetail = Config.MID;
		else shadowDetail = Config.HIGH;

		super.show();

		actGroup = mainGroup = osd.endGroup(); //dialog itself, never be hidden

		Menu	m, m2;
		Style	sty;

		m = osd.createMenu(navbtnStyle, 0.825, -0.325, 0, Osd.MD_VERTICAL);
		m.addItem(new ResourceRef(Osd.RID_ARROWUP), 201, null, null, 1 );
		m.addSeparator();
		m.addSeparator();
		m.addSeparator();
		m.addSeparator();
		m.addSeparator();
		m.addSeparator();
		m.addSeparator();
		m.addItem(new ResourceRef(Osd.RID_ARROWDN), 202, null, null, 1);

		navGroup = osd.endGroup();

		blocks = new Vector();

		addBlock("OPTIONS");
		sty = menuStyle_1;
		m = osd.createMenu(sty, 0.0, calcY(sty, 5), 0);
		m.addItem("VIDEO OPTIONS", CMD_VIDEO_OPTIONS);
		m.addItem("SOUND OPTIONS", CMD_SOUND_OPTIONS);
		m.addItem("CONTROL OPTIONS", CMD_CONTROL_OPTIONS);
		m.addItem("GAME OPTIONS", CMD_GAME_OPTIONS);
		m.addItem("ADVANCED GAME OPTIONS", CMD_ADV_GAME_OPTIONS);
		endBlock();

		addBlock("VIDEO OPTIONS");

		sty = menuStyle_2;
		m = osd.createMenu(sty, 0.0, calcY(sty, 7), 0);
		m.setSliderStyle(sld_lbh, sld_bk);

		String[] str;
		if(!videoWindowed) str = videoModesFSText;
		else str=videoModesWNDText;

		vmSlider = m.addItem("SCREEN RESOLUTION", CMD_RESOLUTION, videoModeIndex, 0, str.length-1, str.length, null); //switch, not a typical slider
		vmSlider.setValues(str);
		
		m.setSliderStyle(sld_bh, sld_bk);
		m.addItem("COLOR DEPTH", CMD_DEPTH, (colorDepth/16)-1, 0, 1, 2, null).setValues(depthText); //switch, patch: colorDepth/16
		m.addItem("WINDOWED MODE", CMD_WINDOWED_MODE, Config.video_windowed, 0, 1, 2, null).setValues(onoffText); //switch
		m.addItem("DYNAMIC LIGHTS", CMD_DYNAMIC_LIGHTS, Config.headlight_rays, 0, 1, 2, null).setValues(onoffText); //switch
		m.addItem("LIGHT FLARES", CMD_LIGHT_FLARES, Config.flares, 0, 1, 2, null).setValues(onoffText); //switch
		m.addItem("TEXTURE DETAIL", CMD_TEXTURE_DETAIL, textureDetail, detailTextLMH, null);
		m.addItem("SHADOW DETAIL", CMD_SHADOW_DETAIL, shadowDetail+1, detailTextOLMH, null);
		addGroup(blocks.lastElement());

		m = osd.createMenu(sty, 0.0, calcY(sty, 5), 0);
		m.setSliderStyle(sld_h, sld_k);
		m.addItem("VIEW RANGE", CMD_VIEW_RANGE, Config.camera_ext_viewrange, 50.0, 1000.0, 0, null).printValue("%.2fm");
		m.addItem("GAMMA", CMD_GAMMA, Config.video_gamma, 0.5, 2.0, 0, null).printValue("%.2f");
		m.addItem("OBJECT DETAIL", CMD_OBJECT_DETAIL, Config.object_detail, 0.0747, 0.010, 0, null);
		m.addItem("LOD DETAIL", CMD_LOD_DETAIL, Config.object_detail_amp, 28.0, 5.5, 0, null);
		m.addItem("PARTICLE DENSITY", CMD_PARTICLE, Config.particle_density, 0.0, 1.5, 5, null);
		endBlock();

		addBlock("SOUND OPTIONS");
		sty = menuStyle_2;
		m = osd.createMenu(sty, 0.0, calcY(sty, 4), 0);
		m.setSliderStyle(sld_lh, sld_k);
		soundHWMulti = m.addItem("MIXING", CMD_SOUND_HW, 0, soundHWText, null);
		m.addItem("EFFECTS VOLUME", CMD_EFFECTS_VOL, Sound.getVolume(Sound.CHANNEL_EFFECTS), null);
		m.addItem("MUSIC VOLUME", CMD_MUSIC_VOL, Sound.getVolume(Sound.CHANNEL_MUSIC), null);
		m.addItem("ENGINE VOLUME", CMD_ENGINE_VOL, Sound.getVolume(Sound.CHANNEL_ENGINE), null);
		endBlock();

		addBlock("CONTROL OPTIONS");
		sty = menuStyle_1;
		m = osd.createMenu(sty, 0.0, calcY(sty, 5), 0);
		m.setSliderStyle(sld_lh, sld_k);
		m.addItem("REDEFINE CAR CONTROLS", CMD_REDEFINE_CONTROLS);
		m.addItem("REDEFINE GAME CONTROLS", CMD_REDEFINE_CONTROLS2);
		m.addItem("ADVANCED SETTINGS", CMD_REDEFINE_CONTROLS3);
		m.addItem("CHECK AXES", CMD_AXISCHECK);
		m.addItem("RESET TO DEFAULTS", CMD_RESET_CONTROLS);
		addGroup(blocks.lastElement());

		sty = menuStyle_1;
		m = osd.createMenu(sty, 0.0, calcY(sty, 2), 0);
		m.addItem("LOAD CONTROLS", CMD_LOAD_CONTROLS);
		m.addItem("SAVE CONTROLS", CMD_SAVE_CONTROLS);
		endBlock();

		addBlock("REDEFINE CAR CONTROLS");
		float lx = -0.8; //left column
		float rx = 0.0; //right column
		float y = calcY(sty, 6);
		float ydef = y;

		sty = menuStyle_2;
		float spc;

		String[] names = new String[10];
		names[0] = "ACCELERATE";
		names[1] = "BRAKE";
		names[2] = "TURN LEFT";
		names[3] = "TURN RIGHT";
		names[4] = "HAND BRAKE";
		names[5] = "SHIFT UP";
		names[6] = "SHIFT DOWN";
		names[7] = "CLUTCH";
		names[8] = "ENGAGE NOS";
		names[9] = "HONK HORN";

		Menu fm = osd.createMenu(sty, 0, 0, 0); //fake menu just to calculate line spacing
		spc = fm.spacing*(2-0.7); //0.7 - def vpHeight
		y+=spc;
		m  = osd.createMenu(sty, lx, y, 0);
		m2 = osd.createMenu(sty, rx, y, 0);

		osd.createText("PRIMARY CONTROLS",	sty.charset, Text.ALIGN_LEFT, lx+0.035, ydef);
		osd.createText("SECONDARY CONTROLS",	sty.charset, Text.ALIGN_LEFT, rx+0.035, ydef);

		int max;
		for(int i=0; i<names.length; i++)
		{
			if(names[i].length()>max) max = names[i].length(); //this is needed to calculate optimal xpos to place keystroke text, we adjust it using the longest key name
		}

		float dx = (max*spc)/3.5;
		for(int i=0; i<names.length/2; i++)
		{
			m.addItem(names[i],  CMD_GETKEY+i);
			m2.addItem(names[i], CMD_GETKEY+(names.length+(names.length/2)+i));

			keyText[i]   = osd.createText("?", sty.charset, Text.ALIGN_LEFT, lx+dx, y);
			keyText[names.length+(names.length/2)+i] = osd.createText("?", sty.charset, Text.ALIGN_LEFT, rx+dx, y);
			y+=spc;
		}

		addGroup(blocks.lastElement());

		y=ydef;
		y += spc;
		m  = osd.createMenu(sty, lx, y, 0);
		m2 = osd.createMenu(sty, rx, y, 0);

		osd.createText("PRIMARY CONTROLS",	sty.charset, Text.ALIGN_LEFT, lx+0.035, ydef);
		osd.createText("SECONDARY CONTROLS",	sty.charset, Text.ALIGN_LEFT, rx+0.035, ydef);

		for(int i=names.length/2; i<names.length; i++)
		{
			m.addItem(names[i],  CMD_GETKEY+i);
			m2.addItem(names[i], CMD_GETKEY+(names.length+(names.length/2)+i));

			keyText[i]   = osd.createText("?", sty.charset, Text.ALIGN_LEFT, lx+dx, y);
			keyText[names.length+(names.length/2)+i] = osd.createText("?", sty.charset, Text.ALIGN_LEFT, rx+dx, y);
			y+=spc;
		}

		endBlock();

		addBlock("REDEFINE GAME CONTROLS");
		fm = osd.createMenu(sty, 0, 0, 0); //we use same fake menu again
		spc = fm.spacing*(2-0.7); //0.7 - def vpHeight

		names = new String[5];
		names[0] = "ACCESS MAIN MENU";
		names[1] = "NEXT MUSIC TRACK";
		names[2] = "PREVIOUS MUSIC TRACK";
		names[3] = "VOLUME UP";
		names[4] = "VOLUME DOWN";

		lx=-0.5;
		sty = menuStyle_2;
		y=calcY(sty, 6);
		m = osd.createMenu(sty, lx, y, 0);

		for(int i=0; i<names.length; i++)
		{
			if(names[i].length()>max) max = names[i].length();
		}

		dx = (max*spc)/4.0;
		for(int i=0; i<names.length; i++)
		{
			m.addItem(names[i],  CMD_GETKEY+10+i);

			keyText[10+i] = osd.createText("?", sty.charset, Text.ALIGN_LEFT, lx+dx, y);
			y+=spc;
		}
		
		//cruise control special key
		int keyID = 65;
		m.addItem("CRUISE CONTROL",  CMD_GETKEY+keyID);
		keyText[keyID] = osd.createText("?", sty.charset, Text.ALIGN_LEFT, lx+dx, y);

		endBlock();

		addBlock("ADVANCED CONTROL SETTINGS");
		sty = menuStyle_1;
		m = osd.createMenu(sty, 0.0, calcY(sty, 4), 0);
		m.setSliderStyle(sld_lh, sld_k);

		names = new String[4];
		names[0] = "ACCELATE GAMMA";
		names[1] = "BRAKE GAMMA";
		names[2] = "STEERING GAMMA";
		names[3] = "CLUTCH GAMMA";

		for(int i=0; i<names.length; i++) powerSlider[i] = m.addItem(names[i], CMD_POWER+i, 0.0, null);
		m.addItem("MOUSE SENSITIVITY", CMD_MOUSE_SENS, Config.mouseSensitivity, null);

		addGroup(blocks.lastElement());

		sty = menuStyle_2;
		m = osd.createMenu(sty, 0.0, calcY(sty, 6), 0);
		m.setSliderStyle(sld_lh, sld_k);

		names = new String[4];
		names[0] = "ACCELERATE DEAD ZONE";
		names[1] = "BRAKE DEAD ZONE";
		names[2] = "STEERING DEAD ZONE";
		names[3] = "CLUTCH DEAD ZONE";

		for(int i=0; i<names.length; i++) dzSlider[i] = m.addItem(names[i], CMD_DEAD_ZONE+i, 0.0, null);

		ffbSlider[0] = m.addItem("REAL FFB STRENGTH", CMD_FFB+0, Config.FFB_strength, 0.0f, 1.0f, 0, null);
		ffbSlider[1] = m.addItem("EMULATED FFB STRENGTH", CMD_FFB+1, Config.FFB_strength_emulated, 0.0f, 0.1f, 0, null);

		endBlock();

		addBlock("CHECK AXES");
		sty = menuStyle_2;
		m = osd.createMenu(sty, 0.0, calcY(sty, 4), 0);
		m.setSliderStyle(sld_lh, sld_k);

		names = new String[4];
		names[0] = "THROTTLE";
		names[1] = "BRAKE";
		names[2] = "STEER";
		names[3] = "CLUTCH";

		for(int i=0; i<names.length; i++)
		{
			acSlider[i] = m.addItem(names[i], CMD_UNUSED, 0.0, 0.0, 1.0, 0, null);
			acSlider[i].nofocus = 1;
		}

		endBlock();

		addBlock("GAME OPTIONS");
		sty = menuStyle_2;
		m = osd.createMenu(sty, 0.0, calcY(sty, 7), 0);
		m.setSliderStyle(sld_lh, sld_k);
		m.addItem("METRIC SYSTEM", CMD_METRIC, Config.metricSystem, metricSystemText, null);
		m.addItem("GPS MODE", CMD_GPSMODE, Config.gpsMode, gpsText, null);
		//m.addSeparator();
		m.addItem("DEFORMATION LEVEL", CMD_DEFORMATION, Config.deformation, 0.5, 1.5, 0, null);
		m.addItem("STEERING HELP", CMD_STEERHELP, Config.player_steeringhelp, null);
		m.addItem("ABS", CMD_ABS_SLIDER, Config.player_abs, null);
		m.addItem("ASR", CMD_ASR_SLIDER, Config.player_asr, null);

		sty = menuStyle_1;
		m = osd.createMenu(sty, 0.0, calcY(sty, -6), 0);
		m.addItem("RESET ACHIEVEMENTS", CMD_ACH_RESET);
		endBlock();

		addBlock("ADVANCED GAME OPTIONS");
		sty = menuStyle_2;
		m = osd.createMenu(sty, 0.0, calcY(sty, 6), 0);
		m.setSliderStyle(sld_lh, sld_k);
		m.addItem("DRIVER MOVEMENT/STEER", CMD_HMF_1, Config.head_move_steer, 0.0, 2.0, 0, "How the steering wheel's position affects the driver's movement");
		m.addItem("DRIVER MOVEMENT/VELOCITY", CMD_HMF_2, Config.head_move_vel, 0.0, 2.0, 0, "How vehicle velocity affects the driver's movement");
		m.addItem("DRIVER MOVEMENT/ACCELERATION", CMD_HMF_3, Config.head_move_acc, 0.0, 2.0, 0, "How vehicle acceleration affects the driver's movement");
		m.addItem("TRAFFIC DENSITY", CMD_TRAFFICDENSITY, Config.trafficDensity, 0.0, 1.5, 0, null);
		m.addItem("PEDESTRIAN DENSITY", CMD_PEDESTRIANDENSITY, Config.pedestrianDensity, 0.0, 1.5, 0, null);
		m.addSeparator();
		
		m.setSliderStyle(sld_bh, sld_bk);
		m.addItem("RACE INTROS", CMD_RACE_INTROS, Config.gm_cam_anim, 0, 1, 2, null).setValues(onoffText); //switch
		endBlock();

		showBlock(BLK_MAIN_PAGE);
		//osd.showGroup(blocks.lastElement().getGroup(1)); //debug! should not be used in the way like this

		enableAnimateHook();
	}

	public void hide()
	{
		disableAnimateHook();
		super.hide();
	}

	public void addGroup(MenuBlock blk)
	{
		int groupID = osd.endGroup();
		blk.push(groupID);
		osd.hideGroup(groupID);
	}

	public void addBlock(String title)
	{
		blocks.addElement(new MenuBlock(blocks.size(), title));
	}

	public void endBlock()
	{
		int groupID = osd.endGroup();
		blocks.lastElement().push(groupID);
		osd.hideGroup(groupID);
	}

	public void showBlock(int blockID)
	{
		if(actBlock)
		{
			if(blockID != BLK_CONTROL_OPTIONS) prevBlock = actBlock;
			else prevBlock = blocks.firstElement();

			hideBlock(actBlock.id);
			actBlock.actGroup=0;
		}

		actBlock = blocks.elementAt(blockID);
		actGroup = actBlock.getFirstGroup();
		osd.showGroup(actGroup);

		if(actBlock.title) setTitle(actBlock.title);
		else setTitle(defTitle);

		if(actBlock.isScrollable()) osd.showGroup(navGroup);
		else osd.hideGroup(navGroup);

		if(actBlock.id > BLK_MAIN_PAGE && actBlock.id <= BLK_ADV_GAME_OPTIONS) setButtonLabel(0, "BACK");
		else setButtonLabel(0, "SAVE");
	}

	public void hideBlock(int blockID)
	{
		osd.hideGroup(blocks.elementAt(blockID).getGroup(blocks.elementAt(blockID).actGroup));
		actGroup = mainGroup;
		setTitle(defTitle);
	}

	public void nextGroup()
	{
		if(actBlock.nextGroup())
		{
			osd.hideGroup(actGroup);
			actGroup = actBlock.getCurrentGroup();
			osd.showGroup(actGroup);
		}
	}

	public void prevGroup()
	{
		if(actBlock.prevGroup())
		{
			osd.hideGroup(actGroup);
			actGroup = actBlock.getCurrentGroup();
			osd.showGroup(actGroup);
		}
	}

	public float calcY(Style sty, int count)
	{
		float sp = sld_k.rt.height;
		if(sty == menuStyle_2) count -= 1;
		return 0-(sp*count);

		return 0;
	}
	
	//returns vector index
	public int getVideoMode(String str)
	{
		Vector vec = videoModes_FS;
		if(videoWindowed) vec = videoModes_WND;

		//attempting to detect current video mode
		int w = str.token(0).intValue();
		int h = str.token(1).intValue();
		
		//comparing current settings with native video modes
		for(int i=0; i<vec.size(); i++)
		{
			VideoMode vm = vec.elementAt(i);
			if(w==vm.width && h==vm.height)
			{
				videoMode = vm;
				return i;
			}
		}
		
		//defaults:
		int defautIndex = 0;
		videoMode = vec.elementAt(defautIndex);
		videoMode.depth = colorDepth;
		
		return defautIndex;
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_SCROLL_UP):
				prevGroup();
				break;

			case(CMD_SCROLL_DN):
				nextGroup();
				break;

			case(CMD_VIDEO_OPTIONS):
				showBlock(BLK_VIDEO_OPTIONS);
				break;

			case(CMD_SOUND_OPTIONS):
				int soundHW = Config.Sound_Mix_HW;

				if(soundHW == 2) soundHW++;
				if(Config.Sound_Mix_HW == 1 && Config.Sound_3D_HW == 1) soundHW++;

				soundHWMulti.setValue(soundHW);
				showBlock(BLK_SOUND_OPTIONS);

				break;

			case(CMD_CONTROL_OPTIONS):
				showBlock(BLK_CONTROL_OPTIONS);
				break;

			case(CMD_GAME_OPTIONS):
				showBlock(BLK_GAME_OPTIONS);
				break;

			case(CMD_ADV_GAME_OPTIONS):
				showBlock(BLK_ADV_GAME_OPTIONS);
				break;

			case(CMD_RESOLUTION):
				Vector vec = videoModes_FS;
				if(videoWindowed) vec = videoModes_WND;
				
				videoModeIndex = osd.sliderValue;
				videoMode = vec.elementAt(videoModeIndex);
				videoMode.depth = colorDepth;
				break;

			case(CMD_DEPTH):
				colorDepth = (osd.sliderValue+1)*16;
				videoMode.depth = colorDepth;
				break;

			case(CMD_WINDOWED_MODE):
				videoWindowed = osd.sliderValue;
				videoMode.windowed = videoWindowed;
				videoMode.depth = colorDepth;
				videoModeIndex = getVideoMode(videoMode.width + " " + videoMode.height + " " + videoMode.depth + " " + videoMode.windowed);
				
				String[] str;
				if(videoWindowed <= 0) str=videoModesFSText;
				else str=videoModesWNDText;

				//update screen resolution switch and fill it up with new values
				vmSlider.setRange(0, str.length-1);
				vmSlider.setTicks(str.length);
				vmSlider.setValue(videoModeIndex);
				vmSlider.setValues(str);
				break;

			case(CMD_DYNAMIC_LIGHTS):
				Config.headlight_rays = osd.sliderValue;
				refreshCameras();
				break;

			case(CMD_LIGHT_FLARES):
				Config.flares = osd.sliderValue;
				refreshCameras();
				break;

			case(CMD_TEXTURE_DETAIL):
				textureDetail = osd.multiValue;
				break;

			case(CMD_SHADOW_DETAIL):
				shadowDetail = osd.multiValue-1;
				break;

			case(CMD_VIEW_RANGE):
				Config.camera_ext_viewrange = osd.sliderValue;
				Config.camera_int_viewrange = osd.sliderValue;
				refreshCameras();
				break;

			case(CMD_GAMMA):
				Config.video_gamma = osd.sliderValue;
				refreshCameras();
				break;

			case(CMD_OBJECT_DETAIL):
				Config.object_detail = osd.sliderValue;
				refreshCameras();
				break;

			case(CMD_LOD_DETAIL):
				Config.object_detail_amp = osd.sliderValue;
				refreshCameras();
				break;

			case(CMD_PARTICLE):
				Config.particle_density = osd.sliderValue;
				refreshCameras();
				break;

			case(CMD_SOUND_HW):
				switch(osd.multiValue)
				{
					case 0:
						Config.Sound_Mix_HW = 0;
						Config.Sound_3D_HW = 0;
						break;
					case 1:
						Config.Sound_Mix_HW = 1;
						Config.Sound_3D_HW = 0;
						break;
					case 2:
						Config.Sound_Mix_HW = 1;
						Config.Sound_3D_HW = 1;
						break;
					case 3:
						Config.Sound_Mix_HW = 2;
						Config.Sound_3D_HW = 2;
						break;
				}
				//TODO: (Invictus) tell the sound engine to apply the changes
				break;

			case(CMD_EFFECTS_VOL):
				Sound.setVolume(Sound.CHANNEL_EFFECTS, osd.sliderValue);
				break;

			case(CMD_MUSIC_VOL):
				Sound.setVolume(Sound.CHANNEL_MUSIC, osd.sliderValue);
				break;

			case(CMD_ENGINE_VOL):
				Sound.setVolume(Sound.CHANNEL_ENGINE, osd.sliderValue);
				break;

			case(CMD_RESET_CONTROLS):
				Dialog d = new YesNoDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_TOPPRI, "CONFIRM RESET", "You are going to reset current control set, are you sure to do this?");
				if(!d.display())
				{
					controlSet = new ControlSet();
					controlSet.save(GameLogic.activeControlFile);
					player.controller.setcontrol(controlSet);
				}

				break;

			case(CMD_LOAD_CONTROLS):
				ControlsFileReqDialog d = new ControlsFileReqDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_TOPPRI, "LOAD CONTROL DEFINITIONS", "LOAD", GameLogic.controlSaveDir, "*");

				if(d.display() == 0)
				{
					String filename = GameLogic.controlSaveDir + d.fileName;

					controlSet = new ControlSet();
					controlSet.load(filename);
					controlSet.save(GameLogic.activeControlFile);
					player.controller.setcontrol(controlSet);
				}
				break;

			case(CMD_SAVE_CONTROLS):
				ControlsFileReqDialog d = new ControlsFileReqDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.FRF_SAVE|Dialog.DF_TOPPRI, "SAVE CONTROL DEFINITIONS", "SAVE", GameLogic.controlSaveDir, "*");

				if(d.display() == 0)
				{
					String filename = GameLogic.controlSaveDir + d.fileName;

					if(File.exists(GameLogic.activeControlFile)) File.copy(GameLogic.activeControlFile, filename);
					else
					{
						controlSet = new ControlSet();
						controlSet.save(filename);
					}
				}
				break;

			case(CMD_FFB):
				Config.FFB_strength = osd.sliderValue;
				System.getConfigOptions();
				break;

			case(CMD_FFB+1):
				Config.FFB_strength_emulated = osd.sliderValue;
				System.getConfigOptions();
				break;

			case(CMD_MOUSE_SENS):
				Config.mouseSensitivity = osd.sliderValue;

				if(Config.mouseSensitivity < 0.1) Config.mouseSensitivity = 0.1; //special request: reset mouse and set sensitivity
				Input.getAxis(1, -1-(Config.mouseSensitivity*100.0f)); //send command to cursor
				Input.cursor.config();

				break;

			case(CMD_METRIC):
				Config.metricSystem = osd.multiValue;
				if(Config.metricSystem == 0) System.setMeasure(1600);
				else System.setMeasure(1000);
				break;

			case(CMD_GPSMODE):
				Config.gpsMode = osd.multiValue;
				break;

			case(CMD_DEFORMATION):
				Config.deformation = osd.sliderValue;
				Config.internal_damage = osd.sliderValue/2.0;
				Config.player_damage_multiplier = osd.sliderValue/2.0;
				break;

			case(CMD_STEERHELP):
				Config.player_steeringhelp = osd.sliderValue;
				break;

			case(CMD_ABS_SLIDER):
				Config.player_abs = osd.sliderValue;
				break;

			case(CMD_ASR_SLIDER):
				Config.player_asr = osd.sliderValue;
				break;
				
			case(CMD_ACH_RESET):
				Dialog d = new YesNoDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_TOPPRI, "CONFIRM RESET", "You are going to reset your Steam achievements for this game, are you sure to do this?");
				if(!d.display())
				{
					GameLogic.player.resetSteamAchievements();
					new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_TOPPRI, "WARNING", "Your Steam achievements for Street Legal Racing: Redline have been reset.").display();
				}
				break;

			case(CMD_HMF_1):
				Config.head_move_steer = osd.sliderValue;
				break;

			case(CMD_HMF_2):
				Config.head_move_vel = osd.sliderValue;
				break;

			case(CMD_HMF_3):
				Config.head_move_acc = osd.sliderValue;
				break;

			case(CMD_TRAFFICDENSITY):
				Config.trafficDensity = osd.sliderValue;
				break;

			case(CMD_PEDESTRIANDENSITY):
				Config.pedestrianDensity = osd.sliderValue;
				break;
				
			case(CMD_RACE_INTROS):
				Config.gm_cam_anim = osd.sliderValue;
				break;

			case(CMD_ESC): //cmd=escapeCmd
				if(actBlock.id == BLK_MAIN_PAGE) osdCommand(CMD_DISCARD);
				else osdCommand(CMD_SAVE_ALL);
				break;

			case(CMD_SAVE_ALL): //cmd=0
				if(actBlock.id == BLK_MAIN_PAGE)
				{
					switch(textureDetail)
					{
						case(Config.HIGH):
							Config.texture_size = Config.texture_size_high;
							Config.shadow_size = Config.shadow_size_high;
							break;

						case(Config.MID):
							Config.texture_size = Config.texture_size_mid;
							Config.shadow_size = Config.shadow_size_mid;
							break;

						case(Config.LOW):
							Config.texture_size = Config.texture_size_low;
							Config.shadow_size = Config.shadow_size_low;
							break;
					}

					switch(shadowDetail)
					{
						case(Config.HIGH):
							Config.shadows = Config.shadows_high;
							Config.shadow_detail = Config.shadow_detail_high;
							break;

						case(Config.MID):
							Config.shadows = Config.shadows_mid;
							Config.shadow_detail = Config.shadow_detail_mid;
							break;

						case(Config.LOW):
							Config.shadows = Config.shadows_low;
							Config.shadow_detail = Config.shadow_detail_low;
							break;

						case(Config.OFF):
							Config.shadows = Config.shadows_off;
							Config.shadow_detail = Config.shadow_detail_off;
							Config.shadow_size = Config.shadow_size_off;
							break;
					}
					
					if(Config.video_x			!= videoMode.width
					|| Config.video_y			!= videoMode.height
					|| Config.video_depth		!= colorDepth
					|| Config.video_windowed	!= videoWindowed)
					{
						Config.restart_video_x = videoMode.width;
						Config.restart_video_y = videoMode.height;
						Config.restart_video_depth = colorDepth;
						Config.restart_video_windowed = videoWindowed;
					
						//player will see this message only once
						if(!Config.restart_apply) new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_TOPPRI, "WARNING", "Some changes will be applied only after restarting the game.").display();
						Config.restart_apply = 1;
					}

					if(controlSet) controlSet.save(GameLogic.activeControlFile);

					//killing old save file and writing new one
					options = new Config(); //empty init to prevent scrambling of system options
					String def = Config.def_path;
					if(File.exists(def)) File.delete(def);
					
					options.saveConfig(def);
					System.getConfigOptions();
					super.osdCommand(cmd);
				}
				else
				{
					if(prevBlock)
					{
						hideBlock(actBlock.id);
						showBlock(prevBlock.id);
					}
				}

				break;

			case(CMD_DISCARD): //cmd=1
				options = new Config();
				options.loadConfig(Config.def_path); //this configuration will be loaded after pressing 'discard'

				System.getConfigOptions();
				refreshCameras();
				
				super.osdCommand(0);
				break;
		}

		if(cmd >= CMD_GETKEY && cmd < CMD_GETKEY + NCONTROLS)
		{
			if(getKey == -1)
			{
				getKey = cmd - CMD_GETKEY;
				keyText[getKey].changeText("_");
				getKeyStat = 0;
				Input.getAxis(1, -1 - (Config.mouseSensitivity * 100.0f)); //will reset mouse and set sensitivity
			}
		}

		if(cmd==CMD_REDEFINE_CONTROLS || cmd==CMD_REDEFINE_CONTROLS2 || cmd==CMD_REDEFINE_CONTROLS3 || cmd==CMD_AXISCHECK)
		{
			switch(cmd)
			{
				case(CMD_REDEFINE_CONTROLS): 	showBlock(BLK_REDEFINE_CAR_CONTROLS); 	break;
				case(CMD_REDEFINE_CONTROLS2):	showBlock(BLK_REDEFINE_GAME_CONTROLS);	break;
				case(CMD_REDEFINE_CONTROLS3):	showBlock(BLK_ADVANCED_CONTROLS);	break;
			}

			if(cmd==CMD_AXISCHECK)
			{
				showBlock(BLK_AXISCHECK);
				player.controller.setcontrol(controlSet);
			}
			else
			{
				for(int i=0; i<NCONTROLS; i++)
				{
					if(keyText[i])
					{
						if(controlSet.deviceID[i] < 0 || controlSet.axisID[i] < 0) keyText[i].changeText("Undefined");
						else keyText[i].changeText(Input.axisName(controlSet.deviceID[i], controlSet.axisID[i]));
					}
				}

				for(int k=0; k<powerSlider.length; k++) powerSlider[k].setValue((controlSet.vasp.elementAt(k).power - 1.0) / 4.0); //value: 1.0~5.0
				for(int l=0; l<dzSlider.length; l++)
				{
					int l2 = l;
					if(l==dzSlider.length-1) l2 = l+4;

					dzSlider[l].setValue(controlSet.dead_zone[l2] / 0.2); //value: 0.0~0.2
				}
			}
		}

		if(cmd >= CMD_POWER && cmd < CMD_POWER+4)
		{
			float k = 4.0;
			float pow = 1.125 + k*osd.sliderValue;
			int ipower = pow*k;
			pow = ipower;
			pow = pow/k;
			controlSet.vasp.elementAt(cmd - CMD_POWER).power = pow;
			player.controller.setcontrol(controlSet);
		}
	}

	public void refreshCameras()
	{
		System.getConfigOptions();

		if(activeCamera) activeCamera.command("render 0");
		if(GameLogic.player && GameLogic.player.car) GameLogic.player.car.command("render 0 "+ GameLogic.player.controller.id());
	}

	public void setActiveCamera(GameRef cam)
	{
		activeCamera = cam;
	}

	public void animate()
	{
		//control remapper
		if(actBlock.id == BLK_REDEFINE_CAR_CONTROLS || actBlock.id == BLK_REDEFINE_GAME_CONTROLS)
		{
			if(getKey >= 0 && getKey < NCONTROLS)
			{
				int device;
				int axis = -1;

				for(device = 0; device < controlSet.nDevices; device++)
				{
					axis = Input.activeAxis(device);
					if(axis >= 0) break;
				}

				if(getKeyStat == 0)
				{
					if(axis < 0) getKeyStat = 1;
				} 
				else 
				if(getKeyStat == 1)
				{
					if(axis >= 0) 
					{
						float val = Input.getAxis(device, axis);

						keyText[getKey].changeText(Input.axisName (device, axis));
						controlSet.change(getKey, device, axis);

						int i;
						for(i=0; i < NCONTROLS; i++)
						{
							if(i != getKey && controlSet.deviceID[i] == device && controlSet.axisID[i] == axis) break;
						}

						if(device == 0 || val > 0.95f)
						{
							controlSet.from_min[getKey] = 0.0f;
							controlSet.from_max[getKey] = 1.0f;
						} 
						else 
						{
							if(i < NCONTROLS) 
							{
								if(val < 0.0f) 
								{
									controlSet.from_min[getKey] = 0.0f;
									controlSet.from_max[getKey] = -1.0f;
									controlSet.from_min[i] = 0.0f;
									controlSet.from_max[i] = 1.0f;
								} 
								else 
								{
									controlSet.from_min[getKey] = 0.0f;
									controlSet.from_max[getKey] = 1.0f;
									controlSet.from_min[i] = 0.0f;
									controlSet.from_max[i] = -1.0f;
								}
							} 
							else 
							{
								controlSet.from_min[getKey] = -1.0f;
								controlSet.from_max[getKey] = 1.0f;
							}
						}

						player.controller.setcontrol(controlSet);
						getKeyStat = 2;
					}
				} 
				else 
				{
					if(axis < 0) getKey = -1;
				}
			}
		}

		//gas/brake/steer axis position updater
		if(actBlock.id == BLK_AXISCHECK)
		{
			acSlider[0].setValue(Input.getInput(Input.AXIS_THROTTLE)/128.0);
			acSlider[1].setValue(Input.getInput(Input.AXIS_BRAKE)/128.0);
			acSlider[2].setValue(Input.getInput(Input.AXIS_TURN_LEFTRIGHT)/-256.0+0.5);
			acSlider[3].setValue(Input.getInput(Input.AXIS_CLUTCH)/128.0);
		}
	}
}

//combines menu groups into blocks
class MenuBlock
{
	Vector groups;
	String title;
	int id;
	int actGroup; //vector element id, not the number of group
	int hasScroll;

	public MenuBlock(int blockID, String blockTitle)
	{
		id = blockID;
		title = blockTitle;
		groups = new Vector();
	}

	public void push(int gID)
	{
		groups.addElement(new Integer(gID));
	}

	public int getFirstGroup()
	{
		return groups.elementAt(0).number;
	}

	public int getGroup(int gID)
	{
		return groups.elementAt(gID).number;
	}

	public int getCurrentGroup()
	{
		return groups.elementAt(actGroup).number;
	}

	public int nextGroup()
	{
		if(actGroup < groups.size()-1)
		{
			actGroup++;
			return 1;
		}

		return 0;
	}

	public int prevGroup()
	{
		if(actGroup > 0)
		{
			actGroup--;
			return 1;
		}

		return 0;
	}

	public int isScrollable()
	{
		if(groups.size()>1) return 1;

		return 0;
	}
}

class ControlsFileReqDialog extends FileRequesterDialog
{
	public ControlsFileReqDialog(Controller ctrl, int myflags, String mytitle, String OKButtonText, String path, String mask)
	{
		super(ctrl, myflags, mytitle, OKButtonText, path, mask);
		if(myflags & FRF_SAVE) osd.defSelection = 5;
	}

	public int validator(String filename)
	{
		return ControlSet.fileCheck(filename);
	}
}