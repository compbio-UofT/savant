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

import savant.controller.event.track.TrackListChangedEvent;
import savant.controller.event.track.TrackListChangedListener;
import savant.model.FileFormat;
import savant.view.swing.ViewTrack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrackController {

    private static TrackController instance;

    private static Log log = LogFactory.getLog(TrackController.class);

    List<ViewTrack> tracks;

    /** Tracks Changed Listeners */
    private List tracksChangedListeners;

    public static TrackController getInstance() {
        if (instance == null) {
            instance = new TrackController();
        }
        return instance;
    }

    private TrackController() {
        tracksChangedListeners = new ArrayList();
        tracks = new ArrayList<ViewTrack>();
    }

        /**
     * Get the loaded tracks.
     * @return A list of tracks
     */
    public synchronized List<ViewTrack> getTracks() {
        return this.tracks;
    }

    /**
     * Get the track at the specified index.
     * @param index An index
     * @return The track at the specified index
     */
    public ViewTrack getTrack(int index) {
        return this.tracks.get(index);
    }

    /**
     * Add a track to the list of tracks.
     * @param track The track to add
     */
    public void addTrack(ViewTrack track) {
        tracks.add(track);
        fireTracksListChangedEvent();
    }

    /**
     * Get tracks of a specified kind.
     * @param kind A track kind
     * @return A list of tracks
     */
    public List<ViewTrack> getTracks(FileFormat kind) {
        List<ViewTrack> tracksOfKind = new ArrayList<ViewTrack>();
        for (ViewTrack t : tracks) {
            if (t.getDataType() == kind) {
                tracksOfKind.add(t);
            }
        }
        return tracksOfKind;
    }

    /**
     * Fire the RangeChangedEvent
     */
    private synchronized void fireTracksListChangedEvent() {
        TrackListChangedEvent evt = new TrackListChangedEvent(this, this.tracks);
        Iterator listeners = this.tracksChangedListeners.iterator();
        while (listeners.hasNext()) {
            ((TrackListChangedListener) listeners.next()).trackListChangeReceived(evt);
        }
    }

    public synchronized void addTracksChangedListener(TrackListChangedListener l) {
        tracksChangedListeners.add(l);
    }

    public synchronized void removeTracksChangedListener(TrackListChangedListener l) {
        tracksChangedListeners.remove(l);
    }

    void removeTrack(ViewTrack track) {
        this.tracks.remove(track);
    }

}