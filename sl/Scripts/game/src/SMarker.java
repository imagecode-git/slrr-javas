package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class SMarker extends Marker
{
	//clampales miatt szuks.
	Vector3		pos;

	public SMarker( RenderRef s, Vector3 p ){ super(s); pos=p; }
}

