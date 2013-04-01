/*
 * FreeVertexPanel.java
 *
 * Created on 07 April 2004, 12:30
 */

/**
 *
 * @author  growe
 */
import java.awt.geom.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class FreeVertexPanel extends DiagramBase
{
  /** Creates a new instance of FreeVertexPanel */
  public FreeVertexPanel()
  {
    setBackground(getFreeVertexBackground());
    setPreferredSize(new Dimension(100,  2 * NODE_DIAM + 5));
  }
  
  public java.awt.image.BufferedImage getJpegImage()
  {
    return null;
  }
  
  protected void drawFreeVertices(Graphics2D gg)
  {
    int x_offset = 0;
    // Draw the free vertices at the bottom of the canvas
    // Free vertices drawn in 2 rows of 26, from left to right
    // Deleting or using a free vertex will cause the others to
    // fill in the gap
    int vertexNum = 0, freeVertexX, freeVertexY;
    if (argument == null) return;
    if (gg == null) return;
    Enumeration freeVertices = argument.getFreeVertexList().elements();
    while (freeVertices.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)freeVertices.nextElement();
      if (vertexNum < 26) {
        freeVertexX = NODE_DIAM / 4 + vertexNum * NODE_DIAM;
        freeVertexY = getHeight() - 2 * NODE_DIAM;
      } else {
        freeVertexX = NODE_DIAM / 4 + (vertexNum - 26) * NODE_DIAM;
        freeVertexY = getHeight() - NODE_DIAM;
      }
      ++vertexNum;
      vertex.setDrawPoint(freeVertexX, freeVertexY);
      char[] shortLabel = vertex.getShortLabel();
      if (shortLabel.length == 1) {
        gg.setFont(labelFont1);
      } else if (shortLabel.length == 2) {
        gg.setFont(labelFont2);
      }
      Point corner = vertex.getDrawPoint();
      Shape node = new Ellipse2D.Float(corner.x + x_offset, corner.y,
				       NODE_DIAM, NODE_DIAM);
      vertex.setShape(node, this);
      if (vertex.isMissing()) {
        gg.setPaint(missingColor);
        gg.fill(node);
        gg.setPaint(Color.darkGray);
      } else {
        gg.setPaint(Color.yellow);
        gg.fill(node);
        gg.setPaint(Color.black);
      }
      if (vertex.isSelected()) {
      	gg.setStroke(selectStroke);
      }
      gg.draw(node);
      if (vertex.isSelected()) {
      	gg.setStroke(solidStroke);
      }
      String shortLabelString = new String(vertex.getShortLabel());
      if (shortLabelString.length() == 1) {
        gg.drawString(shortLabelString,
  		    corner.x + x_offset + NODE_DIAM/4, corner.y + 3*NODE_DIAM/4);
      } else if (shortLabelString.length() == 2) {
        gg.drawString(shortLabelString,
  		    corner.x + x_offset + NODE_DIAM/5, corner.y + 3*NODE_DIAM/4);
      }
    }
  }
  
  public void paint(Graphics g)
  {
    setBackground(getFreeVertexBackground());
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D)g;
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    drawFreeVertices(gg);
  }

  public void redrawTree(boolean doRepaint)
  {
    repaint();
  }
  
  public void leftMouseReleased(MouseEvent e)
  {
    super.leftMouseReleased(e);
    displayFrame.controlFrame.updateDisplays(true);
  }
}
