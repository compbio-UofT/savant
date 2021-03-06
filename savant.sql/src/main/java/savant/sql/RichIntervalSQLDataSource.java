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
package savant.sql;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;
import savant.data.types.TabixIntervalRecord;

/**
 * DataSource class which retrieves rich interval data from an SQL database.
 *
 * @author tarkvara
 */
public class RichIntervalSQLDataSource extends SQLDataSource<TabixIntervalRecord> {
    private final String[] columnNames;
    private final savant.util.ColumnMapping tabixMapping;

    RichIntervalSQLDataSource(MappedTable table, List<String> references) throws SQLException {
        super(table, references);

        String startsColumnName = null, endsColumnName = null;
        int relativeStartsColumn = -1, absoluteStartsColumn = -1, endsColumn = -1, sizesColumn = -1;
        if (columns.blockStartsRelative != null) {
            relativeStartsColumn = 9;
            startsColumnName = columns.blockStartsRelative;
        } else if (columns.blockStartsAbsolute != null) {
            absoluteStartsColumn = 9;
            startsColumnName = columns.blockStartsAbsolute;
        }
        if (columns.blockEnds != null) {
            endsColumn = 10;
            endsColumnName = columns.blockEnds;
        } else if (columns.blockSizes != null) {
            sizesColumn = 10;
            endsColumnName = columns.blockSizes;
        }

        columnNames = new String[] { columns.chrom, columns.start, columns.end, columns.name, columns.score, columns.strand, columns.thickStart, columns.thickEnd, columns.itemRGB, startsColumnName, endsColumnName, columns.name2 };

        // TODO: For UCSC, passing false as the final argument makes sense; for other databases it might not.
        tabixMapping = savant.util.ColumnMapping.createRichIntervalMapping(0, 1, 2, 3, 4, 5, 6, 7, 8, relativeStartsColumn, absoluteStartsColumn, endsColumn, sizesColumn, 11, false);
    }

    @Override
    public List<TabixIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws IOException {
        List<TabixIntervalRecord> result = new ArrayList<TabixIntervalRecord>();
        try {
            ResultSet rs = executeQuery(reference, range.getFrom(), range.getTo());

            while (rs.next()) {
                int start = rs.getInt(columns.start);
                int end = rs.getInt(columns.end);
                String name = rs.getString(columns.name);
                String name2 = "";
                if (columns.name2 != null) {
                    name2 = rs.getString(columns.name2);
                }
                float score = 0.0F;
                if (columns.score != null) {
                    score = rs.getFloat(columns.score);
                }
                String strand = "";
                if (columns.strand != null) {
                    strand = rs.getString(columns.strand);
                }
                int thickStart = start;
                if (columns.thickStart != null) {
                    thickStart = rs.getInt(columns.thickStart);
                }
                int thickEnd = end;
                if (columns.thickEnd != null) {
                    thickEnd = rs.getInt(columns.thickEnd);
                }
                int itemRGB = 0;
                if (columns.itemRGB != null) {
                    itemRGB = rs.getInt(columns.itemRGB);
                }

                String blockStarts = "";
                if (columns.blockStartsRelative != null) {
                    blockStarts = extractBlocks(rs.getBlob(columns.blockStartsRelative));
                } else if (columns.blockStartsAbsolute != null) {
                    blockStarts = extractBlocks(rs.getBlob(columns.blockStartsAbsolute));
                }
                String blockEnds = "";
                if (columns.blockEnds != null) {
                    blockEnds = extractBlocks(rs.getBlob(columns.blockEnds));
                } else if (columns.blockSizes != null) {
                    blockEnds = extractBlocks(rs.getBlob(columns.blockSizes));
                }

                // Because we're pretending to be Tabix, we just slam together a tab-delimited line of data.
                String line = reference +"\t" + start + "\t" + rs.getInt(columns.end) + "\t" + name + "\t" + score + "\t" + strand + "\t" + thickStart + "\t" + thickEnd + "\t" + itemRGB + "\t" + blockStarts + "\t" + blockEnds + "\t" + name2;
                TabixIntervalRecord rec = TabixIntervalRecord.valueOf(line, tabixMapping);
                if (filt == null || filt.accept(rec)) {
                    result.add(rec);
                }
            }
            rs.close();
        } catch (SQLException sqlx) {
            LOG.error(sqlx);
            throw new IOException(sqlx);
        }
        return result;
    }

    /**
     * UCSC stores exon starts and ends as a comma-separated list of numbers packed into a blob.
     *
     * @param b the blob to be unpacked
     * @return
     */
    private String extractBlocks(Blob b) throws IOException, SQLException {
        return new String(b.getBytes(1, (int)b.length()));
    }

    @Override
    public DataFormat getDataFormat() {
        return DataFormat.RICH_INTERVAL;
    }

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }
}
