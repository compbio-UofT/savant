/*
 *    Copyright 2010 University of Toronto
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

/*
 * RangeController.java
 * Created on Jan 19, 2010
 */

/**
 * Controller object to manage changes to viewed range.
 * @author vwilliams
 */
package savant.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;
import savant.util.Range;
import savant.settings.BrowserSettings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import savant.view.swing.Savant;

public class RangeController {

    private static RangeController instance;

    // Undo/Redo Stack
    private Stack undoStack;
    private Stack redoStack;
    private int maxUndoStackSize = 50;
    private boolean shouldClearRedoStack = true;

    private static Log log = LogFactory.getLog(RangeController.class);

    /** The maximum and current viewable range */
    private Range maximumViewableRange;
    private Range currentViewableRange;

    /** Range Changed Listeners */
    // TODO: List of what?
    private List rangeChangedListeners;

    public static synchronized RangeController getInstance() {
        if (instance == null) {
            instance = new RangeController();
        }
        return instance;
    }

    private RangeController() {
        rangeChangedListeners = new ArrayList();
        undoStack = new Stack();
        redoStack = new Stack();
    }

    /**
     * Set the maximum viewable range (usually 0 to genome size).
     * @param r The range to set as max
     */
    public void setMaxRange(Range r) {
        maximumViewableRange = r;
        log.debug("Setting maximum range to " + r);
    }

    /**
     * Set the maximum viewable range (usually 0 to genome size).
     * @param from Lowest value of the maximum viewable range
     * @param to Highest value of the maximum viewable range
     */
    public void setMaxRange(int from, int to) {
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
     * Set the currentViewableRange.
     * @param r The range to set as current
     */
    public void setRange(Range r) {
        log.debug("Setting range to " + r);
        Savant.log("Setting range to " + r, Savant.LOGMODE.NORMAL);

        if (shouldClearRedoStack && this.currentViewableRange != null) {
            redoStack.clear();
            undoStack.push(currentViewableRange);
            while (undoStack.size() > this.maxUndoStackSize) {
                undoStack.remove(0);
            }
        }

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

        //printStacks();

        Savant.getInstance().updateRange();

        // TODO: invoke a range changed event
        fireRangeChangedEvent();
        // try { RangeChanged(getRange(), null); } catch {}
    }

    /**
     * Fire the RangeChangedEvent
     */
    private synchronized void fireRangeChangedEvent() {
        RangeChangedEvent evt = new RangeChangedEvent(this, this.currentViewableRange);
        Iterator listeners = this.rangeChangedListeners.iterator();
        while (listeners.hasNext()) {
            ((RangeChangedListener) listeners.next()).rangeChangeReceived(evt);
        }
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
    public void setRange(int from, int to) {
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
        int shift = (int) Math.ceil(direction * (percentwindow * length)) - ( (direction == -1 && length == 1) ? 1 : 0);

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
    public void zoomToLength(int length) {
        zoomToLength(length, (getRangeEnd() + getRangeStart()) / 2);
    }
    
    public void zoomToLength(int length, int center) {

        if(length > this.maximumViewableRange.getLength()){
            zoomToLength(this.maximumViewableRange.getLength());
        }

        length = Math.max(length, 1);
        log.debug("Zooming to length " + length);

        if (length > getMaxRangeEnd()) {
            return; // can't go any further out, stay at same range.
        }
        //int middle = (getRangeEnd() + getRangeStart()) / 2;
        int half = Math.max(length / 2, 1);

        Range rg = new Range(center-half,center-half+length-1);
        setRange(rg);
        
        /*
        Range r=null;
        if (length == 1){
            r = new Range(center, center);
        }
        else if (half <= center) {
            r = new Range(center - half + 1, center + half);
        }
        else if ((center + half) > getMaxRangeEnd())
        {
            return; // can't go any further out,stay at same range
        }
        else if ( half > center) {
            r = new Range(1, length);
        }
        setRange(r);
         *
         */
    }

    /**
     * Zoom out one level
     */
    public void zoomOut() {
        //TRACKBAR_ZOOM.setValue(Math.max(0, TRACKBAR_ZOOM.getValue() - 1));
        int length = Math.min(this.maximumViewableRange.getLength(), this.currentViewableRange.getLength() * BrowserSettings.zoomAmount);
        zoomToLength(length);
        //zoomToLength(this.currentViewableRange.getLength() * BrowserDefaults.zoomAmount);
    }

    /**
     * Zoom in one level
     */
    public void zoomIn() {
        //TRACKBAR_ZOOM.setValue(Math.min(TRACKBAR_ZOOM.getMaximum(), TRACKBAR_ZOOM.getValue() + 1));
        zoomToLength(this.currentViewableRange.getLength() / BrowserSettings.zoomAmount);
    }

    /**
     * Set the range to the the most recent one in backward stack
     */
    public void undoRangeChange() {
        if (undoStack.size() > 0) {
            shouldClearRedoStack = false;
            redoStack.push(this.currentViewableRange);
            setRange((Range) undoStack.pop());
            shouldClearRedoStack = true;
        }

        //printStacks();
    }

    /**
     * Set the range to the most recent one in forward stack
     */
    public void redoRangeChange() {

        if (redoStack.size() > 0) {
            shouldClearRedoStack = false;
            undoStack.push(this.currentViewableRange);
            setRange((Range)redoStack.pop());
            shouldClearRedoStack = true;
        }

        //printStacks();
    }

    public void zoomInOnMouse() {
        int center = GraphPaneController.getInstance().getMouseXPosition();
        zoomToLength(this.currentViewableRange.getLength() / BrowserSettings.zoomAmount,center);
    }

    public void zoomOutFromMouse() {
        int center = GraphPaneController.getInstance().getMouseXPosition();
        zoomToLength(this.currentViewableRange.getLength() * BrowserSettings.zoomAmount,center);
    }

    public void setRange(String reference, Range range) {
        ReferenceController.getInstance().setReference(reference);
        this.setRange(range);
    }

    /*
     *
     *
     private void printStacks() {
        printStack(undoStack, "Undo");
        printStack(redoStack, "Redo");
    }
     
    public void printStack(Stack s, String name) {
        System.out.println("Stack: " + name);
        for (int i = 0; i < s.size(); i++) {
            System.out.println(i + ". " + s.get(i));
        }
    }
     * 
     */

}
