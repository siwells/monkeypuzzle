import java.util.Stack;

public class UndoStack extends Stack
{
  public int undoPointer; // Points to vector element that is next to undo
  
  public UndoStack()
  {
    super();
    undoPointer = -1;
  }
  
  /**
   * When a new item is pushed onto the undo stack, it means that
   * the user has done something new to the diagram, so we should
   * lose all items that have been undone at the point the new item
   * is added - hence the call to popToPointer().
   */
  public Object push(Object item)
  {
    popToPointer();
    undoPointer++;
    Araucaria.diagramModified = true;
    return super.push(item);
  }
  
  /**
   * Remove all elements above the undoPointer
   */
  public void popToPointer()
  {
    while (this.size() > undoPointer + 1) {
      super.pop();
    }
  }
}
