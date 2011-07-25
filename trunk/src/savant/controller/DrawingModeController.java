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

package savant.controller;

import savant.controller.event.DrawingModeChangedEvent;
import savant.controller.event.DrawingModeChangedListener;
import savant.view.swing.Track;

import java.util.ArrayList;
import java.util.List;
import savant.util.DrawingMode;

/**
 * Controller to manage switching view track draw modes and alerting listeners.
 *
 * @author vwilliams
 */
public class DrawingModeController {

    private static DrawingModeController instance;

    private List<DrawingModeChangedListener> listeners;

    public static synchronized DrawingModeController getInstance() {
        if (instance == null) {
            instance = new DrawingModeController();
        }
        return instance;
    }

    public void addDrawingModeChangedListener(DrawingModeChangedListener listener) {
        listeners.add(listener);
    }

    public void removeDrawingModeChangedListener(DrawingModeChangedListener listener) {
        listeners.remove(listener);
    }

    public void switchMode(Track track, DrawingMode mode) {
        track.setDrawingMode(mode);
        DrawingModeChangedEvent evt = new DrawingModeChangedEvent(track, mode);
        track.getFrame().drawModeChanged(evt);
    }
    
    private synchronized void fireDrawingModeChanged(DrawingModeChangedEvent evt) {
        for (DrawingModeChangedListener listener : listeners) {
            listener.drawModeChanged(evt);
        }
    }
    
    private DrawingModeController() {
        listeners = new ArrayList<DrawingModeChangedListener>();
    }
}
