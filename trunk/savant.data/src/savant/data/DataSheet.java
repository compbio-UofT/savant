/*
 *    Copyright 2009-2011 University of Toronto
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.RowFilter.Entry;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import savant.api.adapter.TrackAdapter;
import savant.api.util.DialogUtils;
import savant.api.util.SelectionUtils;
import savant.api.util.TrackUtils;
import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangeCompletedListener;
import savant.controller.event.SelectionChangedEvent;
import savant.controller.event.SelectionChangedListener;
import savant.data.types.Record;


/**
 * Manages the UI for the Data Table Plugin.
 *
 * @author mfiume
 */
public class DataSheet implements LocationChangeCompletedListener, SelectionChangedListener {

    private JComboBox trackList;
    private ExtendedTable table;
    private boolean autoUpdate = true;
    private boolean onlySelected = false;
    private JLabel numItemsLabel;
    private TrackAdapter currentTrack;
    private DataTableModel tableModel;
    private List<Record> data;

    public DataSheet(JPanel panel) {

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
                if (currentTrack == null || (trackList.getSelectedItem() != null && trackList.getSelectedItem() != currentTrack)) {
                    setCurrentTrack((TrackAdapter) trackList.getSelectedItem());
                    presentDataFromCurrentTrack();
                }
            }
        });

        trackList.setBackground(Color.lightGray);
        toolbar.add(trackList);

        toolbar.add(Box.createHorizontalGlue());

        JCheckBox autoUpdateCheck = new JCheckBox();
        autoUpdateCheck.setText("Auto Update");
        autoUpdateCheck.setSelected(autoUpdate);
        toolbar.add(autoUpdateCheck);

        autoUpdateCheck.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                setAutoUpdate(((JCheckBox)ce.getSource()).isSelected());
            }
        });

        JCheckBox onlySelectedCheck = new JCheckBox("Show Only Selected");
        onlySelectedCheck.setSelected(false);
        toolbar.add(onlySelectedCheck);

        onlySelectedCheck.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                setOnlySelected(((JCheckBox)ce.getSource()).isSelected());
            }
        });

        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportTable((TrackAdapter)trackList.getSelectedItem());
            }
        });
        toolbar.add(exportButton);

        // create a table (the most important component)
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
                    if (currentTrack.isSelectionAllowed()) {
                        JTable table = (JTable)e.getSource();
                        int row = table.getSelectedRow();
                        Record r = data.get(table.getRowSorter().convertRowIndexToModel(row));
                        SelectionUtils.toggleSelection(currentTrack, r);
                        currentTrack.repaint();
                        if (onlySelected) {
                            refreshData();
                        }
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
        numItemsLabel = new JLabel("0");
        jtb.add(numItemsLabel);

        jtb.setVisible(true);
        jtb.setFloatable(false);
        panel.add(jtb);
    }

    private void setAutoUpdate(boolean value) {
        autoUpdate = value;
        if (autoUpdate) {
            refreshData();
        }
    }

    private void setOnlySelected(boolean value) {
        onlySelected = value;
        TableRowSorter sorter = (TableRowSorter<DataTableModel>)table.getRowSorter();
        if (onlySelected) {
            sorter.setRowFilter(new RowFilter<DataTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DataTableModel, ? extends Integer> entry) {
                    return table.getRowSelected(entry.getIdentifier());
                }
            });
        } else {
            sorter.setRowFilter(null);
        }
    }

    private void setCurrentTrack(TrackAdapter t) {
        currentTrack = t;
    }

    private void presentDataFromCurrentTrack() {
        TrackAdapter t = currentTrack;
        tableModel = new DataTableModel(currentTrack.getDataSource());
        table.setModel(tableModel);
        table.setSurrendersFocusOnKeystroke(true);
        refreshData();
        refreshSelection();
    }

    void updateTrackList() {
        trackList.removeAllItems();
        for (TrackAdapter t : TrackUtils.getTracks()) {
            trackList.addItem(t);
        }
    }

    private void refreshData() {
        if (tableModel == null) { return; }
        data = currentTrack.getDataInRange();

        tableModel.setData(data);
        tableModel.fireTableDataChanged();
        if (data == null) {
            //label_num_items.setText(MiscUtils.intToString(0));
            numItemsLabel.setText("(zoom in for records)");
        } else {
            String s = "";

            s += data.size();

            if (((TableRowSorter<DataTableModel>)table.getRowSorter()).getRowFilter() != null) {
                s += " (showing " + table.getRowSorter().getViewRowCount() + " selected)";
            } else {
                DataTableModel dtm = (DataTableModel)tableModel;
                if (data.size() > dtm.getMaxRows()) {
                    s += " (showing first " + dtm.getMaxRows() + ")";
                }
            }

            numItemsLabel.setText(s);
        }
    }

    @Override
    public void locationChangeCompleted(LocationChangedEvent event) {
        if (autoUpdate) {
            refreshData();
            refreshSelection();
        }
    }

    private void exportTable(TrackAdapter track) {

        String[] choices = { "All records", "Only selected records" };
        Object input = choices[0];
        if (table.hasSelectedRows()) {
            input = JOptionPane.showInputDialog(null, "Export: ", "Export", JOptionPane.QUESTION_MESSAGE, null, choices,  choices[0]);
        }

        if (input != null) {

            String defaultName;
            switch (track.getDataFormat()) {
                case SEQUENCE_FASTA:
                    defaultName = "Export.fa";
                    break;
                case INTERVAL_BAM:
                    // If the file-extension is .bam, we will create both a .bam and .bai file.
                    defaultName = "Export.bam";
                    break;
                default:
                    defaultName = "Export.txt";
            }
            File selectedFile = DialogUtils.chooseFileForSave("Export Data", defaultName);

            if (selectedFile != null) {
                try {
                    if (input.equals(choices[0])){
                        exportAllInView(track, selectedFile);
                    } else if (input.equals(choices[1])){
                        exportSelectedInView(track, selectedFile);
                    }
                } catch (IOException ex) {
                    DialogUtils.displayException("Export unsuccessful", "Unable to export rows to file.", ex);
                }
            }
        }
    }

    private void exportAllInView(TrackAdapter track, File selectedFile) throws IOException {
        DataTableModel dtm = new DataTableModel(track.getDataSource());
        dtm.setMaxRows(Integer.MAX_VALUE);
        dtm.setData(track.getDataInRange());
        dtm.openExport(selectedFile);

        int numRows = dtm.getRowCount();
        for (int i = 0; i < numRows; i++) {
            dtm.exportRow(i);
        }
        dtm.closeExport();
    }

    private void exportSelectedInView(TrackAdapter track, File selectedFile) throws IOException {
        DataTableModel dtm = new DataTableModel(track.getDataSource());
        dtm.setMaxRows(Integer.MAX_VALUE);
        dtm.setData(track.getDataInRange());
        dtm.openExport(selectedFile);

        int numRows = dtm.getRowCount();
        for (int i = 0; i < numRows; i++) {
            if (table.getRowSelected(i)){
                dtm.exportRow(i);
            }
        }
        dtm.closeExport();
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        if (onlySelected) {
            refreshSelection();
            refreshData();
        } else if (autoUpdate) {
            refreshSelection();
        }
    }

    public void refreshSelection(){
        if(currentTrack == null || table == null || data == null) return;
        table.clearSelectedRows();
        List<Record> selected = currentTrack.getSelectedDataInRange();

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

        public ExtendedTable(){
            setDefaultRenderer(Boolean.class, new BooleanRenderer());
        }

        public void clearSelectedRows(){
            selectedRows.clear();
        }

        public void addSelectedRow(Integer i){
            selectedRows.add(i);
        }

        public boolean getRowSelected(int row){
            return selectedRows.contains(row);
        }

        public boolean hasSelectedRows() {
            return selectedRows.size() > 0;
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

        /**
         * This class is used to override background colour of selected Boolean cells
         * in table, which cannot be done as other cells are (JCheckBox).
         */
        private class BooleanRenderer implements TableCellRenderer {

            private TableCellRenderer defaultRenderer;

            public BooleanRenderer(){
                defaultRenderer = getDefaultRenderer(Boolean.class);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modRow = getRowSorter().convertRowIndexToModel(row);
                if (!isSelected && getRowSelected(modRow)) {
                    comp.setBackground(Color.green);
                }

                return comp;
            }
        }
    }
}
