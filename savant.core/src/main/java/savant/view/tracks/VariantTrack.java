/*
 *    Copyright 2011-2012 University of Toronto
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
import savant.api.adapter.RecordFilterAdapter;
import savant.api.adapter.VariantDataSourceAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.api.util.Resolution;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ResolutionSettings;
import savant.util.AxisRange;
import savant.util.AxisType;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.DrawingInstruction;
import savant.util.DrawingMode;
import savant.util.Range;


/**
 * Track class for representing VCF files.
 *
 * @author tarkvara
 */
public class VariantTrack extends Track {

    public VariantTrack(DataSourceAdapter ds) throws SavantTrackCreationCancelledException {
        super(ds, new VariantTrackRenderer());
        filter = new ParticipantFilter();
        drawingMode = DrawingMode.MATRIX;
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.INSERTED_BASE, ColourKey.DELETED_BASE, ColourKey.N);
    }

    @Override
    public DrawingMode[] getValidDrawingModes() {
        return new DrawingMode[] { DrawingMode.MATRIX, DrawingMode.FREQUENCY };
    }

    /**
     * Set up the renderer and request data
     * @param ref the reference being rendered
     * @param r the range being rendered
     */
    @Override
    public void prepareForRendering(String ref, Range r) {
        Resolution res = getResolution(r);
        if (res == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving variant data...");
            requestData(ref, r);
        } else {
            renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
            saveNullData(r);
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, res);
        renderer.addInstruction(DrawingInstruction.RANGE, r);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());

        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(ref));

        String[] participants = ((VariantDataSourceAdapter)getDataSource()).getParticipants();
        renderer.addInstruction(DrawingInstruction.PARTICIPANTS, participants);
        if (getDrawingMode() == DrawingMode.MATRIX) {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(r, new Range(0, participants.length)));
        } else {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(r, new Range(0, 1)));            
        }
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
        
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return range.getLength() > ResolutionSettings.getVariantLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    /**
     * We want our X-axis to be NONE because the vertical grey lines look too much like SNPs.
     * @return <code>AxisType.NONE</code>
     */
    @Override
    public AxisType getXAxisType(Resolution ignored) {
        return AxisType.NONE;
    }

    @Override
    public AxisType getYAxisType(Resolution ignored) {
        return getDrawingMode() == DrawingMode.MATRIX ? AxisType.INTEGER : AxisType.REAL;
    }

    public int getParticipantCount() {
        return ((VariantDataSourceAdapter)getDataSource()).getParticipants().length;
    }

    public String[] getParticipantNames() {
        return ((VariantDataSourceAdapter)getDataSource()).getParticipants();
    }

    /**
     * Record filter which only accepts records for which one of the relevant participants
     * has the given variant.
     */
    private class ParticipantFilter implements RecordFilterAdapter {
        public ParticipantFilter() {
        }

        @Override
        public boolean accept(Record rec) {
            VariantRecord varRec = (VariantRecord)rec;
            int count = getParticipantCount();
            for (int i = 0; i < count; i++) {
                VariantType[] participantVars = varRec.getVariantsForParticipant(i);
                if (participantVars != null && (participantVars.length > 1 || participantVars[0] != VariantType.NONE)) {
                    return true;
                }
            }
            return false;
        }
    }
}
