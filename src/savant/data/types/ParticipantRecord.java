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

package savant.data.types;

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
