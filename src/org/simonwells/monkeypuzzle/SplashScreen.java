package org.simonwells.monkeypuzzle;

import java.awt.*;
import javax.swing.*;

public class SplashScreen
{
  public SplashScreen()
  {
          // Create splash screen
    ImageIcon pic;
    try {
      ClassLoader loader = this.getClass().getClassLoader();
      if (loader == null) return;
      pic = new ImageIcon("images/AraucariaSplash3_2.jpg");
      JLabel splashLabel = new JLabel(pic);

      JWindow sScreen = new JWindow();
      sScreen.getContentPane().add(splashLabel);
      sScreen.pack();

      // Centre and display...
      Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
      sScreen.setLocation(d.width/2 - sScreen.getSize().width/2,
        d.height/2 - sScreen.getSize().height/2);
      sScreen.setVisible(true);


      // Do other stuff - eg, load main program...
      try { Thread.sleep(3000); }
      catch (InterruptedException e) {}

      sScreen.setVisible(false);
    } catch(Exception ex){
      System.out.println("Splash screen problem " + "\n" + ex);
      ex.printStackTrace();
    }
  }
}