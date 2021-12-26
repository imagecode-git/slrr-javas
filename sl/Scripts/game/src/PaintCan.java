package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;	//Text
import java.render.osd.*;	//Text
import java.sound.*;

//RAXAT: v2.3.1, I/O added
class PaintCan
{
	final static int MIN_CAPACITY = 600;

	int		color;
	float	capacity; //RAXAT: also amount of pigment inside the paintcan, since pigment can't be restored we equal it to capacity
	float	initialCapacity; //RAXAT: to refill the can
	
	public PaintCan(int color, float capacity, float initialCapacity)
	{
		int i=0; //RAXAT: fictive int, JVM will report syntax error without this for some reason
		this.color=color;
		this.capacity=capacity;
		this.initialCapacity=capacity;
	}

	//RAXAT: old constructor, kept for compatibility
	public PaintCan(int color, float capacity)
	{
		this(color, capacity, capacity);
	}

	public void save(File saveGame)
	{
		saveGame.write(color);
		saveGame.write(capacity);
		saveGame.write(initialCapacity);
	}

	public static PaintCan createFromFile(File saveGame)
	{
		PaintCan result = new PaintCan();

		result.color = saveGame.readInt();
		result.capacity = saveGame.readFloat();
		result.initialCapacity = saveGame.readFloat();

		return result;
	}
	
	public void refill()
	{
		capacity = initialCapacity;
	}
}
