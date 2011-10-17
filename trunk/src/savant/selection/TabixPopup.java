/*
 *    Copyright 2010 University of Toronto
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

import javax.swing.JLabel;

import savant.api.adapter.DataSourceAdapter;
import savant.data.types.TabixIntervalRecord;


/**
 *
 * @author AndrewBrook
 */
public class TabixPopup extends PopupPanel {

    private TabixIntervalRecord rec;
    private String[] columnNames;

    public TabixPopup(TabixIntervalRecord rec, DataSourceAdapter dataSource) {
        this.rec = rec;
        columnNames = dataSource.getColumnNames();
    }

    @Override
    protected void calculateInfo() {
        //name = rec.getDescription();w
        name = rec.getName();
        ref = rec.getReference();
        start = rec.getInterval().getStart();
        end = rec.getInterval().getEnd();
    }

    @Override
    protected void initInfo() {
        String[] values = rec.getValues();
        
        for (int i = 0; i < columnNames.length && i < values.length; i++) {
            if (columnNames[i] != null) {
                add(new JLabel(columnNames[i] + ":\t" + values[i]));
            }
        }
    }
    
    @Override
    protected void initSpecificButtons() {
        initIntervalJumps(rec);
    }
}
