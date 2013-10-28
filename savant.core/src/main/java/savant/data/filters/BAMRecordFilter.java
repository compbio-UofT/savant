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
    private boolean includePaired = true;
    private boolean includeUnpaired = true;
    private boolean includeUnmappedMate = true;
    private int mappingQualityThreshold;
    private boolean arcMode;

    public BAMRecordFilter() {}

    public double getArcLengthThreshold() {
        return arcLengthThreshold;
    }

    /**
     * if > 1, treat as absolute size below which an arc will not be drawn
     * if 0 &lt; 1, treat as percentage of y range below which an arc will not be drawn
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

    public boolean getIncludePairedReads() {
        return includePaired;
    }

    public void setIncludePairedReads(boolean value) {
        includePaired = value;
    }

    public boolean getIncludeUnmappedMate() {
        return includeUnmappedMate;
    }

    public void setIncludeUnmappedMate(boolean value) {
        includeUnmappedMate = value;
    }

    public boolean getIncludeUnpairedReads() {
        return includeUnpaired;
    }

    public void setIncludeUnpairedReads(boolean value) {
        includeUnpaired = value;
    }

    /** Filtering for BAM tracks is slightly different depending on whether we're in normal mode or Arc Pair mode. */
    public void setArcMode(boolean value) {
        arcMode = value;
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

        if (samRecord.getReadPairedFlag()) {
            if (!includePaired) {
                return false;
            }
            if (!includeUnmappedMate && samRecord.getMateUnmappedFlag()) {
                return false;
            }
        } else {
            if (!includeUnpaired) {
                return false;
            }
        }
        
        if (samRecord.getMappingQuality() < mappingQualityThreshold) {
            return false;
        }

        if (arcMode) {
            int arcLength = Math.abs(samRecord.getInferredInsertSize());
            if ((arcLengthThreshold != 0.0d && arcLengthThreshold < 1.0d && arcLength < LocationController.getInstance().getRange().getLength() * arcLengthThreshold) || (arcLengthThreshold > 1.0d && arcLength < arcLengthThreshold)) {
                return false;
            }
        }
        return true;
    }
}
