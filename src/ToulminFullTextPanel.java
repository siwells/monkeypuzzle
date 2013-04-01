/*
 * ToulminFullTextPanel.java
 *
 * Created on 26 May 2004, 10:32
 */

/**
 *
 * @author  growe
 */
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.text.*;
import java.awt.font.*;
import java.awt.geom.*;
import javax.swing.*;

public class ToulminFullTextPanel extends DiagramBase
{
  int minDCArrow = 50;  // Minimum arrow length for datum --> claim link
  int dcArrowOffset = 10; // Distance from top of text box to arrow between D & C
  int dcArrowHeadSpacing = 6; // Distance between successive arrowheads on the C box
  int dcArrowLineSpacing = 6; // Distance between vertical sections of D --> C arrows
  int dcArrowTop = 20;    // Min length of arrow top portion nearest the C box
  int minSupportArrow = 50; // Min arrow length for support nodes (warrants, etc)
  TreeVertex selectedWarrant = null, selectedData = null, selectedRebuttal = null;
  
  int backingArrowLineSpacing = 6; // Distance between horizontal sections of B --> W arrows
  int backingArrowHeadSpacing = 0;
  int backingArrowTop = 20;
  Color dataColor = Color.black;
  Color qualifierColor = new Color(200, 200, 0);
  Color warrantColor = new Color(0, 200, 0);
  Color rebuttalColor = new Color(255, 0, 0);
  Color backingColor = new Color(0, 0, 255);
  
  public static final int DATA_EDGE = 0, QUALIFIER_EDGE = 1, WARRANT_EDGE = 2, REBUTTAL_EDGE = 3;
  
  protected static final Hashtable plainMap = new Hashtable();
  static {
    plainMap.put(TextAttribute.FONT, new Font("Helvetica", Font.PLAIN, 11));
  }

  protected static final Hashtable boldMap = new Hashtable();
  static {
    boldMap.put(TextAttribute.FONT, new Font("Helvetica", Font.BOLD, 11));
  }

  /** Creates a new instance of ToulminFullTextPanel */
  public ToulminFullTextPanel()
  {
  }
  
  public java.awt.image.BufferedImage getJpegImage()
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
    // Only print role for non-datum items
    if (role != null)
    {
      if ( !role.equals("data"))
      {
        vertex.roleLayout = calcLayout(role,  boldMap);
        height = getLayoutHeight(vertex.roleLayout);
        layoutHeight += height;
        vertex.roleLayoutSize = new Dimension(textWidth, height);
      } else {
        vertex.roleLayout = null;
      }
    }
    
    // Layout for main text label
    String text = (String)vertex.getLabel();
    // If the vertex is not to be shown, use ? as its text
    if (vertex.roles.get("toulmin").equals("warrant") &&
            (vertex.toulminBackingEdges.size() > 0 ||
            vertex.toulminDataEdges.size() > 0))
    {
      vertex.isHiddenTable.put("toulmin", "false");
    }
    if (vertex.isHiddenTable.get("toulmin").equals("true"))
    {
      text = "?";
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
  
  protected void calcTextLayoutsForEdges(Vector edgeList)
  {
    for (int i = 0; i < edgeList.size(); i++)
    {
      TreeEdge edge = (TreeEdge)edgeList.elementAt(i);
      TreeVertex vertex = edge.getDestVertex();
      calcTextLayouts(vertex);
    }
  }
  
  protected void calcTextLayouts(TreeVertex root)
  {
    calcTextLayout(root);
    calcTextLayoutsForEdges(root.toulminDataEdges);
    calcTextLayoutsForEdges(root.toulminWarrantEdges);
    calcTextLayoutsForEdges(root.toulminBackingEdges);
    calcTextLayoutsForEdges(root.toulminRebuttalEdges);
    calcTextLayoutsForEdges(root.toulminQualifierEdges);
  }
  
  /**
   * Calculate the size of the canvas by recursively applying the algorithm:
   * Width:
   *  Top level C block:
   *    width of C node + max[width of D blocks] + max[arrows]
   *
   *  D block widths are calculated by recursively applying the algorithm to each one.
   *
   *  Arrow width:
   *    Sum[WB blocks] + Sum[Q blocks] + Sum[R blocks]
   *
   *  WB block width:
   *    max[W block, B block]
   *
   *  Widths of W, B, Q and R blocks all calculated using same algorithm.
   *
   */
  public void calcBlockWidth(TreeVertex claim)
  {
    // If claim is the dummy node in a split tree, calculate the widths
    // of each of its children separately
    if (claim.isDummy())
    {
      Vector rootEdges = claim.getEdgeList();
      for (int i = 0; i < rootEdges.size(); i++)
      {
        TreeVertex root = ((TreeEdge)rootEdges.elementAt(i)).getDestVertex();
        calcBlockWidth(root);
      }
      return;
    }
    // Calculate widths of all data blocks and arrows attached to claim
    int maxWidth = 0;
    claim.arrowLength = 0;
    for (int i = 0; i < claim.toulminDataEdges.size(); i++)
    {
      TreeEdge dataEdge = (TreeEdge)claim.toulminDataEdges.elementAt(i);
      TreeVertex dataBlock = dataEdge.getDestVertex();
      calcBlockWidth(dataBlock);
      if (dataBlock.toulminWidth > maxWidth)
      {
        maxWidth = dataBlock.toulminWidth;
      }
    }
    claim.toulminWidth = textWidth + maxWidth + 2 * textBorderMargin;
    
    // Work out widths of backings
    // Only warrants are allowed to have backings, which are arranged in a 
    // horizontal row beneath the warrant, left-aligned with the warrant
    int backingWidth = 0;
    for (int i = 0; i < claim.toulminBackingEdges.size(); i++)
    {
      TreeEdge backingEdge = (TreeEdge)claim.toulminBackingEdges.elementAt(i);
      TreeVertex backing = (TreeVertex)backingEdge.getDestVertex();
      calcBlockWidth(backing);
      backingWidth += backing.toulminWidth + horizTextSpace;
    }
    // If the total width of all backings is > warrant's layout width,
    // replace layout width with backing width
    if (backingWidth > textWidth + 2 * textBorderMargin)
    {
      claim.toulminWidth += backingWidth - textWidth - 2 * textBorderMargin;
    }
    
    // Arrow width - calculated for arrow extending to the right from claim
    //
    // If claim has no parent, there's nothing more to add
    if (claim.getParent() == null && !claim.truth) 
    {
      return;
    }
    
    // Work out sizes of warrants
    int warrantWidth = 0;
    for (int i = 0; i < claim.toulminWarrantEdges.size(); i++)
    {
      TreeEdge warrantEdge = (TreeEdge)claim.toulminWarrantEdges.elementAt(i);
      TreeVertex warrant = (TreeVertex)warrantEdge.getDestVertex();
      // If warrant is hidden (i.e. it is an added negation and these are hidden) 
      // and has no support, skip it.
      if (warrant.isHiddenTable.get("toulmin").equals("true") && warrant.toulminBackingEdges.size() == 0 &&
        warrant.toulminDataEdges.size() == 0)
      {
        continue;
      }
      calcBlockWidth(warrant);
      warrantWidth += warrant.toulminWidth + horizTextSpace;
    }
    
    // Work out sizes of qualifiers
    int qualifierWidth = 0;
    for (int i = 0; i < claim.toulminQualifierEdges.size(); i++)
    {
      TreeEdge qualifierEdge = (TreeEdge)claim.toulminQualifierEdges.elementAt(i);
      TreeVertex qualifier = (TreeVertex)qualifierEdge.getDestVertex();
      qualifier.roles.put("toulmin", "qualifier");
      calcBlockWidth(qualifier);
      qualifier.arrowLength = minDCArrow;
      qualifier.toulminWidth += qualifier.arrowLength;
      qualifierWidth += qualifier.toulminWidth + horizTextSpace;
    }
    
    // Work out sizes of rebuttals
    int rebuttalWidth = 0;
    for (int i = 0; i < claim.toulminRebuttalEdges.size(); i++)
    {
      TreeEdge rebuttalEdge = (TreeEdge)claim.toulminRebuttalEdges.elementAt(i);
      TreeVertex rebuttal = (TreeVertex)rebuttalEdge.getDestVertex();
      calcBlockWidth(rebuttal);
      rebuttalWidth += rebuttal.toulminWidth + horizTextSpace;
    }
    
    int horizArrow = warrantWidth + qualifierWidth + rebuttalWidth > minDCArrow ? 
      warrantWidth + qualifierWidth + rebuttalWidth : minDCArrow;
    claim.arrowLength = horizArrow + claim.getParent().toulminDataEdges.size() * dcArrowLineSpacing;
    claim.toulminWidth += claim.arrowLength;
  }
  
  /**
   * Height:
   *  max[Sum[D branches], C node]
   *
   *  C node height is the height of the text box - depends on enclosed text
   * and on the number of data nodes connecting to it, since we must have
   * room to draw a separate arrowhead for each D --> C link
   *
   *  Height of D branch:
   *    D block + Max[WBs, Qs, Rs]
   */
  public void calcBlockHeight(TreeVertex claim)
  {
    // If claim is the dummy node in a split tree, calculate the heights
    // of each of its children separately
    if (claim.isDummy())
    {
      Vector rootEdges = claim.getEdgeList();
      for (int i = 0; i < rootEdges.size(); i++)
      {
        TreeVertex root = ((TreeEdge)rootEdges.elementAt(i)).getDestVertex();
        calcBlockHeight(root);
      }
      return;
    }
    int totalHeight = 0;
    int separatorHeight = 0;

    for (int i = 0; i < claim.toulminDataEdges.size(); i++)
    {
      TreeEdge dataEdge = (TreeEdge)claim.toulminDataEdges.elementAt(i);
      TreeVertex dataBlock = dataEdge.getDestVertex();
      calcBlockHeight(dataBlock);
      if (i < claim.toulminDataEdges.size() - 1) {
        separatorHeight += vertTextSpace;
      }
      totalHeight += dataBlock.toulminHeight;
    }
    
    // Support heights
    int maxSupportHeight = calcSupportHeight(claim, claim.toulminWarrantEdges, totalHeight, 0);
    maxSupportHeight = calcSupportHeight(claim, claim.toulminQualifierEdges, totalHeight, maxSupportHeight);
    maxSupportHeight = calcSupportHeight(claim, claim.toulminRebuttalEdges, totalHeight, maxSupportHeight);
    maxSupportHeight = calcSupportHeight(claim, claim.toulminBackingEdges, totalHeight, maxSupportHeight);
    claim.toulminHeight += separatorHeight;
  }
  
  public int calcSupportHeight(TreeVertex claim, Vector edgeList, int totalHeight, int maxSupportHeight)
  {
    boolean isBacking = false;
    for (int i = 0; i < edgeList.size(); i++)
    {
      TreeEdge supportEdge = (TreeEdge)edgeList.elementAt(i);
      TreeVertex support = (TreeVertex)supportEdge.getDestVertex();
      calcBlockHeight(support);
      if (support.roles.get("toulmin").equals("backing"))
      {
        isBacking = true;
        support.toulminHeight += edgeList.size() * backingArrowLineSpacing + minSupportArrow + backingArrowTop;
      }
      maxSupportHeight = support.toulminHeight > maxSupportHeight ?
        support.toulminHeight : maxSupportHeight;
    }
    
    int claimHeight = claim.totalLayoutSize.height + 2 * textBorderMargin;
    claim.toulminHeight = totalHeight > claimHeight ?
      totalHeight : claimHeight;
    int totalSupportHeight = 0;
    
    // Backing nodes have height calculated differently since they hang off
    // the bottom of a warrant rather than off an arrow coming out of the side
    if (isBacking)
    {
      totalSupportHeight = maxSupportHeight + minSupportArrow + claimHeight;
    } else {
      totalSupportHeight = maxSupportHeight + minSupportArrow + dcArrowOffset;
    }
    if (totalSupportHeight > claim.toulminHeight)
    {
      claim.toulminHeight = totalSupportHeight;
    }
    return maxSupportHeight;
  }
  
  /**
   * Places claim's UL corner at xStart, yStart and recursively
   * places its components within the block.
   *
   * Data blocks are right-justified
   * from top to bottom, and arrows from each data block are placed
   * to the right of the corresponding data block.
   *
   * The layer of the recursion is used to determine whether to add an offset
   * to the block's position. Only data nodes that are direct descendents via 
   * data-only links to the top-level claim should be offset, so layer is only
   * incremented whenever a non-data node is considered.
   */
  public void calcBlockCoords(TreeVertex root, int xStart, int yStart, int layer)
  {
    // If root is the dummy node in a split tree, calculate the coords
    // of each subRoot (real root in a subtree) in order, taking account
    // of the width of each one
    if (root.isDummy())
    {
      Vector rootEdges = root.getEdgeList();
      int subRootX = 0, subRootY = 0;
      for (int i = 0; i < rootEdges.size(); i++)
      {
        TreeVertex subRoot = ((TreeEdge)rootEdges.elementAt(i)).getDestVertex();
        calcBlockCoords(subRoot, subRootX + leftOffset, subRootY + leftOffset, layer + 1);
        subRootX += subRoot.toulminWidth;
      }
      return;
    }

    // In order to left-justify backings relative to a warrant, the root node
    // must be positioned at the left edge of all backings attached to it.
    int backingWidth = 0;
    if (!root.hidingChildren)
    {
      for (int i = 0; i < root.toulminBackingEdges.size(); i++)
      {
        TreeEdge backingEdge = (TreeEdge)root.toulminBackingEdges.elementAt(i);
        TreeVertex backingBlock = backingEdge.getDestVertex();
        backingWidth += backingBlock.toulminWidth;
      }
      // Subtract off the width of a single text box since the first backing node
      // is always directly under the warrant
      if (root.toulminBackingEdges.size() > 0)
      {
        backingWidth -= root.totalLayoutSize.width + 2 * textBorderMargin;
      }

      int yData = yStart;
      int xSpace = root.toulminWidth - root.arrowLength - root.totalLayoutSize.width
        - 2 * textBorderMargin - backingWidth;
      for (int i = 0; i < root.toulminDataEdges.size(); i++)
      {
        TreeEdge dataEdge = (TreeEdge)root.toulminDataEdges.elementAt(i);
        TreeVertex dataBlock = dataEdge.getDestVertex();
        calcBlockCoords(dataBlock, xStart + xSpace - dataBlock.toulminWidth, yData, layer);
        yData += dataBlock.toulminHeight;
        if (i < root.toulminDataEdges.size() - 1)
        {
          yData += vertTextSpace;
        }
      }
    }
    
    root.toulminX =  xStart + root.toulminWidth - root.arrowLength - root.totalLayoutSize.width
      - 2 * textBorderMargin - backingWidth;
    root.toulminY =  yStart;
    if (layer == 0)
    {
      root.toulminX += leftOffset;
      root.toulminY += topOffset;
    }

      // Calculate locations of support nodes (warrants, etc)
      // If a warrant is an added negation, we want to display its rebuttal
      // immediately to its right. We use the visited flag of rebuttal nodes to
      // say whether it has been displayed in this fashion so it doesn't get displayed
      // a second time later
      for (int i = 0; i < root.toulminRebuttalEdges.size(); i++)
      {
        TreeEdge rebuttalEdge = (TreeEdge)root.toulminRebuttalEdges.elementAt(i);
        TreeVertex rebuttal = (TreeVertex)rebuttalEdge.getDestVertex();
        rebuttal.setVisited(false);
      }
      int supportX = root.toulminX + root.totalLayoutSize.width + 
        2 * textBorderMargin + horizTextSpace;
      int supportY = root.toulminY + dcArrowOffset + minSupportArrow;
      for (int i = 0; i < root.toulminWarrantEdges.size(); i++)
      {
        TreeEdge warrantEdge = (TreeEdge)root.toulminWarrantEdges.elementAt(i);
        TreeVertex warrant = (TreeVertex)warrantEdge.getDestVertex();
        // If the warrant isn't being displayed, skip it
        if (warrant.isHiddenTable.get("toulmin").equals("true") && warrant.toulminBackingEdges.size() == 0 &&
          warrant.toulminDataEdges.size() == 0)
        {
          warrant.toulminX = warrant.toulminY = 0;
          warrant.setShape(new Rectangle2D.Double(), this);
          continue;
        }

        calcBlockCoords(warrant,  supportX,  supportY, layer + 1);
        calcSupportEdge(warrantEdge);
        supportX += warrant.toulminWidth + horizTextSpace;
        
        String negation = (String)warrant.roles.get("addedNegation");
        if (negation != null && negation.equals("yes"))
        {
          for (int j = 0; j < root.toulminRebuttalEdges.size(); j++)
          {
            TreeEdge rebuttalEdge = (TreeEdge)root.toulminRebuttalEdges.elementAt(j);
            TreeVertex rebuttal = (TreeVertex)rebuttalEdge.getDestVertex();
            if (rebuttal.getParent() == warrant)
            {
              calcBlockCoords(rebuttal,  supportX,  supportY, layer + 1);
              calcSupportEdge(rebuttalEdge);
              supportX += rebuttal.toulminWidth + horizTextSpace;
              rebuttal.setVisited(true);
            }
          }
        }
      }

      for (int i = 0; i < root.toulminQualifierEdges.size(); i++)
      {
        TreeEdge qualifierEdge = (TreeEdge)root.toulminQualifierEdges.elementAt(i);
        TreeVertex qualifier = (TreeVertex)qualifierEdge.getDestVertex();
        // Qualifiers attached to warrants and rebuttals should be displayed at the same level as the warrant
        if (root.roles.get("toulmin").equals("warrant")) 
        {
          calcBlockCoords(qualifier,  supportX,  root.toulminY, layer + 1);
        } else if (root.roles.get("toulmin").equals("rebuttal")) {
          calcBlockCoords(qualifier,  supportX,  root.toulminY, layer + 1);
        } else {
          calcBlockCoords(qualifier,  supportX,  supportY, layer + 1);
        }
        calcSupportEdge(qualifierEdge);
        supportX += qualifier.toulminWidth + horizTextSpace;
      }

      for (int i = 0; i < root.toulminRebuttalEdges.size(); i++)
      {
        TreeEdge rebuttalEdge = (TreeEdge)root.toulminRebuttalEdges.elementAt(i);
        TreeVertex rebuttal = (TreeVertex)rebuttalEdge.getDestVertex();
        if (rebuttal.getVisited()) continue;
        calcBlockCoords(rebuttal,  supportX,  supportY, layer + 1);
        calcSupportEdge(rebuttalEdge);
        supportX += rebuttal.toulminWidth + horizTextSpace;
      }

      // Backing for a warrant
      // The backing blocks are left-aligned with the warrant and spaced
      // a distance of minSupportArrow + (# of backings * backingArrowLineSpacing)
      // below the bottom edge of the warrant box
      supportX = root.toulminX;
      supportY = root.toulminY + root.totalLayoutSize.height + 2 * textBorderMargin +
        minSupportArrow + root.toulminBackingEdges.size() * backingArrowLineSpacing +
        backingArrowTop;
      for (int i = 0; i < root.toulminBackingEdges.size(); i++)
      {
        TreeEdge backingEdge = (TreeEdge)root.toulminBackingEdges.elementAt(i);
        TreeVertex backing = backingEdge.getDestVertex();
        calcBlockCoords(backing, supportX, supportY, layer + 1);

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
        supportX += backing.toulminWidth + horizTextSpace;
      }

      int rootMidpoint = root.toulminY + root.totalLayoutSize.height / 2;

    //if (!root.hidingChildren)
    //{
      for (int dataNum = 0; dataNum < root.toulminDataEdges.size(); dataNum++)
      {
        TreeEdge dataEdge = (TreeEdge)root.toulminDataEdges.elementAt(dataNum);
        TreeVertex dataBlock = dataEdge.getDestVertex();
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
   // }
    Point corner = new Point(root.toulminX, root.toulminY);
    Dimension totalLayoutSize = root.totalLayoutSize; 
    assignVertexShape(root, corner.x, corner.y, 
        (int)totalLayoutSize.getWidth() + 2 * textBorderMargin,
        (int)totalLayoutSize.getHeight() + 2 * textBorderMargin);
  }
  
  /**
   * Calculates the edge shape connecting a data block to its parent claim
   * An edge is an arrow with an initial horizontal section to which all side
   * branches (W, Q and R) are attached, followed by a vertical section if the
   * join point on the claim is not at the same level as the initial horizontal
   * section, followed by a final horizontal section terminating in an arrowhead
   * on the claim box.
   *
   * In order to separate multiple data nodes impinging on the same claim, 
   * arrowheads are separated vertically on the claim by a distance dcArrowHeadSpacing.
   * Similarly, the vertical sections are separated horizontally by dcArrowLineSpacing.
   * Data nodes nearer the centre need to have vertical sections that are 'inside' those
   * that are nearer to the edges.
   */
  public void calcDataBlockEdge(TreeEdge dataEdge, int arrowHeadOffset, int arrowLineOffset)
  {
    TreeVertex dataBlock = dataEdge.getDestVertex();
    TreeVertex root = dataEdge.getSourceVertex();
    GeneralPath blockEdge = new GeneralPath();
    
    // Initial horizontal section
    blockEdge.append(new Line2D.Double(
      dataBlock.toulminX + dataBlock.totalLayoutSize.width + 2 * textBorderMargin, 
      dataBlock.toulminY + dcArrowOffset,  
      root.toulminX - arrowLineOffset, dataBlock.toulminY + dcArrowOffset), false);
    
    blockEdge.append(new Line2D.Double(
      root.toulminX - arrowLineOffset, dataBlock.toulminY + dcArrowOffset, 
      root.toulminX - arrowLineOffset, root.toulminY + arrowHeadOffset), false);

    blockEdge.append(new Line2D.Double(
      root.toulminX - arrowLineOffset, root.toulminY + arrowHeadOffset,
      root.toulminX, root.toulminY + arrowHeadOffset), false);
    
    blockEdge.append(DiagramBase.addArrowHead(
      root.toulminX, root.toulminY + arrowHeadOffset, 
      root.toulminX - arrowLineOffset, root.toulminY + arrowHeadOffset, 
      0, 3, 7), false);
    dataEdge.setShape(blockEdge, this); 
  }
  
  public void calcBackingEdge(TreeEdge backingEdge, int arrowHeadOffset, int arrowLineOffset)
  {
    TreeVertex backingBlock = backingEdge.getDestVertex();
    TreeVertex root = backingEdge.getSourceVertex();
    GeneralPath blockEdge = new GeneralPath();
    int vertDistance = minSupportArrow + arrowLineOffset;
    if (minSupportArrow + arrowLineOffset > 
        backingBlock.toulminY - (root.toulminY + root.totalLayoutSize.height + 2 * textBorderMargin))
    {
      vertDistance = backingBlock.toulminY - (root.toulminY + root.totalLayoutSize.height + 2 * textBorderMargin);
    }
    // Initial vertical section - comes out of midpoint of top edge of backingBlock
    blockEdge.append(new Line2D.Double(
      backingBlock.toulminX + backingBlock.totalLayoutSize.width / 2, 
      backingBlock.toulminY, 
      backingBlock.toulminX + backingBlock.totalLayoutSize.width / 2, 
//      backingBlock.toulminY - vertDistance - arrowLineOffset), false);
      backingBlock.toulminY - vertDistance), false);
    
    // Horizontal section
    blockEdge.append(new Line2D.Double(
      backingBlock.toulminX + backingBlock.totalLayoutSize.width / 2, 
//      backingBlock.toulminY - minSupportArrow - arrowLineOffset, 
      backingBlock.toulminY - vertDistance, 
      root.toulminX + arrowHeadOffset, 
//      backingBlock.toulminY - vertDistance - arrowLineOffset), false);
      backingBlock.toulminY - vertDistance), false);
    
    // Final vertical section leading up to arrowhead
    blockEdge.append(new Line2D.Double(
      root.toulminX + arrowHeadOffset, 
//      backingBlock.toulminY - vertDistance - arrowLineOffset, 
      backingBlock.toulminY - vertDistance, 
      root.toulminX + arrowHeadOffset,
      root.toulminY + root.totalLayoutSize.height + 2 * textBorderMargin), false);

    blockEdge.append(DiagramBase.addArrowHead(
      root.toulminX + arrowHeadOffset,
      root.toulminY + root.totalLayoutSize.height + 2 * textBorderMargin, 
      root.toulminX + arrowHeadOffset, 
//      backingBlock.toulminY - vertDistance - arrowLineOffset, 
      backingBlock.toulminY - vertDistance, 
      0, 3, 7), false);

    backingEdge.setShape(blockEdge, this);
  }
  
  /**
   * Calculates the edge connecting a warrant, rebuttal or qualifier 
   * to the horizontal data-claim arrow above it.
   */
  public void calcSupportEdge(TreeEdge supportEdge)
  {
    TreeVertex supportBlock = supportEdge.getDestVertex();
    TreeVertex dataBlock = supportEdge.getSourceVertex();
    if (!dataBlock.roles.get("toulmin").equals("data"))
    {
      // Edge from rebuttal to a non-data node must be to a qualifier
      if (dataBlock.roles.get("toulmin").equals("rebuttal"))
      {
        // Since the parent of a rebuttal can be a hidden added negation,
        // we find the data node in the LA from the rebuttal's parent
        dataBlock = dataBlock.getParent().findDataNodeInLA();
      } else {
        dataBlock = dataBlock.getParent();
      }
      if (dataBlock.isVirtual())
      {
        dataBlock = dataBlock.getParent();
      } 
    }
    GeneralPath blockEdge = new GeneralPath();
    
    int edgeX = supportBlock.toulminX + 
      supportBlock.totalLayoutSize.width / 2 + textBorderMargin;
    blockEdge.append(new Line2D.Double(edgeX, 
      supportBlock.toulminY, edgeX, 
      dataBlock.toulminY + dcArrowOffset), false);
    blockEdge.append(DiagramBase.addArrowHead(
      edgeX, 
      dataBlock.toulminY + dcArrowOffset, edgeX, 
      supportBlock.toulminY, 0, 3, 7), false);
    supportEdge.setShape(blockEdge, this);
  }
  
  public boolean initializeDrawing(boolean resetSize)
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
    
    calcTextLayouts();
    calcBlockWidth(root);
    calcBlockHeight(root);
    calcBlockCoords(root, 0, 0, 0);
    
    if (root.isDummy())
    {
      canvasWidth = 0;
      canvasHeight = 0;
      for (int i = 0; i < root.getEdgeList().size(); i++)
      {
        TreeVertex subRoot = ((TreeEdge)root.getEdgeList().elementAt(i)).getDestVertex();
        canvasWidth += subRoot.toulminWidth;
        if (subRoot.toulminHeight > canvasHeight)
          canvasHeight = subRoot.toulminHeight;
      }
    } else {
      canvasWidth = root.toulminWidth + 2 * leftOffset;
      canvasHeight = root.toulminHeight + 2 * topOffset;
    }
    if (resetSize)
    {
      Dimension panelDim = new Dimension(canvasWidth, canvasHeight);
      setPreferredSize(panelDim);
      getDisplayFrame().getMainScrollPane().setViewportView(this);
    }
    return true;
  }
  
  public void redrawTree(boolean doRepaint)
  {
    initializeDrawing(true);
    repaint();
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
      drawEdges(vertex.toulminWarrantEdges, gg, warrantColor);
      drawEdges(vertex.toulminQualifierEdges, gg, qualifierColor);
      drawEdges(vertex.toulminRebuttalEdges, gg, rebuttalColor);
      return;
    }
    // Draw vertexes and edges arising from current vertex
    drawEdges(vertex.toulminDataEdges, gg, dataColor);
    drawEdges(vertex.toulminWarrantEdges, gg, warrantColor);
    drawEdges(vertex.toulminQualifierEdges, gg, qualifierColor);
    drawEdges(vertex.toulminRebuttalEdges, gg, rebuttalColor);
    drawEdges(vertex.toulminBackingEdges, gg, backingColor);
  }
  
  public void drawText(Graphics2D gg, TreeVertex vertex, Paint textColor, Point corner)
  {
    int y = corner.y;
    y = drawLayout(gg, vertex.roleLayout, 
      corner.x + textBorderMargin, y, DiagramBase.roleColor,
      vertex.roleLayoutSize);
    drawLayout(gg, vertex.textLayout, corner.x + textBorderMargin, y,
      textColor, vertex.textLayoutSize);
  }
  
  Color textBackground = Color.white;
  public void drawTree(Graphics2D gg)
  {
    Vector roots = argument.getTree().getRoots();
    if (roots.size() == 0) return;
    TreeVertex root = (TreeVertex)roots.firstElement();
    if (!argument.isMultiRoots()) 
    {
      drawVertex(gg, root, Color.black);
    } else {
      for (int i = 0; i < roots.size(); i++)
      {
        TreeVertex vertex = (TreeVertex)roots.elementAt(i);
        if (!vertex.isDummy())
        {
          drawVertex(gg, vertex, Color.black);
        }
      }
    }
  }
  
  /**
   * Draws all destination vertexes arising out of dataEdges,
   * then draws all the edges in dataEdges.
   */
  protected void drawEdges(Vector dataEdges, Graphics2D gg, Color color)
  {
    Paint oldPaint = gg.getPaint();
    gg.setPaint(color);
    for (int i = 0; i < dataEdges.size(); i++)
    {
      TreeEdge edge = (TreeEdge)dataEdges.elementAt(i);
      TreeVertex vertex = edge.getDestVertex();
      if (vertex.roles.get("toulmin").equals("warrant") && 
        vertex.isHiddenTable.get("toulmin").equals("true") && 
        vertex.toulminBackingEdges.size() == 0 &&
        vertex.toulminDataEdges.size() == 0)
      {
        continue;
      }
      drawVertex(gg, vertex, color);
      gg.setPaint(color);
      Shape edgeShape = edge.getShape(this);
      if (edgeShape != null)
      {
        Stroke oldStroke = gg.getStroke();
        gg.setStroke(edge.isSelected() ? selectStroke : solidStroke);
        gg.draw(edgeShape);
        gg.setStroke(oldStroke);
      }
    }
    gg.setPaint(oldPaint);
  }

  public boolean setVertexProperties(TreeVertex startVertex, TreeVertex endVertex)
  {
    if (endVertex.roles.get("toulmin").equals("warrant"))
    {
      startVertex.roles.put("toulmin", "backing");
      return true;
    }
    // Handle case of dragging a new vertex onto a data->claim edge to
    // create a warrant
    else if (endVertex.roles.get("toulmin").equals("data"))
    { 
      if (pendingStartRole.equals("warrant"))
      {
        argument.clearAllSelections();
        TreeVertex claimNode = endVertex.getParent();
        if (claimNode.isVirtual())
        {
          claimNode = claimNode.getParent();
        }
        TreeEdge selectedEdge = claimNode.getEdge(claimNode.toulminDataEdges, endVertex);
        selectedEdge.setSelected(true);
        startVertex.setSelected(true);
        createWarrant(true);
        return false;
      }
    }
    return true;
  }
  
  public void leftMouseReleased(MouseEvent e)
  {
    super.leftMouseReleased(e);
    initializeDrawing(true);
    araucaria.updateDisplays(true, false);
  } 
  
  public void paint(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D)g;
    gg.setPaint(getDiagramBackground());
    gg.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
    drawTree(gg);
    getDisplayFrame().getMainScrollPane().getViewport().setBackground(getDiagramBackground());
  }
  
  /**
   * Creates a line of width EDGE_SELECT_WIDTH for each edge
   * and tests if mouse click was in that Shape's boundary.
   * For Toulmin diagrams, an edge is associated with a data node
   * rather than being a separate entity connecting two TreeVertexes.
   * 
   * Returns the edge Shape if one was selected, null otherwise.
   */
  public TreeEdge testEdgeShapes(MouseEvent event)
  {
    if (argument.getTree() == null) return null;
    double x = event.getX(); 
    double y = event.getY();
    if (argument.getBreadthFirstTraversal() == null) return null;
    Enumeration nodeList = argument.getBreadthFirstTraversal().elements();
    TreeEdge edge = null;
    while (nodeList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      if (argument.isMultiRoots() && vertex.getLayer() == 0)
      	continue;
      // Test edges from this vertex
      edge = testEdgeList(x, y, vertex.toulminDataEdges);
      if (edge == null)
        edge = testEdgeList(x, y, vertex.toulminWarrantEdges);
      if (edge == null)
        edge = testEdgeList(x, y, vertex.toulminRebuttalEdges);
      if (edge == null)
        edge = testEdgeList(x, y, vertex.toulminBackingEdges);
      if (edge != null) 
      {
        translateEdgeToStandard(edge);
        return edge;
      }
    }
    return null;
  }
  
  /**
   * Tests edges in list dataEdges to see if one of them is selected
   * by a mouse click at (x,y). Generic method that can be applied to
   * any list of Toulmin edges.
   */
  public TreeEdge testEdgeList(double x, double y, Vector dataEdges)
  {
    BasicStroke edgeWidth = new BasicStroke(EDGE_SELECT_WIDTH);
    for (int i = 0; i < dataEdges.size(); i++)
    {
      TreeEdge edge = (TreeEdge)dataEdges.elementAt(i);
      Shape edgeShape = edge.getShape(this);
      if (edgeShape == null) continue;
      Shape wideEdge = edgeWidth.createStrokedShape(edgeShape);
      if (wideEdge.contains(x, y)) {
        edge.setSelected(!edge.isSelected());
        String labelText = "\"" + edge.getDestVertex().getLabel().toString() + "\"";
        String supports = "\"" + edge.getSourceVertex().getLabel().toString() + "\"";
        labelText = prepareMessageLabel(labelText, supports, MAX_MESSAGELABEL_SIZE); 
        araucaria.setMessageLabelText(labelText);
        return edge;
      }
    }
    return null;
  }
  
  /**
   * Tests data edge lists to see if any of them contain (x,y). If so, it is
   * assumed that this method has been called in response to a left mouse released
   * event and that a node is being dragged onto an edge. The startVertex's role
   * should then be set to 'warrant'.
   */
  public TreeVertex getVertexFromEdgeContaining(double x, double y)
  {
    if (argument.getBreadthFirstTraversal() == null) return null;
    Enumeration nodeList = argument.getBreadthFirstTraversal().elements();
    while (nodeList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      TreeEdge selectedEdge = testEdgeList(x, y,  vertex.toulminDataEdges);
      if (selectedEdge != null)
      {
        pendingStartRole = "warrant";
        return selectedEdge.getDestVertex();
      }
    }
    return null;
  }
  
  /**
   * When a Toulmin edge is selected, finds the corresponding edge
   * in a standard diagram and selects it.
   */
  public void translateEdgeToStandard(TreeEdge selEdge)
  {
    TreeVertex source = selEdge.getSourceVertex();
    TreeVertex dest = selEdge.getDestVertex();
    TreeVertex parent = dest.getParent();
    TreeEdge edge;
    //if (parent.isVirtual())
    {
      edge = parent.getEdge(dest);
      edge.setSelected(selEdge.isSelected());
      // If the edge's parent is an implicit premise, it should be treated
      // the same way as the edge itself
      if (parent.roles.get("addedNegation") != null &&
              parent.roles.get("addedNegation").equals("yes"))
      {
        parent.setSelected(selEdge.isSelected());
      }
    }
  }
  
  public TreeEdge testEdgeShapesPopup(MouseEvent event)
  {
    double x = event.getX(); 
    double y = event.getY();
    Enumeration nodeList = argument.getTree().getVertexList().elements();
    TreeEdge selectedEdge = null;
    int selectedEdgeType = -1;
    while (nodeList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      if (argument.isMultiRoots() && vertex.getLayer() == 0)
      	continue;
      TreeEdge edge = testEdgeList(x, y, vertex.toulminDataEdges);
      if (edge != null)
      {
        selectedEdge = edge;
        selectedEdgeType = DATA_EDGE;
        break;
      }
      edge = testEdgeList(x, y, vertex.toulminWarrantEdges);
      if (edge != null)
      {
        selectedEdge = edge;
        selectedEdgeType = WARRANT_EDGE;
        break;
      }
      edge = testEdgeList(x, y, vertex.toulminQualifierEdges);
      if (edge != null)
      {
        selectedEdge = edge;
        selectedEdgeType = QUALIFIER_EDGE;
        break;
      }
      edge = testEdgeList(x, y, vertex.toulminRebuttalEdges);
      if (edge != null)
      {
        selectedEdge = edge;
        selectedEdgeType = REBUTTAL_EDGE;
        break;
      }
    }
    if (selectedEdge != null)
    {
      mouseX = (int)x;
      mouseY = (int)y;
      mouseEdge = selectedEdge;
      mouseEdge.setSelected(true);
      mouseVertex = null;
      translateEdgeToStandard(selectedEdge);
      createPopupMenu();
      switch (selectedEdgeType)
      {
        case DATA_EDGE:
            dataEdgePopup.show(event.getComponent(), event.getX(), event.getY());
            break;
        case WARRANT_EDGE:
          warrantEdgePopup.show(event.getComponent(), event.getX(), event.getY());
          break;
        case REBUTTAL_EDGE:
          rebuttalEdgePopup.show(event.getComponent(), event.getX(), event.getY());
          break;
        // TODO: Add other popups for different edge types
      }
      return selectedEdge;
    }
    // If we get here, no edge was selected  
    return null;
  }
  
  /**
   * If user has created a linked argument in a standard diagram, this is
   * translated into Toulmin by taking the first node in the LA as the Data,
   * and converting all the other nodes into warrants. This version differs
   * from createWarrant() since all the checks for correct choice of nodes &
   * edges have been done in doLink().
   * Called from Araucaria.doLink()
   */
  public void createWarrantFromLink(Vector selectedVerts, TreeEdge selectedEdge)
  {
    // Since selectedVerts contains the Data node as element 0, we start at 1
    for (int i = 1; i < selectedVerts.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)selectedVerts.elementAt(i);
      // The vertex's immediate parent is virtual, so the claim is the parent of the virtual node
      TreeVertex claim = vertex.getParent().getParent();
      TreeEdge removeEdge = claim.getEdge(claim.toulminDataEdges, vertex);
      if (removeEdge == null)
      {
        System.out.println("Error: removeEdge not found");
      }
      claim.toulminDataEdges.remove(removeEdge);
      selectedEdge.getDestVertex().toulminWarrantEdges.add(
        new TreeEdge(selectedEdge.getDestVertex(), vertex));
      vertex.roles.put("toulmin", "warrant");
    }
  }
  
  /**
   * Tests that one or more nodes and exactly one D-->C edge have been
   * selected and converts the nodes into warrants for that edge.
   * Must ensure that the D node on the selected edge is not itself selected.
   */
  public void createWarrant(boolean doLinkArg)
  {
    Vector vertexList = argument.getTree().getVertexList();
    Vector warrantList = new Vector();
    
    // Find the selected edge - ensure there is exactly one
    TreeEdge selectedEdge = null;
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)vertexList.elementAt(i);
      if (vertex.isSelected())
      {
        warrantList.add(vertex);
      }
      for (int j = 0; j < vertex.toulminDataEdges.size(); j++)
      {
        TreeEdge edge = (TreeEdge)vertex.toulminDataEdges.elementAt(j);
        if (edge.isSelected())
        {
          if (selectedEdge == null)
          {
            selectedEdge = edge;
          } else {
            araucaria.setMessageLabelText("ERROR: Select only one Data --> Claim link");
            return;
          }
        }
      }
    }
    if (selectedEdge == null)
    {
      araucaria.setMessageLabelText("ERROR: Select exactly one Data --> Claim link");
    }
    
    // Find the selected vertices and ensure that none of them is a data or claim
    // on the selected edge. 
    // Also, a data node that is to be converted to a warrant cannot have any
    // associated W, Q or R nodes.
    if (warrantList.size() == 0)
    {
      araucaria.setMessageLabelText("ERROR: Select one or more nodes to convert to warrants");
      return;
    }
    for (int i = 0; i < warrantList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)warrantList.elementAt(i);
      if (vertex == selectedEdge.getSourceVertex() ||
        vertex == selectedEdge.getDestVertex())
      {
        araucaria.setMessageLabelText("ERROR: Nodes cannot be data or claim of selected link");
        return;
      }
    }
    
    // If we get this far, all selected nodes are ok for conversion to warrants.
    //
    // To convert, we must delete the existing D --> C edge from the claim's data list
    // and add an edge to the data node's warrant list, where the data node is the
    // destination vertex of the selected edge.
    for (int i = 0; i < warrantList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)warrantList.elementAt(i);
      TreeEdge removeEdge = vertex.getParent().getEdge(vertex);
      if (removeEdge == null)
      {
        System.out.println("Error: removeEdge not found");
      }
      vertex.getParent().getEdgeList().remove(removeEdge);
      vertex.getParent().toulminDataEdges.remove(removeEdge);
      selectedEdge.getDestVertex().toulminWarrantEdges.add(
        new TreeEdge(selectedEdge.getDestVertex(), vertex));
      vertex.roles.put("toulmin", "warrant");
    }
    if (doLinkArg) 
    {
      buildLinkedArgument(selectedEdge);
    }
    displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "creating warrant"));
    redrawTree(true);
    araucaria.updateDisplays(true);
  }
  
    // The new warrants should become part of a linked argument with the data
    // node and any other warrants attached to that data node. To do this,
    // we check if the data node is current part of a LA. If so, we unlink it,
    // then relink it with the new list of warrants.
  public void buildLinkedArgument(TreeEdge selectedEdge)
  {
    TreeVertex dataParent = selectedEdge.getDestVertex().getParent();
    if (dataParent.isVirtual())
    {
      argument.clearAllSelections();
      for (int i = 0; i < dataParent.getEdgeList().size(); i++)
      {
        TreeEdge edge = (TreeEdge)dataParent.getEdgeList().elementAt(i);
        edge.getDestVertex().setSelected(true);
      }
      selectedEdge.getDestVertex().setSelected(true);
      try
      {
        argument.unlinkVertices();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    // Now select all the warrants for the data node and link them
    argument.clearAllSelections();
    TreeVertex dataNode = selectedEdge.getDestVertex();
    dataNode.setSelected(true);
    for (int i = 0; i < dataNode.toulminWarrantEdges.size(); i++)
    {
      TreeEdge edge = (TreeEdge)dataNode.toulminWarrantEdges.elementAt(i);
      edge.getDestVertex().setSelected(true);
      // All warrants must have the same parent to get past the checks in linkVertices()
      edge.getDestVertex().setParent(dataNode.getParent());
    }
    try {
      argument.linkVertices(); 
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Checks that a single warrant has been selected. 
   * Then adds right-clicked node as backing to
   * the warrant.
   */
  public void createBacking()
  {
    Vector vertexList = argument.getTree().getVertexList();
    TreeVertex warrant = null;
    boolean warrantFound = false;
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)vertexList.elementAt(i);
      if (vertex.isSelected() && vertex != mouseVertex && vertex.roles.get("toulmin").equals("warrant"))
      {
        if (warrantFound)
        {
          araucaria.setMessageLabelText("ERROR: Select only one warrant");
          return;
        }
        warrant = vertex;
        warrantFound = true;
      }
    }
    if (!warrantFound)
    {
      // If no warrant selected, but select vertex's parent is a warrant, use that
      if (mouseVertex.getParent().roles.get("toulmin").equals("warrant"))
      {
        warrant = mouseVertex.getParent();
      } else {
        araucaria.setMessageLabelText("ERROR: Select a warrant");
        return;
      }
    }

    // mouseVertex is the node to be attached as backing to warrant.
    // For now, require mouseVertex to have no supports (no warrants or rebuttals)
    // TODO: relax this restriction, but requires fiddling with linked arguments etc.
    if (mouseVertex.toulminWarrantEdges.size() > 0 || mouseVertex.toulminRebuttalEdges.size() > 0)
    {
      araucaria.setMessageLabelText("ERROR: Backing cannot have any supports");
      return;
    }
    // Detach mouseVertex from its current parent (if any) and attach to backing list of warrant
    TreeVertex parent = mouseVertex.getParent();
    if (parent != null)
    {
      parent.deleteEdge(mouseVertex);
      parent.toulminDataEdges.remove(parent.getEdge(parent.toulminDataEdges, mouseVertex));
    }
    warrant.toulminBackingEdges.add(new TreeEdge(warrant, mouseVertex));
    warrant.addEdge(mouseVertex);
    mouseVertex.roles.put("toulmin", "backing");
    displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "creating backing"));
    redrawTree(true);
    araucaria.updateDisplays(true);
  }
  
  /**
   * Checks for a single selected edge and a single selected vertex.
   * Adds the vertex to the data node on the edge as a rebuttal.
   * If the edge has a warrant, the rebuttal is added as a refutation to
   * the warrant in the standard diagram. If no warrant exists, a missing
   * premise is created with the text "It is not the case that..." followed
   * by whatever text is in the rebuttal.
   */
  public void createRebuttal()
  {
    // If mouseVertex is data, we create an opposite node, replace mouseVertex
    // with this node in Standard, and attach mouseVertex as a refutation node
    // to this new node.
    TreeVertex rebuttal = mouseVertex;
    TreeVertex parent = rebuttal.getParent();
    if (rebuttal.roles.get("toulmin").equals("data") ||
      rebuttal.roles.get("toulmin").equals("warrant"))
    {
      String oppositeText = "It is not the case that \"" + (String)rebuttal.getLabel() + "\"";
      TreeVertex opposite = argument.addAuxiliaryVertex(oppositeText);
      opposite.roles.put("addedNegation", "yes");
      opposite.isHiddenTable.put("toulmin", "true");
      opposite.isHiddenTable.put("wigmore", "true");
      argument.getTree().addVertex(opposite);
      parent.getEdgeList().remove(parent.getEdge(rebuttal));
      parent.addEdge(opposite);
      opposite.setParent(parent);
      opposite.setHasParent(true);
      opposite.addEdge(rebuttal);
      rebuttal.setParent(opposite);
      rebuttal.setRefutation(true);
      // If rebuttal has any backing nodes, it was converted from a warrant, 
      Vector backingList = rebuttal.toulminBackingEdges;
      for (int i = 0; i < backingList.size(); i++)
      {
        TreeEdge edge = (TreeEdge)backingList.elementAt(i);
        edge.getDestVertex().roles.put("toulmin", "data");
        rebuttal.toulminDataEdges.add(edge);
      }
      rebuttal.toulminBackingEdges = new Vector();
      displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "creating rebuttal"));
    }
    araucaria.doUndo(false, false);
    araucaria.doRedo(false);
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
      // Also need to test qualifier nodes, since these aren't vertices in standard diagram
      Vector qualEdges = vertex.toulminQualifierEdges;
      for (int i = 0; i < qualEdges.size(); i++)
      { 
      	TreeVertex qual = ((TreeEdge)qualEdges.elementAt(i)).getDestVertex();
      	if (testVertex(null, qual, x, y, false))
      	{
          TreeVertex qualSource = ((TreeEdge)qualEdges.elementAt(i)).getSourceVertex();
          TreeVertex qualSourceParent = qualSource.getParent();
          TreeEdge qualEdge = qualSourceParent.getEdge(qualSource);
          araucaria.setMessageLabelText("\"" + (String)qual.getLabel() + "\"");
          mouseEdge = qualEdge;
          //mouseEdge.setSelected(true);
          mouseVertex = null;
          //edgePopup.show(event.getComponent(), event.getX(), event.getY());
          return null;
      	}
      }
    }
    return null;
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
      // Also need to test qualifier nodes, since these aren't vertices in standard diagram
      Vector qualEdges = vertex.toulminQualifierEdges;
      for (int i = 0; i < qualEdges.size(); i++)
      { 
      	TreeVertex qual = ((TreeEdge)qualEdges.elementAt(i)).getDestVertex();
      	if (testVertex(event, qual, x, y))
      	{
            TreeVertex qualSource = ((TreeEdge)qualEdges.elementAt(i)).getSourceVertex();
            TreeVertex qualSourceParent = qualSource.getParent();
            TreeEdge qualEdge = qualSourceParent.getEdge(qualSource);
            mouseEdge = qualEdge;
            mouseEdge.setSelected(true);
            mouseVertex = qual;
            //edgePopup.show(event.getComponent(), event.getX(), event.getY());
      		return true;
      	}
      }
    }
    return false;
  }

  JMenu toulminTypeMenu;
  JMenuItem warrantMenu, backingMenu, dataMenu, rebuttalMenu, 
  	dataWarrantMenu, warrantDataMenu, rebuttalDataMenu, rebuttalWarrantMenu, 
          qualifierMenu, warrantRebuttalMenu, dataRebuttalMenu, rebuttalWarrantConvertMenu;
  JMenuItem showHideNegationMenu, showAllNegationsMenu, hideAllNegationsMenu, editQualifierMenu, deleteQualifierMenu;
  JMenu missingToulminTypeMenu;
  JMenuItem missingWarrantMenu, missingBackingMenu, missingDataMenu, missingRebuttalMenu;
  // Edge menu items
  JMenuItem dataEdgeQualMenu, warrantEdgeQualMenu, rebuttalEdgeQualMenu;
  JPopupMenu dataEdgePopup, warrantEdgePopup, qualifierEdgePopup, rebuttalEdgePopup;

  /**
   * Builds a custom popup menu for the particular vertex.
   */
  public void buildPopup(TreeVertex vertex)
  {
    String className = getClass().getName();
    vertexPopup = new JPopupMenu();
    toulminTypeMenu = new JMenu("Toulmin role");
    rebuttalWarrantConvertMenu = new JMenuItem("Convert to warrant");
    backingMenu = new JMenuItem("Convert to backing");
    dataMenu = new JMenuItem("Convert to datum");
    rebuttalMenu = new JMenuItem("Convert to rebuttal");
    dataWarrantMenu = new JMenuItem("Swap with warrant");
    warrantDataMenu = new JMenuItem("Swap with datum");
    rebuttalDataMenu = new JMenuItem("Swap with datum");
    rebuttalWarrantMenu = new JMenuItem("Swap with warrant");
    warrantRebuttalMenu = new JMenuItem("Swap with rebuttal");
    dataRebuttalMenu = new JMenuItem("Swap with rebuttal");
    showHideNegationMenu = new JMenuItem("Show negation");
    showAllNegationsMenu = new JMenuItem("Show all negations");
    hideAllNegationsMenu = new JMenuItem("Hide all negations");
    editQualifierMenu = new JMenuItem("Edit qualifier");
    deleteQualifierMenu = new JMenuItem("Delete qualifier");
    editMissingTextMenu = new JMenuItem("Edit text");
    editMissingTextMenu.addActionListener(this);
    editVertexIDMenu = new JMenuItem("Edit ID");
    editVertexIDMenu.addActionListener(this);
    vertexTextMenu = new JMenuItem("Show text");
    vertexTextMenu.addActionListener(this);
    dataMenu.addActionListener(this);
    rebuttalWarrantConvertMenu.addActionListener(this);
    backingMenu.addActionListener(this);
    rebuttalMenu.addActionListener(this);
    dataWarrantMenu.addActionListener(this);
    dataRebuttalMenu.addActionListener(this);
    warrantDataMenu.addActionListener(this);
    warrantRebuttalMenu.addActionListener(this);
    rebuttalDataMenu.addActionListener(this);
    rebuttalWarrantMenu.addActionListener(this);
    showHideNegationMenu.addActionListener(this);
    showAllNegationsMenu.addActionListener(this);
    hideAllNegationsMenu.addActionListener(this);
    editQualifierMenu.addActionListener(this);
    deleteQualifierMenu.addActionListener(this);
    setPremisesVisMenu = new JMenuItem("Collapse premises");
    JMenuItem noActionMenu = new JMenuItem("No action available");
    noActionMenu.setEnabled(false);
    if (vertex.hidingChildren)
    {
      setPremisesVisMenu.setText("Expand premises");
    }

    int toulminTypeMembers = 0;
    TreeVertex parent = vertex.getParent();
    if (className.indexOf("FullText") < 0)
    {
      vertexPopup.add(vertexTextMenu);
      if (!vertex.roles.get("toulmin").equals("qualifier"))
      {
        vertexPopup.add(editVertexIDMenu);
      }
    }
    if (vertex.isMissing())
    {
      vertexPopup.add(editMissingTextMenu);
    }
    // A data node's permissible operations depends on its
    // context.
    if (vertex.roles.get("toulmin").equals("qualifier"))
    {
    	vertexPopup.add(editQualifierMenu);
    	vertexPopup.add(deleteQualifierMenu);
    }
    if (parent == null) 
    {
      if (vertex.getEdgeList().size() > 0)
      {
        vertexPopup.add(setPremisesVisMenu);
      }
      setPremisesVisMenu.addActionListener(this);
      return;
    }
    if (vertex.roles.get("toulmin").equals("data"))
    {
      // Don't allow a datum to be converted to a rebuttal
      // Allow a swap with warrant if warrant is not added negation
      if (getNormalWarrant(vertex) != null)
      {
        toulminTypeMenu.add(dataWarrantMenu);
        toulminTypeMembers++;
      }
      if (rebuttalsSelected(vertex) == 1 || vertex.toulminRebuttalEdges.size() == 1)
      {
        TreeVertex selRebuttal = getSelectedRebuttal(vertex);
        if (selRebuttal.getParent().roles.get("addedNegation") != null &&
                selRebuttal.getParent().roles.get("addedNegation").equals("yes"))
        {
          toulminTypeMenu.add(dataRebuttalMenu);
          toulminTypeMembers++;
        }
      }
      if (vertex.getParent().roles.get("toulmin").equals("warrant"))
      {
        toulminTypeMenu.add(backingMenu);
        toulminTypeMembers++;
      }
    } 
    else if (vertex.roles.get("toulmin").equals("backing"))
    {   
      toulminTypeMenu.add(dataMenu);
      toulminTypeMembers++;
    }
    else if (vertex.roles.get("toulmin").equals("warrant") && 
            vertex.roles.get("addedNegation") != null &&
            vertex.roles.get("addedNegation").equals("no"))
    {
      toulminTypeMenu.add(rebuttalMenu);
      toulminTypeMenu.add(warrantDataMenu);
      toulminTypeMembers += 2;
      TreeVertex data = vertex.findDataNodeInLA();
      if (data.toulminRebuttalEdges.size() == 1 || rebuttalsSelected(data) == 1)
      {
        toulminTypeMenu.add(warrantRebuttalMenu);
        toulminTypeMembers += 1;
      }
    }
     else if (vertex.roles.get("toulmin").equals("rebuttal"))
    {
      TreeVertex negation = vertex.getParent();
      // Only allow show/hide negation if rebuttal's parent is addedNegation
      // and has no support
      if (negation.roles.get("addedNegation") != null &&
              negation.roles.get("addedNegation").equals("yes") &&
              negation.toulminBackingEdges.size() == 0 &&
              negation.toulminDataEdges.size() == 0)
      {
        if (negation.isHiddenTable.get("toulmin").equals("true"))
        {
          showHideNegationMenu.setText("Show negation");
        } else {
          showHideNegationMenu.setText("Hide negation");
        }
        vertexPopup.add(showHideNegationMenu);
//        vertexPopup.add(showAllNegationsMenu);
//        vertexPopup.add(hideAllNegationsMenu);
        toulminTypeMenu.add(rebuttalWarrantConvertMenu);
        toulminTypeMembers ++;
        toulminTypeMenu.add(rebuttalDataMenu);
        toulminTypeMembers ++;
      }

      TreeVertex data = vertex.getParent().findDataNodeInLA();
      if (getNormalWarrant(data) != null)
      {
        toulminTypeMenu.add(rebuttalWarrantMenu);
        toulminTypeMembers += 1;
      }
    }
    boolean activePopup = false;
    if (toulminTypeMembers > 0)
    {
      activePopup = true;
      vertexPopup.add(toulminTypeMenu);
    }
    if (vertex.getEdgeList().size() > 0)
    {
      activePopup = true;
      vertexPopup.add(setPremisesVisMenu);
    }
    setPremisesVisMenu.addActionListener(this);
    if (!activePopup)
    {
      vertexPopup.add(noActionMenu);
    }
  }

  /**
   * Creates the default popup menus 
   */
  protected void createPopupMenu()
  {
    // Vertex
    vertexPopup = new JPopupMenu();
    toulminTypeMenu = new JMenu("Toulmin role");
    warrantMenu = new JMenuItem("Warrant");
    backingMenu = new JMenuItem("Backing");
    dataMenu = new JMenuItem("Data");
    rebuttalMenu = new JMenuItem("Rebuttal");
    
    toulminTypeMenu.add(dataMenu);
    toulminTypeMenu.add(warrantMenu);
    toulminTypeMenu.add(backingMenu);
    toulminTypeMenu.add(rebuttalMenu);
    dataMenu.addActionListener(this);
    warrantMenu.addActionListener(this);
    backingMenu.addActionListener(this);
    rebuttalMenu.addActionListener(this);
    
    vertexPopup.add(toulminTypeMenu);
    
    // Missing premise
    missingPopup = new JPopupMenu();
    editMissingTextMenu = new JMenuItem("Edit text");
    
    missingToulminTypeMenu = new JMenu("Toulmin role");
    missingWarrantMenu = new JMenuItem("Warrant");
    missingBackingMenu = new JMenuItem("Backing");
    missingDataMenu = new JMenuItem("Data");
    missingRebuttalMenu = new JMenuItem("Rebuttal");
    missingToulminTypeMenu.add(missingDataMenu);
    missingToulminTypeMenu.add(missingWarrantMenu);
    missingToulminTypeMenu.add(missingBackingMenu);
    missingToulminTypeMenu.add(missingRebuttalMenu);
    missingDataMenu.addActionListener(this);
    missingWarrantMenu.addActionListener(this);
    missingBackingMenu.addActionListener(this);
    missingRebuttalMenu.addActionListener(this);
    missingPopup.add(missingToulminTypeMenu);
    missingPopup.add(editMissingTextMenu);
    editMissingTextMenu.addActionListener(this);

    // Edge
    dataEdgePopup = new JPopupMenu();
    dataEdgeQualMenu = new JMenuItem("Add/edit data qualifier");
    dataEdgePopup.add(dataEdgeQualMenu);
    dataEdgeQualMenu.addActionListener(this);

    warrantEdgePopup = new JPopupMenu();
    warrantEdgeQualMenu = new JMenuItem("Add/edit warrant qualifier");
    warrantEdgePopup.add(warrantEdgeQualMenu);
    warrantEdgeQualMenu.addActionListener(this);
    
    rebuttalEdgePopup = new JPopupMenu();
    rebuttalEdgeQualMenu = new JMenuItem("Add/edit rebuttal qualifier");
    rebuttalEdgePopup.add(rebuttalEdgeQualMenu);
    rebuttalEdgeQualMenu.addActionListener(this);
    
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
  
  /**
   * Selects the corresponding edges for each data edge from the standard diagram,
   * then opens LabelDialog to allow modification of evaluation/qualifier
   * for the selected edge.
   */
  public void doModifyQualifier()
  {
    setupQualifierEdit();
    LabelDialog labelDialog = new LabelDialog(araucaria);
    labelDialog.show();
  }
  
  private void setupQualifierEdit()
  {
    // Scan the whole tree to find all selected data edges.
    Vector nodeList = argument.getTree().getVertexList();
    for (int i = 0; i < nodeList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)nodeList.elementAt(i);
      for (int j = 0; j < vertex.toulminDataEdges.size(); j++)
      {
        TreeEdge edge = (TreeEdge)vertex.toulminDataEdges.elementAt(j);
        if (edge.isSelected())
        {
          TreeVertex standardParent = edge.getDestVertex().getParent();
          TreeEdge standardEdge = standardParent.getEdge(edge.getDestVertex());
          if (standardEdge != null)
          {
            standardEdge.setSelected(true);
          } else {
            System.out.println("Error: Cannot find standard edge for qualifier");
          }
        }
      }
    }
    argument.buildSupportLabelList();
  }
  
  public void doDeleteQualifier()
  {
    if (argument.doDeleteQualifier() > 0)
    {
      araucaria.undoStack.push(new EditAction(araucaria, "deleting evaluation"));    
      araucaria.doUndo(true, false);
      araucaria.doRedo(false);
    }
  }
  
  public void allNegations(boolean show)
  {
    Tree tree = argument.getTree();
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)vertexList.elementAt(i);
      if (vertex.roles.get("toulmin").equals("warrant") &&
        vertex.toulminBackingEdges.size() == 0 && vertex.toulminDataEdges.size() == 0
        && vertex.roles.get("addedNegation") != null &&
              vertex.roles.get("addedNegation").equals("yes"))
      {
        if (!show)  
        vertex.isHiddenTable.put("toulmin",  show ? "false" : "true");
      }
    }
    initializeDrawing(true);
    araucaria.updateDisplays(true);
  }
  
  public void showHideNegation()
  {
    TreeVertex negation = mouseVertex.getParent();
    if (negation.roles.get("toulmin").equals("warrant") &&
      (negation.toulminBackingEdges.size() > 0 || negation.toulminDataEdges.size() > 0))
    {
      araucaria.setMessageLabelText("Cannot hide a warrant with supporting arguments.");
      negation.isHiddenTable.put("toulmin", "false");
      return;
    }
    if (negation.isHiddenTable.get("toulmin").equals("true"))
    {
      negation.isHiddenTable.put("toulmin", "false");
    } else {
      negation.isHiddenTable.put("toulmin", "true");
    }
    initializeDrawing(true);
    araucaria.updateDisplays(true);
  }
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == warrantMenu || e.getSource() == missingWarrantMenu) {
      createWarrant(true);
    } else if (e.getSource() == rebuttalMenu || e.getSource() == missingRebuttalMenu) {
      createRebuttal();
    } else if (e.getSource() == backingMenu || e.getSource() == missingBackingMenu) {
      createBacking();
    } else if (e.getSource() == dataEdgeQualMenu || e.getSource() == editQualifierMenu) {
      doModifyQualifier();
    } else if (e.getSource() == deleteQualifierMenu) {
      doDeleteQualifier();
    } else if (e.getSource() == dataMenu) {
      convertToData();
    } else if (e.getSource() == warrantEdgeQualMenu) {
      doModifyQualifier();
    } else if (e.getSource() == rebuttalEdgeQualMenu) {
      doModifyQualifier();
    } else if (e.getSource() == dataWarrantMenu || e.getSource() == warrantDataMenu) {
      swapDataWarrant();
    } else if (e.getSource() == dataRebuttalMenu || e.getSource() == rebuttalDataMenu) {
      swapDataRebuttal();
    } else if (e.getSource() == warrantRebuttalMenu || e.getSource() == rebuttalWarrantMenu) {
      swapWarrantRebuttal(); 
    } else if (e.getSource() == rebuttalWarrantConvertMenu) {
      convertRebuttalToWarrant();
    } else if (e.getSource() == showHideNegationMenu) {
      showHideNegation();
    } else if (e.getSource() == showAllNegationsMenu) {
      allNegations(true);
    } else if (e.getSource() == hideAllNegationsMenu) {
      allNegations(false);
    } else if (e.getSource() == vertexTextMenu) {
      showVertexText();
    } else if (e.getSource() == editMissingTextMenu) {
      editMissingText();
    } else if (e.getSource() == editVertexIDMenu) {
      editVertexID();
    } else if (e.getSource() == setPremisesVisMenu) {
      showHidePremises();
    }
  }
  
  /**
   * Converts a backing node (assumed to be selected as 'mouseVertex')
   * to a data node supporting the same warrant
   */
  public void convertToData()
  {
    TreeVertex claimNode = null;
    if (mouseVertex.roles.get("toulmin").equals("backing"))
    {
      selectedData = mouseVertex;
      claimNode = selectedData.getParent();
      claimNode.toulminDataEdges.add(selectedData);
      claimNode.toulminBackingEdges.remove(selectedData);
      selectedData.roles.put("toulmin", "data");
    }
    displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "converting to data"));
    araucaria.doUndo(false, false);
    araucaria.doRedo(false);
  }
  
  public void  convertRebuttalToWarrant()
  {
    if (!mouseVertex.roles.get("toulmin").equals("rebuttal"))
    {
      return;
    }
    selectedRebuttal = mouseVertex;
    selectedData = selectedRebuttal.getParent().findDataNodeInLA();
    if (selectedData == null)
    {
      araucaria.setMessageLabelText("ERROR: This rebuttal is not associated with an implicit warrant");
      return;
    }
    selectedData.toulminRebuttalEdges.remove(selectedData.getEdge(selectedData.toulminRebuttalEdges,  selectedRebuttal));
    selectedData.toulminWarrantEdges.add(new TreeEdge(selectedData, selectedRebuttal));
    TreeVertex implicit = selectedRebuttal.getParent();
    implicit.deleteEdge(selectedRebuttal);
    TreeVertex virtual = selectedData.getParent();
    virtual.deleteEdge(implicit);
    virtual.addEdge(selectedRebuttal);
    argument.tree.getVertexList().remove(implicit);
    selectedRebuttal.roles.put("toulmin", "warrant");
    selectedRebuttal.setRefutation(false);
    selectedRebuttal.setParent(virtual);
    displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "converting rebuttal to warrant"));
    araucaria.doUndo(false, false);
    araucaria.doRedo(false);
  }
  
  public void swapWarrantRebuttal()
  {
    TreeVertex rebuttal = null;
    if (mouseVertex.roles.get("toulmin").equals("rebuttal"))
    {
      selectedRebuttal = mouseVertex;
      // Rebuttal is connected to data's LA via the added negation, which is its parent
      selectedData = selectedRebuttal.getParent().findDataNodeInLA();
      if (selectedData == null)
      {
        araucaria.setMessageLabelText("ERROR: This rebuttal is not associated with an implicit warrant");
        return;
      }
      selectedWarrant = getNormalWarrant(selectedData);
    } else if (mouseVertex.roles.get("toulmin").equals("warrant"))
    {
      selectedWarrant = mouseVertex;
      selectedData = selectedWarrant.findDataNodeInLA();
      if (selectedData == null)
      {
        araucaria.setMessageLabelText("ERROR: This warrant is not associated with a data node");
        return;
      }
      selectedRebuttal = getSelectedRebuttal(selectedData);
      if (selectedRebuttal == null)
      {
        araucaria.setMessageLabelText("ERROR: select exactly one rebuttal to swap with warrant");
        return;
      }
    } 
    // If we get this far, we have identified the warrant and rebuttal to be swapped
    //
    // Convert rebuttal to warrant
    TreeVertex virtual = selectedWarrant.getParent();
    TreeVertex implicitWarrant = selectedRebuttal.getParent();
    virtual.deleteEdge(selectedWarrant);
    virtual.addEdge(selectedRebuttal);
    selectedRebuttal.roles.put("toulmin", "warrant");
    selectedRebuttal.setRefutation(false);
    selectedRebuttal.setParent(virtual);
    selectedData.toulminRebuttalEdges.remove(selectedData.getEdge(selectedData.toulminRebuttalEdges,  selectedRebuttal));
    selectedData.toulminRebuttalEdges.add(new TreeEdge(selectedData, selectedWarrant));
    selectedData.toulminWarrantEdges.remove(selectedData.getEdge(selectedData.toulminWarrantEdges,  selectedWarrant));
    selectedData.toulminWarrantEdges.add(new TreeEdge(selectedData, selectedRebuttal));
    // Convert warrant to rebuttal
    implicitWarrant.deleteEdge(selectedRebuttal);
    implicitWarrant.addEdge(selectedWarrant);
    selectedWarrant.setParent(implicitWarrant);
    selectedWarrant.roles.put("toulmin", "rebuttal");
    selectedWarrant.setRefutation(true);
    implicitWarrant.setLabel("It is not the case that \"" + selectedWarrant.getLabel() + "\"");
    displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "swapping warrant with rebuttal"));
    araucaria.doUndo(false, false);
    araucaria.doRedo(false);
  }
  
  public void swapDataRebuttal()
  {
    TreeVertex claimNode = null, rebuttal = null;
    // Find the claim node that is the parent of the two nodes to be swapped.
    if (mouseVertex.roles.get("toulmin").equals("data"))
    {
      selectedData = mouseVertex;
      claimNode = selectedData.getParent(); 
      if (claimNode.isVirtual())
      {
        claimNode = claimNode.getParent();
      }
      // If there is only 1 rebuttal connected to this data, select it
      if (selectedData.toulminRebuttalEdges.size() == 1)
      {
        TreeVertex vertex = ((TreeEdge)selectedData.toulminRebuttalEdges.elementAt(0)).getDestVertex();
        vertex.setSelected(true);
      }
      int rebuttalCount = rebuttalsSelected(selectedData);
      if (rebuttalCount != 1)
      {
        araucaria.setMessageLabelText("Select exactly one rebuttal to swap with data");
        return;
      }
      rebuttal = selectedRebuttal;
    } else if (mouseVertex.roles.get("toulmin").equals("rebuttal"))
    {
      selectedRebuttal = mouseVertex;
      // Rebuttal is connected to data's LA via the added negation, which is its parent
      selectedData = selectedRebuttal.getParent().findDataNodeInLA();
      if (selectedData == null)
      {
        araucaria.setMessageLabelText("ERROR: This rebuttal is not associated with an implicit warrant");
        return;
      }
      claimNode = selectedData.getParent();
      if (claimNode.isVirtual())
      {
        claimNode = claimNode.getParent();
      }
    }
    // If we get this far we should have selected one data and one rebuttal from the
    // same claim, so now swap them
    // We first convert the rebuttal to a data node connected to the claim. 
    claimNode.toulminDataEdges.remove(claimNode.getEdge(claimNode.toulminDataEdges,  selectedData));
    claimNode.toulminDataEdges.add(new TreeEdge(claimNode, selectedRebuttal));
    selectedData.toulminRebuttalEdges.remove(selectedData.getEdge(selectedData.toulminRebuttalEdges,  selectedRebuttal));
    selectedRebuttal.toulminRebuttalEdges.add(new TreeEdge(selectedRebuttal, selectedData));
    
    TreeVertex virtual = selectedData.getParent();
    TreeVertex implicitWarrant = selectedRebuttal.getParent();
    virtual.deleteEdge(selectedData);
    virtual.addEdge(selectedRebuttal);
    selectedRebuttal.roles.put("toulmin", "data");
    selectedRebuttal.setRefutation(false);
    selectedRebuttal.setParent(virtual);
    //
    // Then we convert the data node to be a warrant in the list of the new data node.
    // In doing this, we delete the qualifier list, but all other lists can be transferred
    // directly.
    //selectedData.toulminQualifierEdges = new Vector();
    selectedData.roles.put("toulmin", "rebuttal");
    selectedData.setParent(implicitWarrant);
    implicitWarrant.deleteEdge(selectedRebuttal);
    implicitWarrant.addEdge(selectedData);
    implicitWarrant.setLabel("It is not the case that \"" + selectedData.getLabel() + "\"");
    selectedData.setRefutation(true);
    displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "swapping data with rebuttal"));
    araucaria.doUndo(false, false);
    araucaria.doRedo(false);
  }
  
  /**
   * Checks that the data node and exactly one warrant are selected, both from the same
   * claim. Swaps the roles of the two nodes.
   */
  public void swapDataWarrant()
  {
    TreeVertex claimNode = null, warrant = null;
    // Find the claim node that is the parent of the two nodes to be swapped.
    if (mouseVertex.roles.get("toulmin").equals("data"))
    {
      selectedData = mouseVertex;
      claimNode = selectedData.getParent(); 
      if (claimNode.isVirtual())
      {
        claimNode = claimNode.getParent();
      }
      // If there is only 1 warrant connected to this data, select it
      if (selectedData.toulminWarrantEdges.size() == 1)
      {
        TreeVertex vertex = ((TreeEdge)selectedData.toulminWarrantEdges.elementAt(0)).getDestVertex();
        vertex.setSelected(true);
      }
      
      int warrantCount = warrantsSelected(selectedData);
      if (warrantCount != 1)
      {
        araucaria.setMessageLabelText("Select exactly one warrant to swap with data");
        return;
      }
      warrant = selectedWarrant;
    } else if (mouseVertex.roles.get("toulmin").equals("warrant"))
    {
      selectedWarrant = mouseVertex;
      selectedData = selectedWarrant.findDataNodeInLA();
      if (selectedData == null)
      {
        araucaria.setMessageLabelText("ERROR: This warrant is not associated with a data node");
        return;
      }
      claimNode = selectedData.getParent();
      if (claimNode.isVirtual())
      {
        claimNode = claimNode.getParent();
      }
    }
    // If we get this far we should have selected one data and one warrant from the
    // same claim, so now swap them
    // We first convert the warrant to a data node connected to the claim. In doing this,
    // the backing list is copied to the data list; all other lists can be transferred
    // directly.
    
    claimNode.toulminDataEdges.add(new TreeEdge(claimNode, selectedWarrant));
    Vector backingList = selectedWarrant.toulminBackingEdges;
    for (int i = 0; i < backingList.size(); i++)
    {
      TreeEdge edge = (TreeEdge)backingList.elementAt(i);
      edge.getDestVertex().roles.put("toulmin", "data");
      selectedWarrant.toulminDataEdges.add(edge);
    }
    selectedData.toulminWarrantEdges.remove(selectedData.getEdge(selectedData.toulminWarrantEdges,  selectedWarrant));
    selectedWarrant.toulminBackingEdges = new Vector();
    selectedWarrant.roles.put("toulmin", "data");
    //
    // Then we convert the data node to be a warrant in the list of the new data node.
    // In doing this, we delete the qualifier list, but all other lists can be transferred
    // directly.
    selectedWarrant.toulminWarrantEdges.add(new TreeEdge(selectedWarrant, selectedData));
    selectedData.toulminQualifierEdges = new Vector();
    selectedData.roles.put("toulmin", "warrant");
    displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "swapping data with warrant"));
    araucaria.doUndo(false, false);
    araucaria.doRedo(false);
  }
  
  /**
   * Tests if a rebuttal belonging to data has been selected.
   */
  public int rebuttalsSelected(TreeVertex data)
  {
    Vector rebuttalList = data.toulminRebuttalEdges;
    int rebuttalCount = 0;
    selectedRebuttal = null;
    for (int i = 0; i < rebuttalList.size(); i++)
    {
      TreeVertex vertex = ((TreeEdge)rebuttalList.elementAt(i)).getDestVertex();
      if (vertex.isSelected())
      {
        rebuttalCount ++;
        selectedRebuttal = vertex;
      }
    }
    return rebuttalCount;
  }
  
  /**
   * Tests if a warrant belonging to data has been selected.
   * Can include added negation nodes or not.
   */
  public int warrantsSelected(TreeVertex data, boolean includeAddedNegation)
  {
    Vector warrantList = data.toulminWarrantEdges;
    int warrantCount = 0;
    selectedWarrant = null;
    for (int i = 0; i < warrantList.size(); i++)
    {
      TreeVertex vertex = ((TreeEdge)warrantList.elementAt(i)).getDestVertex();
      if (vertex.isSelected())
      {
        String addedNeg = (String)vertex.roles.get("addedNegation");
        if (addedNeg != null && addedNeg.equals("yes") && includeAddedNegation)
        {
          warrantCount ++;
          selectedWarrant = vertex;
        } else if (addedNeg == null || addedNeg.equals("no"))
        {
          warrantCount ++;
          selectedWarrant = vertex;
        }
      }
    }
    return warrantCount;
  }
  
  public int warrantsSelected(TreeVertex data)
  {
    return warrantsSelected(data, true);
  }
  
  /**
   * Finds if there is either a single normal (not added negation) warrant
   * or else a single selected normal warrant associated with data.
   */
  public TreeVertex getNormalWarrant(TreeVertex data)
  {
    if (data == null) return null;
    Vector warrantList = data.toulminWarrantEdges;
    int count = 0;
    TreeVertex normalWarrant = null;
    for (int i = 0; i < warrantList.size(); i++)
    {
      TreeVertex vertex = ((TreeEdge)warrantList.elementAt(i)).getDestVertex();
      if (vertex.isSelected() || warrantList.size() == 1)
      {
        String addedNeg = (String)vertex.roles.get("addedNegation");
        if (addedNeg == null || addedNeg.equals("no"))
        {
          count++;
          normalWarrant = vertex;
        }
      }
    }
    if (count != 1) return null;
    return normalWarrant;
  }
  
  /**
   * Finds if there is either a single normal rebuttal
   * or else a single selected rebuttal associated with data.
   */
  public TreeVertex getSelectedRebuttal(TreeVertex data)
  {
    if (data == null) return null;
    Vector rebuttalList = data.toulminRebuttalEdges;
    int count = 0;
    TreeVertex selectedRebuttal = null;
    for (int i = 0; i < rebuttalList.size(); i++)
    {
      TreeVertex vertex = ((TreeEdge)rebuttalList.elementAt(i)).getDestVertex();
      if (vertex.isSelected() || rebuttalList.size() == 1)
      {
        count++;
        selectedRebuttal = vertex;
      }
    }
    if (count != 1) return null;
    return selectedRebuttal;
  }
  
  /**
   * Tests if the data node owning the warrant is selected
   */
  public boolean dataSelected(TreeVertex warrant)
  {
    selectedData = null;
    TreeVertex data = warrant.findDataNodeInLA();
    if (data != null && data.isSelected())
    {
      selectedData = data;
      return true;
    }
    return false;
  }
}
