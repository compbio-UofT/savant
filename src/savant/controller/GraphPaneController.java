/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.controller;

import java.util.ArrayList;
import java.util.List;
import savant.controller.event.GraphPaneChangeEvent;
import savant.controller.event.GraphPaneChangeListener;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.Savant;

/**
 *
 * @author mfiume
 */
public class GraphPaneController {

    private boolean isSpotlight;
    private boolean isPlumbing;
    private boolean isZooming;
    private boolean isAiming;
    private boolean isPanning;
    private boolean isSelecting;

    private long mouseClickPosition;
    private long mouseReleasePosition;
    private long mouseXPosition;
    private long mouseYPosition;

    private List<GraphPane> graphpanesQueuedForRendering;

    private long spotlightSize;
    private double spotlightproportion = 0.25;

    private static GraphPaneController instance;

    private List<GraphPaneChangeListener> graphpaneChangeListeners;

    private boolean changeMade = false;

    public void clearRenderingList() {
        graphpanesQueuedForRendering.clear();
    }

    // Get current time
    long start;

    public void enlistRenderingGraphpane(GraphPane p) {
        //System.out.println("Enlisting gp " + p.getTrackRenderers().get(0).toString());
        graphpanesQueuedForRendering.add(p);
        if (graphpanesQueuedForRendering.size() == 1) {
            Savant.getInstance().updateStatus("rendering ...");
            start = System.currentTimeMillis();
        }
    }

    public void delistRenderingGraphpane(GraphPane p) {

        if (!graphpanesQueuedForRendering.contains(p)) {
            return;
        }

        if (graphpanesQueuedForRendering.isEmpty()) {
            return;
        }
        graphpanesQueuedForRendering.remove(p);
        if (graphpanesQueuedForRendering.isEmpty()) {
            long elapsedTimeMillis = System.currentTimeMillis()-start;
            // Get elapsed time in seconds
            float elapsedTimeSec = elapsedTimeMillis/1000F;
            Savant.getInstance().updateStatus("took " + elapsedTimeSec + " s");
            RangeController.getInstance().fireRangeChangeCompletedEvent();
        }
    }

    private GraphPaneController() {
        graphpaneChangeListeners = new ArrayList<GraphPaneChangeListener>();
        graphpanesQueuedForRendering = new ArrayList<GraphPane>();
    }

    public static synchronized GraphPaneController getInstance() {
        if (instance == null) {
            instance = new GraphPaneController();
        }
        return instance;
    }

    public synchronized void addBookmarksChangedListener(GraphPaneChangeListener l) {
        graphpaneChangeListeners.add(l);
    }

    public synchronized void removeBookmarksChangedListener(GraphPaneChangeListener l) {
        graphpaneChangeListeners.remove(l);
    }

    public boolean isChanged(){
        return changeMade;
    }


    public void askForRefresh() {
        if (this.isPanning() || this.isZooming() || this.isPlumbing() || this.isSpotlight() || this.isAiming()) {
            fireGraphPaneChangeEvent();
        }
    }

    public void forceRefresh() {
        fireGraphPaneChangeEvent();
    }

    private synchronized void fireGraphPaneChangeEvent() {
        GraphPaneChangeEvent evt = new GraphPaneChangeEvent(this);
        for (GraphPaneChangeListener listener : this.graphpaneChangeListeners) {
            listener.graphpaneChangeReceived(evt);
        }
    }

    public boolean isSelecting() {
        return this.isSelecting;
    }

    public void setSelecting(boolean selected) {
        this.isSelecting = selected;
    }

    public boolean isPlumbing() {
        return this.isPlumbing;
    }

    public void setPlumbing(boolean isPlumbing) {
        this.isPlumbing = isPlumbing;
        changeMade = true;
        forceRefresh();
        changeMade = false;
    }

    public boolean isSpotlight() {
        return this.isSpotlight;
    }

    public void setSpotlight(boolean isSpotlight) {
        this.isSpotlight = isSpotlight;
        changeMade = true;
        forceRefresh();
        changeMade = false;
    }

    public boolean isZooming() {
        return this.isZooming;
    }

    public boolean isAiming() {
        return this.isAiming;
    }

    public void setAiming(boolean isAiming) {
        this.isAiming = isAiming;
        changeMade = true;
        forceRefresh();
        changeMade = false;
    }

    public void setZooming(boolean isZooming) {
        this.isZooming = isZooming;
        forceRefresh();
    }

    public boolean isPanning() {
        return this.isPanning;
    }

    public void setPanning(boolean isPanning) {
        this.isPanning = isPanning;
        forceRefresh();
    }

    public Range getMouseDragRange() {
        return new Range(this.mouseClickPosition,this.mouseReleasePosition);
    }

    public void setMouseDragRange(Range r) {
        setMouseDragRange(r.getFrom(),r.getTo());
    }

    public void setMouseDragRange(long fromPosition, long toPosition) {
        this.mouseClickPosition = fromPosition;
        this.mouseReleasePosition = toPosition;
        askForRefresh();
    }

    public void setMouseClickPosition(long position) {
        this.mouseClickPosition = position;
        askForRefresh();
    }

    public void setMouseReleasePosition(long position) {
        this.mouseReleasePosition = position;
        askForRefresh();
    }
    
    public long getMouseXPosition() {
        return this.mouseXPosition;
    }

    public void setMouseXPosition(long position) {
        this.mouseXPosition = position;
        askForRefresh();
        Savant.getInstance().updateMousePosition();
    }

    public long getMouseYPosition() {
        return this.mouseYPosition;
    }

    public void setMouseYPosition(long position) {
        this.mouseYPosition = position;
        askForRefresh();
        Savant.getInstance().updateMousePosition();
    }
    
    public void setSpotlightSize(long size) {
        this.spotlightSize = Math.max(2,(int) Math.round(size*spotlightproportion));
        this.spotlightSize = 1;
    }

    public long getSpotlightSize() {
        return this.spotlightSize;
    }
}
