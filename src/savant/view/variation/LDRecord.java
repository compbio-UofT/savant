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
