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

//RAXAT: v2.3.1, transmission type, dedicated center differential support
public class Transmission extends ReciprocatingEnginePart
{
	final static int	DT_FWD = 1;
	final static int	DT_RWD = 2;
	
	//RAXAT: build 932, static transmission type defaults
	final static int	TT_LEGACY 			= 0; //compatibility mode
	final static int	TT_AUTO 			= 1;
	final static int	TT_MANUAL_CLUTCH	= 2;
	final static int	TT_MANUAL			= 3;
	final static int	TT_SEMIAUTO			= 5;

	int		gears = 0;
	float[]	ratio = new float[8];
	float	end_ratio = 0.0;
	float	starter_torque = 0.0;
	float	capacity = 0.0;
	float	best_lubrication_sliction = 1.0;
	float	worst_lubrication_sliction = 1.0;
	int		adjustable_gears = 0;

	float	drive_front = 0.0;
	int		drive_type = 0;
	int		type = TT_LEGACY;

	//RAXAT: following values will stay here only to keep backward compatibility with old mods!
	//-----
	int		adjustable_diff_lock;
	int		adjustable_drive;

	float	def_diff_lock = 0.2;
	float	diff_lock_min = 0.0;
	float	diff_lock_max = 1.0;
	float	drive_front_min = 0.0;
	float	drive_front_max = 0.0;
	//-----

	//RAXAT: theese are not legacy, but now they are being changed by differential/other parts, but not by transmission itself
	float	def_drive_front = 0.0;
	float	diff_lock = 0.0;

	int	centerDifferential_slot_ID;

	public Transmission( int id )
	{
		super( id );

		name = "Transmission";

		ratio[0] = 0.0;
		ratio[7] = -0.0;

		prestige_calc_weight = 30.0;
	}

	//RAXAT: this updates transmission, from manual to auto/semi-auto etc.
	public void updateType()
	{
		if(the_car)
		{
			if(!type) type = -1;
			if(type != -1)
			{
				if(GameLogic.player.car)
				{
					if(the_car.id() == GameLogic.player.car.id())
					{
						if(type !=3 && type !=-1) Config.player_transmission = type;
						if(type == 3) Config.player_transmission = 0;
					}
				}

				if(type !=3 && type !=-1) the_car.queueEvent(null, EVENT_COMMAND, "transmission " + type);
				if(type == 3) the_car.queueEvent(null, EVENT_COMMAND, "transmission " + 0);
			}

			if(type == -1)
			{
				if(GameLogic.player.car)
				{
					if(the_car.id() == GameLogic.player.car.id()) Config.player_transmission = 1;
				}

				the_car.queueEvent(null, EVENT_COMMAND, "transmission " + 1);
			}
		}
	}

	public float getSlictionLoss()
	{
		return clampTo(worst_lubrication_sliction + getWear()*(best_lubrication_sliction-worst_lubrication_sliction),0.0,1.0);
	}

	public void fillDynoData( DynoData dd, int parentSlot )	//different in other (engine) part classes
	{
		super.fillDynoData( dd, parentSlot );

		if (the_car)
		{
			the_car.drive_type |= drive_type;
			WheelRef whl;

			int fwd = the_car.drive_type & DT_FWD;
			int rwd = the_car.drive_type & DT_RWD;

			int defDT; //to define default drivetype params for transmissions that have center diff slot, but don't have a differential installed yet
			if(centerDifferential_slot_ID)
			{
				Differential c_diff = partOnSlot(centerDifferential_slot_ID); //center diff is installed, so we pick drivetype params from it
				if(!c_diff) defDT++; //or we got no center diff, so we need to apply drivetype params from transmission
			}
			else defDT++;

			if(defDT)
			{
				if(fwd&&rwd) drive_front = 0.5;
				else
				{
					if(fwd) drive_front = 1.0;
					if(rwd) drive_front = 0.0;
				}
			}

			whl = the_car.getWheel(0); // FL //
			if (whl && fwd)
				whl.setDrive(drive_front);

			whl = the_car.getWheel(1); // FR //
			if (whl && fwd)
				whl.setDrive(drive_front);

			whl = the_car.getWheel(2); // RL //
			if (whl && rwd)
				whl.setDrive(1.0-drive_front);

			whl = the_car.getWheel(3); // RR //
			if (whl && rwd)
				whl.setDrive(1.0-drive_front);

			the_car.gears = gears;
			for (int i=0; i<8; i++)
				the_car.ratio[i] = ratio[i];
			the_car.rearend_ratio = end_ratio;
			the_car.starter_torque = starter_torque;
		}
	}

	public void setDefDriveType()
	{
		int fwd = the_car.drive_type & DT_FWD;
		int rwd = the_car.drive_type & DT_RWD;

		if(fwd&&rwd) drive_front = 0.5;
		else
		{
			if(fwd) drive_front = 1.0;
			if(rwd) drive_front = 0.0;
		}
	}

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			int i;

			for (i=0; i<8; i++)
				ratio[i] = saveGame.readFloat();
			end_ratio = saveGame.readFloat();
		}
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 2;

		saveGame.write( save_ver );
		if (save_ver >= 1)
		{
			int i;

			for (i=0; i<8; i++)
				saveGame.write(ratio[i]);
			saveGame.write(end_ratio);
		}
	}

	//---------tuning
	public int isTuneable()
	{
		return adjustable_gears;
	}

	// backup values //
	float[]	old_ratio = new float[8];
	float	old_end_ratio;
	float	rev_ratio;

	public void buildTuningMenu( Menu m )
	{
		int i;
		String gear_name = "";

		for (i=0; i<8; i++)
			old_ratio[i] = ratio[i];
		old_end_ratio = end_ratio;

		for (i=1; i<7; i++)
			ratio[i]=-ratio[i];

		if (adjustable_gears & 1) // forward gears //
		{
			for (i=1; i<=gears; i++)
			{
				if (i==1)
					gear_name = "1st";
				else
				if (i==2)
					gear_name = "2nd";
				else
				if (i==3)
					gear_name = "3rd";
				else
				if (i==4)
					gear_name = "4th";
				else
				if (i==5)
					gear_name = "5th";
				else
				if (i==6)
					gear_name = "6th";

				m.addItem( gear_name, i, ratio[i], -5.0, -0.5, 0, null ).changeVLabelText( Float.toString(-ratio[i], "%1.3f:1"));
			}
		}

		if (adjustable_gears & 2) // reversing gear //
		{
			m.addItem( "R", 7, ratio[7], -5.0, -0.5, 0, null ).changeVLabelText( Float.toString(ratio[7], "%1.3f:1"));
		}

		if (adjustable_gears & 4) // end ratio //
		{
			m.addItem( "End ratio", 8, end_ratio, 1.0, 8.0, 0, null ).changeVLabelText( Float.toString(end_ratio, "%1.3f:1"));
		}
	}

	public void endTuningSession( int cancelled )
	{
		int i;

		if( cancelled )
		{
			for (i=0; i<8; i++)
				ratio[i] = old_ratio[i];
			end_ratio = old_end_ratio;
		}
		else
		{
			for (i=0; i<8; i++)
				if (ratio[i] != old_ratio[i])
					break;

			if (i<8)
				GameLogic.spendTime(10*60+gears*2*60);


			for (i=1; i<=6; i++)
				ratio[i] = -ratio[i];
		}

		getCar_LocalVersion();
		if (the_car)
			the_car.forceUpdate();
	}

	public void handleMessage( Event m )
	{
		if( m.cmd >= 1 && m.cmd <= 7)
		{
			ratio[m.cmd] = ((Slider)m.gadget).value;
			if (m.cmd<7)
				((Slider)m.gadget).changeVLabelText( Float.toString(-ratio[m.cmd], "%1.3f:1"));
			else
				((Slider)m.gadget).changeVLabelText( Float.toString(ratio[m.cmd], "%1.3f:1"));
		}
		else
		if( m.cmd == 8)
		{
			end_ratio = ((Slider)m.gadget).value;
			((Slider)m.gadget).changeVLabelText( Float.toString(end_ratio, "%1.3f:1"));
		}
		else
		if( m.cmd == 9 )
		{
			diff_lock = ((Slider)m.gadget).value;
			getCar_LocalVersion();
			if (the_car)
				the_car.updateDifflock();
			((Slider)m.gadget).changeVLabelText( Float.toString(diff_lock*100.0, "%1.0f%%"));
		}
		else
		if( m.cmd == 10 )
		{
			drive_front = -((Slider)m.gadget).value;
			getCar_LocalVersion();
			((Slider)m.gadget).changeVLabelText( Float.toString(drive_front*100.0, "%1.0f") + "/" + Float.toString((1.0-drive_front)*100.0, "%1.0f") + "% F/R" );
		}
	}
	//---------tuning
}
