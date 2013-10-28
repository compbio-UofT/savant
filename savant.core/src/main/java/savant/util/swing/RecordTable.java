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
