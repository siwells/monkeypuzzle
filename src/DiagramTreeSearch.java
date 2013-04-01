/*
 * DiagramTreeSearch.java
 *
 * Created on 19 March 2004, 12:11
 */

/**
 * Draws the original Araucaria tree diagram in which the entire tree is
 * displayed without any scroll bars
 * @author  growe
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.text.*;

public class DiagramTreeSearch extends DiagramBase
{
 /* 
  public DiagramTreeSearch()
  {
    setLayout(new BorderLayout());
    setBackground(Color.white);
    mainDiagramPanel = new TreeSearchPanel();
    add(mainDiagramPanel, BorderLayout.CENTER);  
    freeVertexPanel = new FreeVertexPanel(mainDiagramPanel);
    mainDiagramPanel.setFreeVertexPanel(freeVertexPanel);
    freeVertexPanel.setBackground(new Color(255, 255, 200));
    freeVertexPanel.setPreferredSize(new Dimension(100, 2 * NODE_DIAM));
    add(freeVertexPanel, BorderLayout.SOUTH);
                                
  }
  
  public BufferedImage getJpegImage()
  {
    return mainDiagramPanel.getJpegImage();
  }
  
  public void setSearchFrame(SearchFrame s)
  { ((TreeSearchPanel)mainDiagramPanel).setSearchFrame(s); }
  
  public void setArgument(Argument a)
  {
    argument = a; 
    mainDiagramPanel.setArgument(a);
    freeVertexPanel.setArgument(a);
  }
  
  public void redrawTree(boolean doRepaint) 
  {
//    mainDiagramPanel.setArgument(araucaria.getArgument());
    mainDiagramPanel.redrawTree(doRepaint);
//    freeVertexPanel.setArgument(araucaria.getArgument());
    System.out.println("freevertex redraw");
    freeVertexPanel.repaint();
  }
  **/
  public BufferedImage getJpegImage()
  { return null; }
  public void redrawTree(boolean doRepaint) 
  {}
}
