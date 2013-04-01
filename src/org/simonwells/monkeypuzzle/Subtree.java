package org.simonwells.monkeypuzzle;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;

public class Subtree
{
  Vector m_edgeList;
  Hashtable shapeTable = new Hashtable();
  Hashtable internalShapeTable = new Hashtable();
  Hashtable cqBoxTable = new Hashtable();
  boolean m_selected;
  Color m_fillColor, m_outlineColor;
  ArgType m_argumentType = null;
  String m_shortLabel;    // Used only for reading in XML files
  Vector m_vertexList;  // List of vertices in the subtree - used for reading XML
  TreeVertex m_root;
  private Vector<CQCheck> cqChecks;
  
  public static Color[] m_subtreeColors;
  {
    m_subtreeColors = new Color[6];
    m_subtreeColors[0] = new Color(1.0f, 0.9f, 0.9f);
    m_subtreeColors[1] = new Color(0.9f, 1.0f, 0.9f);
    m_subtreeColors[2] = new Color(0.9f, 0.9f, 1.0f);
    m_subtreeColors[3] = new Color(1f, 1f, 0.7f);
    m_subtreeColors[4] = new Color(1.0f, 0.9f, 1.0f);
    m_subtreeColors[5] = new Color(0.9f, 1.0f, 1.0f);
  }

  public Subtree()
  {
    m_edgeList = new Vector();
    m_vertexList = new Vector();
    m_selected = false;
    m_shortLabel = "-";
    cqChecks = new Vector<CQCheck>();
  }

  public void setShortLabel(String label)
  { m_shortLabel = label; }

  public String getShortLabel()
  { return m_shortLabel; }

  public void addVertex(TreeVertex vertex)
  { m_vertexList.add(vertex); }
  
  public void setRoot(TreeVertex r)
  { m_root = r; }

  public TreeVertex findRoot()
  {
    if (m_root != null) return m_root;
    int rootLayer = 1000;
    TreeVertex root = null;
    for (int i = 0; i < m_vertexList.size(); i++) {
      TreeVertex vertex = (TreeVertex)m_vertexList.elementAt(i);
      if (vertex.getLayer() < rootLayer) {
        rootLayer = vertex.getLayer();
        root = vertex;
      }
    }
    return root;
  }

  private void recurseSelectEdges(TreeVertex root)
  {
     Vector edges = root.getEdgeList();
     for (int i = 0; i < edges.size(); i++) {
      TreeEdge edge = (TreeEdge)edges.elementAt(i);
      TreeVertex dest = edge.getDestVertex();
      if (m_vertexList.contains(dest)) {
        edge.setSelected(true);
        recurseSelectEdges(dest);
      }
     }
  }

  public void selectEdges()
  {
    TreeVertex root = findRoot();
    if (root == null) return;
    recurseSelectEdges(root);
  }

  public void addEdge(TreeEdge edge)
  {
    m_edgeList.add(edge);
  }

  public Vector getEdgeList()
  { return m_edgeList; }

  public Vector getVertexList()
  { return m_vertexList; }
  /**
   * Deletes edge from m_edgeList.
   * Returns true if edge found and deleted, false otherwise
   */
  public boolean deleteEdge(TreeEdge edge)
  {
    return m_edgeList.remove(edge);
  }

  /**
   * Returns the number of edges in the subtree
   */
   public int getNumberOfEdges()
   {
      return m_edgeList.size();
   }

   /**
    * Determines if this subtree contains the given vertex
    */
    public boolean containsTreeVertex(TreeVertex vertex)
    {
      if (vertex == null) return false;
      for (int i = 0; i < m_vertexList.size(); i++) {
        if ((TreeVertex)m_vertexList.elementAt(i) == vertex)
          return true;
      }
      for (int i = 0; i < m_edgeList.size(); i++) {
        TreeEdge edge = (TreeEdge)m_edgeList.elementAt(i);
        if (edge.getSourceVertex() == vertex ||
          edge.getDestVertex() == vertex)  {
          return true;
        }
      }
      return false;
    }
    
  /**
   * Build a shape for the entire subtree by joining together
   * the shapes for each of its edges.
   */
  public Shape constructShape(DiagramBase diagram)
  {
    GeneralPath shape = new GeneralPath();
    Enumeration edges = m_edgeList.elements();
    while (edges.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edges.nextElement();
      if (!edge.visible)
      {
        continue;
      }
      Shape edgeShape = edge.getSchemeShape(diagram);
      PathIterator path = edgeShape.getPathIterator(null);
      shape.append(path, false);
    }
    BasicStroke stroke = new BasicStroke(diagram.getSubtreeLineWidth(), BasicStroke.CAP_ROUND,
           BasicStroke.JOIN_MITER);
    shapeTable.put(diagram, stroke.createStrokedShape(shape));
    return (Shape)shapeTable.get(diagram);
  }

  /**
   * Build a shape for the entire subtree by joining together
   * the shapes for each of its edges. Vertices included since needed for
   * FullTextPanel.
   */
  public Shape constructInternalShape(DiagramBase diagram, boolean includeVertices)
  {
    GeneralPath shape = new GeneralPath();
    Enumeration edges = m_edgeList.elements();
    while (edges.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edges.nextElement();
      Shape edgeShape = edge.getSchemeShape(diagram);
      PathIterator path = edgeShape.getPathIterator(null);
      shape.append(path, false);
      
      if (includeVertices)
      {
        Shape vertexShape;
        if (!edge.getSourceVertex().isVirtual())
        {
          vertexShape = edge.getSourceVertex().getShape(diagram);
          path = vertexShape.getPathIterator(null);
          shape.append(path, false);
        }
        if (!edge.getDestVertex().isVirtual())
        {
          vertexShape = edge.getDestVertex().getShape(diagram);
          path = vertexShape.getPathIterator(null);
          shape.append(path, false);
        }
      }
    }
    BasicStroke stroke = new BasicStroke(diagram.getSubtreeLineWidth() - DiagramBase.EDGE_OUTLINE_WIDTH + 1, 
       BasicStroke.CAP_ROUND, 
       BasicStroke.JOIN_MITER);
    internalShapeTable.put(diagram, stroke.createStrokedShape(shape));
    return (Shape)internalShapeTable.get(diagram);
  }

  /**
   * Build a shape for the entire subtree by joining together
   * the shapes for each of its edges.
   */
  public Shape constructFullTextShape(DiagramBase diagram)
  {
    GeneralPath shape = new GeneralPath();
    Enumeration edges = m_edgeList.elements();
    while (edges.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edges.nextElement();
      if (!edge.visible)
      {
        continue;
      }
      Shape edgeShape = edge.getSchemeShape(diagram);
      PathIterator path = edgeShape.getPathIterator(null);
      shape.append(path, false);
      TreeVertex vertex = edge.getSourceVertex();
      if (!vertex.isVirtual()) {
        shape.append(vertex.getShape(diagram).getPathIterator(null), false);
      }
      vertex = edge.getDestVertex();
      if (!vertex.isVirtual()) {
        shape.append(vertex.getShape(diagram).getPathIterator(null), false);
      }
    }
    BasicStroke stroke = new BasicStroke(20, BasicStroke.CAP_ROUND,
           BasicStroke.JOIN_MITER);
    shapeTable.put(diagram, stroke.createStrokedShape(shape));
    return (Shape)shapeTable.get(diagram);
  }

  public Shape getShape(DiagramBase diagram)
  { return (Shape)shapeTable.get(diagram); }

  public Shape getInternalShape(DiagramBase diagram)
  { return (Shape)internalShapeTable.get(diagram); }
  
  public Shape getCqBoxShape(DiagramBase diagram)
  { return (Shape)cqBoxTable.get(diagram); }

  public String getInfo()
  {
    String info = "Edges: ";
    Enumeration edges = m_edgeList.elements();
    while (edges.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edges.nextElement();
      info += edge.getSourceVertex().getLabel() + " => " +
      	edge.getDestVertex().getLabel() + "; ";
    }
    return info;
  }

  /**
   * Fills a SubtreeFrame with info about the subtree.
   * Used to display info when user right clicks on an
   * existing subtree.
   */
  /* TODO: Restore link to SubtreeFrame
  public void buildSubtreeFrame(SubtreeFrame sFrame)
  {
    // Clear visited flag on all vertices in the tree
    // and determine the layer number of the root node
    int highestLayer = 100;
    Enumeration edges = m_edgeList.elements();
    while (edges.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edges.nextElement();
      edge.getDestVertex().setVisited(false);
      edge.getSourceVertex().setVisited(false);
      if (edge.getSourceVertex().getLayer() < highestLayer) {
        highestLayer = edge.getSourceVertex().getLayer();
      }
    }
    // Create list of premises and conclusion and assign
    // to text areas in the dialog
    Vector premiseList = new Vector();
    String conclusion = "";
    String criticalQuestions = "";
    edges = m_edgeList.elements();
    while (edges.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edges.nextElement();
      TreeVertex source = edge.getSourceVertex();
      TreeVertex dest = edge.getDestVertex();
      if (!source.getVisited()) {
        if (source.getLayer() == highestLayer) {
          conclusion += (String)source.getLabel();
        } else {
          premiseList.add((String)source.getLabel());
        }
        source.setVisited(true);
      }
      if (!dest.getVisited()) {
        premiseList.add((String)dest.getLabel());
        dest.setVisited(true);
      }
    }
    sFrame.setPremisesText(premiseList);
    sFrame.setConclusionText(conclusion);
    if (!sFrame.getApplet()) {
      sFrame.loadArgTypeCombo();
    }
    // Assign the correct argument type to the combo box
    for (int index = 0; index < sFrame.selectArgumentCombo.getItemCount();
      index++) {
      if (m_argumentType.getName().equals(sFrame.selectArgumentCombo.getItemAt(index))) {
        sFrame.selectArgumentCombo.setSelectedIndex(index);
        sFrame.selectArgumentCombo.setEditable(false);
        break;
      }
    }
    // Construct list of critical questions
//    Enumeration quesList = m_argumentType.getCriticalQuestions().elements();
//    while (quesList.hasMoreElements()) {
//      criticalQuestions += (String)quesList.nextElement() +
//          "\r\n _____________________________ \r\n";
 //   }
    sFrame.updateTextBoxes();
  }
*/
  public boolean isSelected()
  { return m_selected; }

  public void setSelected(boolean selected)
  { m_selected = selected; }

  /**
   * Sets the fillColor and defines the outline color
   * to be a non-transparent version of the fill color
   */
  public void setColor(Color fillColor)
  {
    m_fillColor = fillColor;
    m_outlineColor = new Color(
      fillColor.getRed() < 255 ? 0.0f : 1.0f, 
      fillColor.getGreen() < 255 ? 0.0f : 1.0f, 
      fillColor.getBlue() < 255 ? 0.0f : 1.0f);
  }

  public void setColor(Color fillColor, Color outlineColor)
  {
    m_fillColor = fillColor;
    m_outlineColor = outlineColor;
  }

  public Color getFillColor()
  { return m_fillColor; }

  public Color getOutlineColor()
  { return m_outlineColor; }

  public void setArgumentType(ArgType argType)
  { 
    m_argumentType = argType;
    setCqChecks(new Vector<CQCheck>());
    for (Object cq : argType.criticalQuestions)
    {
      getCqChecks().add(new CQCheck((String)cq, false));
    }
  }

  public ArgType getArgumentType()
  { return m_argumentType; }

  /**
   * Fills a SubtreeFrame with info about the subtree.
   * Used to display info when user right clicks on an
   * existing subtree.
   */
  public void buildSubtreeFrame(SubtreeFrame sFrame)
  {
    // Clear visited flag on all vertices in the tree
    // and determine the layer number of the root node
    int highestLayer = 100;
    Enumeration edges = m_edgeList.elements();
    while (edges.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edges.nextElement();
      edge.getDestVertex().setVisited(false);
      edge.getSourceVertex().setVisited(false);
      if (edge.getSourceVertex().getLayer() < highestLayer) {
        highestLayer = edge.getSourceVertex().getLayer();
      }
    }
    // Create list of premises and conclusion and assign
    // to text areas in the dialog
    Vector premiseList = new Vector();
    String conclusion = "";
    String criticalQuestions = "";
    edges = m_edgeList.elements();
    while (edges.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edges.nextElement();
      TreeVertex source = edge.getSourceVertex();
      TreeVertex dest = edge.getDestVertex();
      if (!source.getVisited()) {
        if (source.getLayer() == highestLayer) {
          conclusion += (String)source.getLabel();
        } else {
          premiseList.add((String)source.getLabel());
        }
        source.setVisited(true);
      }
      if (!dest.getVisited()) {
        premiseList.add((String)dest.getLabel());
        dest.setVisited(true);
      }
    }
    sFrame.setPremisesText(premiseList);
    sFrame.setConclusionText(conclusion);
    sFrame.loadArgTypeCombo();
    // Assign the correct argument type to the combo box
    for (int index = 0; index < sFrame.selectArgumentCombo.getItemCount();
      index++) {
      if (m_argumentType.getName().equals(sFrame.selectArgumentCombo.getItemAt(index))) {
        sFrame.selectArgumentCombo.setSelectedIndex(index);
        sFrame.selectArgumentCombo.setEditable(false);
        break;
      }
    }
    // Construct list of critical questions
//    Enumeration quesList = m_argumentType.getCriticalQuestions().elements();
//    while (quesList.hasMoreElements()) {
//      criticalQuestions += (String)quesList.nextElement() +
//          "\r\n _____________________________ \r\n";
 //   }
    sFrame.updateTextBoxes();
  }

  public Vector<CQCheck> getCqChecks()
  {
    return cqChecks;
  }
  
  public int getNumCqsAnswered()
  {
    int num = 0;
    for (CQCheck cq : getCqChecks())
    {
      if (cq.isCqAnswered()) num++;
    }
    return num;
  }

  public void setCqChecks(Vector<CQCheck> cqChecks)
  {
    this.cqChecks = cqChecks;
  }
  
  public CQCheck getCQCheckByText(String text)
  {
    for (CQCheck cQCheck : cqChecks)
    {
      if (cQCheck.getCqText().equals(text))
      {
        return cQCheck;
      }
    }
    return null;
  }

} // Subtree
