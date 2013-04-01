/*
 * SelectText.java
 *
 * Created on 17 March 2004, 15:07
 */

import java.beans.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.text.*;
import java.awt.font.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;

/**
 *
 * @author  growe
 */
public class SelectText extends JPanel implements java.io.Serializable
{
  public Color STRONG_CARET_COLOR = Color.red;
  public Color HIGHLIGHT_COLOR = new Color(1.0f, 1.0f, 0.0f, 0.75f);;
  public Color USED_TEXT_COLOR = new Color(0.7f, 0.7f, 0.7f, 0.75f);;
  public Color TEXT_COLOR = Color.black;
  public static Color TEXT_BACKGROUND = new Color(0.75f, 1.0f, 1.0f);
  public String encoding = "UTF-8";
  public static int PREFERRED_WIDTH = 230;
  public static int PREFERRED_HEIGHT = 400;

  // The TextLayout to hit-test and select.
  Vector textLayout;
  
  // Text to use in samples:
  public String text = "", selectedText;
  private int offset;   // offset of start of selected text within text
  private AttributedString helloAttrib;
  private AttributedCharacterIterator helloIter;
  private LineBreakMeasurer lineBreak;

  // The insertion index of the initial mouse click;  one
  // end of the selection range.  During a mouse drag this end
  // of the selection is constant.
  private int anchorEnd, anchorLine;
  private int lineSpacing, lineSelected;

  // The insertion index of the current mouse location;  the
  // other end of the selection range.  This changes during a mouse
  // drag.
  private int activeEnd, activeLine;
  private boolean textSelected;
  private GeneralPath highlightArea;  // Highlighted area for current selection.
  private LinkedList selectedList;
  private GeneralPath selectedShape;  // Contains all text selected and used in vertices so far
  private final Hashtable map = new Hashtable();
  private final Font textFont = new Font("SansSerif", Font.PLAIN, 14);
  {
    map.put(TextAttribute.FONT, textFont);
  }
  
  // Determine which fonts support Chinese here .
  public void findChineseFonts() 
  {
    
    Font[] allfonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    int fontcount = 0;
    String chinesesample = "\u4e00";
    for (int j = 0; j < allfonts.length; j++) {
//          System.out.println ("Testing: " + allfonts[j].getFontName() + " --- " + 
//            allfonts[j].canDisplayUpTo(chinesesample));
        if (allfonts[j].canDisplayUpTo(chinesesample) == -1) {
          if (fontcount == 0) {
//              mTextArea.setFont(new Font(allfonts[j].getFontName(), Font.PLAIN, 24));
          }
          if (allfonts[j].getFontName().startsWith("dialog") == false &&
              allfonts[j].getFontName().startsWith("monospaced") == false &&
              allfonts[j].getFontName().startsWith("sansserif") == false &&
              allfonts[j].getFontName().startsWith("serif") == false) {
                System.out.println ("Supported font: " + allfonts[j].getFontName());
            fontcount++;
//               cfontchooser.addItem(allfonts[j].getFontName());
          }
        }
    }
    System.out.println ("Found " + fontcount + " Chinese fonts.");
  }

  public void setFont()
  {
//    findChineseFonts();
    if (encoding.equals("UTF-8")) {
      map.put(TextAttribute.FONT, new Font("Helvetica", Font.PLAIN, 14));
    } else if (encoding.equals("BIG5")) {
//      map.put(TextAttribute.FONT, new Font("Batang", Font.PLAIN, 14));
//      map.put(TextAttribute.FONT, new Font("Simsun", Font.PLAIN, 14));
      map.put(TextAttribute.FONT, new Font("Gulim", Font.PLAIN, 18));
    } else {
      map.put(TextAttribute.FONT, new Font("TimesRoman", Font.PLAIN, 14));
    }
  }
  
  public static int wordCount(String text)
  {
    StringTokenizer textToken = new StringTokenizer(text);
    int count = 0;
    while (textToken.hasMoreTokens()) {
      count++;
      textToken.nextToken();
    }
    return count;
  }

  public SelectText(int width, int height)
  {
    setBackground(TEXT_BACKGROUND);
    setPreferredSize(new Dimension(width, height));
    clearText();
  }
  
  public SelectText()
  {
    setBackground(TEXT_BACKGROUND);
    clearText();
  }
  
  public void clearText()
  {
    text = " ";
    textSelected = false;
    addMouseListener(new SelectionMouseListener());
    addMouseMotionListener(new SelectionMouseMotionListener());
    constructTextLayout();
  }

  public void constructTextLayout()
  {
    lineSelected = 0;
    FontRenderContext frc = new FontRenderContext(null, false, false);
    selectedList = new LinkedList();
    selectedShape = new GeneralPath();
    if (text == null || text.equals("")) return;
    // Create a new TextLayout from the given text.

    int textWidth = getPreferredSize().width - 15;
    if (textWidth <= 0) return;
    textLayout = new Vector();
    TextLayout layout;
    
    // Look for line breaks in original text and preserve these
    StringTokenizer lineTokens = new StringTokenizer(text, "\n", true);
    int lineStart = 0;
    int returns = 0;
    while (lineTokens.hasMoreTokens())
    {
      String nextLine = lineTokens.nextToken();
      if (nextLine.equals("\n"))
      {
        returns++;
        continue;
      }
      helloAttrib = new AttributedString(nextLine, map);
      helloIter = helloAttrib.getIterator();
      lineBreak = new LineBreakMeasurer(helloIter, frc);
      int first = helloIter.getBeginIndex();
      int last = helloIter.getEndIndex();
      lineBreak.setPosition(first);
      while (lineBreak.getPosition() < last) {
        layout = lineBreak.nextLayout(textWidth);
        textLayout.add(new TextLine(layout, lineStart + returns + first, 
                lineStart + returns + lineBreak.getPosition()));
        first = lineBreak.getPosition();
      }
      lineStart += last;
    }

    // Initialize activeEnd and anchorEnd.
    anchorEnd = 0;
    activeEnd = 0;
    anchorLine = activeLine = 0;

    layout = ((TextLine)textLayout.get(0)).getLayout();
    lineSpacing = (int)(layout.getAscent() + layout.getDescent());
    setPreferredSize(new Dimension(getPreferredSize().width, lineSpacing*textLayout.size()+25));
  }

  /**
   * Compute a location within this Panel for textLayout's origin,
   * such that textLayout is centered horizontally and vertically.
   *
   * Note that this location is unknown to textLayout;  it is used only
   * by this Panel for positioning.
   */
  private Point2D computeLayoutOrigin(int lineNum) {
    if (textLayout == null ||
      	textLayout.size() == 0)
      return new Point2D.Float(0f, 0f);
    Dimension size = getSize();
    Point2D.Float origin = new Point2D.Float();
    TextLayout layout = ((TextLine)textLayout.get(lineNum)).getLayout();
    origin.x = (float) (size.width - layout.getAdvance()) / 2;
    origin.y = (float) size.height / 2 +
      (layout.getDescent() + layout.getAscent()) *
      (2*lineNum - 1)/ 2;
    return origin;
  }

  // Tests if the highlighted area is separate from greyed out areas
  public boolean isSelectionDisjoint()
  {
    AffineTransform trans = new AffineTransform();
    Rectangle2D bounds;
    // If all selected text is on one line...
    if (anchorLine == activeLine) {
      TextLayout layout = ((TextLine)textLayout.get(anchorLine)).getLayout();
      GeneralPath highlight = new GeneralPath(
        layout.getBlackBoxBounds(anchorEnd, activeEnd).getBounds());
//        layout.getLogicalHighlightShape(anchorEnd, activeEnd));
      trans.setToTranslation(0, (anchorLine+1) * lineSpacing);
      highlight.transform(trans);
      bounds = highlight.getBounds2D();
      if (selectedShape.intersects(bounds)) {
        return false;
      }
    } else {
      // Selection extends over more than one line, so build
      // up GeneralPath from selected portions of each line
      int startLine = activeLine > anchorLine ? anchorLine : activeLine;
      int endLine = activeLine > anchorLine ? activeLine : anchorLine;
      int startChar = activeLine > anchorLine ? anchorEnd : activeEnd;
      int endChar;
      // Examine the first line in the selection
      TextLayout layout = ((TextLine)textLayout.get(startLine)).getLayout();
      endChar = layout.getCharacterCount();
      GeneralPath highlight = new GeneralPath(
        layout.getBlackBoxBounds(startChar, endChar).getBounds());
//        layout.getLogicalHighlightShape(startChar, endChar));
      trans.setToTranslation(0, (startLine+1) * lineSpacing);
      highlight.transform(trans);
      bounds = highlight.getBounds2D();
      if (selectedShape.intersects(bounds)) {
        return false;
      }
      // Examine all intermediate lines - each of which is a full line
      for (int line = startLine+1; line < endLine; line++) {
        layout = ((TextLine)textLayout.get(line)).getLayout();
        endChar = layout.getCharacterCount();
        highlight = new GeneralPath(
          layout.getBlackBoxBounds(0, endChar).getBounds());
//          layout.getLogicalHighlightShape(0, endChar));
        trans.setToTranslation(0, (line+1) * lineSpacing);
        highlight.transform(trans);
        bounds = highlight.getBounds2D();
        if (selectedShape.intersects(bounds)) {
          return false;
        }
      }
      // Examine the final line
      endChar = activeLine > anchorLine ? activeEnd : anchorEnd;
      layout = ((TextLine)textLayout.get(endLine)).getLayout();
      highlight = new GeneralPath(
        layout.getBlackBoxBounds(0, endChar).getBounds());
//        layout.getLogicalHighlightShape(0, endChar));
      trans.setToTranslation(0, (endLine+1) * lineSpacing);
      highlight.transform(trans);
      bounds = highlight.getBounds2D();
      if (selectedShape.intersects(bounds)) {
        return false;
      }
    }
    return true;
  }

  public void findHighlightArea()
  {
    TextLayout layout;
    int start, end;
    AffineTransform trans = new AffineTransform();
    highlightArea = new GeneralPath();
    // If all selected text is on one line...
    if (anchorLine == activeLine) {
      layout = ((TextLine)textLayout.get(anchorLine)).getLayout();
      Shape highlight =
        layout.getBlackBoxBounds(anchorEnd, activeEnd).getBounds();
//        layout.getLogicalHighlightShape(anchorEnd, activeEnd);
      trans.setToTranslation(0, (anchorLine+1) * lineSpacing);
      PathIterator path = highlight.getPathIterator(trans);
      highlightArea.append(path, false);
      start = anchorEnd > activeEnd ? activeEnd : anchorEnd;
      end = anchorEnd > activeEnd ? anchorEnd : activeEnd;
      start += ((TextLine)textLayout.get(anchorLine)).
        getStart();
      end += ((TextLine)textLayout.get(anchorLine)).
        getStart();
    } else {
      // Selection extends over more than one line, so build
      // up GeneralPath from selected portions of each line
      int startLine = activeLine > anchorLine ? anchorLine : activeLine;
      int endLine = activeLine > anchorLine ? activeLine : anchorLine;
      int startChar = activeLine > anchorLine ? anchorEnd : activeEnd;
      start = startChar + ((TextLine)textLayout.get(startLine)).
        getStart();
      int endChar;
      for (int line = startLine; line < endLine; line++) {
        layout = ((TextLine)textLayout.get(line)).getLayout();
        endChar = layout.getCharacterCount();
        Shape highlight =
          layout.getBlackBoxBounds(startChar, endChar).getBounds();
//          layout.getLogicalHighlightShape(startChar, endChar);
        trans.setToTranslation(0, (line+1) * lineSpacing);
        PathIterator path = highlight.getPathIterator(trans);
        highlightArea.append(path, false);
        startChar = 0;
      }
      endChar = activeLine > anchorLine ? activeEnd : anchorEnd;
      layout = ((TextLine)textLayout.get(endLine)).getLayout();
      end = endChar + ((TextLine)textLayout.get(endLine)).getStart();
      Shape highlight =
        layout.getBlackBoxBounds(0, endChar).getBounds();
//        layout.getLogicalHighlightShape(0, endChar);
      trans.setToTranslation(0, (endLine+1) * lineSpacing);
      PathIterator path = highlight.getPathIterator(trans);
      highlightArea.append(path, false);
    }
    selectedText = text.substring(start, end);
    offset = start;
  }

  public void paint(Graphics g) {
    super.paint(g);
    TextLayout layout;
    Graphics2D graphics2D = (Graphics2D) g;
    // Only draw the caret if no text has been selected
    boolean haveCaret = (anchorEnd == activeEnd && anchorLine == activeLine);

    if (textLayout == null || text.equals(""))
      return;
    // No caret --> draw yellow highlight over selected text
    // Use a GeneralPath to define the area to be highlighted
    // and then fill() it.
    if (!haveCaret) {
      graphics2D.setColor(HIGHLIGHT_COLOR);
      findHighlightArea();
      graphics2D.fill(highlightArea);
    }

    // Grey out the used text
    graphics2D.setPaint(USED_TEXT_COLOR);
    getSelectedShape();
    graphics2D.fill(selectedShape);

    // Draw TextLayout.
    graphics2D.setColor(TEXT_COLOR);
    for (int line = 0; line < textLayout.size(); line++) {
      Point2D lineStart = computeLayoutOrigin(line);
      layout = ((TextLine)textLayout.get(line)).getLayout();
      layout.draw(graphics2D, 0, (line+1) * lineSpacing);
    }

    // If no text has been selected, haveCaret is true, so we
    // draw the red line that is the caret.
    if (haveCaret) {
      layout = ((TextLine)textLayout.get(lineSelected)).getLayout();
      Shape[] carets = layout.getCaretShapes(anchorEnd);
      Point2D lineStart = computeLayoutOrigin(lineSelected);
      // Docs are wrong about translate(int, int) - it is a *relative*
      // translation, not absolute.
      graphics2D.translate(0, (lineSelected+1) * lineSpacing);
      graphics2D.setColor(STRONG_CARET_COLOR);
      graphics2D.draw(carets[0]);
    }
  }

  /**
   * Finds the line number containing y coordinate clickY.
   * Used in selecting text with the mouse.
   */
  private int getLineSelected(int clickY)
  {
    if (lineSpacing == 0) return 0;
    lineSelected = clickY / lineSpacing;
    if (lineSelected >= textLayout.size()) {
      lineSelected = textLayout.size() - 1;
    }
    if (lineSelected < 0) {
      lineSelected = 0;
    }
    return lineSelected;
  }

  public void doRightArrow()
  {
//    System.out.println("right arrow");
    /*
   int insertionIndex = ...;
   TextHitInfo next = textLayout.getNextRightHit(insertionIndex);
   if (next != null) {
       // translate graphics to origin of layout on screen
       g.translate(loc.getX(), loc.getY());
       Shape[] carets = layout.getCaretShapes(next.getInsertionIndex());
       g.draw(carets[0]);
       if (carets[1] != null) {
           g.draw(carets[1]);
       }
   }*/
   }

  /**
   * Reads text from a file; returns the word count
   */
  public int readText(InputStream chosenInput, int fileLength)
  {
    try {
      char inBuffer[] = new char[fileLength];
      
      InputStreamReader isReader = new InputStreamReader(chosenInput, encoding);
      BufferedReader r = new BufferedReader(isReader);
      int charsRead = r.read(inBuffer, 0, inBuffer.length);
      text = new String(inBuffer);
      text = text.substring(0, charsRead);
      
      inBuffer = text.toCharArray();
      int badChar;
      while ((badChar = textFont.canDisplayUpTo(inBuffer, 0, inBuffer.length)) != -1) 
      {
        inBuffer[badChar] = ' ';
      }
      for (int i = 0; i<inBuffer.length; i++) {
        if (inBuffer[i] > 65000) {
          inBuffer[i] = ' ';
        }
      }
      text = new String(inBuffer);
    } catch (FileNotFoundException e)  {
      System.out.println(e.toString());
    } catch (IOException e)  {
      System.out.println(e.toString());
    }
    return wordCount(text);
  }
  
  public String stripWhiteSpace(String text)
  {
    // Replace miscellaneous whitespace with blanks
    StringTokenizer textToken = new StringTokenizer(text);
    StringBuffer strippedText = new StringBuffer();
    while (textToken.hasMoreTokens()) {
      String token = textToken.nextToken();
      strippedText.append(token + " ");
    }
    String newText = strippedText.toString();
    newText.trim();
    return newText;
  }

  public String stripWeirdChars(String text)
  {
    char[] chars = new char[text.length()];
    text.getChars(0, chars.length, chars, 0);
    for (int i = 0; i<chars.length; i++) {
      if (chars[i] < 32 || chars[i] > 126) {
        chars[i] = 32;
      }
    }
    String newText = new String(chars);
    return newText;
  }

  public void clearSelection()
  {
    anchorEnd = anchorLine = activeEnd = activeLine = 0;
    textSelected = false;
    selectedText = "";
    repaint();
  }

  public void setSelection(int startLine, int startCol, int endLine, int endCol)
  {
    anchorLine = startLine;
    anchorEnd = startCol;
    activeLine = endLine;
    activeEnd = endCol;
  }

  /**
   * Determines if any text has been selected by the mouse
   */
  public boolean isTextSelected()
  { return textSelected; }

  /**
   * Returns text currently selected by the mouse
   */
  public String getSelectedText()
  { return selectedText; }

  /**
   * Returns offset of selectedText within text
   */
  public int getOffset()
  { return offset; }

  /**
   * Returns the entire text
   */
  public String getText()
  { return text; }

  public void setText(String newText)
  { text = newText; }

  public Vector getTextLayout()
  { return textLayout; }

  public GeneralPath getSelectedShape()
  {
    selectedShape = new GeneralPath();
    ListIterator iter = selectedList.listIterator();
    while (iter.hasNext()) {
      Shape shape = (Shape)iter.next();
      selectedShape.append(shape, false);
    }
    return selectedShape;
  }

  // Appends the current highlightArea to selectedShape so
  // it can be greyed out and excluded from future selections.
  public GeneralPath useCurrentText()
  {
    selectedList.add(highlightArea);
    getSelectedShape();
    repaint();
    return highlightArea;
  }

  public void clearAllSelectedText()
  {
    selectedList.clear();
  }

  public boolean removeSelectedText(GeneralPath shape)
  {
    ListIterator iter = selectedList.listIterator();
    while (iter.hasNext()) {
      if (shape == (Shape)iter.next()) {
        selectedList.remove(shape);
        getSelectedShape();
        repaint();
        return true;
      }
    }
    return false;
  }
  
  public LinkedList getSelectedList()
  { return selectedList; }
  
  public void printSelectedList()
  {
    System.out.println("**********");
    for (int i = 0; i < selectedList.size(); i++)
    {
      System.out.println(selectedList.get(i).toString());
    }
  }
  
  /**
   * Assigns shapes in the SelectText window for each vertex in the tree.
   * @param tree The tree containing the vertices to which shapes are to be assigned.
   */
  public void assignShapes(Tree tree)
  {
    Vector trav = tree.getVertexList();
    for (int i = 0; i < trav.size(); i++)
    {
      TreeVertex vertex = (TreeVertex)trav.elementAt(i);
      if (vertex.isVirtual()) continue;
      String propText = (String)vertex.getLabel();
      int offset = vertex.getOffset();
      if (offset >= 0) {
        // Attach vertex to text layout if not a missing premise
        // Need to find line number and char position within that line
        // for start and end of text
        int endOffset = propText.length() + offset;
        Vector textLayout = getTextLayout();
        Enumeration textLines = textLayout.elements();
        int lineNum = 0, anchorLine = -1, anchorEnd = -1,
          activeLine = -1, activeEnd = -1;
        while (textLines.hasMoreElements()) {
          TextLine line = (TextLine)textLines.nextElement();
          if (offset >= line.getStart() && offset <= line.getEnd()) {
            anchorLine = lineNum;
            anchorEnd = offset - line.getStart();
          }
          if (endOffset >= line.getStart() && endOffset <= line.getEnd()) {
            activeLine = lineNum;
            activeEnd = endOffset - line.getStart();
          }
          lineNum++;
        }
        setSelection(anchorLine, anchorEnd, activeLine, activeEnd);
        findHighlightArea();
        GeneralPath shape = useCurrentText();
        clearSelection();
        vertex.setAuxObject(shape);
      }
    }
  }

  /**
   * Extends the selection of text. Initial position is chosen
   * in SelectionMouseListener.
   */
  private class SelectionMouseMotionListener extends MouseMotionAdapter {
    public void mouseDragged(MouseEvent e) {
      if (text.equals("") || text.equals(" ")) return;
      float clickX = (float) e.getX();
      float clickY = (float) e.getY();
      getLineSelected((int) clickY);
      // Uses Java 2D text classes to determine current mouse point in text.
      TextLayout layout = ((TextLine)textLayout.
			   get(lineSelected)).getLayout();
      TextHitInfo position = layout.hitTestChar(clickX, clickY);
      int newActiveEnd = position.getInsertionIndex();
      if (activeEnd != newActiveEnd || activeLine != lineSelected) {
        activeEnd = newActiveEnd;
        activeLine = lineSelected;
        textSelected = true;
        repaint();
      }
    }
  }

  /**
   * Used to detect the start point for a text selection
   */
  private class SelectionMouseListener extends MouseAdapter {
    public void mousePressed(MouseEvent e)
    {
      if (text.equals("") || text.equals(" ")) return;
      float clickX = (float) e.getX();
      float clickY = (float) e.getY();

      getLineSelected((int)clickY);
      // Uses Text classes from Java 2D to fix the selection point.
      TextLayout layout = ((TextLine)textLayout.
			   get(lineSelected)).getLayout();
      TextHitInfo position = layout.hitTestChar(clickX, clickY);
      anchorEnd = position.getInsertionIndex();
      anchorLine = lineSelected;
      activeEnd = anchorEnd;
      activeLine = lineSelected;
      textSelected = false;
      selectedText = "";
      repaint();
    }
  }
}



