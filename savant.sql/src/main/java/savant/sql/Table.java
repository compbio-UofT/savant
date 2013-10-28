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

    @Override
    public boolean equals(Object o) {
        if (o instanceof Table) {
            Table other = (Table)o;
            return name.equals(other.name) && database.equals(other.database);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 59 * hash + (this.database != null ? this.database.hashCode() : 0);
        return hash;
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
