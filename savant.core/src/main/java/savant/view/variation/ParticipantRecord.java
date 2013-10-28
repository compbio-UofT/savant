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
package savant.view.variation;

import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;


/**
 * Pseudo-record which stores participant information.  We want this to look like a Record so
 * that we can use it as the source for a ParticipantPopup.
 *
 * @author tarkvara
 */
public class ParticipantRecord implements Record {
    VariantRecord variantRecord;
    int index;
    String participantName;

    public ParticipantRecord(VariantRecord varRec, int i, String name) {
        variantRecord = varRec;
        index = i;
        participantName = name;
    }

    @Override
    public String getReference() {
        return variantRecord.getReference();
    }

    @Override
    public int compareTo(Object t) {
        return variantRecord.compareTo(t);
    }

    public VariantRecord getVariantRecord() {
        return variantRecord;
    }

    public String getName() {
        return participantName;
    }

    public VariantType[] getVariants() {
        return variantRecord.getVariantsForParticipant(index);
    }

    public String[] getAlleles() {
        String[] altAlleles = variantRecord.getAltAlleles();
        int[] alleles = variantRecord.getAllelesForParticipant(index);
        String allele0 = alleles[0] > 0 ? altAlleles[alleles[0] - 1] : variantRecord.getRefBases();
        if (alleles.length == 1) {
            return new String[] { allele0 };
        } else {
            return new String[] { allele0, alleles[1] > 0 ? altAlleles[alleles[1] - 1] : variantRecord.getRefBases() };
        }
    }
}
