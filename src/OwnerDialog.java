import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class OwnerDialog extends JDialog implements ActionListener
{
	Araucaria araucaria;
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton("Close");
  JButton leftArrow, rightArrow, addOwnerButton, deleteSourceButton;
  JTextField ownerText;
  JScrollPane ownerSourceScrollPane;
  JTable ownerSourceTable;
  OwnerSourceTableModel ownerSourceTableModel;
  JScrollPane ownerNodesScrollPane;
  JTable ownerNodesTable;
  OwnerNodesTableModel ownerNodesTableModel;
  Vector propText = null;
  SizedPanel masterPanel;
  
	public OwnerDialog(Araucaria parent)
	{

    super((Frame)null, true);
    araucaria = parent;
    propText = araucaria.getArgument().getSelectedProps();
    try
    {
      init();
      okButton.requestFocusInWindow();
      addDialogCloser(masterPanel);
      this.getRootPane().setDefaultButton(okButton);
      this.pack();
    } 
    catch(Exception e)
    {
      e.printStackTrace();
    }
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(d.width/2 - this.getSize().width/2,
      d.height/2 - this.getSize().height/2);
  }
  
  private void init() throws Exception
  {
    this.setResizable(true);
    String title = "Edit owners";
    if (propText.size() == 0) {
    	title += " (no propositions selected)";
  	} else if (propText.size() == 1) {
  		title += " of proposition: \"" + 
  			DiagramBase.prepareMessageLabel((String)propText.elementAt(0), DiagramBase.MAX_MESSAGELABEL_SIZE) 
  			+ "\"";
  	} else {
  		title += " (" + propText.size() + " propositions selected)";
  	}
    this.setTitle(title);
    masterPanel = new SizedPanel(600, 300);
    masterPanel.setLayout(new BorderLayout());
    this.getContentPane().add(masterPanel);
//    okButton.setActionCommand("okButton");
    okButton.setMnemonic(KeyEvent.VK_C);
    buttonPanel.add(okButton);
    okButton.addActionListener(this);
    masterPanel.add(buttonPanel, BorderLayout.SOUTH);
    
    TablePanel ownerSourcePanel = new TablePanel(new BorderLayout());
    
    JPanel topSourcePanel = new JPanel(new BorderLayout());
    topSourcePanel.add(new JLabel("Owner name:", JLabel.LEFT), BorderLayout.NORTH);
    ownerText = new JTextField();
    topSourcePanel.add(ownerText, BorderLayout.CENTER);
    JPanel buttonSourcePanel = new JPanel();
    deleteSourceButton = new JButton("Delete");
    deleteSourceButton.setMnemonic(KeyEvent.VK_D);
    deleteSourceButton.addActionListener(this);
    addOwnerButton = new JButton("Add");
    addOwnerButton.setMnemonic(KeyEvent.VK_A);
    addOwnerButton.addActionListener(this);
    buttonSourcePanel.add(addOwnerButton);
    buttonSourcePanel.add(deleteSourceButton);
    topSourcePanel.add(buttonSourcePanel, BorderLayout.SOUTH);
    ownerSourcePanel.add(topSourcePanel, BorderLayout.SOUTH);
    
    ownerSourceTable = new JTable();
    ownerSourceScrollPane = new JScrollPane();
    ownerSourceScrollPane.setViewportView(ownerSourceTable);
    ownerSourcePanel.add(new JLabel("Available owners", JLabel.CENTER), BorderLayout.NORTH);
    ownerSourcePanel.add(ownerSourceScrollPane, BorderLayout.CENTER);
    ownerSourceTableModel = new OwnerSourceTableModel(araucaria, ownerSourceTable, this);
    ownerSourceTable.setModel(ownerSourceTableModel);
    ownerSourceTableModel.updateTable(araucaria.getArgument().getOwnerList());
		masterPanel.add(ownerSourcePanel, BorderLayout.WEST);
    
    TablePanel ownerNodesPanel = new TablePanel(new BorderLayout());
    ownerNodesTable = new JTable();
    ownerNodesScrollPane = new JScrollPane();
    ownerNodesScrollPane.setViewportView(ownerNodesTable);
    ownerNodesPanel.add(new JLabel("Owners assigned to proposition(s)", JLabel.CENTER), BorderLayout.NORTH);
    ownerNodesPanel.add(ownerNodesScrollPane, BorderLayout.CENTER);
    ownerNodesTableModel = new OwnerNodesTableModel(araucaria, ownerNodesTable);
    ownerNodesTable.setModel(ownerNodesTableModel);
    ownerNodesTableModel.updateTable(araucaria.getArgument().getSelectedVertexOwners()); 
		masterPanel.add(ownerNodesPanel, BorderLayout.EAST);
		
		setupArrows();
		JPanel arrowBox = new JPanel(new GridLayout(3,1,10,10));
		if (propText.size() > 0) {
  		arrowBox.add(leftArrow);
  		arrowBox.add(rightArrow);
    }
		JPanel arrowPanel = new JPanel();
		arrowPanel.add(arrowBox);
		masterPanel.add(arrowPanel, BorderLayout.CENTER);
		
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

  private void setupArrows()
	{
		/*
		leftArrow = new JButton(new ArrowIcon(30, 30,
              ArrowIcon.LEFT, true));
    rightArrow = new JButton(new ArrowIcon(30, 30,
               ArrowIcon.RIGHT, true));
    leftArrow.setDisabledIcon(new ArrowIcon(30, 30,
                ArrowIcon.LEFT, false));
    rightArrow.setDisabledIcon(new ArrowIcon(30, 30,
                ArrowIcon.RIGHT, false));
                */
    leftArrow = new JButton(new ImageIcon("images/Left.gif"));
    rightArrow = new JButton(new ImageIcon("images/Right.gif"));
    leftArrow.addActionListener(this);
    rightArrow.addActionListener(this);
	}
	
	public void updateNodesTable()
	{
		ownerNodesTableModel.updateTable(araucaria.getArgument().getSelectedVertexOwners());
	}

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == okButton) {
	    this.setVisible(false);
    } else if (event.getSource() == rightArrow) {
      try {
      	araucaria.getArgument().addOwnersToSelected(araucaria, ownerSourceTableModel.getSelectedOwners());
  	    ownerNodesTableModel.updateTable(araucaria.getArgument().getSelectedVertexOwners());
  	    araucaria.undoStack.push(new EditAction(araucaria, "adding owners"));
      }
      catch (Exception ex) {
        return;
      }
    } else if (event.getSource() == leftArrow) {
    	// Delete owners only from selected vertices
    	araucaria.getArgument().deleteOwners(ownerNodesTableModel.getSelectedOwners(), true);
	    ownerNodesTableModel.updateTable(araucaria.getArgument().getSelectedVertexOwners());
	    araucaria.undoStack.push(new EditAction(araucaria, "removing owners"));
    } else if (event.getSource() == addOwnerButton) {
    	String newOwnerName = ownerText.getText();
    	if (newOwnerName.length() == 0) {
    	 return;
      }
			ownerSourceTableModel.addOwner(newOwnerName);
			ownerText.setText("");
			ownerText.requestFocus();
		} else if (event.getSource() == deleteSourceButton) {
      Vector selected = null;
		  try {
        selected = ownerSourceTableModel.getSelectedOwners();
      }
      catch (Exception ex) {
        return;
      }
      if (selected == null || selected.size() == 0) return;
      int action = JOptionPane.showConfirmDialog(this, 
        "<html><center><font color=red face=helvetica><b>Delete selected owners?</b></font></center></html>", 
        "Delete owners?",
        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
      if (action == 1) return;
    	araucaria.getArgument().deleteOwners(selected);
    	// Delete owners from ALL vertices
    	araucaria.getArgument().deleteOwners(selected, false);
    	ownerSourceTableModel.updateTable(araucaria.getArgument().getOwnerList());
	    ownerNodesTableModel.updateTable(araucaria.getArgument().getSelectedVertexOwners());
    }
  }
  
  class TablePanel extends JPanel
  {
  	TablePanel(LayoutManager layout)
  	{
  		super(layout);
  	}
  	
  	public Dimension getPreferredSize()
  	{
  		OwnerDialog dialog = OwnerDialog.this;
  		int width = dialog.getWidth();
  		int height = dialog.getHeight();
  		return new Dimension(2 * width / 5, height/2);
  	}
  }
}
