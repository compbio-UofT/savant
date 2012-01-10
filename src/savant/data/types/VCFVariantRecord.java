/*
 *    Copyright 2011-2012 University of Toronto
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

import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.util.ColumnMapping;

/**
 * Record type which holds variant data stored in a VCF record.
 *
 * @author tarkvara
 */
public class VCFVariantRecord extends TabixIntervalRecord implements VariantRecord {
    private static final Log LOG = LogFactory.getLog(VCFVariantRecord.class);
    private static final int NAME_COLUMN = 2;
    private static final int REF_COLUMN = 3;
    private static final int ALT_COLUMN = 4;
    private static final int FIRST_PARTICIPANT_COLUMN = 9;
    
    private final String name;
    private final String refBases;
    private final String[] altBases;
    private final byte[] participants0;
    private final byte[] participants1;

    /**
     * Constructor not be be called directly, but rather through TabixIntervalRecord.valueOf.
     */
    protected VCFVariantRecord(String line, ColumnMapping mapping) {
        super(line, mapping);
        
        // Storing all the string information is grossly inefficient, so we just parse what we
        // need and set the values to null.
        name = values[NAME_COLUMN].equals(".") ? null : values[NAME_COLUMN];    // VCF uses "." for missing value
        refBases = values[REF_COLUMN].intern();
        
        String altValue = values[ALT_COLUMN];
        if (altValue.startsWith("<")) {
            altBases = new String[] { altValue.intern() };
        } else {
            altBases = altValue.split(",");
            for (int i = 0; i < altBases.length; i++) {
                altBases[i] = altBases[i].intern();
            }
        }

        participants0 = new byte[values.length - FIRST_PARTICIPANT_COLUMN];
        participants1 = new byte[values.length - FIRST_PARTICIPANT_COLUMN];
        for (int i = 0; i < participants0.length; i++) {
            String info = values[FIRST_PARTICIPANT_COLUMN + i];
            int colonPos = info.indexOf(':');
            if (colonPos >= 0) {
                info = info.substring(0, colonPos);
            }
            String[] alleleIndices = info.split("[|/]");
            participants0[i] = Byte.parseByte(alleleIndices[0]);
            participants1[i] = Byte.parseByte(alleleIndices[1]);
        }

        values = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VCFVariantRecord other = (VCFVariantRecord) obj;
        if (!interval.equals(other.interval)) return false;
        if ((this.refBases == null) ? (other.refBases != null) : !this.refBases.equals(other.refBases)) {
            return false;
        }
        if (!Arrays.deepEquals(this.altBases, other.altBases)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (interval != null ? interval.hashCode() : 0);
        hash = 83 * hash + (this.refBases != null ? this.refBases.hashCode() : 0);
        hash = 83 * hash + Arrays.deepHashCode(this.altBases);
        return hash;
    }

    @Override
    public int compareTo(Object o) {

        VCFVariantRecord that = (VCFVariantRecord) o;

        // Compare position.  This should be unique.
        if (!interval.equals(that.interval)) {
            return interval.compareTo(that.interval);
        }
        
        if (!refBases.equals(that.refBases)) {
            return refBases.compareTo(that.refBases);
        }
        
        return getAltBases().compareTo(that.getAltBases());
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * TODO: Interpret ALT strings which contain descriptive information rather than bases.
     * TODO: Interpret ALT strings which contain multiple comma-separated values.
     */
    @Override
    public double getAltFrequency() {
        int sum = 0;
        for (int i = 0; i < participants0.length; i++) {
            if (getVariantForParticipant(i) != VariantType.NONE) {
                sum++;
            }
        }
        return (double)sum / participants0.length;
    }

    @Override
    public String getRefBases() {
        return refBases;
    }

    @Override
    public String getAltBases() {
        String result = altBases[0];
        for (int i = 1; i < altBases.length; i++) {
            result += "," + altBases[i];
        }
        return result;
    }

    /**
     * Retrieve the number of participants represented by this record.
     */
    @Override
    public int getParticipantCount() {
        return participants0.length;
    }

    /**
     * Get the variant for the given individual.
     * TODO: Distinguish between homozygous and heterozygous variants.
     *
     * @param index between 0 and getParticipantCount() - 1
     * @return the variant for this participant (possibly NONE)
     */
    @Override
    public VariantType getVariantForParticipant(int index) {
        if (participants0[index] == 0) {
            if (participants1[index] == 0) {
                // Both are 0, so we have no variant for this participant.
                return VariantType.NONE;
            } else {
                // Heterozygous variant.
                return getVariantType(altBases[participants1[index] - 1]);
            }
        } else {
            return getVariantType(altBases[participants0[index] - 1]);
        }
    }
    
    public boolean isHeterozygousForParticipant(int index) {
        return participants0[index] != participants1[index];
    }

    /**
     * Return the type of this record without reference to any participant.  When there
     * are multiple ALT values, we return the first one.
     *
     * TODO: Interpret ALT strings which contain descriptive information rather than bases.
     * @return the type of variant represented by this record
     */
    @Override
    public VariantType getVariantType() {
        return getVariantType(altBases[0]);
    }

    /**
     * TODO: Interpret ALT strings which contain descriptive information rather than bases.
     * TODO: Interpret ALT strings which contain multiple comma-separated values.
     * @return the type of variant represented by this record
     */
    private VariantType getVariantType(String alt) {
        if (refBases.length() == 1 && alt.length() == 1) {
            switch (alt.charAt(0)) {
                case 'A':
                    return VariantType.SNP_A;
                case 'C':
                    return VariantType.SNP_C;
                case 'G':
                    return VariantType.SNP_G;
                case 'T':
                    return VariantType.SNP_T;
                default:
                    LOG.info("Unrecognised base " + alt + " in VCFVariantRecord.");
                    return VariantType.OTHER;
            }
        }
        if (alt.charAt(0) == '<') {
            if (alt.startsWith("<INS")) {
                return VariantType.INSERTION;
            } else if (alt.startsWith("<DEL")) {
                return VariantType.DELETION;
            } else {
                return VariantType.OTHER;
            }
        }
        if (alt.length() > refBases.length()) {
            return VariantType.INSERTION;
        } else if (alt.length() < refBases.length()) {
            return VariantType.DELETION;
        }
        return VariantType.OTHER;
    }
}
