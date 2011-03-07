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
import savant.data.types.GenericIntervalRecord;
import savant.data.types.Interval;
import savant.file.DataFormat;
import savant.util.Resolution;


/**
 * DataSource class which retrieves generic interval data from an SQL database.
 *
 * @author tarkvara
 */
public class IntervalSQLDataSource extends SQLDataSource<GenericIntervalRecord> {

    IntervalSQLDataSource(Table table, ColumnMapping columns) throws SQLException {
        super(table, columns);
    }

    @Override
    public List<GenericIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<GenericIntervalRecord> result = new ArrayList<GenericIntervalRecord>();
        try {
            ResultSet rs = table.database.executeQuery("SELECT * FROM %s WHERE %s = '%s' AND ((%s >= '%d' AND %s <= '%d') OR (%s >= '%d' AND %s <= '%d') OR (%s < '%d' AND %s > '%d'))", table, columns.chrom, reference,
                    columns.start, range.getFrom(), columns.start, range.getTo(),
                    columns.end, range.getFrom(), columns.end, range.getTo(),
                    columns.start, range.getFrom(), columns.end, range.getTo());
            ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next()) {
                result.add(GenericIntervalRecord.valueOf(rs.getString(columns.chrom), Interval.valueOf(rs.getInt(columns.start), rs.getInt(columns.end)), rs.getString(columns.name)));
            }
        } catch (SQLException sqlx) {
            LOG.error(sqlx);
            throw new IOException(sqlx);
        }
        return result;
    }


    @Override
    public DataFormat getDataFormat() {
        return DataFormat.INTERVAL_GENERIC;
    }
}
