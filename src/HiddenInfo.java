/*
 * HiddenInfo.java
 *
 * Created on 16 August 2004, 11:45
 */

/**
 * Utility class for retaining the hidden/display info during undo/redo
 * @author  growe
 */
import java.util.*;
public class HiddenInfo
{
  public Hashtable isHiddenTable;
  public String shortID;
  /** Creates a new instance of HiddenInfo */
  public HiddenInfo(Hashtable h, String s)
  {
    isHiddenTable = h;
    shortID = s;
  }
  
  public String toString()
  {
    String s = shortID + ": ";
    if (isHiddenTable != null)
      s += isHiddenTable.toString();
    return s;
  }
  
}
