/*
 * Argument.java
 *
 * Created on 18 March 2004, 10:11
 */

/**
 * The main data class for storing info required to build an argument markup.
 * There is no graphics in this class.
 * @author  growe
 */
import java.util.*;
import java.awt.geom.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class Argument
{
  DocumentBuilder documentBuilder;
  final static int indentation = 2;
  Tree tree;               // The central tree that stores the argument structure
  TreeVertex dummyRoot;    // Dummy root added during construction of a tree if the tree is in 2 or more pieces
  boolean multiRoots;      // True if tree is disjoint
  String text;             // The text being marked up
  String author;           // Author of the text
  String date;             // Date text was published
  String source;           // Source of text
  String comments;         // Comments on text by author of markup
  Set ownerList;
  Set supportLabelList;
  Vector subtreeList;
  int subtreeNumber = 0;   // Number of subtrees in the argument
  Vector freeVertexList;
  String shortLabel;
  boolean argumentSaved;
  String encoding = "UTF-8";
  Vector schemeList;       // List of schemes currently loaded
  boolean showSupportLabels;
  private boolean showCQsAnswered;
  boolean showOwners;
  boolean invertedTree;
  Vector virtualChildren;
  SubtreeFrame subtreeFrame;

  /** Creates a new instance of Argument */
  public Argument()
  {
    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      documentBuilder = factory.newDocumentBuilder();
    } catch (Exception ex)
    {

    }
    tree = new Tree();
    ownerList = new HashSet();
    supportLabelList = new HashSet();
    subtreeList = new Vector();
    freeVertexList = new Vector();
    shortLabel = "A";
    topWigmoreIndex = 0;
    schemeList = new Vector();
    showOwners = true;
    showSupportLabels = true;
    text = "";
  }

  public Tree getTree()
  {
    return tree;
  }

  public String getText()
  {
    return text;
  }

  public void setText(String s)
  {
    text = s;
  }

  public boolean getInvertedTree()
  {
    return invertedTree;
  }

  public void setInverted(boolean s)
  {
    invertedTree = s;
  }

  public String getAuthor()
  {
    return author;
  }

  public void setAuthor(String s)
  {
    author = s;
  }

  public String getDate()
  {
    return date;
  }

  public void setDate(String s)
  {
    date = s;
  }

  public String getSource()
  {
    return source;
  }

  public void setSource(String s)
  {
    source = s;
  }

  public String getComments()
  {
    return comments;
  }

  public void setComments(String s)
  {
    comments = s;
  }

  public Set getOwnerList()
  {
    return ownerList;
  }

  public void setOwnerList(Vector rows)
  {
    ownerList.addAll(rows);
  }

  public String getShortLabel()
  {
    return shortLabel;
  }

  public void setShortLabel(String s)
  {
    shortLabel = s;
  }

  public void setShowSupportLabels(boolean s)
  {
    showSupportLabels = s;
  }

  public boolean isShowSupportLabels()
  {
    return showSupportLabels;
  }

  public void setShowOwners(boolean s)
  {
    showOwners = s;
  }

  public boolean isShowOwners()
  {
    return showOwners;
  }

  public void setInvertedTree(boolean s)
  {
    invertedTree = s;
  }

  public boolean isInvertedTree()
  {
    return invertedTree;
  }

  public void emptyFreeVertexList()
  {
    freeVertexList = new Vector();
  }

  public void setArgumentSaved(boolean s)
  {
    argumentSaved = s;
  }

  public boolean isArgumentSaved()
  {
    return argumentSaved;
  }

  public void emptyTree(boolean clearFreeVertexList)
  {
    tree.clearVertexList();
    subtreeList.clear();
    if (clearFreeVertexList)
    {
      emptyFreeVertexList();
    }
    ownerList = new HashSet();
    supportLabelList = new HashSet();
    source = "";
    comments = "";
    date = null;
    author = null;
  }

  public void addDummyRoot(Vector roots)
  {
    dummyRoot = new TreeVertex(" ");
    dummyRoot.setShortLabel("DummyRoot");
    dummyRoot.setDummy(true);
    tree.addVertex(dummyRoot);
    Enumeration rootList = roots.elements();
    while (rootList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) rootList.nextElement();
      tree.addEdge(dummyRoot, vertex);
//      vertex.setParent(dummyRoot);
//      vertex.setHasParent(true);
    }
    multiRoots = true;
  }

  public TreeVertex getDummyRoot()
  {
    return dummyRoot;
  }

  public void setDummyRoot(TreeVertex v)
  {
    dummyRoot = v;
  }

  public void deleteDummyRoot()
  {
    Enumeration dummyEdges = dummyRoot.getEdgeList().elements();
    while (dummyEdges.hasMoreElements())
    {
      TreeEdge edge = (TreeEdge) dummyEdges.nextElement();
      TreeVertex vertex = edge.getDestVertex();
      dummyRoot.deleteEdge(vertex);
//      vertex.setParent(null);
//      vertex.setHasParent(false);
    }
    tree.getVertexList().remove(dummyRoot);
    dummyRoot = null;
    multiRoots = false;
  }

  /**
   * 
   */
  public Vector ownerExists(String name, int column)
  {
    Iterator ownerIter = ownerList.iterator();
    while (ownerIter.hasNext())
    {
      Vector row = (Vector) ownerIter.next();
      String rowName = (String) row.elementAt(column);
      if (rowName.equals(name))
      {
        return row;
      }
    }
    return null;
  }

  public Vector getBreadthFirstTraversal()
  {
    if (multiRoots)
    {
      return tree.breadthFirstTraversal(dummyRoot);
    } else
    {
      return tree.breadthFirstTraversal();
    }
  }

  public boolean isMultiRoots()
  {
    return multiRoots;
  }

  public void setMultiRoots(boolean m)
  {
    multiRoots = m;
  }

  public String getTla(String name)
  {
    String tla = null;
    // If name is 2 chars or less, set TLA to name.
    // If this TLA already exists, give up and return null.
    if (name.length() < 3)
    {
      if (ownerExists(name, 1) != null)
      {
        return null;
      } else
      {
        tla = new String(name);
        return tla;
      }
    }
    // If we get this far, name has >= 3 characters.
    int nameLength = name.length();
    byte[] nameBytes = name.getBytes();
    byte[] tlaBytes = new byte[3];
    for (int i = 0; i < nameLength - 2; i++)
    {
      tlaBytes[0] = nameBytes[i];
      for (int j = i + 1; j < nameLength - 1; j++)
      {
        tlaBytes[1] = nameBytes[j];
        for (int k = j + 1; k < nameLength; k++)
        {
          tlaBytes[2] = nameBytes[k];
          tla = new String(tlaBytes);
          if (ownerExists(tla, 1) == null)
          {
            return tla;
          }
        }
      }
    }

    // If we get this far, all acronyms that can be constructed
    // from characters in nameBytes have been tried and rejected.
    // We now try to find an acronym where the first 2 letters match
    // the first 2 letters in nameBytes and the last letter is another
    // character
    tlaBytes[0] = nameBytes[0];
    tlaBytes[1] = nameBytes[1];
    for (byte b = 48; b <= 121; b++)
    {
      tlaBytes[2] = b;
      tla = new String(tlaBytes);
      if (ownerExists(tla, 1) == null)
      {
        return tla;
      }
    }
    return null;
  }

  public String addToOwnerList(Vector row)
  {
    String name = (String) row.elementAt(0);
    if (ownerExists(name, 0) != null)
    {
      return null;
    }
    String tla = getTla(name);
    if (tla == null)
    {
      return null;
    }
    row.setElementAt(tla, 1);
    ownerList.add(row);
    return tla;
  }

  public Vector getOwner(String tla)
  {
    Iterator ownerIter = ownerList.iterator();
    while (ownerIter.hasNext())
    {
      Vector row = (Vector) ownerIter.next();
      String rowName = (String) row.elementAt(1);
      if (rowName.equals(tla))
      {
        return row;
      }
    }
    return null;
  }

  public void printOwnerList()
  {
    Iterator owners = ownerList.iterator();
    while (owners.hasNext())
    {
      Vector row = (Vector) owners.next();
      System.out.println("Owner: " + row + ":" + row.elementAt(0) + ", " + row.elementAt(1));
    }
    System.out.println("********************");
  }

  public void deleteOwners(Vector remList)
  {
    for (int i = 0; i < remList.size(); i++)
    {
      ownerList.remove(remList.elementAt(i));
    }
  }

  public Vector getSubtreeList()
  {
    return subtreeList;
  }

  public void setSubtreeList(Vector subList)
  {
    subtreeList = subList;
  }

  /** 
   * Checks to see if the subtree list contains a subtree with
   * the given short label. Used in reading XML files.
   */
  public boolean containsSubtreeLabel(String label)
  {
    for (int i = 0; i < subtreeList.size(); i++)
    {
      if (((Subtree) subtreeList.elementAt(i)).getShortLabel().equals(label))
      {
        return true;
      }
    }
    return false;
  }

  public Subtree getSubtreeByLabel(String label)
  {
    for (int i = 0; i < subtreeList.size(); i++)
    {
      if (((Subtree) subtreeList.elementAt(i)).getShortLabel().equals(label))
      {
        return (Subtree) subtreeList.elementAt(i);
      }
    }
    return null;
  }

  public void addXMLSubtrees()
  {
    Vector oldSubtreeList = subtreeList;
    subtreeList = new Vector();
    subtreeNumber = 0;
    for (int i = 0; i < oldSubtreeList.size(); i++)
    {
      Subtree subtree = (Subtree) oldSubtreeList.elementAt(i);
      subtree.selectEdges();
      try
      {
        Subtree newSubtree = addSubtree(subtree.getArgumentType());
        newSubtree.setCqChecks(subtree.getCqChecks());
      } catch (SubtreeException e)
      {
      }
      clearAllSelections();
    }
  }

  /**
   * Tests if id is a valid vertex ID. Must be a single or
   * double letter, or a number of Wigmore form.
   * Also checks if the ID is currently in use.
   */
  public String isValidID(String id)
  {
    boolean isWigmore = false;
    if (id == null)
    {
      return "ID is null";
    }
    // Test if id is valid Wigmore ID
    // See if it's an integer...
    try
    {
      Integer.parseInt(id);
      isWigmore = true;
    } catch (Exception ex)
    {
      // If it's not an int, might be a float
      int dotIndex;
      if ((dotIndex = id.indexOf(".")) != -1)
      {
        try
        {
          Float.parseFloat(id);
          isWigmore = true;
        } catch (Exception exx)
        {
          return "ID is not a valid Wigmore type.";
        }
      }
    }

    // If it's not Wigmore, see if it's a valid letter ID
    if (!isWigmore)
    {
      if (id.length() < 1 || id.length() > 2)
      {
        return "Premise ID must be 1 or 2 letters. Please try another ID.";
      }
      for (int i = 0; i < id.length(); i++)
      {
        if (!(Character.getType(id.charAt(i)) == Character.LOWERCASE_LETTER ||
                Character.getType(id.charAt(i)) == Character.UPPERCASE_LETTER))
        {
          return "Non-Wigmore ID must contain letters only. Please try another ID.";
        }
      }
    }
    if (tree.containsVertexID(id) != null)
    {
      return "Premise ID already in use. Please try another ID.";
    }
    // If it is Wigmore, update the maximum Wigmore index
    if (isWigmore)
    {
      updateWigmoreIndex(id);
    }
    return null;
  }

  public boolean clearEdgeSelections(Vector edgeList)
  {
    boolean selected = false;
    Enumeration edges = edgeList.elements();
    while (edges.hasMoreElements())
    {
      TreeEdge edge = (TreeEdge) edges.nextElement();
      if (edge.isSelected())
      {
        edge.setSelected(false);
        selected = true;
      }
      edge.getDestVertex().setSelected(false);
      if (edge.getDestVertex().isVirtual())
      {
        clearEdgeSelections(edge.getDestVertex().getEdgeList());
      }
    }
    return selected;
  }

  /**
   * Clears the selected flag on all vertexes and edges
   * Returns true if any vertices or edges were selected,
   * false otherwise.
   */
  public boolean clearAllSelections()
  {
    boolean selected = false;
    Vector treeTraversal = tree.getVertexList();
    if (treeTraversal == null)
    {
      return false;
    }
    Enumeration nodeList = treeTraversal.elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        vertex.setSelected(false);
        selected = true;
      }
      vertex.setLinkedStemSelected(false);
      clearEdgeSelections(vertex.getEdgeList());
      clearEdgeSelections(vertex.toulminDataEdges);
      clearEdgeSelections(vertex.toulminBackingEdges);
      clearEdgeSelections(vertex.toulminRebuttalEdges);
      clearEdgeSelections(vertex.toulminQualifierEdges);
      clearEdgeSelections(vertex.toulminWarrantEdges);
      clearEdgeSelections(vertex.wigmoreEvidenceEdges);
      clearEdgeSelections(vertex.wigmoreCorroborativeEdges);
      clearEdgeSelections(vertex.wigmoreExplanatoryEdges);
    }

    Enumeration freeVertices = freeVertexList.elements();
    while (freeVertices.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) freeVertices.nextElement();
      if (vertex.isSelected())
      {
        vertex.setSelected(false);
        selected = true;
      }
      vertex.setLinkedStemSelected(false);
    }

    Enumeration subtrees = subtreeList.elements();
    while (subtrees.hasMoreElements())
    {
      Subtree subtree = (Subtree) subtrees.nextElement();
      if (subtree.isSelected())
      {
        subtree.setSelected(false);
        selected = true;
      }
    }
    return selected;
  }

  /**
   * Creates a subtree out of all selected edges and adds it
   * to the m_subtreeList of TestTree.
   * If argType is null, we are constructing a tree interactively, so
   * a dialog will be displayed to allow selection of the argument type.
   * If argType is not null, we are reading in the subtree from an XML
   * file.
   */
  public Subtree addSubtree(ArgType argType) throws SubtreeException
  {
    int highestLayer = 100;
    Subtree subtree = new Subtree();
    subtree.setColor(Subtree.m_subtreeColors[subtreeNumber]);
    subtreeNumber = (subtreeNumber + 1) % Subtree.m_subtreeColors.length;
    int edgeCount = 0;
    TreeVertex root = null;
    Enumeration nodeList = tree.getVertexList().elements();
    boolean verticesSelected = false;
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        verticesSelected = true;
      }
      vertex.setVisited(false);
      Enumeration edges = vertex.getEdgeList().elements();
      boolean edgesSelected = false;
      while (edges.hasMoreElements())
      {
        TreeEdge edge = (TreeEdge) edges.nextElement();
        TreeVertex dest = edge.getDestVertex();
        if (edge.isSelected())
        {
          if (dest.isVirtual())
          {
            dest.setLinkedStemSelected(true);
          }
          edgesSelected = true;
          if (vertex.getLayer() < highestLayer)
          {
            highestLayer = vertex.getLayer();
            root = vertex;
          }
          subtree.addEdge(edge);
          ++edgeCount;
        }
      }
      if (vertex.isVirtual())
      {
        // If the stem of a linked argument is selected, but no branches,
        // disallow the scheme
        if (vertex.isLinkedStemSelected() && !edgesSelected)
        {
          throw new SubtreeException("Select a complete link between premises and conclusion.");
        }
        // If some branches have been selected, but not the stem, add the stem
        // to the scheme
        if (!vertex.isLinkedStemSelected() && edgesSelected)
        {
          TreeVertex parent = vertex.getParent();
          TreeEdge stemEdge = parent.getEdge(vertex);
          stemEdge.setSelected(true);
          subtree.addEdge(stemEdge);
          ++edgeCount;
          if (parent.getLayer() < highestLayer)
          {
            highestLayer = parent.getLayer();
            root = parent;
          }
        }
      }
    }
    // If we have no edges, the subtree is empty, so return
    if (edgeCount == 0)
    {
      if (verticesSelected)
      {
        throw new SubtreeException("Select support arrows to define argumentation scheme.");
      } else
      {
        throw new SubtreeException("First, select support arrows to define argumentation scheme.");
      }
    }
    // If the subtree isn't connected, it's not allowed.
    // We do a DFT starting from the root vertex, but stopping after
    // vertices connected to 'root' have been visited. That way, any
    // vertices not connected to 'root' will remain unvisited after
    // the traversal.
    traverseSubtree(root);
    // Now loop through the vertices in the subtree to see if any
    // have not been visited. If there are any, subtree is not connected
    // so don't allow it.
    Enumeration edgeList = subtree.getEdgeList().elements();
    while (edgeList.hasMoreElements())
    {
      TreeEdge edge = (TreeEdge) edgeList.nextElement();
      TreeVertex source = edge.getSourceVertex();
      TreeVertex dest = edge.getDestVertex();
      if (!source.getVisited() || !dest.getVisited())
      {
        throw new SubtreeException("Error: Premises and conclusion not connected.");
      }
    }

    // Clear all the visited flags, since they are needed in constructing
    // the info dialog below.
    nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      vertex.setVisited(false);
    }

    // If argType is provided as argument to this method,
    // we are adding a subtree read in from an XML file
    if (argType != null)
    {
      subtree.setArgumentType(argType);
      subtreeList.add(subtree);
      subtree.setRoot(root);
      return subtree;
    }

    // If we get here, the subtree is OK and we're adding the tree
    // interactively, so display dialog
    edgeList = subtree.getEdgeList().elements();
    Vector premiseList = new Vector();
    String conclusion = "";
    while (edgeList.hasMoreElements())
    {
      TreeEdge edge = (TreeEdge) edgeList.nextElement();
      TreeVertex source = edge.getSourceVertex();
      TreeVertex dest = edge.getDestVertex();
      if (!source.getVisited())
      {
        if (source.getLayer() == highestLayer)
        {
          conclusion += (String) source.getLabel();
        } else
        {
          premiseList.add((String) source.getLabel());
        }
        source.setVisited(true);
      }
      if (!dest.getVisited())
      {
        premiseList.add((String) dest.getLabel());
        dest.setVisited(true);
      }
    }
    subtreeFrame.setPremisesText(premiseList);
    subtreeFrame.setConclusionText(conclusion);
    subtreeFrame.loadArgTypeCombo();
    subtreeFrame.setVisible(true);
    if (subtreeFrame.isOKPressed())
    {
      subtree.setArgumentType(subtreeFrame.getArgumentType());
      subtreeList.add(subtree);
      subtree.setRoot(root);
    } else
    {
      throw new SubtreeException("Scheme creation cancelled");
    }
    return null;
  }

  public void showSubtreeDialog(Subtree mouseSubtree)
  {
    if (subtreeFrame == null)
    {
      subtreeFrame = new SubtreeFrame(null, this);
    }
    mouseSubtree.buildSubtreeFrame(subtreeFrame);
    subtreeFrame.setVisible(true);
    if (subtreeFrame.isOKPressed())
    {
      mouseSubtree.setArgumentType(subtreeFrame.getArgumentType());
    }
  }

  public void setSubtreeFrame(SubtreeFrame s)
  {
    subtreeFrame = s;
  }

  /**
   * Does a depth-first traversal of a tree (or graph) without
   * saving the vertices visited, and stopping after reaching a
   * dead-end in the recursive traversal from 'start'. Only those
   * vertices that are connected to 'start' will therefore be visited.
   * Used in determining if the subtree is connected.
   */
  void traverseSubtree(TreeVertex start)
  {
    if (start == null)
    {
      return;
    }
    if (start.getVisited())
    {
      return;
    }
    start.setVisited(true);
    Enumeration edgeList = start.getEdgeList().elements();
    while (edgeList.hasMoreElements())
    {
      TreeEdge nextEdge = (TreeEdge) edgeList.nextElement();
      if (nextEdge.isSelected())
      {
        TreeVertex nextVertex = nextEdge.getDestVertex();
        nextEdge.setSelected(false);
        traverseSubtree(nextVertex);
      }
    }
  }

  /**
   * Creates a subtree and adds it
   * to the m_subtreeList.
   * This version assumes that the subtree is being read in from an
   * AML file.
   */
  /*
  public void addSubtree(ArgType argType)
  {
  int highestLayer = 100;
  Subtree subtree = new Subtree();
  subtree.setColor(Subtree.m_subtreeColors[subtreeNumber]);
  subtreeNumber = (subtreeNumber + 1) % Subtree.m_subtreeColors.length;
  int edgeCount = 0;
  TreeVertex root = null;
  Vector treeTraversal = tree.breadthFirstTraversal();
  Enumeration nodeList = treeTraversal.elements();
  boolean verticesSelected = false;
  while (nodeList.hasMoreElements()) {
  TreeVertex vertex = (TreeVertex)nodeList.nextElement();
  if (vertex.isSelected())
  verticesSelected = true;
  vertex.setVisited(false);
  Enumeration edges = vertex.getEdgeList().elements();
  boolean edgesSelected = false;
  while (edges.hasMoreElements()) {
  TreeEdge edge = (TreeEdge)edges.nextElement();
  TreeVertex dest = edge.getDestVertex();
  if (edge.isSelected()) {
  if (dest.isVirtual()) {
  dest.setLinkedStemSelected(true);
  }
  edgesSelected = true;
  if (vertex.getLayer() < highestLayer) {
  highestLayer = vertex.getLayer();
  root = vertex;
  }
  subtree.addEdge(edge);
  ++edgeCount;
  }
  }
  if (vertex.isVirtual()) {
  // If the stem of a linked argument is selected, but no branches,
  // disallow the scheme
  // If some branches have been selected, but not the stem, add the stem
  // to the scheme
  if (!vertex.isLinkedStemSelected() && edgesSelected) {
  TreeVertex parent = vertex.getParent();
  TreeEdge stemEdge = parent.getEdge(vertex);
  stemEdge.setSelected(true);
  subtree.addEdge(stemEdge);
  ++edgeCount;
  if (parent.getLayer() < highestLayer) {
  highestLayer = parent.getLayer();
  root = parent;
  }
  }
  }
  }
  // If we have no edges, the subtree is empty, so return
  if (edgeCount == 0) {
  if (verticesSelected) {
  // araucaria.setMessageLabelText("Select support arrows to define argumentation scheme.");
  } else {
  //araucaria.setMessageLabelText("First, select support arrows to define argumentation scheme.");
  }
  return;
  }
  // Clear all the visited flags, since they are needed in constructing
  // the info dialog below.
  nodeList = treeTraversal.elements();
  while (nodeList.hasMoreElements()) {
  TreeVertex vertex = (TreeVertex)nodeList.nextElement();
  vertex.setVisited(false);
  }
  // If argType is provided as argument to this method,
  // we are adding a subtree read in from an XML file
  if (argType != null) {
  subtree.setArgumentType(argType);
  subtreeList.add(subtree);
  subtree.setRoot(root);
  return;
  }
  }
   */
  /**
   * Increments the short label used to label nodes
   * If the label is a single letter, it increments by following
   * the sequence A B C ... Z a b c ... z
   * When z is reached, the labels start double letters, so we have
   * AA AB AC ... AZ Aa Ab Ac ... Az BA BB BC ... etc
   * No check is made as to what happens after zz
   * This allows 2756 distinct labels - should be enough.
   *
   * Returns shortLabel before the increment
   */
  public String incrementID(String id)
  {
    String idPlus1 = null;
    int labelLength = id.length();
    char[] label = id.toCharArray();
    if (labelLength == 1)
    {
      if (label[0] == 'Z')
      {
        idPlus1 = "a";
      } else if (label[0] == 'z')
      {
        idPlus1 = "AA";
      } else
      {
        label[0]++;
        idPlus1 = new String(label);
      }
    } else if (labelLength == 2)
    {
      if (label[1] == 'Z')
      {
        label[1] = 'a';
        idPlus1 = new String(label);
      } else if (label[1] == 'z')
      {
        if (label[0] == 'Z')
        {
          label[0] = 'a';
        } else if (label[0] == 'z')
        { // We've used up all the IDs
          return null;
        } else
        {
          label[0]++;
        }
        label[1] = 'A';
        idPlus1 = new String(label);
      } else
      {
        label[1]++;
        idPlus1 = new String(label);
      }
    }
    return idPlus1;
  }

  /**
   * Locates the first available short label ID for a new free vertex.
   * Scans the current tree and existing free vertex list to find the earliest
   * unused label.
   */
  private String firstAvailableVertexID()
  {
    String firstID = "A";
    while (firstID != null)
    {
      if (tree.containsVertexID(firstID) == null &&
              freeListContainsVertexID(firstID) == null)
      {
        return firstID;
      }
      firstID = incrementID(firstID);
    }
    return firstID;
  }

  /**
   * Tests the free vertex list to see if it contains a vertex with short label of 'id'
   */
  private TreeVertex freeListContainsVertexID(String id)
  {
    for (int i = 0; i < freeVertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) freeVertexList.elementAt(i);
      if (vertex.m_shortLabel.equals(id))
      {
        return vertex;
      }
    }
    return null;
  }
  int topWigmoreIndex = 0;
  // Test if label contains a numeric component and if so,
  // whether it is greater than topWigmoreIndex
  public void updateWigmoreIndex(String label)
  {
    try
    {
      int wigIndex = Integer.parseInt(label);
      if (wigIndex > topWigmoreIndex)
      {
        topWigmoreIndex = wigIndex;
      }
    } catch (Exception ex)
    {
      int dotIndex;
      if ((dotIndex = label.indexOf(".")) != -1)
      {
        try
        {
          int wigIndex = Integer.parseInt(label.substring(0, dotIndex));
          if (wigIndex > topWigmoreIndex)
          {
            topWigmoreIndex = wigIndex;
          }
        } catch (Exception exx)
        {
        }
      }
    }
  }

  /**
   * Adds a new free vertex at the bottom of the canvas.
   * The free vertex has a label obtained from the selected text,
   * a short label generated as a consecutive char within this class,
   * and a shape reference so that the greyed out text can be un-greyed
   * if the vertex is deleted.
   */
  public void addFreeVertex(String label, GeneralPath shape, int offset, DiagramBase diagram)
  {
    // Label to use is numeric if a Wigmore diagram, letter otherwise
    if (diagram.getClass().getName().indexOf("Wigmore") == -1)
    {
      shortLabel = firstAvailableVertexID();
    } else
    {
      shortLabel = "" + (++topWigmoreIndex);
    }
    TreeVertex free = new TreeVertex(label, shortLabel);
    free.setAuxObject(shape);
    free.setOffset(offset);
    if (offset < 0)
    {
      free.setMissing(true);
    }
    freeVertexList.add(free);
  }

  /**
   * Adds an auxiliary missing premise. Used in adding a rebuttal to a
   * Toulmin diagram.
   */
  public TreeVertex addAuxiliaryVertex(String label)
  {
    shortLabel = firstAvailableVertexID();
    TreeVertex free = new TreeVertex(label, shortLabel);
    free.setAuxObject(null);
    free.setOffset(-1);
    free.setMissing(true);
    return free;
  }

  public Vector getFreeVertexList()
  {
    return freeVertexList;
  }

  public Vector getFreeVerticesInList()
  {
    Vector list = new Vector();
    for (int i = 0; i < freeVertexList.size(); i++)
    {
      list.add(freeVertexList.elementAt(i));
    }
    return list;
  }

  /**
   * Returns a Vector of isHiddenTable hashtables for all vertices in tree.   
   */
  public Vector getHiddenList()
  {
    Vector visible = new Vector();
    for (int i = 0; i < getTree().getVertexList().size(); i++)
    {
      TreeVertex vertex = (TreeVertex) getTree().getVertexList().elementAt(i);
      visible.add(new HiddenInfo(vertex.isHiddenTable, vertex.getShortLabelString()));
    }
    return visible;
  }

  /**
   * Returns a list of Strings giving the current short labels
   * in the free vertex list. Used for undo.
   */
  public Vector getFreeVerticesInListIDs()
  {
    Vector list = new Vector();
    for (int i = 0; i < freeVertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) freeVertexList.elementAt(i);
      list.add(new String(vertex.m_shortLabel));
    }
    return list;
  }

  /**
   * Returns a Vector containing the 'roles' hashtables from all free vertices.
   */
  public Vector getFreeVerticesRoles()
  {
    Vector list = new Vector();
    for (int i = 0; i < freeVertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) freeVertexList.elementAt(i);
      list.add(vertex.roles);
    }
    return list;
  }
  
  public boolean doSaveAIFFile(File currentOpenXMLFile)
  {
    SaveArgumentAif saveArgumentAif = new SaveArgumentAif(currentOpenXMLFile);
    saveArgumentAif.setArgument(this);
    saveArgumentAif.createArgInfo(text);
    saveArgumentAif.graphToFile();
    return true;
  }

  public boolean doSaveXMLFile(File currentOpenXMLFile)
  {
    // If saving as AIF, go elsewhere
    if (currentOpenXMLFile.getPath().indexOf("." + Araucaria.getAifFileSuffix()) != -1)
    {
      return doSaveAIFFile(currentOpenXMLFile);
    }
    try
    {
      FileOutputStream chosenOutput = null;
      chosenOutput = new FileOutputStream(currentOpenXMLFile);
      if (chosenOutput != null)
      {
        byte[] outBuffer = writeXMLAsBytes();
        chosenOutput.write(outBuffer, 0, outBuffer.length);
      }
      chosenOutput.flush();
      chosenOutput.close();
      argumentSaved = true;
      return true;
    } catch (Exception e)
    {
      System.out.println(e.toString());
      return false;
    }
  }

  /**
   * Uses JDOM to build the AML file.
   */
  public String writeXML()
  {
    try
    {
      Document doc = documentBuilder.newDocument();
      DOMImplementation impl = documentBuilder.getDOMImplementation();
      DocumentType argDTD = impl.createDocumentType("ARG", "SYSTEM", "argument.dtd");
      doc.appendChild(argDTD);
      Element root = doc.createElement("ARG");
      doc.appendChild(root);
      root.appendChild(doc.createProcessingInstruction("Araucaria", encoding));
      // Schemeset
      root.appendChild(jdomSchemeset(doc));

      // Text
      Element textElement = doc.createElement("TEXT");
      textElement.appendChild(doc.createTextNode(text));
      root.appendChild(textElement);

      // Argument tree
      jdomTraversal(doc, root);
      // Edata stuff
      addEdataToAML(doc, root);

      // Extract the JDOM tree as XML text
      CharArrayWriter charWriter = new CharArrayWriter();

      // Output text using 2 blanks as indent, and with line breaks
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer();

      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(charWriter);

      // This bit is needed in order for the DOCTYPE node to be written out
      if (doc.getDoctype() != null)
      {
        String systemValue = (new File(doc.getDoctype().getSystemId())).getName();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemValue);
      }
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indentation + "");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.transform(source, result);

      String charString = charWriter.toString();
      return charString;
    } catch (Exception e)
    {
      System.out.println(e.toString());
    }
    return null;
  }

  public byte[] writeXMLAsBytes()
  {
    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      documentBuilder = factory.newDocumentBuilder();
      Document doc = documentBuilder.newDocument();
      DOMImplementation impl = documentBuilder.getDOMImplementation();
      DocumentType argDTD = impl.createDocumentType("ARG", "SYSTEM", "argument.dtd");
      doc.appendChild(argDTD);
      Element root = doc.createElement("ARG");
      doc.appendChild(root);
      root.appendChild(doc.createProcessingInstruction("Araucaria", encoding));
      // Schemeset
      root.appendChild(jdomSchemeset(doc));

      // Text
      Element textElement = doc.createElement("TEXT");
      textElement.appendChild(doc.createTextNode(text));
      root.appendChild(textElement);
      // Edata stuff
      addEdataToAML(doc, root);
      // Argument tree
      jdomTraversal(doc, root);

      // Extract the JDOM tree as XML text
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

      // Output text using 2 blanks as indent, and with line breaks
      TransformerFactory tFactory =
              TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer();

      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(byteOutput);

      // This bit is needed in order for the DOCTYPE node to be written out
      if (doc.getDoctype() != null)
      {
        String systemValue = (new File(doc.getDoctype().getSystemId())).getName();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemValue);
      }
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indentation + "");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.transform(source, result);
      return byteOutput.toByteArray();
    } catch (Exception e)
    {
      System.out.println(e.toString());
      e.printStackTrace();
    }
    return null;
  }

  public byte[] writeSchemeset()
  {
    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      documentBuilder = factory.newDocumentBuilder();
      Document doc = documentBuilder.newDocument();
      DOMImplementation impl = documentBuilder.getDOMImplementation();
      DocumentType argDTD = impl.createDocumentType("ARG", "SYSTEM", "argument.dtd");
      doc.appendChild(argDTD);
      Element root = doc.createElement("ARG");
      doc.appendChild(root);
      root.appendChild(doc.createProcessingInstruction("Araucaria", encoding));
      // Schemeset
      root.appendChild(jdomSchemeset(doc));

//      // Text
//      Element textElement = doc.createElement("TEXT");
//      textElement.appendChild(doc.createTextNode(text));
//      root.appendChild(textElement);
//      // Edata stuff
//      addEdataToAML(doc, root);
//      // Argument tree
//      jdomTraversal(doc, root);

      // Extract the JDOM tree as XML text
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

      // Output text using 2 blanks as indent, and with line breaks
      TransformerFactory tFactory =
              TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer();

      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(byteOutput);

      // This bit is needed in order for the DOCTYPE node to be written out
      if (doc.getDoctype() != null)
      {
        String systemValue = (new File(doc.getDoctype().getSystemId())).getName();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemValue);
      }
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indentation + "");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.transform(source, result);
      return byteOutput.toByteArray();
    } catch (Exception e)
    {
      System.out.println(e.toString());
      e.printStackTrace();
    }
    return null;
  }

  public void addEdataToAML(Document doc, Element root)
  {
    Element edataElement = doc.createElement("EDATA");
    Element authorElement = doc.createElement("AUTHOR");
    if (author == null)
    {
      author = "null";
    }
    authorElement.appendChild(doc.createTextNode(author));
    Element dateElement = doc.createElement("DATE");
    if (date == null)
    {
      date = easyDateFormat("yyyy-MM-dd");
    }
    dateElement.appendChild(doc.createTextNode(date));
    Element sourceElement = doc.createElement("SOURCE");
    if (source == null)
    {
      source = "";
    }
    sourceElement.appendChild(doc.createTextNode(source));
    Element commentsElement = doc.createElement("COMMENTS");
    if (comments == null)
    {
      comments = "";
    }
    commentsElement.appendChild(doc.createTextNode(comments));
    edataElement.appendChild(authorElement);
    edataElement.appendChild(dateElement);
    edataElement.appendChild(sourceElement);
    edataElement.appendChild(commentsElement);
    root.appendChild(edataElement);
  }

  public String easyDateFormat(String format)
  {
    java.util.Date today = new java.util.Date();
    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(format);
    String datenewformat = formatter.format(today);
    return datenewformat;
  }

  public ArgType getArgTypeByName(String name)
  {
    if (schemeList == null)
    {
      return null;
    }
    for (int i = 0; i < schemeList.size(); i++)
    {
      ArgType argType = (ArgType) schemeList.elementAt(i);
      if (argType.getName().equals(name))
      {
        return argType;
      }
    }
    return null;
  }

  public Vector getSchemeList()
  {
    return schemeList;
  }

  public void setSchemeList(Vector s)
  {
    schemeList = s;
  }

  /**
   * Adds schemeset nodes to JDOM tree.
   */
  private Element jdomSchemeset(Document doc)
  {
    Element schemesetElement = doc.createElement("SCHEMESET");
    Enumeration argTypeList = schemeList.elements();
    while (argTypeList.hasMoreElements())
    {
      Element schemeElement = doc.createElement("SCHEME");
      schemesetElement.appendChild(schemeElement);
      Element nameElement = doc.createElement("NAME");
      schemeElement.appendChild(nameElement);
      ArgType argType = (ArgType) argTypeList.nextElement();
      nameElement.appendChild(doc.createTextNode(argType.getName()));
      Element formElement = doc.createElement("FORM");
      schemeElement.appendChild(formElement);

      Vector premiseVector = argType.getPremises();
      for (int i = 0; i < premiseVector.size(); i++)
      {
        Element premiseElement = doc.createElement("PREMISE");
        formElement.appendChild(premiseElement);
        premiseElement.appendChild(doc.createTextNode((String) premiseVector.elementAt(i)));
      }
      Element conclusionElement = doc.createElement("CONCLUSION");
      formElement.appendChild(conclusionElement);
      conclusionElement.appendChild(doc.createTextNode(argType.getConclusion()));

      Vector critQuesVector = argType.getCriticalQuestions();
      Enumeration critQuesList = critQuesVector.elements();
      while (critQuesList.hasMoreElements())
      {
        Element cqElement = doc.createElement("CQ");
        schemeElement.appendChild(cqElement);
        cqElement.appendChild(doc.createTextNode((String) critQuesList.nextElement()));
      }
    }
    return schemesetElement;
  }
  /**
   * Adds tree nodes to the JDOM tree
   */
  static String WigmoreIDPrefix = "Wigmore_";

  protected void recursiveJDOM(Document doc, Element root, TreeVertex start)
  {
    if (start.getVisited())
    {
      return;
    }
    // Add a proposition only if the vertex is not virtual.
    Element auElement = doc.createElement("AU");
    if (!start.isVirtual())
    {
      root.appendChild(auElement);
      Element propElement = doc.createElement("PROP");
      auElement.appendChild(propElement);
      String idTag = new String(start.getShortLabel());
      // If ID is numerical add Wigmore prefix to make it acceptable as XML ID
      if (idTag.charAt(0) >= '0' && idTag.charAt(0) <= '9')
      {
        idTag = WigmoreIDPrefix + idTag;
      }
      propElement.setAttribute("identifier", idTag);
      if (start.isMissing())
      {
        propElement.setAttribute("missing", "yes");
      } else
      {
        propElement.setAttribute("missing", "no");
      }
      if (start.m_nodeLabel != null)
      {
        propElement.setAttribute("nodelabel", start.m_nodeLabel);
      }
      if (start.getSupportLabel() != null)
      {
        propElement.setAttribute("supportlabel", start.getSupportLabel());
      }
      Element proptextElement = doc.createElement("PROPTEXT");
      propElement.appendChild(proptextElement);
      proptextElement.setAttribute("offset", "" + start.getOffset());
      proptextElement.appendChild(doc.createTextNode(start.getLabel().toString()));

      // Add owners of this vertex
      Set ownerSet = start.getOwners();
      Iterator ownerIter = ownerSet.iterator();
      while (ownerIter.hasNext())
      {
        Vector owner = (Vector) ownerIter.next();
        String ownerName = (String) owner.elementAt(0);
        Element ownerElement = doc.createElement("OWNER");
        propElement.appendChild(ownerElement);
        ownerElement.setAttribute("name", ownerName);
      }

      // Add schemes to which this vertex belongs
      for (int i = 0; i < subtreeList.size(); i++)
      {
        Subtree scheme = (Subtree) subtreeList.elementAt(i);
        if (scheme.containsTreeVertex(start))
        {
          Element inschemeElement = doc.createElement("INSCHEME");
          propElement.appendChild(inschemeElement);
          inschemeElement.setAttribute("scheme", scheme.getArgumentType().getName());
          inschemeElement.setAttribute("schid", "" + i);
          for (CQCheck cqc : scheme.getCqChecks())
          {
            Element cqElement = doc.createElement("CQANS");
            inschemeElement.appendChild(cqElement);
            cqElement.setAttribute("answered", cqc.isCqAnswered() ? "yes" : "no");
          }
        }
      }

      // Add in ROLE element(s) for entries in roles Hashtable

      addRoles(doc, propElement, start);

      // If node is used in a tutorial, add a TUTOR element
      if (start.getTutorStart() != start.getOffset() ||
              start.getTutorEnd() != start.getOffset() + start.getLabel().toString().length())
      {
        Element tutorElement = doc.createElement("TUTOR");
        propElement.appendChild(tutorElement);
        tutorElement.setAttribute("start", "" + start.getTutorStart());
        tutorElement.setAttribute("end", "" + start.getTutorEnd());
      }
    }
    start.setVisited(true);
    Element currentElement;
    if (start.isVirtual())
    {
      currentElement = root;
    } else
    {
      currentElement = auElement;
    }
    Element oldCurrentElement = null;

    start.putRefutationFirst();
    Enumeration edgeList = start.getEdgeList().elements();
    while (edgeList.hasMoreElements())
    {
      // Get the next edge in the traversal, and its source and dest vertices.
      TreeEdge edge = (TreeEdge) edgeList.nextElement();
      TreeVertex sourceVertex = edge.getSourceVertex();
      TreeVertex nextVertex = edge.getDestVertex();

      // If both source and dest are virtual, we have a nested virtual tree
      // so we need to add a <LA> tag

      // This should never happen
      /*
      if (sourceVertex.isVirtual() && nextVertex.isVirtual()) {
      Element laElement = new Element("LA");
      currentElement.addContent(laElement);
      oldCurrentElement = currentElement;
      currentElement = laElement;
      }
       */
      // If the source vertex in the edge is the same as the root of the subtree,
      // and neither the source nor dest vertex is virtual, then the 'start' vertex has an
      // ordinary child, so we add a <CA> tag.
      //
      // If the dest vertex is virtual, we have the beginning of a linked subtree, so
      // add a <LA> tag.
      if (sourceVertex == start && !sourceVertex.isVirtual())
      {
        if (nextVertex.isVirtual())
        {
          Element laElement = doc.createElement("LA");
          currentElement.appendChild(laElement);
          oldCurrentElement = currentElement;
          currentElement = laElement;
        } else if (nextVertex.isRefutation())
        {
          Element refutElement = doc.createElement("REFUTATION");
          currentElement.appendChild(refutElement);
          oldCurrentElement = currentElement;
          currentElement = refutElement;
        } else
        {
          Element caElement = doc.createElement("CA");
          currentElement.appendChild(caElement);
          oldCurrentElement = currentElement;
          currentElement = caElement;
        }
      }
      // Continue the traversal with a recursive call for nextVertex

      recursiveJDOM(doc, currentElement, nextVertex);
      // Upon returning from the current layer of recursion, we need to move one
      // layer up in the tree unless ONLY the start vertex is virtual.
      if (!start.isVirtual() ||
              (sourceVertex.isVirtual() && nextVertex.isVirtual()))
      {
        currentElement = oldCurrentElement;
      }
    }
  }

  private void addRoles(Document doc, Element propElement, TreeVertex vertex)
  {
    Hashtable rolesTable = vertex.roles;
    Enumeration keys = rolesTable.keys();
    while (keys.hasMoreElements())
    {
      String key = (String) keys.nextElement();
      String value = (String) rolesTable.get(key);
      Element typeElement = doc.createElement("ROLE");
      typeElement.setAttribute("class", key);
      typeElement.setAttribute("element", value);
      propElement.appendChild(typeElement);
    }
  }

  /**
   * Uses JDOM to build the tree in AML.
   * 'root' is the root of the AML file.
   */
  public void jdomTraversal(Document doc, Element root)
  {
    TreeVertex start = this.getDummyRoot();
    if (start == null)
    {
      // If tree is empty, just return
      if (tree.getRoots().size() == 0)
      {
        return;
      }
      start = (TreeVertex) tree.getRoots().firstElement();
    }
    Vector vertexList = tree.getVertexList();
    if (start == null || !vertexList.contains(start))
    {
      throw new GraphException("Starting vertex not found in tree.");
    }
    int startIndex = vertexList.indexOf(start);
    Enumeration vertexEnum = tree.prepareVertexList(start);
    while (vertexEnum.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) vertexEnum.nextElement();
      recursiveJDOM(doc, root, vertex);
    }
    tree.restoreVertexList(startIndex, start);
  }

  /**
   * Inverts the refutation flag on all selected vertices.
   * A node can only be set to be a refutation if:
   * 1. It has a parent
   * 2. Its parent has no current refutation children
   * 3. Its parent is not virutal.
   */
  public String setRefutations()
  {
    boolean nodeSelected = false;
    if (tree.getRoots().size() == 0)
    {
      return "Diagram is empty";
    }
    Vector traversal = tree.breadthFirstTraversal();
    Enumeration nodeList = traversal.elements();
    tree.clearAllVisited();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        nodeSelected = true;
        TreeVertex parent = vertex.getParent();
        if (parent != null && parent.getNumRefutations() == 0 &&
                !parent.isVirtual() && !vertex.isRefutation())
        {
          vertex.setRefutation(true);
          vertex.setVisited(true);
        } else if (vertex.isRefutation())
        {
          vertex.setRefutation(false);
          vertex.setVisited(true);
        } else
        {
          if (parent == null)
          {
            return "Final conclusion cannot be a refutation.";
          } else if (parent.getNumRefutations() > 0)
          {
            return "Each conclusion can have at most one refutation.";
          } else if (parent.isVirtual())
          {
            return "Linked premise cannot be a refutation.";
          }
        }
      }
      // Check edges 
      Enumeration edgeEnum = vertex.getEdgeList().elements();
      while (edgeEnum.hasMoreElements())
      {
        TreeEdge edge = (TreeEdge) edgeEnum.nextElement();
        if (edge.isSelected())
        {
          nodeSelected = true;
          TreeVertex parent = edge.getSourceVertex();
          TreeVertex child = edge.getDestVertex();
          if (parent != null && parent.getNumRefutations() == 0 &&
                  !parent.isVirtual() && !child.isRefutation() && !child.getVisited())
          {
            child.setRefutation(true);
          } else if (child.isRefutation())
          {
            child.setRefutation(false);
          } else
          {
            if (parent.getNumRefutations() > 0)
            {
              return "Each conclusion can have at most one refutation.";
            } else if (parent.isVirtual())
            {
              return "Linked premise cannot be a refutation.";
            }
          }
        }
      }
    }
    if (!nodeSelected)
    {
      return "Select component(s) to become refutation(s)";
    }
    tree.clearAllVisited();
    return "Refutation status toggled";
  }

  /**
   * Selects all nodes that are parents of a Toulmin rebuttal if the parent
   * is an added negation.
   */
  public int selectAddedNodes()
  {
    int numSelected = 0;
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      // Select a vertex's parent if vertex is a Toulmin rebuttal and parent is 
      // an added node
      if (vertex.isSelected())
      {
        String toulminRole = (String) vertex.roles.get("toulmin");
        if (toulminRole.equals("rebuttal"))
        {
          TreeVertex parent = vertex.getParent();
          String addedNegation = (String) parent.roles.get("addedNegation");
          if (addedNegation != null && addedNegation.equals("yes"))
          {
            parent.setSelected(true);
            numSelected++;
          }
        }
      }
    }
    return numSelected;
  }

  /**
   * Deletes all selected vertices, edges and subtrees.
   * Deleting a vertex also deletes all edges connected to it.
   * Deleting an edge may move a vertex back to the free vertex list
   * if that edge was the only edge joined to the vertex.
   * Returns true if something was actually deleted.
   */
  public boolean deleteSelectedItems()
  {
    boolean deletedSomething = false;
    // Remove vertex if it is in the free vertex list
    Enumeration nodeList = freeVertexList.elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        freeVertexList.remove(vertex);
        nodeList = freeVertexList.elements();
        deletedSomething = true;
        setShortLabel(getMaxShortLabel());
        incrementID(getMaxShortLabel());
      }
    }
    if (tree.getVertexList().size() == 0)
    {
      return deletedSomething;
    }

    selectAddedNodes();
    assignUndoOrder();
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      // Delete selected vertexes
      if (vertex.isSelected())
      {
        removeVertexAndEdges(vertex);
        deletedSomething = true;
        // Required since deleting the vertex skips the next one in the list
        i--;
      } else
      {
        // Check for selected edges and delete them
        Enumeration edges = vertex.getEdgeList().elements();
        while (edges.hasMoreElements())
        {
          TreeEdge edge = (TreeEdge) edges.nextElement();
          if (edge.isSelected())
          {
            // If delVertex has a virtual child, delete it
            TreeVertex dest = edge.getDestVertex();
            if (dest.isVirtual())
            {
              removeVertexAndEdges(dest);
            }
            deleteEdge(edge);
            deletedSomething = true;
            // Needed since deleting an edge messes up the Enumeration
            edges = vertex.getEdgeList().elements();
          }
        }
      }
    }

    removeSingleVirtuals();

    Enumeration subList = subtreeList.elements();
    while (subList.hasMoreElements())
    {
      Subtree subtree = (Subtree) subList.nextElement();
      if (subtree.isSelected())
      {
        subtreeList.remove(subtree);
        subList = subtreeList.elements();
        deletedSomething = true;
      }
    }
    if (multiRoots)
    {
      deleteDummyRoot();
      multiRoots = false;
    }
    Vector roots = tree.getRoots();
    if (roots.size() > 1)
    {
      if (tree.getDummyRoot() == null)
      {
        addDummyRoot(roots);
      }
    }
    setSubtreeList(subtreeList);
    if (doDeleteQualifier() > 0)
    {
      deletedSomething = true;
    }
    vertexList = tree.getVertexList();
    return deletedSomething;
  }

  public int doDeleteQualifier()
  {
    int delCount = 0;
    Vector nodeList = getTree().getVertexList();
    for (int i = 0; i < nodeList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) nodeList.elementAt(i);
      Vector qualEdges = vertex.toulminQualifierEdges;
      for (int j = 0; j < qualEdges.size(); j++)
      {
        TreeVertex qual = ((TreeEdge) qualEdges.elementAt(j)).getDestVertex();
        if (qual.isSelected())
        {
          TreeVertex qualSource = ((TreeEdge) qualEdges.elementAt(j)).getSourceVertex();
          TreeVertex qualSourceParent = qualSource.getParent();
          TreeEdge qualEdge = qualSourceParent.getEdge(qualSource);
          qualEdge.getDestVertex().setSupportLabel(null);
          delCount++;
        }
      }
    }
    return delCount;
  }

  /**
   * Scans the tree and adjusts the roles of nodes after a deletion action.
   */
  public void adjustRoles()
  {
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      TreeVertex parent = vertex.getParent();
      // If vertex is a root, must be a data node
      if (parent == null || parent.isDummy())
      {
        vertex.roles.put("toulmin", "data");
      }
    }
  }

  /**
   * Constructs a Wigmore diagram from a standard diagram by 
   * looking at the roles of each node.
   * A node can have up to 3 edges leading into it, each of which 
   * can be connected to a number of other evidence nodes.
   * Since the group of these evidence nodes can itself have an
   * evaluation, we need a way of attaching an evaluation to the
   * group as well as to each individual node within the group.
   * We do this by introducing a virtual node if a group contains
   * more than a single node. This virtual node then has all the real
   * nodes attached to it.
   * 
   */
  public void standardToWigmore()
  {
    Tree tree = getTree();
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      vertex.clearWigmoreLists();
      // Refutations created from non-evidence nodes must be converted to evidence nodes
      if (vertex.isRefutation() && ((String) vertex.roles.get("wigmore")).indexOf("evidence") == -1)
      {
        vertex.roles.put("wigmore", "evidenceTestAffirm");
      }
    }
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      if (vertex.isVirtual())
      {
        continue;
      }
      Vector edges = vertex.getEdgeList();
      for (int j = 0; j < edges.size(); j++)
      {
        TreeEdge edge = (TreeEdge) edges.elementAt(j);
        TreeVertex dest = edge.getDestVertex();
        // If both vertexes are non-virtual, we have a CA, so
        // we need to examine the wigmore role
        // Wigmore roles begin with 'evidence' if they are to be
        // drawn below the node
        if (!vertex.isVirtual())
        {
          if (!dest.isVirtual())
          {
            if (dest.roles.get("addedNegation") != null &&
                    dest.roles.get("addedNegation").equals("yes"))
            {
              if (dest.getEdgeList().size() > 1)
              {
                dest.isHiddenTable.put("wigmore", "false");
              }
              if (dest.isHiddenTable.get("wigmore").equals("true"))
              {
                TreeVertex hiddenChild = ((TreeEdge) dest.getEdgeList().elementAt(0)).getDestVertex();
                addWigmoreEdge(vertex, hiddenChild, new TreeEdge(vertex, hiddenChild));
              } else
              {
                addWigmoreEdge(vertex, dest, edge);
              }
            } else
            {
              if (!(vertex.roles.get("addedNegation") != null && vertex.roles.get("addedNegation").equals("yes") &&
                      vertex.isHiddenTable.get("wigmore").equals("true")))
              {
                addWigmoreEdge(vertex, dest, edge);
              }
            }
          } else
          {  // Handle LAs
            for (int k = 0; k < dest.getEdgeList().size(); k++)
            {
              TreeEdge virtEdge = (TreeEdge) dest.getEdgeList().elementAt(k);
              TreeVertex virtDest = virtEdge.getDestVertex();
              addWigmoreEdge(vertex, virtDest, new TreeEdge(vertex, virtDest));
            }
          }
        }
      }

      // Lists have been constructed, so we now need to see if any of
      // them contain > 1 node. If so, we introduce a virtual node and
      // shift all real nodes to be children of the virtual node.
      groupWigmoreEdges(vertex, vertex.wigmoreEvidenceEdges);
      groupWigmoreEdges(vertex, vertex.wigmoreCorroborativeEdges);
      groupWigmoreEdges(vertex, vertex.wigmoreExplanatoryEdges);
    }
  }

  private void addWigmoreEdge(TreeVertex vertex, TreeVertex dest, TreeEdge edge)
  {
    String destRole = (String) dest.roles.get("wigmore");
    if (destRole.indexOf("evidence") != -1)
    {
      vertex.wigmoreEvidenceEdges.add(edge);
    } else if (destRole.indexOf("explanatory") != -1)
    {
      vertex.wigmoreExplanatoryEdges.add(edge);
    } else if (destRole.indexOf("corroborative") != -1)
    {
      vertex.wigmoreCorroborativeEdges.add(edge);
    }
  }

  /**
   * If edges contains > 1 node, creates a virtual node and attaches
   * all destination vertices in edges to this node. Clears edges,
   * then adds a single edge from source vertex to the virtual node.
   */
  public void groupWigmoreEdges(TreeVertex vertex, Vector<TreeEdge> edges)
  {
    if (edges != null && edges.size() > 1)
    {
      TreeVertex virtual = new TreeVertex("virtual");
      virtual.setVirtual(true);
      for (TreeEdge edge : edges)
      {
        TreeVertex dest = edge.getDestVertex();
        virtual.addEdge(dest);
      }
      edges.clear();
      edges.add(new TreeEdge(vertex, virtual));
      virtual.setParent(vertex);
      virtual.setHasParent(true);
    }
  }

  /**
   * Creates a Toulmin diagram from a standard diagram
   */
  public void standardToToulmin()
  {
    Tree tree = getTree();
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      vertex.clearToulminLists();
    }
    adjustRoles();
    // Build data and warrant lists
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      Vector edges = vertex.getEdgeList();
      for (int j = 0; j < edges.size(); j++)
      {
        TreeEdge edge = (TreeEdge) edges.elementAt(j);
        TreeVertex dest = edge.getDestVertex();
        // If both vertexes are non-virtual, we have a CA, so
        // add a data edge
        if (!vertex.isVirtual() && !dest.isVirtual())
        {
          if (dest.roles.get("toulmin").equals("data"))
          {
            vertex.toulminDataEdges.add(edge);
          // If dest has toulmin role of rebuttal but is not a refutation
          // it has been toggled back to a normal data node in a standard diagram editor
          } else if (dest.roles.get("toulmin").equals("rebuttal") && !dest.isRefutation())
          {
            dest.roles.put("toulmin", "data");
            vertex.toulminDataEdges.add(edge);
          } else if (dest.roles.get("toulmin").equals("backing"))
          {
            vertex.toulminBackingEdges.add(new TreeEdge(vertex, dest));
          }
        // If source is non-virtual and dest is virtual,
        // restore the supports of source.
        } else if (!vertex.isVirtual() && dest.isVirtual())
        {
          vertex.restoreToulminEdges(dest);
        }
      }
    }

    // Create and attach qualifiers
    // A qualifier node in Toulmin is the supportlabel (evaluation) in a
    // standard diagram. These are not nodes in a standard diagram so we
    // create a TreeVertex to represent them in Toulmin.
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      if (vertex.getSupportLabel() != null)
      {
        TreeVertex qualifier = new TreeVertex(vertex.getSupportLabel());
        qualifier.roles.put("toulmin", "qualifier");
        vertex.toulminQualifierEdges.add(new TreeEdge(vertex, qualifier));
      }
    }

    // Convert refutations into rebuttals
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      if (vertex.isRefutation())
      {
        vertex.refutationToRebuttal();
      }
    }
  }

  /**
   * Finds the maximum short label currently in use in either the
   * free vertex list or the tree.
   */
  public String getMaxShortLabel()
  {
    String maxLabel = "A";
    for (int i = 0; i < freeVertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) freeVertexList.elementAt(i);
      String label = vertex.m_shortLabel;
      if (label.length() > maxLabel.length())
      {
        maxLabel = label;
      } else if (label.length() == maxLabel.length())
      {
        if (label.compareTo(maxLabel) > 0)
        {
          maxLabel = label;
        }
      }
    }
    if (tree.getVertexList().size() == 0)
    {
      return maxLabel;
    }
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      String label = vertex.m_shortLabel;
      if (label.length() > maxLabel.length())
      {
        maxLabel = label;
      } else if (label.length() == maxLabel.length())
      {
        if (label.compareTo(maxLabel) > 0)
        {
          maxLabel = label;
        }
      }
    }
    return maxLabel;
  }

  /**
   * Assigns an undo order to all edges in the tree. Used to put edges
   * back in the right place in an undo.
   */
  private void assignUndoOrder()
  {
    Vector vertexList = tree.getVertexList();
    Enumeration nodeList = vertexList.elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      Vector edgeList = vertex.getEdgeList();
      for (int i = 0; i < edgeList.size(); i++)
      {
        TreeEdge edge = (TreeEdge) edgeList.elementAt(i);
        edge.undoOrder = i;
      }
    }
  }

  /**
   * Scans the tree and removes any virtual nodes that have either 0 or 1
   * child. Used after deleting selected vertices or edges to clean up
   * the tree.
   */
  private void removeSingleVirtuals()
  {
    Enumeration vertexList = tree.getVertexList().elements();
    while (vertexList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) vertexList.nextElement();
      if (vertex.isVirtual())
      {
        if (vertex.getEdgeList().size() == 0)
        {
          removeVertexAndEdges(vertex);
          vertexList = tree.getVertexList().elements();
        } else if (vertex.getEdgeList().size() == 1)
        // If one child in virtual edge list, convert this to a CA
        {
          TreeVertex parent = vertex.getParent();
          TreeVertex dest = ((TreeEdge) vertex.getEdgeList().firstElement()).getDestVertex();
          TreeEdge parentEdge = parent.getEdge(vertex);
          parentEdge.setDestVertex(dest);
          dest.setParent(parent);
          dest.roles.put("toulmin", "data");
          removeVertexAndEdges(vertex);
          vertexList = tree.getVertexList().elements();
        }
      }
    }
  }

  /**
   * Deletes all edges connected to vertex, then deletes
   * the vertex itself
   */
  private void removeVertexAndEdges(TreeVertex delVertex)
  {
    // Remove all edges starting at this vertex
    Enumeration delEdges = delVertex.getEdgeList().elements();
    while (delEdges.hasMoreElements())
    {
      TreeEdge edge = (TreeEdge) delEdges.nextElement();
      TreeVertex dest = edge.getDestVertex();
      // If delVertex has a virtual child, delete it
      if (dest.isVirtual())
      {
        removeVertexAndEdges(dest);
      } else
      {
        // Switch off the refutation flag of any child node
        dest.setRefutation(false);
        dest.setHasParent(false);
        dest.setSupportLabel(null);
      }
      removeEdgeFromSubtrees(edge);
    }
    Vector edgeList = delVertex.getEdgeList();
    delVertex.getEdgeList().removeAllElements();

    // Remove edges ending at this vertex
    Enumeration nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      edgeList = vertex.getEdgeList();
      Enumeration edges = edgeList.elements();
      while (edges.hasMoreElements())
      {
        TreeEdge edge = (TreeEdge) edges.nextElement();
        TreeVertex destVertex = edge.getDestVertex();
        if (destVertex == delVertex)
        {
          removeEdgeFromSubtrees(edge);
          // If the source vertex is the virtual root, don't add the edge to deleteAction
          edgeList.remove(edge);
          edges = edgeList.elements();
        }
      }
    }
    tree.getVertexList().remove(delVertex);
  }

  private void deleteEdge(TreeEdge edge)
  {
    TreeVertex dest = edge.getDestVertex();
    dest.setHasParent(false);
    dest.setRefutation(false);
    dest.setSupportLabel(null);
    removeEdgeFromSubtrees(edge);
    edge.getSourceVertex().getEdgeList().remove(edge);
  }

  /**
   * Removes an edge from any subtree containing it.
   * If this reduces the number of edges in the subtree to zero,
   * the subtree is removed from m_subtreeList
   */
  public void removeEdgeFromSubtrees(TreeEdge edge)
  {
    Enumeration subtreeElem = subtreeList.elements();
    while (subtreeElem.hasMoreElements())
    {
      Subtree subtree = (Subtree) subtreeElem.nextElement();
      subtree.deleteEdge(edge);
      if (subtree.getNumberOfEdges() == 0)
      {
        subtreeList.remove(subtree);
        subtreeElem = subtreeList.elements();
      }
    }
  }

  /**
   * Checks for selected edges and vertices and if appropriate,
   * unlinks them from virtual nodes. The process must do a number of
   * checks:
   *
   * 1. If the selected edge connects a virtual node to its single parent,
   *    the virtual node is deleted along with all the edges that linked it
   *    to its children, and all its children are reconnected by
   *    normal edges to the single parent.
   *
   * 2. If the selected edge connects a virtual node to one of its children,
   *    the edge is deleted, and the child is reconnected by a normal edge
   *    to the parent of the virtual node. After this process, a check is
   *    made to see how many children remain connected to the virtual node.
   *    If this number is 0 or 1, the virtual node is deleted and the
   *    remaining child (if the number is 1) is reconnected to the parent
   *    of the virtual node.
   *
   * 3. If a selected vertex has a virtual node as a parent, the edge
   *    connecting the vertex to the virtual node is deleted following
   *    the algorithm in (2) above. That is, the vertex becomes a normal
   *    child of the parent of the virtual node.
   *
   * 4. Any selected edge that is not connected to a virtual node is
   *    deselected without any other action being taken.
   *
   * 5. Any selected vertex that is not the child of a virtual node is
   *    deselected without any further action being taken.
   */
  public Vector unlinkVertices() throws LinkException
  {
    Vector virtualChildren = new Vector();
    Vector vertexList = tree.getVertexList();
    Enumeration vertexEnum = vertexList.elements();
    while (vertexEnum.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) vertexEnum.nextElement();
      // Treat all selected vertices, even if they are virtual
      if (vertex.isSelected())
      {
        TreeVertex parent = vertex.getParent();
        if (parent == null || !parent.isVirtual())
        {
          vertex.setSelected(false);
          throw new LinkException("No linked premises selected.");
        } else
        {
          TreeEdge edge = parent.edgeExists(vertex);
          if (unlinkEdge(edge, tree, virtualChildren))
          {
            vertex.setSelected(false);
            vertexEnum = vertexList.elements();
            TreeVertex dest = edge.getDestVertex();
            if (!dest.isVirtual())
            {
              virtualChildren.add(dest);
            }
          }
        }
      }
      // Check edges from all vertices, selected or not
      Enumeration edgeEnum = vertex.getEdgeList().elements();
      while (edgeEnum.hasMoreElements())
      {
        TreeEdge edge = (TreeEdge) edgeEnum.nextElement();
        if (edge.isSelected())
        {
          if (edge.getDestVertex().isVirtual() || edge.getSourceVertex().isVirtual())
          {
            if (unlinkEdge(edge, tree, virtualChildren))
            {
              TreeVertex dest = edge.getDestVertex();
              if (!dest.isVirtual())
              {
                virtualChildren.add(dest);
              }
            }
            edge.setSelected(false);
            vertexEnum = vertexList.elements();
          } else
          {
            edge.setSelected(false);
          }
        }
      }
    }
    // Remove any vertexes that still have a virtual parent
    for (int i = 0; i < virtualChildren.size(); i++)
    {
      TreeVertex parent = ((TreeVertex) virtualChildren.elementAt(i)).getParent();
      if (parent.isVirtual())
      {
        virtualChildren.remove(i);
        i--;
      }
    }
    if (virtualChildren.size() > 0)
    {
      return virtualChildren;
    } else
    {
      throw new LinkException("No linked premises selected");
    }
  }

  /**
   * Should only be called if it is known that either the source or
   * destination vertex of the edge is virtual.
   */
  public boolean unlinkEdge(TreeEdge edge, Tree tree, Vector virtualChildren)
  {
    if (edge == null)
    {
      return false;
    }
    TreeVertex dest = edge.getDestVertex();
    TreeVertex source = edge.getSourceVertex();

    // Virtual source - must delete the edge and reattach the dest
    // vertex to the parent of the source vertex.
    if (source.isVirtual())
    {
      TreeVertex parentOfVirtual = source.getParent();
      deleteEdge(edge);
      TreeEdge newEdge = new TreeEdge(parentOfVirtual, dest);
      tree.addEdge(parentOfVirtual, dest);
      // Restore the data edge in the Toulmin diagram as well
//      parentOfVirtual.toulminDataEdges.add(newEdge);
      dest.setHasParent(true);
      dest.setParent(parentOfVirtual);
      dest.setSelected(false);
      int numVirtualChildren = source.getEdgeList().size();
      switch (numVirtualChildren)
      {
        case 1:
          // Delete remaining edge from virtual node
          // Do this by selecting the last vertex so that unlinkVertices() will
          // pick it up
          ((TreeEdge) source.getEdgeList().firstElement()).getDestVertex().setSelected(true);
          break;
        case 0:
          // If no vertices left, delete the virtual node
          removeVertexAndEdges(source);
          break;
        default:
          // If more than one child left on the virtual node, add it to
          // the virtualChildren Vector so that undo can handle the case
          // of a single unlinked edge
          Vector edgeList = source.getEdgeList();
          for (int i = 0; i < edgeList.size(); i++)
          {
            TreeEdge virtEdge = (TreeEdge) edgeList.elementAt(i);
            virtualChildren.add(virtEdge.getDestVertex());
          }
          break;
      }
      // We return here, since if an edge has *both* ends virtual, we don't want
      // the lower virtual node processed.
      return true;
    }

    // Virtual destination - delete edge, virtual destination node,
    // all edges arising from virtual destination, and reattach all
    // children of virtual node to the source node.
    // Do this by selecting all the children of the virtual node, and then
    // rely on unlinkVertices() to do the work for us.
    if (dest.isVirtual())
    {
      Enumeration childEnum = dest.getEdgeList().elements();
      while (childEnum.hasMoreElements())
      {
        TreeEdge childEdge = (TreeEdge) childEnum.nextElement();
        TreeVertex child = childEdge.getDestVertex();
        child.setSelected(true);
      }
      return true;
    }
    // Will only get here if neither end is virtual, which shouldn't happen.
    return false;
  }

  /**
   * Checks for selected edges or vertices and if appropriate,
   * creates a virtual node that links these vertices to their common parent.
   * If an edge is selected, the upper vertex must be the parent of all other
   * edges. If a vertex is selected, it must have an edge with the other vertex
   * == to parent vertex.
   * Returns Vector of linked nodes if successful, null otherwise.
   * Method will fail if:
   * Not all edges have a common parent.
   */
  public Vector linkVertices() throws LinkException
  {
    TreeVertex parentVertex = findParentVertex();
    if (parentVertex == null)
    {
      return null;
    }
    if (virtualChildren.size() <= 1)
    {
      throw new LinkException("Must have at least 2 premises");
    }
    doLinkVertices(parentVertex, virtualChildren);
    return virtualChildren;
  }

  public void doLinkVertices(TreeVertex parentVertex, Vector virtualChildren)
  {
    TreeVertex virtualNode = new TreeVertex("");
    virtualNode.setVirtual(true);
    virtualNode.setShortLabel("V");
    Vector vertexList = tree.getVertexList();

    // Remove existing edges between children and 'real' parent
    Enumeration edgeList = parentVertex.getEdgeList().elements();
    while (edgeList.hasMoreElements())
    {
      TreeEdge edge = (TreeEdge) edgeList.nextElement();
      if (virtualChildren.contains(edge.getDestVertex()))
      {
        deleteEdge(edge);
        edgeList = parentVertex.getEdgeList().elements();
      }
    }

    // Connect the virtual node to the parent node
    virtualNode.setHasParent(true);
    tree.addVertex(virtualNode);
    tree.addEdge(parentVertex, virtualNode);
    virtualNode.setParent(parentVertex);

    // Connect all the children to the virtual node
    Enumeration childList = virtualChildren.elements();
    while (childList.hasMoreElements())
    {
      if (multiRoots)
      {
        deleteDummyRoot();
        multiRoots = false;
      }
      TreeVertex destVertex = (TreeVertex) childList.nextElement();
      tree.addEdge(virtualNode, destVertex);
      destVertex.setHasParent(true);
      destVertex.setParent(virtualNode);
      TreeVertex root;
      Vector roots = tree.getRoots();
      if (roots.size() > 1)
      {
        addDummyRoot(roots);
        root = dummyRoot;
      } else
      {
        root = (TreeVertex) roots.firstElement();
      }
    }
  }

  /**
   * Identify the parent vertex. Two cases to consider:
   * 1. A selected vertex. Candidate is the parent of this vertex.
   * 2. A selected edge. Candidate is the end-vertex of the edge with the
   *    smaller layer number.
   * In order for parent vertex to be found, all candidates must be the same.
   */
  private TreeVertex findParentVertex() throws LinkException
  {
    TreeVertex parentVertex = null;
    virtualChildren = new Vector();
    // Check selected vertexes
    Enumeration nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        TreeVertex currentParent = vertex.getParent();
        if (currentParent == null || currentParent.isVirtual())
        {
//          System.out.println("Parent of " + vertex.getShortLabelString() + " is " +
//            currentParent);
          throw new LinkException("Cannot use root or virtual vertex.");
        }
        if (parentVertex == null)
        {
          parentVertex = currentParent;
        } else if (parentVertex != currentParent)
        {  // Error: parents not all the same
          throw new LinkException("Not all selected premises have the same conclusion.");
        }
        // If vertex's parent matches the parentVertex, add the child
        // to the list of potential children for the new virtual node
        virtualChildren.add(vertex);
        vertex.setSelected(false);
      }

      // Check edges for each vertex
      Enumeration edgeList = vertex.getEdgeList().elements();
      while (edgeList.hasMoreElements())
      {
        TreeEdge edge = (TreeEdge) edgeList.nextElement();
        if (edge.isSelected())
        {
          TreeVertex currentParent = edge.getSourceVertex();
          TreeVertex currentChild = edge.getDestVertex();
          if (currentParent == null || currentParent.isVirtual() ||
                  currentChild.isVirtual())
          {
            throw new LinkException("Cannot use root or virtual vertex.");
          }
          if (parentVertex == null)
          {
            parentVertex = currentParent;
          } else if (parentVertex != currentParent)
          {  // Error: parents not all the same
            throw new LinkException("All selected premises must have the same conclusion.");
          }
          // If edge's parent matches the parentVertex, add the child
          // to the list of potential children for the new virtual node.
          // Check that vertex is not already in the list (vertex could
          // have been selected in addition to edge)
          if (!virtualChildren.contains(edge.getDestVertex()))
          {
            virtualChildren.add(edge.getDestVertex());
          }
        }
      }
    }
    if (virtualChildren.size() == 0)
    {
      throw new LinkException("No premises selected.");
    }
    return parentVertex;
  }

  public void selectAllNodes()
  {
    if (tree.getVertexList().size() == 0)
    {
      return;
    }
    Vector vertexList = tree.getVertexList();
    for (int i = 0; i < vertexList.size(); i++)
    {
      TreeVertex vertex = (TreeVertex) vertexList.elementAt(i);
      if (!vertex.isVirtual())
      {
        vertex.setSelected(true);
      }
    }
  }

  /**
   * Returns a Vector of Strings containing the text of the selected
   * propositions
   */
  public Vector getSelectedProps()
  {
    Vector propList = new Vector();
    Enumeration nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        propList.add(vertex.getLabel());
      }
    }
    return propList;
  }

  /**
   * Creates a Set containing the owners of all selected vertices.
   */
  public Set getSelectedVertexOwners()
  {
    HashSet owners = new HashSet();
    Enumeration nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        owners.addAll(vertex.getOwners());
      }
    }
    return owners;
  }

  /**
   * Adds 'owners' to the owners set of each selected vertex.
   */
  public void addOwnersToSelected(Araucaria araucaria, Vector owners)
  {
    if (freeVerticesSelected())
    {
      araucaria.setMessageLabelText("Owners cannot be assigned to unattached premises.");
    }
    Enumeration nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        vertex.getOwners().addAll(owners);
      }
    }
  }

  private boolean freeVerticesSelected()
  {
    Enumeration freeVertices = freeVertexList.elements();
    while (freeVertices.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) freeVertices.nextElement();
      if (vertex.isSelected())
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Deletes owners in remList from vertices. If "selectedOnly"
   * is true, deletes owners ONLY from selected vertices; otherwise
   * deletes owners from ALL vertices in the tree.
   */
  public void deleteOwners(Vector remList, boolean selectedOnly)
  {
    Enumeration nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (selectedOnly && !vertex.isSelected())
      {
        continue;
      }
      for (int i = 0; i < remList.size(); i++)
      {
        vertex.getOwners().remove((Vector) remList.elementAt(i));
      }
    }
  }

  /**
   * Returns a Vector of TreeVertex containing all vertices selected
   */
  public Vector getSelectedVertices()
  {
    Vector vertexList = new Vector();
    Enumeration nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (vertex.isSelected())
      {
        vertexList.add(vertex);
      }
    }
    return vertexList;
  }

  /**
   * Returns a Vector of TreeEdge containing all selected edges
   */
  public Vector getSelectedEdges()
  {
    Vector edgeList = new Vector();
    Enumeration nodeList = tree.getVertexList().elements();
    // For each vertex...
    while (nodeList.hasMoreElements())
    {
      // Get its edge list...
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      Enumeration edges = vertex.getEdgeList().elements();
      // For each edge in the list...
      while (edges.hasMoreElements())
      {
        TreeEdge edge = (TreeEdge) edges.nextElement();
        if (edge.isSelected())
        {
          edgeList.add(edge);
        }
      }
    }
    return edgeList;
  }

  private void findSelectedWigmoreFromVirtual(Vector edgeList, Vector checkList)
  {
    if (checkList.size() == 1)
    {
      TreeVertex dest = ((TreeEdge) checkList.elementAt(0)).getDestVertex();
      if (dest.isVirtual())
      {
        for (int i = 0; i < dest.getEdgeList().size(); i++)
        {
          TreeEdge edge = (TreeEdge) dest.getEdgeList().elementAt(i);
          if (edge.isSelected())
          {
            edgeList.add(edge);
          }
        }
      }
    }
  }

  public Vector getSelectedWigmoreEdges()
  {
    Vector edgeList = new Vector();
    Enumeration nodeList = getBreadthFirstTraversal().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      if (isMultiRoots() && vertex.getLayer() == 0)
      {
        continue;
      }
      for (TreeEdge edge : vertex.wigmoreCorroborativeEdges)
      {
        if (edge.isSelected())
        {
          edgeList.add(edge);
        }
      }
      findSelectedWigmoreFromVirtual(edgeList, vertex.wigmoreCorroborativeEdges);
      for (TreeEdge edge : vertex.wigmoreEvidenceEdges)
      {
        if (edge.isSelected())
        {
          edgeList.add(edge);
        }
      }
      findSelectedWigmoreFromVirtual(edgeList, vertex.wigmoreEvidenceEdges);
      for (TreeEdge edge : vertex.wigmoreExplanatoryEdges)
      {
        if (edge.isSelected())
        {
          edgeList.add(edge);
        }
      }
      findSelectedWigmoreFromVirtual(edgeList, vertex.wigmoreExplanatoryEdges);
    }
    return edgeList;
  }

  /**
   * Scans all vertices in the tree and builds a set of vertex
   * and edge labels, without duplication
   */
  public void buildSupportLabelList()
  {
    supportLabelList = new HashSet();
    if (tree.getVertexList().size() == 0)
    {
      return;
    }
    Enumeration nodeList = tree.getVertexList().elements();
    while (nodeList.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) nodeList.nextElement();
      addToSupportLabelList(vertex.m_nodeLabel);
      addToSupportLabelList(vertex.getSupportLabel());
    }
  }

  public boolean supportLabelExists(String name)
  {
    Iterator iter = supportLabelList.iterator();
    while (iter.hasNext())
    {
      String label = (String) iter.next();
      if (label.equals(name))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds 'name' to the support label list after checking
   * for duplication
   */
  public boolean addToSupportLabelList(String name)
  {
    if (name == null)
    {
      return false;
    }
    if (supportLabelExists(name))
    {
      return false;
    }
    supportLabelList.add(name);
    return true;
  }

  public Set getSupportLabelList()
  {
    return supportLabelList;
  }

  public boolean isShowCQsAnswered()
  {
    return showCQsAnswered;
  }

  public void setShowCQsAnswered(boolean showCQsAnswered)
  {
    this.showCQsAnswered = showCQsAnswered;
  }
}
