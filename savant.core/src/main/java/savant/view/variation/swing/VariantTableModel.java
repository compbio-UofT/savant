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
package savant.view.variation.swing;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import savant.api.data.VariantRecord;
import savant.util.swing.RecordTableModel;

/**
 * Dirt-simple model class for populating a table of variant records.
 *
 * @author tarkvara
 */
class VariantTableModel extends RecordTableModel<VariantRecord> {

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
                    return rec.getPosition();
                case 3:
                    return rec.getRefBases();
                case 4:
                    return StringUtils.join(rec.getAltAlleles(), ',');
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

    @Override
    public VariantRecord getRecord(int row) {
        return data != null ? data.get(row) : null;
    }
}