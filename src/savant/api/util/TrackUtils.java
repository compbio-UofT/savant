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

package savant.api.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.TrackAdapter;
import savant.controller.TrackController;
import savant.controller.event.TrackAddedListener;
import savant.controller.event.TrackListChangedListener;
import savant.controller.event.TrackRemovedListener;
import savant.data.sources.DataSource;
import savant.file.DataFormat;
import savant.view.swing.Frame;
import savant.view.swing.Savant;
import savant.view.swing.TrackFactory;
import savant.view.swing.Track;

/**
 * Utilities for Savant tracks
 * @author mfiume
 */
public class TrackUtils {
    private static final Log LOG = LogFactory.getLog(TrackUtils.class);

    private static TrackController trackController = TrackController.getInstance();

    /**
     * Get the loaded tracks.
     * @return A list of tracks
     */
    public static TrackAdapter[] getTracks() {
        int numTracks = trackController.getTracks().size();
        TrackAdapter[] result = new TrackAdapter[numTracks];
        for (int i = 0; i < numTracks; i++) {
            result[i] = trackController.getTrack(i);
        }
        return result;
    }

    /**
     * Add a track to the list of tracks.
     * @param track The track to add
     * @deprecated No longer necessary; createTracks does all the work.
     */
    public static void addTrack(TrackAdapter track) {
        addTracks(Arrays.asList(new TrackAdapter[] { track }));
    }

    /**
     * Add multiple tracks to the list of tracks.
     * @param tracs The track to add
     * @deprecated No longer necessary; createTracks does all the work.
     */
    public static void addTracks(List<TrackAdapter> tracks) {
        Track[] myTracks = new Track[tracks.size()];
        for (int i = 0; i < tracks.size(); i++) {
            myTracks[i] = (Track)tracks.get(i);
        }
        Savant.getInstance().createFrameForExistingTrack(myTracks);
    }

    /**
     * Create a track from an existing DataSource.
     *
     * @param ds a DataSource object which has already been created
     */
    public static TrackAdapter createTrack(DataSource ds) throws Throwable {
        return TrackFactory.createTrack(ds);
    }

    /**
     * Create a track from a path (either file or from web).
     * @param path Path to the track (can be either local or remote, e.g. on http or ftp server)
     * @return A list of tracks based on the path (some paths, e.g. to BAM files, can create multiple tracks)
     * @throws IOException Exception opening the track at path
     */
    public static TrackAdapter[] createTrack(URI uri) throws Throwable {
        Frame f = Savant.getInstance().addTrackFromURI(uri);
        return f.getTracks();
    }

    /**
     * Create a track from a local file.  As of 1.5, now adds the track to the UI.
     *
     * @param file Local file
     * @return A list of tracks based on the path (some paths, e.g. to BAM files, can create multiple tracks)
     * @throws IOException Exception opening the track at path
     */
    public static TrackAdapter[] createTrack(File file) throws Throwable {
        Frame f = Savant.getInstance().addTrackFromURI(file.toURI());
        return f.getTracks();
    }

    /**
     * Get the data source of a track
     * @param trackname
     * @return
     */
    public static DataSource getTrackDataSource(String trackname) {
        TrackAdapter t = getTrack(trackname);
        if (t == null) { return null; }
        return t.getDataSource();
    }

    /**
     * Get all tracks of a specific format
     * @param kind The format of tracks wanted
     * @return A list of all tracks of a specific format
     */
    public TrackAdapter[] getTracks(DataFormat kind) {
        List<TrackAdapter> r = new ArrayList<TrackAdapter>();
        for (Track t : trackController.getTracks(kind)) {
            r.add((TrackAdapter)t);
        }
        return r.toArray(new TrackAdapter[0]);
    }

    /**
     * Subscribe a listener to be notified when the track list changes
     * @param l The listener to subscribe
     */
    public static synchronized void addTracksChangedListener(TrackListChangedListener l) {
        trackController.addTrackListChangedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the track list changes
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTracksChangedListener(TrackListChangedListener l) {
        trackController.removeTrackListChangedListener(l);
    }

    /**
     * Subscribe a listener to be notified when a track is added
     * @param l The listener to subscribe
     */
    public static synchronized void addTrackAddedListener(TrackAddedListener l) {
        trackController.addTrackAddedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when a track is added
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTrackAddedListener(TrackAddedListener l) {
        trackController.removeTrackAddedListener(l);
    }

    /**
     * Subscribe a listener to be notified when a track is removed
     * @param l The listener to subscribe
     */
    public static synchronized void addTrackRemovedListener(TrackRemovedListener l) {
        trackController.addTrackRemovedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when a track is removed
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTrackRemovedListener(TrackRemovedListener l) {
        trackController.removeTrackRemovedListener(l);
    }

    /**
     * Get a track
     * @param trackname Name of the track to get
     * @return The track with the specified name, null if none
     */
    public static TrackAdapter getTrack(String trackname) {
        return trackController.getTrack(trackname);
    }
}
