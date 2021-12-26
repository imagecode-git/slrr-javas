package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.render.osd.dialog.*;	//Text
import java.sound.*;

import java.game.parts.*;
import java.game.parts.bodypart.*;

public class Mechanic extends GameType implements Callback //RAXAT: GT to handle osd commands
{
	final static int RID_REPAIR_PART	= frontend:0x9C18r;
	final static int RID_REPAIR_CAR		= frontend:0x9C17r;
	final static int RID_SELL_PART		= frontend:0x9C1Dr;

	final static int RID_FILTER_ENGINE	= frontend:0x9C0Dr;
	final static int RID_FILTER_BODY	= frontend:0x9C06r;
	final static int RID_FILTER_RGEAR	= frontend:0x9C23r;

	final static int RID_BACKGROUND = frontend:0x00C0r;
	final static float BACKGROUND_HEIGHT = 0.40;

	final static float PARTS_VP_TOP = 0.819;
	final static float PARTS_VP_LEFT = 0.173;
	final static float PARTS_VP_WIDTH = 0.752;
	final static float PARTS_VP_HEIGHT = 0.169;

	final static int CMD_SCROLL_UP = 0;
	final static int CMD_SCROLL_DOWN = 1;
	final static int CMD_REPAIR = 2;
	final static int CMD_REPAIR_ALL = 3;
	final static int CMD_SELL = 4;
	final static int CMD_ENGINE		= 119;
	final static int CMD_BODY			= 120;
	final static int CMD_RUNNING_GEAR	= 121;

	final static SfxRef  SfxWRENCH = new SfxRef( GameLogic.SFX_WRENCH );

	Multiplayer	multiplayer;
	Player		player;
	Osd			osd;
	int			osdGroup;
	int			handleControls;

	ControlSetState	css;

	VisualInventory	inventory;
	Text		invLineTxt;

	Text		infoline;
	Text		moneyTxt;

	InventoryPanel	actualPanel;
	int			actualPanelChanged;
	int			overVehicle;

	GameRef		lastLookDestination;
	GameRef		camera, map;

	int			mode, lift; //RAXAT: build 931, garage could prevent waking up the car
	int			filterEngine, filterBody, filterRGear;

	Thread		spinnerThread;

	final static int MF_FREE_REPAIRS = 1;
	static int flags;	//static? khm!!! (mechTIme miatt ko:ll)

	public Mechanic( Player p, Osd o, Text mt, Text il, int ctrl, Multiplayer mp)
	{
		multiplayer = mp;
		createNativeInstance();

		player=p;
		osd=o;
		moneyTxt = mt;
		infoline = il;
		handleControls = ctrl;

		inventory = new VisualInventory( player, PARTS_VP_LEFT, PARTS_VP_TOP, PARTS_VP_WIDTH, PARTS_VP_HEIGHT );
		inventory.caller = this;

		osd.globalHandler = this;

		Style btnUp = new Style( 0.1275, 0.1275, 1.0, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef( Osd.RID_ARROWUP ) );
		osd.createButton( btnUp, 0.9385, 0.6725, CMD_SCROLL_UP, "Scroll up", null );

		Style btnDn = new Style( 0.1275, 0.1275, 1.0, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef( Osd.RID_ARROWDN ) );
		osd.createButton( btnDn, 0.9385, 0.9275, CMD_SCROLL_DOWN, "Scroll down", null );

		invLineTxt = osd.createText( "1", Frontend.largeFont, Text.ALIGN_CENTER, 0.9445,  0.75);
		osd.createRectangle( 0.0, 1.0-(BACKGROUND_HEIGHT/2), 2.0, BACKGROUND_HEIGHT, -1, new ResourceRef( RID_BACKGROUND ) ); //RAXAT: wide inventory background, inventory_holder.png in v2.3.1

		Style buttonStyle = new Style( 0.13, 0.13, Frontend.mediumFont, Text.ALIGN_LEFT, null );
		Menu m = osd.createMenu( buttonStyle, -0.975, 0.705, 0.095, Osd.MD_HORIZONTAL );

		m.addItem( new ResourceRef( RID_REPAIR_PART ), CMD_REPAIR, "Repair Part", null, 1 ).enableDrop();
		m.addItem( new ResourceRef( RID_REPAIR_CAR ), CMD_REPAIR_ALL, "Repair all the car", null, 1 );
		m.addItem( new ResourceRef( RID_SELL_PART ), CMD_SELL, "Sell Part", null, 1 ).enableDrop();

		m = osd.createMenu( buttonStyle, -0.975, 0.91, 0.095, Osd.MD_HORIZONTAL );

		m.addItem( new ResourceRef( RID_FILTER_ENGINE ), CMD_ENGINE, "Hide/Unhide ENGINE Parts", null, 1 );
		m.addItem( new ResourceRef( RID_FILTER_BODY ), CMD_BODY, "Hide/Unhide BODY Parts", null, 1 );
		m.addItem( new ResourceRef( RID_FILTER_RGEAR ), CMD_RUNNING_GEAR, "Hide/Unhide RUNNING GEAR Parts", null, 1 );

		osd.hideGroup( osdGroup=osd.endGroup() );
	}

	public void show()
	{
		osd.showGroup(osdGroup);
		inventory.show();

		spinnerThread = new Thread( this, "Mechanic inventory part spinner" );
		spinnerThread.start();
		//spinnerThread.setPriority( Thread.MAX_PRIORITY );

		Input.cursor.addHandler(this);	//kivancsiak vagyunk ra, mit csinal az eger
		setEventMask( EVENT_CURSOR );

		if( handleControls )
		{
			Input.cursor.setPointer( Frontend.pointers, "G");
			Input.cursor.enable(1);
			Input.getAxis (1, -1);
			css = player.controller.reset();
			player.controller.activateState(ControlSet.CAMTURNSET);
		}
	}

	public void hide()
	{
		clearEventMask( EVENT_ANY );
		Input.cursor.remHandler(this);

		if( handleControls )
		{
			Input.cursor.setPointer( Frontend.pointers, "J");
			Input.cursor.enable(0);
			player.controller.reset(css);
		}

		if( spinnerThread )
		{
			spinnerThread.stop();
			spinnerThread=null;
		}

		infoline.changeText( null );
		inventory.hide();
		osd.hideGroup(osdGroup);
	}

	//RAXAT: v2.3.1, inventory line index update patch
	public void callback()
	{
		if(lift) player.car.command("reset");
		updateLineIndex(); //visual inventory will call this every time when part is being installed to car from it
	}

	public void run()
	{
		for(;;)
		{
			int i;

			//bizony, itt az ideje hogy syncelni is lehessen...
			//(actualpanel fagyas..)
			InventoryPanel	ap = actualPanel;

			if( actualPanelChanged || inventory.visualsUpdated )
			{
				actualPanelChanged=0;
				inventory.visualsUpdated=0;

				if(ap)
				{
					if(ap.invItem) infoline.changeText(ap.invItem.getInfo());
					else infoline.changeText("empty slot");
				}
				else infoline.changeText(null);

			}

			if(ap) ap.focusHook();

			spinnerThread.sleep(20);	//40:25fps  20:50fps
		}
	}

	//RAXAT: v2.3.1, inventory line index visual update patch
	public void updateLineIndex()
	{
		String curIdx = invLineTxt.text;
		String newIdx = Integer.toString(inventory.currentLine()+1);

		if(curIdx != newIdx) invLineTxt.changeText(newIdx);
	}

	public void handleEvent( GameRef obj_ref, int event, String param )
	{
		int	tok = -1;

		if( event == EVENT_CURSOR )
		{
			int	ec = param.token( ++tok ).intValue();
			int	cursor_id = param.token( ++tok ).intValue();

			if(ec == GameType.EC_LCLICK)
			{
				int obj_id = param.token( ++tok ).intValue();	//ignored now
				Object part = obj_ref.getScriptInstance();
				Vector3 obj_pos =  new Vector3(param.token(++tok).floatValue(), param.token(++tok).floatValue(), param.token(++tok).floatValue());
				Vector3 worldpos = new Vector3(param.token(++tok).floatValue(), param.token(++tok).floatValue(), param.token(++tok).floatValue());

				if( part instanceof Part && GameLogic.player.car && GameLogic.player.car.chassis && part.getCarRef() && part.getCarRef().id() == GameLogic.player.car.chassis.getCarRef().id() ) //RAXAT: we check if it's a player's car, so other cars in the garage won't be affected
				{
					if( mode == 0 )
					{	//szereles mode
						if( !(part instanceof Chassis) )
						{
							int reason = part.getInfo(GameType.GII_GETOUT_OK);

							if (reason!=-1)
							{	//semmi akadalya, kiszerelheto

								if( lastLookDestination )
								{
									if( lastLookDestination.id() == part.id() )
									{
										camera.command( "look " + map.id() + " " + part.getPos().toString() + " 0,0,0" );
										lastLookDestination = null;
									}
								}

								SfxWRENCH.play(worldpos, 0.0, 1.0, 1.0, 0); 

								inventory.scrollTo( inventory.addItem( part ) );
								
								invLineTxt.changeText( Integer.toString( inventory.currentLine()+1 ) );

								if(lift) player.car.command("reset"); //RAXAT: this must work only in a lifting mode!
								else player.car.wakeUp();

								//kiszereles time:
								GameLogic.spendTime( GameLogic.mechTime( part, 0 ) );
							} 
						}
					}
					else
					{
						//RAXAT: v2.3.1, improved appearance for tuning dialog windows, read comments for more info
						if(part.isTuneable())
						{
							int flags=Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_DARKEN;

							ResourceRef charset = Frontend.smallFont_strong; //this font will be used to calculate spacing between tuning lines
							Style butt1 = new Style(0.55, 0.08, charset, Text.ALIGN_CENTER, Osd.RRT_TEST);
							Style sld1 = new Style(0.55, 0.03, charset, Text.ALIGN_CENTER, new ResourceRef(Osd.RID_SLD_BACK));
							Style sldk =  new Style(0.04, 0.0525, charset, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB));

							//first of all, we create temp osd where fake dialog and menu will be placed in
							float delta, vpH;
							float spacing = sld1.rt.height; //height of slider's holder rectangle, we use it as a vertical spacing metric unit
							Osd d_osd = new Osd();

							//now creating invisible fake dialog just to estimate its viewport height (vpH)
							Dialog d_dlg = new Dialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_TUNING|Dialog.DF_DARKEN, "TUNE PART", "OK;CANCEL");
							vpH = d_dlg.osd.vp.getHeight();
							d_dlg.osdCommand(0); //this kills the dialog (exit cmd)

							//next step is creating fake menu to count tuning lines in it
							Menu dm = d_osd.createMenu(butt1, 0, 0, 0, sld1, sldk);
							part.buildTuningMenu(dm);
							dm = d_osd.createMenu(butt1, 0, 0, 0, sld1, sldk);
							part.buildTuningMenu(dm); //we have to build it twice to avoid wierd display bugs in transmission tuning sliders (they may appear with inverted values)

							delta = spacing*(dm.elements-1); //this is total height of the menu we built
							d_osd.finalize(); //now kill fake osd
							d_osd=null;

							//if tuning menu is too tall, we build a big dialog, otherwise regular one, which is ~50% thinner, so we save useful screen space
							if(delta < vpH*0.4) flags |= Dialog.DF_TUNING;
							else flags |= Dialog.DF_WIDE;

							//building normal visible dialog with adjusted flags
							Integrator.frozen = true; //drag cursor patch for MODE_TUNE in the garage
							Dialog dlg = new Dialog(player.controller, flags, "TUNE PART", "OK;CANCEL");
							dlg.escapeCmd=999; //v2.3.1, fixing wrong action after pressing ESC key

							/*
							System.print("vpH: " + dlg.osd.vp.getHeight());
							System.print("40% vpH: " + dlg.osd.vp.getHeight()*0.6);
							System.print("delta: " + delta);
							*/

							dlg.osd.globalHandler = part;
							Menu m = dlg.osd.createMenu(butt1, 0.0, 0-delta, 0, sld1, sldk); //finally we adjust Y pos for menu inside our dialog to make it looking perfectly

							part.buildTuningMenu(m);
							part.enterAsyncMode_Script();
							dlg.osd.globalHandler = dlg;
							int choice = dlg.display();	//0-ok, 1-cancel
							part.leaveAsyncMode_Script();
							part.endTuningSession(choice);
							
							Integrator.frozen = false; //drag cursor patch for MODE_TUNE in the garage

						}
						else
						{
							if(!lift)
							{
								part.command("flap_toggle");
								player.car.wakeUp();
							}
							else new SfxRef(Frontend.SFX_WARNING).play();
						}

					}
				}
			}
			else
			if (ec == GameType.EC_HOVER)
			{
				GameRef dest = new GameRef(param.token( ++tok ).intValue());
				int phyId = param.token( ++tok ).intValue();
				//alkatresz azonositas inventory

				InventoryPanel panel;
				if( (panel=inventory.getPanelbyButtonPhyId( phyId ) ) )
				{	//inventorybol
					actualPanel=panel;
					actualPanelChanged = 1;
				}
				else
				{
					if( actualPanel )
					{
						actualPanel = null;
						actualPanelChanged = 1;
					}
				}

				//alkatresz azonositas kocsirol
				Object o = dest.getScriptInstance();
				if( o instanceof Part && GameLogic.player.car && GameLogic.player.car.chassis && o.getCarRef() && o.getCarRef().id() == GameLogic.player.car.chassis.getCarRef().id() ) //RAXAT: we check if it's a player's car, so other cars in the garage won't be affected
				{
					Part p = o;
					//ket helyen van!
					int	percentCondition=p.getConditionNoAttach()*100;
					String flags;
					if( p.isComplex() )
						flags = flags + "+";
					if( p.isTuneable() )
						flags = flags + "T";
					if( !p.isStreetLegal() )
						flags = flags + "!";

					if( flags )
						flags = " [" + flags + "]";

					infoline.changeText( p.name + flags + " (" + percentCondition + "%)" );
					overVehicle = 1;
				}
				else
				{
					//osszeutkozik az osd tooltip kijelzessel, atteve a leave-be!
					//2003juli05
					/*
					if( overVehicle )
					{
						infoline.changeText(null);

						overVehicle = 0;
					}
					*/ 
				}
			}
			else
			if (ec == GameType.EC_LEAVE)
			{
				GameRef part, dest = new GameRef(param.token( ++tok ).intValue());
				Object p = dest.getScriptInstance();

				if( p instanceof Part && overVehicle )
				{
					infoline.changeText(null);

					overVehicle = 0;
				}
			}
			else
			if (ec == GameType.EC_RDRAGBEGIN)
			{
				//enable camera control with mouse
				//player.controller.user_Add( Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);
				//player.controller.user_Add( Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);
				//disable cursor movement
				player.controller.user_Del( Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0 );
				player.controller.user_Del( Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1 );
				Input.cursor.cursor.queueEvent(  null, GameType.EVENT_COMMAND, "lock" );
			} 
			else
			if (ec == GameType.EC_RDRAGEND)
			{
				//disable camera control with mouse
				//player.controller.user_Del( Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1 );
				//player.controller.user_Del( Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0 );
				//enable cursor movement
				player.controller.user_Add( Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);
				player.controller.user_Add( Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);
				Input.cursor.cursor.queueEvent(  null, GameType.EVENT_COMMAND, "unlock" );
			} 
		}
	}

	public void refreshMoneyString()
	{
		if( moneyTxt )
		{
			new SfxRef( Frontend.SFX_MONEY ).play(); 
			moneyTxt.changeText( "$" + Integer.toString( player.getMoney() ) );
		}
	}

	public void osdCommand( int cmd )
	{
		if( cmd == CMD_ENGINE )
		{
			filterEngine=2-filterEngine;
			if( player.car )
			{
				player.car.command( "filter 1 " + filterEngine );
				player.car.wakeUp();
				player.car.command( "suspend" );
			}
			filterInventory( filterEngine, filterBody, filterRGear );
		}
		else
		if( cmd == CMD_BODY )
		{
			filterBody=2-filterBody;
			if( player.car )
			{
				player.car.command( "filter 2 " + filterBody );
				player.car.wakeUp();
				player.car.command( "suspend" );
			}
			filterInventory( filterEngine, filterBody, filterRGear );
		}
		else
		if( cmd == CMD_RUNNING_GEAR )
		{
			filterRGear=2-filterRGear;
			if( player.car )
			{
				player.car.command( "filter 3 " + filterRGear );
				player.car.wakeUp();
				player.car.command( "suspend" );
			}
			filterInventory( filterEngine, filterBody, filterRGear );
		}
		else
		if( cmd == CMD_SCROLL_UP )
		{
			inventory.upScroll();
			invLineTxt.changeText( Integer.toString( inventory.currentLine()+1 ) );
		}
		else
		if( cmd == CMD_SCROLL_DOWN )
		{
			inventory.downScroll();
			invLineTxt.changeText( Integer.toString( inventory.currentLine()+1 ) );
		}
		else
		if( cmd == CMD_REPAIR )
		{
			if( osd.dropObject || osd.dropGadget )
			{
				Part part;

				//inventorybol huztak?
				if( osd.dropGadget )
				{
					InventoryItem item;
					if( item=inventory.getItembyButton( osd.dropGadget ) )
					{
						if(item.type == InventoryItem.IIT_PART) part = item.getPart();
						if(item.type == InventoryItem.IIT_SET) //RAXAT: v2.3.1, more advanced repair patch for Set
						{
							new WarningDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR PART", "Kits can't be repaired.").display();
						} 
					}
				}
				else
				{	//garazsbol huztak, csakk akkor engedjuk, ha az ures kaszni az
					part = osd.dropObject.getScriptInstance();
					if( part )
					{
						if( part instanceof Chassis )
						{
							int reason = part.getInfo(GII_GETOUT_OK);
							if( reason != 0 )
							{
								String beginsWith;
								if( reason != -1 )
								{
									GameRef attach = new GameRef( reason );
									Part otherpart=attach.getScriptInstance();
									beginsWith = "A " + otherpart.name + " is";
								}
								else
									beginsWith = "Some parts are";
								
								new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR CHASSIS", beginsWith + " attached to the chassis. \n All parts have to be disassembled from the chassis before it is going to be repaired." ).display();
								part=null;
							}
						}
						else
						{
							part=null;
						}
					}
				}

				if( part )
				{
					if( part.id() )
					{
						if( part.getCondition() < 1.0 )
						{
							if( part.isRepairable() )
							{
								float fcost = part.repairCost();
								if( fcost > 0 )
								{
									int cost = fcost;
									if( !cost )
										cost = 1;

									if( flags&MF_FREE_REPAIRS && GameLogic.gameMode != GameLogic.GM_MULTIPLAYER)
										cost=0;

									if( cost <= player.getMoney() )
									{
										Dialog d;
										if( cost )
											d = new RepairDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, cost, part );
										else
											d = new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFO", part.name + " has been repaired for free." );

										if( d.display() == 0 )
										{
											if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.spendMoney(cost);
											player.takeMoney(cost);
											part.repair();

											refreshMoneyString();
											GameLogic.spendTime( cost*10 );
										}
									}
									else
									{
										int money_need = cost-player.getMoney();
										new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR PART", "You need $" + money_need + " more to do this!" ).display();
									}
								}
								else
								{
									new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR PART", "This part is already repaired as far as possible!" ).display();
								}
							}
							else
							{
								new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR PART", "This part is so damaged it cannot be repaired." ).display();
							}
						}
						else
						{
							new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR PART", "This part's in perfect condition!" ).display();
						}
					}
				}
			}
			else
			{
				new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFO", "Drop parts here from the Parts Bin to repair them! \n \n You can also drop the chassis here directly from the garage." ).display();
			}
		}
		else
		if( cmd == CMD_SELL )
		{
			if( osd.dropGadget )
			{
				InventoryItem item;
				if( item=inventory.getItembyButton( osd.dropGadget ) )
				{
					int price = item.getPrice();

					Dialog d = new ScrapDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, item, price );
					if( d.display() == 0 )
					{
						if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.spendMoney(-price);
						player.addMoney(price);
						refreshMoneyString();

 						//remove part from inventory;
						inventory.removeItem( item );
						updateLineIndex();
					}
				}
			}
			else				//click
			{
				if(inventory.size()) //RAXAT: if there are some parts in the inventory
				{
					InventoryItem t;
					int price;
					for(int i=0; i<inventory.size(); i++) //RAXAT: begin collecting price
					{
						t = inventory.items.elementAt(i);
						price += t.getPrice();
					}

					Dialog saDlg = new SellAllDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, price, inventory.size());
					if(!saDlg.display())
					{
						price = 0;
						for(int k=0; k<inventory.size(); k++)
						{
							t = inventory.items.elementAt(k);
							price += t.getPrice();
							inventory.items.removeElementAt(k);
							inventory.update();
							k--;
						}

						updateLineIndex();
						player.addMoney(price);
						refreshMoneyString();
					}
				}
				else new WarningDialog(player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFO", "Drop parts here from the Parts Bin to sell them. \n \n To sell entire car go to the Used Car Dealer. \n \n If you want to sell all parts from Parts Bin press this button when you will have at least 1 part there.").display();
			}
		}
		else
		if( cmd == CMD_REPAIR_ALL )
		{
			if( player.car )
			{
				Part part = player.car.chassis;

				if( part.getCondition() < 1.0 )
				{
					if( part.isRepairable() )
					{
						int fcost = part.repairCost();
						if( fcost > 0 )
						{
							int cost = fcost;
							
							if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
							{
								String[] params  = new String[1];
								params[0] = fcost;
								cost = multiplayer.RPC("getRepairCost", params, true).intValue();
							}
							
							if( !cost )
								cost = 1;

							if( flags&MF_FREE_REPAIRS )
								cost=0;

							if( cost <= player.getMoney() )
							{
								Dialog d;
								if( cost )
									d = new RepairAllDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, cost );
								else
									d = new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFO", "Your car has been repaired for free." );
								
								if( d.display() == 0 )
								{
									player.takeMoney(cost);
									player.car.repair(); //RAXAT: BUG!! this will also repair totally damaged parts! fix that!

									refreshMoneyString();
									GameLogic.spendTime( cost*10 );
								}
							}
							else
							{
								new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR PART", "You need $" + (cost-player.getMoney()) + " more to do this!" ).display();
							}
						}
						else
						{
							new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR CAR", "This car is already repaired as far as possible!" ).display();
						}
					}
					else
					{
						new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR CAR", "This car is so damaged it can't be repaired!" ).display();
					}
				}
				else
				{
					new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "REPAIR CAR", "This car's in perfect condition!" ).display();
				}
			}
		}
	}

	public void flushInventory()
	{
		int i=inventory.size();
		while( i-- )
		{
			inventory.moveToInventory( 0, player.parts );
		}
	}


	public void filterInventory( int filterEngine, int filterBody, int filterRGear )
	{
		//atvesszuk a parts binbe azokat, amelyek megfelelnek a szuroknek
		int	carID;
		if( player.car )
			carID = player.car.getInfo( GII_TYPE ) >> 16;

		InventoryItem t;
		Part part;
		for( int i=0; i<player.parts.size(); i++)	//valtozhat a meret!
		{
			t = player.parts.items.elementAt(i);
			if( t instanceof InventoryItem_Part )
			{
				part = t.getPart();	//category innen jon
				//car tipus innen jon, hogy common-e
				if( part.carCategory==Part.COMMON || (part.getInfo( GII_TYPE )>> 16)==carID )
				{
					int cat = part.getInfo( GII_PART_CATEGORY );
					if( cat==1 )
					{
						if( !filterEngine )
							player.parts.moveToInventory( i--, inventory );
					}
					else
					if( cat==2 )
					{
						if( !filterBody )
							player.parts.moveToInventory( i--, inventory );
					}
					else 
					if( cat==3 )
					{
						if( !filterRGear )
							player.parts.moveToInventory( i--, inventory );
					}
					if( cat ==0 )	//PATCH, ennek nem volt megadva kategoria!!!!
					{
						player.parts.moveToInventory( i--, inventory );
					}
				}
			}
			else
			{
				player.parts.moveToInventory( i--, inventory );
			}
		}

		//visszatesszuk azokat, amelyek ne felelnek meg
		for( int i=0; i<inventory.size(); i++)	//valtozhat a meret!
		{
			t = inventory.items.elementAt(i);
			if( t instanceof InventoryItem_Part )
			{
				part = t.getPart();	//category innen jon
				//car tipus innen jon, hogy common-e
				if( part.carCategory==Part.COMMON || (part.getInfo( GII_TYPE )>> 16)==carID )
				{
					int cat = part.getInfo( GII_PART_CATEGORY );
					if( cat==1 )
					{
						if( filterEngine )
							inventory.moveToInventory( i--, player.parts );
					}
					else
					if( cat==2 )
					{
						if( filterBody )
							inventory.moveToInventory( i--, player.parts );
					}
					else 
					if( cat==3 )
					{
						if( filterRGear )
							inventory.moveToInventory( i--, player.parts );
					}
				}
				else
					inventory.moveToInventory( i--, player.parts );
			}
		}
		
		inventory.update();
		updateLineIndex();
	}
}

public class ScrapDialog extends YesNoDialog
{
	public ScrapDialog( Controller ctrl, int myflags, InventoryItem item, int price )
	{ 
		super( ctrl, myflags, "SELL PART", "Do you want to sell this " + item.getName() + " for $" + price + " ?"); 
	}
}

public class RepairDialog extends NoYesDialog
{
	public RepairDialog( Controller ctrl, int myflags, int cost, Part p )
	{ 
		super( ctrl, myflags, "REPAIR PART", "It will cost $" + cost + " to repair this " + p.name + ". Do you want to repair it?" ); 
	}
}

public class SellAllDialog extends NoYesDialog
{
	public SellAllDialog( Controller ctrl, int myflags, int cost, int quantity )
	{
		super( ctrl, myflags, "SELL PARTS", "You are going to sell " + quantity + " parts " + " for " + cost + "$ \n Do you want to do this?" ); 
	}
}

public class RepairAllDialog extends NoYesDialog
{
	public RepairAllDialog( Controller ctrl, int myflags, int cost )
	{ 
		super( ctrl, myflags, "REPAIR CAR", "It will cost $" + cost + " to repair this car. Do you want to repair it?" ); 
	}
}
