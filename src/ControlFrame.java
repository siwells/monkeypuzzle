/*
 * ControlFrame.java
 *
 * Created on 13 April 2004, 09:20
 */

/**
 *
 * @author  growe
 */
import java.util.*;
public interface ControlFrame
{
  
  void setMessageLabelText(String text);
  public void updateDisplays(boolean updateCurrent);
  UndoStack getUndoStack();
  public void doRedo();
  public void doUndo();
}
