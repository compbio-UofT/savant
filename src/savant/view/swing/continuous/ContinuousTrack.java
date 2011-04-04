/*
 * ContinuousTrack.java
 * Created on Jan 19, 2010
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

package savant.view.swing.continuous;

import java.io.IOException;
import java.util.List;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.data.types.ContinuousRecord;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.util.*;
import savant.view.swing.Savant;
import savant.view.swing.Track;


/**
 * A helper class to set up rendering of a ContinuousTrack
 *
 * @author vwilliams
 */
 public class ContinuousTrack extends Track {

    public ContinuousTrack(DataSource track) throws SavantTrackCreationCancelledException {
        super(track, new ContinuousTrackRenderer());
        setColorScheme(getDefaultColorScheme());
        this.notifyControllerOfCreation();
    }

    @Override
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range);
        renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading track...");
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
        long length = range.getLength();

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

        c.addColorSetting("Fill", ColourSettings.getContinuousFill());
        c.addColorSetting("Line", ColourSettings.getContinuousLine());

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    public static float[] getExtremeValues(List<Record> data) {
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        for (Record r: data) {
            float val = ((ContinuousRecord)r).getValue();
            if (val > max) max = val;
            if (val < min) min = val;
        }

        float[] result = new float[2];
        result[0] = min;
        result[1] = max;

        return result;
    }

}
