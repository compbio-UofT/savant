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

package savant.ucsc;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import savant.api.util.DialogUtils;
import savant.plugin.PluginAdapter;
import savant.sql.ColumnMapping;
import savant.sql.Database;
import savant.sql.MappedTable;
import savant.sql.SQLConstants;
import savant.sql.SQLDataSourcePlugin;
import savant.sql.Table;


/**
 * A version of the SQL data-source plugin which is configured to work nicely with
 * the UCSC database.
 *
 * @author tarkvara
 */
public class UCSCDataSourcePlugin extends SQLDataSourcePlugin implements SQLConstants {
    private static final Map<String, ColumnMapping> KNOWN_MAPPINGS = new HashMap<String, ColumnMapping>();

    Database hgcentral;

    static {
        ColumnMapping bed3Mapping = ColumnMapping.getIntervalMapping("chrom", "chromStart", "chromEnd", null);
        KNOWN_MAPPINGS.put("bed 3", bed3Mapping);
        KNOWN_MAPPINGS.put("bed 3 +", bed3Mapping);
        KNOWN_MAPPINGS.put("bed 3 .", bed3Mapping);
        KNOWN_MAPPINGS.put("bed .", bed3Mapping);

        ColumnMapping bed4Mapping = ColumnMapping.getIntervalMapping("chrom", "chromStart", "chromEnd", "name");
        KNOWN_MAPPINGS.put("bed 4", bed4Mapping);
        KNOWN_MAPPINGS.put("bed 4 +", bed4Mapping);
        KNOWN_MAPPINGS.put("bed 4 .", bed4Mapping);
        KNOWN_MAPPINGS.put("ctgPos", bed4Mapping);
        KNOWN_MAPPINGS.put("ld2", bed4Mapping);

        // Like bed 4, but we'll use the finished/draft/predraft column as the name field.
        ColumnMapping clonePosMapping = ColumnMapping.getIntervalMapping("chrom", "chromStart", "chromEnd", "stage");
        KNOWN_MAPPINGS.put("clonePos", clonePosMapping);

        // Our generic interval format lacks a score field, so treat these as Bed with no blocks or strand.
        ColumnMapping bed5Mapping = ColumnMapping.getBedMapping("chrom", "chromStart", "chromEnd", "name", "score", null, null, null, null, null, null, null, null);
        KNOWN_MAPPINGS.put("bed 5", bed5Mapping);
        KNOWN_MAPPINGS.put("bed 5 +", bed5Mapping);
        KNOWN_MAPPINGS.put("bed 5 .", bed5Mapping);
        KNOWN_MAPPINGS.put("bed5FloatScore", bed5Mapping);
        KNOWN_MAPPINGS.put("bed5FloatScoreWithFdr", bed5Mapping);

        // Our generic interval format lacks a score field, so treat these as Bed with no blocks.
        ColumnMapping bed6Mapping = ColumnMapping.getBedMapping("chrom", "chromStart", "chromEnd", "name", "score", "strand", null, null, null, null, null, null, null);
        KNOWN_MAPPINGS.put("bed 6", bed6Mapping);
        KNOWN_MAPPINGS.put("bed 6 +", bed6Mapping);
        KNOWN_MAPPINGS.put("bed 6 .", bed6Mapping);
        KNOWN_MAPPINGS.put("broadPeak", bed6Mapping);
        KNOWN_MAPPINGS.put("narrowPeak", bed6Mapping);

        // bed 8, like BED, but with no ItemRgb or blocks.
        ColumnMapping bed8Mapping = ColumnMapping.getBedMapping("chrom", "chromStart", "chromEnd", "name", "score", "strand", "thickStart", "thickEnd", null, null, null, null, null);
        KNOWN_MAPPINGS.put("bed 8", bed8Mapping);
        KNOWN_MAPPINGS.put("bed 8 +", bed8Mapping);
        KNOWN_MAPPINGS.put("bed 8 .", bed8Mapping);

        // bed 9, like BED, but with no blocks.  Note that in bed 9, the itemRGB column is usually called "reserved".
        ColumnMapping bed9Mapping = ColumnMapping.getBedMapping("chrom", "chromStart", "chromEnd", "name", "score", "strand", "thickStart", "thickEnd", "reserved", null, null, null, null);
        KNOWN_MAPPINGS.put("bed 9", bed9Mapping);
        KNOWN_MAPPINGS.put("bed 9 +", bed9Mapping);
        KNOWN_MAPPINGS.put("bed 9 .", bed9Mapping);

        ColumnMapping bed12Mapping = ColumnMapping.getBedMapping("chrom", "chromStart", "chromEnd", "name", "score", "strand", "thickStart", "thickEnd", "reserved", "chromStarts", null, null, "blockSizes");
        KNOWN_MAPPINGS.put("bed 12", bed12Mapping);
        KNOWN_MAPPINGS.put("bed 12 +", bed12Mapping);
        KNOWN_MAPPINGS.put("bed 12 .", bed12Mapping);
        KNOWN_MAPPINGS.put("expRatio", bed12Mapping);
        KNOWN_MAPPINGS.put("factorSource", bed12Mapping);
        KNOWN_MAPPINGS.put("coloredExon", bed12Mapping);    // Colour is actually stored on a per-block level, but we have no way of representing this.

        ColumnMapping geneMapping = ColumnMapping.getBedMapping("chrom", "txStart", "txEnd", "name", "score", "strand", "cdsStart", "cdsEnd", "reserved", "exonStarts", null, "exonEnds", null);
        KNOWN_MAPPINGS.put("genePred", geneMapping);

        // TODO: What to do with qStart and qEnd?
        ColumnMapping chainMapping = ColumnMapping.getIntervalMapping("tName", "tStart", "tEnd", "qName");
        KNOWN_MAPPINGS.put("chain", chainMapping);

        ColumnMapping netAlignMapping = ColumnMapping.getIntervalMapping("tName", "tStart", "tEnd", "qName");
        KNOWN_MAPPINGS.put("netAlign", netAlignMapping);

        ColumnMapping bedGraph4Mapping = ColumnMapping.getContinuousMapping("chrom", "chromStart", "chromEnd", "dataValue");
        KNOWN_MAPPINGS.put("bedGraph 4", bedGraph4Mapping);
        KNOWN_MAPPINGS.put("bedGraph 5", bedGraph4Mapping);

        //TODO: What do do with qStart, qEnd, and blocks.
        ColumnMapping pslMapping = ColumnMapping.getBedMapping("tName", "tStart", "tEnd", "qName", null, "strand", null, null, null, null, "tStarts", null, "blockSizes");
        KNOWN_MAPPINGS.put("psl", pslMapping);
        KNOWN_MAPPINGS.put("psl .", pslMapping);
        KNOWN_MAPPINGS.put("psl est", pslMapping);
        KNOWN_MAPPINGS.put("psl protein", pslMapping);
        KNOWN_MAPPINGS.put("psl xeno", pslMapping);

        ColumnMapping rmskMapping = ColumnMapping.getIntervalMapping("genoName", "genoStart", "genoEnd", "repName");
        KNOWN_MAPPINGS.put("rmsk", rmskMapping);

        ColumnMapping wigMapping = ColumnMapping.getWigMapping("chrom", "chromStart", "chromEnd", "span", "count", "offset", "file", "lowerLimit", "dataRange");
        KNOWN_MAPPINGS.put("wig", wigMapping);
    }

    @Override
    public void init(PluginAdapter pluginAdapter) {
        driverName = "com.mysql.jdbc.Driver";
        uri = URI.create("jdbc:mysql://genome-mysql.cse.ucsc.edu");
        userName = "genome";
        password = "";
        saveSettings();
    }


    @Override
    public boolean canOpen(URI uri) {
        return uri.getScheme().equals("jdbc") && uri.toString().contains("genome-mysql.cse.ucsc.edu");
    }

    /**
     * Logging into UCSC is always done silently, without a login dialog.
     *
     * @param silent ignored
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    @Override
    protected void tryToLogin() throws ClassNotFoundException, SQLException {
        getConnection();
    }

    /**
     * Instead of showing the base-class MappingDialog, the UCSC plugin builds up the list
     * of tables from the hgcentral database and displays a UCSC-specific dialog.
     *
     * @throws SQLException
     */
    @Override
    protected MappedTable requestMapping(Table t) throws SQLException {
        if (hgcentral == null) {
            hgcentral = new Database("hgcentral", uri, userName, password);
        }
        UCSCNavigationDialog dlg = new UCSCNavigationDialog(DialogUtils.getMainWindow(), this, t);
        dlg.setVisible(true);
        return dlg.getMapping();
    }

    @Override
    public String getTitle() {
        return "UCSC Datasource Plugin";
    }

    /**
     * Get a list of references for this data-source.  If there is a mapping for the chrom
     * field, use that.  If not, use the chromInfo table to determine the list of
     * chromosomes for this genome.  This is necessary to support certain UCSC tracks
     * where the data is spread over one chromosome per table.
     */
    @Override
    public Set<String> getReferences(MappedTable table) throws SQLException {
        return getReferences(table.getDatabase());
    }

    Set<String> getReferences(Database db) throws SQLException {
        Set<String> references = new HashSet<String>();
        ResultSet rs = db.executeQuery("SELECT chrom FROM chromInfo");
        while (rs.next()) {
            references.add(rs.getString(1));
        }
        return references;
    }

    @Override
    public MappedTable getTableByName(String tableName, String dbName, String trackName) throws SQLException {
        try {
            return super.getTableByName(tableName, dbName, trackName);
        } catch (SQLException sqlx) {
            // Must be one of our fake composite tables (e.g. "intronEst" when the actual MySQL tables are "chr1_intronEst" et al).
            // Substitute in an actual table (which one doesn't matter) so we can get column names and such.
            Database db = getDatabase(dbName);
            Table t = new Table(getReferences(db).iterator().next() + "_" + tableName, db);
            return new MappedTable(t, ColumnMapping.getSavedMapping(this, t.getColumns(), true), trackName);
        }
    }


    /**
     * Given a UCSC table format string, return the best mapping we have for it.
     *
     * @param type a value from the "type" column of hg19.trackDb (or equivalent)
     * @return a column mapping for that type, or null if none is known
     */
    static ColumnMapping getKnownMapping(String type) {
        ColumnMapping result = KNOWN_MAPPINGS.get(type);
        if (result == null) {
            if (type.startsWith("chain")) {
                result = KNOWN_MAPPINGS.get("chain");
            } else if (type.startsWith("genePred")) {
                result = KNOWN_MAPPINGS.get("genePred");
            } else if (type.startsWith("netAlign")) {
                result = KNOWN_MAPPINGS.get("netAlign");
            } else if (type.startsWith("wig ")) {  // Note trailing space on string, to avoid catching wigMaf tracks.
                result = KNOWN_MAPPINGS.get("wig");
            }
        }
        return result;
    }

}
