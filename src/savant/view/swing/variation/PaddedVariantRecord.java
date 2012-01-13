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
 * Wraps a variant record, to accommodate the possibility that we may be asked about
 * variants not in our actual participant array.  In such cases, we return NONE instead
 * of throwing an ArrayIndexOutOfBoundsException.
 *
 * @author tarkvara
 */
class PaddedVariantRecord implements VariantRecord {
    private final VariantRecord original;
    private final int padding;

    PaddedVariantRecord(VariantRecord rec, int pad) {
        original = rec;
        padding = pad;
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
        return original.getVariantType();
    }

    @Override
    public String getRefBases() {
        return original.getRefBases();
    }

    @Override
    public String getAltBases() {
        return original.getAltBases();
    }

    @Override
    public int getParticipantCount() {
        return padding + original.getParticipantCount();
    }

    @Override
    public VariantType getVariantForParticipant(int index) {
        if (index < padding) {
            return VariantType.NONE;
        } else if (index < getParticipantCount()) {
            return original.getVariantForParticipant(index - padding);
        }
        return VariantType.NONE;
    }

    @Override
    public Interval getInterval() {
        return original.getInterval();
    }

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public String getReference() {
        return original.getReference();
    }

    @Override
    public int compareTo(Object t) {
        return original.compareTo(t);
    }
}
