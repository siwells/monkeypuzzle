/*
 * LinkException.java
 *
 * Created on 30 March 2004, 08:49
 */

/**
 *
 * @author  growe
 */
public class LinkException extends java.lang.Exception
{
  
  /**
   * Creates a new instance of <code>LinkException</code> without detail message.
   */
  public LinkException()
  {
  }
  
  
  /**
   * Constructs an instance of <code>LinkException</code> with the specified detail message.
   * @param msg the detail message.
   */
  public LinkException(String msg)
  {
    super(msg);
  }
}
