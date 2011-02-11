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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.data.sources.DataSource;
import savant.data.types.Record;
import savant.sql.SQLDataSourcePlugin.Field;
import savant.sql.Table.Column;


/**
 * Base-class for DataSources which create Savant record objects from an SQL query.
 * The derived class is responsible for the nastiness of setting up the query and
 * any extra data-munging required.
 *
 * @author tarkvara
 */
abstract class SQLDataSource<E extends Record> implements DataSource<E> {
    protected static final Log LOG = LogFactory.getLog(SQLDataSource.class);

    protected Table table;
    protected Map<Field, Column> columns;
    private Set<String> references = new HashSet<String>();

    protected SQLDataSource(Table table, Map<Field, Column> columns) throws SQLException {
        this.table = table;
        this.columns = columns;
        ResultSet rs = table.executeQuery("SELECT DISTINCT %s FROM %s", columns.get(Field.CHROM), table);
        while (rs.next()) {
            references.add(rs.getString(1));
        }
    }

    @Override
    public synchronized Set<String> getReferenceNames() {
        return references;
    }

    @Override
    public String getName() {
        return "SQL Datasource";
    }

    @Override
    public void close() {
        table.closeConnection();
    }

    @Override
    public URI getURI() {
        return table.getURI();
    }
}
