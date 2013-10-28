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
