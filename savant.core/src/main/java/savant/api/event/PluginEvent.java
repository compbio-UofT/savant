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
package savant.api.event;

import javax.swing.JPanel;
import savant.plugin.PluginController;
import savant.plugin.SavantPlugin;


/**
 * Event which is fired by the PluginController when our plugin list changes.
 *
 * @author tarkvara
 */
public class PluginEvent {
    public enum Type {
        LOADED,
        QUEUED_FOR_REMOVAL,
        ERROR
    }

    private final Type type;
    private final String pluginID;
    private final JPanel canvas;

    /**
     * Constructor invoked by PluginController.
     */
    public PluginEvent(Type type, String pluginID, JPanel canvas) {
        this.type = type;
        this.pluginID = pluginID;
        this.canvas = canvas;
    }

    public Type getType() {
        return type;
    }

    /**
     * Plugin which loaded or errored.
     */
    public String getPluginID() {
        return pluginID;
    }

    /**
     * Plugin which loaded.  Will be null if the plugin did not successfully load.
     */
    public SavantPlugin getPlugin() {
        return PluginController.getInstance().getPlugin(pluginID);
    }

    public JPanel getCanvas() {
        return canvas;
    }
}
