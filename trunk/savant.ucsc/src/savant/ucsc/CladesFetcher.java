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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import savant.sql.Database;
import savant.sql.SQLWorker;


/**
 * Worker which fetches information about the clades available in the UCSC database.
 *
 * @author tarkvara
 */
public abstract class CladesFetcher extends SQLWorker<String> {
    private final UCSCDataSourcePlugin plugin;

    public CladesFetcher(UCSCDataSourcePlugin plug) {
        super("Fetching database list...", "Unable to fetch database list from UCSC.");
        this.plugin = plug;
    }

    @Override
    public String doInBackground() throws SQLException {
        Database hgCentral = null;
        try {
            hgCentral = plugin.getDatabase("hgcentral");
            ResultSet rs = hgCentral.executeQuery("SELECT DISTINCT name,description,genome,clade FROM dbDb NATURAL JOIN genomeClade WHERE active='1' ORDER by clade,orderKey");

            String lastClade = null;
            String selectedClade = null;
            List<GenomeDef> cladeGenomes = new ArrayList<GenomeDef>();
            while (rs.next()) {
                String dbName = rs.getString("name");
                GenomeDef genome = new GenomeDef(dbName, rs.getString("genome") + " - " + rs.getString("description"));

                // In the database, clades are stored in lowercase, and Nematodes are called worms.
                String clade = rs.getString("clade");
                if (clade.equals("worm")) {
                    clade = "Nematode";
                } else {
                    clade = Character.toUpperCase(clade.charAt(0)) + clade.substring(1);
                }
                if (plugin.genomeDB != null && dbName.equals(plugin.genomeDB.getName())) {
                    selectedClade = clade;
                }

                if (!clade.equals(lastClade)) {
                    if (lastClade != null) {
                        plugin.cladeGenomeMap.put(lastClade, cladeGenomes);
                        cladeGenomes = new ArrayList<GenomeDef>();
                    }
                    lastClade = clade;
                }
                cladeGenomes.add(genome);
            }
            plugin.cladeGenomeMap.put(lastClade, cladeGenomes);
            return selectedClade;
        } finally {
            if (hgCentral != null) {
                hgCentral.closeConnection();
            }
        }
    }
}
