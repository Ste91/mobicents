package org.csapi.ui;

/**
 *	Generated from IDL interface "IpAppUI"
 *	@author JacORB IDL compiler V 2.1, 16-Feb-2004
 */

public final class IpAppUIHolder	implements org.omg.CORBA.portable.Streamable{
	 public IpAppUI value;
	public IpAppUIHolder()
	{
	}
	public IpAppUIHolder (final IpAppUI initial)
	{
		value = initial;
	}
	public org.omg.CORBA.TypeCode _type()
	{
		return IpAppUIHelper.type();
	}
	public void _read (final org.omg.CORBA.portable.InputStream in)
	{
		value = IpAppUIHelper.read (in);
	}
	public void _write (final org.omg.CORBA.portable.OutputStream _out)
	{
		IpAppUIHelper.write (_out,value);
	}
}
