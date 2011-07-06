/*
 *    Copyright 2010-2011 University of Toronto
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

import savant.util.DrawingMode;
import savant.view.swing.Track;

/**
 * Event to signify that a view track has changed its drawing mode.
 * 
 * @author vwilliams
 */
public class DrawingModeChangedEvent {

    private Track track;
    private DrawingMode mode;

    public DrawingModeChangedEvent(Track track, DrawingMode mode) {
        this.track = track;
        this.mode = mode;

    }

    public Track getTrack() {
        return track;
    }

    public DrawingMode getMode() {
        return mode;
    }
}
