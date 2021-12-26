package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

//RAXAT: v2.3.1, new race dialog
public class RaceDialog extends CsDialog
{
	Menu		m;

	Rectangle	bck;
	TextBox		messageText;
	Text[]		timeTxt = new Text[2];

	int[]		profileID = new int[2];
	String[]	profileNames = new String[2];
	Rectangle[]	profileRect = new Rectangle[2];
	float[]		times = new float[2];
	String[]	timeStr = new String[2];

	public RaceDialog(Controller ctrl, ResourceRef background, int rid1, int rid2, String name1, String name2, float time1, float time2, int param, int mode)
	{
		/*--------------------------
		param is bet in night race, roc infocode in roc
		mode==0: night race start
		mode==1: night race end
		mode==2: roc start
		mode==3: roc end
		mode==4: unused
		mode==5: dayrace end
		--------------------------*/

		super(ctrl, Dialog.DF_MODAL|Dialog.DF_FULLSCREEN|Dialog.DF_DARKEN|Dialog.DF_FREEZE, background);

		bck = osd.createRectangle(0.0, -0.7, 1.35, 0.635, -1, new ResourceRef(frontend:0x00009405r));

		float xpos = -0.524;
		float ypos = -0.712;

		profileID[0] = rid1;
		profileID[1] = rid2;

		profileNames[0] = name1;
		profileNames[1] = name2;


		times[0] = time1;
		times[1] = time2;

		for(int i=0; i<profileNames.length; i++)
		{
			profileRect[i] = osd.createRectangle(xpos, ypos, 0.21, 0.465, 1, new ResourceRef(profileID[i]));

			xpos += 1.0367;
		}


		if(mode == 1 || mode == 3)
		{
			for(int j=0; j<times.length; j++)
			{
				String timeString;

				if(times[j] == 1000.0) timeString = "--:--:--";
				else timeString = String.timeToString(times[j], String.TCF_NOHOURS);

				timeStr[j] = timeString;
			}

			xpos = -0.525;
			ypos = -0.59;

			for(int k=0; k<timeTxt.length; k++)
			{
				timeTxt[k] = osd.createText(timeStr[k], Frontend.smallFont, Text.ALIGN_CENTER, xpos, ypos);
				xpos += 1.041;
			}
		}

		String msg;

		switch(mode)
		{
			case 0:
				msg = profileNames[0] + " racing against " + profileNames[1] + " \n The bet is: ";
				break;

			case 1:
				msg = profileNames[0] + " has won against " + profileNames[1] + " \n The prize is: ";
				break;

			case 2:
				msg = profileNames[0] + " racing against " + profileNames[1];
				break;

			case 3:
				msg = profileNames[0] + " has won against " + profileNames[1];
				break;
				
			default:
				msg = profileNames[0] + " has won against " + profileNames[1] + " \n The prize is: ";
		}

		if(mode == 0 || mode == 1  || mode == 5)
		{
			if(param == 0)
			{
				if(mode == 5) msg += "PRESTIGE ONLY";
				else msg += "PINK SLIPS";
			}
			else msg += "$" + param;
		}
		else
		if(mode == 2)
		{
			msg += " \n \n Run " + param + " of 3";
		}

		ResourceRef charset = Frontend.mediumFont;
		float spacing = Text.getLineSpacing(charset, osd.vp);

		ypos = -1.0+(Config.video_y*0.00018)*osd.getViewport().getHeight()+spacing*1.5;
		messageText = osd.createTextBox(msg, charset, Text.ALIGN_CENTER, -0.382, ypos, 0.75);
		messageText.verticalCenter();

		if(mode == 3)
		{
			int curLost;
			if(param < 0)
			{
				curLost = 1;
				param = -param;
			}

			int round = param % 10; param = param/10;
			int lrun = param % 10; param = param/10;
			int wrun = param;
			int roundover = (lrun == 2 || wrun == 2);
			int champion = (round == 1 && wrun == 2);
			int shortcut = roundover && (lrun+wrun == 2);

			if(champion)
			{
				msg = "Congratulations, you won the Race of Champions!";
				if(curLost) msg += " \n (Thought you lost your last run.)";

			}
			else
			if(roundover)
			{
				if(curLost) msg = "You've lost this race and the Race of Champions. The next event will take place in 6 months time.";
				else msg = "Congratulations, you qualified to the " + Roc.roundNames[round-1] + " of the Race of Champions!";

				if(shortcut) msg += " \n (Run 3 is skipped)";
			}
			else
			{
				if(curLost) msg = "You've lost this run, but not this round. Go for the next run!";
				else msg = "Congratulations, you've won this run. Go for the next run!";
			}

			osd.createRectangle(0.0, 0.77, 1.35, 0.46, -1, new ResourceRef(frontend:0x00009406r));
			continueString = "PRESS ENTER";

			float tmp = 3;
			if(curLost) tmp = 2.6;

			ypos = 1.0-(Config.video_y*0.000085)*osd.getViewport().getHeight()-spacing*tmp;
			messageText = osd.createTextBox(msg, Frontend.mediumFont, Text.ALIGN_CENTER, -0.605, ypos, 1.2);
			messageText.verticalCenter();
		}
	}

}