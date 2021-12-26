package java.game;

import java.util.*;
import java.util.resource.*;
import java.render.*;

//RAXAT: v2.3.1, human spectator
public class Spectator
{
	RenderRef	render;
	ResourceRef	bones;
	Animation	anim;

	String alias;
	int renderID, bonesID;
	Vector3 pos;
	Ypr ori;

	public Spectator(ResourceRef parent, int rID, int bID, Vector3 p, Ypr o, String a)
	{
		renderID = rID;
		bonesID = bID;
		alias = a;
		pos = p;
		ori = o;

		bones = new ResourceRef(bonesID);
		render = new RenderRef(parent, renderID, alias);
		anim = new Animation(render, bones);

		render.setMatrix(pos,ori);
	}

	//basic params
	public void beginAnim()
	{
		if(anim)
		{
			anim.setSpeed(0.5+Math.random());
			anim.loopPlay();
		}
	}

	public void setMatrix(Vector3 p, Ypr o)
	{
		if(render)
		{
			pos = p;
			ori = o;
			render.setMatrix(p,o);
		}
	}
}