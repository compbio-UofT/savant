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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import savant.api.adapter.RangeAdapter;
import savant.api.data.DataFormat;
import savant.api.data.Interval;
import savant.api.util.Resolution;
import savant.data.types.GenericIntervalRecord;


/**
 * DataSource class which retrieves generic interval data from an SQL database.
 *
 * @author tarkvara
 */
public class IntervalSQLDataSource extends SQLDataSource<GenericIntervalRecord> {
    private final String[] columnNames;

    IntervalSQLDataSource(MappedTable table, List<String> references) throws SQLException {
        super(table, references);
        columnNames = new String[] { columns.chrom, columns.start, columns.end, columns.name };
    }

    @Override
    public List<GenericIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<GenericIntervalRecord> result = new ArrayList<GenericIntervalRecord>();
        try {
            ResultSet rs = executeQuery(reference, range.getFrom(), range.getTo());
            while (rs.next()) {
                String name = null;
                if (columns.name != null) {
                    name = rs.getString(columns.name);
                }
                result.add(GenericIntervalRecord.valueOf(reference, Interval.valueOf(rs.getInt(columns.start) + 1, rs.getInt(columns.end)), name));
            }
            rs.close();
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

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }
}
