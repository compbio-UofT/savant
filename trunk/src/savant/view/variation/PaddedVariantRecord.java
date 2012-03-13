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
package savant.view.variation;

import java.util.ArrayList;
import java.util.List;

import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.util.AggregateRecord;


/**
 * Wraps a variant record, to accommodate the possibility that we may be asked about
 * variants not in our actual participant array.  In such cases, we return NONE instead
 * of throwing an ArrayIndexOutOfBoundsException.
 *
 * @author tarkvara
 */
public class PaddedVariantRecord implements VariantRecord, AggregateRecord<VariantRecord> {
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
        return getVariantType().toString() + "@" + getPosition();
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
    public String[] getAltAlleles() {
        return original.getAltAlleles();
    }

    @Override
    public int getParticipantCount() {
        return padding + original.getParticipantCount();
    }

    @Override
    public VariantType[] getVariantsForParticipant(int index) {
        if (index >= padding && index < getParticipantCount()) {
            return original.getVariantsForParticipant(index - padding);
        }
        // Participants which fall into the padding implicitly have NONE at this location.
        return new VariantType[] { VariantType.NONE };
    }

    @Override
    public int[] getAllelesForParticipant(int index) {
        if (index >= padding && index < getParticipantCount()) {
            return original.getAllelesForParticipant(index - padding);
        }
        // Participants which fall into the padding implicitly have allele 0 (i.e. ref) at this location.
        return new int[] { 0 };
    }

    @Override
    public int getPosition() {
        return original.getPosition();
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
    public boolean isPhased() {
        return original.isPhased();
    }

    @Override
    public int compareTo(Object t) {
        return original.compareTo(t);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object t) {
        return original.equals(t);
    }

    @Override
    public int hashCode() {
        return original.hashCode();
    }

    @Override
    public List<VariantRecord> getConstituents() {
        List<VariantRecord> result = new ArrayList<VariantRecord>();
        if (original instanceof AggregateRecord) {
            result.addAll(((AggregateRecord)original).getConstituents());
        } else {
            result.add(original);
        }
        return result;
    }
}
