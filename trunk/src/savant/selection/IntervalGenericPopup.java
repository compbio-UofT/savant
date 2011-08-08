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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import savant.data.types.GenericIntervalRecord;


/**
 * Popup to display annotation information about a generic interval record.
 *
 * @author AndrewBrook
 */
public class IntervalGenericPopup extends PopupPanel {

    private GenericIntervalRecord rec;

    public IntervalGenericPopup(GenericIntervalRecord rec){
        this.rec = rec;
    }

    @Override
    protected void calculateInfo() {
        name = rec.getName();
        ref = rec.getReference();
        start = rec.getInterval().getStart();
        end = rec.getInterval().getEnd();
    }

    @Override
    protected void initInfo() {
        String readName = "Description: " + name;
        this.add(new JLabel(readName));

        String readStart = "Start: " + start;
        this.add(new JLabel(readStart));

        String readEnd = "End: " + end;
        this.add(new JLabel(readEnd));

        String readLength = "Length: " + (end - start);
        this.add(new JLabel(readLength));
    }

    @Override
    protected void initSpecificButtons() {
        initIntervalJumps(rec);
    }
}

