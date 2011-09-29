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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.event.TrackEvent;
import savant.file.DataFormat;
import savant.util.Controller;
import savant.view.swing.Track;

/**
 * Controller object to manage changes to the list of tracks.
 *
 * @author vwilliams
 */
public class TrackController extends Controller<TrackEvent> {
    private static final Log LOG = LogFactory.getLog(TrackController.class);

    private static TrackController instance;

    List<Track> tracks;

    public static synchronized TrackController getInstance() {
        if (instance == null) {
            instance = new TrackController();
        }
        return instance;
    }

    private TrackController() {
        tracks = new ArrayList<Track>();
    }

    /**
     * Get the loaded tracks.
     * @return A list of tracks
     */
    public synchronized List<Track> getTracks() {
        return tracks;
    }

    /**
     * Get the track at the specified index.
     * @param index An index
     * @return The track at the specified index
     */
    public Track getTrack(int index) {
        return tracks.get(index);
    }

    /**
     * Add a track to the list of tracks.
     * @param t The track to add
     */
    public void addTrack(Track t) {
        LOG.info("Added " + t + " to track list.");
        tracks.add(t);
        fireEvent(new TrackEvent(TrackEvent.Type.ADDED, t));
    }

    /**
     * Get tracks of a specified kind.
     * @param kind A track kind
     * @return A list of tracks
     */
    public List<Track> getTracks(DataFormat kind) {
        List<Track> tracksOfKind = new ArrayList<Track>();
        for (Track t : tracks) {
            if (t.getDataFormat() == kind) {
                tracksOfKind.add(t);
            }
        }
        return tracksOfKind;
    }

    public void removeTrack(Track t) {
        tracks.remove(t);
        fireEvent(new TrackEvent(TrackEvent.Type.REMOVED, t));
    }

    public Track getTrack(String tname) {
        for (Track t : tracks) {
            if (t.getName().equals(tname)) {
                return t;
            }
        }
        return null;
    }

    public void closeTracks() {
        FrameController.getInstance().closeAllFrames(false);
    }

    /**
     * Remove a track which (for some reason) was created but was not
     * placed in a frame.
     * @param t The track to be removed
     */
    public void removeUnframedTrack(Track t) {
        tracks.remove(t);
    }

    public boolean containsTrack(String name) {
        for (Track t : tracks) {
            if (name.equals(t.getName())) {
                return true;
            }
        }
        return false;
    }
}