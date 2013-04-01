/*
 * WigmoreFullTextPanel.java
 *
 * Created on 11 August 2005, 17:36
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 *
 * @author growe
 */
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.text.*;
import java.awt.font.*;
import java.awt.geom.*;
import javax.swing.*;

public class WigmoreFullTextPanel extends DiagramBase
{

  /** Creates a new instance of WigmoreFullTextPanel */
  public WigmoreFullTextPanel()
  {
    buildRoleHashtable();
    evidencePopup = new JPopupMenu();
    wigmoreForce.addActionListener(this);
    evidencePopup.add(wigmoreForce);
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
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    if (argument != null && argument.getBreadthFirstTraversal() != null) {
      gg.setPaint(Color.white);
      gg.fillRect(0, 0, canvasSize.width, canvasSize.height);
      drawTree(gg);
      return image;
    }
    return null;
  }
  
  public boolean initializeDrawing(boolean resetSize)
  {
    Tree tree = argument.getTree();
    // Clear vertex shapes - needed for show/hide negations
    for (int i = 0; i < tree.getVertexList().size(); i++)
    {
      assignVertexShape((TreeVertex)tree.getVertexList().elementAt(i), 0, 0, 1, 1);
    }
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
        canvasWidth += subRoot.wigmoreWidth;
        if (subRoot.wigmoreHeight > canvasHeight)
          canvasHeight = subRoot.wigmoreHeight;
      }
    } else {
      canvasWidth = root.wigmoreWidth + 4 * leftOffset;
      canvasHeight = root.wigmoreHeight + 2 * topOffset;
    }
    if (resetSize)
    {
      Dimension panelDim = new Dimension(canvasWidth, canvasHeight);
      setPreferredSize(panelDim);
      getDisplayFrame().getMainScrollPane().setViewportView(this);
    }
    return true;
  }

  int minVertSeparation = 70; // Minimum separation between bottom of claim and top of evidence node
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
        subRootX += subRoot.wigmoreWidth;
      }
      return;
    }
    
    if (root.hidingChildren)
    {
      root.wigmoreX = xStart + leftOffset;
      root.wigmoreY = yStart + topOffset;
      Point corner = new Point(root.wigmoreX, root.wigmoreY);
      Dimension totalLayoutSize = root.totalLayoutSize; 
      assignVertexShape(root, corner.x, corner.y, 
          (int)totalLayoutSize.getWidth() + 2 * textBorderMargin,
          (int)totalLayoutSize.getHeight() + 2 * textBorderMargin);
      return;
    }
    
    // Find the horizontal positions of the left, node and right blocks
    int totalWidth = root.wigmoreLeft.width + root.totalLayoutSize.width + 2 * textBorderMargin + root.wigmoreRight.width;
    if (root.wigmoreBottom.width / 2 > root.wigmoreLeft.width + root.totalLayoutSize.width / 2 + textBorderMargin)
    {
      root.wigmoreX = xStart + (root.wigmoreBottom.width - root.totalLayoutSize.width) / 2 - textBorderMargin + leftOffset;
    } else {
      root.wigmoreX = xStart + root.wigmoreLeft.width + leftOffset;
    }
    root.wigmoreY = yStart + topOffset;
    
// Right-justify the explanatory nodes
    int yData = yStart;
    
    if (root.wigmoreExplanatoryEdges.size() == 1)
    {
      TreeVertex dest = root.wigmoreExplanatoryEdges.elementAt(0).getDestVertex();
      if (dest.isVirtual())
      {
        // Put the virtual node a bit down from the top of the text box
        dest.wigmoreX = root.wigmoreX - minHorizSeparation;
        dest.wigmoreY = yData + minVertBlockSeparation + topOffset - 10;
        assignVertexShape(dest,  dest.wigmoreX, dest.wigmoreY, 1, 1);
        for (Object e : dest.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex node = edge.getDestVertex();
          calcBlockCoords(node, dest.wigmoreX - node.wigmoreWidth - combHeight - leftOffset, 
                  yData, layer + 1);
          yData += node.wigmoreHeight + minVertBlockSeparation;
          calcExplanatoryEdge(edge);
        }
      } else {
        calcBlockCoords(dest,  root.wigmoreX - minHorizSeparation - dest.wigmoreWidth - leftOffset, yData, layer + 1);
        yData += dest.wigmoreHeight;
      }
      calcExplanatoryEdge(root.wigmoreExplanatoryEdges.elementAt(0));
    }
    
    // Left-justify the corroborative nodes
    yData = yStart;
    if (root.wigmoreCorroborativeEdges.size() == 1)
    {
      TreeVertex dest = root.wigmoreCorroborativeEdges.elementAt(0).getDestVertex();
      TreeVertex parent = dest.getParent();
      if (dest.isVirtual())
      {
        // Put the virtual node a bit down from the top of the text box
        dest.wigmoreX = root.wigmoreX + root.totalLayoutSize.width + 2*textBorderMargin + minHorizSeparation;
        dest.wigmoreY = yData + minVertBlockSeparation + topOffset;
        assignVertexShape(dest, dest.wigmoreX, dest.wigmoreY, 1, 1);
        for (Object e : dest.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex node = edge.getDestVertex();
          calcBlockCoords(node, 
                  root.wigmoreX + root.totalLayoutSize.width + 2*textBorderMargin + minHorizSeparation + combHeight - leftOffset,
                  yData, layer + 1);
          yData += node.wigmoreHeight + minVertBlockSeparation;
          calcCorroborativeEdge(edge);
        }
      } else {
        calcBlockCoords(dest,  
                root.wigmoreX + root.totalLayoutSize.width + 2*textBorderMargin + minHorizSeparation - leftOffset,
                yData, layer + 1);
        yData += dest.wigmoreHeight;
      }
      calcCorroborativeEdge(root.wigmoreCorroborativeEdges.elementAt(0));
    }
    
    // Centre the bottom block relative to centre portion of parent block
    int xData;
    if (root.wigmoreBottom.width / 2 > root.wigmoreLeft.width + root.totalLayoutSize.width / 2)
    {
      xData = xStart;
    } else {
      xData = xStart + root.wigmoreLeft.width + root.totalLayoutSize.width / 2 - root.wigmoreBottom.width / 2;
    }
    int rootTotalHeight = root.totalLayoutSize.height + 2 * textBorderMargin;
    yData = Math.max(root.wigmoreLeft.height, root.wigmoreRight.height);
    yData = Math.max(yData, rootTotalHeight);
    if (yData - rootTotalHeight < minVertSeparation)
    {
      yData += minVertSeparation;
    }
    if (root.wigmoreEvidenceEdges.size() == 1)
    {
      TreeVertex dest = root.wigmoreEvidenceEdges.elementAt(0).getDestVertex();
      if (dest.isVirtual())
      {
        yData += combHeight;
        dest.wigmoreX = xData + root.wigmoreBottom.width / 2 + leftOffset;
        dest.wigmoreY = yStart + yData + topOffset;
        assignVertexShape(dest,  dest.wigmoreX, dest.wigmoreY, 1, 1);
        yData += combHeight;
        for (Object e : dest.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex node = edge.getDestVertex();
          calcBlockCoords(node, xData, yStart + yData, layer + 1);
          xData += node.wigmoreWidth + minBlockHorizSpace;
          calcEvidenceEdge(edge);
        }
      } else {
        calcBlockCoords(dest, xData, yStart + yData, layer + 1);
        xData += dest.wigmoreWidth + minBlockHorizSpace;
      }
      calcEvidenceEdge(root.wigmoreEvidenceEdges.elementAt(0));
    }
    
    Point corner = new Point(root.wigmoreX, root.wigmoreY);
    Dimension totalLayoutSize = root.totalLayoutSize; 
    assignVertexShape(root, corner.x, corner.y, 
        (int)totalLayoutSize.getWidth() + 2 * textBorderMargin,
        (int)totalLayoutSize.getHeight() + 2 * textBorderMargin);
  }
  
  /**
   * Calculates the edge connecting an explanatory node to its parent
   */
  int vertArrowOffset = 13;
  public void calcExplanatoryEdge(TreeEdge edge)
  {
    TreeVertex source = edge.getSourceVertex();
    TreeVertex dest = edge.getDestVertex();
    String force = (String)dest.getSupportLabel();

    GeneralPath path = new GeneralPath();
    // Virtual source means we draw a dog-leg from explanatory node to 
    // virtual junction, no arrowhead. Line goes from right side of
    // explanatory node's box
    if (source.isVirtual())
    {
      path.append(new Line2D.Double(source.wigmoreX,  
              source.wigmoreY, 
              source.wigmoreX, dest.wigmoreY + vertArrowOffset), false);
      path.append(new Line2D.Double(source.wigmoreX, dest.wigmoreY + vertArrowOffset, 
              dest.wigmoreX + dest.totalLayoutSize.width + 2*textBorderMargin, 
              dest.wigmoreY + vertArrowOffset), true);
      if (force != null)
      {
        path.append(addForceSymbol(source.wigmoreX, dest.wigmoreY + vertArrowOffset, 
              dest.wigmoreX + dest.totalLayoutSize.width + 2*textBorderMargin, 
              dest.wigmoreY + vertArrowOffset, force, false), false);
      }
      edge.setShape(path,  this);
    } else if (dest.isVirtual())
    {
      path.append(new Line2D.Double(source.wigmoreX, source.wigmoreY + vertArrowOffset, 
              dest.wigmoreX, source.wigmoreY + vertArrowOffset), false);
      String explanatoryForce = (String)source.roles.get("wigmoreExplanatoryForce");
      if (explanatoryForce != null)
      {
        path.append(addForceSymbol(
          source.wigmoreX, source.wigmoreY + vertArrowOffset, 
              dest.wigmoreX, source.wigmoreY + vertArrowOffset, explanatoryForce, false), false);
      }
      edge.setShape(path,  this);
    } else {
      path.append(new Line2D.Double(source.wigmoreX, source.wigmoreY + vertArrowOffset, 
              dest.wigmoreX + dest.totalLayoutSize.width + 2*textBorderMargin, 
              dest.wigmoreY + vertArrowOffset), false);
      if (force != null)
      {
        path.append(addForceSymbol(source.wigmoreX, source.wigmoreY + vertArrowOffset, 
              dest.wigmoreX + dest.totalLayoutSize.width + 2*textBorderMargin, 
              dest.wigmoreY + vertArrowOffset, force, false), false);
      }
      edge.setShape(path,  this);
    }
  }
  
  /**
   * Calculates the edge connecting an explanatory node to its parent
   */
  public void calcCorroborativeEdge(TreeEdge edge)
  {
    TreeVertex source = edge.getSourceVertex();
    TreeVertex dest = edge.getDestVertex();
    String force = dest.getSupportLabel();
    GeneralPath path = new GeneralPath();
    // Virtual source means we draw a dog-leg from corroborative node to 
    // virtual junction, no arrowhead. Line goes from right side of
    // corroborative node's box
    if (source.isVirtual())
    {
      path.append(new Line2D.Double(source.wigmoreX,  
              source.wigmoreY, 
              source.wigmoreX, dest.wigmoreY + vertArrowOffset), false);
      path.append(new Line2D.Double(source.wigmoreX, dest.wigmoreY + vertArrowOffset, 
              dest.wigmoreX, 
              dest.wigmoreY + vertArrowOffset), true);
      if (force != null)
      {
        path.append(addForceSymbol(source.wigmoreX, dest.wigmoreY + vertArrowOffset, 
              dest.wigmoreX, dest.wigmoreY + vertArrowOffset, force, false), false);
      }
      edge.setShape(path,  this);
    } else if (dest.isVirtual())
    {
      path.append(new Line2D.Double(source.wigmoreX + source.totalLayoutSize.width + 2*textBorderMargin, 
              source.wigmoreY + vertArrowOffset, 
              dest.wigmoreX, source.wigmoreY + vertArrowOffset), false);
      String corroborativeForce = (String)source.roles.get("wigmoreCorroborativeForce");
      if (corroborativeForce != null)
      {
        path.append(addForceSymbol(
          source.wigmoreX + source.totalLayoutSize.width + 2*textBorderMargin, 
              source.wigmoreY + vertArrowOffset, 
              dest.wigmoreX, source.wigmoreY + vertArrowOffset, corroborativeForce, false), false);
      }
      edge.setShape(path,  this);
    } else {
      path.append(new Line2D.Double(source.wigmoreX + source.totalLayoutSize.width + 2*textBorderMargin, 
              source.wigmoreY + vertArrowOffset, 
              dest.wigmoreX, 
              dest.wigmoreY + vertArrowOffset), false);
      if (force != null)
      {
        path.append(addForceSymbol(source.wigmoreX + source.totalLayoutSize.width + 2*textBorderMargin, 
              source.wigmoreY + vertArrowOffset, dest.wigmoreX, 
              dest.wigmoreY + vertArrowOffset, force, false), false);
      }
      edge.setShape(path,  this);
    }
  }
  
  /**
   * Calculates the edge connecting an evidence node to its parent
   */
  public void calcEvidenceEdge(TreeEdge edge)
  {
    TreeVertex source = edge.getSourceVertex();
    TreeVertex dest = edge.getDestVertex();
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
              source.wigmoreY + source.totalLayoutSize.height + 2*textBorderMargin, 
              dest.wigmoreX, dest.wigmoreY), false);
      String evidenceForce = (String)source.roles.get("wigmoreEvidenceForce");
      path.append(addForceSymbol(
        source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,  
            source.wigmoreY + source.totalLayoutSize.height + 2*textBorderMargin, 
            dest.wigmoreX, dest.wigmoreY, evidenceForce, false), false);
      edge.setShape(path, this);
    }
    // Neither node virtual means a single evidence supporting a claim
    // If evidence not directly under parent, use a dog-leg
    else 
    {
      path.append(new Line2D.Double(source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,  
              source.wigmoreY + source.totalLayoutSize.height + 2*textBorderMargin, 
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
              source.wigmoreY + source.totalLayoutSize.height + 2*textBorderMargin, 
              source.wigmoreX + source.totalLayoutSize.width/2 + textBorderMargin,
              dest.wigmoreY - combHeight, force, dest.isRefutation()), false);
      edge.setShape(path, this);
    }
  }
  
  public static int negatoryDiam = 5;
  public Shape addForceSymbol(int x1, int y1, int x2, int y2, String force, boolean negatory)
  {
    GeneralPath path = new GeneralPath();
    if (force == null) 
    {
      if (negatory)
      {
        path.append(new Ellipse2D.Double(x1 - negatoryDiam/2, y1 + negatoryDiam, negatoryDiam, negatoryDiam), false);
        return path;
      } else {
        return path;
      }
    }
    if (force.equals(evidenceForce[0]) && !negatory)
    {
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 0, 3, 7), false);
    } else if (force.equals(evidenceForce[0] + " Negatory") || (force.equals(evidenceForce[0]) && negatory))
    {
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 0, 3, 7), false);
      path.append(new Ellipse2D.Double(x1 - negatoryDiam/2, y1 + 2*negatoryDiam, negatoryDiam, negatoryDiam), false);
    } else if (force.equals(evidenceForce[1]) && !negatory)
    {
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 0, 3, 7), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 5, 3, 7), false);
    } else if (force.equals(evidenceForce[1] + " Negatory") || (force.equals(evidenceForce[1]) && negatory))
    {
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 0, 3, 7), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 5, 3, 7), false);
      path.append(new Ellipse2D.Double(x1 - negatoryDiam/2, y1 + 3*negatoryDiam, negatoryDiam, negatoryDiam), false);
    } else if (force.equals(evidenceForce[2]) && !negatory)
    {
      path.append(new Line2D.Double(x1 - 5, y1 + 5, x1 + 5, y1 + 5), false);
    } else if (force.equals(evidenceForce[2] + " Negatory") || (force.equals(evidenceForce[2]) && negatory))
    {
      path.append(new Ellipse2D.Double(x1 - negatoryDiam/2, y1 + negatoryDiam, negatoryDiam, negatoryDiam), false);
      path.append(new Line2D.Double(x1 - 5, y1 + 3*negatoryDiam, x1 + 5, y1 + 3*negatoryDiam), false);
    } else if (force.equals(evidenceForce[3]) && !negatory)
    {
      path.append(new Line2D.Double(x1 - 5, y1 + 5, x1 + 5, y1 + 5), false);
      path.append(new Line2D.Double(x1 - 5, y1 + 10, x1 + 5, y1 + 10), false);
    } else if (force.equals(evidenceForce[3] + " Negatory") || (force.equals(evidenceForce[3]) && negatory))
    {
      path.append(new Ellipse2D.Double(x1 - negatoryDiam/2, y1 + negatoryDiam, negatoryDiam, negatoryDiam), false);
      path.append(new Line2D.Double(x1 - 5, y1 + 3*negatoryDiam, x1 + 5, y1 + 3*negatoryDiam), false);
      path.append(new Line2D.Double(x1 - 5, y1 + 3*negatoryDiam + 5, x1 + 5, y1 + 3*negatoryDiam + 5), false);
    } else if (force.equals(evidenceForce[4]) && !negatory)
    {
      int offset = 15;
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (y2-y1)-offset, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, offset, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 0, 3, 7), false);
    } else if (force.equals(evidenceForce[4] + " Negatory") || (force.equals(evidenceForce[4]) && negatory))
    {
      int offset = 20;
      path.append(new Ellipse2D.Double(x1 - negatoryDiam/2, y1 + 2*negatoryDiam, negatoryDiam, negatoryDiam), false);
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (y2-y1)-offset, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, offset, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 0, 3, 7), false);
    } else if (force.equals(evidenceForce[5]) && !negatory)
    {
      int offset = 17;
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (y2-y1)-offset, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, offset, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (y2-y1)-offset+3, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, offset+3, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 0, 3, 7), false);
    } else if (force.equals(evidenceForce[5] + " Negatory") || (force.equals(evidenceForce[5]) && negatory))
    {
      int offset = 17 + negatoryDiam;
      path.append(new Ellipse2D.Double(x1 - negatoryDiam/2, y1 + 2*negatoryDiam, negatoryDiam, negatoryDiam), false);
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (y2-y1)-offset, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, offset, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (y2-y1)-offset+3, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2+offset, offset+3, 4, 4), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, 0, 3, 7), false);
    } else if (force.indexOf(evidenceForce[6]) != -1)   // Doubt
    {
      int offset = 17;
      FontRenderContext frc = ((Graphics2D)getGraphics()).getFontRenderContext();
      Font font = new Font("sans-serif", Font.PLAIN, 12);
      TextLayout quesLayout = new TextLayout("?", font, frc);
      path.append(quesLayout.getOutline(AffineTransform.getTranslateInstance(x1 + 5, y1 + offset)), false);
    } else if (force.equals(explanatoryForce[0]))
    {
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (x1-x2)/2, 3, 7), false);
    } else if (force.equals(explanatoryForce[1]))
    {
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (x1-x2)/2 + 3, 3, 7), false);
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (x1-x2)/2 - 3, 3, 7), false);
    } else if (force.equals(corroborativeForce[0]))
    {
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (x2-x1)/2, 5, 5), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, (x2-x1)/2, 5, 5), false);
    } else if (force.equals(corroborativeForce[1]))
    {
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (x2-x1)/2, 5, 5), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, (x2-x1)/2, 5, 5), false);
      path.append(DiagramBase.addArrowHead(x2, y2, x1, y1, (x2-x1)/2+3, 5, 5), false);
      path.append(DiagramBase.addArrowHead(x1, y1, x2, y2, (x2-x1)/2-3, 5, 5), false);
    }  
    return path;
  }
  
  public void paint(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D)g;
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    gg.setPaint(getDiagramBackground());
    gg.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
    drawTree(gg);
    getDisplayFrame().getMainScrollPane().getViewport().setBackground(getDiagramBackground());
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

  public void drawVertex(Graphics2D gg, TreeVertex vertex, Color color)
  {
    // Draw the box around the node's text
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
    
    //
    // Draw fact symbol, if any
    //
    if (!vertex.isWigmoreLeaf())
    {
      vertex.roles.put("wigmoreFact", "none");
    }
    String wigFact = (String)vertex.roles.get("wigmoreFact");
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
      Font infinityFont = new Font("Sansserif", Font.PLAIN, 15);
      gg.setFont(infinityFont);
      FontMetrics metrics = gg.getFontMetrics();
      Rectangle2D bounds = metrics.getStringBounds(factString, gg);
      gg.drawString(factString, corner.x + textBox.getBounds().width / 2 - (int)bounds.getWidth() / 2, 
              corner.y + textBox.getBounds().height + metrics.getAscent());
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

  public Shape getIcon(TreeVertex vertex, int x, int y, Dimension iconSize)
  {
    WigmoreRole role = getRoleFromLabel((String)vertex.roles.get("wigmore"));
    if (role != null)
    {
      return role.iconImage;
    }
    return null;
  }
  
  public BufferedImage getImage(TreeVertex vertex)
  {
    WigmoreRole role = getRoleFromLabel((String)vertex.roles.get("wigmore"));
    if (role != null)
    {
      return role.menuImage;
    }
    return null;
  }
  
  public void drawText(Graphics2D gg, TreeVertex vertex, Paint textColor, Point corner)
  {
    int y = corner.y;
    y = drawLayout(gg, vertex.roleLayout, 
      corner.x + textBorderMargin + WigmoreRole.wigmoreIconSize.width + WigmoreRole.wigmoreIconOffset.width, 
            y, DiagramBase.roleColor,
      vertex.roleLayoutSize);
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    gg.drawImage(getImage(vertex), corner.x + WigmoreRole.wigmoreIconOffset.width, 
            corner.y + WigmoreRole.wigmoreIconOffset.height, null);
    drawLayout(gg, vertex.textLayout, corner.x + textBorderMargin, y,
      textColor, vertex.textLayoutSize);
    Rectangle2D bound = new Rectangle2D.Double(corner.x + WigmoreRole.wigmoreIconOffset.width, 
            corner.y + WigmoreRole.wigmoreIconOffset.height, getImage(vertex).getWidth(), getImage(vertex).getHeight());
    Shape nodeLabelShape = WigmoreImages.getNodeLabelShape(vertex.m_nodeLabel, bound,  WigmoreImages.NodePosition.CENTRE);
    if (nodeLabelShape != null)
    {
      gg.draw(nodeLabelShape);
    } else if (vertex.m_nodeLabel != null && vertex.m_nodeLabel.toLowerCase().equals("doubt"))
    {
      Font labelFont = new Font("Sansserif", Font.PLAIN, 8);
      gg.setFont(labelFont);
      String doubt = "?";
      FontMetrics metrics = gg.getFontMetrics();
      Rectangle2D bounds = metrics.getStringBounds(doubt, gg);
      gg.drawString(doubt, corner.x + getImage(vertex).getWidth()/2, 
              corner.y + WigmoreRole.wigmoreIconOffset.height + getImage(vertex).getHeight()/2 + metrics.getAscent() / 2);
    }
  }
  
  int combHeight = 40;  // Height of comb edge area above bottom nodes
  int minVertBlockSeparation = 25;    // minimum vertical separation between explanatory and corroborative nodes
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
    
    if (claim.hidingChildren)
    {
      claim.wigmoreHeight = claim.totalLayoutSize.height + 2 * textBorderMargin;
      return;
    }
    int totalHeight = 0;
    int separatorHeight = 0;
    int explanatoryHeight = 0;
    int corroborativeHeight = 0;
    // Calculate explanatory node heights
    if (claim.wigmoreExplanatoryEdges.size() > 1)
    {
      System.out.println("Error in calcBlockHeight: explanatoryEdges contains " + claim.wigmoreExplanatoryEdges.size() + " edges.");
      return;
      // If exactly one evidence edge...
    } else if (claim.wigmoreExplanatoryEdges.size() == 1)
    {
      TreeVertex exNode = claim.wigmoreExplanatoryEdges.elementAt(0).getDestVertex();
      // If explanatory node is virtual, we have several explanatory nodes attached to it
      if (exNode.isVirtual())
      {
        for (Object e : exNode.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex dest = edge.getDestVertex();
          calcBlockHeight(dest);
          explanatoryHeight += dest.wigmoreHeight;
        }
        // Add on vertical separation between blocks
        explanatoryHeight += minVertBlockSeparation * (exNode.getEdgeList().size() - 1);
      } else {
        calcBlockHeight(exNode);
        explanatoryHeight += exNode.wigmoreHeight;
      }
    }
    claim.wigmoreLeft.height = explanatoryHeight;
    
    //
    // Corroborative nodes
    //
    if (claim.wigmoreCorroborativeEdges.size() > 1)
    {
      System.out.println("Error in calcBlockHeight: corroborativeEdges contains " + claim.wigmoreCorroborativeEdges.size() + " edges.");
      return;
      // If exactly one corroborative edge...
    } else if (claim.wigmoreCorroborativeEdges.size() == 1)
    {
      TreeVertex corrNode = claim.wigmoreCorroborativeEdges.elementAt(0).getDestVertex();
      // If explanatory node is virtual, we have several corroborative nodes attached to it
      if (corrNode.isVirtual())
      {
        for (Object e : corrNode.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex dest = edge.getDestVertex();
          calcBlockHeight(dest);
          corroborativeHeight += dest.wigmoreHeight;
        }
        // Add on vertical separation between blocks
        corroborativeHeight += minVertBlockSeparation * (corrNode.getEdgeList().size() - 1);
      } else {
        calcBlockHeight(corrNode);
        corroborativeHeight += corrNode.wigmoreHeight;
      }
    }
    claim.wigmoreRight.height = corroborativeHeight;
    
    // Compare left to right
    int topHeight = explanatoryHeight > corroborativeHeight ?
      explanatoryHeight : corroborativeHeight;
    // Compare max (left, right) to text node
    topHeight = topHeight > claim.totalLayoutSize.height + 2 * textBorderMargin?
      topHeight : claim.totalLayoutSize.height + 2 * textBorderMargin;
    
    // Find max of heights of evidence nodes
    int maxBottom = 0;
    if (claim.wigmoreEvidenceEdges.size() > 1)
    {
      System.out.println("Error: evidenceEdges contains " + claim.wigmoreEvidenceEdges.size() + " edges.");
      return;
      // If exactly one evidence edge...
    } else if (claim.wigmoreEvidenceEdges.size() == 1)
    {
      TreeVertex evNode = claim.wigmoreEvidenceEdges.elementAt(0).getDestVertex();
      // If evidence node is virtual, we have several evidence nodes attached to it
      if (evNode.isVirtual())
      {
        for (Object e : evNode.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex dest = edge.getDestVertex();
          calcBlockHeight(dest);
          if (maxBottom < dest.wigmoreHeight)
          {
            maxBottom = dest.wigmoreHeight;
          }
        }
        // If more than 1 evidence node, need a comb edge to connect
        // them with the claim above them. Allow 2*combHeight to avoid
        // overlap between explanatory/corroborative nodes and the comb lines
        claim.wigmoreHeight = topHeight + maxBottom + minVertSeparation + 2*combHeight;
      } else {      // We have only 1 evidence node
        calcBlockHeight(evNode);
        if (maxBottom < evNode.wigmoreHeight)
        {
          maxBottom = evNode.wigmoreHeight;
        }
        claim.wigmoreHeight = topHeight + maxBottom + minVertSeparation;
      }
      claim.wigmoreBottom.height = maxBottom;
    } else {
      claim.wigmoreHeight = topHeight;
    }
  }
  
  protected void calcTextLayouts(TreeVertex root)
  {
    if (root.isVirtual())
    {
      for (Object e : root.getEdgeList())
      {
        TreeEdge edge = (TreeEdge) e;
        TreeVertex vertex = edge.getDestVertex();
        calcTextLayouts(vertex);
      }
      return;
    }
    calcTextLayout(root);
    calcTextLayoutsForEdges(root.wigmoreCorroborativeEdges);
    calcTextLayoutsForEdges(root.wigmoreEvidenceEdges);
    calcTextLayoutsForEdges(root.wigmoreExplanatoryEdges);
  }
  
  protected void calcTextLayoutsForEdges(Vector<TreeEdge> edgeList)
  {
    for (TreeEdge edge : edgeList)
    {
      TreeVertex vertex = edge.getDestVertex();
      calcTextLayouts(vertex);
    }
  }
  
  public void calcTextLayout(TreeVertex vertex)
  {
    int height = 0;
    int layoutHeight = 0;
    // Get layout for the Wigmore role for this vertex
    // Will need to amend this so that the role labels are readable
   // String role = (String)vertex.roles.get("wigmore");
    // Change this to printing the short label for the vertex instead of the role
    String role = vertex.getShortLabelString();
    if (role != null)
    {
      vertex.roleLayout = calcLayout(role,  boldMap, textWidth - WigmoreRole.wigmoreIconSize.width -
              WigmoreRole.wigmoreIconOffset.width);
      height = getLayoutHeight(vertex.roleLayout);
      layoutHeight += height;
      vertex.roleLayoutSize = new Dimension(textWidth - WigmoreRole.wigmoreIconSize.width -
              WigmoreRole.wigmoreIconOffset.width, height);
    }
    
    // Layout for main text label
    String text = (String)vertex.getLabel();
    vertex.textLayout = calcLayout(text, plainMap);
    height = getLayoutHeight(vertex.textLayout);
    layoutHeight += height;
    vertex.textLayoutSize = new Dimension(textWidth, height);
    vertex.ownersLayout = null;
    vertex.totalLayoutSize = new Dimension(textWidth, layoutHeight);
  }
  
  /**
   * Width of a block is the maximum of:
   * width(left block + node + right block) and
   * width(bottom block)
   */
  int minBlockHorizSpace = 10;  // Minimum horizontal separation between evidence nodes
  int minHorizSeparation = 50;  // Minimum separation between explanatory/corroborative node and its parent
  public void calcBlockWidth(TreeVertex claim)
  {
    // If claim is the dummy node in a split tree, calculate the widths
    // of each of its children separately
    if (claim.isDummy())
    {
      claim.wigmoreWidth = 0;
      claim.wigmoreHeight = 0;
      Vector rootEdges = claim.getEdgeList();
      for (int i = 0; i < rootEdges.size(); i++)
      {
        TreeVertex root = ((TreeEdge)rootEdges.elementAt(i)).getDestVertex();
        calcBlockWidth(root);
        claim.wigmoreWidth += root.wigmoreWidth;
        if (root.wigmoreHeight > claim.wigmoreHeight)
        {
          claim.wigmoreHeight = root.wigmoreHeight;
        }
      }
      return;
    }
    
    if (claim.hidingChildren)
    {
      claim.wigmoreWidth = claim.totalLayoutSize.width + 2 * textBorderMargin;
      return;
    }

    int maxWidth = 0;
    // Calculate widths of explanatory nodes (left side)
    claim.wigmoreWidth = (int)claim.totalLayoutSize.getWidth() + 2 * textBorderMargin;
    if (claim.wigmoreExplanatoryEdges.size() > 1)
    {
      System.out.println("Error: explanatoryEdges contains " + claim.wigmoreExplanatoryEdges.size() + " edges.");
      return;
    } else if (claim.wigmoreExplanatoryEdges.size() == 1)
    {
      TreeVertex exNode = claim.wigmoreExplanatoryEdges.elementAt(0).getDestVertex();
      // If explanatory node is virtual, we have several explanatory nodes attached to it
      if (exNode.isVirtual())
      {
        for (Object e : exNode.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex dest = edge.getDestVertex();
          calcBlockWidth(dest);
          if (dest.wigmoreWidth > maxWidth)
          {
            maxWidth = dest.wigmoreWidth;
          }
        }
        claim.wigmoreLeft.width =  maxWidth + combHeight + minHorizSeparation;
      } else {
        calcBlockWidth(exNode);
        if (exNode.wigmoreWidth > maxWidth)
        {
          maxWidth = exNode.wigmoreWidth;
        }
        claim.wigmoreLeft.width = maxWidth + minHorizSeparation;
      }
    }
     
    claim.wigmoreWidth += claim.wigmoreLeft.width;

      // Calculate widths of corroborative nodes (right side)
    maxWidth = 0;
    if (claim.wigmoreCorroborativeEdges.size() > 1)
    {
      System.out.println("Error: corroborativeEdges contains " + claim.wigmoreCorroborativeEdges.size() + " edges.");
      return;
    } else if (claim.wigmoreCorroborativeEdges.size() == 1)
    {
      TreeVertex corrNode = claim.wigmoreCorroborativeEdges.elementAt(0).getDestVertex();
      // If explanatory node is virtual, we have several explanatory nodes attached to it
      if (corrNode.isVirtual())
      {
        for (Object e : corrNode.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex dest = edge.getDestVertex();
          calcBlockWidth(dest);
          if (dest.wigmoreWidth > maxWidth)
          {
            maxWidth = dest.wigmoreWidth;
          }
        }
        claim.wigmoreRight.width =  maxWidth + combHeight + minHorizSeparation;
      } else {
        calcBlockWidth(corrNode);
        if (corrNode.wigmoreWidth > maxWidth)
        {
          maxWidth = corrNode.wigmoreWidth;
        }
        claim.wigmoreRight.width = maxWidth + minHorizSeparation;
      }
    }
     
    claim.wigmoreWidth += claim.wigmoreRight.width;
//    claim.wigmoreWidth += maxWidth;
//    claim.wigmoreRight.width = maxWidth;
    
    // Calculate widths of evidence nodes (bottom side)
    int bottomWidth = 0;
    if (claim.wigmoreEvidenceEdges.size() > 1)
    {
      System.out.println("Error: evidenceEdges contains " + claim.wigmoreEvidenceEdges.size() + " edges.");
      return;
      // If exactly one evidence edge...
    } else if (claim.wigmoreEvidenceEdges.size() == 1)
    {
      TreeVertex evNode = claim.wigmoreEvidenceEdges.elementAt(0).getDestVertex();
      // If evidence node is virtual, we have several evidence nodes attached to it
      if (evNode.isVirtual())
      {
        for (Object e : evNode.getEdgeList())
        {
          TreeEdge edge = (TreeEdge)e;
          TreeVertex dest = edge.getDestVertex();
          calcBlockWidth(dest);
          bottomWidth += dest.wigmoreWidth;
        }
        // If more than one evidence node, need space between them
        claim.wigmoreWidth += (evNode.getEdgeList().size() - 1) * minBlockHorizSpace;
        bottomWidth += (evNode.getEdgeList().size() - 1) * minBlockHorizSpace;
      } else {      // We have only 1 evidence node
        calcBlockWidth(evNode);
        bottomWidth += evNode.wigmoreWidth;
      }
    }
    
    // Overall width of claim must be calculated in 2 halves, since the bottom portion is centred
    // on the central node, and the left and right supports may not be symmetrical. Compare left part with
    // half of bottom, then right part with half of bottom
    int leftWidth = claim.wigmoreLeft.width + claim.totalLayoutSize.width / 2 + textBorderMargin;
    int rightWidth = claim.wigmoreRight.width + claim.totalLayoutSize.width / 2 + textBorderMargin;
    claim.wigmoreWidth = leftWidth > bottomWidth / 2 ? leftWidth : bottomWidth / 2;
    claim.wigmoreWidth += rightWidth > bottomWidth / 2 ? rightWidth : bottomWidth / 2;
    claim.wigmoreBottom.width = bottomWidth;
}
  
  public void redrawTree(boolean doRepaint)
  {
    initializeDrawing(true);
    repaint();
  }
  
  public void leftMouseReleased(MouseEvent e)
  {
    super.leftMouseReleased(e);
    initializeDrawing(true);
    araucaria.updateDisplays(true, false);
  } 
  
  public TreeEdge testEdgeShapesPopup(MouseEvent event)
  {
    double x = event.getX(); 
    double y = event.getY();
    Enumeration nodeList = argument.getTree().getVertexList().elements();
    TreeEdge selectedEdge = null;
    while (nodeList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeList.nextElement();
      if (argument.isMultiRoots() && vertex.getLayer() == 0)
      	continue;
      TreeEdge edge = testEdgeList(x, y, vertex.wigmoreEvidenceEdges);
      if (edge == null)
      {
        edge = testEdgeList(x, y, vertex.wigmoreCorroborativeEdges);
      }
      if (edge == null)
      {
        edge = testEdgeList(x, y, vertex.wigmoreExplanatoryEdges);
      }
      if (edge != null)
      {
        selectedEdge = edge;
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
//      translateEdgeToStandard(selectedEdge);
      createPopupMenu();
      evidencePopup.show(event.getComponent(), event.getX(), event.getY());
      return selectedEdge;
    }
    // If we get here, no edge was selected  
    return null;
  }
  
  JPopupMenu evidencePopup;
  JMenuItem wigmoreForce = new JMenuItem("Wigmore force...");
  String[] evidenceForce = { "Provisional", "Strong", 
          "Weak inference", "No inference",
          "Strong inference", "Conclusive", "Doubt"};
  String[] explanatoryForce = {  "Detracts", "Strongly detracts" };
  String[] corroborativeForce = {  "Supports", "Strongly supports"};
  String[] forceChoices = null;
  boolean negatoryVisible = false, forcedNegatory = false;
  LabelDialog.EdgeType edgeType = null;
 
protected void createPopupMenu()
  {
    if (mouseEdge == null) return;
    mouseVertex = mouseEdge.getDestVertex();
    String destRole = (String)mouseVertex.roles.get("wigmore");
    forcedNegatory = false;
    if (mouseVertex.isVirtual())
    {
      destRole = (String)((TreeEdge)mouseVertex.getEdgeList().elementAt(0)).getDestVertex().roles.get("wigmore");
    }
    if (destRole.indexOf("evidence") != -1)
    {
      forceChoices = evidenceForce;
      negatoryVisible = true;
      if (mouseVertex.hasAddedNegationExtraSupport())
      {
        forcedNegatory = true;
      }
      edgeType = LabelDialog.EdgeType.WIGMORE_EVIDENCE;
    } else if (destRole.indexOf("explanatory") != -1)
    {
      forceChoices = explanatoryForce;
      negatoryVisible = false;
      edgeType = LabelDialog.EdgeType.WIGMORE_EXPLANATORY;
    } else if (destRole.indexOf("corroborative") != -1)
    {
      forceChoices = corroborativeForce;
      negatoryVisible = false;
      edgeType = LabelDialog.EdgeType.WIGMORE_CORROBORATIVE;
    }
  }
  
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
      edge = testEdgeList(x, y, vertex.wigmoreEvidenceEdges);
      if (edge == null)
        edge = testEdgeList(x, y, vertex.wigmoreExplanatoryEdges);
      if (edge == null)
        edge = testEdgeList(x, y, vertex.wigmoreCorroborativeEdges);
      if (edge != null) 
      {
        return edge;
      }
    }
    return null;
  }
  
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
        TreeVertex dest = edge.getDestVertex();
        String labelText;
        if (dest.isVirtual())
        {
          labelText = "(aggregate)";
        } else {
          labelText = "\"" + dest.getLabel().toString() + "\"";
        }
        TreeVertex source = edge.getSourceVertex();
        if (source.isVirtual())
        {
          source = source.getParent();
        }
        String supports = "\"" + source.getLabel().toString() + "\"";
        labelText = prepareMessageLabel(labelText, supports, MAX_MESSAGELABEL_SIZE); 
        araucaria.setMessageLabelText(labelText);
        return edge;
      }
    }
    // If dataEdges consists of a single virtual node, test edge list
    // from this node
    if (dataEdges.size() == 1)
    {
      TreeVertex dest = ((TreeEdge)dataEdges.elementAt(0)).getDestVertex();
      if (dest.isVirtual())
      {
        return testEdgeList(x, y, dest.getEdgeList());
      }
    }
    return null;
  }
  
  Hashtable<String, WigmoreRole> roleTable;
  String[] roles = {"evidenceTestAffirm", "evidenceTestAffirmDef", "evidenceTestNeg",
    "evidenceTestNegDef", "evidenceCircumAffirm", "evidenceCircumAffirmDef",
    "evidenceCircumNeg", "evidenceCircumNegDef", "explanatory", "explanatoryDef",
    "corroborative", "corroborativeDef"};
  WigmoreRole[] wigRoles;
  public void buildRoleHashtable()
  {
    roleTable = new Hashtable<String, WigmoreRole>();
    for (String role : roles)
    {
      roleTable.put(role, new WigmoreRole(role, WigmoreRole.wigmoreIconSize, this));
    }
    
    // Sort out the roles into an array
    // Order must be correct so menu items appear in correct order
    Collection<WigmoreRole> wigColl = roleTable.values();
    wigRoles = new WigmoreRole[wigColl.size()];
    for (WigmoreRole role : wigColl)
    {
      wigRoles[role.sortOrder - 1] = role;
    }
  }

  JMenu wigmoreTypeMenu;
  JMenuItem editVertexIDMenu;
  JMenu wigmoreFactMenu = new JMenu("Wigmore fact");
  JMenuItem[] factMenuItems = {new JMenuItem("None"), 
          new JMenuItem("\u221E Judicial"), 
          new JMenuItem("\u00B6 Tribunal")
  };
  String[] factRoleStrings = { "none", "judicial", "tribunal" };
  
  JMenuItem wigmoreBelief;
  JMenuItem hideShowNegation;
  public void buildPopup(TreeVertex vertex)
  {
    vertexPopup = new JPopupMenu();
    editVertexIDMenu = new JMenuItem("Edit ID...");
    editVertexIDMenu.addActionListener(this);
    vertexPopup.add(editVertexIDMenu);
    wigmoreBelief = new JMenuItem("Wigmore belief...");
    wigmoreBelief.addActionListener(this);
    vertexPopup.add(wigmoreBelief);
    
    if (vertex.hasAddedNegationNoSupport())
    {
      if (vertex.getParent().isHiddenTable.get("wigmore").equals("true"))
      {
        hideShowNegation = new JMenuItem("Show negation");
      } else {
        hideShowNegation = new JMenuItem("Hide negation");
      }
      hideShowNegation.addActionListener(this);
      vertexPopup.add(hideShowNegation);
    }
    
    if (vertex.isWigmoreLeaf())
    {
      vertexPopup.add(wigmoreFactMenu);
      for (int i = 0; i < factMenuItems.length; i++)
      {
        if (factMenuItems[i].getActionListeners().length == 0)
        {
          factMenuItems[i].addActionListener(this);
        }
      }
      String vertexWigFact = (String)vertex.roles.get("wigmoreFact");
      wigmoreFactMenu.removeAll();
      for (int fact = 0; fact < factMenuItems.length; fact++)
      {
        if (!vertexWigFact.equals(factRoleStrings[fact]))
        {
          wigmoreFactMenu.add(factMenuItems[fact]);
        }
      }
    }
    
    String className = getClass().getName();
    wigmoreTypeMenu = new JMenu("Wigmore role");
    int wigmoreTypeMembers = 0;
    String vertexRole = (String)vertex.roles.get("wigmore");
    for (WigmoreRole role : wigRoles)
    {
      if (!vertexRole.equals(role.roleLabel))
      {
        // Prohibit explanatory nodes from having corroborative support, and vice versa
        TreeVertex parent = vertex.getParent();
        if (parent != null)
        {
          // Vertexes with negatory force can only change type
          // if they have an added negation parent with no additional support
          if (vertex.isRefutation())
          {
            if (!vertex.hasAddedNegationNoSupport() && (role.roleLabel.toLowerCase().indexOf("corroborative") != -1
                    || role.roleLabel.toLowerCase().indexOf("explanatory") != -1))
            {
              continue;
            }
          }
          if (parent.isVirtual())
          {
            parent = parent.getParent();
          }
          String parentRole = (String)parent.roles.get("wigmore");
          if (parentRole.toLowerCase().indexOf("explanatory") != -1 && role.roleLabel.toLowerCase().indexOf("corroborative") != -1)
          {
            continue;
          }
          if (parentRole.toLowerCase().indexOf("corroborative") != -1 && role.roleLabel.toLowerCase().indexOf("explanatory") != -1)
          {
            continue;
          }
        } else {  // root nodes cannot be converted to corroborative or explanatory
          if (role.roleLabel.toLowerCase().indexOf("corroborative") != -1 || 
                  role.roleLabel.toLowerCase().indexOf("explanatory") != -1)
          {
            continue;
          }
        }
        wigmoreTypeMenu.add(role.roleMenu);
        wigmoreTypeMembers ++;
      }
    }
    vertexPopup.add(wigmoreTypeMenu);
    setPremisesVisMenu = new JMenuItem("Collapse premises");
    if (vertex.hidingChildren)
    {
      setPremisesVisMenu.setText("Expand premises");
    }
    if (vertex.getEdgeList().size() > 0)
    {
      vertexPopup.add(setPremisesVisMenu);
    }
    setPremisesVisMenu.addActionListener(this);
  }
  
  public WigmoreRole getRoleFromMenu(JMenuItem menu)
  {
    for (WigmoreRole role : wigRoles)
    {
      if (role.roleMenu == menu)
      {
        return role;
      }
    }
    return null;
  }
  
  public WigmoreRole getRoleFromLabel(String label)
  {
    for (WigmoreRole role : wigRoles)
    {
      if (role.roleLabel.equals(label))
      {
        return role;
      }
    }
    return null;
  }
  
  public void doWigmoreHideShowNegation()
  {
    if (mouseVertex.isRefutation() && mouseVertex.getParent().roles.get("addedNegation") != null
            && mouseVertex.getParent().roles.get("addedNegation").equals("yes"))
    {
      mouseVertex.getParent().isHiddenTable.put("wigmore", mouseVertex.getParent().isHiddenTable.get("wigmore").equals("true") ?
        "false" : "true");
      argument.standardToWigmore();
      initializeDrawing(true);
      araucaria.updateDisplays(true);
    }
  }
    
  public void actionPerformed(ActionEvent e)
  {
    JMenuItem menuItem = (JMenuItem)e.getSource();
    if (e.getSource() == editVertexIDMenu) {
      editVertexID();
    } else if (e.getSource() == wigmoreBelief) {
      araucaria.doWigmoreNodeBelief();
    } else if (e.getSource() == wigmoreForce) {
      araucaria.doWigmoreEdgeForce(forceChoices, negatoryVisible, forcedNegatory, edgeType);
    } else if (e.getSource() == hideShowNegation) {
      doWigmoreHideShowNegation();
    } else if (e.getSource() == setPremisesVisMenu) {
      showHidePremises();
    } else {
      // If Wigmore fact type is being changed...
      for (int i = 0; i < factMenuItems.length; i++)
      {
        if (e.getSource() == factMenuItems[i])
        {
          mouseVertex.roles.put("wigmoreFact", factRoleStrings[i]);
          displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "changing Wigmore fact type"));
          araucaria.doUndo(false, false); 
          araucaria.doRedo(false);
          araucaria.updateDisplays(true);
          return;
        }
      }
            
      // Wigmore role change...
      WigmoreRole wigRole = getRoleFromMenu(menuItem);
      WigmoreRole mouseRole = getRoleFromLabel((String)mouseVertex.roles.get("wigmore"));
      if (mouseRole.group != wigRole.group)
      {
        changeRole(mouseRole, wigRole, mouseVertex);
      }
      // Assign owners in standard
      if (wigRole.isDefence())
      {
        Vector ownerVec = new Vector();
        Vector defVec = new Vector();
        defVec.add("Defense"); defVec.add("Def");
        ownerVec.add(defVec);
        mouseVertex.getOwners().addAll(ownerVec);
      } else {
        Vector defVec = argument.getOwner("Def");
        if (defVec != null)
        {
          mouseVertex.getOwners().remove(defVec);
        }
      }
      mouseVertex.roles.put("wigmore", wigRole.roleLabel);
      displayFrame.controlFrame.getUndoStack().push(new EditAction(araucaria, "changing Wigmore role"));
      araucaria.doUndo(false, false); 
      araucaria.doRedo(false);
      araucaria.updateDisplays(true);
    }
  }
  
  public void changeRole(WigmoreRole oldRole, WigmoreRole newRole, TreeVertex vertex)
  {
    // If vertex is changing role type (e.g. evidence --> corroborative, etc), then
    // remove its force tag since different types have different forces
    // Also need to check if node has an added negation as a parent - if so, remove it first
    if (oldRole.group != newRole.group)
    {
      vertex.setSupportLabel(null);
      if (vertex.hasAddedNegationNoSupport())
      {
        TreeVertex addedNegation = vertex.getParent();
        TreeVertex negationParent = addedNegation.getParent();
        negationParent.deleteEdge(addedNegation);
        negationParent.addEdge(vertex);
        argument.getTree().getVertexList().remove(addedNegation);
        vertex.setParent(negationParent);
        vertex.setRefutation(false);
      }
    }
    TreeVertex parent = vertex.getParent();
    if (parent.isVirtual()) parent = parent.getParent();  // If vertex is part of an LA in standard
    TreeVertex virtual = null;
    Vector oldEdgeList = null, newEdgeList = null, origOldEdgeList;
    switch (oldRole.group)
    {
      case GENERAL:
        oldEdgeList =  parent.wigmoreEvidenceEdges;
        break;
      case CORROBORATIVE:
        oldEdgeList = parent.wigmoreCorroborativeEdges;
        break;
      case EXPLANATORY:
        oldEdgeList = parent.wigmoreExplanatoryEdges;
        break;
    }
    origOldEdgeList = oldEdgeList;
    TreeVertex listHead = ((TreeEdge)oldEdgeList.elementAt(0)).getDestVertex();
    if (listHead.isVirtual())
    {
      virtual = listHead;
      oldEdgeList = virtual.getEdgeList();
    }
    
    switch (newRole.group)
    {
      case GENERAL:
        newEdgeList = parent.wigmoreEvidenceEdges;
        break;
      case CORROBORATIVE:
        newEdgeList = parent.wigmoreCorroborativeEdges;
        break;
      case EXPLANATORY:
        newEdgeList = parent.wigmoreExplanatoryEdges;
        break;
    }
    
    Tree tree = araucaria.argument.getTree();
    // Delete vertex from old list
    TreeVertex source = virtual == null ? parent : virtual;
    boolean removed = oldEdgeList.remove(tree.getEdge(source, vertex));
    
    // If removing the vertex from the old list leaves that list with
    // only one remaining vertex, need to remove the virtual node above it
    if (listHead.isVirtual())
    {
      Vector headEdges = listHead.getEdgeList();
      if (headEdges.size() == 1)
      {
        TreeVertex oneVertex = ((TreeEdge)headEdges.elementAt(0)).getDestVertex();
        origOldEdgeList.clear();
        origOldEdgeList.add(new TreeEdge(parent, oneVertex));
      }
    }
    
    // Add vertex to new list 
    if (newEdgeList.size() == 0)
    {
      newEdgeList.add(new TreeEdge(parent, vertex));
    } else if (newEdgeList.size() == 1)
    {
      TreeVertex node = ((TreeEdge)newEdgeList.elementAt(0)).getDestVertex();
      if (node.isVirtual())
      {
        node.getEdgeList().add(new TreeEdge(parent, vertex));
      } else {
        // If there is only one non-virtual node, insert
        // a virtual node and add both real nodes to it
        TreeVertex newVirt = new TreeVertex("virtual");
        tree.addVertex(newVirt);
        newVirt.setVirtual(true);
        newVirt.setParent(parent);
        newVirt.getEdgeList().add(new TreeEdge(newVirt, node));
        newVirt.getEdgeList().add(new TreeEdge(newVirt, vertex));
        newEdgeList.remove(tree.getEdge(parent, node));
        newEdgeList.add(new TreeEdge(parent, newVirt));
        node.setParent(newVirt);
        vertex.setParent(newVirt);
      }
    } else {
      System.out.println("Error in Wigmore changeRole: should never have more than 1 node in edge list");
    }
  }
}
