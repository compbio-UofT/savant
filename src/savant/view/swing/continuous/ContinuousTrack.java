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

package savant.view.swing.continuous;

import java.util.List;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.data.types.ContinuousRecord;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.settings.TrackResolutionSettings;
import savant.util.AxisType;
import savant.util.ColorScheme;
import savant.util.DrawingInstruction;
import savant.util.Range;
import savant.util.Resolution;
import savant.view.swing.Track;


/**
 * Track class for the various flavours of continuous data.
 *
 * @author vwilliams
 */
 public class ContinuousTrack extends Track {

    public ContinuousTrack(DataSourceAdapter track) throws SavantTrackCreationCancelledException {
        super(track, new ContinuousTrackRenderer());
        setColorScheme(getDefaultColorScheme());
    }

    @Override
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range);
        renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving data...");
        requestData(reference, new Range(range.getFrom(), range.getTo()+2));
        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, getColorScheme());
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));
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
        return range.getLength() > TrackResolutionSettings.getConservationLowToHighThresh() ? Resolution.LOW : Resolution.HIGH;
    }

    @Override
    public AxisType getYAxisType(Resolution r) {
        return AxisType.REAL;
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */

        c.addColorSetting("Fill", ColourSettings.getContinuousFill());
        c.addColorSetting("Line", ColourSettings.getContinuousLine());

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
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
