import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class LabelDialog extends JDialog implements ActionListener
{
  Araucaria araucaria; 
  JPanel buttonPanel = new JPanel();
  JButton addButton = new JButton("Add evaluation");
  JButton deleteButton = new JButton("Delete evaluation");
  JButton cancelButton = new JButton("Cancel");
  JComboBox supportLabelCombo = new JComboBox();
  JCheckBox negatoryCheckBox = new JCheckBox("Negatory");
  public String selectedLabel = null;
  Vector nodeList, edgeList;
  boolean fixedList;      // True if possible evaluations is fixed, as in Wigmore
  String[] fixedChoices;
  String message;
  public enum EdgeType {UNSPECIFIED, WIGMORE_EVIDENCE, WIGMORE_EXPLANATORY, WIGMORE_CORROBORATIVE};
  EdgeType edgeType;
  public boolean createUndoPoint = true;
  public boolean cancelled = false;

  public LabelDialog(Araucaria parent)
  {
    this(parent, false,  null, "Select an evaluation/qualifier or type in your own", EdgeType.UNSPECIFIED);
  }
  
  public void setNodeList(Vector n)
  {
    nodeList = n;
  }
  
  public void setEdgeList(Vector e)
  {
    edgeList = e;
  }
  
  public LabelDialog(Araucaria parent, boolean fixed, String[] f, String m, EdgeType e)
  {
    super((Frame)null, true);
    fixedList = fixed;
    fixedChoices = f;
    message = m;
    edgeType = e;
    araucaria = parent;
    nodeList = araucaria.getArgument().getSelectedVertices();
    edgeList = araucaria.getArgument().getSelectedEdges();
    try
    {
      init();
      this.pack(); 
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation(d.width/2 - this.getSize().width/2,
      d.height/2 - this.getSize().height/2);
  }
  
  private void init() throws Exception
  {
    this.setResizable(true);
    String title = "Edit evaluation or qualifier";
    this.setTitle(title);
    buttonPanel.add(addButton);
    addButton.addActionListener(this);
    addButton.setMnemonic(KeyEvent.VK_A);
    buttonPanel.add(deleteButton);
    deleteButton.addActionListener(this);
    deleteButton.setMnemonic(KeyEvent.VK_DELETE);
    cancelButton.addActionListener(this);
    buttonPanel.add(cancelButton);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    JPanel labelCheckboxPanel = new JPanel(new GridLayout(0,2));
    labelCheckboxPanel.add(new JLabel(message));
    labelCheckboxPanel.add(negatoryCheckBox);
    negatoryCheckBox.setVisible(false);
    
    JPanel comboPanel = new JPanel(new GridLayout(2,0));
    comboPanel.add(labelCheckboxPanel);
    
    if (!fixedList)
    {
      supportLabelCombo.setEditable(true);
      Iterator iter = araucaria.getArgument().getSupportLabelList().iterator();
      while (iter.hasNext()) {
        String label = (String)iter.next();
        supportLabelCombo.addItem(label);
      }
    } else {
      supportLabelCombo.setEditable(false);
      for (int i = 0; i < fixedChoices.length; i++)
      {
        supportLabelCombo.addItem(fixedChoices[i]);
      }
    }
    comboPanel.add(supportLabelCombo);
    addDialogCloser(cancelButton);
    this.getRootPane().setDefaultButton(addButton);
    this.getContentPane().add(comboPanel, BorderLayout.NORTH);
  }
  
  public JCheckBox getNegatoryCheckBox()
  {
    return negatoryCheckBox;
  }
  
  public JButton getDeleteButton()
  {
    return deleteButton;
  }
  
  public void addDialogCloser(JComponent comp)
  { 
    AbstractAction closeAction = new AbstractAction()
    {
      public void actionPerformed(ActionEvent e)
      {
        setVisible(false);
      }
    };
    
    // Then create a keystroke to use for it
    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    
    // Finally, bind the keystroke and the action to *any* component
    // within the dialog. Note the WHEN_IN_FOCUSED bit...this is what
    // stops you having to do it for all components
        
    comp.getInputMap(
      JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "close");
    comp.getActionMap().put("close", closeAction);
  }
  
  public void setFixedList(boolean f)
  { fixedList = f; }
  
  public void setFixedChoices(String[] f)
  { fixedChoices = f; } 

  private void addLabel()
  {
    Enumeration nodeEnum = nodeList.elements();
    while (nodeEnum.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeEnum.nextElement();
      vertex.m_nodeLabel = selectedLabel;
    }
    nodeEnum = edgeList.elements();
    while (nodeEnum.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)nodeEnum.nextElement();
      TreeVertex vertex = edge.getDestVertex();
      if(vertex.isVirtual()) 
      {
        if (edgeType != EdgeType.UNSPECIFIED)
        {
          // We represent both the force + negatoriness of a group edge as
          // part of the evaluation
          if (negatoryCheckBox.isVisible() && negatoryCheckBox.isSelected())
          {
            selectedLabel += " Negatory";
          }
          // Adds evaluation for group edge in a Wigmore cluster
          TreeVertex source = edge.getSourceVertex();
          switch (edgeType)
          {
            case WIGMORE_EVIDENCE:
              source.roles.put("wigmoreEvidenceForce", selectedLabel);
              break;
            case WIGMORE_EXPLANATORY:
              source.roles.put("wigmoreExplanatoryForce", selectedLabel);
              break;
            case WIGMORE_CORROBORATIVE:
              source.roles.put("wigmoreCorroborativeForce", selectedLabel);
              break;
          }
        } else {
          araucaria.setMessageLabelText("Support from a linked argument cannot be evaluated.");
          return;
        }
      } else {
        vertex.previousRefutation = vertex.isRefutation();
        vertex.setSupportLabel(selectedLabel);
        // For an ordinary edge, the negatory nature is stored by labelling
        // the vertex as a refutation
        if (negatoryCheckBox.isVisible())
        {
          vertex.setRefutation(negatoryCheckBox.isSelected());
        }
      }
    }
    if (createUndoPoint)
    {
      araucaria.undoStack.push(new EditAction(araucaria, "editing evaluation"));   
      araucaria.doUndo();
      araucaria.doRedo();
    }
  }
  
  private void deleteLabel()
  {
    Enumeration nodeEnum = nodeList.elements();
    while (nodeEnum.hasMoreElements()) {
      TreeVertex vertex = (TreeVertex)nodeEnum.nextElement();
      vertex.m_nodeLabel = null;
    }
    nodeEnum = edgeList.elements();
    while (nodeEnum.hasMoreElements()) {
      TreeEdge edge = (TreeEdge)nodeEnum.nextElement();
      TreeVertex vertex = edge.getDestVertex();
      if(vertex.isVirtual()) 
      {
        if (edgeType != EdgeType.UNSPECIFIED)
        {
          // Adds evaluation for group edge in a Wigmore cluster
          TreeVertex source = edge.getSourceVertex();
          switch (edgeType)
          {
            case WIGMORE_EVIDENCE:
              source.roles.remove("wigmoreEvidenceForce");
              break;
            case WIGMORE_EXPLANATORY:
              source.roles.remove("wigmoreExplanatoryForce");
              break;
            case WIGMORE_CORROBORATIVE:
              source.roles.remove("wigmoreCorroborativeForce");
              break;
          }
        } else {
          araucaria.setMessageLabelText("Support from a linked argument cannot be evaluated.");
          return;
        }
      } else {
        vertex.previousRefutation = vertex.isRefutation();
        vertex.setSupportLabel(null);
        if (negatoryCheckBox.isVisible())
        {
          vertex.setRefutation(negatoryCheckBox.isSelected());
        }
      }
    }
    if (createUndoPoint)
    {
      araucaria.undoStack.push(new EditAction(araucaria, "deleting evaluation"));    
      araucaria.doUndo();
      araucaria.doRedo();
    }
  }
  
  public void actionPerformed(ActionEvent event)
  {
    cancelled = false;
    if (event.getSource() == addButton) {
      this.setVisible(false);
      selectedLabel = (String)supportLabelCombo.getSelectedItem();
      if (selectedLabel.length() > 0) {
        addLabel();
      }
    }
    else if (event.getSource() == deleteButton) {
      this.setVisible(false);
      deleteLabel();
    } 
    else if (event.getSource() == cancelButton) {
      this.setVisible(false);
      cancelled = true;
    }
  }
}
