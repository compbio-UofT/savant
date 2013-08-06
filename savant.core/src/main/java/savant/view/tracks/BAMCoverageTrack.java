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

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.util.Resolution;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ResolutionSettings;
import savant.util.*;


public class BAMCoverageTrack extends Track {

    public BAMCoverageTrack(DataSourceAdapter dataSource) throws SavantTrackCreationCancelledException {
        super(dataSource, new ContinuousTrackRenderer());
    }

    @Override
    public void prepareForRendering(String ref, Range r) {

        Resolution res = getResolution(r);
        if (res != Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving coverage data...");
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(r, new Range(0, 1)));
            requestData(ref, r);
        } else {
            saveNullData(r);
        }

        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(ref));
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, this.getColourScheme());
        renderer.addInstruction(DrawingInstruction.RANGE, r);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, res);
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.CONTINUOUS_FILL, ColourKey.CONTINUOUS_LINE);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        switch (getDrawingMode()) {
            case ARC_PAIRED:
                return range.getLength() > ResolutionSettings.getBAMArcModeLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
            default:
                return range.getLength() > ResolutionSettings.getBAMLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
        }
    }

    @Override
    public AxisType getYAxisType(Resolution r) {
        return r == Resolution.HIGH ? AxisType.NONE : AxisType.REAL;
    }
}
