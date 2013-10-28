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

import java.io.IOException;
import java.util.List;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.data.SequenceRecord;
import savant.api.util.Resolution;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ResolutionSettings;
import savant.util.*;


/**
 *
 * @author mfiume
 */
public class SequenceTrack extends Track {

    public SequenceTrack(DataSourceAdapter dataTrack) throws SavantTrackCreationCancelledException {
        super(dataTrack, new SequenceTrackRenderer());
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.N);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return range.getLength() > ResolutionSettings.getSequenceLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
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
