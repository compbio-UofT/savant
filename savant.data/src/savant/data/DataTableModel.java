/*
 *    Copyright 2009-2011 University of Toronto
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

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import net.sf.samtools.SAMRecord;

import savant.data.sources.DataSource;
import savant.data.sources.TabixDataSource;
import savant.data.types.*;
import savant.file.DataFormat;


/**
 *
 * @author mfiume
 */
public class DataTableModel extends AbstractTableModel {
    private static final String[] SEQUENCE_COLUMN_NAMES = { "Sequence" };
    private static final Class[] SEQUENCE_COLUMN_CLASSES = { String.class };
    private static final String[] POINT_COLUMN_NAMES = { "Reference", "Position", "Description" };
    private static final Class[] POINT_COLUMN_CLASSES = { String.class, Integer.class, String.class };
    private static final String[] INTERVAL_COLUMN_NAMES = { "Reference", "From", "To", "Description" };
    private static final Class[] INTERVAL_COLUMN_CLASSES = { String.class, Integer.class, Integer.class, String.class };
    private static final String[] BAM_COLUMN_NAMES = { "Read Name", "Sequence", "Length", "First of Pair", "Position", "Strand +", "Mapping Quality", "Base Qualities", "CIGAR", "Mate Position", "Strand +", "Inferred Insert Size" };
    private static final Class[] BAM_COLUMN_CLASSES = { String.class, String.class, Integer.class, Boolean.class, Integer.class, Boolean.class, Integer.class, String.class, String.class, Integer.class, Boolean.class, Integer.class};
    private static final String[] BED_COLUMN_NAMES = {"Reference", "Start", "End", "Name", "Block Count"};
    private static final Class[] BED_COLUMN_CLASSES = { String.class, Integer.class, Integer.class, String.class, Integer.class};
    private static final String[] CONTINUOUS_COLUMN_NAMES = { "Reference", "Position", "Value" };
    private static final Class[] CONTINUOUS_COLUMN_CLASSES = { String.class, Integer.class, Double.class };

    private DataFormat dataType;
    private String[] columnNames;
    private Class[] columnClasses;

    private static boolean dontAllowMoreThanMaxRows = true;
    private static int maxRows = 500;


    protected List<Record> data;

    /** For tabix, some of the columns may not be meaningful for end-users, so have a little lookup table. */
    private int[] tabixColumns;

    public DataTableModel(DataSource dataSource, List<Record> data) {
         this.data = data;
         setDataType(dataSource.getDataFormat());

         switch (dataType) {
             case SEQUENCE_FASTA:
                 columnNames = SEQUENCE_COLUMN_NAMES;
                 columnClasses = SEQUENCE_COLUMN_CLASSES;
                 break;
             case POINT_GENERIC:
                 columnNames = POINT_COLUMN_NAMES;
                 columnClasses = POINT_COLUMN_CLASSES;
                 break;
             case CONTINUOUS_GENERIC:
                 columnNames = CONTINUOUS_COLUMN_NAMES;
                 columnClasses = CONTINUOUS_COLUMN_CLASSES;
                 break;
             case INTERVAL_GENERIC:
                 columnNames = INTERVAL_COLUMN_NAMES;
                 columnClasses = INTERVAL_COLUMN_CLASSES;
                 break;
             case INTERVAL_BAM:
                 columnNames = BAM_COLUMN_NAMES;
                 columnClasses = BAM_COLUMN_CLASSES;
                 break;
             case INTERVAL_BED:
                 columnNames = BED_COLUMN_NAMES;
                 columnClasses = BED_COLUMN_CLASSES;
                 break;
             case TABIX:
                 String[] allColumnNames = ((TabixDataSource)dataSource).getColumnNames();
                 tabixColumns = new int[allColumnNames.length];
                 List<String> usefulNames = new ArrayList<String>(allColumnNames.length);
                 for (int i = 0; i < allColumnNames.length; i++) {
                     if (allColumnNames[i] != null) {
                         tabixColumns[usefulNames.size()] = i;
                         usefulNames.add(allColumnNames[i]);
                     }
                 }
                 columnNames = usefulNames.toArray(new String[0]);
                 break;
             default:
                 assert false;
         }
     }


    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Class getColumnClass(int column) {
        if (dataType == DataFormat.TABIX) {
            return String.class;
        } else {
            return columnClasses[column];
        }
     }

    @Override
    public Object getValueAt(int row, int column) {
        Record datum = data.get(row);
        switch (dataType) {
            case SEQUENCE_FASTA:
                return new String(((SequenceRecord)datum).getSequence());
            case POINT_GENERIC:
                switch (column) {
                    case 0:
                        return datum.getReference();
                    case 1:
                        return ((GenericPointRecord)datum).getPoint().getPosition();
                    case 2:
                        return ((GenericPointRecord)datum).getDescription();
                }
            case CONTINUOUS_GENERIC:
                switch (column) {
                    case 0:
                        return datum.getReference();
                    case 1:
                        return ((GenericContinuousRecord)datum).getPosition();
                    case 2:
                        return ((GenericContinuousRecord)datum).getValue();
                }
            case INTERVAL_GENERIC:
                switch (column) {
                    case 0:
                        return datum.getReference();
                    case 1:
                        return ((IntervalRecord)datum).getInterval().getStart();
                    case 2:
                        return ((IntervalRecord)datum).getInterval().getEnd();
                    case 3:
                        return ((IntervalRecord)datum).getName();
                }
            case INTERVAL_BAM:
                SAMRecord samRecord = ((BAMIntervalRecord)datum).getSamRecord();
                boolean mated = samRecord.getReadPairedFlag();
                switch (column) {
                    case 0:
                        return samRecord.getReadName();
                    case 1:
                        return samRecord.getReadString();
                    case 2:
                        return samRecord.getReadLength();
                    case 3:
                        return mated ? samRecord.getFirstOfPairFlag() : false;
                    case 4:
                        return samRecord.getAlignmentStart();
                    case 5:
                        return !samRecord.getReadNegativeStrandFlag();
                    case 6:
                        return samRecord.getMappingQuality();
                    case 7:
                        return samRecord.getBaseQualityString();
                    case 8:
                        return samRecord.getCigarString();
                    case 9:
                        return mated ? samRecord.getMateAlignmentStart() : -1;
                    case 10:
                        return mated ? !samRecord.getMateNegativeStrandFlag() : false;
                    case 11:
                        return mated ? samRecord.getInferredInsertSize() : 0;
                }
            case INTERVAL_BED:
                switch (column) {
                    case 0:
                        return ((BEDIntervalRecord)datum).getReference();
                    case 1:
                        return ((BEDIntervalRecord)datum).getInterval().getStart();
                    case 2:
                        return ((BEDIntervalRecord)datum).getInterval().getEnd();
                    case 3:
                        return ((BEDIntervalRecord)datum).getName();
                    case 4:
                        List<Block> blocks = ((BEDIntervalRecord)datum).getBlocks();
                        return blocks != null ? blocks.size() : 0;
                }
            case TABIX:
                return ((TabixIntervalRecord)datum).getValues()[tabixColumns[column]];
            default:
                return "?";
        }
    }

    @Override
    public int getRowCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    public void setData(List<Record> dataInRange) {
        if (dataInRange == null) { 
            data = null;
            return;
        }
        if (dontAllowMoreThanMaxRows && dataInRange.size() > maxRows) {
            data = dataInRange.subList(0, maxRows);
        } else {
            data = dataInRange;
        }
    }

    public final void setDataType(DataFormat k) {
        dataType = k;
        fireTableChanged(null);
    }

    public void setMaxRows(int maxNumRows) {
        maxRows = maxNumRows;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setIsNumRowsLimited(boolean isNumRowsLimited) {
        dontAllowMoreThanMaxRows = isNumRowsLimited;
    }

    public boolean getIsNumRowsLimited() {
        return dontAllowMoreThanMaxRows;
    }
}
