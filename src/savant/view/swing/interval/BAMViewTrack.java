/*
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

/*
 * BAMViewTrack.java
 * Created on Feb 1, 2010
 */

package savant.view.swing.interval;

import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.RangeController;
import savant.data.sources.BAMDataSource;
import savant.data.types.BAMIntervalRecord;
import savant.file.FileFormat;
import savant.util.*;
import savant.util.ColorScheme;
import savant.util.DrawingInstructions;
import savant.util.Mode;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.util.ArrayList;
import java.util.List;
import savant.settings.ColourSettings;

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
    public BAMViewTrack(String name, BAMDataSource bamTrack) {
        super(name, FileFormat.INTERVAL_BAM, bamTrack);
        setColorScheme(getDefaultColorScheme());
        setDrawModes(getDefaultDrawModes());
        setDrawMode(VARIANTS_MODE);
        this.notifyViewTrackControllerOfCreation();
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();
        
        c.addColorSetting("Forward Strand", ColourSettings.forwardStrand);
        c.addColorSetting("Reverse Strand", ColourSettings.reverseStrand);
        c.addColorSetting("Inverted Read", ColourSettings.invertedRead);
        c.addColorSetting("Inverted Mate", ColourSettings.invertedMate);
        c.addColorSetting("Everted Pair", ColourSettings.evertedPair);
        c.addColorSetting("Discordant Length", ColourSettings.discordantLength);
        c.addColorSetting("Line", ColourSettings.line);

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }

    private List<Mode> getDefaultDrawModes()
    {
        List<Mode> modes = new ArrayList<Mode>();
        modes.add(STANDARD_MODE);
        modes.add(VARIANTS_MODE);
        modes.add(MATE_PAIRS_MODE);
        modes.add(SNP_MODE);
        return modes;
    }

    @Override
    public void prepareForRendering(String reference, Range range) throws Throwable {

        Resolution r = getResolution(range);
        List<Object> data = null;
        if ((getDrawMode().equals(MATE_PAIRS_MODE)) || (r == Resolution.VERY_HIGH || r == Resolution.HIGH)) {
            data = retrieveAndSaveData(reference, range);
        }
        for (TrackRenderer renderer : getTrackRenderers()) {
            boolean contains = (this.getDataSource().getReferenceNames().contains(reference) || this.getDataSource().getReferenceNames().contains(MiscUtils.homogenizeSequence(reference)));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RANGE, range);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            // TODO: fix this (problem: references appear as 1 and not chr1)
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS, contains); //this.getTrack().getReferenceNames().contains(reference));

            if (getDrawMode().getName().equals("MATE_PAIRS")) {
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
    private long getMaxValue(List<Object>data) {

        double max = 0;
        Range displayedRange = RangeController.getInstance().getRange();
        long displayedRangeLength = displayedRange.getLength();

        for (Object o: data) {

            BAMIntervalRecord record = (BAMIntervalRecord)o;
            SAMRecord samRecord = record.getSamRecord();

            double val;
            int alignmentStart = samRecord.getAlignmentStart();
            int mateAlignmentStart = samRecord.getMateAlignmentStart();
            if (alignmentStart < mateAlignmentStart) {

                val = record.getSamRecord().getInferredInsertSize();
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
    public List<Object> retrieveData(String reference, Range range, Resolution resolution) throws Throwable {
        return new ArrayList<Object>(getDataSource().getRecords(reference, range, resolution));
    }

    @Override
    public Resolution getResolution(Range range) {
        return getResolution(range, getDrawMode());
    }

    private Resolution getResolution(Range range, Mode mode)
    {
        return getDefaultModeResolution(range);       
    }

    private Resolution getDefaultModeResolution(Range range)
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
