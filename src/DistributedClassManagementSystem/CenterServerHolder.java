package DistributedClassManagementSystem;

/**
* DistributedClassManagementSystem/CenterServerHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from DistributedClassManagementSystem.idl
* Thursday, June 14, 2018 10:27:13 o'clock PM EDT
*/

public final class CenterServerHolder implements org.omg.CORBA.portable.Streamable
{
  public DistributedClassManagementSystem.CenterServer value = null;

  public CenterServerHolder ()
  {
  }

  public CenterServerHolder (DistributedClassManagementSystem.CenterServer initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = DistributedClassManagementSystem.CenterServerHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    DistributedClassManagementSystem.CenterServerHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return DistributedClassManagementSystem.CenterServerHelper.type ();
  }

}
