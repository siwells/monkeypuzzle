package org.simonwells.monkeypuzzle;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author home
 */
public class OpenArgumentAif
{
  OntModel model;
  Argument argument;
  Tree argTree;

  private void initializeOntModel()
  {
    OntDocumentManager documentManager = new OntDocumentManager();
    OntModelSpec ontModelSpec = new OntModelSpec(OntModelSpec.OWL_DL_MEM_RULE_INF);
    ontModelSpec.setDocumentManager(documentManager);
    model = ModelFactory.createOntologyModel(ontModelSpec);
    documentManager.loadImport(model, "file:aif+.owl");
    documentManager.loadImport(model, "file:araucaria4.owl");
  }
  Hashtable diagramRoles;

  private void loadInSchemes(Resource nodeInfo, TreeVertex vertex)
  {
    StmtIterator inSchemes = nodeInfo.listProperties(ARAUCARIA4.belongsToScheme);
    while (inSchemes.hasNext())
    {
      Statement inScheme = inSchemes.nextStatement();
      String schemeID = inScheme.getProperty(ARAUCARIA4.inSchemeId).getString();
      String schemeName = inScheme.getProperty(ARAUCARIA4.inSchemeName).getString();
      Subtree subtree = argument.getSubtreeByLabel(schemeID);
      if (subtree == null)
      {
        // Add a new subtree
        subtree = new Subtree();
        subtree.setShortLabel(schemeID);
        ArgType argType = argument.getArgTypeByName(schemeName);
        subtree.setArgumentType(argType); // Creates CQ list as well

        subtree.addVertex(vertex);
        argument.getSubtreeList().add(subtree);
        
        // Restore critical question checks
        StmtIterator cqChecks = inScheme.getResource().listProperties(ARAUCARIA4.hasCQAnswer);
        while (cqChecks.hasNext())
        {
          Statement cqCheck = cqChecks.nextStatement();
          subtree.getCQCheckByText(cqCheck.getProperty(ARAUCARIA4.cqText).getString()).
                  setCqAnswered(cqCheck.getProperty(ARAUCARIA4.cqAnswered).getBoolean());
        }
      } else
      {
        // Add vertex to existing subtree
        subtree.addVertex(vertex);
      }
    }
  }

  // Adds virtual nodes to a scheme if the scheme contains the parent
  // and the child of the virtual node.
  private void includeVirtualNodes(TreeVertex vertex)
  {
    Vector<Subtree> subtrees = argument.getSubtreeList();
    for (Subtree subtree : subtrees)
    {

      if (!subtree.containsTreeVertex(vertex))
      {
        continue;
      }
      // Include virtual nodes if the parent is also in this scheme
      if (vertex.getParent() != null && vertex.getParent().isVirtual())
      {
        if (subtree.containsTreeVertex(vertex.getParent().getParent()) &&
                !subtree.containsTreeVertex(vertex.getParent()))
        {
          subtree.addVertex(vertex.getParent());
        }
      }
    }
  }

  private void loadINode(Resource nodeInfo)
  {
    Statement nodeStatement = nodeInfo.getProperty(ARAUCARIA4.text);
    String vertexText = nodeStatement.getString();
    String vertexLabel = nodeInfo.getProperty(ARAUCARIA4.vertexId).getString();
    TreeVertex vertex = new TreeVertex(vertexText, vertexLabel);
    argTree.addVertex(vertex);
    vertex.setMissing(nodeInfo.getProperty(ARAUCARIA4.missing).getBoolean());
    vertex.setOffset(nodeInfo.getProperty(ARAUCARIA4.textBoundsStart).getInt());
    vertex.setRefutation(nodeInfo.getProperty(ARAUCARIA4.refutation).getBoolean());
    Statement supportLabel = nodeInfo.getProperty(ARAUCARIA4.supportLabel);
    if (supportLabel != null)
    {
      vertex.setSupportLabel(supportLabel.getString());
    }
    Statement nodeLabel = nodeInfo.getProperty(ARAUCARIA4.nodeLabel);
    if (nodeLabel != null)
    {
      vertex.m_nodeLabel = nodeLabel.getString();
    }

    // Owners
    StmtIterator owners = nodeInfo.listProperties(ARAUCARIA4.owner);
    while (owners.hasNext())
    {
      String ownerName = owners.nextStatement().getString();
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
      vertex.getOwners().add(ownerVector);
    }

    // Roles
    diagramRoles = new Hashtable();
    TreeVertex.initRoles(diagramRoles);
    // Add default role for Toulmin node
    diagramRoles.put("toulmin", "data");
    StmtIterator roles = nodeInfo.listProperties(ARAUCARIA4.hasRole);
    while (roles.hasNext())
    {
      Resource role = roles.nextStatement().getResource();
      String roleClass = role.getProperty(ARAUCARIA4.roleClass).getString();
      String roleElement = role.getProperty(ARAUCARIA4.roleElement).getString();
      diagramRoles.put(roleClass, roleElement);
    }
    vertex.roles = diagramRoles;
    argument.updateWigmoreIndex(vertexLabel);
  }

  private void addScheme(Resource scheme)
  {
    ArgType argType = new ArgType();
    argType.setName(scheme.getProperty(AIF.hasSchemeName).getString());
    argument.getSchemeList().add(argType);

    // Conclusion
    Statement conclusion = scheme.getProperty(AIF.hasConclusionDesc);
    argType.setConclusion(conclusion.getProperty(AIF.hasDescription).getString());

    // Premises
    StmtIterator premiseIter = scheme.listProperties(AIF.hasPremiseDesc);
    while (premiseIter.hasNext())
    {
      Statement premise = premiseIter.nextStatement();
      argType.getPremises().add(premise.getProperty(AIF.hasDescription).getString());
    }

    // Presumptions (CQs)
    StmtIterator presumptionIter = scheme.listProperties(AIF.hasPresumptionDesc);
    while (presumptionIter.hasNext())
    {
      Statement presumption = presumptionIter.nextStatement();
      argType.getCriticalQuestions().add(presumption.getProperty(AIF.hasDescription).getString());
    }
  }

  private void loadSchemes()
  {
    Vector schemeList = new Vector();
    argument.setSchemeList(schemeList);
    ResIterator schemeIterator =
            model.listSubjectsWithProperty(RDF.type, AIF.Scheme);
    while (schemeIterator.hasNext())
    {
      Resource scheme = schemeIterator.nextResource();
      addScheme(scheme);
    }
  }

  public void loadArgument(String argFile, Argument viewArgument)
  {
    this.argument = viewArgument;
    initializeOntModel();
    model.read("file:" + argFile);

    // Create the schemeset
    loadSchemes();
    // Find the ArgInfo node
    // This is a bit kludgy since it assumes there is only one ArgInfo
    // resource.

    ResIterator argInfoIterator =
            model.listSubjectsWithProperty(RDF.type, AIF.ArgInfo);
    Resource argInfo;
    if (argInfoIterator.hasNext())
    {
      argInfo = argInfoIterator.nextResource();
    } else
    {
      return;
    }
    Statement argStatement = argInfo.getProperty(AIF.argInfoText);
    viewArgument.setText(argStatement.getString());

    argTree = viewArgument.getTree();

    // Get the I-nodes and add them to the tree
    argInfoIterator = model.listSubjectsWithProperty(RDF.type, AIF.INode);
    while (argInfoIterator.hasNext())
    {
      argInfo = argInfoIterator.nextResource();
      loadINode(argInfo);
    }

    // Process S-nodes and thus add edges between I-nodes in the tree
    argInfoIterator = model.listSubjectsWithProperty(RDF.type, AIF.SNode);
    while (argInfoIterator.hasNext())
    {
      Resource sNode = argInfoIterator.nextResource();
      // Each S-node must have exactly one parent, so find it by finding
      // the resource with an edge to the s-node
      TreeVertex parent;
      ResIterator sNodeIterator = model.listSubjectsWithProperty(AIF.edgeTo, sNode);
      if (sNodeIterator.hasNext())
      {
        Resource iNodeParent = sNodeIterator.nextResource();
        parent = (TreeVertex) argTree.getVertexByShortLabel(
                iNodeParent.getProperty(ARAUCARIA4.vertexId).getString());

        // Find all children of the S-node
        StmtIterator iterator = sNode.listProperties(AIF.edgeTo);
        // Need to count children first to see if it's a linked argument
        int childCount = 0;
        while (iterator.hasNext())
        {
          childCount++;
          iterator.nextStatement();
        }

        // If childCount > 1, we have a virtual vertex
        if (childCount > 1)
        {
          TreeVertex virtualVertex = new TreeVertex("", "V");
          virtualVertex.setVirtual(true);
          virtualVertex.setHasParent(true);
          virtualVertex.setParent(parent);
          argTree.addVertex(virtualVertex);
          parent.addEdge(virtualVertex);
          // Redefine parent so that all the S-node's children get attached to the virtual vertex
          parent = virtualVertex;
        }

        // Reset the iterator to attach the edges
        iterator = sNode.listProperties(AIF.edgeTo);
        while (iterator.hasNext())
        {
          Resource sChild = iterator.nextStatement().getResource();
          String childId = sChild.getProperty(ARAUCARIA4.vertexId).getString();
          TreeVertex child = (TreeVertex) argTree.getVertexByShortLabel(childId);
          parent.addEdge(child);
          child.setHasParent(true);
          child.setParent(parent);
        }
      } else
      {
        System.out.println("Error: S-node without a parent.");
        return;
      }
    }

    // Restore the schemes
    argInfoIterator = model.listSubjectsWithProperty(RDF.type, AIF.INode);
    while (argInfoIterator.hasNext())
    {
      argInfo = argInfoIterator.nextResource();
      String vertexId = argInfo.getProperty(ARAUCARIA4.vertexId).getString();
      TreeVertex vertex = (TreeVertex) argTree.getVertexByShortLabel(vertexId);
      loadInSchemes(argInfo, vertex);
    }

    // One more pass to restore schemes in linked arguments
    argInfoIterator = model.listSubjectsWithProperty(RDF.type, AIF.INode);
    while (argInfoIterator.hasNext())
    {
      argInfo = argInfoIterator.nextResource();
      String vertexId = argInfo.getProperty(ARAUCARIA4.vertexId).getString();
      TreeVertex vertex = (TreeVertex) argTree.getVertexByShortLabel(vertexId);
      includeVirtualNodes(vertex);
    }


    argument.addXMLSubtrees();
    argument.standardToToulmin();
    argument.standardToWigmore();
  }
}
