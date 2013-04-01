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
 * Allows user to enter source & comments when saving an argument to
 * the database.
 *
 * Also allows viewing of the source & comments when retrieving an
 * entry from the database.
 * 
 * Distinguish between the two uses by setting the enterMode flag.
 */
public class DBSourceComments extends JDialog implements ActionListener
{
  public Araucaria owner;
  JButton okButton, cancelButton;
  public JTextArea sourceText, commentsText;
  public boolean okPressed, enterMode;
  String enterSource = "Enter source of text:";
  String enterComments = "Enter comments on argument:";
  String showSource = "Source of text:";
  String showComments = "Comments on argument:";

  /**
   * If enter == true, dialog is created to allow entry of data.
   * Otherwise, allows read-only access to data.
   */
  public DBSourceComments(Frame parent, boolean modal, boolean enter) {
    super(parent, modal);
    owner = (Araucaria)parent;
    enterMode = enter;
    WindowHandler windowHandler = new WindowHandler();
    addWindowListener(windowHandler);
    setTitle("Source & Comments");
    setSize(300, 300);
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation(d.width/2 - getSize().width/2,
      d.height/2 - getSize().height/2);
    initComponents();
  }

  private void initComponents()
  {
    JPanel panel = buildPanel();
    getContentPane().add(panel, BorderLayout.CENTER);
    okButton = new JButton("OK");
    okButton.addActionListener(this);
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(this);
    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
  }
  
  private JPanel buildPanel()
  {
    JPanel mainPanel = new JPanel(new GridLayout(2,0,10,10));
    JPanel sourcePanel = new JPanel(new BorderLayout());
    if (enterMode) {
      sourcePanel.add(new JLabel(enterSource), BorderLayout.NORTH);
    } else {
      sourcePanel.add(new JLabel(showSource), BorderLayout.NORTH);
    }
    sourceText = new JTextArea();
    sourceText.setLineWrap(true);
    sourceText.setWrapStyleWord(true);
    if (!enterMode) {
      sourceText.setEditable(false);
    }
    JScrollPane sourceScroll = new JScrollPane(sourceText);
    sourcePanel.add(sourceScroll, BorderLayout.CENTER);
    
    JPanel commentsPanel = new JPanel(new BorderLayout());
    if (enterMode) {
      commentsPanel.add(new JLabel(enterComments), BorderLayout.NORTH);
    } else {
      commentsPanel.add(new JLabel(showComments), BorderLayout.NORTH);
    }
    commentsText = new JTextArea();
    commentsText.setLineWrap(true);
    commentsText.setWrapStyleWord(true);
    if (!enterMode) {
      commentsText.setEditable(false);
    }
    JScrollPane commentsScroll = new JScrollPane(commentsText);
    commentsPanel.add(commentsScroll, BorderLayout.CENTER);
    
    mainPanel.add(sourcePanel);
    mainPanel.add(commentsPanel);
    return mainPanel;
  }
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == okButton) {
      okPressed = true;
      this.setVisible(false);
    } else if (e.getSource() == cancelButton) {
      okPressed = false;
      this.setVisible(false);
    }
  }
  
   /**
   * Window handler closes the main frame window
   */
    class WindowHandler extends WindowAdapter
    {
      public void windowClosing(WindowEvent event)
      {
        Object object = event.getSource();
        if (object == DBSourceComments.this)
          setVisible(false);
      }
    }
}
