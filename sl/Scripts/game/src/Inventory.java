package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

import java.game.parts.*;

public class Inventory extends GameType
{
	//the owner of the inventory
	Player		player;
	Vector		items;
	int			wakeUpOnInstall = true;

	public Inventory( Player player_ )
	{
		createNativeInstance();

		player=player_;
		items= new Vector();
	}

	public int size()
	{
		return items.size();
	}

	public void addItem( InventoryItem item )
	{
		items.addElement( item );
	}

	//RAXATt: vehicle inventory items, not finished yet
	public int addItem( Vehicle vhc )
	{
		int	index = items.size();
		items.addElement( new InventoryItem_Vehicle( this, vhc ) );
		
		update();

		return index;
	}

	//'paintcan'
	public int addItem( PaintCan can )
	{
		int	index = items.size();
		items.addElement( new InventoryItem_Paint( this, can ) );
		
		update();

		return index;
	}

	//'addpart'
	public int addItem( int part_type_rid )
	{
		int	index = items.size();
		items.addElement( new InventoryItem_Part( this, part_type_rid ) );
		
		update();

		return index;
	}

	//'addpart'
	public int insertItem( int part_type_rid )
	{
		int	index = 0;
		items.insertElementAt( new InventoryItem_Part( this, part_type_rid ), index );
		
		update();

		return index;
	}

	public int addItem( Part part )
	{
		int	index = items.size();
		items.addElement( new InventoryItem_Part( this, part ) );
		
		update();

		return index;
	}

	//use only with empty destination!
	public void addItem( Part part, int index )
	{
		items.setElementAt( new InventoryItem_Part( this, part ), index ); 
		update();
	}

	//'use'
	public String installToCar( int index, Vehicle car, Vector3 pos )
	{
		String error;

		if(car)
		{
			if( index<items.size() )
			{
				InventoryItem item = items.elementAt( index );
				if( item instanceof InventoryItem_Folder )
				{
					if( !(error=item.installToCar( car, pos )) )
					{//mindent felszerelt!  ( item.inv.size() == 0 )
						items.removeElementAt( index );
						update();
					}
				}
				else
				{
					if( car )
					{
						Part part = item.getPart();
						if( !(error=item.installToCar( car, pos )) )	//sikerult?
						{
							if(wakeUpOnInstall) car.wakeUp();
							items.removeElementAt( index );
							update();

							new SfxRef( GameLogic.SFX_WRENCH ).play(); 

							//beszereles time
							GameLogic.spendTime( GameLogic.mechTime( part, 1 ) );
						}
					}
				}
			}
		}
		else error = "You need a car to use this item! \n Buy a car or get one from the car lot.";

		if( error )
			new SfxRef( Frontend.SFX_WARNING ).play(); 

		return error;
	}

	public void moveToInventory( int index, Inventory other )
	{
		if( index<items.size() )
		{
			InventoryItem item = items.removeElementAt( index );
			update();

			other.items.addElement( item ); 
			other.update();
		}
	}

	public void removeItem( int index )
	{
		if( index<items.size() )
		{
			items.removeElementAt( index );
			update();
		}
	}

	public void removeItem( InventoryItem item )
	{
		items.removeElement( item );
		update();
	}

	//csak original alkatreszeket tartalmazo inventoryra szabad hivni!!!
	//kulonben "uj"-ra csereli a benne foglalt alkatreszeket.
	public void flushAll()
	{
		InventoryItem item;
		for( int index=0; index<items.size(); index++ )
		{
			item = items.elementAt( index );
			item.flush();
		}
	}


	//gets the part's position in the inventory
	public int getItemIDbyPart( GameRef part )
	{
		int index;
		InventoryItem item;

		for( index=0; index<items.size(); index++ )
		{
			item = items.elementAt( index );
			if( item.getPart().id() == part.id() )
					break;
		}

		return index;
	}	

	public PaintCan getCanbyIndex( int index )
	{
		if( index<items.size() )
		{
			InventoryItem item = items.elementAt( index );
			return item.getCan();
		}

		return null;
	}	

	public Part getPartbyItemID( int index )
	{
		if( index<items.size() )
		{
			InventoryItem item = items.elementAt( index );
			return item.getPart();
		}

		return null;
	}	

	public void swap( int index_a, int index_b )
	{
		if( index_a < items.size() && index_b < items.size() )
		{
			InventoryItem swap;
			swap = items.elementAt(index_a);
			items.setElementAt( items.elementAt(index_b), index_a );
			items.setElementAt( swap, index_b );
		}
	}

	public void drop( int index_src, int index_dst )
	{
		if( index_src < items.size() && index_dst < items.size() )
		{
			InventoryItem src=items.elementAt(index_src);
			InventoryItem dst=items.elementAt(index_dst);

			//nem engedunk multi level seteket
			if( !(src instanceof InventoryItem_Folder) )
			{
				if( !(dst instanceof InventoryItem_Folder) )
				{
					InventoryItem_Folder tmp = new InventoryItem_Folder( this );
					tmp.addItem( this, index_dst );				//-1
					items.insertElementAt( tmp, index_dst );	//+1, tehat a sorrend valtozatlan, src indexe hasznalhato!
					dst = tmp;
				}
				dst.addItem( this, index_src );
			}
		}
	}

	//childclasses will implement this
	public void update(){}


	public void save(File saveGame)
	{
		InventoryItem item;
		int n = items.size();

		saveGame.write(n);
		for(int index=0; index<n; index++)
		{
			item = items.elementAt(index);
			if(item instanceof InventoryItem_Part)
			{
				saveGame.write(0);	//tell the loader it will be a part
				item.getPart().save(saveGame);
			}
			else
			{
				int k;
				if(item instanceof InventoryItem_Folder) k=1;	//tell the loader it will be a set
				else k=2; //or a paint can

				saveGame.write(k);
				item.save(saveGame);
			}
		}
	}

	public void load(File saveGame)
	{
		int n = saveGame.readInt();
		for(int i=0; i<n; i++)
		{
			int kinda = saveGame.readInt();
			if(kinda == 0)
			{
				Part part = Part.createFromFile(saveGame, player);
				addItem(part);
			}
			else
			{
				if(kinda == 1)
				{
					InventoryItem_Folder folder = InventoryItem_Folder.createFromFile(saveGame, this);
					addItem(folder);
				}
				else
				{
					InventoryItem_Paint paintcan = InventoryItem_Paint.createFromFile(saveGame, this);
					addItem(paintcan);
				}
			}
		}
	}
}
