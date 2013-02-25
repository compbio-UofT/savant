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

import java.util.List;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.data.ContinuousRecord;
import savant.api.data.Record;
import savant.api.util.Resolution;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ResolutionSettings;
import savant.util.*;


/**
 * Track class for the various flavours of continuous data.
 *
 * @author vwilliams
 */
 public class ContinuousTrack extends Track {

    public ContinuousTrack(DataSourceAdapter track) throws SavantTrackCreationCancelledException {
        super(track, new ContinuousTrackRenderer());
    }

    @Override
    public void prepareForRendering(String ref, Range r) {

        Resolution res = getResolution(r);
        renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving data...");
        requestData(ref, r);
        renderer.addInstruction(DrawingInstruction.RANGE, r);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, res);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(ref));
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
    }

    /**
     * With multi-level rendering, continuous tracks no longer have much use for the
     * Resolution enum.  However, it remains useful for Continuous tracks which are
     * lacking the level information (e.g. Wig tracks fetched from UCSC).
     *
     * @param range
     */
    @Override
    public Resolution getResolution(RangeAdapter range) {
        return range.getLength() > ResolutionSettings.getContinuousLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    @Override
    public AxisType getYAxisType(Resolution r) {
        return AxisType.REAL;
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.CONTINUOUS_FILL, ColourKey.CONTINUOUS_LINE);
    }

    public static float[] getExtremeValues(List<Record> data) {
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        if (data != null) {
            for (Record r: data) {
                float val = ((ContinuousRecord)r).getValue();
                if (val > max) max = val;
                if (val < min) min = val;
            }
        }

        return new float[] { min, max };
    }
}
