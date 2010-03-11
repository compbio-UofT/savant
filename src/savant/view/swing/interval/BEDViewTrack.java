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

import savant.model.FileFormat;
import savant.model.Resolution;
import savant.model.data.interval.BEDIntervalTrack;
import savant.model.view.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.model.view.Mode;
import savant.util.Range;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * View track for a BED interval file (containing BED Interval Records)
 * 
 * @author vwilliams
 */
public class BEDViewTrack extends ViewTrack {

    private static Log log = LogFactory.getLog(BEDViewTrack.class);

    public enum DrawingMode {
        STANDARD
    };

    private static final Mode STANDARD_MODE = new Mode(DrawingMode.STANDARD, "Standard Gene View");

    public BEDViewTrack(String name, BEDIntervalTrack bedTrack) {
        super(name, FileFormat.INTERVAL_BED, bedTrack);
        setColorScheme(getDefaultColorScheme());
        setDrawModes(getDefaultDrawModes());
        setDrawMode(STANDARD_MODE);
    }


    @Override
    public void prepareForRendering(Range range) throws Exception {
        Resolution r = getResolution(range);
        List<Object> data = retrieveAndSaveData(range);
        for (TrackRenderer renderer : getTrackRenderers()) {
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RANGE, range);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(range, getDefaultYRange()));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.MODE, getDrawMode());
            renderer.setData(data);
        }
    }


    @Override
    public List<Object> retrieveData(Range range, Resolution resolution) throws Exception {
        return new ArrayList<Object>(getTrack().getRecords(range, resolution));
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
        c.addColorSetting("FORWARD_STRAND", new Color(0,131,192));
        c.addColorSetting("REVERSE_STRAND", new Color(0,174,255));
        c.addColorSetting("LINE", new Color(128,128,128));

        return c;
    }

    private List<Mode> getDefaultDrawModes()
    {
        List<Mode> modes = new ArrayList<Mode>();
        modes.add(STANDARD_MODE);
        return modes;
    }
    
    private Range getDefaultYRange() {
        return new Range(0, 1);
    }


}
