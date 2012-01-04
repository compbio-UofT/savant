/*
 *    Copyright 2011-2012 University of Toronto
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
package savant.view.swing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import javax.swing.JScrollBar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.controller.GraphPaneController;
import savant.util.swing.TallScrollingPanel;
import savant.view.tracks.VariantTrack;


/**
 * GraphPane which is used to render the contents of a Variant track.  Unlike other GraphPanes,
 * it is laid out vertically, has scrollbars, and has different mouse behaviour.
 *
 * @author tarkvara
 */
public class VariantGraphPane extends GraphPane {
    private static final Log LOG = LogFactory.getLog(VariantGraphPane.class);

    public VariantGraphPane(Frame f) {
        super(f);
        scaledToFit = true;
        unitHeight = 1.0;
        forcedHeight = true;
    }

    /**
     * VariantGraphPanes don't have the same mouse behaviour as regular GraphPanes.
     */
    @Override
    public void mouseClicked(MouseEvent event) {
    }

    /**
     * VariantGraphPanes don't have the same mouse behaviour as regular GraphPanes.
     */
    @Override
    public void mousePressed(MouseEvent event) {
    }

    /**
     * VariantGraphPanes don't have the same mouse behaviour as regular GraphPanes.
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    /**
     * VariantGraphPanes don't have the same mouse behaviour as regular GraphPanes.
     */
    @Override
    public void mouseDragged(MouseEvent event) {
    }

    /**
     * Override mouseMoved because the x-position we report is actually derived from
     * our y-position.
     */
    @Override
    public void mouseMoved(MouseEvent event) {
        int mouseY = event.getY();
        int logicalY = (int)transformYPixel(mouseY);
        List<Record> data = getTracks()[0].getDataInRange();
        if (data != null && logicalY >= 0 && logicalY < data.size()) {
            VariantRecord rec = (VariantRecord)data.get(data.size() - logicalY);
            GraphPaneController.getInstance().setMouseXPosition(rec.getInterval().getStart());
        } else {
            GraphPaneController.getInstance().setMouseXPosition(-1);
        }
    }

    /**
     * Access to the scrollbar associated with this VariantGraphPane.  For the variant GraphPane,
     * the scroll-bar belongs to our parent.
     */
    @Override
    public JScrollBar getVerticalScrollBar() {
        return ((TallScrollingPanel)getParent()).getScrollBar();
    }

    /**
     * Determine whether the display is within the current scroll bounds, or whether it
     * needs to be re-generated.  Overridden by VariantGraphPane, which has a different
     * notion of scroll bounds.
     */
    @Override
    protected boolean isWithinScrollBounds() {
        return false;
    }

    /**
     * Suppress the default behaviour, which is to update the scroll-bar's position based
     * on the height of the canvas.
     */
    @Override
    protected void updateScrollForHeight() {
    }

    
    /**
     * Update the display of ymax.  Overridden by VariantGraphPanes to show the current range.
     */
    @Override
    protected void updateYMax() {
        VariantTrack t = (VariantTrack)getTracks()[0];
        ((Frame)getParentFrame()).updateYMax("%s:%s", t.getReference(), t.getVisibleRange());
    }

    /**
     * Override unforceFullHeight because we always want a full-height rendering.
     */
    @Override
    public void unforceFullHeight(){
    }
    
    
}
