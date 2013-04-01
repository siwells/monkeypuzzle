package org.simonwells.monkeypuzzle;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;

/*
 * Created on Nov 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author growe
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ToulminFullSizePanel extends ToulminFullTextPanel
{
  /**
   * 
   */
  public ToulminFullSizePanel()
  {
    super();
    // TODO Auto-generated constructor stub
    textWidth = 25;
  }

  public void drawVertex(Graphics2D gg, TreeVertex vertex, Color color)
  {
    // Draw the box around the node's text
    Point corner = new Point(vertex.toulminX, vertex.toulminY);
    Dimension totalLayoutSize = vertex.totalLayoutSize;
    Paint textColor;
    if (vertex.isMissing()) {
      gg.setStroke(dashStroke);
      textColor = DiagramBase.missingColor;
    } else {
      gg.setStroke(solidStroke);
      textColor = textBackground;
    }
    gg.setPaint(textColor); 
    Shape textBox = vertex.getShape(this);
    gg.fill(textBox);
    drawText(gg, vertex, textColor, corner);
    gg.setPaint(color);
    if (vertex.isSelected())
    {
      if (vertex.isMissing())
      {
        gg.setStroke(selectDashStroke);
      } else {
        gg.setStroke(selectStroke);
      }
    }
    gg.draw(textBox);
    if (vertex.hidingChildren)
    {
      gg.setFont(labelFont2);
      gg.setPaint(Color.blue);
      Rectangle bounds = vertex.getShape(this).getBounds();
      gg.drawString("+", corner.x + bounds.width - 10, 
          corner.y + bounds.height - 5);
      gg.setPaint(Color.black);
      return;
    }
    // Draw vertexes and edges arising from current vertex
    drawEdges(vertex.toulminDataEdges, gg, dataColor);
    drawEdges(vertex.toulminWarrantEdges, gg, warrantColor);
    drawEdges(vertex.toulminQualifierEdges, gg, qualifierColor);
    drawEdges(vertex.toulminRebuttalEdges, gg, rebuttalColor);
    drawEdges(vertex.toulminBackingEdges, gg, backingColor);
  }
  
  public void paint(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D)g;
    gg.setPaint(getDiagramBackground());
    gg.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
    drawTree(gg);
    if (displayText) {
      drawText(gg);
    }
    getDisplayFrame().getMainScrollPane().getViewport().setBackground(getDiagramBackground());
  }
  
  public void calcTextLayout(TreeVertex vertex)
  {
    int height;
    int layoutHeight = 0;
    if (vertex.m_nodeLabel != null && argument.isShowSupportLabels()) {
      vertex.nodeLabelLayout = calcLayout(vertex.m_nodeLabel, boldMap);
      height = getLayoutHeight(vertex.nodeLabelLayout);
      layoutHeight += height;
      vertex.nodeLabelLayoutSize = new Dimension(textWidth, height);
    }
    // Get layout for the Toulmin role for this vertex
    String role = (String)vertex.roles.get("toulmin");
    // Layout for main text label
    String text = (String)vertex.getShortLabelString();
    // If the vertex is not to be shown, use ? as its text
    if (vertex.isHiddenTable.get("toulmin").equals("true"))
    {
      text = "?";
    } 
    else if (role.equals("qualifier"))
    {
      text = (String)vertex.getLabel();
      if (text.length() > 3)
        text = text.substring(0, 3);
    }
    vertex.textLayout = calcLayout(text, plainMap);
    height = getLayoutHeight(vertex.textLayout);
    layoutHeight += height;
    vertex.textLayoutSize = new Dimension(textWidth, height);
    vertex.ownersLayout = null;

    // Allow for separate arrows from each data node to be connected to the
    // text box
    int arrowHeight = 0;
    if (vertex.toulminDataEdges.size() > 0)
    {
      arrowHeight = dcArrowTop + (vertex.toulminDataEdges.size() - 1) * dcArrowHeadSpacing;
    }
    if (layoutHeight < arrowHeight)
    {
      layoutHeight = arrowHeight;
    }
    vertex.totalLayoutSize = new Dimension(textWidth, layoutHeight);
  }
  
  public void assignVertexShape(TreeVertex vertex, int x, int y, int width, int height)
  {
    String role = (String)vertex.roles.get("toulmin");
    Shape shape;
    if (role.equals("rebuttal"))
    {
      shape = new Ellipse2D.Double(x, y, width, height);
    } else if (role.equals("qualifier")) {
      GeneralPath path = new GeneralPath();
      path.moveTo(x + width/2, y);
      path.lineTo(x + width, y + height);
      path.lineTo(x, y + height);
      path.closePath();
      shape = path;
    } else {
      shape = new Rectangle2D.Double(x, y, width, height);
    }
    vertex.setShape(shape, this);
  }
  
  public void drawText(Graphics2D gg, TreeVertex vertex, Paint textColor, Point corner)
  {
    int y = corner.y;
    String role = (String)vertex.roles.get("toulmin");
    TextLayout textLayout = ((TextLine)vertex.textLayout.elementAt(0)).getLayout();
    Rectangle2D textBounds = textLayout.getBounds();
    Rectangle2D boundingBox = vertex.getShape(this).getBounds2D();
    int startX = vertex.toulminX + (int)(boundingBox.getWidth() - textBounds.getWidth())/2;
    int startY;
    // For rebuttals & data nodes, centre the text in the shape
    if (role.equals("qualifier"))
    {
      startY = vertex.toulminY + (int)(boundingBox.getHeight() - textBounds.getHeight())
        - textBorderMargin;
    } else {
      startY = vertex.toulminY + (int)(boundingBox.getHeight() - textBounds.getHeight())/2
        - textBorderMargin;
    }
    Shape currentClip = gg.getClip();
    if (currentClip == null)
    {
      gg.setClip(new Rectangle(0, 0, getWidth(), getHeight()));
      currentClip = gg.getClip();
    }
    Area backgroundArea = new Area(currentClip);
    backgroundArea.intersect(new Area(vertex.getShape(this)));
    gg.setClip(backgroundArea);
    drawLayout(gg, vertex.textLayout, startX, startY,
        textColor, vertex.textLayoutSize);
    gg.setClip(currentClip);
  }
  
}
