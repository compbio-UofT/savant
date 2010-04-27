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
 * TrackController.java
 * Created on Mar 11, 2010
 */

package savant.controller;

import savant.controller.event.track.TrackListChangedEvent;
import savant.controller.event.track.TrackListChangedListener;
import savant.model.data.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton controller class to manage data tracks.
 */
public class TrackController {

    /**
     * Singleton instance. Use getInstance() to get a reference.
     */
    private static TrackController instance;

    // list of currently managed tracks
    private List<Track> tracks;

    private List<TrackListChangedListener> listeners;

    /**
     * Constructor. Private access, use getInstance() instead.
     */
    private TrackController() {
        this.tracks = new ArrayList<Track>();
        this.listeners = new ArrayList<TrackListChangedListener>();
    }

    public static synchronized TrackController getInstance() {
        if (instance == null) instance = new TrackController();
        return instance;
    }

    public void addTrack(Track track) {
        tracks.add(track);
        fireTracksChangedEvent();
    }

    public void removeTrack(Track track) {
        tracks.remove(track);
        fireTracksChangedEvent();
    }

    public List<Track> getTracks() {
        return this.tracks;
    }

    public Track getTrack(int index) {
        return tracks.get(index);
    }
    
    public synchronized void addTrackListChangedListener(TrackListChangedListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeTrackListChangedListener(TrackListChangedListener listener) {
        listeners.remove(listener);
    }

    private void fireTracksChangedEvent() {
        TrackListChangedEvent evt = new TrackListChangedEvent(this, this.tracks);
        for (TrackListChangedListener listener : listeners) {
            listener.trackListChangeReceived(evt);
        }
    }
}
