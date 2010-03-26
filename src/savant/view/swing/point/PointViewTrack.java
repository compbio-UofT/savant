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
package savant.view.swing.point;

import savant.model.FileFormat;
import savant.model.Resolution;
import savant.model.data.point.GenericPointTrack;
import savant.model.view.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.util.Range;
import savant.view.swing.BrowserDefaults;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class PointViewTrack extends ViewTrack {

    public PointViewTrack(String name, GenericPointTrack pointTrack) {
        super(name, FileFormat.POINT, pointTrack);
        setColorScheme(getDefaultColorScheme());
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("BACKGROUND", BrowserDefaults.colorGraphMain);
        c.addColorSetting("LINE", BrowserDefaults.colorAccent);

        return c; 
    }

    public Resolution getResolution(Range range)
    {
        int length = range.getLength();

        if (length > 100000) { return Resolution.VERY_LOW; }
        return Resolution.VERY_HIGH;
    }

    @Override
    public List<Object> retrieveData(Range range, Resolution resolution) throws Exception {
        return new ArrayList<Object>(getTrack().getRecords(range, resolution));
    }

    public void prepareForRendering(Range range) throws Exception {
        Resolution r = getResolution(range);

        List<Object> data = null;

        switch (r)
        {
            case VERY_HIGH:
                data = this.retrieveAndSaveData(range);
                break;
            default:
                break;
        }

        for (TrackRenderer renderer : getTrackRenderers()) {

            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(range, getDefaultYRange()));

            renderer.setData(data);
        }
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

}
