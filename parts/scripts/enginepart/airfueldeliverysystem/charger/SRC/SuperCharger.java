package java.game.parts.enginepart.airfueldeliverysystem.charger;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;
import java.game.parts.enginepart.airfueldeliverysystem.*;

public class SuperCharger extends Charger
{
	float	pulley_diameter = 1.0; // in milimeters //
	float	pulley_width	= 1.0; // in milimeters //
	float	default_rpm_turbo_mul	= 1.0;

	public SuperCharger( int id )
	{
		super( id );

		name = "supercharger";
	}

	public void updatevariables()
	{
	}

	public float kmToMaxWear(float km)
	{
		if (rpm_turbo_mul>0.0)
			return super.kmToMaxWear(km)*(default_rpm_turbo_mul/rpm_turbo_mul)*(default_rpm_turbo_mul/rpm_turbo_mul);
		return super.kmToMaxWear(km);
	}

	public void fillDynoData( DynoData dd, int parentSlot )
	{
		updatevariables();

		super.fillDynoData(dd,parentSlot);

		dd.rpm_turbo_mul = rpm_turbo_mul;
		dd.rpm_turbo_opt = rpm_turbo_opt*rpm_turbo_mul*rpm_turbo_mul;
		dd.rpm_turbo_range = rpm_turbo_range*rpm_turbo_mul*rpm_turbo_mul;
		if (default_rpm_turbo_mul>0.0)
			dd.P_turbo_max = P_turbo_max*rpm_turbo_mul/default_rpm_turbo_mul;
		else
			dd.P_turbo_max = P_turbo_max;

		if (P_turbo_waste>0.0)
			dd.P_turbo_waste = P_turbo_waste;
		else
			dd.P_turbo_waste = dd.P_turbo_max;
	}
}
