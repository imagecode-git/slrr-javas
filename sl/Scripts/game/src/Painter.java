package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.render.osd.dialog.*;	//Text
import java.sound.*;

import java.game.parts.*;

//RAXAT: v2.3.1, paintcans and decals now have complete inventory, it's like working with normal parts
public class Painter extends GameType implements GameState, Callback
{
	final static int RID_PAINT_SPRAY	= frontend:0x9C16r;
	final static int RID_PAINT_PART		= frontend:0x9C04r;
	final static int RID_PAINT_DECAL	= frontend:0x9C09r;

	final static int RID_TRASH		= frontend:0x9C1Dr;
	final static int RID_PAINT_LOAD		= frontend:0x9C14r;
	final static int RID_PAINT_SAVE		= frontend:0x9C1Br;

	final static int RID_BTN_L		= frontend:0x0098r;
	final static int RID_BTN_M		= frontend:0x0099r;
	final static int RID_BTN_R		= frontend:0x009Ar;
		 
	final static int RID_BACKGROUND		= frontend:0x00C0r;
	final static float BACKGROUND_HEIGHT	= 0.40;

	final static float PARTS_VP_TOP		= 0.819;
	final static float PARTS_VP_LEFT	= 0.173;
	final static float PARTS_VP_WIDTH	= 0.752;
	final static float PARTS_VP_HEIGHT	= 0.169;

	final static ResourceRef PAINTBRUSH	= new ResourceRef(misc.garage:0x0102r);
	final static SfxRef SfxSPRAY		= new SfxRef(GameLogic.SFX_SPRAY);
	final static SfxRef SfxDECAL		= new SfxRef(GameLogic.SFX_DECAL);
	final static SfxRef SfxTRASH		= new SfxRef(GameLogic.SFX_TRASH);
	final static SfxRef SfxEMPTYCAN		= new SfxRef(GameLogic.SFX_EMPTYCAN);

	final static int RID_button_decal	= misc.garage:0x0000008Cr;

	final static int CMD_SCROLL_UP		= 0;
	final static int CMD_SCROLL_DOWN	= 1;
	final static int CMD_SPRAY		= 2;
	final static int CMD_PART		= 3;
	final static int CMD_DECALS		= 4;

	final static int CMD_DECALBUTTONS	= 64;  //0-63: colorbuttons hasznaljak!
	final static int CMD_NONE		= 100; //64-68: decalsbuttons hasznaljak!
	final static int CMD_BRUSHINC		= 102;
	final static int CMD_BRUSHDEC		= 103;
	final static int CMD_BRUSHRCW		= 104;
	final static int CMD_BRUSHRCCW		= 105;
	final static int CMD_BRUSHFLIP		= 106;
	final static int CMD_LOADSKIN		= 107;
	final static int CMD_SAVESKIN		= 108;
	final static int CMD_TRASH		= 109;

	final static int MODE_NONE 		= 0;
	final static int MODE_PAINTCOLOR	= 1;
	final static int MODE_PAINTPART		= 2;
	final static int MODE_PAINTDECAL	= 3;

	ControlSetState	css;

	Thread		painterThread;
	PaintInventory	paintCans;
	InventoryPanel	actualPanel;
	int		actualPanelChanged;
	int		overVehicle; //where is the pointer?

	int		mode;
	int		lastPaintMode = MODE_PAINTPART;

	GameRef		paintCursor;
	ResourceRef	paintBrush;

	int		brushColor = 0xFFFFFFFF;
	float		decalRotation;
	float		decalSize = 2.0;
	int		decalFlip;
	
	GameRef[]	decalButtons;
	RenderRef[]	decalButtonTypes = new RenderRef[5];
	RenderRef[]	decalButtonInstances = new RenderRef[5];
	ResourceRef[]	decalButtonTextures = new ResourceRef[5];
	int		showDecals;

	int		spraySfxID, spraySfxOn;

	int		handleControls;

	Osd		osd;
	int		paintGroup, colorsGroup, decalsGroup, brushGroup, sprayGroup, osdGroup;
	Text	moneyTxt, infoline, invLineTxt;
	
	Player	player;
	Multiplayer multiplayer;

	Gadget	red0, red1;

	int		flags;

	Object	caller;
	int		draggedDecal = -1;
	int		drain;

	final static int PF_REDUCED_FUNCTIONALITY = 1;
	final static int LOAD_PJ_PRICE = 1500; //RAXAT: this price is being taken for applying paintjobs

	//RAXAT: pigment consumption constants
	final static float CONS_PAINT = 1.5; //painting parts manually is more beneficial
	final static float CONS_PART = 165.0;

	public Painter(Player p, Multiplayer mp, Osd o, Object c, Text mt, Text il, int ctrl)
	{
		createNativeInstance();

		player = p;
		multiplayer = mp;
		osd = o;
		caller = c;
		moneyTxt = mt;
		infoline = il;
		handleControls = ctrl;

		//initialize osd
		osd.globalHandler = this;

		Style btnUp = new Style(0.1275, 0.1275, 1.0, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef(Osd.RID_ARROWUP));
		osd.createButton(btnUp, 0.9385, 0.6725, CMD_SCROLL_UP, "Scroll up", null);

		Style btnDn = new Style(0.1275, 0.1275, 1.0, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef(Osd.RID_ARROWDN));
		osd.createButton(btnDn, 0.9385, 0.9275, CMD_SCROLL_DOWN, "Scroll down", null);

		invLineTxt = osd.createText("1", Frontend.largeFont, Text.ALIGN_CENTER, 0.9445,  0.75);
		osd.createRectangle(0.0, 1.0-(BACKGROUND_HEIGHT/2), 2.0, BACKGROUND_HEIGHT, -1, new ResourceRef(RID_BACKGROUND)); //RAXAT: wide inventory background, inventory_holder.png in v2.3.1

		osd.hideGroup(osdGroup=osd.endGroup());

		Style buttonStyle = new Style(0.13, 0.13, Frontend.mediumFont, Text.ALIGN_LEFT, null);
		Menu m = osd.createMenu(buttonStyle, -0.975, 0.705, 0.095, Osd.MD_HORIZONTAL);

		red0 =	m.addItem(new ResourceRef(RID_PAINT_SPRAY), CMD_SPRAY, "Spray paint", null, 1);
			m.addItem(new ResourceRef(RID_PAINT_PART), CMD_PART, "Paint whole parts", null, 1);
		red1 =	m.addItem(new ResourceRef(RID_PAINT_DECAL), CMD_DECALS, "Apply decals", null, 1);

		m = osd.createMenu(buttonStyle, -0.975, 0.89, 0.095, Osd.MD_HORIZONTAL);

		m.addItem(new ResourceRef(RID_TRASH), CMD_TRASH, "Trash item", null, 1).enableDrop();
		m.addItem(new ResourceRef(RID_PAINT_LOAD), CMD_LOADSKIN, "Load paint job", null, 1);
		m.addItem(new ResourceRef(RID_PAINT_SAVE), CMD_SAVESKIN, "Save paint job", null, 1);

		osd.hideGroup(paintGroup = osd.endGroup());
		osd.hideGroup(colorsGroup = osd.endGroup());
	
		decalButtons=new GameRef[5];
		float k=-0.525, l=0.305;
		decalButtons[0] = osd.createButton(RID_button_decal,   k, 0.80, null, Frontend.mediumFont, Text.ALIGN_LEFT, this, CMD_DECALBUTTONS);   k+=l;
		decalButtons[1] = osd.createButton(RID_button_decal,   k, 0.80, null, Frontend.mediumFont, Text.ALIGN_LEFT, this, CMD_DECALBUTTONS+1); k+=l;
		decalButtons[2] = osd.createButton(RID_button_decal,   k, 0.80, null, Frontend.mediumFont, Text.ALIGN_LEFT, this, CMD_DECALBUTTONS+2); k+=l;
		decalButtons[3] = osd.createButton(RID_button_decal,   k, 0.80, null, Frontend.mediumFont, Text.ALIGN_LEFT, this, CMD_DECALBUTTONS+3); k+=l;
		decalButtons[4] = osd.createButton(RID_button_decal,   k, 0.80, null, Frontend.mediumFont, Text.ALIGN_LEFT, this, CMD_DECALBUTTONS+4); k+=l;

		osd.hideGroup(decalsGroup = osd.endGroup());

		buttonStyle = new Style(0.33, 0.12, Frontend.smallFont, Text.ALIGN_RIGHT, new ResourceRef(RID_BTN_R));
		m = osd.createMenu(buttonStyle, 1.01, 0.42, -0.125, Osd.MD_VERTICAL);
		m.addItem("FLIP", CMD_BRUSHFLIP);
		m.addItem("ROTATE CCW", CMD_BRUSHRCCW);
		m.addItem("ROTATE CW", CMD_BRUSHRCW);

		buttonStyle = new Style(0.33, 0.12, Frontend.smallFont, Text.ALIGN_LEFT, new ResourceRef(RID_BTN_L));
		m = osd.createMenu(buttonStyle, -1.01, 0.525, -0.125, Osd.MD_VERTICAL);
		m.addItem("BIGGER", CMD_BRUSHINC);
		m.addItem("SMALLER", CMD_BRUSHDEC);

		osd.createHotkey(Input.RCDIK_TAB, Input.KEY, CMD_BRUSHFLIP, this);
		osd.createHotkey(Input.RCDIK_Q,   Input.KEY, CMD_BRUSHRCCW, this);
		osd.createHotkey(Input.RCDIK_W,   Input.KEY, CMD_BRUSHRCW, this);
		osd.createHotkey(Input.RCDIK_A,   Input.KEY, CMD_BRUSHINC, this);
		osd.createHotkey(Input.RCDIK_S,   Input.KEY, CMD_BRUSHDEC, this);

		osd.createHotkey(Input.RCDIK_NUMPAD4, Input.KEY, CMD_BRUSHRCCW, this);
		osd.createHotkey(Input.RCDIK_NUMPAD6, Input.KEY, CMD_BRUSHRCW, this);
		osd.createHotkey(Input.RCDIK_NUMPAD8, Input.KEY, CMD_BRUSHINC, this);
		osd.createHotkey(Input.RCDIK_NUMPAD2, Input.KEY, CMD_BRUSHDEC, this);
		osd.createHotkey(Input.RCDIK_NUMPAD5, Input.KEY, CMD_BRUSHFLIP, this);	

		osd.hideGroup(brushGroup = osd.endGroup());

		m = osd.createMenu(buttonStyle, -1.01, 0.525, -0.125, Osd.MD_VERTICAL);
		m.addItem("BIGGER", CMD_BRUSHINC);
		m.addItem("SMALLER", CMD_BRUSHDEC);

		osd.createHotkey(Input.RCDIK_A, Input.KEY, CMD_BRUSHINC, this);
		osd.createHotkey(Input.RCDIK_S, Input.KEY, CMD_BRUSHDEC, this);

		osd.createHotkey(Input.RCDIK_NUMPAD8, Input.KEY, CMD_BRUSHINC, this);
		osd.createHotkey(Input.RCDIK_NUMPAD2, Input.KEY, CMD_BRUSHDEC, this);

		osd.hideGroup(sprayGroup = osd.endGroup());

		//se bug workaround, fix it! (test with new carrer)
		Part dummy = new Part();

		//paint cans
		paintCans = new PaintInventory(player, this, PARTS_VP_LEFT, PARTS_VP_TOP, PARTS_VP_WIDTH, PARTS_VP_HEIGHT);
	}


	public void show()
	{
		if(player.decals.size() <= showDecals) showDecals = 0;

		if(flags & PF_REDUCED_FUNCTIONALITY)
		{
			red0.disable();
			red1.disable();

			lastPaintMode = MODE_PAINTPART;
		}
		else
		{
			red0.enable();
			red1.enable();
		}

		osd.showGroup(osdGroup);
		changeMode(lastPaintMode);

		painterThread = new Thread(this, "Painter inventory part thread");
		painterThread.start();

		//RAXAT: build 934, previously loaded paintcan id could be incorrect
		if(paintCans.items.size()) paintCans.paintColor = paintCans.getCanbyIndex(paintCans.lastCanId).color;

		Input.cursor.addHandler(this);	//kivancsiak vagyunk ra, mit csinal az eger
		setEventMask(EVENT_CURSOR|EVENT_TIME);

		if(handleControls)
		{
			Input.cursor.setPointer(Frontend.pointers, "D");
			Input.cursor.enable(1);
			Input.getAxis(1, -1);
			css = player.controller.reset();
			player.controller.activateState(ControlSet.CAMTURNSET);
		}
	}


	public void hide()
	{
		clearEventMask(EVENT_ANY);
		Input.cursor.remHandler(this);

	
		if(handleControls)
		{
			Input.cursor.setPointer(Frontend.pointers, "J");
			Input.cursor.enable(0);
			player.controller.reset(css);
		}

		if(painterThread)
		{
			painterThread.stop();
			painterThread = null;
		}

		changeMode(MODE_NONE);
		osd.hideGroup(osdGroup);

		infoline.changeText(null);
	}

	public void changeMode(int newMode)
	{
		if(mode != newMode)
		{
			if(mode == MODE_PAINTCOLOR)
			{
				paintCans.hide();
				osd.hideGroup(paintGroup);
				osd.hideGroup(colorsGroup);
				osd.hideGroup(sprayGroup);
				lastPaintMode=newMode;
			}
			else
			if(mode == MODE_PAINTPART)
			{
				paintCans.hide();
				osd.hideGroup(paintGroup);
				osd.hideGroup(colorsGroup);
				lastPaintMode=newMode;
			}
			else
			if(mode == MODE_PAINTDECAL)
			{
				clearDecalButtons();
				osd.hideGroup(paintGroup);
				osd.hideGroup(decalsGroup);
				osd.hideGroup(brushGroup);
				lastPaintMode=newMode;
			}

			mode=newMode;

			//-----------mode ONs
			if(mode == MODE_PAINTCOLOR)
			{
				paintCans.show();
				invLineTxt.changeText(Integer.toString(paintCans.currentLine()+1));
				osd.showGroup(paintGroup);
				osd.showGroup(colorsGroup);
				osd.showGroup(sprayGroup);

				paintBrush=PAINTBRUSH;
			}
			else
			if(mode == MODE_PAINTPART)
			{
				paintCans.show();
				invLineTxt.changeText(Integer.toString(paintCans.currentLine()+1));
				osd.showGroup(paintGroup);
				osd.showGroup(colorsGroup);
			}
			else
			if(mode == MODE_PAINTDECAL)
			{
				setDecalButtons();

				if(!player.decals.isEmpty())
				{
					paintBrush=player.decals.elementAt(showDecals);
				}
				else paintBrush=null;

				osd.showGroup(paintGroup);
				osd.showGroup(decalsGroup);
				osd.showGroup(brushGroup);
			}
		}
	}

	native static void doPaint(GameRef cursor, int color, int brush, int temp, float rot, float size, int flip);
	native void paintPart(GameRef cursor, int color);
	native void xPaint(GameRef part);

	public void beginPaint(int cursor_id)
	{
		paintCursor=new GameRef(cursor_id);
		//enableControlHook();
		enableAnimateHook(); //build 931: using animate(), which is faster than control()
	}

	public void endPaint()
	{
		if(paintCursor)
		{
			//disableControlHook();
			disableAnimateHook(); //build 931: using animate(), which is faster than control()
			paintCursor=null;
		}
	}

	public void setDecalButtons()
	{
		int	max = player.decals.size();

		clearDecalButtons();

		for(int i=0; i<decalButtons.length; i++)
		{
			RenderRef base = new RenderRef(misc.garage:0x0083r);
	
			if(max > showDecals+i)
			{
				//duplikaljuk a tipust, hogy a texturamodositas egyedi legyen
				decalButtonTypes[i] = new RenderRef();
				decalButtonTypes[i].duplicate(base);

				ResourceRef baseTex = new ResourceRef(misc.garage:0x0103r);//a gomb tipus default texturaja
				ResourceRef rr = player.decals.elementAt(showDecals+i); //az uj textura
				//lecsereljuk!
				decalButtonTypes[i].changeResource(baseTex, rr);
				decalButtonTextures[i] = rr;

				decalButtonInstances[i] = new RenderRef(decalButtons[i], decalButtonTypes[i].id(), "decal button");
				decalButtons[i].queueEvent(null, GameType.EVENT_COMMAND, "render " + decalButtonInstances[i].id());
			}
			else	decalButtonTypes[i] = null;//base;

		}

		invLineTxt.changeText(Integer.toString((showDecals+1)/decalButtons.length+1));
	}

	public void clearDecalButtons()
	{
		for(int i=0; i<decalButtons.length; i++)
		{
			if(decalButtonInstances[i])
			{
				decalButtonInstances[i].destroy();
				decalButtonInstances[i]=null;
			}
			if(decalButtonTypes[i])
			{
				decalButtonTypes[i].destroy();
				decalButtonTypes[i]=null;
			}
			if(decalButtonTextures[i])
			{
				decalButtonTextures[i].unload();
				decalButtonTextures[i] = null;
			}
		}
	}

	//RAXAT: re-adressed event from osd
	public void handleOsdEvent(GameRef obj_ref, int event, String param)
	{
		int drag = param.token(3).intValue();
		if(caller) caller.callback(drag); //RAXAT: 3rd token does tell us if the drag is activated or not

		if(drag) draggedDecal = param.token(4).intValue(); //RAXAT: 4th token is decal cmd, we could use it to figure out what decal is being dragged
	}

	public void handleEvent(GameRef obj_ref, int event, int param)
	{
		if(event == EVENT_TIME)
		{
			switch(param)
			{
				case 1:
					if(drain)
					{
						updateCanCapacity(CONS_PAINT*decalSize);
						addTimer(0.1, 1);
					}
					else removeAllTimers();
					break;
			}
		}
	}

	public void handleEvent(GameRef obj_ref, int event, String param)
	{
		int	tok = -1;

		if(event == EVENT_CURSOR)
		{
			int ec = param.token(++tok).intValue();
			int cursor_id = param.token(++tok).intValue();

			if(ec == GameType.EC_LCLICK)
			{
				int	obj_id = param.token(++tok).intValue();	//ignored now
				Vector3 obj_pos =  new Vector3(param.token(++tok).floatValue(), param.token(++tok).floatValue(), param.token(++tok).floatValue());
				Vector3 worldpos = new Vector3(param.token(++tok).floatValue(), param.token(++tok).floatValue(), param.token(++tok).floatValue());

				GameRef dest = obj_ref;
				int cat = dest.getInfo(GameType.GII_CATEGORY);
				
				if(dest.getInfo(GameType.GII_PAINTABLE)) //RAXAT: only paintable parts will be painted now
				{
					if(mode==MODE_PAINTPART)
					{
						if(paintCans.size())
						{
							if(cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE)
							{
								PaintCan p = paintCans.getCanbyIndex(paintCans.lastCanId);
								if(p)
								{
									float cons = CONS_PART*(dest.getInfo(GameType.GII_SIZE)/100.0);
									if(p.capacity >= cons)
									{
										updateCanCapacity(cons);

										SfxSPRAY.play(worldpos, 0.0, 1.0, 1.0, 0);
										paintPart(new GameRef(cursor_id), paintCans.paintColor|0xFF000000);
									}
									else
									{
										YesNoDialog d = new YesNoDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFO", "Not enough pigment in selected paint can! At least " + (int)cons + "ml is required. Do you want to refill this paint can?");
										if(!d.display())
										{
											float price = PaintBooth.calcPaintCanPrice(p.initialCapacity, 1);
											
											if(player.money < price) new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_HIGHPRI, "NOT ENOUGH MONEY", "You don't have $" + price + " to refill this paint can!").display();
											else
											{
												if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.spendMoney(price);
												player.takeMoney(price);
												if(caller) caller.callback(0); //we call this to update moneytxt in paint booth
												
												p.refill();
												updateCanCapacity(0.0f);
												new SfxRef(Frontend.SFX_MONEY).play();
											}
										}
									}
								}
							}
						}
					}
					else
					if(mode==MODE_PAINTDECAL)
					{	
						if(cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE)
						{
							if(paintBrush)
							{
								SfxDECAL.play(worldpos, 0.0, 1.0, 1.0, 0); 
								doPaint(new GameRef(cursor_id), brushColor, paintBrush.id(), 0, decalRotation, decalSize, decalFlip);

								//itt biztos hogy Decal tipusu, es a player.decals-ban van
								if(!(-- ((Decal)paintBrush).stickies))
								{
									player.decals.removeElement(paintBrush);
									paintBrush=null;

									int max = player.decals.size();	//kiurult sor?
									if(showDecals >= decalButtons.length && showDecals >= max) showDecals-=decalButtons.length;
							
									setDecalButtons();
									infoline.changeText(null);
								}
								else	infoline.changeText(((Decal)paintBrush).stickies + " stickies left"); //mirrored!
							}
						}
					} 
				}
			}
			else
			if(ec == GameType.EC_LDOWN)
			{
				if(overVehicle)
				{
					if(mode==MODE_PAINTCOLOR)
					{
						if(paintCans.size())
						{
							PaintCan p = paintCans.getCanbyIndex(paintCans.lastCanId);
							if(p)
							{
								Vector3 worldpos = Input.cursor.getPickedPos();

								float cons = CONS_PAINT*decalSize;
								if(p.capacity >= cons)
								{
									drain = 1;
									addTimer(0.1, 1);

									startSpray(worldpos, cursor_id);
								}
								else //RAXAT: build 934
								{
									YesNoDialog d = new YesNoDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFO", "Not enough pigment in selected paint can! At least " + (int)cons + "ml is required. Do you want to refill this paint can?");
									if(!d.display())
									{
										float price = PaintBooth.calcPaintCanPrice(p.initialCapacity, 1);
										
										if(player.money < price) new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_HIGHPRI, "NOT ENOUGH MONEY", "You don't have $" + price + " to refill this paint can!").display();
										else
										{
											if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.spendMoney(price);
											player.takeMoney(price);
											if(caller) caller.callback(0); //we call this to update moneytxt in paint booth
											
											p.refill();
											updateCanCapacity(0.0f);
											new SfxRef(Frontend.SFX_MONEY).play();
										}
									}
								}
							}
						}
					}
				}
			} 
			else
			if(ec == GameType.EC_LUP)
			{
				if(mode==MODE_PAINTCOLOR) stopSpray();
			}
			else
			if(ec == GameType.EC_HOVER)
			{
				GameRef part, dest = new GameRef(param.token(++tok).intValue());
				int phyId = param.token(++tok).intValue();
				int objectChanged;

				Object p = dest.getScriptInstance();
				if(p instanceof Part)
				{
					if(!overVehicle)
					{
						overVehicle=1;
						objectChanged=1;
					}
				}
				else
				{
					if(overVehicle)
					{
						overVehicle=0;
						objectChanged=1;
					}
				}

				if(mode==MODE_PAINTDECAL)
				{
					if(objectChanged)
					{
						if(overVehicle) beginPaint(cursor_id);
						else
						{
							endPaint();
							if(player.car) xPaint(player.car); //flush texture
						}
					}
				}
				else
				{
					InventoryPanel panel;
					if((panel = paintCans.getPanelbyButtonPhyId(phyId)))
					{
						actualPanel = panel;
						actualPanelChanged = 1;
					}
					else
					{
						if(actualPanel)
						{
							actualPanel = null;
							actualPanelChanged = 1;
						}
					}
				}
			}
			else
			if(ec == GameType.EC_RDRAGBEGIN)
			{
				//enable camera control with mouse
				//player.controller.user_Add(Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);
				//player.controller.user_Add(Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);
				//disable cursor movement
				player.controller.user_Del(Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0);
				player.controller.user_Del(Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1);
				Input.cursor.cursor.queueEvent(null, GameType.EVENT_COMMAND, "lock");
			} 
			else
			if(ec == GameType.EC_RDRAGEND)
			{
				//disable camera control with mouse
				//player.controller.user_Del(Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1);
				//player.controller.user_Del(Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0);
				//enable cursor movement
				player.controller.user_Add( Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);
				player.controller.user_Add( Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);
				Input.cursor.cursor.queueEvent(  null, GameType.EVENT_COMMAND, "unlock");

				draggedDecal = -1;
			}
		}
	}

	//public void control(float t)
	public void animate() //build 931: using animate(), which is faster than control()
	{
		if(mode == MODE_PAINTDECAL)
		{
			if(paintBrush) doPaint(paintCursor, brushColor, paintBrush.id(), 1, decalRotation, decalSize, decalFlip);
		}
		else
		if(mode == MODE_PAINTCOLOR)
		{
			Vector3 worldpos = Input.cursor.getPickedPos();

			spraySfxID = SfxSPRAY.play(worldpos, 0.0, 1.0, 1.0, SfxRef.SFX_LOOP | SfxRef.SFX_NOAUTOSTOP, Input.cursor.id());  //update looped sfx
			doPaint(paintCursor, paintCans.paintColor|0x66000000, paintBrush.id(), 0, decalRotation, decalSize, decalFlip);
		}
	}

	public void startSpray(Vector3 pos, int cursor_id)
	{
		spraySfxOn=1;
		spraySfxID = SfxSPRAY.play(pos, 0.0, 1.0, 1.0, SfxRef.SFX_LOOP | SfxRef.SFX_NOAUTOSTOP, Input.cursor.id());  //start looped sfx
		beginPaint(cursor_id);
	}

	public void stopSpray()
	{
		if(spraySfxOn)
		{
			drain = 0;
			SfxSPRAY.stop(spraySfxID);  //end looped sfx
			spraySfxOn=0;
		}
		endPaint();
	}

	//RAXAT: v2.3.1, load/unload player's paint inventory
	public void flushInventory()
	{
		int i = paintCans.size();
		while(i--) paintCans.moveToInventory(0, player.paintcans);
	}

	public void filterInventory()
	{
		InventoryItem t;
		for(int i=0; i<player.paintcans.size(); i++)
		{
			t = player.paintcans.items.elementAt(i);
			if(t instanceof InventoryItem_Paint) player.paintcans.moveToInventory(i--, paintCans);
		}
	}

	//RAXAT: v2.3.1, inventory line index visual update patch
	public void updateLineIndex()
	{
		String curIdx = invLineTxt.text;
		String newIdx = Integer.toString(paintCans.currentLine()+1);

		if(curIdx != newIdx) invLineTxt.changeText(newIdx);
	}

	public void callback()
	{
		updateCanCapacity(0.0f); //RAXAT: this will be always called when we click paintcan in the inventory
	}
	
	public void run()
	{
		for(;;)
		{
			InventoryPanel ap = actualPanel;
			if(ap) ap.focusHook();

			painterThread.sleep(20);
		}
	}

	public void updateCanCapacity(float value)
	{
		PaintCan p = paintCans.getCanbyIndex(paintCans.lastCanId);
		if(p)
		{
			String str;
			if(value >= 0)
			{
				if(p.capacity >= value) p.capacity -= value;
				else
				{
					SfxEMPTYCAN.play();
					stopSpray();
				}

				if(p.capacity < 0)
				{
					SfxEMPTYCAN.play();
					stopSpray();
					p.capacity = 0;
				}
			}

			if(p.capacity >= 1) str = "Pigment inside: " + (int)p.capacity + "ml";
			else str = "Empty! Trash it!";

			infoline.changeText(str);
		}
		else infoline.changeText(null);
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_TRASH):
				if(osd.dropGadget || osd.dropObject)
				{
					InventoryItem item;
					if(item = paintCans.getItembyButton(osd.dropGadget)) //RAXAT: paint can is dragged
					{
						YesNoDialog d = new YesNoDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "TRASH ITEM", "Do you want to trash this paint can?");
						if(!d.display())
						{
							paintCans.removeItem(item);
							paintCans.update();
							updateLineIndex();
							updateCanCapacity(0.0f);

							SfxTRASH.play();
						}
					}
					else
					{
						if(draggedDecal >= 0) //RAXAT: a decal is being dragged
						{
							YesNoDialog d = new YesNoDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "TRASH ITEM", "Do you want to trash this decal?");
							if(!d.display())
							{
								int decalIndex = showDecals + draggedDecal-CMD_DECALBUTTONS;
								if(player.decals.size() > decalIndex)
								{
									ResourceRef decal = player.decals.elementAt(decalIndex);
									if(decal)
									{
										if(paintBrush)
										{
											if(paintBrush.id() == decal.id()) //RAXAT: we check if this decal is currently in use
											{
												infoline.changeText(null);
												paintBrush = null;
											}
										}

										player.decals.removeElement(decal);
										decal = null;

										SfxTRASH.play();

										int max = player.decals.size();
										if(showDecals >= decalButtons.length && showDecals >= max) showDecals-=decalButtons.length;
						
										setDecalButtons();
									}
								}
							}
						}
					}
				}
				else
				{
					//RAXAT: now we can trash all paintcans from inventory
					int size = paintCans.size();
					
					if(size)
					{
						YesNoDialog d = new YesNoDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "TRASH ITEM", "You're about to trash all your " + size + " paint cans, are you sure?");
						if(!d.display())
						{
							for(int i=0; i<size; i++) paintCans.items.removeElementAt(i);

							paintCans.update();
							updateLineIndex();
							updateCanCapacity(0.0f);
							SfxTRASH.play();
						}
					}
					else new WarningDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFO", "Drop items here from the inventory to trash them.").display();
				}
				break;

			case(CMD_LOADSKIN):
				VehicleSkinFileReqDialog d = new VehicleSkinFileReqDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "LOAD PAINT JOB", "LOAD", GameLogic.skinSaveDir, "*");

				if(d.display() == 0)
				{
					String filename = GameLogic.skinSaveDir + d.fileName;
					player.car.loadSkin(filename);
					if(moneyTxt)
					{
						if(player.getMoney() >= LOAD_PJ_PRICE)
						{
							player.takeMoney(LOAD_PJ_PRICE);
							moneyTxt.changeText("$" + Integer.toString(player.getMoney()));
							GameLogic.spendTime(GameLogic.timeRefreshRate*GameLogic.timeFactor*4*((1+Math.random())*12));
						}
						else new WarningDialog
						(
							player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "NOT ENOUGH MONEY", "You don't have enough money to apply paintjob to your car."
						).display();
					}
				}
				break;

			case(CMD_SAVESKIN):
				VehicleSkinFileReqDialog d = new VehicleSkinFileReqDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.FRF_SAVE, "SAVE PAINT JOB", "SAVE", GameLogic.skinSaveDir, "*");

				if(d.display() == 0)
				{
					//fixed errors when attempting to overwrite existing files
					String filename = GameLogic.skinSaveDir + d.fileName;
					if (File.exists(filename))
					{
						File.delete(filename);
						File.delete(GameLogic.skinSaveDir, d.fileName+".*");
					}

					player.car.saveSkin(filename);
					new WarningDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "SAVE PAINT JOB", "Paint job has been saved.").display();
				}
				break;

			case(CMD_SCROLL_UP):
				if(mode == MODE_PAINTCOLOR || mode == MODE_PAINTPART)
				{
					paintCans.upScroll();
					invLineTxt.changeText(Integer.toString(paintCans.currentLine()+1));
				}
				else
				if(mode == MODE_PAINTDECAL)
				{
					int max = player.decals.size();
					if(max > decalButtons.length)
					{
						if(showDecals >= decalButtons.length) showDecals-=decalButtons.length;
						else showDecals = (max-1)-((max-1)%decalButtons.length);

						setDecalButtons();
					}
				}
				break;

			case(CMD_SCROLL_DOWN):
				if(mode == MODE_PAINTCOLOR || mode == MODE_PAINTPART)
				{
					paintCans.downScroll();
					invLineTxt.changeText(Integer.toString(paintCans.currentLine()+1));
				}
				else
				if(mode == MODE_PAINTDECAL)
				{
					int max = player.decals.size();
					if(max > decalButtons.length)
					{
						if(showDecals+decalButtons.length < max) showDecals+=decalButtons.length;
						else showDecals = 0;

						setDecalButtons();
					}
				}
				break;

			case(CMD_SPRAY):
				changeMode(MODE_PAINTCOLOR);
				updateCanCapacity(0.0f);
				break;

			case(CMD_PART):
				changeMode(MODE_PAINTPART);
				updateCanCapacity(0.0f);
				break;

			case(CMD_DECALS):
				changeMode(MODE_PAINTDECAL);
				String str = null;
				if(paintBrush) str = ((Decal)paintBrush).stickies + " stickies left";
				infoline.changeText(str);
				break;

			case(CMD_BRUSHRCCW):
				decalRotation -= 0.15;
				break;

			case(CMD_BRUSHRCW):
				decalRotation += 0.15;
				break;

			case(CMD_BRUSHINC):
				if(decalSize < 3.0) decalSize += 0.05;
				break;

			case(CMD_BRUSHDEC):
				if(decalSize > 0.05) decalSize -= 0.05;
				break;

			case(CMD_BRUSHFLIP):
				decalFlip = 1-decalFlip;
				break;
		}

		if(cmd >= CMD_DECALBUTTONS && cmd <= CMD_DECALBUTTONS + decalButtons.length)
		{
			int decalIndex = showDecals + cmd-CMD_DECALBUTTONS;
			if(player.decals.size() > decalIndex)
			{
				paintBrush=player.decals.elementAt(decalIndex);
				infoline.changeText(((Decal)paintBrush).stickies + " stickies left");	//mirrored!
			}
		}
	}
}

class PaintInventory extends VisualInventory
{
	int	paintColor;
	int	lastCanId;

	public PaintInventory(Player player, Object caller, float left, float top, float width, float height)
	{
		super(player, left, top, width, height, 1);
		this.caller = caller; //RAXAT: v2.3.1, paint inventory is calling back painter to force it update itself
	}

	public void panelLeftClick(int index)
	{
		index += currentLine()*partsPerLine;
		if(index < items.size()) //sohasem legyen ures!!
		{		
			lastCanId=index;

			PaintCan pc = getCanbyIndex(index);
			paintColor = pc.color;

			new SfxRef(Frontend.SFX_MENU_SELECT).play();
			caller.callback();
		}
	}

	public void panelSwap(int index_a, Gadget dropped)
	{
		super.panelSwap(index_a, dropped); //RAXAT: new in v2.3.1
	}

	public void panelDragNDrop(int index, GameRef dropped)
	{
	}

	public int addItem(PaintCan p)
	{
		if(!items.size()) paintColor = p.color;
		return super.addItem(p);
	}
}

class VehicleSkinFileReqDialog extends FileRequesterDialog
{
	public VehicleSkinFileReqDialog(Controller ctrl, int myflags, String mytitle, String OKButtonText, String path, String mask)
	{
		super(ctrl, myflags, mytitle, OKButtonText, path, mask);
	}

	public int validator(String filename)
	{
		return Vehicle.fileCheck_Skin(filename);
	}
}
