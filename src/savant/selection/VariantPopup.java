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

import savant.api.data.VariantRecord;


/**
 * Popup panel which is displayed over a variant track.
 *
 * @author tarkvara
 */
public class VariantPopup extends PopupPanel {
    private String type;
    private int position;
    private String refBases;
    private String altBases;

    @Override
    protected void calculateInfo() {
        VariantRecord rec = (VariantRecord)record;
        type = rec.getVariantType().name();
        position = rec.getInterval().getStart();
        refBases = rec.getRefBases();
        altBases = rec.getAltBases();
    }

    @Override
    protected void initInfo() {
        add(new JLabel("Type: " + type));
        add(new JLabel("Position: " + position));
        add(new JLabel("Reference: " + refBases));
        add(new JLabel("Alt: " + altBases));
    }
}
