package DistributedClassManagementSystem;


/**
* DistributedClassManagementSystem/CoursesHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from DistributedClassManagementSystem.idl
* Tuesday, June 12, 2018 2:17:35 PM EDT
*/

public final class CoursesHolder implements org.omg.CORBA.portable.Streamable
{
  public String value[] = null;

  public CoursesHolder ()
  {
  }

  public CoursesHolder (String[] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = DistributedClassManagementSystem.CoursesHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    DistributedClassManagementSystem.CoursesHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return DistributedClassManagementSystem.CoursesHelper.type ();
  }

}
