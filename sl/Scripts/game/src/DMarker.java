package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

public class DMarker extends Marker
{
	GameRef		mover;

	public DMarker( RenderRef s, GameRef m ){ super(s); mover=m; }
}
