package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;

public class Freeride extends Gamemode
{
	public Freeride(){}

	public void init()
	{
		name = "Freeride";
		shieldIcon = new ResourceRef(sl.Scripts.game.Gamemodes.stdpack_231:0x1006r);
	}

	public void launch()
	{
		failable = 0; //events for this gamemode cannot have a "failed" status when exit
		gmcEvent.useCamAnimation = 0;

		//override GPS frame created by Track()
		Navigator nav = gmcEvent.track.nav;
		if(!nav || (nav && nav.type>Navigator.TYPE_CLASSIC))
		{
			GameLogic.player.gpsState = 0;
			GameLogic.player.handleGPSframe(GameLogic.player.gpsState);
		}

		usePolice = 0;

		if(useDebug) enableAnimateHook();

		super.launch();

		showMinimap();
	}

	public void animate()
	{
		if(useDebug) //enable debug in Gamemode.class to use material index detection
		{
			if(GameLogic.player.car.chassis.getMaterialIndex() >= 0)
				debug("matIndex: " + GameLogic.player.car.chassis.getMaterialIndex() + ", name: " + gTrack.map.getMaterialName(GameLogic.player.car.chassis.getMaterialIndex()));
		}

		super.animate();
	}

	public void finalize()
	{
		if(useDebug) disableAnimateHook();
		super.finalize();
	}
}