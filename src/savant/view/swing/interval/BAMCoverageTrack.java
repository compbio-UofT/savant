/*
 * BAMCoverageTrack.java
 * Created on Mar 4, 2010
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

import java.io.IOException;
import java.util.List;

import savant.api.adapter.ModeAdapter;
import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.data.types.ContinuousRecord;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.util.*;
import savant.view.swing.Track;
import savant.view.swing.continuous.ContinuousTrackRenderer;


public class BAMCoverageTrack extends Track {

    private boolean enabled = true;

    // DO NOT DELETE THIS CONSTRUCTOR!!! THIS SHOULD BE THE DEFAULT
    public BAMCoverageTrack(DataSource dataSource) throws SavantTrackCreationCancelledException {
        super(dataSource, new ContinuousTrackRenderer());
        setColorScheme(getDefaultColorScheme());
        this.notifyControllerOfCreation();
    }

    @Override
    public void prepareForRendering(String reference, Range range) throws IOException {

        List<Record> data = null;
        Resolution r = getResolution(range);
        if (getDataSource() != null) {
            if (isEnabled() && (r == Resolution.LOW || r == Resolution.VERY_LOW || r == Resolution.MEDIUM)) {
                data = retrieveAndSaveData(reference, range);
                //System.out.println("BAM data: " + data);
            }
        }

        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, true);


        if (isEnabled() && (r == Resolution.LOW || r == Resolution.VERY_LOW || r == Resolution.MEDIUM)) {
            renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));
            renderer.removeInstruction(DrawingInstruction.MESSAGE);
            int maxDataValue = getMaxValue(data);
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(0, maxDataValue)));
        } else {
            renderer.removeInstruction(DrawingInstruction.MESSAGE);
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        }
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, this.getColorScheme());
        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
        renderer.setData(data);
    }

    @Override
    public List<Record> retrieveData(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        return getDataSource().getRecords(reference, range, resolution);
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("Line", ColourSettings.getContinuousLine());

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
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

    private int getMaxValue(List<Record> data) {
        
        if (data == null) return 0;

        float max = 0;
        for (Record r: data) {
            ContinuousRecord record = (ContinuousRecord)r;
            float val = record.getValue().getValue();
            if (val > max) max = val;
        }
        return (int)Math.ceil(max);
    }

    // TODO: pull this property up into Track
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
