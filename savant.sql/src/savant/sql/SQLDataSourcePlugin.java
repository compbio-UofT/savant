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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
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
    private static final String DRIVER_NAME_SETTING = "DRIVER_NAME";
    private static final String URI_SETTING = "URI";
    private static final String USER_SETTING = "USER";
    private static final String PASSWORD_SETTING = "PASSWORD";

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
     * Table which keeps track of which fields we expect to find in the mappings for
     * a given data format.
     */
    private static final Field[][] EXPECTED_FIELDS = new Field[][] {
        null, // SEQUENCE_FASTA
        null, // POINT_GENERIC
        new Field[] { Field.CHROM, Field.POSITION, Field.VALUE },
        null, // INTERVAL_GENERIC
        new Field[] { Field.CHROM, Field.START, Field.END, Field.NAME, Field.SCORE, Field.STRAND, Field.THICK_START, Field.THICK_END, Field.BLOCK_STARTS, Field.BLOCK_ENDS },
        null  // INTERVAL_BAM
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
        DataSource result = null;
        closeConnection();

        // By default, log in using the last-used URI.
        uri = new URI(SettingsUtils.getString(this, URI_SETTING));
        tryToLogin(false);

        // Connection will be null if user cancelled login dialog.
        if (connection != null) {
            getMappings();

            // Table will be null if user cancelled mapping dialog.
            if (table != null) {
                result = createCachedDataSource();
            }
        }
        return result;
    }

    /**
     * Try to make a connection using the saved login info.  If that fails, put up
     * the login dialog.
     */
    private void tryToLogin(boolean silent) throws ClassNotFoundException, SQLException {
        driverName = SettingsUtils.getString(this, DRIVER_NAME_SETTING);
        userName = SettingsUtils.getString(this, USER_SETTING);
        password = SettingsUtils.getPassword(this, PASSWORD_SETTING);

        if (silent) {
            // If possible, open the database connection without a dialog.
            try {
                getConnection();
                return;
            } catch (Exception ignored) {
            }
        }
        LoginDialog login = new LoginDialog(Savant.getInstance(), this);
        login.setVisible(true);
    }


    /**
     * After a successful login, save the login settings.
     * TODO: Saving password should be optional.
     */
    void saveSettings() {
        SettingsUtils.setString(this, SQLDataSourcePlugin.DRIVER_NAME_SETTING, driverName);
        SettingsUtils.setString(this, URI_SETTING, uri.toString());
        SettingsUtils.setString(this, USER_SETTING, userName);
        SettingsUtils.setPassword(this, PASSWORD_SETTING, password);
        SettingsUtils.store();
    }

    /**
     * Try to use existing mappings to figure out the table.  If that fails, put up
     * the mappings dialog.
     */
    private void tryToMap() throws SQLException {
        format = null;
        mappings = null;
        for (int i = 0; i < EXPECTED_FIELDS.length; i++) {
            if (EXPECTED_FIELDS[i] != null) {
                Map<Field, Column> m = new EnumMap<Field, Column>(Field.class);
                for (Field f: EXPECTED_FIELDS[i]) {
                    String colName = SettingsUtils.getString(this, f.toString());
                    if (colName != null) {
                        Column col = table.findColumnByName(colName);
                        if (col != null) {
                            m.put(f, col);
                            continue;
                        }
                    }
                    // No saved mapping found for this required field.
                    m = null;
                    break;
                }
                if (m != null) {
                    format = DataFormat.values()[i];
                    mappings = m;
                    break;
                }
            }
        }
    }

    /**
     * No field mappings found.  We'll have to put up a dialog to ask for them.
     *
     * @throws SQLException
     */
    private void getMappings() throws SQLException {
        // No field mappings established.  Have to fall back on the mapping dialog.
        MappingDialog mapping = new MappingDialog(Savant.getInstance(), this);
        mapping.setVisible(true);

        // Now that we've got the relevant info about databases, clean up connections
        // to the tables we're not using.
        for (Database db: databases) {
            if (!db.containsTable(table)) {
                db.closeConnection();
            }
        }
    }

    @Override
    public DataSource getDataSource(URI uri) {
        DataSource result = null;
        if (uri.getScheme().equals("jdbc")) {
            try {
                closeConnection();

                // The full URI will include database and table components, which
                // we need to extract.
                String uriString = uri.toString();
                int lastSlash = uriString.lastIndexOf("/");
                int penultimateSlash = uriString.lastIndexOf("/", lastSlash - 1);
                String tableName = uriString.substring(lastSlash + 1);
                String dbName = uriString.substring(penultimateSlash + 1, lastSlash);

                // Try to log in to the given URI using credentials from the
                this.uri = new URI(uriString.substring(0, penultimateSlash));
                tryToLogin(true);

                if (connection != null) {
                    table = new Table(tableName, new Database(dbName, this.uri, userName, password));
                    tryToMap();
                    if (mappings == null) {
                        getMappings();
                    }

                    if (mappings != null) {
                        result = createCachedDataSource();
                    }
                }
            } catch (Exception x) {
                LOG.warn("SQLDataSourcePlugin unable to open " + uri, x);
            }
        }
        return result;
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
     * Everything is set up to create the new data source.  Do it.
     * @return
     * @throws SQLException
     */
    private DataSource createCachedDataSource() throws SQLException {
        DataSource result = null;
        switch (format) {
            case INTERVAL_BED:
                result = new BEDSQLDataSource(table, mappings);
                break;
        }
        if (result != null) {
            result = new RecordCachingDataSource(result);
        }
        return result;
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
