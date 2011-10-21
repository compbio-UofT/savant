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

package savant.view.tracks;

import savant.api.util.Resolution;
import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.SavantROFile;
import savant.settings.TrackResolutionSettings;
import savant.util.*;
import savant.view.tracks.Track;


/**
 *
 * @author mfiume
 */
public class SequenceTrack extends Track {

    SavantROFile dFile;

    public SequenceTrack(DataSourceAdapter dataTrack) throws SavantTrackCreationCancelledException {
        super(dataTrack, new SequenceTrackRenderer());
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.N);
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
            renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, this.getColourScheme());
        }
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }
}
