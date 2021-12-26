package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

//RAXAT: v2.3.1, I/O, getInfo() added
public class InventoryItem_Paint extends InventoryItem
{
	PaintCan paintCan;

	public InventoryItem_Paint( Inventory inv, PaintCan can )
	{
		super(inv);
		type = InventoryItem.IIT_PAINTCAN;
		paintCan = can;
	}

	public PaintCan getCan()
	{
		return paintCan;
	}

	//visualization
	RenderRef canObject;

	public void show(InventoryPanel ip)
	{
		canObject = new RenderRef( localroot, frontend:0x0021r, "cannaatska" );
		canObject.setColor( paintCan.color );
		ip.createDefCamera( 0.13, 1 );	//size, enableRotate
		ip.createDefLight();
	}

	public void hide(InventoryPanel ip)
	{
		ip.cleanup();
		canObject.destroy();
		canObject = null;
	}

	//RAXAT: v2.3.1, now we can check HEX of our paintcan right in the inventory
	public String getInfo()
	{
		return "Color: " + Integer.getHex(paintCan.color|0xFF000000);
	}

	public void save(File saveGame)
	{
		paintCan.save(saveGame);
	}

	public static InventoryItem_Paint createFromFile(File saveGame, Inventory inv)
	{
		PaintCan can = PaintCan.createFromFile(saveGame);
		InventoryItem_Paint result = new InventoryItem_Paint(inv, can);
		return result;
	}

	public void finalize()
	{
		if(canObject)
		{
			canObject.destroy();
			canObject = null;
		}
	}

}
