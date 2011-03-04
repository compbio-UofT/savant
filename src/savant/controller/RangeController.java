/*
 * RangeController.java
 * Created on Jan 19, 2010
 *
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

package savant.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.event.RangeChangeCompletedListener;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.RangeChangedListener;
import savant.settings.BrowserSettings;
import savant.util.Range;
import savant.view.swing.Savant;

/**
 * Controller object to manage changes to viewed range.
 * @author vwilliams
 */
public class RangeController {

    private static RangeController instance;

    // Undo/Redo Stack
    private Stack undoStack;
    private Stack redoStack;
    private int maxUndoStackSize = 50;
    private boolean shouldClearRedoStack = true;

    private static final Log LOG = LogFactory.getLog(RangeController.class);

    /** The maximum and current viewable range */
    private Range maximumViewableRange;
    private Range currentViewableRange;

    /** Range Changed Listeners */
    private List<RangeChangedListener> rangeChangedListeners;
    private List<RangeChangeCompletedListener> rangeChangeCompletedListeners;

    public static synchronized RangeController getInstance() {
        if (instance == null) {
            instance = new RangeController();
        }
        return instance;
    }

    private RangeController() {
        rangeChangedListeners = new ArrayList();
        rangeChangeCompletedListeners = new ArrayList();
        undoStack = new Stack();
        redoStack = new Stack();
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
     * Set the maximum viewable range (usually 0 to genome size).
     * @param from Lowest value of the maximum viewable range
     * @param to Highest value of the maximum viewable range
     */
    public void setMaxRange(long from, long to) {
        setMaxRange(new Range(from, to));
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
    public long getMaxRangeStart() {
        return maximumViewableRange.getFrom();
    }

    /**
     * Get the upper bound on the maximumViewableRange.
     * @return The upper bound on the maximumViewableRange
     */
    public long getMaxRangeEnd() {
        return maximumViewableRange.getTo();
    }

    /**
     * Set the currentViewableRange.
     * @param r The range to set as current
     */
    public void setRange(Range r) {
        LOG.debug("Setting range to " + r);

        if (shouldClearRedoStack && currentViewableRange != null) {
            redoStack.clear();
            undoStack.push(currentViewableRange);
            while (undoStack.size() > maxUndoStackSize) {
                undoStack.remove(0);
            }
        }

        long from = r.getFrom();
        long to = r.getTo();

        /*
         * Make sure the current viewable range
         * stays within the maximum viewable range
         */
        if (from < getMaxRangeStart()) {
            long diff = getMaxRangeStart() - from;
            from = getMaxRangeStart();
            to += diff;
        }

        if (to > getMaxRangeEnd()) {
            long diff = to - getMaxRangeEnd();
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

        Savant.getInstance().updateRange();

        fireRangeChangedEvent();

        System.gc();
    }

    public synchronized void fireRangeChangeCompletedEvent() {
        RangeChangedEvent evt = new RangeChangedEvent(this, currentViewableRange);
        for (RangeChangeCompletedListener l : rangeChangeCompletedListeners) {
            l.rangeChangeCompletedReceived(evt);
        }
    }

    /**
     * Fire the RangeChangedEvent
     */
    private synchronized void fireRangeChangedEvent() {
        RangeChangedEvent evt = new RangeChangedEvent(this, currentViewableRange);
        for (RangeChangedListener l : rangeChangedListeners) {
            l.rangeChangeReceived(evt);
        }
    }
    
     public synchronized void addRangeChangeCompletedListener(RangeChangeCompletedListener l) {
        rangeChangeCompletedListeners.add(l);
    }

    public synchronized void removeRangeChangeCompletedListener(RangeChangeCompletedListener l) {
        rangeChangeCompletedListeners.remove(l);
    }


    public synchronized void addRangeChangedListener(RangeChangedListener l) {
        rangeChangedListeners.add(l);
    }

    public synchronized void removeRangeChangedListener(RangeChangedListener l) {
        rangeChangedListeners.remove(l);
    }

    /**
     * Set the currentViewableRange.
     * @param from The lower bound on the viewed range
     * @param to The upper bound on the viewed range
     */
    public void setRange(long from, long to) {
        setRange(new Range(from, to));
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
    public long getRangeStart() {
        return currentViewableRange.getFrom();
    }

    /**
     * Get the upper bound on the currentViewableRange.
     * @return The upper bound on the currentViewableRange
     */
    public long getRangeEnd() {
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
        long length = r.getLength();
        int direction = 1;
        if (!shiftRight) {
            direction = -1;
        }
        //int shift = (int) Math.ceil(direction * (percentwindow * length)) - ( (direction == 1) ? 1 : 0);
        long shift = (long) Math.ceil(direction * (percentwindow * length)) - ( (direction == -1 && length == 1) ? 1 : 0);

        r = new Range(r.getFrom() + shift, r.getTo() + shift);
        setRange(r);
    }

    /**
     * Shift the currentViewableRange all the way to the left
     */
    public void shiftRangeFarLeft() {
        Range r = new Range(1, getRange().getLength());
        setRange(r);
    }

    /**
     * Shift the currentViewableRange all the way to the right
     */
    public void shiftRangeFarRight() {
        Range r = new Range(getMaxRangeEnd() - getRange().getLength() + 1, getMaxRangeEnd());
        setRange(r);
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
    public void zoomToLength(long length) {
        zoomToLength(length, (getRangeEnd()+ 1 + getRangeStart()) / 2); // + 1 because ranges are inclusive
    }
    
    public void zoomToLength(long length, long center) {

        if(length > maximumViewableRange.getLength()){
            zoomToLength(maximumViewableRange.getLength());
        }

        length = Math.max(length, 1);
        LOG.debug("Zooming to length " + length);

        if (length > getMaxRangeEnd()) {
            return; // can't go any further out, stay at same range.
        }

        long half = Math.max(length / 2, 1);

        Range rg = new Range(center-half,center-half+length-1);
        setRange(rg);
    }

    /**
     * Zoom out one level
     */
    public void zoomOut() {
        System.out.println("current len = " + currentViewableRange.getLength() + " to length: " + currentViewableRange.getLength() * BrowserSettings.zoomAmount);
        long length = Math.min(maximumViewableRange.getLength(), currentViewableRange.getLength() * BrowserSettings.zoomAmount);
        zoomToLength(length);
    }

    /**
     * Zoom in one level
     */
    public void zoomIn() {
        zoomToLength(currentViewableRange.getLength() / BrowserSettings.zoomAmount);
    }

    /**
     * Set the range to the the most recent one in backward stack
     */
    public void undoRangeChange() {
        if (undoStack.size() > 0) {
            shouldClearRedoStack = false;
            redoStack.push(currentViewableRange);
            setRange((Range) undoStack.pop());
            shouldClearRedoStack = true;
        }
    }

    /**
     * Set the range to the most recent one in forward stack
     */
    public void redoRangeChange() {

        if (redoStack.size() > 0) {
            shouldClearRedoStack = false;
            undoStack.push(currentViewableRange);
            setRange((Range)redoStack.pop());
            shouldClearRedoStack = true;
        }
    }

    public void zoomInOnMouse() {
        long center = GraphPaneController.getInstance().getMouseXPosition();
        zoomToLength(currentViewableRange.getLength() / BrowserSettings.zoomAmount,center);
    }

    public void zoomOutFromMouse() {
        long center = GraphPaneController.getInstance().getMouseXPosition();
        zoomToLength(currentViewableRange.getLength() * BrowserSettings.zoomAmount,center);
    }

    public void setRange(String reference, Range range) {
        ReferenceController.getInstance().setReference(reference);
        setRange(range);
    }
}
