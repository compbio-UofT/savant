/*
 *    Copyright 2012 University of Toronto
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
package savant.util.swing;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import savant.api.data.Record;
import savant.api.event.SelectionChangedEvent;
import savant.api.util.Listener;
import savant.controller.TrackController;
import savant.selection.SelectionController;
import savant.view.tracks.Track;

/**
 * Adapted from table used by Data Table plugin.  Just a table which maintains a selection
 * corresponding to the currently-selected records.
 *
 * @author tarkvara
 */
public class RecordTable extends JTable {

    private List<Integer> selectedRows = new ArrayList<Integer>();

    public RecordTable(RecordTableModel model) {
        super(model);
        setAutoCreateRowSorter(true);
        setDefaultRenderer(Boolean.class, new BooleanRenderer());

        SelectionController.getInstance().addListener(new Listener<SelectionChangedEvent>() {
            @Override
            public void handleEvent(SelectionChangedEvent event) {
                refreshSelection();
            }
        });

    }

    public void refreshSelection() {
        selectedRows.clear();

        Set<Record> allSelected = new HashSet<Record>();
        for (Track t: TrackController.getInstance().getTracks()) {
            List<Record> selected = t.getSelectedDataInRange();
            if (selected != null) {
                allSelected.addAll(selected);
            }
        }
        RecordTableModel model = (RecordTableModel)getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Record rec = model.getRecord(i);
            if (allSelected.contains(rec)) {
                selectedRows.add(i);
            }
        }
        updateUI();
    }
    
    @Override
    public TableCellRenderer getCellRenderer(int row, int column){
        int modRow = getRowSorter().convertRowIndexToModel(row);
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

        private final TableCellRenderer defaultRenderer;

        public BooleanRenderer(){
            defaultRenderer = getDefaultRenderer(Boolean.class);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int modRow = getRowSorter().convertRowIndexToModel(row);
            if (!isSelected && selectedRows.contains(modRow)) {
                comp.setBackground(Color.green);
            }

            return comp;
        }
    }
}
