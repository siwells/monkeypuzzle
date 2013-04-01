/*
 * WigmoreRole.java
 *
 * Created on 07 September 2005, 07:42
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 *
 * @author growe
 */
import javax.swing.*;
import java.awt.image.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

public class WigmoreRole
{
  String roleLabel;
  int sortOrder;
  JMenuItem roleMenu;
  BufferedImage menuImage;
  Shape iconImage;
  private boolean defence = false;
  public static Dimension wigmoreIconSize = new Dimension(10, 10);
  public static Dimension wigmoreIconOffset = new Dimension(2, 2);
  enum RoleGroup
  {
    EXPLANATORY, CORROBORATIVE, GENERAL
  }
  RoleGroup group;

  /** Creates a new instance of WigmoreRole */
  public WigmoreRole()
  {
  }
  
  public WigmoreRole(String label, Dimension wigmoreIconSize, int xPos, int yPos, DiagramBase owner)
  {
    Initialize(label,  wigmoreIconSize,  xPos,  yPos,  owner);
  }
  
  public WigmoreRole(String label, Dimension wigmoreIconSize, DiagramBase owner)
  {
    Initialize(label,  wigmoreIconSize,  0,  0,  owner);
  }
  
  private void Initialize(String label, Dimension wigmoreIconSize, int xPos, int yPos, DiagramBase owner)
  {
    Graphics2D menuG;
    roleLabel = label;
    menuImage = new BufferedImage(wigmoreIconSize.width+1, wigmoreIconSize.height+1, BufferedImage.TYPE_INT_ARGB);
    menuG = menuImage.createGraphics();
    menuG.setPaint(Color.black);
    menuG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    if (label.equals("evidenceTestAffirm"))
    {
      iconImage = WigmoreImages.TestimonialAffirmatory(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Testimonial Affirmatory", new ImageIcon(menuImage));
      sortOrder = 1;
      group = RoleGroup.GENERAL;
      defence = false;
    } else if (label.equals("evidenceTestAffirmDef"))
    {
      iconImage = WigmoreImages.TestimonialAffirmatoryDef(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Testimonial Affirmatory Def", new ImageIcon(menuImage));
      sortOrder = 2;
      group = RoleGroup.GENERAL;
      defence = true;
    } else if (label.equals("evidenceTestNeg"))
    {
      iconImage = WigmoreImages.TestimonialNegatory(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Testimonial Negatory", new ImageIcon(menuImage));
      sortOrder = 3;
      group = RoleGroup.GENERAL;
      defence = false;
    } else if (label.equals("evidenceTestNegDef"))
    {
      iconImage = WigmoreImages.TestimonialNegatoryDef(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Testimonial Negatory Def", new ImageIcon(menuImage));
      sortOrder = 4;
      group = RoleGroup.GENERAL;
      defence = true;
    } else if (label.equals("evidenceCircumAffirm"))
    {
      iconImage = WigmoreImages.CircumstantialAffirmatory(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Circumstantial Affirmatory", new ImageIcon(menuImage));
      sortOrder = 5;
      group = RoleGroup.GENERAL;
      defence = false;
    } else if (label.equals("evidenceCircumAffirmDef"))
    {
      iconImage = WigmoreImages.CircumstantialAffirmatoryDef(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Circumstantial Affirmatory Def", new ImageIcon(menuImage));
      sortOrder = 6;
      group = RoleGroup.GENERAL;
      defence = true;
    } else if (label.equals("evidenceCircumNeg"))
    {
      iconImage = WigmoreImages.CircumstantialNegatory(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Circumstantial Negatory", new ImageIcon(menuImage));
      sortOrder = 7;
      group = RoleGroup.GENERAL;
      defence = false;
    } else if (label.equals("evidenceCircumNegDef"))
    {
      iconImage = WigmoreImages.CircumstantialNegatoryDef(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Circumstantial Negatory Def", new ImageIcon(menuImage));
      sortOrder = 8;
      group = RoleGroup.GENERAL;
      defence = true;
    } else if (label.equals("explanatory"))
    {
      iconImage = WigmoreImages.Explanatory(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Explanatory", new ImageIcon(menuImage));
      sortOrder = 9;
      group = RoleGroup.EXPLANATORY;
      defence = false;
    } else if (label.equals("explanatoryDef"))
    {
      iconImage = WigmoreImages.ExplanatoryDef(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Explanatory Def", new ImageIcon(menuImage));
      sortOrder = 10;
      group = RoleGroup.EXPLANATORY;
      defence = true;
    } else if (label.equals("corroborative"))
    {
      iconImage = WigmoreImages.Corroborative(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Corroborative", new ImageIcon(menuImage));
      sortOrder = 11;
      group = RoleGroup.CORROBORATIVE;
      defence = false;
    } else if (label.equals("corroborativeDef"))
    { 
      iconImage = WigmoreImages.CorroborativeDef(xPos, yPos, wigmoreIconSize.width, wigmoreIconSize.height);
      menuG.draw(iconImage);
      roleMenu = new JMenuItem("Corroborative Def", new ImageIcon(menuImage));
      sortOrder = 12;
      group = RoleGroup.CORROBORATIVE;
      defence = true;
    }
    roleMenu.addActionListener(owner);
  }

  public boolean isDefence()
  {
    return defence;
  }

  public void setDefence(boolean defence)
  {
    this.defence = defence;
  }
}
