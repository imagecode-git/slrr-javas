package java.game.parts;

import java.lang.Integer.*; //RAXAT: for Integer.getHex(I)
import java.game.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.parts.bodypart.*;
import java.game.cars.*;

//RAXAT: v2.3.1, various improvements, see code
//RAXAT: positions of all parts are adjustable since build 928
public class Part extends GameType
{
	final static int COMMON = 0;
	final static int PACKAGE = 1;

	static float PRESTIGE_DOWNGRADE_MUL=1.08;
	static float PRESTIGE_VALUE_MUL=0.0121875;

	final static int MF_STOCK 	= 0x00000000;
	final static int MF_HOLLEY	= parts:0x0035r;
	final static int MF_HOLLEY2	= parts:0x0035r;
	final static int MF_AUTHORITY	= parts:0x0036r;
	final static int MF_NEUSPEED 	= 0;
	final static int MF_BILLSTEIN 	= parts:0x0033r;
	final static int MF_OZ 		= 0;
	final static int MF_BAER	= parts:0x0037r;
	final static int MF_BFGOODRICH 	= 0;
	final static int MF_BFGOODRICH2	= 0;
	final static int MF_BFGOODRICH3	= 0;
	final static int MF_BORLA	= parts:0x003Ar;
	final static int MF_NOS		= parts:0x003Br;
	final static int MF_HR		= parts:0x003Cr;
	final static int MF_JSP		= parts:0x003Dr;
	final static int MF_JSP2	= parts:0x003Dr;
	final static int MF_MICHELIN	= 0;
	final static int MF_MICHELIN2	= 0;
	final static int MF_PROCHARGER	= parts:0x0042r;
	final static int MF_PROCHARGER2 = parts:0x0042r;
	final static int MF_KN		= parts:0x0044r;
	final static int MF_LAMA	= parts:0xAA00r;

	//final static int SAVEFILEVERSION_PART = 2; //old!
	final static int SAVEFILEVERSION_PART = 3; //drag_matrix added

	String	name = "<unnamed>";
	String	description = "<description not available yet>";

	float	value = 100.0;	//price when in perfect condition
	float	police_check_fine_value = 0.0; // ennyit fizetsz ha a rendor lefulel - Sala
	float	thoroughness_limit	= 0.0; // barmikor eszreveszi a rendor ha akarja - Sala
	float	brand_new_prestige_value = 10.0;
	float	prestige_calc_weight = 25.0;
	float	price_catalog	= 1.0;	//price multipliers
	float	price_shop	= 0.9;	//rel. to catalog
	float	price_used	= 0.7;	//rel. to catalog
	float	price_repair	= 0.8;	//rel. to price diff.

	Ypr	catalog_view_ypr= null;

	float	repair_min_wear = 0.1;	//below this % it can't be repaired
	float	repair_min_tear = 0.1;	//below this % it can't be repaired

	float	repair_max_wear = 0.95;	// for what % can it be repaired
	float	repair_max_tear = 0.95;	// for what % can it be repaired

	int		carCategory = COMMON;	//ez lesz helyette
	int		manufacturer = MF_STOCK;

	int	need_update = 1;
	int	banned; //RAXAT: v2.3.1, forces part to be _not_ shown in the catalog
	int file_ver = SAVEFILEVERSION_PART;

	float	drag_occluded = 0.0;	// mennyit takar ki a parentjebol
	float	drag_own = 0.0;		// es mennyi a sajatja (ha senki nem takarja)
	Vector3	drag_own_center = new Vector3(0,0,0);	//position (ha senki nem takarja)
	float	C_drag = 0.0;		// az aktualis (kitakarva a childjei altal)
	Vector3	drag_center = new Vector3(0,0,0);	//actual position
	
	//RAXAT: new position, adjusted by player
	Pori	drag_matrix = new Pori(new Vector3(0), new Ypr(0));

	//res.enginebol
	public Part( int id )
	{
//		errorVM("attempting to create part: " + Integer.getHex(id)); //RAXAT: build 900, drops a dump with JVM class name and id of created part in 0x00000000 format
		//super( id );
	}

	public String installCheck( Part p, int[] slotId )
	{
		return null;
	}

	public String isDynoable()
	{
		return null;
	}
	
	public String isDriveable()
	{
		return null;
	}
	
	public int getInfo( int query )
	{ 
		return getInfo( query, 0 ); //igy elerjuk a hivatkozott peldany getinfojat!!! :)))
	}

	//no def tuning 
	public int isTuneable(){ return 0; }

	//---------------------------------------------------------part load/save
	//static loader
	public static Part createFromFile( File saveGame, GameRef world )
	{
		int id = saveGame.readResID();
		if( id )
		{
			int version = saveGame.readInt();

			GameRef xa = new GameRef();	//getScriptInstance forbidden!!
			Part part = xa.create( world, new GameRef(id),	"100,0,0,0,0,0", "loaded carpart" );
			part.file_ver = version;

			//ToDo: ha nem jott letre, hibauzenet (pl. unknown part type), abort load, back to garage vagy menu
			//if( part ) //amig nincs kesz, pukkanjon meg inkabb itt, kulonben mindenfele unexpected hibat okoz!!!
				part.load( saveGame );

			return part;
		}

		return null;
	}


	public void load( File saveGame )
	{
		setTexture( saveGame.readResID() );
		setMesh( saveGame.readResID() );

		setWear( saveGame.readFloat() );
		setTear( saveGame.readFloat() );
		
		//RAXAT: loading adjusted position/rotation for all parts since file version 3
		if(file_ver >= 3)
		{
			//todo: add this as String constructor to Vector3 and Ypr
			String s = saveGame.readString();
			//System.log(s);
			StringTokenizer t = new StringTokenizer(s, ",");
			Vector3 pos = new Vector3();
			
			if(t.countTokens() > 1)
			{
				pos.x = t.nextToken().floatValue();
				pos.y = t.nextToken().floatValue();
				pos.z = t.nextToken().floatValue();
			}
			drag_matrix.pos = pos;
			
			s = saveGame.readString();
			//System.log(s);
			t = new StringTokenizer(s, ",");
			Ypr ori = new Ypr();
			
			if(t.countTokens() > 1)
			{
				ori.y = t.nextToken().floatValue();
				ori.p = t.nextToken().floatValue();
				ori.r = t.nextToken().floatValue();
			}
			drag_matrix.ori = ori;
			
			setSlotPos(getSlotID(-1), drag_matrix.pos, drag_matrix.ori);
		}
		
		String dmg;
		int slotIndex, slotID, otherSlotID;

		while( (slotIndex=saveGame.readInt()) != -1 )
		{
			dmg = saveGame.readString();
			otherSlotID = saveGame.readInt();

			setSlotDamage( slotIndex, dmg );

			if( otherSlotID )
			{
				Part attachedPart = Part.createFromFile( saveGame, getParent() );
				attachedPart.file_ver = file_ver;
				
				slotID = getSlotID( slotIndex ); //cursor, kire, melyik slotommal, pontosan kire, melyik slotjara
				attachedPart.command( "install " + 0 + " " + id() + " " + otherSlotID + " " + id() + " " + slotID );
				
				if(file_ver >= 3) attachedPart.setSlotPos(attachedPart.getSlotID(-1), attachedPart.drag_matrix.pos, attachedPart.drag_matrix.ori); //RAXAT: file version 3, applying adjusted position/rotation to attached parts
			}
		}
	}

	public void save( File saveGame )
	{
		//header: to be read by static loader 'createFromFile()'
		saveGame.write( new GameRef( getInfo( GII_TYPE )));
		saveGame.write( SAVEFILEVERSION_PART );

		//body: to be read by instance method 'load()'
		int texID = getTexture();
		ResourceRef texture = new ResourceRef(texID);
		saveGame.write( texture );
		int mshID = getMesh();
		ResourceRef mesh = new ResourceRef(mshID);
		saveGame.write( mesh );

		saveGame.write( getWear() );
		saveGame.write( getTear() );
		
		//RAXAT: file version 3, saving adjusted position/rotation for all parts
		saveGame.write(drag_matrix.pos.toString());
		saveGame.write(drag_matrix.ori.toString());

		int	slots, slotIndex, slotID, otherSlotID, parentSlotID;
		Part attachedPart;
		String dmg;
		slots = getSlots();
		parentSlotID = getSlotID( -1 );
		for( slotIndex=0; slotIndex<slots; slotIndex++ )
		{
			slotID = getSlotID( slotIndex );

			if( slotID != parentSlotID )	//elzarjuk a rekurzio utjat visszafele
			{
				dmg = getSlotDamage( slotIndex );
				otherSlotID = slotIDOnSlot( slotID );

				if( dmg!=null || otherSlotID )
				{
					saveGame.write( slotIndex );
					saveGame.write( dmg );
					saveGame.write( otherSlotID );

					if( otherSlotID )
					{
						attachedPart=partOnSlot( slotID );
						attachedPart.save( saveGame );
					}
				}
			}
		}
		saveGame.write( -1 );	//EndOfSlots

		//any part specific data should be written at this point by the curresponding parts' save() method
	}
	//--------------------------------------------------------------------------------------



	//----------------
	//lista, csak a gc miatt
	//kicsit kesobb: mar ne csak a gc miatt..
	//meg kesobb: at kell tenni a partba, es alkatreszenkent szamontartani a racsatlakozokat..
	Vector	attachedParts = new Vector();
	float	prev_prestige = 0.0;
	
	//a rendszer(g_car.cpp) hivja alkatreszek be/kiszerelodeskor
	//nehogy eldobja aztan a gc
	//-megcsinaljuk create-el (van ra refunk!)
	//-beszerelesre atadjuk (ekkor elveszhet a ref, de a gc meg nem futhat)
	//-beszereles meghivja ezt a rutint, ujra van ra ref!
	//-beszerelo visszater a scriptbe, gc mar nem tudja elhajitani

	public void addpart( GameRef ref )
	{
		attachedParts.addElement( ref.getScriptInstance() );
		prev_prestige = 0.0;
	}
	public void rempart( GameRef ref )
	{
		attachedParts.removeElement( ref.getScriptInstance() );
		prev_prestige = 0.0;
	}
/*
	public void addpart( int id )
	{
		GameRef ref = new GameRef( id );
		attachedParts.addElement( ref.getScriptInstance() );
	}
	public void rempart( int id )
	{
		GameRef ref = new GameRef( id );
		attachedParts.removeElement( ref.getScriptInstance() );
	}
*/	//----------------

	public int isStreetLegal()
	{
		return (police_check_fine_value==0.0);
	}

	//egy alkatresz, vagy tobb darabbol osszeszerelt cucc?
	public int isComplex()
	{
		if(attachedParts) return attachedParts.size(); //RAXAT: patch, attachedParts could be null
		return 0;
	}

	public float calcPoliceFine( float thoroughness )
	{
		if( thoroughness > thoroughness_limit)
			return police_check_fine_value;
		return 0.0;
	}

	public float calcPrestige( float tear, float wear )
	{
		float inter = tear*(0.3 + 0.7*wear) * brand_new_prestige_value * prestige_calc_weight;
//		System.log("prestige: "+inter+" for "+name);
		inter *= PRESTIGE_VALUE_MUL;
		return inter;
	}

	public float currentPrestige()
	{
		if (prev_prestige<=0.0)
		{
//			if (attachedParts.size()==0)
//				return calcPrestige( getTear(), getWear() );

			Part	p;
			float	eng_prestige = 0.0;
			float	bdy_prestige = 0.0;
			float	rgr_prestige = 0.0;
			float	eng_parts_prestige_weight = 0.0;
			float	bdy_parts_prestige_weight = 0.0;
			float	rgr_parts_prestige_weight = 0.0;

			float	eng_multiplier = 1.0;
			float	bdy_multiplier = 1.0;
			float	rgr_multiplier = 1.0;

			if (this instanceof EnginePart)
			{
				eng_prestige = calcPrestige( getTear(), getWear() );
				eng_parts_prestige_weight = prestige_calc_weight;
				eng_multiplier *= PRESTIGE_DOWNGRADE_MUL;
			}
			else
			if (this instanceof BodyPart)
			{
				bdy_prestige = calcPrestige( getTear(), getWear() );
				bdy_parts_prestige_weight = prestige_calc_weight;
				bdy_multiplier *= PRESTIGE_DOWNGRADE_MUL;
			}
			else
			if (this instanceof RGearPart)
			{
				rgr_prestige = calcPrestige( getTear(), getWear() );
				rgr_parts_prestige_weight = prestige_calc_weight;
				rgr_multiplier *= PRESTIGE_DOWNGRADE_MUL;
			}

			for( int i=attachedParts.size()-1; i>=0; i-- )
			{
				p=attachedParts.elementAt(i);

				if (p instanceof EnginePart)
				{
					eng_prestige += p.calcPrestige( p.getTear(), p.getWear() );//p.currentPrestige();
					eng_parts_prestige_weight += p.prestige_calc_weight;
					eng_multiplier*=PRESTIGE_DOWNGRADE_MUL;
				}
				else
				if (p instanceof BodyPart)
				{
					bdy_prestige += p.calcPrestige( p.getTear(), p.getWear() );//p.currentPrestige();
					bdy_parts_prestige_weight += p.prestige_calc_weight;
					bdy_multiplier*=PRESTIGE_DOWNGRADE_MUL;
				}
				else
				if (p instanceof RGearPart)
				{
					rgr_prestige += p.calcPrestige( p.getTear(), p.getWear() );//p.currentPrestige();
					rgr_parts_prestige_weight += p.prestige_calc_weight;
					rgr_multiplier*=PRESTIGE_DOWNGRADE_MUL;
				}
			}

			eng_prestige *= eng_multiplier;
			bdy_prestige *= bdy_multiplier;
			rgr_prestige *= rgr_multiplier;

			float pres = 0.0;
			float e = -1.0;
			float b = -1.0;
			float r = -1.0;

			if (eng_parts_prestige_weight>0.0)
			{
				e = eng_prestige/eng_parts_prestige_weight*0.30;
				pres += e;
			}

			if (bdy_parts_prestige_weight>0.0)
			{
				b = bdy_prestige/bdy_parts_prestige_weight*0.50;
				pres += b;
			}

			if (rgr_parts_prestige_weight>0.0)
			{
				r = rgr_prestige/rgr_parts_prestige_weight*0.20;
				pres += r;
			}

/*
			System.log("");
			System.log("-----------------------------------");
			System.log("Prestige calculations for "+name+":");
			System.log("-----------------------------------");
			System.log(" engine parts prestige:       "+eng_prestige);
			System.log(" body parts prestige:         "+bdy_prestige);
			System.log(" running gear parts prestige: "+rgr_prestige);
			System.log("-----------------------------------");
			System.log(" engine parts weight:         "+eng_parts_prestige_weight);
			System.log(" body parts weight:           "+bdy_parts_prestige_weight);
			System.log(" running gear parts weight:   "+rgr_parts_prestige_weight);
			System.log("-----------------------------------");
			System.log(" downgrade of engine parts:       "+eng_multiplier);
			System.log(" downgrade of body parts:         "+bdy_multiplier);
			System.log(" downgrade of running gear parts: "+rgr_multiplier);
			System.log("-----------------------------------");
			System.log(" E:                           "+e);
			System.log(" B:                           "+b);
			System.log(" R:                           "+r);
			System.log("-----------------------------------");
			System.log("");
*/

			prev_prestige = pres;
		}

		return prev_prestige;
	}

	public float currentPrestigeNoAttach()
	{
		return calcPrestige( getTear(), getWear() );
	}

	public float calcPrice( float tear, float wear )
	{
		float inter = tear*(0.3 + 0.7*wear) * value;
		return inter;
	}

	//1.0-perfect  0.0-total scrap
	public float getConditionNoAttach() { return getWear()*getTear(); }

	public float getCondition()
	{
		float	cond = getWear()*getTear();

		for( int i=attachedParts.size()-1; i>=0; i-- )
			cond*=attachedParts.elementAt(i).getCondition();

		return cond;
	}

	public float currentPrice()
	{
		float	price = calcPrice( getTear(), getWear() );

		for( int i=attachedParts.size()-1; i>=0; i-- )
			price+=attachedParts.elementAt(i).currentPrice();

		return price;
	}

	public float currentPriceNoAttach()
	{
		return calcPrice( getTear(), getWear() );
	}

	public int isRepairable()
	{//ToDo: 3 kul. eredmenye lehetne: tul jo (mar nem lehet jobb); tul rossz (hasznald, aztan dobd ki); ill. javithato.
		int repairable = (getWear() > repair_min_wear)
					  && (getTear() > repair_min_tear)
					  && ( repair_max_wear > 0.0 || repair_max_tear > 0.0 );

		for( int i=attachedParts.size()-1; i>=0; i-- )
			repairable = repairable || attachedParts.elementAt(i).isRepairable();

		return repairable;
	}

	public float repairCost()
	{
		float maxTear = getTear();


		float maxWear = getWear();

		float cost;

		//nem halal rossz meg?
		if( maxTear > repair_min_tear || maxWear > repair_min_wear )
		{
			if( maxTear < repair_max_tear )
				maxTear = repair_max_tear;	//max tearig tudjuk feljavitani (persze csak ha nem jobb meg ennel!)
			if( maxWear < repair_max_wear )
				maxWear = repair_max_wear;

			cost = (calcPrice( maxTear, maxWear ) - calcPrice( getTear(), getWear() )) * price_repair;
		}

		for( int i=attachedParts.size()-1; i>=0; i-- )
			cost += attachedParts.elementAt(i).repairCost();

		return cost;
	}


	public float getRealCondition( float value, float bottom_offset, float top_offset, float tear_wear, String comment )
	{
		if (top_offset < bottom_offset)
		{
			float t;

			t=top_offset;
			top_offset=bottom_offset;
			bottom_offset=t;
		}

		float r = value*bottom_offset + value*(top_offset-bottom_offset)*tear_wear;

//		System.log(name+"->"+comment+" - getRealCondition("+value+", "+bottom_offset+", "+top_offset+", "+tear_wear+") = "+r);

		return r;
	}

	public float clampTo(float val, float min, float max)
	{
//		System.log("clamp "+val+" ["+min+" to "+max+"]");
		if (val < min)
		{
//			System.log("  result: "+min);
			return min;
		}
		else
		if (val > max)
		{
//			System.log("  result: "+max);
			return max;
		}

//		System.log("  result: "+val);
		return val;
	}

	public float rollTo(float val, float min, float max)
	{
//		System.log("roll "+val+" ["+min+" to "+max+"]");
		float res = val;

		if (res < min)
		{
			while (res < min)
			{
				res += max-min;
//				System.log("  result: "+res);
			}
		}
		else
		if (res > max)
		{
			while (res > max)
			{
				res -= max-min;
//				System.log("  result: "+res);
			}
		}

		return res;
	}

	public float HUF2USD(float huf)
	{
		return huf/211.0;
	}

	public float tHUF2USD(float huf)
	{
		return huf*1000.0/211.0;
	}

	public float mHUF2USD(float huf)
	{
		return huf*1000000.0/211.0;
	}

	public float Inch2Meter(float i)
	{
		return i*25.4/1000.0;
	}

	public float kmToMaxWear(float km)
	{
		return km*1000.0;
	}

	public void repair()
	{
		prev_prestige = 0.0;

		//wear-tear valtozok
		if( getWear() < repair_max_wear )
			setWear(repair_max_wear);
		if( getTear() < repair_max_tear )
			setTear(repair_max_tear);

		//mesh/texture
		//GameRef brandNew = new GameRef();
//		GameRef root = new GameRef( getParentID() );
		//GameRef root = getParent();
		
/*		//PATCH TO RETRIEVE ORIG. MESH
		Part newpart = brandNew.create( root, new GameRef(getInfo( GII_TYPE )),	"0,1000,0,0,0,0", "temp repair part" );
		if( newpart )
		{
			//setTexture( newpart.getTexture() );
			//newpart.setTexture(0);

			setMesh( newpart.getMesh() );
			newpart.setMesh(0);

			//a temp. alkatreszt eldobjuk
			newpart.destroy();
		}
		else
		{
			System.log( "part repair failed: scripted instance does not exist" );
			brandNew.destroy();
		}
*/
		//reset hidden variables
		command( "repair" );
		
		//RAXAT: if parts have been moved by player, we place them back after repair
		if(drag_matrix) updateDraggable();

		for( int i=attachedParts.size()-1; i>=0; i-- )
			attachedParts.elementAt(i).repair();
	}
	
	public void updateDraggable()
	{
		updateDraggable(drag_matrix.pos, drag_matrix.ori);
	}
	
	public void updateDraggable(Vector3 pos, Ypr ori)
	{
		setSlotPos(getSlotID(-1), pos, ori);
	}
	
	//RAXAT: simplified method for retrieving damage
	public int getDamage()
	{
		return getInfo(GII_DAMAGE);
		return 0;
	}

	//RAXAT: v2.3.1, get mass for some dedicated part
	public native float getMass();

	public int setSfxLoopParams(float vol, float pitch); //RAXAT: hidden

	public void updatevariables() { need_update = 0; }
	public void updateDamage() {}
	public void install() {}	//called after installing into sg.
	public void remove() {}		//called before removing from sg. (properly disassembling)
	public void falloff() {}	//called before removing from sg. (just falling off by hit or sg.)

	public native float getWear();					//kopottsag
	public native float setWear( float value );
	public native void setMaxWear( float value );
	public native float getTear();					//deformaltsag
	public native float setTear( float value );

	public native int getTexture();
	public native int setTexture(int ID);
	public native int getMesh();
	public native int setMesh(int ID);
	public native int getRenderType();
	public native int setRenderType(int ID);
	public native int[] install_OK( GameRef dest, int slot, GameRef part, int slot2, Vector3 pos );

	public int getFlap(){ return flap(0); }
	public void toggleFlap(){ flap(1); }
	//ne ezt hasznald, hanem a fenti kettot!
	public native int flap( int mode );

	public int getLogo() { return manufacturer; }
	public native int getCar();
	public native int getWheelID();
	public WheelRef getWheel( )
	{
		Part chass = getCarRef();

		if (chass instanceof Chassis)
			return	((Chassis)chass).getWheel(getWheelID());

		return null;
	}

	public native void disableSlot( int slotID, int status );
	public native String getSlotDamage( int slotIndex );				//slot index
	public native void setSlotDamage( int slotIndex, String data );	//slot index
	public native void setSlotPos( int slotID, Vector3 pos, Ypr ypr );	//slot ID

	public native int getSlots();					//max slot index+1
	public native int getSlotID( int slotIndex );	//slot index -> slot ID;  -1 -> parent slot ID
	public native Part partOnSlot( int slotID );	//slot ID
	public native int slotIDOnSlot( int slotID );	//slot ID
	public native Part getCarRef();

	public void reCreate(){}

	//for used car generator: creates parts for 'required' slots, sets default color (from a set)
	public void addStockParts()
	{
		addStockParts( 0, 1.0, 1.0 );	//for compatibility
	}

	public void addStockParts( int actcolor, float optical, float power )
	{
	}

	public void addStockParts( Descriptor desc )
	{
		setWear(desc.wear);
		setTear(desc.tear);
		int TextureID = desc.color;
		setTexture( TextureID );
		addStockParts( desc.color, desc.optical, desc.power );	//ignore wear,tear for compatibility
	}
	
	//RAXAT: build 931, allows to safely install parts to objects that are not vehicles
	public Part	addPartIgnoreCar( int id, String name )
	{
		int	carID = this.id();
		int rootID;
		
		GameRef car = new GameRef(carID);
		rootID = car.getParentID();

		if (!rootID) return null;

		GameRef xa = new GameRef();
		Part part = xa.create( new GameRef(rootID), new GameRef(id), "", name );
		if( part )
		{
			part.setWear(1.0);
			part.setTear(1.0);

			xa.command( "install 0 " + carID + " 0 " + this.id() + " 0" );
			return part;
		}
		
		return null;
	}

	//beszereli az adott alkatreszt, es annak esetleges opcionalis alkatreszeit (addStockPart() hivja)
	//csak beszerelt alkatreszen illik hivni!!
	public Part	addPart( int id, String name )
	{
		return addPart( id, name, 0, 1.0, 1.0 );
	}

	public Part	addPart( int id, String name, int textureid, float optical, float power )
	{
		return addPart( id, name, new Descriptor(textureid, optical, power, 1.0, 1.0) );
	}
	
	public Part	addPart( int id, String name, Descriptor desc )
	{
		int	carID = getCar();
		int rootID;
		if (!carID)
			carID = this.id();
		
		GameRef car = new GameRef(carID);
		rootID = car.getParentID();

		if (!rootID) return null;

		GameRef xa = new GameRef();
		
		//getScriptInstance forbidden!!
		Part part = xa.create( new GameRef(rootID), new GameRef(id), "", name );

		if( part )
		{
			part.setWear(desc.wear);
			part.setTear(desc.tear);
/*			GameRef xpart = new GameRef();
			int[] slotId = part.install_OK( car, 0, xpart, 0, null );

			if( slotId )
			{
				String error;
				if( !(error=part.installCheck( xpart.getScriptInstance(), slotId )) )
				{
*/					xa.command( "install 0 " + carID + " 0 " + this.id() + " 0" );

					int	succeeded = (xa.getParentID() != rootID);
					if( succeeded )
					{
						part.addStockParts(desc);
						if (desc.color)
							part.setTexture(desc.color);

					}
					else
					{	//nem sikerult beszerelodnie, ottmaradt ahol volt!
//						System.log( "Cannot install part \"" + part.name + "\" code:" + succeeded );
						xa.destroy();
					}
/*				}
				else
					System.log( "Cannot install part \"" + part.name + "\" cause:" + error );
			}
			else
				System.log( "Cannot install part \"" + part.name );
*/
			return part;
		}
/*		else
		{
			System.log( "Classless part created! (id:" + id + ")" );
		}
*/
		return null;
	}

	public void fillDynoData( DynoData dd, int parentSlot )	//different in other (engine) part classes
	{
//		System.log("FilleDynoData called for "+name);
		//fill _my_ variables in dd,
		//...and get those from my child parts
		int slots = getSlots();
		while( slots-- )
		{
			int	slotID = getSlotID( slots );
			if (slotID != parentSlot)
				getDynoData( dd, slotID );
		}
	}

	public int getDynoData( DynoData dd, int slotID )	//just support 'macro' for fill*; will NOT be different in other part classes
	{
		Part p;
		if( p = partOnSlot( slotID ) )
		{
			p.fillDynoData( dd, slotIDOnSlot(slotID) );
			return 1;
		}
		return 0;
	}


	//ideiglenes!!! random stuff
	static float seed;

	public static void randomize( float seed_ )
	{
		seed = seed_;
		while( seed >= 1.0 )
			seed-=1.0;
	}

	public static float random()
	{
		seed += 0.097865739;		
		seed *= 1.314159265;

		while( seed >= 1.0 )
			seed-=1.0;

		return seed;
	}

	public void check4warnings()
	{
//		System.log("!!!"+name+" warnings!!!:");
	}
}