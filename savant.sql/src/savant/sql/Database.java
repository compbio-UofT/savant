/*
 * Database.java
 *
 * Created on Jan 24, 2011, 01:16:20 PM
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

import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Small class which provides information about a database.
 *
 * @author tarkvara
 */
class Database {
    private String name;
    URI uri;
    String userName;
    String password;

    private List<Table> tables;
    private Connection connection;

    Database(String name, URI uri, String userName, String password) {
        this.name = name;
        this.uri = uri;
        this.userName = userName;
        this.password = password;
    }

    @Override
    public String toString() {
        return name;
    }

    synchronized List<Table> getTables() throws SQLException {
        if (tables == null) {
            tables = new ArrayList<Table>();
            DatabaseMetaData md = getConnection(name).getMetaData();
            ResultSet rs = md.getTables(null, null, "%", new String[] { "TABLE" });
            while (rs.next()) {
                tables.add(new Table(rs.getString("TABLE_NAME"), connection));
            }
        }
        return tables;
    }

    /**
     * Is the database currently using the given table?  If the database has not queried
     * for tables, just return false.
     *
     * @param t the table we're looking for
     * @return true if the table is in use.
     */
    boolean containsTable(Table t) {
        return tables != null ? tables.contains(t) : false;
    }

    private Connection getConnection(String database) throws SQLException {
        if (connection == null) {
            connection = DriverManager.getConnection(uri + "/" + database, userName, password);
        }
        return connection;
    }


    void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
