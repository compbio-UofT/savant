/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.analysis;

import savant.controller.BookmarkController;
import savant.controller.RangeController;
import savant.controller.event.bookmark.BookmarksChangedEvent;
import savant.controller.event.bookmark.BookmarksChangedListener;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class BatchAnalysis implements RangeChangedListener, BookmarksChangedListener {

    private boolean isOn = true;

    /** Analyze Event Listeners */
    private List analyzeEventListeners;

    public synchronized void addAnalyzeEventListener(AnalyzeEventListener l) {
        analyzeEventListeners.add(l);
    }

    public synchronized void removeAnalyzeEventListener(AnalyzeEventListener l) {
        analyzeEventListeners.remove(l);
    }

       /**
     * Fire the ViewTrackListChangedEvent
     */
    private synchronized void fireAnalyzeEvent() {
        AnalyzeEvent evt = new AnalyzeEvent(this);
        Iterator listeners = this.analyzeEventListeners.iterator();
        while (listeners.hasNext()) {
            ((AnalyzeEventListener) listeners.next()).runAnalysis(evt);
        }
    }


    private void doAnalysis() {
        if (this.isOn) {
            fireAnalyzeEvent();
        }
    }

    private void listenToAppropriateEvent() {
        switch(this.fireOnEvent) {
            case RANGE_CHANGE:
                RangeController rc = RangeController.getInstance();
                rc.addRangeChangedListener(this);
                break;
            case BOOKMARK_CHANGE:
                BookmarkController bc = BookmarkController.getInstance();
                bc.addFavoritesChangedListener(this);
                break;
            default:
                break;
        }
    }

    public enum FIREONEVENT { ON_DEMAND, RANGE_CHANGE, BOOKMARK_CHANGE };

    private FIREONEVENT fireOnEvent;

    public BatchAnalysis(FIREONEVENT when, List<AnalyzeEventListener> listeners) {
        fireOnEvent = when;
        analyzeEventListeners = listeners;
        listenToAppropriateEvent();
    }

    public BatchAnalysis(FIREONEVENT when) {
        this(when, new ArrayList());
    }

    public void rangeChangeReceived(RangeChangedEvent event) {
        if (fireOnEvent == FIREONEVENT.RANGE_CHANGE) {
            doAnalysis();
        }
    }

    public void bookmarksChangeReceived(BookmarksChangedEvent event) {
        if (fireOnEvent == FIREONEVENT.BOOKMARK_CHANGE) {
            doAnalysis();
        }
    }

    public boolean toggleOn() {
        this.isOn = !this.isOn;
        return this.isOn;
    }

    public boolean isOn() {
        return this.isOn;
    }

    public void setOn(boolean isOn) {
        this.isOn = isOn;
    }

}
