/*
 * Table.java
 *
 * Created on Jan 24, 2011, 12:02:44 PM
 *
 *
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Small class which provides information about a table within a database.
 *
 * @author tarkvara
 */
public class Table {
    private static final Log LOG = LogFactory.getLog(Table.class);

    private String name;
    private Column[] columns;
    private Connection connection;

    Table(String name, Connection conn) {
        this.name = name;
        this.connection = conn;
    }

    /**
     * Override toString so that we can display a Table object directly in a combo-box.
     *
     * @return
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Close the associated JDBC connection.  This is done when the plugin itself
     * is being closed.
     */
    void closeConnection() {
        try {
            connection.close();
        } catch (SQLException sqlx) {
            LOG.error("Error closing SQL connection for " + name, sqlx);
        }
    }

    synchronized Column[] getColumns() throws SQLException {
        if (columns == null) {
            ResultSet rs = executeQuery("SELECT * FROM %s LIMIT 1", name);
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            columns = new Column[numCols];
            for (int i = 0; i < numCols; i++) {
                columns[i] = new Column(md.getColumnName(i + 1), md.getColumnType(i + 1));
                LOG.info(name + " " + i + ": " + columns[i] + ": " + columns[i].type);
            }
        }
        return columns;
    }

    ResultSet executeQuery(String format, Object... args) throws SQLException {
        Statement st = connection.createStatement();
        return st.executeQuery(String.format(format, args));
    }

    /**
     * Small class which provides information about a column within a table.
     */
    class Column {
        String name;
        int type;

        Column(String name, int type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

