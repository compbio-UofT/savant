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
import savant.file.FileFormat;
import savant.util.Resolution;
import savant.model.data.interval.BEDIntervalTrack;
import savant.util.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.model.view.Mode;
import savant.util.Range;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.util.ArrayList;
import java.util.List;
import savant.settings.ColourSettings;
import savant.util.MiscUtils;

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

    private static final Mode STANDARD_MODE = new Mode(DrawingMode.STANDARD, "Standard Gene View");
    private static final Mode SQUISH_MODE = new Mode(DrawingMode.SQUISH, "All on one line");

    public BEDViewTrack(String name, BEDIntervalTrack bedTrack, String fn) {
        super(name, FileFormat.INTERVAL_BED, bedTrack, fn);
        setColorScheme(getDefaultColorScheme());
        setDrawModes(getDefaultDrawModes());
        setDrawMode(STANDARD_MODE);
    }


    @Override
    public void prepareForRendering(String reference, Range range) throws Throwable {
        Resolution r = getResolution(range);
        List<Object> data = retrieveAndSaveData(reference, range);
        for (TrackRenderer renderer : getTrackRenderers()) {
            boolean contains = (this.getTrack().getReferenceNames().contains(reference) || this.getTrack().getReferenceNames().contains(MiscUtils.homogenizeSequence(reference)));
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
    public List<Object> retrieveData(String reference, Range range, Resolution resolution) throws Exception {
        return new ArrayList<Object>(getTrack().getRecords(reference, range, resolution));
    }

    @Override
    public Resolution getResolution(Range range) {
        return getResolution(range, getDrawMode());
    }

    public Resolution getResolution(Range range, Mode mode)
    {
        return getDefaultModeResolution(range);
    }

    public Resolution getDefaultModeResolution(Range range)
    {
        int length = range.getLength();

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
        c.addColorSetting("Forward Strand", ColourSettings.forwardStrand);
        c.addColorSetting("Reverse Strand", ColourSettings.reverseStrand);
        c.addColorSetting("Translucent Graph", ColourSettings.translucentGraph);        
        c.addColorSetting("Line", ColourSettings.line);

        return c;
    }

    private List<Mode> getDefaultDrawModes()
    {
        List<Mode> modes = new ArrayList<Mode>();
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
