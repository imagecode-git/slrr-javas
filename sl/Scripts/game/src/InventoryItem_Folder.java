package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

import java.game.parts.*;

public class InventoryItem_Folder extends InventoryItem
{
	Inventory	inv;
	Set		set;

	public InventoryItem_Folder( Inventory parentInventory )
	{
		super( parentInventory );
		inv = new Inventory( parentInventory.player );
		type = InventoryItem.IIT_SET;

		//patch amig nincs fully q class name
		PaintCan x = (PaintCan)null;
	}

	public void addItem( Inventory src, int index )
	{
		src.moveToInventory( index, inv );
	}

	public void flush()
	{
		inv.flushAll();
	}


	public int getName()
	{
		if( set )
			return set.name;
		
		//user made sets:
		return "Set: " + inv.items.elementAt(0).getPart().name + "... (" + inv.items.size() + " items)";
	}

	public int getPart()
	{
		return 0; //nothing can be returned, use getItem() to access set
	}

	//RAXAT: v2.3.1, access to Set
	public Set getItem()
	{
		if(set) return set;
		return null;
	}

	public int getDescription()
	{
		if( set )
			return set.description;
		
		//user made sets:
		return null;
	}

	public int getPrice()
	{
		int price;
		for( int i=inv.items.size()-1; i>=0; i-- )
			price+=inv.items.elementAt(i).getPrice();

		return price;
	}

	public int getLogo()
	{
		int logo;

		if( set )
			logo = set.logo;

		if( !logo )
		{
			for( int i=inv.items.size()-1; i>=0; i-- )
				if( logo = inv.items.elementAt(i).getLogo() )
					break;
		}

		return logo;
	}

	public void copyToInventory( Inventory other )
	{
		InventoryItem_Folder folder = new InventoryItem_Folder( other );
		folder.set = set;

		for( int i=inv.items.size()-1; i>=0; i-- )
			inv.items.elementAt(i).copyToInventory( folder.inv );

		other.addItem( folder );
	}

	//InventoryItem_Folder
	public String installToCar( Vehicle car, Vector3 pos )
	{
		String error;

		if( car )
		{
			while( !inv.items.isEmpty() )
			{
				if( (error = inv.installToCar( 0, car, pos )) )
					break;
			}
		}
		return error;
	}

	public String getInfo()
	{
		return getName();
	}

	public int getType()
	{
		return type;
		return 0;
	}


	//visualization
	RenderRef canObject;

	public void show( InventoryPanel ip )
	{
		canObject = new RenderRef( localroot, frontend:0x00ABr, "partsbinfolder" );
		canObject.setColor( 0xFFFF0000 );
		ip.createDefCamera( 1.6, 1 );	//size, enableRotate
		ip.createDefLight();
	}

	public void hide( InventoryPanel ip )
	{
		ip.cleanup();
		canObject.destroy();
		canObject = null;
	}

	//io
	public void save( File saveGame )
	{
		set.save( saveGame );
		inv.save( saveGame );
	}

	public static InventoryItem_Folder createFromFile( File saveGame, Inventory inv )
	{
		InventoryItem_Folder folder = new InventoryItem_Folder( inv );
		folder.set = Set.createFromFile( saveGame );
		folder.inv.load( saveGame );
		return folder;
	}

	public void finalize()
	{
		if( canObject )
		{
			canObject.destroy();
			canObject = null;
		}
	}
}
