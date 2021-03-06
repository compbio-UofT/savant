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
package savant.api.util;

import java.io.File;
import java.net.URL;
import javax.swing.JPanel;

import savant.api.event.PluginEvent;
import savant.plugin.PluginController;
import savant.settings.DirectorySettings;
import savant.view.dialog.DownloadDialog;
import savant.view.swing.Savant;

/**
 * Utility functions by which plugins can deal with the plugin interface itself.
 *
 * @author tarkvara
 */
public class PluginUtils {
    /**
     * Add a listener to monitor plugin loading and unloading.
     *
     * @param l the listener to be added
     */
    public static void addPluginListener(Listener<PluginEvent> l) {
        PluginController.getInstance().addListener(l);
    }

    /**
     * Remove an existing plugin listener
     *
     * @param l the listener to be removed
     */
    public static void removePluginListener(Listener<PluginEvent> l) {
        PluginController.getInstance().removeListener(l);
    }
    
    /**
     * Download a plugin from the net and install it.
     *
     * @param url URL of the jar file containing the plugin
     * @since 2.0.0
     */
    public static void installPlugin(URL url) {
        DownloadDialog dd = new DownloadDialog(DialogUtils.getMainWindow(), true);
        dd.downloadFile(url, DirectorySettings.getPluginsDirectory(), null);

        if (dd.getDownloadedFile() != null) {
            installPlugin(dd.getDownloadedFile());
        }
    }

    
    /**
     * Install a plugin from a local file.
     *
     * @param f jar file containing the plugin
     * @since 2.0.0
     */
    public static void installPlugin(File f) {
        try {
            PluginController.getInstance().installPlugin(f);
        } catch (Throwable x) {
            DialogUtils.displayException("Installation Error", String.format("<html>Unable to install <i>%s</i>: %s.</html>", f.getName(), x), x);
        }
    }
    
    /**
     * Access to a JPanel on which plugins can place toolbar items for quick access.  The JPanel
     * is located just above the main Savant tool-bar and has a FlowLayout.
     *
     * @return a JPanel near the top of the screen
     * @since 2.0.0
     */
    public static JPanel getPluginToolbar() {
        return Savant.getInstance().getPluginToolbar();
    }
}
