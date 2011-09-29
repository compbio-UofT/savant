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
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockingManager;
import com.jidesoft.docking.DockingManager.FrameHandle;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangedListener;
import savant.file.DataFormat;
import savant.view.swing.DockableFrameFactory;
import savant.view.swing.Frame;
import savant.view.swing.Savant;
import savant.view.swing.Track;
import savant.view.swing.TrackFactory;


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
        locationController.addLocationChangedListener(new LocationChangedListener() {
            /**
             * Listen for RangeChangedEvents, which will cause all the frames to be drawn.
             * This code used to be in Savant.java.
             */

            @Override
            public void locationChanged(LocationChangedEvent event) {
                drawFrames();
            }
        });

        Savant.getInstance().getTrackDockingManager().addDockableFrameListener(new DockableFrameAdapter() {
            /**
             * The appearance of the first frame is a good opportunity to clear away
             * the start page and set up the main window's navigation widgets.
             */
            @Override
            public void dockableFrameAdded(DockableFrameEvent evt) {
                Savant.getInstance().showBrowserControls();
            }

            @Override
            public void dockableFrameRemoved(DockableFrameEvent evt) {
                Frame f = (Frame)evt.getDockableFrame();
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

                } catch (Exception e) {
                    LOG.error("Error closing frame.", e);
                }
            }

            @Override
            public void dockableFrameActivated(DockableFrameEvent evt) {
                ((Frame)evt.getDockableFrame()).setActiveFrame();
            }

            @Override
            public void dockableFrameDeactivated(DockableFrameEvent evt) {
                ((Frame)evt.getDockableFrame()).setInactiveFrame();
            }
        });
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

        if (askFirst) {
            if (DialogUtils.askYesNo("Confirm", "Are you sure you want to close this track?") == DialogUtils.NO) {
                return;
            }
        }
        frame.getDockingManager().removeFrame(frame.getName());
    }

    /**
     * Create a frame for the given tracks.  Currently only used when creating a frame for
     * a track which has been created by a DataSource plugin.
     *
     * FIXME: Can we use the normal track-creation code?
     */
    public Frame createFrame(Track[] tracks) {
        Frame frame = DockableFrameFactory.createTrackFrame(tracks[0].getDataFormat() == DataFormat.SEQUENCE_FASTA);
        frame.setKey(tracks[0].getName());
        addFrame(frame);
        frame.setTracks(tracks);
        return frame;
    }


    public Frame addTrackFromPath(String fileOrURI, boolean seq) {

        URI uri = null;
        try {
            uri = new URI(fileOrURI);
            if (uri.getScheme() == null) {
                uri = new File(fileOrURI).toURI();
            }
        } catch (URISyntaxException usx) {
            // This can happen if we're passed a file-name containing spaces.
            uri = new File(fileOrURI).toURI();
        }
        return addTrackFromURI(uri, seq || fileOrURI.endsWith(".fa.savant"));
    }

    public Frame addTrackFromURI(URI uri, boolean seq) {
        Frame frame = DockableFrameFactory.createTrackFrame(seq);
        //Force a unique frame key. The title of frame is overwritten by track name later.
        frame.setKey(uri.toString()+System.nanoTime());
        addFrame(frame);
        TrackFactory.createTrack(uri, frame);
        return frame;
    }


    private void addFrame(Frame f) {
        frames.add(f);

        DockingManager trackDockingManager = Savant.getInstance().getTrackDockingManager();

        trackDockingManager.addFrame(f);

        // Insert the frame immediately below the currently-active frame.
        if (frames.size() > 1) {
            FrameHandle lastFrame = getFrontmostFrame(trackDockingManager);
            trackDockingManager.moveFrame(f.getKey(), lastFrame.getKey(), DockContext.DOCK_SIDE_SOUTH);
        }
    }

    /**
     * Determine which one of our frames is the frontmost.  This will be used to determine the
     * insertion position for the next frame.
     */
    private FrameHandle getFrontmostFrame(DockingManager dm) {
        for (FrameHandle h : dm.getOrderedFrames()) {
            if (!h.getKey().startsWith("#")) {
                return h;
            }
        }
        return null;
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
}
