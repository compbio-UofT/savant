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
package savant.view.tracks;

import java.util.List;

import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.data.Record;
import savant.api.util.Resolution;
import savant.data.filters.BAMRecordFilter;
import savant.data.types.BAMIntervalRecord;
import savant.exception.RenderingException;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ResolutionSettings;
import savant.util.*;
import savant.util.SAMReadUtils.PairedSequencingProtocol;


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

    // arcs below concordantMin or above concordantMax are coloured as discordant-by-length
    private int concordantMin = 50;
    private int concordantMax = 1000;
    private static int maxBPForYMax = 10000;
    private boolean baseQualityEnabled = false;
    private boolean mappingQualityEnabled = false;

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
                                   DrawingMode.STANDARD_PAIRED, DrawingMode.ARC_PAIRED,
                                   DrawingMode.SNP, DrawingMode.STRAND_SNP };
    }

    /**
     * Set the current draw mode.  Switching to ARC_PAIRED mode requires us to modify our record filter.
     *
     * @param mode
     */
    @Override
    public void setDrawingMode(DrawingMode mode) {
        getFilter().setArcMode(mode == DrawingMode.ARC_PAIRED);
        super.setDrawingMode(mode);
    }


    @Override
    public void prepareForRendering(String ref, Range r) {

        DrawingMode mode = getDrawingMode();
        Resolution res = getResolution(r);
        if (res == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving BAM data...");
            requestData(ref, r);
        } else {
            saveNullData(r);
            if (mode == DrawingMode.ARC_PAIRED) {
                renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
            } else {
                // If there is an actual coverage track, this error message will never be drawn.
                renderer.addInstruction(DrawingInstruction.ERROR, new RenderingException("No coverage file available\nTo generate a coverage file, go to File > Format File", RenderingException.LOWEST_PRIORITY));
            }
        }

        renderer.addInstruction(DrawingInstruction.RANGE, r);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, res);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());
        renderer.addInstruction(DrawingInstruction.PAIRED_PROTOCOL, pairedProtocol);

        boolean f = containsReference(ref);
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(ref));

        if (mode == DrawingMode.ARC_PAIRED) {
            renderer.addInstruction(DrawingInstruction.DISCORDANT_MIN, getConcordantMin());
            renderer.addInstruction(DrawingInstruction.DISCORDANT_MAX, getConcordantMax());
        } else {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(r, new Range(0, 1)));
        }
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, mode != DrawingMode.SNP && mode != DrawingMode.STRAND_SNP);
        renderer.addInstruction(DrawingInstruction.MODE, mode);
        renderer.addInstruction(DrawingInstruction.BASE_QUALITY, baseQualityEnabled);
        renderer.addInstruction(DrawingInstruction.MAPPING_QUALITY, mappingQualityEnabled);
    }

    /**
     * Calculate the maximum (within reason) arc height to be used to set the Y axis for drawing.
     */
    public static int getArcYMax(List<Record> data) {

        int max = 0;

        if (data != null) {
            for (Record r: data) {

                SAMRecord samRecord = ((BAMIntervalRecord)r).getSAMRecord();

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

    @Override
    public Resolution getResolution(RangeAdapter range) {
        switch (getDrawingMode()) {
            case ARC_PAIRED:
                return range.getLength() > ResolutionSettings.getBAMArcModeLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
            default:
                return range.getLength() > ResolutionSettings.getBAMLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
        }
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
        concordantMax = value;
    }

    public void setPairedProtocol(PairedSequencingProtocol t) {
        pairedProtocol = t;
    }

    public PairedSequencingProtocol getPairedProtocol() {
        return pairedProtocol;
    }

    public int getMaxBPForYMax(){
        return maxBPForYMax;
    }

    public void setMaxBPForYMax(int max) {
        maxBPForYMax = max;
    }

    public BAMRecordFilter getFilter() {
        if (filter == null) {
            filter = new BAMRecordFilter();
        }
        return (BAMRecordFilter)filter;
    }

    public void setFilter(BAMRecordFilter value) {
        filter = value;
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

    public BAMIntervalRecord getMate(BAMIntervalRecord rec) {
        List<Record> data = getDataInRange();
        SAMRecord samRec = rec.getSAMRecord();
        for (Record rec2: data) {
            SAMRecord current = ((BAMIntervalRecord)rec2).getSAMRecord();
            if (MiscUtils.isMate(samRec, current, false)){
                return (BAMIntervalRecord)rec2;
            }
        }
        return null;
    }
    
    /**
     * Toggle our current setting for Base Quality.  Returns true if the mapping quality
     * was disabled as a side-effect.
     */
    public boolean toggleBaseQualityEnabled(){
        boolean result = false;
        if (baseQualityEnabled) {
            baseQualityEnabled = false;
        } else {
            baseQualityEnabled = true;

            // When user enables base quality, it disables mapping quality.
            if (mappingQualityEnabled) {
                mappingQualityEnabled = false;
                renderer.addInstruction(DrawingInstruction.MAPPING_QUALITY, false);
                result = true;
            }
        }
        renderer.addInstruction(DrawingInstruction.BASE_QUALITY, baseQualityEnabled);
        return result;
    }

    /**
     * Toggle our current setting for Mapping Quality.  Returns true if the base quality
     * was disabled as a side-effect.
     */
    public boolean toggleMappingQualityEnabled(){
        boolean result = false;
        if (mappingQualityEnabled) {
            mappingQualityEnabled = false;
        } else {
            mappingQualityEnabled = true;

            if (baseQualityEnabled) {
                // When user enables mapping quality, it disables base quality.
                baseQualityEnabled = false;
                renderer.addInstruction(DrawingInstruction.BASE_QUALITY, false);
                result = true;
            }
        }
        renderer.addInstruction(DrawingInstruction.MAPPING_QUALITY, mappingQualityEnabled);
        return result;
    }
}
