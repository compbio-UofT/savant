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

package savant.selection;

import javax.swing.JLabel;

import savant.api.data.Interval;
import savant.api.data.VariantRecord;


/**
 * Popup panel which is displayed over a variant track.
 *
 * @author tarkvara
 */
public class VariantPopup extends PopupPanel {

    VariantPopup() {
        Interval inter = ((VariantRecord)record).getInterval();
        start = inter.getStart();
        end = inter.getEnd();
    }

    @Override
    protected void initInfo() {
        VariantRecord rec = (VariantRecord)record;
        add(new JLabel("Type: " + rec.getVariantType().name()));
        add(new JLabel("Position: " + start));
        add(new JLabel("Reference: " + rec.getRefBases()));
        add(new JLabel("Alt: " + rec.getAltBases()));
    }
}
