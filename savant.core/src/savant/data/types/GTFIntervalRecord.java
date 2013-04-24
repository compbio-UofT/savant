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
package savant.data.types;

import java.util.LinkedHashMap;
import java.util.Map;
import savant.util.ColumnMapping;


/**
 * Class which represents a GTF record pulled out of a Tabix file.
 *
 * @author tarkvara
 */
public class GTFIntervalRecord extends GFFIntervalRecord {
    GTFIntervalRecord(String line) {
        super(line, ColumnMapping.GTF);
        name = extractGTFAttribute("gene_id");
        name2 = extractGTFAttribute("transcript_id");
    }

    /**
     * Examine the GTF attributes field and extract the given item.  In practice, the
     * only ones we're interested in are "gene_id" and "transcript_id".
     * @param index
     * @return 
     */
    private String extractGTFAttribute(String key) {
        String attributes = values[ATTRIBUTE_COLUMN].trim();
        if (!attributes.startsWith(key)) {
            int semiPos = attributes.indexOf("; ");
            if (semiPos > 0) {
                attributes = attributes.substring(semiPos + 2);
            }
            // Malformed attributes column.
            if (!attributes.startsWith(key)) {
                return "";
            }
        }
        // Skip past the key, a space, and the initial double-quote.
        return attributes.substring(key.length() + 2, attributes.indexOf("\";"));
    }
    
    /**
     * Populate a map with values extracted from our attribute column.
     */
    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> result = new LinkedHashMap<String, String>();
        String[] attributes = values[ATTRIBUTE_COLUMN].trim().split("; ");
        for (String s: attributes) {
            int quotePos = s.indexOf('\"');
            if (quotePos > 0) {
                result.put(s.substring(0, quotePos).trim(), s.substring(quotePos + 1, s.indexOf('\"', quotePos + 1)));
            }
        }
        return result;
    }

    @Override
    protected boolean isParentOf(GFFIntervalRecord child) {
        return name.equals(child.name) && name2.equals(child.name2);
    }
}
