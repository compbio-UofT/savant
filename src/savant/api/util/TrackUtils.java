/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.ViewTrackAdapter;
import savant.controller.ViewTrackController;
import savant.controller.event.ViewTrackAddedListener;
import savant.controller.event.ViewTrackListChangedListener;
import savant.controller.event.ViewTrackRemovedListener;
import savant.data.sources.DataSource;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.DataFormat;
import savant.view.swing.Savant;
import savant.view.swing.TrackFactory;
import savant.view.swing.ViewTrack;

/**
 * Utilities for Savant tracks
 * @author mfiume
 */
public class TrackUtils {
    private static final Log LOG = LogFactory.getLog(TrackUtils.class);

    private static ViewTrackController vtc = ViewTrackController.getInstance();

    /**
     * Get the loaded tracks.
     * @return A list of tracks
     */
    public static List<ViewTrackAdapter> getTracks() {
        List<ViewTrackAdapter> r = new ArrayList<ViewTrackAdapter>();
        for (ViewTrack t : vtc.getTracks()) {
            r.add((ViewTrackAdapter) t);
        }
        return r;
    }

    /**
     * Add a track to the list of tracks.
     * @param track The track to add
     */
    public static void addTrack(ViewTrackAdapter track) {
        List<ViewTrackAdapter> tracks = new ArrayList<ViewTrackAdapter>();
        tracks.add(track);
        addTracks(tracks);
    }

    /**
     * Add multiple tracks to the list of tracks.
     * @param tracs The track to add
     */
    public static void addTracks(List<ViewTrackAdapter> tracks) {
        List<ViewTrack> myTracks = new ArrayList<ViewTrack>();
        for (ViewTrackAdapter t : tracks) {
            myTracks.add((ViewTrack)t);
        }
        Savant.getInstance().createFrameForTracks(myTracks);
    }

    /**
     * Create a track from a path (either file or from web).
     * @param path Path to the track (can be either local or remote, e.g. on http or ftp server)
     * @return A list of tracks based on the path (some paths, e.g. to BAM files, can create multiple tracks)
     * @throws IOException Exception opening the track at path
     */
    public static List<ViewTrackAdapter> createTrack(URI uri) throws IOException, SavantTrackCreationCancelledException {
        List<ViewTrackAdapter> r = new ArrayList<ViewTrackAdapter>();
        for (ViewTrack t : TrackFactory.createTrack(uri)) {
            r.add((ViewTrackAdapter) t);
        }
        return r;
    }

    /**
     * Create a track from a local file.
     * @param file Local file
     * @return A list of tracks based on the path (some paths, e.g. to BAM files, can create multiple tracks)
     * @throws IOException Exception opening the track at path
     */
    public static List<ViewTrackAdapter> createTrack(File file) throws IOException, SavantTrackCreationCancelledException {
        List<ViewTrackAdapter> r = new ArrayList<ViewTrackAdapter>();
        for (ViewTrack t : TrackFactory.createTrack(file.toURI())) {
            r.add((ViewTrackAdapter) t);
        }
        return r;
    }

    /**
     * Get the data source of a track
     * @param trackname
     * @return
     */
    public static DataSource getTrackDataSource(String trackname) {
        ViewTrackAdapter t = getTrack(trackname);
        if (t == null) { return null; }
        return t.getDataSource();
    }

    /**
     * Get all tracks of a specific format
     * @param kind The format of tracks wanted
     * @return A list of all tracks of a specific format
     */
    public List<ViewTrackAdapter> getTracks(DataFormat kind) {
        List<ViewTrackAdapter> r = new ArrayList<ViewTrackAdapter>();
        for (ViewTrack t : vtc.getTracks(kind)) {
            r.add((ViewTrackAdapter) t);
        }
        return r;
    }

    /**
     * Subscribe a listener to be notified when the track list changes
     * @param l The listener to subscribe
     */
    public static synchronized void addTracksChangedListener(ViewTrackListChangedListener l) {
        vtc.addTracksChangedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the track list changes
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTracksChangedListener(ViewTrackListChangedListener l) {
        vtc.removeTracksChangedListener(l);
    }

    /**
     * Subscribe a listener to be notified when a track is added
     * @param l The listener to subscribe
     */
    public static synchronized void addTracksAddedListener(ViewTrackAddedListener l) {
        vtc.addTracksAddedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when a track is added
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTracksAddedListener(ViewTrackAddedListener l) {
        vtc.removeTracksAddedListener(l);
    }

    /**
     * Subscribe a listener to be notified when a track is removed
     * @param l The listener to subscribe
     */
    public static synchronized void addTracksRemovedListener(ViewTrackRemovedListener l) {
        vtc.addTracksRemovedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when a track is removed
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeTracksRemovedListener(ViewTrackRemovedListener l) {
        vtc.removeTracksRemovedListener(l);
    }

    /**
     * Get a track
     * @param trackname Name of the track to get
     * @return The track with the specified name, null if none
     */
    public static ViewTrackAdapter getTrack(String trackname) {
        return vtc.getTrack(trackname);
    }

}
