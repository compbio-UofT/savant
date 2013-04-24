/*
 *    Copyright 2010-2012 University of Toronto
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
