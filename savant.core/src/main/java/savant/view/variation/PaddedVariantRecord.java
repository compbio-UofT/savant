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
