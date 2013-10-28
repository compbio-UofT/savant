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
