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

import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.settings.TrackResolutionSettings;
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
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range);
        if (isEnabled() && r != Resolution.VERY_HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading coverage track...");
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
            requestData(reference, range);
        } else {
            saveNullData();
        }

        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, this.getColorScheme());
        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("Line", ColourSettings.getContinuousFill());

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return getResolution((Range)range, getDrawingMode());
    }

    public Resolution getResolution(Range range, DrawingMode mode) {
        return getDefaultModeResolution(range);
    }

    public Resolution getDefaultModeResolution(Range range) {
        int length = range.getLength();

        if (length > TrackResolutionSettings.getBamDefaultModeLowToHighThresh()) { return Resolution.LOW; }
        else { return Resolution.VERY_HIGH; }
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    // TODO: pull this property up into Track
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
