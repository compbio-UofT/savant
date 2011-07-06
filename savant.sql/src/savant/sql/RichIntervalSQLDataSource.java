/*
 *    Copyright 2011 University of Toronto
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

package savant.sql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import savant.api.adapter.RangeAdapter;
import savant.data.types.Block;
import savant.data.types.ItemRGB;
import savant.data.types.Strand;
import savant.data.types.TabixIntervalRecord;
import savant.file.DataFormat;
import savant.util.Resolution;

/**
 * DataSource class which retrieves rich interval data from an SQL database.
 *
 * @author tarkvara
 */
public class RichIntervalSQLDataSource extends SQLDataSource<TabixIntervalRecord> {
    private final String[] columnNames;
    private final savant.util.ColumnMapping tabixMapping;

    RichIntervalSQLDataSource(MappedTable table, Set<String> references) throws SQLException {
        super(table, references);
        columnNames = new String[] { columns.chrom, columns.start, columns.end, columns.name, columns.score, columns.strand, columns.thickStart, columns.thickEnd, columns.name2 };
        tabixMapping = savant.util.ColumnMapping.createRichIntervalMapping(0, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1, -1, -1, 8, true);
    }

    @Override
    public List<TabixIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<TabixIntervalRecord> result = new ArrayList<TabixIntervalRecord>();
        try {
            ResultSet rs = executeQuery(reference, range.getFrom(), range.getTo());

            while (rs.next()) {
                int start = rs.getInt(columns.start) + 1;
                String name = rs.getString(columns.name);
                String name2 = rs.getString(columns.name2);
                float score = 0.0F;
                if (columns.score != null) {
                    score = rs.getFloat(columns.score);
                }
                Strand strand = null;
                if (columns.strand != null) {
                    strand = rs.getString(columns.strand).charAt(0) == '+' ? Strand.FORWARD : Strand.REVERSE;
                }
                int thickStart = -1;
                if (columns.thickStart != null) {
                    thickStart = rs.getInt(columns.thickStart);
                }
                int thickEnd = -1;
                if (columns.thickEnd != null) {
                    thickEnd = rs.getInt(columns.thickEnd) - 1;
                }
                ItemRGB itemRGB = null;
                if (columns.itemRGB != null) {
                    int rgb = rs.getInt(columns.itemRGB);
                    itemRGB = ItemRGB.valueOf((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                }
                List<Block> blocks = null;
                boolean relativeStarts = columns.blockStartsRelative != null;
                if (relativeStarts || columns.blockStartsAbsolute != null) {
                    List<Integer> blockStarts = extractBlocks(rs.getBlob(relativeStarts ? columns.blockStartsRelative : columns.blockStartsAbsolute));
                    blocks = new ArrayList<Block>(blockStarts.size());
                    int offset = relativeStarts ? 0 : start - 1;
                    if (columns.blockEnds != null) {
                        List<Integer> blockEnds = extractBlocks(rs.getBlob(columns.blockEnds));
                        for (int i = 0; i < blockEnds.size(); i++) {
                            blocks.add(Block.valueOf(blockStarts.get(i) - offset, blockEnds.get(i) - blockStarts.get(i)));
                        }
                    } else if (columns.blockSizes != null) {
                        List<Integer> blockSizes = extractBlocks(rs.getBlob(columns.blockSizes));
                        for (int i = 0; i < blockSizes.size(); i++) {
                            blocks.add(Block.valueOf(blockStarts.get(i) - offset, blockSizes.get(i)));
                        }
                    } else {
                        throw new IOException("No column provided for block ends/sizes.");
                    }
                }
                // Because we're pretending to be Tabix, we just slam together a tab-delimited line of data.
                String line = reference +"\t" + start + "\t" + rs.getInt(columns.end) + "\t" + name + "\t" + score + "\t" + strand + "\t" + thickStart + "\t" + thickEnd + "\t" + name2;
                result.add(TabixIntervalRecord.valueOf(line, tabixMapping));
            }
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
    private List<Integer> extractBlocks(Blob b) throws IOException, SQLException {
        List<Integer> result = new ArrayList<Integer>();
        InputStream input = b.getBinaryStream();
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = input.read()) != -1) {
            if (c == ',') {
                // Finished building the current number.
                result.add(Integer.parseInt(sb.toString()));
                sb = new StringBuilder();
            } else {
                sb.append(Character.valueOf((char)c));
            }
        }
        // One final one after the last comma.
        if (sb.length() > 0) {
            result.add(Integer.parseInt(sb.toString()));
        }
        return result;
    }

    @Override
    public DataFormat getDataFormat() {
        return DataFormat.INTERVAL_RICH;
    }

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }
}
