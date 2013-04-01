import java.io.*;
import java.awt.*;
import java.util.*;

/**
 *
 */

public class TreeEdge  implements Serializable {
  TreeVertex m_sourceVertex, m_destVertex;
  double m_weight;
  boolean m_visited;
  boolean m_selected;
  Hashtable shapeTable = new Hashtable();   // Hashtable for storing diagram shapes for various diagrams
  Hashtable schemeShapeTable = new Hashtable(); // An outline of the edge without the arrowhead, used for schemes
  public int undoOrder;  // Number the edges coming out of a vertex so they can be undone in the same order
  public boolean visible = true;

  /**
   * Creates an Edge leading from sourceVertex to destVertex.
   * The edge has a weight of zero.
   * @param sourceVertex The start of the Edge.
   * @param destVertex The end of the Edge.
   */
  public TreeEdge(TreeVertex sourceVertex, TreeVertex destVertex)
  {
    initialize(sourceVertex, destVertex);
  }

  /**
   * Creates an TreeEdge leading from sourceVertex to destVertex.
   * The edge has the weight specified.
   * @param sourceVertex The start of the Edge.
   * @param destVertex The end of the Edge.
   * @param weight The weight of the Edge.
   */
  public TreeEdge(TreeVertex sourceVertex, TreeVertex destVertex,
	      double weight)
  {
    initialize(sourceVertex, destVertex);
    m_weight = weight;
  }

  protected void initialize(TreeVertex sourceVertex, TreeVertex destVertex)
  {
    m_sourceVertex = sourceVertex;
    m_destVertex = destVertex;
    m_weight = 0.0;
    m_visited = false;
    m_selected = false;
  }

  public TreeVertex getDestVertex()
  {
    return m_destVertex;
  }

  public TreeVertex getSourceVertex()
  {
    return m_sourceVertex;
  }

  public void setDestVertex(TreeVertex dest)
  { m_destVertex = dest; }

  public void setSourceVertex(TreeVertex source)
  { m_sourceVertex = source; }

  public double getWeight()
  { return m_weight; }

  public void setWeight(double weight)
  { m_weight = weight; }

  public boolean isVisited()
  { return m_visited; }

  public void setVisited(boolean visited)
  { m_visited = visited; }

  public Shape getShape(DiagramBase diagram)
  { return (Shape)shapeTable.get(diagram); }

  public void setShape(Shape shape, DiagramBase diagram)
  { shapeTable.put(diagram, shape); }

  public Shape getSchemeShape(DiagramBase diagram)
  { return (Shape)schemeShapeTable.get(diagram); }

  public void setSchemeShape(Shape shape, DiagramBase diagram)
  { schemeShapeTable.put(diagram, shape); }

  public void setSelected(boolean selected)
  { m_selected = selected; }

  public boolean isSelected()
  { return m_selected; }

  public String printEdge()
  {
    String result = m_sourceVertex.getLabel().toString();
    result += " --> " + m_destVertex.getLabel().toString();
    return result;
  }

  public String printEdge(boolean printWeight)
  {
    String result = printEdge();
    if(printWeight) {
      result += "<" + m_weight + ">";
    }
    return result;
  }

} // TreeEdge
