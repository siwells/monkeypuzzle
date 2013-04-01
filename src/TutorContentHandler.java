/*
 * TutorContentHandler.java
 *
 * Created on 07 March 2004, 08:08
 */

/**
 *
 * @author  growe
 */
import org.xml.sax.*;
import java.util.*;
/**
 * <b><code>MyContentHandler</code></b> implements the SAX 
 *   <code>ContentHandler</code> interface and defines callback
 *   behavior for the SAX callbacks associated with an XML
 *   document's content.
 */

/**
 * Used by SAX to read in an AML file and extract the tree
 * structure from it - ignores schemesets, prop-texts etc.
 * Used in tree searches.
 */
public class TutorContentHandler implements ContentHandler {
    /** Hold onto the locator for location information */
    private Locator locator;
    private boolean readContents = false;
    String contents = "";
    Tree m_tree;
    Stack vertexStack = new Stack();
    TreeVertex currentVertex = null;
    boolean refutation = false;
    Vector argTypeVector;
    ArgType argType = null;

    // For PROP tag:
    String identifierString = "-", missingString = "no", 
      textString, supportLabel = "", nodeLabel = "";
    String shortLabel = " ";
    boolean missing = false;
    int offset, tutorStart, tutorEnd;
    
    // PROPTEXT
    int offsetValue = -1;
    String propText;
  
  /** Creates a new instance of TutorContentHandler */
  public TutorContentHandler(Tree tree) 
  {
    m_tree = tree;
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
    public void setDocumentLocator(Locator locator) {
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
    public void startDocument() throws SAXException {
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
    public void endDocument() throws SAXException {
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
        throws SAXException {
            
//        System.out.println("PI: Target:" + target + " and Data:" + data);
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
    public void startPrefixMapping(String prefix, String uri) {
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
    public void endPrefixMapping(String prefix) {
//        System.out.println("Mapping ends for prefix " + prefix);
    }
    
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
        throws SAXException {
            
        // We've now read the opening tag and attribute list, so we
        // can now process each tag and build the argument tree
        if (rawName.equals("ARG")) {
          argTypeVector = null;
        } else if (rawName.equals("SCHEMESET")) {
          argTypeVector = new Vector();
        } else if (rawName.equals("SCHEME")) {
          argType = new ArgType();
        } else if (rawName.equals("NAME")) {
        } else if (rawName.equals("FORM")) {
        } else if (rawName.equals("PREMISE")) {
        } else if (rawName.equals("CONCLUSION")) {
        } else if (rawName.equals("CQ")) {
        } else if (rawName.equals("TEXT")) {
        } else if (rawName.equals("CA")) {
          vertexStack.push(currentVertex);
        } else if (rawName.equals("LA")) {
          TreeVertex virtualVertex = new TreeVertex("", "V");
          virtualVertex.setVirtual(true);
          virtualVertex.setHasParent(true);
          m_tree.addVertex(virtualVertex);
          currentVertex.addEdge(virtualVertex);
          vertexStack.push(currentVertex);
          vertexStack.push(virtualVertex);
        } else if (rawName.equals("REFUTATION")) {
          vertexStack.push(currentVertex);
        	refutation = true;
        } else if (rawName.equals("PROP")) {
          for (int i=0; i<atts.getLength(); i++) {
            String attName = atts.getQName(i);
            String attValue = atts.getValue(i);
            if (attName.equals("identifier")) {
              shortLabel = attValue;
            } else if (attName.equals("missing")) {
              missingString = attValue;
              missing = (missingString.equals("yes") ? true : false);
            } else if (attName.equals("supportlabel")) {
              supportLabel = attValue;
            } else if (attName.equals("nodelabel")) {
              nodeLabel = attValue;
            }
          }
        } else if (rawName.equals("PROPTEXT")) {
          for (int i=0; i<atts.getLength(); i++) {
            String attName = atts.getQName(i);
            String attValue = atts.getValue(i);
            if (attName.equals("offset")) {
              offset = Integer.parseInt(attValue);
            }
          }
        } else if (rawName.equals("INSCHEME")) {
        } else if (rawName.equals("TUTOR")) {
          for (int i=0; i<atts.getLength(); i++) {
            String attName = atts.getQName(i);
            String attValue = atts.getValue(i);
            if (attName.equals("start")) {
              tutorStart = Integer.parseInt(attValue);
            } else if (attName.equals("end")) {
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
    public void endElement(String namespaceURI, String localName, 
                           String rawName) 
        throws SAXException {
        if (readContents) {
          readContents = false;
        }
        
        if (rawName.equals("ARG")) {
        } else if (rawName.equals("SCHEMESET")) {
        } else if (rawName.equals("SCHEME")) {
        } else if (rawName.equals("NAME")) {
        } else if (rawName.equals("FORM")) {
        } else if (rawName.equals("PREMISE")) {
        } else if (rawName.equals("CONCLUSION")) {
        } else if (rawName.equals("CQ")) {
        } else if (rawName.equals("TEXT")) {
        } else if (rawName.equals("REFUTATION")) {
          TreeVertex poppedVertex = (TreeVertex)vertexStack.pop();
          poppedVertex.addEdge(currentVertex);
          currentVertex.setHasParent(true);
          currentVertex.setRefutation(true);
          currentVertex = poppedVertex;
        } else if (rawName.equals("CA")) {
          TreeVertex poppedVertex = (TreeVertex)vertexStack.pop();
          poppedVertex.addEdge(currentVertex);
          currentVertex.setHasParent(true);
          currentVertex = poppedVertex;
        } else if (rawName.equals("LA")) {
          TreeVertex virtPop = (TreeVertex)vertexStack.pop();    // Pops virtual vertex
          currentVertex = (TreeVertex)vertexStack.pop();
        } else if (rawName.equals("PROPTEXT")) {
          propText = contents;
          currentVertex = new TreeVertex(propText, shortLabel);
          currentVertex.setSupportLabel(supportLabel);
          currentVertex.m_nodeLabel = nodeLabel;
          currentVertex.setOffset(offset);
          tutorStart = offset;
          tutorEnd = offset + propText.length();
          currentVertex.setTutorStart(tutorStart);
          currentVertex.setTutorEnd(tutorEnd);

          if (missing) {
            currentVertex.setMissing(true);
          }
          if (refutation) {
          	currentVertex.setRefutation(true);
          	refutation = false;
          }
          m_tree.addVertex(currentVertex);
          // If the top vertex on the stack is virtual, we are within an LA,
          // so we add an edge between the virtual node and the new PROP
          if (!vertexStack.isEmpty()) {
            TreeVertex topVertex = (TreeVertex)vertexStack.peek();
            if (topVertex.isVirtual()) {
              topVertex.addEdge(currentVertex);
              currentVertex.setHasParent(true);
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
        throws SAXException {
            
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
        throws SAXException {
            
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
    public void skippedEntity(String name) throws SAXException {
//        System.out.println("Skipping entity " + name);
    }

}


  

