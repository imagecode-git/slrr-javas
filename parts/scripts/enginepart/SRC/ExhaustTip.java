package java.game.parts.enginepart;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;

//todo: spawn particle system through the native code
public class ExhaustTip extends EnginePart
{
	final static String	PST_NORMAL	= "exhaust_smoke";
	final static String	PST_HOT		= "nitrogen_hot";
	final static String	PST_MIXED	= "nitrogen_mix";
	
	RenderRef		psRes; //cfg instance
	ParticleSystem	psObject;
	Vector3			psVelocity = new Vector3( 0.0, 0.0, 3.0 ); //def. velocity
	Vector3			psDelta; //adjusted pos
	float			psAmount = 10.0;
	float			psDSMinRnd = 0.1; //density randomness
	float			psDSMaxRnd = 1.0;
	float			psV3MinRnd = 0.0; //velocity randomness
	float			psV3MaxRnd = 0.1;
	float			upperRaiseDelta = 0.0;

	SfxRef			nosBlowLoop = new SfxRef(multibot.sounds:0x00002040r);
	int				psTypeInt = multibot.scripts:0x00001021r;
	int				nosBlowLoopID = multibot.sounds:0x00002040r;

	String			psTypeCmd = PST_HOT;

	public ExhaustTip( int id )
	{
		super( id );

		name = "Exhaust tip";

		if(psTypeCmd)
		{
			int type = 0;
			
			switch(psTypeCmd)
			{
				case(PST_NORMAL):
					type = multibot.scripts:0x00001024r;	break; //grey smoke
					
				case(PST_HOT):
					type = multibot.scripts:0x00001021r;	break; //normal fire
					
				case(PST_MIXED):
					type = multibot.scripts:0x00001022r;	break; //blue fire
			}
			
			psTypeInt = type;
		}
	}

	public void destroyAllParticles()
	{
		if(psObject)
		{
			psObject.destroy();
			psObject = null;
		}
	}

	public void createExhaustPS()
	{
		if(!psDelta) psDelta = new Vector3(0.0f);
		Vector3 pos = this.getPos().add(psDelta);
		upperRaiseDelta = 2;
		
		psRes = new RenderRef(psTypeInt);
		psVelocity = new Vector3(this.getOri().y, this.getOri().p*upperRaiseDelta, this.getOri().r);
		psObject = new ParticleSystem(this, psRes);
		psObject.setDirectSource("exhaust_PS (" + name + ")", pos, psV3MinRnd, psV3MaxRnd+Math.random()/7, psVelocity, psDSMinRnd, psDSMaxRnd, psAmount, null);
		nosBlowLoop.play(pos, 2.0, 2.0, 2.0, 130);
	}

}