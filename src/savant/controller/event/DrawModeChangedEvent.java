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
 * DrawModeChangedEvent.java
 * Created on Apr 14, 2010
 */

package savant.controller.event;

import savant.util.Mode;
import savant.view.swing.ViewTrack;

/**
 * Event to signify that a view track has changed its drawing mode.
 * 
 * @author vwilliams
 */
public class DrawModeChangedEvent {

    private ViewTrack viewTrack;
    private Mode mode;

    public DrawModeChangedEvent(ViewTrack viewTrack, Mode mode) {
        this.viewTrack = viewTrack;
        this.mode = mode;

    }

    public ViewTrack getViewTrack() {
        return viewTrack;
    }

    public Mode getMode() {
        return mode;
    }
}
