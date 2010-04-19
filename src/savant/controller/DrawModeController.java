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
 * DrawModeController.java
 * Created on Apr 15, 2010
 */

package savant.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.event.drawmode.DrawModeChangedEvent;
import savant.controller.event.drawmode.DrawModeChangedListener;
import savant.model.view.Mode;
import savant.view.swing.ViewTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller to manage switching view track draw modes and alerting listeners.
 *
 * @author vwilliams
 */
public class DrawModeController {

    private static Log log = LogFactory.getLog(DrawModeController.class);

    private static DrawModeController instance;

    private List<DrawModeChangedListener> listeners;

    public static DrawModeController getInstance() {
        if (instance == null) {
            instance = new DrawModeController();
        }
        return instance;
    }

    public void addDrawModeChangedListener(DrawModeChangedListener listener) {
        listeners.add(listener);
    }

    public void removeDrawModeChangedListener(DrawModeChangedListener listener) {
        listeners.remove(listener);
    }

    public void switchMode(ViewTrack track, Mode mode) {
        track.setDrawMode(mode);
        DrawModeChangedEvent evt = new DrawModeChangedEvent(track, mode);
        fireDrawModeChangedEvent(evt);

    }
    
    private synchronized void fireDrawModeChangedEvent(DrawModeChangedEvent evt) {
        for (DrawModeChangedListener listener : listeners) {
            listener.drawModeChangeReceived(evt);
        }
    }
    
    private DrawModeController() {
        listeners = new ArrayList<DrawModeChangedListener>();
    }


}
