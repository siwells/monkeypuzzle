import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
/**
 * Specifies user-definable preferences for the Araucaria interface.
 * To add a new preference:
 * 1. Add a field to the PrefParams class. Make sure the new field is Serializable.
 * 2. Add a line to applyPrefs() to apply the pref to whatever parameter it affects.
 * 3. Add code to refreshDisplay() if necessary. This is for prefs that affect the display of Araucaria.
 *    Shouldn't be needed for things like pathnames.
 * 4. Add code to the panel setup method so that the pref appears as a choice. For colors,
 *    add to addColourPanel()
 * 5. Add code to the event handler to apply the user's choice. For colours, this is runColorChooser()
 *    in ColorObject.
 */
public class Preferences extends JDialog implements ActionListener, ChangeListener
{
  public Araucaria owner;
  JTabbedPane m_tabbedPane;
  JPanel m_colourPanel, m_directoryPanel, m_databasePanel;
  ArgDisplayPanel argDisplayPanel;
  public PrefParams params;
  JButton okButton, cancelButton, resetButton;
  
  // Colour panel
  JList colourList;

  public Preferences(Frame parent, boolean modal) {
    super(parent, modal);
    owner = (Araucaria)parent;
    this.setResizable(false);
    WindowHandler windowHandler = new WindowHandler();
    addWindowListener(windowHandler);
    setTitle("Preferences");
    setSize(400, 300);
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation(d.width/2 - getSize().width/2,
      d.height/2 - getSize().height/2);
    readPrefs();
    initComponents();
  }

  private void initComponents()
  {
    m_tabbedPane = new JTabbedPane();
    addColourPanel();
    addDirectoryPanel();
    addDatabasePanel();
    addArgTypePanel();
    getContentPane().add(m_tabbedPane, BorderLayout.CENTER);
    okButton = new JButton("OK");
    okButton.addActionListener(this);
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(this);
    addDialogCloser(okButton);
    this.getRootPane().setDefaultButton(okButton);
    resetButton = new JButton("Reset");
    resetButton.addActionListener(this);
    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    buttonPanel.add(resetButton);
    cancelButton.setVisible(false);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    m_tabbedPane.addChangeListener(this);
  }
  
  private void okButtonPressed()
  {
    setDirectories();
    setDatabases();
    setArgDisplays();
    this.setVisible(false);
    writePrefs();
  }
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == okButton) {
      okButtonPressed();
    } else if (e.getSource() == resetButton) {
      int yesNo = JOptionPane.showConfirmDialog(null, "This will reset all your " +
        "preferences to the default values.\nDo you want to continue?",
        "Reset all parameters",
        JOptionPane.YES_NO_OPTION);
      if (yesNo == 0) {
        owner.resetDefaultParams();
        params = new PrefParams();
        refreshDisplay();
        refreshDialog();
      }
    } else if (e.getSource() == cancelButton) {
      this.setVisible(false);
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

  public void stateChanged(ChangeEvent e)
  {
    if (m_tabbedPane.getSelectedIndex() == 0) {
      cancelButton.setVisible(false);
    } else {
      cancelButton.setVisible(true);
    }
  }
  
  void readPrefs()
  {
    try {
      FileInputStream prefStream = new FileInputStream("prefs.dat");
      ObjectInputStream objStream = new ObjectInputStream(prefStream);
      params = (PrefParams)objStream.readObject();
      objStream.close();
      // Hard-code the database IP address
      params.ipAddress = Araucaria.ipAddress;
    }
    catch (Exception ex) {  // If we get an error, redefine prefs.dat from defaults
      params = new PrefParams();
      writePrefs();
    }
    refreshDisplay();
  }
  
  void writePrefs()
  {
    try {
      FileOutputStream prefStream = new FileOutputStream("prefs.dat");
      ObjectOutputStream objStream = new ObjectOutputStream(prefStream);
      objStream.writeObject(params);
      objStream.close();
    }
    catch (Exception ex) {
      System.out.println ("Exception in writePrefs: " + ex.toString());
    }
  }
  
  public void applyPrefs()
  {
    DiagramBase.DIAGRAM_BACKGROUND = params.backgroundColor;
    DiagramBase.FREE_VERTEX_BACKGROUND = params.freeVertexBackground;
    SelectText.TEXT_BACKGROUND = params.backgroundText;
    Araucaria.STATUSBAR_BACKGROUND = params.backgroundStatusBar;
    Araucaria.textDirectory = params.textDirectory;
    Araucaria.amlDirectory = params.amlDirectory;
    Araucaria.schemeDirectory = params.schemeDirectory;
    Araucaria.numRecentSchemeFiles = params.numRecentSchemeFiles;
    Araucaria.numRecentAmlFiles = params.numRecentAmlFiles;
    Araucaria.numRecentTextFiles = params.numRecentTextFiles;
    //Araucaria.ipAddress = params.ipAddress;
    Araucaria.databaseName = params.databaseName;
    Araucaria.username = params.username;
    Araucaria.password = params.password;
    Araucaria.databaseType = params.databaseType;
    owner.initDatabase();
    Araucaria.tutorModeOn = params.tutorMode;
    Araucaria.imageType = params.imageType;
  }
  
  public void refreshDisplay()
  {
    applyPrefs();
    writePrefs();
    owner.getSelectText().setBackground(SelectText.TEXT_BACKGROUND);
    owner.getSelectText().repaint();
    owner.messagePanel.setBackground(Araucaria.STATUSBAR_BACKGROUND);
    owner.messagePanel.repaint();
    owner.updateDisplays(true);
    owner.tutorMenu.setVisible(Araucaria.tutorModeOn);
  }
  
  public void refreshDialog()
  {
    ListModel model = colourList.getModel();
    for (int i = 0; i<model.getSize(); i++) {
      ColorObject co = (ColorObject)model.getElementAt(i);
      co.setColorFromParams();
    }
    colourList.repaint();
    this.amlPanel.directory.setText(params.amlDirectory);
    this.textPanel.directory.setText(params.textDirectory);
    this.schemePanel.directory.setText(params.schemeDirectory);
    amlPanel.numberModel.setValue(params.numRecentAmlFiles);
    this.databaseNameText.setText(params.databaseName);
    this.ipAddressText.setText(params.ipAddress);
    this.usernameText.setText(params.username);
    this.passwordText.setText(params.password);
    switch (params.databaseType)
    {
    case Araucaria.MYSQL:
      mysqlRadio.setSelected(true);
      break;
    case Araucaria.SQLSERVER:
      sqlServerRadio.setSelected(true);
      break;
    }
  }

  private void changeColor()
  {
    ColorObject co = (ColorObject) colourList.getSelectedValue();
    if (co != null)
      co.runColorChooser();
  }

  private void addColourPanel()
  {
    m_colourPanel = new JPanel(new BorderLayout());
    m_tabbedPane.addTab("Appearance", null, m_colourPanel, "Specify colours");
    colourList = new JList();
    DefaultListModel m = new DefaultListModel();
    m.addElement(new ColorObject(this, "Diagram background", DiagramBase.DIAGRAM_BACKGROUND, 
      "Background colour of main diagram area"));
    m.addElement(new ColorObject(this, "Free premise background", DiagramBase.FREE_VERTEX_BACKGROUND, 
      "Background colour of free premise panel"));
    m.addElement(new ColorObject(this, "Text background", SelectText.TEXT_BACKGROUND, 
      "Background colour of text area"));
    m.addElement(new ColorObject(this, "Status bar background", Araucaria.STATUSBAR_BACKGROUND, 
      "Background colour of status bar"));
    colourList.setModel(m);
    colourList.setCellRenderer(new ColorListRenderer()); 
    colourList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    colourList.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e)
      {     
//        if (e.getClickCount() == 2)
          changeColor();
      }
    });
    m_colourPanel.add(colourList, BorderLayout.CENTER);
  }
  
  JTextField ipAddressText, databaseNameText, usernameText;
  JPasswordField passwordText;
  JRadioButton mysqlRadio, sqlServerRadio;
  private void addDatabasePanel()
  {
    m_databasePanel = new JPanel(new GridLayout(5,1));
    m_tabbedPane.addTab("Database", null, m_databasePanel, "Specify database");
    
    ipAddressText = new JTextField(params.ipAddress);
    ipAddressText.setBorder(BorderFactory.createTitledBorder("IP address"));
    ipAddressText.setEnabled(false);
    
    databaseNameText = new JTextField(params.databaseName);
    databaseNameText.setBorder(BorderFactory.createTitledBorder("Database name"));
    
    mysqlRadio = new JRadioButton("MySQL");
    sqlServerRadio = new JRadioButton("SQL Server");
    ButtonGroup group = new ButtonGroup();
    group.add(mysqlRadio);
    group.add(sqlServerRadio);
    switch (params.databaseType)
    {
    case Araucaria.MYSQL:
      mysqlRadio.setSelected(true);
      break;
    case Araucaria.SQLSERVER:
      sqlServerRadio.setSelected(true);
      break;
    }
    JPanel radioPanel = new JPanel(new GridLayout(1,2));
    radioPanel.add(mysqlRadio);
    radioPanel.add(sqlServerRadio);
    radioPanel.setBorder(BorderFactory.createTitledBorder("Database type"));
    radioPanel.setBackground(Color.white);
    mysqlRadio.setBackground(Color.white);
    sqlServerRadio.setBackground(Color.white);
    
    usernameText = new JTextField(params.username);
    usernameText.setBorder(BorderFactory.createTitledBorder("Username"));
    
    passwordText = new JPasswordField(params.password);
    passwordText.setBorder(BorderFactory.createTitledBorder("Password"));
    
    m_databasePanel.add(ipAddressText);
    m_databasePanel.add(databaseNameText);
    //m_databasePanel.add(radioPanel);
    m_databasePanel.add(usernameText);
    m_databasePanel.add(passwordText);
  }
  
  private void setDatabases()
  {
    params.ipAddress = this.ipAddressText.getText();
    params.databaseName = this.databaseNameText.getText();
    if (this.mysqlRadio.isSelected()) {
      params.databaseType = Araucaria.MYSQL;
    } else if (this.sqlServerRadio.isSelected()) {
      params.databaseType = Araucaria.SQLSERVER;
    }
    params.username = this.usernameText.getText();
    params.password = new String(this.passwordText.getPassword());
    //Araucaria.ipAddress = params.ipAddress;
    Araucaria.databaseName = params.databaseName;
    Araucaria.username = params.username;
    Araucaria.password = params.password;
    Araucaria.databaseType = params.databaseType;
    owner.initDatabase();
    this.writePrefs();
  }
  
  private void setArgDisplays()
  {
    if (argDisplayPanel.tutorCheckBox.isSelected() && !owner.tutorModeOn)
    {
      owner.tutorMenu.setVisible(true);
      owner.tutorModeOn = true;
      params.tutorMode = true;
    } else if (!argDisplayPanel.tutorCheckBox.isSelected() && owner.tutorModeOn)
    {
      owner.tutorMenu.setVisible(false);
      owner.tutorModeOn = false;
      params.tutorMode = false;
    }
    
    // Image styles
    if (argDisplayPanel.tifRadio.isSelected())
    {
      Araucaria.imageType = Araucaria.IMAGE_TIF;
    } 
    else if (argDisplayPanel.jpgRadio.isSelected())
    {
      Araucaria.imageType = Araucaria.IMAGE_JPG;
    }
  }
  
  DirectoryPanel textPanel = new DirectoryPanel("Text (txt) files", this, Araucaria.numRecentTextFiles );
  DirectoryPanel amlPanel = new DirectoryPanel("Argument (AML) files", this, Araucaria.numRecentAmlFiles );
  DirectoryPanel schemePanel = new DirectoryPanel("Schemeset (scm) files", this, Araucaria.numRecentSchemeFiles );
  private void addDirectoryPanel()
  {
    m_directoryPanel = new JPanel(new BorderLayout());
    m_tabbedPane.addTab("Folders", null, m_directoryPanel, "Specify folders");
    m_directoryPanel.setLayout(new GridLayout(3,0));
    textPanel.directory.setText(params.textDirectory);
    amlPanel.directory.setText(params.amlDirectory);
    schemePanel.directory.setText(params.schemeDirectory);
    amlPanel.numberModel.setValue(params.numRecentAmlFiles);
    textPanel.numberModel.setValue(params.numRecentTextFiles);
    schemePanel.numberModel.setValue(params.numRecentSchemeFiles);
    m_directoryPanel.add(textPanel);
    m_directoryPanel.add(amlPanel);
    m_directoryPanel.add(schemePanel);
  }
  
  private void setDirectories()
  {
    if (setDirectory(textPanel.directory.getText(), "text")) {
      Araucaria.textDirectory = textPanel.directory.getText();
      params.textDirectory = Araucaria.textDirectory;
    }
    if (setDirectory(amlPanel.directory.getText(), "argument")) {
      Araucaria.amlDirectory = amlPanel.directory.getText();
      params.amlDirectory = Araucaria.amlDirectory;
    }
    if (setDirectory(schemePanel.directory.getText(), "schemeset")) {
      Araucaria.schemeDirectory = schemePanel.directory.getText();
      params.schemeDirectory = Araucaria.schemeDirectory;
    }
    Araucaria.numRecentAmlFiles = params.numRecentAmlFiles = amlPanel.numberModel.getNumber().intValue();
    Araucaria.numRecentTextFiles = params.numRecentTextFiles = textPanel.numberModel.getNumber().intValue();
    Araucaria.numRecentSchemeFiles = params.numRecentSchemeFiles = schemePanel.numberModel.getNumber().intValue();
    owner.buildRecentFileMenus();
    this.writePrefs();
  }
  
  private boolean setDirectory(String dirName, String messageWord)
  {
    File dir;
    try {
      dir = new File(dirName);
      if (!dir.exists()) {
        if (!dir.mkdir()) {
          JOptionPane.showMessageDialog(this, "Cannot create " + messageWord + " folder " + dirName +
            "\nCheck that you are not using illegal characters for folder names,\n" +
            "and that you are creating only one folder at a time.", 
            "Cannot create directory", JOptionPane.ERROR_MESSAGE);
          return false;
        } else {
          return true;
        }
      } else if (!dir.isDirectory()) {
        JOptionPane.showMessageDialog(this, "You must specify a folder (directory), not a file name\n" +
          "for the " + messageWord + " folder.", 
          "Cannot assign folder", JOptionPane.ERROR_MESSAGE);
        return false;
      }  else  {
        return true;
      }
    }
    catch (Exception ex) {
      System.out.println ("Error in Preferences.setDirectories: " + ex.toString());
    }
    return true;
  }
  
  private void addArgTypePanel()
  {
    argDisplayPanel = new ArgDisplayPanel(this);
    argDisplayPanel.tutorCheckBox.setSelected(params.tutorMode);
    m_tabbedPane.addTab("General", null, argDisplayPanel, "Other parameters");
  }
  
 /**
 * Window handler closes the main frame window
 */
  class WindowHandler extends WindowAdapter
  {
    public void windowClosing(WindowEvent event)
    {
      Object object = event.getSource();
      if (object == Preferences.this)
        setVisible(false);
    }
  }
  
}

class ArgDisplayPanel extends JPanel
{
  public JCheckBox tutorCheckBox;
  public JRadioButton tifRadio, jpgRadio;
  public ButtonGroup imageGroup;
  public ArgDisplayPanel(Preferences parent)
  {
    this.setLayout(new GridLayout(3,1));
    tutorCheckBox = new JCheckBox("Tutor mode");
    this.add(tutorCheckBox);
    
    tifRadio = new JRadioButton("TIFF");
    jpgRadio = new JRadioButton("JPEG");
    imageGroup = new ButtonGroup();
    imageGroup.add(tifRadio);
    imageGroup.add(jpgRadio);
    JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    imagePanel.setBorder(BorderFactory.createTitledBorder("Saved image type"));
    imagePanel.add(tifRadio);
    imagePanel.add(jpgRadio);
    
    if (Araucaria.imageType == Araucaria.IMAGE_TIF)
    {
      tifRadio.setSelected(true);
    } 
    else if (Araucaria.imageType == Araucaria.IMAGE_JPG)
    {
      jpgRadio.setSelected(true);
    }
    // Remove image panel since file dialog now selects image type
    //this.add(imagePanel);
  }
}

class DirectoryPanel extends JPanel implements ActionListener
{
  JButton browseButton;
  public JTextField directory;
  public String chosenDirectory;
  File lastDirectory;
  public JSpinner numRecentFiles;
  public SpinnerNumberModel numberModel;
  
  public DirectoryPanel(String label, Preferences parent, int recentFilesValue)
  {
    this.setBorder(BorderFactory.createTitledBorder(label));
    browseButton = new JButton("Browse...");
    browseButton.addActionListener(this);
    directory = new JTextField(20);
    JPanel entryPanel = new JPanel(new BorderLayout(10,10));
    entryPanel.add(directory, BorderLayout.CENTER);
    entryPanel.add(browseButton, BorderLayout.EAST);
    setLayout(new BorderLayout(5,5));
    add(entryPanel, BorderLayout.NORTH);
    lastDirectory = new File(".");
    numberModel = new SpinnerNumberModel(recentFilesValue, 1, 9, 1);
    numRecentFiles = new JSpinner(numberModel);
    JLabel recentFilesLabel = new JLabel("Recent files: ");
    JPanel recentPanel = new JPanel(new BorderLayout(10, 10));
    recentPanel.add(recentFilesLabel, BorderLayout.WEST);
    recentPanel.add(numRecentFiles, BorderLayout.EAST);
    JPanel recentBox = new JPanel(new BorderLayout());
    recentBox.add(recentPanel, BorderLayout.WEST);
    add(recentBox, BorderLayout.SOUTH);
  }
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == browseButton) {
      try {
        JFileChooser fileChoice = new JFileChooser();
        fileChoice.setDialogTitle("Select directory");
        fileChoice.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // Sets directory to last directory where a file was chosen
        fileChoice.setCurrentDirectory(lastDirectory);
        if (fileChoice.showOpenDialog(this) ==
            JFileChooser.APPROVE_OPTION)  {
          File selectedFile = fileChoice.getSelectedFile();
          lastDirectory = fileChoice.getCurrentDirectory();
          if (!selectedFile.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Choose a directory", "Choose a directory", JOptionPane.ERROR_MESSAGE);
            return;
          }
          directory.setText(selectedFile.getPath());
          chosenDirectory = selectedFile.getPath();
        }
      } catch (Exception ex) {
        System.out.println ("Error in Directory Panel action: " + ex.toString());
      }
    }
  }
  
}



class ColorListRenderer extends JLabel implements ListCellRenderer 
{
  public ColorListRenderer()
  {
    // Don't paint behind the component
    setOpaque(true);
  }
  
  // Set the attributes of the class and return a reference
  public Component getListCellRendererComponent(JList list, Object o, int i, boolean iss, boolean chf)  // cell has focus?
  {
    ColorObject co = (ColorObject) o;
  
    // Set the font
    setFont(list.getFont());
    
    // Set the text
      setText(co.m_name);
      
      setToolTipText(co.m_tooltip);
      
    // Set the icon     
    BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = (Graphics2D)image.createGraphics();
        
    g.setPaint(co.m_paint);
    g.fill(new Rectangle2D.Double(0, 0, 20, 10));
    g.setPaint(Color.black);
    g.draw(new Rectangle2D.Double(0, 0, 20, 10));
    g.dispose();
    
    setIcon(new ImageIcon(image));
    

      // Set background/foreground colours
    if (iss) { 
      setBackground(list.getSelectionBackground());
      setForeground(list.getSelectionForeground());
    }
    else {
      setBackground(list.getBackground());
      setForeground(list.getForeground());
    }

    return this;
  }
  
  public Insets getInsets(Insets i)
  {
    return new Insets(0, 3, 0, 0);
  }
}

class ColorObject
{
  Preferences m_parent;
  String m_name = null;
  String m_tooltip = null;
  public Color m_paint = null;
    
  ColorObject(Preferences parent, String name, Color paint, String tooltip)
  {
    m_parent = parent;
    m_name    = name;
    m_paint   = paint;
    m_tooltip = tooltip;
  }
  
  void setColorFromParams()
  {
    if (m_name.equals("Diagram background")) {
      m_paint = m_parent.params.backgroundColor;
    } else if (m_name.equals("Text background")) {
      m_paint = m_parent.params.backgroundText;
    } else if (m_name.equals("Free premise background")) {
      m_paint = m_parent.params.freeVertexBackground;
    } else if (m_name.equals("Status bar background")) {
      m_paint = m_parent.params.backgroundStatusBar;
    }
  }
  
  void runColorChooser()
  {
    m_paint = getColor(m_paint);
    if (m_name.equals("Diagram background")) {
      m_parent.params.backgroundColor = m_paint;
    } else if (m_name.equals("Text background")) {
      m_parent.params.backgroundText = m_paint;
    } else if (m_name.equals("Free premise background")) {
      m_parent.params.freeVertexBackground = m_paint;
    } else if (m_name.equals("Status bar background")) {
      m_parent.params.backgroundStatusBar = m_paint;
    }
    m_parent.refreshDisplay();
  }
  
  Color getColor(Color old)
  {
    Color newColor = JColorChooser.showDialog(m_parent, "Colour Chooser", old);

    if (newColor != null)
      return newColor;
    else
      return old;
  }
  
  public String toString()
  {
    return m_name;
  }
}

class PrefParams implements java.io.Serializable
{
  public Color backgroundColor;  // Background of main tree windows
  public Color backgroundText;   // Background of left-hand text panel
  public Color backgroundStatusBar;   // Background of status bar
  public Color freeVertexBackground;  // Background of free vertex panel
  // Directories
  public String textDirectory, amlDirectory, schemeDirectory;
  public int numRecentTextFiles, numRecentAmlFiles, numRecentSchemeFiles;
  // Databases
  public String ipAddress, databaseName, username, password;
  public int databaseType;
  // Argument display options
  public boolean tutorMode;
  public int imageType;
  
  public PrefParams()
  {
    // Colors
    backgroundColor = DiagramBase.DIAGRAM_BACKGROUND;
    freeVertexBackground = DiagramBase.FREE_VERTEX_BACKGROUND;
    backgroundText = SelectText.TEXT_BACKGROUND;
    backgroundStatusBar = Araucaria.STATUSBAR_BACKGROUND;
    // Directories
    textDirectory = Araucaria.textDirectory;
    amlDirectory = Araucaria.amlDirectory;
    schemeDirectory = Araucaria.schemeDirectory;
    numRecentTextFiles = Araucaria.numRecentTextFiles;
    numRecentAmlFiles = Araucaria.numRecentAmlFiles;
    numRecentSchemeFiles = Araucaria.numRecentSchemeFiles;
    // Database
    ipAddress = Araucaria.ipAddress;
    databaseName = Araucaria.databaseName;
    username = Araucaria.username;
    password = Araucaria.password;
    databaseType = Araucaria.databaseType;
    // Argument display options
    tutorMode = Araucaria.tutorModeOn;
    imageType = Araucaria.imageType;
  }
  
}