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
import savant.data.types.GenericContinuousRecord;
import savant.file.DataFormat;
import savant.util.Resolution;


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
    public List<GenericContinuousRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<GenericContinuousRecord> result = new ArrayList<GenericContinuousRecord>();
        try {
            ResultSet rs = executeQuery(reference, range.getFrom(), range.getTo());
            while (rs.next()) {
                String chrom = rs.getString(columns.chrom);
                int start = rs.getInt(columns.start);
                int end = rs.getInt(columns.end);
                for (int i = start; i <= end; i++) {
                    result.add(GenericContinuousRecord.valueOf(chrom, i, rs.getFloat(columns.value)));
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
        return DataFormat.CONTINUOUS_GENERIC;
    }

    @Override
    public String[] getColumnNames() {
        return GenericContinuousRecord.COLUMN_NAMES;
    }
}
