/*
 * FullTextPanel.java
 *
 * Created on 25 March 2004, 15:36
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

public class FullTextPanel extends DiagramBase
{
//  int textWidth = 100;  // Width allowed for the text label for a given vertex
  int horizTextSpace = 30; // Minimum horizontal distance between adjacent text boxes
  int minHorizCells = 0;   // Minimum number of horizontal cells required for non-overlapping text boxes
  int edgeHeight = 50;  // Vertical extent of the edge line between layers.
//  int textBorderMargin = 3; // Distance between text and bounding box
  int refutationOffset = 7; // Distance from top of bounding box to horizontal refutation link
//  int topOffset = 15;    // Distance from top of client area to root node
  int totalLeaves;
//  int canvasWidth, canvasHeight;
  Vector layerHeights;
  Vector supportLabelText;
  int supportLabelTextX, supportLabelTextY;
  Color textBackground = Color.white;
  float SUBTREE_LINE_WIDTH = 20.0f;


  /** Creates a new instance of FullTextPanel */
  public FullTextPanel()
  {
    // Required to prevent corrupted graphics when panel is smaller than viewport
    // in the scroll pane
    setOpaque(false);
  }
  
  public void clearLeafCounts(Tree tree)
  {
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++) 
    {
      TreeVertex vertex = (TreeVertex)vertexList.elementAt(i);
      vertex.leafCount = 0;
    }
  }
  
  public float getSubtreeLineWidth()
  { return SUBTREE_LINE_WIDTH; }

  public void addSubtreeLabels()
  {
    argument.getTree().clearSchemeLabels();
    Vector subtreeList = argument.getSubtreeList();
    Enumeration subtrees = subtreeList.elements();
    while (subtrees.hasMoreElements()) {
      Subtree subtree = (Subtree)subtrees.nextElement();
      // Add the scheme's name and colour to the subtree's root vertex
      TreeVertex root = subtree.findRoot();
      Vector labelList = root.schemeLabels;
      labelList.add(subtree.getArgumentType().getName());
      root.m_schemeColorList.add(subtree.getOutlineColor());
      root.schemeList.add(subtree);
    }
  }
  
  // Calculates the text layout for each vertex's text
  protected void calcTextLayouts() 
  {
    int height;
    Enumeration enumer = argument.getTree().getVertexList().elements();
    while (enumer.hasMoreElements()) {
      int layoutHeight = 0;
      TreeVertex vertex = (TreeVertex)enumer.nextElement(); 
      if (vertex.m_nodeLabel != null && argument.isShowSupportLabels()) {
        vertex.nodeLabelLayout = calcLayout(vertex.m_nodeLabel, boldMap);
        height = getLayoutHeight(vertex.nodeLabelLayout);
        layoutHeight += height;
        vertex.nodeLabelLayoutSize = new Dimension(textWidth, height);
      }
      String text = (String)vertex.getLabel();
      vertex.textLayout = calcLayout(text, plainMap);
      height = getLayoutHeight(vertex.textLayout);
      layoutHeight += height;
      vertex.textLayoutSize = new Dimension(textWidth, height);
      vertex.ownersLayout = null;
      
      if (vertex.getOwners().size() > 0 && argument.isShowOwners()) {
        vertex.ownersLayout = createOwnerText(vertex.getOwners());
        height = getLayoutHeight(vertex.ownersLayout);
        layoutHeight += height;
        vertex.ownersLayoutSize = new Dimension(textWidth, height);
      }
      // Scheme labels
      if (vertex.schemeLabels.size() > 0) {
        vertex.schemeLayout = createSchemeText(vertex.schemeLabels, vertex);
        height = getLayoutHeight(vertex.schemeLayout);
        layoutHeight += height;
        vertex.schemeLayoutSize = new Dimension(textWidth, height);
      }
      
      vertex.totalLayoutSize = new Dimension(textWidth, layoutHeight);
      
      // Edge label
      if (vertex.getSupportLabel() != null && argument.isShowSupportLabels()) {
        vertex.supportLabelLayout = calcLayout(vertex.getSupportLabel(), plainMap);
        vertex.supportLabelLayoutSize = new Dimension(textWidth, height);
      }
    }
  }
  
  public Vector calcLayout(String text, Hashtable map)
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
        layout = lineBreak.nextLayout(textWidth);
        textLayout.add(new TextLine(layout, lineStart + returns + first, 
                lineStart + returns + lineBreak.getPosition()));
//        textLayout.add(new TextLine(layout, first, lineBreak.getPosition()));
        first = lineBreak.getPosition();
      }
      lineStart += last;
    }
    return textLayout;
  }
  
  protected int getLayoutHeight(Vector textLayout)
  {
    TextLayout layout = ((TextLine)textLayout.get(0)).getLayout();
    int lineSpacing = (int)(layout.getAscent() + layout.getDescent() + layout.getLeading());
    return lineSpacing*textLayout.size();
  }
  
  /**
   * Creates the text for a list of owners of a given vertex
   */
  protected Vector createOwnerText(Set ownerSet)
  {
    Vector ownerText = new Vector();
    String ownerString = "";
    Vector rows = new Vector();
    
    Iterator ownerIter = ownerSet.iterator();
    while (ownerIter.hasNext()) {
      Vector owner = (Vector)ownerIter.next();
      rows.add(owner);
    }
    OwnerSourceTableModel.sort(rows);
    
    for (int i = 0; i < rows.size(); i++)
    {
      Vector owner = (Vector)rows.elementAt(i);
      ownerString = (String)owner.elementAt(1);
      if (ownerString.length() == 0)
        continue;
      AttributedString ownerAttrib = new AttributedString(ownerString, italicMap);
      AttributedCharacterIterator ownerCharIter = ownerAttrib.getIterator();
      FontRenderContext frc = new FontRenderContext(null, false, false);
      LineBreakMeasurer lineBreak = new LineBreakMeasurer(ownerCharIter, frc);;
      int first = ownerCharIter.getBeginIndex();
      int last = ownerCharIter.getEndIndex();
      lineBreak.setPosition(first);
      TextLayout layout;
      while (lineBreak.getPosition() < last) {
        layout = lineBreak.nextLayout(textWidth);
        ownerText.add(new TextLine(layout, first, lineBreak.getPosition()));
        first = lineBreak.getPosition();
      }
    }
    return ownerText;
  }
  
  /**
   * Creates the text for a list of schemes of which vertex is the parent
   */
  protected Vector createSchemeText(Vector schemeSet, TreeVertex vertex)
  {
    Vector schemeText = new Vector();
    String schemeString = "";
    Vector rows = new Vector();
    
    Iterator schemeIter = schemeSet.iterator();
    while (schemeIter.hasNext()) {
      String scheme = (String)schemeIter.next();
      rows.add(scheme);
    }
    
    for (int i = 0; i < rows.size(); i++)
    {
      schemeString = (String)rows.elementAt(i);
      if (schemeString.length() == 0)
        continue;
      AttributedString schemeAttrib = new AttributedString(schemeString, boldMap);
      AttributedCharacterIterator schemeCharIter = schemeAttrib.getIterator();
      FontRenderContext frc = new FontRenderContext(null, false, false);
      LineBreakMeasurer lineBreak = new LineBreakMeasurer(schemeCharIter, frc);;
      int first = schemeCharIter.getBeginIndex();
      int last = schemeCharIter.getEndIndex();
      lineBreak.setPosition(first);
      TextLayout layout;
      while (lineBreak.getPosition() < last) {
        layout = lineBreak.nextLayout(textWidth);
        TextLine textLine = new TextLine(layout, first, lineBreak.getPosition());
        textLine.textColor = (Color)vertex.m_schemeColorList.elementAt(i);
        schemeText.add(textLine);
        first = lineBreak.getPosition();
      }
    }
    return schemeText;
  }
  
  protected void calcLayerSizes(TreeVertex root, int layerNum)
  {
    Vector refutationList = new Vector();
    root.getRefutationList(refutationList);
    refutationList.add(root);
    int numRefutations = refutationList.size();

    double maxTextHeight = 0, maxSubtreeLabelHeight = 0, maxOverallHeight = 0;
    // Find the max subtree label height
    
    for (int i=0; i < refutationList.size(); i++) {
      TreeVertex vertex = (TreeVertex)refutationList.elementAt(i);
      if(vertex.schemeLayoutSize.getHeight() > maxSubtreeLabelHeight)
      {
        maxSubtreeLabelHeight = vertex.schemeLayoutSize.getHeight();
      }
    }
    // Find max textbox height
    for (int i=0; i < refutationList.size(); i++) {
      TreeVertex vertex = (TreeVertex)refutationList.elementAt(i);
      if (vertex.totalLayoutSize.getHeight() - vertex.schemeLayoutSize.getHeight() > maxTextHeight) {
        maxTextHeight = vertex.totalLayoutSize.getHeight() - vertex.schemeLayoutSize.getHeight();
      }
    }
    // Max overall height = subtree labels + textbox. Allows aligning textboxes in refuts.
    maxOverallHeight = maxSubtreeLabelHeight + maxTextHeight;
    if (layerHeights.size() < layerNum + 1) {
      layerHeights.add(new Double(maxOverallHeight));
    } else {
      if (maxTextHeight > ((Double)layerHeights.elementAt(layerNum)).doubleValue()) {
        layerHeights.setElementAt(new Double(maxOverallHeight), layerNum);
      }
    }

   // Do recursion to work out lower layers
    for (int i=0; i < refutationList.size(); i++) {
      TreeVertex vertex = (TreeVertex)refutationList.elementAt(i);
      int numNonRefutationChildren = vertex.getNumberOfChildren() - vertex.getNumRefutations();
      if (numNonRefutationChildren == 0) {
        continue;
      }
      Vector childEdges = vertex.getEdgeList();
      int nonRefutCount = 0;
      for (int j = 0; j < childEdges.size(); j++) {
        TreeEdge edge = (TreeEdge)childEdges.elementAt(j);
        TreeVertex child = edge.getDestVertex();
        if (!child.isRefutation()) {
          calcLayerSizes(child, layerNum + 1);
          nonRefutCount ++;
        }
      }
    }
  }
  
  protected int getTotalLeaves(TreeVertex root)
  {
    Vector refutationList = new Vector();
    root.getRefutationList(refutationList);
    refutationList.add(root);
    int totalLeaves = 0;
    for (int i = 0; i < refutationList.size(); i++) 
    {
      totalLeaves += ((TreeVertex)refutationList.elementAt(i)).leafCount;
    }
    return totalLeaves;
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
    
    // Find the max subtree label height
    double maxSubtreeLabelHeight = 0;
    for (int i=0; i < refutationList.size(); i++) {
      TreeVertex vertex = (TreeVertex)refutationList.elementAt(i);
      if(vertex.schemeLayoutSize.getHeight() > maxSubtreeLabelHeight)
      {
        maxSubtreeLabelHeight = vertex.schemeLayoutSize.getHeight();
      }
    }
    
    int xMin = leftWidth;
    int xMax = xMin;
    for (int i = 0; i < refutationList.size(); i++) {
      TreeVertex vertex = (TreeVertex)refutationList.elementAt(i);
      xMax = xMin + vertex.leafCount * (textWidth + horizTextSpace);
      vertex.setExtentFullText(xMin, xMax);
      // Align all text boxes in a refut arrangement - allows for scheme labels
      int nodeHeight = (int)(height + maxSubtreeLabelHeight - vertex.schemeLayoutSize.getHeight());
      if (invertedTree) {
        vertex.setDrawPointFullText((xMin + xMax)/2, canvasHeight - 
          topOffset - nodeHeight - layerNum * edgeHeight);
      } else {
        vertex.setDrawPointFullText((xMin + xMax)/2, topOffset + nodeHeight + layerNum * edgeHeight);
      }
      Point corner = vertex.getDrawPointFullText();
      Dimension totalLayoutSize = vertex.totalLayoutSize;
      int schemeLayoutHeight = 0;
      if (vertex.schemeLayoutSize != null) {
        schemeLayoutHeight = (int)vertex.schemeLayoutSize.getHeight();
      }
      Shape boxShape = new Rectangle2D.Double(corner.x - textWidth/2 - textBorderMargin, 
        corner.y + schemeLayoutHeight - textBorderMargin, 
        (int)totalLayoutSize.getWidth() + 2 * textBorderMargin,
        (int)totalLayoutSize.getHeight() - schemeLayoutHeight + 2 * textBorderMargin);
      vertex.setShape(boxShape, this);
      xMin = xMax;
    }
    
   // Do recursion to work out lower layers
    if (!root.hidingChildren)
    {
      for (int i=0; i < refutationList.size(); i++) {
        TreeVertex vertex = (TreeVertex)refutationList.elementAt(i);
        xMin = vertex.getXMinFullText(); xMax = vertex.getXMaxFullText();
        int numNonRefutationChildren = vertex.getNumberOfChildren() - vertex.getNumRefutations();
        if (numNonRefutationChildren == 0) {
          continue;
        }
   //     int childWidth = (xMax - xMin) / numNonRefutationChildren;
        Vector childEdges = vertex.getEdgeList();
        for (int j = 0; j < childEdges.size(); j++) {
          TreeEdge edge = (TreeEdge)childEdges.elementAt(j);
          TreeVertex child = edge.getDestVertex();
          if (!child.isRefutation()) {
            int childWidth = getFullLeafCount(child) * (textWidth + horizTextSpace);
            int nextLayer = invertedTree ? layerNum + 1 : layerNum;
            calcNodeCoords(child, childWidth, xMin,
              height + (int)((Double)layerHeights.elementAt(nextLayer)).doubleValue(), 
              layerNum + 1, invertedTree);
            xMin += childWidth;
         }
        }
      }
    }

    // All nodes now have positions calculated, so calculate edges
    Vector vertexList = argument.getTree().getVertexList();
    for (int i = 0; i < vertexList.size(); i++) {
      TreeVertex child = (TreeVertex)vertexList.elementAt(i);
      if (!child.visible)
      {
        continue;
      }
      TreeVertex parent = child.getParent();
      if (parent != null) {
        Vector edges = parent.getEdgeList();
        for (int j = 0; j < edges.size(); j++) {
          TreeEdge parentEdge = (TreeEdge)edges.elementAt(j);
          if (parent.isVirtual()) {
            calcVirtualEdgeFullText(parentEdge);
          } else {
            calcStraightEdgeFullText(parentEdge);
          }
        }
      }
    }
  }

  // Calculates the straight edge between normal vertices on FullTextDialog
  protected void calcStraightEdgeFullText(TreeEdge edge)
  {
    try {
      Point parentCorner = edge.getSourceVertex().getDrawPointFullText();
      Dimension parentSize = edge.getSourceVertex().totalLayoutSize;
      Point corner = edge.getDestVertex().getDrawPointFullText();
      Dimension childSize = edge.getDestVertex().totalLayoutSize;
      
      if (edge.getDestVertex().isRefutation()) {
        int box1y = (int)((Rectangle2D.Double)edge.getSourceVertex().getShape(this)).getY();
        int box2y = (int)((Rectangle2D.Double)edge.getDestVertex().getShape(this)).getY();
        int refutY = Math.max(box1y, box2y);
        Shape lineE = new Line2D.Float(
              parentCorner.x - (int)parentSize.getWidth()/2 - textBorderMargin,
              refutY + refutationOffset,
              corner.x + (int)childSize.getWidth()/2 + textBorderMargin,
              refutY + refutationOffset);
        GeneralPath linePath = new GeneralPath(lineE); 
        edge.setSchemeShape((GeneralPath)linePath.clone(), this);
        // Add the arrowhead
        linePath.append(DiagramBase.addArrowHead(parentCorner.x - (int)parentSize.getWidth()/2 - textBorderMargin,
              refutY + refutationOffset,
              corner.x + (int)childSize.getWidth()/2 + textBorderMargin,
              refutY + refutationOffset, 0, 5, 7), false); 
        
        // Refutation edges have arrows in both directions
        linePath.append(DiagramBase.addArrowHead(corner.x + (int)childSize.getWidth()/2 + textBorderMargin,
              refutY + refutationOffset,
              parentCorner.x - (int)parentSize.getWidth()/2 - textBorderMargin,
              refutY + refutationOffset, 0, 5, 7), false);
        edge.setShape(linePath, this); 
        return;
      }
      if (argument.isInvertedTree()) {
        // For inverted trees, normal edge is drawn from the bottom of the
        // premise's text box to the top of the conclusion's text box
        Shape lineE = new Line2D.Float(parentCorner.x,
              parentCorner.y - textBorderMargin,
              corner.x,
              corner.y + (int)childSize.getHeight() + textBorderMargin);
        GeneralPath linePath = new GeneralPath(lineE);
        // Set the scheme shape to be the line without the arrowhead
        edge.setSchemeShape((GeneralPath)linePath.clone(), this);
        // Add the arrowhead
        linePath.append(DiagramBase.addArrowHead(parentCorner.x,
              parentCorner.y - textBorderMargin,
              corner.x,
              corner.y + (int)childSize.getHeight() + textBorderMargin, 0, 5, 7), false);
        edge.setShape(linePath, this); 
        return;
      }
      // Draw point for text box is the middle of the top edge
      Shape lineE = new Line2D.Float(parentCorner.x,
            parentCorner.y + (int)parentSize.getHeight() + 2 * textBorderMargin,
            corner.x,
            corner.y - textBorderMargin);
      GeneralPath linePath = new GeneralPath(lineE);
      // Set the scheme shape to be the line without the arrowhead
      edge.setSchemeShape((GeneralPath)linePath.clone(), this);
      // Add the arrowhead
      linePath.append(DiagramBase.addArrowHead(parentCorner.x,
            parentCorner.y + (int)parentSize.getHeight() + 2 * textBorderMargin,
            corner.x,
            corner.y - textBorderMargin, 0, 5, 7), false);
      edge.setShape(linePath, this); 
    }
    catch (Exception ex) {
    }
  }

  // Calculates the bent line used to draw a virtual edge
  // rather than the straight edge between normal vertices
  protected void calcVirtualEdgeFullText(TreeEdge edge)
  {
    try {
      Point parentCorner = edge.getSourceVertex().getDrawPointFullText();
      Dimension parentSize = edge.getSourceVertex().totalLayoutSize;
      Point corner = edge.getDestVertex().getDrawPointFullText();
      Dimension childSize = edge.getDestVertex().totalLayoutSize;
      GeneralPath bentLine = new GeneralPath();
      if (argument.isInvertedTree()) {
        // Horizontal bit
        Shape lineE = new Line2D.Float(parentCorner.x,
              parentCorner.y + (int)parentSize.getHeight() + textBorderMargin,
              corner.x,
              parentCorner.y + (int)parentSize.getHeight() + textBorderMargin);
        bentLine.append(lineE, true);
        // Vertical bit
        lineE  = new Line2D.Float(corner.x,
               parentCorner.y + (int)parentSize.getHeight() + textBorderMargin,
               corner.x,
               corner.y + (int)childSize.getHeight() + textBorderMargin);
        bentLine.append(lineE, true);
        edge.setShape(bentLine, this);
        edge.setSchemeShape(bentLine, this);
        return;
      }
      // Horizontal bit
      Shape lineE = new Line2D.Float(parentCorner.x,
            parentCorner.y - textBorderMargin,
            corner.x,
            parentCorner.y - textBorderMargin);
      bentLine.append(lineE, true); 
      // Vertical bit
      lineE  = new Line2D.Float(corner.x,
             parentCorner.y - textBorderMargin,
             corner.x,
             corner.y - textBorderMargin);
      bentLine.append(lineE, true);
      edge.setShape(bentLine, this);
      edge.setSchemeShape(bentLine, this);
    }
    catch (Exception ex) {
    }
  }
  
  public void calcSubtreeShapes()
  {
    Vector subtreeList = argument.getSubtreeList();
    Enumeration subtrees = subtreeList.elements();
    while (subtrees.hasMoreElements()) {
      Subtree subtree = (Subtree)subtrees.nextElement();
      // Build the shape to be filled when the subtree is drawn
      subtree.constructFullTextShape(this);
    }
  }

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

  protected int createSupportLabelText(String supportLabel, Color backColor)
  {
    Font font = new Font("SansSerif", Font.PLAIN, 11);
    int maxWidth = 0;
    supportLabelText = new Vector();
    AttributedString ownerAttrib = new AttributedString(supportLabel);
    ownerAttrib.addAttribute(TextAttribute.FONT, font);
    ownerAttrib.addAttribute(TextAttribute.BACKGROUND, backColor);
    supportLabelText.add(ownerAttrib);
    Rectangle2D bounds = font.getStringBounds(supportLabel, new FontRenderContext(null, false, false));
    if ((int)bounds.getWidth() > maxWidth)
      maxWidth = (int)bounds.getWidth();
    return maxWidth;
  }
  
  /**
   * Sets upper left position for an edge label. To match node labels
   * we put labels on the left.
   */
  protected void setSupportLabelTextPos(int width, TreeVertex vertex, TreeVertex edgeSource)
  {
    Point point1 = vertex.getDrawPointFullText();
    Point point2 = edgeSource.getDrawPointFullText();
    Point labelPoint;
    if (vertex.isVirtual()) {
      if (argument.isInvertedTree()) {
        labelPoint = new Point(point2.x - width, 
          (int)(point1.y + point2.y + edgeSource.totalLayoutSize.getHeight())/2);
      } else {
        labelPoint = new Point(point2.x - width, 
          (int)(point1.y + point2.y + vertex.totalLayoutSize.getHeight())/2);
      }
    } else if (edgeSource.isRefutation()) {
      int box1y = (int)((Rectangle2D.Double)vertex.getShape(this)).getY();
      int box2y = (int)((Rectangle2D.Double)edgeSource.getShape(this)).getY();
      int refutY = Math.max(box1y, box2y);
      labelPoint = new Point((point1.x + point2.x)/2 - width/2, 
        (int)(refutY + this.refutationOffset + 2));
    } else {
      if (argument.isInvertedTree()) {
        labelPoint = new Point((point1.x + point2.x)/2 - width, 
          (int)(point1.y + point2.y + edgeSource.totalLayoutSize.getHeight())/2);
      } else {
        labelPoint = new Point((point1.x + point2.x)/2 - width, 
          (int)(point1.y + point2.y + vertex.totalLayoutSize.getHeight())/2);
      }  
    }
    int canvasWidth = this.getWidth();
    if (labelPoint.x - 3 < 0)
      supportLabelTextX = 3;
    else
      supportLabelTextX = labelPoint.x;
    if (labelPoint.y - 12 < 0)
      supportLabelTextY = 12;
    else
      supportLabelTextY = labelPoint.y;
  }

  protected void drawSupportLabelText(Graphics2D gg)
  {
    int y;
    for (int i = 0; i < supportLabelText.size(); i++) {
      AttributedString attrString = (AttributedString)supportLabelText.elementAt(i);
      AttributedCharacterIterator iter = attrString.getIterator();
      FontMetrics fontMetrics = gg.getFontMetrics();
      y = supportLabelTextY + i * (fontMetrics.getAscent());
      gg.drawString(iter, supportLabelTextX, y);
    }
  }

  public boolean initializeDrawing()
  {
    Tree tree = argument.getTree();
    Vector roots = tree.getRoots();
    if (roots.size() == 0) return false;
    
    if (argument.isMultiRoots()) {
      argument.deleteDummyRoot();
      argument.setMultiRoots(false);
    }
    TreeVertex root = null;
    if (roots.size() > 1) {
      if (argument.getDummyRoot() == null) {
        argument.addDummyRoot(roots);
      }
      root = argument.getDummyRoot();
    } else if (roots.size() == 1) {
      root = (TreeVertex)roots.firstElement();
    }
    
//    TreeVertex root = (TreeVertex)roots.elementAt(0);
    Vector traversal = tree.breadthFirstTopSort(root);
    clearLeafCounts(tree);
    doLeafCounts(root);
    addSubtreeLabels();
    calcTextLayouts();
    layerHeights = new Vector();
    calcLayerSizes(root, 0);
    totalLeaves = getTotalLeaves(root);
    canvasWidth = totalLeaves * (textWidth + horizTextSpace);
    canvasHeight = topOffset;
    for (int i = 0; i<layerHeights.size(); i++) {
      canvasHeight += (int)((Double)layerHeights.elementAt(i)).doubleValue() + edgeHeight;
    }
    if (argument.isInvertedTree()) {
      calcNodeCoords(root, canvasWidth, 0, 
        (int)((Double)layerHeights.elementAt(0)).doubleValue(), 0, argument.isInvertedTree());
    } else {
      calcNodeCoords(root, canvasWidth, 0, 0, 0, argument.isInvertedTree());
    }
    calcSubtreeShapes();
    Dimension panelDim = new Dimension(canvasWidth, canvasHeight);
    setPreferredSize(panelDim);
    getDisplayFrame().getMainScrollPane().setViewportView(this);
    return true;
  }
  
  int cqTrafficLightSize = 10;
  Color cqGreen = Color.green;
  Color cqAmber = Color.yellow;
  Color cqRed = Color.red;
  public void drawTree(Graphics2D gg)
  {
    gg.setPaint(getDiagramBackground());
    gg.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
    if (argument == null || argument.getTree() == null ||
      argument.getTree().breadthFirstTraversal() == null) return;
    drawSubtrees(gg);
    Enumeration nodeList = argument.getTree().breadthFirstTraversal().elements();
    // Run through the traversal and draw each vertex
    // The draw point has been determined previously
    while (nodeList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      if (vertex.isVirtual() || !vertex.visible ||
                (argument.isMultiRoots() && vertex.getLayer() == 0))
      {
        continue;
      }
      
      // Draw the box around the node's text
      Point corner = vertex.getDrawPointFullText();
      Dimension totalLayoutSize = vertex.totalLayoutSize;
      Paint textColor;
      if (vertex.isMissing()) {
        gg.setStroke(dashStroke);
        if (vertex.isRefutation()) {
          textColor = new GradientPaint(0, 0, DiagramBase.refutationColor, 
            DiagramBase.NODE_DIAM/4, DiagramBase.NODE_DIAM/4, DiagramBase.missingColor, true);
        } else {
          textColor = DiagramBase.missingColor;
        }
      } else if (vertex.isRefutation()) {
        textColor = DiagramBase.refutationColor;
      } else {
        textColor = textBackground;
      }
      gg.setPaint(textColor); 
      Shape textBox = vertex.getShape(this);
      gg.fill(textBox);
      
      int y = corner.y;
      if (vertex.schemeLabels.size() > 0) {
        y = drawLayout(gg, vertex.schemeLayout, corner.x - textWidth/2, 
        y - this.textBorderMargin,
        Color.white, vertex.schemeLayoutSize);
      }
      y = drawLayout(gg, vertex.textLayout, corner.x - textWidth/2, y,
        textColor, vertex.textLayoutSize);
      if (argument.isShowSupportLabels()) {
        y = drawLayout(gg, vertex.nodeLabelLayout, 
          corner.x - textWidth/2, y, DiagramBase.nodeLabelColor,
          vertex.nodeLabelLayoutSize);
      }
      if (argument.isShowOwners()) {
        y = drawLayout(gg, vertex.ownersLayout, corner.x - textWidth/2, y,
          DiagramBase.ownersColor, vertex.ownersLayoutSize);
      }
      
      // Draw symbol if node is hiding its premises
      if (vertex.hidingChildren)
      {
        gg.setFont(labelFont2);
        gg.setPaint(Color.blue);
        Rectangle bounds = vertex.getShape(this).getBounds();
        gg.drawString("+", corner.x + bounds.width/2 - 10, 
            corner.y + bounds.height - 5);
      }
      gg.setPaint(Color.black);
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
      gg.setStroke(solidStroke);
      if (argument.isShowCQsAnswered())
      {
        int cqX = textBox.getBounds().x + textBox.getBounds().width;
        int cqY = textBox.getBounds().y;
        for (Subtree scheme : vertex.schemeList)
        {
          Shape cqBox = new Rectangle2D.Double(cqX, cqY, cqTrafficLightSize, cqTrafficLightSize);
          scheme.cqBoxTable.put(this, cqBox);
          gg.setPaint(scheme.getOutlineColor());
          gg.fill(cqBox);
          gg.setPaint(Color.black);
          gg.draw(cqBox);

          int numAnswered = scheme.getNumCqsAnswered();
          int numCQs = scheme.getCqChecks().size();
          if (numAnswered == numCQs)
          {
            gg.setPaint(cqGreen);
          } else if (numAnswered == 0)
          {
            gg.setPaint(cqRed);
          } else {
            gg.setPaint(cqAmber);
          }
          Shape cqCircle = new Ellipse2D.Double(cqX + 2, cqY + 2, cqTrafficLightSize - 4, cqTrafficLightSize - 4);
          gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
          gg.fill(cqCircle);
          gg.setPaint(Color.black);
          gg.draw(cqCircle);
          cqY += this.cqTrafficLightSize;
        }
      }
    }
    
    gg.setPaint(Color.black);
    nodeList = argument.getTree().breadthFirstTraversal().elements();
    // For each vertex...
    while (nodeList.hasMoreElements()) {
      // Get its edge list...
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      if (argument.isMultiRoots() && vertex.getLayer() == 0)
      {
        continue;
      }
      Enumeration edges = vertex.getEdgeList().elements();
      // For each edge in the list...
      while (edges.hasMoreElements()) {
        TreeEdge edge = (TreeEdge)edges.nextElement();
        if (!edge.visible)
        {
          continue;
        }
        if (edge.isSelected())
        {
          gg.setStroke(selectStroke);
        }
        if (edge.getShape(this) == null)
        System.out.println("source " + edge.getSourceVertex().getShortLabelString() + " to " +
                edge.getDestVertex().getShortLabelString());
        gg.draw(edge.getShape(this));
        TreeVertex destVertex = edge.getDestVertex();
        gg.setStroke(solidStroke);
        if (destVertex.getSupportLabel() != null && argument.isShowSupportLabels()) {
          int supportLabelWidth = 
            createSupportLabelText(destVertex.getSupportLabel(), DiagramBase.supportLabelColor);
          if (supportLabelWidth > 0) {
            setSupportLabelTextPos(supportLabelWidth, vertex, destVertex);
            drawSupportLabelText(gg);
          }
        }
      }
    }
  }
  
  public void showCQFrame(Subtree scheme)
  {
    CQFrame cqFrame = new CQFrame(araucaria);
    cqFrame.setScheme(scheme);
    cqFrame.setVisible(true);
    if (cqFrame.isOkPressed())
    {
      displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "editing critical questions"));
      araucaria.doUndo(false, false);
      araucaria.doRedo(false);
    }
  }

  public boolean testSubtrees(MouseEvent event)
  {
    displayText = false;
    double x = event.getX();
    double y = event.getY();
    String text = " ";
    Enumeration subtrees = argument.getSubtreeList().elements();
    if (argument.isShowCQsAnswered())
    {
      while (subtrees.hasMoreElements()) {
        Subtree subtree = (Subtree)subtrees.nextElement();
        if (subtree.getCqBoxShape(this) != null && subtree.getCqBoxShape(this).contains(x, y))
        {
          showCQFrame(subtree);
          leftMouseReleased(event);
          return true;
        }
      }
      subtrees = argument.getSubtreeList().elements();
    }
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

  public void leftMouseReleased(MouseEvent e)
  {
    super.leftMouseReleased(e);
    initializeDrawing();
    araucaria.updateDisplays(true, false);
  }
  
  public void paint(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D)g;
    drawTree(gg);
    getDisplayFrame().getMainScrollPane().getViewport().setBackground(getDiagramBackground());
  }
  
  public BufferedImage getJpegImage()
  {
    Dimension canvasSize = this.getSize();
    BufferedImage image;
    try {
      image = new BufferedImage((int)getPreferredSize().getWidth(), 
        (int)getPreferredSize().getHeight(), BufferedImage.TYPE_INT_RGB);
    } catch (IllegalArgumentException ex)
    {
      image = new BufferedImage(getDisplayFrame().getMainDiagramPanel().getWidth(), 
        getDisplayFrame().getMainDiagramPanel().getHeight(),
			BufferedImage.TYPE_INT_RGB);
    }
    Graphics2D gg = image.createGraphics();
    if (argument != null && argument.getBreadthFirstTraversal() != null) {
      gg.setPaint(Color.white);
      gg.fillRect(0, 0, canvasSize.width, canvasSize.height);
      drawTree(gg);
      return image;
    }
    return null;
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
//    vertexPopup.add(vertexTextMenu);
//    vertexTextMenu.addActionListener(this);
//    vertexPopup.add(editVertexIDMenu);
//    editVertexIDMenu.addActionListener(this);
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
//    missingPopup.add(missingTextMenu);
//    missingTextMenu.addActionListener(this);
    missingPopup.add(editMissingTextMenu);
    editMissingTextMenu.addActionListener(this);
//    missingPopup.add(editMissingIDMenu);
//    editMissingIDMenu.addActionListener(this);
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
//    edgePopup.add(edgeTextMenu);
//    edgeTextMenu.addActionListener(this);
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
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == vertexTextMenu || e.getSource() == missingTextMenu) {
      displayText = true;
      String postitString = prepareMessageLabel(mouseVertex.getLabel().toString(),
        DiagramBase.MAX_POSTIT_SIZE);
//      setTextPos(createText(postitString), mouseX, mouseY);
      repaint();
    } else if (e.getSource() == editMissingTextMenu) {
      editMissingText();
    } else if (e.getSource() == editVertexIDMenu || e.getSource() == editMissingIDMenu) {
      editVertexID(); 
    } else if (e.getSource() == ownerVertexMenu || e.getSource() == ownerMissingMenu) {
      araucaria.doModifyOwnership();
    } else if (e.getSource() == labelVertexMenu || e.getSource() == edgeLabelMenu ||
      e.getSource() == labelMissingMenu) {    
      araucaria.doModifyEvaluation();
    } else if (e.getSource() == linkMenu || e.getSource() == edgeLinkMenu ||
      e.getSource() == linkMissingMenu) { 
      araucaria.doLink();
    } else if (e.getSource() == unlinkMenu || e.getSource() == edgeUnlinkMenu ||
      e.getSource() == unlinkMissingMenu) {    
      araucaria.doUnlink();
    } else if (e.getSource() == edgeTextMenu) {    
//      displayEdgeText(mouseEdge);
    } else if (e.getSource() == editSchemeMenu) {
      argument.showSubtreeDialog(mouseSubtree);
      displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "editing scheme"));
    } else if (e.getSource() == setPremisesVisMenu) {
      showHidePremises();
    }
  }

}
