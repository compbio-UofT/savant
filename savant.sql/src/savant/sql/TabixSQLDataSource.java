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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import savant.api.adapter.RangeAdapter;
import savant.data.types.TabixIntervalRecord;
import savant.file.DataFormat;
import savant.util.Resolution;


/**
 * DataSource class which retrieves generic interval data from an SQL database.
 *
 * @author tarkvara
 */
public class TabixSQLDataSource extends SQLDataSource<TabixIntervalRecord> {

    TabixSQLDataSource(Table table, ColumnMapping columns) throws SQLException {
        super(table, columns);
    }

    @Override
    public List<TabixIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<TabixIntervalRecord> result = new ArrayList<TabixIntervalRecord>();
        try {
            ResultSet rs = table.database.executeQuery("SELECT * FROM %s WHERE %s = '%s' AND ((%s >= '%d' AND %s <= '%d') OR (%s >= '%d' AND %s <= '%d') OR (%s < '%d' AND %s > '%d')) ORDER BY %s", table, columns.chrom, reference,
                    columns.start, range.getFrom(), columns.start, range.getTo(),
                    columns.end, range.getFrom(), columns.end, range.getTo(),
                    columns.start, range.getFrom(), columns.end, range.getTo(),
                    columns.start);
            ResultSetMetaData rsmd = rs.getMetaData();
            int chromPos = -1;
            int startPos = -1;
            int endPos = -1;
            int numCols = rsmd.getColumnCount();
            for (int i = 1; i <= numCols && chromPos < 0 && startPos < 0 && endPos < 0; i++) {
                String colName = rsmd.getColumnName(i);
                if (chromPos < 0 && colName.equals(columns.chrom)) {
                    chromPos = i;
                } else if (startPos < 0 && colName.equals(columns.start)) {
                    startPos = i;
                } else if (endPos < 0 && colName.equals(columns.end)) {
                    endPos = i;
                }
            }
            while (rs.next()) {
                StringBuilder tabDelimited = new StringBuilder(rs.getString(1));
                for (int i = 2; i < numCols; i++) {
                    tabDelimited.append('\t').append(rs.getString(i));
                }
                result.add(TabixIntervalRecord.valueOf(tabDelimited.toString(), chromPos, startPos, endPos));
            }
        } catch (SQLException sqlx) {
            LOG.error(sqlx);
            throw new IOException(sqlx);
        }
        return result;
    }


    @Override
    public DataFormat getDataFormat() {
        return DataFormat.TABIX;
    }
}
