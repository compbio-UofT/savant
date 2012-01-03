/*
 *    Copyright 2011 University of Toronto
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

import savant.api.data.VariantRecord;
import savant.util.ColumnMapping;

/**
 * Record type which holds variant data stored in a VCF record.
 *
 * @author tarkvara
 */
public class VCFVariantRecord extends TabixIntervalRecord implements VariantRecord {
    public static final int REF_COLUMN = 3;
    public static final int ALT_COLUMN = 4;
    public static final int FIRST_PARTICIPANT_COLUMN = 9;

    /**
     * Constructor not be be called directly, but rather through TabixIntervalRecord.valueOf.
     */
    protected VCFVariantRecord(String line, ColumnMapping mapping) {
        super(line, mapping);
    }
    
    @Override
    public String getRefBases() {
        return values[REF_COLUMN];
    }

    @Override
    public String getAltBases() {
        return values[ALT_COLUMN];
    }

    /**
     * Retrieve the number of participants represented by this record.
     */
    @Override
    public int getParticipantCount() {
        return values.length - FIRST_PARTICIPANT_COLUMN;
    }

    /**
     * Retrieve the string corresponding to the given participant.
     * @param index
     * @return 
     */
    public String getParticipantInfo(int index) {
        return values[FIRST_PARTICIPANT_COLUMN + index];
    }

    @Override
    public VariantType getVariantForParticipant(int index) {
        String info = getParticipantInfo(index);
        int colonPos = info.indexOf(':');
        if (colonPos >= 0) {
            info = info.substring(0, colonPos);
        }
        String[] alleleIndices = info.split("[|/]");
        int alleleIndex0 = Integer.parseInt(alleleIndices[0]);
        int alleleIndex1 = Integer.parseInt(alleleIndices[1]);
        if (alleleIndex0 == 0 && alleleIndex1 == 0) {
            // Participant has no variant at this location.
            return VariantType.NONE;
        }
        // TODO: Distinguish between homozygous and heterozygous variants.
        return getVariantType();
    }

    /**
     * TODO: Interpret ALT strings which contain descriptive information rather than bases.
     * TODO: Interpret ALT strings which contain multiple comma-separated value.
     * @return the type of variant represented by this record
     */
    @Override
    public VariantType getVariantType() {
        String refBases = getRefBases();
        String altBases = getAltBases();
        if (refBases.length() == 1 && altBases.length() == 1) {
            return VariantType.SNP;
        }
        if (altBases.charAt(0) == '<') {
            if (altBases.startsWith("<INS")) {
                return VariantType.INSERTION;
            } else if (altBases.startsWith("<DEL")) {
                return VariantType.DELETION;
            } else {
                return VariantType.OTHER;
            }
        }
        if (altBases.indexOf(',') >= 0) {
            // TODO: ALT contains multiple entries.  For now, we bail and call it OTHER.
            return VariantType.OTHER;
        }
        if (altBases.length() > refBases.length()) {
            return VariantType.INSERTION;
        } else if (altBases.length() < refBases.length()) {
            return VariantType.DELETION;
        }
        return VariantType.OTHER;
    }
}
