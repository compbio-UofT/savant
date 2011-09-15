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
import savant.controller.event.TrackEvent;
import savant.plugin.SavantPanelPlugin;
import savant.util.Listener;


public class DataTab extends SavantPanelPlugin {
    private DataSheet dataSheet;

    @Override
    public void init(JPanel tablePanel) {
        dataSheet = new DataSheet(tablePanel);

        TrackUtils.addTrackListener(new Listener<TrackEvent>() {
            @Override
            public void handleEvent(TrackEvent event) {
                dataSheet.updateTrackList();
            }
        });

        NavigationUtils.addLocationChangeListener(dataSheet);
        SelectionUtils.addSelectionChangedListener(dataSheet);
    }


    @Override
    public String getTitle() {
        return "Data Table";
    }
}
