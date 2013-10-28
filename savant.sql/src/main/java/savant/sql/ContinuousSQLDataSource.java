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
import savant.api.util.Resolution;
import savant.data.types.GenericContinuousRecord;


/**
 * DataSource class which retrieves continuous data from an SQL database.  This assumes
 * that the data in question is stored in a "value" column within the database.
 *
 * @author tarkvara
 */
public class ContinuousSQLDataSource extends SQLDataSource<GenericContinuousRecord> {

    ContinuousSQLDataSource(MappedTable table, List<String> references) throws SQLException {
        super(table, references);
    }

    @Override
    public List<GenericContinuousRecord> getRecords(String reference, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws IOException {
        List<GenericContinuousRecord> result = new ArrayList<GenericContinuousRecord>();
        try {
            ResultSet rs = executeQuery(reference, range.getFrom(), range.getTo());
            while (rs.next()) {
                String chrom = rs.getString(columns.chrom);
                int start = rs.getInt(columns.start);
                int end = rs.getInt(columns.end);
                for (int i = start; i <= end; i++) {
                    GenericContinuousRecord rec = GenericContinuousRecord.valueOf(chrom, i, rs.getFloat(columns.value));
                    if (filt == null || filt.accept(rec)) {
                        result.add(rec);
                    }
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
        return DataFormat.CONTINUOUS;
    }

    @Override
    public String[] getColumnNames() {
        return GenericContinuousRecord.COLUMN_NAMES;
    }
}
