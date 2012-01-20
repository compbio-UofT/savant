/*
 *    Copyright 2010-2012 University of Toronto
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

import java.io.IOException;
import java.util.List;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.data.SequenceRecord;
import savant.api.util.Resolution;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.SavantROFile;
import savant.settings.TrackResolutionSettings;
import savant.util.*;


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
        return range.getLength() > TrackResolutionSettings.getSequenceLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    @Override
    public void prepareForRendering(String ref, Range r) {

        Resolution res = getResolution(r);

        if (res == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading sequence data...");
            requestData(ref, r);
        } else {
            renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
            saveNullData(r);
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, res);
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(r, new Range(0, 1)));
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(ref));
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, false);

        if (res == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.RANGE, r);
            renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, this.getColourScheme());
        }
    }
    
    /**
     * We often want to retrieve just the sequence.  This will be stored in a single SequenceRecord.
     */
    public byte[] getSequence(String ref, RangeAdapter r) throws IOException, InterruptedException {
        List<SequenceRecord> recs = getDataSource().getRecords(ref, r, Resolution.HIGH, null);
        if (recs != null && recs.size() > 0) {
            return recs.get(0).getSequence();
        }
        // No data retrieved.
        return null;
    }
}
