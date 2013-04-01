package org.simonwells.monkeypuzzle;


import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.File;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.dgc.VMID;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author home
 */
public class SaveArgumentAif
{
  OntModel model;
  Resource argInfo;
  File chosenFile;
  Argument argument;

  public Argument getArgument()
  {
    return argument;
  }

  public void setArgument(Argument argument)
  {
    this.argument = argument;
  }

  public SaveArgumentAif(File xmlFile)
  {
    chosenFile = xmlFile;
    initializeOntModel();
  }

  private void initializeOntModel()
  {
    OntDocumentManager documentManager = new OntDocumentManager();
    OntModelSpec ontModelSpec = new OntModelSpec(OntModelSpec.OWL_DL_MEM_RULE_INF);
    ontModelSpec.setDocumentManager(documentManager);
    model = ModelFactory.createOntologyModel(ontModelSpec);
    model.removeAll();
    documentManager.loadImport(model, "file:aif+.owl");
    documentManager.loadImport(model, "file:araucaria4.owl");
  }

  public void createArgInfo(String argText)
  {
    VMID argGuid = new VMID();
    String argInfoId = argGuid.toString();
    argInfo = model.createResource(AIF.getURI() + argInfoId);
    argInfo.addProperty(RDF.type, AIF.ArgInfo);
    argInfo.addProperty(AIF.argInfoText, argText);
  }

  public void graphToFile()
  {
    schemesetToFile();
    saveTreeAif();
    writeToFile(model);
  }
  public static String DEFAULTRA = "Default";
  public static String DEFAULTCA = "Default CA";

  public void addDefaultSchemes(Vector schemeList)
  {
    // Default RA scheme
    ArgType defaultRA = new ArgType();
    defaultRA.setConclusion("Default conclusion");
    defaultRA.setName(DEFAULTRA);
    schemeList.add(defaultRA);

    // Default CA
    ArgType defaultCA = new ArgType();
    defaultCA.setConclusion("Default conclusion");
    defaultCA.setName(DEFAULTCA);
    schemeList.add(defaultCA);
  }

  /**
   * Saves schemes in the currently loaded schemeset
   */
  public void schemesetToFile()
  {
    // Use a clone since we add default schemes to it
    Vector schemeList = (Vector) argument.getSchemeList().clone();
    addDefaultSchemes(schemeList);
    Enumeration argTypeList = schemeList.elements();
    while (argTypeList.hasMoreElements())
    {
      ArgType argType = (ArgType) argTypeList.nextElement();
      saveSchemeAif(argType);
    }
  }

  /**
   * Creates a Resource for the current scheme and adds it and its
   * properties to the model.
   * @param argType The ArgType object describing the scheme
   */
  public void saveSchemeAif(ArgType argType)
  {
    // Main scheme resource
    String schemeUri = AIF.NS + argType.getName();
    Resource schemeResource = model.getResource(schemeUri);
    schemeResource.addProperty(RDF.type, "Scheme");
    schemeResource.addProperty(AIF.hasSchemeName, argType.getName());

    // Conclusion
    Resource conclusionResource = model.getResource(schemeUri + "Conclusion");
    conclusionResource.addProperty(RDF.type, "ConclusionDesc");
    conclusionResource.addProperty(AIF.hasDescription, argType.getConclusion());
    schemeResource.addProperty(AIF.hasConclusionDesc, conclusionResource);

    // Premises
    int premiseCount = 1;
    Vector premiseVector = argType.getPremises();
    for (int i = 0; i < premiseVector.size(); i++)
    {
      Resource premiseResource = model.getResource(schemeUri + "Premise" + premiseCount++);
      premiseResource.addProperty(RDF.type, "PremiseDesc");
      premiseResource.addProperty(AIF.hasDescription, (String) premiseVector.elementAt(i));
      schemeResource.addProperty(AIF.hasPremiseDesc, premiseResource);
    }

    // Presumptions
    int presumptionCount = 1;
    Vector critQuesVector = argType.getCriticalQuestions();
    Enumeration critQuesList = critQuesVector.elements();
    while (critQuesList.hasMoreElements())
    {
      Resource presumptionResource = model.getResource(schemeUri + "Presumption" + presumptionCount++);
      presumptionResource.addProperty(RDF.type, "PresumptionDesc");
      presumptionResource.addProperty(AIF.hasDescription, (String) critQuesList.nextElement());
      schemeResource.addProperty(AIF.hasPresumptionDesc, presumptionResource);
    }
  }
  private static Random sNodeRandom = new Random((new Date()).getTime());

  public void saveTreeAif()
  {
    Tree tree = argument.getTree();
    TreeVertex start = argument.getDummyRoot();
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

    // Process the vertices
    Enumeration vertexEnum = tree.prepareVertexList(start);
    while (vertexEnum.hasMoreElements())
    {
      TreeVertex vertex = (TreeVertex) vertexEnum.nextElement();
      if (vertex.isVirtual())
      {
        continue;
      }
      String vertexUri = AIF.NS + vertex.getShortLabelString();
      Resource vertexResource = model.createResource(vertexUri);
      vertexResource.addProperty(RDF.type, AIF.INode);
      vertexResource.addProperty(ARAUCARIA4.vertexId,
              model.createTypedLiteral(vertex.getShortLabelString()));
      vertexResource.addProperty(ARAUCARIA4.missing,
              model.createTypedLiteral(vertex.isMissing()));
      vertexResource.addProperty(ARAUCARIA4.textBoundsStart,
              model.createTypedLiteral(vertex.getOffset()));
      vertexResource.addProperty(ARAUCARIA4.locationX,
              model.createTypedLiteral((double) vertex.getDrawPointFullText().x));
      vertexResource.addProperty(ARAUCARIA4.locationY,
              model.createTypedLiteral((double) vertex.getDrawPointFullText().y));
      String vertexText = vertex.getLabel().toString();
      vertexResource.addProperty(ARAUCARIA4.textBoundsEnd,
              model.createTypedLiteral((int) (vertex.getOffset() + vertexText.length())));
      vertexResource.addProperty(ARAUCARIA4.text,
              model.createTypedLiteral(vertex.getLabel().toString()));
      vertexResource.addProperty(ARAUCARIA4.refutation,
              model.createTypedLiteral(vertex.isRefutation()));
      if (vertex.m_nodeLabel != null)
      {
        vertexResource.addProperty(ARAUCARIA4.nodeLabel,
                model.createTypedLiteral(vertex.m_nodeLabel));
      }
      if (vertex.getSupportLabel() != null)
      {
        vertexResource.addProperty(ARAUCARIA4.supportLabel,
                model.createTypedLiteral(vertex.getSupportLabel()));
      }

      for (Object object : vertex.getOwners())
      {
        Vector<String> owner = (Vector<String>) object;
        vertexResource.addProperty(ARAUCARIA4.owner,
                model.createTypedLiteral(owner.elementAt(0).toString()));
      }

      Hashtable rolesTable = vertex.roles;
      Enumeration keys = rolesTable.keys();
      int roleCount = 0;
      while (keys.hasMoreElements())
      {
        String roleUri = AIF.NS + vertex.getShortLabelString() + "Role" + roleCount;
        roleCount++;
        Resource roleResource = model.createResource(roleUri);
        roleResource.addProperty(RDF.type, ARAUCARIA4.Role);
        String key = (String) keys.nextElement();
        String value = (String) rolesTable.get(key);
        roleResource.addProperty(ARAUCARIA4.roleClass, model.createTypedLiteral(key));
        roleResource.addProperty(ARAUCARIA4.roleElement, model.createTypedLiteral(value));
        vertexResource.addProperty(ARAUCARIA4.hasRole, roleResource);
      }

//      for (int i = 0; i < subtreeList.size(); i++)
//      {
//        Subtree scheme = (Subtree) subtreeList.elementAt(i);
//        if (scheme.containsTreeVertex(start))
//        {
//          Element inschemeElement = doc.createElement("INSCHEME");
//          propElement.appendChild(inschemeElement);
//          inschemeElement.setAttribute("scheme", scheme.getArgumentType().getName());
//          inschemeElement.setAttribute("schid", "" + i);
//          for (CQCheck cqc : scheme.getCqChecks())
//          {
//            Element cqElement = doc.createElement("CQANS");
//            inschemeElement.appendChild(cqElement);
//            cqElement.setAttribute("answered", cqc.isCqAnswered() ? "yes" : "no");
//          }
//        }
//      }
      // Use existing schemes even though these are not compatible with AIF in
      // general. Store some scheme info in the S-node, but this is never used
      // in Arau 3.2.
      for (int i = 0; i < argument.getSubtreeList().size(); i++)
      {
        Subtree scheme = (Subtree) argument.getSubtreeList().elementAt(i);
        if (scheme.containsTreeVertex(vertex))
        {
          Resource inscheme = model.createResource(AIF.NS +
                  vertex.getShortLabelString() + "InScheme" + i);
          inscheme.addProperty(RDF.type, ARAUCARIA4.InScheme);
          inscheme.addProperty(ARAUCARIA4.inSchemeId, "" + i);
          inscheme.addProperty(ARAUCARIA4.inSchemeName, scheme.getArgumentType().getName());
          int cqCount = 0;
          for (CQCheck cQCheck : scheme.getCqChecks())
          {
            Resource cq = model.createResource(AIF.NS +
                  vertex.getShortLabelString() + "InScheme" + i + "CQ" + cqCount);
            cqCount++;
            cq.addProperty(RDF.type, ARAUCARIA4.CQAnswer);
            cq.addProperty(ARAUCARIA4.cqAnswered, model.createTypedLiteral(cQCheck.isCqAnswered()));
            cq.addProperty(ARAUCARIA4.cqText, model.createTypedLiteral(cQCheck.getCqText()));
            inscheme.addProperty(ARAUCARIA4.hasCQAnswer, cq);
          }

          vertexResource.addProperty(ARAUCARIA4.belongsToScheme, inscheme);
        }
      }
    }
    tree.restoreVertexList(startIndex, start);

    // Process the edges
    vertexEnum = tree.prepareVertexList(start);
    while (vertexEnum.hasMoreElements())
    {
      TreeVertex source = (TreeVertex) vertexEnum.nextElement();
      if (source.isVirtual())
      {
        continue;
      }
      Resource sourceResource =
              model.getResource(AIF.NS + source.getShortLabelString());
      Vector edges = source.getEdgeList();
      for (Object object : edges)
      {
        TreeEdge edge = (TreeEdge) object;
        TreeVertex dest = edge.getDestVertex();
        // Create the S-node to insert between premise and conclusion
        String sNodeLabel = "S" + sNodeRandom.nextLong();
        String sNodeUri = AIF.NS + sNodeLabel;
        Resource sNodeResource = model.createResource(sNodeUri);
        sNodeResource.addProperty(RDF.type, AIF.SNode);
        sNodeResource.addProperty(ARAUCARIA4.vertexId,
                model.createTypedLiteral(sNodeLabel));
        sourceResource.addProperty(AIF.edgeTo, sNodeResource);
        addEdgeResource(source.getShortLabelString(), sNodeLabel, ARAUCARIA4.ArauEdgeINodetoSNode);

        // Insert the edges
        if (dest.isVirtual())   // linked argument
        {
          Vector virtualEdges = dest.getEdgeList();
          for (Object object1 : virtualEdges)
          {
            TreeEdge virtualEdge = (TreeEdge) object1;
            TreeVertex virtualDest = virtualEdge.getDestVertex();
            Resource destResource =
                    model.getResource(AIF.NS + virtualDest.getShortLabelString());
            sNodeResource.addProperty(AIF.edgeTo, destResource);
            addEdgeResource(sNodeLabel, virtualDest.getShortLabelString(), ARAUCARIA4.ArauEdgeSNodetoINode);
          }
        } else  // convergent argument
        {
          Resource destResource =
                  model.getResource(AIF.NS + dest.getShortLabelString());
          // Position the S-node midway between I-nodes
          sNodeResource.addProperty(ARAUCARIA4.locationX,
                  model.createTypedLiteral((source.getDrawPointFullText().x +
                  dest.getDrawPointFullText().x) / 2.0));
          sNodeResource.addProperty(ARAUCARIA4.locationY,
                  model.createTypedLiteral((source.getDrawPointFullText().y +
                  dest.getDrawPointFullText().y) / 2.0));
          sNodeResource.addProperty(AIF.edgeTo, destResource);
          addEdgeResource(sNodeLabel, dest.getShortLabelString(), ARAUCARIA4.ArauEdgeSNodetoINode);
        }
      }
    }

    // Assign schemes to S-nodes
    // For each S-node, finds its parent and assigns whatever scheme the parent
    // belongs to to the S-node. Seems the easiest way of coping with the
    // possibly incorrect scheme setups in Arau 3.1. It will always give the
    // correct scheme assignment if the schemes are allocated according to AIF rules.
    ResIterator sNodeIterator = model.listSubjectsWithProperty(RDF.type, AIF.SNode);
    while (sNodeIterator.hasNext())
    {
      Resource sNodeResource = sNodeIterator.nextResource();
      ResIterator sNodeParents = model.listSubjectsWithProperty(AIF.edgeTo, sNodeResource);
      while (sNodeParents.hasNext())
      {
        Resource parentResource = sNodeParents.nextResource();
        String parentId = parentResource.getProperty(ARAUCARIA4.vertexId).getString();
        TreeVertex parent = (TreeVertex) argument.getTree().getVertexByShortLabel(parentId);
        Vector<Subtree> subtreeList = argument.getSubtreeList();
        boolean schemeAssigned = false;
        for (Subtree subtree : subtreeList)
        {
          if (subtree.containsTreeVertex(parent))
          {
            sNodeResource.addProperty(ARAUCARIA4.schemeName,
                    model.createTypedLiteral(subtree.getArgumentType().getName()));
            schemeAssigned = true;
            break;
          }
        }
        if (!schemeAssigned)
        {
          sNodeResource.addProperty(ARAUCARIA4.schemeName,
                  model.createTypedLiteral(DEFAULTRA));
        }
      }
    }
  }
  static String WigmoreIDPrefix = "Wigmore_";

  /**
   * Adds a resource to the local AIF model describing the edge between
   * two vertices with vertex IDs given as strings.
   * @param sourceId
   * @param destId
   * @param edgeType node to node type (e.g. INode_INode, etc)
   */
  void addEdgeResource(String sourceId, String destId, Resource edgeType)
  {
    String edgeUri = AIF.NS + sourceId + "_to_" + destId;
    Resource edgeResource = model.createResource(edgeUri);
    edgeResource.addProperty(ARAUCARIA4.startVertexId, model.createTypedLiteral(sourceId));
    edgeResource.addProperty(ARAUCARIA4.endVertexId, model.createTypedLiteral(destId));
    edgeResource.addProperty(RDF.type, edgeType);
  }

  /**
   * Save the 'start' source as AIF
   * @param start
   */
  protected void recursiveSaveTreeAif(TreeVertex start)
  {
    if (start.getVisited())
    {
      return;
    }
    String vertexUri = AIF.NS + start.getShortLabelString();
//    ContentNode nodeType = arauVertex.getNodeType();
//    String vertexUri = AIF.NS + arauVertex.getProperties().getGeneral().getVertexId();
    Resource vertexResource = model.createResource(vertexUri);
    vertexResource.addProperty(RDF.type, AIF.INode);
//    arauVertex.getProperties().addAifRdf(sourceResource, model);


  /*
  // Add a proposition only if the source is not virtual.
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
  // Add owners of this source
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
  // Add schemes to which this source belongs
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
  //      if (sourceVertex.isVirtual() && nextVertex.isVirtual()) {
  //      Element laElement = new Element("LA");
  //      currentElement.addContent(laElement);
  //      oldCurrentElement = currentElement;
  //      currentElement = laElement;
  //      }
  // If the source source in the edge is the same as the root of the subtree,
  // and neither the source nor dest source is virtual, then the 'start' source has an
  // ordinary child, so we add a <CA> tag.
  //
  // If the dest source is virtual, we have the beginning of a linked subtree, so
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
  // layer up in the tree unless ONLY the start source is virtual.
  if (!start.isVirtual() ||
  (sourceVertex.isVirtual() && nextVertex.isVirtual()))
  {
  currentElement = oldCurrentElement;
  }
  }
   * */
  }

  private void writeToFile(Model model)
  {
//    if (chosenFile.exists())
//    {
//      int action = JOptionPane.showConfirmDialog(null,
//              "<html><center><font color=red face=helvetica><b>Overwrite existing file?</b></face></center></html>", "Overwrite?",
//              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
//      if (action == JOptionPane.NO_OPTION)
//      {
//        writeToFile(model);
//        return;
//      }
//    }
    writeModel(model, chosenFile.getAbsolutePath());
  }

  public static void writeModel(Model model, String outputFileName)
  {
    OutputStream output = null;
    try
    {
      output = new FileOutputStream(outputFileName);
      if (output == null)
      {
        throw new IllegalArgumentException("File: " + outputFileName + " not found");
      }
      model.write(output);
      output.close();
    } catch (IOException ex)
    {
      Logger.getLogger(SaveArgumentAif.class.getName()).log(Level.SEVERE, null, ex);
    } finally
    {
      try
      {
        output.close();
      } catch (IOException ex)
      {
        Logger.getLogger(SaveArgumentAif.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
