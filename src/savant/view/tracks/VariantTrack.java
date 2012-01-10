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

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JScrollBar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.FrameAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
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
import savant.util.swing.TallScrollingPanel;
import savant.view.swing.VariantGraphPane;

/**
 * Track class for representing VCF files.
 *
 * @author tarkvara
 */
public class VariantTrack extends Track {
    private static final Log LOG = LogFactory.getLog(VariantTrack.class);

    private String reference;
    private Range visibleRange;

    public VariantTrack(DataSourceAdapter ds) throws SavantTrackCreationCancelledException {
        super(ds, new VariantTrackRenderer());
        filter = new ParticipantFilter();
        
        // Set the initial range and reference to whatever Savant is displaying.
        LocationController lc = LocationController.getInstance();
        setLocation(lc.getReferenceName(), lc.getRange());
        lc.addListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                setLocation(event.getReference(), (Range)event.getRange());
            }
        });
    }

    /**
     * Our frame may be late in arriving, so override setFrame to update the visible range correctly.
     */
    @Override
    public void setFrame(FrameAdapter frame) {
        super.setFrame(frame);
        setVisibleRange(LocationController.getInstance().getRange());
        ((VariantGraphPane)frame.getGraphPane()).getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                int start = ae.getValue();
                if (start != visibleRange.getFrom()) {
                    setVisibleRange(new Range(start, Math.min(start + visibleRange.getLength(), LocationController.getInstance().getMaxRangeEnd())));
                }
            }
        });
    }

    private void setLocation(String ref, Range r) {
        reference = ref;
        setVisibleRange(r);
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
        Resolution r = getResolution(visibleRange);
        if (r == Resolution.HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving variant data...");
            requestData(reference, visibleRange);
        } else {
            saveNullData();
        }

        renderer.addInstruction(DrawingInstruction.RANGE, visibleRange);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());

        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        // Shove in a placeholder axis range since we won't know the actual range until the data arrives.
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(0, 1, 0, 1));
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

    public String getReference() {
        return reference;
    }
    
    /**
     * The variant track purports to display the entire chromosome, but only a sub-range may actually 
     * be visible in the scrolling area.
     * @return the range which is currently visible in the scrolling area
     */
    public Range getVisibleRange() {
        return visibleRange;
    }

    public void setVisibleRange(Range r) {
        if (getFrame() != null) {
            if (!r.equals(visibleRange)) {
                visibleRange = r;
                JScrollBar scroller = ((TallScrollingPanel)((VariantGraphPane)getFrame().getGraphPane()).getParent()).getScrollBar();
                scroller.setMaximum(LocationController.getInstance().getMaxRangeEnd());
                scroller.setValue(visibleRange.getFrom());
                scroller.setVisibleAmount(visibleRange.getLength());

                getRenderer().clearInstructions();
                prepareForRendering(reference, visibleRange);
                repaint();
            }
        }
    }

    /**
     * Zoom out by a factor of two.
     */
    public void zoomOut() {
        zoomToLength(visibleRange.getLength() * 2);
    }

    /**
     * Zoom in by a factor of two.
     */
    public void zoomIn() {
        zoomToLength(visibleRange.getLength() / 2);
    }

    private void zoomToLength(int length) {
        int maxLen = LocationController.getInstance().getMaxRangeEnd();
        if (length > maxLen) {
            length = maxLen;
        } else if (length < 1) {
            length = 1;
        }
        LOG.info("Zooming to length " + length);
        int from = (visibleRange.getFrom() + 1 + visibleRange.getTo() - length) / 2;
        int to = from + length - 1;

        if (from < 1) {
            to += 1 - from;
            from = 1;
        }

        if (to > maxLen) {
            from -= to - maxLen;
            to = maxLen;

            if (from < 1) {
                from = 1;
            }
        }

        setVisibleRange(new Range(from, to));
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
                if (varRec.getVariantForParticipant(i) != VariantType.NONE) {
                    return true;
                }
            }
            return false;
        }
    }
}
