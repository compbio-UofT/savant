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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.api.util.SettingsUtils;
import savant.data.sources.DataSource;
import savant.plugin.PluginAdapter;
import savant.plugin.SavantDataSourcePlugin;


/**
 * Plugin class which exposes data retrieved from an SQL database.
 *
 * @author tarkvara
 */
public class SQLDataSourcePlugin extends SavantDataSourcePlugin {
    protected static final Log LOG = LogFactory.getLog(SQLDataSourcePlugin.class);
    protected static final String DRIVER_NAME_SETTING = "DRIVER_NAME";
    protected static final String URI_SETTING = "URI";
    protected static final String USER_SETTING = "USER";
    protected static final String PASSWORD_SETTING = "PASSWORD";

    protected String driverName;
    protected URI uri;
    protected String userName;
    protected String password;

    List<Database> databases;

    /**
     * For performance reasons, keep track of the last database used.
     */
    Database lastDatabase;

    private Connection connection;

    @Override
    public void init(PluginAdapter pluginAdapter) {
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
        tryToLogin();

        // Connection will be null if user cancelled login dialog.
        if (connection != null) {
            MappedTable table = requestMapping(null);

            // Table will be null if user cancelled mapping dialog.
            if (table != null) {
                result = createCachedDataSource(table);
            }
        }
        return result;
    }

    /**
     * Try to make a connection using the saved login info.  If that fails, put up
     * the login dialog.
     */
    protected void tryToLogin() throws ClassNotFoundException, SQLException {
        driverName = SettingsUtils.getString(this, DRIVER_NAME_SETTING);
        userName = SettingsUtils.getString(this, USER_SETTING);
        password = SettingsUtils.getPassword(this, PASSWORD_SETTING);

        // If possible, open the database connection without a dialog.
        try {
            getConnection();
        } catch (Exception ignored) {
            LoginDialog login = new LoginDialog(DialogUtils.getMainWindow(), this);
            login.setVisible(true);
        }
    }


    /**
     * After a successful login, save the login settings.
     * TODO: Saving password should be optional.
     */
    protected void saveSettings() {
        SettingsUtils.setString(this, SQLDataSourcePlugin.DRIVER_NAME_SETTING, driverName);
        SettingsUtils.setString(this, URI_SETTING, uri.toString());
        SettingsUtils.setString(this, USER_SETTING, userName);
        SettingsUtils.setPassword(this, PASSWORD_SETTING, password);
        SettingsUtils.store();
    }

    /**
     * No field mappings found.  We'll have to put up a dialog to ask for them.
     *
     * @throws SQLException
     */
    protected MappedTable requestMapping(Table t) throws SQLException {
        // No field mappings established.  Have to fall back on the mapping dialog.
        MappingDialog dlg = new MappingDialog(DialogUtils.getMainWindow(), this, t);
        dlg.setVisible(true);

        // Now that we've got the relevant info about databases, clean up connections
        // to the tables we're not using.
        MappedTable result = dlg.getMapping();
        for (Database db: databases) {
            if (!db.containsTable(result)) {
                db.closeConnection();
            }
        }
        return result;
    }

    @Override
    public DataSource getDataSource(URI uri) {
        DataSource result = null;
        if (canOpen(uri)) {
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
                tryToLogin();

                if (connection != null) {
                    MappedTable table = getTableByName(tableName, dbName, tableName);
                    if (table.mapping.format == null) {
                        table = requestMapping(table);
                    }

                    if (table.mapping.format != null) {
                        result = createCachedDataSource(table);
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
     * The base-class SQL plugin can open any jdbc URIs, but leaves UCSC database
     * URIs for the UCSC plugin to handle
     *
     * @param uri
     * @return true if the plugin is willing to open the given URI
     */
    public boolean canOpen(URI uri) {
        if (uri.getScheme().equals("jdbc")) {
            if (!uri.toString().contains("genome-mysql.cse.ucsc.edu")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Connect to the database URI without worrying about a specific database.
     *
     * @return a JDBC connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    protected Connection getConnection() throws ClassNotFoundException, SQLException {
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
    private DataSource createCachedDataSource(MappedTable table) throws SQLException {
        DataSource result = null;
        Set<String> references = getReferences(table);
        switch (table.mapping.format) {
            case CONTINUOUS_VALUE_COLUMN:
                result = new ContinuousSQLDataSource(table, references);
                break;
            case CONTINUOUS_WIG:
                result = new WigSQLDataSource(table, references);
                break;
            case CONTINUOUS_BIGWIG:
                throw new UnsupportedOperationException("BigWig SQL access not yet implemented.");
//                result = new BigWigSQLDataSource(table, references);
//                break;
            case INTERVAL_GENERIC:
                result = new IntervalSQLDataSource(table, references);
                break;
            case INTERVAL_BED:
                result = new BedSQLDataSource(table, references);
                break;
            case INTERVAL_TABIX:
                result = new TabixSQLDataSource(table, references);
                break;
        }
        if (result != null) {
            result = new RecordCachingDataSource(result);
        }
        return result;
    }

    /**
     * Get a connection to a particular database.
     */
    public synchronized Database getDatabase(String name) throws SQLException {
        if (lastDatabase == null || !name.equals(lastDatabase.name)) {
            lastDatabase = new Database(name, uri, userName, password);
        } else {
            LOG.info("Reusing existing database " + lastDatabase.name);
        }
        return lastDatabase;
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
            LOG.info("Calling SHOW DATABASES...");
            PreparedStatement ps = connection.prepareStatement("SHOW DATABASES");
            ResultSet rs = ps.executeQuery();
            int numDBs = 0;
            while (rs.next()) {
                String name = rs.getString(1);
                if (!name.equals("information_schema")) {
                    databases.add(new Database(name, uri, userName, password));
                    numDBs++;
                    LOG.info(numDBs + ": " + name);
                }
            }
            LOG.info("Retrieved " + numDBs + " databases.");
            closeConnection();
        }
        return databases;
    }

    /**
     * Get a list of references for this data-source.  For the basic SQLDataSource,
     * this is not needed.  For UCSC, it will use the chromInfo table to determine
     * the list of chromosomes for this genome.  This is necessary to support certain
     * UCSC tracks where the data is spread over one chromosome per table.
     */
    public Set<String> getReferences(MappedTable table) throws SQLException {
        Set<String> references = new HashSet<String>();
        ResultSet rs = table.database.executeQuery("SELECT DISTINCT %s FROM %s", table.mapping.chrom, table);
        while (rs.next()) {
            references.add(rs.getString(1));
        }
        return references;
    }

    /**
     * Restore all the information about a table, given information from a JDBC URI.
     *
     * @param tableName name of the MySQL table (e.g. knownGene or chr6_mrna)
     * @param dbName name of the MySQL database (e.g. hg18)
     * @param trackName name of the UCSC track (e.g. knownGene or mrna)
     * @return a database table with associated column mapping
     * @throws SQLException
     */
    public MappedTable getTableByName(String tableName, String dbName, String trackName) throws SQLException {
        Table t = new Table(tableName, new Database(dbName, uri, userName, password));
        return new MappedTable(t, ColumnMapping.getSavedMapping(this, t.getColumns(), false), trackName);
    }
}
