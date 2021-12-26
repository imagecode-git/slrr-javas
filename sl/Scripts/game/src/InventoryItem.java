package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class InventoryItem
{
	//RAXAT: v2.3.1, introducing inventory item types
	final static int IIT_UNDEFINED	= 0x00;
	final static int IIT_PART	= 0x01;
	final static int IIT_SET	= 0x02;
	final static int IIT_PAINTCAN	= 0x03;
	final static int IIT_VEHICLE	= 0x04;

	Inventory inventory;
	Dummy	localroot;	//local root to prevent crossrendering of button & part
	int type = IIT_UNDEFINED;

	public InventoryItem( Inventory inv )
	{
		inventory = inv;
		localroot= new Dummy( inventory, GameRef.WORLDTREEROOT );
	}

	public String getInfo()
	{
		//this should be abstract
	}
}
