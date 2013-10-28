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


/**
 * Base class for all Savant plugins.  Not much here yet.
 *
 * @author tarkvara
 */
public abstract class SavantPlugin {
    private PluginDescriptor descriptor;

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Called by the PluginController after instantiating the plugin.
     *
     * @param desc descriptor from which this plugin was created
     */
    public void setDescriptor(PluginDescriptor desc) {
        descriptor = desc;
    }

    public String getTitle() {
        return descriptor.getName();
    }

    /**
     * Give plugins an opportunity to clean up any state when the program is closing.
     * Plugins should avoid doing anything too heavyweight here.
     */
    public void shutDown() throws Exception {
    }
}
