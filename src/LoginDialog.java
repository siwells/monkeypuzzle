import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginDialog extends JDialog implements ActionListener
{
  JPanel textPanel = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JTextField usernameTextField = new JTextField();
  JLabel whyLoginLabel = new JLabel("<html><CENTER><P>\n" +
		 "Please enter your username to log on to AraucariaDB. You need to " +
		"log on in order to be able to search the online repository, and " +
		"to save your own analyses to the repository. (To log on you " +
		"must previously have registered. Select AraucariaDB - Register " +
		"from the menu).</P></CENTER> </html>", new ImageIcon("images/Key.gif"),
			JLabel.CENTER);
  
  JLabel messageLabel = new JLabel(" ", JLabel.CENTER);
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  boolean okPressed = false;
  Araucaria owner;
/**
 * 

 */
  public LoginDialog(Araucaria parent)
  {
    super(parent, true);
    owner = parent;
    try
    {
      jbInit();
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
  
  private void jbInit() throws Exception
  {
    this.setResizable(true);
    this.setTitle("Login to use database");
    SizedPanel masterPanel = new SizedPanel(500, 160);
    masterPanel.setLayout(new BorderLayout(10,10));
    JPanel whyPanel = new JPanel(new GridLayout(1,1));
    whyPanel.add(whyLoginLabel);
    
    JPanel labelPanel = new JPanel(new GridLayout(1,1));
    textPanel.setLayout(new GridLayout(1,1));
    labelPanel.add(new JLabel("Enter username: ", JLabel.RIGHT));
    textPanel.add(usernameTextField);
    
    JPanel dataPanel = new JPanel(new BorderLayout());
    dataPanel.add(labelPanel, BorderLayout.WEST);
    dataPanel.add(textPanel, BorderLayout.CENTER);
    dataPanel.add(messageLabel, BorderLayout.SOUTH);
    
    okButton.setActionCommand("okButton");
    okButton.setText("OK");
    cancelButton.setActionCommand("cancelButton");
    cancelButton.setText("Cancel");
    masterPanel.add(whyPanel, BorderLayout.NORTH);
    masterPanel.add(dataPanel, BorderLayout.CENTER);
    masterPanel.add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    okButton.addActionListener(this);
    cancelButton.addActionListener(this);
    addDialogCloser(cancelButton);
    this.getRootPane().setDefaultButton(okButton);
    this.getContentPane().add(masterPanel);
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

  public boolean isOKPressed()
  { return okPressed; }
  
  private void okButtonPressed()
  {
    okPressed = true;
   	if (owner.doLogin()) {
   		messageLabel.setText("Login successful");
   		try {
	     		Thread.sleep(500);
				 }
				 catch (Exception ex) { }
   		this.hide();
   	} else {
   		messageLabel.setText("Username not found. Please try another or register.");
   		usernameTextField.setText("");
   		owner.setMessageLabelText("Username not found. Please try another or register.");
   	}
  }

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == okButton) {
      okButtonPressed();
    } else {
      okPressed = false;
	    this.hide();
    }
  }
}

