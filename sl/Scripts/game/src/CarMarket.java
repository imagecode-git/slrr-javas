//v 1.04
package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

import java.game.parts.*;
import java.game.parts.bodypart.*;

//RAXAT: v2.3.1, trade-in, test mode
public class CarMarket extends Scene implements GameState, Runnable
{
	final static int  RID_MAP_DEALER	= misc.dealer:0x0001r;
	final static int  RID_MAP_DEALER2	= misc.dealer2:0x0001r;
	final static int  RID_MAP_DEALER3	= misc.dealer3:0x0001r;

	final static int  RID_STUFF_DEALER	= misc.dealer:0x000Fr;
	final static int  RID_STUFF_DEALER2	= misc.dealer2:0x000Dr;
	final static int  RID_STUFF_DEALER3	= misc.dealer3:0x0103r;

	final static float FLOOR_HEIGHT		= 3.3;

	final static float PRICERATIO_NEW	= 1.3;
	final static float PRICERATIO_TRADEIN	= 1.1;
	final static float PRICERATIO_USED	= 1.1;
	final static float PRICERATIO_BUY	= 0.7;
	final static float PRICERATIO_BUY_TI	= 0.9;
	final static float PRESTIGE_RATIO_BUY	= 0.25;
	final static float PRESTIGE_RATIO_SELL	= 0.50;

	final static int CMD_PREVCAR	= 0;
	final static int CMD_NEXTCAR	= 1;

	final static int MODE_BROWSE	= 0;
	final static int MODE_TEST	= 1;

	Multiplayer		multiplayer;
	Player			player;
	GameState		parentState;

	GameRef			cam;
	int				move;

	Osd				osd;
	Text			moneytxt;
	Text			carName;
	Gadget			buyButton;

	int				used, tradein;
	DealerData		positions;
	int				numpos;
	int				welcomeTextureId, sellTextureId;
	float			priceRatio;

	RenderRef		stuff1;

	VehicleDescriptor[]	carDescriptors;
	Vehicle[]		cars;
	int				curcar;

	int				mode; //RAXAT: dealership mode

	MapObject		standObj;
	PhysicsRef		standPhys;
	Vector3			standPhysPos = new Vector3(15.900, 0.800, 8.300);
	Vector3			standPos = new Vector3(15.900, 0.500, 8.300);
	Ypr				standOri = new Ypr(0,0,0);
	float			standRot;
	float			standRotSpeed = 0.01;
	int				rotVhcIdx;
	Thread			rotThread;

	int				defGroup, browseGroup, testGroup;

	float			defDeform;
	float			defVol;

	//RAXAT: wrecked vehicles generator, buggy!! disabled in release version
	int			useWrecked = 0;
	int			processedIdx;
	Vector			wreckedIndices; //to remove vd's
	float			wreckedPrevalence = 0.2; //how often wrecked vehicles will appear in used car dealer

	public static VehicleDescriptor[] getInitialCars(int used)
	{
		int i, vt;
		VehicleDescriptor[] result = new VehicleDescriptor[13];

		if(used)
		{
			vt = VehicleType.VS_USED;

			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Einvagen:0x00000006r;
			result[i].power = 0.90;
			result[i].optical = 0.95;
			result[i].tear = 0.93;
			result[i].wear = 0.83;
			result[i].stockPrestige = 204;
			result[i].fullPrestige = 244;
			result[i].stockQM = VehicleType.qm_stock_Einvagen_110_GT;
			result[i].fullQM = VehicleType.qm_full_Einvagen_110_GT;
			result[i].vehicleName = "Einvagen 110 GT";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;

			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Einvagen:0x00000112r;
			result[i].power = 0.85;
			result[i].optical = 0.92;
			result[i].tear = 0.89;
			result[i].wear = 0.75;
			result[i].stockPrestige = 225;
			result[i].fullPrestige = 265;
			result[i].stockQM = VehicleType.qm_stock_Einvagen_110_GTK;
			result[i].fullQM = VehicleType.qm_full_Einvagen_110_GTK;
			result[i].vehicleName = "Einvagen 110 GTK";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;

			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Duhen:0x00000006r;
			result[i].power = 0.90;
			result[i].optical = 0.90;
			result[i].tear = 0.87;
			result[i].wear = 0.88;
			result[i].stockPrestige = 265;
			result[i].fullPrestige = 286;
			result[i].stockQM = VehicleType.qm_stock_Duhen_SunStrip_1_5_DVC;
			result[i].fullQM = VehicleType.qm_full_Duhen_SunStrip_1_5_DVC;
			result[i].vehicleName = "Duhen SunStrip 1.5 DVC";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;

			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Duhen:0x00000126r;
			result[i].power = 0.90;
			result[i].optical = 0.90;
			result[i].tear = 0.85;
			result[i].wear = 0.83;
			result[i].stockPrestige = 292;
			result[i].fullPrestige = 313;
			result[i].stockQM = VehicleType.qm_stock_Duhen_SunStrip_1_8_DVC;
			result[i].fullQM = VehicleType.qm_full_Duhen_SunStrip_1_8_DVC;
			result[i].vehicleName = "Duhen SunStrip 1.8 DVC";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;
			
			//RAXAT: more initial cars in v2.3.1
			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Axis:0x00000006r;
			result[i].power = 0.90;
			result[i].optical = 0.90;
			result[i].tear = 0.81;
			result[i].wear = 0.79;
			result[i].stockPrestige = 254;
			result[i].fullPrestige = 273;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_1;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_1;
			result[i].vehicleName = "Axis 200S";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;
			
			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Axis:0x00000157r;
			result[i].power = 0.93;
			result[i].optical = 0.86;
			result[i].tear = 0.91;
			result[i].wear = 0.83;
			result[i].stockPrestige = 276;
			result[i].fullPrestige = 303;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_2;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_2;
			result[i].vehicleName = "Axis 200XT";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;
			
			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Ninja:0x00000006r;
			result[i].power = 0.78;
			result[i].optical = 0.81;
			result[i].tear = 0.75;
			result[i].wear = 0.81;
			result[i].stockPrestige = 235;
			result[i].fullPrestige = 281;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_1;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_1;
			result[i].vehicleName = "Ninja TurboHatch";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;
			
			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Ninja:0x00000156r;
			result[i].power = 0.89;
			result[i].optical = 0.92;
			result[i].tear = 0.82;
			result[i].wear = 0.87;
			result[i].stockPrestige = 257;
			result[i].fullPrestige = 335;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_2;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_2;
			result[i].vehicleName = "Ninja PowerLine S";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;

			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Codrac:0x00000006r;
			result[i].power = 0.76;
			result[i].optical = 0.79;
			result[i].tear = 0.82;
			result[i].wear = 0.71;
			result[i].stockPrestige = 198;
			result[i].fullPrestige = 239;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_1;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_1;
			result[i].vehicleName = "Einvagen Codrac Sport";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;
			
			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Codrac:0x00000006r;
			result[i].power = 0.77;
			result[i].optical = 0.82;
			result[i].tear = 0.78;
			result[i].wear = 0.67;
			result[i].stockPrestige = 174;
			result[i].fullPrestige = 224;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_1;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_1;
			result[i].vehicleName = "Einvagen Remo 1.8L";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;
			
			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Sunset:0x00000006r;
			result[i].power = 0.92;
			result[i].optical = 0.86;
			result[i].tear = 0.91;
			result[i].wear = 0.85;
			result[i].stockPrestige = 275;
			result[i].fullPrestige = 306;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_1;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_1;
			result[i].vehicleName = "Shimutshibu Sunset E96S";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;

			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Sunset:0x00000156r;
			result[i].power = 0.94;
			result[i].optical = 0.91;
			result[i].tear = 0.81;
			result[i].wear = 0.78;
			result[i].stockPrestige = 297;
			result[i].fullPrestige = 334;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_2;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_2;
			result[i].vehicleName = "Shimutshibu Sunset E98T";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;
			
			result[i] = new VehicleDescriptor();
			result[i].id = cars.racers.Codrac:0x00000006r;
			result[i].power = 0.72;
			result[i].optical = 0.78;
			result[i].tear = 0.82;
			result[i].wear = 0.71;
			result[i].stockPrestige = 184;
			result[i].fullPrestige = 214;
			result[i].stockQM = VehicleType.qm_stock_Universal_stage_1;
			result[i].fullQM = VehicleType.qm_full_Universal_stage_1;
			result[i].vehicleName = "Teg S199";
			result[i++].colorIndex = Math.random()*GameLogic.CARCOLORS.length;
		}
		else	vt = VehicleType.VS_STOCK;

		for(;i<result.length; i++) result[i] = GameLogic.getVehicleDescriptor(vt);

		return result;
	}

	public static void alterCars(int used, VehicleDescriptor[] cars, float hoursPassed)
	{
		int vt;
		if(used) vt = VehicleType.VS_USED;
		else vt = VehicleType.VS_STOCK;

		for(int i=0; i<cars.length; i++)
		{
			
			if(!cars[i] || Math.random() < 0.02*hoursPassed) cars[i] = GameLogic.getVehicleDescriptor(vt);
			else
			{
				if(Math.random() < 0.02*hoursPassed) cars[i] = null;
			}
		}
	}
	

	public CarMarket(int used, VehicleDescriptor[] carDescriptors, Multiplayer mp)
	{
		multiplayer = mp;
		createNativeInstance();

		if(used < 0)
		{
			this.tradein = 1;
			used = 0;
		}

		this.used = used;
		this.carDescriptors = carDescriptors;
		this.player = GameLogic.player;
	}
	
	public Vector3 getCarPos(int n)
	{
		return new Vector3(positions.carPos[n]);
	}

	public Ypr getCarOri(int n)
	{
		Ypr ypr = new Ypr(positions.carOri[n]);
		ypr.y += 0.2-Math.random()*0.4;
		return ypr;
	}

	public void moveCamera()
	{
		if(cars[curcar])
		{
			int price = cars[curcar].getTotalPrice();

			if(used && curcar == 0)
			{
				price *= 1.0+((cars[curcar].getPrestigeMultiplier()-1.0)*PRESTIGE_RATIO_BUY);
				price *= PRICERATIO_BUY;
				carName.changeText("YOUR CAR  $" + price);
			}
			else
			{
				price *= 1.0+((cars[curcar].getPrestigeMultiplier()-1.0)*PRESTIGE_RATIO_SELL);
				price *= priceRatio;
				
				if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
				{
					String[] params = new String[1];
					params[0] = price;
					price = multiplayer.RPC("getCarCost", params, true).intValue();
				}
				
				carName.changeText( cars[curcar].toString() + " $" + price );
			}

			cam.command("move " + cars[curcar].id() + " 0,0,0 4.5");
			cam.command("look " + cars[curcar].id() + " 0,0,0 0,0,0");
			cam.command("height 1.9");

			int tex = frontend:0x9C05r; //buy
			if((used && curcar == 0) || tradein) tex = frontend:0x9C1Dr; //sell
			buyButton.changeTexture(new ResourceRef(tex));
		}
	}


	public void enter(GameState prevState)
	{
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) enableAnimateHook();
		
		if(prevState instanceof CarInfo) osd.show();
		else
		{
			parentState = prevState;
			mode = MODE_BROWSE;

			if(used)
			{
				if(useWrecked)
				{
					//RAXAT: this class will attempt to change config params, so current config needs to be backed up before that
					GameLogic.saveLastConfig();
					wreckedIndices = new Vector();
				}

				map = new GroundRef(RID_MAP_DEALER2);
				positions = new DealerSHData();
				stuff1 = new RenderRef(map, RID_STUFF_DEALER2, null);
				welcomeTextureId = frontend:0x00A4r;
				sellTextureId = frontend:0x00CEr;
				priceRatio = PRICERATIO_USED;
			}
			else
			{
				if(!tradein)
				{
					map = new GroundRef(RID_MAP_DEALER);
					positions = new DealerNewData();
					stuff1 = new RenderRef(map, RID_STUFF_DEALER, null);
					welcomeTextureId = frontend:0x00CAr;
					sellTextureId = frontend:0x00CDr;
					priceRatio = PRICERATIO_NEW;
				}
				else
				{
					//RAXAT: new in v2.3.1
					map = new GroundRef(RID_MAP_DEALER3);
					standObj = new MapObject(map, misc.dealer3:0x00000032r); //stand.cfg
					standObj.setMatrix(standPos, standOri);

					//RAXAT: stand has its own phys, but for some reasons car keeps randomly shaking when colliding it
					standPhys = new PhysicsRef();
					standPhys.createBox(map, 10.000, 0.01, 10.000, null);
					standPhys.setMatrix(standPos, standOri);

					positions = new DealerTIData();
					stuff1 = new RenderRef(map, RID_STUFF_DEALER3, null);
					welcomeTextureId = frontend:0x9408r;
					sellTextureId = frontend:0x9409r;
					priceRatio = PRICERATIO_TRADEIN;
				}
			}

			Frontend.loadingScreen.show(new ResourceRef(welcomeTextureId));

			numpos = positions.carPos.length;
			cars = new Vehicle[numpos];

			osd = new Osd();
			osd.globalHandler = this;

			carName = osd.createText(null, Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, -0.98);
			moneytxt=osd.createFooter("$"); //RAXAT: v2.3.1, new money txt

			osd.createStrongHeader("CAR DEALER");

			Style buttonStyle = new Style(0.1, 0.1, Frontend.mediumFont, Text.ALIGN_LEFT, null);
			float posx = -0.18; if(used) posx = -0.12;
			Menu m = osd.createMenu(buttonStyle, 0.925, -0.93, 0, Osd.MD_HORIZONTAL);
			m.addItem(new ResourceRef(Osd.RID_BACK), Input.AXIS_CANCEL, null, null, 1);

			defGroup = osd.endGroup();

			m = osd.createMenu(buttonStyle, posx, 0.92, 0, Osd.MD_HORIZONTAL);

			m.addItem(new ResourceRef(Osd.RID_ARROWLF), Input.AXIS_MENU_LEFT, null, null, 1);
			m.addItem(new ResourceRef(Osd.RID_ARROWRG), Input.AXIS_MENU_RIGHT, null, null, 1);
			if(!used) m.addItem(new ResourceRef(frontend:0x9C11r), Input.AXIS_HELP, null, null, 1); //RAXAT: v2.3.1, info icon is hidden in joe's dealership
			buyButton = m.addItem(new ResourceRef(frontend:0x9C05r), Input.AXIS_SELECT, null, null, 1);
			if(!used) m.addItem(new ResourceRef(frontend:0x9C12r), Input.AXIS_MENU, null, null, 1); //RAXAT: v2.3.1, vehicle check button

			browseGroup = osd.endGroup();

			//RAXAT: buttons for vehicle test mode
			m = osd.createMenu(buttonStyle, 0.0, 0.92, 0, Osd.MD_HORIZONTAL);
			m.addItem(new ResourceRef(Osd.RID_OK), Input.AXIS_SELECT, null, null, 1);
			testGroup = osd.endGroup();
			osd.hideGroup(testGroup);

			lockPlayerCar();

			//RAXAT: trade-in building has a lot of windows, so we have to add some fog to visually enhance environment around it
			if(tradein)
			{
				enableFog = 1;
				customFog = 1;

				fogVars[0] = 20;
				fogVars[1] = 80;
			}

			addSceneElements(GameLogic.getTime()); //RAXAT: this will actually call the fog

			if(!used)
			{
				if(!tradein) GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x003Ar));
				else GfxEngine.setGlobalEnvmap(new ResourceRef(misc.dealer3:0x00000033r));
			}
			else
			{
				if(useWrecked)
				{
					defDeform = Config.deformation;
					defVol = Sound.getVolume(Sound.CHANNEL_EFFECTS);

					Sound.setVolume(Sound.CHANNEL_EFFECTS, 0.0);

					Config.deformation = 0.5+(Math.random()/3);
					Config.external_damage = 0.4+(Math.random()/3);
					Config.internal_damage = (0.2+(Math.random()/3))/2;
				}
			}

			Number price = new Number();
			int emptySpaces = cars.length-carDescriptors.length-used;
			int offsetVd;
			for(int i=used; i<cars.length; i++)
			{
				if((emptySpaces && Math.random() < 0.4 ) || cars.length-emptySpaces<=i)
				{
					--emptySpaces;
				}
				else
				{
					int idx = offsetVd++;
					VehicleDescriptor vd = carDescriptors[idx];

					if(vd)
					{
						Vehicle	car = new Vehicle(map, vd.id, vd.colorIndex, vd.optical, vd.power, vd.wear, vd.tear);

						//RAXAT: build 938, broken vehicle descriptors patch
						if(!car || !car.chassis)
						{
							int vt_patch = VehicleType.VS_STOCK;
							if(used) vt_patch = VehicleType.VS_USED;
							
							VehicleDescriptor vd_patch = GameLogic.getVehicleDescriptor(vt_patch);
							car = new Vehicle(map, vd_patch.id, vd_patch.colorIndex, vd_patch.optical, vd_patch.power, vd_patch.wear, vd_patch.tear);
						}
						car.chassis.setMileage((1-vd.wear)*10000000f);
						processedIdx = idx;
						addCar(car, i);

						if(tradein) //RAXAT: new in v2.3.1
						{
							//RAXAT: attempting to detect the most expensive car
							int temp = car.getTotalPrice();
							if(temp > price.f)
							{
								price.f = temp;
								price.i = i;
							}
						}
					}
				}
			}

			//RAXAT: now put this car onto the rotating stand
			if(tradein)
			{
				int idx = price.i;
				if(cars[idx])
				{
					positions.carPos[idx] = standPos;
					positions.carOri[idx] = standOri;
					cars[idx].setMatrix(standPos, standOri);

					rotVhcIdx = idx;
					rotThread = new Thread(this, "Vehicle stand rotator thread");
					rotThread.start(); //begin rotating stand and vehicle on it
				}
			}

			System.timeWarp(1.0);
			osd.getViewport().activate(Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET);

			if(used) cam = new GameRef(map, GameRef.RID_CAMERA, "-3,2,0, 0,0,0, 0x13, 1.8,0.0, 0.05", "Car Dealer camera");
			else
			{
				if(!tradein) cam = new GameRef(map, GameRef.RID_CAMERA, "10,2,-3, 0,0,0, 0x13, 1.8,0.0, 0.05", "Car Dealer camera");
				else cam = new GameRef(map, GameRef.RID_CAMERA, "10,2,-5, 0,0,0, 0x0, 1.8,0.0, 0.05", "Car Dealer camera");
			}

			resetCamera();

			refreshMoneyString(1);
			Frontend.loadingScreen.userWait(5.0);

			if(used)
			{
				for(int i=0; i<cars.length; i++)
				{
					if(cars[i])
					{
						if(useWrecked)
						{
							cars[i].setMatrix(getCarPos(i), getCarOri(i));
							if(cars[i].chassis)
							{
								cars[i].chassis.setState(getCarPos(i), getCarOri(i), new Vector3(0), new Vector3(0));
							}
						}

						cars[i].command("reset");
						cars[i].command("stop");
					}
				}

				if(useWrecked)
				{
					//RAXAT: restoring previously saved config params
					Config.deformation = defDeform;
					Config.external_damage = defDeform;
					Config.internal_damage = defDeform/2.0;
					Sound.setVolume(Sound.CHANNEL_EFFECTS, defVol);
				}
			}

			osd.createHotkey(Input.AXIS_MENU_LEFT, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_MENU_LEFT, this);
			osd.createHotkey(Input.AXIS_MENU_RIGHT, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_MENU_RIGHT, this);
			osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_CANCEL, this);
			osd.createHotkey(Input.AXIS_SELECT, Input.VIRTUAL|Osd.HK_STATIC, Input.AXIS_SELECT, this);

			osd.show();

			setEventMask(EVENT_CURSOR);
		}

		//special request: reset mouse and set sensitivity to 0
		Input.getAxis (1, -1);
		Input.cursor.enable(1);
		Input.cursor.addHandler(this);
		Input.cursor.enableCameraControl(cam);
		
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
		{
			multiplayer.osd = osd;
			multiplayer.player = player;
			multiplayer.cam = cam;
			multiplayer.map = map;
			multiplayer.RPC("inMarket", null);
		}
	}
	
    public void animate()
	{
		multiplayer.runRPCScript();
	}

	public void resetCamera()
	{
		cam.command("render " + osd.getViewport().id() + " 0 0 1 " + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
		cam.command("dist 2.5 4.6");
		cam.command("smooth 0.05 0.5");
		cam.command("force 0.3 0.5 -0.7");
		cam.command("torque 0.06");
		cam.command("zoom 60 5");

		goRight();
		goLeft();
	}

	public void exit(GameState nextState)
	{
		if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) disableAnimateHook();
	
		Input.cursor.enable(0);
		Input.cursor.remHandler(this);
		Input.cursor.disableCameraControl();

		if(nextState instanceof CarInfo) osd.hide();
		else
		{
			clearEventMask(EVENT_ANY);

			cam.destroy();
			releasePlayerCar();
			for(int i=0; i<cars.length; i++)
			{
				if(cars[i])
				{
					cars[i].destroy();
					cars[i]=null;
				}
			}

			if(used)
			{
				if(useWrecked)
				{
					for(int j=0; j<wreckedIndices.size(); j++)
					{
						int idx = wreckedIndices.elementAt(j).intValue();
						if(carDescriptors[idx]) carDescriptors[idx] = null;
					}
				}
			}

			enableFog = 0;
			remSceneElements();
			map.unload();
			if(tradein)
			{
				if(standObj) standObj.finalize();

				if(standPhys)
				{
					standPhys.finalize();
					standPhys = null;
				}

				if(rotThread)
				{
					rotThread.stop();
					rotThread=null;
				}
			}
			osd.hide();
			osd=null;
			stuff1.destroy();
			parentState=null;
		}
	}

	//RAXAT: v2.3.1, stand/vehicle rotator for trade-in dealer
	public void run()
	{
		while(1)
		{
			if(standObj && cars[rotVhcIdx])
			{
				if(mode == MODE_TEST && curcar == rotVhcIdx)
				{
					//no operation! stand will not rotate if player is testing a vehicle placed right on it
				}
				else
				{
					standRot += standRotSpeed;
					Ypr rot = new Ypr(standRot, standOri.p, standOri.r);

					//RAXAT: setState is used instead of simple setMatrix because we need to eliminate all outside forces when rotating the vehicle
					//so these forces (gravity, rotational impulse, etc.) won't interfere opening flappable parts when vehicle does rotate
					if(cars[rotVhcIdx].chassis) cars[rotVhcIdx].chassis.setState(standPos, rot, new Vector3(0), new Vector3(0));
					standObj.setMatrix(standPos, rot);
				}
			}

			Thread.sleep(30);
		}
	}


	public void lockPlayerCar()
	{
		if(used && GameLogic.player.car)
		{
			addCar(GameLogic.player.car, 0);
			GameLogic.player.car=null;
		}
	}

	public void releasePlayerCar()
	{
		if(used && cars[0])
		{
			cars[0].command("start");
			GameLogic.player.car = cars[0];
			GameLogic.player.car.setParent(GameLogic.player);
			cars[0] = null;
		}
	}

	public int addCar(Vehicle car, int slot)
	{
		if(car)
		{
			if(!cars[slot])
			{
				cars[slot] = car;

				car.setParent(map);
				Vector3 pos = getCarPos(slot);
				Ypr ori = getCarOri(slot);
				pos.z += Math.random()*0.1;
				pos.x += Math.random()*0.1;
				car.setMatrix(pos, ori);
				car.command("reset");
				car.command("setsteer "+ (-0.7+(Math.random()*1.4)));

				if(used && useWrecked)
				{
					if(wreckedPrevalence > 1) wreckedPrevalence = 1.0;

					if(Math.random() >= (1.0-wreckedPrevalence) && slot != 0)
					{
						//RAXAT: these forces will smash vehicles into the walls while dealership is being loaded
						float forceF = 0.4; //forward
						float forceR = 0.6; //rewind

						car.chassis.setState
						(
							new Vector3(pos.x+3, pos.y, pos.z-3), ori,
							new Vector3(3,0,3), new Vector3(Math.random()*30*forceF,5,Math.random()*30*forceF)
						);

						car.chassis.setState
						(
							pos, ori,
							new Vector3(Math.random()*(-50)*forceR,0,Math.random()*(50)*forceR),
							new Vector3(Math.random()*(-15)*forceR,0,Math.random()*(30)*forceR)
						);
						car.wakeUp();
						wreckedIndices.addElement(new Integer(processedIdx));
					}
				}

				if(!tradein) car.command("stop");

				return 1;
			}
		}

		return 0;
	}

	public GameRef createCar(GameRef carType)
	{
		Vehicle car;
		float opti, engi, wear, tear;

		if(used)
		{
			opti = 0.3 + Math.random()*1.5;	//0.3-1.8
			engi = 0.5 + Math.random()*1.0;	//0.5-1.5
			wear = 0.4 + Math.random()*0.6;
			tear = 0.7 + Math.random()*0.3;
		}
		else
		{
			opti = 1.0;
			engi = 1.0;
			wear = 1.0;
			tear = 1.0;
		}

		car = new Vehicle(map, carType.id(), Math.random(), opti, engi, wear, tear);
		car.chassis.setMileage((1-wear)*10000000f);

		return car;
	}

	public void refreshMoneyString()
	{
		refreshMoneyString(0);
	}

	public void refreshMoneyString(int init)
	{
		if(moneytxt)
		{
			String diff = "";
			if(moneytxt && moneytxt.text) diff = moneytxt.text.cut("$");
			if(Integer.toString(player.getMoney()) != diff)
			{
				if(moneytxt.text && !init) new SfxRef(Frontend.SFX_MONEY).play();
				moneytxt.changeText("$" + Integer.toString(player.getMoney()));
			}
		}
	}

	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(Input.AXIS_MENU_LEFT):
				if(mode == MODE_BROWSE) goLeft();
				break;

			case(Input.AXIS_MENU_RIGHT):
				if(mode == MODE_BROWSE) goRight();
				break;

			case(Input.AXIS_CANCEL):
				//RAXAT: bug! camera must reset when return to garage!
				if(mode == MODE_TEST) osdCommand(Input.AXIS_MENU);
				GameLogic.changeActiveSection(parentState);
				break;

			case(Input.AXIS_HELP):
				if(mode == MODE_BROWSE)
				{
					if(cars[curcar]) GameLogic.changeActiveSection(new CarInfo(cars[curcar]));
				}
				break;

			case(Input.AXIS_SELECT):
				if(mode == MODE_TEST)
				{
					osd.hideGroup(testGroup);
					osd.showGroup(browseGroup);

					if(cars[curcar])
					{
						if(player.render) player.render.destroy(); //driver's render instance

						GameRef con = new GameRef(cars[curcar].getInfo(GII_OWNER));
						cars[curcar].command("stop");
						cars[curcar].command("reset"); //this will reset wheel steer
						cars[curcar].command("setsteer "+ (-0.7+(Math.random()*1.4))); //set random wheel steer back
						cars[curcar].command("hide " + osd.getViewport().id()); //kills internal cam
					}

					player.controller.reset();
					player.controller.activateState(ControlSet.MENUSET);
					player.controller.command("leave " + cars[curcar].id());

					resetCamera();

					mode = MODE_BROWSE;
				}
				else buyCar();
				break;

			case(Input.AXIS_MENU):
				if(mode == MODE_BROWSE)
				{
					if(cars[curcar])
					{
						mode = MODE_TEST;

						cam.command("hide " + osd.getViewport().id()); //hides (but not eliminates!) active external cam
						player.render = new RenderRef(map, player.driverID, "player_render");
						player.controller.command("renderinstance " + player.render.id());
						player.controller.command("controllable " + cars[curcar].id());
						GameRef con = new GameRef(cars[curcar].getInfo(GII_OWNER));
						cars[curcar].command("render " + osd.getViewport().id() + " " + con.id() + " " + 0); //switch to internal cam
						cars[curcar].command("stop");
						cars[curcar].command("osd " + con.id() + " " + con.id()); //show up _internal_ gauges (not osd!)
						cars[curcar].wakeUp();

						player.controller.reset();
						player.controller.activateState(ControlSet.DRIVERSET);

						osd.hideGroup(browseGroup);
						osd.showGroup(testGroup);
					}
				}
				break;
		}
	}

	public void goLeft()
	{
		while(1)
		{
			if(--curcar < 0) curcar = numpos-1;
			if(cars[curcar]) break;
		}

		moveCamera();
	}

	public void goRight()
	{
		while(1)
		{
			if(++curcar >= numpos) curcar = 0;
			if(cars[curcar]) break;
		}

		moveCamera();
	}

	public void changePointer()
	{
		if(move) Input.cursor.setPointer(Frontend.pointers, "M");
		else Input.cursor.setPointer(Frontend.pointers, "J");
	}

	public void handleEvent(GameRef obj_ref, int event, String param)
	{
		int	tok = -1;

		if(event == EVENT_CURSOR)
		{
			int ec = param.token( ++tok).intValue();
			int cursor_id = param.token(++tok).intValue();

			if(ec == GameType.EC_LCLICK)
			{
				GameRef dest = new GameRef(param.token(++tok).intValue());
				int cat = dest.getInfo(GameType.GII_CATEGORY);

				if(cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE)
				{
					int carID = dest.getScriptInstance().getCar();
					if(cars[curcar].chassis.id())
					{
						if(carID == cars[curcar].chassis.id())
						{
							Object part = obj_ref.getScriptInstance();
							if(part instanceof Part)
							{
								part.command("flap_toggle");
							}
						}
					}
				}
			}
			else
			if(ec == GameType.EC_RCLICK)
			{
				GameRef dest = new GameRef(param.token(++tok).intValue());
				int cat = dest.getInfo(GameType.GII_CATEGORY);

				if(cat == GIR_CAT_PART || cat == GIR_CAT_VEHICLE)
				{
					int carID = dest.getScriptInstance().getCar();
					if(cars[curcar])
					{
						if(cars[curcar].chassis && cars[curcar].chassis.id() == carID)
						{
							cam.command("look " + dest.id() + " " + 
									param.token(++tok) + "," +
									param.token(++tok) + "," +
									param.token(++tok)
								   );
						}
					}
				}
			}
			else
			if(ec == GameType.EC_RDRAGBEGIN)
			{
				move=1;
				changePointer();

				//enable camera control with mouse
				player.controller.user_Add(Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);
				player.controller.user_Add(Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);

				//disable cursor movement
				player.controller.user_Del(Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0);
				player.controller.user_Del(Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1);

				Input.cursor.cursor.command("lock");
			}
			else
			if(ec == GameType.EC_RDRAGEND)
			{
				move=0;
				changePointer();

				//disable camera control with mouse
				player.controller.user_Del(Input.AXIS_LOOK_UPDOWN,	ControlSet.MOUSE, 1);
				player.controller.user_Del(Input.AXIS_LOOK_LEFTRIGHT,	ControlSet.MOUSE, 0);

				//enable cursor movement
				player.controller.user_Add(Input.AXIS_CURSOR_X,	ControlSet.MOUSE, 0,	-1.0f, 1.0f, -1.0f, 1.0f);
				player.controller.user_Add(Input.AXIS_CURSOR_Y,	ControlSet.MOUSE, 1,	-1.0f, 1.0f, -1.0f, 1.0f);

				Input.cursor.cursor.command("unlock");
			}
		}
	}

	public void buyCar()
	{
		if(cars[curcar])
		{
			int price = cars[curcar].getTotalPrice();

			if(used && curcar == 0)
			{
				price *= 1.0+((cars[curcar].getPrestigeMultiplier()-1.0)*PRESTIGE_RATIO_BUY);
				price *= PRICERATIO_BUY;

				Dialog d = new NoYesDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "SELL CAR", "Do you want to sell your car for $" + price +" ?");
				if(d.display() == 0)
				{
					player.addMoney(price);
					if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.spendMoney(-price);
					refreshMoneyString();

					//g13ba: this seems to avoid the "Buy, Buy, Sell" crash at Carlot.java 
					releasePlayerCar();
					player.carlot.lockPlayerCar();
					player.carlot.releasePlayerCar();
					lockPlayerCar();

 					//remove car;
					cars[curcar].command("reset");
					cars[curcar].command("start");

					//find a new slot for it:
					for(int i=numpos-1; i; i--)
					{
						if(!cars[i])
						{
							addCar(cars[curcar], i);
							cars[curcar]=null;
							goRight();
						}
					}

					Frontend.loadingScreen.display(new SimpleLoadingDialog(Dialog.DF_FULLSCREEN|Dialog.DF_MODAL, new ResourceRef(frontend:0x00A3r)), 5.0);
				}
			}
			else
			{
				price *= 1.0+((cars[curcar].getPrestigeMultiplier()-1.0)*PRESTIGE_RATIO_SELL);
				price*=priceRatio;
				
				if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER)
				{
					String[] params  = new String[1];
					params[0] = price;
					price = multiplayer.RPC("getCarCost", params, true).intValue();
				}

				if((price <= player.getMoney()) || tradein)
				{
					if(!tradein)
					{
						releasePlayerCar();
						player.carlot.lockPlayerCar();
						player.carlot.releasePlayerCar();
						lockPlayerCar();
					}

					int buy = 0;
					int freeSlot = player.carlot.getFreeSlot();
					if(freeSlot >= 0)
					{
						if(!tradein)
						{
							Dialog dialog = new NoYesDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_WIDE, "BUY CAR", "Do you want to buy this " + cars[curcar].toString()  + " for $" + price + " ? \n \n About the " + cars[curcar].toString()+ ": " + cars[curcar].chassis.description);
							if(dialog.display() == 0) buy++;
						}
						else
						{
							int price_buy = player.car.getTotalPrice();
							int price_dealer = price;
							price_buy *= 1.0+((player.car.getPrestigeMultiplier()-1.0)*PRESTIGE_RATIO_SELL);
							price_buy *= PRICERATIO_BUY_TI;
							price -= price_buy;

							if(price <= player.getMoney())
							{
								if(price >= (price_dealer*0.2))
								{
									Dialog dialog = new NoYesDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_WIDE, "TRADE-IN CARS", "Our experts priced your car at $" + price_buy + ". Do you want to trade it in and buy this " + cars[curcar].toString()  + " for $" + price + " ? \n \n About the " + cars[curcar].toString()+ ": " + cars[curcar].chassis.description);
									if(dialog.display() == 0)
									{
										if(!player.car.isDriveable()) buy++;
										else new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "BROKEN CAR", "Sorry, our dealership only accept cars that don't need a tow truck to travel. Please, repair your car or bring an another one.").display();
									}
								}
								else new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "UNPROFITABLE DEAL", "Our dealership cannot accept your car, because it's too expensive to offer you a trade-in deal for a vehicle you desired.").display();
							}
							else
							{
								String str = "$" + price;
								if(price <= 0) str = "enough money";
								new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "NOT ENOUGH MONEY", "You don't have " + str + " to pay for this car!").display();
							}
						}

						if(buy>0)
						{
							if(GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) multiplayer.spendMoney(price);
							
							player.takeMoney(price);
							refreshMoneyString();

							if(tradein)
							{
								releasePlayerCar();
								player.carlot.lockPlayerCar();
								player.carlot.releasePlayerCar();
								lockPlayerCar();
								player.carlot.saveCar(player.carlot.curcar);
								player.car = cars[curcar];
								player.carlot.flushCars();
								GameLogic.garage.releaseCar();
							}
							else
							{
								releasePlayerCar();
								player.carlot.lockPlayerCar();
								player.carlot.saveCar(player.carlot.curcar);
								player.carlot.flushCars();

								player.car=cars[curcar];
								GameLogic.garage.releaseCar();
							}

							cars[curcar] = null;

							for(int i=0; i<carDescriptors.length; i++)
							{
								if(player.car)
								{
									if(carDescriptors[i] && player.car.getInfo(GII_TYPE) == carDescriptors[i].id) carDescriptors[i] = null;
								}
							}

							Frontend.loadingScreen.display(new SimpleLoadingDialog(Dialog.DF_FULLSCREEN|Dialog.DF_MODAL, new ResourceRef(sellTextureId)), 5.0);
							osdCommand(Input.AXIS_CANCEL);
						}
					}
					else
					{
						new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "CAR LOT FULL", "There is no more free space in your car lot.\nSell some cars to free up parking space!").display();
					}
				}
				else
				{
					new WarningDialog(player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "NOT ENOUGH MONEY", "You don't have $" + price + " to buy this car!").display();
				}
			}
		}
	}
}

public class DealerData
{
	Vector3[]	carPos;
	Ypr[]		carOri;
}

public class DealerNewData extends DealerData
{
	public DealerNewData()
	{
		carPos = new Vector3[7];
		carOri = new Ypr[7];

		carPos[0] = new Vector3	( 3.901, 0.000,  -3.499);
		carOri[0] = new Ypr	(-1.963, 0.000,   0.000);
		carPos[1] = new Vector3	( 3.901, 0.000,  -7.603);
		carOri[1] = new Ypr	(-1.963, 0.000,   0.000);
		carPos[2] = new Vector3	( 5.099, 0.000, -12.927);
		carOri[2] = new Ypr	(-1.876, 0.000,   0.000);
		carPos[3] = new Vector3	(10.922, 0.000, -11.930);
		carOri[3] = new Ypr	(-2.923, 0.000,   0.000);
		carPos[4] = new Vector3	(17.295, 0.000, -13.173);
		carOri[4] = new Ypr	( 1.981, 0.000,   0.000);
		carPos[5] = new Vector3	(17.226, 0.000,  -7.603);
		carOri[5] = new Ypr	( 1.981, 0.000,   0.000);
		carPos[6] = new Vector3	(17.226, 0.000,  -3.267);
		carOri[6] = new Ypr	( 1.981, 0.000,   0.000);
	}
}

public class DealerSHData extends DealerData
{
	public DealerSHData()
	{
		carPos = new Vector3[9];
		carOri = new Ypr[9];

		carPos[0] = new Vector3	(  0.380, 0.000,   3.916);
		carOri[0] = new Ypr	(  2.618, 0.000,   0.000);
		carPos[1] = new Vector3	(-11.733, 0.000,  -2.666);
		carOri[1] = new Ypr	( -1.552, 0.000,   0.000);
		carPos[2] = new Vector3	(-11.733, 0.000,  -8.452);
		carOri[2] = new Ypr	( -1.552, 0.000,   0.000);
		carPos[3] = new Vector3	(-11.733, 0.000, -14.563);
		carOri[3] = new Ypr	( -1.552, 0.000,   0.000);
		carPos[4] = new Vector3	(-11.733, 0.000, -20.468);
		carOri[4] = new Ypr	( -1.552, 0.000,   0.000);
		carPos[5] = new Vector3	( -4.475, 0.000, -20.492);
		carOri[5] = new Ypr	(  1.592, 0.000,   0.000);
		carPos[6] = new Vector3	( -4.475, 0.000, -14.101);
		carOri[6] = new Ypr	(  1.592, 0.000,   0.000);
		carPos[7] = new Vector3	( -4.475, 0.000,  -8.507);
		carOri[7] = new Ypr	(  1.592, 0.000,   0.000);
		carPos[8] = new Vector3	( -4.475, 0.000,  -2.775);
		carOri[8] = new Ypr	(  1.592, 0.000,   0.000);
	}
}

public class DealerTIData extends DealerData
{
	public DealerTIData()
	{
		carPos = new Vector3[6];
		carOri = new Ypr[6];

		carPos[5] = new Vector3	(  0.660, 0.011,  2.239);
		carOri[5] = new Ypr	(  2.086, 0.001, -0.005);
		carPos[4] = new Vector3	( 11.050, 0.012,  1.724);
		carOri[4] = new Ypr	(  2.135, 0.001, -0.005);
		carPos[3] = new Vector3	(-10.342,-0.075,-15.170);
		carOri[3] = new Ypr	( -2.748, 0.004,  0.000);
		carPos[2] = new Vector3	( -5.588,-0.069,-14.380);
		carOri[2] = new Ypr	(  2.359, 0.002, -0.004);
		carPos[1] = new Vector3	( -9.728,-0.030, -5.897);
		carOri[1] = new Ypr	( -2.329, 0.003,  0.002);
		carPos[0] = new Vector3	(-14.284, 0.021,  5.487);
		carOri[0] = new Ypr	( -1.931, 0.001,  0.003);
	}
}