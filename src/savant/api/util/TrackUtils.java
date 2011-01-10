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
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.DataFormat;
import savant.view.swing.Savant;
import savant.view.swing.TrackFactory;
import savant.view.swing.Track;

/**
 * Utilities for Savant tracks
 * @author mfiume
 */
public class TrackUtils {
    private static final Log LOG = LogFactory.getLog(TrackUtils.class);

    private static TrackController vtc = TrackController.getInstance();

    /**
     * Get the loaded tracks.
     * @return A list of tracks
     */
    public static List<TrackAdapter> getTracks() {
        List<TrackAdapter> r = new ArrayList<TrackAdapter>();
        for (Track t : vtc.getTracks()) {
            r.add((TrackAdapter) t);
        }
        return r;
    }

    /**
     * Add a track to the list of tracks.
     * @param track The track to add
     */
    public static void addTrack(TrackAdapter track) {
        addTracks(Arrays.asList(new TrackAdapter[] { track }));
    }

    /**
     * Add multiple tracks to the list of tracks.
     * @param tracs The track to add
     */
    public static void addTracks(List<TrackAdapter> tracks) {
        List<Track> myTracks = new ArrayList<Track>();
        for (TrackAdapter t : tracks) {
            myTracks.add((Track)t);
        }
        Savant.getInstance().createFrameForExistingTracks(myTracks);
    }

    /**
     * Create a track from a path (either file or from web).
     * @param path Path to the track (can be either local or remote, e.g. on http or ftp server)
     * @return A list of tracks based on the path (some paths, e.g. to BAM files, can create multiple tracks)
     * @throws IOException Exception opening the track at path
     */
    public static List<TrackAdapter> createTrack(URI uri) throws SavantTrackCreationCancelledException {
        List<TrackAdapter> r = new ArrayList<TrackAdapter>();
        for (Track t : TrackFactory.createTrackSync(uri)) {
            r.add((TrackAdapter) t);
        }
        return r;
    }

    /**
     * Create a track from a local file.
     * @param file Local file
     * @return A list of tracks based on the path (some paths, e.g. to BAM files, can create multiple tracks)
     * @throws IOException Exception opening the track at path
     */
    public static List<TrackAdapter> createTrack(File file) throws SavantTrackCreationCancelledException {
        List<TrackAdapter> r = new ArrayList<TrackAdapter>();
        for (Track t : TrackFactory.createTrackSync(file.toURI())) {
            r.add((TrackAdapter) t);
        }
        return r;
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
    public List<TrackAdapter> getTracks(DataFormat kind) {
        List<TrackAdapter> r = new ArrayList<TrackAdapter>();
        for (Track t : vtc.getTracks(kind)) {
            r.add((TrackAdapter) t);
        }
        return r;
    }

    /**
     * Subscribe a listener to be notified when the track list changes
     * @param l The listener to subscribe
     */
    public static synchronized void addTracksChangedListener(TrackListChangedListener l) {
        vtc.addTrackListChangedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the track list changes
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTracksChangedListener(TrackListChangedListener l) {
        vtc.removeTrackListChangedListener(l);
    }

    /**
     * Subscribe a listener to be notified when a track is added
     * @param l The listener to subscribe
     */
    public static synchronized void addTracksAddedListener(TrackAddedListener l) {
        vtc.addTrackAddedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when a track is added
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTracksAddedListener(TrackAddedListener l) {
        vtc.removeTrackAddedListener(l);
    }

    /**
     * Subscribe a listener to be notified when a track is removed
     * @param l The listener to subscribe
     */
    public static synchronized void addTracksRemovedListener(TrackRemovedListener l) {
        vtc.addTrackRemovedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when a track is removed
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTracksRemovedListener(TrackRemovedListener l) {
        vtc.removeTrackRemovedListener(l);
    }

    /**
     * Get a track
     * @param trackname Name of the track to get
     * @return The track with the specified name, null if none
     */
    public static TrackAdapter getTrack(String trackname) {
        return vtc.getTrack(trackname);
    }

}
