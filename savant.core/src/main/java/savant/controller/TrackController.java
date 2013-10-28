/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.controller;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.NameValuePair;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ut.biolab.savant.analytics.savantanalytics.AnalyticsAgent;

import savant.api.adapter.TrackAdapter;
import savant.api.event.TrackEvent;
import savant.api.data.DataFormat;
import savant.util.Controller;
import savant.view.tracks.Track;

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
        AnalyticsAgent.log(
                    new NameValuePair[]{
                        new NameValuePair("track-event", "opened"),
                        new NameValuePair("track-type", t.getClass().getSimpleName())
                    });
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