/*
 * DisplayFrame.java
 *
 * Created on 12 April 2004, 11:16
 */

/**
 * Contains a main diagram (derived from DiagramBase) and a FreeVertexPanel.
 * Display Frame should be used to add a new display to the tabbed pane in the
 * main Araucaria window, and for the tree search panel in the database search 
 * dialog.
 * @author  growe
 */
import javax.swing.*;
import java.awt.*;

public class DisplayFrame extends JPanel
{
  JScrollPane mainScrollPane;
  DiagramBase mainDiagramPanel;
  FreeVertexPanel freeVertexPanel;
  /**
   * controlFrame is the top-level frame containing the DisplayFrame.
   * For displays in the main window, this will be the Araucaria class;
   * for displays in the database search dialog, this will be SearchFrame
   */
  ControlFrame controlFrame;
  Argument argument;
  Araucaria araucaria;
  
  /** Creates a new instance of DisplayFrame */
  public DisplayFrame()
  {
    setLayout(new BorderLayout());
  }
  
  public DisplayFrame(DiagramBase d, FreeVertexPanel f)
  {
    setLayout(new BorderLayout());
    add(d, BorderLayout.CENTER);
    add(f, BorderLayout.SOUTH);
  }
  
  public void setMainDiagramPanel(DiagramBase d)
  {
    mainDiagramPanel = d;
    mainDiagramPanel.setDisplayFrame(this);
    mainScrollPane = new JScrollPane(d);
    add(mainScrollPane, BorderLayout.CENTER);
    mainScrollPane.getVerticalScrollBar().setUnitIncrement(30);
    mainScrollPane.getHorizontalScrollBar().setUnitIncrement(30);
  }
  
  public JScrollPane getMainScrollPane()
  {
    return mainScrollPane;
  }
  
  public DiagramBase getMainDiagramPanel()
  {
    return mainDiagramPanel;
  }
  
  public void setFreeVertexPanel(FreeVertexPanel f)
  {
    freeVertexPanel = f;
    freeVertexPanel.setDisplayFrame(this);
    add(f, BorderLayout.SOUTH);
  }
  
  public FreeVertexPanel getFreeVertexPanel()
  {
    return freeVertexPanel;
  }
  
  public void setControlFrame(ControlFrame c)
  {
    controlFrame = c;
  }
  
  public void setArgument(Argument a)
  {
    argument = a;
    mainDiagramPanel.setArgument(a);
    freeVertexPanel.setArgument(a);
  }
  
  public void setAraucaria(Araucaria a)
  {
    araucaria = a;
    mainDiagramPanel.setAraucaria(a);
    freeVertexPanel.setAraucaria(a);
  }
  
  public void refreshPanels(boolean doRepaint)
  { 
    mainDiagramPanel.redrawTree(doRepaint);
    this.mainScrollPane.setViewportView(mainDiagramPanel);
    freeVertexPanel.redrawTree(doRepaint);
  }
}
