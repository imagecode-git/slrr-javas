package java.game.parts;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.bodypart.*;

public class DynoData extends Native
{
	//automatikusak lehetnenek, a hivasuk is!! (kerdes mi van ha parameterezni kell..)
	public native void newNative();
	public native void deleteNative();

	//konstruktor-destruktor
	DynoData()	{ newNative(); }
	public void finalize()	{ deleteNative(); }


//blokk, hengerfej meretei
	float	cylinders		= 4;
	float	bore			= 0.082;
	float	stroke			= 0.090;
	float	Vmin			= 0.00005;

//szelepek meretei
	float	in_min			= 0.005;
	float	out_min			= 0.005;
	float	in_max			= 0.8;
	float	out_max			= 0.8;

//szelepek idozitese
	float	time_in_open	= 0.001;
	float	time_in_close	= 0.25;
	float	time_out_open	= 0.75;
	float	time_out_close	= 0.99;

//gyujtas idozitese (valami bonyolultabb kellene)
	float	time_spark_min	= 0.48;
	float	time_spark_inc	= -0.05;
	float	time_spark_RPM0	= 2000.0;
	float	time_spark_RPM1	= 5000.0;
	float	time_burn		= 0.003;
	float	RPM_limit		= 7000.0;

	float	T_loss			= 95.0;	//hoveszteseg% 1 sec alatt

//turbo params
	float	rpm_turbo_mul	= 16.0;
	float	rpm_turbo_opt	= 85000.0;
	float	rpm_turbo_range = 20000.0;
	float	P_turbo_max	= 0.0;
	float	P_turbo_waste	= 10.0;
//ToDo: supercharger params
//ToDo: intercooler params
	float	intercooling = 1.0;

//ToDo: nitro params
	float	nitro_H			= 1.0;	//multiplier for mixture heating value
	float	nitro_cooling	= 1.0;	//how much it cools air down
	float	nitro_consumption = 0.0;

	float	mixture_ratio	= 14.0;
	float	mixture_H	= 1000000.0;
	float	max_fuel_consumption	= 0.0;	//virtually unlimited
	float	max_air_consumption	= 0.0;	//-"-

	//results
//	float	table_stepsize	= 500;	//torquetable stepsize
//	float[]	torquetable		= new float[16];
//	float[]	torquetable2	= new float[16];
//	float[]	HPtable			= new float[16];

	float	torque			= 1.0;		//multiplier for torquetable
	float	torque2			= 1.0;		//-"-, boosted

	//statistics
	float	maxRPM			= 7000.0;
	float	Displacement	= 0;
	float	Compression		= 0;
	float	minCompression		= 0;
	float	maxCompression		= 0;
	String	fuelType = "";
	float	maxTorque		= 0;
	float	maxHP			= 0;
	float	RPM_maxTorque	= 0;
	float	RPM_maxHP		= 0;

	// common flags for all types of engines //
	static int flagsXX_inoperable	= 0x80000000;
	static int flagsXX_has_flywheel	= 0x40000000;
	static int flagsXX_has_port_nos	= 0x20000000;
	static int flagsXX_has_wet_nos	= 0x10000000;
	static int flagsXX_has_dry_nos	= 0x08000000;

	// flags for the inline-4 blocks
	static int flagsI4_has_SC_intake_tube                 = 0x00000001;
	static int flagsI4_has_turbo_intake_tube              = 0x00000002;
	static int flagsI4_has_stock_intake_tube              = 0x00000004;
	static int flagsI4_has_air_filter_for_SC_intake       = 0x00000008;
	static int flagsI4_has_air_filter_for_turbo_intake    = 0x00000010;
	static int flagsI4_has_air_filter_for_stock_intake    = 0x00000020;

	// flags for the vee-6 blocks
	static int flagsV6_has_SC_intake_tube      = 0x00000001;
	static int flagsV6_has_L_turbo_intake_tube = 0x00000002;
	static int flagsV6_has_R_turbo_intake_tube = 0x00000004;
	static int flagsV6_has_biturbo_intake_tube = 0x00000008;
	static int flagsV6_has_stock_intake_tube   = 0x00000010;
	static int flagsV6_has_air_filter_for_SC_intake       = 0x00000020;
	static int flagsV6_has_air_filter_for_L_turbo_intake  = 0x00000040;
	static int flagsV6_has_air_filter_for_R_turbo_intake  = 0x00000080;
	static int flagsV6_has_air_filter_for_stock_intake    = 0x00000100;

	int		flags			= 0;
/*
	public void LogVars()
	{
		System.log("Dynodata dump:");
		System.log("--------------");
		System.log("cylinders = "+cylinders);
		System.log("bore = "+bore*1000.0);
		System.log("stroke = "+stroke*1000.0);
		System.log("Vmin = "+Vmin*100.0*100.0*100.0);

		System.log("in_min = "+in_min);
		System.log("out_min = "+out_min);
		System.log("in_max = "+in_max);
		System.log("out_max = "+out_max);

		System.log("time_in_open = "+time_in_open);
		System.log("time_in_close = "+time_in_close);
		System.log("time_out_open = "+time_out_open);
		System.log("time_out_close = "+time_out_close);

		System.log("time_spark_min = "+time_spark_min);
		System.log("time_spark_inc = "+time_spark_inc);
		System.log("time_spark_RPM0 = "+time_spark_RPM0);
		System.log("time_spark_RPM1 = "+time_spark_RPM1);
		System.log("time_burn = "+time_burn);
		System.log("RPM_limit = "+RPM_limit);

		System.log("T_loss = "+T_loss);

		System.log("rpm_turbo_mul = "+rpm_turbo_mul);
		System.log("rpm_turbo_opt = "+rpm_turbo_opt);
		System.log("rpm_turbo_range = "+rpm_turbo_range);
		System.log("P_turbo_max = "+P_turbo_max);
		System.log("P_turbo_waste = "+P_turbo_waste);
		System.log("nitro_H = "+nitro_H);
		System.log("nitro_cooling = "+nitro_cooling);
		System.log("nitro_consumption = "+nitro_consumption);

		System.log("mixture_ratio = "+mixture_ratio);
		System.log("mixture_H = "+mixture_H);
		System.log("max_fuel_consumption = "+max_fuel_consumption);
		System.log("max_air_consumption = "+max_air_consumption);

//		System.log("table_stepsize = "+table_stepsize);
//		System.log("torque = "+torque);
//		System.log("torque2 = "+torque2);

		System.log("maxRPM = "+maxRPM);
		System.log("Displacement = "+(Displacement*100*100*100)+" cc");
		System.log("Compression = "+Compression);
		System.log("minCompression = "+minCompression);
		System.log("maxCompression = "+maxCompression);
		System.log("fuelType = \""+fuelType+"\"");
		System.log("maxTorque = "+maxTorque);
		System.log("maxHP = "+maxHP);
		System.log("RPM_maxTorque = "+RPM_maxTorque);
		System.log("RPM_maxHP = "+RPM_maxHP);

		System.log("flags = "+flags);
		System.log("");
	}
*/
	public void clear()
	{
		cylinders		= 0;
		bore			= 0.01;
		stroke			= 0.01;
		Vmin			= 100000.0; // infinite

		in_min			= 1.0;
		out_min			= 1.0;
		in_max			= 1.0;
		out_max			= 1.0;

		time_in_open	= 0.001;
		time_in_close	= 0.002;
		time_out_open	= 0.003;
		time_out_close	= 0.004;

		time_spark_min	= 0.50;
		time_spark_inc	= 0.00;
		time_spark_RPM0	= 0.0;
		time_spark_RPM1	= 10000.0;
		time_burn		= 100000.0; // infinite
		RPM_limit		= 10000.0;

		T_loss			= 100.0; // thermal loss in percent per second

//		intercooling = 1.0;
		rpm_turbo_mul   = 0.0;
		rpm_turbo_opt   = 0.0;
		rpm_turbo_range = 0.0;
		P_turbo_max     = 0.0;
		P_turbo_waste   = 0.0;

		nitro_H			= 1.0;
		nitro_cooling	= 1.0;
		nitro_consumption = 0.0;

		mixture_ratio	= 14.0;
		mixture_H	= 1000000.0;
		max_fuel_consumption	= 0.0;
		max_air_consumption	= 0.0;

		torque			= 0.0;
		torque2			= 0.0;
		maxRPM			= 0.0;

//    for (int i=0; i<16; i++)
//		{
//			torquetable[i] = 0.0;
//			torquetable2[i] = 0.0;
//		}

		flags			= 0;
	}

	public void beAverageOf(DynoData one, DynoData other)
	{
		bore                 = (one.bore                 + other.bore)*0.5;
		stroke               = (one.stroke               + other.stroke)*0.5;
		Vmin                 = (one.Vmin                 + other.Vmin)*0.5;

		in_min               = (one.in_min               + other.in_min)*0.5;
		out_min              = (one.out_min              + other.out_min)*0.5;
		in_max               = (one.in_max               + other.in_max)*0.5;
		out_max              = (one.out_max              + other.out_max)*0.5;

		time_in_open         = (one.time_in_open         + other.time_in_open)*0.5;
		time_in_close        = (one.time_in_close        + other.time_in_close)*0.5;
		time_out_open        = (one.time_out_open        + other.time_out_open)*0.5;
		time_out_close       = (one.time_out_close       + other.time_out_close)*0.5;

		time_spark_min       = (one.time_spark_min       + other.time_spark_min)*0.5;
		time_spark_inc       = (one.time_spark_inc       + other.time_spark_inc)*0.5;
		time_spark_RPM0      = (one.time_spark_RPM0      + other.time_spark_RPM0)*0.5;
		time_spark_RPM1      = (one.time_spark_RPM1      + other.time_spark_RPM1)*0.5;
		time_burn            = (one.time_burn            + other.time_burn)*0.5;
		RPM_limit            = (one.RPM_limit            + other.RPM_limit)*0.5;

		T_loss               = (one.T_loss               + other.T_loss)*0.5;

//		intercooling         = (one.intercooling          + other.intercooling)*0.5;
		rpm_turbo_mul        = (one.rpm_turbo_mul        + other.rpm_turbo_mul)*0.5;
		rpm_turbo_opt        = (one.rpm_turbo_opt        + other.rpm_turbo_opt)*0.5;
		rpm_turbo_range      = (one.rpm_turbo_range      + other.rpm_turbo_range)*0.5;
		P_turbo_max          = (one.P_turbo_max          + other.P_turbo_max)*0.5;
		P_turbo_waste        = (one.P_turbo_waste        + other.P_turbo_waste)*0.5;
		nitro_H		     = (one.nitro_H              + other.nitro_H)*0.5;
		nitro_cooling        = (one.nitro_cooling        + other.nitro_cooling)*0.5;
		nitro_consumption    = (one.nitro_consumption    + other.nitro_consumption)*0.5;

		mixture_ratio        = (one.mixture_ratio        + other.mixture_ratio)*0.5;
		mixture_H            = (one.mixture_H            + other.mixture_H)*0.5;
		max_fuel_consumption = (one.max_fuel_consumption + other.max_fuel_consumption)*0.5;
		max_air_consumption  = (one.max_air_consumption  + other.max_air_consumption)*0.5;

		maxRPM               = (one.maxRPM               + other.maxRPM)*0.5;
	}

	public DynoData clone()
	{
		DynoData clonedDynoData = new DynoData();

		clonedDynoData.cylinders		= cylinders;
		clonedDynoData.bore				= bore;
		clonedDynoData.stroke			= stroke;
		clonedDynoData.Vmin				= Vmin;

		clonedDynoData.in_min			= in_min;
		clonedDynoData.out_min			= out_min;
		clonedDynoData.in_max			= in_max;
		clonedDynoData.out_max			= out_max;

		clonedDynoData.time_in_open		= time_in_open;
		clonedDynoData.time_in_close	= time_in_close;
		clonedDynoData.time_out_open	= time_out_open;
		clonedDynoData.time_out_close	= time_out_close;

		clonedDynoData.time_spark_min	= time_spark_min;
		clonedDynoData.time_spark_inc	= time_spark_inc;
		clonedDynoData.time_spark_RPM0	= time_spark_RPM0;
		clonedDynoData.time_spark_RPM1	= time_spark_RPM1;
		clonedDynoData.time_burn		= time_burn;
		clonedDynoData.RPM_limit		= RPM_limit;

		clonedDynoData.T_loss			= T_loss;

//		clonedDynoData.intercooling	= intercooling;
		clonedDynoData.rpm_turbo_mul	= rpm_turbo_mul;
		clonedDynoData.rpm_turbo_opt	= rpm_turbo_opt;
		clonedDynoData.rpm_turbo_range	= rpm_turbo_range;
		clonedDynoData.P_turbo_max	= P_turbo_max;
		clonedDynoData.P_turbo_waste	= P_turbo_waste;
		clonedDynoData.nitro_H		= nitro_H;
		clonedDynoData.nitro_cooling	= nitro_cooling;
		clonedDynoData.nitro_consumption= nitro_consumption;

		clonedDynoData.mixture_ratio = mixture_ratio;
		clonedDynoData.mixture_H = mixture_H;
		clonedDynoData.max_fuel_consumption = max_fuel_consumption;
		clonedDynoData.max_air_consumption = max_air_consumption;

//		clonedDynoData.table_stepsize	= table_stepsize;
//		clonedDynoData.torquetable		= new float[16];
//		clonedDynoData.torquetable2		= new float[16];
//		clonedDynoData.HPtable			= new float[16];

		clonedDynoData.torque			= torque;
		clonedDynoData.torque2			= torque2;

		clonedDynoData.maxRPM			= maxRPM;
		clonedDynoData.Displacement		= Displacement;
		clonedDynoData.Compression		= Compression;
		clonedDynoData.minCompression		= minCompression;
		clonedDynoData.maxCompression		= maxCompression;
		clonedDynoData.fuelType			= fuelType;
		clonedDynoData.maxTorque		= maxTorque;
		clonedDynoData.maxHP			= maxHP;
		clonedDynoData.RPM_maxTorque	= RPM_maxTorque;
		clonedDynoData.RPM_maxHP		= RPM_maxHP;

		clonedDynoData.flags			= flags;

		return clonedDynoData;
	}
/*
	final static int SYNC_INPUT	= 0x00000001;
	final static int SYNC_RESULTS	= 0x00000002;
	final static int SYNC_TURBOS	= 0x00000004;
	final static int SYNC_ALL	= 0xFFFFFFFF;

	public void syncWith( DynoData other, int syncFlags )
	{
		if( syncFlags && SYNC_INPUT )
		{	
			in_min = (in_min+other.in_min)/2;
		}
		if( syncFlags && SYNC_TURBOS )
		{	
			if( vMax == 0.0 || other.vMax == 0.0 )
				vMax = 0.0;
		}
	}
*/

	public native float calcDyno( float tablesize );
	public native float getTorque( float RPM, float nitro );
	public native float getHP( float RPM, float nitro );
}
