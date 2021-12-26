package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class Decal extends ResourceRef
{
	int	stickies = 5;	

	public Decal( String fileName )
	{
		makeTexture( new ResourceRef(system:0x0008r), fileName );
	}

	public Decal( int resid )
	{
		super(resid);
	}
}