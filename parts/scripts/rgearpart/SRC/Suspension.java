package java.game.parts.rgearpart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;

public class Suspension extends RGearPart
{
	float	VICx = 10000.0;
	float	VICy = 10000.0;

	float	LICy = 10000.0;
	float	LICz = 10000.0;

	float	camber = 0.0;
	float	toe = 0.0;
	float	side = 0.0;

	final static float	DEG2RAD  =  0.0174532925199432957692369076848861;
	final static float	SD_LEFT  =  1.0;
	final static float	SD_RIGHT = -1.0;

	public Suspension( int id )
	{
		super( id );

		name = "Suspension";

		prestige_calc_weight = 15.0;
	}

	public void setRollCenter(WheelRef w)
	{
		if (w)
			w.setInstantCenter( side*VICx, VICy, 0.00, 0.00, LICy, LICz );
	}

	public void setSuspensionGeometry(WheelRef w)
	{
		if (w)
			w.setYpr(new Ypr(side*toe*DEG2RAD,0.0,side*camber*DEG2RAD));
	}
}
