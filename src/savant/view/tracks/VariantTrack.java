/*
 *    Copyright 2011 University of Toronto
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.broad.igv.feature.Genome;
import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.util.GenomeUtils;
import savant.api.util.Resolution;
import savant.exception.SavantTrackCreationCancelledException;
import savant.util.AxisRange;
import savant.util.AxisType;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.DrawingInstruction;
import savant.util.Range;

/**
 * Track class for representing VCF files.
 *
 * @author tarkvara
 */
public class VariantTrack extends Track {
    private static final Log LOG = LogFactory.getLog(VariantTrack.class);

    public VariantTrack(DataSourceAdapter ds) throws SavantTrackCreationCancelledException {
        super(ds, new VariantTrackRenderer());
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.N);
    }

    @Override
    public void prepareForRendering(String reference, Range range) {
        Resolution r = getResolution(range);
        if (r == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving variant data...");
            requestData(reference, new Range(1, GenomeUtils.getGenome().getLength(reference)));
        } else {
            saveNullData();
        }

        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());

        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return Resolution.HIGH;
    }

    @Override
    public AxisType getXAxisType(Resolution r) {
        return AxisType.NONE;
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }
}
