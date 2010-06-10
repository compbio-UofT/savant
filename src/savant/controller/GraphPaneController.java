/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.controller;

import java.util.ArrayList;
import java.util.List;
import savant.controller.event.graphpane.GraphPaneChangeEvent;
import savant.controller.event.graphpane.GraphPaneChangeListener;
import savant.util.Range;
import savant.view.swing.Savant;

/**
 *
 * @author mfiume
 */
public class GraphPaneController {

    private boolean isSpotlight;
    private boolean isPlumbing;
    private boolean isZooming;
    private boolean isPanning;
    private boolean isSelecting;

    private int mouseClickPosition;
    private int mouseReleasePosition;
    private int mouseXPosition;
    private int mouseYPosition;

    private int spotlightSize;
    private double spotlightproportion = 0.25;

    private static GraphPaneController instance;

    private List<GraphPaneChangeListener> graphpaneChangeListeners;

    private GraphPaneController() {
        graphpaneChangeListeners = new ArrayList<GraphPaneChangeListener>();
    }

    public static synchronized GraphPaneController getInstance() {
        if (instance == null) {
            instance = new GraphPaneController();
        }
        return instance;
    }

    public synchronized void addFavoritesChangedListener(GraphPaneChangeListener l) {
        graphpaneChangeListeners.add(l);
    }

    public synchronized void removeFavoritesChangedListener(GraphPaneChangeListener l) {
        graphpaneChangeListeners.remove(l);
    }

    public void askForRefresh() {
        if (this.isPanning() || this.isZooming() || this.isPlumbing() || this.isSpotlight()) {
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
        forceRefresh();
    }

    public boolean isSpotlight() {
        return this.isSpotlight;
    }

    public void setSpotlight(boolean isSpotlight) {
        this.isSpotlight = isSpotlight;
        forceRefresh();
    }

    public boolean isZooming() {
        return this.isZooming;
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

    public void setMouseDragRange(int fromPosition, int toPosition) {
        this.mouseClickPosition = fromPosition;
        this.mouseReleasePosition = toPosition;
        askForRefresh();
    }

    public void setMouseClickPosition(int position) {
        this.mouseClickPosition = position;
        askForRefresh();
    }

    public void setMouseReleasePosition(int position) {
        this.mouseReleasePosition = position;
        askForRefresh();
    }
    
    public int getMouseXPosition() {
        return this.mouseXPosition;
    }

    public void setMouseXPosition(int position) {
        this.mouseXPosition = position;
        askForRefresh();
        Savant.getInstance().updateMousePosition();
    }

    public int getMouseYPosition() {
        return this.mouseYPosition;
    }

    public void setMouseYPosition(int position) {
        this.mouseYPosition = position;
        askForRefresh();
        Savant.getInstance().updateMousePosition();
    }
    
    public void setSpotlightSize(int size) {
        this.spotlightSize = Math.max(2,(int) Math.round(size*spotlightproportion));
    }

    public int getSpotlightSize() {
        return this.spotlightSize;
    }


}
