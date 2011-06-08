/*
 * FrameController.java
 * Created on Jan 19, 2010
 *
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

package savant.controller;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.event.FrameShownEvent;
import savant.controller.event.FrameChangedListener;
import savant.controller.event.FrameShownListener;
import savant.controller.event.FrameChangedEvent;
import savant.controller.event.FrameHiddenListener;
import savant.controller.event.FrameHiddenEvent;
import savant.view.swing.Frame;
import savant.view.swing.GraphPane;
import savant.view.swing.Track;


/**
 * Controller object to manage our collection of track frames.
 *
 * @author vwilliams
 */
public class FrameController {
    private static final Log LOG = LogFactory.getLog(FrameController.class);

    private static FrameController instance;

    /** The maximum and current viewable range */
    //private HashMap<GraphPane,JComponent> graphpane2dockable;
    //private HashMap<GraphPane,Frame> graphpane2frame;

    List<Frame> frames;

    private List frameHiddenListeners;
    private List frameShownListeners;
    private List frameChangedListeners;

    public static synchronized FrameController getInstance() {
        if (instance == null) {
            instance = new FrameController();
        }
        return instance;
    }

    private FrameController() {
        frames = new ArrayList<Frame>();
        frameHiddenListeners = new ArrayList();
        frameShownListeners = new ArrayList();
        frameChangedListeners = new ArrayList();
    }

    public void addFrame(Frame f, JComponent panel) {
        frames.add(f);

        drawFrames(); // crucially important, prepares initial renderer
        fireFrameChangedEvent(f);
    }

    /**
     * Fire the RangeChangedEvent
     */
    private synchronized void fireFrameHiddenEvent(Frame f) {
        FrameHiddenEvent evt = new FrameHiddenEvent(this, f);
        Iterator listeners = this.frameHiddenListeners.iterator();
        while (listeners.hasNext()) {
            ((FrameHiddenListener) listeners.next()).frameHiddenReceived(evt);
        }
        fireFrameChangedEvent(f);
    }

    private synchronized void fireFrameShownEvent(Frame f) {
        FrameShownEvent evt = new FrameShownEvent(this, f);
        Iterator listeners = this.frameShownListeners.iterator();
        while (listeners.hasNext()) {
            ((FrameShownListener) listeners.next()).frameShownReceived(evt);
        }
        fireFrameChangedEvent(f);
    }

    private synchronized void fireFrameChangedEvent(Frame f) {
        LOG.info("Frames changed event being fired");
        FrameChangedEvent evt = new FrameChangedEvent(this, f);
        Iterator listeners = this.frameChangedListeners.iterator();
        while (listeners.hasNext()) {
            ((FrameChangedListener) listeners.next()).frameChangedReceived(evt);
        }
    }

    public synchronized void addFrameHiddenListener(FrameHiddenListener l) {
        frameHiddenListeners.add(l);
    }

    public synchronized void removeFrameHiddenListener(FrameHiddenListener l) {
        frameHiddenListeners.remove(l);
    }

    public synchronized void addFrameShownListener(FrameShownListener l) {
        frameShownListeners.add(l);
    }

    public synchronized void removeFrameShownListener(FrameShownListener l) {
        frameShownListeners.remove(l);
    }

    public synchronized void addFrameChangedListener(FrameChangedListener l) {
        frameChangedListeners.add(l);
    }

    public synchronized void removeFrameChangedListener(FrameChangedListener l) {
        frameChangedListeners.remove(l);
    }


    /**
     * Draw the frames in the current viewable range
     */
    public void drawFrames() {
        RangeController rc = RangeController.getInstance();

        GraphPaneController.getInstance().clearRenderingList();

        for (Frame f : frames) {
            if (f.getTracks() != null) {
                // added to detect when rendering has completed
                GraphPaneController.getInstance().enlistRenderingGraphpane(f.getGraphPane());

                f.drawTracksInRange(ReferenceController.getInstance().getReferenceName(), rc.getRange());
            }
        }
    }

    public void closeFrameAndTrack(Frame frame) {

        try {
            TrackController tc = TrackController.getInstance();
            for (Track t : frame.getTracks()) {
                tc.removeTrackHelper(t);
            }
            this.frames.remove(frame);
            frame.getDockingManager().removeFrame(frame.getKey());


        } catch (Exception e) {
            LOG.error("Error closing frame.", e);
        }

    }

    public List<Frame> getFrames() { return this.frames; }

    protected Frame getFrameForTrack(Track t) {
        for (Frame f : frames) {
            for (Track t0 : f.getTracks()) {
                if (t == t0) { return f; }
            }
        }
        return null;
    }
}
