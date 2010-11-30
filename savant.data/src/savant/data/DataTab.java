/*
 * DataTab.java
 * Created on Feb 25, 2010
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

package savant.data;

import javax.swing.JPanel;

import savant.api.util.NavigationUtils;
import savant.api.util.SelectionUtils;
import savant.api.util.TrackUtils;
import savant.plugin.GUIPlugin;
import savant.plugin.PluginAdapter;

public class DataTab extends GUIPlugin {

    @Override
    public void init(JPanel tablePanel, PluginAdapter pluginAdapter) {
        DataSheet currentRangeDataSheet = new DataSheet(tablePanel, pluginAdapter);
        NavigationUtils.addRangeChangeListener(currentRangeDataSheet);
        SelectionUtils.addSelectionChangedListener(currentRangeDataSheet);
        TrackUtils.addTracksChangedListener(currentRangeDataSheet);
    }


    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public String getTitle() {
        return "Table View";
    }

}
