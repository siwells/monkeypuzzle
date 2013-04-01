/*
 * DiagramBase.java
 *
 * Created on 19 March 2004, 16:56
 */

/**
 *
 * @author  growe
 */
import javax.swing.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.text.*;
import java.awt.font.*;

public abstract class DiagramBase extends javax.swing.JPanel 
  implements ActionListener
{
  Araucaria araucaria;
  Argument argument;
   
  // Standard colours
  public static Color DIAGRAM_BACKGROUND = Color.white;
  public static Color FREE_VERTEX_BACKGROUND = new Color(1.0f, 1.0f, 0.8f);
  static Color supportLabelColor = new Color(0.8f, 0.8f, 1.0f);
  static Color nodeLabelColor = new Color(1.0f, 0.8f, 1.0f);
  static Color roleColor = new Color(1.0f, 1.0f, 0.8f);
  static Color ownersColor = new Color(0.8f, 1.0f, 0.8f);
  static Color refutationColor = new Color(1.0f, 0.8f, 0.8f);
  static Color missingColor = new Color(0.9f, 0.9f, 0.9f);
  static Color missingBorderColor = Color.darkGray;
  static Color missingRefutGradientColor = Color.gray;
  static Color nodeFillColor = Color.orange;

  // Node and edge properties
  static final int NODE_DIAM = 20;
  static final float EDGE_SELECT_WIDTH = 10.0f;
  static final float EDGE_OUTLINE_WIDTH = 3.0f;
  static final float SUBTREE_OUTLINE_WIDTH = 6.0f;
  float SUBTREE_LINE_WIDTH = 50.0f;
  BasicStroke dashStroke, solidStroke, boldStroke, selectStroke, selectDashStroke;
  Font labelFont1 = new Font("sansserif", Font.BOLD, 3*NODE_DIAM/4); // Single-letter nodes
  Font labelFont2 = new Font("sansserif", Font.BOLD, 2*NODE_DIAM/4); // Double-letter nodes
  Font labelFont3 = new Font("sansserif", Font.BOLD, 42*NODE_DIAM/100); // Double-letter nodes
  Point2D.Double mainStartPoint, mainOldEndPoint, freeStartPoint, freeOldEndPoint;
  String pendingStartRole = "none";       // Role for new node being added to a non-standard diagram

  // Miscellaneous display properties
  boolean showOwners = true;
  boolean showSupportLabels = true;
  static int MAX_MESSAGELABEL_SIZE = 100;
  boolean displayText;
  boolean invertedTree;
  TreeVertex startVertex, endVertex;
  boolean canCreateEdge;
  DisplayFrame displayFrame;
  int mouseX, mouseY;
  TreeEdge mouseEdge; 
  TreeVertex mouseVertex;
  Subtree mouseSubtree;
  Dimension diagramSize;
  public static int TEXTWIDTH = 150;
  int textWidth = TEXTWIDTH;  // Width allowed for the text label for a given vertex
  int vertTextSpace = 10; // Minimum vertical distance between adjacent text boxes
  int horizTextSpace = 10; // Minimum horizontal distance between adjacent text boxes
  int textBorderMargin = 3; // Distance between text and bounding box
  int topOffset = 15;    // Distance from top of client area to root node
  int leftOffset = 15; // Offsets from top & left of window
  int canvasWidth, canvasHeight;

  // Popup menu items
  //
  JPopupMenu vertexPopup, missingPopup, edgePopup, schemePopup, backgroundPopup;
  // Vertex menu items
  JMenuItem vertexTextMenu, editVertexIDMenu, ownerVertexMenu, labelVertexMenu,
    linkMenu, unlinkMenu;
  JMenuItem setPremisesVisMenu;
  // Missing premise menu items
  JMenuItem missingTextMenu, editMissingTextMenu, editMissingIDMenu, ownerMissingMenu, labelMissingMenu,
    linkMissingMenu, unlinkMissingMenu;
  // Edge menu items
  JMenuItem edgeTextMenu, edgeLinkMenu, edgeUnlinkMenu, edgeLabelMenu;
  // Scheme menu items
  JMenuItem editSchemeMenu;
  // Background menu items
  JMenuItem aboutAraucariaMenu;
  static int MAX_POSTIT_SIZE = 60;

  protected static final Hashtable plainMap = new Hashtable();
  static {
    plainMap.put(TextAttribute.FONT, new Font("Helvetica", Font.PLAIN, 11));
  }

  protected static final Hashtable boldMap = new Hashtable();
  static {
    boldMap.put(TextAttribute.FONT, new Font("Helvetica", Font.BOLD, 11));
  }

  protected static final Hashtable italicMap = new Hashtable();
  static {
    italicMap.put(TextAttribute.FONT, new Font("Helvetica", Font.ITALIC, 11));
  }

  protected static final Hashtable transparentMap = new Hashtable();
  static {
    transparentMap.put(TextAttribute.FONT, new Font("Helvetica", Font.PLAIN, 11));
    transparentMap.put(TextAttribute.BACKGROUND,  new Color(0,0,0,1));
  }

  /** Creates new form DiagramBase */
  public DiagramBase()
  {
    setBackground(DIAGRAM_BACKGROUND);
    float[] dashPattern = {10.0f, 10.0f};
    dashStroke = new BasicStroke
      (1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
       10.0f, dashPattern, 0.0f);
    solidStroke = new BasicStroke();
    boldStroke = new BasicStroke(SUBTREE_OUTLINE_WIDTH);
    selectStroke = new BasicStroke(EDGE_OUTLINE_WIDTH);
    selectDashStroke = new BasicStroke
      (EDGE_OUTLINE_WIDTH, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
       10.0f, dashPattern, 0.0f);
    initComponents();
    handleMouse();
    createPopupMenu();
  }
  
  protected void createPopupMenu()
  {
  }
  
  public Color getDiagramBackground()
  {
    return DIAGRAM_BACKGROUND;
  }
  
  public Color getFreeVertexBackground()
  {
    return FREE_VERTEX_BACKGROUND;
  }
  
  public static String prepareMessageLabel(String text, int maxSize)
  {
    if (text.length() <= maxSize)
      return text;
    String label = text.substring(0, maxSize / 2);
    label += " ... " +
      text.substring(text.length() - maxSize / 2, text.length());
    return label;
  }

  public static String prepareMessageLabel(String text1, String text2, int maxSize)
  {
    if (text1.length() > maxSize / 2)
      text1 = prepareMessageLabel(text1, maxSize / 2);
    if (text2.length() > maxSize / 2)
      text2 = prepareMessageLabel(text2, maxSize / 2);
    String label = text1 + " ==> " + text2;
    return label;
  }
  
  public float getSubtreeLineWidth()
  { return 50.0f; }
  
  /**
   * Draws a text layout starting at (startX, startY).
   * Returns final value of y used
   */
  protected int drawLayout(Graphics2D gg, Vector textLayout, 
    int startX, int startY, Paint backColor, Dimension backSize)
  {
    if (textLayout == null) {
      return startY;
    }
    // Draw background
    gg.setPaint(backColor);
    gg.fill(new Rectangle2D.Double(startX, startY, backSize.getWidth(), backSize.getHeight()));
    gg.setPaint(Color.black);
    int y = 0;
    int lineSpacing = 0;
    for (int line = 0; line < textLayout.size(); line++) {
      TextLine textLine = (TextLine)textLayout.get(line);
      TextLayout layout = textLine.getLayout();
      if (textLine.textColor != null) {
        gg.setPaint(textLine.textColor);
      } else {
        gg.setPaint(Color.black);
      }
      lineSpacing = (int)(layout.getAscent() + layout.getDescent() + layout.getLeading());
      y = startY + (line+1) * lineSpacing;
     
      layout.draw(gg, startX, y - layout.getDescent());
    }
    return y;
  }

  protected int getLayoutHeight(Vector textLayout)
  {
    TextLayout layout = ((TextLine)textLayout.get(0)).getLayout();
    int lineSpacing = (int)(layout.getAscent() + layout.getDescent() + layout.getLeading());
    return lineSpacing * textLayout.size();
  }
  
  // Calculates the text layout for each vertex's text
  protected void calcTextLayouts() 
  {
    Vector roots = argument.getTree().getRoots();
    for (int i = 0; i < roots.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)roots.elementAt(i);
      calcTextLayouts(vertex);
    }
  }
  
  /**
   * Must be replaced by a real method in all full text classes.
   */
  protected void calcTextLayouts(TreeVertex root) {}
  
  public void assignVertexShape(TreeVertex vertex, int x, int y, int width, int height)
  {
    Shape boxShape = new Rectangle2D.Double(x, y, width, height);
    vertex.setShape(boxShape, this);
  }
  
  public Vector calcLayout(String text, Hashtable map)
  {
    return calcLayout(text, map, textWidth);
  }

  public Vector calcLayout(String text, Hashtable map, int lineLength)
  {
    Vector textLayout = new Vector();
    // Look for line breaks in original text and preserve these
    StringTokenizer lineTokens = new StringTokenizer(text, "\n", true);
    int lineStart = 0;
    int returns = 0;
    while (lineTokens.hasMoreTokens())
    {
      String nextLine = lineTokens.nextToken();
      if (nextLine.equals("\n"))
      {
        returns++;
        continue;
      }
      AttributedString helloAttrib;
      AttributedCharacterIterator helloIter;
      LineBreakMeasurer lineBreak;
      FontRenderContext frc = new FontRenderContext(null, false, false);
      helloAttrib = new AttributedString(nextLine, map);
      helloIter = helloAttrib.getIterator();
      lineBreak = new LineBreakMeasurer(helloIter, frc);
      int first = helloIter.getBeginIndex();
      int last = helloIter.getEndIndex();
      lineBreak.setPosition(first);
      TextLayout layout;
      while (lineBreak.getPosition() < last) {
        layout = lineBreak.nextLayout(lineLength);
        textLayout.add(new TextLine(layout, lineStart + returns + first, 
                lineStart + returns + lineBreak.getPosition()));
//        textLayout.add(new TextLine(layout, first, lineBreak.getPosition()));
        first = lineBreak.getPosition();
      }
      lineStart += last;
    }
    return textLayout;
  }
  
  /**
   * Adds an arrowhead at the (x1, y1) end of a line segment.
   * The args give the endpoints of the line.
   * The arrowhead is drawn so that the point of the arrow lies
   * a distance 'offset' back from (x1,y1) so that it
   * doesn't overlap with the node's circle in the tree.
   * Returns a GeneralPath representing the arrowhead.
   */
  
  public static Shape addArrowHead(int x1, int y1, int x2, int y2, 
    int offset, int arrowWidth, int arrowLength)
  {
    GeneralPath arrowPath = new GeneralPath();
    // If start and end points are the same, return empty path
    // Can happen in big diagrams where nodes get close together
    if (x1 == x2 && y1 == y2) return arrowPath;
    // Work out location of tip of arrow - this is offset from the
    // endpoint (x1, y1) by the radius of the vertex node
		double xa, ya, xb, yb, xd, yd;
    double r = Math.sqrt((y2 - y1)*(y2 - y1) + (x2 - x1)*(x2 - x1));
    xd = offset/r * (x2 - x1) + x1;
    yd = offset/r * (y2 - y1) + y1;

		if (y1 == y2)
		{
			xa = xb = xd;
      ya = yd + arrowWidth; yb = yd - arrowWidth;
		}
		else
		{
			double m = -((double)x2 - x1)/(y2 - y1);
      double d1m2 = arrowWidth/Math.sqrt(1.0 + m*m);
			xa = d1m2 + xd; xb = -d1m2 + xd;
			ya = m*d1m2 + yd; yb = -m*d1m2 + yd;
		}
    // Redefine the endpoint of the line to include the offset
    x1 = (int)(xa + xb)/2; y1 = (int)(ya + yb)/2;

    // Work out corners of arrow
    r = Math.sqrt((y2 - y1)*(y2 - y1) + (x2 - x1)*(x2 - x1));
    xd = arrowLength/r * (x2 - x1) + x1;
    yd = arrowLength/r * (y2 - y1) + y1;

		if (y1 == y2)
		{
			xa = xb = xd;
      ya = yd + arrowWidth; yb = yd - arrowWidth;
		}
		else
		{
			double m = -((double)x2 - x1)/(y2 - y1);
      double d1m2 = arrowWidth/Math.sqrt(1.0 + m*m);
			xa = d1m2 + xd; xb = -d1m2 + xd;
			ya = m*d1m2 + yd; yb = -m*d1m2 + yd;
		}
    arrowPath.append(new Line2D.Double(xa, ya, (double)x1, (double)y1), false);
    arrowPath.append(new Line2D.Double(xb, yb, (double)x1, (double)y1), false);
    return arrowPath;
  }
  
  public void paint(Graphics g)
  { 
    super.paintComponent(g); 
  }
/*
  public void doLeafCounts(TreeVertex vertex)
  {
    
  }
*/  
  public void clearLeafCounts(Tree tree)
  {
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++) 
    {
      TreeVertex vertex = (TreeVertex)vertexList.elementAt(i);
      vertex.leafCount = 0;
    }
  }
  
  /**
   * Method must be overridden to update tree structure in each diagram
   */
  public abstract void redrawTree(boolean doRepaint);
  
  /**
   * Method must be overridden to provide a JPEG image of the diagram
   */
  public abstract BufferedImage getJpegImage();

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  private void initComponents()//GEN-BEGIN:initComponents
  {

    setLayout(new java.awt.BorderLayout());

  }//GEN-END:initComponents

  public boolean addFreeVertex()
  {
    // Adds a new free vertex if text selected
    SelectText selectText = araucaria.getSelectText();
    if (selectText.isTextSelected()) {
      // If highlighted (yellow) selection overlaps greyed out
      // text, selection isn't allowed
      if (!selectText.isSelectionDisjoint()) {
        selectText.clearSelection();
        araucaria.setMessageLabelText("Selection must contain only ungreyed text.");
        return true;
      }
      String selectedText = selectText.getSelectedText();
      int selectedOffset = selectText.getOffset();
      GeneralPath shape = selectText.useCurrentText();
      selectText.clearSelection();
//      System.out.println("DiagramBase: " + this.getClass().getName());
      argument.addFreeVertex(selectedText, shape, selectedOffset,  this);
      displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "adding a new premise"));
      return true;
    }
    return false;
  }
  
  public TreeVertex getSelectedVertex(double x, double y, DiagramBase mainPanel,
    DiagramBase freeVertexPanel)
  {
    // Test the free vertex list
    if (argument == null) return null;
    int mainHeight = mainPanel.getHeight();
    JScrollPane scrollPane = displayFrame.getMainScrollPane();
    JScrollBar horizBar = scrollPane.getHorizontalScrollBar();
    int horizOffset = horizBar.isVisible() ? horizBar.getValue() : 0;
    
    JScrollBar vertBar = scrollPane.getVerticalScrollBar();
    int vertOffset = vertBar.isVisible() ? 
      vertBar.getMaximum() - vertBar.getValue() - scrollPane.getViewport().getHeight() : 0;
    int horizBarHeight = horizBar.isVisible() ? horizBar.getHeight() : 0;
    
    Enumeration freeVertices = argument.getFreeVertexList().elements();
    while (freeVertices.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)freeVertices.nextElement();
      Shape node = vertex.getShape(freeVertexPanel);
      if (this == freeVertexPanel && y > 0)
      {
        if (node.contains(x, y)) {
        	return vertex;
        }
      } else if (this == mainPanel && y > mainHeight + horizBarHeight - vertOffset)
      {
        if (node.contains(x - horizOffset, y - mainHeight - horizBarHeight + vertOffset))
        {
          return vertex;
        }
      }
    }

    // Test the tree
    if (argument.getBreadthFirstTraversal() == null) return null;
    Enumeration nodeList = argument.getBreadthFirstTraversal().elements();
    while (nodeList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      Shape node;
      // node can be null for a virtual vertex or a dummy root
      if (this == mainPanel && y < mainHeight) 
      {
        node = vertex.getShape(this);
        if (node != null && node.contains(x, y)) 
        {
        	return vertex;
        }
      } else if (this == freeVertexPanel && y < 0)
      {
        node = vertex.getShape(mainPanel);
        if (node != null && node.contains(x + horizOffset, y + mainHeight + horizBarHeight - vertOffset))
        {
          return vertex;
        }
      }
    }
    return null;
  }
  
  /**
   * Dummy method to be overridden in diagram classes that allow nodes to be
   * dragged onto edges (e.g. Toulmin)
   */
  public TreeVertex getSelectedVertexFromEdge(double x, double y, DiagramBase mainPanel,
    DiagramBase freeVertexPanel)
  {
    if (argument == null) return null;
    int mainHeight = mainPanel.getHeight();
    JScrollPane scrollPane = displayFrame.getMainScrollPane();
    JScrollBar horizBar = scrollPane.getHorizontalScrollBar();
    int horizOffset = horizBar.isVisible() ? horizBar.getValue() : 0;
    
    JScrollBar vertBar = scrollPane.getVerticalScrollBar();
    int vertOffset = vertBar.isVisible() ? 
      vertBar.getMaximum() - vertBar.getValue() - scrollPane.getViewport().getHeight() : 0;
    int horizBarHeight = horizBar.isVisible() ? horizBar.getHeight() : 0;

    if (this == mainPanel && y < mainHeight) 
    {
      return mainPanel.getVertexFromEdgeContaining(x, y);
    } 
    else if (this == freeVertexPanel && y < 0)
    {
      return mainPanel.getVertexFromEdgeContaining(x + horizOffset, y + mainHeight + horizBarHeight - vertOffset);
    }
    return null;
  }
  
  public TreeVertex getVertexFromEdgeContaining(double x, double y)
  {
    return null;
  }

  // Tests free vertex list and tree to find vertex containing point
  public void selectVertexEdge(MouseEvent e)
  {
    double x = e.getX();
    double y = e.getY();
    startVertex = getSelectedVertex(x, y, displayFrame.getMainDiagramPanel(),
      displayFrame.getFreeVertexPanel()); 
    if (startVertex != null && e.getClickCount() == 2)
    {
      if (testVertex(e, startVertex, x, y, false) && startVertex.getEdgeList().size() > 0)
      {
        showHidePremises();
      }
      return;
    }
    if (startVertex == null)  { // No vertex selected
      // See if an edge was selected. If not, see if a subtree
      // was selected.
      canCreateEdge = false;
      if (testEdgeShapes(e) == null) {
        testSubtrees(e);
      }
      return;
    }
    if (startVertex.isVirtual())
      return;
    if (startVertex.getHasParent()) {
      if (startVertex.isRefutation()) {
          araucaria.setMessageLabelText("Refutations cannot be used as premises " +
            "in the current version of Araucaria");
      } else {
        araucaria.setMessageLabelText("Premise is already supporting a conclusion");
      }
      canCreateEdge = false;
      return;
    } else {
      // Allow mouse to be dragged to select other vertex
      canCreateEdge = true;
      initDragCoords(x, y);
    }
  }
  
  public void initDragCoords(double x, double y)
  {
    int mainHeight = displayFrame.getMainDiagramPanel().getHeight();
    JScrollBar horizBar = displayFrame.getMainScrollPane().getHorizontalScrollBar();
    int horizOffset = horizBar.isVisible() ? horizBar.getValue() : 0;
    JScrollPane scrollPane = displayFrame.getMainScrollPane();
    JScrollBar vertBar = scrollPane.getVerticalScrollBar();
    int vertOffset = vertBar.isVisible() ? 
      vertBar.getMaximum() - vertBar.getValue() - scrollPane.getViewport().getHeight() : 0;
    int horizBarHeight = horizBar.isVisible() ? horizBar.getHeight() : 0;

    if (this == displayFrame.getFreeVertexPanel())
    {
      freeOldEndPoint = new Point2D.Double(x, y);
      freeStartPoint = new Point2D.Double(x, y);
      mainOldEndPoint = new Point2D.Double(x + horizOffset, 
        y + mainHeight + horizBarHeight - vertOffset);
      mainStartPoint = new Point2D.Double(x + horizOffset, 
        y + mainHeight + horizBarHeight - vertOffset);
    } else if (this == displayFrame.getMainDiagramPanel())
    {
      mainOldEndPoint = new Point2D.Double(x, y);
      mainStartPoint = new Point2D.Double(x, y);
      freeOldEndPoint = new Point2D.Double(x - horizOffset, 
        y - mainHeight - horizBarHeight + vertOffset);
      freeStartPoint = new Point2D.Double(x - horizOffset, 
        y - mainHeight - horizBarHeight + vertOffset);
    }
  }
  
  /*
   * Creates a line of width EDGE_SELECT_WIDTH for each edge
   * and tests if mouse click was in that Shape's boundary.
   * Returns the edge if one was selected, null otherwise.
   */
  public TreeEdge testEdgeShapes(MouseEvent event)
  {
    if (argument.getTree() == null) return null;
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
        if (shape == null) continue;
        Shape wideEdge = edgeWidth.createStrokedShape(shape);
        TreeVertex child = edge.getDestVertex();
        if (wideEdge.contains(x, y)) {
          edge.setSelected(!edge.isSelected());
          // If the edge's parent is an implicit premise, it should be treated
          // the same way as the edge itself
          TreeVertex parent = edge.getSourceVertex();
          if (parent.roles.get("addedNegation") != null &&
                  parent.roles.get("addedNegation").equals("yes"))
          {
            parent.setSelected(edge.isSelected());
          }
          String labelText = "\"" + child.getLabel().toString() + "\"";
          if (child.isVirtual()) {
            labelText = "(linked premises)";
          }
          String supports = "\"" + vertex.getLabel().toString() + "\"";
          if (vertex.isVirtual()) {
            supports = "\"" + vertex.getParent().getLabel().toString() + "\"";
          }
          labelText = prepareMessageLabel(labelText, supports, MAX_MESSAGELABEL_SIZE); 
          araucaria.setMessageLabelText(labelText);
          return edge;
        }
      }
    }
    return null;
  }
  
  public TreeEdge testEdgeShapesPopup(MouseEvent event)
  {
    double x = event.getX(); 
    double y = event.getY();
    BasicStroke edgeWidth = new BasicStroke(EDGE_SELECT_WIDTH);
    Enumeration nodeList = argument.getTree().getVertexList().elements();
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
          mouseX = (int)x;
          mouseY = (int)y;
          mouseEdge = edge;
          mouseEdge.setSelected(true);
          mouseVertex = null;
          createPopupMenu();
          edgePopup.show(event.getComponent(), event.getX(), event.getY());
          return edge;
        }
      }
    }
    return null;
  }
  
  public boolean testSubtreeShapes(MouseEvent event)
  {
    displayText = false;
    double x = event.getX();
    double y = event.getY();
    String text = " ";
    Enumeration subtrees = argument.getSubtreeList().elements();
    while (subtrees.hasMoreElements()) {
      Subtree subtree = (Subtree)subtrees.nextElement();
      if (subtree.getShape(this).contains(x,y)) { 
        // Right button - show popup menu
        mouseSubtree = subtree;
        schemePopup.show(event.getComponent(), event.getX(), event.getY());
        return true;
      }
    }
    return false;
  }
  
  public boolean testSubtrees(MouseEvent event)
  {
    displayText = false;
    double x = event.getX();
    double y = event.getY();
    String text = " ";
    Enumeration subtrees = argument.getSubtreeList().elements();
    while (subtrees.hasMoreElements()) {
      Subtree subtree = (Subtree)subtrees.nextElement();
      if (subtree.getShape(this) != null && subtree.getShape(this).contains(x,y)) {
        subtree.setSelected(!subtree.isSelected());
        if (subtree.isSelected()) {
          text = subtree.getArgumentType().getName(); 
        }
        araucaria.setMessageLabelText(text);
        return true;
      }
    }
    return false;
  }

  public void calcSubtreeShapes()
  {
    Enumeration subtrees = argument.subtreeList.elements();
    while (subtrees.hasMoreElements()) {
      ((Subtree)subtrees.nextElement()).constructShape(this);
    }
  }

  public void drawSubtrees(Graphics2D gg)
  {
    // If any subtrees are defined, draws in the coloured shapes for them
    calcSubtreeShapes();
    Enumeration subtrees = argument.subtreeList.elements();
    while (subtrees.hasMoreElements()) {
      Subtree subtree = (Subtree)subtrees.nextElement();
      Shape subShape = subtree.getShape(this); 
      if (subShape != null) {
        gg.setPaint(subtree.getFillColor());
        gg.fill(subShape);
        if (subtree.isSelected()) {
          GeneralPath outline = new GeneralPath();
          outline.append(subShape.getPathIterator(null), false);
          gg.setPaint(subtree.getOutlineColor());
          gg.setStroke(selectStroke);
          gg.draw(outline);
          gg.setPaint(subtree.getFillColor()); 
          if (this.getClass().getName().equals(FullTextPanel.class.getName()))
          {
            gg.fill(subtree.constructInternalShape(this, true));
          } else {
            gg.fill(subtree.constructInternalShape(this, false));
          }
          gg.setStroke(solidStroke);
        }
      }
    }
  }
  
  public boolean testVertex(MouseEvent event, TreeVertex vertex, double x, double y)
  {
    return testVertex(event, vertex, x, y, true);
  }

  public boolean testVertex(MouseEvent event, TreeVertex vertex, double x, double y, boolean showPopup)
  {
    Shape node = vertex.getShape(this);
    if (node == null) return false;
    if (node.contains(x, y)) {
      mouseX = (int)x;
      mouseY = (int)y;
      mouseVertex = vertex;
      mouseVertex.setSelected(true);
      mouseEdge = null;
      buildPopup(vertex);
      if (showPopup)
      {
        vertexPopup.show(event.getComponent(), event.getX(), event.getY());
      }
      /*
      if (vertex.isMissing()) {
        missingPopup.show(event.getComponent(), event.getX(), event.getY());
      } else {
        vertexPopup.show(event.getComponent(), event.getX(), event.getY());
      }*/
     return true;
    }
    return false;
  }
  
  public void buildPopup(TreeVertex vertex)
  {
    String callingClass = this.getClass().getName();
    vertexPopup = new JPopupMenu();
    vertexTextMenu = new JMenuItem("Show text");
    editMissingTextMenu = new JMenuItem("Edit text");
    editVertexIDMenu = new JMenuItem("Edit ID"); 
    linkMenu = new JMenuItem("Link");
    unlinkMenu = new JMenuItem("Unlink");
    ownerVertexMenu = new JMenuItem("Modify ownership");
    labelVertexMenu = new JMenuItem("Modify evaluation");
    setPremisesVisMenu = new JMenuItem("Collapse premises");
    if (vertex.hidingChildren)
    {
      setPremisesVisMenu.setText("Expand premises");
    }
    if (callingClass.indexOf("FullText") < 0)
    {
      vertexPopup.add(vertexTextMenu);
      vertexTextMenu.addActionListener(this);
      vertexPopup.add(editVertexIDMenu);
      editVertexIDMenu.addActionListener(this);
    }
    if (vertex.isMissing())
    {
      vertexPopup.add(editMissingTextMenu);
      editMissingTextMenu.addActionListener(this);
    }
    vertexPopup.add(linkMenu);
    linkMenu.addActionListener(this);
    vertexPopup.add(unlinkMenu);
    unlinkMenu.addActionListener(this);
    vertexPopup.add(ownerVertexMenu);
    ownerVertexMenu.addActionListener(this);
    vertexPopup.add(labelVertexMenu);
    labelVertexMenu.addActionListener(this);
    if (vertex.getEdgeList().size() > 0)
    {
      vertexPopup.add(setPremisesVisMenu);
    }
    setPremisesVisMenu.addActionListener(this);
    schemePopup = new JPopupMenu();
  }

  public boolean testVertexShapes(MouseEvent event)
  {
    double x = event.getX();
    double y = event.getY();
    Enumeration freeVertices = argument.getFreeVertexList().elements();
    while (freeVertices.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)freeVertices.nextElement();
      if (testVertex(event, vertex, x, y))
        return true;
    }
    Enumeration nodeList = argument.getTree().getVertexList().elements();
    while (nodeList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      if (!vertex.isVirtual() && testVertex(event, vertex, x, y))
        return true;
    }
    return false;
  }

  /**
   * Draws the rubber band dashed line when building the
   * tree.
   */
  void rubberBand(Point2D.Double startPoint,
    Point2D.Double oldEndPoint, Point2D.Double endPoint, 
    DiagramBase diagram)
  {
    Graphics2D gg = (Graphics2D)diagram.getGraphics();
    gg.setColor(diagram.getBackground());
    Line2D.Double line = new Line2D.Double(startPoint, oldEndPoint);
    Stroke fatLine = new BasicStroke(5);
    gg.clip(fatLine.createStrokedShape(line));
    gg.draw(line);
    gg.setStroke(solidStroke);
    diagram.paint(gg);
    gg.setClip(null);

    gg.setStroke(dashStroke);
    gg.setColor(Color.blue);
    gg.setClip(null);
    line = new Line2D.Double(startPoint, endPoint);
    gg.draw(line);
    oldEndPoint.x = endPoint.x;
    oldEndPoint.y = endPoint.y;
  }

  public void doLeafCounts(TreeVertex root)
  {
    if(root.leafCount != 0)
    {
      return;
    }
    Vector edgeList = root.getEdgeList();
    if(edgeList.size() == 0)
    {
      root.leafCount = 1;
      return;
    }
    int refutationCount = 0;
    int visibleEdgeCount = 0;
    for (int i = 0; i<edgeList.size(); i++) {
      TreeEdge edge = (TreeEdge)edgeList.elementAt(i);
      if (!edge.visible)
      {
        continue;
      }
      visibleEdgeCount++;
      TreeVertex child = edge.getDestVertex();
      doLeafCounts(child);
      if(!child.isRefutation() && child.visible)
      {
        root.leafCount += getFullLeafCount(child);
      } else {
        refutationCount++;
      }
    }
    if (visibleEdgeCount == 0)
    {
      root.leafCount = 1;
    }
    if(root.leafCount == 0 && refutationCount > 0)
    {
      root.leafCount = 1;
    }
  }
  
  public int getFullLeafCount(TreeVertex vertex)
  {
    int fullCount = vertex.leafCount;
    Vector edgeList = vertex.getEdgeList();
    for (int i = 0; i < edgeList.size(); i++) 
    {
      TreeEdge edge = (TreeEdge)edgeList.elementAt(i);
      TreeVertex child = edge.getDestVertex();
      if(child.isRefutation() && child.visible)
      {
        fullCount += getFullLeafCount(child);
      }
    }
    return fullCount;
  }
  
  public void showHidePremises()
  {
    if (setPremisesVisMenu.getText().equals("Collapse premises"))
    {
      setPremisesVisMenu.setText("Expand premises");
      mouseVertex.hidingChildren = true;
      setChildVisibility(mouseVertex, false);
    } else if (setPremisesVisMenu.getText().equals("Expand premises")) 
    {
      setPremisesVisMenu.setText("Collapse premises");
      mouseVertex.hidingChildren = false;
      setChildVisibility(mouseVertex, true);
    }
    clearLeafCounts(argument.getTree());
    Vector roots = argument.getTree().getRoots();
    for (int i = 0; i < roots.size(); i++)
    {
      TreeVertex root = (TreeVertex)roots.elementAt(i);
      doLeafCounts(root);
    }
    araucaria.updateDisplays(true);
  }

  public void editMissingText()
  {
    String missingPremise = JOptionPane.showInputDialog(araucaria, 
      "Enter new text for missing premise.                                                                            ", 
      (String)mouseVertex.m_label);
    if (missingPremise != null) {
      mouseVertex.m_label = missingPremise;
      araucaria.updateDisplays(true);
      displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "editing missing premise"));
    }
  }
  
  public void editVertexID()
  {
    String newID = null;
    String validID = null;
    do {
      newID = (String)JOptionPane.showInputDialog(this, "Enter a new ID for the premise.\n" +
        "An ID must be 1 or 2 letters, an integer\nor a real number with a single decimal place\n and may not already be in use.\n" +
        "Uppercase or lowercase letters may be used.", 
        "Edit vertex ID", JOptionPane.QUESTION_MESSAGE, null, null, mouseVertex.getShortLabelString());
      if (newID == null) 
        break;
      validID = argument.isValidID(newID);
      if (validID != null)
      {
        araucaria.setMessageLabelText(validID);
        newID = "Error";
      } else if (validID == null) 
      {
        mouseVertex.setShortLabel(newID);
        displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "editing premise ID"));
        argument.argumentSaved = false;
        araucaria.updateDisplays(true);
      }
    } while (newID.equals("Error"));
    araucaria.setMessageLabelText(" ");
  }
  
  /**
   * Creates the text for a post-it note.
   * Returns the length of the note in pixels.
   */
  AttributedString postitText;
  int postitTextX, postitTextY;
  
  public Dimension createText(String text)
  {
    postitText = new AttributedString(text);
    Font font = new Font("SansSerif", Font.BOLD, 12);
    postitText.addAttribute(TextAttribute.FONT, font);
    Color backColor = new Color(1.0f, 1.0f, 0.8f);
    postitText.addAttribute(TextAttribute.BACKGROUND, backColor);
    Rectangle2D bounds = font.getStringBounds(text, new FontRenderContext(null, false, false));
    return new Dimension((int)bounds.getWidth(), (int)bounds.getHeight());
  }

  /**
   * Sets the upper left position for a post-it note on the canvas.
   * If the node is too wide to fit between the given x value and
   * the right-hand edge of the canvas, the x value is shifted over.
   */
  public void setTextPos(Dimension dim, int x, int y)
  {
    int canvasWidth = this.getWidth();
    if (x + dim.width > canvasWidth)
      postitTextX = canvasWidth - dim.width - 10;
    else
      postitTextX = x;
    FontMetrics fontMetrics = getGraphics().getFontMetrics();
    int fontLine = 2*(fontMetrics.getAscent() + fontMetrics.getDescent() +
      fontMetrics.getLeading());
    if (y + dim.height + fontLine > this.getHeight()) {
      postitTextY = this.getHeight() - dim.height - fontLine;
    } else {
      postitTextY = y;
    }
  }

  public void drawText(Graphics2D gg)
  {
    AttributedCharacterIterator iter = postitText.getIterator();
    FontMetrics fontMetrics = gg.getFontMetrics();
    int finalPostitTextY = postitTextY + 2*(fontMetrics.getAscent() + fontMetrics.getDescent() +
      fontMetrics.getLeading());
    gg.drawString(iter, postitTextX, finalPostitTextY);
  }
  
  public void showVertexText()
  {
    displayText = true;
    String postitString = prepareMessageLabel(mouseVertex.getLabel().toString(),
      DiagramBase.MAX_POSTIT_SIZE);
    setTextPos(createText(postitString), mouseX, mouseY);
    araucaria.updateDisplays(true);
  }

  public void actionPerformed(ActionEvent e)
  {
  }
  
  /**
   * Provides routes to methods that handle various mouse events. Normally these
   * methods would be overridden in derived classes to provide customized behaviour.
   */
  public void handleMouse()
  {
    // Mouse clicks
    addMouseListener(new MouseAdapter()
      {
        // Mouse button down
        public void mousePressed(MouseEvent e)
        { 
          // Right mouse button
          if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == 
            InputEvent.BUTTON3_DOWN_MASK)
          {
            rightMousePressed(e);
          }
          // Left button + shift key
          else if ((e.getModifiersEx() & (InputEvent.BUTTON1_DOWN_MASK | 
            InputEvent.SHIFT_DOWN_MASK)) == 
            (InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))
          {
            leftMousePressedShift(e);
          } 
          // Left mouse button
          else if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 
            InputEvent.BUTTON1_DOWN_MASK)
          {
            leftMousePressed(e);
          }
        }
        
        public void mouseReleased(MouseEvent e)
        {
          // Right mouse button
          // We must use isPopupTrigger rather than InputEvent masks here
          // since the masks don't work for the released event
          if (e.isPopupTrigger())
          {
            rightMouseReleased(e);
          }
          // Left mouse button
          else 
          {
            leftMouseReleased(e);
          }
        }
      }
    );
    
    // Mouse dragging
    addMouseMotionListener(new MouseMotionAdapter()
      {
        public void mouseDragged(MouseEvent e) 
        {
          dragMouse(e);
        }
      }
    );
  }
  
  /**
   * Handles unmodified left mouse press. A left mouse press can do various
   * things depending on the context. In order:
   *
   * 1. If text has been selected in the left panel, an attempt is made to
   *    add a free vertex to the diagram. This is possible if the selected
   *    text doesn't overlap with the existing selections.
   * 2. If no text has been selected, attempt to select a vertex, edge or
   *    subtree. This will clear any previously selected components. To make
   *    multiple selections, hold down the shift key (see next method).
   */
  public void leftMousePressed(MouseEvent e)
  {
    araucaria.setMessageLabelText(" ");
    pendingStartRole = "none";
    if (argument != null)
    {
      argument.clearAllSelections();  
    }
    if (addFreeVertex())
    {
      canCreateEdge = false;
    } else {             // No text selected, so try to select a vertex or edge
      if (e.getClickCount() == 2)
      {
        // Double-click
      }
      selectVertexEdge(e);
    }
  }
   
  public void leftMousePressedShift(MouseEvent e)
  {
    if (araucaria.getSelectText().isTextSelected()) {
      araucaria.getSelectText().clearSelection();
    }
    selectVertexEdge(e);
  }
 
  public void leftMouseReleased(MouseEvent e)
  {
    double x = e.getX(); 
    double y = e.getY();

    // If no start vertex selected or start vertex is virtual, do nothing
    if (startVertex == null || startVertex.isVirtual()) {
      return;
    }
    endVertex = getSelectedVertex(x, y, displayFrame.getMainDiagramPanel(),
      displayFrame.getFreeVertexPanel());
    // If no end vertex selected, see if diagram supports selecting end vertex
    // by dragging onto an edge (e.g. Toulmin)
    if (endVertex == null) {
      endVertex = getSelectedVertexFromEdge(x, y, displayFrame.getMainDiagramPanel(),
        displayFrame.getFreeVertexPanel());
      // If still no luck, do nothing
      if (endVertex == null)
      { return; }
    }
    // If mouse pressed and released within the same vertex, print vertex label
    if (endVertex == startVertex) {
      String messageLabel = prepareMessageLabel( 
        "\"" + (String)endVertex.getLabel() + "\"",
        MAX_MESSAGELABEL_SIZE);
      araucaria.setMessageLabelText(messageLabel);
      canCreateEdge = false; 
      startVertex.setSelected(!startVertex.isSelected());
      return;
    } else {
      addNewEdge();
    }
  }
  
  /**
   * Dummy method to be overridden to handle right mouse clicks.
   */
  public void rightMousePressed(MouseEvent e)
  {
    if (testVertexShapes(e))
    {
     return;
    }
    if (testEdgeShapesPopup(e) != null)
    {
      return;
    }
    if (testSubtreeShapes(e))
    {
      repaint();
      return;
    }
  }
  
  /**
   * Dummy method to be overridden to handle right mouse clicks.
   */
  public void rightMouseReleased(MouseEvent e)
  {
    //argument.clearAllSelections();
  }
  
  /**
   * Dummy method allowing a diagram type (e.g. Toulmin) to set properties
   * of vertices when they are added to the tree.
   *
   * @return flag indicating whether an undo event should be added. Should only be
   * false if the method itself handles the undo event.
   */
  public boolean setVertexProperties(TreeVertex start, TreeVertex end)
  {
    return true;
  }
  
  public void dragMouse(MouseEvent e)
  {
    // Right mouse button - do nothing
    if ((e.getModifiers() & InputEvent.BUTTON3_DOWN_MASK) ==
        InputEvent.BUTTON3_DOWN_MASK) {
      return;
    }

    if (!canCreateEdge) return;
    double x = e.getX();
    double y = e.getY();
    int mainHeight = displayFrame.getMainDiagramPanel().getHeight();
    JScrollPane scrollPane = displayFrame.getMainScrollPane();
    JScrollBar horizBar = scrollPane.getHorizontalScrollBar();
    int horizOffset = horizBar.isVisible() ? horizBar.getValue() : 0;
    JScrollBar vertBar = displayFrame.getMainScrollPane().getVerticalScrollBar();
    int vertOffset = vertBar.isVisible() ? 
      vertBar.getMaximum() - vertBar.getValue() - scrollPane.getViewport().getHeight() : 0;
    int horizBarHeight = horizBar.isVisible() ? horizBar.getHeight() : 0;
    
    if (this == displayFrame.getFreeVertexPanel())
    {
      rubberBand(freeStartPoint, freeOldEndPoint, new Point2D.Double(x,y), this);
      if (y < 5) 
      {
        rubberBand(mainStartPoint, mainOldEndPoint, 
          new Point2D.Double(x + horizOffset, y + mainHeight + horizBarHeight - vertOffset), 
          displayFrame.getMainDiagramPanel());
      }
    } else if (this == displayFrame.getMainDiagramPanel()) 
    {
      rubberBand(mainStartPoint, mainOldEndPoint, new Point2D.Double(x,y), this);
      if (y > mainHeight + horizBarHeight - vertOffset - 5)
      {
        rubberBand(freeStartPoint, freeOldEndPoint, 
          new Point2D.Double(x - horizOffset, y - mainHeight - horizBarHeight + vertOffset), 
          displayFrame.getFreeVertexPanel());
      }
    }
  }

  public void addNewEdge()
  {
    if (!canCreateEdge) {
      repaint();
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
      // If addNewEdge is true here, either or both vertexes are new
      // to the tree, so new edge must be OK
      argument.getTree().addEdge(endVertex, startVertex);
      startVertex.setHasParent(true);
      startVertex.setParent(endVertex);

//      calcSubtreeShapes();
      if (argument.multiRoots) {
        argument.deleteDummyRoot();
        argument.multiRoots = false;
      }
      Vector roots = argument.getTree().getRoots();
      if (roots.size() > 1) {
        if (argument.getTree().getDummyRoot() == null) {
          argument.addDummyRoot(roots);
        }
      }
      
      boolean addUndoEvent = displayFrame.getMainDiagramPanel().setVertexProperties(startVertex, endVertex);
      redrawTree(true);
      if (addUndoEvent)
      {
        displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "adding a support"));
        araucaria.doUndo(false, false);
        araucaria.doRedo(false);
      }
      return;
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
    startVertex.setParent(endVertex);
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
    boolean addUndoEvent = displayFrame.getMainDiagramPanel().setVertexProperties(startVertex, endVertex);
    if (addUndoEvent)
    {
      displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "adding a support"));
      araucaria.doUndo(false, false);
      araucaria.doRedo(false);
    }
  }

  public void deleteCycle(TreeVertex startVertex, TreeVertex endVertex)
  {
    araucaria.setMessageLabelText("Adding this edge would create a cycle.");
    endVertex.deleteEdge(startVertex);
    startVertex.setHasParent(false);
    repaint();
  }

  public void setArgument(Argument a)
  {
    argument = a; 
  }
  
  public Argument getArgument()
  {
    return argument; 
  }
  
  public void setDisplayFrame(DisplayFrame d)
  {
    displayFrame = d;
  }
  
  public void setAraucaria(Araucaria a)
  { araucaria = a; }
  
  public DisplayFrame getDisplayFrame()
  { return displayFrame; }
  
  public static void sort(Vector v)
  {
  	if (v.size() <= 1) return;
  	Vector temp;
  	int marker1, marker2;
  	for (marker1 = 1; marker1 < v.size(); marker1++) {
  		Vector elem1 = (Vector)v.elementAt(marker1);
  		Vector elem2 = (Vector)v.elementAt(marker1 - 1);
  		if (((String)elem1.elementAt(0)).compareTo((String)elem2.elementAt(0)) < 0) {
  			temp = elem1;
  			for (marker2 = marker1 - 1; marker2 >= 0; --marker2) {
  				v.set(marker2 + 1, v.elementAt(marker2));
  				if (marker2 == 0 || 
  					((String)((Vector)v.elementAt(marker2 - 1)).elementAt(0)).
  						compareTo((String)temp.elementAt(0)) < 0)
  					break;
  			}
  			v.set(marker2, temp);
  		}
	  }
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
  
  /**
   * Starting at root, recursively sets the invisible flag on all 
   * vertexes and edges arising out of root
   */
  public void setChildVisibility(TreeVertex root, boolean isVisible)
  {
    if (root == null)
    {
      return;
    }
    Vector edgeList = root.m_edgeList;
    for (int i = 0; i < edgeList.size(); i++)
    {
      TreeEdge edge = (TreeEdge)edgeList.elementAt(i);
      edge.visible = isVisible;
      TreeVertex vertex = (TreeVertex)edge.getDestVertex();
      vertex.visible = isVisible;
      if (!vertex.hidingChildren)
      {
        setChildVisibility(vertex, isVisible);
      }
    }
  }
}
