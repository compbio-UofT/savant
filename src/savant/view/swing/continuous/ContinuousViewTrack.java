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
 * ContinuousViewTrack.java
 * Created on Jan 19, 2010
 */

package savant.view.swing.continuous;

import savant.model.ContinuousRecord;
import savant.model.FileFormat;
import savant.model.Resolution;
import savant.model.data.continuous.GenericContinuousTrack;
import savant.model.view.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.util.Range;
import savant.view.swing.Savant;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to set up rendering of a ContinuousTrack
 * @author vwilliams
 */
public class ContinuousViewTrack extends ViewTrack {

    public ContinuousViewTrack(String name, GenericContinuousTrack track) {
        super(name, FileFormat.CONTINUOUS_GENERIC, track);
        setColorScheme(getDefaultColorScheme());
    }

    @Override
    public void prepareForRendering(Range range) throws Throwable {
        Resolution r = getResolution(range);
        List<Object> data = retrieveAndSaveData(range);
        for (TrackRenderer renderer : getTrackRenderers()) {
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RANGE, range);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            int maxDataValue = getMaxValue(data);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(range, new Range(0, maxDataValue)));

            Savant.log("Max Data Value is " + maxDataValue);
            renderer.setData(data);
        }
    }

    @Override
    public List<Object> retrieveData(Range range, Resolution resolution) throws Exception {
        return new ArrayList<Object>(getTrack().getRecords(range, resolution));
    }

    @Override
    public Resolution getResolution(Range range) {
        int length = range.getLength();

        if (length < 10000) { return Resolution.VERY_HIGH; }
        else if (length < 50000) { return Resolution.HIGH; }
        else if (length < 1000000) { return Resolution.MEDIUM; }
        else if (length < 10000000) { return Resolution.LOW; }
        else if (length >= 10000000) { return Resolution.VERY_LOW; }
        else { return Resolution.VERY_HIGH; }
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("LINE", new Color(0, 174, 255, 200));

        return c;
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    private int getMaxValue(List<Object>data) {
        double max = 0;
        for (Object o: data) {
            ContinuousRecord record = (ContinuousRecord)o;
            double val = record.getValue().getValue();
            if (val > max) max = val;
        }
        return (int)Math.ceil(max);
    }
}
