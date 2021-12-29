package java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.differential;

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

public class Transaxle_Differential extends Differential
{
	public Transaxle_Differential();

	public Transaxle_Differential(int id)
	{
		super(id);

		name = "Transaxle differential";

		drive_base_title = "left";
		drive_comp_title = "right";
	}
}
