import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      University of Dundee
 * @author
 * @version 1.0
 */

public class ArgInfoFrame extends JFrame implements ActionListener
{
  JPanel argTypePanel = new JPanel();
  JLabel jLabel1 = new JLabel();
  BorderLayout borderLayout1 = new BorderLayout();
  JComboBox argTypeCombo = new JComboBox();
  JPanel argTypeButtonPanel = new JPanel();
  JButton newArgTypeButton = new JButton();
  JButton editArgTypeButton = new JButton();
  JButton deleteArgTypeButton = new JButton();
  JPanel jPanel1 = new JPanel();
  JButton closeArgTypeButton = new JButton();
  Vector schemeList = new Vector();
  String argInfoXML;
  Argument argument;

  public ArgInfoFrame(Argument a)
  {
    try
    {
      jbInit();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(d.width/2 - this.getSize().width/2,
      d.height/2 - this.getSize().height/2);
    argument = a;
    schemeList = argument.getSchemeList();
    loadArgTypeCombo();
  }

  private void jbInit() throws Exception
  {
//    Araucaria.createIcon(this);
    closeArgTypeButton.setFont(new java.awt.Font("Dialog", 1, 12));
    closeArgTypeButton.setText("Close");
    jLabel1.setFont(new java.awt.Font("Dialog", 1, 12));
    jLabel1.setText("Argument Type");
    argTypePanel.setLayout(borderLayout1);
    newArgTypeButton.setFont(new java.awt.Font("Dialog", 1, 12));
    newArgTypeButton.setText("New...");
    newArgTypeButton.setMnemonic(KeyEvent.VK_N);
    editArgTypeButton.setFont(new java.awt.Font("Dialog", 1, 12));
    editArgTypeButton.setText("Edit...");
    editArgTypeButton.setMnemonic(KeyEvent.VK_E);
    deleteArgTypeButton.setFont(new java.awt.Font("Dialog", 1, 12));
    deleteArgTypeButton.setText("Delete");
    deleteArgTypeButton.setMnemonic(KeyEvent.VK_DELETE);
    this.getContentPane().setBackground(Color.red);
    this.setForeground(Color.red);
    this.setSize(600,150);
    this.setResizable(true);
    this.setTitle("Argument types");
    this.getContentPane().add(argTypePanel, BorderLayout.NORTH);

    argTypePanel.add(argTypeCombo, BorderLayout.SOUTH);
    argTypePanel.add(jLabel1, BorderLayout.NORTH);
    this.getContentPane().add(argTypeButtonPanel, BorderLayout.CENTER);
    newArgTypeButton.addActionListener(this);
    argTypeButtonPanel.add(newArgTypeButton, null);
    editArgTypeButton.addActionListener(this);
    argTypeButtonPanel.add(editArgTypeButton, null);
    deleteArgTypeButton.addActionListener(this);
    argTypeButtonPanel.add(deleteArgTypeButton, null);
    this.getContentPane().add(jPanel1, BorderLayout.SOUTH);
    closeArgTypeButton.addActionListener(this);
    addDialogCloser(closeArgTypeButton);
    this.getRootPane().setDefaultButton(closeArgTypeButton);
    jPanel1.add(closeArgTypeButton, null);
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(600, 150);
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

  public void loadArgTypeCombo()
  {
    argTypeCombo.removeAllItems();
    Enumeration argInfoList = schemeList.elements();
    while (argInfoList.hasMoreElements()) {
      ArgType argType = (ArgType)argInfoList.nextElement();
      argTypeCombo.addItem(argType.getName());
    }
  }

  public  ArgType getArgTypeByName(String name)
  {
    Vector argTypeVector = getSchemeList();
    if (argTypeVector == null)
      return null;
    for (int i = 0; i < argTypeVector.size(); i++) {
      ArgType argType = (ArgType)argTypeVector.elementAt(i);
      if (argType.getName().equals(name))
        return argType;
    }
    return null;
  }

  public  Vector getSchemeList()
  {
    return schemeList;
  }

  public void setSchemeList(Vector s)
  {
    schemeList = s;
  }

  private boolean deleteArgType(String argName)
  {
    Enumeration argInfoList = schemeList.elements();
    while (argInfoList.hasMoreElements()) {
      ArgType argType = (ArgType)argInfoList.nextElement();
      if (argType.getName().equals(argName)) {
        schemeList.remove(argType);
        loadArgTypeCombo();
        replaceArgType(argName, null);
        return true;
      }
    }
    return false;
  }

  private void replaceArgType(String oldArgName, ArgType newArgType)
  {
    Vector subtreeList = argument.getSubtreeList();
    for (int i = 0; i<subtreeList.size(); i++) 
    {
      Subtree subtree = (Subtree)subtreeList.elementAt(i);
      ArgType subtreeArgType = subtree.getArgumentType();
      if (subtreeArgType.getName().equals(oldArgName))
      {
        if (newArgType == null)
        {
          subtreeList.remove(subtree);
//          owner.getTreeCanvas().redrawTree();
        } else {
          subtree.setArgumentType(newArgType);
        }
      }
    }
  }

  private void editArgType(String argName)
  {
    Enumeration argInfoList = schemeList.elements();
    while (argInfoList.hasMoreElements()) {
      ArgType argType = (ArgType)argInfoList.nextElement();
      if (argType.getName().equals(argName)) { 
        AddEditArgDialog argEditFrame = new AddEditArgDialog(this, argType); 
        argEditFrame.show();
        if (argEditFrame.doSave()) {
          int argIndex = schemeList.indexOf(argType);
          schemeList.set(argIndex, argEditFrame.getArgType());
          loadArgTypeCombo();
          replaceArgType(argName, argEditFrame.getArgType());
        }
      }
    }
  }

  public void actionPerformed(ActionEvent event)
  {
    Object button = event.getSource();
    if (button == newArgTypeButton) {
      AddEditArgDialog argEditFrame = new AddEditArgDialog(this);
      argEditFrame.show();
      if (argEditFrame.doSave()) {
        ArgType argType = argEditFrame.getArgType();
        schemeList.add(argType);
        loadArgTypeCombo();
      }
    } else if (button == deleteArgTypeButton) {
      int action = JOptionPane.showConfirmDialog(this, 
        "<html><center><font color=red face=helvetica><b>Delete selected scheme?</b></font></center></html>", 
        "Delete scheme?",
        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
      if (action == 1) return;
      String selectedArg = (String)argTypeCombo.getSelectedItem();
      deleteArgType(selectedArg);
    } else if (button == editArgTypeButton) {
      String selectedArg = (String)argTypeCombo.getSelectedItem();
      editArgType(selectedArg);
    } else if (button == closeArgTypeButton) {
      this.hide();
    }
  }
}


