/*
 *    Copyright 2010-2012 University of Toronto
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

import savant.api.adapter.GraphPaneAdapter;
import savant.controller.event.GraphPaneEvent;
import savant.util.Controller;
import savant.util.Range;


/**
 *
 * @author mfiume
 */
public class GraphPaneController extends Controller {

    private boolean spotlit;
    private boolean plumbing;
    private boolean zooming;
    private boolean aiming;
    private boolean panning;
    private boolean selecting;

    private int mouseClickPosition;
    private int mouseReleasePosition;
    private int mouseXPosition;
    private double mouseYPosition;
    private boolean yIntegral;

    private List<GraphPaneAdapter> graphpanesQueuedForRendering;

    private int spotlightSize;

    private static GraphPaneController instance;

    public void clearRenderingList() {
        graphpanesQueuedForRendering.clear();
    }

    // Get current time
    long start;

    public void enlistRenderingGraphpane(GraphPaneAdapter p) {
        //System.out.println("Enlisting gp " + p.getTrackRenderers().get(0).toString());
        graphpanesQueuedForRendering.add(p);
        if (graphpanesQueuedForRendering.size() == 1) {
            fireEvent(new GraphPaneEvent("rendering..."));
            start = System.currentTimeMillis();
        }
    }

    public void delistRenderingGraphpane(GraphPaneAdapter p) {

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
        graphpanesQueuedForRendering = new ArrayList<GraphPaneAdapter>();
    }

    public static synchronized GraphPaneController getInstance() {
        if (instance == null) {
            instance = new GraphPaneController();
        }
        return instance;
    }

    public void askForRefresh() {
        if (isPanning() || isZooming() || isPlumbing() || isSpotlight() || isAiming() || isSelecting()) {
            fireEvent(new GraphPaneEvent());
        }
    }

    public void forceRefresh() {
        fireEvent(new GraphPaneEvent());
    }

    public boolean isSelecting() {
        return selecting;
    }

    public void setSelecting(boolean value) {
        selecting = value;
    }

    public boolean isPlumbing() {
        return plumbing;
    }

    public void setPlumbing(boolean value) {
        if (plumbing != value) {
            plumbing = value;
            if (value) {
                spotlit = false;
                aiming = false;
            }
            forceRefresh();
        }
    }

    public boolean isSpotlight() {
        return spotlit;
    }

    public void setSpotlight(boolean value) {
        if (spotlit != value) {
            spotlit = value;
            if (value) {
                plumbing = false;
                aiming = false;
            }
            forceRefresh();
        }
    }

    public boolean isZooming() {
        return zooming;
    }

    public void setZooming(boolean value) {
        if (zooming != value) {
            zooming = value;
            forceRefresh();
        }
    }

    public boolean isAiming() {
        return aiming;
    }

    public void setAiming(boolean value) {
        if (aiming != value) {
            aiming = value;
            if (value) {
                plumbing = false;
                spotlit = false;
            }
            forceRefresh();
        }
    }

    public boolean isPanning() {
        return panning;
    }

    public void setPanning(boolean value) {
        if (panning != value) {
            panning = value;
            forceRefresh();
        }
    }

    public Range getMouseDragRange() {
        return new Range(mouseClickPosition, mouseReleasePosition);
    }

    public void setMouseDragRange(Range r) {
        setMouseDragRange(r.getFrom(),r.getTo());
    }

    public void setMouseDragRange(int fromPosition, int toPosition) {
        mouseClickPosition = fromPosition;
        mouseReleasePosition = toPosition;
        askForRefresh();
    }

    public void setMouseClickPosition(int position) {
        mouseClickPosition = position;
        askForRefresh();
    }

    public void setMouseReleasePosition(int position) {
        mouseReleasePosition = position;
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
        spotlightSize = 1;
    }

    public int getSpotlightSize() {
        return spotlightSize;
    }
}
