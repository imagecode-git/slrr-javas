//v 1.04
package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

import java.game.parts.*;

//the visualizer and control receiver of visible inventory items
public class InventoryPanel
{
	final static int RID_INVENTORY_LIGHT = misc.garage:0x00000024r;
	final static float SIZE_PATCH = 0.8; //RAXAT: some parts are too big and clicking them outside gadget could produce buggy ghost parts

	VisualInventory	inv;
	//Dummy	localroot;
	Dummy	bgroot;
	Osd		osd;

	InventoryItem	invItem;

	RenderRef light, osdlight=new RenderRef(), bglight=new RenderRef();
	Camera cam, osdcam, bgcam;
//	GameRef button=new GameRef();
	Rectangle bgRect;
	Gadget button;

//	RenderRef bgobj=new RenderRef();

	float	size;
	int		flags;

	Ypr		ypr = new Ypr( -2.5, -0.7, 0.0 );

	//Viewport	vp = new Viewport();
	
	//the item that this panel will display, and the panels number on the screen
	public InventoryPanel( VisualInventory inventory, int index, float top, float left, float width, float height)
	{
		//vp.create( 15, top, left, width, height );
		//localroot = new Dummy( inventory, GameType.WORLDTREEROOT );

		//bgroot = new Dummy( inventory, GameType.WORLDTREEROOT );	nobg2/4

		//to 'render' the button
		//osdlight.create( localroot, somepack:0xSOMELIGHTr, null );
//		osdcam = new Camera( localroot, vp, 2, 90.0, 0.1, 10.0 );
//		osdcam.setMatrix( new Vector3(0.0, 0.0, 5.0), null );

//		button.create( localroot, GameRef.RID_BUTTON, "0.1,0,4.7, 0,0,0", "" );
//		button.create( localroot, new GameRef(Osd.RID_GHOSTBUTTON), "0.1,0,4.7, 0,0,0", "" );
//		inventory.addNotification( button, GameType.EVENT_CURSOR, GameType.EVENT_SAME, null, "event_handlerPartsButton" );


		inv = inventory;

		osd = new Osd( new Viewport( 15, top, left, width*SIZE_PATCH, height ) );
		osd.iLevel = Osd.IL_TIPS;
		osd.globalHandler = this;
		
		Style style = new Style( width*2, height*2, Frontend.mediumFont, Text.ALIGN_CENTER, /*Osd.RRT_NONE*/ null );
		button = osd.createButton( style, 0.0, 0.0, index, null );
		button.enableDrop();

		/* nobg3/4
		//part background renderer
		bgcam = new Camera( bgroot, vp, 0, 90, 0.1, 10.0 );
		bgcam.setMatrix( new Vector3(0.0, 0.0, 3.65), null );
		bgobj.create( bgroot, inventory.backObject, null );
		*/
	}

	public void finalize()
	{
		//attachItem(null); gc killer
		cleanup();	

		//vp.destroy();

		//osdlight.destroy();
		//button.destroy();

		//localroot.destroy();

		//bglight
		//bgobj
		//bgroot.destroy(); nobg4/4

	}

	public void osdCommand( int cmd )
	{
		if( inv.interactive )
		{
			if( osd.dropObject )
			{	//drop
				inv.panelDragNDrop( cmd, osd.dropObject );
			}
			else
			if( osd.dropGadget )
			{	//swap
				inv.panelSwap( cmd, osd.dropGadget );
			}
			else
			{	//click
				inv.panelLeftClick( cmd );
			}
		}
	}

	public void show()
	{
		osd.show();
		//vp.activate( Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET);
	}

	public void hide()
	{
		//vp.deactivate();
		osd.hide();
	}

	public void attachItem( InventoryItem newInvItem )
	{
		if( newInvItem )
		{
			if (newInvItem instanceof InventoryItem_Part)
			{
				Part part = newInvItem.getPart();
				if (part)
					if (part.catalog_view_ypr)
						ypr = part.catalog_view_ypr;
			}
			invItem = newInvItem;
			invItem.show(this);
		}
		else
		{
			if( invItem )
			{
				invItem.hide( this );
				invItem = null;
				button.setToolTip( null );
			}

		}
	}

	//callback rutinok, melyeket az inventoriitemet show/hide metodusai hivhatnak:
	public void createDefCamera( float size, int flags)
	{
		this.size=size;
		this.flags=flags;

		cam = new Camera( invItem.localroot, osd.getViewport(), 1, 110, 0.001, 10.0 );
		Vector3 camPos = new Vector3( 0.0, size, size );
		camPos.rotate( new Ypr( ypr.y, 0.0, 0.0 ) );
		cam.setMatrix( camPos, ypr );
	}

	public void createDefLight()
	{
		light = new RenderRef( invItem.localroot, RID_INVENTORY_LIGHT, null );
		//light.setMatrix( new Vector3( 0.1, 0, 0.1 ), null );	/ez nem megy!!!
	}

	public void cleanup()
	{
		if( light )
		{
			light.destroy();
			light = null;
		}

		if( cam )
		{
			cam.destroy();	//stop rendering right now
			light = null;
		}
	}

	//what to do when panel is focused
	public void focusHook()
	{
		if(button && invItem) button.setToolTip(invItem.getInfo()); //RAXAT: v2.3.1, tooltip text update patch
		else button.setToolTip(null);

		if( flags )
		{//rotating enabled?
			if( cam )
			{
				ypr.y+=0.03;
				Vector3 v = new Vector3( 0.0, size, size );
				v.rotate( new Ypr( ypr.y, 0.0, 0.0 ) );
				cam.setMatrix( v, ypr );
			}
		}
	}
}

