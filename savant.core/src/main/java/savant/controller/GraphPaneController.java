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

import java.util.ArrayList;
import java.util.List;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.util.NavigationUtils;
import savant.controller.event.GraphPaneEvent;
import savant.util.Controller;


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
            
            // For the benefit of plugins, fire a LocationChangeCompletedEvent.
            NavigationUtils.fireLocationChangeCompletedEvent();
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

    public int getMouseClickPosition() {
        return mouseClickPosition;
    }

    public void setMouseClickPosition(int position) {
        mouseClickPosition = position;
        askForRefresh();
    }

    public int getMouseReleasePosition() {
        return mouseReleasePosition;
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
