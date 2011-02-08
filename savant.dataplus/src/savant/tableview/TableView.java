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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.table.TableModel;
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
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Record;
import savant.data.types.SequenceRecord;
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
        System.out.println("Setting track to: " + t.getName());
        currentTrack = t;
    }

    /*
    private void presentDataFromCurrentTrack() {

        Vector data = new Vector();
        Vector classes = new Vector();
        Vector names = new Vector();

        Vector dataline = new Vector();
        dataline.add("this is data");
        data.add(dataline);

        classes.add(String.class);

        names.add("Bogus");

        this.tablePanel.setTableModel(data, names, classes);

        /*
        TrackAdapter t = currentTrack;
        tableModel = getTableModelForTrack(t);
        table.setModel(tableModel);
        table.setSurrendersFocusOnKeystroke(true);
        refreshData();
        refreshSelection();
         
    }
     * 
     */

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
        this.tablePanel.updateData(getDataForTrack(currentTrack));
    }

    private void prepareCurrentTrack() {

        if (currentTrack != null) {
            this.tablePanel.setTableModel(getDataForTrack(currentTrack), getColumnNamesForTrack(currentTrack), getColumnClassesForTrack(currentTrack));
        }

        /*
        data = currentTrack.

        this.tablePanel.getTable().getModel().set

        tableModel.setData(data);
        tableModel.fireTableDataChanged();
        if (data == null) {
        //label_num_items.setText(MiscUtils.intToString(0));
        label_num_items.setText("(zoom in for records)");
        } else {
        String s = "";

        //TODO: add commas? current formatting doesn't work
        //s += String.format("###,###", data.size());
        s += data.size();

        DataTableModel dtm = (DataTableModel) table.getModel();
        if (dtm.getIsNumRowsLimited() && data.size() > dtm.getMaxNumRows()) {
        s += " (showing first " + dtm.getMaxNumRows() + ")";
        }

        label_num_items.setText(s);
        }
         * 
         */
    }

    private static Vector getDataForTrack(TrackAdapter t) {

        Vector result = new Vector();
        Vector s;

        if (t.getDataInRange() == null) { return result; }

        int id = 1;

        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                for (Record r : t.getDataInRange()) {
                    s = new Vector();
                    s.add(id++);
                    s.add(new String(((SequenceRecord) r).getSequence()));
                    result.add(s);
                }
                break;
            case INTERVAL_BAM:
                for (Record r : t.getDataInRange()) {
                    BAMIntervalRecord b = (BAMIntervalRecord) r;
                    s = new Vector();
                    s.add(id++);
                    s.add(b.getSamRecord().getReadName());
                    s.add(b.getSamRecord().getReadString());
                    s.add(b.getSamRecord().getMappingQuality());
                    result.add(s);
                }
                break;
            default:
                throw new UnsupportedOperationException("");
        }

        return result;
    }


    private static Vector getColumnNamesForTrack(TrackAdapter t) {
        Vector result = new Vector();
        result.add("No.");
        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                result.add("Sequence");
                break;
            case INTERVAL_BAM:
                result.add("Read Name");
                result.add("Read Sequence");
                result.add("MQ");
                break;
            default:
                throw new UnsupportedOperationException("");
        }
        return result;
    }

    private static Vector getColumnClassesForTrack(TrackAdapter t) {
        Vector result = new Vector();
        result.add(Integer.class);

        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                result.add(String.class);
                break;
            case INTERVAL_BAM:
                result.add(String.class);
                result.add(String.class);
                result.add(Integer.class);
                break;
            default:
                throw new UnsupportedOperationException("");
        }
        return result;
    }


}
