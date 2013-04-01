import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
/*
 * WigmoreFullSizePanel.java
 *
 * Created on 01 November 2005, 14:54
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 *
 * @author growe
 */
public class WigmoreFullSizePanel extends WigmoreFullTextPanel
{
  int TEXTWIDTH = 20;
  /** Creates a new instance of WigmoreFullSizePanel */
  public WigmoreFullSizePanel()
  {
    super();
    textWidth = TEXTWIDTH;
    leftOffset = 50;
    minVertBlockSeparation = 50;
    minBlockHorizSpace = 50;
    combHeight = 40;
  }
  
  public void drawVertex(Graphics2D gg, TreeVertex vertex, Color color)
  {
//    if (vertex.getParent() == null)
//    {
//      super.drawVertex(gg, vertex, color);
//      return;
//    }
    Point corner = new Point(vertex.wigmoreX, vertex.wigmoreY);
    Dimension totalLayoutSize = vertex.totalLayoutSize;
    Paint textColor;
    if (vertex.isVirtual()) 
    {
      drawEdges(vertex.getEdgeList(), gg, Color.black);
      return;
    }
    if (vertex.isMissing()) {
      gg.setStroke(dashStroke);
      textColor = DiagramBase.missingColor;
    } else {
      gg.setStroke(solidStroke);
      textColor = textBackground;
    }
    if (vertex.isSelected())
    {
      if (vertex.isMissing())
      {
        gg.setStroke(selectDashStroke);
      } else {
        gg.setStroke(selectStroke);
      }
    }
    Shape textBox = vertex.getShape(this);
    Rectangle2D bound = textBox.getBounds2D();
    // Use the width for both dimensions to make the image a square
    bound.setRect(bound.getX(), bound.getY(), bound.getWidth(), bound.getWidth());
    WigmoreRole role = new WigmoreRole((String)vertex.roles.get("wigmore"), 
            new Dimension((int)bound.getWidth(), (int)bound.getHeight()), (int)bound.getX(), (int)bound.getY(), this);
    vertex.setShape(bound, this);
    gg.setPaint(textColor); 
    gg.fill(vertex.getShape(this));
    Shape roleShape = role.iconImage;
    gg.setPaint(color);
    gg.draw(roleShape);
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
    //
    // Draw fact symbol, if any
    //
    String wigFact = (String)vertex.roles.get("wigmoreFact");
    Font labelFont = new Font("Sansserif", Font.PLAIN, 15);
    if (!wigFact.equals("none"))
    {
      String factString = "";
      if (wigFact.equals("judicial"))
      {
        factString = "\u221E";
      } else if (wigFact.equals("tribunal"))
      {
        factString = "\u00B6";
      }
      gg.setFont(labelFont);
      FontMetrics metrics = gg.getFontMetrics();
      Rectangle2D bounds = metrics.getStringBounds(factString, gg);
      gg.drawString(factString, corner.x + textBox.getBounds().width / 2 - (int)bounds.getWidth() / 2, 
              corner.y + (int)bound.getWidth() + metrics.getAscent());
    }
    
    String wigmoreRole = (String)vertex.roles.get("wigmore");
    WigmoreImages.NodePosition position = WigmoreImages.NodePosition.CENTRE;
    int offset = 0;
    if (wigmoreRole.toLowerCase().indexOf("explan") != -1)
    {
      position = WigmoreImages.NodePosition.LEFT;
      offset = -(int)bound.getWidth() / 4;
    } else if (wigmoreRole.toLowerCase().indexOf("corrob") != -1)
    {
      position = WigmoreImages.NodePosition.RIGHT;
      offset = (int)bound.getWidth() / 4;
    }
    Shape nodeLabelShape = WigmoreImages.getNodeLabelShape(vertex.m_nodeLabel, bound,  position);
    if (nodeLabelShape != null)
    {
      gg.draw(nodeLabelShape);
    } else if (vertex.m_nodeLabel != null && vertex.m_nodeLabel.toLowerCase().equals("doubt"))
    {
      gg.setFont(labelFont);
      String doubt = "?";
      FontMetrics metrics = gg.getFontMetrics();
      Rectangle2D bounds = metrics.getStringBounds(doubt, gg);
      gg.drawString(doubt, offset + corner.x + textBox.getBounds().width / 2 - (int)bounds.getWidth() / 2, 
              corner.y + textBox.getBounds().width / 2 + metrics.getAscent() / 2);
    }

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
    drawEdges(vertex.wigmoreEvidenceEdges, gg, Color.black);
    drawEdges(vertex.wigmoreExplanatoryEdges, gg, Color.black);
    drawEdges(vertex.wigmoreCorroborativeEdges, gg, Color.black);
  }

  public void calcTextLayout(TreeVertex vertex)
  {
//    if (vertex.getParent() == null)
//    {
//      textWidth = DiagramBase.TEXTWIDTH;
//      super.calcTextLayout(vertex);
//      textWidth = this.TEXTWIDTH;
//      return;
//    }
    int height;
    int layoutHeight = 0;
    // Get layout for the Wigmore role for this vertex
    String role = (String)vertex.roles.get("wigmore");
    // Layout for main text label
    String text = (String)vertex.getShortLabelString();
    vertex.textLayout = calcLayout(text, transparentMap, 20);
    height = getLayoutHeight(vertex.textLayout);
    layoutHeight += height;
    vertex.textLayoutSize = new Dimension(textWidth, height);
    vertex.ownersLayout = null;

    vertex.totalLayoutSize = new Dimension(textWidth, layoutHeight);
  }

  public void drawText(Graphics2D gg, TreeVertex vertex, Paint textColor, Point corner)
  {
//    if (vertex.getParent() == null)
//    {
//      super.drawText(gg, vertex, textColor, corner);
//      return;
//    }
    FontRenderContext fontRenderContext = 
      gg.getFontRenderContext();
    Font font = new Font("SansSerif", Font.PLAIN, 11);

    TextLayout textLayout = new TextLayout(vertex.getShortLabelString(), font, fontRenderContext);
    Rectangle2D textBounds = textLayout.getBounds();
    int startX = vertex.wigmoreX - (int)textBounds.getWidth() - textBorderMargin;
    int startY;

    startY = vertex.wigmoreY ;
    textLayout.draw(gg, startX,  startY);
  }

  /**
   * Calculates the edge connecting an evidence node to its parent
   */
  public void calcEvidenceEdge(TreeEdge edge)
  {
    TreeVertex source = edge.getSourceVertex();
    TreeVertex dest = edge.getDestVertex();
//    if (source.getParent() == null)
//    {
//      super.calcEvidenceEdge(edge);
//      return;
//    }
    String force = (String)dest.getSupportLabel();
    GeneralPath path = new GeneralPath();
    // Virtual source means we draw a dog-leg from evidence node up to 
    // virtual junction, no arrowhead
    if (source.isVirtual())
    {
      path.append(new Line2D.Double(source.wigmoreX,  
              source.wigmoreY, 
              dest.wigmoreX + dest.totalLayoutSize.width/2 + textBorderMargin, source.wigmoreY), false);
      path.append(new Line2D.Double(
              dest.wigmoreX + dest.totalLayoutSize.width/2 + textBorderMargin, source.wigmoreY, 
              dest.wigmoreX + dest.totalLayoutSize.width/2 + textBorderMargin, dest.wigmoreY), true);
      path.append(addForceSymbol(
        dest.wigmoreX + dest.totalLayoutSize.width/2 + textBorderMargin, source.wigmoreY, 
            dest.wigmoreX + dest.totalLayoutSize.width/2 + textBorderMargin, dest.wigmoreY, force, dest.isRefutation()), false);
      edge.setShape(path, this);
    }
    // Virtual destination means a straight edge from junction up to claim
    else if (dest.isVirtual())
    {
      path.append(new Line2D.Double(source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,  
              source.wigmoreY + source.totalLayoutSize.width + 2*textBorderMargin, 
              dest.wigmoreX, dest.wigmoreY), false);
      String evidenceForce = (String)source.roles.get("wigmoreEvidenceForce");
      path.append(addForceSymbol(
        source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,  
            source.wigmoreY + source.totalLayoutSize.width + 2*textBorderMargin, 
            dest.wigmoreX, dest.wigmoreY, evidenceForce, false), false);
      edge.setShape(path, this);
    }
    // Neither node virtual means a single evidence supporting a claim
    else 
    {
      path.append(new Line2D.Double(source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,  
              source.wigmoreY + source.totalLayoutSize.width + 2*textBorderMargin, 
              source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,
              dest.wigmoreY - combHeight), false);
      path.append(new Line2D.Double(source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,
              dest.wigmoreY - combHeight, 
              dest.wigmoreX + dest.totalLayoutSize.width/2 + textBorderMargin,
              dest.wigmoreY - combHeight), true);
      path.append(new Line2D.Double(dest.wigmoreX + dest.totalLayoutSize.width/2 + textBorderMargin,
              dest.wigmoreY - combHeight, 
              dest.wigmoreX + dest.totalLayoutSize.width/2 + textBorderMargin,
              dest.wigmoreY), true);
      path.append(addForceSymbol(
        source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,  
            source.wigmoreY + source.totalLayoutSize.width + 2*textBorderMargin, 
            source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,
            dest.wigmoreY - combHeight, force, dest.isRefutation()), false);
      edge.setShape(path, this);
    }
  }
  
}
