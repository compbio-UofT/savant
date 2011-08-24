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

package savant.view.swing.sequence;

import java.awt.Color;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.SavantROFile;
import savant.settings.ColourSettings;
import savant.settings.TrackResolutionSettings;
import savant.util.*;
import savant.view.swing.Track;


/**
 *
 * @author mfiume
 */
public class SequenceTrack extends Track {

    SavantROFile dFile;

    public SequenceTrack(DataSourceAdapter dataTrack) throws SavantTrackCreationCancelledException {
        super(dataTrack, new SequenceTrackRenderer());
        setColorScheme(getDefaultColorScheme());
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("A", ColourSettings.getA());
        c.addColorSetting("C", ColourSettings.getC());
        c.addColorSetting("G", ColourSettings.getG());
        c.addColorSetting("T", ColourSettings.getT());
        c.addColorSetting("N", ColourSettings.getN());
        c.addColorSetting("Line", Color.black);
        c.addColorSetting("Background", new Color(100,100,100,220));

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return range.getLength() > TrackResolutionSettings.getSequenceLowToHighThresh() ? Resolution.LOW : Resolution.HIGH;
    }

    @Override
    public void prepareForRendering(String reference, Range range) {

        if (range == null) { return; }

        Resolution r = getResolution(range);

        if (r == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading sequence data...");
            requestData(reference, range);
        } else {
            renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
            saveNullData();
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, false);

        if (r == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.RANGE, range);
            renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, this.getColorScheme());
        }
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }
}
