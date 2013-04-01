import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RegistrationDialog extends JDialog implements ActionListener
{
  JPanel textPanel = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JTextField usernameTextField = new JTextField();
  JTextField fullNameTextField = new JTextField();
  JTextField addressTextField = new JTextField();
  JTextField emailTextField = new JTextField();
  JLabel whyLoginLabel = new JLabel("<html><CENTER><P>\n" +
		 "You need to register once before you can log on to AraucariaDB " +
			"and use the online repository. Please provide complete details " +
			"(these will not be released to any third party).</P></CENTER> </html>", 
			new ImageIcon("images/Key.gif"),	JLabel.CENTER);
  
  JLabel messageLabel = new JLabel(" ", JLabel.CENTER);
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  boolean okPressed = false;
  Araucaria owner;

  public RegistrationDialog(Araucaria parent)
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
    this.setTitle("Register to use database");
    SizedPanel masterPanel = new SizedPanel(500, 200);
    masterPanel.setLayout(new BorderLayout(5,5));
    JPanel whyPanel = new JPanel(new GridLayout(1,1));
    whyPanel.add(whyLoginLabel);

    JPanel labelPanel = new JPanel(new GridLayout(4,1));
    textPanel.setLayout(new GridLayout(4,1));
    labelPanel.add(new JLabel("Choose username: ", JLabel.RIGHT));
    textPanel.add(usernameTextField);
    labelPanel.add(new JLabel("Full name: ", JLabel.RIGHT));
    textPanel.add(fullNameTextField);
    labelPanel.add(new JLabel("Address: ", JLabel.RIGHT));
    textPanel.add(addressTextField);
    labelPanel.add(new JLabel("Email: ", JLabel.RIGHT));
    textPanel.add(emailTextField);
    
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

  private void okButtonPressed()
  {
    okPressed = true;
    if (emptyFields()) {
      messageLabel.setText("Please fill in all boxes.");
    } else if (owner.doRegister()) {
      messageLabel.setText("Registration successful");
      owner.setMessageLabelText("Registration successful");
      try {
        Thread.sleep(500);
       }
       catch (Exception ex) { }
      this.hide();
    } else {
      messageLabel.setText("Username already taken. Please choose another.");
      usernameTextField.setText("");
      owner.setMessageLabelText("Username already taken. Please choose another.");
    }
  }

  public boolean isOKPressed()
  { return okPressed; }
  
  private boolean emptyFields()
  {
  	if (usernameTextField.getText().length() == 0)
  		return true;
  	if (fullNameTextField.getText().length() == 0)
  		return true;
  	if (addressTextField.getText().length() == 0)
  		return true;
  	if (emailTextField.getText().length() == 0)
  		return true;
		return false;
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
