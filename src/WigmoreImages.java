/*
 * WigmoreImages.java
 *
 * Created on 11 August 2005, 17:55
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 * Static methods providing vector representations of the symbols used in
 * Wigmore diagrams. Each image requires the rectangular dimensions of the
 * enclosing box, and will scale the image to fit that box.
 * @author growe
 */
import java.awt.*;
import java.awt.font.*;
import javax.swing.*;
import java.awt.geom.*;
public class WigmoreImages
{
  /** Creates a new instance of WigmoreImages */
  public WigmoreImages()
  {
  }
  
  public static Shape TestimonialAffirmatory(int x, int y, int w, int h)
  {
    return new Rectangle2D.Double(x, y, w, h);
  }
  
  public static Shape TestimonialAffirmatoryDef(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.append(TestimonialAffirmatory(x, y, w, h), false);
    path.append(new Line2D.Double(x, y + h/4, x + w, y + h/4), false);
    return path;
  }
  
  public static Shape TestimonialNegatory(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.moveTo(x, y + h);
    path.lineTo(x, y);
    path.lineTo(x + w, y);
    path.lineTo(x + w, y + h);
    return path;
  }
  
  public static Shape TestimonialNegatoryDef(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.append(TestimonialNegatory(x, y, w, h), false);
    path.append(new Line2D.Double(x, y + h/4, x + w, y + h/4), false);
    return path;
  }
  
  public static Shape CircumstantialAffirmatory(int x, int y, int w, int h)
  {
    return new Ellipse2D.Double(x, y, w, h);
  }
  
  public static Shape CircumstantialAffirmatoryDef(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.append(new Arc2D.Double(new Rectangle2D.Double(x, y, w, h), 30, 120, Arc2D.CHORD), false);
    path.append(new Arc2D.Double(new Rectangle2D.Double(x, y, w, h), 150, 240, Arc2D.OPEN), false);
    return path;
  }
  
  public static Shape CircumstantialNegatory(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.moveTo(x, y + h);
    path.lineTo(x, y + h/2);
    path.append(new Arc2D.Double(new Rectangle2D.Double(x, y, w, h), 0, 180, Arc2D.OPEN), false);
    path.moveTo(x + w, y + h/2);
    path.lineTo(x + w, y + h);
    return path;
  }
  
  public static Shape CircumstantialNegatoryDef(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.moveTo(x, y + h);
    path.lineTo(x, y + h/2);
    path.append(new Arc2D.Double(new Rectangle2D.Double(x, y, w, h), 0, 30, Arc2D.OPEN), false);
    path.append(new Arc2D.Double(new Rectangle2D.Double(x, y, w, h), 30, 120, Arc2D.CHORD), false);
    path.append(new Arc2D.Double(new Rectangle2D.Double(x, y, w, h), 150, 30, Arc2D.OPEN), false);
    path.moveTo(x + w, y + h/2);
    path.lineTo(x + w, y + h);
    return path;
  }
  
  public static Shape Explanatory(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.moveTo(x, y);
    path.lineTo(x + w, y + h/2);
    path.lineTo(x, y + h);
    return path;
  }
  
  public static Shape ExplanatoryDef(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.append(Explanatory(x, y, w, h), false);
    int gap = h / 5;
    path.moveTo(x, y + gap);
    path.lineTo(x + w - w*gap/h, y + (h + gap) / 2);
    return path;
  }
  
  public static Shape Corroborative(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.moveTo(x, y + h/2);
    path.lineTo(x + w, y);
    path.lineTo(x + w, y + h);
    path.lineTo(x, y + h/2);
    return path;
  }
  
  public static Shape CorroborativeDef(int x, int y, int w, int h)
  {
    GeneralPath path = new GeneralPath();
    path.append(Corroborative(x, y, w, h), false);
    int gap = h / 5;
    path.moveTo(x + w, y + gap);
    path.lineTo(x + w*gap/h, y + (h + gap) / 2);
    return path;
  }
  
  public enum NodePosition
  { LEFT, CENTRE, RIGHT };
  
  public static Shape getNodeLabelShape(String nodeLabel, Rectangle2D bound, NodePosition position)
  {
    if (nodeLabel == null) return null;
    GeneralPath path = new GeneralPath();
    int x = (int)bound.getX();
    int y = (int)bound.getY();
    int w = (int)bound.getWidth();
    int h = (int)bound.getHeight();
    Point centre = new Point(x + w/2, y + h/2);
    int offset = 0;
    
    switch (position)
    {
      case LEFT:
        offset = -w/4;
        break;
      case RIGHT:
        offset = w/4;
        break;
    }
    if (nodeLabel.toLowerCase().equals("belief"))
    {
      path.append(new Ellipse2D.Double(offset + centre.x - 1,  centre.y - 1, 1,  1), false);
      return path;
    } else if (nodeLabel.toLowerCase().equals("strong belief"))
    {
      path.append(new Ellipse2D.Double(offset + centre.x - w/4 - 1,  centre.y - 1, 1,  1), false);
      path.append(new Ellipse2D.Double(offset + centre.x + w/4 - 1,  centre.y - 1, 1,  1), false);
      return path;
    } else if (nodeLabel.toLowerCase().equals("disbelief"))
    {
      path.append(new Ellipse2D.Double(offset + centre.x - 2,  centre.y - 2, 4,  4), false);
      return path;
    } else if (nodeLabel.toLowerCase().equals("strong disbelief"))
    {
      path.append(new Ellipse2D.Double(offset + centre.x - w/4 - 2,  centre.y - 2, 4,  4), false);
      path.append(new Ellipse2D.Double(offset + centre.x + w/4 - 2,  centre.y - 2, 4,  4), false);
      return path;
    }
    return null;
  }
}
