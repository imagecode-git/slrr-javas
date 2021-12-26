package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class Marker
{
	final static RenderRef	RR_PLAYER	= new RenderRef( frontend:0x005Cr );
	final static RenderRef	RR_CAR1		= new RenderRef( frontend:0x0059r );
	final static RenderRef	RR_CAR2		= new RenderRef( frontend:0x006br );
	final static RenderRef	RR_CAR3		= new RenderRef( frontend:0x006cr );
	final static RenderRef	RR_CAR4		= new RenderRef( frontend:0x006dr );
	final static RenderRef	RR_POLICE	= new RenderRef( frontend:0x005dr );
	final static RenderRef	RR_START	= new RenderRef( frontend:0x0060r );
	final static RenderRef	RR_FINISH	= new RenderRef( frontend:0x005Ar );
	final static RenderRef	RR_GARAGE1	= new RenderRef( frontend:0x005br );
	final static RenderRef	RR_GARAGE2	= new RenderRef( frontend:0x005Fr );
	final static RenderRef	RR_GARAGE3	= new RenderRef( frontend:0x0073r );

	//RAXAT: v2.3.1, custom markers
	final static RenderRef	RR_CAMERA	= new RenderRef( frontend:0xF0F4r );
	final static RenderRef	RR_TELEPORT	= new RenderRef( frontend:0xF0F9r );
	final static RenderRef	RR_TARGET	= new RenderRef( frontend:0xF0FCr );
	final static RenderRef	RR_TARGET_BLACK	= new RenderRef( frontend:0xF0FEr );

	//RAXAT: GPS v4
	final static RenderRef	RR_V4_PLAYER	= new RenderRef( frontend:0xD177r );
	final static RenderRef	RR_V4_BOT	= new RenderRef( frontend:0xD178r );
	final static RenderRef	RR_V4_START	= new RenderRef( frontend:0xD179r );
	final static RenderRef	RR_V4_FINISH	= new RenderRef( frontend:0xD17Ar );

	RenderRef	symbol;

	public Marker( RenderRef s )
	{ 
		symbol=s; 
	}
}
