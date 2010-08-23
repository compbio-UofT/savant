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

import net.sf.samtools.SAMRecord;
import savant.file.FileFormat;
import javax.swing.table.AbstractTableModel;
import java.util.List;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.BEDIntervalRecord;
import savant.data.types.GenericContinuousRecord;
import savant.data.types.GenericIntervalRecord;
import savant.data.types.GenericPointRecord;


/**
 *
 * @author mfiume
 */
public class DataTableModel extends AbstractTableModel {

     private FileFormat dataType;
     private String[] columnNames;

     private static boolean dontAllowMoreThanMaxNumRows = true;
     private static int maxNumRows = 500;

     //public static final int SEQUENCE_INDEX = 0;
     /*
     public static final int ARTIST_INDEX = 1;
     public static final int ALBUM_INDEX = 2;
     public static final int HIDDEN_INDEX = 3;
      */
    protected Class[] sequenceColumnClasses = { String.class };
    protected String[] sequenceColumnNames = { "Sequence" };

    protected Class[] pointColumnClasses = { String.class, Integer.class, String.class };
    protected String[] pointColumnNames = { "Reference", "Position", "Description" };

    protected Class[] intervalColumnClasses = { String.class, Integer.class, Integer.class, String.class };
    protected String[] intervalColumnNames = { "Reference", "From", "To", "Description" };

    protected Class[] bamColumnClasses = { String.class, String.class, Integer.class, Boolean.class, Integer.class, Boolean.class, Integer.class, String.class, Integer.class, Boolean.class, Integer.class};
    protected String[] bamColumnNames = { "Read Name", "Sequence", "Length", "First of Pair", "Position", "Strand +", "Mapping Quality", "CIGAR", "Mate Position", "Strand +", "Inferred Insert Size" };

    protected Class[] bedColumnClasses = { String.class, Integer.class, Integer.class, String.class, Integer.class};
    protected String[] bedColumnNames = {"Reference", "Start", "End", "Name", "Block Count"};

    protected Class[] continuousColumnClasses = { String.class, Integer.class, Double.class };
    protected String[] continuousColumnNames = { "Reference", "Position", "Value" };


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
             case POINT_GENERIC:
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
                 case POINT_GENERIC:
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
                 case POINT_GENERIC:
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
             case POINT_GENERIC:
                 switch (column) {
                     case 0:
                         return ((GenericPointRecord) datum).getReference();
                     case 1:
                        return ((GenericPointRecord) datum).getPoint().getPosition();
                     case 2:
                         return ((GenericPointRecord) datum).getDescription();
                 }
             case CONTINUOUS_GENERIC:
                 switch (column) {
                     case 0:
                         return ((GenericContinuousRecord) datum).getReference();
                     case 1:
                         return ((GenericContinuousRecord) datum).getPosition();
                     case 2:
                         return ((GenericContinuousRecord) datum).getValue().getValue();
                 }
             case INTERVAL_GENERIC:
                 switch (column) {
                     case 0:
                         return ((GenericIntervalRecord) datum).getReference();
                     case 1:
                        return ((GenericIntervalRecord) datum).getInterval().getStart();
                     case 2:
                         return ((GenericIntervalRecord) datum).getInterval().getEnd();
                     case 3:
                         return ((GenericIntervalRecord) datum).getDescription();
                 }
             case INTERVAL_BAM:
                 SAMRecord samRecord = ((BAMIntervalRecord) datum).getSamRecord();
                 boolean mated = samRecord.getReadPairedFlag();
                 switch (column) {
                     case 0:
                         return samRecord.getReadName();
                     case 1:
                         return samRecord.getReadString();
                     case 2:
                         return samRecord.getReadLength();
                     case 3:
                         if (mated) {
                            return samRecord.getFirstOfPairFlag();
                         }
                         else {
                             return false;
                         }
                     case 4:
                         return samRecord.getAlignmentStart();
                     case 5:
                         return !samRecord.getReadNegativeStrandFlag();
                     case 6:
                         return samRecord.getMappingQuality();
                     case 7:
                         return samRecord.getCigarString();
                     case 8:
                         return mated ? samRecord.getMateAlignmentStart() : -1;
                     case 9:
                         return mated ? !samRecord.getMateNegativeStrandFlag() : false;
                     case 10:
                         return mated ? samRecord.getInferredInsertSize() : 0;
                 }
             case INTERVAL_BED:
                 switch (column) {
                     case 0:
                         return ((BEDIntervalRecord) datum).getChrom();
                     case 1:
                         return ((BEDIntervalRecord) datum).getInterval().getStart();
                     case 2:
                         return ((BEDIntervalRecord) datum).getInterval().getEnd();
                     case 3:
                         return ((BEDIntervalRecord) datum).getName();
                     case 4:
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
             case POINT_GENERIC:
                 switch (column) {
                     case 0:
//                        ((GenericPointRecord) datum).getPoint().setPosition(Integer.parseInt((String) value));
                         break;
                     case 1:
//                         ((GenericPointRecord) datum).setDescription((String) value);
                         break;
                 }
             case CONTINUOUS_GENERIC:
                 switch (column) {
                     case 0:
//                         ((ContinuousRecord) datum).setReference((String) value);
                         break;
                     case 1:
//                         ((ContinuousRecord) datum).setPosition(Integer.parseInt((String) value));
                         break;
                     case 2:
//                         ((ContinuousRecord) datum).getValue().setValue(Float.parseFloat((String) value));
                         break;
                 }
             case INTERVAL_GENERIC:
                 switch (column) {
                     case 0:
//                         ((GenericIntervalRecord) datum).setReference((String) value);
                         break;
                     case 1:
//                        ((GenericIntervalRecord) datum).getInterval().setStart(Integer.parseInt((String) value));
                         break;
                     case 2:
//                         ((GenericIntervalRecord) datum).getInterval().setEnd(Integer.parseInt((String) value));
                         break;
                     case 3:
//                         ((GenericIntervalRecord) datum).setDescription((String) value);
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
//                         ((BEDIntervalRecord) datum).setChrom((String) value);
                         break;
                     case 1:
//                         ((BEDIntervalRecord) datum).getInterval().setStart(Integer.parseInt((String) value));
                         break;
                     case 2:
//                         ((BEDIntervalRecord) datum).getInterval().setEnd(Integer.parseInt((String) value));
                         break;
                     case 3:
//                         ((BEDIntervalRecord) datum).setName((String) value);
                         break;
                     case 4:
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
        if (dataInRange == null) { 
            this.data = null;
            return;
        }
        if (dontAllowMoreThanMaxNumRows && dataInRange.size() > maxNumRows) {
            this.data = dataInRange.subList(0, maxNumRows);
        } else {
            this.data = dataInRange;
        }
    }

    public boolean isData() { return this.data != null; }

    public void setDataType(FileFormat k) {
        this.dataType = k;
        this.fireTableChanged(null);
    }

    public void setMaxNumRows(int maxNumRows) {
        this.maxNumRows = maxNumRows;
    }

    public int getMaxNumRows() {
        return this.maxNumRows;
    }

    public void setIsNumRowsLimited(boolean isNumRowsLimited) {
        dontAllowMoreThanMaxNumRows = isNumRowsLimited;
    }

    public boolean getIsNumRowsLimited() {
        return dontAllowMoreThanMaxNumRows;
    }
}
