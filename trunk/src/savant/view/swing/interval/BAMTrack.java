/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.view.swing.interval;

import java.util.List;

import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.data.sources.BAMDataSource;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.TrackResolutionSettings;
import savant.util.*;
import savant.util.SAMReadUtils.PairedSequencingProtocol;
import savant.view.swing.Track;


/**
 * Class to handle the preparation for rendering of a BAM track. Handles colour schemes and
 * drawing instructions, getting and filtering of data, setting of vertical axis, etc. The
 * ranges associated with various resolutions are also handled here, and the drawing modes
 * are defined.
 *
 * @author vwilliams
 */
public class BAMTrack extends Track {
    
    private static final Log LOG = LogFactory.getLog(BAMTrack.class);

    private SAMReadUtils.PairedSequencingProtocol pairedProtocol = SAMReadUtils.PairedSequencingProtocol.MATEPAIR;

    // if > 1, treat as absolute size below which an arc will not be drawn
    // if 0 < 1, treat as percentage of y range below which an arc will not be drawn
    // if = 0, draw all arcs
    private double arcSizeVisibilityThreshold=0.0d;

    // arcs below concordantMin or above concordantMax are coloured as discordant-by-length
    private int concordantMin = 50;
    private int concordantMax = 1000;
    private static int maxBPForYMax = 10000;
    private boolean includeVendorFailedReads = true;
    private boolean includeDuplicateReads = true;
    private int mappingQualityThreshold = 0;

    /**
     * Constructor.
     *
     * @param dataSource data source which this track represents
     */
    public BAMTrack(DataSourceAdapter dataSource) throws SavantTrackCreationCancelledException {
        super(dataSource, new BAMTrackRenderer());
        drawingMode = DrawingMode.MISMATCH;
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.FORWARD_STRAND, ColourKey.REVERSE_STRAND, ColourKey.CONCORDANT_LENGTH, ColourKey.DISCORDANT_LENGTH, ColourKey.ONE_READ_INVERTED, ColourKey.EVERTED_PAIR);
    }

    @Override
    public DrawingMode[] getValidDrawingModes() {
        return new DrawingMode[] { DrawingMode.STANDARD, DrawingMode.MISMATCH, /*DrawingMode.COLOURSPACE,*/ DrawingMode.SEQUENCE,
                                   DrawingMode.STANDARD_PAIRED, DrawingMode.ARC_PAIRED, DrawingMode.MAPPING_QUALITY, DrawingMode.BASE_QUALITY,
                                   DrawingMode.SNP, DrawingMode.STRAND_SNP };
    }

    @Override
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range);
        if (r == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving BAM data...");
            requestData(reference, range);
        } else {
            saveNullData();
            if (getDrawingMode() == DrawingMode.ARC_PAIRED) {
                renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
            } else {
                // If there is an actual coverage track, this error message will never be drawn.
                renderer.addInstruction(DrawingInstruction.ERROR, new RenderingException("No coverage file available\nTo generate a coverage file, go to File > Format File", 0));
            }
        }

        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, getColourScheme());
        renderer.addInstruction(DrawingInstruction.PAIRED_PROTOCOL, pairedProtocol);

        boolean f = containsReference(reference);
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        if (getDrawingMode() == DrawingMode.ARC_PAIRED) {
            renderer.addInstruction(DrawingInstruction.ARC_MIN, getArcSizeVisibilityThreshold());
            renderer.addInstruction(DrawingInstruction.DISCORDANT_MIN, getConcordantMin());
            renderer.addInstruction(DrawingInstruction.DISCORDANT_MAX, getConcordantMax());
        } else {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        }
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
    }

    /**
     * Calculate the maximum (within reason) arc height to be used to set the Y axis for drawing.
     */
    public static int getArcYMax(List<Record> data) {

        int max = 0;

        if (data != null) {
            for (Record r: data) {

                SAMRecord samRecord = ((BAMIntervalRecord)r).getSamRecord();

                int val = Math.abs(samRecord.getInferredInsertSize());

                //TODO: make this value user settable
                // never adjust max greater than this value
                if (val > maxBPForYMax) { continue; }

                // adjust the max if this value is larger
                if (val > max) { max = val; }
            }
        }
        return max;
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        switch (getDrawingMode()) {
            case ARC_PAIRED:
                return range.getLength() > TrackResolutionSettings.getBamArcModeLowToHighThresh() ? Resolution.LOW : Resolution.HIGH;
            default:
                return range.getLength() > TrackResolutionSettings.getBamDefaultModeLowToHighThresh() ? Resolution.LOW : Resolution.HIGH;
        }
    }

    public double getArcSizeVisibilityThreshold() {
        return arcSizeVisibilityThreshold;
    }

    public void setArcSizeVisibilityThreshold(double arcSizeVisibilityThreshold) {
        this.arcSizeVisibilityThreshold = arcSizeVisibilityThreshold;
    }

    public int getConcordantMin() {
        return concordantMin;
    }

    public void setConcordantMin(int value) {
        this.concordantMin = value;
    }

    public int getConcordantMax() {
        return concordantMax;
    }

    public void setConcordantMax(int value) {
        this.concordantMax = value;
    }

    public void setPairedProtocol(PairedSequencingProtocol t) {
        this.pairedProtocol = t;
    }

    public PairedSequencingProtocol getPairedSequencingProtocol() {
        return this.pairedProtocol;
    }

    public int getMaxBPForYMax(){
        return maxBPForYMax;
    }

    public void setMaxBPForYMax(int max) {
        maxBPForYMax = max;
    }

    public boolean getIncludeDuplicateReads() {
        return includeDuplicateReads;
    }

    public void setIncludeDuplicateReads(boolean value) {
        includeDuplicateReads = value;
    }

    public boolean getIncludeVendorFailedReads() {
        return includeVendorFailedReads;
    }

    public void setIncludeVendorFailedReads(boolean value) {
        includeVendorFailedReads = value;
    }

    public int getMappingQualityThreshold() {
        return mappingQualityThreshold;
    }

    public void setMappingQualityThreshold(int value) {
        mappingQualityThreshold = value;
    }

    @Override
    public AxisType getYAxisType(Resolution r) {
        switch (getDrawingMode()) {
            case ARC_PAIRED:
                return AxisType.INTEGER;
            case SNP:
            case STRAND_SNP:
                return AxisType.REAL;
            default:
                return AxisType.INTEGER_GRIDLESS;
        }
    }

    @Override
    protected synchronized List<Record> retrieveData(String reference, Range range, Resolution resolution) throws Exception {
        DrawingMode mode = getDrawingMode();
        return (List<Record>)(List)((BAMDataSource)getDataSource()).getRecords(reference, range, resolution, getArcSizeVisibilityThreshold(), AxisRange.initWithRanges(range, getDefaultYRange()), mode == DrawingMode.STANDARD_PAIRED || mode == DrawingMode.ARC_PAIRED, includeDuplicateReads, includeVendorFailedReads, mappingQualityThreshold);
    }
    
    public BAMIntervalRecord getMate(BAMIntervalRecord rec) {
        List<Record> data = getDataInRange();
        SAMRecord samRec = rec.getSamRecord();
        for (Record rec2: data) {
            SAMRecord current = ((BAMIntervalRecord)rec2).getSamRecord();
            if(MiscUtils.isMate(samRec, current)){
                return (BAMIntervalRecord)rec2;
            }
        }
        return null;
    }
}
