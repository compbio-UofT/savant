/*
 * GUIPlugin.java
 * Created on Feb 23, 2010
 *
 *
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

package savant.plugin;

import javax.swing.JPanel;
import org.java.plugin.Plugin;

public abstract class SavantPanelPlugin extends Plugin {

    /**
     * This method is called once during application life cycle to allow a third-party
     * plugin to initialize and show itself.
     *
     * @param panel parent panel for auxiliary data components
     * @param adapter gives access to functionality provided by Savant
     */
    public abstract void init(JPanel panel, PluginAdapter pluginAdapter);

    /**
     * @return title to be used in Plugins menu and for frame in which plugin is rendered
     */
    public abstract String getTitle();
}
