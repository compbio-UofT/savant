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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import savant.api.adapter.RangeAdapter;
import savant.data.types.BEDIntervalRecord;
import savant.data.types.Block;
import savant.data.types.ItemRGB;
import savant.data.types.Strand;
import savant.file.DataFormat;
import savant.util.Resolution;

/**
 * DataSource class which retrieves BED data from an SQL database.
 *
 * @author tarkvara
 */
public class BedSQLDataSource extends SQLDataSource<BEDIntervalRecord> {
    private Map extraData;

    BedSQLDataSource(MappedTable table, Set<String> references) throws SQLException {
        super(table, references);
    }

    @Override
    public List<BEDIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<BEDIntervalRecord> result = new ArrayList<BEDIntervalRecord>();
        try {
            ResultSet rs = executeQuery(reference, range.getFrom(), range.getTo());

            // Hack for Aziz.
            extraData = null;
            for (Column col: table.getColumns()) {
                if (col.name.equals("name2")) {
                    extraData = new HashMap();
                    break;
                }
            }

            while (rs.next()) {
                int start = rs.getInt(columns.start) + 1;
                String name = rs.getString(columns.name);
                if (extraData != null) {
                    extraData.put(name, rs.getString("name2"));
                }
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
                result.add(BEDIntervalRecord.valueOf(reference, start, rs.getInt(columns.end), name, score, strand, thickStart, thickEnd, itemRGB, blocks));
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

    @Override
    public Object getExtraData() {
        return extraData;
    }
}
