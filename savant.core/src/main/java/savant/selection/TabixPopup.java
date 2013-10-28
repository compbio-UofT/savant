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

package savant.selection;

import java.util.Map;
import javax.swing.JLabel;

import savant.api.adapter.DataSourceAdapter;
import savant.data.types.GFFIntervalRecord;
import savant.data.types.TabixIntervalRecord;


/**
 * Popup panel for Tabix interval records.
 * @author AndrewBrook
 */
public class TabixPopup extends PopupPanel {

    private String[] columnNames;

    public TabixPopup(DataSourceAdapter dataSource) {
        columnNames = dataSource.getColumnNames();
    }

    @Override
    protected void initInfo() {
        TabixIntervalRecord rec = (TabixIntervalRecord)record;
        name = rec.getName();
        ref = rec.getReference();
        start = rec.getInterval().getStart();
        end = rec.getInterval().getEnd();
        String[] values = rec.getValues();
        boolean gff = rec instanceof GFFIntervalRecord;
        
        for (int i = 0; i < columnNames.length && i < values.length; i++) {
            if (columnNames[i] != null && !(gff && i == GFFIntervalRecord.ATTRIBUTE_COLUMN)) {
                add(new JLabel(columnNames[i] + ":\t" + values[i]));
            }
        }
        if (gff) {
            Map<String, String> attributes = ((GFFIntervalRecord)rec).getAttributes();
            for (String k: attributes.keySet()) {
                add(new JLabel(k + ":\t" + attributes.get(k)));
            }
        }
    }
    
    @Override
    protected void initSpecificButtons() {
        initIntervalJumps();
    }
}
