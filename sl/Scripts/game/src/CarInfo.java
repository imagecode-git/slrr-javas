package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.block.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.camshaft.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.charger.*;

public class CarInfo extends GameType implements GameState
{
	// resource ID constants
	final static int  RID_CAR_BG = frontend:0x0095r;
	final static int  RID_ENGINE_BG = frontend:0x0087r;
	final static int  RID_FINANCIAL_BG = frontend:0x0097r;
	final static int  RID_RECORDS_BG = frontend:0x0096r;

	// commands
	final static int	CMD_CAR_PAGE = 1;
	final static int	CMD_ENGINE_PAGE = 2;
	final static int	CMD_FINANCIAL_PAGE = 3;
	final static int	CMD_RECORDS_PAGE = 4;
	final static int	CMD_EXIT = 5;
	final static int	CMD_SCROLL_UP = 6;
	final static int	CMD_SCROLL_DOWN = 7;

	// 408,718 - 914,292
	// -0.20, 0.40 - 0.78, -0.43
	final static float graphX = -0.205;
	final static float graphY = 0.395;
	final static float graphW = 1.02;
	final static float graphH = 0.85;
	
	float			graphRPMMin = 0.00;
	float			graphRPMMax = 10000.00;
	float			graphHPMin = 0.00;
	float			graphHPMax = 900.00;
	float			graphTorqueMin = 0.00;	//ft-lbs!!
	float			graphTorqueMax = 600.00;	//ft-lbs!!

	GameState		parentState;

	Osd			osd;

	Vehicle			car;

	int			carGroup, engineGroup, financialGroup, recordsGroup, actGroup;
	int			firstPart;
	int			nParts;

	Text[]			partText = new Text[100];
	ResourceRef		graphFont;

	public CarInfo( Vehicle car_ )
	{
		createNativeInstance();

		car=car_;
	}

	public void page( int pg )
	{
		if( actGroup != pg )
		{
			osd.hideGroup( carGroup );
			osd.hideGroup( engineGroup );
			osd.hideGroup( financialGroup );
			osd.hideGroup( recordsGroup );
			actGroup = pg;
			osd.showGroup( actGroup );
		}
	}

	public void enter( GameState prevState )
	{
		parentState=prevState;

		osd = new Osd();
		osd.globalHandler = this;

		firstPart = 0;
		nParts = 0;
		createOSDObjects();
		osd.show();

		Input.cursor.enable(1);

		setEventMask( EVENT_CURSOR );
	}

	public void exit( GameState nextState )
	{
		clearEventMask( EVENT_ANY );

		Input.cursor.enable(0);
		osd.hide();
		deleteOSDObjects();
		parentState=null;
	}

	public int wearColor( float f )
	{
		int	color;

		if( f > 1.0 )
			f = 1.0;
		if( f < 0.0 )
			f = 0.0;

		if( f >= 0.5 )
		{
			color = ((1.0 - f) * 2.0) * 0xFF;
			color = ((color & 0xFF) << 16) + 0xFF00FF00;
		}
		else
		{
			color = (f * 2.0) * 0xFF;
			color = ((color & 0xFF) << 8) + 0xFFFF0000;
		}
		return color;
	}

	public float listParts( Vehicle car, int first )
	{
		int i;
		int	iv;
		float fv;

		float totalValue;
		nParts = 0;

		if( car.iteratePartsInit() )
		{
			Part part;
			while( part = car.iterateParts() )
			{
				float value = part.currentPriceNoAttach();
				float SILfine = part.police_check_fine_value;
				if( first > 0 )
				{
					first--;
				}
				else
				{
					if( i < 100 )
					{
						partText[i++].changeText( part.name );
						fv = part.getWear();
						iv = fv * 100.0;
						partText[i].changeColor( wearColor( fv ) );
						partText[i++].changeText( iv + "%" );
						fv = part.getTear();
						iv = fv * 100.0;
						partText[i].changeColor( wearColor( fv ) );
						partText[i++].changeText( iv + "%" );
						if (SILfine>0.0)
						{
							iv = SILfine;
							partText[i++].changeText( "$" + iv );
						}
						else
							partText[i++].changeText( "" );
						iv = value;
						partText[i++].changeText( "$" + iv );
					}
				}
				totalValue += value;
				nParts++;
			}
		}

		while( i < 100 )
		{
			partText[i++].changeText( "" );
		}

		return totalValue;
	}

	public void createButtons()
	{
		//osd.createRectangle( 1.01, -0.82, 1.2, 0.22, -1, new ResourceRef(frontend:0x0024r) ); //button holder

		Style buttonStyle = new Style( 0.11, 0.11, Frontend.smallFont, Text.ALIGN_LEFT, null );
		Menu m = osd.createMenu( buttonStyle, 0.495, -0.93, 0, Osd.MD_HORIZONTAL );

		m.addItem( new ResourceRef( frontend:0x00009C11r ), CMD_CAR_PAGE, "Main car information", null, 1 );
		m.addItem( new ResourceRef( frontend:0x00009C10r ), CMD_ENGINE_PAGE, "Engine information", null, 1 );
		m.addItem( new ResourceRef( frontend:0x00009C0Ar ), CMD_FINANCIAL_PAGE, "Detailed parts information", null, 1 );
		m.addItem( new ResourceRef( frontend:0x00009C08r ), CMD_RECORDS_PAGE, "Records", null, 1 );
		m.addSeparator();
		m.addSeparator();

		if(parentState instanceof Garage) m.addItem( new ResourceRef( frontend:0x00009C0Er ), CMD_EXIT, "Back to garage", null, 1 );
			else m.addItem( new ResourceRef( frontend:0x00009C02r ), CMD_EXIT, "Exit", null, 1 );

		osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL|Osd.HK_STATIC, CMD_EXIT, this );
	}

	public String driveType()
	{
		int dt = car.getInfo( 52/*GII_CAR_DRIVETYPE*/ );
		if( dt == 0 )
			return "No wheel drive";
		else if( dt == 1 )
			return "All wheel drive";
		else if( dt == 2 )
			return "Front wheel drive";
		else if( dt == 3 )
			return "Rear wheel drive";
		else if( dt == 4 )
			return "Cross wheel drive";

		return "?";
	}

	public String exists( int ex )
	{
		if( ex == 0 )
			return "Not exists";

		return "Exists";
	}

	public void createOSDObjects()
	{
		int		i, val, line;
		float	xpos, xpos2, xpos3, ypos, fval, gx, gy;
		String	name;

		graphFont = Frontend.smallFont;

		float fontCenter = ( (1.2*osd.createText( "", graphFont, Text.ALIGN_LEFT, 0, 0).getFontSize( graphFont ))/(Config.video_y * osd.getViewport().getHeight()) );

		Chassis chas = car.chassis;

		//-------------------------------------------------
		osd.createBG( new ResourceRef(RID_CAR_BG) );
		osd.createHeader( "CAR INFORMATION" );
		createButtons();

		if( chas )
			name = chas.name.token(0);
		else
			name = "undefined";

		Vector3	CM = chas.getCM();		//ToDo: display
		Vector3	Min = chas.getMin();
		Vector3	Max = chas.getMax();

		int	wheels = chas.getWheels();
		Vector3[]	WP = new Vector3[wheels];
		float[]		R = new float[wheels];
		for (int i=0; i<wheels; i++)
		{
			WheelRef whl = chas.getWheel(i);

			WP[i] = whl.getPos();//chas.getWheelPos(i);
			R[i] = whl.getRadius();
		}

		xpos = -0.94;
		xpos2 = -0.5;
		ypos = -0.53;
		osd.createText( chas.vehicleName, Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line ); line++;
		line++;
//		osd.createText( "Drag:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
//		osd.createText( chas.C_drag, Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;

		fval = car.chassis.getMileage();
		osd.createText( "Mileage:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( (int)(fval*0.01) +" km", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line++ );
		osd.createText( (int)(fval*0.00621) + " mi", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line++ );

		val = chas.getMassPatch(); //RAXAT: v2.3.1, fixed wrong vehicle mass calculation (see Chassis.class for details)
		osd.createText( "Mass:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( val + " kg", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		osd.createText( Float.toString(val*2.2, "%1.0f pounds"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		line++;
		osd.createText( "Chassis length:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( Float.toString((Max.z - Min.z)*1000.0, "%1.0f mm"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		osd.createText( Float.toString((Max.z - Min.z)*100.0/2.54, "%1.1f inch"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		osd.createText( "Chassis width:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( Float.toString((Max.x - Min.x)*1000.0, "%1.0f mm"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		osd.createText( Float.toString((Max.x - Min.x)*100.0/2.54, "%1.1f inch"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		osd.createText( "Chassis height:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( Float.toString((Max.y - Min.y)*1000.0, "%1.0f mm"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		osd.createText( Float.toString((Max.y - Min.y)*100.0/2.54, "%1.1f inch"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		osd.createText( "Ground clearance:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( Float.toString((R[0]-WP[0].y+Min.y)*1000.0, "%1.0f mm"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		osd.createText( Float.toString((R[0]-WP[0].y+Min.y)*100.0/2.54, "%1.1f inch"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
		line++;
		osd.createText( "Powertrain layout:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line ); line++;
		osd.createText( "   " + driveType(), Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line ); line++;
		line++;
		/*
			osd.createText( "Vehicle class:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
			CareerEvent c = new CareerEvent();
			osd.createText( "" + c.getClass(c.checkClass(car)), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line ); line++;
			c=null;
			line++;
		*/

		fval = WP[1].x - WP[0].x;
		osd.createText( "Front Track", Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.075, -0.155, 0 ).setColor(0xFFE6313E);
		osd.createText( Float.toString(fval*1000.0,"%1.0f mm"), Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.08, -0.15, 1 ).setColor(0xFFE6313E);
		osd.createText( Float.toString(fval*100.0/2.54,"%1.1f inch"), Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.08, -0.15, 2 ).setColor(0xFFE6313E);
		fval = WP[3].x - WP[2].x;
		osd.createText( "Rear Track", Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.685, -0.155, 0 ).setColor(0xFFE6313E);
		osd.createText( Float.toString(fval*1000.0,"%1.0f mm"), Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.68, -0.15, 1 ).setColor(0xFFE6313E);
		osd.createText( Float.toString(fval*100.0/2.54,"%1.1f inch"), Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.68, -0.15, 2 ).setColor(0xFFE6313E);
		fval = (WP[2].z+WP[3].z)*0.5 - (WP[0].z+WP[1].z)*0.5;
		osd.createText( "Weight distribution", Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.35, 0.50, 0 ).setColor(0xFFE6313E);
		osd.createText( "Wheelbase", Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.35, 0.50, 1 ).setColor(0xFFE6313E);
		osd.createText( Float.toString(fval*1000.0,"%1.0f mm"), Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.35, 0.50, 2 ).setColor(0xFFE6313E);
		osd.createText( Float.toString(fval*100.0/2.54,"%1.1f inch"), Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.35, 0.50, 3 ).setColor(0xFFE6313E);
		float fw = ((WP[0].z+WP[1].z)*0.5)-CM.z;
		float rr = ((WP[2].z+WP[3].z)*0.5)-CM.z;
		fval = -fw/(rr-fw);
		osd.createText( Float.toString((1.0-fval)*100.0,"%1.1f%%"), Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.07, 0.50 ).setColor(0xFFE6313E);
		osd.createText( Float.toString(fval*100.0,"%1.1f%%"), Frontend.smallFont_strong, Text.ALIGN_CENTER, 0.6425, 0.50 ).setColor(0xFFE6313E);

		osd.hideGroup( carGroup = osd.endGroup() );

		//-------------------------------------------------
		osd.createBG( new ResourceRef(RID_ENGINE_BG) );
		osd.createHeader( "ENGINE INFORMATION" );
		createButtons();

		int engineInstalled;
		Block engine;
		int its_vee=0;
		int has_crank=0;
		int has_pistons=0;
		int has_heads=0;

		xpos2 = -0.53;
		xpos = -0.95;
		ypos = -0.53;
		line = 0;

		//megkeressuk a motorblokkot, hogy tobb adatot kapjunk
		if( car.iteratePartsInit() )
		{
			Part part;
			while( part = car.iterateParts() )
			{
				if ( part instanceof Block )
				{
					engine = part;
					if (part instanceof Block_Vee )
						its_vee = 1;
				} 
				else
				if ( part instanceof Crankshaft ) 
					has_crank = 1; 
				else
				if ( part instanceof Piston ) 
					has_pistons = 1; 
				else
				if ( part instanceof CylinderHead )
					has_heads++;
			}
		}

		int comp_ok=0;

		/*
		System.log("its_vee="+its_vee);
		System.log("has_heads="+has_heads);
		System.log("has_pistons="+has_pistons);
		System.log("comp_ok="+comp_ok);
		*/

		if (its_vee && has_heads==2)
			comp_ok=1;
		else
		if (!its_vee && has_heads==1)
			comp_ok=1;

		if (comp_ok && has_pistons)
			comp_ok=1;
		else
			comp_ok=0;

		String error_text = null;

		DynoData dyno = null;

		if( engine )
		{
			if( car.iteratePartsInit() )
			{
				Part part;
				while( part = car.iterateParts() )
				{
					error_text = part.isDynoable();
					if (error_text)
						break;
				}
			}

			dyno = engine.dynodata;

			val = dyno.cylinders;
			osd.createText( "Cylinders:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			osd.createText( val, Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
			val = dyno.Displacement*1000000.0;
			osd.createText( "Displacement:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			osd.createText( val +  " cc ", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
			osd.createText( "Bore:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			osd.createText( Float.toString(dyno.bore*1000.0, "%1.1f mm"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;

			osd.createText( "Stroke:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			if (has_crank)
				osd.createText( Float.toString(dyno.stroke*1000.0, "%1.1f mm"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			else
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			line++;

			osd.createText( "Static comp.:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			if (comp_ok)
				osd.createText( Float.toString(dyno.Compression, "%1.1f:1"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			else
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			line++;

			osd.createText( "HP/liter:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			if (error_text)
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			else
			{
				//RAXAT: v2.3.1, division by zero patch
				int displ = dyno.Displacement*1000.0;
				float ratio;

				if(displ>0) ratio = dyno.maxHP/displ;
				else ratio = 0.0f;
				osd.createText( Float.toString(ratio, "%1.1f"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			}
			line++;

			osd.createText( "kg/HP:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			if (error_text)
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			else
			{
				//RAXAT: v2.3.1, division by zero patch
				int hp = dyno.maxHP;
				float ratio;

				if(hp>0) ratio = chas.getMass()/hp;
				else ratio = 0.0f;
				osd.createText( Float.toString(ratio, "%1.3f"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			}
			line++;

			osd.createText( "kg/Nm:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			if (error_text)
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			else
			{
				//RAXAT: v2.3.1, division by zero patch
				int torque = dyno.maxTorque/10.0;
				float ratio;

				if(torque>0) ratio = chas.getMass()/torque;
				else ratio = 0.0f;
				osd.createText( Float.toString(ratio, "%1.3f"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			}
			line++;

			line++;
			osd.createText( "Gears:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line); line++;
			String[] gear_names = new String[6];
			gear_names[0] = " 1st:";
			gear_names[1] = " 2nd:";
			gear_names[2] = " 3rd:";
			gear_names[3] = " 4th:";
			gear_names[4] = " 5th:";
			gear_names[5] = " 6th:";
			for ( i = 1; i <= chas.gears; i++ )
			{
				osd.createText( gear_names[i-1], Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
				osd.createText( chas.ratio[i], Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
			}
			osd.createText( " Reverse:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			osd.createText( -chas.ratio[7], Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
			osd.createText( " End ratio:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			osd.createText( chas.rearend_ratio+":1", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
			line++;
			val = chas.engine_rpm_idle;
			osd.createText( "Idle:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			osd.createText( val+" RPM", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
			val = chas.RPM_limit;
			osd.createText( "Redline:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			osd.createText( val+" RPM", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
			val = dyno.maxRPM;
			osd.createText( "Engine destruction:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			osd.createText( val+" RPM", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;

			osd.createText( "Peak flywheel torque:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			if (error_text)
			{
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			}
			else
			{
				osd.createText( Float.toString(dyno.maxTorque*0.7353, "%1.0f ft-lbs"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
				osd.createText( Float.toString(dyno.maxTorque, "%1.0f Nm"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			}
			val = dyno.RPM_maxTorque;

			if (!error_text)
				osd.createText( " at "+val+" RPM", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			line++;

			osd.createText( "Peak flywheel power:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);
			if (error_text)
			{
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
				osd.createText( "N/A", Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			}
			else
			{
				osd.createText( Float.toString(dyno.maxHP, "%1.0f HP"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line); line++;
				osd.createText( Float.toString(dyno.maxHP*0.7355, "%1.0f KW"), Frontend.smallFont, Text.ALIGN_LEFT, xpos2, ypos, line);
			}
			val = dyno.RPM_maxHP;

			if (!error_text)
				osd.createText( " at "+val+" RPM", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);

			line++;
			line++;

			osd.createText( "Fuel type:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line); line++;
			osd.createText( " "+dyno.fuelType, Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line); line++;
		}
		else
			osd.createText( "No engine installed!", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line);

		if (error_text)
			osd.createTextBox( "No dyno info is available, because "+error_text, Frontend.smallFont, Text.ALIGN_LEFT, graphX, ypos, graphW );
		else
			if (dyno)
			{
				graphRPMMin = 0.00;
				graphRPMMax = 5000.00;
				graphHPMin = 0.00;
				graphHPMax = 150.00;
				graphTorqueMin = 0.00;		//ft-lbs!!
				graphTorqueMax = 150.00;	//ft-lbs!!
				while (graphRPMMax < dyno.RPM_limit)
				{
					graphRPMMax += 5000;
				}
				while (graphHPMax < dyno.maxHP)
				{
					graphHPMax *= 2;
				}
				while (graphTorqueMax < dyno.maxTorque * 0.7376)
				{
					graphTorqueMax *= 2;
				}

				//value marks
				for (i = 1; i <= 6; i++)
				{
					gx = graphX + 0.03;
					gy = graphY - i * (graphH / 6.0) - fontCenter;
					val = i * (graphTorqueMax / 6.0);
					osd.createText( val, graphFont, Text.ALIGN_LEFT, gx, gy).changeColor(0xFFFFFFFF);
					gx = graphX + graphW + 0.03;
					val = i * (graphHPMax / 6.0);
					osd.createText( val, graphFont, Text.ALIGN_LEFT, gx, gy).changeColor(0xFFFFFFFF);
				}
				for (i = 0; i <= 10; i += 2)
				{
					gx = graphX + i * (graphW / 9.80) - 0.03;
					gy = graphY + fontCenter + 0.02;
					val = i * (graphRPMMax / 10.0);
					osd.createText( val, graphFont, Text.ALIGN_LEFT, gx, gy).changeColor(0xFFFFFFFF);
				}

				//graph lines
				float torque;
				float hp;
				float RPM;

				//horsepower line
				RPM = graphRPMMin;
				while(RPM<=graphRPMMax)
				{
					hp = dyno.getHP(RPM,0)*0.001f*1.341f;	//HP  (kW*1.341)
					if(hp<0) hp=0; //RAXAT: v2.3.1, sub-zero values patch
					gx = graphX+(RPM-graphRPMMin)/(graphRPMMax-graphRPMMin)*graphW;
					gy = graphY-(hp-graphHPMin)/(graphHPMax-graphHPMin)*graphH - fontCenter;
					osd.createText( ".", graphFont, Text.ALIGN_LEFT, gx, gy).changeColor(0xFF8080FF);

					RPM += 50.0;
				}

				//torque line
				RPM = graphRPMMin;
				while(RPM<=graphRPMMax)
				{
					torque = dyno.getTorque(RPM, 0.0) * 0.7376;	//normal ft-lbs!!
					if(torque<0) torque=0; //RAXAT: v2.3.1, sub-zero values patch
					gx = graphX+(RPM-graphRPMMin)/(graphRPMMax-graphRPMMin)*graphW;
					gy = graphY-(torque-graphTorqueMin)/(graphTorqueMax-graphTorqueMin)*graphH - fontCenter;
					osd.createText( ".", graphFont, Text.ALIGN_LEFT, gx, gy).changeColor(0xFFFF8080);

					RPM += 50.0;
				}
			}

		osd.hideGroup( engineGroup = osd.endGroup() );

		//-------------------------------------------------
		osd.createBG( new ResourceRef(RID_FINANCIAL_BG) );
		osd.createHeader( "FINANCIAL INFORMATION" );
		createButtons();

		ypos = -0.6125;
		osd.createText( "Name", Frontend.smallFont, Text.ALIGN_LEFT, -0.92, ypos);
		osd.createText( "Wear", Frontend.smallFont, Text.ALIGN_LEFT, 0.08, ypos);
		osd.createText( "Tear", Frontend.smallFont, Text.ALIGN_LEFT, 0.26, ypos);
		osd.createText( "IP fine", Frontend.smallFont, Text.ALIGN_RIGHT, 0.49, ypos);
		osd.createText( "Value", Frontend.smallFont, Text.ALIGN_RIGHT, 0.695, ypos);
		ypos += 0.11;

		for( i = 0; i < 100; )
		{
			partText[i] = osd.createText( "", Frontend.smallFont, Text.ALIGN_LEFT, -0.92, ypos);
			partText[i].changeColor(0xFFC0C0C0);
			i++;
			partText[i] = osd.createText( "", Frontend.smallFont, Text.ALIGN_LEFT, 0.08, ypos);
			partText[i].changeColor(0xFFC0C0C0);
			i++;
			partText[i] = osd.createText( "", Frontend.smallFont, Text.ALIGN_LEFT, 0.26, ypos);
			partText[i].changeColor(0xFFC0C0C0);
			i++;
			partText[i] = osd.createText( "", Frontend.smallFont, Text.ALIGN_RIGHT, 0.49, ypos);
			partText[i].changeColor(0xFFFF2020);
			i++;
			partText[i] = osd.createText( "", Frontend.smallFont, Text.ALIGN_RIGHT, 0.695, ypos);
			partText[i].changeColor(0xFFC0C0C0);
			i++;
			ypos+=0.05;
		}

		int totalValue = listParts( car, 0 );

		ypos += 0.0625;
		osd.createText( "Total value", Frontend.smallFont, Text.ALIGN_LEFT, -0.92, ypos);
		osd.createText( "$" + totalValue, Frontend.smallFont, Text.ALIGN_RIGHT, 0.695, ypos);

		Style btnUp = new Style( 0.13, 0.13, 1.0, Frontend.smallFont, Text.ALIGN_CENTER, new ResourceRef( Osd.RID_ARROWUP ) );
		osd.createButton( btnUp, 0.80, -0.45, CMD_SCROLL_UP, null, null );

		Style btnDn = new Style( 0.13, 0.13, 1.0, Frontend.smallFont, Text.ALIGN_CENTER, new ResourceRef( Osd.RID_ARROWDN ) );
		osd.createButton( btnDn, 0.80,  0.45, CMD_SCROLL_DOWN, null, null );


		osd.hideGroup( financialGroup = osd.endGroup() );

		//-------------------------------------------------
		osd.createBG( new ResourceRef(RID_RECORDS_BG) );
		osd.createHeader( "RECORDS INFORMATION" );
		createButtons();

		ypos = -0.63;
		line = 0;
		xpos2 = 0.275;
		xpos3 = -0.1;

		osd.createText( "Statistics for this "+chas.vehicleName+":", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line ); line++;
		line++;

		osd.createText( "Test Track best results:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line ); line++;

		osd.createText( " Top speed:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.maxTestTrackSpeedSq < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
		{
			osd.createText( Float.toString(Math.sqrt( car.maxTestTrackSpeedSq ) * 2.24 * 1.61, "%1.1f KPH"), Frontend.smallFont, Text.ALIGN_RIGHT, xpos3, ypos, line );
			osd.createText( Float.toString(Math.sqrt( car.maxTestTrackSpeedSq ) * 2.24, "%1.1f MPH"), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		}
		line++;

		osd.createText( " 0-100 KPH (0-62.1 MPH):", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.bestTestTrackAcc < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
			osd.createText( String.timeToString( car.bestTestTrackAcc, String.TCF_NOMINUTES ), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line);
		line++;

		osd.createText( " 0-200 KPH (0-124.2 MPH):", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.bestTestTrackAcc120 < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
			osd.createText( String.timeToString( car.bestTestTrackAcc120, String.TCF_NOMINUTES ), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		line++;

		osd.createText( " 1/4 mile time:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.bestTestTrackTime2 < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
			osd.createText( String.timeToString( car.bestTestTrackTime2, String.TCF_NOMINUTES ), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line);
		line++;

		osd.createText( " 1/4 mile speed:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.bestTestTrackTime2_speedSq < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
		{
			osd.createText( Float.toString(Math.sqrt( car.bestTestTrackTime2_speedSq ) * 2.24 * 1.61, "%1.1f KPH"), Frontend.smallFont, Text.ALIGN_RIGHT, xpos3, ypos, line );
			osd.createText( Float.toString(Math.sqrt( car.bestTestTrackTime2_speedSq ) * 2.24, "%1.1f MPH"), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		}
		line++;

		osd.createText( " 1 mile time:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.bestTestTrackTime0 < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
			osd.createText( String.timeToString( car.bestTestTrackTime0, String.TCF_NOMINUTES ), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line);
		line++;

		osd.createText( " 1 mile speed:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.bestTestTrackTime0_speedSq < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
		{
			osd.createText( Float.toString(Math.sqrt( car.bestTestTrackTime0_speedSq ) * 2.24 * 1.61, "%1.1f KPH"), Frontend.smallFont, Text.ALIGN_RIGHT, xpos3, ypos, line );
			osd.createText( Float.toString(Math.sqrt( car.bestTestTrackTime0_speedSq ) * 2.24, "%1.1f MPH"), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		}
		line++;

		osd.createText( " Best lap time:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.bestTestTrackTime1 < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
			osd.createText( String.timeToString( car.bestTestTrackTime1, String.TCF_NOHOURS ), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		ypos += 0.10;

		osd.createText( "Your race history for this car:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		line++;

		osd.createText( " Races run:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( car.races_won + car.races_lost, Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line ); line++;
		osd.createText( " Wins:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( car.races_won, Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line ); line++;
		osd.createText( " Loses:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( car.races_lost, Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line ); line++;
		osd.createText( " History prestige ratio:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		osd.createText( Float.toString(car.getPrestigeMultiplier()*100.0, "%1.1f %%"), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line ); line++;

		osd.createText( " Best night 1/4 mile time:", Frontend.smallFont, Text.ALIGN_LEFT, xpos, ypos, line );
		if( car.bestNightQM < 0.10 )
			osd.createText( "Not set", Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		else
			osd.createText( String.timeToString( car.bestNightQM, String.TCF_NOMINUTES ), Frontend.smallFont, Text.ALIGN_RIGHT, xpos2, ypos, line );
		line++;

		osd.hideGroup( recordsGroup = osd.endGroup() );

		actGroup = -1;
		page( carGroup );
	}	

	public void deleteOSDObjects()
	{
	}

//----------------------------------------------------------------------

	public void osdCommand (int command)
	{
		if (command < 0)
			return;
		else
		if (command == CMD_CAR_PAGE)
		{
			page( carGroup );
		}
		else
		if (command == CMD_ENGINE_PAGE)
		{
			page( engineGroup );
		}
		else
		if (command == CMD_FINANCIAL_PAGE)
		{
			page( financialGroup );
		}
		else
		if (command == CMD_RECORDS_PAGE)
		{
			page( recordsGroup );
		}
		else
		if (command == CMD_EXIT)
		{
			GameLogic.changeActiveSection( parentState );
		}
		else
		if( command == CMD_SCROLL_UP )
		{
			if( firstPart > 0 )
			{
				firstPart-=20;
				if( firstPart < 0 )
					firstPart = 0;

				listParts( car, firstPart );
			}
		}
		else
		if( command == CMD_SCROLL_DOWN )
		{
			if( firstPart < nParts - 20 )
			{
				firstPart+=20;
				if( firstPart > nParts - 20 )
					firstPart = nParts - 20;

				listParts( car, firstPart );
			}
		}
	}
}
