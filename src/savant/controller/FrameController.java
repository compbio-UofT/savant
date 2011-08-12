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

import java.beans.PropertyVetoException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangedListener;
import savant.view.swing.DockableFrameFactory;
import savant.view.swing.Frame;
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
    }

    /**
     * Create a frame for the given tracks.
     */
    public Frame createFrame(Track[] tracks) {
        Frame frame = DockableFrameFactory.createTrackFrame();
        frame.setTracks(tracks);
        return frame;
    }


    public Frame addTrackFromPath(String fileOrURI) {

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
        return addTrackFromURI(uri);
    }

    public Frame addTrackFromURI(URI uri) {
        LOG.info("Loading track " + uri);
        Frame frame = DockableFrameFactory.createTrackFrame();
        //Force a unique frame key. The title of frame is overwritten by track name later.
        frame.setKey(uri.toString()+System.nanoTime());
        TrackFactory.createTrack(uri, frame);
        return frame;
    }


    public void addFrame(Frame f) {
        frames.add(f);

        GraphPaneController.getInstance().enlistRenderingGraphpane(f.getGraphPane());
        f.drawTracksInRange(locationController.getReferenceName(), locationController.getRange());
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

    public void closeFrame(Frame frame) {
        hideFrame(frame);

        try {
            
            TrackController vtc = TrackController.getInstance();
            SelectionController sc = SelectionController.getInstance();
            for (Track t : frame.getTracks()) {
                vtc.removeTrack(t);
                sc.removeAll(t.getName());
            }
            frames.remove(frame);

        } catch (Exception e) {
            LOG.error("Error closing frame.", e);
        }
    }

    public List<Frame> getFrames() {
        return frames;
    }
}
