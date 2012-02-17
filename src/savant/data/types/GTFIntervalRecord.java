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

import java.util.ArrayList;
import java.util.List;

import savant.api.data.Block;
import savant.util.ColumnMapping;


/**
 * Class which represents a GTF record pulled out of a Tabix file.
 *
 * @author tarkvara
 */
public class GTFIntervalRecord extends TabixRichIntervalRecord {
    public static final int FEATURE_COLUMN = 2;
    public static final int ATTRIBUTE_COLUMN = 8;

    private final String name;
    private final String name2;
    private List<Block> blocks;
    private int thickStart, thickEnd;

    GTFIntervalRecord(String line) {
        super(line, ColumnMapping.GTF);
        name = extractGTFAttribute("gene_id");
        name2 = extractGTFAttribute("transcript_id");
        thickStart = interval.getStart();
        thickEnd = interval.getEnd();
    }

    @Override
    public List<Block> getBlocks() {
        if (blocks == null) {
            blocks = new ArrayList<Block>();
        }
        return blocks;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlternateName() {
        return name2;
    }

    @Override
    public int getThickStart() {
        return thickStart;
    }

    @Override
    public int getThickEnd() {
        return thickEnd;
    }

    /**
     * Examine the GTF attributes field and extract the given item.  In practice, the
     * only ones we're interested in are "gene_id" and "transcript_id".
     * @param index
     * @return 
     */
    private String extractGTFAttribute(String key) {
        String attributes = values[ATTRIBUTE_COLUMN];
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
    
    public void setThickness(int start, int end) {
        thickStart = start;
        thickEnd = end;
    }
}
