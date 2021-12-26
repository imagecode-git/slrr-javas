package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;

//RAXAT: template for all dynamically generated career events
public class EventTemplate extends CareerEvent
{
	public EventTemplate();

	public void init()
	{
		super.init();
	}

	public int reqCheck(int reqId)
	{
		return 1;
	}
}
