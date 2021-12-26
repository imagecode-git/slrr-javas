package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

//RAXAT: special transition class, for optimizing loading speeds
//FIX LOADING SCREENS!
public class Transit implements GameState
{
	Portal gate;
	int portalData, picId;

	public Transit( int pd )
	{
		portalData = pd;
	}

	public Transit( int pd, int pid )
	{
		portalData = pd;
		picId = pid;
	}

	public void enter( GameState prevState )
	{
		if(picId)
			gate = new Portal(portalData, picId);
		else
			gate = new Portal(portalData);

		gate.myPrevState = prevState;
		gate.show();
	}

	public void exit( GameState nextState )
	{
		gate.hide();
		gate = null;
	}
}

public class Portal extends OptionsDialog
{
	static	GameState myPrevState;
	int pData;
	int pPic;

	public Portal( int p )
	{
		super(0);
		pData = p;
	}

	public Portal( int pd, int pp )
	{
		super(0);
		pData = pd;
		pPic = pp;
	}

	public void show()
	{
		if(pPic)
		{
			Frontend.loadingScreen.show(new ResourceRef(pPic), 1, 0x00D); //crossfade (black, turbo-speed), no-bck
			Frontend.loadingScreen.userWait( 2.0 );
		}
		else
			osd.createBG( new ResourceRef(frontend:0xE0E2r) ); //default_noir.png

		if(pData == 0x0001) //load career from main menu
		{
			Sound.changeMusicSet( Sound.MUSIC_SET_NONE );
			GameLogic.carrerInProgress = 1;
			Integrator.clubInfoCheat = 0;
			GameLogic.changeActiveSection(GameLogic.garage);
		}

		if(pData == 0x1001) //load car from main menu
		{
			Sound.changeMusicSet( Sound.MUSIC_SET_NONE );
			GameLogic.loadDefaults();
			GameLogic.gameMode = GameLogic.GM_SINGLECAR;
			player.setMoney(999999);
			player.car = Vehicle.load( Integrator.transitString, player );
			GameLogic.carrerInProgress = 1;
			Integrator.clubInfoCheat = 0;
			Integrator.transitString = null;
			GameLogic.changeActiveSection(GameLogic.garage);
		}
	}

	public void hide()
	{
		super.hide();

		if(pPic)
		{
			Frontend.loadingScreen.show(new ResourceRef(pPic), 1, 0x10D); //crossfade (black, turbo-speed), no-bck
			Frontend.loadingScreen.userWait( 1.0 );
		}
	}
}
