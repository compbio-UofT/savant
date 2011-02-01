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

import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Small class which provides information about a database.
 *
 * @author tarkvara
 */
class Database {
    private static final Log LOG = LogFactory.getLog(Database.class);

    String name;
    URI serverURI;
    String userName;
    String password;

    private List<Table> tables;
    private Connection connection;

    /**
     * Create a new database access object (but don't connect yet).
     *
     * @param name database name
     * @param serverURI URI to the server (without database name portion)
     * @param userName user name for SQL login
     * @param password password for SQL login
     */
    Database(String name, URI serverURI, String userName, String password) {
        this.name = name;
        this.serverURI = serverURI;
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
            DatabaseMetaData md = getConnection().getMetaData();
            ResultSet rs = md.getTables(null, null, "%", new String[] { "TABLE" });
            while (rs.next()) {
                tables.add(new Table(rs.getString("TABLE_NAME"), this));
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

    Connection getConnection() throws SQLException {
        if (connection != null && !connection.isValid(0)) {
            // Connection no longer valid.  Close it and recreate.
            LOG.info("Connection to " + serverURI + " no longer valid; recreating.");
            connection.close();
            connection = null;
        }
        if (connection == null) {
            connection = DriverManager.getConnection(serverURI + "/" + name, userName, password);
        }
        return connection;
    }


    void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException sqlx) {
                LOG.warn("Error closing connection to " + serverURI, sqlx);
            }
            connection = null;
        }
    }
}
