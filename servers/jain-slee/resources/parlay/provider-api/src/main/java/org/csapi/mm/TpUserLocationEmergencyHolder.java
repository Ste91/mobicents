package org.csapi.mm;

/**
 *	Generated from IDL definition of struct "TpUserLocationEmergency"
 *	@author JacORB IDL compiler 
 */

public final class TpUserLocationEmergencyHolder
	implements org.omg.CORBA.portable.Streamable
{
	public org.csapi.mm.TpUserLocationEmergency value;

	public TpUserLocationEmergencyHolder ()
	{
	}
	public TpUserLocationEmergencyHolder(final org.csapi.mm.TpUserLocationEmergency initial)
	{
		value = initial;
	}
	public org.omg.CORBA.TypeCode _type ()
	{
		return org.csapi.mm.TpUserLocationEmergencyHelper.type ();
	}
	public void _read(final org.omg.CORBA.portable.InputStream _in)
	{
		value = org.csapi.mm.TpUserLocationEmergencyHelper.read(_in);
	}
	public void _write(final org.omg.CORBA.portable.OutputStream _out)
	{
		org.csapi.mm.TpUserLocationEmergencyHelper.write(_out, value);
	}
}
