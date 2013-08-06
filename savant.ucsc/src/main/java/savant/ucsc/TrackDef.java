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


/**
 * Class which keeps track of information about a single track within the UCSC database.
 *
 * @author tarkvara
 */
public class TrackDef {
    final String track;   // Name of track as seen by user (e.g. refGene, mrna, est).
    final String table;   // Only occasionally different from track (e.g. all_mrna, chr1_est).
    final String label;
    final String type;

    TrackDef(String track, String table, String label, String type) {
        this.track = track;
        this.table = table;
        this.label = label;
        this.type = type;
    }

    @Override
    public String toString() {
        return track + " - " + label;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof TrackDef) {
            TrackDef other = (TrackDef)o;
            return track.equals(other.track);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (this.track != null ? this.track.hashCode() : 0);
        return hash;
    }

    public String getTableName() {
        return table;
    }
    
    public String getTrackName() {
        return track;
    }
}
