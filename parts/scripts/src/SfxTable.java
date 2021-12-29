package java.game.parts;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;

public class SfxTable extends Native
{
//	public native void finalize( );	//ha _ref lesz
	public native int getItems( );
	public native void clear( );
	public native void addItem( ResourceRef sfx, float pitch, float pmin, float pmax, float vmin, float vmax );
}

