/*
 *    Copyright 2010-2011 University of Toronto
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

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.data.sources.DataSource;
import savant.data.types.Record;


/**
 * Base-class for DataSources which create Savant record objects from an SQL query.
 * The derived class is responsible for the nastiness of setting up the query and
 * any extra data-munging required.
 *
 * @author tarkvara
 */
abstract class SQLDataSource<E extends Record> implements DataSource<E> {
    protected static final Log LOG = LogFactory.getLog(SQLDataSource.class);

    protected MappedTable table;
    protected ColumnMapping columns;
    private Set<String> references = new HashSet<String>();
    private PreparedStatement prep;
    private String lastChrom;

    protected SQLDataSource(MappedTable table, Set<String> references) {
        this.table = table;
        this.columns = table.mapping;
        this.references = references;
    }

    public ResultSet executeQuery(String chrom, long start, long end) throws SQLException {
        if (columns.chrom == null) {
            return table.database.executeQuery("SELECT * FROM %s WHERE ((%s >= '%d' AND %s <= '%d') OR (%s >= '%d' AND %s <= '%d') OR (%s < '%d' AND %s > '%d')) ORDER BY %s", chrom + "_" + table.trackName,
                columns.start, start, columns.start, end,
                columns.end, start, columns.end, end,
                columns.start, start, columns.end, end, columns.start);
        } else {
            return table.database.executeQuery("SELECT * FROM %s WHERE %s = '%s' AND ((%s >= '%d' AND %s <= '%d') OR (%s >= '%d' AND %s <= '%d') OR (%s < '%d' AND %s > '%d')) ORDER BY %s", table.name, columns.chrom, chrom,
                columns.start, start, columns.start, end,
                columns.end, start, columns.end, end,
                columns.start, start, columns.end, end, columns.start);
        }
    }

    @Override
    public synchronized Set<String> getReferenceNames() {
        return references;
    }

    @Override
    public String getName() {
        return getURI().toString();
    }

    @Override
    public void close() {
        table.closeConnection();
    }

    @Override
    public URI getURI() {
        return table.getURI();
    }

    @Override
    public Object getExtraData() {
        return null;
    }

}
