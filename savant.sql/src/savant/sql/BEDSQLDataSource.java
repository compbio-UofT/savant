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
            ResultSet rs = table.executeQuery("SELECT * FROM %s WHERE %s = '%s' AND ((%s >= '%d' AND %s <= '%d') OR (%s >= '%d' AND %s <= '%d') OR (%s < '%d' AND %s > '%d'))", table, columns.get(Field.CHROM), reference,
                    columns.get(Field.START), range.getFrom(), columns.get(Field.START), range.getTo(),
                    columns.get(Field.END), range.getFrom(), columns.get(Field.END), range.getTo(),
                    columns.get(Field.START), range.getFrom(), columns.get(Field.END), range.getTo());
            while (rs.next()) {
                List<Integer> blockStarts = extractBlocks(rs.getBlob(columns.get(Field.BLOCK_STARTS).name));
                List<Integer> blockEnds = extractBlocks(rs.getBlob(columns.get(Field.BLOCK_ENDS).name));

                if (blockStarts.size() != blockEnds.size()) {
                    throw new IOException(String.format("Mismatch: found %d block starts and %d block ends.", blockStarts.size(), blockEnds.size()));
                }
                int start = rs.getInt(columns.get(Field.START).name);
                List<Block> blocks = new ArrayList<Block>(blockStarts.size());
                for (int i = 0; i < blockStarts.size(); i++) {
                    blocks.add(new Block(blockStarts.get(i) - start, blockEnds.get(i) - blockStarts.get(i) - 1));
                }

                Strand strand = rs.getString(columns.get(Field.STRAND).name).charAt(0) == '+' ? Strand.FORWARD : Strand.REVERSE;
                result.add(BEDIntervalRecord.valueOf(reference,
                                                     start,
                                                     rs.getInt(columns.get(Field.END).name) - 1,
                                                     rs.getString(columns.get(Field.NAME).name),
                                                     rs.getFloat(columns.get(Field.SCORE).name),
                                                     strand,
                                                     rs.getInt(columns.get(Field.THICK_START).name),
                                                     rs.getInt(columns.get(Field.THICK_END).name) - 1,
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
