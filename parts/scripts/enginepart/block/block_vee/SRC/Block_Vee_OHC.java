package java.game.parts.enginepart.block.block_vee;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.block.*;
import java.game.parts.rgearpart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.cylinderhead.*;


public class Block_Vee_OHC extends Block_Vee
{
	public Block_Vee_OHC(){}

	public Block_Vee_OHC( int id )
	{
		super( id );
	}

	public Part getLeftCylinderHead()
	{
		if (L_cylinder_head_slot_ID <= 0)
			return null;

		Part res = partOnSlot(L_cylinder_head_slot_ID);

		if (res)
		{
			if (res instanceof SOHC_CylinderHead || res instanceof DOHC_CylinderHead)
				return res;
		}
//		else
//			System.log("!!!SOHC/DOHC cylinder head required on slot!!!");

		return null;
	}

	public Part getRightCylinderHead()
	{
		if (R_cylinder_head_slot_ID <= 0)
			return null;

		Part res = partOnSlot(R_cylinder_head_slot_ID);

		if (res)
		{
			if (res instanceof SOHC_CylinderHead || res instanceof DOHC_CylinderHead)
				return res;
		}
//		else
//			System.log("!!!SOHC/DOHC cylinder head required on slot!!!");

		return null;
	}

	public void check4warnings()
	{
//		super.check4warnings();
	}

}
