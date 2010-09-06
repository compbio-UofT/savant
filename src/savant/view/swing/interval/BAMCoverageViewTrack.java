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
 * BAMCoverageViewTrack.java
 * Created on Mar 4, 2010
 */

package savant.view.swing.interval;

import savant.data.types.ContinuousRecord;
import savant.file.FileFormat;
import savant.util.Resolution;
import savant.model.data.continuous.GenericContinuousTrack;
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

public class BAMCoverageViewTrack extends ViewTrack {

    private boolean enabled = true;

    public BAMCoverageViewTrack(String name, GenericContinuousTrack track) {
        super(name, FileFormat.CONTINUOUS_GENERIC, track);
        setColorScheme(getDefaultColorScheme());
    }

    @Override
    public void prepareForRendering(String reference, Range range) throws Throwable {

        List<Object> data = null;
        Resolution r = getResolution(range);
        if (getTrack() != null) {
            if (isEnabled() && (r == Resolution.LOW || r == Resolution.VERY_LOW || r == Resolution.MEDIUM)) {
                data = retrieveAndSaveData(reference, range);
                //System.out.println("BAM data: " + data);
            }
        }

        for (TrackRenderer renderer : getTrackRenderers()) {

            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS, true);


            //reference = "1";

            // FIXME: another nasty hack to accommodate coverage
            if (getTrack() == null && isEnabled() && (r == Resolution.LOW || r == Resolution.VERY_LOW || r == Resolution.MEDIUM)) {
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.MESSAGE, "No coverage file available");
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
            }
            else if (isEnabled() && (r == Resolution.LOW || r == Resolution.VERY_LOW || r == Resolution.MEDIUM)) {
                //FIXME: temporary fix for chrx != x issue
                boolean contains = (this.getTrack().getReferenceNames().contains(reference) || this.getTrack().getReferenceNames().contains(MiscUtils.homogenizeSequence(reference)));
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS, contains);
                renderer.getDrawingInstructions().getInstructions().remove(DrawingInstructions.InstructionName.MESSAGE.toString());
                int maxDataValue = getMaxValue(data);
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(0, maxDataValue)));
            } else {
                renderer.getDrawingInstructions().getInstructions().remove(DrawingInstructions.InstructionName.MESSAGE.toString());
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));            
            }
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RANGE, range);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.SELECTION_ALLOWED, true);
            renderer.setData(data);
        }
    }

    @Override
    public List<Object> retrieveData(String reference, Range range, Resolution resolution) throws Throwable {
        return new ArrayList<Object>(getTrack().getRecords(reference, range, resolution));
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("Line", ColourSettings.continuousLine);

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
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

        float max = 0;
        for (Object o: data) {
            ContinuousRecord record = (ContinuousRecord)o;
            float val = record.getValue().getValue();
            if (val > max) max = val;
        }
        return (int)Math.ceil(max);
    }

    // TODO: pull this property up into ViewTrack
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
