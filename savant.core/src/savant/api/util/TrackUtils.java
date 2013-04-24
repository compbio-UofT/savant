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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.event.TrackEvent;
import savant.controller.FrameController;
import savant.controller.TrackController;
import savant.view.swing.DockableFrameFactory;
import savant.view.swing.Frame;
import savant.view.tracks.TrackFactory;
import savant.view.tracks.Track;

/**
 * Utilities for Savant tracks
 * @author mfiume
 */
public class TrackUtils {
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
     * Add a single track to the list of tracks.  
     * @param track the track to add
     */
    public static void addTrack(TrackAdapter track) {
        addTracks(Arrays.asList(new TrackAdapter[] { track }));
    }

    /**
     * Add multiple tracks to the list of tracks.
     *
     * @param tracks the tracks to add
     */
    public static void addTracks(List<TrackAdapter> tracks) {
        Track[] myTracks = new Track[tracks.size()];
        for (int i = 0; i < tracks.size(); i++) {
            myTracks[i] = (Track)tracks.get(i);
        }
        Frame f = DockableFrameFactory.createTrackFrame(myTracks[0].getDataFormat());
        f.setTracks(myTracks);
    }

    /**
     * Create a track from an existing DataSource.
     *
     * @param ds a DataSource object which has already been created
     */
    public static TrackAdapter createTrack(DataSourceAdapter ds) throws Throwable {
        return TrackFactory.createTrack(ds);
    }

    /**
     * Create a track from a path (either file or from web).
     * @param uri Path to the track (can be either local or remote, e.g. on http or ftp server)
     * @return A list of tracks based on the path (some paths, e.g. to BAM files, can create multiple tracks)
     * @throws IOException Exception opening the track at path
     */
    public static TrackAdapter[] createTrack(URI uri) throws Throwable {
        Frame f = FrameController.getInstance().addTrackFromURI(uri, null, null);
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
        Frame f = FrameController.getInstance().addTrackFromURI(file.toURI(), null, null);
        return f.getTracks();
    }

    /**
     * Get the data source of a track.
     *
     * @param trackName name of track
     * @return the <code>DataSource</code> associated with this track (may be null)
     */
    public static DataSourceAdapter getTrackDataSource(String trackName) {
        TrackAdapter t = getTrack(trackName);
        if (t == null) { return null; }
        return t.getDataSource();
    }

    /**
     * Get all tracks of a specific format.
     *
     * @param kind The format of tracks requested
     * @return An array of all tracks of a specific format
     */
    public static TrackAdapter[] getTracks(DataFormat kind) {
        List<TrackAdapter> r = new ArrayList<TrackAdapter>();
        for (Track t : trackController.getTracks(kind)) {
            r.add((TrackAdapter)t);
        }
        return r.toArray(new TrackAdapter[0]);
    }

    /**
     * Register a listener to be notified of tracks being added, removed, or opened.
     * @param l the listener to receive notifications
     * @since 1.6.0
     */
    public static synchronized void addTrackListener(Listener<TrackEvent> l) {
        trackController.addListener(l);
    }

    /**
     * Remove a track event listener.
     * @param l the listener to be removed
     */
    public static synchronized void removeTrackListener(Listener<TrackEvent> l) {
        trackController.removeListener(l);
    }

    /**
     * Get a track by name.
     *
     * @param trackName Name of the track to get (generally the same as the track's URI)
     * @return The track with the specified name, null if none
     */
    public static TrackAdapter getTrack(String trackName) {
        return trackController.getTrack(trackName);
    }
}
