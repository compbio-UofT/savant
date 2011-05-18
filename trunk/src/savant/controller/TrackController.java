/*
 * TrackController.java
 * Created on Jan 19, 2010
 *
 *
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

/**
 * Controller object to manage changes to the list of tracks.
 *
 * @author vwilliams
 */
package savant.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import savant.controller.event.TrackAddedListener;
import savant.controller.event.TrackAddedOrRemovedEvent;
import savant.controller.event.TrackListChangedEvent;
import savant.controller.event.TrackListChangedListener;
import savant.controller.event.TrackRemovedListener;
import savant.file.DataFormat;
import savant.view.swing.Savant;
import savant.view.swing.Track;

public class TrackController {

    private static TrackController instance;

    List<Track> tracks;

    /** Tracks Changed Listeners */
    private List tracksChangedListeners;
    private List tracksAddedListeners;
    private List tracksRemovedListeners;

    public static synchronized TrackController getInstance() {
        if (instance == null) {
            instance = new TrackController();
        }
        return instance;
    }

    private TrackController() {
        tracksChangedListeners = new ArrayList();
        tracksAddedListeners = new ArrayList();
        tracksRemovedListeners = new ArrayList();
        tracks = new ArrayList<Track>();
    }

    /**
     * Get the loaded tracks.
     * @return A list of tracks
     */
    public synchronized List<Track> getTracks() {
        return this.tracks;
    }

    /**
     * Get the track at the specified index.
     * @param index An index
     * @return The track at the specified index
     */
    public Track getTrack(int index) {
        return this.tracks.get(index);
    }

    /**
     * Add a track to the list of tracks.
     * @param track The track to add
     */
    public void addTrack(Track track) {
        tracks.add(track);
        fireTrackAddedEvent(track);
        fireTracksListChangedEvent();
    }

    /**
     * Get tracks of a specified kind.
     * @param kind A track kind
     * @return A list of tracks
     */
    public List<Track> getTracks(DataFormat kind) {
        List<Track> tracksOfKind = new ArrayList<Track>();
        for (Track t : tracks) {
            if (t.getDataSource().getDataFormat() == kind) {
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


    public synchronized void addTrackListChangedListener(TrackListChangedListener l) {
        tracksChangedListeners.add(l);
    }

    public synchronized void removeTrackListChangedListener(TrackListChangedListener l) {
        tracksChangedListeners.remove(l);
    }

    private synchronized void fireTrackRemovedEvent(Track track) {
        TrackAddedOrRemovedEvent evt = new TrackAddedOrRemovedEvent(this, track);
        Iterator listeners = this.tracksRemovedListeners.iterator();
        while (listeners.hasNext()) {
            ((TrackRemovedListener) listeners.next()).trackRemovedEventReceived(evt);
        }
    }


    public synchronized void addTrackRemovedListener(TrackRemovedListener l) {
        tracksRemovedListeners.add(l);
    }

    public synchronized void removeTrackRemovedListener(TrackRemovedListener l) {
        tracksRemovedListeners.remove(l);
    }

    private synchronized void fireTrackAddedEvent(Track track) {
        TrackAddedOrRemovedEvent evt = new TrackAddedOrRemovedEvent(this, track);
        Iterator listeners = this.tracksAddedListeners.iterator();
        while (listeners.hasNext()) {
            ((TrackAddedListener) listeners.next()).trackAddedReceived(evt);
        }
    }

    public synchronized void addTrackAddedListener(TrackAddedListener l) {
        tracksAddedListeners.add(l);
    }

    public synchronized void removeTrackAddedListener(TrackAddedListener l) {
        tracksAddedListeners.remove(l);
    }

    public void removeTrack(Track track) {
        this.tracks.remove(track);
        fireTrackRemovedEvent(track);
        fireTracksListChangedEvent();
    }

    public Track getTrack(String tname) {
        for (Track t : this.tracks) {
            if (t.getName().equals(tname)) {
                return t;
            }
        }
        return null;
    }

    public void closeTracks() {
        DockableFrameController.getInstance().closeAllDockableFrames(
                Savant.getInstance().getTrackDockingManager(),false);
    }

    /**
     * Remove a track which (for some reason) was created but was not
     * placed in a frame.
     * @param t The track to be removed
     */
    public void removeUnframedTrack(Track t) {
        this.tracks.remove(t);
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