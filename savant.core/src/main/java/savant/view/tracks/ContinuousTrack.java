/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
