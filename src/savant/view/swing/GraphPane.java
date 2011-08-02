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

package savant.view.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import com.jidesoft.popup.JidePopup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.GraphPaneController;
import savant.controller.Listener;
import savant.controller.LocationController;
import savant.controller.event.GraphPaneEvent;
import savant.data.event.ExportEvent;
import savant.data.event.ExportEventListener;
import savant.data.event.PopupEvent;
import savant.data.event.PopupEventListener;
import savant.data.types.GenericContinuousRecord;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.selection.PopupThread;
import savant.selection.PopupPanel;
import savant.settings.ColourSettings;
import savant.swing.component.ProgressPanel;
import savant.util.*;
import savant.view.dialog.BAMParametersDialog;
import savant.view.swing.continuous.ContinuousTrackRenderer;
import savant.view.swing.interval.BAMTrack;

/**
 *
 * @author mfiume
 */
public class GraphPane extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener {

    private static final Log LOG = LogFactory.getLog(GraphPane.class);

    private Track[] tracks;
    private Frame parentFrame;

    private int mouse_x = 0;
    private int mouse_y = 0;

    /** min / max axis values */
    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;
    private double unitWidth;
    private double unitHeight;

    private AxisType yAxisType = AxisType.NONE;
    private boolean yGridOn = true;
    private boolean isXGridOn = true;
    private boolean mouseInside = false;

    // Locking
    private Range lockedRange;

    private boolean scaledToContents = false;

    /** Selection Variables */
    private Rectangle2D selectionRect = new Rectangle2D.Double();
    private boolean isDragging = false;

    //scrolling...
    private BufferedImage bufferedImage;
    private Range prevRange = null;
    private DrawingMode prevMode = null;
    private Dimension prevSize = null;
    private String prevRef = null;
    public boolean paneResize = false;
    public int newHeight;
    private int oldWidth = -1;
    private int oldHeight = -1;
    private int oldViewHeight = -1;
    private int newScroll = 0;
    private boolean renderRequired = false;
    //private int buffTop = -1;
    //private int buffBottom = -1;
    private int posOffset = 0;
    private boolean forcedHeight = false;

    //dragging
    private int startX;
    private int startY;
    private int baseX;
    private int initialScroll;
    private boolean panVert = false;

    //popup
    public Thread popupThread;
    public JidePopup jp = new JidePopup();
    private Record currentOverRecord = null;
    private Shape currentOverShape = null;
    private boolean popupVisible = false;
    private JPanel popPanel;
    //private boolean mouseWheel = false; //used by popupThread

    /**
     * Provides progress indication when loading a track.
     */
    private ProgressPanel progressPanel;

    // mouse and key modifiers
    private enum mouseModifier { NONE, LEFT, MIDDLE, RIGHT };
    private mouseModifier mouseMod = mouseModifier.LEFT;

    private enum keyModifier { DEFAULT, CTRL, SHIFT, META, ALT; };
    private keyModifier keyMod = keyModifier.DEFAULT;

    //awaiting exported images
    private final List<ExportEventListener> exportListeners = new ArrayList<ExportEventListener>();
    private final List<PopupEventListener> popupListeners = new ArrayList<PopupEventListener>();

    /**
     * CONSTRUCTOR
     */
    public GraphPane(Frame parent) {
        this.parentFrame = parent;
        addMouseListener(this); // listens for own mouse and
        addMouseMotionListener( this ); // mouse-motion events
        //addKeyListener( this );
        getInputMap().allKeys();
        addMouseWheelListener(this);

        popupThread = new Thread(new PopupThread(this));
        popupThread.start();

        GraphPaneController controller = GraphPaneController.getInstance();
        controller.addListener(new Listener<GraphPaneEvent>() {
            @Override
            public void handleEvent(GraphPaneEvent event) {
                parentFrame.resetLayers();
            }
        });

        // GraphPaneController listens to popup events to make sure that only one
        // popup is open at a time.
        addPopupEventListener(controller);
    }

    /**
     * Set the tracks to be displayed in this GraphPane
     *
     * @param tracks an array of Track objects to be added
     */
    public void setTracks(Track[] tracks) {
        this.tracks = tracks;
        setYAxisType(AxisType.NONE);   // We don't get a y-axis until the renderer kicks in.
    }

    /**
     * Return the list of tracks associated with this GraphPane.
     */
    public Track[] getTracks() {
        return tracks;
    }

    /**
     * Render the contents of the GraphPane. Includes drawing a common
     * background for all tracks.
     *
     * @param g2 the Graphics object into which to draw.
     */
    public void render(Graphics2D g2) {
        render(g2, new Range(xMin, xMax), null);
    }

    public void render(Graphics2D g2, Range xRange, Range yRange) {
        LOG.debug("GraphPane.render(g2, " + xRange + ", " + yRange + ")");
        double oldUnitHeight = unitHeight;
        int oldYMax = yMax;


        // Paint a gradient from top to bottom
        GradientPaint gp0 = new GradientPaint(0, 0, ColourSettings.getGraphPaneBackgroundTop(), 0, getHeight(), ColourSettings.getGraphPaneBackgroundBottom());
        g2.setPaint( gp0 );
        g2.fillRect( 0, 0, getWidth(), getHeight() );

        GraphPaneController gpc = GraphPaneController.getInstance();

        if (gpc.isPanning() && !isLocked()) {

            double fromX = transformXPos(gpc.getMouseDragRange().getFrom());
            double toX = transformXPos(gpc.getMouseDragRange().getTo());

            g2.translate(toX - fromX, 0);
        }

        // Deal with the progress-bar.
        if (tracks == null) {
            showProgress("Creating track...");
            return;
        } else {
            for (Track t: tracks) {
                if (t.getRenderer().isWaitingForData()) {
                    String progressMsg = (String)t.getRenderer().getInstruction(DrawingInstruction.PROGRESS);
                    setPreferredSize(new Dimension(getWidth(), 0));
                    showProgress(progressMsg);
                    return;
                }
            }
        }
        if (progressPanel != null) {
            remove(progressPanel);
            progressPanel = null;
        }

        int minYRange = Integer.MAX_VALUE;
        int maxYRange = Integer.MIN_VALUE;
        yGridOn = false;
        for (Track t: tracks) {

            // ask renderers for extra info on range; consolidate to maximum Y range
            AxisRange axisRange = (AxisRange)t.getRenderer().getInstruction(DrawingInstruction.AXIS_RANGE);

            if (axisRange != null) {
                int axisYMin = axisRange.getYMin();
                int axisYMax = axisRange.getYMax();
                if (axisYMin < minYRange) minYRange = axisYMin;
                if (axisYMax > maxYRange) maxYRange = axisYMax;
            }

            // Ask renderers if they want horizontal lines; if any say yes, draw them.
            yGridOn |= t.getRenderer().hasHorizontalGrid();
        }

        setXRange(xRange);
        Range consolidatedYRange = new Range(minYRange, maxYRange);
        setYRange(consolidatedYRange);

        yMin = minYRange;
        yMax = maxYRange;

        DrawingMode currentMode = tracks[0].getDrawingMode();

        boolean sameRange = (prevRange != null && xRange.equals(prevRange));
        if (!sameRange) {
            hidePopup();
        }
        boolean sameMode = currentMode == prevMode;
        boolean sameSize = prevSize != null && getSize().equals(prevSize) && parentFrame.getFrameLandscape().getWidth() == oldWidth && getParentFrame().getFrameLandscape().getHeight() == oldHeight;
        boolean sameRef = prevRef != null && LocationController.getInstance().getReferenceName().equals(prevRef);
        boolean withinScrollBounds = bufferedImage != null && getVerticalScrollBar().getValue() >= getOffset() && getVerticalScrollBar().getValue() < getOffset() + getViewportHeight() * 2;

        //bufferedImage stores the current graphic for future use. If nothing
        //has changed in the track since the last render, bufferedImage will
        //be used to redraw the current view. This method allows for fast repaints
        //on tracks where nothing has changed (panning, selection, plumbline,...)

        //if nothing has changed draw buffered image
        if (sameRange && sameMode && sameSize && sameRef && !renderRequired && withinScrollBounds){
            g2.drawImage(bufferedImage, 0, getOffset(), this);
            renderCurrentSelected(g2);

            //force unitHeight from last render
            unitHeight = oldUnitHeight;
            yMax = oldYMax;

        } else {
            // Otherwise prepare for new render.
            renderRequired = false;

            int h = getHeight();
            if (!forcedHeight) {
                h = Math.min(h, getViewportHeight() * 3);
            }
            bufferedImage = new BufferedImage(getWidth(), h, BufferedImage.TYPE_INT_RGB);
            if (bufferedImage.getHeight() == getHeight()){
                setOffset(0);
            } else {
                setOffset(getVerticalScrollBar().getValue() - getViewportHeight());
            }
            LOG.trace("Rendering fresh " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight() + " bufferedImage at (0, " + getOffset() + ")");

            Graphics2D g3 = bufferedImage.createGraphics();
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            prevRange = LocationController.getInstance().getRange();
            prevSize = this.getSize();
            prevMode = tracks[0].getDrawingMode();
            prevRef = LocationController.getInstance().getReferenceName();

            renderBackground(g3);            

            // Call the actual render() methods.
            boolean nothingRendered = true;
            String message = null;
            String subMessage = null;
            for (Track t: tracks) {
                // Change renderers' drawing instructions to reflect consolidated YRange
                t.getRenderer().addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(xRange, consolidatedYRange));
                try {
                    t.getRenderer().render(g3, this);
                    nothingRendered = false;
                } catch (RenderingException rx) {
                    if (message == null) {
                        message = rx.getMessage();
                        //TODO: this shouldn't really be done here...
                        if (message.equals("Zoom in to see data")){
                            if (MiscUtils.MAC) {
                                subMessage = "To view data at this range, change Preferences > Track Resolutions";
                            } else {
                                subMessage = "To view data at this range, change Edit > Preferences > Track Resolutions";
                            }
                        }
                    }
                }
            }
            if (nothingRendered && message != null) {
                setPreferredSize(new Dimension(getWidth(), 0));
                revalidate();
                drawMessage(g3, message, subMessage);
            }

            parentFrame.updateYMax(yMax);
            renderSides(g3);

            //if a change has occured that affects scrollbar...
            if (paneResize) {
                paneResize = false;

                //get old scroll position
                int oldScroll = getVerticalScrollBar().getValue();
                int oldBottomHeight = oldHeight - oldScroll - oldViewHeight;
                int newViewHeight = getViewportHeight();

                // Change size of current frame
                parentFrame.getFrameLandscape().setPreferredSize(new Dimension(this.getWidth(), newHeight));
                setPreferredSize(new Dimension(parentFrame.getFrameLandscape().getWidth(), newHeight));
                parentFrame.getFrameLandscape().setSize(new Dimension(parentFrame.getFrameLandscape().getWidth(), newHeight));
                setSize(new Dimension(parentFrame.getFrameLandscape().getWidth(), newHeight));
                revalidate();

                //scroll so that bottom matches previous view
                newScroll = newHeight - newViewHeight - oldBottomHeight;
                oldViewHeight = newViewHeight;

                return; // new Dimension(frame.getFrameLandscape().getWidth(), newHeight);

            } else if (oldViewHeight != getViewportHeight()) {
                int newViewHeight = getViewportHeight();
                int oldScroll = getVerticalScrollBar().getValue();
                newScroll = oldScroll + (oldViewHeight - newViewHeight);
                oldViewHeight = newViewHeight;
            }

            if (newScroll != -1){
                getVerticalScrollBar().setValue(newScroll);
                newScroll = -1;
            }

            oldWidth = parentFrame.getFrameLandscape().getWidth();
            oldHeight = parentFrame.getFrameLandscape().getHeight();

            g2.drawImage(bufferedImage, 0, getOffset(), this);
            fireExportReady(xRange, bufferedImage);

            renderCurrentSelected(g2);
            parentFrame.redrawSidePanel();
        }

        return;

    }

    /**
     * Get the height of the viewport.  The viewport is the grandparent of this GraphPane.
     */
    private int getViewportHeight() {
        return getParent().getParent().getHeight();
    }

    /**
     * Access to the scrollbar associated with this GraphPane.  The JScrollPane is our great-grandparent.
     */
    private JScrollBar getVerticalScrollBar() {
        return ((JScrollPane)getParent().getParent().getParent()).getVerticalScrollBar();
    }

    private void renderCurrentSelected(Graphics2D g2){
        // Temporarily shift the origin
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(0, getOffset());
        for (Track t: tracks) {
            if (t.getRenderer().hasMappedValues()) {
                List<Shape> currentSelected = t.getRenderer().getCurrentSelectedShapes(this);
                boolean arcMode = t.getDrawingMode() == DrawingMode.ARC_PAIRED;
                for (Shape selectedShape: currentSelected) {
                    if (selectedShape != currentOverShape) {
                        if (arcMode) {
                            g2.setColor(Color.GREEN);
                            g2.draw(selectedShape);
                        } else {
                            //g2.setColor(Color.GREEN);
                            g2.setColor(new Color(0,255,0,150));
                            g2.fill(selectedShape);
                            if (selectedShape.getBounds().getWidth() > 5){
                                g2.setColor(Color.BLACK);
                                g2.draw(selectedShape);
                            }
                        }
                    }
                }
                break;
            }
        }
        if (currentOverShape != null) {
            if (tracks[0].getDrawingMode() == DrawingMode.ARC_PAIRED) {
                g2.setColor(Color.RED);
                g2.draw(currentOverShape);
            } else {
                g2.setColor(new Color(255,0,0,150));
                g2.fill(currentOverShape);
                if (currentOverShape.getBounds() != null && currentOverShape.getBounds().getWidth() > 5 && currentOverShape.getBounds().getHeight() > 3) {
                    g2.setColor(Color.BLACK);
                    g2.draw(currentOverShape);
                }
            }
        }
        //shift the origin back
        g2.translate(0, -getOffset());
    }

    public void setRenderForced(){
        renderRequired = true;
    }

    public boolean isRenderForced() {
        return renderRequired;
    }

    /**
     * Force the bufferedImage to contain entire height at current range.  Intended
     * for creating images for track export.  Make sure you unforce immediately after!
     */
    public void forceFullHeight(){
        forcedHeight = true;
    }

    public void unforceFullHeight(){
        forcedHeight = false;
    }

    private void setOffset(int offset){
        posOffset = offset;
    }

    public int getOffset(){
        return posOffset;
    }

    public void setPaneResize(boolean resized){
        paneResize = resized;
    }

    int rendercount = 0;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;
        render(g2);

        GraphPaneController gpc = GraphPaneController.getInstance();
        int h = getHeight();

        /* AIMING ADJUSTMENTS */
        if (gpc.isAiming() && mouseInside) {
            g2.setColor(Color.BLACK);
            Font thickfont = new Font("Arial", Font.BOLD, 15);
            g2.setFont(thickfont);
            int genomeX = gpc.getMouseXPosition();
            double genomeY = gpc.getMouseYPosition();
            String target = "";
            target += "X: " + MiscUtils.numToString(genomeX);
            if (!Double.isNaN(genomeY)) {
                target += " Y: " + MiscUtils.numToString(genomeY);
            }

            g2.drawLine(mouse_x, 0, mouse_x, h);
            if (genomeY != -1) g.drawLine(0, mouse_y, this.getWidth(), mouse_y);
            g2.drawString(target, mouse_x + 5, mouse_y - 5);
        }

        double x1 = transformXPos(gpc.getMouseDragRange().getFrom());
        double x2 = transformXPos(gpc.getMouseDragRange().getTo());

        double width = x1 - x2;

        selectionRect = new Rectangle2D.Double(width < 0 ? x1 : x2, 0.0, Math.max(2.0 ,Math.abs(width)), h);

        /* PANNING ADJUSTMENTS */
        if (gpc.isPanning()) {}

        /* ZOOMING ADJUSTMENTS */
        else if (gpc.isZooming() || gpc.isSelecting()) {

            Rectangle2D rectangle = new Rectangle2D.Double(selectionRect.getX(), selectionRect.getY() - 10.0, selectionRect.getWidth(), selectionRect.getHeight() + 10.0);
            g2.setColor(Color.gray);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 3f, new float[] {4f}, 4f));
            g2.draw(rectangle);

            if (gpc.isZooming()) {
                g.setColor(ColourSettings.getGraphPaneZoomFill());
            } else if (gpc.isSelecting()) {
                g.setColor(ColourSettings.getGraphPaneSelectionFill());
            }
            g2.fill(selectionRect);
        }

        /* PLUMBING ADJUSTMENTS */
        Range xRange = getXRange();
        if (gpc.isPlumbing()) {
            g2.setColor(Color.BLACK);
            double spos = transformXPos(gpc.getMouseXPosition());
            g2.draw(new Line2D.Double(spos, 0, spos, h));
            double rpos = transformXPos(gpc.getMouseXPosition() + 1);
            g2.draw(new Line2D.Double(rpos, 0, rpos, h));
        }

        /* SPOTLIGHT */
        if (gpc.isSpotlight() && !gpc.isZooming()) {

            int center = gpc.getMouseXPosition();
            int left = center - gpc.getSpotlightSize()/2;
            int right = left + gpc.getSpotlightSize();

            g2.setColor(new Color(0,0,0,200));

            // draw left of spotlight
            if (left >= xRange.getFrom()) {
                g2.fill(new Rectangle2D.Double(0.0, 0.0, transformXPos(left), h));
            }
            // draw right of spotlight
            if (right <= xRange.getTo()) {
                double pix = transformXPos(right);
                g2.fill(new Rectangle2D.Double(pix, 0, getWidth() - pix, h));
            }
        }

        if (isLocked()) {
            drawMessage((Graphics2D)g, "Locked", null);
        }

        gpc.delistRenderingGraphpane(this);
    }

    /**
     * Render the sides of this GraphPane
     * @param g The graphics object to use
     */
    private void renderSides(Graphics g) {
        g.setColor(getBackground());
        int w = getWidth();
        int h = getHeight();
        g.fillRect(w, 0, w, h);
        g.fillRect(-w, 0, w, h);
    }

    /**
     * Render the background of this GraphPane
     * @param g The graphics object to use
     */
    public void renderBackground(Graphics2D g2) {
        int h = getHeight();
        int w = getWidth();

        // Paint a gradient from top to bottom
        GradientPaint gp0 = new GradientPaint(0, 0, ColourSettings.getGraphPaneBackgroundTop(), 0, h, ColourSettings.getGraphPaneBackgroundBottom());

        g2.setPaint(gp0);
        g2.fillRect(0, 0, w, h);

        if (isXGridOn) {
            Range r = LocationController.getInstance().getRange();
            int[] ticks = MiscUtils.getTickPositions(r);
            g2.setColor(ColourSettings.getAxisGrid());
            for (int t: ticks) {
                double x = transformXPos(t);
                g2.draw(new Line2D.Double(x, 0, x, h));
            }
        }

        if (yGridOn) {
            // FIXME: This method of determining separators is broken.
            int numseparators = (int) Math.ceil(Math.log(yMax-yMin));

            if (numseparators != 0) {
                int height = this.getHeight();
                double separation = height / numseparators;

                g2.setColor(ColourSettings.getAxisGrid());
                for (int i = 0; i <= numseparators; i++) {
                    g2.drawLine(0, (int)Math.ceil(i*separation)+1, w, (int) Math.ceil(i*separation)+1);
                }
            }
        }
    }

    public Range getXRange() {
        return new Range(xMin, xMax);
    }

    /**
     * Set the range for the horizontal axis, adjusting the width of graph units.
     *
     * @param r an X range
     */
    public void setXRange(Range r) {
        if (r != null) {
            xMin = r.getFrom();
            xMax = r.getTo();
            setUnitWidth();
        }
    }

    public Range getYRange() {
        return new Range(yMin, yMax);
    }

    /**
     * Set the vertical range, adjusting the height of graph units.
     *
     * @param r a Y range
     */
    public void setYRange(Range r) {
        if (r != null && yAxisType != AxisType.NONE) {
            yMin = r.getFrom();
            yMax = r.getTo();
            setUnitHeight();
        }
    }

    /**
     * Set the pane's vertical coordinate system to be 0-1
     *
     * @param type true for ordinal, false otherwise.
     */
    public void setYAxisType(AxisType type) {
        yAxisType = type;
        if (yAxisType == AxisType.NONE) {
            // don't call setYRange, because it's just going to return without doing anything
            yMin = 0;
            yMax = 1;
            setUnitHeight();
        }
    }

    /**
     *
     * @return  the number of pixels equal to one graph unit of width.
     */
    public double getUnitWidth() {
        return unitWidth;
    }

    /**
     * Transform a graph width into a pixel width
     *
     * @param len width in graph units
     * @return corresponding number of pixels
     */
    public double getWidth(int len) {
        return unitWidth * len;
    }

    /**
     *
     * @return the number of pixels equal to one graph unit of height.
     */
    public double getUnitHeight() {
        return unitHeight;
    }

    /**
     * Transform a graph height into a pixel height
     *
     * @param len height in graph units
     * @return corresponding number of pixels
     */
    public double getHeight(double len) {
        return unitHeight * len;
    }

    /**
     * Transform a horizontal position in terms of drawing coordinates into graph units.
     *
     * @param pix drawing position in pixels
     * @return a corresponding logical position
     */
    public int transformXPixel(double pix) {
        return (int)Math.floor(pix / unitWidth + xMin);
    }

    /**
     * Transform a horizontal position in terms of graph units into a drawing coordinate.
     *
     * @param pos position in graph coordinates
     * @return a corresponding drawing coordinate
     */
    public double transformXPos(int pos) {
        return (pos - xMin) * unitWidth;
    }

    /**
     * Transform a vertical position in terms of drawing coordinate into graph units.
     *
     * @param pos position in graph coordinates
     * @return a corresponding drawing coordinate
     */
    public double transformYPixel(double pix) {
        return (getHeight() - pix) / unitHeight + yMin;
    }


    /**
     * Transform a vertical position in terms of graph units into a drawing coordinate
     *
     * @param pos position in graph coordinates
     * @return a corresponding drawing coordinate
     */
    public double transformYPos(double pos) {
        return getHeight() - ((pos - yMin) * unitHeight);
    }

    /**
     * Set the number of pixels equal to one graph unit of width.
     */
    public void setUnitWidth() {
        unitWidth = (double)getWidth() / (xMax - xMin + 1);
    }

    /**
     * Set the number of pixels equal to one graph unit of height.
     */
    public void setUnitHeight() {
        unitHeight = (double)getHeight() / (yMax - yMin);
    }

    /**
     * Set the number of pixels equal to one graph unit of height.
     */
    public void setUnitHeight(int height) {
        unitHeight = height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        //this.setMouseWheel(true);
        int notches = e.getWheelRotation();

        if (MiscUtils.MAC && e.isMetaDown() || e.isControlDown()) {
            if (notches < 0) {
                LocationController lc = LocationController.getInstance();
                lc.shiftRangeLeft();
            } else {
                LocationController lc = LocationController.getInstance();
                lc.shiftRangeRight();
            }
        }
        else {
            if (notches < 0) {
                LocationController lc = LocationController.getInstance();
                lc.zoomInOnMouse();
           } else {
                LocationController lc = LocationController.getInstance();
                lc.zoomOutFromMouse();
           }
        }
        parentFrame.resetLayers();
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
        return (MiscUtils.MAC && isMetaModifierPressed()) || (!MiscUtils.MAC && isCtrlKeyModifierPressed());
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


    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked( final MouseEvent event ) {

        if (event.getClickCount() == 2) {
            LocationController.getInstance().zoomInOnMouse();
            return;
        }

        trySelect(event.getPoint());

        setMouseModifier(event);
        parentFrame.resetLayers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent event) {

        setMouseModifier(event);

        this.requestFocus();

        int w = getWidth();
        Range xRange = getXRange();
        int x1 = getConstrainedX(event);

        baseX = transformXPixel(x1);
        initialScroll = getVerticalScrollBar().getValue();

        Point l = event.getLocationOnScreen();
        startX = l.x;
        startY = l.y;

        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setMouseClickPosition(transformXPixel(x1));
        parentFrame.resetLayers();
    }

    public void resetCursor() {
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent event) {

        GraphPaneController gpc = GraphPaneController.getInstance();
        LocationController lc = LocationController.getInstance();
        Range xRange = getXRange();
        int w = getWidth();
        int x2 = getConstrainedX(event);

        resetCursor();

        double x1 = transformXPos(gpc.getMouseDragRange().getFrom());

        if (gpc.isPanning()) {

            if(!panVert){
                Range r = lc.getRange();
                int shiftVal = (int) (Math.round((x1-x2) / getUnitWidth()));

                Range newr = new Range(r.getFrom()+shiftVal,r.getTo()+shiftVal);
                lc.setLocation(newr);
            }
        } else if (gpc.isZooming()) {
            Range r;
            if (isLocked()) {
                r = lockedRange;
            } else {
                r = lc.getRange();
            }
            int newMin = (int) Math.round(Math.min(x1, x2) / getUnitWidth());
            // some weirdness here, but it's to get around an off by one
            int newMax = (int) Math.max(Math.round(Math.max(x1, x2) / getUnitWidth())-1, newMin);
            Range newr = new Range(r.getFrom() + newMin, r.getFrom() + newMax);

            lc.setLocation(newr);
        } else if (gpc.isSelecting()) {
            for (Track t: tracks) {
                if (t.getRenderer().hasMappedValues()) {
                    if (t.getRenderer().rectangleSelect(selectionRect)) {
                        repaint();
                    }
                    break;
                }
            }
        }

        isDragging = false;
        setMouseModifier(event);

        gpc.setMouseReleasePosition(transformXPixel(x2));
        parentFrame.resetLayers();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered( final MouseEvent event ) {
        resetCursor();
        mouseInside = true;
        setMouseModifier(event);
        hidePopup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited( final MouseEvent event ) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        setMouseModifier(event);

        mouseInside = false;
        GraphPaneController.getInstance().setMouseXPosition(-1);
        GraphPaneController.getInstance().setMouseYPosition(Double.NaN, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent event) {

        setMouseModifier(event);

        GraphPaneController gpc = GraphPaneController.getInstance();
        int x2 = getConstrainedX(event);

        isDragging = true;

        if (gpc.isPanning()) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else if (gpc.isZooming() || gpc.isSelecting()) {
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }

        // Check if scrollbar is present (only vertical pan if present)
        boolean scroll = parentFrame.getVerticalScrollBar().isVisible();

        if (scroll) {

            //get new points
            Point l = event.getLocationOnScreen();
            int currX = l.x;
            int currY = l.y;

            //magnitude
            int magX = Math.abs(currX - startX);
            int magY = Math.abs(currY - startY);


            if (magX >= magY){
                //pan horizontally, reset vertical pan
                panVert = false;
                gpc.setMouseReleasePosition(transformXPixel(x2));
                parentFrame.getVerticalScrollBar().setValue(initialScroll);
            } else {
                //pan vertically, reset horizontal pan
                panVert = true;
                gpc.setMouseReleasePosition(baseX);
                parentFrame.getVerticalScrollBar().setValue(initialScroll - (currY - startY));
            }
        } else {
            //pan horizontally
            panVert = false;
            gpc.setMouseReleasePosition(transformXPixel(x2));
        }

        parentFrame.resetLayers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent event) {

        mouse_x = event.getX();
        mouse_y = event.getY();

        GraphPaneController gpc = GraphPaneController.getInstance();

        // Update the GraphPaneController's record of the mouse position
        gpc.setMouseXPosition(transformXPixel(event.getX()));
        switch (yAxisType) {
            case NONE:
                gpc.setMouseYPosition(Double.NaN, false);
                break;
            case INTEGER:
                gpc.setMouseYPosition(Math.floor(transformYPixel(event.getY())), true);
                break;
            case REAL:
                gpc.setMouseYPosition(transformYPixel(event.getY()), false);
                break;
        }
        gpc.setSpotlightSize(getXRange().getLength());
    }

    /**
     * Given a MouseEvent, return the x value constrained by the dimensions of the GraphPane.
     */
    private int getConstrainedX(MouseEvent event) {
        int x = event.getX();
        if (x < 0) {
            return 0;
        }
        return Math.min(x, getWidth());
    }

    private void tellModifiersToGraphPaneController() {
        GraphPaneController gpc = GraphPaneController.getInstance();
        setZooming(gpc);
        setPanning(gpc);
        setSelecting(gpc);
    }

    private void setZooming(GraphPaneController gpc) {
        gpc.setZooming(isDragging &&  ((isLeftClick() && isZoomModifierPressed()) || (isRightClick() && isZoomModifierPressed())));
    }

    private void setSelecting(GraphPaneController gpc) {
        gpc.setSelecting(isDragging &&  ((isLeftClick() && isSelectModifierPressed()) || (isRightClick() && isSelectModifierPressed())));
    }

    private void setPanning(GraphPaneController gpc) {
        gpc.setPanning(isDragging && isNoKeyModifierPressed());
    }

    /**
     * @return true if the track is locked, false o/w
     */
    public boolean isLocked() {
        return lockedRange != null;
    }

    public void setLocked(boolean b) {
        if (b) {
            lockedRange = LocationController.getInstance().getRange();
        } else {
            lockedRange = null;
            renderRequired = true;
        }
        repaint();
    }

    private BAMParametersDialog paramDialog;

    public void getBAMParams(BAMTrack bamTrack) {
        // capture parameters needed to adjust display
        if (paramDialog == null) {
            paramDialog = new BAMParametersDialog(Savant.getInstance(), true);
        }
        paramDialog.showDialog(bamTrack);
        if (paramDialog.isAccepted()) {
            bamTrack.setArcSizeVisibilityThreshold(paramDialog.getArcLengthThreshold());
            bamTrack.setPairedProtocol(paramDialog.getSequencingProtocol());
            bamTrack.setDiscordantMin(paramDialog.getDiscordantMin());
            bamTrack.setDiscordantMax(paramDialog.getDiscordantMax());
            bamTrack.setmaxBPForYMax(paramDialog.getMaxBPForYMax());

            bamTrack.prepareForRendering(LocationController.getInstance().getReferenceName() , LocationController.getInstance().getRange());
            repaint();
        }
    }

    public Frame getParentFrame(){
        return parentFrame;
    }

    public void setYGridOn(boolean value){
        this.yGridOn = value;
    }

    public void setBufferedImage(BufferedImage bi){
        this.bufferedImage = bi;
    }

    //POPUP
    public void tryPopup(Point p){

        Point p_offset = new Point(p.x, p.y - this.getOffset());
        if(tracks == null) return;
        for (Track t: tracks) {
            Map<Record, Shape> map = t.getRenderer().searchPoint(p_offset);
            if (map != null) {
                /** XXX: This line is here to get around what looks like a bug in the 1.6.0_20 JVM for Snow Leopard
                 * which causes the mouseExited events not to be triggered sometimes. We hide the popup before
                 * showing another. This needs to be done exactly here: after we know we have a new popup to show
                 * and before we set currentOverRecord. Otherwise, it won't work.
                 */
                hidePopup();

                currentOverRecord = (Record)map.keySet().toArray()[0];
                currentOverShape = map.get(currentOverRecord);
                if (currentOverRecord instanceof GenericContinuousRecord){
                    currentOverShape = ContinuousTrackRenderer.continuousRecordToEllipse(this, currentOverRecord);
                }

                createJidePopup();
                PopupPanel pp = PopupPanel.create(this, tracks[0].getDrawingMode(), t.getDataSource(), currentOverRecord);
                fireNewPopup(pp);
                if (pp != null){
                    popPanel.add(pp, BorderLayout.CENTER);
                    Point p1 = (Point)p.clone();
                    SwingUtilities.convertPointToScreen(p1, this);
                    jp.showPopup(p1.x -2, p1.y -2);
                    popupVisible = true;
                }
                repaint();
                return;
            }
        }
        // Didn't get a hit on any track.
        currentOverShape = null;
        currentOverRecord = null;
    }

    public void hidePopup(){
        if (popupVisible){
            popupVisible = false;
            jp.hidePopupImmediately();
        }
        if (currentOverShape != null) {
            currentOverShape = null;
            currentOverRecord = null;
            repaint();
        }
    }

    public void trySelect(Point p){

        Point p_offset = new Point(p.x, p.y - getOffset());

        for (Track t: tracks) {
            Map<Record, Shape> map = t.getRenderer().searchPoint(p_offset);
            if (map != null) {
                Record o = (Record)map.keySet().toArray()[0];
                t.getRenderer().addToSelected(o);
                repaint();
                break;
            }
        }
    }

    private void createJidePopup(){
        this.jp = new JidePopup();
        jp.setBackground(Color.WHITE);
        jp.getContentPane().setBackground(Color.WHITE);
        jp.getRootPane().setBackground(Color.WHITE);
        jp.getLayeredPane().setBackground(Color.WHITE);
        jp.setLayout(new BorderLayout());
        JPanel fill1 = new JPanel();
        fill1.setBackground(Color.WHITE);
        fill1.setPreferredSize(new Dimension(5,5));
        JPanel fill2 = new JPanel();
        fill2.setBackground(Color.WHITE);
        fill2.setPreferredSize(new Dimension(5,5));
        JPanel fill3 = new JPanel();
        fill3.setBackground(Color.WHITE);
        fill3.setPreferredSize(new Dimension(5,5));
        JPanel fill4 = new JPanel();
        fill4.setBackground(Color.WHITE);
        fill4.setPreferredSize(new Dimension(15,5));
        jp.add(fill1, BorderLayout.NORTH);
        jp.add(fill2, BorderLayout.SOUTH);
        jp.add(fill3, BorderLayout.EAST);
        jp.add(fill4, BorderLayout.WEST);

        jp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if(e.getX() < 0 || e.getY() < 0 || e.getX() >= e.getComponent().getWidth() || e.getY() >= e.getComponent().getHeight()){
                    hidePopup();
                }
            }
        });

        popPanel = new JPanel(new BorderLayout());
        popPanel.setBackground(Color.WHITE);
        jp.add(popPanel);
        jp.packPopup();
    }

    /**
     * Draw an informational message on top of this GraphPane.
     *
     * @param g2 the graphics to be rendered
     * @param message text of the message to be displayed
     */
    private void drawMessage(Graphics2D g2, String message, String subMessage) {

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = g2.getFont();
        Font subFont = font;

        int h = getSize().height/3;
        int w = getWidth();

        if (w > 500) {
            font = font.deriveFont(Font.PLAIN, 36);
            subFont = subFont.deriveFont(Font.PLAIN, 18);
        } else if (w > 150) {
            font = font.deriveFont(Font.PLAIN, 24);
            subFont = subFont.deriveFont(Font.PLAIN, 12);
        } else {
            font = font.deriveFont(Font.PLAIN, 12);
            subFont = subFont.deriveFont(Font.PLAIN, 8);
        }

        if(subMessage != null){
            drawMessageHelper(g2, message, font, w, h, -(subFont.getSize()/2));
            drawMessageHelper(g2, subMessage, subFont, w, h, font.getSize()-(subFont.getSize()/2));
        } else {
            drawMessageHelper(g2, message, font, w, h, 0);
        }
    }
    
    private void drawMessageHelper(Graphics2D g2, String message, Font font, int w, int h, int offset){
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();

        Rectangle2D stringBounds = font.getStringBounds(message, g2.getFontRenderContext());

        int preferredWidth = (int)stringBounds.getWidth()+metrics.getHeight();
        int preferredHeight = (int)stringBounds.getHeight()+metrics.getHeight();

        w = Math.min(preferredWidth,w);
        h = Math.min(preferredHeight,h);

        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;

        g2.setColor(ColourSettings.getGlassPaneBackground());
        x = (getWidth() - (int)stringBounds.getWidth()) / 2;
        y = (getHeight() / 2) + ((metrics.getAscent()- metrics.getDescent()) / 2) + offset;

        g2.drawString(message,x,y);
    }

    /**
     * One of tracks is still loading.  Instead of rendering, put up a progress-bar.
     *
     * @param msg progress message to be displayed
     */
    private void showProgress(String msg) {
        if (progressPanel == null) {
            progressPanel = new ProgressPanel();
            add(progressPanel);
        }
        progressPanel.setMessage(msg);
    }
    
    public void addExportEventListener(ExportEventListener eel){
        synchronized (exportListeners) {
            exportListeners.add(eel);
        }
    }

    public void removeExportListener(ExportEventListener eel){
        synchronized (exportListeners) {
            exportListeners.remove(eel);
        }
    }

    public void removeExportListeners(){
        synchronized (exportListeners) {
            exportListeners.clear();
        }
    }

    public void fireExportReady(Range range, BufferedImage image){
        int size = exportListeners.size();
        for (int i = 0; i < size; i++){
            exportListeners.get(i).exportCompleted(new ExportEvent(range, image));
            size = exportListeners.size(); //a listener may get removed
        }
    }

    public final void addPopupEventListener(PopupEventListener pel){
        synchronized (popupListeners) {
            popupListeners.add(pel);
        }
    }

    public void removePopupListener(PopupEventListener eel){
        synchronized (popupListeners) {
            popupListeners.remove(eel);
        }
    }

    public void fireNewPopup(PopupPanel popup){
        int size = popupListeners.size();
        for (int i = 0; i < size; i++){
            popupListeners.get(i).newPopup(new PopupEvent(popup));
            size = popupListeners.size(); //a listener may get removed
        }
    }
    
    public void setScaledToContents(boolean value) {
        scaledToContents = value;
    }


    public boolean isScaledToContents() {
        return scaledToContents;
    }
}
