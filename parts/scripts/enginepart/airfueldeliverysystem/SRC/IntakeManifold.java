package java.game.parts.enginepart.airfueldeliverysystem;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;
import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.rgearpart.*;
import java.game.parts.enginepart.airfueldeliverysystem.charger.*;

public class IntakeManifold extends AirFuelDeliverySystem
{
	int	fuel_rail_slot_ID = 0;
	int	converter_slot_ID = 0;
	float	efficiency		= 1.0; // flow efficiency //

	public IntakeManifold( int id )
	{
		super( id );

		name = "Intake manifold";

		prestige_calc_weight = 25.0;
	}

	public FuelInjectorSystem getFuelRail()
	{
		if (fuel_rail_slot_ID <= 0)
			return null;

		Part res = partOnSlot(fuel_rail_slot_ID);

		if (res && res instanceof FuelInjectorSystem)
			return (FuelInjectorSystem)res;
//		else
//			System.log("!!!FuelInjectorSystem required on slot!!!");

		return null;
	}

	public String isDynoable()
	{
		if (fuel_rail_slot_ID)
			if (!partOnSlot(fuel_rail_slot_ID))
				return "It's missing the fuel system.";

		return super.isDynoable();
	}

	//RAXAT: should not be located here, remove this
	public Part getConverter()
	{
		if(converter_slot_ID) return partOnSlot(converter_slot_ID);
		return null;
	}
	
	//RAXAT: build 933, always returns null, required by Block_Vee.class
	public Part getIntakeManifold()
	{
		return null;
	}
	
	public Part getSuperCharger()
	{
		int	slots, slotIndex, slotID, otherSlotID, parentSlotID;
		Part attachedPart = null;
		slots = getSlots();
		parentSlotID = getSlotID( -1 );
		
		for(slotIndex = 0; slotIndex < slots; slotIndex++)
		{
			slotID = getSlotID(slotIndex);
			if(slotID != parentSlotID)
			{
				attachedPart = partOnSlot(slotID);
				if(attachedPart instanceof SuperCharger) return attachedPart;
			}
		}
		
		return null;
	}
}