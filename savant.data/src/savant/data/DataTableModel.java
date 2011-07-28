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

import savant.api.adapter.DataSourceAdapter;
import savant.data.types.*;
import savant.file.DataFormat;


/**
 *
 * @author mfiume
 */
public class DataTableModel extends AbstractTableModel {
    private static final Class[] SEQUENCE_COLUMN_CLASSES = { String.class };
    private static final Class[] POINT_COLUMN_CLASSES = { String.class, Integer.class, String.class };
    private static final Class[] INTERVAL_COLUMN_CLASSES = { String.class, Integer.class, Integer.class, String.class };
    private static final Class[] BAM_COLUMN_CLASSES = { String.class, String.class, Integer.class, Boolean.class, Integer.class, Boolean.class, Integer.class, String.class, String.class, Integer.class, Boolean.class, Integer.class};
    private static final Class[] CONTINUOUS_COLUMN_CLASSES = { String.class, Integer.class, Double.class };

    private DataFormat dataType;
    private String[] columnNames;
    private Class[] columnClasses;

    private static boolean dontAllowMoreThanMaxRows = true;
    private static int maxRows = 500;

    protected List<Record> data;

    /** For tabix, some of the columns may not be meaningful for end-users, so have a little lookup table. */
    private int[] remappedColumns;

    public DataTableModel(DataSourceAdapter dataSource) {
        setDataType(dataSource.getDataFormat());

        columnNames = dataSource.getColumnNames();
        switch (dataType) {
            case SEQUENCE_FASTA:
                columnClasses = SEQUENCE_COLUMN_CLASSES;
                break;
            case POINT_GENERIC:
                columnClasses = POINT_COLUMN_CLASSES;
                break;
            case CONTINUOUS_GENERIC:
                columnClasses = CONTINUOUS_COLUMN_CLASSES;
                break;
            case INTERVAL_GENERIC:
                columnClasses = INTERVAL_COLUMN_CLASSES;
                break;
            case INTERVAL_BAM:
                columnClasses = BAM_COLUMN_CLASSES;
                break;
            case INTERVAL_RICH:
                // Special treatment for Tabix data, which may have some suppressed fields indicated by nulls in the column list.
                remappedColumns = new int[columnNames.length];
                List<String> usefulNames = new ArrayList<String>(columnNames.length);
                for (int i = 0; i < columnNames.length; i++) {
                    if (columnNames[i] != null) {
                        remappedColumns[usefulNames.size()] = i;
                        usefulNames.add(columnNames[i]);
                    }
                }
                columnNames = usefulNames.toArray(new String[0]);
                break;
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
        if (remappedColumns != null) {
            // All Tabix data is implicitly string.
            return String.class;
        } else {
            return columnClasses[column];
        }
     }

    @Override
    public Object getValueAt(int row, int column) {
        Record datum = data.get(row);
        if (remappedColumns != null) {
            return ((TabixIntervalRecord)datum).getValues()[remappedColumns[column]];
        } else {
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
                case INTERVAL_RICH:
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
                default:
                    return "?";
            }
        }
    }

    /**
     * Count of records stored in this model.
     */
    @Override
    public int getRowCount() {
        if (data != null) {
            if (dontAllowMoreThanMaxRows && data.size() > maxRows) {
                return maxRows;
            }
            return data.size();
        }
        return 0;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    public void setData(List<Record> dataInRange) {
        if (dataInRange == null) { 
            data = null;
        } else {
            if (dataType == DataFormat.CONTINUOUS_GENERIC) {
                // Continuous tracks now use NaNs for missing values.  Filter them out.
                data = new ArrayList<Record>();
                for (Record r: dataInRange) {
                    if (!Float.isNaN(((GenericContinuousRecord)r).getValue())) {
                        data.add(r);
                        if (dontAllowMoreThanMaxRows && data.size() >= maxRows) {
                            break;
                        }
                    }
                }
            } else {
                if (dontAllowMoreThanMaxRows && dataInRange.size() > maxRows) {
                    //data = dataInRange.subList(0, maxRows);
                    data = dataInRange;
                } else {
                    data = dataInRange;
                }
            }
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
