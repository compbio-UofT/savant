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
 * BEDViewTrack.java
 * Created on Feb 19, 2010
 */

package savant.view.swing.interval;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.data.sources.file.BEDFileDataSource;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.DataFormat;
import savant.util.*;
import savant.util.ColorScheme;
import savant.util.DrawingInstructions;
import savant.util.Mode;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.util.ArrayList;
import java.util.List;
import savant.api.adapter.ModeAdapter;
import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.data.types.Record;
import savant.settings.ColourSettings;

/**
 * View track for a BED interval file (containing BED Interval Records)
 * 
 * @author vwilliams
 */
public class BEDViewTrack extends ViewTrack {

    private static Log log = LogFactory.getLog(BEDViewTrack.class);   

    public enum DrawingMode {
        STANDARD,
        SQUISH
    };

    private static final Mode STANDARD_MODE = Mode.fromObject(DrawingMode.STANDARD, "Standard Gene View");
    private static final Mode SQUISH_MODE = Mode.fromObject(DrawingMode.SQUISH, "All on one line");

    public BEDViewTrack(DataSource bedTrack) throws SavantTrackCreationCancelledException {
        super(DataFormat.INTERVAL_BED, bedTrack);
        setColorScheme(getDefaultColorScheme());
        setDrawModes(getDefaultDrawModes());
        setDrawMode(STANDARD_MODE);
        this.notifyViewTrackControllerOfCreation();
    }


    @Override
    public void prepareForRendering(String reference, Range range) throws Throwable {
        Resolution r = getResolution(range);
        List<Record> data = retrieveAndSaveData(reference, range);
        for (TrackRenderer renderer : getTrackRenderers()) {
            boolean contains = (this.getDataSource().getReferenceNames().contains(reference) || this.getDataSource().getReferenceNames().contains(MiscUtils.homogenizeSequence(reference)));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RANGE, range);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS, contains);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.MODE, getDrawMode());
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.SELECTION_ALLOWED, true);
            renderer.setData(data);
        }
    }


    @Override
    public List<Record> retrieveData(String reference, RangeAdapter range, Resolution resolution) throws Exception {
        return getDataSource().getRecords(reference, range, resolution);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return getResolution(range, getDrawMode());
    }

    public Resolution getResolution(RangeAdapter range, ModeAdapter mode)
    {
        return getDefaultModeResolution(range);
    }

    public Resolution getDefaultModeResolution(RangeAdapter range)
    {
        long length = range.getLength();

        if (length < 10000) { return Resolution.VERY_HIGH; }
        else if (length < 50000) { return Resolution.HIGH; }
        else if (length < 1000000) { return Resolution.MEDIUM; }
        else if (length < 10000000) { return Resolution.LOW; }
        else if (length >= 10000000) { return Resolution.VERY_LOW; }
        else { return Resolution.VERY_HIGH; }
    }

    @Override
    public Mode getDefaultDrawMode() {
        return STANDARD_MODE;
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("Forward Strand", ColourSettings.getForwardStrand());
        c.addColorSetting("Reverse Strand", ColourSettings.getReverseStrand());
        c.addColorSetting("Translucent Graph", ColourSettings.getTranslucentGraph());
        c.addColorSetting("Line", ColourSettings.getLine());

        return c;
    }

    private List<ModeAdapter> getDefaultDrawModes()
    {
        List<ModeAdapter> modes = new ArrayList<ModeAdapter>();
        modes.add(STANDARD_MODE);
        modes.add(SQUISH_MODE);
        return modes;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }
    
    private Range getDefaultYRange() {
        return new Range(0, 1);
    }
 
}
