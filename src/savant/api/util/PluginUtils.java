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
package savant.api.util;

import java.io.File;
import java.net.URL;
import savant.api.event.PluginEvent;
import savant.plugin.PluginController;
import savant.settings.DirectorySettings;
import savant.view.dialog.DownloadDialog;

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
}
