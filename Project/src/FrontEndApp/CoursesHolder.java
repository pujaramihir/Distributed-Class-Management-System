package FrontEndApp;


/**
* FrontEndApp/CoursesHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from FrontEnd.idl
* Thursday, July 19, 2018 7:47:26 o'clock PM EDT
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
    value = FrontEndApp.CoursesHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    FrontEndApp.CoursesHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return FrontEndApp.CoursesHelper.type ();
  }

}