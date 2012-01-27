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

import javax.swing.JSeparator;
import org.apache.commons.lang3.StringUtils;

import savant.api.data.VariantRecord;
import savant.data.types.ParticipantRecord;


/**
 * Popup panel which is displayed over a variant track.
 *
 * @author tarkvara
 */
public class VariantPopup extends PopupPanel {

    VariantPopup() {
    }

    @Override
    protected void initInfo() {
        VariantRecord varRec;
        ParticipantRecord partRec = null;
        if (record instanceof VariantRecord) {
            varRec = (VariantRecord)record;
        } else {
            partRec = (ParticipantRecord)record;
            varRec = partRec.getVariantRecord();
        }
        name = varRec.getName();
        start = end = varRec.getPosition();
        if (name != null) {
            add(new JLabel("Variant Name: " + name));
        }
        add(new JLabel("Type: " + varRec.getVariantType().getDescription()));
        add(new JLabel("Position: " + start));
        add(new JLabel("Reference: " + varRec.getRefBases()));
        if (partRec == null) {
            add(new JLabel("Alt: " + StringUtils.join(varRec.getAltAlleles(), ',')));
        } else {
            add(new JSeparator());
            add(new JLabel("Participant: " + partRec.getName()));
            add(new JLabel("Alleles: " + StringUtils.join(partRec.getAlleles(), ',')));
        }
    }
}
