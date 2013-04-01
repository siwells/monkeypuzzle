import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;

/**
 *  Extends the Vertex class to represent nodes in a tree
 */
public class TreeVertex extends Vertex implements Serializable
{
  String m_shortLabel;		// Single character label
  int m_layer;			// Depth of the vertex in the tree; root = 0
  TreeVertex m_parent;		// Parent of this vertex
  TreeVertex m_sibling;   // Link to vertex on same layer as this one
  Point m_drawPoint;		// Point where the vertex should be drawn
  Point m_drawPointFullText;  // Point for drawing the vertex in FullTextDialog
  float m_width;		// Horizontal space allocated to vertex in drawing
  int m_xMin, m_xMax; // Pixel values for min and max x extent of horizontal space
  int m_xMinFullText, m_xMaxFullText; // Same for FullTextDialog
  public int m_numHorizCells;  // Minimum number of horizontal cells required to display full text
  Hashtable shapeTable = new Hashtable();   // Hashtable for storing diagram shapes for various diagrams
  Hashtable edgeShapeTable = new Hashtable(); // ...and for storing edge shapes in Toulmin diagrams
  Hashtable isHiddenTable = new Hashtable(); // Is vertex displayed in a given diagram?
  boolean m_virtual;		// Not displayed - merges two edges
  boolean m_missing;    // Corresponds to a missing premise
  boolean m_refutation; // Is this node a refutation of another node?
  public boolean visible = true; // Is node to be displayed?
  public boolean hidingChildren = false; // Is vertex hiding its children in a diagram?
  public boolean truth;      // Is node a 'truth' node - one added to provide an inference in Toulmin
  boolean m_hasParent;		// True if vertex has a parent
  boolean m_selected;		// Vertex is selected (highlighted)
  boolean dummy = false;        // Vertex is the dummy root in a split tree
  boolean m_linkedStemSelected; // If this is a virtual vertex, true if edge leading to vertex is selected.
  Object m_auxObject;      // Auxiliary object associated with vertex
  int m_offset;     // Offset of vertex label within a larger body of text
  int m_tutorStart, m_tutorEnd;   // Offsets of earliest and latest acceptable offsets for a premise; used in tutorial mode
  private String supportLabel = null;   // Label for edge to parent
  public boolean previousRefutation = false;  // Previous refutation state; used in Wigmore for switching to/from negatory
  public String m_nodeLabel = null;   // External label for vertex
  public Vector schemeLabels = new Vector(); // List of names of all schemes for which this vertex is the root node
  public Vector m_schemeColorList = new Vector();
  public Vector schemeLayout = null;
  public Dimension schemeLayoutSize = new Dimension(0,0);
  public Vector<Subtree> schemeList = new Vector<Subtree>(); // List of schemes having this vertex as root
  
  public Set owners = new HashSet();
  public Vector textLayout = null;    // Contains a layout of the full text for the FullTextDialog
  public Vector nodeLabelLayout = null;    // Contains a layout of the node label for the FullTextDialog
  public Vector supportLabelLayout = null;    // Contains a layout of the support label for the FullTextDialog
  public Vector ownersLayout = null;    // Contains a layout of the owners for the FullTextDialog
  public Vector roleLayout = null;      // Layout of roles in various diagram types (e.g. Toulmin)
  public Dimension textLayoutSize = null; // Stores the dimensions of the text displayed 
  public Dimension nodeLabelLayoutSize = null; // Stores the dimensions of the nodeLabel text displayed 
  public Dimension ownersLayoutSize = null; // Stores the dimensions of the owners text displayed 
  public Dimension totalLayoutSize = null; // Stores the dimensions of text + nodeLabel + ownersLabel + schemeLayout
  public Dimension supportLabelLayoutSize = null; // Stores the dimensions of the support layout (edge label)
  public Dimension roleLayoutSize = null;
  
  public Paint fillPaint = null;
  public Paint outlinePaint = null;
  public Paint textPaint = null;
  
  public int leafCount;  // Leaf index for displaying full text mode
  
  // Tutoring stuff
  public TreeVertex tutorLink, studentLink; // Correspondence between tutor and student vertexes
  public Vector studentVirtualList = new Vector();
  
  // General diagram type stuff
  public Hashtable roles = new Hashtable();   // Stores roles for non-standard diagram types
  
  // Toulmin stuff
  public Vector toulminDataEdges = new Vector();   // List of Data nodes attached to this node
  public Vector toulminWarrantEdges = new Vector();
  public Vector toulminBackingEdges = new Vector();
  public Vector toulminQualifierEdges = new Vector();
  public Vector toulminRebuttalEdges = new Vector();
  // Possible toulminType values:
  public static final int DATA = 0;
  public static final int WARRANT = 1;
  public static final int BACKING = 2;
  public static final int QUALIFIER = 3;
  public static final int REBUTTAL = 4;
  
  public int toulminWidth, toulminHeight;   // width & height of this block in a diagram
  public int toulminX, toulminY;            // Coords of UL corner of this vertex
  public int arrowLength;                   // Length of arrow between data and claim
  
  // Wigmore stuff
  public Vector<TreeEdge> wigmoreExplanatoryEdges = new Vector<TreeEdge>();
  public Vector<TreeEdge> wigmoreCorroborativeEdges = new Vector<TreeEdge>();
  public Vector<TreeEdge> wigmoreEvidenceEdges = new Vector<TreeEdge>();
  // Possible wigmoreType values:
  public static final int EVIDENCE = 0;
  public static final int EXPLANATORY = 1;
  public static final int CORROBORATIVE = 2;
  public int wigmoreWidth, wigmoreHeight;   // width & height of this block in a diagram
  public Dimension wigmoreLeft = new Dimension(), 
          wigmoreRight = new Dimension(), wigmoreBottom = new Dimension();  // Sizes of the 3 blocks attached to this node
  public int wigmoreX, wigmoreY;            // Coords of UL corner of this vertex
  enum WigmoreFact    // Basic fact in the argument - represented by pilcrow or infinity
  {
      NONE, JUDICIAL, TRIBUNAL
  }
  WigmoreFact wigmoreFact = WigmoreFact.NONE;
  
  public TreeVertex(Object label)
  {
    super(label);
    m_layer = -1;
    m_parent = null;
    m_sibling = null;
    m_drawPoint = null;
    m_width = 1.0f;
    m_virtual = false;
    m_refutation = false;
    m_missing = false;
    m_hasParent = false;
    m_selected = false;
    m_linkedStemSelected = false;
    initRoles();
  }

	private boolean m_showOwners;
  public TreeVertex(Object label, String shortLabel)
  {
    super(label);
    m_shortLabel = shortLabel;
    m_layer = -1;
    m_parent = null;
    m_drawPoint = null;
    m_width = 1.0f;
    m_virtual = false;
    m_refutation = false;
    m_missing = false;
    m_hasParent = false;
    m_selected = false;
    m_linkedStemSelected = false;
    initRoles();
  }

  public TreeVertex(Object label, boolean virtual)
  {
    super(label);
    m_layer = -1;
    m_parent = null;
    m_sibling = null;
    m_drawPoint = null;
    m_width = 1.0f;
    m_virtual = virtual;
    m_hasParent = false;
    m_selected = false;
    m_linkedStemSelected = false;
    initRoles();
  }

  public TreeVertex(Object label, int layer, TreeVertex parent)
  {
    super(label);
    m_layer = layer;
    m_parent = parent;
    m_sibling = null;
    m_drawPoint = null;
    m_width = 1.0f;
    m_virtual = false;
    m_hasParent = false;
    m_selected = false;
    m_linkedStemSelected = false;
    initRoles();
  }

  public TreeVertex(Object label, int layer, TreeVertex parent,
		    boolean virtual)
  {
    super(label);
    m_layer = layer;
    m_parent = parent;
    m_sibling = null;
    m_drawPoint = null;
    m_width = 1.0f;
    m_virtual = virtual;
    m_hasParent = false;
    m_selected = false;
    m_linkedStemSelected = false;
    initRoles();
  }
  
  public void initRoles()
  {
    roles.put("toulmin", "data");
    isHiddenTable.put("toulmin", "false");
    isHiddenTable.put("wigmore", "true");
    roles.put("addedNegation", "no");
    if (!isVirtual())
      roles.put("wigmore", "evidenceTestAffirm");
    else
      roles.put("wigmore", "virtual");
    roles.put("wigmoreFact", "none");
  }
  
  public static void initRoles(Hashtable roles)
  {
    roles.put("toulmin", "data");
    roles.put("addedNegation", "no");
    roles.put("wigmore", "evidenceTestAffirm");
    roles.put("wigmoreFact", "none");
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
  public boolean addEdge(TreeVertex destVertex)
  {
    if (edgeExists(destVertex) != null)
      return false;
    TreeEdge newEdge = new TreeEdge(this, destVertex);
    m_edgeList.add(newEdge);
    return true;
  }
  
  /**
   * Adds an existing TreeEdge - used in undo
   */
  public void addEdge(TreeEdge edge, int order)
  {
    try {
      m_edgeList.add(order, edge);
    }
    catch (ArrayIndexOutOfBoundsException ex) {
      m_edgeList.add(edge);
    }
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
  public void addEdge(TreeVertex destVertex, double weight)
  {
    if (edgeExists(destVertex) != null)
	    return;
    TreeEdge newEdge = new TreeEdge(this, destVertex, weight);
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
  public void addEdge(TreeVertex destVertex, boolean directed)
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
  public void addEdge(TreeVertex destVertex, boolean directed, double weight)
  {
    addEdge(destVertex, weight);
    if (!directed) {
      destVertex.addEdge(this, weight);
    }
  }
  
  public boolean isRefutationWithAddedNegation()
  {
    return isRefutation() && getParent().roles.get("addedNegation") != null &&
            getParent().roles.get("addedNegation").equals("yes");
  }
  
  public boolean hasAddedNegationNoSupport()
  {
    if (getParent() == null) return false;
    if (getParent().roles.get("addedNegation") != null &&
            getParent().roles.get("addedNegation").equals("yes"))
    {
      return getParent().getEdgeList().size() == 1;
    }
    return false;
  }

  public boolean hasAddedNegationExtraSupport()
  {
    if (getParent() == null) return false;
    if (getParent().roles.get("addedNegation") != null &&
            getParent().roles.get("addedNegation").equals("yes"))
    {
      return getParent().getEdgeList().size() > 1;
    }
    return false;
  }

  /**
   * Searches for an edge from the current vertex to destVertex.
   * @param destVertex The destination Vertex.
   * @return The Edge object if it exists, null otherwise.
   */
  public TreeEdge edgeExists(TreeVertex destVertex)
  {
    Enumeration edgeList = m_edgeList.elements();
    while (edgeList.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edgeList.nextElement();
      TreeVertex dest = edge.getDestVertex();
      if (dest == destVertex)
      	return edge;
    }
    return null;
  }
  
  public void clearTutorParameters()
  {
    studentLink = null;
    tutorLink = null;
    studentVirtualList = new Vector();
  }
  
  public boolean isWigmoreLeaf()
  {
    if (this.wigmoreCorroborativeEdges.size() > 0)
      return false;
    if (this.wigmoreEvidenceEdges.size() > 0)
      return false;
    if (this.wigmoreExplanatoryEdges.size() > 0)
      return false;
    return true;
  }

  /**
   * Tests if an edge to destVertex exists and if so,
   * deletes it from the edge list. Returns true if
   * edge was found and deleted, false otherwise.
   */
  public boolean deleteEdge(TreeVertex destVertex)
  {
    TreeEdge edge = edgeExists(destVertex);
    if (edge == null)
      return false;
    m_edgeList.remove(edge);
    return true;
  }
  
  public void deleteAllEdges()
  {
    m_edgeList.removeAllElements();
  }
  
  public void deleteEdgeList(Vector edgeList)
  {
    edgeList.removeAllElements();
  }

  public int getLayer()
  { return m_layer; }

  public void setLayer(int layer)
  { m_layer = layer; }

  public TreeVertex getParent()
  { return m_parent; }

  /**
   * Returns TreeEdge with 'this' as source vertex and
   * 'vertex' as dest vertex; null if not found.
   */
  public TreeEdge getEdge(TreeVertex vertex)
  {
    return getEdge(m_edgeList, vertex);
  }
  
  /**
   * As above except requires an edge list as a parameter.
   * Used for scanning Toulmin edge lists
   */
  public TreeEdge getEdge(Vector eList, TreeVertex vertex)
  {
    Enumeration edgeList = eList.elements();
    while (edgeList.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)edgeList.nextElement();
      if (edge.getDestVertex() == vertex) {
        return edge;
      }
    }
    return null;
  }
  
  /**
   * Restores the toulmin edges in a linked argument when an AML file is read.
   * 'virtual' is the parent of the LA in the standard diagram.
   */
  public void restoreToulminEdges(TreeVertex virtual)
  {
    // Find the data node in virtual's children
    TreeVertex dataNode = null;
    Vector virtChildren = virtual.getEdgeList();
    for (int i = 0; i < virtChildren.size(); i++)
    {
      TreeVertex node = null;
      node = ((TreeEdge)virtChildren.elementAt(i)).getDestVertex();
      if (node.roles.get("toulmin").equals("data"))
      {
        dataNode = node;
        break;
      }
    }
    // If there is no data node (as can happen when the first child of a
    // virtual node is deleted) set the first node to be the data node
    if (dataNode == null)
    {
      dataNode = ((TreeEdge)virtChildren.elementAt(0)).getDestVertex();
      dataNode.roles.put("toulmin", "data");
    }
    toulminDataEdges.add(new TreeEdge(this, dataNode));
    for (int i = 0; i < virtChildren.size(); i++)
    {
      TreeVertex node = ((TreeEdge)virtChildren.elementAt(i)).getDestVertex();
      // To convert old AML files (pre-Toulmin) we assign all nodes labelled
      // as either warrant or data (except for dataNode itself) as warrants.
      // This is because pre-Toulmin AML didn't have a ROLE element,
      // and by default all new nodes are labelled as DATA nodes.
      if (node.roles.get("toulmin").equals("warrant") || 
        (dataNode != node && node.roles.get("toulmin").equals("data")))
      {
        node.roles.put("toulmin", "warrant");
        dataNode.toulminWarrantEdges.add(new TreeEdge(dataNode, node));
      }
    }
  }
  
  public void clearToulminLists()
  {
    toulminDataEdges = new Vector();   // List of Data nodes attached to this node
    toulminWarrantEdges = new Vector();
    toulminBackingEdges = new Vector();
    toulminQualifierEdges = new Vector();
    toulminRebuttalEdges = new Vector();
  }
  
  public void clearWigmoreLists()
  {
    this.wigmoreCorroborativeEdges = new Vector();
    this.wigmoreEvidenceEdges = new Vector();
    this.wigmoreExplanatoryEdges = new Vector();
    if (roles.get("wigmore") == null)
    {
      roles.put("wigmore", "evidenceTestAffirm");
    }
  }
  
  /**
   * Converts a standard diagram refutation node into a rebuttal in Toulmin.
   * In Toulmin, the node being rebutted is not displayed (unless it has support).
   * Rather, the rebuttal is shown as rebutting the claim supported by the node
   * making the opposite claim to the rebuttal.
   */
  public void refutationToRebuttal()
  {
    // parent is the node being refuted in standard
    TreeVertex parent = getParent();
    TreeVertex uberParent = parent.getParent();
    
    // This should only happen if the refutation is the child of the root
    // In this case, the Toulmin diagram should ignore the vertex
    if (uberParent == null) 
    {
      System.out.println("refutationToRebuttal: ignoring " + m_shortLabel);
      parent.toulminDataEdges.remove(parent.getEdge(parent.toulminDataEdges, this));
      return;
    }
    
    // If the parent is a data node, then refutation is added to rebuttal
    // list of the parent node.
    if (parent.roles.get("toulmin").equals("data"))
    {
      parent.toulminRebuttalEdges.add(new TreeEdge(parent, this));
    }
    // If parent is a warrant, then the current node is either backing or data.
    // In either case, the current node must be added to the rebuttal list of
    // the data node to which the parent belongs.
    if (parent.roles.get("toulmin").equals("warrant"))
    {
      TreeVertex dataNode = parent.findDataNodeInLA();
      dataNode.toulminRebuttalEdges.add(new TreeEdge(dataNode, this));
    }
    if (roles.get("toulmin").equals("data"))
    {
      parent.toulminDataEdges.remove(parent.getEdge(parent.toulminDataEdges, this));
    } else if (roles.get("toulmin").equals("backing"))
    {
      parent.toulminBackingEdges.remove(parent.getEdge(parent.toulminBackingEdges, this));
    }
    // By default, set the display flag of any parent that is an addedNegation to false,
    // unless it has support
    if (parent.roles.get("addedNegation") != null && 
            (parent.roles.get("addedNegation").equals("yes") &&
            parent.toulminBackingEdges.size() == 0 &&
            parent.toulminDataEdges.size() == 0))
    {
      parent.isHiddenTable.put("toulmin", "true");
    } else {
      parent.isHiddenTable.put("toulmin", "false");
    }
    roles.put("toulmin", "rebuttal");
  }
  
  /**
   * Assuming that 'this' is in an LA, finds the data node from
   * within that LA.
   */
  public TreeVertex findDataNodeInLA()
  {
    if (roles.get("toulmin").equals("data"))
      return this;
    if (roles.get("toulmin").equals("warrant"))
    {
      TreeVertex virtual = getParent();
      Vector edgeList = virtual.getEdgeList();
      for (int i = 0; i < edgeList.size(); i++)
      {
        TreeVertex laNode = ((TreeEdge)edgeList.elementAt(i)).getDestVertex();
        if (laNode.roles.get("toulmin").equals("data"))
          return laNode;
      }
    }
    return null;
  }

  /**
   * Sorts the edge list so that the refutation (if there is one)
   * is element 0. Returns true if any rearrangement took place.
   */
  public boolean putRefutationFirst()
  {
  	if (this.getNumRefutations() == 0 || 
  		((TreeEdge)m_edgeList.elementAt(0)).getDestVertex().isRefutation())
  		return false;
  	TreeEdge edge0 = (TreeEdge)m_edgeList.elementAt(0);
  	for (int i = 1; i < m_edgeList.size(); i++) {
  		if (((TreeEdge)m_edgeList.elementAt(i)).getDestVertex().isRefutation())
  		{
  			m_edgeList.set(0, (TreeEdge)m_edgeList.elementAt(i));
  			m_edgeList.set(i, edge0);
  			return true;
  		}
	  }
  	return false;
  }
  
  public TreeVertex getFirstChild()
  {
    if (m_edgeList.size() == 0)
      return null;
    return ((TreeEdge)m_edgeList.elementAt(0)).getDestVertex();
  }
  
  public int getNumberOfChildren()
  {
    return m_edgeList.size();
  }
  
  public Object getLabel()
  {
  	if (isVirtual())
  		return "**Virtual**";
  	return super.getLabel();
  }
  
  /**
   * Returns the number of children of this vertex that are refutations
   */
  public int getNumRefutations()
  {
    int numRefuts = 0;
    for (int i = 0; i < m_edgeList.size(); i++) {
      if (((TreeEdge)m_edgeList.elementAt(i)).getDestVertex().isRefutation())
        numRefuts ++;
    }
    return numRefuts;
  }
  
  /**
   * Recursive method: fills refutList with refutation nodes that
   * are children of the current vertex.
   */
  public void getRefutationList(Vector refutList)
  {
    for (int i = 0; i < m_edgeList.size(); i++) {
      TreeVertex vertex = ((TreeEdge)m_edgeList.elementAt(i)).getDestVertex();
      if (vertex.isRefutation()) {
      	refutList.insertElementAt(vertex, 0);
        vertex.getRefutationList(refutList);
      }
    }
  } 
  
  public Vector getRefutationEdgeList()
  {
    Vector refutList = new Vector();
    for (int i = 0; i < m_edgeList.size(); i++) {
      TreeEdge edge = (TreeEdge)m_edgeList.elementAt(i);
      TreeVertex vertex = edge.getDestVertex();
      if (vertex.isRefutation())
        refutList.add(edge);
    }
    return refutList;
  }
  
  /**
   * Gets a list of edges in the next layer down from the
   * current vertex. The list contains all children of the
   * current vertex that are NOT refutations, and all children
   * of those children that ARE refutations.
   */
  public Vector getNextLayerList()
  {
    Vector layerList = new Vector();
    for (int i = 0; i < m_edgeList.size(); i++) {
      TreeEdge edge = (TreeEdge)m_edgeList.elementAt(i);
      TreeVertex vertex = edge.getDestVertex();
      if (!vertex.isRefutation()) {
        layerList.add(edge);
        layerList.addAll(vertex.getRefutationEdgeList());
      }
    }
    return layerList;
  }
  
  public void setParent(TreeVertex parent)
  { m_parent = parent; }
  
  public void setSibling(TreeVertex sibling)
  { m_sibling = sibling; }
  
  public TreeVertex getSibling()
  { return m_sibling; }
  
  public String getSiblingListString()
  {
    String list = new String(this.getShortLabel());
    TreeVertex sibling = m_sibling;
    while (sibling != null) {
      list += " -> " + new String(sibling.getShortLabel());
      sibling = sibling.getSibling();
    }
    return list;
  }
  
  public Point getDrawPoint()
  { return m_drawPoint; }

  public Point getDrawPointFullText()
  { return m_drawPointFullText; }

  public void setDrawPoint(int x, int y)
  { m_drawPoint = new Point(x, y); }

  public void setDrawPointFullText(int x, int y)
  { m_drawPointFullText = new Point(x, y); }

  public float getWidth()
  { return m_width; }

  public void setWidth(float width)
  { m_width = width; }

  public Shape getShape(DiagramBase diagram)
  { 
    return (Shape)shapeTable.get(diagram); 
  }

  public void setShape(Shape shape, DiagramBase diagram)
  { 
    shapeTable.put(diagram, shape);
  }

  public boolean isVirtual()
  { return m_virtual; }

  public void setVirtual(boolean virtual)
  { m_virtual = virtual; }

  public boolean isMissing()
  { return m_missing; }

  public void setMissing(boolean missing)
  { m_missing = missing; }
  
  public boolean isRefutation()
  { return m_refutation; }
  
  public void setRefutation(boolean refut)
  { m_refutation = refut; }

  public void setHasParent(boolean hasParent)
  { m_hasParent = hasParent; }

  public boolean getHasParent()
  { return m_hasParent; }

  public void setSelected(boolean selected)
  { m_selected = selected; }

  public boolean isSelected()
  { return m_selected; }
  
  public boolean isDummy()
  { return dummy; }
  
  public void setDummy(boolean d)
  { dummy = d; }

  public void setLinkedStemSelected(boolean selected)
  { m_linkedStemSelected = selected; }

  public boolean isLinkedStemSelected()
  { return m_linkedStemSelected; }

  public char[] getShortLabel()
  {
    if (m_shortLabel != null) {
      return m_shortLabel.toCharArray();
    }
    return "null".toCharArray();
  }
  
  public String getShortLabelString()
  { return m_shortLabel; }

  public void setShortLabel(String shortLabel)
  { m_shortLabel = shortLabel; }

  public void setAuxObject(Object auxObject)
  { m_auxObject = auxObject; }

  public Object getAuxObject()
  { return m_auxObject; }

  public void setOffset(int offset)
  { m_offset = offset; }

  public int getOffset()
  { return m_offset; }
  
  public int getTutorStart()
  { return m_tutorStart; }
  
  public int getTutorEnd()
  { return m_tutorEnd; }
  
  public void setTutorStart(int tut)
  { m_tutorStart = tut; }
  
  public void setTutorEnd(int tut)
  { m_tutorEnd = tut; }
  
  public void setExtent(int min, int max)
  { m_xMin = min; m_xMax = max; }
  
  public int getXMin()
  { return m_xMin; }
  
  public int getXMax()
  { return m_xMax; }

   public void setExtentFullText(int min, int max)
  { m_xMinFullText = min; m_xMaxFullText = max; }
  
  public int getXMinFullText()
  { return m_xMinFullText; }
  
  public int getXMaxFullText()
  { return m_xMaxFullText; }

 public void setOwners(Set ownerList)
	{
		owners = ownerList;
	}

	public Set getOwners()
	{
		return owners;
	}

  public String getSupportLabel()
  {
    return supportLabel;
  }

  public void setSupportLabel(String supportLabel)
  {
    this.supportLabel = supportLabel;
  }

} // TreeVertex

/**
 * RoleItem is used to store the role of a node in a particular
 * diagram. E.g. diagramClass = toulmin and element = warrant.
 */
class RoleItem
{
  public String diagramClass;
  public String element;
  
  public RoleItem(String d, String e)
  {
    diagramClass = d;
    element = e;
  }
}
