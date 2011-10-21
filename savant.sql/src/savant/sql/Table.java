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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


/**
 * Small class which provides information about a table within a database.
 *
 * @author tarkvara
 */
public class Table {
    final String name;
    final Database database;
    private Column[] columns;

    public Table(String name, Database database) {
        this.name = name;
        this.database = database;
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
     * As part of the plugin's cleanup, it should close the JDBC connection.
     */
    public void closeConnection() {
        database.closeConnection();
    }

    public synchronized Column[] getColumns() throws SQLException {
        if (columns == null) {
            ResultSet rs = database.executeQuery("SELECT * FROM %s LIMIT 1", name);
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            columns = new Column[numCols];
            for (int i = 0; i < numCols; i++) {
                columns[i] = new Column(md.getColumnName(i + 1), md.getColumnType(i + 1));
            }
            rs.close();
        }
        return columns;
    }

    public Database getDatabase() {
        return database;
    }

    public String getName() {
        return name;
    }
}
