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

package savant.data;

import savant.model.*;

import javax.swing.table.AbstractTableModel;
import java.util.List;


/**
 *
 * @author mfiume
 */
public class DataTableModel extends AbstractTableModel {

     private FileFormat dataType;
     private String[] columnNames;

     //public static final int SEQUENCE_INDEX = 0;
     /*
     public static final int ARTIST_INDEX = 1;
     public static final int ALBUM_INDEX = 2;
     public static final int HIDDEN_INDEX = 3;
      */
     protected Class[] sequenceColumnClasses = { String.class };
     protected String[] sequenceColumnNames = { "Sequence" };
     protected Class[] pointColumnClasses = { Integer.class, String.class };
     protected String[] pointColumnNames = { "Position", "Description" };
     protected Class[] intervalColumnClasses = { Integer.class, Integer.class, String.class };
     protected String[] intervalColumnNames = { "From", "To", "Description" };

     protected Class[] bamColumnClasses = { String.class, String.class, Integer.class, Boolean.class, Integer.class, Boolean.class, Integer.class, String.class, Integer.class, Boolean.class, Integer.class};
     protected String[] bamColumnNames = { "Read Name", "Sequence", "Length", "First of Pair", "Position", "Strand +", "Mapping Quality", "CIGAR", "Mate Position", "Strand +", "Inferred Insert Size" };

    protected Class[] bedColumnClasses = { String.class,  Integer.class, Integer.class, Integer.class};
    protected String[] bedColumnNames = {"Name", "Start", "End", "Block Count"};

    protected Class[] continuousColumnClasses = { Integer.class, Double.class };
    protected String[] continuousColumnNames = { "Position", "Value" };


     //protected Vector dataVector;
     protected List<Object> data;

    public DataTableModel(FileFormat dataType, List<Object> data) {
         //this.columnNames = { "Sequence","" };
         //dataVector = new Vector();
         this.data = data;
         this.setDataType(dataType);

         switch(this.dataType) {
             case SEQUENCE_FASTA:
                 columnNames = sequenceColumnNames;
                 break;
             case POINT:
                 columnNames = pointColumnNames;
                 break;
             case CONTINUOUS_GENERIC:
                 columnNames = continuousColumnNames;
                 break;
             case INTERVAL_GENERIC:
                 columnNames = intervalColumnNames;
                 break;
             case INTERVAL_BAM:
                 columnNames = bamColumnNames;
                 break;
             case INTERVAL_BED:
                 columnNames = bedColumnNames;
                 break;
             default:
                 assert false;
         }
     }


     public String getColumnName(int column) {
         try {
             switch(dataType) {
                 case SEQUENCE_FASTA:
                     return sequenceColumnNames[column];
                 case POINT:
                     return pointColumnNames[column];
                 case CONTINUOUS_GENERIC:
                     return continuousColumnNames[column];
                 case INTERVAL_GENERIC:
                     return intervalColumnNames[column];
                 case INTERVAL_BAM:
                     return bamColumnNames[column];
                 case INTERVAL_BED:
                     return bedColumnNames[column];
                 default:
                     return "Unknown";
             }
         } catch(Exception e) { return "??"; }
     }

     public boolean isCellEditable(int row, int column) {
         return false;
     }

    @Override
    public Class getColumnClass(int column) {
        try {
             switch (dataType) {
                 case SEQUENCE_FASTA:
                     return sequenceColumnClasses[column];
                 case POINT:
                     return pointColumnClasses[column];
                 case CONTINUOUS_GENERIC:
                     return continuousColumnClasses[column];
                 case INTERVAL_GENERIC:
                     return intervalColumnClasses[column];
                 case INTERVAL_BAM:
                     return bamColumnClasses[column];
                 case INTERVAL_BED:
                     return bedColumnClasses[column];
                 default:
                     return String.class;
             }
        } catch(Exception e) { return String.class; }
     }

     public Object getValueAt(int row, int column) {
         Object datum = data.get(row);
         switch (dataType) {
             case SEQUENCE_FASTA:
                 return datum;
             case POINT:
                 switch (column) {
                     case 0:
                        return ((GenericPointRecord) datum).getPoint().getPosition();
                     case 1:
                         return ((GenericPointRecord) datum).getDescription();
                 }
             case CONTINUOUS_GENERIC:
                 switch (column) {
                     case 0:
                         return ((ContinuousRecord) datum).getPosition();
                     case 1:
                         return ((ContinuousRecord) datum).getValue().getValue();
                 }
             case INTERVAL_GENERIC:
                 switch (column) {
                     case 0:
                        return ((GenericIntervalRecord) datum).getInterval().getStart();
                     case 1:
                         return ((GenericIntervalRecord) datum).getInterval().getEnd();
                     case 2:
                         return ((GenericIntervalRecord) datum).getDescription();
                 }
             case INTERVAL_BAM:
                 switch (column) {
                     case 0:
                         return ((BAMIntervalRecord) datum).getSamRecord().getReadName();
                     case 1:
                         return ((BAMIntervalRecord) datum).getSamRecord().getReadString();
                     case 2:
                         return ((BAMIntervalRecord) datum).getSamRecord().getReadLength();
                     case 3:
                         return ((BAMIntervalRecord) datum).getSamRecord().getFirstOfPairFlag();
                     case 4:
                         return ((BAMIntervalRecord) datum).getSamRecord().getAlignmentStart();
                     case 5:
                         return !((BAMIntervalRecord) datum).getSamRecord().getReadNegativeStrandFlag();
                     case 6:
                         return ((BAMIntervalRecord) datum).getSamRecord().getMappingQuality();
                     case 7:
                         return ((BAMIntervalRecord) datum).getSamRecord().getCigarString();
                     case 8:
                         boolean mated = ((BAMIntervalRecord) datum).getSamRecord().getReadPairedFlag();
                         if (mated) {
                            return ((BAMIntervalRecord) datum).getSamRecord().getMateAlignmentStart();
                         }
                         else {
                             return -1;
                         }
                     case 9:
                         return !((BAMIntervalRecord) datum).getSamRecord().getMateNegativeStrandFlag();
                     case 10:
                         return ((BAMIntervalRecord) datum).getSamRecord().getInferredInsertSize();
                 }
             case INTERVAL_BED:
                 switch (column) {
                     case 0:
                         return ((BEDIntervalRecord) datum).getName();
                     case 1:
                         return ((BEDIntervalRecord) datum).getInterval().getStart();
                     case 2:
                         return ((BEDIntervalRecord) datum).getInterval().getEnd();
                     case 3:
                         return ((BEDIntervalRecord) datum).getBlocks().size();
                 }
             default:
                 return "?";
         }
     }

     public void setValueAt(Object value, int row, int column) {

         Object datum = data.get(row);
         switch (dataType) {
             case SEQUENCE_FASTA:
                 datum = value;
                 break;
             case POINT:
                 switch (column) {
                     case 0:
                        ((GenericPointRecord) datum).getPoint().setPosition(Integer.parseInt((String) value));
                         break;
                     case 1:
                         ((GenericPointRecord) datum).setDescription((String) value);
                         break;
                 }
             case CONTINUOUS_GENERIC:
                 switch (column) {
                     case 0:
                         ((ContinuousRecord) datum).setPosition(Integer.parseInt((String) value));
                         break;
                     case 1:
                         ((ContinuousRecord) datum).getValue().setValue(Float.parseFloat((String) value));
                         break;
                 }
             case INTERVAL_GENERIC:
                 switch (column) {
                     case 0:
                        ((GenericIntervalRecord) datum).getInterval().setStart(Integer.parseInt((String) value));
                         break;
                     case 1:
                         ((GenericIntervalRecord) datum).getInterval().setEnd(Integer.parseInt((String) value));
                         break;
                     case 2:
                         ((GenericPointRecord) datum).setDescription((String) value);
                         break;
                 }
             case INTERVAL_BAM:
                 switch (column) {
                     case 0:
                         ((BAMIntervalRecord) datum).getSamRecord().setReadName((String) value);
                         break;
                     case 1:
                         ((BAMIntervalRecord) datum).getSamRecord().setReadString((String) value);
                         break;
                     case 2:
                         break;
                     case 3:
                         ((BAMIntervalRecord) datum).getSamRecord().setDuplicateReadFlag((Boolean) value);
                     case 4:
                         ((BAMIntervalRecord) datum).getSamRecord().setAlignmentStart(Integer.parseInt((String) value));
                         break;
                     case 5:
                         ((BAMIntervalRecord) datum).getSamRecord().setReadNegativeStrandFlag(!((Boolean) value));
                         break;
                     case 6:
                         ((BAMIntervalRecord) datum).getSamRecord().setMappingQuality(Integer.parseInt((String) value));
                         break;
                     case 7:
                         ((BAMIntervalRecord) datum).getSamRecord().setCigarString((String) value);
                         break;
                     case 8:
                         ((BAMIntervalRecord) datum).getSamRecord().setMateAlignmentStart(Integer.parseInt((String) value));
                         break;
                     case 9:
                         ((BAMIntervalRecord) datum).getSamRecord().setMateNegativeStrandFlag(!((Boolean) value));
                         break;
                     case 10:
                         ((BAMIntervalRecord) datum).getSamRecord().setInferredInsertSize(Integer.parseInt((String) value));
                         break;
                 }
             case INTERVAL_BED:
                 switch(column) {
                     case 0:
                         ((BEDIntervalRecord) datum).setName((String) value);
                         break;
                     case 1:
                         ((BEDIntervalRecord) datum).getInterval().setStart(Integer.parseInt((String) value));
                         break;
                     case 2:
                         ((BEDIntervalRecord) datum).getInterval().setEnd(Integer.parseInt((String) value));
                         break;
                     case 3:
                         break;
                 }
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

    public void setData(List<Object> dataInRange) {
        this.data = dataInRange;
    }

    public boolean isData() { return this.data != null; }

    public void setDataType(FileFormat k) {
        this.dataType = k;
        this.fireTableChanged(null);
    }
}
