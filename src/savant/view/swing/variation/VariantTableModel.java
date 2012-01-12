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
package savant.view.swing.variation;

import java.util.List;
import javax.swing.table.AbstractTableModel;

import savant.api.data.VariantRecord;

/**
 * Dirt-simple model class for populating a table of variant records.
 *
 * @author tarkvara
 */
class VariantTableModel extends AbstractTableModel {

    private static final Class[] COLUMN_CLASSES = { String.class, String.class, Integer.class, String.class, String.class };
    private static final String[] COLUMN_NAMES = { "Name", "Type", "Position", "Ref", "Alt" };
    
    private final List<VariantRecord> data;

    VariantTableModel(List<VariantRecord> data) {
        this.data = data;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Class getColumnClass(int column) {
        return COLUMN_CLASSES[column];
     }

    @Override
    public Object getValueAt(int row, int column) {
        if (data != null) {
            VariantRecord rec = data.get(row);

            switch(column) {
                case 0:
                    return rec.getName();
                case 1:
                    return rec.getVariantType().getDescription();
                case 2:
                    return rec.getInterval().getStart();
                case 3:
                    return rec.getRefBases();
                case 4:
                    return rec.getAltBases();
                default:
                    return "";
            }
        }
        return "";
    }

    @Override
    public int getRowCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }
}