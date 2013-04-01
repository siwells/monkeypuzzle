import javax.swing.*;
import java.awt.Dimension;

class SizedPanel extends JPanel
{
  int m_width, m_height;

  public SizedPanel(int width, int height)
  {
    m_width = width;
    m_height = height;
  }

  public Dimension getPreferredSize()
  { return new Dimension(m_width, m_height); }
}
