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
package savant.plugin;

import com.jidesoft.docking.DockingManager;
import javax.swing.JPanel;
import savant.controller.TrackController;
import savant.plugin.SavantPlugin;
import savant.util.MiscUtils;
import savant.view.swing.Savant;
import savant.view.tracks.Track;

/**
 * Plugin which displays its contents in a JPanel managed by the Savant
 * user-interface. The canonical example is our own data table plugin.
 *
 * @author mfiume
 */
public abstract class SavantPanelPlugin extends SavantPlugin {

    /**
     * This method is called once during application life cycle to allow a
     * third-party plugin to initialize and show itself.
     *
     * @param panel parent panel for auxiliary data components
     */
    public abstract void init(JPanel panel);

    /**
     * Show or hide all UI elements associated with this plugin. This includes
     * both the frame for the plugin's GUI as well as any layer canvasses which
     * have been created on the plugin's behalf.
     */
    public void setVisible(boolean value) {
        String frameKey = getTitle();
        DockingManager auxDockingManager = Savant.getInstance().getAuxDockingManager();
        MiscUtils.setFrameVisibility(frameKey, value, auxDockingManager);

        for (Track t : TrackController.getInstance().getTracks()) {
            JPanel layerCanvas = t.getFrame().getLayerCanvas(this, false);
            if (layerCanvas != null) {
                layerCanvas.setVisible(value);
            }
        }
    }

    /**
     * Is the associated plugin currently visible?
     */
    public boolean isVisible() {
        try {
            return Savant.getInstance().getAuxDockingManager().getFrame(getTitle()).isVisible();
        } catch (Exception e) {
            return false;
        }
    }
}
