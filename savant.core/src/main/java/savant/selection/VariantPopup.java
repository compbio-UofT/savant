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
