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

import savant.api.data.Interval;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;

/**
 * VariantRecord which the VariationSheet uses to aggregate information about a particular variant which
 * happens to coincide in two different source files.
 *
 * @author tarkvara
 */
class MergedVariantRecord implements VariantRecord {
    private final VariantRecord original1, original2;
    private final int padding;
    private final String name;
    private final String altBases;

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
        s = rec1.getAltBases();
        if (!s.contains(rec2.getAltBases())) {
            // Blecch.  The records have different alt bases.  Since this string is largely
            // decorative, we'll just slam them together.
            s += "," + rec2.getAltBases();
        }
        altBases = s;
    }

    /**
     * For debug purposes, provides a succinct summary of the variant.
     */
    @Override
    public String toString() {
        return getVariantType().toString() + "@" + getInterval().getStart();
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
    public String getAltBases() {
        return altBases;
    }

    @Override
    public int getParticipantCount() {
        return original1.getParticipantCount() + padding + original2.getParticipantCount();
    }

    @Override
    public VariantType getVariantForParticipant(int index) {
        int count1 = original1.getParticipantCount();
        if (index < count1) {
            return original1.getVariantForParticipant(index);
        } else if (index < count1 + padding) {
            return VariantType.NONE;
        } else if (index < getParticipantCount()) {
            return original2.getVariantForParticipant(index - count1 - padding);
        }
        return VariantType.NONE;
    }

    @Override
    public Interval getInterval() {
        return original1.getInterval();
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

        // Compare position.  This should be unique.
        Interval interval = getInterval();
        if (!interval.equals(that.getInterval())) {
            return interval.compareTo(that.getInterval());
        }

        String refBases = getRefBases();
        if (!refBases.equals(that.getRefBases())) {
            return refBases.compareTo(that.getRefBases());
        }

        return getAltBases().compareTo(that.getAltBases());
    }
}
