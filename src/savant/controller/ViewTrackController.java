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

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import savant.controller.event.viewtrack.ViewTrackAddedListener;
import savant.controller.event.viewtrack.ViewTrackAddedOrRemovedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.controller.event.viewtrack.ViewTrackRemovedListener;
import savant.file.DataFormat;
import savant.view.swing.Savant;
import savant.view.swing.ViewTrack;

public class ViewTrackController {

    private static ViewTrackController instance;

    List<ViewTrack> tracks;

    /** Tracks Changed Listeners */
    private List tracksChangedListeners;
    private List tracksAddedListeners;
    private List tracksRemovedListeners;

    public static synchronized ViewTrackController getInstance() {
        if (instance == null) {
            instance = new ViewTrackController();
        }
        return instance;
    }

    private ViewTrackController() {
        tracksChangedListeners = new ArrayList();
        tracksAddedListeners = new ArrayList();
        tracksRemovedListeners = new ArrayList();
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
        fireTracksAddedEvent(track);
        fireTracksListChangedEvent();
    }

    /**
     * Get tracks of a specified kind.
     * @param kind A track kind
     * @return A list of tracks
     */
    public List<ViewTrack> getTracks(DataFormat kind) {
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
        ViewTrackListChangedEvent evt = new ViewTrackListChangedEvent(this, this.tracks);
        Iterator listeners = this.tracksChangedListeners.iterator();
        while (listeners.hasNext()) {
            ((ViewTrackListChangedListener) listeners.next()).viewTrackListChangeReceived(evt);
        }
    }


    public synchronized void addTracksChangedListener(ViewTrackListChangedListener l) {
        tracksChangedListeners.add(l);
    }

    public synchronized void removeTracksChangedListener(ViewTrackListChangedListener l) {
        tracksChangedListeners.remove(l);
    }

    private synchronized void fireTracksRemovedEvent(ViewTrack track) {
        ViewTrackAddedOrRemovedEvent evt = new ViewTrackAddedOrRemovedEvent(this, track);
        Iterator listeners = this.tracksRemovedListeners.iterator();
        while (listeners.hasNext()) {
            ((ViewTrackRemovedListener) listeners.next()).viewTrackRemovedEventReceived(evt);
        }
    }


    public synchronized void addTracksRemovedListener(ViewTrackRemovedListener l) {
        tracksRemovedListeners.add(l);
    }

    public synchronized void removeTracksRemovedListener(ViewTrackRemovedListener l) {
        tracksRemovedListeners.remove(l);
    }

    private synchronized void fireTracksAddedEvent(ViewTrack track) {
        ViewTrackAddedOrRemovedEvent evt = new ViewTrackAddedOrRemovedEvent(this, track);
        Iterator listeners = this.tracksAddedListeners.iterator();
        while (listeners.hasNext()) {
            ((ViewTrackAddedListener) listeners.next()).viewTrackAddedEventReceived(evt);
        }
    }

    public synchronized void addTracksAddedListener(ViewTrackAddedListener l) {
        tracksAddedListeners.add(l);
    }

    public synchronized void removeTracksAddedListener(ViewTrackAddedListener l) {
        tracksAddedListeners.remove(l);
    }

    void removeTrack(ViewTrack track) {
        this.tracks.remove(track);
        fireTracksRemovedEvent(track);
        fireTracksListChangedEvent();
    }

    public ViewTrack getTrack(String tname) {
        for (ViewTrack t : this.tracks) {
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

    public boolean containsTrack(URI uri) {
        for (ViewTrack t : tracks) {
            if (uri.equals(t.getURI())) {
                return true;
            }
        }
        return false;
    }
}