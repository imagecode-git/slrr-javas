package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.camshaft;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.slidingenginepart.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.game.parts.rgearpart.*;

public class SOHC_Camshaft extends Camshaft
{
	public SOHC_Camshaft(){}

	public SOHC_Camshaft( int id )
	{
		super( id );

		name = "SOHC camshaft";
	}
}
