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

package savant.controller.event;

import savant.view.swing.Track;


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

    public Track getTrack() {
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

