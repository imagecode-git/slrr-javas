package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.rgearpart.*;

public class Camshaft extends ReciprocatingEnginePart
{
	// undefinable inputs //
	float	static_input_lobe_separation=0.0;	// in degrees //
	float	static_input_lift_in_open=0.0;		// in milimeters //
	float	static_input_lift_in_close=0.0;		// in milimeters //
	float	static_input_lift_out_open=0.0;		// in milimeters //
	float	static_input_lift_out_close=0.0;	// in milimeters //
	float	static_input_time_in_duration=0.0;	// in degrees //
	float	static_input_time_out_duration=0.0;	// in degrees //

	// tuning rules //
	float	advance_positive_peak=0.0;	// in degrees //
	float	advance_negative_peak=0.0;	// in degrees //
	float	advance_minimum_step=0.1;	// in degrees //

	// user definable inputs //
	float	advance=0.0;		// in degrees //
	float	default_advance=0.0;

	// used inside //
	float	ins_advance=0.0;		// in degrees //

	// results //
	float	lift_in_open=0.0;		// in milimeters //
	float	lift_in_close=0.0;		// in milimeters //
	float	lift_out_open=0.0;		// in milimeters //
	float	lift_out_close=0.0;		// in milimeters //
	float	time_in_open=0.0;		// clamped to inbetween 0..1 //
	float	time_in_close=0.0;		// clamped to inbetween 0..1 //
	float	time_out_open=0.0;		// clamped to inbetween 0..1 //
	float	time_out_close=0.0;		// clamped to inbetween 0..1 //

	public Camshaft(){}

	public Camshaft( int id )
	{
		super( id );

		name = "Camshaft";

		prestige_calc_weight = 25.0;
	}

	public void updatevariables()
	{
		if (advance < advance_negative_peak)
		{
			advance = advance_negative_peak;
			ins_advance = advance;
//			System.log("  low peak advance = "+advance);
		}
		else
		if (advance > advance_positive_peak)
		{
			advance = advance_positive_peak;
			ins_advance = advance;
//			System.log("  top peak advance = "+advance);
		}
		else
		{
//			System.log("  starting advance = "+advance);
			float	adv = advance_negative_peak;

			while ((adv < advance_positive_peak) && (adv < advance))
				adv += advance_minimum_step;

			ins_advance = adv;
//			System.log("  resulting ins_advance = "+ins_advance);
		}

		lift_in_open = static_input_lift_in_open;
		lift_in_close = static_input_lift_in_close;
		lift_out_open = static_input_lift_out_open;
		lift_out_close = static_input_lift_out_close;

		if (static_input_time_in_duration>0.0)
		{
			time_in_open = ins_advance+360.0-static_input_lobe_separation-static_input_time_in_duration/2.0;
			time_in_close = time_in_open+static_input_time_in_duration;
		}
		else
		{
			time_in_open = 0.0;
			time_in_close = 0.0;
		}

		if (static_input_time_out_duration>0.0)
		{
			time_out_open = ins_advance+360.0+static_input_lobe_separation-static_input_time_out_duration/2.0;
			time_out_close = time_out_open+static_input_time_out_duration;
		}
		else
		{
			time_out_open = 0.0;
			time_out_close = 0.0;
		}

		time_in_open = rollTo( time_in_open, 0.0, 720.0 );
		time_in_close = rollTo( time_in_close, 0.0, 720.0 );
		time_out_open = rollTo( time_out_open, 0.0, 720.0 );
		time_out_close = rollTo( time_out_close, 0.0, 720.0 );

//		System.log(">>> Valve timing in degrees for "+name+": <<<");
//		System.log("    intake open: "+time_in_open+" deg");
//		System.log("    intake close: "+time_in_close+" deg");
//		System.log("    exhaust open: "+time_out_open+" deg");
//		System.log("    exhaust close: "+time_out_close+" deg");

		time_in_open /= 720.0;
		time_in_close /= 720.0;
		time_out_open /= 720.0;
		time_out_close /= 720.0;

//		System.log(">>> Valve timing from 0 to 1 for "+name+": <<<");
//		System.log("    intake open: "+time_in_open);
//		System.log("    intake close: "+time_in_close);
//		System.log("    exhaust open: "+time_out_open);
//		System.log("    exhaust close: "+time_out_close);
//		System.log(">>> Valve timing for "+name+" end <<<");
	}

	public String liftToText(float val)
	{
		return Float.toString(val,"%1.1f mm")+Float.toString(val/25.4," (%1.2f inch)");
	}

	public String advanceCapabsToText()
	{
		if (advance_positive_peak-advance_negative_peak>0.0)
			return "can be adjusted "+Float.toString(advance_negative_peak,"%1.1f")+Float.toString(advance_positive_peak," to %1.1f degrees")+Float.toString(advance_minimum_step," in %1.1f degree steps");
		return "cannot be adjusted";
	}

	public String massToText(float mass)
	{
		return Float.toString(mass, "%1.1f kg")+Float.toString(mass*2.2, " (%1.1f pounds)");
	}

	public String degToText(float val)
	{
		return Float.toString(val,"%1.0f degrees");
	}

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			advance = saveGame.readFloat();
			clampTo(advance, advance_negative_peak, advance_positive_peak);
			int xthstep = (int)((advance - advance_negative_peak)/advance_minimum_step);
			advance = ((float)xthstep)*advance_minimum_step+advance_negative_peak;
		}
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 1;

		saveGame.write( save_ver );
		if (save_ver >= 1)
		{
			saveGame.write( advance );
		}
	}

	public float getInertia()
	{
		return super.getInertia()*2.0; // because it rotates twice faster than the crankshaft //
	}

	//---------tuning
	public int isTuneable()
	{
		return (advance_positive_peak-advance_negative_peak>0.0);
	}

	// backup values //
	int	old_advance;

	public void buildTuningMenu( Menu m )
	{
		old_advance = advance;
		m.addItem( "Advance angle",			1, advance, advance_negative_peak, advance_positive_peak, (advance_positive_peak-advance_negative_peak)/advance_minimum_step+1, null ).printValue("   %1.1f deg.");

		m.addItem( "Reset to factory defaults",		0);	//this should always be with cmd=0
	}

	public void endTuningSession( int cancelled )
	{
		if( cancelled )
		{
			advance = old_advance;
		}
		else
		{
			getCar_LocalVersion();
			if (the_car)
				the_car.forceUpdate();
			if (old_advance != advance)
				GameLogic.spendTime(7*60);
		}
	}

	public void handleMessage( Event m )
	{
		if( m.cmd == 0 )
		{
			advance = default_advance;

			m.gadget.osd.findGadget( this, 1 ).setValue( advance );
		}
		else
		if( m.cmd == 1 )
		{
			advance = ((Slider)m.gadget).value;
		}
	}
	//---------tuning
}
