package java.game.parts;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.cars.*;

public class WheelRef extends Native
{
//	public native void finalize( );	//ha _ref lesz

	public native Vector3 getPos( );
	public native Ypr getYpr( );
	public native float getDrive( );
	public native float getSteer( );
	public native float getRadius( );

	public native void setPos( Vector3 val );
	public native void setYpr( Ypr val );
	public native void setDrive( float val );
	public native void setSteer( float val );

	public native void setRadius( float val );
	public native void setWidth( float val );
	public native void setCPatch( float halfwidth, float angle, float offset );
	public native void setFriction( float val );
	public native void setFrictn_x( float val );
	public native void setSliction( float val );
	public native void setStiffness( float val );
	public native void setRollRes( float val );
	public native void setBearing( float val );
	public native void setMaxLoad( float val );
	public native void setLoadSmooth( float val );
	public native void setPacejka( int i, float val );

	public native void setForce( float val );
	public native void setDamping( float val );
	public native void setDamping( float bound, float rebound );
	public native void setRestLen( float val );
	public native void setMinLen( float val );
	public native void setMaxLen( float val );

	public native void setInstantCenter( float Hx, float Hy, float Hz, float Lx, float Ly, float Lz );

	public native void setBrake( float val );
	public native void setHBrake( float val );

	public native void setArm( float len, float px, float py, float pz, float nx, float ny, float nz );
	public native void setHub( float len, float p1x, float p1y, float p1z, float p2x, float p2y, float p2z, float pcx, float pcy, float pcz );

}
