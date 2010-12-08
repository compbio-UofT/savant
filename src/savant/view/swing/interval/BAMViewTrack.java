/*
 * BAMViewTrack.java
 * Created on Feb 1, 2010
 *
 *
 *    Copyright 2010 University of Toronto
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.ModeAdapter;
import savant.api.adapter.RangeAdapter;
import savant.controller.RangeController;
import savant.data.sources.DataSource;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.util.AxisRange;
import savant.util.ColorScheme;
import savant.util.DrawingInstructions;
import savant.util.MiscUtils;
import savant.util.Mode;
import savant.util.Range;
import savant.util.Resolution;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;


/**
 * Class to handle the preparation for rendering of a BAM track. Handles colour schemes and
 * drawing instructions, getting and filtering of data, setting of vertical axis, etc. The
 * ranges associated with various resolutions are also handled here, and the drawing modes
 * are defined.
 *
 * @author vwilliams
 */
public class BAMViewTrack extends ViewTrack {
    
    private static Log log = LogFactory.getLog(BAMViewTrack.class);

    public enum DrawingMode {
        STANDARD,
        VARIANTS,
        MATE_PAIRS,
        SNP
    };

    private static final Mode STANDARD_MODE = Mode.fromObject(DrawingMode.STANDARD, "Colour by strand");
    private static final Mode VARIANTS_MODE = Mode.fromObject(DrawingMode.VARIANTS, "Show indels and mismatches");
    private static final Mode MATE_PAIRS_MODE = Mode.fromObject(DrawingMode.MATE_PAIRS, "Join mate pairs with arcs");
    private static final Mode SNP_MODE = Mode.fromObject(DrawingMode.SNP, "Show values per position");

    // if > 1, treat as absolute size below which an arc will not be drawn
    // if 0 < 1, treat as percentage of y range below which an arc will not be drawn
    // if = 0, draw all arcs
    private double arcSizeVisibilityThreshold=0.0d;

    // arcs below discordantMin or above discordantMax are coloured as discordant-by-length
    private int discordantMin=Integer.MIN_VALUE;
    private int discordantMax=Integer.MAX_VALUE;

    /**
     * Constructor.
     *
     * @param name track name
     * @param bamTrack data track which this view track represents
     */
    public BAMViewTrack(DataSource bamTrack) throws SavantTrackCreationCancelledException {
        super(bamTrack);
        setColorScheme(getDefaultColorScheme());
        setDrawModes(getDefaultDrawModes());
        setDrawMode(VARIANTS_MODE);
        this.notifyViewTrackControllerOfCreation();
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();
        
        c.addColorSetting("Forward Strand", ColourSettings.getForwardStrand());
        c.addColorSetting("Reverse Strand", ColourSettings.getReverseStrand());
        c.addColorSetting("Inverted Read", ColourSettings.getInvertedRead());
        c.addColorSetting("Inverted Mate", ColourSettings.getInvertedMate());
        c.addColorSetting("Everted Pair", ColourSettings.getEvertedPair());
        c.addColorSetting("Discordant Length", ColourSettings.getDiscordantLength());
        c.addColorSetting("Line", ColourSettings.getLine());

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }

    private List<ModeAdapter> getDefaultDrawModes()
    {
        List<ModeAdapter> modes = new ArrayList<ModeAdapter>();
        modes.add(STANDARD_MODE);
        modes.add(VARIANTS_MODE);
        modes.add(MATE_PAIRS_MODE);
        modes.add(SNP_MODE);
        return modes;
    }

    @Override
    public void prepareForRendering(String reference, Range range) throws IOException {

        Resolution r = getResolution(range);
        List<Record> data = null;
        boolean zoomIn = false;
        if ((getDrawMode().equals(MATE_PAIRS_MODE) && (r == Resolution.HIGH || r == Resolution.VERY_HIGH || r == Resolution.MEDIUM))
                || (r == Resolution.VERY_HIGH || r == Resolution.HIGH)) {
            data = retrieveAndSaveData(reference, range);
        } else if(getDrawMode().equals(MATE_PAIRS_MODE)){
            zoomIn = true;
        }
        for (TrackRenderer renderer : getTrackRenderers()) {
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.ZOOM_IN, zoomIn);
            boolean contains = (this.getDataSource().getReferenceNames().contains(reference) || this.getDataSource().getReferenceNames().contains(MiscUtils.homogenizeSequence(reference)));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RANGE, range);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            // TODO: fix this (problem: references appear as 1 and not chr1)
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS, contains); //this.getTrack().getReferenceNames().contains(reference));

            if (getDrawMode().getName().equals("MATE_PAIRS") && !zoomIn) {
                long maxDataValue = getMaxValue(data);
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(0,(int)Math.round(maxDataValue+maxDataValue*0.1))));
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.ARC_MIN, getArcSizeVisibilityThreshold());
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.DISCORDANT_MIN, getDiscordantMin());
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.DISCORDANT_MAX, getDiscordantMax());
            }
            else renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.SELECTION_ALLOWED, true);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.MODE, getDrawMode());
            renderer.setData(data);
        }
    }

    /*
    * Calculate the maximum (within reason) arc height to be used to set the Y axis for drawing.
     */
    private long getMaxValue(List<Record> data) {

        double max = 0;
        Range displayedRange = RangeController.getInstance().getRange();
        long displayedRangeLength = displayedRange.getLength();

        for (Record r: data) {

            SAMRecord samRecord = ((BAMIntervalRecord)r).getSamRecord();

            double val;
            int alignmentStart = samRecord.getAlignmentStart();
            int mateAlignmentStart = samRecord.getMateAlignmentStart();
            if (alignmentStart < mateAlignmentStart) {

                val = samRecord.getInferredInsertSize();
                // throw away, for purposes of calculating the max y axis, any insert sizes larger than the displayed range.
                if (val > displayedRangeLength) continue;
            }
            else {
                continue;
            }
            if (val > max) max = val;

        }
        return (long)Math.ceil(max);
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    @Override
    public List<Record> retrieveData(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        return getDataSource().getRecords(reference, range, resolution);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return getResolution(range, getDrawMode());
    }

    private Resolution getResolution(RangeAdapter range, ModeAdapter mode)
    {
        return getDefaultModeResolution(range);       
    }

    private Resolution getDefaultModeResolution(RangeAdapter range)
    {
        long length = range.getLength();

        if (length < 5000) { return Resolution.VERY_HIGH; }
        else if (length < 10000) { return Resolution.HIGH; }
        else if (length < 20000) { return Resolution.MEDIUM; }
        else if (length < 10000000) { return Resolution.LOW; }
        else if (length >= 10000000) { return Resolution.VERY_LOW; }
        else { return Resolution.VERY_HIGH; }
    }

    @Override
    public Mode getDefaultDrawMode() {
        return VARIANTS_MODE;
    }

    public double getArcSizeVisibilityThreshold() {
        return arcSizeVisibilityThreshold;
    }

    public void setArcSizeVisibilityThreshold(double arcSizeVisibilityThreshold) {
        this.arcSizeVisibilityThreshold = arcSizeVisibilityThreshold;
    }

    public int getDiscordantMin() {
        return discordantMin;
    }

    public void setDiscordantMin(int discordantMin) {
        this.discordantMin = discordantMin;
    }

    public int getDiscordantMax() {
        return discordantMax;
    }

    public void setDiscordantMax(int discordantMax) {
        this.discordantMax = discordantMax;
    }
}
