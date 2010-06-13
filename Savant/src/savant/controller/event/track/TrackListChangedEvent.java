/*
 *    Copyright 2010 University of Toronto
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

/*
 * TrackListChangedEvent.java
 * Created on Mar 11, 2010
 */

package savant.controller.event.track;

import savant.model.data.Track;

import java.util.EventObject;
import java.util.List;

public class TrackListChangedEvent extends EventObject {

    private List<Track> tracks;

    public TrackListChangedEvent(Object source, List<Track> tracks) {
        super(source);
        this.tracks = tracks;
    }

    public List<Track> getTracks() {
        return this.tracks;
    }
}
