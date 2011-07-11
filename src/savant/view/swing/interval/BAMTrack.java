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
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
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

    // arcs below discordantMin or above discordantMax are coloured as discordant-by-length
    private int discordantMin=Integer.MIN_VALUE;
    private int discordantMax=Integer.MAX_VALUE;
    private static double maxBPForYMax = 10000;

    /**
     * Constructor.
     *
     * @param dataSource data source which this track represents
     */
    public BAMTrack(DataSourceAdapter dataSource) throws SavantTrackCreationCancelledException {
        super(dataSource, new BAMTrackRenderer());
        setColorScheme(getDefaultColorScheme());
        setValidDrawingModes(renderer.getDrawingModes());
        setDrawingMode(renderer.getDefaultDrawingMode());
        notifyControllerOfCreation();
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

    @Override
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range, getDrawingMode());
        if (r == Resolution.VERY_HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading BAM track...");
            requestData(reference, range);
        } else {
            saveNullData();
            if (getDrawingMode() == DrawingMode.ARC_PAIRED){
                renderer.addInstruction(DrawingInstruction.ERROR, "Zoom in to see data");
            } else {
                // If there is an actual coverage track, this error message will never be drawn.
                renderer.addInstruction(DrawingInstruction.ERROR, "No coverage file available");
            }
        }

        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, getColorScheme());
        renderer.addInstruction(DrawingInstruction.PAIRED_PROTOCOL, pairedProtocol);

        boolean f = containsReference(reference);
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        //if (errorMessage == null) {
            if (getDrawingMode() == DrawingMode.ARC_PAIRED) {
                renderer.addInstruction(DrawingInstruction.ARC_MIN, getArcSizeVisibilityThreshold());
                renderer.addInstruction(DrawingInstruction.DISCORDANT_MIN, getDiscordantMin());
                renderer.addInstruction(DrawingInstruction.DISCORDANT_MAX, getDiscordantMax());
            } else {
                renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
            }
        //}
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
    }

    /**
     * Calculate the maximum (within reason) arc height to be used to set the Y axis for drawing.
     */
    public static int getArcYMax(List<Record> data) {

        double max = 0;

        for (Record r: data) {

            SAMRecord samRecord = ((BAMIntervalRecord)r).getSamRecord();

            double val;

            val = Math.abs(samRecord.getInferredInsertSize());

            //TODO: make this value user settable
            // never adjust max greater than this value
            if (val > maxBPForYMax) { continue; }

            // adjust the max if this value is larger
            if (val > max) { max = val; }

        }
        return (int)Math.ceil(max);
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return getResolution((Range)range, getDrawingMode());
    }

    private Resolution getResolution(Range range, DrawingMode mode) {
        return mode == DrawingMode.ARC_PAIRED ? getArcModeResolution(range) : getDefaultModeResolution(range);
    }

    private Resolution getArcModeResolution(Range range) {
        int length = range.getLength();

        if (length > TrackResolutionSettings.getBamArcModeLowToHighThresh()) { return Resolution.LOW; }
        else { return Resolution.VERY_HIGH; }
    }

    private Resolution getDefaultModeResolution(Range range) {
        int length = range.getLength();

        if (length > TrackResolutionSettings.getBamDefaultModeLowToHighThresh()) { return Resolution.LOW; }
        else { return Resolution.VERY_HIGH; }
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

    public void setPairedProtocol(PairedSequencingProtocol t) {
        this.pairedProtocol = t;
    }

    public PairedSequencingProtocol getPairedSequencingProtocol() {
        return this.pairedProtocol;
    }

    public void setDiscordantMax(int discordantMax) {
        this.discordantMax = discordantMax;
    }

    public double getmaxBPForYMax(){
        return maxBPForYMax;
    }

    public void setmaxBPForYMax(double max){
        maxBPForYMax = max;
    }
}
