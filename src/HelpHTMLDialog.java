import javax.swing.*;
import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.border.*;
import java.util.*;

public class HelpHTMLDialog extends JDialog
{
  JScrollPane helpScrollPane = new JScrollPane();
  JEditorPane helpPane;
  URL contents;
  JToolBar helpToolBar;
  JButton homePageToolBar, backPageToolBar, forwardPageToolBar;
  JLabel statusBar = new JLabel("Status bar");
  LinkedList history = new LinkedList();
  int historyPosition = 0;
  static final int WIDTH = 600, HEIGHT = 480;
  
  public HelpHTMLDialog(Frame owner)
  {
    super(owner);
    try
    {
      contents = new File("Help" + File.separator + "index.html").toURL();
      history.add(historyPosition, contents);
      
      helpPane = new JEditorPane(contents);
      statusBar.setText("Last link: " + contents);

      helpPane.setEditable(false);
      helpPane.addHyperlinkListener(new Hyperactive());
    }
    catch (Exception e)
    {
      JOptionPane.showMessageDialog(this, "There was an error while attempting to display help");
    }
    init();
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation(d.width/2 - this.getSize().width/2,
      d.height/2 - this.getSize().height/2);
  }
  
  public void init()
  {
    this.setTitle("Help");
    JPanel helpPanel = new JPanel(new BorderLayout());
    helpScrollPane = new JScrollPane(helpPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    helpScrollPane.getViewport().setBackground(new Color(1.0f, 0.8f, 0.5f));
    helpPanel.add(helpScrollPane, BorderLayout.CENTER);
    
    initToolBar();
    helpPanel.add(helpToolBar, BorderLayout.NORTH);
    
    helpPanel.add(statusBar, BorderLayout.SOUTH);
    
    this.getContentPane().add(helpPanel, BorderLayout.CENTER);
    this.setSize(WIDTH, HEIGHT);
  } 
  
  private void initToolBar()
  {
    helpToolBar = new JToolBar();
    helpToolBar.setFloatable(false);
    helpToolBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    helpToolBar.setAlignmentX(LEFT_ALIGNMENT);

    Action homePageAction = new PageActionHandler("Help start", new ImageIcon("images/Help.gif"));
    homePageAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
        KeyEvent.VK_UP, 0));
    homePageAction.putValue(Action.SHORT_DESCRIPTION, "Help start");
    homePageToolBar = helpToolBar.add(homePageAction);
    homePageToolBar.setEnabled(true);

    Action backPageAction = new PageActionHandler("Back a page", new ImageIcon("images/Left.gif"));
    backPageAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
        KeyEvent.VK_LEFT, 0));
    backPageAction.putValue(Action.SHORT_DESCRIPTION, "Back a page");
    backPageToolBar = helpToolBar.add(backPageAction);
    backPageToolBar.setEnabled(false);

    Action forwardPageAction = new PageActionHandler("Forward a page", new ImageIcon("images/Right.gif"));
    forwardPageAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
        KeyEvent.VK_RIGHT, 0));
    forwardPageAction.putValue(Action.SHORT_DESCRIPTION, "Forward a page");
    forwardPageToolBar = helpToolBar.add(forwardPageAction);
    forwardPageToolBar.setEnabled(false);
  }       

  private void followLink(URL link)
  {
    // Block everything but the commonest types of web page
    if ((!link.toString().endsWith(".htm")) &&
      (!link.toString().endsWith(".html")) &&
      (!link.toString().endsWith("/")) &&
      (!link.toString().endsWith(".txt")) &&
      (!link.toString().endsWith(".asp")))
    {
      JOptionPane.showMessageDialog(this, "The help panel doesn't support this type of link\n" +
        "Please use a full-featured browser such as IE or Netscape");
      return;
    }
    
    history.add(++historyPosition, link);
            
    int count = history.size() - historyPosition - 1;
    for (int i = 0; i < count; i++)
      history.removeLast();
    
    backPageToolBar.setEnabled(true);
    forwardPageToolBar.setEnabled(false);
    
    try {
      helpPane.setPage(link);
    }
    catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "The help panel doesn't support this type of link\n" +
        "Please use a full-featured browser such as IE or Netscape");
    }
  }
    
  void displayContents() 
  { 
    followLink(contents); 
  }
  
  void goBack()
  {
    historyPosition--;
  
    URL url = (URL) history.get(historyPosition);
    try {
      helpPane.setPage(url);
      forwardPageToolBar.setEnabled(true);      
      if (historyPosition == 0)
        backPageToolBar.setEnabled(false);
    }
    catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "The help panel doesn't support this type of link\n" +
        "Please use a full-featured browser such as IE or Netscape");
    }    
  }
  
  void goForward()
  {
    historyPosition++;
    
    URL url = (URL) history.get(historyPosition);
    try {
      helpPane.setPage(url);
      backPageToolBar.setEnabled(true);     
      if (historyPosition == history.size() - 1)
        forwardPageToolBar.setEnabled(false);
    }
    catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "The help panel doesn't support this type of link\n" +
        "Please use a full-featured browser such as IE or Netscape");
    }    
  }
  
  class Hyperactive implements HyperlinkListener
  {
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        if (e instanceof HTMLFrameHyperlinkEvent)
        {
          HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
          HTMLDocument doc = (HTMLDocument) helpPane.getDocument();
          doc.processHTMLFrameHyperlinkEvent(evt);
        }
        else
          try { followLink(e.getURL()); }
          catch (Throwable t)
          {
            t.printStackTrace();
            JOptionPane.showMessageDialog(HelpHTMLDialog.this, "Help cannot load this page.");           
          }
      }
      else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED)
      {
        statusBar.setText("Last link: " + e.getURL());
      }
      
      else if (e.getEventType() == HyperlinkEvent.EventType.EXITED)
      {
 //       m_statusBar.setStatus(m_statusString.trim());
      }
    }
  }

  class PageActionHandler extends AbstractAction //implements ActionListener
  {
    PageActionHandler()
    { }
    
    PageActionHandler(String name, Icon icon)
    { super(name, icon); }
    
    PageActionHandler(String name)
    { super(name); }
    
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == backPageToolBar) {
        goBack();
      }
      else if (event.getSource() == forwardPageToolBar) {
        goForward();
      }
      else if (event.getSource() == homePageToolBar) {
        displayContents();
      }
    }
  } 
}
