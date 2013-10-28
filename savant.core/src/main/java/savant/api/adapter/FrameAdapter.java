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
     * @param track the track whose mode has changed
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
    
    public void setCloseable(boolean closeable);
}
