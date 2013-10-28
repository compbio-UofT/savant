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
package savant.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import org.apache.commons.httpclient.NameValuePair;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ut.biolab.savant.analytics.savantanalytics.AnalyticsAgent;

import savant.api.event.LocationChangedEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Listener;
import savant.api.event.GenomeChangedEvent;
import savant.data.types.Genome;
import savant.util.Controller;
import savant.util.Range;


/**
 * Controller object to manage changes to current chromosome and viewed range.
 *
 * @author AndrewBrook, vwilliams
 */
public class LocationController extends Controller<LocationChangedEvent> implements Listener<GenomeChangedEvent> {

    private static LocationController instance;
    private static final Log LOG = LogFactory.getLog(LocationController.class);

    private String currentReference;

    // Undo/Redo Stack
    private Stack<History> undoStack;
    private Stack<History> redoStack;
    private int maxUndoStackSize = 50;
    private boolean shouldClearRedoStack = true;

    // The maximum and current viewable range
    private Range maximumViewableRange;
    private Range currentViewableRange;

    /** Reference which was set before genome was loaded (e.g. while loading a project). */
    private String pendingReference;

    /** Range which was set before genome was loaded (e.g. while loading a project). */
    private Range pendingRange;

    public static synchronized LocationController getInstance() {
        if (instance == null) {
            instance = new LocationController();
            GenomeController.getInstance().addListener(instance);
        }
        return instance;
    }

    private LocationController() {
        undoStack = new Stack<History>();
        redoStack = new Stack<History>();
    }

    public void setLocation(String ref, boolean forceEvent) {
        if (isValidAndNewReference(ref) || forceEvent) {
            updateHistory();
            setReference(ref);
            fireEvent(new LocationChangedEvent(true, currentReference, currentViewableRange));
        }
    }

    public void setLocation(Range r) {
        if (!r.equals(currentViewableRange)) {
            updateHistory();
            setRange(r);
            fireEvent(new LocationChangedEvent(false, currentReference, currentViewableRange));
        }
    }

    public void setLocation(int from, int to) {
        setLocation(new Range(from, to));
    }

    /**
     * This is the version of setLocation() which does the actual work.  All the other
     * overloads should be calling this one.
     */
    public void setLocation(String ref, Range range) {
        if (GenomeController.getInstance().isGenomeLoaded()) {
            boolean refChanging = isValidAndNewReference(ref);
            boolean rangeChanging = !range.equals(currentViewableRange);
            if (refChanging || rangeChanging) {
                updateHistory();
                if (refChanging) {
                    setReference(ref);
                }
                if (rangeChanging) {
                    setRange(range);
                }
                fireEvent(new LocationChangedEvent(refChanging, currentReference, currentViewableRange));
            }
            pendingReference = null;
            pendingRange = null;
        } else {
            pendingReference = ref;
            pendingRange = range;
        }
    }

    private void setLocation(History history) {
        setLocation(history.reference, history.range);
    }

    /**
     * Set the reference. Always check isValidAndNewReference before doing this.
     */
    private void setReference(String ref) {
        Set<String> allRefs = getAllReferenceNames();
        if (!allRefs.contains(ref) && allRefs.contains("chr" + ref)) {
            ref = "chr" + ref;
        }
        currentReference = ref;
        Genome loadedGenome = GenomeController.getInstance().getGenome();
        setMaxRange(new Range(1, loadedGenome.getLength()));
        setRange(1, Math.min(1000, loadedGenome.getLength()));
    }

    /**
     * Check if reference exists and is not the current reference
     *
     * @param ref           the new reference
     * @return True iff reference can be changed
     */
    private boolean isValidAndNewReference(String ref) {
        Set<String> allRefs = getAllReferenceNames();
        if (allRefs.contains(ref)) {
            if (!ref.equals(currentReference)) {
                return true;
            }
        } else if (allRefs.contains("chr" + ref)) {
            // Looks like a number.  Let's try slapping on "chr" and see if it works.
            return isValidAndNewReference("chr" + ref);
        } else {
            if (TrackController.getInstance().getTracks().size() > 0) {
                DialogUtils.displayMessage(String.format("<html>Reference <i>%s</i> not found in loaded tracks.</html>", ref));
            }
        }
        return false;
    }

    public String getReferenceName() {
        return this.currentReference;
    }

    public Set<String> getAllReferenceNames() {
        Set<String> all = new HashSet<String>();
        all.addAll(GenomeController.getInstance().getGenome().getReferenceNames());
        all.addAll(getNonGenomicReferenceNames());
        return all;
    }

    public Set<String> getReferenceNames() {
        return GenomeController.getInstance().getGenome().getReferenceNames();
    }

    public int getReferenceLength(String refname) {
        return GenomeController.getInstance().getGenome().getLength(refname);
    }

    public Set<String> getNonGenomicReferenceNames() {
        return new HashSet<String>();
    }


    private void setRange(int from, int to) {
        setRange(new Range(from, to));
    }

    private void setRange(Range r) {
        LOG.debug("Setting range to " + r);

        int from = r.getFrom();
        int to = r.getTo();

        /*
         * Make sure the current viewable range
         * stays within the maximum viewable range
         */
        if (from < getMaxRangeStart()) {
            int diff = getMaxRangeStart() - from;
            from = getMaxRangeStart();
            to += diff;
        }

        if (to > getMaxRangeEnd()) {
            int diff = to - getMaxRangeEnd();
            to = getMaxRangeEnd();
            from -= diff;
        }

        if (from < getMaxRangeStart()) {
            from = getMaxRangeStart();
            to = getMaxRangeEnd();
        }

        r = new Range(from, to);



        // set the current viewable range
        currentViewableRange = r;

        System.gc();
    }

    /**
     * Set the maximum viewable range (usually 0 to genome size).
     * @param r The range to set as max
     */
    public void setMaxRange(Range r) {
        maximumViewableRange = r;
        LOG.debug("Setting maximum range to " + r);
    }

    /**
     * Get the maximumViewableRange.
     * @return The maximumViewableRange
     */
    public Range getMaxRange() {
        return maximumViewableRange;
    }

    /**
     * Get the lower bound on the maximumViewableRange.
     * @return The lower bound on the maximumViewableRange
     */
    public int getMaxRangeStart() {
        return maximumViewableRange.getFrom();
    }

    /**
     * Get the upper bound on the maximumViewableRange.
     * @return The upper bound on the maximumViewableRange
     */
    public int getMaxRangeEnd() {
        return maximumViewableRange.getTo();
    }

    /**
     * Get the currentViewableRange.
     * @return The currentViewableRange
     */
    public Range getRange() {
        return currentViewableRange;
    }

    /**
     * Get the lower bound on the currentViewableRange.
     * @return The lower bound on the currentViewableRange
     */
    public int getRangeStart() {
        return currentViewableRange.getFrom();
    }

    /**
     * Get the upper bound on the currentViewableRange.
     * @return The upper bound on the currentViewableRange
     */
    public int getRangeEnd() {
        return currentViewableRange.getTo();
    }
    /**
     * == [[ SHIFTS ]] ==
     *  Shifts move the current viewable range left
     *  or right.
     */
    /**
     * Shift the currentViewableRange to the left
     */
    public void shiftRangeLeft() {
        shiftRange(false, 0.5);
    }

    /**
     * Shift the currentViewableRange to the right
     */
    public void shiftRangeRight() {
        shiftRange(true, 0.5);
    }

    /**
     * Shift the range left or right a specified percentage of the current
     * viewable range
     * @param shiftRight True if shift is to the right, false for left
     * @param percentwindow How much of the current window to be visible after
     * the shift
     */
    public void shiftRange(boolean shiftRight, double percentwindow) {
        Range r = getRange();
        int length = r.getLength();
        int direction = 1;
        if (!shiftRight) {
            direction = -1;
        }
        //int shift = (int) Math.ceil(direction * (percentwindow * length)) - ( (direction == 1) ? 1 : 0);
        int shift = (int)Math.ceil(direction * (percentwindow * length)) - ( (direction == -1 && length == 1) ? 1 : 0);

        r = new Range(r.getFrom() + shift, r.getTo() + shift);
        setLocation(r);
    }

    /**
     * Shift the currentViewableRange all the way to the left
     */
    public void shiftRangeFarLeft() {
        Range r = new Range(1, getRange().getLength());
        setLocation(r);
    }

    /**
     * Shift the currentViewableRange all the way to the right
     */
    public void shiftRangeFarRight() {
        Range r = new Range(getMaxRangeEnd() - getRange().getLength() + 1, getMaxRangeEnd());
        setLocation(r);
    }

    /**
     * == [[ Zoom ]] ==
     *  Zoom in makes the viewable range smaller
     *  Zoom out makes the viewable range larger
     */
    /**
     * Zoom to the specified length.
     * @param length The length to which to zoom
     */
    public void zoomToLength(int length) {
        zoomToLength(length, (getRangeEnd()+ 1 + getRangeStart()) / 2); // + 1 because ranges are inclusive
    }

    public void zoomToLength(int length, int center) {
        length = Math.min(length, maximumViewableRange.getLength());
        length = Math.max(length, 1);

        LOG.debug("Zooming to length " + length);

        if (length > getMaxRangeEnd()) {
            return; // can't go any further out, stay at same range.
        }

        int half = Math.max(length / 2, 1);

        Range rg = new Range(center-half,center-half+length-1);
        setLocation(rg);
    }

    /**
     * Zoom out one level
     */
    public void zoomOut() {
        zoomToLength(currentViewableRange.getLength() * 2);
    }

    /**
     * Zoom in one level
     */
    public void zoomIn() {
        if (currentViewableRange.getLength() > 1) {
            zoomToLength(currentViewableRange.getLength() / 2);
        }
    }

    public void zoomInOnMouse() {
        int center = GraphPaneController.getInstance().getMouseXPosition();
        zoomToLength(currentViewableRange.getLength() / 2, center);
    }

    public void zoomOutFromMouse() {
        int center = GraphPaneController.getInstance().getMouseXPosition();
        zoomToLength(currentViewableRange.getLength() * 2, center);
    }

    //HISTORY///////////////////////////////////////////////////////////////////

    private void updateHistory() {
        if (shouldClearRedoStack && currentViewableRange != null && currentReference != null) {
            redoStack.clear();
            undoStack.push(new History(currentReference, currentViewableRange));
            while (undoStack.size() > maxUndoStackSize) {
                undoStack.remove(0);
            }
        }
    }

    public void undoLocationChange() {
        if (undoStack.size() > 0) {
            shouldClearRedoStack = false;
            redoStack.push(new History(currentReference, currentViewableRange));
            setLocation(undoStack.pop());
            shouldClearRedoStack = true;
        }
    }

    public void redoLocationChange() {
        if (redoStack.size() > 0) {
            shouldClearRedoStack = false;
            undoStack.push(new History(currentReference, currentViewableRange));
            setLocation(redoStack.pop());
            shouldClearRedoStack = true;
        }
    }

    @Override
    public void handleEvent(GenomeChangedEvent event) {
        if (pendingReference != null) {
            setLocation(pendingReference, pendingRange);
        } else if (event.getNewGenome() != event.getOldGenome()) {
            // Auto-select the first reference on the new genome.
            String ref = event.getNewGenome().getReferenceNames().iterator().next();
            setLocation(ref, true);
        }
    }

    private class History {
        public Range range;
        public String reference;
        public History(String ref, Range range) {
            this.range = range;
            this.reference = ref;
        }
    }
}
