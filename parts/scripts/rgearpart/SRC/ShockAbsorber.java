package java.game.parts.rgearpart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;

public class ShockAbsorber extends RGearPart
{
	float	damping				= 500.0;// N/(m/s)
	float	max_damping			= 0.0;	// N/(m/s)
	float	min_damping			= 0.0;	// N/(m/s)
	float	max_length			= 0.45;	// m
	float	min_length			= 0.15;	// m
	float	rebound_factor_interval		= 0.0;

	float	rebound_factor			= 1.0;
	float	min_rebound_factor		= 0.0;
	float	max_rebound_factor		= 0.0;

	final static int	WI_GAS		= 0;
	final static int	WI_OIL		= 1;
	final static int	WI_GAS_OIL	= 2;

	int	adjustable_damping		= 0;
	int	adjustable_rebound_factor	= 0;
	float	default_damping;
	float	default_rebound_factor;

	int	whats_inside			= WI_GAS;

	public ShockAbsorber( int id )
	{
		super( id );

		name = "Shock absorber";

		prestige_calc_weight = 20.0;
	}

	public void calcStuffs()
	{
		adjustable_damping = (min_damping < max_damping && min_damping > 0.0 && max_damping > 0.0);
		adjustable_rebound_factor = (rebound_factor_interval!=0.0);

		if (whats_inside == WI_GAS)
			rebound_factor = 1.000;
		else
		if (whats_inside == WI_OIL)
			rebound_factor = 0.750;
		else
		if (whats_inside == WI_GAS_OIL)
			rebound_factor = 1.333;

		min_rebound_factor = rebound_factor*(1.0-rebound_factor_interval);
		max_rebound_factor = rebound_factor*(1.0+rebound_factor_interval);

		/*
		float travel = max_length - min_length;
		String travel_text = "";
		*/
		String adj_text = "";
		String wi_text = "";

		/*
		if (travel <= 0.075)
			travel_text = "ultra short";
		else
		if (travel <= 0.150)
			travel_text = "short";
		else
		if (travel <= 0.225)
			travel_text = "medium";
		else
		if (travel <= 0.300)
			travel_text = "long";
		else
			travel_text = "ultra long";
		*/

		if (whats_inside == WI_GAS)
			wi_text = "gas";
		else
		if (whats_inside == WI_OIL)
			wi_text = "oil";
		else
		if (whats_inside == WI_GAS_OIL)
			wi_text = "oil-gas";

		if ( adjustable_damping )
		{
			adj_text = "adjustable ";
			value = brand_prestige_factor * max_damping / 18.0;
			brand_new_prestige_value = value / 3.0;
		}
		else
		{
			value = brand_prestige_factor * damping / 38.0;
			brand_new_prestige_value = value / 3.0;
		}

		if (adjustable_rebound_factor)
		{
			value *= 1.35;
			brand_new_prestige_value *= 2.0;
		}

		default_damping = damping;
		default_rebound_factor = rebound_factor;

		int dI=damping;
		name = name_prefix + " " + dI+" N/m/s "+adj_text+wi_text+" shock";

		description = "This is a";

		if (adjustable_damping)
		{
			int mindI=min_damping;
			int maxdI=max_damping;
			int rdI=(damping*rebound_factor);
			description = description+"n adjustable damping, "+wi_text+" filled shock absorber ranging from "+mindI+" to "+maxdI+" N/m/s bound damping values, factory default is "+dI+" N/m/s. Factory rebound value is "+rdI+" N/m/s.";
		}
		else
		{
			int rdI=(damping*rebound_factor);
			description = description+" non-adjustable damping, "+wi_text+" filled shock absorber. \n Bound damping is "+dI+" N/m/s, rebound is "+rdI+" N/m/s.";
		}

		int mclI=min_length*1000;
		int lclI=max_length*1000;
		description = description+" \n Most compressed length is "+mclI+" mm, least compressed length is "+lclI+".";

		if (adjustable_rebound_factor)
		{
			int minrfI=(min_rebound_factor*100);
			int maxrfI=(max_rebound_factor*100);
			description = description+" \n In the case of this shock, the bound-rebound factor can be adjusted from "+minrfI+" to "+maxrfI+" percent.";
		}

	}

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			damping = saveGame.readFloat();
			rebound_factor = saveGame.readFloat();
		}
		if (min_damping != max_damping)
			damping = clampTo(damping,min_damping,max_damping);
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 2;

		saveGame.write( save_ver );
		if (save_ver >= 1)
		{
			saveGame.write( damping );
			saveGame.write( rebound_factor );
		}
	}

	public void updatevariables()
	{
		WheelRef whl = getWheel();

		if (whl)
		{
			whl.setDamping(damping, damping*rebound_factor);

//			System.log("  bound damping   = " + damping);
//			System.log("  rebound damping = " + (damping*rebound_factor));
		}
	}

	//---------tuning
	public int isTuneable()
	{
		return (adjustable_damping && adjustable_rebound_factor);
	}

	// backup values //
	int	old_damping;
	int	old_rebound_factor;

	public void buildTuningMenu( Menu m )
	{
		old_damping = damping;
		old_rebound_factor = rebound_factor;
		if (adjustable_damping)
			m.addItem( "Bound damping",			1, damping, min_damping, max_damping, (max_damping-min_damping)/100+1, null ).printValue("   %1.0f N/m/s");
		if (adjustable_rebound_factor)
			m.addItem( "Rebound factor",			2, rebound_factor, min_rebound_factor, max_rebound_factor, max_rebound_factor-min_rebound_factor-1, null ).changeVLabelText( Float.toString(rebound_factor*100.0, "   %1.0f%%"));

		m.addItem( "Reset to factory defaults",			0);	//this should always be with cmd=0
	}

	public void endTuningSession( int cancelled )
	{
		if( cancelled )
		{
			damping = old_damping;
			rebound_factor = old_rebound_factor;
		}
		else
		{
			if (old_damping != damping)
				GameLogic.spendTime(5*60);
			if (old_rebound_factor != rebound_factor)
				GameLogic.spendTime(5*60);
			getCar_LocalVersion();
			if (the_car)
				the_car.forceUpdate();
		}
	}

	public void handleMessage( Event m )
	{
		if( m.cmd == 0 )
		{
			damping = default_damping;
			rebound_factor = default_rebound_factor;

			m.gadget.osd.findGadget( this, 1 ).setValue( damping );
			m.gadget.osd.findGadget( this, 2 ).setValue( rebound_factor );
		}
		else
		if( m.cmd == 1 )
		{
			damping = ((Slider)m.gadget).value;
		}
		else
		if( m.cmd == 2 )
		{
			rebound_factor = ((Slider)m.gadget).value;
			((Slider)m.gadget).changeVLabelText( Float.toString(rebound_factor*100.0, "   %1.0f%%"));
		}
	}
	//---------tuning
}
