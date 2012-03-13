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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.Block;
import savant.util.ColumnMapping;


/**
 * Class which represents a GTF record pulled out of a Tabix file.
 *
 * @author tarkvara
 */
public class GTFIntervalRecord extends TabixRichIntervalRecord {
    private static final Log LOG = LogFactory.getLog(GTFIntervalRecord.class);

    private static final int FEATURE_COLUMN = 2;
    private static final int ATTRIBUTE_COLUMN = 8;

    private final String name;
    private final String name2;
    private List<Block> blocks;
    private int thickStart = -1, thickEnd = -1;

    GTFIntervalRecord(String line) {
        super(line, ColumnMapping.GTF);
        name = extractGTFAttribute("gene_id");
        name2 = extractGTFAttribute("transcript_id");
    }

    @Override
    public final List<Block> getBlocks() {
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
        return thickStart >= 0 ? thickStart : interval.getStart();
    }

    @Override
    public int getThickEnd() {
        return thickEnd >= 0 ? thickEnd : interval.getEnd();
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
     * If possible, absorb the information from the given record.  That is, if this record is
     * a transcript, it can absorb both exon lines and CDS lines.
     * @param child a GTF record containing an "exon" or "CDS" line we can potentially absorb
     * @return true if the record was absorbed.
     */
    public boolean absorbRecord(GTFIntervalRecord child) {
        
        if (name.equals(child.name) && name2.equals(child.name2)) {
            String feature = child.values[FEATURE_COLUMN];
            if (feature.equals("exon")) {
                if (values[FEATURE_COLUMN].equals("transcript")) {
                    // Only transcripts are allowed to have blocks.
                    if (blocks == null) {
                        blocks = new ArrayList<Block>();
                    }
                    blocks.add(Block.valueOf(child.interval.getStart() - interval.getStart(), child.interval.getLength()));
                    return true;
                }
            } else if (feature.equals("CDS")) {
                // Potentially we could have multiple CDS lines for a single transcript.
                // Our thickness will be the region which subsumes all the CDS lines.
                if (thickStart < 0) {
                    thickStart = child.interval.getStart();
                }
                thickEnd = Math.max(thickEnd, child.interval.getEnd());
                return true;
            }
        }
        return false;
    }
}
