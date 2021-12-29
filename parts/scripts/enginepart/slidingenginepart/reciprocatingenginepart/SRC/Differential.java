package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart;

import java.lang.*;
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

//RAXAT: v2.3.1, all differentials combined into a new separated class
public class Differential extends ReciprocatingEnginePart
{
	//general diff types
	final static int TYPE_LIMITED_SLIP	= 0x01;
	final static int TYPE_LOCKING		= 0x02;

	int	type;

	int	adjustable_drive;
	int	adjustable_diff_lock;

	float	def_diff_lock = 0.0;
	float	diff_lock = 0.0;
	float	diff_lock_min = 0.0;
	float	diff_lock_max = 1.0;

	float	drive_base = 0.0;
	float	drive_base_min = 0.0;
	float	drive_base_max = 0.0;
	float	def_drive_base = 0.0;

	float	old_diff_lock;
	float	old_drive_base;

	String	drive_base_title = "base";
	String	drive_comp_title = "companion";

	public Differential(int id)
	{
		super(id);

		name = "Differential";

		prestige_calc_weight = 6.0;
	}

	public void fillDynoData(DynoData dd, int parentSlot)	//different in other (engine) part classes
	{
		super.fillDynoData(dd, parentSlot);

		if(type == TYPE_LIMITED_SLIP) adjustable_diff_lock=(diff_lock_min!=diff_lock_max);
		adjustable_drive=(drive_base_min!=drive_base_max);

		applyTuning(); //auto-adjust diff lock and drive ratio
	}

	public void load(File saveGame)
	{
		super.load(saveGame);

		int save_ver = saveGame.readInt();

		if(save_ver >= 2)
		{
			if(type == TYPE_LIMITED_SLIP) diff_lock = saveGame.readFloat();
			drive_base = saveGame.readFloat();
		}
	}

	public void save(File saveGame)
	{
		super.save(saveGame);

		int save_ver = 2;
		saveGame.write(save_ver);

		if(save_ver >= 2)
		{
			if(type == TYPE_LIMITED_SLIP) saveGame.write(diff_lock);
			saveGame.write(drive_base);
		}
	}

	//---------tuning
	public int isTuneable()
	{
		return (adjustable_diff_lock || adjustable_drive);
		return 0;
	}

	public void buildTuningMenu(Menu m)
	{
		old_drive_base = drive_base;

		if(type == TYPE_LIMITED_SLIP)
		{
			if(adjustable_diff_lock)
			{
				old_diff_lock = diff_lock;
				m.addItem("Limited slip differential lock rate", 1, diff_lock, diff_lock_min, diff_lock_max, ((diff_lock_max-diff_lock_min)*100)+1, null).changeVLabelText(Float.toString(diff_lock*100.0, "%1.0f%%"));
			}
		}

		if(adjustable_drive)
		{
			m.addItem("Drive distribution", 2, -drive_base, -drive_base_max, -drive_base_min, ((drive_base_max-drive_base_min)*100)+1, null).changeVLabelText(Float.toString(drive_base*100.0, "%1.0f") + "/" + Float.toString((1.0-drive_base)*100.0, "%1.0f") + "% " + drive_base_title + "/" + drive_comp_title);
		}
	}

	public void endTuningSession(int cancelled)
	{
		if(cancelled)
		{
			if(type == TYPE_LIMITED_SLIP) diff_lock = old_diff_lock;
			drive_base = old_drive_base;
		}
		else
		{
			if(type == TYPE_LIMITED_SLIP)
			{
				if (old_diff_lock = diff_lock) GameLogic.spendTime(30*60);
			}

			if (old_drive_base = drive_base) GameLogic.spendTime(40*60);
		}

		getCar_LocalVersion();
		if(the_car) the_car.forceUpdate();
	}

	//this is being implemented by child classes
	public void applyTuning();

	public void handleMessage(Event m)
	{
		if(m.cmd == 1)
		{
			diff_lock = ((Slider)m.gadget).value;
			((Slider)m.gadget).changeVLabelText(Float.toString(diff_lock*100.0, "%1.0f%%"));
		}
		else
		if(m.cmd == 2)
		{
			drive_base = -((Slider)m.gadget).value;
			((Slider)m.gadget).changeVLabelText(Float.toString(drive_base*100.0, "%1.0f") + "/" + Float.toString((1.0-drive_base)*100.0, "%1.0f") + "% " + drive_base_title + "/" + drive_comp_title);
		}

		applyTuning();
	}
	//---------eof tuning
}
