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
package savant.api.event;

import savant.api.adapter.TrackAdapter;
import savant.view.tracks.Track;


/**
 * New class which is used for indicating changes to the state of a single track.
 *
 * @author tarkvara
 * @since 1.6.0
 */
public class TrackEvent {

    private final Type type;
    private final Track track;

    public TrackEvent(Type t, Track tr) {
        type = t;
        track = tr;
    }

    public TrackAdapter getTrack() {
        return track;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        ADDED,
        REMOVED
    }
}

