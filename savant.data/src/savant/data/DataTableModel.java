/*
 *    Copyright 2009-2012 University of Toronto
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import savant.api.adapter.DataSourceAdapter;
import savant.api.data.*;
import savant.api.util.NavigationUtils;
import savant.data.sources.BAMDataSource;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.TabixIntervalRecord;


/**
 *
 * @author mfiume
 */
public class DataTableModel extends AbstractTableModel {
    private static final Class[] SEQUENCE_COLUMN_CLASSES = { String.class };
    private static final Class[] POINT_COLUMN_CLASSES = { String.class, Integer.class, String.class };
    private static final Class[] INTERVAL_COLUMN_CLASSES = { String.class, Integer.class, Integer.class, String.class };
    private static final Class[] ALIGNMENT_COLUMN_CLASSES = { String.class, String.class, Integer.class, Boolean.class, Integer.class, Boolean.class, Integer.class, String.class, String.class, Integer.class, Boolean.class, Integer.class};
    private static final Class[] CONTINUOUS_COLUMN_CLASSES = { String.class, Integer.class, Double.class };
    private static final Class[] VARIANT_COLUMN_CLASSES = { String.class, String.class, Integer.class, String.class, String.class };

    private final DataSourceAdapter dataSource;
    private String[] columnNames;
    private Class[] columnClasses;

    private int maxRows = 500;

    protected List<Record> data;

    /** For tabix, some of the columns may not be meaningful for end-users, so have a little lookup table. */
    private int[] remappedColumns;

    /** Destination for export of non-BAM tracks. */
    private PrintWriter exportWriter;

    /** Destination for export of BAM tracks. */
    private SAMFileWriter samWriter;

    public DataTableModel(DataSourceAdapter ds) {
        dataSource = ds;

        columnNames = ds.getColumnNames();
        switch (ds.getDataFormat()) {
            case SEQUENCE:
                columnClasses = SEQUENCE_COLUMN_CLASSES;
                break;
            case POINT:
                columnClasses = POINT_COLUMN_CLASSES;
                break;
            case CONTINUOUS:
                columnClasses = CONTINUOUS_COLUMN_CLASSES;
                break;
            case GENERIC_INTERVAL:
                columnClasses = INTERVAL_COLUMN_CLASSES;
                break;
            case ALIGNMENT:
                columnClasses = ALIGNMENT_COLUMN_CLASSES;
                break;
            case VARIANT:
                columnClasses = VARIANT_COLUMN_CLASSES;
                columnNames = new String[] { "Name", "Type", "Position", "Ref", "Alt" };
                break;
            case RICH_INTERVAL:
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
            switch (dataSource.getDataFormat()) {
                case SEQUENCE:
                    return new String(((SequenceRecord)datum).getSequence());
                case POINT:
                    switch (column) {
                        case 0:
                            return datum.getReference();
                        case 1:
                            return ((PointRecord)datum).getPoint();
                        case 2:
                            return ((PointRecord)datum).getDescription();
                    }
                case CONTINUOUS:
                    switch (column) {
                        case 0:
                            return datum.getReference();
                        case 1:
                            return ((ContinuousRecord)datum).getPosition();
                        case 2:
                            return ((ContinuousRecord)datum).getValue();
                    }
                case GENERIC_INTERVAL:
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
                case ALIGNMENT:
                    SAMRecord samRecord = ((BAMIntervalRecord)datum).getSAMRecord();
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
                case VARIANT:
                    VariantRecord varRec = (VariantRecord)datum;
                    switch (column) {
                        case 0:
                            return varRec.getName();
                        case 1:
                            return varRec.getVariantType().toString();
                        case 2:
                            return varRec.getInterval().getStart();
                        case 3:
                            return varRec.getRefBases();
                        case 4:
                            return varRec.getAltBases();
                    }
                case RICH_INTERVAL:
                    switch (column) {
                        case 0:
                            return ((RichIntervalRecord)datum).getReference();
                        case 1:
                            return ((RichIntervalRecord)datum).getInterval().getStart();
                        case 2:
                            return ((RichIntervalRecord)datum).getInterval().getEnd();
                        case 3:
                            return ((RichIntervalRecord)datum).getName();
                        case 4:
                            List<Block> blocks = ((RichIntervalRecord)datum).getBlocks();
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
            if (data.size() > maxRows) {
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
            if (dataSource.getDataFormat() == DataFormat.CONTINUOUS) {
                // Continuous tracks now use NaNs for missing values.  Filter them out.
                data = new ArrayList<Record>();
                for (Record r: dataInRange) {
                    if (!Float.isNaN(((ContinuousRecord)r).getValue())) {
                        data.add(r);
                        if (data.size() >= maxRows) {
                            break;
                        }
                    }
                }
            } else {
                data = dataInRange;
            }
        }
    }

    public void setMaxRows(int maxNumRows) {
        maxRows = maxNumRows;
    }

    public int getMaxRows() {
        return maxRows;
    }

    /**
     * Open the given file for export.  Write the file header (if any).
     * @param destFile
     */
    public void openExport(File destFile) throws IOException {
        if (dataSource.getDataFormat() == DataFormat.ALIGNMENT) {
            // If the file extension is .sam, it will create a SAM text file.
            // If it's .bam, it will create a BAM file and corresponding index.
            samWriter = new SAMFileWriterFactory().setCreateIndex(true).makeSAMOrBAMWriter(((BAMDataSource)dataSource).getHeader(), true, destFile);
        } else {
            exportWriter = new PrintWriter(destFile);
        }

        // Write an appropriate header.
        switch (dataSource.getDataFormat()) {
            case SEQUENCE:
                exportWriter.printf(">%s", NavigationUtils.getCurrentReferenceName()).println();
                break;
            case POINT:
            case CONTINUOUS:
            case GENERIC_INTERVAL:
            case VARIANT:
            case RICH_INTERVAL:
                exportWriter.println("# Savant Data Table Plugin 1.2.5");
                exportWriter.printf("#%s", columnNames[0]);
                for (int i = 1; i < columnNames.length; i++) {
                    exportWriter.printf("\t%s", columnNames[i]);
                }
                exportWriter.println();
                break;
            case ALIGNMENT:
                break;
        }
    }

    /**
     * Close the current export file.
     */
    public void closeExport() {
        if (dataSource.getDataFormat() == DataFormat.ALIGNMENT) {
            samWriter.close();
            samWriter = null;
        } else {
            exportWriter.close();
            exportWriter = null;
        }
    }

    /**
     * Export an entire row in the format appropriate for the data contained in the table.
     * @param row
     */
    public void exportRow(int row) {
        Record datum = data.get(row);
        switch (dataSource.getDataFormat()) {
            case SEQUENCE:
                exportWriter.println(new String(((SequenceRecord)datum).getSequence()));
                break;
            case POINT:
                exportWriter.printf("%s\t%d\t%s", datum.getReference(), ((PointRecord)datum).getPoint(), ((PointRecord)datum).getDescription()).println();
                break;
            case CONTINUOUS:
                exportWriter.printf("%s\t%d\t%f", datum.getReference(), ((ContinuousRecord)datum).getPosition(), ((ContinuousRecord)datum).getValue()).println();
                break;
            case GENERIC_INTERVAL:
                Interval inter = ((IntervalRecord)datum).getInterval();
                exportWriter.printf("%s\t%d\t%d\t%s", datum.getReference(), inter.getStart(), inter.getEnd(), ((IntervalRecord)datum).getName()).println();
                break;
            case ALIGNMENT:
                samWriter.addAlignment(((BAMIntervalRecord)datum).getSAMRecord());
                break;
            case VARIANT:
                VariantRecord varRec = (VariantRecord)datum;
                exportWriter.printf("%s\t%d\t%s\t%s\t%s", datum.getReference(), varRec.getInterval().getStart(), varRec.getInterval().getEnd(), varRec.getVariantType(), varRec.getRefBases(), varRec.getAltBases()).println();
                break;
            case RICH_INTERVAL:
                String[] values = ((TabixIntervalRecord)datum).getValues();
                for (int i = 0; i < columnNames.length; i++) {
                    if (i > 0) {
                        exportWriter.print('\t');
                    }
                    exportWriter.print(values[remappedColumns[i]]);
                }
                exportWriter.println();
                break;
        }
    }
}
