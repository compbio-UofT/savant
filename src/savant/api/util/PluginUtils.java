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

import savant.api.event.PluginEvent;
import savant.plugin.PluginController;

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
}
