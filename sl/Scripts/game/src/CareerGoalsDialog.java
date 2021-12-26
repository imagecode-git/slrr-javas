package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;

//RAXAT: v2.3.1, reaching all goals unlocks cheats
public class CareerGoalsDialog extends Dialog
{
	Player player;

	ResourceRef res_badge_pos = new ResourceRef(frontend:0xD100r);
	ResourceRef res_badge_neg = new ResourceRef(frontend:0xD101r);

	public CareerGoalsDialog()
	{
		super(GameLogic.player.controller, DF_DEFAULTBG|DF_MODAL|DF_MEDIUM|DF_DARKEN, "ACHIEVEMENTS", "CLOSE");
		player = GameLogic.player;
		osd.globalHandler = this;
	}

	public void show()
	{
		super.show();

		float[] xpos = new float[2];
		float[] ypos = new float[2];
		float[] dlt  = new float[2];

		xpos[0] = -0.385;
		ypos[0] = 0.165;
		dlt[0]  = 0.075;

		xpos[1] = -0.62;
		ypos[1] = -0.5075;
		dlt[1]  = 0.205;

		float mrg = 0.25;
		ypos[0] -= 0.09;
		ypos[1] += 0.25;

		Number progress = GameLogic.careerComplete();
		ResourceRef res;

		for(int i=0; i<GameLogic.goals.length; i++)
		{
			if(progress.i & GameLogic.goals[i].flag || progress.f) res = res_badge_pos;
			else res = res_badge_neg;

			osd.createRectangle(xpos[0], ypos[0], 0.029, 0.0453, 3, res, 0);
			osd.createText(GameLogic.goals[i].desc, Frontend.largeFont_strong, Text.ALIGN_LEFT, xpos[1], ypos[1]).setColor(Palette.RGB_GREY);

			ypos[0] -= dlt[0];
			ypos[1] += dlt[1];
		}

		String str = "Complete all achievements to gain special bonus.";
		if(progress.f) str = "Well done! Find your reward in \"EXTRAS\".";

		osd.createText(str, Frontend.largeFont_strong, Text.ALIGN_CENTER, 0.0, ypos[1]).setColor(Palette.RGB_GREY);
	}
}