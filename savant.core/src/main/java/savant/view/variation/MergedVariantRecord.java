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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.util.AggregateRecord;

/**
 * VariantRecord which the VariationSheet uses to aggregate information about a particular variant which
 * happens to coincide in two different source files.
 *
 * @author tarkvara
 */
public class MergedVariantRecord implements VariantRecord, AggregateRecord<VariantRecord> {
    private final VariantRecord original1, original2;
    
    /** Number of participants after the end of original1 and before the first participant from original2. */
    private final int padding;

    /** Name may be composed of portions from two records. */
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
        List<String> alleles1 = new ArrayList();
        alleles1.addAll(Arrays.asList(rec1.getAltAlleles()));
        String[] alleles2 = rec2.getAltAlleles();
        renumberedAlleles = new byte[alleles2.length + 1];
        
        for (int i = 0; i < alleles2.length; i++) {
            int index = alleles1.indexOf(alleles2[i]);
            if (index < 0) {
                index = alleles1.size();
                alleles1.add(alleles2[i]);
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
            VariantType[] result = original1.getVariantsForParticipant(index);
            if (result != null) {
                return result;
            }
        }
        if (index >= count1 + padding && index < getParticipantCount()) {
            return original2.getVariantsForParticipant(index - count1 - padding);
        }
        // Participants which fall into the padding implicitly have NONE at this location.
        return new VariantType[] { VariantType.NONE };
    }

    @Override
    public int[] getAllelesForParticipant(int index) {
        int count1 = original1.getParticipantCount();
        if (index < count1) {
            int[] result = original1.getAllelesForParticipant(index);
            if (result != null) {
                return result;
            }
        }
        if (index >= count1 + padding && index < getParticipantCount()) {
            int[] alleles2 = original2.getAllelesForParticipant(index - count1 - padding);
            if (alleles2.length == 1) {
                return new int[] { renumberedAlleles[alleles2[0]] };
            } else {
                return new int[] { renumberedAlleles[alleles2[0]], renumberedAlleles[alleles2[1]] };
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
    public boolean isPhased() {
        return original1.isPhased() && original2.isPhased();
    }

    @Override
    public int compareTo(Object t) {
        VariantRecord that = (VariantRecord)t;
        return new CompareToBuilder().append(getReference(), that.getReference()).
                                      append(getPosition(), that.getPosition()).
                                      append(getRefBases(), that.getRefBases()).
                                      append(getAltAlleles(), that.getAltAlleles()).toComparison();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof VariantRecord) {
            VariantRecord that = (VariantRecord)t;
            return new EqualsBuilder().append(getReference(), that.getReference()).
                                       append(getPosition(), that.getPosition()).
                                       append(getRefBases(), that.getRefBases()).
                                       append(getAltAlleles(), that.getAltAlleles()).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getReference()).
                                     append(getPosition()).
                                     append(getRefBases()).
                                     append(getAltAlleles()).toHashCode();
    }


    @Override
    public List<VariantRecord> getConstituents() {
        List<VariantRecord> result = new ArrayList<VariantRecord>();
        if (original1 instanceof AggregateRecord) {
            result.addAll(((AggregateRecord)original1).getConstituents());
        } else {
            result.add(original1);
        }
        if (original2 instanceof AggregateRecord) {
            result.addAll(((AggregateRecord)original2).getConstituents());
        } else {
            result.add(original2);
        }
        return result;
    }
}
