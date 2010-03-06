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

import savant.model.FileFormat;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author mfiume
 */
public class SequenceTableModel extends AbstractTableModel {
     public static final int SEQUENCE_INDEX = 0;
     /*
     public static final int ARTIST_INDEX = 1;
     public static final int ALBUM_INDEX = 2;
     public static final int HIDDEN_INDEX = 3;
      */
     protected String[] columnNames = { "Sequence" };
     //protected Vector dataVector;
     protected List<Object> data;

     public SequenceTableModel(FileFormat kind, List<Object> data) {
         //this.columnNames = { "Sequence","" };
         //dataVector = new Vector();
         this.data = data;
     }

     public String getColumnName(int column) {
         return columnNames[column];
     }

     public boolean isCellEditable(int row, int column) {
         //if (column == HIDDEN_INDEX) return false;
         //else return true;
         return false;
     }

     public Class getColumnClass(int column) {
         switch (column) {
             case SEQUENCE_INDEX:
             /*
             case ARTIST_INDEX:
             case ALBUM_INDEX:
              */
                return String.class;
             default:
                return Object.class;
         }
     }

     public Object getValueAt(int row, int column) {
         Object datum = data.get(row);
         switch (column) {
             case SEQUENCE_INDEX:
                return (String) datum;
             /*
             case ARTIST_INDEX:
                return record.getArtist();
             case ALBUM_INDEX:
                return record.getAlbum();
              */
             default:
                return new Object();
         }
     }

     public void setValueAt(Object value, int row, int column) {
         Object datum = data.get(row);
         switch (column) {
             case SEQUENCE_INDEX:
                datum = (String) value;
                break;
               /*
             case ARTIST_INDEX:
                record.setArtist((String)value);
                break;
             case ALBUM_INDEX:
                record.setAlbum((String)value);
                break;
                */
             default:
                System.out.println("invalid index");
         }
         fireTableCellUpdated(row, column);
     }

     public int getRowCount() {
         return data.size();
     }

     public int getColumnCount() {
         return columnNames.length;
     }

     public boolean hasEmptyRow() {
         if (data.size() == 0) return false;
         Object datum = data.get(data.size() - 1);
         if (((String)datum).trim().equals(""))
         {
            return true;
         }
         else return false;
     }

     public void addEmptyRow() {
         data.add(new String());
         fireTableRowsInserted(
            data.size() - 1,
            data.size() - 1);
     }

    void setData(List<Object> dataInRange) {
        this.data = dataInRange;
    }
 }
