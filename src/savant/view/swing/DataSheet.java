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
package savant.view.swing;

import savant.controller.ViewTrackController;
import savant.controller.event.viewtrack.ViewTrackListChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.view.swing.Savant;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;
import savant.view.swing.model.DataTableModel;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author mfiume
 */
public class DataSheet implements RangeChangedListener, ViewTrackListChangedListener {

    private JComboBox trackList;
    private JTable table;
    private boolean autoUpdate = true;
    private JCheckBox autoUpdateCheckBox;
    private Savant parent;
    private ViewTrack currentViewTrack;
    private DataTableModel tableModel;

    public DataSheet(Savant parent, JPanel panel) {

        // set the parent
        this.parent = parent;

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
        l.setText("Track: ");
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
        toolbar.add(trackList);

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
        table = new JTable();
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        JPanel tmp = new JPanel();
        tmp.setLayout(new BoxLayout(tmp,BoxLayout.Y_AXIS));
        tmp.add(table.getTableHeader());
        tmp.add(table);

        JScrollPane jsp = new JScrollPane(tmp);

        panel.add(jsp);
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
        /*
        if (!tableModel.hasEmptyRow()) {
             tableModel.addEmptyRow();
         }
         */
    }

    private void updateTrackList() {
        this.trackList.removeAllItems();
        ViewTrackController tc = ViewTrackController.getInstance();
        for (ViewTrack t : tc.getTracks()) {
            trackList.addItem(t);
        }
    }

    private void refreshData() {
        if (tableModel == null) { return; }
        tableModel.setData(currentViewTrack.getDataInRange());
        tableModel.fireTableDataChanged();
    }

    public void rangeChangeReceived(RangeChangedEvent event) {
        if (this.autoUpdate) {
            refreshData();
        }
    }

    public void viewTrackListChangeReceived(ViewTrackListChangedEvent event) {
        updateTrackList();
    }

    private static void exportTable(JTable table, ViewTrack track) {

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
                exportTable(table, track, selectedFileName);
            } catch (IOException ex) {
                String message = "Export unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void exportTable(JTable table, ViewTrack track, String selectedFileName) throws IOException {
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

}
