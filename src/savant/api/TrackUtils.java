/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api;

import java.io.IOException;
import java.util.List;
import savant.controller.ViewTrackController;
import savant.controller.event.viewtrack.ViewTrackAddedListener;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.controller.event.viewtrack.ViewTrackRemovedListener;
import savant.data.sources.DataSource;
import savant.file.FileFormat;
import savant.view.swing.ViewTrack;

/**
 * Utilities for Savant tracks
 * @author mfiume
 */
public class TrackUtils {

    private static ViewTrackController vtc = ViewTrackController.getInstance();

    /**
     * Get the loaded tracks.
     * @return A list of tracks
     */
    public static List<ViewTrack> getTracks() {
        return vtc.getTracks();
    }

    /**
     * Add a track to the list of tracks.
     * @param track The track to add
     */
    public void addTrack(ViewTrack track) {
        vtc.addTrack(track);
    }

    /**
     * Create a track from a path (either file or from web)
     * @param path Path to the track (can be either local or remote, e.g. on http or ftp server)
     * @return A list of tracks based on the path (some paths, e.g. to BAM files, can create multiple tracks)
     * @throws IOException Exception opening the track at path
     */
    public List<ViewTrack> createTrack(String path) throws IOException {
        return ViewTrack.create(path);
    }

    /**
     * Get the data source of a track
     * @param trackname
     * @return
     */
    public DataSource getTrackDatasource(String trackname) {
        ViewTrack t = this.getTrack(trackname);
        if (t == null) { return null; }
        return t.getDataSource();
    }

    /**
     * Get all tracks of a specific format
     * @param kind The format of tracks wanted
     * @return A list of all tracks of a specific format
     */
    public List<ViewTrack> getTracks(FileFormat kind) {
        return vtc.getTracks(kind);
    }

    /**
     * Subscribe a listener to be notified when the track list changes
     * @param l The listener to subscribe
     */
    public synchronized void addTracksChangedListener(ViewTrackListChangedListener l) {
        vtc.addTracksChangedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the track list changes
     * @param l The listener to unsubscribe
     */
    public synchronized void removeTracksChangedListener(ViewTrackListChangedListener l) {
        vtc.removeTracksChangedListener(l);
    }

    /**
     * Subscribe a listener to be notified when a track is added
     * @param l The listener to subscribe
     */
    public synchronized void addTracksAddedListener(ViewTrackAddedListener l) {
        vtc.addTracksAddedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when a track is added
     * @param l The listener to unsubscribe
     */
    public synchronized void removeTracksAddedListener(ViewTrackAddedListener l) {
        vtc.removeTracksAddedListener(l);
    }

    /**
     * Subscribe a listener to be notified when a track is removed
     * @param l The listener to subscribe
     */
    public synchronized void addTracksRemovedListener(ViewTrackRemovedListener l) {
        vtc.addTracksRemovedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when a track is removed
     * @param l The listener to unsubscribe
     */
    public synchronized void removeTracksRemovedListener(ViewTrackRemovedListener l) {
        vtc.removeTracksRemovedListener(l);
    }

    /**
     * Get a track
     * @param trackname Name of the track to get
     * @return The track with the specified name, null if none
     */
    public ViewTrack getTrack(String trackname) {
        return vtc.getTrack(trackname);
    }
}

