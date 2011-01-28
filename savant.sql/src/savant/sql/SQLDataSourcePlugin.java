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
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.api.util.SettingsUtils;

import savant.data.sources.DataSource;
import savant.file.DataFormat;
import savant.plugin.PluginAdapter;
import savant.plugin.SavantDataSourcePlugin;
import savant.sql.Table.Column;
import savant.view.swing.Savant;

/**
 * Plugin class which exposes data retrieved from an SQL database.
 *
 * @author tarkvara
 */
public class SQLDataSourcePlugin extends SavantDataSourcePlugin {
    private static final Log LOG = LogFactory.getLog(SQLDataSourcePlugin.class);

    String driverName;
    URI uri;
    String userName;
    String password;

    List<Database> databases;

    /**
     * Which table is being used as the source for the records?
     */
    Table table;

    /**
     * Identifies fields within the Savant record type.
     */
    enum Field {
        // Shared by all record types
        CHROM,

        // Fields for BED interval format
        START,
        END,
        NAME,
        SCORE,
        STRAND,
        THICK_START,
        THICK_END,
        BLOCK_STARTS,
        BLOCK_ENDS,

        // Fields for generic continuous.
        POSITION,
        VALUE
    };

    /**
     * Keeps track of which database columns are mapped to fields in the records we're returning.
     */
    Map<Field, Column> mappings;

    DataFormat format;

    private Connection connection;

    @Override
    public void init(PluginAdapter pluginAdapter) {
/*        driverName = SettingsUtils.getString(this, "DriverName");
        uri = new URI("jdbc:mysql://ucscmirror.db.7053764.hostedresource.com:3306");
        userName = "ucscmirror";
        password = "Watson1";*/
    }

    @Override
    public String getTitle() {
        return "SQL Datasource Plugin";
    }

    @Override
    public DataSource getDataSource() throws Exception {
        closeConnection();
        LoginDialog login = new LoginDialog(Savant.getInstance(), this);
        login.setVisible(true);
        if (connection != null) {
            MappingDialog mapping = new MappingDialog(Savant.getInstance(), this);
            mapping.setVisible(true);

            // Now that we've got the relevant info about databases, clean up connections
            // to the tables we're not using.
            for (Database db: databases) {
                if (!db.containsTable(table)) {
                    db.closeConnection();
                }
            }

            // table will be null if user cancelled mapping dialog.
            if (table != null) {
                switch (format) {
                    case INTERVAL_BED:
                        return new BEDSQLDataSource(table, mappings);
                }
            }
        }
        return null;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    /**
     * Connect to the database URI without worrying about a specific database.
     *
     * @return a JDBC connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    Connection getConnection() throws ClassNotFoundException, SQLException {
        if (connection == null) {
            Class.forName(driverName);
            connection = DriverManager.getConnection(uri.toString(), userName, password);
        }
        return connection;
    }

    void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    /**
     * Retrieve a list of all databases on this server.  Assumes that the JDBC connection
     * was already opened for the LoginDialog.
     *
     * @return list of databases on the server
     * @throws SQLException
     */
    synchronized List<Database> getDatabases() throws SQLException {
        if (databases == null) {
            databases = new ArrayList<Database>();
            PreparedStatement ps = connection.prepareStatement("SHOW DATABASES");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                databases.add(new Database(rs.getString(1), uri, userName, password));
            }
            closeConnection();
        }
        return databases;
    }

}
