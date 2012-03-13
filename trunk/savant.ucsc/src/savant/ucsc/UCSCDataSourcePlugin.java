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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import savant.api.util.DialogUtils;
import savant.api.util.GenomeUtils;
import savant.api.util.SettingsUtils;
import savant.sql.ColumnMapping;
import savant.sql.Database;
import savant.sql.MappedTable;
import savant.sql.SQLConstants;
import savant.sql.SQLDataSourcePlugin;
import savant.sql.Table;
import savant.util.ReferenceComparator;


/**
 * A version of the SQL data-source plugin which is configured to work nicely with
 * the UCSC database.
 *
 * @author tarkvara
 */
public class UCSCDataSourcePlugin extends SQLDataSourcePlugin implements SQLConstants {
    private static final Map<String, ColumnMapping> STANDARD_MAPPINGS = new HashMap<String, ColumnMapping>();
    
    public static final String[] STANDARD_CLADES = new String[] { "Mammal", "Vertebrate", "Deuterostome", "Insect", "Nematode", "Other" };

    /** Database of the currently-selected genome. */
    Database genomeDB = null;

    /** Directory of which genomes are available for each clade. */
    Map<String, List<GenomeDef>> cladeGenomeMap = new HashMap<String, List<GenomeDef>>();

    static {
        ColumnMapping bed3Mapping = ColumnMapping.getIntervalMapping("chrom", "chromStart", "chromEnd", null);
        STANDARD_MAPPINGS.put("bed 3", bed3Mapping);
        STANDARD_MAPPINGS.put("bed 3 +", bed3Mapping);
        STANDARD_MAPPINGS.put("bed 3 .", bed3Mapping);
        STANDARD_MAPPINGS.put("bed .", bed3Mapping);

        ColumnMapping bed4Mapping = ColumnMapping.getIntervalMapping("chrom", "chromStart", "chromEnd", "name");
        STANDARD_MAPPINGS.put("bed 4", bed4Mapping);
        STANDARD_MAPPINGS.put("bed 4 +", bed4Mapping);
        STANDARD_MAPPINGS.put("bed 4 .", bed4Mapping);
        STANDARD_MAPPINGS.put("ctgPos", bed4Mapping);
        STANDARD_MAPPINGS.put("ld2", bed4Mapping);

        // Like bed 4, but we'll use the finished/draft/predraft column as the name field.
        ColumnMapping clonePosMapping = ColumnMapping.getIntervalMapping("chrom", "chromStart", "chromEnd", "stage");
        STANDARD_MAPPINGS.put("clonePos", clonePosMapping);

        // Our generic interval format lacks a score field, so treat these as rich intervals with no blocks or strand.
        ColumnMapping bed5Mapping = ColumnMapping.getRichIntervalMapping("chrom", "chromStart", "chromEnd", "name", "score", null, null, null, null, null, null, null, null, null);
        STANDARD_MAPPINGS.put("bed 5", bed5Mapping);
        STANDARD_MAPPINGS.put("bed 5 +", bed5Mapping);
        STANDARD_MAPPINGS.put("bed 5 .", bed5Mapping);
        STANDARD_MAPPINGS.put("bed5FloatScore", bed5Mapping);
        STANDARD_MAPPINGS.put("bed5FloatScoreWithFdr", bed5Mapping);

        // Our generic interval format lacks a score field, so treat these as rich intervals with no blocks.
        ColumnMapping bed6Mapping = ColumnMapping.getRichIntervalMapping("chrom", "chromStart", "chromEnd", "name", "score", "strand", null, null, null, null, null, null, null, null);
        STANDARD_MAPPINGS.put("bed 6", bed6Mapping);
        STANDARD_MAPPINGS.put("bed 6 +", bed6Mapping);
        STANDARD_MAPPINGS.put("bed 6 .", bed6Mapping);
        STANDARD_MAPPINGS.put("broadPeak", bed6Mapping);
        STANDARD_MAPPINGS.put("narrowPeak", bed6Mapping);
        STANDARD_MAPPINGS.put("peptideMapping", bed6Mapping);   

        // bed 8, like Bed, but with no ItemRgb or blocks.
        ColumnMapping bed8Mapping = ColumnMapping.getRichIntervalMapping("chrom", "chromStart", "chromEnd", "name", "score", "strand", "thickStart", "thickEnd", null, null, null, null, null, null);
        STANDARD_MAPPINGS.put("bed 8", bed8Mapping);
        STANDARD_MAPPINGS.put("bed 8 +", bed8Mapping);
        STANDARD_MAPPINGS.put("bed 8 .", bed8Mapping);
        STANDARD_MAPPINGS.put("gvf", bed8Mapping);      // bed 8 with extra attribute columns, used for ISCA data

        // bed 9, like Bed, but with no blocks.  Note that in bed 9, the itemRGB column is usually called "reserved".
        ColumnMapping bed9Mapping = ColumnMapping.getRichIntervalMapping("chrom", "chromStart", "chromEnd", "name", "score", "strand", "thickStart", "thickEnd", "reserved", null, null, null, null, null);
        STANDARD_MAPPINGS.put("bed 9", bed9Mapping);
        STANDARD_MAPPINGS.put("bed 9 +", bed9Mapping);
        STANDARD_MAPPINGS.put("bed 9 .", bed9Mapping);
        STANDARD_MAPPINGS.put("bed 10", bed9Mapping);   // bed 9 with useless "blockCount" column

        ColumnMapping bed12Mapping = ColumnMapping.getRichIntervalMapping("chrom", "chromStart", "chromEnd", "name", "score", "strand", "thickStart", "thickEnd", "reserved", "chromStarts", null, null, "blockSizes", null);
        STANDARD_MAPPINGS.put("bed 12", bed12Mapping);
        STANDARD_MAPPINGS.put("bed 12 +", bed12Mapping);
        STANDARD_MAPPINGS.put("bed 12 .", bed12Mapping);
        STANDARD_MAPPINGS.put("expRatio", bed12Mapping);
        STANDARD_MAPPINGS.put("factorSource", bed12Mapping);
        STANDARD_MAPPINGS.put("coloredExon", bed12Mapping);    // Colour is actually stored on a per-block level, but we have no way of representing this.

        // knownGene table has a proteinID column instead of a name2 column.
        ColumnMapping knownGeneMapping = ColumnMapping.getRichIntervalMapping("chrom", "txStart", "txEnd", "name", "score", "strand", "cdsStart", "cdsEnd", "reserved", null, "exonStarts", "exonEnds", null, "proteinID");
        STANDARD_MAPPINGS.put("genePred knownGenePep knownGeneMrna", knownGeneMapping);

        // Most genes have a name2 column.
        ColumnMapping geneMapping = ColumnMapping.getRichIntervalMapping("chrom", "txStart", "txEnd", "name", "score", "strand", "cdsStart", "cdsEnd", "reserved", null, "exonStarts", "exonEnds", null, "name2");
        STANDARD_MAPPINGS.put("genePred", geneMapping);

        // TODO: What to do with qStart and qEnd?
        ColumnMapping chainMapping = ColumnMapping.getIntervalMapping("tName", "tStart", "tEnd", "qName");
        STANDARD_MAPPINGS.put("chain", chainMapping);

        ColumnMapping netAlignMapping = ColumnMapping.getIntervalMapping("tName", "tStart", "tEnd", "qName");
        STANDARD_MAPPINGS.put("netAlign", netAlignMapping);

        ColumnMapping bedGraph4Mapping = ColumnMapping.getContinuousMapping("chrom", "chromStart", "chromEnd", "dataValue");
        STANDARD_MAPPINGS.put("bedGraph 4", bedGraph4Mapping);
        STANDARD_MAPPINGS.put("bedGraph 5", bedGraph4Mapping);

        //TODO: What do do with qStart, qEnd, and blocks.
        ColumnMapping pslMapping = ColumnMapping.getRichIntervalMapping("tName", "tStart", "tEnd", "qName", null, "strand", null, null, null, null, "tStarts", null, "blockSizes", null);
        STANDARD_MAPPINGS.put("psl", pslMapping);
        STANDARD_MAPPINGS.put("psl .", pslMapping);
        STANDARD_MAPPINGS.put("psl est", pslMapping);
        STANDARD_MAPPINGS.put("psl protein", pslMapping);
        STANDARD_MAPPINGS.put("psl xeno", pslMapping);

        ColumnMapping rmskMapping = ColumnMapping.getIntervalMapping("genoName", "genoStart", "genoEnd", "repName");
        STANDARD_MAPPINGS.put("rmsk", rmskMapping);

        ColumnMapping wigMapping = ColumnMapping.getWigMapping("chrom", "chromStart", "chromEnd", "span", "count", "offset", "file", "lowerLimit", "dataRange");
        STANDARD_MAPPINGS.put("wig", wigMapping);

        ColumnMapping externalFileMapping = ColumnMapping.getExternalFileMapping("fileName");
        STANDARD_MAPPINGS.put("bigWig", externalFileMapping);
        STANDARD_MAPPINGS.put("bam", externalFileMapping);
    }

    @Override
    public void init() {
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
    protected void tryToLogin(boolean silent) throws ClassNotFoundException, SQLException {
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
    public List<String> getReferences(MappedTable table) throws SQLException {
        return getReferences(table.getDatabase());
    }

    List<String> getReferences(Database db) throws SQLException {
        List<String> result = new ArrayList<String>();
        ResultSet rs = db.executeQuery("SELECT chrom FROM chromInfo");
        while (rs.next()) {
            result.add(rs.getString(1));
        }
        rs.close();
        Collections.sort(result, new ReferenceComparator());
        return result;
    }

    /**
     * Get a table with the columns mapped based on saved settings.  In theory, the mappings
     * will have been saved during some earlier visit to the UCSC Navigation Dialog.
     */
    @Override
    public MappedTable getTableByName(String tableName, String dbName, String trackName) throws SQLException {
        try {
            return super.getTableByName(tableName, dbName, trackName);
        } catch (SQLException sqlx) {
            // It's either a renamed table (e.g. "all_est") or one of UCSC's fake composite tables (e.g. "intronEst" when the actual MySQL tables
            // are "chr1_intronEst" et al). Substitute in an actual table (which one doesn't matter) so we can get column names and such.
            Table t = findTable(tableName);
            boolean oneTablePerChromosome = !t.getName().startsWith("all_");
            return new MappedTable(t, ColumnMapping.getSavedMapping(this, t.getColumns(), oneTablePerChromosome), trackName);
        }
    }

    /**
     * Look through the database's tables for one representing this track.  It can be:
     * 1) an exact match for the track name
     * 2) equal to "all_" plus the track name
     * 3) equal to the chromosome name plus the track name (e.g. "chr1_rmsk" and friends)
     *
     * Intererstingly enough, since the initial development of this plugin, UCSC seems to have replaced all occurrences of case 3) with case 2).
     * Nonetheless, the code for case 3) remains here just in case UCSC missed a few tracks.
     */
    public Table findTable(String track) throws SQLException {
        Table t = genomeDB.findTable(track);
        if (t == null) {
            t = genomeDB.findTable("all_" + track);
            if (t == null) {
                t = genomeDB.findTable(getReferences(genomeDB).iterator().next() + "_" + track);
            }
        }
        return t;
    }

    /**
     * Get a set of mappings for a track which has <b>not</b> been configured using the navigation dialog.
     * This is used by the UCSC chooser dialog to avoid the mapping dialog as much as possible.
     *
     * @param t the track for which we're getting the mapping
     * @return a table with associated (possibly null) column mappings
     */
    public MappedTable getTableWithStandardMapping(TrackDef t) {
        return new MappedTable(new Table(t.getTableName(), genomeDB), getStandardMapping(t.type), t.track);
    }

    /**
     * Given a UCSC table format string, return the best mapping we have for it.
     *
     * @param type a value from the "type" column of hg19.trackDb (or equivalent)
     * @return a column mapping for that type, or null if none is known
     */
    static ColumnMapping getStandardMapping(String type) {
        ColumnMapping result = STANDARD_MAPPINGS.get(type);
        if (result == null) {
            if (type.startsWith("bigWig")) {
                result = STANDARD_MAPPINGS.get("bigWig");
            } else if (type.startsWith("chain")) {
                result = STANDARD_MAPPINGS.get("chain");
            } else if (type.startsWith("genePred")) {
                result = STANDARD_MAPPINGS.get("genePred");
            } else if (type.startsWith("netAlign")) {
                result = STANDARD_MAPPINGS.get("netAlign");
            } else if (type.startsWith("wig ")) {  // Note trailing space on string, to avoid catching wigMaf tracks.
                result = STANDARD_MAPPINGS.get("wig");
            }
        }
        return result;
    }

    /**
     * Get all the genomes which are available for the given clade.
     * @param clade one of the six standard clades
     * @return 
     */
    public GenomeDef[] getCladeGenomes(String clade) {
        return cladeGenomeMap.get(clade).toArray(new GenomeDef[0]);
    }

    /**
     * Figure out which clade contains the given genome.
     * @param g
     * @return 
     */
    public String findCladeForGenome(GenomeDef g) {
        for (String c: cladeGenomeMap.keySet()) {
            if (cladeGenomeMap.get(c).contains(g)) {
                return c;
            }
        }
        // Genome not found in any of the known clades.  How did it get here?
        return null;
    }

    public GenomeDef getCurrentGenome(String clade) {
        List<GenomeDef> genomes = cladeGenomeMap.get(clade);
        for (GenomeDef g: genomes) {
            if (g.database.equals(genomeDB.getName())) {
                return g;
            }
        }
        // genomeDB not found in this clade.  Default to the first one.
        return genomes.get(0);
    }

    
    private boolean isKnownDatabase(String name) {
        try {
            List<Database> databases = getDatabases();
            for (Database db: databases) {
                if (db.getName().equals(name)) {
                    return true;
                }
            }
            LOG.info(name + " was not a known UCSC database.");
        } catch (SQLException sqlx) {
            LOG.error("Unable to retrieve database list.", sqlx);
        }
        return false;
    }

    /**
     * Select the current genome database based on the given table.  If the table is null,
     * use the current genome.  If the genome is null, look for "GENOME" in the settings.
     * If that is null, use "hg18".
     * @param t 
     */
    public void selectGenomeDB(Table t) {
        if (t != null) {
            genomeDB = t.getDatabase();
        } else {
            String genomeName = null;
            if (GenomeUtils.isGenomeLoaded()) {
                genomeName = GenomeUtils.getGenome().getName();
                // If we're loaded from a Fasta file, we probably want to trim off the ".fa.savant" from the genome name.
                int extPos = genomeName.indexOf(".fa");
                if (extPos > 0) {
                    genomeName = genomeName.substring(0, extPos);
                }
                if (!isKnownDatabase(genomeName)) {
                    genomeName = null;
                }
            }
            if (genomeName == null) {
                genomeName = SettingsUtils.getString(this, "GENOME");
                if (genomeName == null) {
                    genomeName = "hg18";
                }
            }
            genomeDB = getDatabase(genomeName);
        }
    }
}
