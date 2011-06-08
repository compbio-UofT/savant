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
package savant.tableview;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import savant.api.adapter.TrackAdapter;
import savant.api.util.NavigationUtils;
import savant.api.util.SelectionUtils;
import savant.api.util.TrackUtils;
import savant.controller.event.RangeChangeCompletedListener;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.SelectionChangedEvent;
import savant.controller.event.SelectionChangedListener;
import savant.controller.event.TrackListChangedEvent;
import savant.controller.event.TrackListChangedListener;
import savant.file.DataFormat;
import savant.plugin.PluginAdapter;
import savant.plugin.SavantPanelPlugin;
import savant.tableview.table.SearchableTablePanel;

public class TableView
        extends SavantPanelPlugin
        implements RangeChangeCompletedListener, TrackListChangedListener, SelectionChangedListener {

    private JComboBox trackList;
    private JPanel centralPanel;
    private TrackAdapter currentTrack;
    SearchableTablePanel tablePanel;

    @Override
    public void init(JPanel tablePanel, PluginAdapter pluginAdapter) {

        initGUI(tablePanel);
        subscribeToEvents(pluginAdapter);

    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public String getTitle() {
        return "Table View ++";
    }

    private void initGUI(JPanel tablePanel) {
        JMenuBar bar = new JMenuBar();
        JLabel l = new JLabel("Track: ");
        trackList = new JComboBox();
        trackList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTrack == null || (trackList.getSelectedItem() != null && trackList.getSelectedItem() != currentTrack)) {
                    setCurrentTrack((TrackAdapter) trackList.getSelectedItem());
                    prepareCurrentTrack();
                    //presentDataFromCurrentTrack();
                }
            }
        });

        updateTrackList();

        bar.setFloatable(false);
        bar.setAlignmentX(JToolBar.CENTER_ALIGNMENT);
        bar.add(l);
        bar.add(trackList);

        Vector names = new Vector();
        names.add("<No Track Selected>");
        this.tablePanel = new SearchableTablePanel(new Vector(), names);

        centralPanel = new JPanel();
        centralPanel.setLayout(new BorderLayout());
        centralPanel.add(this.tablePanel);

        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(bar, BorderLayout.NORTH);
        tablePanel.add(centralPanel, BorderLayout.CENTER);
    }

    private void subscribeToEvents(PluginAdapter pluginAdapter) {
        NavigationUtils.addRangeChangeListener(this);
        SelectionUtils.addSelectionChangedListener(this);
        TrackUtils.addTracksChangedListener(this);
    }

    private void setCurrentTrack(TrackAdapter t) {
        currentTrack = t;
    }

    @Override
    public void rangeChangeCompletedReceived(RangeChangedEvent event) {
        refreshData();
    }

    @Override
    public void trackListChangeReceived(TrackListChangedEvent event) {
        updateTrackList();
    }

    @Override
    public void selectionChangeReceived(SelectionChangedEvent event) {
    }

    private void updateTrackList() {

        trackList.removeAllItems();
        if (TrackUtils.getTracks().size() == 0) {
            trackList.setEnabled(false);
        } else {
            trackList.setEnabled(true);
            for (TrackAdapter t : TrackUtils.getTracks()) {
                trackList.addItem(t);
            }
        }
    }

    private void refreshData() {
        if (currentTrack.getDataSource().getDataFormat() == DataFormat.TABIX) {
            this.tablePanel.setTableModel(
                    SavantRecordModel.getDataForTrack(currentTrack),
                    SavantRecordModel.getColumnNamesForTrack(currentTrack),
                    SavantRecordModel.getColumnClassesForTrack(currentTrack));
        } else {
            this.tablePanel.updateData(SavantRecordModel.getDataForTrack(currentTrack));
        }
    }

    private void prepareCurrentTrack() {

        if (currentTrack != null) {
            this.tablePanel.setTableModel(
                    SavantRecordModel.getDataForTrack(currentTrack),
                    SavantRecordModel.getColumnNamesForTrack(currentTrack),
                    SavantRecordModel.getColumnClassesForTrack(currentTrack));
        }
    }

    

    


}
