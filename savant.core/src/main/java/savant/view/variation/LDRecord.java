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

import java.util.Arrays;
import java.util.List;
import savant.api.data.VariantRecord;
import savant.util.AggregateRecord;


/**
 * Pseudo-record which stores linkage disequilibrium information.  We want this to look like a Record so
 * that we can use it as the source for a VariantPopup.
 *
 * @author tarkvara
 */
public class LDRecord implements AggregateRecord<VariantRecord> {
    private final VariantRecord original1, original2;
    private final float dPrime, rSquared;

    public LDRecord(VariantRecord varRec1, VariantRecord varRec2, float d, float r) {
        original1 = varRec1;
        original2 = varRec2;
        dPrime = d;
        rSquared = r;
    }

    public float getDPrime() {
        return dPrime;
    }
    
    public float getRSquared() {
        return rSquared;
    }

    @Override
    public List<VariantRecord> getConstituents() {
        return Arrays.asList(original1, original2);
    }

    @Override
    public String getReference() {
        return original1.getReference();
    }

    @Override
    public int compareTo(Object t) {
        int result = original1.compareTo(t);
        if (result == 0) {
            result = original2.compareTo(t);
        }
        return result;
    }
}
