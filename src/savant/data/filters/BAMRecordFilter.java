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
import savant.controller.LocationController;
import savant.data.types.BAMIntervalRecord;


/**
 * Class which allows for filtering of BAM records based on the settings in the BAM settings dialog.
 *
 * @author tarkvara
 */
public class BAMRecordFilter implements RecordFilterAdapter<BAMIntervalRecord> {
    public static final double DEFAULT_ARC_LENGTH_THRESHOLD = 1.0;

    private double arcLengthThreshold = 1.0;
    private boolean includeDuplicates = false;
    private boolean includeVendorFailed = false;
    private int mappingQualityThreshold;
    private boolean pairMode;

    public BAMRecordFilter() {}

    public double getArcLengthThreshold() {
        return arcLengthThreshold;
    }

    /**
     * if > 1, treat as absolute size below which an arc will not be drawn
     * if 0 < 1, treat as percentage of y range below which an arc will not be drawn
     * if = 0, draw all arcs
     */
    public void setArcLengthThreshold(double value) {
        arcLengthThreshold = value;
    }

    public int getMappingQualityThreshold() {
        return mappingQualityThreshold;
    }

    public void setMappingQualityThreshold(int value) {
        mappingQualityThreshold = value;
    }

    public boolean getIncludeDuplicateReads() {
        return includeDuplicates;
    }

    public void setIncludeDuplicateReads(boolean value) {
        includeDuplicates = value;
    }

    public boolean getIncludeVendorFailedReads() {
        return includeVendorFailed;
    }

    public void setIncludeVendorFailedReads(boolean value) {
        includeVendorFailed = value;
    }

    /** Filtering for BAM tracks is slightly different depending on whether we're in normal mode or Arc Pair mode. */
    public void setPairMode(boolean value) {
        pairMode = value;
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

        if (samRecord.getMappingQuality() < mappingQualityThreshold) {
            return false;
        }

        if (pairMode) {
            int arcLength = Math.abs(samRecord.getInferredInsertSize());
            // skip reads with a zero insert length--probably mapping errors
            if (arcLength == 0) {
                return false;
            }

            if ((arcLengthThreshold != 0.0d && arcLengthThreshold < 1.0d && arcLength < LocationController.getInstance().getRange().getLength() * arcLengthThreshold) || (arcLengthThreshold > 1.0d && arcLength < arcLengthThreshold)) {
                return false;
            }
        }
        return true;
    }
}
