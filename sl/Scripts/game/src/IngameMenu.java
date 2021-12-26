//v 1.10
package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;

//RAXAT: v2.3.1, updated ingame menu
public class IngameMenu extends Dialog
{
	Style	menuStyle = new Style(0.45, 0.12, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
	Menu	m;

	Player	player;
	Track	track;
	GameRef	activeCamera; //RAXAT: to be passed to OptionsDialog

	int	reqTrackExit;
	int	towCarFee = 0;
	int	pinksActive = 0;

	float	spacing;

	final static int	CMD_CONTINUE	= 0;
	final static int	CMD_OPTIONS	= 1;
	final static int	CMD_EXIT	= 2;

	final static int	MM_ROC		= 0x01;
	final static int	MM_CITYROAM	= 0x02;
	final static int	MM_TRACK	= 0x04;
	final static int	MM_GAMEMODE	= 0x08;
	final static int	MM_OTHER	= 0x10;

	public IngameMenu(Track t, int additionalFlags)
	{
		super(GameLogic.player.controller, additionalFlags|Dialog.DF_TUNING|Dialog.DF_DEFAULTBG|DF_MODAL|DF_FREEZE, "INGAME MENU", null);
		track = t;
		escapeCmd = CMD_CONTINUE;
		player = GameLogic.player;
		osd.globalHandler = this;
		spacing = Text.getLineSpacing(menuStyle.charset, osd.vp);
	}

	public void show()
	{
		Integrator.IngameMenuActive = 1;

		String defTxt = "RETURN TO GARAGE";
		String backTxt = defTxt;
		towCarFee = 0;

		if(GameLogic.gameMode == GameLogic.GM_CARREER)
		{
			if(Integrator.isCity && !track.game)
			{
				backTxt = "TOW CAR TO GARAGE FOR $";

				City cty = track;
				Vector3 distance = player.car.getPos();
				distance.sub(cty.posStart);
				towCarFee += distance.length()*0.03 + 10;

				if(cty.nightTime)
				{
					if(cty.nrPlayerRace == 2)
					{
						if(cty.nrPrize)
						{
							backTxt = "PAY BET AND " + backTxt;
							towCarFee += cty.nrPrize;
						}
						else
						{
							backTxt = "LOOSE CAR AND " + backTxt;
							pinksActive = 1;
						}
					}
				}
				else
				{
					if(cty.raceState)
					{
						if(cty.prize) backTxt = "PAY BET AND " + backTxt;
						else backTxt = "QUIT RACE AND " + backTxt;

						towCarFee += cty.prize;	
					}
				}


				if(cty.policeState)
				{
					backTxt = "PAY FINE AND " + backTxt;
					int[] fine = cty.calculateFineSum(0);
					towCarFee += fine[0];
				}

				backTxt = backTxt + towCarFee;

				if(towCarFee < 13) //if player is close to garage
				{
					towCarFee = 0;
					backTxt = defTxt;
				}
			}
			else
			{
				if(track instanceof ROCTrack)
				{
					if(((ROCTrack)track).testMode) backTxt = "EXIT TEST RUN";
					else backTxt = "GIVE UP RACE";
				}
				else
				{
					if(track.game && track.game.failable) backTxt = "GIVE UP RACE"; //if add-on gamemode is active (except freeride)
				}
			}
		}
		else
		{
			backTxt = "EXIT TO MAIN MENU";
			
			switch(GameLogic.gameMode)
			{
				case(GameLogic.GM_CARREER):		backTxt = defTxt;			break;
				case(GameLogic.GM_SINGLECAR):	backTxt = defTxt;			break;
				case(GameLogic.GM_FREERIDE):	backTxt = "EXIT TO MENU";	break;
			}
		}

		super.show();

		m = osd.createMenu(menuStyle, 0.0, spacing*(2-0.5)*(-1), 0);
		m.addItem("CONTINUE", CMD_CONTINUE);
		m.addItem("OPTIONS", CMD_OPTIONS);
		m.addItem(backTxt, CMD_EXIT);
	}
	
	public void hide()
	{
		Integrator.IngameMenuActive = 0;
		super.hide();
	}
	
	public void setActiveCamera(GameRef cam)
	{
		activeCamera = cam;
	}
	
	public void osdCommand(int cmd)
	{
		switch(cmd)
		{
			case(CMD_CONTINUE):
				super.osdCommand(0);
				break;

			case(CMD_OPTIONS):
				OptionsDialog od = new OptionsDialog(Dialog.DF_HIGHPRI);
				od.setActiveCamera(activeCamera);
				od.display();
				break;

			case(CMD_EXIT):
				if(GameLogic.gameMode == GameLogic.GM_CARREER && track instanceof City)
				{
					track.calculateFineSum(1);
					player.takeMoney(towCarFee);

					City cty = track;
					if(cty.policeState) Gamemode.updatePoliceStats(0); //v2.3.1, update police chase stats (busted++)

					if(pinksActive)	track.killCar = 1;

					GameLogic.spendTime(towCarFee*60);
				}

				reqTrackExit = 1;
				super.osdCommand(0);
				break;
		}
	}
}