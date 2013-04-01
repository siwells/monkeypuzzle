/*
 * TreeSearchPanel.java
 *
 * Created on 31 March 2004, 09:18
 */

/**
 *
 * @author  growe
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.geom.*;
import javax.swing.*;

public class TreeSearchPanel extends FullPanel
{
  SearchFrame searchFrame;
  boolean selectedSomething;
  
  /** Creates a new instance of TreeSearchPanel */
  public TreeSearchPanel()
  {
  }
  
  public void setSearchFrame(SearchFrame s)
  { searchFrame = s; }
  
  /**
   * Draw vertices on top of the edge structure
   */
  public void drawNodes(Graphics2D gg)
  {
    Enumeration nodeList = argument.getBreadthFirstTraversal().elements();
    // Run through the traversal and draw each vertex
    // using an Ellipse2D
    // The draw point has been determined previously in
    // calcNodeCoords()
    while (nodeList.hasMoreElements()) { 
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      // Don't draw virtual nodes
      if (vertex.isVirtual())
      	continue;
      
      // If tree is incomplete and we're on the top layer, skip it
      if (argument.isMultiRoots() && vertex.getLayer() == 0)
      	continue;
      
      Point corner = vertex.getDrawPoint();
      Shape node = new Ellipse2D.Float(corner.x, corner.y,
				       NODE_DIAM, NODE_DIAM);
      vertex.setShape(node, this);
      
      // Fill the interior of the node with vertex's fillPaint
      gg.setPaint(vertex.fillPaint);
      gg.fill(node);
      
      // Draw the outline with vertex's outlinePaint; bold if selected
      gg.setPaint(vertex.outlinePaint);
      if (vertex.isSelected()) {
        gg.setStroke(selectStroke);
      } else {
        gg.setStroke(solidStroke);
      }
      gg.draw(node);
      
      // Draw the short label on top of the vertex
      gg.setPaint(vertex.textPaint);
      String shortLabelString = new String(vertex.getShortLabel());
      if (shortLabelString.length() == 1) {
        gg.setFont(labelFont1);
        gg.drawString(shortLabelString,
          corner.x + NODE_DIAM/4, corner.y + 3*NODE_DIAM/4);
      } else if (shortLabelString.length() == 2) {
        gg.setFont(labelFont2);
        gg.drawString(shortLabelString,
          corner.x + NODE_DIAM/5, corner.y + 3*NODE_DIAM/4);
      }
    }
  }

  /**
   * Draws the edge structure of the tree
   */
  public void drawEdges(Graphics2D gg)
  {
    gg.setPaint(Color.black);
    Enumeration nodeList = argument.getBreadthFirstTraversal().elements();
    // For each vertex...
    while (nodeList.hasMoreElements()) {
      // Get its edge list...
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      Enumeration edges = vertex.getEdgeList().elements();
      // For each edge in the list...
      while (edges.hasMoreElements()) {
        TreeEdge edge = (TreeEdge)edges.nextElement();
        // If we have several vertices on layer 0, only draw
        // edges for layers below that
        if (!(argument.isMultiRoots() && vertex.getLayer() == 0)) {
          // If the edge has been selected with the mouse,
          // use a thick line
          if (edge.isSelected()) {
            gg.setStroke(selectStroke);
          }
          gg.draw(edge.getShape(this));
          // If we used a thick line, reset the stroke to normal
          // line for next edge.
          if (edge.isSelected()) {
            gg.setStroke(solidStroke);
          }
          TreeVertex edgeSource = edge.getDestVertex();
        }
      }
    }
  }

  public java.awt.image.BufferedImage getJpegImage()
  {
    return null;
  }
  
  public boolean addFreeVertex()
  {
    argument.addFreeVertex(null, null, 0, this);
    displayFrame.controlFrame.updateDisplays(true); 
    return true;
  }
  
  public void selectVertexEdge(MouseEvent e)
  {
    double x = e.getX();
    double y = e.getY();
    selectedSomething = false;
    startVertex = getSelectedVertex(x, y, displayFrame.getMainDiagramPanel(), 
      displayFrame.getFreeVertexPanel());
    if (startVertex == null)  { // No vertex selected
      // See if an edge was selected. If not, see if a subtree
      // was selected.
      canCreateEdge = false;
      if (testEdgeShapes(e) != null)
      {
        selectedSomething = true;
      }
//      refreshDisplay();
      return;
    } else {
      selectedSomething = true;
    }
    if (startVertex.isVirtual())
      return;
    if (startVertex.getHasParent()) {
      if (startVertex.isRefutation()) {
          displayFrame.controlFrame.setMessageLabelText("Refutations cannot be used as premises " +
            "in the current version of Araucaria");
      } else {
          displayFrame.controlFrame.setMessageLabelText("Premise is already supporting a conclusion");
        }
      canCreateEdge = false;
//      refreshDisplay();
      return;
    } else {
      // Allow mouse to be dragged to select other vertex
      canCreateEdge = true;
      initDragCoords(x, y);
    }
  }
  
  /*
   * Creates a line of width EDGE_SELECT_WIDTH for each edge
   * and tests if mouse click was in that Shape's boundary.
   * Returns the edge if one was selected, null otherwise.
   */
  public TreeEdge testEdgeShapes(MouseEvent event) 
  {
    if (argument == null || argument.getTree() == null) return null;
    double x = event.getX(); 
    double y = event.getY();
    BasicStroke edgeWidth = new BasicStroke(EDGE_SELECT_WIDTH);
    if (argument.getBreadthFirstTraversal() == null) return null;
    Enumeration nodeList = argument.getBreadthFirstTraversal().elements();
    while (nodeList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      if (argument.isMultiRoots() && vertex.getLayer() == 0)
      	continue;
      Enumeration edges = vertex.getEdgeList().elements();
      while (edges.hasMoreElements()) {
        TreeEdge edge = (TreeEdge)edges.nextElement();
        Shape shape = edge.getShape(this);
        Shape wideEdge = edgeWidth.createStrokedShape(shape);
        TreeVertex child = edge.getDestVertex();
        if (wideEdge.contains(x, y)) {
          edge.setSelected(!edge.isSelected());
          return edge;
        }
      }
    }
    return null;
  }
  
  public void addNewEdge()
  {
    if (!canCreateEdge) {
//      refreshDisplay();
      return;
    }
    canCreateEdge = false;
    Vector vertexList = argument.getTree().getVertexList();
    boolean addNewEdge = false;
    if (!vertexList.contains(startVertex)) {
      addNewEdge = true;
      argument.getTree().addVertex(startVertex);
      argument.getFreeVertexList().remove(startVertex);
    }
    if (!vertexList.contains(endVertex)) {
      addNewEdge = true;
      argument.getTree().addVertex(endVertex);
      argument.getFreeVertexList().remove(endVertex);
    }
    if (addNewEdge) {
      argument.getTree().addEdge(endVertex, startVertex);
      // If addNewEdge is true here, either or both vertexes are new
      // to the tree, so new edge must be OK
      startVertex.setHasParent(true);
      
      calcSubtreeShapes();
      displayFrame.controlFrame.updateDisplays(true); 
//      displayFrame.controlFrame.getUndoStack().push(new EditAction(displayFrame.controlFrame, "adding a support"));
    }

    // If reach this far, tree already contains both vertices.
    // No need to check if edge already exists since have checked if
    // vertex has a parent. Just need to check if new edge creates a
    // cycle. If it does, we delete the edge we just added.
    if (argument.isMultiRoots()) {
      argument.deleteDummyRoot();
      argument.setMultiRoots(false);
    }
    argument.getTree().addEdge(endVertex, startVertex);
    startVertex.setHasParent(true);
    TreeVertex root;
    Vector roots = argument.getTree().getRoots();
    if (roots.size() == 0) {
      deleteCycle(startVertex, endVertex);
      return;
    }
    if (roots.size() > 1) {
      argument.addDummyRoot(roots);
      root = argument.getDummyRoot();
    } else {
      root = (TreeVertex)roots.firstElement();
    }
    try {
      argument.getTree().breadthFirstTopSort(root);
    } catch (GraphException e) {
      deleteCycle(startVertex, endVertex);
      return;
    }
    // If we survive the cycle test, the edge is OK.
    redrawTree(true);
  }

  public void deleteCycle(TreeVertex startVertex, TreeVertex endVertex)
  {
    displayFrame.controlFrame.setMessageLabelText("Adding this edge would create a cycle.");
    endVertex.deleteEdge(startVertex);
    startVertex.setHasParent(false);
    repaint();
  }

  public void redrawTree(boolean doRepaint)
  {
    displayText = false;
    if (argument == null) return;
    if (argument.isMultiRoots()) {
      argument.deleteDummyRoot();
      argument.setMultiRoots(false);
    }
    TreeVertex root = null;
    Vector roots = argument.getTree().getRoots();
    if (roots.size() > 1) {
      if (argument.getDummyRoot() == null) {
        argument.addDummyRoot(roots);
      }
      root = argument.getDummyRoot();
    } else if (roots.size() == 1) {
      root = (TreeVertex)roots.firstElement();
    }
    // if root == null, the entire tree has been erased,
    // so call emptyTree() to clean things up and
    // clear the display 
    if (root == null) {
      argument.emptyTree(false);
    } else {
      calcTreeShape(root);
    }
    if (doRepaint)
    {
      repaint();
    }
    /*
    if (displayFrame.getMainDiagramPanel() == this) 
    {
      searchFrame.updateDisplays(false);
    }
     */
  }

  public void leftMousePressed(MouseEvent e)
  {
    boolean clearSelected = false;
    if (argument != null)
    {
      clearSelected = argument.clearAllSelections();
    }
    repaint();
    selectVertexEdge(e);
    if (!selectedSomething && !clearSelected)
    {
      addFreeVertex();
      canCreateEdge = false;
      return;
    }
  }
  
  public void leftMousePressedShift(MouseEvent e)
  {
    selectVertexEdge(e);
  }
  
  public void leftMouseReleased(MouseEvent e)
  {
    double x = e.getX();
    double y = e.getY();

    // If no start vertex selected or start vertex is virtual, do nothing
    if (startVertex == null || startVertex.isVirtual()) {
      displayFrame.controlFrame.updateDisplays(true); 
      return;
    }
    endVertex = getSelectedVertex(x, y, displayFrame.getMainDiagramPanel(), 
      displayFrame.getFreeVertexPanel());
    // If no end vertex selected, still do nothing
    if (endVertex == null) {
      displayFrame.controlFrame.updateDisplays(true); 
      return;
    }
    // If mouse pressed and released within the same vertex, print vertex label
    if (endVertex == startVertex) {
      canCreateEdge = false;
      startVertex.setSelected(!startVertex.isSelected());
      displayFrame.controlFrame.updateDisplays(true); 
      return;
    } else {
      addNewEdge();
    }
  }
  
  public void paint(java.awt.Graphics g)
  {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D)g;
    // Draws the underlying tree
    if (argument != null && argument.getBreadthFirstTraversal() != null) {
      redrawTree(false);
      drawEdges(gg);
      drawNodes(gg);
    }
  }  
    
  protected void createPopupMenu()
  {
    // Vertex
    vertexPopup = new JPopupMenu();
    linkMenu = new JMenuItem("Link");
    unlinkMenu = new JMenuItem("Unlink");
    vertexPopup.add(linkMenu);
    linkMenu.addActionListener(this);
    vertexPopup.add(unlinkMenu);
    unlinkMenu.addActionListener(this);
    // Edge
    edgePopup = new JPopupMenu();
    edgeLinkMenu = new JMenuItem("Link");
    edgeUnlinkMenu = new JMenuItem("Unlink");
    edgePopup.add(edgeLinkMenu);
    edgeLinkMenu.addActionListener(this);
    edgePopup.add(edgeUnlinkMenu);
    edgeUnlinkMenu.addActionListener(this);
  }
  
  // Popup menu event handlers
  //
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == linkMenu || e.getSource() == edgeLinkMenu ||
      e.getSource() == linkMissingMenu) 
    { 
      try {
        argument.linkVertices();
      } catch (LinkException ex) {
        displayFrame.controlFrame.setMessageLabelText(ex.getMessage());
      }
    } else if (e.getSource() == unlinkMenu || e.getSource() == edgeUnlinkMenu ||
      e.getSource() == unlinkMissingMenu) {    
      try {
        argument.unlinkVertices();
      } catch (LinkException ex) {
        displayFrame.controlFrame.setMessageLabelText(ex.getMessage());
      }
    }
    displayFrame.controlFrame.updateDisplays(true);
  }  
}
