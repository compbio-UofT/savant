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
package savant.view.swing.variation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import savant.api.data.Interval;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.util.MiscUtils;

/**
 * VariantRecord which the VariationSheet uses to aggregate information about a particular variant which
 * happens to coincide in two different source files.
 *
 * @author tarkvara
 */
class MergedVariantRecord implements VariantRecord {
    private final VariantRecord original1, original2;
    
    /** Number of participants after the end of original1 and before the first participant from original2. */
    private final int padding;
    private final String name;
    private final String[] altAlleles;
    
    /**
     * Allele indices from original2 need to be mapped to indices in the merged altBases.
     */
    private final byte[] renumberedAlleles;

    /**
     * Create a variant which aggregates the variant data from two separate variants.
     * Assumes that:
     * <ol>
     * <li>rec1 and rec2 have the same refBases</li>
     * <li>rec1 and rec2 have the same interval (should be true if refBases the same)</li>
     * </ol>
     * 
     * @param rec1 the first variant record
     * @param rec2 the second variant record
     * @param pad  number of extra participants found after the end of rec1 but before the start of rec2
     */
    MergedVariantRecord(VariantRecord rec1, VariantRecord rec2, int pad) {
        original1 = rec1;
        original2 = rec2;
        padding = pad;

        String s = rec1.getName();
        if (s == null || s.isEmpty()) {
            s = rec2.getName();
        }
        name = s;

        // Create comma-separated list of alt bases which includes contributions from
        // both rec1 and rec2.
        List<String> alleles1 = Arrays.asList(rec1.getAltAlleles());
        String[] alleles2 = rec2.getAltAlleles();
        renumberedAlleles = new byte[alleles2.length + 1];
        
        for (int i = 0; i < alleles2.length; i++) {
            int index = alleles1.indexOf(alleles2[i]);
            if (index < 0) {
                index = alleles1.size();
                alleles1.add(alleles2[i]);
            }
            if (i + 1 != index + 1) {
                VariationSheet.LOG.info("At " + rec1.getPosition() + " " + (i + 1) + " will be renumbered to " + (index + 1));
            }
            renumberedAlleles[i + 1] = (byte)(index + 1);
        }
        altAlleles = alleles1.toArray(new String[0]);
    }

    /**
     * For debug purposes, provides a succinct summary of the variant.
     */
    @Override
    public String toString() {
        return getVariantType().toString() + "@" + getPosition();
    }

    @Override
    public VariantType getVariantType() {
        return original1.getVariantType();
    }

    @Override
    public String getRefBases() {
        return original1.getRefBases();
    }

    @Override
    public String[] getAltAlleles() {
        return altAlleles;
    }

    @Override
    public int getParticipantCount() {
        return original1.getParticipantCount() + padding + original2.getParticipantCount();
    }

    @Override
    public VariantType[] getVariantsForParticipant(int index) {
        int count1 = original1.getParticipantCount();
        if (index < count1) {
            return original1.getVariantsForParticipant(index);
        } else if (index >= count1 + padding && index < getParticipantCount()) {
            return original2.getVariantsForParticipant(index - count1 - padding);
        }
        // Participants which fall into the padding implicitly have NONE at this location.
        return new VariantType[] { VariantType.NONE };
    }

    @Override
    public int[] getAllelesForParticipant(int index) {
        int count1 = original1.getParticipantCount();
        if (index < count1) {
            return original1.getAllelesForParticipant(index);
        } else if (index >= count1 + padding && index < getParticipantCount()) {
            int[] alleles2 = original2.getAllelesForParticipant(index - count1 - padding);
            if (alleles2.length == 1) {
                return new int[] { renumberedAlleles[alleles2[0]] };
            }
        }
        // Participants which fall into the padding implicitly have allele 0 (i.e. ref) at this location.
        return new int[] { 0 };
    }

    @Override
    public int getPosition() {
        return original1.getPosition();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getReference() {
        return original1.getReference();
    }

    @Override
    public int compareTo(Object t) {
        VariantRecord that = (VariantRecord)t;
        return new CompareToBuilder().append(getPosition(), that.getPosition()).
                                      append(getRefBases(), that.getRefBases()).
                                      append(getAltAlleles(), that.getAltAlleles()).toComparison();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof VariantRecord) {
            VariantRecord that = (VariantRecord)t;
            return new EqualsBuilder().appendSuper(super.equals(that)).
                                       append(getPosition(), that.getPosition()).
                                       append(getRefBases(), that.getRefBases()).
                                       append(getAltAlleles(), that.getAltAlleles()).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).
                                     append(getPosition()).
                                     append(getRefBases()).
                                     append(getAltAlleles()).toHashCode();
    }

    /**
     * When choosing Select/Deselect from the popup menu, we need to know which actual
     * VariantRecords were used to create this one.
     */
    public Collection<VariantRecord> getConstituents() {
        List<VariantRecord> result = new ArrayList<VariantRecord>();
        if (original1 instanceof MergedVariantRecord) {
            result.addAll(((MergedVariantRecord)original1).getConstituents());
        } else {
            result.add(original1);
        }
        if (original2 instanceof MergedVariantRecord) {
            result.addAll(((MergedVariantRecord)original2).getConstituents());
        } else {
            result.add(original2);
        }
        return result;
    }
}
