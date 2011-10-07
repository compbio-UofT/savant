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

package savant.data.event;

import savant.view.tracks.Track;


/**
 * Event class which allows asynchronous creation of tracks.
 *
 * @author tarkvara
 */
public class TrackCreationEvent {
    public enum Type {
        STARTED,
        COMPLETED,
        FAILED
    };
    Type type;
    Track[] tracks;
    String name;
    Throwable error;

    /**
     * Constructor for event which is fired as track-creation begins.
     */
    public TrackCreationEvent() {
        this.type = Type.STARTED;
    }

    /**
     * Constructor when tracks have been successfully created.
     *
     * @param tracks the tracks created
     * @param name the display name for this collection of tracks
     */
    public TrackCreationEvent(Track[] tracks, String name) {
        this.type = Type.COMPLETED;
        this.tracks = tracks;
        this.name = name;
    }

    /**
     * Constructor when track creation has failed.
     *
     * @param error
     */
    public TrackCreationEvent(Throwable error) {
        this.type = Type.FAILED;
        this.error = error;
    }

    public Track[] getTracks() {
        return tracks;
    }

    public String getName() {
        return name;
    }

    public Throwable getError() {
        return error;
    }
}
