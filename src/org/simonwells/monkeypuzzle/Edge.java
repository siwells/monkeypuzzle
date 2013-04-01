package org.simonwells.monkeypuzzle;

import java.io.*;

/**
 * Represents an Edge connecting two Vertex objects in a Graph object.
 */
public class Edge implements Serializable
{
  Vertex m_sourceVertex, m_destVertex;
  double m_weight;
  boolean m_visited;

  /**
   * Creates an Edge leading from sourceVertex to destVertex.
   * The edge has a weight of zero.
   * @param sourceVertex The start of the Edge.
   * @param destVertex The end of the Edge.
   */
  public Edge(Vertex sourceVertex, Vertex destVertex)
  {
    initialize(sourceVertex, destVertex);
  }

  /**
   * Creates an Edge leading from sourceVertex to destVertex.
   * The edge has the weight specified.
   * @param sourceVertex The start of the Edge.
   * @param destVertex The end of the Edge.
   * @param weight The weight of the Edge.
   */
  public Edge(Vertex sourceVertex, Vertex destVertex,
	      double weight)
  {
    initialize(sourceVertex, destVertex);
    m_weight = weight;
  }

  protected void initialize(Vertex sourceVertex, Vertex destVertex)
  {
    m_sourceVertex = sourceVertex;
    m_destVertex = destVertex;
    m_weight = 0.0;
    m_visited = false;
  }

  public Vertex getDestVertex()
  {
    return m_destVertex;
  }

  public Vertex getSourceVertex()
  {
    return m_sourceVertex;
  }

  public void setDestVertex(Vertex dest)
  { m_destVertex = dest; }

  public void setSourceVertex(Vertex source)
  { m_sourceVertex = source; }

  public double getWeight()
  { return m_weight; }

  public void setWeight(double weight)
  { m_weight = weight; }

  public boolean isVisited()
  { return m_visited; }

  public void setVisited(boolean visited)
  { m_visited = visited; }

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
}
