import java.sql.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;

public class OwnerSourceTableModel extends AbstractTableModel {

  static  int colNo = 0;
  static final int FULL = colNo++;
  static final int SHORT = colNo++;

  String[] columnNames = { "Full owner name", "TLA"};
  Vector rows = new Vector();
  Araucaria parent;
  JTable owner;
  OwnerDialog dialog;
  boolean fixDuplicateOwner = false;

  public OwnerSourceTableModel(Araucaria p, JTable table, OwnerDialog d)
  {
    parent = p;
    owner = table;   
    dialog = d;
  }

  public void updateTable(Set ownerSet)
  {
    try {
      rows = new Vector();
      
      if (ownerSet != null) {
	   		Iterator ownerIter = ownerSet.iterator();
	   		while (ownerIter.hasNext()) {
	   			Vector ownerItem = (Vector)ownerIter.next();
	   			Vector newRow = new Vector();
	   			newRow.add(ownerItem.elementAt(0)); newRow.add(ownerItem.elementAt(1));
	   			rows.addElement(newRow);
	   		}
	   		sort(rows);
	   	}
     
      fireTableChanged(null); // Tell the listeners a new table has arrived.
      // If the rows vector is empty, there were no records in the record set.
      if (rows.size() == 0)
        return;
      setColumnWidths(); // Have to set column widths *after* table is refreshed.
      //table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
/*
      // Attach selection listener for row selections with the mouse.
      ListSelectionModel rowSM = table.getSelectionModel();
      rowSM.addListSelectionListener(new ListSelectionListener() {
          public void valueChanged(ListSelectionEvent e) {
              //Ignore extra messages.
              if (e.getValueIsAdjusting()) return;

              ListSelectionModel lsm =
                  (ListSelectionModel)e.getSource();
              if (lsm.isSelectionEmpty()) {

              } else {
                  int selectedRow = lsm.getMinSelectionIndex();
              }
          }
      }); */
    }
    catch (Exception ex) {
        System.err.println("In updateTable: " + ex);
    }
  }

  public void setColumnWidths()
  {
  	try {
	    String[] longValues = {"MMMMMMMMMMMMMMMMMMMMM", "MMM"};
	    TableColumn column = null;
	    for (int i = 0; i < getColumnCount(); i++) {
	      column = owner.getColumnModel().getColumn(i);
	      Component comp = owner.getDefaultRenderer(getColumnClass(i)).
	                       getTableCellRendererComponent(
	                           owner, longValues[i],
	                           false, false, 0, i);
	      int cellWidth = comp.getPreferredSize().width;
	      column.setPreferredWidth(cellWidth);
	    }
    } catch (Exception e) {
        System.err.println("In setColumnWidths: " + e);
    }
  }

  public int getColumnCount()
  {
    return columnNames.length;
  }

  public Class getColumnClass(int c)
  {
    if (getValueAt(0, c) != null)
      return getValueAt(0, c).getClass();
    return Object.class;
  }

  public String getColumnName(int column) {
      if (columnNames[column] != null) {
          return columnNames[column];
      } else {
          return "";
      }
  }

  public boolean isCellEditable(int row, int col)
  {
  		return false;
  }

  public Object getValueAt(int aRow, int aColumn)
  {
    Vector row = (Vector)rows.elementAt(aRow);
    return row.elementAt(aColumn);
  }

  /**
   * Called automatically when a cell is edited.
   */
  public void setValueAt(Object value, int aRow, int aColumn)
  {
    
  }
  
  public static void sort(Vector v)
  {
  	if (v.size() <= 1) return;
  	Vector temp;
  	int marker1, marker2;
  	for (marker1 = 1; marker1 < v.size(); marker1++) {
  		Vector elem1 = (Vector)v.elementAt(marker1);
  		Vector elem2 = (Vector)v.elementAt(marker1 - 1);
  		if (((String)elem1.elementAt(0)).compareTo((String)elem2.elementAt(0)) < 0) {
  			temp = elem1;
  			for (marker2 = marker1 - 1; marker2 >= 0; --marker2) {
  				v.set(marker2 + 1, v.elementAt(marker2));
  				if (marker2 == 0 || 
  					((String)((Vector)v.elementAt(marker2 - 1)).elementAt(0)).
  						compareTo((String)temp.elementAt(0)) < 0)
  					break;
  			}
  			v.set(marker2, temp);
  		}
	  }
  }
  
  public void addOwner(String name)
  {
   	Vector newRow = new Vector();
		newRow.add(name);
		newRow.add("");
    String tla = parent.getArgument().addToOwnerList(newRow);
    if (tla != null) {
			newRow.setElementAt(tla, 1);
	  	rows.add(newRow);
	  	sort(rows);
	  	this.fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
	  	setColumnWidths();
	  }
  }
  	

  public int getRowCount()
  {
    return rows.size();
  }
  
  public Vector getSelectedOwners()
  {
  	int[] selectedRows = owner.getSelectedRows();
  	Vector selectedOwners = new Vector();
  	for (int i = 0; i < selectedRows.length; i++) {
  		selectedOwners.add((Vector)rows.elementAt(selectedRows[i]));
	  }
	  return selectedOwners;
  }

}
