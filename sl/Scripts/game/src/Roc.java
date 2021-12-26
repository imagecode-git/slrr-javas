package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class Roc
{
	final static int COMPETITORS = 16;
	final static int ROUNDS = 4;

	Object[]		rounds = new Object[ROUNDS];
	String[]		carNames = new String[ROUNDS];
	String[]		splineNames = new String[ROUNDS];
	String[]		aiParams = new String[ROUNDS];
	final static String	carSaveDir = "cars/racers/ROC_cars/";

	final static String[]	roundNames = new String[ROUNDS];

	int	actRound;		//selejtezok+dontok
	int	lostRuns, wonRuns;	//futamok (max3, min2 koronkent)
	int	init = 1;

	public Roc( Player player )
	{
		roundNames[0] = "final";
		roundNames[1] = "semi finals";
		roundNames[2] = "quarter-finals";
		roundNames[3] = "qualifying rounds";

		Racer[] round;
		int c = COMPETITORS;

		for( int i=0; i<ROUNDS; i++ )
		{
			round = new Racer[c];
			rounds[i] = round;

			c/=2;
		}

		int r;
		if (Math.random() < 0.5) {
			carNames[0] = "roc_SD";
			aiParams[0] = " 8.1 0.36";
			r = Math.random()*1;
		} else {
			carNames[0] = "roc_ST";
			aiParams[0] = " 4.0 0.4";
			r = Math.random()*1;
		}
		splineNames[0] = carNames[0]+".spl";
		carNames[0] = carNames[0]+"_"+(r+1);

		if (Math.random() < 0.5) {
			carNames[1] = "roc_MC";	
			aiParams[1] = " 5.4 0.36";
			r = Math.random()*1;
		} else {
			carNames[1] = "roc_SS";
			aiParams[1] = " 7.5 0.4";
			r = Math.random()*1;
		}
		splineNames[1] = carNames[1]+".spl";
		carNames[1] = carNames[1]+"_"+(r+1);

		if (Math.random() < 0.5) {
			carNames[2] = "roc_GT2";	
			aiParams[2] = " 7.5 0.42";
			r = Math.random()*1;
		} else {
			carNames[2] = "roc_DS";
			aiParams[2] = " 7.5 0.42";
			r = Math.random()*2;
		}
		splineNames[2] = carNames[2]+".spl";
		carNames[2] = carNames[2]+"_"+(r+1);

		carNames[3] = "roc_GT3";	
		aiParams[3] = " 8.0 0.4";
		r = Math.random()*1;
		splineNames[3] = carNames[3]+".spl";
		carNames[3] = carNames[3]+"_"+(r+1);

		round = rounds[0];
		round[0] = player;
		int diff;
		for( int k=1; k<COMPETITORS; k++ )
		{
			//bots with random name and character
			if(Racer.RID_FEJ+k+diff == player.character.id()) diff++;

			Bot b = new Bot( k+diff, Math.random()*10000, Math.random(), 2.0, 2.0, 1.0);
			b.botVd = GameLogic.getVehicleDescriptor( VehicleType.VS_RRACE, 1.0 );

			round[k] = b;
		}

	}

	public int numRounds()
	{
		return rounds.length;
	}

	public int getCurrentRound()
	{
		return actRound;
	}

	public String getCurrentCarName()
	{
		return carSaveDir+carNames[actRound];
	}

	public String getCurrentSplineName()
	{
		return carSaveDir+splineNames[actRound];
	}

	public String getCurrentAIParam()
	{
		return "AI_params "+aiParams[actRound];
	}

	public int isFinalRound()
	{
		return actRound == ROUNDS-1;
	}

	public int isRoundClosed()
	{
		//sikerult eldonteni, ki gyozott ebben a korben? (max 3 menet)
		return (lostRuns == 2 || wonRuns == 2 );
	}

	public Bot getNextOpponent()
	{
		Bot opponent;
		Racer[] round;

		round = rounds[actRound];
		opponent = round[1];

		return opponent;
	}

	public void closeRun()
	{
		Racer[] round, round2;

		int roundFinished = isRoundClosed();

		if( roundFinished )
		{
			lostRuns=0;
			wonRuns=0;


			if( actRound+1 < ROUNDS )
			{
				round = rounds[actRound];
				round2 = rounds[actRound+1];

				round2[0] = round[0];	//ha vesztett a player, ugyse latja
				for( int l=round2.length-1; l; l-- )
				{
					Racer winner;
					if( Math.random() > 0.5 )
						winner = round[l*2];
					else
						winner = round[l*2+1];

					round2[l]=winner;
				}
			}

			
			actRound++;
		}
	}

}

