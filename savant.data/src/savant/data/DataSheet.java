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
import java.awt.event.MouseEvent;
import savant.controller.ViewTrackController;
import savant.controller.event.selection.SelectionChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.plugin.PluginAdapter;
import savant.view.swing.Savant;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;
import savant.view.swing.ViewTrack;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;
import savant.controller.RangeController;
import savant.controller.SelectionController;
import savant.controller.event.selection.SelectionChangedListener;
import savant.util.MiscUtils;
import savant.view.swing.GraphPane;

/**
 *
 * @author mfiume
 */
public class DataSheet implements RangeChangedListener, ViewTrackListChangedListener, SelectionChangedListener {

    private PluginAdapter pluginAdapter;

    private JComboBox trackList;
    private JTable table;
    private boolean autoUpdate = true;
    private JCheckBox autoUpdateCheckBox;
    private JLabel label_num_items;
    private ViewTrack currentViewTrack;
    private DataTableModel tableModel;
    private List<Object> data;

    public DataSheet(JPanel panel, PluginAdapter pluginAdapter) {

        this.pluginAdapter = pluginAdapter;

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
            public void actionPerformed(ActionEvent e) {
                if (currentViewTrack == null || (trackList.getSelectedItem() != null && trackList.getSelectedItem() != currentViewTrack)) {
                    setCurrentTrack((ViewTrack) trackList.getSelectedItem());
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
                public void stateChanged(ChangeEvent e) {
                    setAutoUpdate(autoUpdateCheckBox.isSelected());
                }
            }
        );

        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportTable(table, (ViewTrack) trackList.getSelectedItem());
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

        table.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if(!currentViewTrack.getTrackRenderers().get(0).selectionAllowed()) return;
                    JTable table = (JTable)e.getSource();
                    int row = table.getSelectedRow();
                    GraphPane gp = currentViewTrack.getFrame().getGraphPane();
                    Object o = data.get(table.getRowSorter().convertRowIndexToModel(row));
                    SelectionController.getInstance().toggleSelection(currentViewTrack.getURI(), (Comparable) o);
                    gp.repaint();
                }
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
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
        this.autoUpdate = au;
        if (this.autoUpdate) { refreshData(); }
    }

    private void setCurrentTrack(ViewTrack t) {
        currentViewTrack = t;
    }

    private void presentDataFromCurrentTrack() {
        ViewTrack t = currentViewTrack;
        //Savant.log("Presenting data from track: " + t.getPath());
        tableModel = new DataTableModel(currentViewTrack.getDataType(), t.getDataInRange());
        table.setModel(tableModel);
        table.setSurrendersFocusOnKeystroke(true);
        refreshData();
        refreshSelection();
    }

    private void updateTrackList() {
        this.trackList.removeAllItems();
        ViewTrackController tc = pluginAdapter.getViewTrackController();
        for (ViewTrack t : tc.getTracks()) {
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

            s += MiscUtils.numToString(data.size());

            DataTableModel dtm = (DataTableModel) table.getModel();
            if (dtm.getIsNumRowsLimited() && data.size() > dtm.getMaxNumRows()) {
                s += " (showing first " + dtm.getMaxNumRows() + ")";
            }

            label_num_items.setText(s);
        }



    }

    public void rangeChangeReceived(RangeChangedEvent event) {
        if (this.autoUpdate) {
            refreshData();
            refreshSelection();
        }
    }

    public void viewTrackListChangeReceived(ViewTrackListChangedEvent event) {
        updateTrackList();
    }

    private static void exportTable(JTable table, ViewTrack track) {

        String[] choices = { "All data in view", "All selected data in view", "All selected data"};
        String input = (String) JOptionPane.showInputDialog(null, "Export: ",
            "Export", JOptionPane.QUESTION_MESSAGE, null,
            choices, // Array of choices
            choices[0]); // Initial choice

        if(input == null) return;

        JFrame jf = new JFrame();
        String selectedFileName;
        if (Savant.mac) {
            FileDialog fd = new FileDialog(jf, "Export Data", FileDialog.SAVE);
            fd.setVisible(true);
            jf.setAlwaysOnTop(true);
            // get the path (null if none selected)
            selectedFileName = fd.getFile();
            if (selectedFileName != null) {
                selectedFileName = fd.getDirectory() + selectedFileName;
            }
        }
        else {
            JFileChooser fd = new JFileChooser();
            fd.setDialogTitle("Export Data");
            fd.setDialogType(JFileChooser.SAVE_DIALOG);
            int result = fd.showOpenDialog(jf);
            if (result == JFileChooser.CANCEL_OPTION || result == JFileChooser.ERROR_OPTION ) return;
            selectedFileName = fd.getSelectedFile().getPath();
        }

        // set the genome
        if (selectedFileName != null) {
            try {
                if(input.equals("All data in view")){
                    exportAllInView(table, track, selectedFileName);
                } else if(input.equals("All selected data in view")){
                    exportSelectedInView(table, track, selectedFileName);
                } else if(input.equals("All selected data")){
                    exportAllSelected(table, track, selectedFileName);
                }                
            } catch (IOException ex) {
                String message = "Export unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void exportAllInView(JTable table, ViewTrack track, String selectedFileName) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFileName));

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

    private static void exportSelectedInView(JTable table, ViewTrack track, String selectedFileName) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFileName));

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

    private static void exportAllSelected(JTable table, ViewTrack track, String selectedFileName) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFileName));
        
        List dataInRange = SelectionController.getInstance().getSelections(track.getURI());
        DataTableModel dtm = new DataTableModel(track.getDataType(), dataInRange);

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

    public void selectionChangeReceived(SelectionChangedEvent event) {
        if (this.autoUpdate) {
            refreshSelection();
        }
    }

    public void refreshSelection(){
        if(currentViewTrack == null || table == null || data == null) return;
        ((ExtendedTable) table).clearSelectedRows();
        //List<Comparable> selected = SelectionController.getInstance().getSelections(currentViewTrack.getURI());
        List<Comparable> selected = SelectionController.getInstance().getSelectedFromList(currentViewTrack.getURI(), RangeController.getInstance().getRange(), this.data);
        if(selected == null) return;
        for(int i = 0; i < selected.size(); i++){
            Object o = (Object)selected.get(i);
            int index = data.indexOf(o);
            ((ExtendedTable) table).addSelectedRow(index);
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
            return this.selectedRows;
        }

        @Override
        public TableCellRenderer getCellRenderer (int row, int column){
            int modRow = table.getRowSorter().convertRowIndexToModel(row);
            if(selectedRows.contains(modRow)){
                super.getCellRenderer(row, column).getTableCellRendererComponent(this, null, false, false, row, column).setBackground(Color.GREEN);
            } else {
                super.getCellRenderer(row, column).getTableCellRendererComponent(this, null, false, false, row, column).setBackground(Color.WHITE);
            }
            return super.getCellRenderer(row, column);
        }

    }
}
