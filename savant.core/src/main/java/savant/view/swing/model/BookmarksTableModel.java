/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.view.swing.model;

import savant.util.Bookmark;
import savant.util.Range;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author mfiume
 */
public class BookmarksTableModel extends AbstractTableModel {

     private Class[] columnClasses = { String.class, Integer.class, Integer.class, String.class };
     private String[] columnNames = { "Reference", "From", "To", "Annotation" };

     //protected Vector dataVector;
     protected List<Bookmark> data;

     public BookmarksTableModel() {
         this(new ArrayList<Bookmark>());
     }

    public BookmarksTableModel(List<Bookmark> data) {
         //this.columnNames = { "Sequence","" };
         //dataVector = new Vector();
         this.data = data;
     }


     public String getColumnName(int column) {
         return columnNames[column];
     }

     public boolean isCellEditable(int row, int column) {
         return true;
         //if (column == 3) { return true; }
         //return false;
     }

    @Override
    public Class getColumnClass(int column) {
        return this.columnClasses[column];
     }

     public Object getValueAt(int row, int column) {

         Bookmark fave = data.get(row);

         switch(column) {
             case 0:
                 return fave.getReference();
             case 1:
                 return fave.getRange().getFrom();
             case 2:
                 return fave.getRange().getTo();
             case 3:
                 return fave.getAnnotation();
             default:
                 return "";
         }
     }

     public void setValueAt(Object value, int row, int column) {

         Bookmark fave = data.get(row);

         switch (column) {
             case 0:
                 fave.setReference((String)value);
                 break;
             case 1:
                 Range r1 = (Range) fave.getRange();
                 Range newr1 = new Range((Integer) value, r1.getTo());
                 fave.setRange(newr1);
                 break;
             case 2:
                 Range r2 = (Range) fave.getRange();
                 Range newr2 = new Range(r2.getFrom(), (Integer) value);
                 fave.setRange(newr2);
                 break;
             case 3:
                 fave.setAnnotation((String) value);
                 break;
             default:
                 break;
         }

         fireTableCellUpdated(row, column);
     }

     public int getRowCount() {
         if (!isData()) { return 0; }
         return data.size();
     }

     public int getColumnCount() {
         return columnNames.length;
     }

     public boolean hasEmptyRow() {
         if (data.size() == 0) return false;
         Bookmark fave = data.get(data.size() - 1);
         if (fave.getRange().getFrom() == 0 && fave.getRange().getTo() == 0)
         {
            return true;
         }
         else return false;
     }

     public void addEmptyRow() {
         data.add(new Bookmark("",new Range(0,0)));
         fireTableRowsInserted(
            data.size() - 1,
            data.size() - 1);
     }

    public boolean isData() { return this.data != null; }

    public void setData(List<Bookmark> favorites) {
        this.data = favorites;
        this.fireTableChanged(null);
    }

    public List<Bookmark> getData() { return this.data; }

    public void clearData(){
        this.data.clear();
    }
}
