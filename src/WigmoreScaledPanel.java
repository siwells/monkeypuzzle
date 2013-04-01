import java.awt.*;
import java.awt.geom.*;
import java.util.*;
/*
 * WigmoreScaledPanel.java
 *
 * Created on 12 January 2006, 13:15
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 *
 * @author growe
 */
public class WigmoreScaledPanel extends WigmoreFullSizePanel
{
  float scaleWidth;
  float scaleHeight;
  /** Creates a new instance of WigmoreScaledPanel */
  public WigmoreScaledPanel()
  {
  }
  
  // Scales a Wigmore diagram so that it fits into the display panel
  public void scaleWigmoreTree()
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
    float treeWidth = root.wigmoreWidth, treeHeight = root.wigmoreHeight;
    scaleWidth = panelWidth / treeWidth;
    scaleHeight = panelHeight / treeHeight;
    if (root.hidingChildren) return;
    scaleWigmoreVertex(root);
  }
  
  public void scaleSupports(Vector<TreeEdge> edges)
  {
    for (TreeEdge edge : edges)
    {
      TreeVertex dest = edge.getDestVertex();
      scaleWigmoreVertex(dest);
    }
  }
  
  public void scaleWigmoreVertex(TreeVertex root)
  {
    root.wigmoreX = (int)((root.wigmoreX - leftOffset) * scaleWidth + leftOffset);
    root.wigmoreY = (int)((root.wigmoreY - topOffset) * scaleHeight + topOffset);
    if (root.getShape(this) == null) return;
    assignVertexShape(root, root.wigmoreX, root.wigmoreY, root.getShape(this).getBounds().width,
        root.getShape(this).getBounds().height);
    
    if (root.hidingChildren) return;
    
    if (root.isVirtual()) 
    {
      scaleSupports(root.getEdgeList());
      return;
    }
    scaleSupports(root.wigmoreCorroborativeEdges);
    scaleSupports(root.wigmoreEvidenceEdges);
    scaleSupports(root.wigmoreExplanatoryEdges);
    
    // Scale edges
    if (root.wigmoreCorroborativeEdges.size() == 1)
    {
      TreeEdge firstEdge = root.wigmoreCorroborativeEdges.elementAt(0);
      TreeVertex dest = firstEdge.getDestVertex();
      if (dest.isVirtual())
      {
        Vector<TreeEdge> edges = dest.getEdgeList();
        for (TreeEdge edge : edges)
        {
          calcCorroborativeEdge(edge);
        }
      }
      calcCorroborativeEdge(firstEdge);
    }
    if (root.wigmoreEvidenceEdges.size() == 1)
    {
      TreeEdge firstEdge = root.wigmoreEvidenceEdges.elementAt(0);
      TreeVertex dest = firstEdge.getDestVertex();
      if (dest.isVirtual())
      {
        // Need to adjust the X coordinate of a virtual node since a simple
        // scale doesn't allow for the size of the parent symbol not changing
        TreeVertex virtParent = dest.getParent();
        dest.wigmoreX = virtParent.wigmoreX + virtParent.totalLayoutSize.width/2 + textBorderMargin;
        Vector<TreeEdge> edges = dest.getEdgeList();
        for (TreeEdge edge : edges)
        {
          calcEvidenceEdge(edge);
        }
      }
      calcEvidenceEdge(firstEdge);
    }
    if (root.wigmoreExplanatoryEdges.size() == 1)
    {
      TreeEdge firstEdge = root.wigmoreExplanatoryEdges.elementAt(0);
      TreeVertex dest = firstEdge.getDestVertex();
      if (dest.isVirtual())
      {
        Vector<TreeEdge> edges = dest.getEdgeList();
        for (TreeEdge edge : edges)
        {
          calcExplanatoryEdge(edge);
        }
      }
      calcExplanatoryEdge(firstEdge);
    }
  }
  
  public void redrawTree(boolean doRepaint)
  {
    initializeDrawing(false);
    scaleWigmoreTree();
    repaint();
  }
  
  public void paint(Graphics g)
  {
    super.paintComponent(g);
    initializeDrawing(false);
    scaleWigmoreTree();
    Graphics2D gg = (Graphics2D)g;
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    gg.setPaint(getDiagramBackground());
    gg.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
    drawTree(gg);
    if (displayText) {
      drawText(gg);
    }
    getDisplayFrame().getMainScrollPane().getViewport().setBackground(getDiagramBackground());
  }
}
