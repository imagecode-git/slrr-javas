package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class VehicleModel
{
	float	prevalence = 1.0;	//0.0 - never; 1.0 - always
	int		vehicleSetMask = VehicleType.VS_DEMO|VehicleType.VS_STOCK|VehicleType.VS_USED|VehicleType.VS_DRACE|VehicleType.VS_NRACE|VehicleType.VS_RRACE|VehicleType.VS_CIRCUIT|VehicleType.VS_DTM|VehicleType.VS_SPONSOR|VehicleType.VS_CROSS|VehicleType.VS_DRIFT;
	Vector	preferredColorIndexes = new Vector();
	int		exclusiveColors;	//ha ki van toltve a preferredColorIndexes itt a Modelben,
								//es ez a valtozo 1, CSAK ezek kozul valaszthat, a tipust szinei kozul nem!

	int		id;
	float	minPower=1.0, maxPower=1.5;
	float	minOptical=1.0, maxOptical=1.5;
	float	minTear=1.0, maxTear=1.0;
	float	minWear=1.0, maxWear=1.0;
	float	stockPrestige = 0.0;
	float	fullPrestige = 0.0;
	String	vehicleName = "unknown";
	float	stockQM = 16.000;
	float	fullQM = 9.000;
	float	valid = 2.300;

	public VehicleModel( int id_, int mask )
	{
		id = id_;
		vehicleSetMask = mask;
	}

	public void addColorIndex( int index )
	{
		preferredColorIndexes.addElement( new Integer(index) );
	}

}
