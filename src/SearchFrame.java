import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.*;

public class SearchFrame extends JDialog implements Runnable, ControlFrame 
{
  Araucaria araucaria;
  JTabbedPane m_tabbedPane;
  JPanel m_textSearchPanel, m_treeSearchPanel, m_schemesetSearchPanel;
  TextSearchTableModel textSearchTableModel;

  // Store various diagram types in an array of the abstract base class
  private final int numDiagrams = 1;
  private DiagramBase[] diagrams;
  // Labels for the various diagram types
  private final int DIAGRAM_TREESEARCH = 0;
  private DisplayFrame currentDiagram;    // The currently displayed diagram in the tabbed pane
  
  // Text searching panel
  private JPanel enterTextPanel;
  private JLabel enterTextLabel;
  private JTextField textSearchTextField;
  private JButton searchButton, closeButton, cancelButton;
  private JPanel searchTablePanel;
  private JScrollPane textSearchScrollPane;
  private JTable textSearchTable;
  private JPanel buttonPanel;
  JLabel textMessageLabel = new JLabel("");
  int m_width, m_height;
  
  JPanel searchResultsPanel;
  ActionEvent searchEvent;
  
  // Tree searching panel
  Argument argument;
  DiagramTreeSearch diagram;
  private JPanel enterTreePanel;
  private JButton searchTreeButton, clearDiagramButton, linkButton,
    unlinkButton, refutationButton, deleteButton;
  private JToolBar treeToolBar;
  private JPanel treeStatusBar;
  private JLabel treeStatusLabel;
  public UndoStack undoStack = new UndoStack();
  
  // Schemeset searching panel
  private JPanel enterSchemesetPanel;
  private JLabel enterSchemesetLabel;
  private JComboBox searchSchemesetCombo;
  private JButton searchSchemesetButton;
  private JPanel searchSchemesetTablePanel; 
  private JScrollPane schemesetSearchScrollPane;
  private JTable schemesetSearchTable;
  TextSearchTableModel schemesetSearchTableModel; 
  private JPanel buttonSchemesetPanel;
  JLabel schemeMessageLabel = new JLabel("");
  
  /** Creates new form SearchTextDialog */
    public SearchFrame(Frame parent, boolean modal) {
      super(parent, modal);
      araucaria = (Araucaria)parent;
      argument = new Argument();
      WindowHandler windowHandler = new WindowHandler();
      addWindowListener(windowHandler);
      setTitle("Search argument database");
      setSize(900, 630);
      
      this.addComponentListener(new ComponentAdapter() {
        public void componentResized(ComponentEvent e) {
          diagram.redrawTree(true);
        }
      });
      
      initComponents();
    }
    
    public Araucaria getAraucaria()
    { return araucaria; }
    
    public DisplayFrame getCurrentDiagram()
    { return currentDiagram; }
  
    public void updateDisplays(boolean updateCurrent)
    {
      for (int i = 0; i < numDiagrams; i++)
      {
        if (displays[i] != currentDiagram)
        {
          displays[i].setArgument(argument);
          displays[i].refreshPanels(true);
        }
        if (updateCurrent)
        {
          currentDiagram.setArgument(argument);
          currentDiagram.refreshPanels(true);
        }
      }
    }

    private void initComponents()
    {
      m_tabbedPane = new JTabbedPane();
      JPanel mainGridPanel = new JPanel(new GridLayout(1,2));
      mainGridPanel.add(m_tabbedPane);
      
      addTextSearchPanel();
      addTreeSearchPanel();
      addSchemesetSearchPanel();
      
      searchResultsPanel = new JPanel(new BorderLayout());
      searchResultsPanel.add(new JLabel("Search results", JLabel.CENTER), BorderLayout.NORTH);
      textSearchScrollPane = new JScrollPane();
      textSearchTable = new JTable();
      textSearchTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
      textSearchScrollPane.setViewportView(textSearchTable);
      JPanel searchTablePanel = new JPanel(new BorderLayout());
      searchTablePanel.add(textSearchScrollPane, BorderLayout.CENTER);
      searchResultsPanel.add(searchTablePanel, java.awt.BorderLayout.CENTER);

      textSearchTableModel = new TextSearchTableModel(araucaria);
      textSearchTable.setModel(textSearchTableModel);

      mainGridPanel.add(searchResultsPanel);
      
      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(mainGridPanel, BorderLayout.CENTER);
      
      JPanel buttonPanel = new JPanel();
      closeButton = new JButton("Close window");
      closeButton.setMnemonic(KeyEvent.VK_C);
      buttonPanel.add(closeButton);
      closeButton.addActionListener(new java.awt.event.ActionListener()
      {
        public void actionPerformed(java.awt.event.ActionEvent evt)
        {
          SearchFrame.this.hide();
          araucaria.setMessageLabelText(" ");
        }
      });
      cancelButton = new JButton("Cancel search");
      cancelButton.setMnemonic(KeyEvent.VK_A);
      buttonPanel.add(cancelButton);
      cancelButton.addActionListener(new java.awt.event.ActionListener()
      {
        public void actionPerformed(java.awt.event.ActionEvent evt)
        {
          araucaria.cancelSearch();
        }
      });
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt)
    {
      araucaria.doTextSearchOnDB(textSearchTextField.getText(), "<TEXT>", "</TEXT>",
        textSearchTableModel, textSearchTable);
    }

    private void addTextSearchPanel()
    {
      m_textSearchPanel = new JPanel(new BorderLayout());
      m_tabbedPane.addTab("Text", null, m_textSearchPanel, "Search text field");
      
      enterTextPanel = new JPanel();
      enterTextLabel = new JLabel();
      textSearchTextField = new JTextField();
      searchButton = new JButton();

      enterTextPanel.setLayout(new java.awt.BorderLayout());
      enterTextLabel.setText("Enter text to search for:");
      enterTextPanel.add(enterTextLabel, BorderLayout.NORTH);
      enterTextPanel.add(textSearchTextField, BorderLayout.CENTER);
      searchButton.setText("Search");
      searchButton.setMnemonic(KeyEvent.VK_S);
      enterTextPanel.add(searchButton, BorderLayout.SOUTH);
      searchButton.addActionListener(new java.awt.event.ActionListener()
      {
        public void actionPerformed(ActionEvent evt)
        {
          textMessageLabel.setText("Searching database. Please wait...");
          searchEvent = evt;
          Thread searchThread = new Thread(SearchFrame.this, "TextSearch");
          searchThread.start();
        }
      });
       
      m_textSearchPanel.add(enterTextPanel, BorderLayout.NORTH);
      m_textSearchPanel.add(textMessageLabel, BorderLayout.CENTER);
    }
    
    public void run()
    {
      Thread currentThread = Thread.currentThread();
      if(currentThread.getName().equals("TextSearch"))
      {
        searchButtonActionPerformed(searchEvent);
        textMessageLabel.setText("Search complete.");
      } else if(currentThread.getName().equals("SchemeSearch"))
      {
        searchSchemesetButtonActionPerformed(searchEvent);
        schemeMessageLabel.setText("Search complete.");
      } else if(currentThread.getName().equals("TreeSearch"))
      {
        treeStatusLabel.setText("Searching database. Please wait...");
        int matches = araucaria.doTreeSearch(argument.getTree(),
          textSearchTableModel, textSearchTable);
        if (matches == 0) {
          treeStatusLabel.setText("Search complete. No matches found.");
        } else if (matches == 1) {
          treeStatusLabel.setText("Search complete. Found 1 match.");
        } else {
          treeStatusLabel.setText("Search complete. Found " + matches + " matches.");
        }
      }
    }
    
    // Tree searching panel
    int numDisplays = 1;
    DisplayFrame[] displays = new DisplayFrame[numDisplays];
    
    private void addTreeSearchPanel()
    {
      m_treeSearchPanel = new JPanel(new BorderLayout());
      treeToolBar = new JToolBar();
      treeToolBar.setFloatable(false);
      treeToolBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
      treeToolBar.setAlignmentX(LEFT_ALIGNMENT);

      // Toolbar
      Action clearDiagramAction = new FileActionHandler("Clear diagram", new ImageIcon("images/Document.gif"));
      clearDiagramAction.putValue(Action.SHORT_DESCRIPTION, "Clear diagram");
      clearDiagramButton = treeToolBar.add(clearDiagramAction);

      Action linkAction = new FileActionHandler("Link premises", new ImageIcon("images/Pin.gif"));
      linkAction.putValue(Action.SHORT_DESCRIPTION, "Link premises");
      linkButton = treeToolBar.add(linkAction);

      Action unlinkAction = new FileActionHandler("Unlink premises", new ImageIcon("images/PinLeft.gif"));
      unlinkAction.putValue(Action.SHORT_DESCRIPTION, "Unlink premises");
      unlinkButton = treeToolBar.add(unlinkAction);

	    Action refutationAction = new FileActionHandler("Refutation", new ImageIcon("images/Widen.gif"));
	    refutationAction.putValue(Action.SHORT_DESCRIPTION, "Refutation");
	    refutationButton = treeToolBar.add(refutationAction);

      Action deleteAction = new FileActionHandler("Delete", new ImageIcon("images/Error.gif"));
      deleteAction.putValue(Action.SHORT_DESCRIPTION, "Delete selected items");
      deleteButton = treeToolBar.add(deleteAction);
			treeToolBar.addSeparator();
			
//      Action searchDBAction = new FileActionHandler("Search database", new ImageIcon("images/SearchRow.gif"));
//      searchDBAction.putValue(Action.SHORT_DESCRIPTION, "Search database");
//      searchTreeButton = treeToolBar.add(searchDBAction);
      searchTreeButton = new JButton(new ImageIcon("images/SearchRow.gif"));
      treeToolBar.add(searchTreeButton);
      searchTreeButton.setToolTipText("Search database");
      searchTreeButton.addActionListener(new java.awt.event.ActionListener()
      {
        public void actionPerformed(ActionEvent evt)
        {
          searchEvent = evt;
          Thread treeThread = new Thread(SearchFrame.this, "TreeSearch");
          treeThread.start();
        }
      });

      m_treeSearchPanel.add(treeToolBar, BorderLayout.NORTH);
      
      // Scaled diagram
      displays[0] = new DisplayFrame();
      displays[0].setMainDiagramPanel(new TreeSearchPanel());
      displays[0].setFreeVertexPanel(new FreeVertexPanel());
      displays[0].setControlFrame(this);
      displays[0].setAraucaria(araucaria);
      displays[0].setArgument(argument); 
      currentDiagram = displays[0];

      JPanel canvasPanel = new JPanel(new BorderLayout());
      canvasPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
      canvasPanel.add(displays[0], BorderLayout.CENTER);
      canvasPanel.setBackground(new Color(255, 255, 180));
      m_treeSearchPanel.add(canvasPanel, BorderLayout.CENTER);
      
      treeStatusBar = new JPanel();
      treeStatusLabel = new JLabel("Click to add a node to the search pattern");
      treeStatusBar.add(treeStatusLabel);
      m_treeSearchPanel.add(treeStatusBar, BorderLayout.SOUTH);
      m_tabbedPane.addTab("Structure", null, m_treeSearchPanel, "Search by tree structure");
    }
    
    public JLabel getMessageLabel()
    { return treeStatusLabel; }
    
    // Schemeset searching panel
    private void searchSchemesetButtonActionPerformed(java.awt.event.ActionEvent evt)
    {
      araucaria.doTextSearchOnDB((String)searchSchemesetCombo.getSelectedItem(), "<INSCHEME", "/>",
        textSearchTableModel, textSearchTable);
    }

    public void updateSchemesetSearchCombo()
    {
      searchSchemesetCombo.removeAllItems();
      Vector schemesetList = araucaria.getArgument().getSchemeList();
      if (schemesetList != null) {
        for (int i = 0; i < schemesetList.size(); i++) {
          searchSchemesetCombo.addItem(((ArgType)schemesetList.elementAt(i)).getName());
        }
      }
    }
    
    private void addSchemesetSearchPanel()
    {
      m_schemesetSearchPanel = new JPanel(new BorderLayout());
      m_tabbedPane.addTab("Scheme", null, m_schemesetSearchPanel, "Search for scheme");
      
      enterSchemesetPanel = new JPanel();
      enterSchemesetLabel = new JLabel();
      searchSchemesetCombo = new JComboBox();
      searchSchemesetCombo.setEditable(true);
      searchSchemesetButton = new JButton();
      searchSchemesetTablePanel = new JPanel();
      schemesetSearchScrollPane = new JScrollPane();
      schemesetSearchTable = new JTable();

      enterSchemesetPanel.setLayout(new java.awt.BorderLayout());
      enterSchemesetLabel.setText("Enter scheme to search for:");
      enterSchemesetPanel.add(enterSchemesetLabel, BorderLayout.NORTH);
      enterSchemesetPanel.add(searchSchemesetCombo, BorderLayout.CENTER);
      searchSchemesetButton.setText("Search");
      searchSchemesetButton.setMnemonic(KeyEvent.VK_S);
      enterSchemesetPanel.add(searchSchemesetButton, BorderLayout.SOUTH);
      searchSchemesetButton.addActionListener(new java.awt.event.ActionListener()
      {
        public void actionPerformed(ActionEvent evt)
        {
          searchEvent = evt;
          schemeMessageLabel.setText("Searching database. Please wait...");
          Thread schemeThread = new Thread(SearchFrame.this, "SchemeSearch");
          schemeThread.start();
        }
      });
       
      m_schemesetSearchPanel.add(enterSchemesetPanel, BorderLayout.NORTH);
      m_schemesetSearchPanel.add(schemeMessageLabel, BorderLayout.CENTER);
    }
    
    public void setMessageLabelText(String text)
    {
      treeStatusLabel.setText(text);
    }
    
    public UndoStack getUndoStack()
    {
      return undoStack;
    }
    
    public void doRedo()
    {
    }
    
    public void doUndo()
    {
    }
    
    /**
   * Window handler closes the main frame window
   */
    class WindowHandler extends WindowAdapter
    {
      public void windowClosing(WindowEvent event)
      {
        Object object = event.getSource();
        if (object == SearchFrame.this)
          setVisible(false);
      }
    }

    class FileActionHandler extends AbstractAction //implements ActionListener
    {
      FileActionHandler()
      { }

      FileActionHandler(String name, Icon icon)
      { super(name, icon); }

      public void actionPerformed(ActionEvent event)
      {
        String sourceCode = "";
        if (event.getSource() == searchTreeButton) {
        } else if (event.getSource() == clearDiagramButton) {
          argument.emptyTree(true);
        } else if (event.getSource() == linkButton) {
          try {
            argument.linkVertices();
          } catch (LinkException e) {
          }
        } else if (event.getSource() == unlinkButton) {
          try {
            argument.unlinkVertices();
          } catch (LinkException e) {
          }
        } else if (event.getSource() == refutationButton) {
          argument.setRefutations();
        } else if (event.getSource() == deleteButton) {
          argument.deleteSelectedItems();
        }
        updateDisplays(true);
      }
    }
}