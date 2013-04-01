package org.simonwells.monkeypuzzle;


/**
 * <b><code>MyContentHandler</code></b> implements the SAX 
 *   <code>ContentHandler</code> interface and defines callback
 *   behavior for the SAX callbacks associated with an XML
 *   document's content.
 */
import org.xml.sax.*;
import java.awt.geom.*;
import java.util.*;

class XMLContentHandler implements ContentHandler
{
  /** Hold onto the locator for location information */
  private Locator locator;
  private boolean readContents = false;
  String contents = "";
  Argument argument;
  Tree tree;
  Stack vertexStack = new Stack();
  TreeVertex currentVertex = null;
  GeneralPath shape = null;
  Vector argTypeVector;
  Hashtable diagramRoles;
  ArgType argType = null;

  // For PROP tag:
  String identifierString = "-", missingString = "no",
          textString, supportLabel = "", nodeLabel = "";
  String shortLabel = " ";
  boolean missing = false;
  boolean refutation = false;
  int offset, tutorStart, tutorEnd;
  // PROPTEXT
  int offsetValue = -1;
  String propText;

  public XMLContentHandler(Argument arg)
  {
    argument = arg;
    tree = arg.getTree();
  }

  /**
   * <p>
   * Provide reference to <code>Locator</code> which provides
   *   information about where in a document callbacks occur.
   * </p>
   *
   * @param locator <code>Locator</code> object tied to callback
   *                process
   */
  public void setDocumentLocator(Locator locator)
  {
//        System.out.println("    * setDocumentLocator() called");
    // We save this for later use if desired.
    this.locator = locator;
  }

  /**
   * <p>
   * This indicates the start of a Document parse - this precedes
   *   all callbacks in all SAX Handlers with the sole exception
   *   of <code>{@link #setDocumentLocator}</code>.
   * </p>
   *
   * @throws <code>SAXException</code> when things go wrong
   */
  public void startDocument() throws SAXException
  {
//        System.out.println("Parsing begins...");
  }

  /**
   * <p>
   * This indicates the end of a Document parse - this occurs after
   *   all callbacks in all SAX Handlers.</code>.
   * </p>
   *
   * @throws <code>SAXException</code> when things go wrong
   */
  public void endDocument() throws SAXException
  {
//        System.out.println("...Parsing ends.");
  }

  /**
   * <p>
   * This will indicate that a processing instruction (other than
   *   the XML declaration) has been encountered.
   * </p>
   *
   * @param target <code>String</code> target of PI
   * @param data <code>String</code containing all data sent to the PI.
   *             This typically looks like one or more attribute value
   *             pairs.
   * @throws <code>SAXException</code> when things go wrong
   */
  public void processingInstruction(String target, String data)
          throws SAXException
  {
  // TODO: restore the data encoding in Araucaria class
//        m_parent.setEncoding(data);
  }

  /**
   * <p>
   * This will indicate the beginning of an XML Namespace prefix 
   *   mapping.  Although this typically occur within the root element 
   *   of an XML document, it can occur at any point within the 
   *   document.  Note that a prefix mapping on an element triggers 
   *   this callback <i>before</i> the callback for the actual element 
   *   itself (<code>{@link #startElement}</code>) occurs.
   * </p>
   *
   * @param prefix <code>String</code> prefix used for the namespace 
   *               being reported
   * @param uri <code>String</code> URI for the namespace 
   *            being reported
   * @throws <code>SAXException</code> when things go wrong
   */
  public void startPrefixMapping(String prefix, String uri)
  {
//        System.out.println("Mapping starts for prefix " + prefix + 
//                           " mapped to URI " + uri);
  }

  /**
   * <p>
   * This indicates the end of a prefix mapping, when the namespace 
   *   reported in a <code>{@link #startPrefixMapping}</code> callback 
   *   is no longer available.
   * </p>
   *
   * @param prefix <code>String</code> of namespace being reported
   * @throws <code>SAXException</code> when things go wrong
   */
  public void endPrefixMapping(String prefix)
  {
//        System.out.println("Mapping ends for prefix " + prefix);
  }
  boolean addCQAnswers = false;
  int cqNum = 0;
  Subtree subtree = null;

  /**
   * <p>
   * This reports the occurrence of an actual element.  It will include
   *   the element's attributes, with the exception of XML vocabulary 
   *   specific attributes, such as 
   *   <code>xmlns:[namespace prefix]</code> and
   *   <code>xsi:schemaLocation</code>.
   * </p>
   *
   * @param namespaceURI <code>String</code> namespace URI this element
   *                     is associated with, or an empty 
   *                     <code>String</code>
   * @param localName <code>String</code> name of element (with no 
   *                  namespace prefix, if one is present)
   * @param rawName <code>String</code> XML 1.0 version of element name:
   *                [namespace prefix]:[localName]
   * @param atts <code>Attributes</code> list for this element
   * @throws <code>SAXException</code> when things go wrong
   */
  public void startElement(String namespaceURI, String localName,
          String rawName, Attributes atts)
          throws SAXException
  {

    // We've now read the opening tag and attribute list, so we
    // can now process each tag and build the argument tree
    if (rawName.equals("ARG"))
    {
      argTypeVector = null;
    } else if (rawName.equals("SCHEMESET"))
    {
      argTypeVector = new Vector();
    } else if (rawName.equals("SCHEME"))
    {
      argType = new ArgType();
    } else if (rawName.equals("NAME"))
    {
    } else if (rawName.equals("FORM"))
    {
    } else if (rawName.equals("PREMISE"))
    {
    } else if (rawName.equals("CONCLUSION"))
    {
    } else if (rawName.equals("CQ"))
    {
    } else if (rawName.equals("TEXT"))
    {
    } else if (rawName.equals("AU"))
    {    // Argument unit
    } else if (rawName.equals("CA"))
    {
      vertexStack.push(currentVertex);
    } else if (rawName.equals("LA"))
    {
      TreeVertex virtualVertex = new TreeVertex("", "V");
      virtualVertex.setVirtual(true);
      virtualVertex.setHasParent(true);
      tree.addVertex(virtualVertex);
      currentVertex.addEdge(virtualVertex);
      vertexStack.push(currentVertex);
      vertexStack.push(virtualVertex);
    } else if (rawName.equals("REFUTATION"))
    {
      vertexStack.push(currentVertex);
      refutation = true;
    } else if (rawName.equals("PROP"))
    {
      tutorStart = tutorEnd = -1;
      supportLabel = null;
      nodeLabel = null;
      diagramRoles = new Hashtable();
      TreeVertex.initRoles(diagramRoles);
      // Add default role for Toulmin node
      diagramRoles.put("toulmin", "data");

      for (int i = 0; i < atts.getLength(); i++)
      {
        String attName = atts.getQName(i);
        String attValue = atts.getValue(i);
        if (attName.equals("identifier"))
        {
          String idTag = attValue;
          // Remove Wigmore prefix for numerical IDs
          if (idTag.indexOf(Argument.WigmoreIDPrefix) != -1)
          {
            idTag = idTag.substring(Argument.WigmoreIDPrefix.length());
          }
          shortLabel = idTag;
        } else if (attName.equals("missing"))
        {
          missingString = attValue;
          missing = (missingString.equals("yes") ? true : false);
        } else if (attName.equals("supportlabel"))
        {
          supportLabel = attValue;
        } else if (attName.equals("nodelabel"))
        {
          nodeLabel = attValue;
        }
      }
    } else if (rawName.equals("PROPTEXT"))
    {
      for (int i = 0; i < atts.getLength(); i++)
      {
        String attName = atts.getQName(i);
        String attValue = atts.getValue(i);
        if (attName.equals("offset"))
        {
          offset = Integer.parseInt(attValue);
        }
      }
    } else if (rawName.equals("OWNER"))
    {
      String ownerName = "";
      String attName = atts.getQName(0);
      String attValue = atts.getValue(0);
      if (attName.equals("name"))
      {
        ownerName = attValue;
        Vector ownerVector = argument.ownerExists(ownerName, 0);
        if (ownerVector == null)
        {
          ownerVector = new Vector();
          String tla = argument.getTla(ownerName);
          if (tla == null)
          {
            tla = "***";
          }
          ownerVector.add(ownerName);
          ownerVector.add(tla);
          argument.addToOwnerList(ownerVector);
        }
        currentVertex.getOwners().add(ownerVector);
      }
    } else if (rawName.equals("INSCHEME"))
    {
      String schemeID = "";
      String schemeName = "";
      for (int i = 0; i < atts.getLength(); i++)
      {
        String attName = atts.getQName(i);
        String attValue = atts.getValue(i);
        if (attName.equals("scheme"))
        {
          schemeName = attValue;
        } else if (attName.equals("schid"))
        {
          schemeID = attValue;
        }
      }
      subtree = argument.getSubtreeByLabel(schemeID);
      if (subtree == null)
      {
        // Add a new subtree
        subtree = new Subtree();
        subtree.setShortLabel(schemeID);
        addCQAnswers = true;   // Add CQChecks only if this is the first occurrence of schemeID
        cqNum = 0;
        ArgType argType = argument.getArgTypeByName(schemeName);
        subtree.setArgumentType(argType); // Creates CQ list as well

        subtree.addVertex(currentVertex);
        argument.getSubtreeList().add(subtree);
      } else
      {
        // Add vertex to existing subtree
        addCQAnswers = false;
        subtree.addVertex(currentVertex);
      }
      // Deal with subtrees containing linked arguments
      if (!vertexStack.isEmpty())
      {
        if (((TreeVertex) vertexStack.peek()).isVirtual())
        {
          TreeVertex virtualTemp = (TreeVertex) vertexStack.pop();
          TreeVertex parentTemp = (TreeVertex) vertexStack.peek();
          if (subtree.containsTreeVertex(parentTemp))
          {
            subtree.addVertex(virtualTemp);
          }
          vertexStack.push(virtualTemp);
        }
      }
    } else if (rawName.equals("CQANS"))
    {
      if (addCQAnswers)
      {
        String attValue = atts.getValue("answered");
        subtree.getCqChecks().elementAt(cqNum).setCqAnswered(attValue.equals("yes"));
        cqNum++;
      }
    } else if (rawName.equals("ROLE"))
    {
      String diagType = null, element = null;
      for (int i = 0; i < atts.getLength(); i++)
      {
        String attName = atts.getQName(i);
        String attValue = atts.getValue(i);
        if (attName.equals("class"))
        {
          diagType = attValue;
        } else if (attName.equals("element"))
        {
          element = attValue;
        }
      }
//      RoleItem roleItem = new RoleItem(diagType, element);
      diagramRoles.put(diagType, element);
    } else if (rawName.equals("TUTOR"))
    {
      for (int i = 0; i < atts.getLength(); i++)
      {
        String attName = atts.getQName(i);
        String attValue = atts.getValue(i);
        if (attName.equals("start"))
        {
          tutorStart = Integer.parseInt(attValue);
        } else if (attName.equals("end"))
        {
          tutorEnd = Integer.parseInt(attValue);
        }
      }
      currentVertex.setTutorStart(tutorStart);
      currentVertex.setTutorEnd(tutorEnd);
    }
    // Turn on the readContents flag so that the characters() method
    // will store the data in a single string.
    readContents = true;
    contents = new String();
  }

  public String removeNonAsciiChars(String source)
  {
    char[] charArray = source.toCharArray();
    for (int i = 0; i < charArray.length; i++)
    {
      if (charArray[i] > 126 || charArray[i] < 32)
      {
        System.out.println("Invalid character");
        charArray[i] = ' ';
      }
    }
    return new String(charArray);
  }

  /** 
   * <p>
   * Indicates the end of an element 
   *   (<code>&lt;/[element name]&gt;</code>) is reached.  Note that 
   *   the parser does not distinguish between empty
   *   elements and non-empty elements, so this will occur uniformly.
   * </p>
   *
   * @param namespaceURI <code>String</code> URI of namespace this 
   *                     element is associated with
   * @param localName <code>String</code> name of element without prefix
   * @param rawName <code>String</code> name of element in XML 1.0 form
   * @throws <code>SAXException</code> when things go wrong
   */
  // TODO: Remove references to m_canvas since display stuff doesn't belong here
  public void endElement(String namespaceURI, String localName,
          String rawName)
          throws SAXException
  {
    if (readContents)
    {
      readContents = false;
    }

    if (rawName.equals("ARG"))
    {
      argument.addXMLSubtrees();
      argument.standardToToulmin();
      argument.standardToWigmore();
    } else if (rawName.equals("SCHEMESET"))
    {
      argument.setSchemeList(argTypeVector);
    } else if (rawName.equals("SCHEME"))
    {
    } else if (rawName.equals("AU"))
    {
    } else if (rawName.equals("NAME"))
    {
      argType.setName(contents);
      argTypeVector.add(argType);
    } else if (rawName.equals("FORM"))
    {
    } else if (rawName.equals("PREMISE"))
    {
      if (contents.length() > 0)
      {
        argType.getPremises().add(contents);
      }
    } else if (rawName.equals("CONCLUSION"))
    {
      if (contents.length() > 0)
      {
        argType.setConclusion(contents);
      }
    } else if (rawName.equals("CQ"))
    {
      if (contents.length() > 0)
      {
        argType.getCriticalQuestions().add(contents);
      }
    } else if (rawName.equals("TEXT"))
    {
      if (contents.length() > 0)
      {
        argument.setText(contents);
      }
    } else if (rawName.equals("AUTHOR"))
    {
      if (contents.length() > 0)
      {
        argument.setAuthor(contents);
      }
    } else if (rawName.equals("DATE"))
    {
      if (contents.length() > 0)
      {
        argument.setDate(contents);
      }
    } else if (rawName.equals("SOURCE"))
    {
      if (contents.length() > 0)
      {
        argument.setSource(contents);
      }
    } else if (rawName.equals("COMMENTS"))
    {
      if (contents.length() > 0)
      {
        argument.setComments(contents);
      }
    } else if (rawName.equals("REFUTATION"))
    {
      TreeVertex poppedVertex = (TreeVertex) vertexStack.pop();
      poppedVertex.addEdge(currentVertex);
      currentVertex.setHasParent(true);
      currentVertex.setParent(poppedVertex);
      currentVertex.setRefutation(true);
      currentVertex = poppedVertex;
    } else if (rawName.equals("CA"))
    {
      TreeVertex poppedVertex = (TreeVertex) vertexStack.pop();
      poppedVertex.addEdge(currentVertex);
      // If we are adding an edge to a dummy root, we are restoring
      // a broken tree, so we are in an undo operation.
      // The dummy root is the invisible root that holds the true roots
      // of the tree fragments.
      // The parent should only be set to 'true' if it is not a dummy root
      if (!new String(poppedVertex.getShortLabel()).equals("DummyRoot"))
      {
        currentVertex.setHasParent(true);
        currentVertex.setParent(poppedVertex);
      } else
      {
        argument.setMultiRoots(true);
        argument.setDummyRoot(poppedVertex);
      }
      currentVertex = poppedVertex;
    } else if (rawName.equals("LA"))
    {
      TreeVertex virtPop = (TreeVertex) vertexStack.pop();    // Pops virtual vertex
      currentVertex = (TreeVertex) vertexStack.pop();
      virtPop.setParent(currentVertex);
    } else if (rawName.equals("PROPTEXT"))
    {
      propText = contents;
      currentVertex = new TreeVertex(propText, shortLabel);
      currentVertex.setSupportLabel(supportLabel);
      currentVertex.m_nodeLabel = nodeLabel;
      currentVertex.roles = diagramRoles;
      // Check if the shortLabel is numeric and thus update Wigmore index
      argument.updateWigmoreIndex(shortLabel);

      // Set the short label so that new nodes will be added
      // after nodes read in from AML file
      // Only update the label if the current label is greater than
      // or equal to the current short label

      // TODO: restore short label display parameters
          /*
      if (shortLabel.length() > m_parent.getShortLabel().length() ||
      shortLabel.compareTo(m_parent.getShortLabel()) >= 0) {
      m_parent.setShortLabel(shortLabel);
      m_parent.incrementShortLabel(); 
      }
       */
      currentVertex.setOffset(offset);
      tutorStart = offset;
      tutorEnd = offset + propText.length();
      currentVertex.setTutorStart(tutorStart);
      currentVertex.setTutorEnd(tutorEnd);
      if (missing)
      {
        currentVertex.setMissing(true);
      }
      if (refutation)
      {
        currentVertex.setRefutation(true);
        refutation = false;
      }
      tree.addVertex(currentVertex);
      // If the top vertex on the stack is virtual, we are within an LA,
      // so we add an edge between the virtual node and the new PROP
      if (!vertexStack.isEmpty())
      {
        TreeVertex topVertex = (TreeVertex) vertexStack.peek();
        if (topVertex.isVirtual())
        {
          topVertex.addEdge(currentVertex);
          currentVertex.setHasParent(true);
          currentVertex.setParent(topVertex);
        }
      }
    }
  }

  /**
   * <p>
   * This will report character data (within an element).
   * </p>
   *
   * @param ch <code>char[]</code> character array with character data
   * @param start <code>int</code> index in array where data starts.
   * @param end <code>int</code> index in array where data ends.
   * @throws <code>SAXException</code> when things go wrong
   */
  public void characters(char[] ch, int start, int end)
          throws SAXException
  {

    contents += new String(ch, start, end);
//        System.out.println("characters: " + s);
  }

  /**
   * <p>
   * This will report whitespace that can be ignored in the 
   *   originating document.  This is typically only invoked when
   *   validation is ocurring in the parsing process.
   * </p>
   *
   * @param ch <code>char[]</code> character array with character data
   * @param start <code>int</code> index in array where data starts.
   * @param end <code>int</code> index in array where data ends.
   * @throws <code>SAXException</code> when things go wrong     
   */
  public void ignorableWhitespace(char[] ch, int start, int end)
          throws SAXException
  {

    String s = new String(ch, start, end);
//        System.out.println("ignorableWhitespace: [" + s + "]");
  }

  /**
   * <p>
   * This will report an entity that is skipped by the parser.  This
   *   should only occur for non-validating parsers, and then is still
   *   implementation-dependent behavior.
   * </p>
   *
   * @param name <code>String</code> name of entity being skipped
   * @throws <code>SAXException</code> when things go wrong     
   */
  public void skippedEntity(String name) throws SAXException
  {
//        System.out.println("Skipping entity " + name);
  }
}

