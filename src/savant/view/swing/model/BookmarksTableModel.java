/*
 *    Copyright 2009-2010 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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
                 Range r1 = fave.getRange();
                 Range newr1 = new Range((Integer) value - 1, r1.getTo());
                 fave.setRange(newr1);
                 break;
             case 2:
                 Range r2 = fave.getRange();
                 Range newr2 = new Range(r2.getFrom(), (Integer) value - 1);
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
