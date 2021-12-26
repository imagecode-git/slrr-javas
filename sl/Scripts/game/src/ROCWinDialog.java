package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

//RAXAT: v2.3.1, dedicated ROC win dialog
public class ROCWinDialog extends Dialog
{
	Player player;

	Rectangle	rect_congrats_title, rect_msg, rect_holder, rect_vhc_title, rect_vhc_img;
	Osd			losd;
	int			code; //ROC vehicle index
	Text		messageText;
	Thread		flashTextThread;
	ROCTrack	track;
	SfxRef bckLoop;
	int		kill;

	public ROCWinDialog(ROCTrack rt)
	{
		this();
		track = rt;
	}

	public ROCWinDialog()
	{
		super(GameLogic.player.controller, DF_FULLSCREEN|DF_LOWPRI|DF_HIDEPOINTER|DF_DARKEN, null, null);
		player = GameLogic.player;
		osd.globalHandler = this;

		losd = new Osd(1.0, 0.0, priority+2);
		code = GameLogic.decodeROC();

		Input.cursor.enable(1);

		flashTextThread = new Thread(this, "Dialog text flasher");
		flashTextThread.start();
	}

	public void show()
	{
		//remapping hotkeys to local OSD
		losd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, osd.defSelection, this);
		losd.createHotkey(Input.AXIS_SELECT, Input.VIRTUAL, osd.defSelection, this);

		super.show();

		losd.show();

		rect_congrats_title = losd.createRectangle(-0.75, -0.55, 1, 0.3, 1, new ResourceRef(frontend:0x0000940Dr));
		rect_msg = losd.createRectangle(0.75, -0.325, 1.25, 0.25, 1, new ResourceRef(frontend:0x0000940Fr));
		rect_holder = losd.createRectangle(0.75, 0.4, 2.05, 0.7, 1, new ResourceRef(frontend:0x0000940Er));

		int resid = frontend:0x00009410r;
		rect_vhc_title = losd.createRectangle(-1.3175, 0.4, 0.85, 0.55, 1, new ResourceRef(resid+code));

		resid = frontend:0x00009420r;
		rect_vhc_img = losd.createRectangle(1.25, 0.45, 0.825, 1.45, 1, new ResourceRef(resid+code));

		messageText = losd.createText(null, Frontend.largeFont, Text.ALIGN_RIGHT, 0.95, 0.825);

		bckLoop = new SfxRef("frontend\\sounds\\misc\\roc_win.wav", Sound.getVolume(Sound.CHANNEL_MUSIC), 7.65);
		bckLoop.loopPlay();

		float step = 0.1;
		int steps = 20;
		int dir = 1;

		rect_congrats_title.setupAnimation(step, steps, dir, "X");
		rect_congrats_title.runThread();

		rect_msg.setupAnimation(step, steps, -dir, "X");
		rect_msg.runThread();

		rect_holder.setupAnimation(step, steps, -dir, "X");
		rect_holder.runThread();

		rect_vhc_title.setupAnimation(step, steps, dir, "X");
		rect_vhc_title.runThread();

		rect_vhc_img.setupAnimation(step, steps, -dir, "X");
		rect_vhc_img.runThread();
	}

	public void osdCommand(int cmd)
	{
		if(cmd == osd.defSelection) hide();
	}

	public void run()
	{
		for(;;)
		{

			messageText.changeText(null);
			flashTextThread.sleep(600);

			messageText.changeText("PRESS ENTER TO CONTINUE...");
			flashTextThread.sleep(600);

			if(kill)
			{
				messageText.changeText(null);
				flashTextThread.sleep(400);

				losd.hide();
				losd = null;

				bckLoop.loopStop();
				if(track) track.notify();

				super.hide();

				kill = 0;
				flashTextThread.stop();
				flashTextThread = null;
			}
		}
	}

	public void hide()
	{
		float step = 0.2;
		int steps = 40;
		int dir = -1;

		rect_congrats_title.restartAnimation("X");
		rect_congrats_title.setupAnimation(step, steps, dir, "X");
		rect_congrats_title.runThread();

		rect_msg.restartAnimation("X");
		rect_msg.setupAnimation(step, steps, -dir, "X");
		rect_msg.runThread();

		rect_holder.restartAnimation("X");
		rect_holder.setupAnimation(step, steps, -dir, "X");
		rect_holder.runThread();

		rect_vhc_title.restartAnimation("X");
		rect_vhc_title.setupAnimation(step, steps, dir, "X");
		rect_vhc_title.runThread();

		rect_vhc_img.restartAnimation("X");
		rect_vhc_img.setupAnimation(step, steps, -dir, "X");
		rect_vhc_img.runThread();

		messageText.changeText(null);
		osd.darken();
		kill++;
	}
}