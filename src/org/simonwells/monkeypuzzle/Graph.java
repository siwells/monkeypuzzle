package org.simonwells.monkeypuzzle;

import java.util.*;
import java.io.*;

/**
 * The Graph class represents a graph data structure.
 * To work with the graph package, you must first create a Graph object.
 * This can be done by calling the argumentless constructor, which creates
 * an empty graph that is assumed to be directed an unweighted (once
 * some vertices and edges are added): <br> <br>
 * <tt> Graph myGraph = new Graph(); </tt>
 * <br><br>
 * Alternatively, the 'directed' and 'weighted' nature of the graph may
 * be specified in the two-argument constructor.
 * <p>Once the Graph object has been created, you must add Vertex objects
 * to it. Create the Vertex objects (see Vertex class docs for details),
 * and use the addVertex() method to add a Vertex to the Graph.</p>
 * <p>To add an Edge between two Vertex objects, you should use one of
 * the addEdge() methods. One allows you to add an unweighted edge between
 * two vertices, while the other allows you to specify a weight.</p>
 * <p>Once you have built up the graph by adding Vertex and Edge objects,
 * you may call the other methods in this class to run some of the algorithms
 * such as traversal, minimum cost spanning tree, and so on.
 */
public class Graph implements Serializable
{
  public static int DEAD = 0;
  public static int ALIVE = 1;
  public static int ACTIVE = 2;
  public static int DORMANT = 3;

  static int DIRECTED = 0;
  static int UNDIRECTED = 1;

  public static final int DFT = 0;
  public static final int BFT = 1;
  public static final int DFTOPSORT = 2;
  public static final int BFTOPSORT = 3;

  static double MAXWEIGHT = 100.0;

  protected Vector m_vertexList;
  protected Vector m_depthFirstTraversal;
  protected Vector m_breadthFirstTraversal;
  protected Vector m_depthFirstTopSort;
  protected Vector m_breadthFirstTopSort;
  protected Graph m_spanningTree;	// Min spanning tree from Kruskal's alg
  protected int m_state;
  protected boolean m_directed;    // Is the graph directed or undirected?
  protected boolean m_weighted;    // Do the edges have weights or costs?
  protected boolean m_isModel;     // True if this graph is in the model set

  // A graph in the decomposition can be mapped to one or more subgraphs
  // of the target graph. We store these maps in another Vector.
  protected Vector m_targetMapList;

  /**
   * Creates an empty, directed, unweighted graph.
   */
  public Graph()
  {
    initialize();
  }

  /**
   * Creates an empty graph.
   * @param directed Specifies if the graph is directed or undirected.
   * @param weighted Specifies if the edges in the graph have weights
   */
  public Graph(boolean directed, boolean weighted)
  {
    initialize();
    m_directed = directed;
    m_weighted = weighted;
  }

  protected void initialize()
  {
    m_vertexList = new Vector(10, 10);
    m_depthFirstTraversal = new Vector(10, 10);
    m_targetMapList = new Vector(5, 5);
    m_state = DORMANT;
    m_directed = true;
    m_weighted = false;
    m_isModel = false;
  }

  /**
   * Set whether the graph is a member of the model set in the graph matching algorithm.
   */
  public void setIsModel(boolean isModel)
  { m_isModel = isModel; }

  /**
   * Determines if the graph is a member of the model set in the graph matching algorithm.
   */
  public boolean isModel()
  { return m_isModel; }

  /**
   * Retrieves the state (ALIVE, DEAD, or DORMANT) of the graph.
   * Used in the matching algorithm.
   */
  public int getState()
  { return m_state; }

  /**
   * Sets the state (ALIVE, DEAD, or DORMANT) of the graph.
   * Used in the matching algorithm.
   */
  public void setState(int state)
  { m_state = state; }

  /**
   * Returns the number of vertices in the graph.
   */
  public int getNumberOfVertices()
  {
    return m_vertexList.size();
  }

  /**
   * Returns the list of vertices as a Vector of Vertex objects.
   */
  public Vector getVertexList()
  { return m_vertexList; }

  /**
   * Adds a Vertex object to the list of vertices.
   */
  public void addVertex(Vertex newVertex)
  {
    m_vertexList.add(newVertex);
  }

  public boolean containsVertex(Vertex vertex)
  { return m_vertexList.contains(vertex); }

  /**
   * Adds an edge from the source vertex to the dest vertex.
   * Tests that both vertices are part of the graph.
   * If the graph is undirected, an edge from dest to source
   * is also added.
   * @return false if either vertex is not in the graph,
   * true otherwise
   */
  public boolean addEdge(Vertex source, Vertex dest)
  {
    if(!m_vertexList.contains(source) ||
       !m_vertexList.contains(dest))
      return false;
    source.addEdge(dest, m_directed);
    return true;
  }

  /**
   * Adds an edge with the specified weight from the source vertex to the dest vertex.
   * Tests that both vertices are part of the graph.
   * If the graph is undirected, an identical edge from dest to source
   * is also added.
   * @return false if either vertex is not in the graph,
   * true otherwise
   */
  public boolean addEdge(Vertex source, Vertex dest, double weight)
  {
    if(!m_vertexList.contains(source) ||
       !m_vertexList.contains(dest))
      return false;
    source.addEdge(dest, m_directed, weight);
    return true;
  }

  /**
   * For model graphs in the matching algorithm, returns the first map from this graph to a subgraph in the target.
   */
  public HashMap getFirstTargetMap()
  {
    return (HashMap)m_targetMapList.firstElement();
  }

  /**
   * For model graphs in the matching algorithm,
   * returns the entire list of maps from this graph to the target graph.
   * The returned Vector is a list of HashMap objects.
   */
  public Vector getTargetMap()
  { return m_targetMapList; }

  /**
   * Appends a new map to the end of the map list.
   */
  public void addTargetMap(HashMap newMap)
  {
    m_targetMapList.add(newMap);
  }

  /**
   * Produces a String containing a textual representation of all maps from this graph to the target graph.
   */
  public String printTargetMap()
  {
    Enumeration targetList = m_targetMapList.elements();
    String result = "\n********* Target Map " + m_targetMapList.size() +
      " elements\n";
    int mapNum = 1;
    while (targetList.hasMoreElements()) {
      result += "==========Map #" + mapNum++;
      HashMap map = (HashMap)targetList.nextElement();
      Set mapSet = map.entrySet();
      Iterator mapList = mapSet.iterator();
      while (mapList.hasNext()) {
	Map.Entry mapEntry = (Map.Entry)mapList.next();
	Vertex source = (Vertex)mapEntry.getKey();
	Vertex target = (Vertex)mapEntry.getValue();
	result += source.getLabel().toString() + " --> " +
	  target.getLabel().toString() + "\n";
      }
    }
    return result;
  }

  /**
   * Returned string contains a list of all vertices (by printing their labels).
   * This simply iterates through the vertex list to print the vertices:
   * it does not use a standard traversal algorithm.
   */
  public String printVertexList()
  {
    Enumeration vertexList = m_vertexList.elements();
    String printList = "";
    while (vertexList.hasMoreElements()) {
      Vertex vertex = (Vertex)vertexList.nextElement();
      printList += vertex.getLabel().toString();
      if (vertexList.hasMoreElements()) {
	printList += ", ";
      }
    }
    return printList;
  }

  /**
   * A utility method which returns a String containing information in a Vector of Vertex objects.
   * @param trav A Vector containing Vertex objects.
   * Each item in the printed list contains the vertex label followed by
   * the int index of that vertex within the Vector.
   */
  public String printTraversal(Vector trav)
  {
    Enumeration travList = trav.elements();
    String printList = "";
    while (travList.hasMoreElements()) {
      Vertex vertex = (Vertex)travList.nextElement();
      printList += vertex.getLabel().toString() + "(" +
      	m_vertexList.indexOf(vertex) + ")";
      if (travList.hasMoreElements()) {
      	printList += ", ";
      }
    }
    return printList;
  }

  /**
   * Returned String contains a traversal of the graph using a standard traversal algorithm.
   * @param travType Must be one of:
   * @param Graph.BFT: A breadth first traversal.
   * @param Graph.DFT: A depth-first traversal.
   * @param Graph.DFTOPSORT: A depth-first topological sort.
   * @param Graph.BFTOPSORT: A breadth-first topological sort.
   */
  public String printTraversal(int travType)
  {
    Enumeration travList = null;
    switch (travType) {
    case BFT:
      travList = m_breadthFirstTraversal.elements();
      break;
    case DFT:
      travList = m_depthFirstTraversal.elements();
      break;
    case DFTOPSORT:
      travList = m_depthFirstTopSort.elements();
      break;
    case BFTOPSORT:
      travList = m_breadthFirstTopSort.elements();
      break;
    default:
      return "";
    };
    String printList = "";
    while (travList.hasMoreElements()) {
      Vertex vertex = (Vertex)travList.nextElement();
      printList += vertex.getLabel().toString() + "(" +
      	m_vertexList.indexOf(vertex) + ")";
      if (travList.hasMoreElements()) {
      	printList += ", ";
      }
    }
    return printList;
  }

  /**
   * Searches the graph for the first occurrence of a Vertex with label vertexLabel.
   * @param vertexLabel An Object (or derived class) used as the Vertex label.
   * The class used for labels must have an equals() method defined.
   * @return the Vertex object if found; null otherwise.
   */
  public Vertex getVertexByLabel(Object vertexLabel)
  {
    Enumeration vertexList = m_vertexList.elements();
    while (vertexList.hasMoreElements()) {
      Vertex vertex = (Vertex)vertexList.nextElement();
      if (vertex.getLabel().equals(vertexLabel)) {
	return vertex;
      }
    }
    return null;
  }

  /**
   * Searches the graph for the first occurrence of a Vertex with short label vertexLabel.
   * @param vertexLabel An Object (or derived class) used as the Vertex label.
   * The class used for labels must have an equals() method defined.
   * @return the Vertex object if found; null otherwise.
   */
  public Vertex getVertexByShortLabel(String vertexLabel)
  {
    Enumeration vertexList = m_vertexList.elements();
    while (vertexList.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)(Vertex) vertexList.nextElement();
      if (vertex.getShortLabelString().equals(vertexLabel)) {
	return vertex;
      }
    }
    return null;
  }

  /**
   * Finds the Vertex with specified index in the vertex list.
   * @return the specified vertex.
   * @exception GraphException if vertex index is out of bounds.
   */
  public Vertex getVertexByIndex(int index) throws GraphException
  {
    if (index < 0 || index >= m_vertexList.size()) {
      throw new GraphException("Vertex index out of bounds.");
    }
    return (Vertex)m_vertexList.elementAt(index);
  }

  /**
   * Considers all possible pairs of vertices in the graph and adds an Edge between each pair, with probability edgeProb.
   * If the graph is weighted, a random weight is assigned to each edge.
   * If the graph is undirected, all edges are added symmetrically.
   */
  public void addRandomEdges(double edgeProb)
  {
    Enumeration outerList = m_vertexList.elements();
    while (outerList.hasMoreElements()) {
      Vertex sourceVertex = (Vertex)outerList.nextElement();
      Enumeration innerList = m_vertexList.elements();
      while (innerList.hasMoreElements()) {
	Vertex destVertex = (Vertex)innerList.nextElement();
	if (sourceVertex != destVertex && Math.random() < edgeProb) {
	  if (m_weighted) {
	    double weight = Math.random() * MAXWEIGHT;
	    sourceVertex.addEdge(destVertex, m_directed, weight);
	  } else {
	    sourceVertex.addEdge(destVertex, m_directed);
	  }
	}
      }
    }
  }

  /**
   * Prints a list of edges arising from each vertex.
   * @return a String containing the edge table.
   * Each edge is given as the label of the destination vertex, followed
   * by that vertex's index in the vertex list, followed by the weight
   * on the edge (if the graph is weighted). Thus a listing of
   * F(7)<34.2341> indicates the destination vertex has label 'F',
   * index 7, and the edge has weight 34.2341.
   */
  public String printEdgeTable()
  {
    String edgeTable = "";
    Enumeration outerList = m_vertexList.elements();
    while (outerList.hasMoreElements()) {
      Vertex sourceVertex = (Vertex)outerList.nextElement();
      edgeTable += "(" + m_vertexList.indexOf(sourceVertex) + ") " +
	sourceVertex.getLabel().toString() + ": ";
      edgeTable += sourceVertex.printEdgeList(m_vertexList, m_weighted);
      edgeTable += "\n";
    }
    return edgeTable;
  }

  protected void recursiveDFT(Vertex start)
  {
    if (start.getVisited()) return;
    m_depthFirstTraversal.add(start);
    start.setVisited(true);
    Enumeration edgeList = start.getEdgeList().elements();
    while (edgeList.hasMoreElements()) {
      Vertex nextVertex = ((Edge)edgeList.nextElement()).getDestVertex();
      recursiveDFT(nextVertex);
    }
  }

  /**
   * Calculates the depth-first traversal.
   * Starts with the first vertex in the vertex list.
   * @return a Vector containing the traversal, or null if the graph is empty.
   */
  public Vector depthFirstTraversal()
  {
    Enumeration vertexList = m_vertexList.elements();
    if (vertexList.hasMoreElements()) {
      return depthFirstTraversal((Vertex)vertexList.nextElement());
    }
    return null;
  }

  /*
   * Swaps the start vertex to the beginning of the vertexList.
   * Used in traversal algorithms where the start vertex is not
   * the first vertex in the vertex list.
   * @return an Enumeration of the vertex list Vector.
   */
  protected Enumeration prepareVertexList(Vertex start)
  {
    m_vertexList.remove(start);
    m_vertexList.add(0, start);
    Enumeration vertexList = m_vertexList.elements();
    while (vertexList.hasMoreElements()) {
      ((Vertex)vertexList.nextElement()).setVisited(false);
    }
    vertexList = m_vertexList.elements();
    return vertexList;
  }

  /*
   * Restores the graph's vertex order by swapping the start vertex back to its original location in the vertex list.
   */
  protected void restoreVertexList(int startIndex, Vertex start)
  {
    m_vertexList.remove(start);
    m_vertexList.add(startIndex, start);
  }

  /**
   * Calculates the depth-first traversal.
   * @param start The starting vertex for the traversal.
   * @return a Vector containing the traversal as a list of Vertex objects.
   * @exception GraphException if the starting vertex is not found in the graph.
   */
  public Vector depthFirstTraversal(Vertex start) throws GraphException
  {
    if (!m_vertexList.contains(start)) {
      throw new GraphException("Starting vertex not found in graph.");
    }
    int startIndex = m_vertexList.indexOf(start);
    m_depthFirstTraversal = new Vector(5, 5);

    Enumeration vertexList = prepareVertexList(start);
    while (vertexList.hasMoreElements()) {
      recursiveDFT((Vertex)vertexList.nextElement());
    }
    restoreVertexList(startIndex, start);
    return m_depthFirstTraversal;
  }

  /*
   * The recursive portion of the depth-first topological sort algorithm.
   * @return false if a cycle exists, true otherwise.
   */
  protected boolean DFRecursiveSort(Vertex start)
  {
    Vertex nextVertex;
    start.setVisited(true);
    Enumeration edgeList = start.getEdgeList().elements();
    while (edgeList.hasMoreElements()) {
      nextVertex = ((Edge)edgeList.nextElement()).getDestVertex();
      if (!nextVertex.getVisited())
	DFRecursiveSort(nextVertex);
    }

    // Check for cycle - all vertices to which start is joined by
    // an edge should already be in m_depthFirstTopSort
    edgeList = start.getEdgeList().elements();
    while (edgeList.hasMoreElements()) {
      nextVertex = ((Edge)edgeList.nextElement()).getDestVertex();
      if (!m_depthFirstTopSort.contains(nextVertex)) {
	return false;
      }
    }
    m_depthFirstTopSort.add(0, start);
    return true;
  }

  /**
   * Depth-first topological sort.
   * @param start The starting vertex for the sort.
   * @return a Vector containing the sorted list of Vertex objects.
   * @exception GraphException if the graph is undirected.
   * @exception GraphException if the starting vertex is not found.
   * @exception GraphException if the graph contains a cycle.
   */
  public Vector depthFirstTopSort(Vertex start) throws GraphException
  {
    // Topological sorts only apply to directed graphs
    if (!m_directed) {
      throw new GraphException("Graph is undirected - topological sort applies only to directed graphs.");
    }
    if (!m_vertexList.contains(start)) {
      throw new GraphException("Starting vertex not found in graph.");
    }
    int startIndex = m_vertexList.indexOf(start);
    m_depthFirstTopSort = new Vector(5, 5);

    Enumeration vertexList = prepareVertexList(start);
    while (vertexList.hasMoreElements()) {
      Vertex nextVertex = (Vertex)vertexList.nextElement();
      if (!nextVertex.getVisited()) {
	if (!DFRecursiveSort(nextVertex)) {
	  restoreVertexList(startIndex, start);
	  throw new GraphException(
            "Graph contains a cycle - cannot calculate a topological sort.");
	}
      }
    }
    restoreVertexList(startIndex, start);
    return m_depthFirstTopSort;
  }

  /**
   * Breadth-first traversal.
   * @param start The starting vertex for the traversal.
   * @return a Vector containing the traversal as a list of Vertex objects
   * @exception GraphException if the starting vertex is not found
   * in the graph.
   */
  public Vector breadthFirstTraversal(Vertex start) throws GraphException
  {
    if (!m_vertexList.contains(start)) {
      throw new GraphException(
        "Starting vertex not found in graph.");
    }
    int startIndex = m_vertexList.indexOf(start);
    m_breadthFirstTraversal = new Vector(5, 5);

    Enumeration vertexList = prepareVertexList(start);
    Vector vertexQueue = new Vector(5,5);
    while (vertexList.hasMoreElements()) {
      Vertex nextVertex = (Vertex)vertexList.nextElement();
      if (!nextVertex.getVisited()) {
	vertexQueue.add(nextVertex);
	nextVertex.setVisited(true);
      }
      while (vertexQueue.size() > 0) {
	Vertex addVertex = (Vertex)vertexQueue.remove(0);
	m_breadthFirstTraversal.add(addVertex);
	Enumeration edgeList = addVertex.getEdgeList().elements();
	while (edgeList.hasMoreElements()) {
	  Vertex destVertex = ((Edge)edgeList.nextElement()).
	    getDestVertex();
	  if (!destVertex.getVisited()) {
	    vertexQueue.add(destVertex);
	    destVertex.setVisited(true);
	  }
	}
      }
    }
    restoreVertexList(startIndex, start);
    return m_breadthFirstTraversal;
  }

  /**
   * Breadth-first topological sort.
   * @param start The starting vertex for the sort.
   * @return a Vector containing the sorted list of Vertex objects.
   * @exception GraphException if the graph is undirected.
   * @exception GraphException if the starting vertex is not found.
   * @exception GraphException if the graph contains a cycle.
   */
  public Vector breadthFirstTopSort(Vertex start) throws GraphException
  {
    Enumeration edgeList;
    Vertex vertex;

    // Topological sorts only apply to directed graphs
    if (!m_directed) {
      throw new GraphException(
        "Graph is undirected - topological sort applies only to directed graphs.");
    }
    if (!m_vertexList.contains(start)) {
      throw new GraphException("Starting vertex not found.");
    }
    int startIndex = m_vertexList.indexOf(start);
    m_breadthFirstTopSort = new Vector(5, 5);
    Vector vertexQueue = new Vector(5,5);

    Enumeration vertexList = prepareVertexList(start);
    while (vertexList.hasMoreElements()) {
      vertex = (Vertex)vertexList.nextElement();
      vertex.setPredecessorCount(0);
    }
				// Initialize predecessor counts
    vertexList = m_vertexList.elements();
    while (vertexList.hasMoreElements()) {
      vertex = (Vertex)vertexList.nextElement();
      edgeList = vertex.getEdgeList().elements();
      while (edgeList.hasMoreElements()) {
	Edge edge = (Edge)edgeList.nextElement();
	edge.getDestVertex().changePredecessorCount(1);
      }
    }
				// Initialize queue
    vertexList = m_vertexList.elements();
    while (vertexList.hasMoreElements()) {
      vertex = (Vertex)vertexList.nextElement();
      if (vertex.getPredecessorCount() == 0) {
	vertexQueue.add(vertex);
      }
    }
				// No source vertices - graph has cycle
    if (vertexQueue.size() == 0)
      throw new GraphException("Graph has a cycle.");

    while (vertexQueue.size() > 0) {
				// Add first queue element to top sort
      vertex = (Vertex)vertexQueue.remove(0);
      m_breadthFirstTopSort.add(vertex);
				// Adjust predecessor counts
      edgeList = vertex.getEdgeList().elements();
      while (edgeList.hasMoreElements()) {
	Vertex destVertex = ((Edge)edgeList.nextElement()).getDestVertex();
	destVertex.changePredecessorCount(-1);
	if (destVertex.getPredecessorCount() == 0) {
	  vertexQueue.add(destVertex);
	}
      }
				// If queue is empty & not all vertices added
				// to sort, graph has a cycle so give up
      if (vertexQueue.size() == 0 && m_breadthFirstTopSort.size() <
	  m_vertexList.size()) {
	throw new GraphException("Graph has a cycle.");
      }
    }
    return m_breadthFirstTopSort;
  }

  /**
   * Prints the minimum cost from starting vertex to all vertices in the graph.
   * Uses Dijkstra's algorithm.
   * @return A String with a table of the minimum cost to each vertex from
   * the starting vertex.
   */
  public String printMinCosts(Vertex start)
  {
    Dijkstra(start);

    String minCosts = "";
    Enumeration outerList = m_vertexList.elements();
    while (outerList.hasMoreElements()) {
      Vertex sourceVertex = (Vertex)outerList.nextElement();
      minCosts += "(" + m_vertexList.indexOf(sourceVertex) + ") " +
	sourceVertex.getLabel().toString() + ": ";
      minCosts += sourceVertex.getDistance();
      minCosts += "\n";
    }
    return minCosts;
  }

  /**
   * Dijkstra's algorithm for finding minimum costs from specified vertex to all other vertices in the graph.
   * @param start The starting vertex.
   */
  public void Dijkstra(Vertex start)
  {
    HashSet pathFound = new HashSet();
    Vertex tempMin = start;
    Vertex vertex;
    Enumeration vertexList, endList;
    Edge edgeToVertex;

    pathFound.add(start);
    start.setDistance(0.0);
				// Set up initial min weights
    vertexList = m_vertexList.elements();
    while (vertexList.hasMoreElements()) {
      vertex = (Vertex)vertexList.nextElement();
      if (vertex != start) {
        edgeToVertex = start.edgeExists(vertex);
        vertex.setDistance(edgeToVertex == null ? Double.MAX_VALUE :
			   edgeToVertex.getWeight());
      }
    }
				// Find cheapest path to vertex not in set
    vertexList = m_vertexList.elements();
    while (vertexList.hasMoreElements()) {
      vertex = (Vertex)vertexList.nextElement();
      double mindist = Double.MAX_VALUE;
      if (vertex != start) {
        endList = m_vertexList.elements();
        while (endList.hasMoreElements()) {
          Vertex end = (Vertex)endList.nextElement();
          if (!pathFound.contains(end)) {
            if (end.getDistance() < mindist) {
              tempMin = end;
              mindist = end.getDistance();
            }
          }
        }
				// If a path is found, add vertex to set
				// and update other distances
        if (mindist < Double.MAX_VALUE) {
          pathFound.add(tempMin);
          endList = m_vertexList.elements();
          while (endList.hasMoreElements()) {
            Vertex end = (Vertex)endList.nextElement();
            if (!pathFound.contains(end)) {
              edgeToVertex = tempMin.edgeExists(end);
              double weight = edgeToVertex == null ? Double.MAX_VALUE :
                edgeToVertex.getWeight();
              if (weight < Double.MAX_VALUE &&
                  mindist + weight < end.getDistance()) {
                end.setDistance(mindist + weight);
              }
            }
          }
        }
      }
    }
  }

  /*
   * Used in Kruskal's algorithm
   */
  protected void addToEdgeList(Vertex vertex, LinkedList sortedEdges)
  {
    boolean edgeAdded = false;
    ListIterator edges;
    Vector vertexEdgeList = vertex.getEdgeList();
    Enumeration vertexEdges = vertexEdgeList.elements();
    while (vertexEdges.hasMoreElements()) {
      Edge vertexEdge = (Edge)vertexEdges.nextElement();
      edges = sortedEdges.listIterator(0);
      int edgeCounter = 0;
      edgeAdded = false;
      while (!edgeAdded && edges.hasNext()) {
	if (vertexEdge.getWeight() <
	    ((Edge)edges.next()).getWeight()) {
	  sortedEdges.add(edgeCounter, vertexEdge);
	  edgeAdded = true;
	} else {
	  edgeCounter ++;
	}
      }
      if (!edgeAdded) {
	sortedEdges.add(vertexEdge);
      }
    }
  }

  /**
   * Prints the edge table for the minimum cost spanning tree.
   * Tree obtained using Kruskal's algorithm.
   * @return a String containing the edge table.
   */
  public String printSpanningTree()
  {
    Graph testGraph = Kruskal();
    return testGraph.printEdgeTable();
  }

  // Marks the return edge of 'edge' as visited
  // Used for Kruskal's algorithm with undirected graphs
  protected Edge visitReturnEdge(Edge edge, LinkedList sortedEdges)
  {
    Vertex source = edge.getSourceVertex();
    Vertex dest = edge.getDestVertex();
    ListIterator tempEdgeList = sortedEdges.listIterator();
    while (tempEdgeList.hasNext()) {
      Edge tempEdge = (Edge)tempEdgeList.next();
      Vertex tempSource = tempEdge.getSourceVertex();
      Vertex tempDest = tempEdge.getDestVertex();
      if (tempSource == dest && tempDest == source) {
	tempEdge.setVisited(true);
	return tempEdge;
      }
    }
    return null;
  }

  /**
   * Calculates the set union of two HashSet objects.
   * @return the HashSet containing the union.
   */
  public HashSet setUnion(HashSet set1, HashSet set2)
  {
    HashSet union = new HashSet();
    Iterator set = set1.iterator();
    while (set.hasNext())
      union.add(set.next());
    set = set2.iterator();
    while (set.hasNext())
      union.add(set.next());
    return union;
  }

  protected Vertex treeVertex(Vertex vertex)
  {
    int index = m_vertexList.indexOf(vertex);
    if (index > -1)
      return (Vertex)m_spanningTree.getVertexList().elementAt(index);
    return null;
  }

  /**
   * Kruskal's algorithm for finding the minimum cost spanning tree.
   * @return a new Graph object containing the spanning tree.
   */
  public Graph Kruskal()
  {
    m_spanningTree = new Graph(m_directed, m_weighted);
    LinkedList sortedEdges = new LinkedList();
    HashSet[] setArray = new HashSet[m_vertexList.size()];
    int setIndex1 = 0, setIndex2 = 0;
    Edge returnEdge = null;
				// Sort edges by cost
    Enumeration vertexList = m_vertexList.elements();
    int vertexCount = 0;
    while (vertexList.hasMoreElements()) {
      Vertex vertex = (Vertex)vertexList.nextElement();
      vertex.clearEdgesVisited();
      addToEdgeList(vertex, sortedEdges);
      m_spanningTree.addVertex(vertex.noEdgesCopy());
      setArray[vertexCount] = new HashSet();
      setArray[vertexCount++].add(vertex);
    }

    boolean sameSet;
    ListIterator sortedEdgeList = sortedEdges.listIterator();
    while (sortedEdgeList.hasNext()) {
      sameSet = false;
      Edge edge = (Edge)sortedEdgeList.next();
      if (edge.isVisited()) continue;
      edge.setVisited(true);
      if (m_directed == false) {
	returnEdge = visitReturnEdge(edge, sortedEdges);
      }
      Vertex source = edge.getSourceVertex();
      Vertex dest = edge.getDestVertex();
      for (int setIndex = 0; setIndex < m_vertexList.size(); setIndex++) {
	if (setArray[setIndex] == null) continue;
	if (setArray[setIndex].contains(source))
	  setIndex1 = setIndex;
	if (setArray[setIndex].contains(dest))
	  setIndex2 = setIndex;
      }
      if (setIndex1 == setIndex2)
	sameSet = true;
      if (!sameSet) {
	int unionSetIndex = setIndex1 < setIndex2 ?
	  setIndex1 : setIndex2;
	int otherSetIndex = setIndex1 < setIndex2 ?
	  setIndex2 : setIndex1;
	setArray[unionSetIndex] = setUnion(setArray[unionSetIndex],
					   setArray[otherSetIndex]);
	setArray[otherSetIndex] = null;
	Vertex treeSource = treeVertex(source);
	Vertex treeDest = treeVertex(dest);
	treeSource.addEdge(treeDest, edge.getWeight());
	if (m_directed == false)
	  treeDest.addEdge(treeSource, edge.getWeight());
      }
    }
    return m_spanningTree;
  }
}
