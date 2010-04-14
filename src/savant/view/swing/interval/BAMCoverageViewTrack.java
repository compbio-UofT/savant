/*
 *    Copyright 2009 Vanessa Williams
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
 * BAMCoverageViewTrack.java
 * Created on Mar 4, 2010
 */

package savant.view.swing.interval;

import savant.model.ContinuousRecord;
import savant.model.FileFormat;
import savant.model.Resolution;
import savant.model.data.continuous.GenericContinuousTrack;
import savant.model.view.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.model.view.Mode;
import savant.util.Range;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BAMCoverageViewTrack extends ViewTrack {

    public BAMCoverageViewTrack(String name, GenericContinuousTrack track) {
        super(name, FileFormat.CONTINUOUS_GENERIC, track);
        setColorScheme(getDefaultColorScheme());
    }

    @Override
    public void prepareForRendering(Range range) throws Throwable {
        Resolution r = getResolution(range);
        List<Object> data = null;
        if (r == Resolution.LOW || r == Resolution.VERY_LOW || r == Resolution.MEDIUM) {
            data = retrieveAndSaveData(range);
        }
        for (TrackRenderer renderer : getTrackRenderers()) {
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RANGE, range);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            int maxDataValue = getMaxValue(data);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(range, new Range(0, maxDataValue)));
            renderer.setData(data);
        }
    }

    @Override
    public List<Object> retrieveData(Range range, Resolution resolution) throws Throwable {
        return new ArrayList<Object>(getTrack().getRecords(range, resolution));
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("LINE", new Color(0, 174, 255, 200));

        return c;
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

        if (length < 5000) { return Resolution.VERY_HIGH; }
        else if (length < 10000) { return Resolution.HIGH; }
        else if (length < 20000) { return Resolution.MEDIUM; }
        else if (length < 10000000) { return Resolution.LOW; }
        else if (length >= 10000000) { return Resolution.VERY_LOW; }
        else { return Resolution.VERY_HIGH; }
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    private int getMaxValue(List<Object>data) {
        
        if (data == null) return 0;

        double max = 0;
        for (Object o: data) {
            ContinuousRecord record = (ContinuousRecord)o;
            double val = record.getValue().getValue();
            if (val > max) max = val;
        }
        return (int)Math.ceil(max);
    }

}
