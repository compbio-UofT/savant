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
 * RangeController.java
 * Created on Jan 19, 2010
 */
/**
 * Controller object to manage changes to viewed range.
 * @author vwilliams
 */
package savant.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.event.frame.*;
import savant.view.swing.Frame;
import savant.view.swing.GraphPane;
import savant.view.swing.Savant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import savant.view.swing.ViewTrack;

public class FrameController {

    private static FrameController instance;

    private static Log log = LogFactory.getLog(FrameController.class);

    /** The maximum and current viewable range */
    private HashMap<GraphPane,JComponent> graphpane2dockable;
    private HashMap<GraphPane,Frame> graphpane2frame;

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
        graphpane2dockable = new HashMap<GraphPane,JComponent>();
        graphpane2frame = new HashMap<GraphPane,Frame>();
    }

    public void addFrame(Frame f, JComponent panel) {
        frames.add(f);
        graphpane2dockable.put(f.getGraphPane(), panel);
        graphpane2frame.put(f.getGraphPane(), f);

        //System.out.println("Drawing Frames");
        this.drawFrames(); // crucially important, prepares initial renderer
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
        Savant.log("Frames changed event being fired");
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

        for (Frame frame : frames) {
            try {
                // added to detect when rendering has completed
                GraphPaneController.getInstance().enlistRenderingGraphpane(frame.getGraphPane());

                frame.drawTracksInRange(ReferenceController.getInstance().getReferenceName(), rc.getRange());
            } catch (Exception e) {
                Savant.log("Error: could not draw frames (" + e.getMessage() + ")");
                if (frame == null) {
                    Savant.log("Frame is null");
                } else if (rc == null) {
                    Savant.log("RC is null");
                }
            }
        }
    }


    public void hideFrame(Frame frame) {
        JComponent jc = this.graphpane2dockable.get(frame.getGraphPane());
        frame.setHidden(true);
        fireFrameHiddenEvent(frame);
    }

    public void hideFrame(GraphPane graphpane) {
        hideFrame(this.graphpane2frame.get(graphpane));
    }

    public void showFrame(Frame frame) {
        JComponent jc = this.graphpane2dockable.get(frame.getGraphPane());
        frame.setHidden(false);
        fireFrameShownEvent(frame);
    }

    public void showFrame(GraphPane graphpane) {
        showFrame(this.graphpane2frame.get(graphpane));
    }

    public void closeFrame(GraphPane graphpane) {
        closeFrame(this.graphpane2frame.get(graphpane));
    }

    /**
    public void closeTrack(String trackname) {
        Frame frameTrackIsIn = null;
        ViewTrack track = null;
        for (Frame f : this.getFrames()) {
            for (ViewTrack t : f.getTracks()) {
                if (t.getName().equals(trackname)) {
                    frameTrackIsIn = f;
                    track = t;
                    break;
                }
            }
            if (frameTrackIsIn != null) { break; }
        }

        frameTrackIsIn.getTracks().remove(track);
        if (frameTrackIsIn.getTracks().size() == 0) {
            closeFrame(frameTrackIsIn);
        }
    }
     */

    public void closeFrame(Frame frame) {
        this.hideFrame(frame);

        ViewTrackController vtc = ViewTrackController.getInstance();
        for (ViewTrack t : frame.getTracks()) {
            vtc.removeTrack(t);
        }

        this.frames.remove(frame);
    }

    public List<Frame> getFrames() { return this.frames; }

}
