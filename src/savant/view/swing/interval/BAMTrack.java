/*
 * BAMTrack.java
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
import savant.util.*;
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

    /*
    public enum DrawingMode {
        STANDARD,
        VARIANTS,
        MATE_PAIRS,
        SNP,
        MAPPING_QUALITY
    };
     * 
     */

    private static final Mode STANDARD_MODE = Mode.fromObject("Standard", "Colour by strand");
    private static final Mode VARIANTS_MODE = Mode.fromObject("Mismatch", "Show indels and mismatches");
    private static final Mode MATE_PAIRS_MODE = Mode.fromObject("Pair arc", "Join mate pairs with arcs");
    private static final Mode MAPPING_QUALITY_MODE = Mode.fromObject("Mapping quality", "Shade alignments by mapping quality");
    private static final Mode SNP_MODE = Mode.fromObject("SNP", "Show values per position");

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
     * @param dataSource data source which this track represents
     */
    public BAMTrack(DataSource dataSource) throws SavantTrackCreationCancelledException {
        super(dataSource, new BAMTrackRenderer());
        setColorScheme(getDefaultColorScheme());
        setDrawModes(getDefaultDrawModes());
        setDrawMode(VARIANTS_MODE);
        this.notifyControllerOfCreation();
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
        modes.add(MAPPING_QUALITY_MODE);
        modes.add(SNP_MODE);
        return modes;
    }

    @Override
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range);
        String errorMessage = null;
        if (r == Resolution.VERY_HIGH || r == Resolution.HIGH || (getDrawMode().equals(MATE_PAIRS_MODE) && r == Resolution.MEDIUM)) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading BAM track...");
            requestData(reference, range);
        } else if (getDrawMode().equals(MATE_PAIRS_MODE)){
            errorMessage = "Zoom in to see data";
        } else {
            errorMessage = "No coverage file available";
        }
        renderer.addInstruction(DrawingInstruction.ERROR, errorMessage);

        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, getColorScheme());
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        if (errorMessage == null) {
        if (getDrawMode().getName().equals("Pair arc") && errorMessage == null) {
            renderer.addInstruction(DrawingInstruction.ARC_MIN, getArcSizeVisibilityThreshold());
            renderer.addInstruction(DrawingInstruction.DISCORDANT_MIN, getDiscordantMin());
            renderer.addInstruction(DrawingInstruction.DISCORDANT_MAX, getDiscordantMax());
        } else {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        }
        }
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
        renderer.addInstruction(DrawingInstruction.MODE, getDrawMode());
    }

    /**
     * Calculate the maximum (within reason) arc height to be used to set the Y axis for drawing.
     */
    public static long getMaxValue(List<Record> data) {

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
