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
import java.util.Map;

import savant.api.adapter.RangeAdapter;
import savant.data.types.BEDIntervalRecord;
import savant.data.types.Block;
import savant.data.types.Interval;
import savant.file.DataFormat;
import savant.sql.SQLDataSourcePlugin.Field;
import savant.sql.Table.Column;
import savant.util.Resolution;
import savant.util.Strand;

/**
 * DataSource class which retrieves BED data from an SQL database.
 *
 * @author tarkvara
 */
public class BEDSQLDataSource extends SQLDataSource<BEDIntervalRecord> {

    BEDSQLDataSource(Table table, Map<Field, Column> columns) throws SQLException {
        super(table, columns);
    }

    @Override
    public List<BEDIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<BEDIntervalRecord> result = new ArrayList<BEDIntervalRecord>();
        try {
            ResultSet rs = table.executeQuery("SELECT * FROM %s WHERE %s = '%s' AND %s >= '%d' AND %s <= '%d'", table, columns.get(Field.CHROM), reference, columns.get(Field.START), range.getFrom(), columns.get(Field.END), range.getTo());
            while (rs.next()) {
                List<Block> blocks = extractBlocks(rs.getBlob(columns.get(Field.BLOCK_STARTS).name),  rs.getBlob(columns.get(Field.BLOCK_ENDS).name));

                Strand strand = rs.getString(columns.get(Field.STRAND).name).charAt(0) == '+' ? Strand.FORWARD : Strand.REVERSE;
                LOG.info(String.format("Adding %s at (%d,%d), %s with %d blocks:", rs.getString(columns.get(Field.NAME).name), rs.getInt(columns.get(Field.START).name), rs.getInt(columns.get(Field.END).name), strand, blocks.size()));
                for (Block b: blocks) {
                    LOG.info(String.format("{%d, %d}", b.getPosition(), b.getSize()));
                }
                result.add(new BEDIntervalRecord(reference,
                                                 new Interval(rs.getInt(columns.get(Field.START).name), rs.getInt(columns.get(Field.END).name)),
                                                 rs.getString(columns.get(Field.NAME).name),
                                                 rs.getFloat(columns.get(Field.SCORE).name),
                                                 strand,
                                                 rs.getInt(columns.get(Field.THICK_START).name),
                                                 rs.getInt(columns.get(Field.THICK_END).name),
                                                 null,
                                                 blocks));
            }
        } catch (SQLException sqlx) {
            LOG.error(sqlx);
            throw new IOException(sqlx);
        }
        return result;
    }

    /**
     * Given two blobs full of exon starts and end, retrieve the associated blocks.
     *
     * @param startsBlob  blob containing comma-separated list of block starts
     * @param endsBlob    blob containing comma-separated list of block ends
     * @return
     */
    private List<Block> extractBlocks(Blob startsBlob, Blob endsBlob) throws IOException, SQLException {
        List<Integer> starts = unpackBlob(startsBlob);
        List<Integer> ends = unpackBlob(endsBlob);
        if (starts.size() != ends.size()) {
            throw new IOException(String.format("Mismatch: found %d block starts and %d block ends.", starts.size(), ends.size()));
        }
        List<Block> result = new ArrayList<Block>(starts.size());
        for (int i = 0; i < starts.size(); i++) {
            result.add(new Block(starts.get(i), ends.get(i) - starts.get(i)));
        }
        return result;
    }

    /**
     * UCSC stores exon starts and ends as a comma-separated list of numbers packed into a blob.
     * @param b
     * @return
     */
    private List<Integer> unpackBlob(Blob b) throws IOException, SQLException {
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
