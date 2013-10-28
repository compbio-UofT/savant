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
package savant.tableview;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
        JToolBar bar = new JToolBar();
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
