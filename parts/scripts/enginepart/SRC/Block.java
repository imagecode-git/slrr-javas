package java.game.parts.enginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.game.parts.enginepart.airfueldeliverysystem.*;

public class Block extends EnginePart
{
	float	inertia			= 1.0;	//ToDo: calc from components
	float	friction_fwd	= 1.0;
	float	friction_rev	= 1.0;
	float	rpm_idle		= 750.0;
	float	time_spark_min	= 0.5;
	float	time_spark_inc	= 0.0;
	float	time_spark_RPM0	= 0;
	float	time_spark_RPM1	= 1;
	float	RPM_limit		= 6500;
	int		cylinders		= 4;
	float	cylinder_length_from_top		= 0.0; // in milimeters //
	float	crank_center_to_cylinder_top	= 0.0; // in milimeters //
	float	total_sliction_loss				= 1.0; // multiplier //

	static float	in_breather_magic = 0.900;
	static float	ex_breather_magic = 0.900;

	int	crankshaft_slot_ID = 0;
	int	crankshaft_bearing_slot_ID = 0;
	int	transmission_slot_ID = 0;
	int	oil_pan_slot_ID = 0;

	int	skip_end_of_update = 0;

	float	bore = 0.0;

	DynoData	dynodata;//= new DynoData();

	int		SFX_trans_fwd	= sound:0x00000007r;
	int		SFX_trans_rev	= sound:0x00000009r;
	int		SFX_ignition	= sound:0x0000000Ar;

	float	rpm_trans_fwd	= 700.0;
	float	rpm_trans_rev	= 700.0;
	float	sfx_starter_rpm	= 200.0;

	int[]		redlineRecColors = new int[3];
	String[]	redlineRecTexts = new String[3];

	int[]		idleRecColors = new int[2];
	String[]	idleRecTexts = new String[2];

	public Block( int id )
	{
		super( id );

		name = "engine block";

		dynodata = new DynoData();
		dynodata.clear();

		prestige_calc_weight = 35.0;

		redlineRecColors[0] = 0xFFFFFFFF;
		redlineRecColors[1] = 0xFFFF0000;
		redlineRecColors[2] = 0xFFFFFF00;

		redlineRecTexts[0] = "";
		redlineRecTexts[1] = " (over destruction point)";
		redlineRecTexts[2] = " (over peak power point)";

		idleRecColors[0] = 0xFFFFFFFF;
		idleRecColors[1] = 0xFFFFFF00;

		idleRecTexts[0] = "";
		idleRecTexts[1] = " (torque may not be enough)";
	}

	public Part getTransmission()
	{
		if (transmission_slot_ID <= 0)
			return null;

		Part res = partOnSlot(transmission_slot_ID);

		if (res && res instanceof Transmission)
			return res;
//		else
//			System.log("!!!Transmission required on slot!!!");

		return null;
	}

	public Part getCrankshaft()
	{
		if (crankshaft_slot_ID <= 0)
			return null;

		Part res = partOnSlot(crankshaft_slot_ID);

		if (res && res instanceof Crankshaft)
			return res;
//		else
//			System.log("!!!Crankshaft required on slot!!!");

		return null;
	}

	public Part getCrankshaftBearing()
	{
		if (crankshaft_bearing_slot_ID <= 0)
			return null;

		Part res = partOnSlot(crankshaft_bearing_slot_ID);

		if (res && res instanceof SlidingEnginePart)
			return res;
//		else
//			System.log("!!!Crankshaft bearing bridge required on slot!!!");

		return null;
	}

	public Part getOilPan()
	{
		if (oil_pan_slot_ID <= 0)
			return null;

		Part res = partOnSlot(oil_pan_slot_ID);

		if (res && res instanceof OilPan)
			return res;
//		else
//			System.log("!!!Oil pan required on slot!!!");

		return null;
	}

	//RAXAT: build 932, returns 1 if the engine has no forced induction
	public int naturallyAspirated()
	{
		if(dynodata.rpm_turbo_mul > 0) return 0;
		return 1;
	}

	public void updatevariables()
	{
		getCar_LocalVersion();

		if (!the_car)
			return;

		if (crankshaft_slot_ID)
		{
			if (getCrankshaftBearing())
				disableSlot(crankshaft_slot_ID,1);
			else
				disableSlot(crankshaft_slot_ID,0);
		}

		if (oil_pan_slot_ID)
		{
			if (getOilPan())
				disableSlot(crankshaft_slot_ID,1);
			else
				disableSlot(crankshaft_slot_ID,0);
		}

//		System.log("------------------------------");
//		System.log("Block updatevariables() called");
//		System.log("------------------------------");

		super.updatevariables();

		inertia = 0.0;
		total_sliction_loss = 1.0;

		// dyno stuff //
		dynodata.clear();
		dynodata.maxRPM		= -1.0;//30000.0;

		dynodata.bore		= bore*0.001;

		dynodata.time_spark_min	= time_spark_min;
		dynodata.time_spark_inc	= time_spark_inc;
		dynodata.time_spark_RPM0= time_spark_RPM0;
		dynodata.time_spark_RPM1= time_spark_RPM1;
		dynodata.RPM_limit	= RPM_limit;

		dynodata.cylinders	= cylinders;

		Piston piston=null;
		ConnectingRod connecting_rod=null;
		Crankshaft crank=getCrankshaft();
		if (crank)
		{
			total_sliction_loss *= crank.getSlictionLoss();
			inertia += crank.getInertia();

			crank.the_car = the_car;
			crank.fillDynoData(dynodata,slotIDOnSlot(crankshaft_slot_ID)); // sets maxRPM and the_car for the whole subtree //

			dynodata.stroke = crank.stroke*0.001;
			connecting_rod=crank.getConnectingRod();

			if (connecting_rod)
			{
				total_sliction_loss *= connecting_rod.getSlictionLoss();
				inertia += connecting_rod.getInertia();
				piston = connecting_rod.getPiston();

				if (piston)
				{
					total_sliction_loss *= piston.getSlictionLoss();
					inertia += piston.getInertia();
				}
			}
		}

		SlidingEnginePart crank_bearing=getCrankshaftBearing();
		if (crank_bearing)
			total_sliction_loss *= crank_bearing.getSlictionLoss();

		// transmission //
		Transmission tranny = getTransmission();
		if (tranny)
		{
			tranny.fillDynoData(dynodata,transmission_slot_ID);
//			total_sliction_loss *= tranny.getSlictionLoss(); // szandekos, ugyanis a motor nem hajtaja allandoan a valtot
//			drive_train_inertia += tranny.getInertia();
		}

		if (skip_end_of_update)
			skip_end_of_update -= 1;
		else
		{
			if (total_sliction_loss < 0.001)
				total_sliction_loss = 0.001;

			if (total_sliction_loss > 1.0)
				total_sliction_loss = 1.0;

			// lubrication //
			OilPan oilpan = getOilPan();
			if (oilpan)
			{
				oilpan.updatevariables();

				friction_fwd = oilpan.friction_fwd/total_sliction_loss;
				friction_rev = oilpan.friction_rev/total_sliction_loss;
			}
			else
			{
				friction_fwd = 1.0;
				friction_rev = 1.0;
			}

//			System.log(">>"+name+", BLOCK.JAVA<<:");
//			System.log("  friction_fwd = "+friction_fwd);
//			System.log("  friction_rev = "+friction_rev);
//			System.log("  inertia = "+inertia);
//			System.log("-------------------------------");

		}
	}

	public void check4warnings()
	{
/*		super.check4warnings();
		if (!crankshaft_slot_ID)
			System.log("   crankshaft_slot_ID is 0");
		if (!crankshaft_bearing_slot_ID)
			System.log("   crankshaft_bearing_slot_ID is 0");
		if (!oil_pan_slot_ID)
			System.log("   oil_pan_slot_ID is 0");
*/	}

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			rpm_idle = saveGame.readFloat();
		}
		if (save_ver >= 2)
		{
			RPM_limit = saveGame.readFloat();
		}
		if (save_ver >= 3)
		{
			time_spark_min	= saveGame.readFloat();
			time_spark_inc	= saveGame.readFloat();
			time_spark_RPM0	= saveGame.readFloat();
			time_spark_RPM1	= saveGame.readFloat();
		}
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 3;

		saveGame.write( save_ver );
		if (save_ver >= 1)
		{
			saveGame.write( rpm_idle );
		}
		if (save_ver >= 2)
		{
			saveGame.write( RPM_limit );
		}
		if (save_ver >= 3)
		{
			saveGame.write( time_spark_min );
			saveGame.write( time_spark_inc );
			saveGame.write( time_spark_RPM0 );
			saveGame.write( time_spark_RPM1 );
		}
	}

	public String isDynoable()
	{
		Part p;

		p = getCrankshaft();
		if (!p && crankshaft_slot_ID)
			return "it's missing the crankshaft.";

		p = getCrankshaftBearing();
		if (!p && crankshaft_bearing_slot_ID)
			return "it's missing the crankshaft bearing bridge.";

		p = getOilPan();
		if (!p && oil_pan_slot_ID)
			return "it's missing the oil pan.";

		p = getTransmission();
		if (!p && transmission_slot_ID)
			return "it's missing the transmission.";

		if (dynodata.Compression < dynodata.minCompression)
			return "the static compression is too low in the engine. At least "+Float.toString(dynodata.minCompression, "%1.1f:1")+" is needed. Try adjusting the air/fuel mixture ratio or lowering the compression by replacing cylinder head(s), connecting rods or pistons.";

		if (dynodata.Compression > dynodata.maxCompression)
			return "the static compression is too high in the engine. Not more than "+Float.toString(dynodata.maxCompression, "%1.1f:1")+" is allowed. Try adjusting the air/fuel mixture ratio or lowering the compression by replacing cylinder head(s), connecting rods or pistons.";

		return super.isDynoable();
	}

	//---------tuning
	public int isTuneable()
	{
		return 1;
	}

	// backup values //
	float	old_rpm_idle;
	float	old_RPM_limit;

	public int redlineToRecommendation()
	{
		if (RPM_limit > dynodata.maxRPM)
			return 1;
		else
		if (RPM_limit > dynodata.RPM_maxHP)
			return 2;

		return 0;
	}

	public int idleToRecommendation()
	{
		if (dynodata.getTorque(rpm_idle, 0.0) < dynodata.maxTorque*0.10) // signal under 10 Nm torque at idle //
			return 1;
		return 0;
	}

	public void buildTuningMenu( Menu m )
	{
		Slider s;

		old_rpm_idle = rpm_idle;
		old_RPM_limit = RPM_limit;

		int ir = idleToRecommendation();
		s = m.addItem( "Idle", 1, rpm_idle, 150.0, 2500.0, (2500-150)/50+1, null ); // in 50 steps
		s.changeVLabelText( Float.toString(rpm_idle, "   %1.0f RPM")+idleRecTexts[ir] );
		s.changeVLabelColor(idleRecColors[ir]);

		int rr = redlineToRecommendation();
		s = m.addItem( "Redline", 2, RPM_limit, 3000.0, 12000.0, (12000-3000)/250+1, null ); // in 250 steps
		s.changeVLabelText( Float.toString(RPM_limit, "   %1.0f RPM")+redlineRecTexts[rr] );
		s.changeVLabelColor(redlineRecColors[rr]);
	}

	public void endTuningSession( int cancelled )
	{
		if( cancelled )
		{
			rpm_idle = old_rpm_idle;
			RPM_limit = old_RPM_limit;
		}
		else
		{
			if (old_rpm_idle != rpm_idle)
				GameLogic.spendTime(4*60);
			if (old_RPM_limit != RPM_limit)
				GameLogic.spendTime(8*60);
			getCar_LocalVersion();
			if (the_car)
				the_car.forceUpdate();
		}
	}

	public void handleMessage( Event m )
	{
		if( m.cmd == 1 )
		{
			rpm_idle = ((Slider)m.gadget).value;
			int ir = idleToRecommendation();
			((Slider)m.gadget).changeVLabelText( Float.toString(rpm_idle, "   %1.0f RPM")+idleRecTexts[ir] );
			((Slider)m.gadget).changeVLabelColor(idleRecColors[ir]);
		}
		if( m.cmd == 2 )
		{
			RPM_limit = ((Slider)m.gadget).value;
			int rr = redlineToRecommendation();
			((Slider)m.gadget).changeVLabelText( Float.toString(RPM_limit, "   %1.0f RPM")+redlineRecTexts[rr] );
			((Slider)m.gadget).changeVLabelColor(redlineRecColors[rr]);
		}
	}
	//---------tuning
}