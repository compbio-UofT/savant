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

import java.util.ArrayList;
import java.util.List;

import savant.api.data.Strand;
import savant.api.data.Block;
import savant.api.data.RichIntervalRecord;
import savant.util.ColumnMapping;


/**
 * Record containing bed-like information, but extracted from a Tabix file.
 *
 * @author tarkvara
 */
public class TabixRichIntervalRecord extends TabixIntervalRecord implements RichIntervalRecord {

    /**
     * Constructor not be be called directly, but rather through TabixIntervalRecord.valueOf.
     */
    protected TabixRichIntervalRecord(String line, ColumnMapping mapping) {
        super(line, mapping);
    }

    @Override
    public String getAlternateName() {
        return mapping.name2 >= 0 ? values[mapping.name2] : null;
    }

    /**
     * Does the actual work of calculating the blocks.  Note that this method allows for
     * the possibility that block starts are specified as positions relative to the start
     * of the feature rather than as absolute positions within the chromosome.  This
     * functionality is not currently used by our Tabix files.
     */
    @Override
    public List<Block> getBlocks() {
        List<Block> blocks = null;
        boolean relativeStarts = mapping.blockStartsRelative >= 0;
        if (relativeStarts || mapping.blockStartsAbsolute >= 0) {
            int[] blockStarts = Block.extractBlocks(values[relativeStarts ? mapping.blockStartsRelative : mapping.blockStartsAbsolute]);
            blocks = new ArrayList<Block>(blockStarts.length);
            int offset = relativeStarts ? 0 : getInterval().getStart();
            int endExtra = 1;
            if (!mapping.oneBased) {
                endExtra = 0;
                if (!relativeStarts) {
                    offset--;
                }
            }
            if (mapping.blockEnds >= 0) {
                int[] blockEnds = Block.extractBlocks(values[mapping.blockEnds]);
                for (int i = 0; i < blockEnds.length; i++) {
                    blocks.add(Block.valueOf(blockStarts[i] - offset, blockEnds[i] - blockStarts[i] + endExtra));
                }
            } else if (mapping.blockSizes >= 0) {
                int[] blockSizes = Block.extractBlocks(values[mapping.blockSizes]);
                for (int i = 0; i < blockSizes.length; i++) {
                    blocks.add(Block.valueOf(blockStarts[i] - offset, blockSizes[i] + endExtra));
                }
            } else {
                throw new IllegalArgumentException("No column provided for block ends/sizes.");
            }
        }
        return blocks;
    }

    @Override
    public float getScore() {
        return mapping.score >= 0 ? Float.parseFloat(values[mapping.score]) : Float.NaN;
    }

    @Override
    public Strand getStrand() {
        if (mapping.strand >= 0 && values[mapping.strand].length() > 0) {
            switch (values[mapping.strand].charAt(0)) {
                case '-':
                    return Strand.REVERSE;
                default:
                    return Strand.FORWARD;
            }
        } else {
            return Strand.FORWARD;
        }
    }

    @Override
    public int getThickStart() {
        if (mapping.thickStart >= 0) {
            int result = Integer.parseInt(values[mapping.thickStart]);
            return mapping.oneBased ? result : result + 1;
        }
        return interval.getStart();
    }

    @Override
    public int getThickEnd() {
        return mapping.thickEnd >= 0 ? Integer.parseInt(values[mapping.thickEnd]) : interval.getEnd();
    }

    @Override
    public ItemRGB getItemRGB() {
        return mapping.itemRGB >= 0 ? ItemRGB.parseItemRGB(values[mapping.itemRGB]) : null;
    }
}
