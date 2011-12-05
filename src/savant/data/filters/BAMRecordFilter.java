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

package savant.data.filters;

import net.sf.samtools.SAMRecord;

import savant.api.adapter.RecordFilterAdapter;
import savant.data.types.BAMIntervalRecord;


/**
 * Class which allows for filtering of BAM records based on the settings in the BAM settings dialog.
 *
 * @author tarkvara
 */
public class BAMRecordFilter implements RecordFilterAdapter<BAMIntervalRecord> {
    private final double lengthThreshold;
    private final int xRange;
    private final boolean pairMode;
    private final boolean includeDuplicates;
    private final boolean includeVendorFailed;
    private final int qualityThreshold;

    public BAMRecordFilter(double lengthThreshold, int xRange, boolean pairMode, boolean includeDuplicates, boolean includeVendorFailed, int qualityThreshold) {
        this.lengthThreshold = lengthThreshold;
        this.xRange = xRange;
        this.pairMode = pairMode;
        this.includeDuplicates = includeDuplicates;
        this.includeVendorFailed = includeVendorFailed;
        this.qualityThreshold = qualityThreshold;
    }

    @Override
    public boolean accept(BAMIntervalRecord rec) {
        SAMRecord samRecord = rec.getSAMRecord();
        
        if (!includeDuplicates && samRecord.getDuplicateReadFlag()) {
            return false;
        }

        if (!includeVendorFailed && samRecord.getReadFailsVendorQualityCheckFlag()) {
            return false;
        }

        if (samRecord.getMappingQuality() < qualityThreshold) {
            return false;
        }

        if (pairMode) {
            int arcLength = Math.abs(samRecord.getInferredInsertSize());
            // skip reads with a zero insert length--probably mapping errors
            if (arcLength == 0) {
                return false;
            }

            if ((lengthThreshold != 0.0d && lengthThreshold < 1.0d && arcLength < xRange * lengthThreshold) || (lengthThreshold > 1.0d && arcLength < lengthThreshold)) {
                return false;
            }
        }
        return true;
    }
}
