package org.simonwells.monkeypuzzle;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/**
 * Title:        Araucaria
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      University of Dundee
 * @author Glenn Rowe
 * @version 1.0
 */

public class AboutDialog extends JDialog implements ActionListener
{
  JLabel aboutMessage;
  JButton okButton;

  public AboutDialog(Frame owner)
  {
    super(owner);
    Araucaria.createIcon(owner);
    this.setModal(true);
    this.getContentPane().setBackground(Color.yellow);
    setTitle("About Araucaria - version 3.2; July 2008");

    ClassLoader loader = this.getClass().getClassLoader();
    if (loader == null) return;
    ImageIcon pic = new ImageIcon("images/AraucariaSplash3_2.jpg");
    aboutMessage = new JLabel(pic);
    okButton = new JButton("OK");
    okButton.addActionListener(this);
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(aboutMessage, BorderLayout.NORTH);
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(okButton);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    this.setSize(480, 255);
    this.pack();
    addDialogCloser();
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation(d.width/2 - this.getSize().width/2,
      d.height/2 - this.getSize().height/2);
    this.setVisible(true);
  }

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == okButton)
    {
      this.setVisible(false);
      this.dispose();
    }
  }
   
  public void addDialogCloser()
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
        
    okButton.getInputMap(
      JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "close");
    okButton.getActionMap().put("close", closeAction);
  }
}
