/*
 * FullSizePanel.java
 *
 * Created on 19 April 2004, 18:29
 */

/**
 *
 * @author  growe
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.io.*;
import com.sun.image.codec.jpeg.*;

public class FullSizePanel extends FullPanel
{
  static final int ARROW_LENGTH = 7;
  static final int ARROW_WIDTH  = 5;
  final int layerHeight = 30, leafSpace = 50, topOffset = 15;
  int edgeHeight = 50;  // Vertical extent of the edge line between layers.
  int totalLeaves, canvasWidth, canvasHeight;
  protected static final Hashtable plainMap = new Hashtable();
  static
  {
    plainMap.put(TextAttribute.FONT, new Font("Helvetica", Font.BOLD, 11));
  }
  
  protected void createPopupMenu()
  {
    // Vertex
    vertexPopup = new JPopupMenu();
    vertexTextMenu = new JMenuItem("Show text");
    editVertexIDMenu = new JMenuItem("Edit ID");
    linkMenu = new JMenuItem("Link");
    unlinkMenu = new JMenuItem("Unlink");
    ownerVertexMenu = new JMenuItem("Modify ownership");
    labelVertexMenu = new JMenuItem("Modify evaluation");
    vertexPopup.add(vertexTextMenu);
    vertexTextMenu.addActionListener(this);
    vertexPopup.add(editVertexIDMenu);
    editVertexIDMenu.addActionListener(this);
    vertexPopup.add(linkMenu);
    linkMenu.addActionListener(this);
    vertexPopup.add(unlinkMenu);
    unlinkMenu.addActionListener(this);
    vertexPopup.add(ownerVertexMenu);
    ownerVertexMenu.addActionListener(this);
    vertexPopup.add(labelVertexMenu);
    labelVertexMenu.addActionListener(this);
    // Missing premise
    missingPopup = new JPopupMenu();
    missingTextMenu = new JMenuItem("Show text");
    editMissingTextMenu = new JMenuItem("Edit text");
    editMissingIDMenu = new JMenuItem("Edit ID");
    linkMissingMenu = new JMenuItem("Link");
    unlinkMissingMenu = new JMenuItem("Unlink");
    ownerMissingMenu = new JMenuItem("Modify ownership");
    labelMissingMenu = new JMenuItem("Modify evaluation");
    missingPopup.add(missingTextMenu);
    missingTextMenu.addActionListener(this);
    missingPopup.add(editMissingTextMenu);
    editMissingTextMenu.addActionListener(this);
    missingPopup.add(editMissingIDMenu);
    editMissingIDMenu.addActionListener(this);
    missingPopup.add(linkMissingMenu);
    linkMissingMenu.addActionListener(this);
    missingPopup.add(unlinkMissingMenu);
    unlinkMissingMenu.addActionListener(this);
    missingPopup.add(ownerMissingMenu);
    ownerMissingMenu.addActionListener(this);
    missingPopup.add(labelMissingMenu);
    labelMissingMenu.addActionListener(this);
    // Edge
    edgePopup = new JPopupMenu();
    edgeTextMenu = new JMenuItem("Show text");
    edgeLinkMenu = new JMenuItem("Link");
    edgeUnlinkMenu = new JMenuItem("Unlink");
    edgeLabelMenu = new JMenuItem("Modify evaluation");
    edgePopup.add(edgeTextMenu);
    edgeTextMenu.addActionListener(this);
    edgePopup.add(edgeLinkMenu);
    edgeLinkMenu.addActionListener(this);
    edgePopup.add(edgeUnlinkMenu);
    edgeUnlinkMenu.addActionListener(this);
    edgePopup.add(edgeLabelMenu);
    edgeLabelMenu.addActionListener(this);
    // Scheme
    schemePopup = new JPopupMenu();
    editSchemeMenu = new JMenuItem("Edit scheme");
    schemePopup.add(editSchemeMenu);
    editSchemeMenu.addActionListener(this);
    // Background
    backgroundPopup = new JPopupMenu();
    aboutAraucariaMenu = new JMenuItem("About Araucaria");
    backgroundPopup.add(aboutAraucariaMenu);
    aboutAraucariaMenu.addActionListener(this);
  }
  
  protected int getTotalLeaves(TreeVertex root)
  {
    Vector refutationList = new Vector();
    if (!root.hidingChildren)
    {
      root.getRefutationList(refutationList);
    }
    refutationList.add(root);
    int totalLeaves = 0;
    for (int i = 0; i < refutationList.size(); i++)
    {
      totalLeaves += ((TreeVertex)refutationList.elementAt(i)).leafCount;
    }
    return totalLeaves;
  }
  
  public void paint(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D)g;
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    drawSubtrees(gg);
    drawEdges(gg);
    drawNodes(gg);
    if (displayText)
    {
      drawText(gg);
    }
    getDisplayFrame().getMainScrollPane().getViewport().setBackground(getDiagramBackground());
  }
  
  public void redrawTree(boolean doRepaint)
  {
    if (!initializeDrawing())
    {
      Graphics g = getGraphics();
      if (g != null)
      {
        super.paintComponent(g);
        Graphics2D gg = (Graphics2D)g;
        gg.setPaint(getDiagramBackground());
        gg.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
      }
      return;
    }
    repaint();
  }
  
  public boolean initializeDrawing()
  {
    if (argument.isMultiRoots())
    {
      argument.deleteDummyRoot();
      argument.setMultiRoots(false);
    }
    TreeVertex root = null;
    Vector roots = argument.getTree().getRoots();
    if (roots.size() == 0) return false;
    if (roots.size() > 1)
    {
      if (argument.getDummyRoot() == null)
      {
        argument.addDummyRoot(roots);
      }
      root = argument.getDummyRoot();
    }
    else if (roots.size() == 1)
    {
      root = (TreeVertex)roots.firstElement();
    }
    // if root == null, the entire tree has been erased,
    // so call emptyTree() to clean things up and
    // clear the display
    if (root == null)
    {
      argument.emptyTree(false);
    }
    else
    {
      totalLeaves = getTotalLeaves(root);
      canvasWidth = totalLeaves * leafSpace;
      canvasHeight = topOffset + (edgeHeight + layerHeight) * argument.getTree().getDepth(root, true);
      clearLeafCounts(argument.getTree());
      doLeafCounts(root);
      if (argument.isInvertedTree())
      {
        calcNodeCoords(root, canvasWidth, 0,
                layerHeight, 0, argument.isInvertedTree());
      }
      else
      {
        calcNodeCoords(root, canvasWidth, 0, 0, 0, argument.isInvertedTree());
      }
      Dimension panelDim = new Dimension(canvasWidth, canvasHeight);
      setPreferredSize(panelDim);
      getDisplayFrame().getMainScrollPane().setViewportView(this);
    }
    return true;
  }
  
  public void calcNodeCoords(TreeVertex root, int width, int leftWidth, int height,
          int layerNum, boolean invertedTree)
  {
    // Set root node's position and width
    Vector refutationList = new Vector();
    // If the vertex's children aren't being hidden, add in refutations
    if (!root.hidingChildren)
    {
      root.getRefutationList(refutationList);
    }
    refutationList.add(root);
    int numRefutations = refutationList.size();
    
    int xMin = leftWidth;
    int xMax = xMin;
    for (int i = 0; i < refutationList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)refutationList.elementAt(i);
      xMax = xMin + vertex.leafCount * leafSpace;
      vertex.setExtent(xMin, xMax);
      // Align all text boxes in a refut arrangement - allows for scheme labels
      int nodeHeight = (int)(height );
      if (invertedTree)
      {
        vertex.setDrawPoint((xMin + xMax)/2, canvasHeight -
                topOffset - nodeHeight - layerNum * edgeHeight);
      }
      else
      {
        vertex.setDrawPoint((xMin + xMax)/2, topOffset + nodeHeight + layerNum * edgeHeight);
      }
      Point corner = vertex.getDrawPoint();
      Dimension totalLayoutSize = vertex.totalLayoutSize;
      int schemeLayoutHeight = 0;
      if (vertex.schemeLayoutSize != null)
      {
        schemeLayoutHeight = (int)vertex.schemeLayoutSize.getHeight();
      }
      Shape node = new Ellipse2D.Float(corner.x, corner.y,
              NODE_DIAM, NODE_DIAM);
      vertex.setShape(node, this);
      xMin = xMax;
    }
    
    // Do recursion to work out lower layers
    if (!root.hidingChildren)
    {
      for (int i=0; i < refutationList.size(); i++)
      {
        TreeVertex vertex = (TreeVertex)refutationList.elementAt(i);
        xMin = vertex.getXMin(); xMax = vertex.getXMax();
        int numNonRefutationChildren = vertex.getNumberOfChildren() - vertex.getNumRefutations();
        if (numNonRefutationChildren == 0)
        {
          continue;
        }
        //     int childWidth = (xMax - xMin) / numNonRefutationChildren;
        Vector childEdges = vertex.getEdgeList();
        for (int j = 0; j < childEdges.size(); j++)
        {
          TreeEdge edge = (TreeEdge)childEdges.elementAt(j);
          TreeVertex child = edge.getDestVertex();
          if (!child.isRefutation())
          {
            int childWidth = getFullLeafCount(child) * leafSpace;
            int nextLayer = invertedTree ? layerNum + 1 : layerNum;
            calcNodeCoords(child, childWidth, xMin,
                    height + layerHeight,
                    layerNum + 1, invertedTree);
            xMin += childWidth;
          }
        }
      }
    }
    
    // All nodes now have positions calculated, so calculate edges
    Vector vertexList = argument.getTree().getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex child = (TreeVertex)vertexList.elementAt(i);
      if (!child.visible)
      {
        continue;
      }
      TreeVertex parent = child.getParent();
      if (parent != null)
      {
        Vector edges = parent.getEdgeList();
        for (int j = 0; j < edges.size(); j++)
        {
          TreeEdge parentEdge = (TreeEdge)edges.elementAt(j);
          if (parent.isVirtual())
          {
            calcVirtualEdge(parentEdge, NODE_DIAM);
          }
          else
          {
            calcStraightEdge(parentEdge, NODE_DIAM);
          }
        }
      }
    }
  }
  
  public void leftMouseReleased(MouseEvent e)
  {
    super.leftMouseReleased(e);
    if (!initializeDrawing())
    {
      araucaria.updateDisplays(true, false);
      return;
    }
    araucaria.updateDisplays(true, false);
  }
}
