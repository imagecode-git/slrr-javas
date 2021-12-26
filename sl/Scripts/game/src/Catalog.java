package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.game.parts.bodypart.*;

import java.game.parts.*;

//RAXAT: v2.3.1, restyling, various bugfixes
public class Catalog extends GameType implements GameState
{
	final static ResourceRef RR_ICON_CAR_COMP = new ResourceRef( misc.catalog:0x0004r );
	final static ResourceRef RR_ICON_CAR_1STEP = new ResourceRef( misc.catalog:0x0005r );
	final static ResourceRef RR_ICON_INV_COMP = new ResourceRef( misc.catalog:0x0007r );

	final static int DECALPRICE = 20;

	final static int SC_ENGINE	= 0;
	final static int SC_BODY	= 1;
	final static int SC_RGEAR	= 2;
	final static int SC_NONE	= 3;	//max+1

	final static int CMD_EXIT  = 0;
	final static int CMD_FRONTPAGE = 1;
	final static int CMD_MAIN1 = 2;
	final static int CMD_MAIN2 = 3;
	final static int CMD_MAIN3 = 4;
	final static int CMD_MAIN4 = 5;
	final static int CMD_MAIN5 = 6;
	final static int CMD_MAIN6 = 7;

	final static int CMD_MAIN1_1 = 8;
	final static int CMD_MAIN1_2 = 9;
	final static int CMD_MAIN1_3 =10;
	final static int CMD_MAIN1_4 =11;
	final static int CMD_MAIN1_5 =12;
	final static int CMD_MAIN1_6 =13;
	final static int CMD_MAIN1_7 =14;
	final static int CMD_MAIN1_8 =15;

	final static int CMD_MAIN2_1 =16;
	final static int CMD_MAIN2_2 =17;
	final static int CMD_MAIN2_3 =18;
	final static int CMD_MAIN2_4 =19;
	final static int CMD_MAIN2_5 =20;
	final static int CMD_MAIN2_6 =21;
	final static int CMD_MAIN2_7 =22;
	final static int CMD_MAIN2_8 =23;
	final static int CMD_MAIN4_5 =24;

	final static int CMD_MAIN3_1 =25;
	final static int CMD_MAIN3_2 =26;
	final static int CMD_MAIN3_3 =27;
	final static int CMD_MAIN3_4 =28;
	final static int CMD_MAIN3_5 =29;
	final static int CMD_MAIN3_6 =30;
	final static int CMD_MAIN3_7 =31;

	final static int CMD_MAIN4_1 =32;
	final static int CMD_MAIN4_2 =33;
	final static int CMD_MAIN4_3 =34;
	final static int CMD_MAIN4_4 =35;
	final static int CMD_MAIN4_6 =36;
	final static int CMD_MAIN4_7 =37;
	final static int CMD_MAIN4_8 =38;

	final static int CMD_MAIN5_1 =39;
	final static int CMD_MAIN5_2 =40;
	final static int CMD_MAIN5_3 =41;
	final static int CMD_MAIN5_4 =42;
	final static int CMD_MAIN5_5 =43;
	final static int CMD_MAIN5_6 =44;

	final static int CMD_MAIN6_1 =45;
	final static int CMD_MAIN6_2 =46;
	final static int CMD_MAIN6_3 =47;
	final static int CMD_MAIN6_4 =48;
	final static int CMD_MAIN6_5 =49;
	final static int CMD_MAIN6_6 =50;
	final static int CMD_MAIN6_7 =51;

	final static int CMD_MAIN7_1 =52;
	final static int CMD_MAIN7_2 =53;
	final static int CMD_MAIN7_3 =54;
	final static int CMD_MAIN7_4 =55;
	final static int CMD_MAIN7_5 =56;
	final static int CMD_MAIN7_6 =57;
	final static int CMD_MAIN7_7 =58;


	final static int CMD_DB0 =59;
	final static int CMD_DB1 =60;
	final static int CMD_DB2 =61;
	final static int CMD_DB3 =62;
	final static int CMD_DB4 =63;
	final static int CMD_DB5 =64;
	final static int CMD_DB6 =65;
	final static int CMD_DB7 =66;

	final static int CMD_PREVDECALPAGE =67;
	final static int CMD_NEXTDECALPAGE =68;

	final static int CMD_PREVPARTPAGE =69;
	final static int CMD_NEXTPARTPAGE =70;

	final static int CMD_MAIN1_1_1 =71; // inline4 commands
	final static int CMD_MAIN1_1_2 =72;
	final static int CMD_MAIN1_1_3 =73;
	final static int CMD_MAIN1_1_4 =74;
	final static int CMD_MAIN1_1_5 =75;
	final static int CMD_MAIN1_1_6 =76;
	final static int CMD_MAIN1_1_7 =77;
	final static int CMD_MAIN1_1_8 =78;
	final static int CMD_MAIN1_1_9 =79;
	final static int CMD_MAIN1_1_10 =80;
	final static int CMD_MAIN1_1_11 =81;
	final static int CMD_MAIN1_1_12 =82;
	final static int CMD_MAIN1_1_13 =83;

	final static int CMD_MAIN1_2_1 =84; // inline6 commands
	final static int CMD_MAIN1_2_2 =85;
	final static int CMD_MAIN1_2_3 =86;
	final static int CMD_MAIN1_2_4 =87;
	final static int CMD_MAIN1_2_5 =88;
	final static int CMD_MAIN1_2_6 =89;
	final static int CMD_MAIN1_2_7 =90;
	final static int CMD_MAIN1_2_8 =91;
	final static int CMD_MAIN1_2_9 =92;
	final static int CMD_MAIN1_2_10 =93;
	final static int CMD_MAIN1_2_11 =94;
	final static int CMD_MAIN1_2_12 =95;
	final static int CMD_MAIN1_2_13 =96;

	final static int CMD_MAIN1_3_1 =97; // v6 commands
	final static int CMD_MAIN1_3_2 =98;
	final static int CMD_MAIN1_3_3 =99;
	final static int CMD_MAIN1_3_4 =100;
	final static int CMD_MAIN1_3_5 =101;
	final static int CMD_MAIN1_3_6 =102;
	final static int CMD_MAIN1_3_7 =103;
	final static int CMD_MAIN1_3_8 =104;
	final static int CMD_MAIN1_3_9 =105;
	final static int CMD_MAIN1_3_10 =106;
	final static int CMD_MAIN1_3_11 =107;
	final static int CMD_MAIN1_3_12 =108;
	final static int CMD_MAIN1_3_13 =109;

	final static int CMD_MAIN1_4_1 =110; // v8 commands
	final static int CMD_MAIN1_4_2 =111;
	final static int CMD_MAIN1_4_3 =112;
	final static int CMD_MAIN1_4_4 =113;
	final static int CMD_MAIN1_4_5 =114;
	final static int CMD_MAIN1_4_6 =115;
	final static int CMD_MAIN1_4_7 =116;
	final static int CMD_MAIN1_4_8 =117;
	final static int CMD_MAIN1_4_9 =118;
	final static int CMD_MAIN1_4_10 =119;
	final static int CMD_MAIN1_4_11 =120;
	final static int CMD_MAIN1_4_12 =121;
	final static int CMD_MAIN1_4_13 =122;

	final static int CMD_MAIN1_5_1 =123; // v10 commands
	final static int CMD_MAIN1_5_2 =124;
	final static int CMD_MAIN1_5_3 =125;
	final static int CMD_MAIN1_5_4 =126;
	final static int CMD_MAIN1_5_5 =127;
	final static int CMD_MAIN1_5_6 =128;
	final static int CMD_MAIN1_5_7 =129;
	final static int CMD_MAIN1_5_8 =130;
	final static int CMD_MAIN1_5_9 =131;
	final static int CMD_MAIN1_5_10 =132;
	final static int CMD_MAIN1_5_11 =133;
	final static int CMD_MAIN1_5_12 =134;
	final static int CMD_MAIN1_5_13 =135;

	final static int CMD_MAIN1_6_1 =136; // v12 commands
	final static int CMD_MAIN1_6_2 =137;
	final static int CMD_MAIN1_6_3 =138;
	final static int CMD_MAIN1_6_4 =139;
	final static int CMD_MAIN1_6_5 =140;
	final static int CMD_MAIN1_6_6 =141;
	final static int CMD_MAIN1_6_7 =142;
	final static int CMD_MAIN1_6_8 =143;
	final static int CMD_MAIN1_6_9 =144;
	final static int CMD_MAIN1_6_10 =145;
	final static int CMD_MAIN1_6_11 =146;
	final static int CMD_MAIN1_6_12 =147;
	final static int CMD_MAIN1_6_13 =148;

	final static int CMD_MAIN1_7_1 =149; // v16 commands
	final static int CMD_MAIN1_7_2 =150;
	final static int CMD_MAIN1_7_3 =151;
	final static int CMD_MAIN1_7_4 =152;
	final static int CMD_MAIN1_7_5 =153;
	final static int CMD_MAIN1_7_6 =154;
	final static int CMD_MAIN1_7_7 =155;
	final static int CMD_MAIN1_7_8 =156;
	final static int CMD_MAIN1_7_9 =157;
	final static int CMD_MAIN1_7_10 =158;
	final static int CMD_MAIN1_7_11 =159;
	final static int CMD_MAIN1_7_12 =160;
	final static int CMD_MAIN1_7_13 =161;

	final static int CMD_INFO = 200;


	final static String pageNumberPrefix;

	final static float PRICERATIO = 1.1;	//katalogus ar-szorzo

	Multiplayer		multiplayer;
	Player			player;

	Osd			osd;

	int			actGroup;

	Text			moneytxt, pgNumberL, pgNumberR, sectionstxt;
	Text[]			pageNumberL = new Text[14];
	Text[]			pageNumberR = new Text[14];

	CatalogInventory	inventory;
	Vector			collector;

	int			curpage=1;

	int			currentCarPackId, partsPackId;

	Decal[]			curDecals;
	Button[]		decalButtons;
	Vector			decals;
	int			showDecals;

	int			mainGroup, main1Group, main2Group, main3Group, main4Group, main5Group, main6Group, main7Group, main8Group, main9Group, main10Group, main11Group, main12Group, main13Group;
	int			decalsGroup, parts1Group, parts2Group, parts3Group, parts4Group, parts5Group, parts6Group, parts7Group, parts8Group, parts9Group, parts10Group, parts11Group, parts12Group;

	ResourceRef[]		carTypes;

	Vehicle			carexVhc;

	public Catalog(Multiplayer mp)
	{
		multiplayer = mp;
		createNativeInstance();

		player = GameLogic.player;

		carTypes = GameLogic.VEHICLETYPE_ROOT.getChildNodes();

		GfxEngine.setGlobalEnvmap(new ResourceRef(misc.catalog:0x0000001Ar)); //RAXAT: v2.3.1, envmap patch, each time player enter the catalog, he will see the same reflection on all parts
	}

	public static Vector collectDecals( String subdir )
	{
		Vector ds = new Vector();

		FindFile ff = new FindFile();
		String name=ff.first( "decals/textures/catalog/" + subdir + "/*.png" );
		while( name )
		{
			Decal decal = new Decal( "decals/textures/catalog/" +subdir + "/" + name );	//extends ResourceRef
			ds.addElement( decal );
			name = ff.next();
		}
		ff.close();

		return ds;
	}

	public void clearObjectCache()
	{
		if( inventory )
		{
			inventory.hide();
			inventory.flushAll();
			inventory = null;
		}

		collector = null;
	}

	//puts parts of the given category and car type (+common parts) to the given inventory
	public void collectObjectsBegin()
	{
		clearObjectCache();

		inventory = new CatalogInventory(this, player, 0.02, 0.25, 0.96, 0.50, multiplayer);
		collector = new Vector();

		if( player.car )
			currentCarPackId = player.car.getInfo( GII_TYPE ) >> 16;
		else
			currentCarPackId = 0;

		partsPackId = System.openLib( "parts.rpk" );
	}

	public void collectObjectsEnd()
	{
		collectObjectsStep(9);

		inventory.update();
		inventory.show();
	}
	
	public void collectObjects( GameRef root )
	{
		Vector stack = collector;

		if( root )
		{
			stack.addElement( root );
		}
	}

	//return false if finished collecting
	public int collectObjectsStep( int step )
	{
		Inventory destination = inventory;
		Vector stack = collector;

		GameRef parts;

		if( stack )
		{
			while( !stack.isEmpty() && step )
			{
				parts=stack.removeLastElement();

				if( parts.isScripted( "java.game.parts.Part" ) )	//alkatresz?
				{
					int testVhcBuy = 0;
					InventoryItem_Part ip = new InventoryItem_Part(destination, parts.id());

					if(testVhcBuy)
					{
						//RAXAT: vehicles instead of chassis parts in car exchange
						VehicleDescriptor vd = GameLogic.getVehicleDescriptor(VehicleType.VS_STOCK);
						Vehicle vhc =  new Vehicle(null, ip.getPart(), 2, vd.optical, vd.power, vd.wear, vd.tear);
						destination.items.addElement(new InventoryItem_Vehicle(destination, vhc));
						step--;
					}
					else
					{
						//RAXAT: chassis will never be hidden in chassis catalog
						int ban = 0;
						if(ip.getPart().banned && !(ip.getPart() instanceof Chassis)) ban++; //RAXAT: part loaded via getPart() will not throw a "wrong gametype" error
						if(!ban)
						{
							destination.items.addElement(new InventoryItem_Part(destination, parts.id()));	//az update hivast megsporoljuk :)
							step--;
						}
					}
				}
				else
				if( parts.isScripted( "java.game.parts.Set" ) )	//set?
				{
					Set set = parts.create( null, parts, null, "set_loader" );

					InventoryItem_Folder tmp = new InventoryItem_Folder( destination );
					tmp.set = set;
					set.build( tmp.inv );
					destination.items.addElement( tmp );
				}
				else
				{	//dummy eloszto node!
					
					//vegigszkenneljuk a tartalmat:
					//a lonely partok, parts dummy alattiak es aktualis kocsi alattiak menek tovabi feldolgozasra
					//a mas kocsik dummyjai alattiak nem jelennek meg!
					if( parts=parts.getFirstChild() )
					{
						while( parts )
						{
							if( parts.isScripted() )
								stack.addElement( parts );
							else
							{
								int	carPack;
								int packId = parts.id() >> 16;
	
								/*old
								if( packId == partsPackId || packId == currentCarPackId )
									stack.addElement( parts );
								*/

								//new
								for( int i=carTypes.length-1; i>=0; i-- )
								{
									if( packId == carTypes[i].id()>>16 )
									{
										carPack=1;
										break;
									}
								}
								
								if( !carPack || packId == currentCarPackId )
									stack.addElement( parts );
								//
							}

							parts=parts.getNextChild();
						}
					}
				}
			}

			destination.update();

			return !stack.isEmpty();
		}

		return 0;
	}


	public void setDecalButtons()
	{
		int	max = decals.size();

		clearDecalButtons();

		for( int i=0; i<decalButtons.length; i++ )
		{
			//a gomb tipus (nagy)
			RenderRef	base = new RenderRef( frontend:0x00AEr );
	
			if( max > showDecals+i )
			{
				curDecals[i] = decals.elementAt( showDecals+i );
				decalButtons[i].rect.changeTexture( decals.elementAt( showDecals+i ) );
			}
		}
	}

	public void clearDecalButtons()
	{
		for( int i=0; i<decalButtons.length; i++ )
		{
			decalButtons[i].rect.changeTexture( Osd.RRT_EMPTY );
			if( curDecals[i] )
			{
				curDecals[i].unload();
				curDecals[i] = null;
			}
		}
	}

	public void enter( GameState prev_state )
	{
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) enableAnimateHook();
	
		osd = new Osd();
		osd.globalHandler = this;

		showDecals=0;

		setEventMask( EVENT_TIME );

		createOSDObjects();
		osd.show();

		Input.cursor.enable(1);
		setEventMask( EVENT_CURSOR );
		
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
		{
			multiplayer.osd = osd;
			multiplayer.player = player;
			multiplayer.RPC("inCatalog", null);
		}
	}
	
    public void animate()
	{
		multiplayer.runRPCScript();
	}

	public void exit( GameState next_state )
	{
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) disableAnimateHook();
		
		Frontend.loadingScreen.hide();

		clearEventMask( EVENT_ANY );
		removeAllTimers();
		Input.cursor.enable(0);

		pageNumberPrefix=null;

		osd.hide();
	}

	public void createOSDObjects()
	{
		Style bsm = new Style( 0.70, 0.13, Frontend.largeFont_strong, Text.ALIGN_RIGHT, Osd.RRT_TEST );
		Style bs = new Style( 0.70, 0.13, Frontend.largeFont_strong, Text.ALIGN_RIGHT, Osd.RRT_TEST );
		Style bs2 = new Style( 0.70, 0.13, Frontend.largeFont_strong, Text.ALIGN_LEFT, Osd.RRT_TEST );
		
		Style bbsr = new Style( 0.15, 0.15, 1.0, Frontend.largeFont_strong, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_ARROWRG) );
		Style bbsl = new Style( 0.15, 0.15, 1.0, Frontend.largeFont_strong, Text.ALIGN_LEFT, new ResourceRef(Osd.RID_ARROWLF) );

		Style bex = new Style( 0.12, 0.12, 1.0, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef(frontend:0x9C0Er) );
		Style bek = new Style( 0.12, 0.12, 1.0, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef(frontend:0x9C02r) );

		Style info = new Style( 0.12, 0.12, 1.0, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef(frontend:0x9C11r) );

		Menu m, m1, m2;

		//always visible objects:
		moneytxt = osd.createText(null, Frontend.largeFont_strong, Text.ALIGN_CENTER,	0.50, -0.975);
		moneytxt.setColor(Palette.RGB_BLACK);
		sectionstxt = osd.createText(null, Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98); //RAXAT: v2.3.1, additional informer for sections
		sectionstxt.setColor(Palette.RGB_RED);
		osd.endGroup();

		//----------------------------------------Grand Index; main page, calls menus, see below

		osd.createBG( new ResourceRef(misc.catalog:0x0006r) );

		m = osd.createMenu( bsm, 1.0375, -0.55, 0.2 );

		//RAXAT: v2.3.1, special text buttons are used (with focus animation and adjustable color, but variety of supported colors is limited yet, see Text.class for more info)
		m.addItem( "ENGINE", CMD_MAIN1, Palette.RGB_BLACK );
		m.addItem( "BODY", CMD_MAIN2, Palette.RGB_BLACK );
		m.addItem( "RUNNING GEAR", CMD_MAIN3, Palette.RGB_BLACK );
		m.addItem( "INTERIOR", CMD_MAIN4, Palette.RGB_BLACK );
		m.addItem( "AUDIO", CMD_MAIN5, Palette.RGB_BLACK );
		m.addItem( "DECALS", CMD_MAIN6, Palette.RGB_BLACK );

		PRICERATIO = 1.1;
		
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null ); //"garage" icon

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_EXIT, this );
		actGroup = mainGroup = osd.endGroup();

		//----------------------------------------Engines Index

		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		m = osd.createMenu( bsm, 1.0375, -0.55, 0.2 );
		m.addItem( "INLINE 4", CMD_MAIN1_1, Palette.RGB_BLACK );
		m.addItem( "INLINE 6", CMD_MAIN1_2, Palette.RGB_BLACK );
		m.addItem( "V6 TYPE", CMD_MAIN1_3, Palette.RGB_BLACK );
		m.addItem( "V8 TYPE", CMD_MAIN1_4, Palette.RGB_BLACK );
		m.addItem( "V10 TYPE", CMD_MAIN1_5, Palette.RGB_BLACK );
		m.addItem( "V12 TYPE", CMD_MAIN1_6, Palette.RGB_BLACK );
		m.addItem( "V16 TYPE", CMD_MAIN1_7, Palette.RGB_BLACK );
		if(Player.c_catalog) m.addItem( "ENGINE KITS", CMD_MAIN1_8, Palette.RGB_BLACK );
		
		osd.createButton( bek, 0.08, -0.92, CMD_FRONTPAGE, null, null ); //"X" icon
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null ); //"garage" icon

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_FRONTPAGE, this );
		osd.hideGroup( main1Group = osd.endGroup() );

		//-----------------------------------------Inline4 Index
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		m1 = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m1.addItem( "EXHAUST SYSTEMS", CMD_MAIN1_1_8, Palette.RGB_BLACK );
		m1.addItem( "FUEL SYSTEMS", CMD_MAIN1_1_9, Palette.RGB_BLACK );
		m1.addItem( "PERFORMANCE SYSTEMS", CMD_MAIN1_1_10, Palette.RGB_BLACK );
		m1.addItem( "TRANSMISSIONS", CMD_MAIN1_1_11, Palette.RGB_BLACK );
		m1.addItem( "CLUTCH & FLYWHEELS", CMD_MAIN1_1_12, Palette.RGB_BLACK );
		m1.addItem( "MISC", CMD_MAIN1_1_13, Palette.RGB_BLACK );

		m2 = osd.createMenu( bs2, -1.0375, -0.55, 0.2 );
		m2.addItem( "ENGINE BLOCKS", CMD_MAIN1_1_1, Palette.RGB_BLACK );
		m2.addItem( "CRANK SHAFTS", CMD_MAIN1_1_2, Palette.RGB_BLACK );
		m2.addItem( "CONNECTING RODS", CMD_MAIN1_1_3, Palette.RGB_BLACK );
		m2.addItem( "PISTONS", CMD_MAIN1_1_4, Palette.RGB_BLACK );
		m2.addItem( "CYLINDER HEADS", CMD_MAIN1_1_5, Palette.RGB_BLACK );
		m2.addItem( "CAMSHAFTS", CMD_MAIN1_1_6, Palette.RGB_BLACK );
		m2.addItem( "INTAKE SYSTEMS", CMD_MAIN1_1_7, Palette.RGB_BLACK );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );
		osd.createText("INLINE 4", Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98).setColor(Palette.RGB_RED);

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1, this );
		osd.hideGroup( main7Group = osd.endGroup() );

		//-----------------------------------------Inline6 Index
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		m1 = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m1.addItem( "EXHAUST SYSTEMS", CMD_MAIN1_2_8, Palette.RGB_BLACK );
		m1.addItem( "FUEL SYSTEMS", CMD_MAIN1_2_9, Palette.RGB_BLACK );
		m1.addItem( "PERFORMANCE SYSTEMS", CMD_MAIN1_2_10, Palette.RGB_BLACK );
		m1.addItem( "TRANSMISSIONS", CMD_MAIN1_2_11, Palette.RGB_BLACK );
		m1.addItem( "CLUTCH & FLYWHEELS", CMD_MAIN1_2_12, Palette.RGB_BLACK );
		m1.addItem( "MISC", CMD_MAIN1_2_13, Palette.RGB_BLACK );

		m2 = osd.createMenu( bs2, -1.0375, -0.55, 0.2 );
		m2.addItem( "ENGINE BLOCKS", CMD_MAIN1_2_1, Palette.RGB_BLACK );
		m2.addItem( "CRANK SHAFTS", CMD_MAIN1_2_2, Palette.RGB_BLACK );
		m2.addItem( "CONNECTING RODS", CMD_MAIN1_2_3, Palette.RGB_BLACK );
		m2.addItem( "PISTONS", CMD_MAIN1_2_4, Palette.RGB_BLACK );
		m2.addItem( "CYLINDER HEADS", CMD_MAIN1_2_5, Palette.RGB_BLACK );
		m2.addItem( "CAMSHAFTS", CMD_MAIN1_2_6, Palette.RGB_BLACK );
		m2.addItem( "INTAKE SYSTEMS", CMD_MAIN1_2_7, Palette.RGB_BLACK );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );
		osd.createText("INLINE 6", Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98).setColor(Palette.RGB_RED);

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1, this );
		osd.hideGroup( main8Group = osd.endGroup() );

		//-----------------------------------------V6 Index
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		m1 = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m1.addItem( "EXHAUST SYSTEMS", CMD_MAIN1_3_8, Palette.RGB_BLACK );
		m1.addItem( "FUEL SYSTEMS", CMD_MAIN1_3_9, Palette.RGB_BLACK );
		m1.addItem( "PERFORMANCE SYSTEMS", CMD_MAIN1_3_10, Palette.RGB_BLACK );
		m1.addItem( "TRANSMISSIONS", CMD_MAIN1_3_11, Palette.RGB_BLACK );
		m1.addItem( "CLUTCH & FLYWHEELS", CMD_MAIN1_3_12, Palette.RGB_BLACK );
		m1.addItem( "MISC", CMD_MAIN1_3_13, Palette.RGB_BLACK );

		m2 = osd.createMenu( bs2, -1.0375, -0.55, 0.2 );
		m2.addItem( "ENGINE BLOCKS", CMD_MAIN1_3_1, Palette.RGB_BLACK );
		m2.addItem( "CRANK SHAFTS", CMD_MAIN1_3_2, Palette.RGB_BLACK );
		m2.addItem( "CONNECTING RODS", CMD_MAIN1_3_3, Palette.RGB_BLACK );
		m2.addItem( "PISTONS", CMD_MAIN1_3_4, Palette.RGB_BLACK );
		m2.addItem( "CYLINDER HEADS", CMD_MAIN1_3_5, Palette.RGB_BLACK );
		m2.addItem( "CAMSHAFTS", CMD_MAIN1_3_6, Palette.RGB_BLACK );
		m2.addItem( "INTAKE SYSTEMS", CMD_MAIN1_3_7, Palette.RGB_BLACK );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );
		osd.createText("V6" + " TYPE", Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98 ).setColor(Palette.RGB_RED);

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1, this );
		osd.hideGroup( main9Group = osd.endGroup() );

		//-----------------------------------------V8 Index
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		m1 = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m1.addItem( "EXHAUST SYSTEMS", CMD_MAIN1_4_8, Palette.RGB_BLACK );
		m1.addItem( "FUEL SYSTEMS", CMD_MAIN1_4_9, Palette.RGB_BLACK );
		m1.addItem( "PERFORMANCE SYSTEMS", CMD_MAIN1_4_10, Palette.RGB_BLACK );
		m1.addItem( "TRANSMISSIONS", CMD_MAIN1_4_11, Palette.RGB_BLACK );
		m1.addItem( "CLUTCH & FLYWHEELS", CMD_MAIN1_4_12, Palette.RGB_BLACK );
		m1.addItem( "MISC", CMD_MAIN1_4_13, Palette.RGB_BLACK );

		m2 = osd.createMenu( bs2, -1.0375, -0.55, 0.2 );
		m2.addItem( "ENGINE BLOCKS", CMD_MAIN1_4_1, Palette.RGB_BLACK );
		m2.addItem( "CRANK SHAFTS", CMD_MAIN1_4_2, Palette.RGB_BLACK );
		m2.addItem( "CONNECTING RODS", CMD_MAIN1_4_3, Palette.RGB_BLACK );
		m2.addItem( "PISTONS", CMD_MAIN1_4_4, Palette.RGB_BLACK );
		m2.addItem( "CYLINDER HEADS", CMD_MAIN1_4_5, Palette.RGB_BLACK );
		m2.addItem( "CAMSHAFTS", CMD_MAIN1_4_6, Palette.RGB_BLACK );
		m2.addItem( "INTAKE SYSTEMS", CMD_MAIN1_4_7, Palette.RGB_BLACK );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );
		osd.createText("V8" + " TYPE", Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98 ).setColor(Palette.RGB_RED);

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1, this );
		osd.hideGroup( main10Group = osd.endGroup() );

		//-----------------------------------------V10 Index
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		m1 = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m1.addItem( "EXHAUST SYSTEMS", CMD_MAIN1_5_8, Palette.RGB_BLACK );
		m1.addItem( "FUEL SYSTEMS", CMD_MAIN1_5_9, Palette.RGB_BLACK );
		m1.addItem( "PERFORMANCE SYSTEMS", CMD_MAIN1_5_10, Palette.RGB_BLACK );
		m1.addItem( "TRANSMISSIONS", CMD_MAIN1_5_11, Palette.RGB_BLACK );
		m1.addItem( "CLUTCH & FLYWHEELS", CMD_MAIN1_5_12, Palette.RGB_BLACK );
		m1.addItem( "MISC", CMD_MAIN1_5_13, Palette.RGB_BLACK );

		m2 = osd.createMenu( bs2, -1.0375, -0.55, 0.2 );
		m2.addItem( "ENGINE BLOCKS", CMD_MAIN1_5_1, Palette.RGB_BLACK );
		m2.addItem( "CRANK SHAFTS", CMD_MAIN1_5_2, Palette.RGB_BLACK );
		m2.addItem( "CONNECTING RODS", CMD_MAIN1_5_3, Palette.RGB_BLACK );
		m2.addItem( "PISTONS", CMD_MAIN1_5_4, Palette.RGB_BLACK );
		m2.addItem( "CYLINDER HEADS", CMD_MAIN1_5_5, Palette.RGB_BLACK );
		m2.addItem( "CAMSHAFTS", CMD_MAIN1_5_6, Palette.RGB_BLACK );
		m2.addItem( "INTAKE SYSTEMS", CMD_MAIN1_5_7, Palette.RGB_BLACK );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );
		osd.createText("V10" + " TYPE", Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98 ).setColor(Palette.RGB_RED);

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1, this );
		osd.hideGroup( main11Group = osd.endGroup() );

		//-----------------------------------------V12 Index
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		m1 = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m1.addItem( "EXHAUST SYSTEMS", CMD_MAIN1_6_8, Palette.RGB_BLACK );
		m1.addItem( "FUEL SYSTEMS", CMD_MAIN1_6_9, Palette.RGB_BLACK );
		m1.addItem( "PERFORMANCE SYSTEMS", CMD_MAIN1_6_10, Palette.RGB_BLACK );
		m1.addItem( "TRANSMISSIONS", CMD_MAIN1_6_11, Palette.RGB_BLACK );
		m1.addItem( "CLUTCH & FLYWHEELS", CMD_MAIN1_6_12, Palette.RGB_BLACK );
		m1.addItem( "MISC", CMD_MAIN1_6_13, Palette.RGB_BLACK );

		m2 = osd.createMenu( bs2, -1.0375, -0.55, 0.2 );
		m2.addItem( "ENGINE BLOCKS", CMD_MAIN1_6_1, Palette.RGB_BLACK );
		m2.addItem( "CRANK SHAFTS", CMD_MAIN1_6_2, Palette.RGB_BLACK );
		m2.addItem( "CONNECTING RODS", CMD_MAIN1_6_3, Palette.RGB_BLACK );
		m2.addItem( "PISTONS", CMD_MAIN1_6_4, Palette.RGB_BLACK );
		m2.addItem( "CYLINDER HEADS", CMD_MAIN1_6_5, Palette.RGB_BLACK );
		m2.addItem( "CAMSHAFTS", CMD_MAIN1_6_6, Palette.RGB_BLACK );
		m2.addItem( "INTAKE SYSTEMS", CMD_MAIN1_6_7, Palette.RGB_BLACK );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );
		osd.createText("V12" + " TYPE", Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98 ).setColor(Palette.RGB_RED);

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1, this );
		osd.hideGroup( main12Group = osd.endGroup() );

		//-----------------------------------------V16 Index
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		m1 = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m1.addItem( "EXHAUST SYSTEMS", CMD_MAIN1_7_8, Palette.RGB_BLACK );
		m1.addItem( "FUEL SYSTEMS", CMD_MAIN1_7_9, Palette.RGB_BLACK );
		m1.addItem( "PERFORMANCE SYSTEMS", CMD_MAIN1_7_10, Palette.RGB_BLACK );
		m1.addItem( "TRANSMISSIONS", CMD_MAIN1_7_11, Palette.RGB_BLACK );
		m1.addItem( "CLUTCH & FLYWHEELS", CMD_MAIN1_7_12, Palette.RGB_BLACK );
		m1.addItem( "MISC", CMD_MAIN1_7_13, Palette.RGB_BLACK );

		m2 = osd.createMenu( bs2, -1.0375, -0.55, 0.2 );
		m2.addItem( "ENGINE BLOCKS", CMD_MAIN1_7_1, Palette.RGB_BLACK );
		m2.addItem( "CRANK SHAFTS", CMD_MAIN1_7_2, Palette.RGB_BLACK );
		m2.addItem( "CONNECTING RODS", CMD_MAIN1_7_3, Palette.RGB_BLACK );
		m2.addItem( "PISTONS", CMD_MAIN1_7_4, Palette.RGB_BLACK );
		m2.addItem( "CYLINDER HEADS", CMD_MAIN1_7_5, Palette.RGB_BLACK );
		m2.addItem( "CAMSHAFTS", CMD_MAIN1_7_6, Palette.RGB_BLACK );
		m2.addItem( "INTAKE SYSTEMS", CMD_MAIN1_7_7, Palette.RGB_BLACK );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );
		osd.createText("V16" + " TYPE", Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98 ).setColor(Palette.RGB_RED);

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1, this );
		osd.hideGroup( main13Group = osd.endGroup() );

		//----------------------------------------Body Index

		osd.createBG( new ResourceRef(misc.catalog:0x0010r) );

		m = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m.addItem( "STOCK BODY PARTS", CMD_MAIN2_1, Palette.RGB_BLACK );
		m.addItem( "AFTERMARKET BODY PARTS", CMD_MAIN2_2, Palette.RGB_BLACK );
		m.addItem( "CHASSIS", CMD_MAIN2_8, Palette.RGB_BLACK );
		m.addItem( "LIGHTS & WINDOWS", CMD_MAIN2_3, Palette.RGB_BLACK );
//		m.addItem( "AFTERMARKET LIGHTS & WINDOWS", CMD_MAIN2_4, Palette.RGB_BLACK );
		m.addItem( "AERODYNAMICS", CMD_MAIN2_5, Palette.RGB_BLACK );
		m.addItem( "NEON LIGHTS", CMD_MAIN2_6, Palette.RGB_BLACK );
		m.addItem( "MUFFLERS", CMD_MAIN4_5, Palette.RGB_BLACK );
		if(Player.c_catalog) m.addItem( "BODY KITS", CMD_MAIN2_7, Palette.RGB_BLACK );
		
		osd.createButton( bek, 0.08, -0.92, CMD_FRONTPAGE, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_FRONTPAGE, this );
		osd.hideGroup( main2Group = osd.endGroup() );

		//----------------------------------------Running Gear Index

		osd.createBG( new ResourceRef(misc.catalog:0x0011r) );

		m = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m.addItem( "SUSPENSIONS", CMD_MAIN3_1, Palette.RGB_BLACK );
		m.addItem( "SHOCKS & SPRINGS & BARS", CMD_MAIN3_2, Palette.RGB_BLACK );
		m.addItem( "BRAKE SYSTEMS", CMD_MAIN3_3, Palette.RGB_BLACK );
		m.addItem( "STOCK RIMS", CMD_MAIN3_4, Palette.RGB_BLACK );
		m.addItem( "AFTERMARKET RIMS", CMD_MAIN3_5, Palette.RGB_BLACK );
		m.addItem( "TYRES", CMD_MAIN3_6, Palette.RGB_BLACK );
		if(Player.c_catalog) m.addItem( "RUNNING GEAR KITS", CMD_MAIN3_7, Palette.RGB_BLACK );
		
		osd.createButton( bek, 0.08, -0.92, CMD_FRONTPAGE, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_FRONTPAGE, this );
		osd.hideGroup( main3Group = osd.endGroup() );

		//----------------------------------------Interior Index

		osd.createBG( new ResourceRef(misc.catalog:0x001Er) );

		m = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m.addItem( "STEERING WHEELS", CMD_MAIN4_1, Palette.RGB_BLACK );
		m.addItem( "DASHBOARDS", CMD_MAIN4_2, Palette.RGB_BLACK );
		m.addItem( "GEAR KNOBS", CMD_MAIN4_3, Palette.RGB_BLACK );
		m.addItem( "PEDALS", CMD_MAIN4_4, Palette.RGB_BLACK );
//		m.addItem( "GAUGES", CMD_MAIN4_5, Palette.RGB_BLACK );
		m.addItem( "SEATS", CMD_MAIN4_6, Palette.RGB_BLACK );
		m.addItem( "EXTRAS", CMD_MAIN4_7, Palette.RGB_BLACK );
		m.addItem( "DECORATION", CMD_MAIN4_8, Palette.RGB_BLACK );
		
		osd.createButton( bek, 0.08, -0.92, CMD_FRONTPAGE, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_FRONTPAGE, this );
		osd.hideGroup( main4Group = osd.endGroup() );

		//----------------------------------------Audio Index

		osd.createBG( new ResourceRef(misc.catalog:0x001Fr) );

		m = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m.addItem( "HEAD UNITS", CMD_MAIN5_1, Palette.RGB_BLACK );
		m.addItem( "AMPLIFIERS", CMD_MAIN5_2, Palette.RGB_BLACK );
		m.addItem( "BOXES", CMD_MAIN5_3, Palette.RGB_BLACK );
		m.addItem( "SUBWOOFERS", CMD_MAIN5_4, Palette.RGB_BLACK );
		m.addItem( "ACCESSORIES", CMD_MAIN5_5, Palette.RGB_BLACK );
		if(Player.c_catalog) m.addItem( "AUDIO KITS", CMD_MAIN5_6, Palette.RGB_BLACK );
		
		osd.createButton( bek, 0.08, -0.92, CMD_FRONTPAGE, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_FRONTPAGE, this );
		osd.hideGroup( main5Group = osd.endGroup() );

		//----------------------------------------Decal Index

		osd.createBG( new ResourceRef(misc.catalog:0x0022r) );

		m = osd.createMenu( bs, 1.0375, -0.55, 0.2 );
		m.addItem( "MANUFACTURERS", CMD_MAIN6_1, Palette.RGB_BLACK );
		m.addItem( "LOGOS", CMD_MAIN6_2, Palette.RGB_BLACK );
		m.addItem( "NUMBERS", CMD_MAIN6_3, Palette.RGB_BLACK );
		m.addItem( "DIGITS", CMD_MAIN6_4, Palette.RGB_BLACK );
		m.addItem( "SMILIES", CMD_MAIN6_5, Palette.RGB_BLACK );
		m.addItem( "ANIMALS", CMD_MAIN6_6, Palette.RGB_BLACK );
		m.addItem( "MISC", CMD_MAIN6_7, Palette.RGB_BLACK );
		
		osd.createButton( bek, 0.08, -0.92, CMD_FRONTPAGE, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_FRONTPAGE, this );
		osd.hideGroup( main6Group = osd.endGroup() );

		//----------------------------------------Engine kits

		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		pageNumberL[0] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[0] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[0].changeColor(Palette.RGB_BLACK);
		pageNumberR[0].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );
		osd.createText("ENGINE KITS", Frontend.largeFont_strong, Text.ALIGN_LEFT, -0.98, -0.98 ).setColor(Palette.RGB_RED);

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1, this );
		osd.hideGroup( parts1Group = osd.endGroup() );

		//----------------------------------------Body parts
		osd.createBG( new ResourceRef(misc.catalog:0x0010r) );

		pageNumberL[1] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[1] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[1].changeColor(Palette.RGB_BLACK);
		pageNumberR[1].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN2, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN2, this );
		osd.hideGroup( parts2Group = osd.endGroup() );

		//----------------------------------------Running gear parts
		osd.createBG( new ResourceRef(misc.catalog:0x0011r) );

		pageNumberL[2] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[2] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[2].changeColor(Palette.RGB_BLACK);
		pageNumberR[2].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN3, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN3, this );
		osd.hideGroup( parts3Group = osd.endGroup() );

		//----------------------------------------Interior parts
		osd.createBG( new ResourceRef(misc.catalog:0x001Er) );

		pageNumberL[3] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[3] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[3].changeColor(Palette.RGB_BLACK);
		pageNumberR[3].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN4, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN4, this );
		osd.hideGroup( parts4Group = osd.endGroup() );

		//----------------------------------------Audio parts
		osd.createBG( new ResourceRef(misc.catalog:0x001Fr) );

		pageNumberL[4] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[4] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[4].changeColor(Palette.RGB_BLACK);
		pageNumberR[4].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN5, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN5, this );
		osd.hideGroup( parts5Group = osd.endGroup() );

		//----------------------------------------Decal subsections

		osd.createBG( new ResourceRef(misc.catalog:0x0022r) );

		Style dbs = new Style( 0.45, 0.45, Frontend.largeFont, Text.ALIGN_CENTER, Osd.RRT_TEST );

		curDecals = new Decal[8];
		decalButtons=new Button[8];
		decalButtons[0] = osd.createButton( dbs, -0.75, -0.35, "", CMD_DB0 );
		decalButtons[1] = osd.createButton( dbs, -0.25, -0.35, "", CMD_DB1 );
		decalButtons[2] = osd.createButton( dbs,  0.25, -0.35, "", CMD_DB2 );
		decalButtons[3] = osd.createButton( dbs,  0.75, -0.35, "", CMD_DB3 );
		decalButtons[4] = osd.createButton( dbs, -0.75,  0.40, "", CMD_DB4 );
		decalButtons[5] = osd.createButton( dbs, -0.25,  0.40, "", CMD_DB5 );
		decalButtons[6] = osd.createButton( dbs,  0.25,  0.40, "", CMD_DB6 );
		decalButtons[7] = osd.createButton( dbs,  0.75,  0.40, "", CMD_DB7 );

		pageNumberL[5] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[5] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[5].changeColor(Palette.RGB_BLACK);
		pageNumberR[5].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVDECALPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTDECALPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN6, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN6, this );
		osd.hideGroup( decalsGroup = osd.endGroup() );

		//----------------------------------------Inline4 parts
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		pageNumberL[6] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[6] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[6].changeColor(Palette.RGB_BLACK);
		pageNumberR[6].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1_1, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1_1, this );
		osd.hideGroup( parts6Group = osd.endGroup() );

		//----------------------------------------Inline6 parts
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		pageNumberL[7] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[7] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[7].changeColor(Palette.RGB_BLACK);
		pageNumberR[7].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1_2, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1_2, this );
		osd.hideGroup( parts7Group = osd.endGroup() );

		//----------------------------------------V6 parts
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		pageNumberL[8] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[8] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[8].changeColor(Palette.RGB_BLACK);
		pageNumberR[8].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1_3, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1_3, this );
		osd.hideGroup( parts8Group = osd.endGroup() );

		//----------------------------------------V8 parts
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		pageNumberL[9] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[9] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[9].changeColor(Palette.RGB_BLACK);
		pageNumberR[9].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1_4, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1_4, this );
		osd.hideGroup( parts9Group = osd.endGroup() );

		//----------------------------------------V10 parts
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		pageNumberL[10] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[10] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[10].changeColor(Palette.RGB_BLACK);
		pageNumberR[10].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1_5, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1_5, this );
		osd.hideGroup( parts10Group = osd.endGroup() );

		//----------------------------------------V12 parts
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		pageNumberL[11] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[11] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[11].changeColor(Palette.RGB_BLACK);
		pageNumberR[11].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1_6, null, null );
		osd.createButton( bex, 0.935, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1_6, this );
		osd.hideGroup( parts11Group = osd.endGroup() );

		//----------------------------------------V16 parts
		osd.createBG( new ResourceRef(misc.catalog:0x0009r) );

		pageNumberL[12] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_LEFT,	-0.92,  0.82);
		pageNumberR[12] = osd.createText( null, Frontend.largeFont_strong, Text.ALIGN_RIGHT,	0.92,  0.82);
		pageNumberL[12].changeColor(Palette.RGB_BLACK);
		pageNumberR[12].changeColor(Palette.RGB_BLACK);

		osd.createButton( bbsl, -0.9975, 0.875, CMD_PREVPARTPAGE, null, null );
		osd.createButton( bbsr, 0.995, 0.875, CMD_NEXTPARTPAGE, null, null );

		osd.createButton( bek, 0.08, -0.92, CMD_MAIN1_7, null, null );
		osd.createButton( bex, 0.92, -0.92, CMD_EXIT, null, null );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN1_7, this );
		osd.hideGroup( parts12Group = osd.endGroup() );

		refreshMoneyString();
		refreshPage();
	}	


	public void changeGroup(int group)
	{
		if( actGroup != group )
		{
			if (actGroup >= 0)
			{
				osd.hideGroup (actGroup);

				//group deinit code:
				if( actGroup == decalsGroup )
				{
					clearDecalButtons();
				}
				else
				if( actGroup >= parts1Group && actGroup <= parts12Group )
				{
					clearObjectCache();
				}
			}

			actGroup = group;

			if (actGroup >= 0)
			{
				osd.showGroup (actGroup);
				osd.changeSelection2( -1, 0 );
			}
		}
	}


	public void refreshMoneyString()
	{
		if(moneytxt)
		{
			String diff = "";
			if(moneytxt && moneytxt.text) diff = moneytxt.text.cut("$");
			if(Integer.toString(player.getMoney()) != diff)
			{
				if(moneytxt.text) new SfxRef(Frontend.SFX_MONEY).play();
				moneytxt.changeText("$" + Integer.toString(player.getMoney()));
			}
		}
	}

	public void refreshPage()
	{
		if( pageNumberPrefix )
		{
			pgNumberL.changeText( pageNumberPrefix + curpage );
			pgNumberR.changeText( pageNumberPrefix + (curpage+1) );
		}
	}

//----------------------------------------------------------------------

	public void decalButtonPressed( int n )
	{
		if( showDecals+n < decals.size() )
		{
			int price=DECALPRICE;
			if( price <= player.getMoney() )
			{
				Decal decal = decals.elementAt( showDecals+n );

				Dialog dialog = new YesNoDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "BUY DECAL SET", "Do you want to buy this decal set for $" + price + " ?\n(Each set contains 5 decals)" );
				if( dialog.display() == 0 )
				{
					player.takeMoney(price);
					player.decals.addElement( new Decal( decal.id() ) );
					refreshMoneyString();
				}
			}
			else
			{
				new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "NOT ENOUGH MONEY", "You don't have $" + price + " to buy a decal!" ).display();
			}
		}
	}

//----------------------------------------------------------------------
	public void osdCommand( int cmd )
	{
		if( cmd==CMD_EXIT ) //main index pages
		{
			changeGroup(-1);
			if(inventory) inventory.hide(); //RAXAT: v2.3.1, patch to destroy all unused inventory parts

			GameLogic.changeActiveSection(GameLogic.garage);
		}
		else
		if( cmd==CMD_INFO )
		{
		new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "INFO", "You are using Valo City Car Exchange service. \n You can purchase chassis of any car here. You must have your own car in good condition to take part in exchange. The rule is simple - you sell your old car to exchange dealer for its half price and then you get any car chassis from this catalogue for its triple price." ).display();
		}
		else
		if( cmd==CMD_FRONTPAGE ) //grand index
		{
			changeGroup(mainGroup);
			if(inventory) inventory.hide(); //RAXAT: v2.3.1, patch to destroy all unused inventory parts
		}
		else
		if( cmd==CMD_MAIN1 ) //engine index
		{
			pgNumberL=pageNumberL[0];
			pgNumberR=pageNumberR[0];
			sectionstxt.changeText(null);
			changeGroup( main1Group );
		}
		else
		if( cmd==CMD_MAIN2 ) //body index
		{
			pgNumberL=pageNumberL[1];
			pgNumberR=pageNumberR[1];
			sectionstxt.changeText(null);
			changeGroup( main2Group );
		}
		else
		if( cmd==CMD_MAIN3 ) //running gear index
		{
			pgNumberL=pageNumberL[2];
			pgNumberR=pageNumberR[2];
			sectionstxt.changeText(null);
			changeGroup( main3Group );
		}
		else
		if( cmd==CMD_MAIN4 ) //interior index
		{
			pgNumberL=pageNumberL[3];
			pgNumberR=pageNumberR[3];
			sectionstxt.changeText(null);
			changeGroup( main4Group );
		}
		else
		if( cmd==CMD_MAIN5 ) //audio index
		{
			pgNumberL=pageNumberL[4];
			pgNumberR=pageNumberR[4];
			sectionstxt.changeText(null);
			changeGroup( main5Group );
		}
		else
		if( cmd==CMD_MAIN6 ) //decals index
		{
			pgNumberL=pageNumberL[5];
			pgNumberR=pageNumberR[5];
			sectionstxt.changeText(null);
			changeGroup( main6Group );
		}
		else
		if( cmd==CMD_MAIN1_1 ) //i4 index
		{
			pgNumberL=pageNumberL[6];
			pgNumberR=pageNumberR[6];
			sectionstxt.changeText(null);
			changeGroup( main7Group );
		}
		else
		if( cmd==CMD_MAIN1_2 ) //i6 index
		{
			pgNumberL=pageNumberL[7];
			pgNumberR=pageNumberR[7];
			sectionstxt.changeText(null);
			changeGroup( main8Group );
		}
		else
		if( cmd==CMD_MAIN1_3 ) //V6 index
		{
			pgNumberL=pageNumberL[8];
			pgNumberR=pageNumberR[8];
			sectionstxt.changeText(null);
			changeGroup( main9Group );
		}
		else
		if( cmd==CMD_MAIN1_4 ) //V8 index
		{
			pgNumberL=pageNumberL[9];
			pgNumberR=pageNumberR[9];
			sectionstxt.changeText(null);
			changeGroup( main10Group );
		}
		else
		if( cmd==CMD_MAIN1_5 ) //V10 index
		{
			pgNumberL=pageNumberL[10];
			pgNumberR=pageNumberR[10];
			sectionstxt.changeText(null);
			changeGroup( main11Group );
		}
		if( cmd==CMD_MAIN1_6 ) //V12 index
		{
			pgNumberL=pageNumberL[11];
			pgNumberR=pageNumberR[11];
			sectionstxt.changeText(null);
			changeGroup( main12Group );
		}
		else
		if( cmd==CMD_MAIN1_7 ) //V16 index
		{
			pgNumberL=pageNumberL[12];
			pgNumberR=pageNumberR[12];
			sectionstxt.changeText(null);
			if(inventory) inventory.hide(); //RAXAT: v2.3.1, patch to destroy unused inventories
			changeGroup( main13Group );
		}
		else
		if( cmd>=CMD_DB0 && cmd<=CMD_DB7 ) //decal buttons
		{
			decalButtonPressed( cmd - CMD_DB0 );
		}
		else
		if( cmd>=CMD_MAIN6_1 && cmd<=CMD_MAIN6_7 ) //decal subsections
		{
			String decalsDir;
			if( cmd == CMD_MAIN6_1 )
			{
				pageNumberPrefix="D-I/";
				decalsDir = "manufacturers";
				sectionstxt.changeText("MANUFACTURERS");
			}
			else
			if( cmd == CMD_MAIN6_2 )
			{
				pageNumberPrefix="D-II/";
				decalsDir = "logos";
				sectionstxt.changeText("LOGOS");
			}
			else
			if( cmd == CMD_MAIN6_3 )
			{
				pageNumberPrefix="D-III/";
				decalsDir = "numbers";
				sectionstxt.changeText("NUMBERS");
			}
			else
			if( cmd == CMD_MAIN6_4 )
			{
				pageNumberPrefix="D-IV/";
				decalsDir = "digits";
				sectionstxt.changeText("DIGITS");
			}
			else
			if( cmd == CMD_MAIN6_5 )
			{
				pageNumberPrefix="D-V/";
				decalsDir = "smilies";
				sectionstxt.changeText("SMILIES");
			}
			else
			if( cmd == CMD_MAIN6_6 )
			{
				pageNumberPrefix="D-VI/";
				decalsDir = "animals";
				sectionstxt.changeText("ANIMALS");
			}
			else
			if( cmd == CMD_MAIN6_7 )
			{
				pageNumberPrefix="D-VII/";
				decalsDir = "misc";
				sectionstxt.changeText("MISC");
			}

			decals = collectDecals( decalsDir );

			curpage=1;
			showDecals=0;

			setDecalButtons();
			refreshPage();
			changeGroup( decalsGroup );
		}
		else
		if( cmd == CMD_PREVDECALPAGE )	//page control
		{
			if( showDecals >= decalButtons.length )
			{
				showDecals-=decalButtons.length;
				setDecalButtons();

				curpage-=2;
				refreshPage();
			}
		}
		else
		if( cmd == CMD_NEXTDECALPAGE )
		{
			int	max = decals.size();
			if( showDecals+decalButtons.length < max )
			{
				showDecals+=decalButtons.length;
				setDecalButtons();

				curpage+=2;
				refreshPage();
			}
		}
		else
		if( cmd == CMD_PREVPARTPAGE )
		{
			if( inventory.upScroll() )
			{
				curpage-=2;
				refreshPage();
			}
		}
		else
		if( cmd == CMD_NEXTPARTPAGE )
		{
			//biztositsuk, hogy a kov oldal is tele van (az init csak a legelsot tolti fel rendesen!)
			//ha az user cselez, atugrik masik sectionbe, aztan visszalapoz, megszivja!
			collectObjectsStep( 8 );

			if(inventory.downScroll())
			{
				curpage+=2;
				refreshPage();
			}
		}
		else
		if( cmd > CMD_MAIN1_1 && cmd <= CMD_MAIN1_7_13 )
		{
			if(cmd != CMD_MAIN1_1 && cmd != CMD_MAIN1_2 && cmd != CMD_MAIN1_3 && cmd != CMD_MAIN1_4 && cmd != CMD_MAIN1_5 && cmd != CMD_MAIN1_6) collectObjectsBegin(); //RAXAT: v2.3.1, HUGE gadget hotspot patch!

			if( cmd >= CMD_MAIN1_8 && cmd < CMD_MAIN2_1 ) //all engine part page
			{
				changeGroup( parts1Group );
				if( cmd == CMD_MAIN1_8 )
				{
					pageNumberPrefix="K-I/";
					collectObjects( new GameRef(parts:0xF23Cr) ); //engine kits
//					collectObjects( new GameRef(parts:0x000Ar) ); //RAXAT: to test exhaust flame fake parts
				}
			}
			else
			if( cmd >= CMD_MAIN2_1 && cmd < CMD_MAIN3_1 ) //body subs
			{
				changeGroup( parts2Group );
				if( cmd == CMD_MAIN2_1 )					
				{
					pageNumberPrefix="B-I/";
					sectionstxt.changeText("STOCK BODY PARTS");
					collectObjects( new GameRef(parts:0xF242r) );
				}
				else
				if( cmd == CMD_MAIN2_2 )					
				{
					pageNumberPrefix="B-II/";
					sectionstxt.changeText("AFTERMARKET BODY PARTS");
					collectObjects( new GameRef(parts:0xF24Fr) );
				}
				else
				if( cmd == CMD_MAIN2_7 ) //body kits
				{
					pageNumberPrefix="B-III/";
					sectionstxt.changeText("BODY KITS");
					collectObjects( new GameRef(parts:0xF23Br) );
				}
				else
				if( cmd == CMD_MAIN2_3 )					
				{
					pageNumberPrefix="B-IV/";
					sectionstxt.changeText("LIGHTS & WINDOWS");
					collectObjects( new GameRef(parts:0xF24Cr) );
				}
				else
				if( cmd == CMD_MAIN2_8 )
				{
					pageNumberPrefix="B-V/";
					sectionstxt.changeText("CHASSIS");
					collectObjects( new GameRef(cars:0x0101r) );
				}
				else
				if( cmd == CMD_MAIN2_5 )				
				{
					pageNumberPrefix="B-VI/";
					sectionstxt.changeText("AERODYNAMICS");
					collectObjects( new GameRef(parts:0xF233r) );
				}
				else
				if( cmd == CMD_MAIN2_6 )				
				{
					pageNumberPrefix="B-VII/";
					sectionstxt.changeText("NEON LIGHTS");
					collectObjects( new GameRef(parts:0xF228r) );
				}
				else
				if( cmd == CMD_MAIN4_5 )
				{
					pageNumberPrefix="I-V/";
					sectionstxt.changeText("MUFFLERS");
					collectObjects( new GameRef(parts:0x00C9r) );
				}
				else
				if( cmd == CMD_MAIN2_7 ) //body kits
				{
					pageNumberPrefix="B-VIII/";
					sectionstxt.changeText("BODY KITS");
					collectObjects( new GameRef(parts:0xF23Br) );
				}
			}
			if( cmd >= CMD_MAIN3_1 && cmd < CMD_MAIN4_1 )
			{
				changeGroup( parts3Group );
				if( cmd == CMD_MAIN3_1 ) //running gear subsections
				{
					pageNumberPrefix="R-I/";
					sectionstxt.changeText("SUSPENSIONS");
					collectObjects( new GameRef(parts:0xF229r) );
				}
				else
				if( cmd == CMD_MAIN3_2 )
				{
					pageNumberPrefix="R-II/";
					sectionstxt.changeText("SHOCKS & SPRINGS & BARS");
					collectObjects( new GameRef(parts:0xF22Br) );
				}
				else
				if( cmd == CMD_MAIN3_3 )
				{
					pageNumberPrefix="R-III/";
					sectionstxt.changeText("BRAKE SYSTEMS");
					collectObjects( new GameRef(parts:0xF22Dr) );
				}
				else
				if( cmd == CMD_MAIN3_4 )
				{
					pageNumberPrefix="R-IV/";
					sectionstxt.changeText("STOCK RIMS");
					collectObjects( new GameRef(parts:0xF235r) );
				}
				else
				if( cmd == CMD_MAIN3_5 )
				{
					pageNumberPrefix="R-V/";
					sectionstxt.changeText("AFTERMARKET RIMS");
					collectObjects( new GameRef(parts:0xF23Dr) );
				}
				else
				if( cmd == CMD_MAIN3_6 )
				{
					pageNumberPrefix="R-VI/";
					sectionstxt.changeText("TYRES");
					collectObjects( new GameRef(parts:0xF23Er) );
				}
				else
				if( cmd == CMD_MAIN3_7 )
				{
					pageNumberPrefix="R-VII/";
					sectionstxt.changeText("RUNNING GEAR KITS");
					collectObjects( new GameRef(parts:0xF249r) );
				}
			}
			else
			if( cmd >= CMD_MAIN4_1 && cmd < CMD_MAIN5_1 )
			{
				changeGroup( parts4Group );
				if( cmd == CMD_MAIN4_1 ) //interior subsection
				{
					pageNumberPrefix="I-I/";
					sectionstxt.changeText("STEERING WHEELS");
					collectObjects( new GameRef(parts:0xF243r) );
				}
				else
				if( cmd == CMD_MAIN4_2 )
				{
					pageNumberPrefix="I-II/";
					sectionstxt.changeText("DASHBOARDS");
					collectObjects( new GameRef(parts:0xF245r) );
				}
				else
				if( cmd == CMD_MAIN4_3 )
				{
					pageNumberPrefix="I-III/";
					sectionstxt.changeText("GEAR KNOBS");
					collectObjects( new GameRef(parts:0xF23fr) );
				}
				else
				if( cmd == CMD_MAIN4_4 )
				{
					pageNumberPrefix="I-IV/";
					sectionstxt.changeText("PEDALS");
					collectObjects( new GameRef(parts:0xF241r) );
				}
				else
				if( cmd == CMD_MAIN4_6 )
				{
					pageNumberPrefix="I-V/";
					sectionstxt.changeText("SEATS");
					collectObjects( new GameRef(parts:0xF246r) );
				}
				else
				if( cmd == CMD_MAIN4_7 )
				{
					pageNumberPrefix="I-VI/";
					sectionstxt.changeText("EXTRAS");
					collectObjects( new GameRef(parts:0xF247r) );
				}
				else
				if( cmd == CMD_MAIN4_8 )
				{
					pageNumberPrefix="I-VII/";
					sectionstxt.changeText("DECORATION");
					collectObjects( new GameRef(parts:0xF248r) );
				}
			}
			if( cmd >= CMD_MAIN5_1 && cmd < CMD_MAIN6_1 )
			{
				changeGroup( parts5Group );
				if( cmd == CMD_MAIN5_1 ) //audio subsection
				{
					pageNumberPrefix="A-I/";
					sectionstxt.changeText("HEAD UNITS");
					collectObjects( new GameRef(parts:0xF240r) );
				}
				else
				if( cmd == CMD_MAIN5_2 )
				{
					pageNumberPrefix="A-II/";
					sectionstxt.changeText("AMPLIFIERS");
					collectObjects( new GameRef(parts:0x0246r) );
				}
				else
				if( cmd == CMD_MAIN5_3 )
				{
					pageNumberPrefix="A-III/";
					sectionstxt.changeText("BOXES");
					collectObjects( new GameRef(parts:0x0247r) );
				}
				else
				if( cmd == CMD_MAIN5_4 )
				{
					pageNumberPrefix="A-IV/";
					sectionstxt.changeText("SUBWOOFERS");
					collectObjects( new GameRef(parts:0x0248r) );
				}
				else
				if( cmd == CMD_MAIN5_5 )
				{
					pageNumberPrefix="A-V/";
					sectionstxt.changeText("ACCESSORIES");
					collectObjects( new GameRef(parts:0x924Br) );
				}
				else
				if( cmd == CMD_MAIN5_6 )
				{
					pageNumberPrefix="A-VI/";
					sectionstxt.changeText("AUDIO KITS");
					collectObjects( new GameRef(parts:0x025Br) );
				}
			}
			if( cmd >= CMD_MAIN1_1_1 && cmd < CMD_MAIN1_2_1 ) //i4
			{
				changeGroup( parts6Group );
				if( cmd == CMD_MAIN1_1_1 )				
				{
					pageNumberPrefix="I4-I/";
					sectionstxt.changeText("ENGINE BLOCKS");
					collectObjects( new GameRef(parts:0xAB5Cr) );
				}
				else
				if( cmd == CMD_MAIN1_1_2 )				
				{
					pageNumberPrefix="I4-II/";
					sectionstxt.changeText("CRANK SHAFTS");
					collectObjects( new GameRef(parts:0xAA5Er) );
				}
				else
				if( cmd == CMD_MAIN1_1_3 )				
				{
					pageNumberPrefix="I4-III/";
					sectionstxt.changeText("CONNECTING RODS");
					collectObjects( new GameRef(parts:0xAB7Er) );
				}
				else
				if( cmd == CMD_MAIN1_1_4 )
				{
					pageNumberPrefix="I4-IV/";
					sectionstxt.changeText("PISTONS");
					collectObjects( new GameRef(parts:0x0AB6r) );
				}
				else
				if( cmd == CMD_MAIN1_1_5 )				
				{
					pageNumberPrefix="I4-V/";
					sectionstxt.changeText("CYLINDER HEADS");
					collectObjects( new GameRef(parts:0xA25Dr) );
				}
				else
				if( cmd == CMD_MAIN1_1_6 )				
				{
					pageNumberPrefix="I4-VI/";
					sectionstxt.changeText("CAMSHAFTS");
					collectObjects( new GameRef(parts:0xA25Fr) );
				}
				else
				if( cmd == CMD_MAIN1_1_7 )				
				{
					pageNumberPrefix="I4-VII/";
					sectionstxt.changeText("INTAKE SYSTEMS");
					collectObjects( new GameRef(parts:0xA26Fr) );
				}
				else
				if( cmd == CMD_MAIN1_1_8 )				
				{
					pageNumberPrefix="I4-VIII/";
					sectionstxt.changeText("EXHAUST SYSTEMS");
					collectObjects( new GameRef(parts:0xA282r) );
				}
				else
				if( cmd == CMD_MAIN1_1_9 )				
				{
					pageNumberPrefix="I4-IX/";
					sectionstxt.changeText("FUEL SYSTEMS");
					collectObjects( new GameRef(parts:0xA26Br) );
				}
				else
				if( cmd == CMD_MAIN1_1_10 )				
				{
					pageNumberPrefix="I4-X/";
					sectionstxt.changeText("PERFORMANCE SYSTEMS");
					collectObjects( new GameRef(parts:0x0AB4r) );
				}
				else
				if( cmd == CMD_MAIN1_1_11 )				
				{
					pageNumberPrefix="I4-XI/";
					sectionstxt.changeText("TRANSMISSIONS");
					collectObjects( new GameRef(parts:0xA24Br) );
				}
				else
				if( cmd == CMD_MAIN1_1_12 )				
				{
					pageNumberPrefix="I4-XII/";
					sectionstxt.changeText("CLUTCH & FLYWHEELS");
					collectObjects( new GameRef(parts:0xA23Cr) );
				}
				else
				if( cmd == CMD_MAIN1_1_13 )				
				{
					pageNumberPrefix="I4-XIII/";
					sectionstxt.changeText("MISC");
					collectObjects( new GameRef(parts:0x0ABEr) );
				}
			}
			if( cmd >= CMD_MAIN1_2_1 && cmd < CMD_MAIN1_3_1 ) //i6
			{
				changeGroup( parts7Group );

				if( cmd == CMD_MAIN1_2_1 )				
				{
					pageNumberPrefix="I6-I/";
					sectionstxt.changeText("ENGINE BLOCKS");
					collectObjects( new GameRef(parts:0xAA5Cr) );
				}
				else
				if( cmd == CMD_MAIN1_2_2 )				
				{
					pageNumberPrefix="I6-II/";
					sectionstxt.changeText("CRANK SHAFTS");
					collectObjects( new GameRef(parts:0xAB5Er) );
				}
				else
				if( cmd == CMD_MAIN1_2_3 )				
				{
					pageNumberPrefix="I6-III/";
					sectionstxt.changeText("CONNECTING RODS");
					collectObjects( new GameRef(parts:0xBB7Er) );
				}
				else
				if( cmd == CMD_MAIN1_2_4 )				
				{
					pageNumberPrefix="I6-IV/";
					sectionstxt.changeText("PISTONS");
					collectObjects( new GameRef(parts:0x0BB6r) );
				}
				else
				if( cmd == CMD_MAIN1_2_5 )				
				{
					pageNumberPrefix="I6-V/";
					sectionstxt.changeText("CYLINDER HEADS");
					collectObjects( new GameRef(parts:0xB25Dr) );
				}
				else
				if( cmd == CMD_MAIN1_2_6 )				
				{
					pageNumberPrefix="I6-VI/";
					sectionstxt.changeText("CAMSHAFTS");
					collectObjects( new GameRef(parts:0xB25Fr) );
				}
				else
				if( cmd == CMD_MAIN1_2_7 )				
				{
					pageNumberPrefix="I6-VII/";
					sectionstxt.changeText("INTAKE SYSTEMS");
					collectObjects( new GameRef(parts:0xB26Fr) );
				}
				else
				if( cmd == CMD_MAIN1_2_8 )				
				{
					pageNumberPrefix="I6-VIII/";
					sectionstxt.changeText("EXHAUST SYSTEMS");
					collectObjects( new GameRef(parts:0xB282r) );
				}
				else
				if( cmd == CMD_MAIN1_2_9 )				
				{
					pageNumberPrefix="I6-XI/";
					sectionstxt.changeText("FUEL SYSTEMS");
					collectObjects( new GameRef(parts:0xB26Br) );
				}
				else
				if( cmd == CMD_MAIN1_2_10 )				
				{
					pageNumberPrefix="I6-X/";
					sectionstxt.changeText("PERFORMANCE SYSTEMS");
					collectObjects( new GameRef(parts:0x0BB4r) );
				}
				else
				if( cmd == CMD_MAIN1_2_11 )				
				{
					pageNumberPrefix="I6-XI/";
					sectionstxt.changeText("TRANSMISSIONS");
					collectObjects( new GameRef(parts:0xB24Br) );
				}
				else
				if( cmd == CMD_MAIN1_2_12 )				
				{
					pageNumberPrefix="I6-XII/";
					sectionstxt.changeText("CLUTCH & FLYWHEELS");
					collectObjects( new GameRef(parts:0xB23Cr) );
				}
				else
				if( cmd == CMD_MAIN1_2_13 )				
				{
					pageNumberPrefix="I6-XIII/";
					sectionstxt.changeText("MISC");
					collectObjects( new GameRef(parts:0x0BBEr) );
				}
			}
			if( cmd >= CMD_MAIN1_3_1 && cmd < CMD_MAIN1_4_1 ) //V6
			{
				changeGroup( parts8Group );

				if( cmd == CMD_MAIN1_3_1 )				
				{
					pageNumberPrefix="V6-I/";
					sectionstxt.changeText("ENGINE BLOCKS");
					collectObjects( new GameRef(parts:0xAD5Cr) );
				}
				else
				if( cmd == CMD_MAIN1_3_2 )				
				{
					pageNumberPrefix="V6-II/";
					sectionstxt.changeText("CRANK SHAFTS");
					collectObjects( new GameRef(parts:0xAD5Er) );
				}
				else
				if( cmd == CMD_MAIN1_3_3 )				
				{
					pageNumberPrefix="V6-III/";
					sectionstxt.changeText("CONNECTING RODS");
					collectObjects( new GameRef(parts:0xDB7Er) );
				}
				else
				if( cmd == CMD_MAIN1_3_4 )				
				{
					pageNumberPrefix="V6-IV/";
					sectionstxt.changeText("PISTONS");
					collectObjects( new GameRef(parts:0x0DB6r) );
				}
				else
				if( cmd == CMD_MAIN1_3_5 )				
				{
					pageNumberPrefix="V6-V/";
					sectionstxt.changeText("CYLINDER HEADS");
					collectObjects( new GameRef(parts:0xD25Dr) );
				}
				else
				if( cmd == CMD_MAIN1_3_6 )				
				{
					pageNumberPrefix="V6-VI/";
					sectionstxt.changeText("CAMSHAFTS");
					collectObjects( new GameRef(parts:0xD25Fr) );
				}
				else
				if( cmd == CMD_MAIN1_3_7 )				
				{
					pageNumberPrefix="V6-VII/";
					sectionstxt.changeText("INTAKE SYSTEMS");
					collectObjects( new GameRef(parts:0xD26Fr) );
				}
				else
				if( cmd == CMD_MAIN1_3_8 )				
				{
					pageNumberPrefix="V6-VIII/";
					sectionstxt.changeText("EXHAUST SYSTEMS");
					collectObjects( new GameRef(parts:0xD282r) );
				}
				else
				if( cmd == CMD_MAIN1_3_9 )				
				{
					pageNumberPrefix="V6-IX/";
					sectionstxt.changeText("FUEL SYSTEMS");
					collectObjects( new GameRef(parts:0xD26Br) );
				}
				else
				if( cmd == CMD_MAIN1_3_10 )				
				{
					pageNumberPrefix="V6-X/";
					sectionstxt.changeText("PERFORMANCE SYSTEMS");
					collectObjects( new GameRef(parts:0x0DB4r) );
				}
				else
				if( cmd == CMD_MAIN1_3_11 )				
				{
					pageNumberPrefix="V6-XI/";
					sectionstxt.changeText("TRANSMISSIONS");
					collectObjects( new GameRef(parts:0xD24Br) );
				}
				else
				if( cmd == CMD_MAIN1_3_12 )				
				{
					pageNumberPrefix="V6-XII/";
					sectionstxt.changeText("CLUTCH & FLYWHEELS");
					collectObjects( new GameRef(parts:0xD23Cr) );
				}
				else
				if( cmd == CMD_MAIN1_3_13 )				
				{
					pageNumberPrefix="V6-XIII/";
					sectionstxt.changeText("MISC");
					collectObjects( new GameRef(parts:0x0DBEr) );
				}
			}
			if( cmd >= CMD_MAIN1_4_1 && cmd < CMD_MAIN1_5_1 ) //V8
			{
				changeGroup( parts9Group );

				if( cmd == CMD_MAIN1_4_1 )				
				{
					pageNumberPrefix="V8-I/";
					sectionstxt.changeText("ENGINE BLOCKS");
					collectObjects( new GameRef(parts:0xAC5Cr) );
				}
				else
				if( cmd == CMD_MAIN1_4_2 )				
				{
					pageNumberPrefix="V8-II/";
					sectionstxt.changeText("CRANK SHAFTS");
					collectObjects( new GameRef(parts:0xAC5Er) );
				}
				else
				if( cmd == CMD_MAIN1_4_3 )				
				{
					pageNumberPrefix="V8-III/";
					sectionstxt.changeText("CONNECTING RODS");
					collectObjects( new GameRef(parts:0xCB7Er) );
				}
				else
				if( cmd == CMD_MAIN1_4_4 )				
				{
					pageNumberPrefix="V8-IV/";
					sectionstxt.changeText("PISTONS");
					collectObjects( new GameRef(parts:0x0CB6r) );
				}
				else
				if( cmd == CMD_MAIN1_4_5 )				
				{
					pageNumberPrefix="V8-V/";
					sectionstxt.changeText("CYLINDER HEADS");
					collectObjects( new GameRef(parts:0xC25Dr) );
				}
				else
				if( cmd == CMD_MAIN1_4_6 )				
				{
					pageNumberPrefix="V8-VI/";
					sectionstxt.changeText("CAMSHAFTS");
					collectObjectsBegin();
					collectObjects( new GameRef(parts:0xC25Fr) );
					collectObjectsEnd();
				}
				else
				if( cmd == CMD_MAIN1_4_7 )				
				{
					pageNumberPrefix="V8-VII/";
					sectionstxt.changeText("INTAKE SYSTEMS");
					collectObjects( new GameRef(parts:0xC26Fr) );
				}
				else
				if( cmd == CMD_MAIN1_4_8 )				
				{
					pageNumberPrefix="V8-VIII/";
					sectionstxt.changeText("EXHAUST SYSTEMS");
					collectObjects( new GameRef(parts:0xC282r) );
				}
				else
				if( cmd == CMD_MAIN1_4_9 )				
				{
					pageNumberPrefix="V8-IX/";
					sectionstxt.changeText("FUEL SYSTEMS");
					collectObjects( new GameRef(parts:0xC26Br) );
				}
				else
				if( cmd == CMD_MAIN1_4_10 )				
				{
					pageNumberPrefix="V8-X/";
					sectionstxt.changeText("PERFORMANCE SYSTEMS");
					collectObjects( new GameRef(parts:0x0CB4r) );
				}
				else
				if( cmd == CMD_MAIN1_4_11 )				
				{
					pageNumberPrefix="V8-XI/";
					sectionstxt.changeText("TRANSMISSIONS");
					collectObjects( new GameRef(parts:0xC24Br) );
				}
				else
				if( cmd == CMD_MAIN1_4_12 )				
				{
					pageNumberPrefix="V8-XII/";
					sectionstxt.changeText("CLUTCH & FLYWHEELS");
					collectObjects( new GameRef(parts:0xC23Cr) );
				}
				else
				if( cmd == CMD_MAIN1_4_13 )				
				{
					pageNumberPrefix="V8-XIII/";
					sectionstxt.changeText("MISC");
					collectObjects( new GameRef(parts:0x0CBEr) );
				}
			}
			if( cmd >= CMD_MAIN1_5_1 && cmd < CMD_MAIN1_6_1 ) //V10
			{
				changeGroup( parts10Group );

				if( cmd == CMD_MAIN1_5_1 )				
				{
					pageNumberPrefix="V10-I/";
					sectionstxt.changeText("ENGINE BLOCKS");
					collectObjects( new GameRef(parts:0xAE5Cr) );
				}
				else
				if( cmd == CMD_MAIN1_5_2 )				
				{
					pageNumberPrefix="V10-II/";
					sectionstxt.changeText("CRANK SHAFTS");
					collectObjects( new GameRef(parts:0xAE5Er) );
				}
				else
				if( cmd == CMD_MAIN1_5_3 )				
				{
					pageNumberPrefix="V10-III/";
					sectionstxt.changeText("CONNECTING RODS");
					collectObjects( new GameRef(parts:0xEB7Er) );
				}
				else
				if( cmd == CMD_MAIN1_5_4 )				
				{
					pageNumberPrefix="V10-IV/";
					sectionstxt.changeText("PISTONS");
					collectObjects( new GameRef(parts:0x0EB6r) );
				}
				else
				if( cmd == CMD_MAIN1_5_5 )				
				{
					pageNumberPrefix="V10-V/";
					sectionstxt.changeText("CYLINDER HEADS");
					collectObjects( new GameRef(parts:0xE25Dr) );
				}
				else
				if( cmd == CMD_MAIN1_5_6 )				
				{
					pageNumberPrefix="V10-VI/";
					sectionstxt.changeText("CAMSHAFTS");
					collectObjects( new GameRef(parts:0xE25Fr) );
				}
				else
				if( cmd == CMD_MAIN1_5_7 )				
				{
					pageNumberPrefix="V10-VII/";
					sectionstxt.changeText("INTAKE SYSTEMS");
					collectObjects( new GameRef(parts:0xE26Fr) );
				}
				else
				if( cmd == CMD_MAIN1_5_8 )				
				{
					pageNumberPrefix="V10-VIII/";
					sectionstxt.changeText("EXHAUST SYSTEMS");
					collectObjects( new GameRef(parts:0xE282r) );
				}
				else
				if( cmd == CMD_MAIN1_5_9 )				
				{
					pageNumberPrefix="V10-IX/";
					sectionstxt.changeText("FUEL SYSTEMS");
					collectObjects( new GameRef(parts:0xE26Br) );
				}
				else
				if( cmd == CMD_MAIN1_5_10 )				
				{
					pageNumberPrefix="V10-X/";
					sectionstxt.changeText("PERFORMANCE SYSTEMS");
					collectObjects( new GameRef(parts:0x0EB4r) );
				}
				else
				if( cmd == CMD_MAIN1_5_11 )				
				{
					pageNumberPrefix="V10-XI/";
					sectionstxt.changeText("TRANSMISSIONS");
					collectObjects( new GameRef(parts:0xE24Br) );
				}
				else
				if( cmd == CMD_MAIN1_5_12 )				
				{
					pageNumberPrefix="V10-XII/";
					sectionstxt.changeText("CLUTCH & FLYWHEELS");
					collectObjects( new GameRef(parts:0xE23Cr) );
				}
				else
				if( cmd == CMD_MAIN1_5_13 )				
				{
					pageNumberPrefix="V10-XIII/";
					sectionstxt.changeText("MISC");
					collectObjects( new GameRef(parts:0x0EBEr) );
				}
			}
			if( cmd >= CMD_MAIN1_6_1 && cmd <= CMD_MAIN1_6_13 ) //V12
			{
				changeGroup( parts11Group );

				if( cmd == CMD_MAIN1_6_1 )				
				{
					pageNumberPrefix="V12-I/";
					sectionstxt.changeText("ENGINE BLOCKS");
					collectObjects( new GameRef(parts:0xAF5Cr) );
				}
				else
				if( cmd == CMD_MAIN1_6_2 )				
				{
					pageNumberPrefix="V12-II/";
					sectionstxt.changeText("CRANK SHAFTS");
					collectObjects( new GameRef(parts:0xAF5Er) );
				}
				else
				if( cmd == CMD_MAIN1_6_3 )				
				{
					pageNumberPrefix="V12-III/";
					sectionstxt.changeText("CONNECTING RODS");
					collectObjects( new GameRef(parts:0xFB7Er) );
				}
				else
				if( cmd == CMD_MAIN1_6_4 )				
				{
					pageNumberPrefix="V12-IV/";
					sectionstxt.changeText("PISTONS");
					collectObjects( new GameRef(parts:0x0FB6r) );
				}
				else
				if( cmd == CMD_MAIN1_6_5 )				
				{
					pageNumberPrefix="V12-V/";
					sectionstxt.changeText("CYLINDER HEADS");
					collectObjects( new GameRef(parts:0xF25Dr) );
				}
				else
				if( cmd == CMD_MAIN1_6_6 )				
				{
					pageNumberPrefix="V12-VI/";
					sectionstxt.changeText("CAMSHAFTS");
					collectObjects( new GameRef(parts:0xF25Fr) );
				}
				else
				if( cmd == CMD_MAIN1_6_7 )				
				{
					pageNumberPrefix="V12-VII/";
					sectionstxt.changeText("INTAKE SYSTEMS");
					collectObjects( new GameRef(parts:0xF26Fr) );
				}
				else
				if( cmd == CMD_MAIN1_6_8 )				
				{
					pageNumberPrefix="V12-VIII/";
					sectionstxt.changeText("EXHAUST SYSTEMS");
					collectObjects( new GameRef(parts:0xF282r) );
				}
				else
				if( cmd == CMD_MAIN1_6_9 )				
				{
					pageNumberPrefix="V12-IX/";
					sectionstxt.changeText("FUEL SYSTEMS");
					collectObjects( new GameRef(parts:0xF26Br) );
				}
				else
				if( cmd == CMD_MAIN1_6_10 )				
				{
					pageNumberPrefix="V12-X/";
					sectionstxt.changeText("PERFORMANCE SYSTEMS");
					collectObjects( new GameRef(parts:0x0FB4r) );
				}
				else
				if( cmd == CMD_MAIN1_6_11 )				
				{
					pageNumberPrefix="V12-XI/";
					sectionstxt.changeText("TRANSMISSIONS");
					collectObjects( new GameRef(parts:0xF24Br) );
				}
				else
				if( cmd == CMD_MAIN1_6_12 )				
				{
					pageNumberPrefix="V12-XII/";
					sectionstxt.changeText("CLUTCH & FLYWHEELS");
					collectObjects( new GameRef(parts:0xFF3Cr) );
				}
				else
				if( cmd == CMD_MAIN1_6_13 )				
				{
					pageNumberPrefix="V12-XIII/";
					sectionstxt.changeText("MISC");
					collectObjects( new GameRef(parts:0x0FBEr) );
				}
			}
			if( cmd >= CMD_MAIN1_7_1 && cmd <= CMD_MAIN1_7_13 ) //V16
			{
				changeGroup( parts12Group );
				if( cmd == CMD_MAIN1_7_1 )			
				{
					pageNumberPrefix="V16-I/";
					sectionstxt.changeText("ENGINE BLOCKS");
					collectObjects( new GameRef(parts:0xA05Cr) );
				}
				else
				if( cmd == CMD_MAIN1_7_2 )				
				{
					pageNumberPrefix="V16-II/";
					sectionstxt.changeText("CRANK SHAFTS");
					collectObjects( new GameRef(parts:0xA05Er) );
				}
				else
				if( cmd == CMD_MAIN1_7_3 )				
				{
					pageNumberPrefix="V16-III/";
					sectionstxt.changeText("CONNECTING RODS");
					collectObjects( new GameRef(parts:0x0B7Er) );
				}
				else
				if( cmd == CMD_MAIN1_7_4 )				
				{
					pageNumberPrefix="V16-IV/";
					sectionstxt.changeText("PISTONS");
					collectObjects( new GameRef(parts:0x00B6r) );
				}
				else
				if( cmd == CMD_MAIN1_7_5 )				
				{
					pageNumberPrefix="V16-V/";
					sectionstxt.changeText("CYLINDER HEADS");
					collectObjects( new GameRef(parts:0x025Dr) );
				}
				else
				if( cmd == CMD_MAIN1_7_6 )				
				{
					pageNumberPrefix="V16-VI/";
					sectionstxt.changeText("CAMSHAFTS");
					collectObjects( new GameRef(parts:0x025Fr) );
				}
				else
				if( cmd == CMD_MAIN1_7_7 )				
				{
					pageNumberPrefix="V16-VII/";
					sectionstxt.changeText("INTAKE SYSTEMS");
					collectObjects( new GameRef(parts:0x026Fr) );
				}
				else
				if( cmd == CMD_MAIN1_7_8 )				
				{
					pageNumberPrefix="V16-VIII/";
					sectionstxt.changeText("EXHAUST SYSTEMS");
					collectObjects( new GameRef(parts:0x0282r) );
				}
				else
				if( cmd == CMD_MAIN1_7_9 )				
				{
					pageNumberPrefix="V16-IX/";
					sectionstxt.changeText("FUEL SYSTEMS");
					collectObjects( new GameRef(parts:0x026Br) );
				}
				else
				if( cmd == CMD_MAIN1_7_10 )				
				{
					pageNumberPrefix="V16-X/";
					sectionstxt.changeText("PERFORMANCE SYSTEMS");
					collectObjects( new GameRef(parts:0x00B4r) );
				}
				else
				if( cmd == CMD_MAIN1_7_11 )				
				{
					pageNumberPrefix="V16-XI/";
					sectionstxt.changeText("TRANSMISSIONS");
					collectObjects( new GameRef(parts:0x024Br) );
				}
				else
				if( cmd == CMD_MAIN1_7_12 )				
				{
					pageNumberPrefix="V16-XII/";
					sectionstxt.changeText("CLUTCH & FLYWHEELS");
					collectObjects( new GameRef(parts:0x0F3Cr) );
				}
				else
				if( cmd == CMD_MAIN1_7_13 )				
				{
					pageNumberPrefix="V16-XIII/";
					sectionstxt.changeText("MISC");
					collectObjects( new GameRef(parts:0x00BEr) );
				}
			}

			curpage=1; //RAXAT: v2.3.1, messed up page number patch
			if(cmd != CMD_MAIN1_1 && cmd != CMD_MAIN1_2 && cmd != CMD_MAIN1_3 && cmd != CMD_MAIN1_4 && cmd != CMD_MAIN1_5 && cmd != CMD_MAIN1_6 && cmd != CMD_MAIN1_7)
			{
				collectObjectsEnd(); //RAXAT: v2.3.1, HUGE gadget hotspot patch!
			}
			refreshPage();
		}
	}
}

//----------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------

public class CatalogInventory extends VisualInventory
{
	Osd		osd;	//for da namez
	TextBox[]	names;

	Multiplayer multiplayer;
	Catalog	catalog;

	public CatalogInventory( Catalog cat, Player player, float left, float top, float width, float height, Multiplayer mp )
	{
		super(player, left, top, width, height );
		multiplayer = mp;
		catalog=cat;
	}
		
	public void initVisuals( float left, float top, float width, float height  )
	{
		linesPerPage=2;
		partsPerLine=4;

		//mely itemek lehetnek lathatoak kezdetben?
		cline=0;
		start = cline * partsPerLine;
		stop = start + linesPerPage * partsPerLine;

		//0..1 viewport coordinatarendszerben!
		float	itemWidth=0.20, itemHeight=0.15;
		float	hSpacing = (width-itemWidth*partsPerLine)/partsPerLine;
		float	vSpacing = (height-itemHeight*linesPerPage)/(linesPerPage-1);
		
		//backObject = new RenderRef( misc.catalog:0x00000020r );
		panels=new InventoryPanel[partsPerLine*linesPerPage];
		names=new TextBox[panels.length];

		//eggyel magasabb pri vp kell, mint maga a katalogus, kulonben eltunnek a szovegek!
		osd = new Osd( 1.0, 0.0, 11 );
		osd.iLevel = Osd.IL_NONE;

		int	index;
		float cheight=top;
		for( int i=0; i<linesPerPage; i++ )
		{
			float cwidth=left;
			for( int j=0; j<partsPerLine; j++ )
			{
				if( j == partsPerLine/2 )
					cwidth+=hSpacing;

				index = i*partsPerLine+j; 
				panels[index]=new CatalogInventoryPanel( this, index, cwidth, cheight, itemWidth, itemHeight, player );
				names[index]=osd.createTextBox( null, Frontend.smallFont_strong, Text.ALIGN_CENTER, (cwidth)*2-1, ((cheight+itemHeight)*2)-0.975, itemWidth*2 );
				names[index].changeColor( Palette.RGB_BLACK );

				cwidth+=itemWidth+hSpacing;
			}
			cheight+=itemHeight+vSpacing;
		}
	}

	public int upScroll()
	{
		if( cline )
		{
			cline-=linesPerPage;
			update();
			return 1;
		}
		return 0;
	}

	public int downScroll()
	{
		if( cline+linesPerPage < pages()*linesPerPage )
		{
			cline+=linesPerPage;
			update();
			return 1;
		}
		return 0;
	}

	//number of twin pages in a catalog section
	public int	pages()
	{
		if( items.size() > 1)
			return (items.size()-1)/(partsPerLine*linesPerPage)+1;
			
		return 1;
	}

	public void panelLeftClick( int index )
	{
		index += currentLine()*partsPerLine;

		if( index<items.size() )
		{
			InventoryItem item = items.elementAt( index );

			int price = item.getPrice() * Catalog.PRICERATIO;
			if( price <= player.getMoney() )
			{
				Dialog d = new BuyCatalogItemDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_WIDE, item, price );

				if( d.display() == 0 )
				{
					if(item.getPart() instanceof Chassis)
					{
						int freeSlot = player.carlot.getFreeSlot();
						if(freeSlot >= 0)
						{
							//generate new car
							GameRef xa = new GameRef();
							Part part = xa.create(player, new GameRef(item.getPart().getInfo(GameType.GII_TYPE)), "100,0,0,0,0,0", "chassis");
							
							//store previous car and give the new one to player
							player.carlot.lockPlayerCar();
							player.carlot.addCar(player.car); //RAXAT: build 937, prevents crash on saving player's car
							player.carlot.releasePlayerCar();
							player.carlot.flushCars();
							
							player.car = new Vehicle(part);
							GameLogic.garage.releaseCar();
							
							player.takeMoney(price);
							if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.spendMoney(price);
							catalog.osdCommand(Catalog.CMD_EXIT);
						}
						else new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "NOT ENOUGH SPACE", "You don't have enough free slots in car lot to buy this chassis!" ).display();
					}
					else
					{
						item.copyToInventory( player.parts ); //buy part
						player.takeMoney(price);
						catalog.refreshMoneyString();
					}

					//bonus decal for branded parts
					int decalID = item.getLogo();
					if( decalID )
					{
						player.decals.addElement( new Decal( decalID ) );
					}
				}
			}
			else
			{
				new WarningDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "NOT ENOUGH MONEY", "You don't have enough money to buy this part!" ).display();
			}
		}
	}

	public void update()
	{
		//hol kapcsolodnak ki a buttonok?
		int begin = start;
		int end = stop;

		//mibol lesznek buttonok?
		start = cline * partsPerLine;
		stop = start + linesPerPage * partsPerLine;

		int i, vis;

		//clear changed ones
		vis=0;
		for( i=begin; i<end; i++ )
		{
			names[vis++].changeText( null );
		}

		//add new ones
		vis=0;
		for( i=start; i<stop; i++ )
		{
			if( i<items.size() )
			{
				InventoryItem item = items.elementAt(i);
				names[vis++].changeText( item.getName() + " $"+ (int)(item.getPrice() * Catalog.PRICERATIO) );
			}
		}

		super.update();
	}

	public void show()
	{
		super.show();
		osd.show();
	}

	public void hide()
	{
		osd.hide();
		super.hide();
	}
}

//----------------------------------------------------------------------------------------------

public class CatalogInventoryPanel extends InventoryPanel
{
	Player	player;

	ResourceRef	infoTex0, infoTex1, infoTex2;
	Rectangle 	infoRect0, infoRect1, infoRect2;

	public CatalogInventoryPanel( CatalogInventory inventory, int index, float left, float top, float width, float height, Player p )
	{
		super( inventory, index, left, top, width, height );

		infoRect0 = inventory.osd.createRectangle( (left+width/2)*2-1, ((top-0.04)*2)-1, 0.1, 0.1, 1.0, -0.5, 0.0, 0, null );
		infoRect1 = inventory.osd.createRectangle( (left+width/2)*2-1, ((top-0.04)*2)-1, 0.1, 0.1, 1.0,  0.5, 0.0, 0, null );
		infoRect2 = inventory.osd.createRectangle( (left+width/2)*2-1, ((top-0.10)*2)-1, 0.3, 0.3, 1.0,  0.0, 0.0, 0, null );
		player = p;
	}

	public void swap( int index_a, int index_b )
	{
		//nincs swap!!
	}

	public void attachItem( InventoryItem invitem )
	{
		ypr = new Ypr( -1.4, -0.7, 0.0 );

		super.attachItem( invitem );

		infoTex0 = infoTex1 = null;

		if( invitem && invitem instanceof InventoryItem_Part)
		{
			((InventoryItem_Part)invitem).compatibility = 0;

			Part p=invitem.getPart();
			if( player.car )
			{
				if( p.getInfo( p.GII_COMPATIBLE, player.car.id() + "" ) )
				{
					if( p.getInfo( p.GII_INSTALL_OK, player.car.id() + "" ) )
					{
						infoTex0 = Catalog.RR_ICON_CAR_1STEP;
						((InventoryItem_Part)invitem).compatibility |= 1;
					}
					else
					{
						infoTex0 = Catalog.RR_ICON_CAR_COMP;
						((InventoryItem_Part)invitem).compatibility |= 2;
					}

					if(p.getLogo()) //RAXAT: v2.3.1, show logos above branded parts
					{
						if( p.getInfo( p.GII_COMPATIBLE, player.car.id() + "" ) )
						{
							infoTex2 = new ResourceRef( p.getLogo() );
						}
					}
				}
			}

			int compatibleParts;
			for( int i=player.parts.size()-1; i>=0; i-- )
			{
				if( player.parts.items.elementAt(i) instanceof InventoryItem_Part )
					if( p.getInfo( p.GII_INSTALL_OK, player.parts.items.elementAt(i).getPart().id() + "" ) )
					{
						compatibleParts++;
						((InventoryItem_Part)invitem).compatibility |= 4;
						break;	//inkabb ne nezzuk tovabb, lassit
					}
			}

			if( compatibleParts )
				infoTex1 = Catalog.RR_ICON_INV_COMP;
		}

		if(infoTex0) infoRect0.changeTexture( infoTex0 );
			else infoRect0.clearTexture(); //RAXAT: v2.3.1, fixes messed up part compatibility textures (that was possibly caused by a memory leak, so that's a temporary solution!
							//ResourceRef seems to destroy itself if there's no texture, but in v2.3.1 it keeps staying in memory (or rectangle keeps doing that)

		if(infoTex1) infoRect1.changeTexture( infoTex1 );
			else infoRect1.clearTexture();

		infoRect2.changeTexture( infoTex2 );
	}

}

public class BuyCatalogItemDialog extends YesNoDialog
{
	Player	player;
	public BuyCatalogItemDialog( Controller ctrl, int myflags, InventoryItem item, int price )
	{ 
		super( ctrl, myflags, "BUY PART", genBody( item, price) );
	}

	public String genBody( InventoryItem item, int price )
	{
		String body;
		body = "Do you want to buy this " + item.getName()  + " for $" + price + " ? \n \n Description: " + item.getDescription() + " \n \n Note: ";

		//RAXAT: chassis/part purchase
		if(item.getPart() instanceof Chassis) body = body + "you're about to buy chassis, this action will return you to the garage.";
		else if( item instanceof InventoryItem_Part )
		{
			if( !((InventoryItem_Part)item).compatibility )
				body = body + "This part is incompatible with your current car!";
			else
			{
				if( ((InventoryItem_Part)item).compatibility & 1 )
					body = body + "installable right away to your car";
				if( ((InventoryItem_Part)item).compatibility & 2 )
					body = body + "interchangeable with your cars parts";
				if( ((InventoryItem_Part)item).compatibility & 4 )
				{
					if( ((InventoryItem_Part)item).compatibility & 3 )
						body = body + " and ";
					body = body + "parts are fully compatible with your car";
				}

			}
		}

		return body;
	}
}