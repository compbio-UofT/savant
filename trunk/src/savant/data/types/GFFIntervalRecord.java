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
import java.util.LinkedHashMap;
import java.util.List;

import java.util.Map;
import savant.api.adapter.RangeAdapter;
import savant.api.data.Block;
import savant.util.ColumnMapping;
import savant.util.Range;


/**
 * Class which represents a GFF record pulled out of a Tabix file.
 *
 * @author tarkvara
 */
public class GFFIntervalRecord extends TabixRichIntervalRecord {
    protected static final int FEATURE_COLUMN = 2;
    public static final int ATTRIBUTE_COLUMN = 8;

    protected String name;
    protected String name2;
    private List<Block> blocks;
    private int thickStart = -1, thickEnd = -1;

    /**
     * Constructor used for initialising plain vanilla GFF records.
     * @param line
     * @param mapping 
     */
    GFFIntervalRecord(String line) {
        super(line, ColumnMapping.GFF);
        name = extractGFF3Attribute("Name");
        if (name != null) {
            name2 = extractGFF3Attribute("Alias");
        }
    }
    
    /**
     * Constructor used for initialising GTF records.
     * @param line
     * @param mapping 
     */
    protected GFFIntervalRecord(String line, ColumnMapping mapping) {
        super(line, mapping);
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

    public String getFeatureType() {
        return values[FEATURE_COLUMN];
    }

    /**
     * If possible, absorb the information from the given record.  That is, if this record is
     * a transcript, it can absorb both exon lines and CDS lines.
     * @param child a GTF record containing an "exon" or "CDS" line we can potentially absorb
     * @return true if the record was absorbed.
     */
    public boolean absorbRecord(GFFIntervalRecord child) {
        if (isParentOf(child)) {
            String childType = child.getFeatureType();
            if (childType.equals("exon")) {
                if (values[FEATURE_COLUMN].equals("transcript") || values[FEATURE_COLUMN].equals("mRNA")) {
                    // Only transcripts and mRNA entries are allowed to have blocks.
                    if (blocks == null) {
                        blocks = new ArrayList<Block>();
                    }
                    blocks.add(Block.valueOf(child.interval.getStart() - interval.getStart(), child.interval.getLength()));
                    return true;
                }
            } else if (childType.equals("CDS")) {
                // Potentially we could have multiple CDS lines for a single transcript.
                // Our thickness will be the region which subsumes all the CDS lines.
                if (thickStart < 0) {
                    thickStart = child.interval.getStart();
                }
                thickEnd = Math.max(thickEnd, child.interval.getEnd());
                return true;
            } else if (childType.equals("intron")) {
                // Introns are just ignored.
                return true;
            }
        }
        return false;
    }

    /**
     * If we're a GTF or GFF3 transcript and the visible range doesn't fully include the entire gene, expand the range to ensure that we retrieve
     * all the blocks and the CDS.
     * @param r the current range for TabixDataSource.getRecords
     * @return null if the current range is okay, or the gene's range if it needs to be expanded
     */
    @Override
    public RangeAdapter getExpandedRange(RangeAdapter r) {
        if (values[FEATURE_COLUMN].equals("transcript") || values[FEATURE_COLUMN].equals("mRNA")) {
            if (interval.getStart() < r.getFrom() || interval.getEnd() > r.getTo()) {
                return new Range(Math.min(interval.getStart(), r.getFrom()), Math.max(interval.getEnd(), r.getTo()));
            }
        }
        return null;
    }

    /**
     * Return true if this record is the parent of the given child.
     * For GFF3 records, we rely on the ID= and Parent= attributes.
     * For GTF records, we rely on the transcript_id and gene_id attributes.
     * For plain GFF records, we make no attempt at creating a hierarchy.
     */
    protected boolean isParentOf(GFFIntervalRecord child) {
        if (name != null) {
            // If there's no name, it's not a GFF3 file, so we're not a parent.
            String parent = child.extractGFF3Attribute("Parent");
            if (parent != null) {
                String id = extractGFF3Attribute("ID");
                if (parent.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractGFF3Attribute(String key) {
        key += '=';
        String attributes = values[ATTRIBUTE_COLUMN].trim();
        while (attributes.length() > 0) {
            int semiPos = attributes.indexOf(";");
            if (attributes.startsWith(key)) {
                // Skip past the key, and the equals sign.
                int end = semiPos > 0 ? semiPos : attributes.length();
                return attributes.substring(key.length(), end);
            }
            if (semiPos > 0) {
                attributes = attributes.substring(semiPos + 1).trim();
            } else {
                break;
            }
        }
        return null;
    }
    
    /**
     * Populate a map with values extracted from our attribute column.
     */
    public Map<String, String> getAttributes() {
        Map<String, String> result = new LinkedHashMap<String, String>();
        String[] attributes = values[ATTRIBUTE_COLUMN].trim().split(";");
        if (attributes.length == 1 && attributes[0].indexOf(';') < 0) {
            // Just an ordinary GFF file, not GFF3.
            result.put("Group", attributes[0]);
        } else {
            for (String s: attributes) {
                int equalPos = s.indexOf('=');
                if (equalPos > 0) {
                    result.put(s.substring(0, equalPos), s.substring(equalPos + 1));
                }
            }
        }
        return result;
    }
}
