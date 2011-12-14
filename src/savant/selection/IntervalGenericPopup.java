/*
 *    Copyright 2010-2011 University of Toronto
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
import savant.api.data.IntervalRecord;


/**
 * Popup to display annotation information about a generic interval record.
 *
 * @author AndrewBrook
 */
public class IntervalGenericPopup extends PopupPanel {

    protected IntervalGenericPopup() {
    }

    @Override
    protected void calculateInfo() {
        IntervalRecord rec = (IntervalRecord)record;
        name = rec.getName();
        ref = rec.getReference();
        start = rec.getInterval().getStart();
        end = rec.getInterval().getEnd();
    }

    @Override
    protected void initInfo() {
        add(new JLabel("Name: " + name));
        add(new JLabel("Start: " + start));
        add(new JLabel("End: " + end));
        add(new JLabel("Length: " + (end - start)));
    }

    @Override
    protected void initSpecificButtons() {
        initIntervalJumps();
    }
}
