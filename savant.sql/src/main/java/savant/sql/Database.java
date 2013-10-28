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

import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Small class which provides information about a database.
 *
 * @author tarkvara
 */
public class Database {
    private static final Log LOG = LogFactory.getLog(Database.class);

    final String name;
    final URI serverURI;
    private final String userName;
    private final String password;

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
    public Database(String name, URI serverURI, String userName, String password) {
        this.name = name;
        this.serverURI = serverURI;
        this.userName = userName;
        this.password = password;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Database) {
            Database other = (Database)o;
            return name.equals(other.name) && serverURI.equals(other.serverURI);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 59 * hash + (this.serverURI != null ? this.serverURI.hashCode() : 0);
        return hash;
    }

    public synchronized List<Table> getTables() throws SQLException {
        if (tables == null) {
            tables = new ArrayList<Table>();
            DatabaseMetaData md = getConnection().getMetaData();
            ResultSet rs = md.getTables(null, null, "%", new String[] { "TABLE" });
            int numTables = 0;
            while (rs.next()) {
                String s = rs.getString("TABLE_NAME");
                tables.add(new Table(s, this));
                numTables++;
                LOG.debug(numTables + ": " + s);
            }
            LOG.info("Retrieved " + numTables + " tables for " + name);
        }
        return tables;
    }

    public ResultSet executeQuery(String format, Object... args) throws SQLException {
        Statement st = getConnection().createStatement();
        String query = String.format(format, args);
        LOG.debug(query);
        return st.executeQuery(query);
    }


    public PreparedStatement prepareStatement(String format, Object... args) throws SQLException {
        String query = String.format(format, args);
        return getConnection().prepareStatement(query);
    }

    /**
     * Is the database currently using the given table?  If the database has not queried
     * for tables, just return false.
     *
     * @param t the table we're looking for
     * @return true if the table is in use.
     */
    public boolean containsTable(Table t) {
        return tables != null ? tables.contains(t) : false;
    }

    /**
     * Get the table corresponding to the given name.
     */
    public Table findTable(String name) throws SQLException {
        for (Table t: getTables()) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    public Connection getConnection() throws SQLException {
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


    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException sqlx) {
                LOG.warn("Error closing connection to " + serverURI, sqlx);
            }
            connection = null;
        }
    }

    public String getName() {
        return name;
    }
}
