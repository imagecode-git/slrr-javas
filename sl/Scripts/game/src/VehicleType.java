package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

//describes a vehicle type's all models
public class VehicleType extends GameType
{
	//remove this asap!
	final static int VS_DEMO	= 0x0001;	//free ridehoz, quick racehez
	final static int VS_USED	= 0x0002;	//hasznaltautokereskedesbe
	final static int VS_STOCK	= 0x0004;	//ujautokereskedesbe
	final static int VS_DRACE	= 0x0008;	//day racehez
	final static int VS_NRACE	= 0x0010;	//night racehez
	final static int VS_RRACE	= 0x0020;	//roc racehez
	final static int VS_CIRCUIT	= 0x0040;	//MRM CR
	final static int VS_DTM		= 0x0080;	//MRM DTM
	final static int VS_SPONSOR	= 0x008A;	//MRM Sponsored Events
	final static int VS_CROSS	= 0x008F;	//MRM Cross Racing
	final static int VS_DRIFT	= 0x008E;	//MRM Drift

	//RAXAT: BV DESCRIPTOR PATCH
	final static int VS_DEMO_P	= 0x0090;
	final static int VS_USED_P	= 0x0091;
	final static int VS_STOCK_P	= 0x0092;
	final static int VS_DRACE_P	= 0x0093;
	final static int VS_NRACE_P	= 0x0094;
	final static int VS_RRACE_P	= 0x0095;
	final static int VS_CIRCUIT_P	= 0x0096;
	final static int VS_DTM_P	= 0x0097;
	final static int VS_SPONSOR_P	= 0x0098;
	final static int VS_CROSS_P	= 0x0099;
	final static int VS_DRIFT_P	= 0x009A;


	final static float	qm_stock_Baiern_CoupeSport_2_5 = 14.8294;
	final static float	qm_full_Baiern_CoupeSport_2_5 = 12.6816;
	final static float	qm_stock_Baiern_CoupeSport_GT_III = 9.9572;
	final static float	qm_full_Baiern_CoupeSport_GT_III = 8.6888;
	final static float	qm_stock_Baiern_DevilSport = 12.5978;
	final static float	qm_full_Baiern_DevilSport = 10.921;

	final static float	qm_stock_Duhen_Racing_SunStrip_2_0_CDVC = 11.448;
	final static float	qm_full_Duhen_Racing_SunStrip_2_0_CDVC = 10.139;
	final static float	qm_stock_Duhen_SunStrip_1_5_DVC = 13.9432;
	final static float	qm_full_Duhen_SunStrip_1_5_DVC = 11.1434;
	final static float	qm_stock_Duhen_SunStrip_1_8_DVC = 14.067;
	final static float	qm_full_Duhen_SunStrip_1_8_DVC = 11.3494;
	final static float	qm_stock_Duhen_SunStrip_2_2_DVC = 13.5854;
	final static float	qm_full_Duhen_SunStrip_2_2_DVC = 11.337;
		
	final static float	qm_stock_Einvagen_110_GT = 14.3126666666667;
	final static float	qm_full_Einvagen_110_GT = 11.6938333333333;
	final static float	qm_stock_Einvagen_110_GTK = 13.6701666666667;
	final static float	qm_full_Einvagen_110_GTK = 11.7413333333333;
	final static float	qm_stock_Einvagen_140_GTA = 12.5596666666667;
	final static float	qm_full_Einvagen_140_GTA = 10.5381666666667;
		
	final static float	qm_stock_Emer_MotorSport_Nonus_GT2 = 10.1594;
	final static float	qm_full_Emer_MotorSport_Nonus_GT2 = 8.7044;
	final static float	qm_stock_Emer_Nonus_Street_GT = 13.632;
	final static float	qm_full_Emer_Nonus_Street_GT = 11.1332;
		
	final static float	qm_stock_Hauler_s_SuperDuty_500 = 12.919;
	final static float	qm_full_Hauler_s_SuperDuty_500 = 8.17;
	final static float	qm_stock_Hauler_s_SuperDuty_Extra_750 = 12.231;
	final static float	qm_full_Hauler_s_SuperDuty_Extra_750 = 8.9984;

	final static float	qm_stock_Ishima_Enula_WR_SuperTurizmo = 12.3078571428571;
	final static float	qm_full_Ishima_Enula_WR_SuperTurizmo = 10.3145714285714;
	final static float	qm_stock_Ishima_Enula_WRY = 15.145;
	final static float	qm_full_Ishima_Enula_WRY = 11.8602857142857;
	final static float	qm_stock_Ishima_Enula_WRZ = 13.8452857142857;
	final static float	qm_full_Ishima_Enula_WRZ = 10.614;
		
	final static float	qm_stock_MC_GT = 12.7918;
	final static float	qm_full_MC_GT = 9.5882;
	final static float	qm_stock_MC_GT_B_series = 12.6636;
	final static float	qm_full_MC_GT_B_series = 9.91;
	final static float	qm_stock_MC_GT_Limited_Edition = 11.747;
	final static float	qm_full_MC_GT_Limited_Edition = 10.6166;

	final static float	qm_stock_Prime_DLH_500 = 11.4848;
	final static float	qm_full_Prime_DLH_500 = 11.747;
	final static float	qm_stock_Prime_DLH_700 = 10.1814;
	final static float	qm_full_Prime_DLH_700 = 10.1627;

	final static float	qm_stock_Shimutshibu_Focer_RC_200 = 15.5492857142857;
	final static float	qm_full_Shimutshibu_Focer_RC_200 = 12.3081428571429;
	final static float	qm_stock_Shimutshibu_Focer_RC_300 = 14.0142857142857;
	final static float	qm_full_Shimutshibu_Focer_RC_300 = 11.5444285714286;
	final static float	qm_stock_Shimutshibu_Focer_WRC = 12.9868571428571;
	final static float	qm_full_Shimutshibu_Focer_WRC = 10.7257142857143;

	final static float	qm_stock_Universal_stage_1 = 15.145;
	final static float	qm_full_Universal_stage_1 = 11.8602857142857;
	final static float	qm_stock_Universal_stage_2 = 12.1236814825245;
	final static float	qm_full_Universal_stage_2 = 10.3145714285714;
	final static float	qm_stock_Universal_stage_3 = 12.3078571428571;
	final static float	qm_full_Universal_stage_3 = 9.9572;
	final static float	qm_stock_Universal_stage_4 = 12.231;
	final static float	qm_full_Universal_stage_4 = 8.9984;

	Vector	vtdarr = new Vector();

	float	prevalence = 1.0;
	int	vehicleSetMask;		//megmondja, milyen seteket kepes ez a tipus generalni
								//generalt!! (a modellek alapjan)
	Vector	preferredColorIndexes = new Vector();

	public void addColorIndex( int index )
	{
		preferredColorIndexes.addElement( new Integer(index) );
	}

	public VehicleDescriptor getVehicleDescriptor( int set, float param )
	{
		VehicleModel vtd = getVehicleModel( set );

		VehicleDescriptor vd = new VehicleDescriptor();

		vd.id = vtd.id;
		vd.stockPrestige = vtd.stockPrestige;
		vd.fullPrestige = vtd.fullPrestige;
		vd.vehicleName = vtd.vehicleName;
		vd.stockQM = vtd.stockQM;
		vd.fullQM = vtd.fullQM;

		Vector colorIndexes;

		if( vtd.exclusiveColors && vtd.preferredColorIndexes.size() )
			colorIndexes = vtd.preferredColorIndexes;	//model exluziv szinvalasztekkal
		else
			if( vtd.preferredColorIndexes.size() || preferredColorIndexes.size() )
			{	//a modellnek van nehany speci szine is az alap tipusszineken tul, random valasztunk
				int m = vtd.preferredColorIndexes.size();
				int t = preferredColorIndexes.size();

				if( (m+t) * Math.random() < m )
					colorIndexes = vtd.preferredColorIndexes;
				else
					colorIndexes = preferredColorIndexes;
			}
			//else
			//	System.log( "VehicleType.getVehicleDescriptor(): color information missing - using default. id:" + id() );

		if( colorIndexes )		
			vd.colorIndex = colorIndexes.elementAt( (int)(Math.random()*colorIndexes.size()) ).intValue();

		if( param < 0 )
		{	//random selection
			vd.power = vtd.minPower + Math.random()*(vtd.maxPower - vtd.minPower);
			vd.optical = vtd.minOptical + Math.random()*(vtd.maxOptical - vtd.minOptical);
			vd.tear = vtd.minTear + Math.random()*(vtd.maxTear - vtd.minTear);
			vd.wear = vtd.minWear + Math.random()*(vtd.maxWear - vtd.minWear);
		}
		else
		{	//use param

			if (param>1.0)
				param=1.0;
			vd.power = vtd.minPower + param*(vtd.maxPower - vtd.minPower);
			vd.optical = vtd.minOptical + param*(vtd.maxOptical - vtd.minOptical);
			vd.tear = vtd.minTear + param*(vtd.maxTear - vtd.minTear);
			vd.wear = vtd.minWear + param*(vtd.maxWear - vtd.minWear);
		}

		return vd;
	}

	//adott set szerinti modellt ad vissza
	private VehicleModel getVehicleModel( int set )
	{
		VehicleModel vtd;
		float	grossPrevalence, grossPrevalence2;

		for( int i=vtdarr.size()-1; i>=0; i-- )
		{
			vtd = vtdarr.elementAt(i);
			if( set & vtd.vehicleSetMask )
				grossPrevalence += vtd.prevalence;
		}

		grossPrevalence*=Math.random();

		for( int i=vtdarr.size()-1; i>=0; i-- )
		{
			vtd = vtdarr.elementAt(i);
			if( set & vtd.vehicleSetMask )
			{
				grossPrevalence2 += vtd.prevalence;
				if( grossPrevalence2 > grossPrevalence )
				{
					//megvan a jo kis vtd!
					break;
				}
			}
		}

		return vtd;
	}

	public void init()
	{
		//calc vehicleSetMask:
		for( int i=vtdarr.size()-1; i>=0; i-- )
			vehicleSetMask |= vtdarr.elementAt(i).vehicleSetMask;
	}

}
