package org.simonwells.monkeypuzzle;

import java.util.*;
import java.io.*;

/**
 * Represents a Vertex in a Graph object
 */
public class Vertex implements Serializable
{
  Object m_label;
  Vector m_edgeList;
  boolean m_visited;
  double m_distance;            // Distance from source in Dijkstra's alg
  int m_predecessorCount;	// Number of edges leading to vertex

  /**
   * Creates a Vertex with the specified label.
   * @param label An Object (or derivative) to be used to label this vertex.
   * Vertex labels need not be unique within a graph.
   */
  public Vertex(Object label)
  {
    m_label = label;
    m_visited = false;
    m_edgeList = new Vector(10, 10);
    m_distance = 0.0;
    m_predecessorCount = 0;
  }

  /**
   * Creates a clone of the Vertex object but with an empty edge list.
   * Used in Kruskal's algorithm, but may be used elsewhere if a Vertex
   * must be copied to a different Graph, but with different edge connections.
   * @return The cloned Vertex.
   */
  public Vertex noEdgesCopy()
  {
    Vertex copy = new Vertex(m_label);
    copy.m_visited = m_visited;
    copy.m_edgeList = new Vector(5,5);
    copy.m_distance = m_distance;
    return copy;
  }

  /**
   * Adds an edge (with no weight) from the current Vertex to destVertex.
   * Checks if an edge to destVertex already exists,
   * and if not, adds an edge to destVertex. You aren't allowed to
   * have more than one edge from one vertex to another.
   * @param destVertex The destination Vertex for the edge.
   * @return true if the edge was added, false if an edge already exists
   * between these two vertices.
   */
  public boolean addEdge(Vertex destVertex)
  {
    if (edgeExists(destVertex) != null)
	return false;
    Edge newEdge = new Edge(this, destVertex);
    m_edgeList.add(newEdge);
    return true;
  }

  /**
   * Adds an edge with specified weight from the current Vertex to destVertex.
   * Checks if an edge to destVertex already exists,
   * and if not, adds an edge to destVertex. You aren't allowed to
   * have more than one edge from one vertex to another.
   * @param destVertex The destination Vertex for the edge.
   * @param weight The weight of the edge to be added.
   * @return true if the edge was added, false if an edge already exists
   * between these two vertices.
   */
  public void addEdge(Vertex destVertex, double weight)
  {
    if (edgeExists(destVertex) != null)
	return;
    Edge newEdge = new Edge(this, destVertex, weight);
    m_edgeList.add(newEdge);
  }

  /**
   * Adds an edge (with no weight) from the current Vertex to destVertex.
   * Checks if an edge to destVertex already exists,
   * and if not, adds an edge to destVertex. You aren't allowed to
   * have more than one edge from one vertex to another.
   * @param destVertex The destination Vertex for the edge.
   * @param directed 'true' if the edge is directed; false if undirected.
   * In the latter case, an identical edge in the opposite direction is also
   * added.
   * @return true if the edge was added, false if an edge already exists
   * between these two vertices.
   */
  public void addEdge(Vertex destVertex, boolean directed)
  {
    addEdge(destVertex);
    if (!directed) {
      destVertex.addEdge(this);
    }
  }

  /**
   * Adds an edge with specified weight from the current Vertex to destVertex.
   * Checks if an edge to destVertex already exists,
   * and if not, adds an edge to destVertex. You aren't allowed to
   * have more than one edge from one vertex to another.
   * @param destVertex The destination Vertex for the edge.
   * @param directed 'true' if the edge is directed; false if undirected.
   * In the latter case, an identical edge in the opposite direction is also
   * added.
   * @param weight The weight of the edge to be added.
   * @return true if the edge was added, false if an edge already exists
   * between these two vertices.
   */
  public void addEdge(Vertex destVertex, boolean directed, double weight)
  {
    addEdge(destVertex, weight);
    if (!directed) {
      destVertex.addEdge(this, weight);
    }
  }

  /**
   * Retrieves the label of the vertex.
   * @return An Object containing the label.
   */
  public Object getLabel()
  {
    return m_label;
  }
  
  public void setLabel(Object label)
  {
    m_label = label;
  }

  /**
   * Searches for an edge from the current vertex to destVertex.
   * @param destVertex The destination Vertex.
   * @return The Edge object if it exists, null otherwise.
   */
  public Edge edgeExists(Vertex destVertex)
  {
    Enumeration edgeList = m_edgeList.elements();
    while (edgeList.hasMoreElements()) {
      Edge edge = (Edge)edgeList.nextElement();
      Vertex dest = edge.getDestVertex();
      if (dest == destVertex)
	return edge;
    }
    return null;
  }

  /**
   * Clears the m_visited flag on all edges originating from this vertex.
   * Used in traversal algorithms which must keep track of which edges
   * have been visited.
   */
  public void clearEdgesVisited()
  {
    Enumeration edgeList = m_edgeList.elements();
    while (edgeList.hasMoreElements()) {
      ((Edge)edgeList.nextElement()).setVisited(false);
    }
  }

  /**
   * Prints out a list of edges originating from this vertex.
   * Each edge is printed by specifying the label of the destination
   * vertex followed by its index in the graph's vertex list, enclosed
   * in square brackets.
   * @param vertexList The vertexList of the Graph in which the Vertex
   * is contained. This is required in order to obtain the index of
   * each destination vertex.
   * @return The edge list as text.
   */
  public String printEdgeList(Vector vertexList)
  {
    String edgeString = "";
    Enumeration edgeList = m_edgeList.elements();
    while (edgeList.hasMoreElements()) {
      Edge edge = (Edge)edgeList.nextElement();
      Vertex dest = edge.getDestVertex();
      edgeString += dest.getLabel().toString() + "[" +
	vertexList.indexOf(dest) + "]";
      if (edgeList.hasMoreElements()) {
	edgeString += ", ";
      }
    }
    return edgeString;
  }

  /**
   * Prints out a list of edges originating from this vertex.
   * Each edge is printed by specifying the label of the destination
   * vertex followed by its index in the graph's vertex list, enclosed
   * in square brackets, followed by the Edge's weight, enclosed in
   * angle brackets.
   * @param vertexList The vertexList of the Graph in which the Vertex
   * is contained. This is required in order to obtain the index of
   * each destination vertex.
   * @param weighted Indicating if the graph's edges carry weights. If
   * this parameter is false, the one-argument version of printEdgeList()
   * is called instead.
   * @return The edge list as text.
   */
  public String printEdgeList(Vector vertexList, boolean weighted)
  {
    if (!weighted)
      return printEdgeList(vertexList);
    String edgeString = "";
    Enumeration edgeList = m_edgeList.elements();
    while (edgeList.hasMoreElements()) {
      Edge edge = (Edge)edgeList.nextElement();
      Vertex dest = edge.getDestVertex();
      edgeString += dest.getLabel().toString() + "[" +
	vertexList.indexOf(dest) + "]<" +
	edge.getWeight() + ">";
      if (edgeList.hasMoreElements()) {
	edgeString += ", ";
      }
    }
    return edgeString;
  }

  /**
   * Prints out a list of edges originating from this vertex.
   * Each edge is printed by specifying the label of the destination
   * vertex.
   * @return The edge list as text.
   */
  public String printEdgeList()
  {
    String edgeString = "";
    Enumeration edgeList = m_edgeList.elements();
    while (edgeList.hasMoreElements()) {
      Vertex dest = ((Edge)edgeList.nextElement()).getDestVertex();
      edgeString += dest.getLabel().toString();
      if (edgeList.hasMoreElements()) {
	edgeString += ", ";
      }
    }
    return edgeString;
  }

  public boolean getVisited()
  { return m_visited; }

  public void setVisited(boolean visited)
  { m_visited = visited; }

  public Vector getEdgeList()
  { return m_edgeList; }

  public double getDistance()
  { return m_distance; }

  public void setDistance(double distance)
  { m_distance = distance; }

  public void setPredecessorCount(int count)
  { m_predecessorCount = count; }

  public void changePredecessorCount(int count)
  { m_predecessorCount += count; }

  public int getPredecessorCount()
  { return m_predecessorCount; }

}
