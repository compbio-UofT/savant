/*
 *    Copyright 2012 University of Toronto
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
package savant.view.swing.variation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.api.data.DataFormat;
import savant.api.data.VariantRecord;
import savant.api.event.DataRetrievalEvent;
import savant.api.event.LocationChangedEvent;
import savant.api.event.TrackEvent;
import savant.api.util.Listener;
import savant.api.util.RangeUtils;
import savant.controller.GraphPaneController;
import savant.controller.LocationController;
import savant.controller.TrackController;
import savant.data.sources.TabixDataSource;
import savant.settings.TrackResolutionSettings;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.tracks.VariantTrack;

/**
 * Controller class which governs the behaviour of the various variation-related views.
 * @author tarkvara
 */
public class VariationController implements Listener<DataRetrievalEvent> {
    static final Log LOG = LogFactory.getLog(VariationController.class);

    private static final Comparator<VariantRecord> VARIANT_COMPARATOR = new Comparator<VariantRecord>() {
        @Override
        public int compare(VariantRecord t, VariantRecord t1) {
            return t.compareTo(t1);
        }
    };

    private static VariationController instance;

    List<VariantTrack> tracks = new ArrayList<VariantTrack>();
    List<List<VariantRecord>> rawData = new ArrayList<List<VariantRecord>>();
    List<VariantRecord> aggregateData = null;
    List<String> participantNames = new ArrayList<String>();
    Set<String> controls = new HashSet<String>();

    private int participantCount;
    private String visibleRef;
    private Range visibleRange;
    private boolean adjustingRange = false;
    
    private VariationSheet sheet;

    private VariationController() {
        LocationController.getInstance().addListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                // Only change the variation range if the track range falls outside.
                String ref = event.getReference();
                RangeAdapter r = event.getRange();
                if (!ref.equals(visibleRef) || !RangeUtils.contains(visibleRange, r)) {
                    setLocation(ref, (Range)r);
                }
            }
        });

        // Attach listeners for track change events.
        TrackController.getInstance().addListener(new Listener<TrackEvent>() {

            @Override
            public void handleEvent(TrackEvent event) {
                if (event.getTrack().getDataFormat() == DataFormat.VARIANT) {
                    VariantTrack t = (VariantTrack)event.getTrack();
                    switch (event.getType()) {
                        case ADDED:
                            tracks.add(t);
                            rawData.add(null);
                            participantCount += t.getParticipantCount();
                            t.addListener(VariationController.this);
                            break;
                        case REMOVED:
                            participantCount -= t.getParticipantCount();
                            int index = tracks.indexOf(t);
                            tracks.remove(index);
                            rawData.remove(index);
                            t.removeListener(VariationController.this);
                            recalculate();
                            break;
                    }
                }
            }
        });
    }
    
    public static VariationController getInstance() {
        if (instance == null) {
            instance = new VariationController();
        }
        return instance;
    }

    /**
     * Sets the data to null so we know that there's nothing to render.
     *
     * @param evt describes the data being received
     */
    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        if (evt.getRange().equals(visibleRange)) {
            int index = tracks.indexOf(evt.getTrack());
            if (index >= 0) {
                switch (evt.getType()) {
                    case STARTED:
                        rawData.set(index, null);
                        sheet.showProgress("Retrieving variant data…");
                        break;
                    case COMPLETED:
                        if (evt.getData() != null) {
                            LOG.trace("Received " + evt.getData().size() + " records for " + evt.getTrack() + "; recalculating.");
                            rawData.set(index, (List)evt.getData());
                            recalculate();
                        }
                        break;
                    case FAILED:
                        LOG.info("Received " + evt.getError() + " error for " + evt.getTrack());
                        sheet.showMessage(MiscUtils.getMessage(evt.getError()));
                        break;
                }
            }
        }
    }

    synchronized List<VariantRecord> getData() {
        if (aggregateData == null) {
            int n = 0;
            int i = 0;
            participantNames.clear();
            for (VariantTrack t: tracks) {
                List<VariantRecord> trackData = rawData.get(i++);
                if (trackData != null) {
                    if (aggregateData == null) {
                        aggregateData = new ArrayList<VariantRecord>(trackData.size());
                        for (VariantRecord rec: trackData) {
                            aggregateData.add(new PaddedVariantRecord(rec, 0));
                        }
                    } else {
                        // Slower process.  Traverse the list inserting data as we go.
                        // It would might be more efficient to insert everything and sort,
                        // but we have to allow for the possibility of having to munge together
                        // two VariantRecords.
                        for (VariantRecord rec: trackData) {
                            int index = Collections.binarySearch(aggregateData, rec, VARIANT_COMPARATOR);
                            if (index < 0) {
                                // Not found in list.  Insert it at the given location.
                                int insertionPos = -index - 1;
                                if (LOG.isDebugEnabled()) {
                                    String before = insertionPos > 0 ? aggregateData.get(insertionPos - 1).toString() : "START";
                                    String after = insertionPos < aggregateData.size() ? aggregateData.get(insertionPos).toString() : "END";

                                    LOG.debug("Inserting " + rec + " after " + before + " and before " + after);
                                }
                                aggregateData.add(insertionPos, new PaddedVariantRecord(rec, n));
                            } else {
                                VariantRecord oldRec = aggregateData.get(index);
                                LOG.debug("Merging " + rec + " into " + oldRec + " padding " + (n - oldRec.getParticipantCount()));
                                aggregateData.set(index, new MergedVariantRecord(oldRec, rec, n - oldRec.getParticipantCount()));
                            }
                        }
                    }
                    participantNames.addAll(Arrays.asList(((TabixDataSource)t.getDataSource()).getExtraColumns()));
                    n += t.getParticipantCount();
                }
            }
            participantCount = n;
        }
        return aggregateData;
    }

    /**
     * This method is used when storing the controls in the project.  We don't need to double-store the participants
     * for any tracks which are recorded as being stored.
     */
    public Set<String> getControls() {
        for (VariantTrack t: tracks) {
            if (controls.contains(t.getName())) {
                controls.removeAll(Arrays.asList(t.getParticipantNames()));
            }
        }
        return controls;
    }

    public void setControls(Collection<String> value) {
        controls.clear();
        controls.addAll(value);
        for (VariantTrack t: tracks) {
            if (controls.contains(t.getName())) {
                controls.addAll(Arrays.asList(t.getParticipantNames()));
            }
        }
    }

    int getParticipantCount() {
        return participantCount;
    }

    private void setLocation(String ref, Range r) {
        visibleRef = ref;
        setVisibleRange(r);
    }

    Range getVisibleRange() {
        return visibleRange;
    }

    void setVisibleRange(Range r) {
        if (!r.equals(visibleRange)) {
            adjustingRange = true;
            visibleRange = r;
            sheet.visibleRangeChanged(visibleRef, r);
            if (r.getLength() <= TrackResolutionSettings.getVariantLowToHighThreshold()) {
                for (VariantTrack t: tracks) {
                    t.requestData(visibleRef, visibleRange);
                }
            }
            adjustingRange = false;
        }
    }

    /**
     * Zoom out by a factor of two.
     */
    void zoomOut() {
        zoomToLength(visibleRange.getLength() * 2);
    }

    /**
     * Zoom in by a factor of two.
     */
    void zoomIn() {
        zoomToLength(visibleRange.getLength() / 2);
    }

    private void zoomToLength(int length) {
        int maxLen = LocationController.getInstance().getMaxRangeEnd();
        if (length > maxLen) {
            length = maxLen;
        } else if (length < 1) {
            length = 1;
        }
        LOG.trace("Zooming to length " + length);
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
     * One of our panels has clicked or double-clicked.  Navigate to the record in the main window.
     */
    void navigateToRecord(VariantRecord rec) {
        LocationController.getInstance().setLocation((Range)RangeUtils.addMargin(new Range(rec.getPosition(), rec.getPosition())));
    }
    
    /**
     * A mouse-move on one of our sub-panels.  Update the status bar in the main window.
     */
    void updateStatusBar(VariantRecord rec) {
        if (rec != null) {
            GraphPaneController.getInstance().setMouseXPosition(rec.getPosition());
        } else {
            GraphPaneController.getInstance().setMouseXPosition(-1);
        }
    }
    
    void recalculate() {
        for (List<VariantRecord> trackData: rawData) {
            if (trackData == null) {
                // One of the tracks hasn't reported in yet.
                return;
            }
        }
        sheet.showProgress("Aggregating variant data…");
        aggregateData = null;
        sheet.recalculated(getData());
    }

    void cancelDataRequests() {
        LOG.info("Cancelling data requests for " + tracks.size() + " variant tracks.");
        for (VariantTrack t: tracks) {
            t.cancelDataRequest();
        }
    }

    void adjustRange(int start) {
        if (!adjustingRange) {
            if (start != visibleRange.getFrom()) {
                setVisibleRange(new Range(start, Math.min(start + visibleRange.getLength(), LocationController.getInstance().getMaxRangeEnd())));
            }
        }
    }
    
    public VariationSheet getSheet() {
        sheet = new VariationSheet(this);
        return sheet;
    }
    
    boolean isDPrimeSelected() {
        return sheet.isDPrimeSelected();
    }
}
