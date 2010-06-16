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
 * VizPane.java
 * Created on Jun 15, 2010
 */

package savant.refactor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.GraphPaneController;
import savant.controller.RangeController;
import savant.util.MiscUtils;
import savant.util.Range;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

/**
 * Swing component to hold a {@Track}.
 *
 * @see Track
 * @author vwilliams
 */
public class VizPane extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener {

    private static Log log = LogFactory.getLog(VizPane.class);
    
    private SwingTrack track;

    // TODO_REFACTOR: this should go and be replaced by Andrew's toolbars, i.e. JMenuBar
    private JPopupMenu menu;

    // mouse and key modifiers
    private enum mouseModifier { NONE, LEFT, MIDDLE, RIGHT };
    private mouseModifier mouseMod = mouseModifier.LEFT;

    private enum keyModifier { DEFAULT, CTRL, SHIFT, META, ALT; };
    private keyModifier keyMod = keyModifier.DEFAULT;

    // let's behave nicely for the appropriate platform
    private static String os = System.getProperty("os.name").toLowerCase();
    private static boolean mac = os.contains("mac");
    
    public VizPane(SwingTrack track) {
        this.track = track;
        this.menu = track.getMenu();
    }

    public SwingTrack getTrack() {
        return track;
    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent mouseEvent) {

        if (mouseEvent.getClickCount() == 2) {
            RangeController.getInstance().zoomInOnMouse();
            return;
        }

        setMouseModifier(mouseEvent);

        if (mac && mouseEvent.isControlDown() || isRightClick()) {
            menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {

        int notches = mouseWheelEvent.getWheelRotation();

        if (mac && mouseWheelEvent.isMetaDown() || mouseWheelEvent.isControlDown()) {
            if (notches < 0) {
                RangeController rc = RangeController.getInstance();
                rc.shiftRangeLeft();
            } else {
                RangeController rc = RangeController.getInstance();
                rc.shiftRangeRight();
            }
        }
        else {
             if (notches < 0) {
                RangeController rc = RangeController.getInstance();
                rc.zoomInOnMouse();
            } else {
                RangeController rc = RangeController.getInstance();
                rc.zoomOutFromMouse();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(MouseEvent mouseEvent) {

        setMouseModifier(mouseEvent);

        // TODO_REFACTOR: I believe gpc functionality belongs in Track controller.  Find all instances of GraphPaneController and deal with them
        GraphPaneController gpc = GraphPaneController.getInstance();

        int x2 = mouseEvent.getX();
        if (x2 < 0) { x2 = 0; }
        if (x2 > this.getWidth()) { x2 = this.getWidth(); }

        SwingTrack swingTrack = getTrack();
        swingTrack.setDragging(true);

        if (gpc.isPanning()) {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else if (gpc.isZooming() || gpc.isSelecting()) {
            this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }

        gpc.setMouseReleasePosition(MiscUtils.transformPixelToPosition(x2, this.getWidth(), swingTrack.getGrapher().getRange()));
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent mouseEvent) {

        setMouseModifier(mouseEvent);

        this.requestFocus();

        int x1 = mouseEvent.getX();
        if (x1 < 0) { x1 = 0; }
        if (x1 > this.getWidth()) { x1 = this.getWidth(); }

        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setMouseClickPosition(MiscUtils.transformPixelToPosition(x1, this.getWidth(), getTrack().getGrapher().getRange()));
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(MouseEvent mouseEvent) {
        // update the GraphPaneController's record of the mouse position
        Grapher grapher = getTrack().getGrapher();
        GraphPaneController.getInstance().setMouseXPosition(MiscUtils.transformPixelToPosition(mouseEvent.getX(), this.getWidth(), grapher.getRange()));
        if (grapher.isOrdinal()) {
            GraphPaneController.getInstance().setMouseYPosition(-1);
        } else {
            GraphPaneController.getInstance().setMouseYPosition(MiscUtils.transformPixelToPosition(this.getHeight() - mouseEvent.getY(), this.getHeight(), grapher.getYRange()));
        }
        GraphPaneController.getInstance().setSpotlightSize(grapher.getRange().getLength());
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent mouseEvent) {
        GraphPaneController gpc = GraphPaneController.getInstance();

        int x2 = mouseEvent.getX();
        if (x2 < 0) { x2 = 0; }
        if (x2 > this.getWidth()) { x2 = this.getWidth(); }

        //if (gpc.isSelecting()) {
         //   this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        //} else {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        //}

        SwingTrack swingTrack = getTrack();
        Grapher grapher = swingTrack.getGrapher();
        int x1 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getFrom(), this.getWidth(), grapher.getRange());

        if (gpc.isPanning()) {

            RangeController rc = RangeController.getInstance();
            Range r = rc.getRange();
            int shiftVal = (int) (Math.round((x1-x2) / grapher.getUnitWidth()));

            Range newr = new Range(r.getFrom()+shiftVal,r.getTo()+shiftVal);
            rc.setRange(newr);

        } else if (gpc.isZooming()) {

            RangeController rc = RangeController.getInstance();
            Range r;
            if (swingTrack.isLocked()) {
                r = swingTrack.getLockedRange();
            } else {
                r = rc.getRange();
            }
            int newMin = (int) Math.round(Math.min(x1, x2) / grapher.getUnitWidth());
            // some weirdness here, but it's to get around an off by one
            int newMax = (int) Math.max(Math.round(Math.max(x1, x2) / grapher.getUnitWidth())-1, newMin);
            Range newr = new Range(r.getFrom()+newMin,r.getFrom()+newMax);

            rc.setRange(newr);
        }
        // TODO_REFACTOR: deal with selection properly
//         else if (gpc.isSelecting()) {
//            selectElementsInRectangle(new Rectangle2D.Double(this.x, this.y, this.w, this.h));
//        }

        swingTrack.setDragging(false);
        setMouseModifier(mouseEvent);

        gpc.setMouseReleasePosition(MiscUtils.transformPixelToPosition(x2, this.getWidth(), grapher.getRange()));
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent mouseEvent) {
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMouseModifier(mouseEvent);
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(MouseEvent mouseEvent) {
        setMouseModifier(mouseEvent);
    }

    /** Mouse modifiers */
    private boolean isRightClick() {
        return mouseMod == mouseModifier.RIGHT;
    }

    private boolean isLeftClick() {
        return mouseMod == mouseModifier.LEFT;
    }

    private boolean isMiddleClick() {
        return mouseMod == mouseModifier.MIDDLE;
    }

    // Key modifiers
    private boolean isNoKeyModifierPressed() {
        return keyMod == keyModifier.DEFAULT;
    }

    private boolean isShiftKeyModifierPressed() {
        return keyMod == keyModifier.SHIFT;
    }

    private boolean isCtrlKeyModifierPressed() {
        return keyMod == keyModifier.CTRL;
    }

    private boolean isMetaModifierPressed() {
        return keyMod == keyModifier.META;
    }

    private boolean isAltModifierPressed() {
        return keyMod == keyModifier.ALT;
    }

    private boolean isZoomModifierPressed() {
        if ((mac && isMetaModifierPressed()) || (!mac && isCtrlKeyModifierPressed())) return true;
        else return false;
    }

    private boolean isSelectModifierPressed() {
        if (isShiftKeyModifierPressed()) return true;
        else return false;
    }

    private void setMouseModifier(MouseEvent e) {

        if (e.getButton() == 1) mouseMod= mouseModifier.LEFT;
        else if (e.getButton() == 2) mouseMod = mouseModifier.MIDDLE;
        else if (e.getButton() == 3) mouseMod = mouseModifier.RIGHT;

        if (e.isControlDown()) {
            keyMod = keyModifier.CTRL;
        }
        else if (e.isShiftDown()) {
            keyMod = keyModifier.SHIFT;
        }
        else if (e.isMetaDown()) {
            keyMod = keyModifier.META;
        }
        else if (e.isAltDown()) {
            keyMod = keyModifier.ALT;
        }
        else {
            keyMod = keyModifier.DEFAULT;
        }

        tellModifiersToGraphPaneController();
    }

    private void tellModifiersToGraphPaneController() {
        GraphPaneController gpc = GraphPaneController.getInstance();
        setZooming(gpc);
        setPanning(gpc);
        setSelecting(gpc);
    }

    private void setZooming(GraphPaneController gpc) {
        gpc.setZooming(getTrack().isDragging() &&  ((isLeftClick() && isZoomModifierPressed()) || (isRightClick() && isZoomModifierPressed())));
    }

    private void setSelecting(GraphPaneController gpc) {
        gpc.setSelecting(getTrack().isDragging() &&  ((isLeftClick() && isSelectModifierPressed()) || (isRightClick() && isSelectModifierPressed())));
    }

    private void setPanning(GraphPaneController gpc) {
        gpc.setPanning(getTrack().isDragging() && isNoKeyModifierPressed());
    }

}
