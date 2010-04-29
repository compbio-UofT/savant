/*
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

/*
 * DataTab.java
 * Created on Feb 25, 2010
 */

package savant.data;

import org.java.plugin.Plugin;
import savant.plugin.AuxData;
import savant.plugin.PluginAdapter;
import savant.view.swing.BrowserDefaults;

import javax.swing.*;
import java.awt.*;

public class DataTab extends Plugin implements AuxData {

    public void init(JTabbedPane tabbedPane, PluginAdapter pluginAdapter) {
        JPanel tablePanel = createTabPanel(tabbedPane, "Data View");
        savant.data.DataSheet currentRangeDataSheet = new DataSheet(tablePanel, pluginAdapter);
        pluginAdapter.getRangeController().addRangeChangedListener(currentRangeDataSheet);
        pluginAdapter.getViewTrackController().addTracksChangedListener(currentRangeDataSheet);
    }

    private JPanel createTabPanel(JTabbedPane jtp, String name) {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.setBackground(BrowserDefaults.colorTabBackground);
        jtp.addTab(name, pan);
        return pan;
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }
}
