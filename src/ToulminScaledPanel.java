import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/*
 * Created on Nov 23, 2004
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
public class ToulminScaledPanel extends ToulminFullSizePanel
{
  float scaleWidth;
  float scaleHeight;
  /**
   * 
   */
  public ToulminScaledPanel()
  {
    super();
    // TODO Auto-generated constructor stub
  }
  
  // Scales a Toulmin diagram so that it fits into the display panel
  public void scaleToulminTree()
  {
    Vector roots = argument.getTree().getRoots();
    if (roots.size() == 0) return;
    TreeVertex root = (TreeVertex)roots.firstElement();
    if (argument.isMultiRoots()) 
    {
      root = argument.getTree().getDummyRoot();
    }
    float panelWidth = 0.9f * araucaria.getCurrentDiagram().mainScrollPane.getWidth(), 
      panelHeight = 0.9f * araucaria.getCurrentDiagram().mainScrollPane.getHeight();
    float treeWidth = root.toulminWidth, treeHeight = root.toulminHeight;
    scaleWidth = panelWidth / treeWidth;
    scaleHeight = panelHeight / treeHeight;
    scaleToulminVertex(root);
    /*
    // Draw vertexes and edges arising from current vertex
    drawEdges(vertex.toulminDataEdges, gg, dataColor);
    drawEdges(vertex.toulminWarrantEdges, gg, warrantColor);
    drawEdges(vertex.toulminQualifierEdges, gg, qualifierColor);
    drawEdges(vertex.toulminRebuttalEdges, gg, rebuttalColor);
    drawEdges(vertex.toulminBackingEdges, gg, backingColor);
    */
  }
  
  public void scaleToulminVertex(TreeVertex root)
  {
    root.toulminX = (int)((root.toulminX - leftOffset) * scaleWidth + leftOffset);
    root.toulminY = (int)((root.toulminY - topOffset) * scaleHeight + topOffset);
    if (root.getShape(this) == null) return;
    assignVertexShape(root, root.toulminX, root.toulminY, root.getShape(this).getBounds().width,
        root.getShape(this).getBounds().height);
    scaleDataBlockEdges(root);
    scaleSupportEdges(root.toulminWarrantEdges);
    scaleSupportEdges(root.toulminQualifierEdges);
    scaleSupportEdges(root.toulminRebuttalEdges);
    scaleBackingEdges(root);
  }
  
  public void scaleSupportEdges(Vector edges)
  {
    for (int i = 0; i < edges.size(); i++)
    {
      TreeEdge edge = (TreeEdge)edges.elementAt(i);
      TreeVertex vertex = (TreeVertex)edge.getDestVertex();
      scaleToulminVertex(vertex);
      calcSupportEdge(edge);
    }
  }

  public void scaleBackingEdges(TreeVertex root)
  {
    for (int i = 0; i < root.toulminBackingEdges.size(); i++)
    {
      TreeEdge backingEdge = (TreeEdge)root.toulminBackingEdges.elementAt(i);
      TreeVertex vertex = backingEdge.getDestVertex();
      scaleToulminVertex(vertex);
      // Backing edge...
      // Edge starts at midpoint of top edge of backing box, then dog-legs
      // over to bottom edge of warrant box. The location on warrant where
      // arrow should join is determined by how many backing nodes there are
      // If there are N nodes then the distance of the ith node (where i counted
      // from 0) along the bottom of the box is given by 
      //
      // (box width) / (N + 1) * (i + 1)
      int arrowHeadOffset = root.totalLayoutSize.width / (root.toulminBackingEdges.size() + 1) *
        (i + 1);
      // The line offset is extra distance extending from the arrowhead to the
      // first bend
      int arrowLineOffset = i * backingArrowLineSpacing;
      calcBackingEdge(backingEdge, arrowHeadOffset, arrowLineOffset);
    }
  }
  
  public void scaleDataBlockEdges(TreeVertex root)
  {
    int rootMidpoint = root.toulminY + root.totalLayoutSize.height / 2;
    
    for (int dataNum = 0; dataNum < root.toulminDataEdges.size(); dataNum++)
    {
      TreeEdge dataEdge = (TreeEdge)root.toulminDataEdges.elementAt(dataNum);
      TreeVertex dataBlock = dataEdge.getDestVertex();
      scaleToulminVertex(dataBlock);
      int dataY = dataBlock.toulminY + dcArrowOffset;
      int arrowHeadOffset = dcArrowOffset + dataNum * dcArrowHeadSpacing;
      int arrowLineOffset = dcArrowTop;
      if (dataY < rootMidpoint)
      {
        arrowLineOffset += dataNum * dcArrowLineSpacing;
      } else {
        arrowLineOffset += (root.toulminDataEdges.size() - 1 - dataNum) * dcArrowLineSpacing;
      }
      calcDataBlockEdge(dataEdge, arrowHeadOffset, arrowLineOffset);
    }
  }
  
  public void redrawTree(boolean doRepaint)
  {
    initializeDrawing(false);
    scaleToulminTree();
    repaint();
  }
  
  public void paint(Graphics g)
  {
    super.paintComponent(g);
    initializeDrawing(false);
    scaleToulminTree();
    Graphics2D gg = (Graphics2D)g;
    gg.setPaint(getDiagramBackground());
    gg.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
    //AffineTransform origTrans = gg.getTransform();
    //gg.setTransform(AffineTransform.getScaleInstance(scaleWidth, scaleHeight));
    drawTree(gg);
    if (displayText) {
      drawText(gg);
    }
    getDisplayFrame().getMainScrollPane().getViewport().setBackground(getDiagramBackground());
    //gg.setTransform(origTrans);
  }
  
}
