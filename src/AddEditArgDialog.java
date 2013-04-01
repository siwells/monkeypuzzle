import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/**
 * Title:        Araucaria
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      University of Dundee
 * @author Glenn Rowe
 * @version 1.0
 */

public class AddEditArgDialog extends JDialog implements ActionListener
{
  ArgType editingArgType = null;
  static final int NEW_QUESTION = 0;
  static final int EDIT_QUESTION = 1;
  static final int NEW_PREMISE = 0;
  static final int EDIT_PREMISE = 1;
  int questionState, premiseState;
  boolean saveRequested = false;

  DefaultListModel criticalQuestionListModel = new DefaultListModel();
  JList criticalQuestionList = new JList(criticalQuestionListModel);
  int selectedQuestionIndex;

  DefaultListModel premiseListModel = new DefaultListModel();
  JList premiseList = new JList(premiseListModel);
  int selectedPremiseIndex;

  JPanel argNameConcPanel = new JPanel();
  JLabel argNameLabel = new JLabel();
  JTextField argNameTextField = new JTextField();
  JLabel conclusionLabel = new JLabel();
  JTextField conclusionTextField = new JTextField();
  JPanel premisePanel = new JPanel();
  TitledBorder titledBorder1;
  JScrollPane premiseScroll = new JScrollPane();
  JButton newPremiseButton = new JButton();
  JButton editPremiseButton = new JButton();
  JButton deletePremiseButton = new JButton();
  JButton savePremiseButton = new JButton();
  JLabel editPremiseLabel = new JLabel();
  JTextField editPremiseTextField = new JTextField();
  JPanel critQuesPanel = new JPanel();
  TitledBorder titledBorder2;
  JPanel okCancelPanel = new JPanel();
  JButton saveButton = new JButton();
  JButton cancelButton = new JButton();
  JScrollPane critQuesListScroll = new JScrollPane();
  JButton newCriticalQuestionButton = new JButton();
  JButton editCriticalQuestionButton = new JButton();
  JButton deleteCriticalQuestionButton = new JButton();
  JButton saveCriticalQuestionButton = new JButton();
  JLabel editCriticalQuestionLabel = new JLabel();
  JTextField editCriticalQuestionTextField = new JTextField();

  public AddEditArgDialog(Frame owner)
  {
    super (owner, true);
    this.setSize(new Dimension(410, 690));
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(d.width/2 - this.getSize().width/2,
      d.height/2 - this.getSize().height/2);
		this.setLocationRelativeTo(owner);
    try
    {
      jbInit();
      addEventHandlers();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    initializeLists();
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(410, 690);
  }

  public AddEditArgDialog(Frame owner, ArgType argType)
  {
    super (owner, true);
    this.setSize(new Dimension(410, 690));
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(d.width/2 - this.getSize().width/2,
      d.height/2 - this.getSize().height/2);
    try
    {
      jbInit();
      addEventHandlers();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    editingArgType = argType;
    initializeComponents();
    initializeLists();
  }

  private void initializeComponents()
  {
    argNameTextField.setText(editingArgType.getName());
    conclusionTextField.setText(editingArgType.getConclusion());
    for (int i = 0; i < editingArgType.getPremises().size(); i++)
      premiseListModel.addElement((String)editingArgType.getPremises().elementAt(i));
    Enumeration quesList = editingArgType.getCriticalQuestions().elements();
    while (quesList.hasMoreElements()) {
      criticalQuestionListModel.addElement((String)quesList.nextElement());
    }
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

  private void initializeLists()
  {
    criticalQuestionList.setToolTipText("Select critical question");
    criticalQuestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    criticalQuestionList.addListSelectionListener(
      new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent event) {
          editCriticalQuestionButton.setEnabled(true);
          deleteCriticalQuestionButton.setEnabled(true);
        }
      }
    );
    premiseList.setToolTipText("Select premise");
    premiseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    premiseList.addListSelectionListener(
      new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent event) {
          editPremiseButton.setEnabled(true);
          deletePremiseButton.setEnabled(true);
        }
      }
    );
    critQuesListScroll.getViewport().add(criticalQuestionList, null);
    premiseScroll.getViewport().add(premiseList, null);
  }

  public boolean doSave()
  { return saveRequested; }

  public ArgType getArgType()
  {
    ArgType argType = new ArgType();
    argType.setName(argNameTextField.getText());
    argType.setConclusion(conclusionTextField.getText());
    Vector premiseVector = argType.getPremises();
    ListModel listModel = premiseList.getModel();
    for (int i = 0; i < listModel.getSize(); i++) {
      premiseVector.add(listModel.getElementAt(i));
    }
    Vector critQuesVector = argType.getCriticalQuestions();
    listModel = criticalQuestionList.getModel();
    for (int i = 0; i < listModel.getSize(); i++) {
      critQuesVector.add(listModel.getElementAt(i));
    }
    return argType;
  }

  private void processQuestion()
  {
    String question = editCriticalQuestionTextField.getText();
    switch (questionState) {
    case NEW_QUESTION:
      if (question.length() > 0) {
        criticalQuestionListModel.addElement(question);
      }
      break;
    case EDIT_QUESTION:
      if (question.length() == 0) {
        selectedQuestionIndex = criticalQuestionList.getSelectedIndex();
        criticalQuestionListModel.removeElementAt(selectedQuestionIndex);
      } else {
        criticalQuestionListModel.set(selectedQuestionIndex, question);
      }
      break;
    }
    editCriticalQuestionLabel.setText("       ");
    editCriticalQuestionTextField.setText("");
    editCriticalQuestionTextField.setEnabled(false);
    saveCriticalQuestionButton.setEnabled(false);
    newCriticalQuestionButton.setEnabled(true);
  }

  private void processPremise()
  {
    String premise = editPremiseTextField.getText();
    switch (premiseState) {
    case NEW_PREMISE:
      if (premise.length() > 0) {
        premiseListModel.addElement(premise);
      }
      break;
    case EDIT_PREMISE:
      if (premise.length() == 0) {
        selectedPremiseIndex = premiseList.getSelectedIndex();
        premiseListModel.removeElementAt(selectedPremiseIndex);
      } else {
        premiseListModel.set(selectedPremiseIndex, premise);
      }
      break;
    }
    editPremiseLabel.setText("       ");
    editPremiseTextField.setText("");
    editPremiseTextField.setEnabled(false);
    savePremiseButton.setEnabled(false);
    newPremiseButton.setEnabled(true);
  }

  private void addEventHandlers()
  {
    saveButton.addActionListener(this);
    cancelButton.addActionListener(this);
    newCriticalQuestionButton.addActionListener(this);
    editCriticalQuestionButton.addActionListener(this);
    saveCriticalQuestionButton.addActionListener(this);
    deleteCriticalQuestionButton.addActionListener(this);
    newPremiseButton.addActionListener(this);
    editPremiseButton.addActionListener(this);
    savePremiseButton.addActionListener(this);
    deletePremiseButton.addActionListener(this);
    addDialogCloser(cancelButton);
    this.getRootPane().setDefaultButton(saveButton);
  }

  private void jbInit() throws Exception
  {
    SizedPanel masterPanel = new SizedPanel(398,660);
    titledBorder1 = new TitledBorder(BorderFactory.createBevelBorder
      (BevelBorder.LOWERED,Color.white,Color.white,new Color(148, 145, 140),
      new Color(103, 101, 98)),"Premises");
    titledBorder2 = new TitledBorder(BorderFactory.createBevelBorder
      (BevelBorder.LOWERED,Color.white,Color.white,new Color(148, 145, 140),
      new Color(103, 101, 98)),"Critical questions");
    masterPanel.setLayout(null);
    argNameConcPanel.setBackground(new Color(255, 255, 190));
    argNameConcPanel.setBounds(new Rectangle(1, 0, 399, 109));
    argNameConcPanel.setLayout(null);
    argNameLabel.setText("Scheme name");
    argNameLabel.setBounds(new Rectangle(8, 2, 260, 22));
    argNameTextField.setBounds(new Rectangle(7, 24, 387, 23));
    conclusionLabel.setText("Conclusion");
    conclusionLabel.setBounds(new Rectangle(8, 54, 383, 24));
    conclusionTextField.setBounds(new Rectangle(7, 75, 387, 25));
    premisePanel.setBackground(new Color(197, 255, 203));
    premisePanel.setBorder(titledBorder1);
    premisePanel.setBounds(new Rectangle(4, 114, 389, 248));
    premisePanel.setLayout(null);
    premiseScroll.setBorder(BorderFactory.createLineBorder(Color.black));
    premiseScroll.setBounds(new Rectangle(8, 24, 370, 135));
    newPremiseButton.setText("New");
    newPremiseButton.setBounds(new Rectangle(33, 166, 75, 23));
    editPremiseButton.setBounds(new Rectangle(116, 166, 75, 23));
    editPremiseButton.setText("Edit");
    deletePremiseButton.setBounds(new Rectangle(200, 166, 75, 23));
    deletePremiseButton.setText("Delete");
    savePremiseButton.setBounds(new Rectangle(283, 166, 75, 23));
    savePremiseButton.setText("Save");
    masterPanel.setBackground(new Color(0, 151, 0));
    this.setTitle("Add/Edit Scheme");
    editPremiseLabel.setText("Edit premise");
    editPremiseLabel.setBounds(new Rectangle(7, 194, 366, 18));
    editPremiseTextField.setBounds(new Rectangle(6, 214, 374, 22));
    critQuesPanel.setBackground(new Color(197, 255, 203));
    critQuesPanel.setBorder(titledBorder2);
    critQuesPanel.setBounds(new Rectangle(4, 372, 389, 248));
    critQuesPanel.setLayout(null);
    okCancelPanel.setBackground(new Color(255, 255, 190));
    okCancelPanel.setBounds(new Rectangle(6, 625, 386, 27));
    okCancelPanel.setLayout(null);
    saveButton.setText("OK");
    saveButton.setBounds(new Rectangle(87, 0, 79, 27));
    cancelButton.setBounds(new Rectangle(208, 0, 79, 27));
    cancelButton.setText("Cancel");
    critQuesListScroll.setBorder(BorderFactory.createLineBorder(Color.black));
    critQuesListScroll.setBounds(new Rectangle(10, 28, 368, 138));
    newCriticalQuestionButton.setBounds(new Rectangle(34, 173, 75, 23));
    newCriticalQuestionButton.setText("New");
    editCriticalQuestionButton.setBounds(new Rectangle(116, 173, 75, 23));
    editCriticalQuestionButton.setText("Edit");
    deleteCriticalQuestionButton.setBounds(new Rectangle(198, 173, 75, 23));
    deleteCriticalQuestionButton.setText("Delete");
    saveCriticalQuestionButton.setBounds(new Rectangle(280, 173, 75, 23));
    saveCriticalQuestionButton.setText("Save");
    editCriticalQuestionLabel.setBounds(new Rectangle(8, 201, 366, 18));
    editCriticalQuestionLabel.setText("Edit critical question");
    editCriticalQuestionTextField.setBounds(new Rectangle(7, 219, 374, 22));
    masterPanel.add(argNameConcPanel, null);
    argNameConcPanel.add(argNameTextField, null);
    argNameConcPanel.add(conclusionTextField, null);
    argNameConcPanel.add(argNameLabel, null);
    argNameConcPanel.add(conclusionLabel, null);
    masterPanel.add(premisePanel, null);
    premisePanel.add(premiseScroll, null);
    premisePanel.add(deletePremiseButton, null);
    premisePanel.add(newPremiseButton, null);
    premisePanel.add(editPremiseButton, null);
    premisePanel.add(savePremiseButton, null);
    premisePanel.add(editPremiseLabel, null);
    premisePanel.add(editPremiseTextField, null);
    masterPanel.add(critQuesPanel, null);
    critQuesPanel.add(critQuesListScroll, null);
    critQuesPanel.add(newCriticalQuestionButton, null);
    critQuesPanel.add(editCriticalQuestionButton, null);
    critQuesPanel.add(deleteCriticalQuestionButton, null);
    critQuesPanel.add(saveCriticalQuestionButton, null);
    critQuesPanel.add(editCriticalQuestionLabel, null);
    critQuesPanel.add(editCriticalQuestionTextField, null);
    masterPanel.add(okCancelPanel, null);
    okCancelPanel.add(saveButton, null);
    okCancelPanel.add(cancelButton, null);
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(masterPanel, BorderLayout.CENTER);
    this.pack();

    editPremiseButton.setEnabled(false);
    deletePremiseButton.setEnabled(false);
    savePremiseButton.setEnabled(false);
    editPremiseTextField.setEnabled(false);
    editPremiseTextField.addKeyListener(
      new KeyAdapter() {
        public void keyPressed(KeyEvent event)
        {
          switch (event.getKeyCode()) {
          case KeyEvent.VK_ENTER:
            processPremise();
            break;
          }
        }
      }
    );
    editCriticalQuestionButton.setEnabled(false);
    deleteCriticalQuestionButton.setEnabled(false);
    saveCriticalQuestionButton.setEnabled(false);
    editCriticalQuestionTextField.setEnabled(false);
    editCriticalQuestionTextField.addKeyListener(
      new KeyAdapter() {
        public void keyPressed(KeyEvent event)
        {
          switch (event.getKeyCode()) {
          case KeyEvent.VK_ENTER:
            processQuestion();
            break;
          }
        }
      }
    );
  }

  public void actionPerformed(ActionEvent event)
  {
    Object button = event.getSource();
    if (button == saveButton) {
      saveRequested = true;
      this.hide();
    } else if (button == cancelButton) {
      saveRequested = false;
      this.hide();
    } else if (button == newCriticalQuestionButton) {
      criticalQuestionList.clearSelection();
      questionState = NEW_QUESTION;
      editCriticalQuestionLabel.setText("Add question - press return to save");
      editCriticalQuestionTextField.setEnabled(true);
      saveCriticalQuestionButton.setEnabled(true);
      editCriticalQuestionButton.setEnabled(false);
      deleteCriticalQuestionButton.setEnabled(false);
      newCriticalQuestionButton.setEnabled(false);
      editCriticalQuestionTextField.setText("");
      editCriticalQuestionTextField.requestFocus();
    } else if (button == editCriticalQuestionButton) {
      String selectedQuestion = (String)criticalQuestionList.getSelectedValue();
      selectedQuestionIndex = criticalQuestionList.getSelectedIndex();
      if (selectedQuestion == null) return;
      questionState = EDIT_QUESTION;
      editCriticalQuestionLabel.setText("Edit question - press return to save");
      editCriticalQuestionTextField.setEnabled(true);
      editCriticalQuestionTextField.setText(selectedQuestion);
      saveCriticalQuestionButton.setEnabled(true);
      editCriticalQuestionTextField.requestFocus();
    } else if (button == deleteCriticalQuestionButton) {
      selectedQuestionIndex = criticalQuestionList.getSelectedIndex();
      criticalQuestionListModel.removeElementAt(selectedQuestionIndex);
      deleteCriticalQuestionButton.setEnabled(false);
      editCriticalQuestionButton.setEnabled(false);
      saveCriticalQuestionButton.setEnabled(false);
      editCriticalQuestionTextField.setText("");
    } else if (button == saveCriticalQuestionButton) {
      processQuestion();
    } else if (button == newPremiseButton) {
      premiseList.clearSelection();
      premiseState = NEW_PREMISE;
      editPremiseLabel.setText("Add premise - press return to save");
      editPremiseTextField.setEnabled(true);
      savePremiseButton.setEnabled(true);
      editPremiseButton.setEnabled(false);
      deletePremiseButton.setEnabled(false);
      newPremiseButton.setEnabled(false);
      editPremiseTextField.setText("");
      editPremiseTextField.requestFocus();
    } else if (button == editPremiseButton) {
      String selectedPremise = (String)premiseList.getSelectedValue();
      selectedPremiseIndex = premiseList.getSelectedIndex();
      if (selectedPremise == null) return;
      premiseState = EDIT_PREMISE;
      editPremiseLabel.setText("Edit premise - press return to save");
      editPremiseTextField.setEnabled(true);
      editPremiseTextField.setText(selectedPremise);
      savePremiseButton.setEnabled(true);
      editPremiseTextField.requestFocus();
    } else if (button == deletePremiseButton) {
      selectedPremiseIndex = premiseList.getSelectedIndex();
      premiseListModel.removeElementAt(selectedPremiseIndex);
      deletePremiseButton.setEnabled(false);
      editPremiseButton.setEnabled(false);
      savePremiseButton.setEnabled(false);
      editPremiseTextField.setText("");
    } else if (button == savePremiseButton) {
      processPremise();
    }
  }
}
