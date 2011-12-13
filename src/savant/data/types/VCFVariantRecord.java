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
     * TODO: Interpret ALT strings which contain descriptive information rather than bases.
     * @return the type of variant represented by this record
     */
    @Override
    public Type getType() {
        String refBases = getRefBases();
        String altBases = getAltBases();
        if (refBases.length() == 1 && altBases.length() == 1) {
            return Type.SNP;
        }
        if (altBases.charAt(0) == '<') {
            if (altBases.startsWith("<INS")) {
                return Type.INSERTION;
            } else if (altBases.startsWith("<DEL")) {
                return Type.DELETION;
            } else {
                return Type.OTHER;
            }
        }
        if (altBases.indexOf(',') >= 0) {
            // ALT contains multiple entries.  Let's bail and call it OTHER.
            return Type.OTHER;
        }
        if (altBases.length() > refBases.length()) {
            return Type.INSERTION;
        } else if (altBases.length() < refBases.length()) {
            return Type.DELETION;
        }
        return Type.OTHER;
    }
}
