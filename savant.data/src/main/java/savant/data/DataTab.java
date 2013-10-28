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
package savant.data;

import javax.swing.JPanel;

import savant.api.event.TrackEvent;
import savant.api.util.Listener;
import savant.api.util.TrackUtils;
import savant.plugin.SavantPanelPlugin;


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
    }

    @Override
    public String getTitle() {
        return "Data Table";
    }
}
