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

import java.util.List;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import org.apache.commons.lang3.StringUtils;

import savant.api.data.VariantRecord;
import savant.view.variation.LDRecord;
import savant.view.variation.ParticipantRecord;
import savant.view.variation.VariationController;


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
        if (record instanceof LDRecord) {
            LDRecord ldRec = (LDRecord)record;
            float dPrime = ldRec.getDPrime();
            if (!Float.isNaN(dPrime)) {
                add(new JLabel(String.format("D\u2032: %.2f", dPrime)));
            }
            add(new JLabel(String.format("r\u00B2: %.2f", ldRec.getRSquared())));

            List<VariantRecord> varRecs = ldRec.getConstituents();
            VariantRecord varRec0 = varRecs.get(0);
            VariantRecord varRec1 = varRecs.get(1);

            add(new JSeparator());
            initVariantRecord(varRec0, null);
            add(new JSeparator());
            initVariantRecord(varRec1, null);
            name = VariationController.getDisplayName(varRec0) + " vs. " + VariationController.getDisplayName(varRec1);
            start = Math.min(varRec0.getPosition(), varRec1.getPosition());
            end = Math.max(varRec0.getPosition(), varRec1.getPosition());
        } else {
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
            initVariantRecord(varRec, partRec);
        }
    }
    
    private void initVariantRecord(VariantRecord varRec, ParticipantRecord partRec) {
        if (varRec.getName() != null) {
            add(new JLabel("Variant Name: " + varRec.getName()));
        }
        add(new JLabel("Type: " + varRec.getVariantType().getDescription()));
        add(new JLabel("Position: " + varRec.getPosition()));
        add(new PopupLabel("Reference: " + varRec.getRefBases()));
        if (partRec == null) {
            add(new PopupLabel("Alt: " + StringUtils.join(varRec.getAltAlleles(), ',')));
        } else {
            add(new JSeparator());
            add(new JLabel("Participant: " + partRec.getName()));
            add(new PopupLabel("Alleles: " + StringUtils.join(partRec.getAlleles(), ',')));
        }
    }
}
