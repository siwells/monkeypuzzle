import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Popup dialog for information on subtrees
 */
public class SubtreeFrame extends JDialog implements ActionListener
{
  String argTypes = null;
  String treeConclusion;
  Vector treePremises = new Vector();
  JPanel selectArgumentPanel = new JPanel();
  JLabel selectArgumentLabel = new JLabel();
  JComboBox selectArgumentCombo = new JComboBox();
  GridLayout selectArgumentLayout = new GridLayout();
  TitledBorder titledBorder1;
  TitledBorder titledBorder2;
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel textAreaPanel = new JPanel();
  GridLayout textAreaLayout = new GridLayout();
  JScrollPane premisesScrollPane = new JScrollPane();
  JScrollPane conclusionScrollPane = new JScrollPane();
  JScrollPane criticalQuestionsScrollPane = new JScrollPane();
  JTextArea premisesTextArea = new JTextArea();
  JTextArea conclusionTextArea = new JTextArea();
  DefaultListModel critQuesListModel = new DefaultListModel();
  DefaultListModel argumentListModel = new DefaultListModel();
  JList critQuesList = new JList(critQuesListModel);
  
  JPanel templatePanel = new JPanel(new GridLayout(2,1,5,5));
  DefaultListModel tempPremListModel = new DefaultListModel();
  DefaultListModel tempConcListModel = new DefaultListModel();
  JList tempPremList = new JList(tempPremListModel);
  JList tempConcList = new JList(tempConcListModel);
  JScrollPane tempPremScroll = new JScrollPane(tempPremList);
  JScrollPane tempConcScroll = new JScrollPane(tempConcList);
  
  JPanel argumentPanel = new JPanel(new GridLayout(2,1,5,5));
  DefaultListModel argPremListModel = new DefaultListModel();
  DefaultListModel argConcListModel = new DefaultListModel();
  JList argPremList = new JList(argPremListModel);
  JList argConcList = new JList(argConcListModel);
  JScrollPane argPremScroll = new JScrollPane(argPremList);
  JScrollPane argConcScroll = new JScrollPane(argConcList);
  
  JPanel premisePanel = new JPanel();
  JPanel conclusionPanel = new JPanel();
  JPanel critQuesPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  boolean okPressed = false;
  Vector argInfoVector = new Vector();
  Argument argument;

  public SubtreeFrame(Frame owner, Argument arg)
  {
    super(owner);
    this.setModal(true);
    this.setSize(700, 550);
    argument = arg;
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(d.width/2 - this.getSize().width/2,
      d.height/2 - this.getSize().height/2);
    this.setTitle("Select argument scheme");
    try
    {
      initGUI();
      this.pack();
      addDialogCloser(this.cancelButton);
      this.getRootPane().setDefaultButton(this.okButton);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    loadArgTypeCombo();
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(600, 480);
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
    try {
      argInfoVector = argument.getSchemeList(); 
    } catch (Exception e)
    {
      System.out.println("ArgInfo under WebStart");
    }

    selectArgumentCombo.removeAllItems();
    Enumeration argInfoList = argInfoVector.elements();
    while (argInfoList.hasMoreElements()) {
      ArgType argType = (ArgType)argInfoList.nextElement();
      selectArgumentCombo.addItem(argType.getName());
    }
    updateTextBoxes();
  }

  public void updateTextBoxes()
  {
    String critString = "", premiseString = "", concString = "";
    String argName = (String)selectArgumentCombo.getSelectedItem();
    Enumeration argInfoList = argInfoVector.elements();
    SubtreeCellRenderer critQuesRenderer = 
      new SubtreeCellRenderer(criticalQuestionsScrollPane.getViewport().getWidth());
    critQuesList.setCellRenderer(critQuesRenderer);
    critQuesListModel.clear();
    
    SubtreeCellRenderer tempConcRenderer = 
      new SubtreeCellRenderer(tempConcScroll.getViewport().getWidth());
    tempConcList.setCellRenderer(tempConcRenderer);
    tempConcListModel.clear();
    
    SubtreeCellRenderer tempPremRenderer = 
      new SubtreeCellRenderer(tempPremScroll.getViewport().getWidth());
    tempPremList.setCellRenderer(tempPremRenderer);
    tempPremListModel.clear();
    
    SubtreeCellRenderer argConcRenderer = 
      new SubtreeCellRenderer(argConcScroll.getViewport().getWidth());
    argConcList.setCellRenderer(argConcRenderer);
    argConcListModel.clear();
    
    SubtreeCellRenderer argPremRenderer = 
      new SubtreeCellRenderer(argPremScroll.getViewport().getWidth());
    argPremList.setCellRenderer(argPremRenderer);
    argPremListModel.clear();
    
    while (argInfoList.hasMoreElements()) {
      ArgType argType = (ArgType)argInfoList.nextElement();
      if (argType.getName().equals(argName)) {
        Vector critQuestions = argType.getCriticalQuestions();
        Enumeration quesList = critQuestions.elements();
        while (quesList.hasMoreElements()) {
          critString = (String)quesList.nextElement();
          critQuesListModel.addElement(critString);
        }
        
        tempConcListModel.addElement(argType.getConclusion());
        argConcListModel.addElement(treeConclusion);

        for (int i = 0; i < argType.getPremises().size(); i++) {
          premiseString = (String)argType.getPremises().elementAt(i);
          tempPremListModel.addElement(premiseString);
        }
        
        for (int i = 0; i < treePremises.size(); i++) {
          String premise = (String)treePremises.elementAt(i);
          if (!premise.equals("**Virtual**")) {
            argPremListModel.addElement(premise);
          }
        }
      }
    }
  }

  private void initTemplatePanel()
  {
    templatePanel.setBorder(BorderFactory.createTitledBorder("Scheme"));
    JPanel premisePanel = new JPanel(new BorderLayout(5,5));
    premisePanel.add(new JLabel("Premises"), BorderLayout.NORTH);
    premisePanel.add(tempPremScroll, BorderLayout.CENTER);
    
    JPanel concPanel = new JPanel(new BorderLayout(5,5));
    concPanel.add(new JLabel("Conclusion"), BorderLayout.NORTH);
    concPanel.add(tempConcScroll, BorderLayout.CENTER);
    
    templatePanel.add(premisePanel);
    templatePanel.add(concPanel);
  }

  private void initArgumentPanel()
  {
    argumentPanel.setBorder(BorderFactory.createTitledBorder("Argument"));
    JPanel premisePanel = new JPanel(new BorderLayout(5,5));
    premisePanel.add(new JLabel("Premises"), BorderLayout.NORTH);
    premisePanel.add(argPremScroll, BorderLayout.CENTER);
    
    JPanel concPanel = new JPanel(new BorderLayout(5,5));
    concPanel.add(new JLabel("Conclusion"), BorderLayout.NORTH);
    concPanel.add(argConcScroll, BorderLayout.CENTER);
    
    argumentPanel.add(premisePanel);
    argumentPanel.add(concPanel);
  }

  private void initGUI() throws Exception
  {
    SizedPanel masterPanel = new SizedPanel(600, 480);
    masterPanel.setLayout(new BorderLayout());
    titledBorder1 = new TitledBorder("");
    titledBorder2 = new TitledBorder("");
    this.getContentPane().setLayout(borderLayout1);
    selectArgumentLabel.setText("Select scheme:");
    selectArgumentPanel.setLayout(selectArgumentLayout);
    selectArgumentLayout.setRows(2);
    selectArgumentLayout.setColumns(1);
    selectArgumentPanel.setBackground(Color.orange);
    selectArgumentPanel.setBorder(titledBorder1);
    textAreaPanel.setLayout(textAreaLayout);
    textAreaLayout.setRows(3);
    textAreaLayout.setColumns(1);
    textAreaLayout.setVgap(5);
    
    initTemplatePanel();
    initArgumentPanel();
    critQuesPanel.setLayout(new BorderLayout());
    critQuesPanel.add(new JLabel("Critical questions"), BorderLayout.NORTH);
    critQuesPanel.add(criticalQuestionsScrollPane, BorderLayout.CENTER);
    okButton.setFont(new java.awt.Font("Dialog", 1, 14));
    okButton.setText("OK");
    okButton.addActionListener(this);
    cancelButton.setFont(new java.awt.Font("Dialog", 1, 14));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(this);
    masterPanel.add(selectArgumentPanel, BorderLayout.NORTH);
    selectArgumentPanel.add(selectArgumentLabel, null);
    selectArgumentPanel.add(selectArgumentCombo, null);
    selectArgumentCombo.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          updateTextBoxes();
        }
      }
    );
    JPanel scrollPanel = new JPanel();
    scrollPanel.setLayout(new BorderLayout());
    JPanel tempArgPanel = new JPanel(new GridLayout(1,2,10,0));
    tempArgPanel.add(templatePanel);
    tempArgPanel.add(argumentPanel);
    scrollPanel.add(tempArgPanel, BorderLayout.CENTER);
    scrollPanel.add(critQuesPanel, BorderLayout.SOUTH);
    premisesScrollPane.getViewport().add(premisesTextArea, null);
    conclusionScrollPane.getViewport().add(conclusionTextArea, null);
    masterPanel.add(scrollPanel, BorderLayout.CENTER);
    masterPanel.add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    criticalQuestionsScrollPane.getViewport().add(critQuesList, null);
    this.getContentPane().add(masterPanel);
  }

  public boolean isOKPressed()
  { return okPressed; }

  public ArgType getArgumentType()
  {
    String argName = (String)selectArgumentCombo.getSelectedItem();
    Enumeration argList = argInfoVector.elements();
    while (argList.hasMoreElements()) {
      ArgType argType = (ArgType)argList.nextElement();
      if (argType.getName() == argName)
        return argType;
    }
    return null;
  }

  public void setPremisesText(Vector premiseList)
  {
    treePremises = premiseList;
    updateTextBoxes();
  }

  public void setConclusionText(String text)
  {
    treeConclusion = text;
    updateTextBoxes();
  }

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == okButton) {
      okPressed = true;
      this.hide();
    } else if (event.getSource() == cancelButton) {
      okPressed = false;
      this.hide();
    }
  }
}
