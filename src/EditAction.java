import java.awt.geom.*;
import java.util.*;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class EditAction 
{
  public String amlString;
  public Vector freeVertexList, freeVertexListIDs;
  public Vector hiddenList;
  public String shortLabel;
  public Araucaria parent;
  public Argument argument;
  public String description, message;
  
  public EditAction(Araucaria a, String d)
  {
    parent = a;
    argument = parent.getArgument();
    amlString = parent.getUndoAML();
    shortLabel = new String(argument.getShortLabel());
    freeVertexList = argument.getFreeVerticesInList();
    freeVertexListIDs = argument.getFreeVerticesInListIDs();
    hiddenList = argument.getHiddenList();
    description = d;
    parent.undoMenuItem.setEnabled(true);
    parent.undoToolBar.setEnabled(true);
  }
  
  public void restore(boolean undo, String m)
  {
    restore(undo, m,  true);
  }
  
  public void restore(boolean undo, String m, boolean showMessage)
  {
    message = m;
    argument.emptyTree(true);
    try {
      ByteArrayInputStream byteStream = new ByteArrayInputStream(amlString.getBytes());
      InputSource saxInput = new InputSource(byteStream);
      parent.parseXMLwithSAX(saxInput);
    } catch (SAXException e) {
        System.out.println ("SAXException in parsing undo: " + e.getMessage());
        e.printStackTrace();
        System.out.println(amlString);
    } catch (Exception e)  {
      System.out.println ("Exception in GeneralAction undo: " + e.getMessage());
      e.printStackTrace();
    } catch (Error e) {
      System.out.println ("Error in GeneralAction undo: " + e.getMessage());
    }
    // Call updateSelectText to restore all the links between the text and the
    // vertexes in the tree
    parent.updateSelectText();
    // Now add any free vertexes
    for (int i = 0; i < freeVertexList.size(); i++) {
      TreeVertex freeVertex = (TreeVertex)freeVertexList.elementAt(i);
      freeVertex.setSelected(false);
      String freeVertexID = (String)freeVertexListIDs.elementAt(i);
      freeVertex.m_shortLabel = freeVertexID;
      argument.getFreeVertexList().add(freeVertex);
      freeVertex.setHasParent(false);
      freeVertex.deleteAllEdges(); 
      freeVertex.initRoles();
      if (!freeVertex.isMissing()) {
        parent.getSelectText().getSelectedList().add((GeneralPath)freeVertex.getAuxObject());
      }
    }
    
    // Restore hidden properties
    Vector vertexList = argument.getTree().getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)vertexList.elementAt(i);
      vertex.isHiddenTable = getHiddenTable(vertex.getShortLabelString());
    }
    parent.getSelectText().repaint();
    argument.setShortLabel(shortLabel);
    if (showMessage)
    {
      String infoString = undo ? "Undoing " : "Redoing ";
      infoString += message;
      parent.setMessageLabelText(infoString);
    }
    // Needed since the standardToWigmore call made from the XML parser is done
    // before the hidden properties of the added negations have been restored
    argument.standardToWigmore();
  }
  
  Hashtable getHiddenTable(String id)
  {
    for (int i = 0; i < hiddenList.size(); i++)
    {
      HiddenInfo info = (HiddenInfo)hiddenList.elementAt(i);
      if (id.equals(info.shortID))
      {
        return info.isHiddenTable;
      }
    }
    return null;
  }
}
