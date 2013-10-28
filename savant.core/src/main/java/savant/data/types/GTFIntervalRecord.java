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
