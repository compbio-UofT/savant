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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.sql.SQLWorker;
import savant.sql.Table;

/**
 * SQL worker which is responsible for fetching the track groups for a given genome.
 *
 * @author tarkvara
 */
public abstract class GroupsFetcher extends SQLWorker<List<GroupDef>> {
    private static final Log LOG = LogFactory.getLog(UCSCNavigationDialog.class);
    
    final UCSCDataSourcePlugin plugin;
    final GenomeDef genome;
    private List<String> references = null;

    public GroupsFetcher(UCSCDataSourcePlugin plug, GenomeDef g) {
        super(String.format("Fetching tables for %s...", g.label), "Unable to fetch table list from UCSC.");
        plugin = plug;
        genome = g;
    }

    @Override
    public List<GroupDef> doInBackground() throws SQLException {
        LOG.debug("Starting GroupsFetcher");
        List<GroupDef> groups = new ArrayList<GroupDef>();
        List<String> unknownTracks = new ArrayList<String>();
        LOG.debug("Executing query");
        ResultSet rs = plugin.genomeDB.executeQuery("SELECT label,tableName,shortLabel,type FROM trackDb,grp WHERE trackDb.grp = grp.name ORDER BY grp.priority,trackDb.priority,trackDb.tableName");
        GroupDef lastGroup = null;
        LOG.debug("Looping over result set.");
        while (rs.next()) {
            String type = rs.getString("type");
            String track = rs.getString("tableName");
            String group = rs.getString("label");
            String label = rs.getString("shortLabel");
            TrackDef def = null;

            // Look through the database's tables for one representing this track.  It can be:
            // 1) an exact match for the track name
            // 2) equal to "all_" plus the track name
            // 3) equal to the chromosome name plus the track name (e.g. "chr1_rmsk" and friends)
            Table t = plugin.findTable(track);
            if (t != null) {
                if (UCSCDataSourcePlugin.getStandardMapping(type) != null) {
                    def = new TrackDef(track, t.getName(), label, type);
                } else {
                    LOG.debug("Track type " + type + " unmapped for table " + track);
                    continue;
                }
            }

            if (def != null) {
                if (lastGroup == null || !group.equals(lastGroup.name)) {
                    lastGroup = new GroupDef(group);
                    groups.add(lastGroup);
                }
                lastGroup.tracks.add(def);
            } else {
                if (track.contains("View")) {
                    // Not a real track.  May imply that the last unknown track is actually a composite, in which
                    // case we can get rid of it.
                    if (unknownTracks.size() > 0 && track.startsWith(unknownTracks.get(unknownTracks.size() - 1))) {
                        LOG.debug("Removing composite track " + unknownTracks.get(unknownTracks.size() - 1) + " on evidence from " + track);
                        unknownTracks.remove(unknownTracks.size() - 1);
                    }
                } else {
                    unknownTracks.add(track);
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            Collections.sort(unknownTracks);
            for (String s: unknownTracks) {
                LOG.debug("Unknown track " + s + " not found in " + plugin.genomeDB);
            }
        }
        LOG.debug("Returning " + groups.size() + " groups.");
        return groups;
    }
}
