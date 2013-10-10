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
package savant.controller;

import java.awt.Component;
import java.beans.PropertyVetoException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import com.jidesoft.docking.DockingManager.FrameHandle;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ut.biolab.savant.analytics.savantanalytics.AnalyticsAgent;

import savant.api.data.DataFormat;
import savant.api.event.LocationChangedEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Listener;
import savant.selection.SelectionController;
import savant.util.DrawingMode;
import savant.util.NetworkUtils;
import savant.view.swing.DockableFrameFactory;
import savant.view.swing.Frame;
import savant.view.swing.Savant;
import savant.view.tracks.Track;
import savant.view.tracks.TrackFactory;

/**
 * Controller object to manage our collection of track frames.
 *
 * @author vwilliams
 */
public class FrameController {

    private static final Log LOG = LogFactory.getLog(FrameController.class);
    private static FrameController instance;
    private static LocationController locationController = LocationController.getInstance();
    List<Frame> frames;

    public static synchronized FrameController getInstance() {
        if (instance == null) {
            instance = new FrameController();
        }
        return instance;
    }

    private FrameController() {
        frames = new ArrayList<Frame>();
        locationController.addListener(new Listener<LocationChangedEvent>() {
            /**
             * Listen for RangeChangedEvents, which will cause all the frames to
             * be drawn. This code used to be in Savant.java.
             */
            @Override
            public void handleEvent(LocationChangedEvent event) {
                drawFrames();
            }
        });

        Savant.getInstance().getTrackDockingManager().addDockableFrameListener(new TrackFrameAdapter());
        Savant.getInstance().getAuxDockingManager().addDockableFrameListener(new TrackFrameAdapter());
    }

    public void closeAllFrames(boolean askFirst) {
        if (askFirst) {
            if (DialogUtils.askYesNo("Confirm", "Are you sure you want to close all tracks?") == DialogUtils.NO) {
                return;
            }
        }

        for (int i = frames.size() - 1; i >= 0; i--) {
            closeFrame(frames.get(i), false);
        }
    }

    public void closeFrame(Frame frame, boolean askFirst) {
        if (!frame.isCloseable()) {
            return;
        }
        if (askFirst) {
            if (DialogUtils.askYesNo("Confirm", "Are you sure you want to close this track?") == DialogUtils.NO) {
                return;
            }
        }
        frame.getDockingManager().removeFrame(frame.getName());

        try {
            for (Track t : frame.getTracks()) {
                AnalyticsAgent.log(
                        new NameValuePair[]{
                            new NameValuePair("track-event", "closed"),
                            new NameValuePair("track-type", t.getClass().getSimpleName())
                        });
            }
        } catch (Exception e) {
        }
    }

    /**
     * Create a frame for the given tracks. Currently only used when creating a
     * frame for a track which has been created by a DataSource plugin.
     *
     * FIXME: Can we use the normal track-creation code?
     */
    public Frame createFrame(Track[] tracks) {
        DataFormat df = tracks[0].getDataFormat();
        Frame frame = DockableFrameFactory.createTrackFrame(df);
        frame.setKey(tracks[0].getName());
        addFrame(frame, df);
        frame.setTracks(tracks);
        return frame;
    }

    public Frame addTrackFromPath(String fileOrURI, DataFormat df, DrawingMode dm) {
        if (df == null) {
            if (fileOrURI.endsWith(".fa") || fileOrURI.endsWith(".fa.savant")) {
                df = DataFormat.SEQUENCE;
            } else if (fileOrURI.endsWith(".vcf.gz")) {
                df = DataFormat.VARIANT;
            }
        }
        return addTrackFromURI(NetworkUtils.getURIFromPath(fileOrURI), df, dm);
    }

    public Frame addTrackFromURI(URI uri, DataFormat df, DrawingMode dm) {
        Frame frame = DockableFrameFactory.createTrackFrame(df);
        //Force a unique frame key. The title of frame is overwritten by track name later.
        frame.setKey(uri.toString() + System.nanoTime());
        frame.setInitialDrawingMode(dm);
        addFrame(frame, df);
        TrackFactory.createTrack(uri, frame);
        return frame;
    }

    private void addFrame(Frame f, DataFormat df) {

        frames.add(f);

        DockingManager dm = Savant.getInstance().getTrackDockingManager();
        dm.addFrame(f);

        // Insert the frame immediately below the currently-active frame.
        if (frames.size() > 1) {
            FrameHandle lastFrame = getFrontmostFrame(dm);
            dm.moveFrame(f.getKey(), lastFrame.getKey(), DockContext.DOCK_SIDE_SOUTH);
        }
    }

    /**
     * Determine which one of our frames is the frontmost. This will be used to
     * determine the insertion position for the next frame.
     */
    private FrameHandle getFrontmostFrame(DockingManager dm) {
        for (FrameHandle h : dm.getOrderedFrames()) {
            if (!h.getKey().startsWith("#")) {
                return h;
            }
        }
        return null;
    }

    public Frame getActiveFrame() {
        DockingManager dm = Savant.getInstance().getTrackDockingManager();
        FrameHandle fh = getFrontmostFrame(dm);
        return fh != null ? (Frame) dm.getFrame(fh.getKey()) : null;
    }

    /**
     * Draw the frames in the current viewable range
     */
    public void drawFrames() {
        GraphPaneController.getInstance().clearRenderingList();

        for (Frame f : frames) {
            if (f.getTracks() != null) {
                // added to detect when rendering has completed
                GraphPaneController.getInstance().enlistRenderingGraphpane(f.getGraphPane());
                f.drawTracksInRange(locationController.getReferenceName(), locationController.getRange());
            }
        }
    }

    public void hideFrame(Frame frame) {
        try {
            frame.setHidden(true);
        } catch (PropertyVetoException ignored) {
        }
    }

    public void showFrame(Frame frame) {
        try {
            frame.setHidden(false);
        } catch (PropertyVetoException ignored) {
        }
    }

    public List<Frame> getFrames() {
        return frames;
    }

    /**
     * Returns the Frames ordered by their on-screen locations.
     */
    public Frame[] getOrderedFrames() {
        Frame[] result = frames.toArray(new Frame[0]);
        Arrays.sort(result, new Comparator<Component>() {
            @Override
            public int compare(Component t, Component t1) {
                if (t == null) {
                    return t1 == null ? 0 : 1;
                } else if (t1 == null) {
                    return -1;
                }
                int result = t.getY() - t1.getY();
                if (result == 0) {
                    result = t.getX() - t1.getX();
                }
                if (result == 0) {
                    result = compare(t.getParent(), t1.getParent());
                }
                return result;
            }
        });
        return result;
    }

    /**
     * Listens for track frames being added. Either ordinary ones to the
     * TrackDockingManager, or Variant frames to the AuxDockingManager.
     */
    private class TrackFrameAdapter extends DockableFrameAdapter {

        /**
         * The appearance of the first frame is a good opportunity to clear away
         * the start page and set up the main window's navigation widgets.
         */
        @Override
        public void dockableFrameAdded(DockableFrameEvent evt) {
            if (evt.getDockableFrame() instanceof Frame) {
                Savant.getInstance().showBrowserControls();
            }
        }

        @Override
        public void dockableFrameRemoved(DockableFrameEvent evt) {
            DockableFrame df = evt.getDockableFrame();
            if (df instanceof Frame) {
                Frame f = (Frame) df;
                hideFrame(f);

                try {

                    TrackController vtc = TrackController.getInstance();
                    SelectionController sc = SelectionController.getInstance();
                    LOG.info("Closing " + f + " with " + f.getTracks().length + " tracks.");
                    for (Track t : f.getTracks()) {
                        vtc.removeTrack(t);
                        sc.removeAll(t.getName());
                    }
                    frames.remove(f);

                } catch (Throwable x) {
                    LOG.error("Error closing frame.", x);
                }
            }
        }

        @Override
        public void dockableFrameActivated(DockableFrameEvent evt) {
            DockableFrame df = evt.getDockableFrame();
            if (df instanceof Frame) {
                ((Frame) df).setActiveFrame(true);

            }
        }

        @Override
        public void dockableFrameDeactivated(DockableFrameEvent evt) {
            DockableFrame df = evt.getDockableFrame();
            if (df instanceof Frame) {
                ((Frame) df).setActiveFrame(false);
            }
        }
    }
}
