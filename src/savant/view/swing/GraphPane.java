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
package savant.view.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.FrameAdapter;
import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.ContinuousRecord;
import savant.api.data.Record;
import savant.api.event.ExportEvent;
import savant.api.event.PopupEvent;
import savant.api.util.Listener;
import savant.controller.GraphPaneController;
import savant.controller.LocationController;
import savant.controller.event.GraphPaneEvent;
import savant.data.types.BAMIntervalRecord;
import savant.exception.RenderingException;
import savant.selection.PopupThread;
import savant.selection.PopupPanel;
import savant.settings.ColourSettings;
import savant.settings.InterfaceSettings;
import savant.util.swing.ProgressPanel;
import savant.util.*;
import savant.view.tracks.BAMTrack;
import savant.view.tracks.BAMTrackRenderer;
import savant.view.tracks.ContinuousTrackRenderer;
import savant.view.tracks.Track;
import savant.view.tracks.TrackCancellationListener;

/**
 *
 * @author mfiume
 */
public class GraphPane extends JPanel implements GraphPaneAdapter, MouseWheelListener, MouseListener, MouseMotionListener {

    private static final Log LOG = LogFactory.getLog(GraphPane.class);
    private Track[] tracks;
    private Frame parentFrame;
    private int mouseX = 0;
    private int mouseY = 0;
    /**
     * min / max axis values
     */
    private int xMin;
    private int xMax;
    protected int yMin;
    protected int yMax;
    private double unitWidth = Double.NaN;
    protected double unitHeight;
    private AxisType yAxisType = AxisType.NONE;
    private AxisType xAxisType = AxisType.NONE;
    private boolean mouseInside = false;
    // Locking
    private Range lockedRange;
    /**
     * By default, tracks adjust their unitHeight to accommodate the contents
     * without a scroll-bar.
     */
    protected boolean scaledToFit = true;
    /**
     * Selection Variables
     */
    private Rectangle2D selectionRect = new Rectangle2D.Double();
    private boolean isDragging = false;
    //scrolling...
    private BufferedImage bufferedImage;
    private Range prevRange = null;
    private DrawingMode prevMode = null;
    private Dimension prevSize = null;
    private String prevRef = null;
    private boolean paneResize = false;
    private int newHeight;
    private int oldWidth = -1;
    private int oldHeight = -1;
    private int oldViewHeight = -1;
    private boolean renderRequired = false;
    private int posOffset = 0;
    protected boolean forcedHeight = false;
    //dragging
    private int startX;
    private int startY;
    private int baseX;
    private int initialScroll;
    private boolean panVert = false;
    //popup
    public Thread popupThread;
    private Record currentOverRecord = null;
    private Shape currentOverShape = null;
    /**
     * Provides progress indication when loading a track.
     */
    private ProgressPanel progressPanel;
    //awaiting exported images
    private final List<Listener<ExportEvent>> exportListeners = new ArrayList<Listener<ExportEvent>>();
    private final List<Listener<PopupEvent>> popupListeners = new ArrayList<Listener<PopupEvent>>();
    private boolean yAxisLocked;

    /**
     * CONSTRUCTOR
     */
    public GraphPane(Frame parent) {
        this.parentFrame = parent;
        addMouseListener(this); // listens for own mouse and
        addMouseMotionListener(this); // mouse-motion events
        //addKeyListener( this );
        getInputMap().allKeys();
        addMouseWheelListener(this);

        //trackToYRangeMap = new HashMap<Track, Range>();

        popupThread = new Thread(new PopupThread(this), "PopupThread");
        popupThread.start();

        //
        GraphPaneController gpc = GraphPaneController.getInstance();

        gpc.addListener(new Listener<GraphPaneEvent>() {
            @Override
            public void handleEvent(GraphPaneEvent event) {
                if (event.getType() == GraphPaneEvent.Type.HIGHLIGHTING) {
                    repaint();
                }
            }
        });
    }

    /**
     * Lock the Y Axes from changing automatically
     *
     * @param b
     */
    public void setYMaxLocked(boolean b) {
        System.out.println("Locking Y max: " + b);
        this.yAxisLocked = b;
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
    public boolean render(Graphics2D g2) {
        return render(g2, new Range(xMin, xMax), null);
    }

    public boolean render(Graphics2D g2, Range xRange, Range yRange) {
        LOG.trace("GraphPane.render(g2, " + xRange + ", " + yRange + ")");
        double oldUnitHeight = unitHeight;
        int oldYMax = yMax;

        // Paint a gradient from top to bottom
        GradientPaint gp0 = new GradientPaint(0, 0, ColourSettings.getColor(ColourKey.GRAPH_PANE_BACKGROUND_TOP), 0, getHeight(), ColourSettings.getColor(ColourKey.GRAPH_PANE_BACKGROUND_BOTTOM));
        g2.setPaint(gp0);
        g2.fillRect(0, 0, getWidth(), getHeight());

        GraphPaneController gpc = GraphPaneController.getInstance();
        LocationController lc = LocationController.getInstance();
        JScrollBar scroller = getVerticalScrollBar();

        if (gpc.isPanning() && !isLocked()) {

            double fromX = transformXPos(gpc.getMouseClickPosition());
            double toX = transformXPos(gpc.getMouseReleasePosition());
            g2.translate(toX - fromX, 0);
        }

        // Deal with the progress-bar.
        if (tracks == null) {
            parentFrame.updateProgress();
            return false;
        } else {
            for (Track t : tracks) {
                if (t.getRenderer().isWaitingForData()) {
                    String progressMsg = (String) t.getRenderer().getInstruction(DrawingInstruction.PROGRESS);
                    setPreferredSize(new Dimension(getWidth(), 0));
                    showProgress(progressMsg, -1.0);
                    return false;
                }
            }
        }
        if (progressPanel != null) {
            remove(progressPanel);
            progressPanel = null;
        }

        int minYRange = Integer.MAX_VALUE;
        int maxYRange = Integer.MIN_VALUE;
        AxisType bestYAxis = AxisType.NONE;
        for (Track t : tracks) {

            // ask renderers for extra info on range; consolidate to maximum Y range
            AxisRange axisRange = (AxisRange) t.getRenderer().getInstruction(DrawingInstruction.AXIS_RANGE);

            if (axisRange != null) {
                int axisYMin = axisRange.getYMin();
                int axisYMax = axisRange.getYMax();
                if (axisYMin < minYRange) {
                    minYRange = axisYMin;
                }
                if (axisYMax > maxYRange) {
                    maxYRange = axisYMax;
                }
            }

            // Ask renderers if they want horizontal grid-lines; if any say yes, draw them.
            switch (t.getYAxisType(t.getResolution(xRange))) {
                case INTEGER_GRIDLESS:
                    if (bestYAxis == AxisType.NONE) {
                        bestYAxis = AxisType.INTEGER_GRIDLESS;
                    }
                    break;
                case INTEGER:
                    if (bestYAxis != AxisType.REAL) {
                        bestYAxis = AxisType.INTEGER;
                    }
                    break;
                case REAL:
                    bestYAxis = AxisType.REAL;
                    break;
            }
        }

        setXAxisType(tracks[0].getXAxisType(tracks[0].getResolution(xRange)));
        setXRange(xRange);
        setYAxisType(bestYAxis);
        Range consolidatedYRange = new Range(minYRange, maxYRange);

        setYRange(consolidatedYRange);
        consolidatedYRange = new Range(yMin, yMax);

        DrawingMode currentMode = tracks[0].getDrawingMode();

        boolean sameRange = (prevRange != null && xRange.equals(prevRange));
        if (!sameRange) {
            PopupPanel.hidePopup();
        }
        boolean sameMode = currentMode == prevMode;
        boolean sameSize = prevSize != null && getSize().equals(prevSize) && parentFrame.getFrameLandscape().getWidth() == oldWidth && parentFrame.getFrameLandscape().getHeight() == oldHeight;
        boolean sameRef = prevRef != null && lc.getReferenceName().equals(prevRef);
        boolean withinScrollBounds = bufferedImage != null && scroller.getValue() >= getOffset() && scroller.getValue() < getOffset() + getViewportHeight() * 2;

        //bufferedImage stores the current graphic for future use. If nothing
        //has changed in the track since the last render, bufferedImage will
        //be used to redraw the current view. This method allows for fast repaints
        //on tracks where nothing has changed (panning, selection, plumbline,...)

        //if nothing has changed draw buffered image
        if (sameRange && sameMode && sameSize && sameRef && !renderRequired && withinScrollBounds) {
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
            LOG.debug("Requesting " + getWidth() + "×" + h + " bufferedImage.");
            bufferedImage = new BufferedImage(getWidth(), h, BufferedImage.TYPE_INT_RGB);
            if (bufferedImage.getHeight() == getHeight()) {
                setOffset(0);
            } else {
                setOffset(scroller.getValue() - getViewportHeight());
            }
            LOG.debug("Rendering fresh " + bufferedImage.getWidth() + "×" + bufferedImage.getHeight() + " bufferedImage at (0, " + getOffset() + ")");

            Graphics2D g3 = bufferedImage.createGraphics();
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            prevRange = xRange;
            prevSize = getSize();
            prevMode = tracks[0].getDrawingMode();
            prevRef = lc.getReferenceName();

            renderBackground(g3, xAxisType == AxisType.INTEGER || xAxisType == AxisType.REAL, yAxisType == AxisType.INTEGER || yAxisType == AxisType.REAL);

            // Call the actual render() methods.
            boolean nothingRendered = true;
            String message = null;
            int priority = -1;


            for (Track t : tracks) {
                // Change renderers' drawing instructions to reflect consolidated YRange
                AxisRange axes = (AxisRange) t.getRenderer().getInstruction(DrawingInstruction.AXIS_RANGE);

                if (axes == null) {
                    axes = new AxisRange(xRange, consolidatedYRange);
                } else {
                    axes = new AxisRange(axes.getXRange(), consolidatedYRange);
                }

                //System.out.println("Consolidated y range for " + t.getName() + " is " + consolidatedYRange);
                t.getRenderer().addInstruction(DrawingInstruction.AXIS_RANGE, axes);

                try {
                    t.getRenderer().render(g3, this);
                    nothingRendered = false;
                } catch (RenderingException rx) {
                    if (rx.getPriority() > priority) {
                        // If we have more than one message with the same priority, the first one will end up being drawn.
                        message = rx.getMessage();
                        priority = rx.getPriority();
                    }
                } catch (Throwable x) {
                    // Renderer itself threw an exception.
                    LOG.error("Error rendering " + t, x);
                    message = MiscUtils.getMessage(x);
                    priority = RenderingException.ERROR_PRIORITY;
                }
            }
            if (nothingRendered && message != null) {
                setPreferredSize(new Dimension(getWidth(), 0));
                revalidate();
                drawMessage(g3, message);
            }

            updateYMax();

            // If a change has occured that affects scrollbar...
            if (paneResize) {
                paneResize = false;

                // Change size of current frame
                if (getHeight() != newHeight) {
                    Dimension newSize = new Dimension(getWidth(), newHeight);
                    setPreferredSize(newSize);
                    setSize(newSize);
                    parentFrame.validate(); // Ensures that scroller.getMaximum() is up to date.

                    // If pane is resized, scrolling always starts at the bottom.  The only place
                    // where looks wrong is when we have a continuous track with negative values.
                    scroller.setValue(scroller.getMaximum());
                    repaint();
                    return false;
                }
            } else if (oldViewHeight != -1 && oldViewHeight != getViewportHeight()) {
                int newViewHeight = getViewportHeight();
                int oldScroll = scroller.getValue();
                scroller.setValue(oldScroll + (oldViewHeight - newViewHeight));
                oldViewHeight = newViewHeight;
            }
            oldWidth = parentFrame.getFrameLandscape().getWidth();
            oldHeight = parentFrame.getFrameLandscape().getHeight();

            g2.drawImage(bufferedImage, 0, getOffset(), this);
            fireExportEvent(xRange, bufferedImage);

            renderCurrentSelected(g2);
        }
        return true;
    }

    /**
     * Get the height of the viewport. The viewport is the grandparent of this
     * GraphPane.
     */
    private int getViewportHeight() {
        return getParent().getParent().getHeight();
    }

    /**
     * Access to the scrollbar associated with this GraphPane. For ordinary
     * GraphPanes, the JScrollPane is our great-grandparent.
     */
    public JScrollBar getVerticalScrollBar() {
        return ((JScrollPane) getParent().getParent().getParent()).getVerticalScrollBar();
    }

    private void renderCurrentSelected(Graphics2D g2) {
        // Temporarily shift the origin
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(0, getOffset());
        for (Track t : tracks) {
            if (t.getRenderer().hasMappedValues()) {
                List<Shape> currentSelected = t.getRenderer().getCurrentSelectedShapes(this);
                if (currentSelected.size() > 0) {
                    boolean arcMode = t.getDrawingMode() == DrawingMode.ARC_PAIRED;
                    for (Shape selectedShape : currentSelected) {
                        if (selectedShape != currentOverShape) {
                            if (arcMode) {
                                g2.setColor(Color.GREEN);
                                g2.draw(selectedShape);
                            } else {
                                //g2.setColor(Color.GREEN);
                                g2.setColor(new Color(0, 255, 0, 150));
                                g2.fill(selectedShape);
                                if (selectedShape.getBounds().getWidth() > 5) {
                                    g2.setColor(Color.BLACK);
                                    g2.draw(selectedShape);
                                }
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

                //get record pair
                BAMIntervalRecord rec1 = (BAMIntervalRecord) currentOverRecord;
                BAMIntervalRecord rec2 = ((BAMTrack) tracks[0]).getMate(rec1); //mate

                //render reads with mismatches
                ((BAMTrackRenderer) tracks[0].getRenderer()).renderReadsFromArc(g2, this, rec1, rec2, prevRange);

            } else {
                g2.setColor(new Color(255, 0, 0, 150));
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

    @Override
    public void setRenderForced() {
        renderRequired = true;
    }

    public boolean isRenderForced() {
        return renderRequired;
    }

    /**
     * Force the bufferedImage to contain entire height at current range.
     * Intended for creating images for track export. Make sure you unforce
     * immediately after!
     */
    public void forceFullHeight() {
        forcedHeight = true;
    }

    public void unforceFullHeight() {
        forcedHeight = false;
    }

    private void setOffset(int offset) {
        posOffset = offset;
    }

    @Override
    public int getOffset() {
        return posOffset;
    }

    private void requestHeight(int h) {
        newHeight = h;
        paneResize = true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (tracks != null && tracks.length > 0) {
            LOG.trace("GraphPane.paintComponent(" + tracks[0].getName() + ")");
        }
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        boolean trueRender = render(g2);

        GraphPaneController gpc = GraphPaneController.getInstance();
        int h = getHeight();

        // Aiming adjustments.
        if (gpc.isAiming() && mouseInside) {
            g2.setColor(Color.BLACK);
            Font thickfont = g2.getFont().deriveFont(Font.BOLD, 15.0F);
            g2.setFont(thickfont);
            int genomeX = gpc.getMouseXPosition();
            double genomeY = gpc.getMouseYPosition();
            String target = "";
            target += "X: " + MiscUtils.numToString(genomeX);
            if (!Double.isNaN(genomeY)) {
                target += " Y: " + MiscUtils.numToString(genomeY);
            }

            g2.drawLine(mouseX, 0, mouseX, h);
            if (genomeY != -1) {
                g.drawLine(0, mouseY, this.getWidth(), mouseY);
            }
            g2.drawString(target, mouseX + 5, mouseY - 5);
        }

        double x1 = transformXPos(gpc.getMouseClickPosition());
        double x2 = transformXPos(gpc.getMouseReleasePosition());

        double width = x1 - x2;

        selectionRect = new Rectangle2D.Double(width < 0 ? x1 : x2, 0.0, Math.max(2.0, Math.abs(width)), h);

        if (gpc.isPanning()) {
            // Panning adjustments (none).
        } else if (gpc.isZooming() || gpc.isSelecting()) {
            // Zooming adjustments.
            Rectangle2D rectangle = new Rectangle2D.Double(selectionRect.getX(), selectionRect.getY() - 10.0, selectionRect.getWidth(), selectionRect.getHeight() + 10.0);
            g2.setColor(Color.gray);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 3f, new float[]{4f}, 4f));
            g2.draw(rectangle);

            if (gpc.isZooming()) {
                g.setColor(ColourSettings.getColor(ColourKey.GRAPH_PANE_ZOOM_FILL));
            } else if (gpc.isSelecting()) {
                g.setColor(ColourSettings.getColor(ColourKey.GRAPH_PANE_SELECTION_FILL));
            }
            g2.fill(selectionRect);
        }

        // Plumbing adjustments.
        Range xRange = getXRange();
        if (gpc.isPlumbing()) {
            g2.setColor(Color.BLACK);
            double spos = transformXPos(gpc.getMouseXPosition());
            g2.draw(new Line2D.Double(spos, 0, spos, h));
            double rpos = transformXPos(gpc.getMouseXPosition() + 1);
            g2.draw(new Line2D.Double(rpos, 0, rpos, h));
        }

        // Spotlight
        if (gpc.isSpotlight() && !gpc.isZooming()) {

            int center = gpc.getMouseXPosition();
            int left = center - gpc.getSpotlightSize() / 2;
            int right = left + gpc.getSpotlightSize();

            g2.setColor(new Color(0, 0, 0, 200));

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
            drawMessage((Graphics2D) g, "Locked");
        }
        if (trueRender) {
            gpc.delistRenderingGraphpane(this);
        }
    }

    /**
     * Render the background of this GraphPane
     *
     * @param g The graphics object to use
     */
    private void renderBackground(Graphics2D g2, boolean xGridOn, boolean yGridOn) {
        int h = getHeight();
        int w = getWidth();

        // Paint a gradient from top to bottom
        GradientPaint gp0 = new GradientPaint(0, 0, ColourSettings.getColor(ColourKey.GRAPH_PANE_BACKGROUND_TOP), 0, h, ColourSettings.getColor(ColourKey.GRAPH_PANE_BACKGROUND_BOTTOM));
        g2.setPaint(gp0);
        g2.fillRect(0, 0, w, h);

        // We don't want the axes stomping on our labels, so make sure the clip excludes them.
        Area clipArea = new Area(new Rectangle(0, 0, w, h));
        Color gridColor = ColourSettings.getColor(ColourKey.AXIS_GRID);

        if (yGridOn) {
            // Smallish font for tick labels.
            Font tickFont = g2.getFont().deriveFont(Font.PLAIN, 9);

            int[] yTicks = MiscUtils.getTickPositions(transformYPixel(getHeight()), transformYPixel(0.0));

            g2.setColor(gridColor);
            g2.setFont(tickFont);
            for (int t : yTicks) {
                double y = transformYPos(t);

                // Skip labels at the top or bottom of the window because they look stupid.
                if (y != 0.0 && y != getHeight()) {
                    String s = Integer.toString(t);
                    Rectangle2D labelRect = tickFont.getStringBounds(s, g2.getFontRenderContext());
                    double baseline = y + labelRect.getHeight() * 0.5 - 2.0;
                    g2.drawString(s, 4.0F, (float) baseline);
                    clipArea.subtract(new Area(new Rectangle2D.Double(3.0, baseline - labelRect.getHeight() - 1.0, labelRect.getWidth() + 2.0, labelRect.getHeight() + 2.0)));
                }
            }
            g2.setClip(clipArea);
            for (int t2 : yTicks) {
                double y = transformYPos(t2);
                g2.draw(new Line2D.Double(0.0, y, w, y));
            }
        }
        if (xGridOn) {
            Range r = LocationController.getInstance().getRange();
            int[] xTicks = MiscUtils.getTickPositions(r);

            g2.setColor(gridColor);
            for (int t : xTicks) {
                double x = transformXPos(t);
                g2.draw(new Line2D.Double(x, 0, x, h));
            }
        }
        g2.setClip(null);
    }

    public Range getXRange() {
        return new Range(xMin, xMax);
    }

    /**
     * Set the range for the horizontal axis, adjusting the width of graph
     * units.
     *
     * @param r an X range
     */
    @Override
    public void setXRange(Range r) {
        if (r != null) {
            xMin = r.getFrom();
            xMax = r.getTo();
            setUnitWidth();
        }
    }

    /**
     * Set the interpretation of the pane's horizontal coordinate system.
     */
    public void setXAxisType(AxisType type) {
        xAxisType = type;
    }

    @Override
    public Range getYRange() {
        return new Range(yMin, yMax);
    }

    /**
     * Set the vertical range, adjusting the height of graph units.
     *
     * @param r a Y range
     */
    @Override
    public void setYRange(Range r) {

        /*
         *         if (yAxisLocked) {
         if (lastYRange != null) {
         consolidatedYRange = lastYRange;
         System.out.println("Got previous y range for " + this.getTracks()[0].getName() + " at " + consolidatedYRange);
         } else {
         System.out.println("No y range stored for " + this.getTracks()[0].getName());
         }
         }
         */

        if (yAxisLocked) {
            return;
        }

        if (r != null && yAxisType != AxisType.NONE) {
            int oldYMin = yMin;
            yMin = r.getFrom();
            yMax = r.getTo();

            if (scaledToFit) {
                setUnitHeight();
                LOG.debug("setYRange set unit height to " + unitHeight);
            } else {
                // Adjust ymin to keep the x-axis from dropping down as we scroll along.
                if ((yMin < 0.0 || oldYMin < 0.0) && oldYMin < yMin) {
                    yMin = oldYMin;
                }
            }
        }

        //System.out.println("Saving range for " + this.getTracks()[0].getName() + " at " + new Range(yMin, yMax));

    }

    /**
     * Set the interpretation of the pane's vertical coordinate system.
     */
    public void setYAxisType(AxisType type) {
        yAxisType = type;
        parentFrame.setYMaxVisible(type != AxisType.NONE);
    }

    /**
     * Calculate the number of pixels equal to one graph unit of height.
     *
     * @return the height of a graph unit in pixels
     */
    @Override
    public double getUnitHeight() {
        return unitHeight;
    }

    /**
     * Set the number of pixels equal to one graph unit of height.
     */
    public void setUnitHeight() {
        unitHeight = (double) getHeight() / (yMax - yMin);
    }

    /**
     * Set the number of pixels equal to one graph unit of height.
     */
    @Override
    public void setUnitHeight(double height) {
        unitHeight = height;
    }

    /**
     *
     * @return the number of pixels equal to one graph unit of width.
     */
    @Override
    public double getUnitWidth() {
        return unitWidth;
    }

    /**
     * Set the number of pixels equal to one graph unit of width.
     */
    public void setUnitWidth() {
        unitWidth = (double) getWidth() / (xMax - xMin + 1);
    }

    /**
     * Transform a horizontal position in terms of drawing coordinates into
     * graph units.
     *
     * @param pix drawing position in pixels
     * @return corresponding logical position
     */
    @Override
    public int transformXPixel(double pix) {
        return (int) Math.floor(pix / unitWidth + xMin);
    }

    /**
     * Transform a horizontal position in terms of graph units into a drawing
     * coordinate.
     *
     * @param pos position in graph coordinates
     * @return corresponding drawing coordinate
     */
    @Override
    public double transformXPos(int pos) {
        return (pos - xMin) * unitWidth;
    }

    /**
     * Transform a vertical position in terms of pixels into graph units.
     *
     * @param pix position in pixel coordinates
     * @return corresponding graph coordinate
     */
    @Override
    public double transformYPixel(double pix) {
        return (getHeight() - pix) / unitHeight + yMin;
    }

    /**
     * Transform a vertical position in terms of graph units into a pixel
     * position.
     *
     * @param pos position in graph coordinates
     * @return a corresponding drawing coordinate
     */
    @Override
    public double transformYPos(double pos) {
        return getHeight() - ((pos - yMin) * unitHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        int notches = e.getWheelRotation();
        LocationController lc = LocationController.getInstance();

        if (MiscUtils.MAC && e.isMetaDown() || e.isControlDown()) {
            if (notches < 0) {
                lc.shiftRangeLeft();
            } else {
                lc.shiftRangeRight();
            }
        } else {
            if (InterfaceSettings.doesWheelZoom()) {
                if (notches < 0) {
                    lc.zoomInOnMouse();
                } else {
                    lc.zoomOutFromMouse();
                }
            } else {
                JScrollBar sb = getVerticalScrollBar();
                if (sb.isVisible()) {
                    sb.setValue(sb.getValue() + notches * 15);
                }
            }
        }
    }

    private void setMouseModifier(MouseEvent e) {
        GraphPaneController gpc = GraphPaneController.getInstance();
        boolean zooming = MiscUtils.MAC ? e.isMetaDown() : e.isControlDown();
        boolean selecting = e.isShiftDown() && !zooming;
        gpc.setZooming(isDragging && zooming);
        gpc.setPanning(isDragging && !zooming && !selecting);
        gpc.setSelecting(isDragging && selecting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent event) {

        if (event.getClickCount() == 2) {
            LocationController.getInstance().zoomInOnMouse();
            return;
        }

        trySelect(event.getPoint());

        setMouseModifier(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent event) {

        setMouseModifier(event);

        requestFocus();

        int x1 = getConstrainedX(event);

        baseX = transformXPixel(x1);
        initialScroll = getVerticalScrollBar().getValue();

        Point l = event.getLocationOnScreen();
        startX = l.x;
        startY = l.y;

        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setMouseClickPosition(transformXPixel(x1));
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
        int x2 = getConstrainedX(event);

        resetCursor();

        double x1 = transformXPos(gpc.getMouseClickPosition());

        if (gpc.isPanning()) {

            if (!panVert) {
                Range r = lc.getRange();
                int shiftVal = gpc.getMouseClickPosition() - transformXPixel(x2);
                Range newr = new Range(r.getFrom() + shiftVal, r.getTo() + shiftVal);
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
            int newMax = (int) Math.max(Math.round(Math.max(x1, x2) / getUnitWidth()) - 1, newMin);
            Range newr = new Range(r.getFrom() + newMin, r.getFrom() + newMax);

            lc.setLocation(newr);
        } else if (gpc.isSelecting()) {
            for (Track t : tracks) {
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(final MouseEvent event) {
        resetCursor();
        mouseInside = true;
        setMouseModifier(event);
        PopupPanel.hidePopup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(final MouseEvent event) {
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
        JScrollBar scroller = getVerticalScrollBar();
        boolean scroll = scroller.isVisible();

        if (scroll) {

            //get new points
            Point l = event.getLocationOnScreen();
            int currX = l.x;
            int currY = l.y;

            //magnitude
            int magX = Math.abs(currX - startX);
            int magY = Math.abs(currY - startY);


            if (magX >= magY) {
                //pan horizontally, reset vertical pan
                panVert = false;
                gpc.setMouseReleasePosition(transformXPixel(x2));
                scroller.setValue(initialScroll);
            } else {
                //pan vertically, reset horizontal pan
                panVert = true;
                gpc.setMouseReleasePosition(baseX);
                scroller.setValue(initialScroll - (currY - startY));
            }
        } else {
            //pan horizontally
            panVert = false;
            gpc.setMouseReleasePosition(transformXPixel(x2));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent event) {

        mouseX = event.getX();
        mouseY = event.getY();

        if (!Double.isNaN(unitWidth)) {
            GraphPaneController gpc = GraphPaneController.getInstance();

            // Update the GraphPaneController's record of the mouse position
            gpc.setMouseXPosition(transformXPixel(mouseX));
            switch (yAxisType) {
                case NONE:
                    gpc.setMouseYPosition(Double.NaN, false);
                    break;
                case INTEGER:
                case INTEGER_GRIDLESS:
                    gpc.setMouseYPosition(Math.floor(transformYPixel(mouseY)), true);
                    break;
                case REAL:
                    gpc.setMouseYPosition(transformYPixel(mouseY), false);
                    break;
            }
            gpc.setSpotlightSize(getXRange().getLength());
        }
    }

    /**
     * Given a MouseEvent, return the x value constrained by the dimensions of
     * the GraphPane.
     */
    private int getConstrainedX(MouseEvent event) {
        int x = event.getX();
        if (x < 0) {
            return 0;
        }
        return Math.min(x, getWidth());
    }

    /**
     * A locked track maintains its horizontal location while other tracks
     * scroll.
     *
     * @return true if the track is locked, false otherwise
     */
    public boolean isLocked() {
        return lockedRange != null;
    }

    /**
     * Set a track so that it doesn't scroll horizontally.
     *
     * @param b true to prevent horizontal scrolling
     */
    public void setLocked(boolean b) {
        if (b) {
            lockedRange = LocationController.getInstance().getRange();
        } else {
            lockedRange = null;
            renderRequired = true;
        }
        repaint();
    }

    @Override
    public FrameAdapter getParentFrame() {
        return parentFrame;
    }

    //POPUP
    public void tryPopup(Point p) {

        if (tracks == null) {
            return;
        }

        Point pt = new Point(p.x, p.y - getOffset());

        for (Track t : tracks) {
            Map<Record, Shape> map = t.getRenderer().searchPoint(pt);
            if (map != null) {
                /**
                 * XXX: This line is here to get around what looks like a bug in
                 * the 1.6.0_20 JVM for Snow Leopard which causes the
                 * mouseExited events not to be triggered sometimes. We hide the
                 * popup before showing another. This needs to be done exactly
                 * here: after we know we have a new popup to show and before we
                 * set currentOverRecord. Otherwise, it won't work.
                 */
                PopupPanel.hidePopup();

                // Arbitrarily pick the first record in the map.  Most of the time, there will be only one.
                Record overRecord = map.keySet().iterator().next();

                Point p1 = (Point) p.clone();
                SwingUtilities.convertPointToScreen(p1, this);
                PopupPanel.showPopup(this, p1, t, overRecord);

                currentOverRecord = overRecord;
                currentOverShape = map.get(currentOverRecord);
                if (currentOverRecord instanceof ContinuousRecord) {
                    currentOverShape = ContinuousTrackRenderer.continuousRecordToEllipse(this, currentOverRecord);
                }

                repaint();
                return;
            }
        }
        // Didn't get a hit on any track.
        currentOverShape = null;
        currentOverRecord = null;
    }

    /**
     * Invoked when user selects something in the popup menu.
     *
     * @param rec
     */
    @Override
    public void recordSelected(Record rec) {
        for (Track t : tracks) {
            t.getRenderer().addToSelected(rec);
        }
        repaint();
    }

    @Override
    public void popupHidden() {
        if (currentOverShape != null) {
            currentOverShape = null;
            currentOverRecord = null;
            repaint();
        }
    }

    public void trySelect(Point p) {

        Point p_offset = new Point(p.x, p.y - getOffset());

        for (Track t : tracks) {
            Map<Record, Shape> map = t.getRenderer().searchPoint(p_offset);
            if (map != null) {
                Object[] recs = map.keySet().toArray();
                if (recs.length == 1) {
                    t.getRenderer().addToSelected((Record) recs[0]);
                } else {
                    ArrayList<Record> array = new ArrayList<Record>();
                    for (Object rec : recs) {
                        array.add((Record) rec);
                    }
                    t.getRenderer().toggleGroup(array);
                }
                repaint();
                break;
            }
        }
    }

    /**
     * Draw an informational message on top of this GraphPane.
     *
     * @param g2 the graphics to be rendered
     * @param message text of the message to be displayed
     */
    private void drawMessage(Graphics2D g2, String message) {

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = g2.getFont();
        Font subFont = font;

        int h = getSize().height / 3;
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

        int returnPos = message.indexOf('\n');
        g2.setColor(ColourSettings.getColor(ColourKey.GRAPH_PANE_MESSAGE));
        if (returnPos > 0) {
            drawMessageHelper(g2, message.substring(0, returnPos), font, w, h, -(subFont.getSize() / 2));
            drawMessageHelper(g2, message.substring(returnPos + 1), subFont, w, h, font.getSize() - (subFont.getSize() / 2));
        } else {
            drawMessageHelper(g2, message, font, w, h, 0);
        }
    }

    private void drawMessageHelper(Graphics2D g2, String message, Font font, int w, int h, int offset) {
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();

        Rectangle2D stringBounds = font.getStringBounds(message, g2.getFontRenderContext());

        int preferredWidth = (int) stringBounds.getWidth() + metrics.getHeight();
        int preferredHeight = (int) stringBounds.getHeight() + metrics.getHeight();

        w = Math.min(preferredWidth, w);
        h = Math.min(preferredHeight, h);

        int x = (getWidth() - (int) stringBounds.getWidth()) / 2;
        int y = (getHeight() / 2) + ((metrics.getAscent() - metrics.getDescent()) / 2) + offset;

        g2.drawString(message, x, y);
    }

    /**
     * One of tracks is still loading. Instead of rendering, put up a
     * progress-bar.
     *
     * @param msg progress message to be displayed
     */
    void showProgress(String msg, double fraction) {
        if (progressPanel == null) {
            progressPanel = new ProgressPanel(new TrackCancellationListener(parentFrame));
            add(progressPanel);
        }
        progressPanel.setMessage(msg);
        progressPanel.setFraction(fraction);
        validate();
    }

    public void addExportEventListener(Listener<ExportEvent> eel) {
        synchronized (exportListeners) {
            exportListeners.add(eel);
        }
    }

    public void removeExportListener(Listener<ExportEvent> eel) {
        synchronized (exportListeners) {
            exportListeners.remove(eel);
        }
    }

    public void removeExportListeners() {
        synchronized (exportListeners) {
            exportListeners.clear();
        }
    }

    public void fireExportEvent(Range range, BufferedImage image) {
        int size = exportListeners.size();
        for (int i = 0; i < size; i++) {
            exportListeners.get(i).handleEvent(new ExportEvent(range, image));
            size = exportListeners.size(); //a listener may get removed
        }
    }

    @Override
    public final void addPopupListener(Listener<PopupEvent> pel) {
        synchronized (popupListeners) {
            popupListeners.add(pel);
        }
    }

    @Override
    public void removePopupListener(Listener<PopupEvent> eel) {
        synchronized (popupListeners) {
            popupListeners.remove(eel);
        }
    }

    @Override
    public void firePopupEvent(PopupPanel popup) {
        int size = popupListeners.size();
        for (int i = 0; i < size; i++) {
            popupListeners.get(i).handleEvent(new PopupEvent(popup));
            size = popupListeners.size(); //a listener may get removed
        }
    }

    @Override
    public void setScaledToFit(boolean value) {
        if (scaledToFit != value) {
            scaledToFit = value;
            if (value) {
                // If we have just switched to scaled-to-fit mode, we will have to reevaluate our scroll-bars.
                requestHeight(getViewportHeight());
            }
            renderRequired = true;
            repaint();
        }
    }

    @Override
    public boolean isScaledToFit() {
        return scaledToFit;
    }

    /**
     * Check to see if the frame needs to be resized. Only required if we're not
     * using scaledToFit mode, and thus have a pre-existing notion of what the
     * unit-height (or interval height) should be.
     *
     * @return true if pane needs to be resized
     */
    @Override
    public boolean needsToResize() {
        if (!scaledToFit) {
            int currentHeight = getHeight();
            int expectedHeight = (int) ((yMax - yMin) * unitHeight);

            expectedHeight = Math.max(expectedHeight, parentFrame.getFrameLandscape().getHeight());
            if (expectedHeight != currentHeight) {
                requestHeight(expectedHeight);
                return true;
            }
        }
        return false;
    }

    /**
     * Update the display of ymax. Overridden by VariantGraphPanes to show the
     */
    protected void updateYMax() {
        parentFrame.updateYMax(yMax);
    }
}
