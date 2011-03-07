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

import savant.api.adapter.RangeAdapter;
import savant.data.types.BEDIntervalRecord;
import savant.data.types.Block;
import savant.data.types.ItemRGB;
import savant.file.DataFormat;
import savant.util.Resolution;
import savant.util.Strand;

/**
 * DataSource class which retrieves BED data from an SQL database.
 *
 * @author tarkvara
 */
public class BEDSQLDataSource extends SQLDataSource<BEDIntervalRecord> {

    BEDSQLDataSource(Table table, ColumnMapping columns) throws SQLException {
        super(table, columns);
    }

    @Override
    public List<BEDIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<BEDIntervalRecord> result = new ArrayList<BEDIntervalRecord>();
        try {
            ResultSet rs = table.database.executeQuery("SELECT * FROM %s WHERE %s = '%s' AND ((%s >= '%d' AND %s <= '%d') OR (%s >= '%d' AND %s <= '%d') OR (%s < '%d' AND %s > '%d'))", table, columns.chrom, reference,
                    columns.start, range.getFrom(), columns.start, range.getTo(),
                    columns.end, range.getFrom(), columns.end, range.getTo(),
                    columns.start, range.getFrom(), columns.end, range.getTo());
            while (rs.next()) {
                int start = rs.getInt(columns.start);
                String name = rs.getString(columns.name);
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
                if (columns.blockStarts != null) {
                    List<Integer> blockStarts = extractBlocks(rs.getBlob(columns.blockStarts));
                    blocks = new ArrayList<Block>(blockStarts.size());
                    if (columns.blockEnds != null) {
                        List<Integer> blockEnds = extractBlocks(rs.getBlob(columns.blockEnds));
                        for (int i = 0; i < blockEnds.size(); i++) {
                            blocks.add(new Block(blockStarts.get(i) - start, blockEnds.get(i) - blockStarts.get(i) - 1));
                        }
                    } else if (columns.blockSizes != null) {
                        List<Integer> blockSizes = extractBlocks(rs.getBlob(columns.blockSizes));
                        for (int i = 0; i < blockSizes.size(); i++) {
                            blocks.add(new Block(blockStarts.get(i) - start, blockSizes.get(i) - 1));
                        }
                    } else {
                        throw new IOException("No column provided for block ends/sizes.");
                    }
                }
                result.add(BEDIntervalRecord.valueOf(reference, start, rs.getInt(columns.end) - 1, name, score, strand, thickStart, thickEnd, itemRGB, blocks));
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
        return DataFormat.INTERVAL_BED;
    }
}
