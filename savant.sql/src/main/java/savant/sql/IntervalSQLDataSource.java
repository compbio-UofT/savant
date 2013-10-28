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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
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
    public List<GenericIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws IOException {
        List<GenericIntervalRecord> result = new ArrayList<GenericIntervalRecord>();
        try {
            ResultSet rs = executeQuery(reference, range.getFrom(), range.getTo());
            while (rs.next()) {
                String name = null;
                if (columns.name != null) {
                    name = rs.getString(columns.name);
                }
                GenericIntervalRecord rec = GenericIntervalRecord.valueOf(reference, Interval.valueOf(rs.getInt(columns.start) + 1, rs.getInt(columns.end)), name);
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


    @Override
    public DataFormat getDataFormat() {
        return DataFormat.GENERIC_INTERVAL;
    }

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }
}
