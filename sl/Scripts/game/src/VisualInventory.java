package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

import java.game.parts.*;
import java.game.parts.bodypart.*;

public class VisualInventory extends Inventory
{
	int			linesPerPage, partsPerLine;
	InventoryPanel[]	panels;

	int			cline;
	int			start, stop;	//mely inventoryitemek lathatoak
	int			visualsUpdated;	//olvassa es nullaza akit erdekel valtozott-e valami
	int			interactive;
	Object			caller;

	public VisualInventory( Player player, float left, float top, float width, float height )
	{
		this( player, left, top, width, height, 1 );
	}

	public VisualInventory( Player player, float left, float top, float width, float height, int intr )
	{
		super( player );

		interactive = intr;
		initVisuals( left, top, width, height );
	}

	public void initVisuals( float left, float top, float width, float height )
	{
		linesPerPage=1;
		partsPerLine=5;

		//mely itemek lehetnek lathatoak kezdetben?
		cline=0;
		start = cline * partsPerLine;
		stop = start + linesPerPage * partsPerLine;

		float hSpacing = 0.013, vSpacing = 0.01;
		float	itemWidth, itemHeight;
		itemWidth=(width-(partsPerLine-1)*hSpacing)/partsPerLine;
		itemHeight=(height-(linesPerPage-1)*vSpacing)/linesPerPage;

		panels=new InventoryPanel[partsPerLine*linesPerPage];

		int	index;
		float cheight=top;
		for( int i=0; i<linesPerPage; i++ )
		{
			float cwidth=left;
			for( int j=0; j<partsPerLine; j++ )
			{
				index = i*partsPerLine+j;
				panels[index]=new InventoryPanel( this, index, cwidth, cheight, itemWidth, itemHeight );
				cwidth+=itemWidth+hSpacing;
			}
			cheight+=itemHeight+vSpacing;
		}
	}

	public int addItem( int part_type_rid )
	{
		int	index = super.addItem( part_type_rid );
		scrollTo( index );
		return index;
	}

	public int addItem( Part part )
	{
		int	index = super.addItem( part );
		scrollTo( index );
		return index;
	}

	public int getItemIDbyButtonPhyId( int id )
	{
		for( int i=0; i<panels.length; i++ )
			if( panels[i].button.phy.id() == id )
				return i+start;
	
		return items.size();	//this signals the error
	}	

	public int getItemIDbyButton( Gadget button )
	{
		for( int i=0; i<panels.length; i++ )
			if( panels[i].button == button )
				return i+start;
		
		return items.size();	//this signals the error
	}	

	public InventoryItem getItembyButton( Gadget button )
	{
		int index = getItemIDbyButton( button );

		if( index < items.size() )
			return items.elementAt(index);

		return null;			//this signals the error
	}	


	//null: not a panel; id==0: empty slot
	public Part getPartbyButton( GameRef button )
	{
		int i;
		for( i=0; i<panels.length; i++ )
			if( panels[i].button.id() == button.id() )
				break;

		if( i != panels.length )
		{
			if( i+start < items.size() )
			{
				InventoryItem item = items.elementAt( i+start );
				return item.getPart();
			}
			else
				return new Part();
		}

		return null;
	}	

	public InventoryPanel getPanelbyButtonPhyId( int id )
	{
		int i;
		for( i=0; i<panels.length; i++ )
			if( panels[i].button.phy.id() == id )
				break;

		if( i != panels.length )
			return panels[i];

		return null;
	}	


	public void show()
	{
		for( int i=0; i<panels.length; i++ )
			panels[i].show();

		setEventMask( EVENT_CURSOR|EVENT_COMMAND );
	}

	public void hide()
	{
		clearEventMask( EVENT_ANY );

		for( int i=0; i<panels.length; i++ )
			panels[i].hide();
	}


	public void panelSwap( int index_a, Gadget dropped )
	{
		//swap between to panels
		int index_b = getItemIDbyButton( dropped );

		if( index_a < panels.length && index_b < items.size() )
		{
			new SfxRef( GameLogic.SFX_DRAGDROP ).play(); 

			swap(index_a+start, index_b );

			update();
		}
	}

	public void panelDragNDrop( int index, GameRef dropped )
	{
		//car->inventory
		if( index < panels.length )
		{
			Object o = dropped.getScriptInstance();
			if( o instanceof Part && GameLogic.player.car && GameLogic.player.car.chassis && o.getCarRef().id() == GameLogic.player.car.chassis.getCarRef().id() ) //RAXAT: we check if it's a player's car, so other cars in the garage won't be affected
			{
				Part part = o;

				if( !(part instanceof Chassis ) )
				{
					int reason = part.getInfo(GameType.GII_REMOVE_OK);
					if (reason!=-1)
					{	//semmi akadalya, elso korben kiszerelheto
						float time = GameLogic.mechTime( part, 0 );

						new SfxRef( GameLogic.SFX_WRENCH ).play(); 
						addItem( part );
						player.car.wakeUp();
						if( index+start < items.size()-1 )
							swap( index+start, items.size()-1 );

						//kiszereles time
						GameLogic.spendTime( GameLogic.mechTime( part, 0 ) );

					}
				}
			}
		}
	}

	public void panelLeftClick( int index  )
	{
		String	error;
		Part	part;
		int		screws;

		index += currentLine()*partsPerLine;

		if( index < items.size() )
		{
			if( (error = installToCar( index, player.car, null )) && error!="" )
			{
				new WarningDialog( player.controller, Dialog.DF_FREEZE|Dialog.DF_MODAL|Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", error ).display();
			}
			else
			{
				if(caller) caller.callback(); //patch
			}
		}
	}

	public void upScroll()
	{
		if( cline )
		{
			cline--;
			update();
		}
	}

	public void downScroll()
	{
		if( items.size() > 1 )
		{
			if( cline < (items.size()-1)/partsPerLine )
			{
				cline++;
				update();
			}
		}
	}

	public void scrollTo( int index )
	{
		int newline = index / partsPerLine;
		scrollToLine( newline );
	}

	public void scrollToLine( int lineNumber )
	{
		if( cline != lineNumber )
		{
			if( lineNumber >= 0 && lineNumber <= (items.size()-1)/partsPerLine )
			{
				cline=lineNumber;
				update();
			}
		}
	}


	public int currentLine()
	{
		return cline;
	}

	//reset all panels
	public void update()
	{
		//ures sor ne maradhasson:
		if( cline && cline * partsPerLine >= items.size() )
			cline--;

		int i, vis;

		//hol kapcsolodnak ki a buttonok?
		int begin = start;
		int end = stop;

		//mibol lesznek buttonok?
		start = cline * partsPerLine;
		stop = start + linesPerPage * partsPerLine;

		if( begin == start && end == stop )
		{
			//refresh only
			vis=0;
			for( i=start; i<stop; i++ )
			{
				InventoryItem item;
				if( i < items.size() )
					item = items.elementAt(i);
				if( item != panels[vis].invItem )
					panels[vis].attachItem(null);

				vis++;
			}
			vis=0;
			for( i=start; i<stop; i++ )
			{
				InventoryItem item;
				if( i < items.size() )
					item = items.elementAt(i);

				if( item != panels[vis].invItem )
				{
					panels[vis].attachItem(item);
				}

				vis++;
			}
		}
		else
		{
			//clear changed ones
			vis=0;
			for( i=begin; i<end; i++ )
			{
				panels[vis++].attachItem( null );
			}

			//add new ones
			vis=0;
			for( i=start; i<stop; i++ )
			{
				InventoryItem item;
				if( i < items.size() )
					item = items.elementAt(i);
				panels[vis++].attachItem( item );
			}
		}

		visualsUpdated=1;
	}
}
