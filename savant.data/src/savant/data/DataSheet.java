/*
 *    Copyright 2009-2010 University of Toronto
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;

import savant.api.adapter.ViewTrackAdapter;
import savant.api.util.DialogUtils;
import savant.api.util.SelectionUtils;
import savant.api.util.TrackUtils;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.RangeChangeCompletedListener;
import savant.controller.event.SelectionChangedEvent;
import savant.controller.event.SelectionChangedListener;
import savant.controller.event.ViewTrackListChangedEvent;
import savant.controller.event.ViewTrackListChangedListener;
import savant.data.types.Record;
import savant.plugin.PluginAdapter;


/**
 *
 * @author mfiume
 */
public class DataSheet implements RangeChangeCompletedListener, ViewTrackListChangedListener, SelectionChangedListener {

    private JComboBox trackList;
    private ExtendedTable table;
    private boolean autoUpdate = true;
    private JCheckBox autoUpdateCheckBox;
    private JLabel label_num_items;
    private ViewTrackAdapter currentViewTrack;
    private DataTableModel tableModel;
    private List<Record> data;

    public DataSheet(JPanel panel, PluginAdapter pluginAdapter) {

        // set the layout of the data sheet
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        /**
         * Create a toolbar.
         */
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setMinimumSize(new Dimension(22,22));
        toolbar.setPreferredSize(new Dimension(22,22));
        toolbar.setMaximumSize(new Dimension(999999,22));
        panel.add(toolbar);

        // add a label to the toolbar
        JLabel l = new JLabel();
        l.setText(" Track: ");
        toolbar.add(l);

        // add a dropdown, populated with tracks
        trackList = new JComboBox();
        trackList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentViewTrack == null || (trackList.getSelectedItem() != null && trackList.getSelectedItem() != currentViewTrack)) {
                    setCurrentTrack((ViewTrackAdapter) trackList.getSelectedItem());
                    presentDataFromCurrentTrack();
                }
            }
        });
        trackList.setMinimumSize(new Dimension(100,100));
        trackList.setMaximumSize(new Dimension(500,500));
        trackList.setPreferredSize(new Dimension(300,300));
        trackList.setBackground(Color.lightGray);
        toolbar.add(trackList);

        toolbar.add(Box.createHorizontalGlue());

        autoUpdateCheckBox = new JCheckBox();
        autoUpdateCheckBox.setText("Auto Update");
        autoUpdateCheckBox.setSelected(autoUpdate);
        toolbar.add(autoUpdateCheckBox);

        autoUpdateCheckBox.addChangeListener(
            new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    setAutoUpdate(autoUpdateCheckBox.isSelected());
                }
            }
        );

        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportTable(table, (ViewTrackAdapter)trackList.getSelectedItem());
            }
        });
        toolbar.add(exportButton);

        // create a table (the most important component)
        //table = new JTable();
        table = new ExtendedTable();
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        JPanel tmp = new JPanel();
        tmp.setLayout(new BoxLayout(tmp,BoxLayout.Y_AXIS));
        tmp.add(table.getTableHeader());
        tmp.add(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (currentViewTrack.isSelectionAllowed()) {
                        JTable table = (JTable)e.getSource();
                        int row = table.getSelectedRow();
                        Record r = data.get(table.getRowSorter().convertRowIndexToModel(row));
                        SelectionUtils.toggleSelection(currentViewTrack, r);
                        currentViewTrack.repaint();
                    }
                }
            }
        });

        JScrollPane jsp = new JScrollPane(tmp);

        panel.add(jsp);

        JToolBar jtb = new JToolBar();
        JLabel label_num_items_title = new JLabel("Number of records: ");
        label_num_items_title.setBackground(jtb.getBackground());
        jtb.add(label_num_items_title);
        label_num_items = new JLabel("0");
        jtb.add(label_num_items);

        jtb.setVisible(true);
        jtb.setFloatable(false);
        panel.add(jtb);
    }

    private void setAutoUpdate(boolean au) {
        autoUpdate = au;
        if (autoUpdate) {
            refreshData();
        }
    }

    private void setCurrentTrack(ViewTrackAdapter t) {
        currentViewTrack = t;
    }

    private void presentDataFromCurrentTrack() {
        ViewTrackAdapter t = currentViewTrack;
        tableModel = new DataTableModel(currentViewTrack.getDataSource().getDataFormat(), t.getDataInRange());
        table.setModel(tableModel);
        table.setSurrendersFocusOnKeystroke(true);
        refreshData();
        refreshSelection();
    }

    private void updateTrackList() {
        trackList.removeAllItems();
        for (ViewTrackAdapter t : TrackUtils.getTracks()) {
            trackList.addItem(t);
        }
    }

    private void refreshData() {
        if (tableModel == null) { return; }
        data = currentViewTrack.getDataInRange();

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



    }

    @Override
    public void rangeChangeCompletedReceived(RangeChangedEvent event) {
        if (autoUpdate) {
            refreshData();
            refreshSelection();
        }
    }

    @Override
    public void viewTrackListChangeReceived(ViewTrackListChangedEvent event) {
        updateTrackList();
    }

    private static void exportTable(JTable table, ViewTrackAdapter track) {

        String[] choices = { "All data in view", "All selected data in view", "All selected data"};
        String input = (String) JOptionPane.showInputDialog(null, "Export: ",
            "Export", JOptionPane.QUESTION_MESSAGE, null,
            choices, // Array of choices
            choices[0]); // Initial choice

        if(input == null) return;

        File selectedFile = DialogUtils.chooseFileForSave("Export Data", "Export.txt");

        // set the genome
        if (selectedFile != null) {
            try {
                if(input.equals("All data in view")){
                    exportAllInView(table, track, selectedFile);
                } else if(input.equals("All selected data in view")){
                    exportSelectedInView(table, track, selectedFile);
                } else if(input.equals("All selected data")){
                    exportAllSelected(table, track, selectedFile);
                }                
            } catch (IOException ex) {
                String message = "Export unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void exportAllInView(JTable table, ViewTrackAdapter track, File selectedFile) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile));

        DataTableModel dtm = (DataTableModel) table.getModel();
        int numRows = dtm.getRowCount();
        int numCols = dtm.getColumnCount();

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                bw.write(dtm.getValueAt(i, j) + "\t");
            }
            bw.write("\n");
        }

        bw.close();
    }

    private static void exportSelectedInView(JTable table, ViewTrackAdapter track, File selectedFile) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile));

        DataTableModel dtm = (DataTableModel) table.getModel();
        int numRows = dtm.getRowCount();
        int numCols = dtm.getColumnCount();

        List<Integer> selectedRows = ((ExtendedTable)table).getRowsSelected();
        for (int i = 0; i < numRows; i++) {
            if(!selectedRows.contains(i)){
                continue;
            }
            for (int j = 0; j < numCols; j++) {
                bw.write(dtm.getValueAt(i, j) + "\t");
            }
            bw.write("\n");
        }

        bw.close();
    }

    private static void exportAllSelected(JTable table, ViewTrackAdapter track, File selectedFile) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile));
        
        // FIXME: Does this actually do the same as the commented-out lines?
//      List dataInRange = SelectionController.getInstance().getSelections(track.getURI());
//      DataTableModel dtm = new DataTableModel(track.getDataType(), dataInRange);
        DataTableModel dtm = new DataTableModel(track.getDataSource().getDataFormat(), track.getDataInRange());

        int numRows = dtm.getRowCount();
        int numCols = dtm.getColumnCount();

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                bw.write(dtm.getValueAt(i, j) + "\t");
            }
            bw.write("\n");
        }

        bw.close();        
    }

    @Override
    public void selectionChangeReceived(SelectionChangedEvent event) {
        if (autoUpdate) {
            refreshSelection();
        }
    }

    public void refreshSelection(){
        if(currentViewTrack == null || table == null || data == null) return;
        table.clearSelectedRows();
        //List<Comparable> selected = SelectionController.getInstance().getSelections(currentViewTrack.getURI());
        List<Record> selected = currentViewTrack.getSelectedDataInRange();

        if (selected != null) {
            for (Record c : selected) {
                int index = data.indexOf(c);
                if (index >= 0) {
                    table.addSelectedRow(index);
                }
            }
        }
        table.updateUI();
    }

    private class ExtendedTable extends JTable {

        private List<Integer> selectedRows = new ArrayList<Integer>();

        public void clearSelectedRows(){
            selectedRows.clear();
        }

        public void addSelectedRow(Integer i){
            selectedRows.add(i);
        }

        public List<Integer> getRowsSelected(){
            return selectedRows;
        }

        @Override
        public TableCellRenderer getCellRenderer (int row, int column){
            int modRow = table.getRowSorter().convertRowIndexToModel(row);
            if (selectedRows.contains(modRow)){
                super.getCellRenderer(row, column).getTableCellRendererComponent(this, null, false, false, row, column).setBackground(Color.GREEN);
            } else {
                super.getCellRenderer(row, column).getTableCellRendererComponent(this, null, false, false, row, column).setBackground(Color.WHITE);
            }
            return super.getCellRenderer(row, column);
        }

    }
}
