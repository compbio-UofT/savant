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
package savant.view.variation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.VariantDataSourceAdapter;
import savant.api.data.DataFormat;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.event.DataRetrievalEvent;
import savant.api.event.LocationChangedEvent;
import savant.api.event.TrackEvent;
import savant.api.util.Listener;
import savant.api.util.RangeUtils;
import savant.controller.GraphPaneController;
import savant.controller.LocationController;
import savant.controller.TrackController;
import savant.settings.ResolutionSettings;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.tracks.VariantTrack;
import savant.view.variation.swing.VariationModule;

/**
 * Controller class which governs the behaviour of the various variation-related views.
 * @author tarkvara
 */
public class VariationController implements Listener<DataRetrievalEvent> {
    static final Log LOG = LogFactory.getLog(VariationController.class);

    /**
     * Compare two variant records with an eye to merger.  The comparison is based on reference and position only (ignoring
     * the refBases and altAlleles fields which are usually considered by compareTo.
     */
    private static final Comparator<VariantRecord> VARIANT_COMPARATOR = new Comparator<VariantRecord>() {
        @Override
        public int compare(VariantRecord t, VariantRecord t1) {
            return new CompareToBuilder().append(t.getReference(), t1.getReference()).
                                          append(t.getPosition(), t1.getPosition()).toComparison();

        }
    };

    private static VariationController instance;

    private List<VariantTrack> tracks = new ArrayList<VariantTrack>();
    private List<List<VariantRecord>> rawData = new ArrayList<List<VariantRecord>>();
    private List<VariantRecord> aggregateData = null;
    private String[] participants = new String[0];
    private Set<String> controls = new HashSet<String>();

    private String visibleRef;
    private Range visibleRange;
    
    private VariationModule module;

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
                            t.addListener(VariationController.this);
                            if (controls.contains(t.getName())) {
                                controls.addAll(Arrays.asList(t.getParticipantNames()));
                            }
                            break;
                        case REMOVED:
                            int index = tracks.indexOf(t);
                            tracks.remove(index);
                            rawData.remove(index);
                            t.removeListener(VariationController.this);
                            if (controls.contains(t.getName())) {
                                controls.remove(t.getName());
                                controls.removeAll(Arrays.asList(t.getParticipantNames()));
                            }
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
                        module.showProgress("Retrieving variant data\u2026", -1.0);
                        break;
                    case COMPLETED:
                        if (evt.getData() != null) {
                            rawData.set(index, (List)evt.getData());
                        } else {
                            rawData.set(index, new ArrayList<VariantRecord>(0));
                        }
                        LOG.trace("Received " + rawData.get(index).size() + " records for " + evt.getTrack() + "; recalculating.");
                        recalculate();
                        break;
                    case FAILED:
                        LOG.info("Received " + evt.getError() + " error for " + evt.getTrack());
                        module.showMessage(MiscUtils.getMessage(evt.getError()));
                        break;
                }
            }
        }
    }

    public synchronized List<VariantRecord> getData() {
        if (aggregateData == null) {
            int n = 0;
            int i = 0;
            List<String> names = new ArrayList<String>();
            for (VariantTrack t: tracks) {
                List<VariantRecord> trackData = rawData.get(i++);
                if (trackData != null) {
                    if (aggregateData == null) {
                        aggregateData = new ArrayList<VariantRecord>(trackData.size());
                    }
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
                    names.addAll(Arrays.asList(((VariantDataSourceAdapter)t.getDataSource()).getParticipants()));
                    n += t.getParticipantCount();
                }
            }
            participants = names.toArray(new String[0]);
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

    public boolean isAControl(String p) {
        return controls.contains(p);
    }

    public String[] getParticipants() {
        return participants;
    }

    public int getParticipantCount() {
        return participants.length;
    }

    public VariantTrack[] getTracks() {
        return tracks.toArray(new VariantTrack[0]);
    }

    public void setLocation(String ref, Range r) {
        visibleRef = ref;
        setVisibleRange(r);
    }

    public Range getVisibleRange() {
        return visibleRange;
    }

    void setVisibleRange(Range r) {
        if (!r.equals(visibleRange)) {
            visibleRange = r;
            module.visibleRangeChanged(visibleRef, r);
            if (r.getLength() <= ResolutionSettings.getVariantLowToHighThreshold()) {
                for (VariantTrack t: tracks) {
                    t.requestData(visibleRef, visibleRange);
                }
            }
        }
    }

    /**
     * Adjust the visible range by setting its start.  Used when scrolling.
     */
    public void setVisibleRange(int start) {
        if (start != visibleRange.getFrom()) {
            setVisibleRange(new Range(start, Math.min(start + visibleRange.getLength(), LocationController.getInstance().getMaxRangeEnd())));
        }
    }
    
    public String getReference() {
        return visibleRef;
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
    public void navigateToRecord(Record rec) {
        if (rec != null) {
            Range r;
            if (rec instanceof LDRecord) {
                // Unlike other records, LDRecords consist of two actual locations.
                List<VariantRecord> recs = ((LDRecord)rec).getConstituents();
                int pos0 = recs.get(0).getPosition();
                int pos1 = recs.get(1).getPosition();
                if (pos0 < pos1) {
                    r = new Range(pos0, pos1);
                } else {
                    r = new Range(pos1, pos0);
                }
            } else {
                int pos = ((VariantRecord)rec).getPosition();
                r = new Range(pos, pos);
            }
            LocationController.getInstance().setLocation(visibleRef, (Range)RangeUtils.addMargin(r));
        }
    }
    
    /**
     * A mouse-move on one of our sub-panels.  Update the status bar in the main window.
     */
    public void updateStatusBar(VariantRecord rec) {
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
        module.showProgress("Aggregating variant data\u2026", -1.0);
        aggregateData = null;
        module.recalculated(getData());
    }

    public void cancelDataRequests() {
        LOG.info("Cancelling data requests for " + tracks.size() + " variant tracks.");
        for (VariantTrack t: tracks) {
            t.cancelDataRequest();
        }
    }

    public VariationModule getModule() {
        if (module == null) {
            module = new VariationModule(this);
        }
        return module;
    }
    
    public boolean isDPrimeSelected() {
        return module.isDPrimeSelected();
    }

    /**
     * Provides a displayable name for variant records which lack a name of their own.
     */
    public static String getDisplayName(VariantRecord rec) {
        String result = rec.getName();
        if (result == null || result.isEmpty()) {
            result = rec.getReference() + ":" + rec.getPosition();
        }
        return result;
    }
}
