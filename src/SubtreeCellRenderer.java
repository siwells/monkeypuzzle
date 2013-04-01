import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import javax.swing.*;

public class SubtreeCellRenderer extends JTextArea
  implements ListCellRenderer
{
    int paneWidth;
    Color evenColor = new Color(1.0f, 1.0f, 0.75f);
    Color oddColor = Color.white;
    Color textColor = new Color(0.0f, 0.0f, 0.25f);
    
    public SubtreeCellRenderer(int width) {
        super();
        paneWidth = width - 20;
        setOpaque(true);
    }
    
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus)
    {
        if (index % 2 != 0) {
            setBackground(oddColor);
            setForeground(textColor);
        } else {
            setBackground(evenColor);
            setForeground(textColor);
        }
        String text = (String)value;
        setText(text);
        setLineWrap(true);
        setWrapStyleWord(true);
        Graphics2D gg = (Graphics2D)list.getGraphics();
        if (gg == null) {
          System.out.println ("Null graphics - index = " + index + " " + text);
          return this;
        }
        
        Font font = gg.getFont();
        FontRenderContext frc = gg.getFontRenderContext();
        Rectangle2D textBounds = font.getStringBounds(text, frc);
        int numLines = (int)(textBounds.getWidth() / paneWidth) + 1;
        int textHeight = (int)textBounds.getHeight();
        setRows(numLines);
        setPreferredSize(new Dimension(paneWidth, textHeight * numLines));
        return this;
    }
}