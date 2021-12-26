package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.game.parts.*;

//RAXAT: invetory item as vehicle, only basic stuff is implemented yet
public class InventoryItem_Vehicle extends InventoryItem
{
	private Vehicle vehicle;
	RenderRef canObject; //debug
	private Part partXXX;

	public InventoryItem_Vehicle(Inventory inv, Vehicle vhc)
	{
		super(inv);
		type = InventoryItem.IIT_VEHICLE;
		vehicle = vhc;
	}

	public Vehicle getVhc()
	{
		return vehicle;
	}

	public String getName()
	{
		return getVhc().chassis.vehicleName;
		return "<name not set>";
	}

	public int getPrice()
	{
		return getVhc().getTotalPrice();
		return 0;
	}

	public String getInfo()
	{
		return getName();
	}

	public int getDescription()
	{
		return getVhc().chassis.description;
	}

	public void show(InventoryPanel ip)
	{
		getVhc().setParent(localroot);

		if(getVhc())
		{
			getVhc().setMatrix(null, null);
			ip.createDefCamera(getVhc().chassis.getInfo(GameType.GII_SIZE)/100.0, 1);
			ip.createDefLight();
		}
		else //debug
		{
			canObject = new RenderRef( localroot, frontend:0x00ABr, "partsbinfolder" );
			canObject.setColor(0xFFFF0000);
			ip.createDefCamera(1.6, 1);	//size, enableRotate
			ip.createDefLight();
		}
	}

	public void hide(InventoryPanel ip)
	{
		ip.cleanup();
	}

	public void finalize()
	{
		if(getVhc())
		{
			vehicle.destroy();
			vehicle = null;
		}
	}
}
