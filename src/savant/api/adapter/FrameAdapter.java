/*
 *    Copyright 2011 University of Toronto
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

package savant.api.adapter;

import javax.swing.JPanel;

import savant.api.event.DataRetrievalEvent;
import savant.api.util.Listener;
import savant.plugin.SavantPanelPlugin;
import savant.view.tracks.Track;
import savant.view.tracks.TrackCreationEvent;


/**
 * Interface which defines the type of objects which can contain a track.  In the current implementation,
 * these are JIDE DockableFrame objects.
 * 
 * This interface is intended for internal use only.
 *
 * @author tarkvara
 * @since 2.0.0
 */
public interface FrameAdapter extends Listener<DataRetrievalEvent> {
    
    /**
     * Tells the frame that the draw mode for this track has changed.  Gives the frame
     * a chance to rebuild its user interface and request a repaint. 
     * @param t the track whose mode has changed
     */
    public void drawModeChanged(Track track);
    
    /**
     * Get a panel for a plugin to draw on.  If necessary, a new one will be created.
     * @param plugin    the plugin which is requesting a canvas
     * @param mayCreate if true, Frame should create a new canvas for the plugin if one doesn't already exist
     */
    public JPanel getLayerCanvas(SavantPanelPlugin plugin, boolean mayCreate);

    /**
     * Get a GraphPane on which the track can render.
     * @return the GraphPane associated with this Frame
     */
    public GraphPaneAdapter getGraphPane();

    /**
     * Force the associated track to redraw.  Used when the colour scheme has been changed by the Preferences dialog.
     */
    public void forceRedraw();

    public void setHeightFromSlider();

    public Track[] getTracks();

    public void handleEvent(TrackCreationEvent evt);

    public int getIntervalHeight();
}
