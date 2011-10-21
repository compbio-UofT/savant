/*
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.event.GraphPaneEvent;
import savant.selection.PopupPanel;
import savant.util.Controller;
import savant.util.Range;
import savant.view.swing.GraphPane;


/**
 *
 * @author mfiume
 */
public class GraphPaneController extends Controller {
    private static final Log LOG = LogFactory.getLog(GraphPaneController.class);

    private boolean isSpotlight;
    private boolean isPlumbing;
    private boolean isZooming;
    private boolean isAiming;
    private boolean isPanning;
    private boolean isSelecting;

    private int mouseClickPosition;
    private int mouseReleasePosition;
    private int mouseXPosition;
    private double mouseYPosition;
    private boolean yIntegral;

    private List<GraphPane> graphpanesQueuedForRendering;

    private int spotlightSize;
    private double spotlightproportion = 0.25;

    private static GraphPaneController instance;

    private boolean changeMade = false;

    /** Panel (if any) which is currently popped up. */
    private PopupPanel poppedUp = null;

    public void clearRenderingList() {
        graphpanesQueuedForRendering.clear();
    }

    // Get current time
    long start;

    public void enlistRenderingGraphpane(GraphPane p) {
        //System.out.println("Enlisting gp " + p.getTrackRenderers().get(0).toString());
        graphpanesQueuedForRendering.add(p);
        if (graphpanesQueuedForRendering.size() == 1) {
            fireEvent(new GraphPaneEvent("rendering..."));
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
            float elapsedTimeSec = elapsedTimeMillis * 0.001F;
            fireEvent(new GraphPaneEvent(String.format("took %.3f s", elapsedTimeSec)));
        }
    }

    private GraphPaneController() {
        graphpanesQueuedForRendering = new ArrayList<GraphPane>();
    }

    public static synchronized GraphPaneController getInstance() {
        if (instance == null) {
            instance = new GraphPaneController();
        }
        return instance;
    }

    public boolean isChanged(){
        return changeMade;
    }


    public void askForRefresh() {
        if (isPanning() || isZooming() || isPlumbing() || isSpotlight() || isAiming()) {
            fireEvent(new GraphPaneEvent());
        }
    }

    public void forceRefresh() {
        fireEvent(new GraphPaneEvent());
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
        if (isPlumbing) {
            this.isSpotlight = false;
            this.isAiming = false;
        }

        this.isPlumbing = isPlumbing;
        changeMade = true;
        forceRefresh();
        changeMade = false;
    }

    public boolean isSpotlight() {
        return this.isSpotlight;
    }

    public void setSpotlight(boolean isSpotlight) {
        if (isSpotlight) {
            this.isPlumbing = false;
            this.isAiming = false;
        }
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
        if (isAiming) {
            this.isPlumbing = false;
            this.isSpotlight = false;
        }
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
        return mouseXPosition;
    }

    public void setMouseXPosition(int x) {
        mouseXPosition = x;
        askForRefresh();
        fireEvent(new GraphPaneEvent(mouseXPosition, mouseYPosition, yIntegral));
    }

    public double getMouseYPosition() {
        return mouseYPosition;
    }

    public void setMouseYPosition(double y, boolean integral) {
        mouseYPosition = y;
        yIntegral = integral;
        askForRefresh();
        fireEvent(new GraphPaneEvent(mouseXPosition, mouseYPosition, integral));
    }
    
    public void setSpotlightSize(int size) {
        this.spotlightSize = Math.max(2,(int) Math.round(size*spotlightproportion));
        this.spotlightSize = 1;
    }

    public int getSpotlightSize() {
        return this.spotlightSize;
    }

    /**
     * Recognise when a new popup has opened so that we can close the previous one.
     */
    public void setPopup(PopupPanel pp) {
        if (poppedUp != null) {
            LOG.info("Hiding popup for " + poppedUp.getRecord());
            poppedUp.hidePopup();
        }
        poppedUp = pp;
    }
}
