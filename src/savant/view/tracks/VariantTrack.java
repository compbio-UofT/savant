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

import java.util.BitSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.GraphPaneAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.event.LocationChangedEvent;
import savant.api.util.Listener;
import savant.api.util.Resolution;
import savant.controller.LocationController;
import savant.exception.SavantTrackCreationCancelledException;
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
    private static final Log LOG = LogFactory.getLog(VariantTrack.class);

    private String reference;
    private Range range;
    private BitSet participants;

    public VariantTrack(DataSourceAdapter ds) throws SavantTrackCreationCancelledException {
        super(ds, new VariantTrackRenderer());
        filter = new ParticipantFilter();
        
        // Set the initial range and reference to whatever Savant is displaying.
        LocationController lc = LocationController.getInstance();
        reference = lc.getReferenceName();
        range = lc.getMaxRange();
        lc.addListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                reference = event.getReference();
                range = LocationController.getInstance().getMaxRange();
            }
        });
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.N);
    }

    @Override
    public DrawingMode[] getValidDrawingModes() {
        return new DrawingMode[] { DrawingMode.STANDARD, DrawingMode.MATRIX, DrawingMode.LD_PLOT };
    }


    /**
     * Unlike other tracks, variant tracks have their own internal notion of the current range
     * separate from whatever the LocationController provides.
     * @param ignored1
     * @param ignored2
     */
    @Override
    public void prepareForRendering(String ignored1, Range ignored2) {
        Resolution r = getResolution(range);
        if (r == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving variant data...");
            requestData(reference, range);
        } else {
            saveNullData();
        }

        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());

        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        // Shove in a placeholder axis range since we won't know the actual range until the data arrives.
        AxisRange axes = null;
        switch (getDrawingMode()) {
            case STANDARD:
                axes = new AxisRange(0, 1, 0, 1);
                break;
            case MATRIX:
                axes = new AxisRange(0, participants.size(), 0, 1);
                break;
            case LD_PLOT:
                // TODO: Implement this properly.
                axes = new AxisRange(0, 1, 0, 1);
                break;
        }
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, axes);
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, false);
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return Resolution.HIGH;
    }

    @Override
    public AxisType getXAxisType(Resolution ignored) {
        return AxisType.NONE;
    }

    @Override
    public AxisType getYAxisType(Resolution ignored) {
        return AxisType.INTEGER_GRIDLESS;
    }

    /**
     * Zoom out one level
     */
    public void zoomOut() {
        GraphPaneAdapter gp = getFrame().getGraphPane();
        gp.setUnitHeight(Math.max(1.0, gp.getUnitHeight() * 0.5));
        repaint();
    }

    /**
     * Zoom in one level
     */
    public void zoomIn() {
        GraphPaneAdapter gp = getFrame().getGraphPane();
        gp.setUnitHeight(Math.min(8.0, gp.getUnitHeight() * 2.0));
        repaint();
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
            int count = varRec.getParticipantCount();
            for (int i = 0; i < count; i++) {
                if (varRec.getVariantForParticipant(i) != VariantRecord.VariantType.NONE) {
                    if (participants == null) {
                        // TODO: There's probably a better place to init this.
                        participants = new BitSet(count);
                        participants.set(0, count);
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
