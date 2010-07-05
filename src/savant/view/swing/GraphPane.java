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

package savant.view.swing;

import java.awt.geom.Rectangle2D.Double;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.DrawModeController;
import savant.controller.RangeController;
import savant.controller.event.graphpane.GraphPaneChangeEvent;
import savant.model.FileFormat;
import savant.model.view.AxisRange;
import savant.model.view.DrawingInstructions;
import savant.model.view.Mode;
import savant.util.Range;
import savant.view.swing.interval.BAMViewTrack;
import savant.view.swing.util.GlassMessagePane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import savant.controller.GraphPaneController;
import savant.controller.event.graphpane.GraphPaneChangeListener;
import savant.util.MiscUtils;

/**
 *
 * @author mfiume
 */
public class GraphPane extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener, GraphPaneChangeListener {

    /**
     * VARIABLES
     */

    private static Log log = LogFactory.getLog(GraphPane.class);

    private List<TrackRenderer> trackRenderers;
    private List<ViewTrack> tracks;

    /** min / max axis values */
    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;
    private double unitWidth;
    private double unitHeight;

    private boolean isOrdinal = false;
    private boolean isYGridOn = true;
    private boolean isXGridOn = true;

    // Popup menu
    //private JPopupMenu menu;

    // Locking
    private boolean isLocked = false;
    private Range lockedRange;

    /** Selection Variables */
    //private int x1, x2;
    private int y1, y2;
    private int x, y, w, h;
    private boolean isDragging = false;

    // mouse and key modifiers
    private enum mouseModifier { NONE, LEFT, MIDDLE, RIGHT };
    private mouseModifier mouseMod = mouseModifier.LEFT;

    private enum keyModifier { DEFAULT, CTRL, SHIFT, META, ALT; };
    private keyModifier keyMod = keyModifier.DEFAULT;

    // let's behave nicely for the appropriate platform
    private static String os = System.getProperty("os.name").toLowerCase();
    private static boolean mac = os.contains("mac");

    /**
     * CONSTRUCTOR
     */
    public GraphPane() {
        trackRenderers = new ArrayList<TrackRenderer>();
        tracks = new ArrayList<ViewTrack>();
        this.setDoubleBuffered(true);
        addMouseListener( this ); // listens for own mouse and
        addMouseMotionListener( this ); // mouse-motion events
        //addKeyListener( this );
        this.getInputMap().allKeys();
        addMouseWheelListener(this);

        //initContextualMenu();

        ((GraphPaneController) GraphPaneController.getInstance()).addFavoritesChangedListener(this);
    }

    /**
     * CONTEXT MENU (WHEN USER RIGHT CLICKS GP)
     */

    /*private void initContextualMenu() {
        menu = new JPopupMenu();

        JMenuItem lockMI = new JCheckBoxMenuItem("Lock");
        lockMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchLocked();
            }
        });

        menu.add ( lockMI );

        menu.addSeparator();
    }*/

    /**
     * GRAPHPANE CHANGE LISTENER
     */

    public void graphpaneChangeReceived(GraphPaneChangeEvent event) {
        //repaint();
        GraphPaneController gpc = GraphPaneController.getInstance();
        if(gpc.isPanning() || gpc.isChanged() || gpc.isPlumbing() || gpc.isSpotlight()) this.resetFrameLayers();
    }

    /**
     * TRACKS AND RENDERERS
     */
    

    /**
     * Add a track renderer to the GraphPane
     *
     * @param trackRenderer the renderer to add
     */
    public void addTrackRenderer(TrackRenderer trackRenderer) {
        trackRenderers.add(trackRenderer);
        this.setIsOrdinal(trackRenderer.isOrdinal());
        setYRange(trackRenderer.getDefaultYRange());
    }

    /**
     * Add a view track to the graph pane
     *
     * @param track - the ViewTrack to add
     */
    public void addTrack(ViewTrack track) {
        tracks.add(track);
        /*JMenu trackMenu = new JMenu(track.getName());
        JMenu modeMenu = new JMenu("Change Display Mode");
        List<Mode> viewModes = track.getDrawModes();
        if (viewModes.isEmpty()) {
            modeMenu.setEnabled(false);
        }
        else {
            ButtonGroup modeGroup = new ButtonGroup();
            for (Mode mode: viewModes) {
                JMenuItem changeModeMI = new JRadioButtonMenuItem(mode.getName());
                if (mode == track.getDefaultDrawMode()) {
                    changeModeMI.setSelected(true);
                }
                final ViewTrack innerTrack = track;
                final Mode innerMode = mode;
                changeModeMI.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        switchMode(innerTrack, innerMode);
                    }
                });
                modeGroup.add(changeModeMI);
                modeMenu.add(changeModeMI);
            }
        }
        trackMenu.add(modeMenu);
        // if it's a BAM track, add a menu item to allow changing the display parameters
        if (track.getDataType() == FileFormat.INTERVAL_BAM) {
            JMenuItem bamParamChangeMI = new JMenuItem("Change Arc Parameters...");
            final BAMViewTrack innerTrack = (BAMViewTrack)track;
            bamParamChangeMI.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getBAMParams(innerTrack);
                }
            });
            trackMenu.add(bamParamChangeMI);
        }
        menu.add(trackMenu);*/
    }

    /**
     * DRAWING
     */

    /**
     * Render the contents of the graphpane. Includes drawing a common
     * background for all tracks.
     *
     * @param g The Graphics object into which to draw.
     */
    public void render(Graphics g) {
        render(g, new Range(xMin,xMax), null);
    }

    public void render(Graphics g, Range xRange) {
        render(g, xRange, null);
    }

    public void render(Graphics g, Range xRange, Range yRange) {

        GraphPaneController gpc = GraphPaneController.getInstance();
        int x1 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getFrom(), this.getWidth(), new Range(this.xMin, this.xMax));
        int x2 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getTo(), this.getWidth(), new Range(this.xMin, this.xMax));

        if (gpc.isPanning() && !this.isLocked()) { g.translate(x2-x1, 0); }

        int minYRange = Integer.MAX_VALUE;
        int maxYRange = Integer.MIN_VALUE;
        isYGridOn = false;
        for (TrackRenderer tr: trackRenderers) {
            // ask renderers for extra info on range; consolidate to maximum Y range
            AxisRange axisRange = (AxisRange)tr.getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);

            int yMin = axisRange.getYMin();
            int yMax = axisRange.getYMax();
            if (yMin < minYRange) minYRange = yMin;
            if (yMax > maxYRange) maxYRange = yMax;

            // ask renders if they want horizontal lines; if any say yes, draw them
            if (tr.hasHorizontalGrid()) {
                isYGridOn = true;
            }
        }
        setXRange(xRange);
        Range consolidatedYRange = new Range(minYRange, maxYRange);
        setYRange(consolidatedYRange);

        yMin = minYRange;
        yMax = maxYRange;

        renderBackground(g);

        /*
        // Get current time
        long start = System.currentTimeMillis();
         */

        for (TrackRenderer tr : trackRenderers) {
            // change renderers' drawing instructions to reflect consolidated YRange
            tr.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(xRange, consolidatedYRange));
            tr.render(g, this);
        }

        // draw max Y plot value
        if (this.isYGridOn) {
            Graphics2D g2 = (Graphics2D) g;
            Font smallFont = new Font("Sans-Serif", Font.PLAIN, 10);
            g2.setColor(BrowserDefaults.colorAccent);
            String maxPlotString = "ymax=" + Integer.toString(yMax);
            g2.setFont(smallFont);
            Rectangle2D stringRect = smallFont.getStringBounds(maxPlotString, g2.getFontRenderContext());
            g2.drawString(maxPlotString, (int)(getWidth()-stringRect.getWidth()-5), (int)(stringRect.getHeight() + 5));
        }

        /*
        // Get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis()-start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis/1000F;

        System.out.println("\tRendering of " + tracks.get(0).getName() + " took " + elapsedTimeSec + " seconds");
         */

        renderSides(g);
       
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        render(g);

        GraphPaneController gpc = GraphPaneController.getInstance();
        int x1 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getFrom(), this.getWidth(), new Range(this.xMin, this.xMax));
        int x2 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getTo(), this.getWidth(), new Range(this.xMin, this.xMax));

        int width = x1 - x2;
        int height = this.getHeight();// this.y1 - this.y2;

        this.w = Math.max(2 ,Math.abs( width ));
        this.h = Math.abs( height );
        this.x = width < 0 ? x1 : x2;
        this.y = 0; //height < 0 ? this.y1 : this.y2;

        /** PANNING ADJUSTMENTS */
        if (gpc.isPanning()) {}
        
        /** ZOOMING ADJUSTMENTS */
        else if (gpc.isZooming() || gpc.isSelecting()) {
            Graphics2D g2d = (Graphics2D)g;

            Rectangle2D rectangle =
              new Rectangle2D.Double (
              this.x, this.y, this.w, this.h);
            g2d.setColor (Color.gray);
            g2d.setStroke (new BasicStroke(
              1f,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND,
              3f,
              new float[] {4f},
              4f));
            g2d.draw (rectangle);

            if (gpc.isZooming()) {
                g.setColor(BrowserDefaults.colorGraphPaneZoomFill);
            } else if (gpc.isSelecting()) {
                g.setColor(BrowserDefaults.colorGraphPaneSelectionFill);
            }
            g.fillRect(this.x, this.y, this.w, this.h);
        }

        /** PLUMBING ADJUSTMENTS */
        if (gpc.isPlumbing()) {
            g.setColor(Color.BLACK);
            int spos = MiscUtils.transformPositionToPixel(GraphPaneController.getInstance().getMouseXPosition(), this.getWidth(), this.getPositionalRange());
            g.drawLine(spos, 0, spos, this.getHeight());
            int rpos = MiscUtils.transformPositionToPixel(GraphPaneController.getInstance().getMouseXPosition()+1, this.getWidth(), this.getPositionalRange());
            g.drawLine(rpos, 0, rpos, this.getHeight());
        }

        /** SPOTLIGHT */
        if (gpc.isSpotlight() && !gpc.isZooming()) {
            int center = gpc.getMouseXPosition();
            int left = center - gpc.getSpotlightSize()/2;
            int right = center + gpc.getSpotlightSize()/2;
            if (gpc.getSpotlightSize() == 1) { right = center + 1; }
            
            g.setColor(new Color(0,0,0,200));

            // draw left of spotlight
            if (left > this.getPositionalRange().getFrom()) {
                g.fillRect(0, 0, MiscUtils.transformPositionToPixel(left, this.getWidth(), this.getPositionalRange()), this.getHeight());
            }
            // draw right of spotlight
            if (right < this.getPositionalRange().getTo()) {
                int pix = MiscUtils.transformPositionToPixel(right, this.getWidth(), this.getPositionalRange());
                g.fillRect(pix, 0, this.getWidth()-pix, this.getHeight());
            }
        }
        
        if (this.isLocked()) {
            GlassMessagePane.draw((Graphics2D) g, this, "Locked", 300);
        }
    }

    /**
     * Render the sides of this graphpane
     * @param g The graphics object to use
     */
    private void renderSides(Graphics g) {
        //Color c = Color.black;
        //this.setBackground(c);
        g.setColor(this.getBackground());
        int w = this.getWidth();
        int h = this.getHeight();
        int mult = 1;
        g.fillRect(w, 0, w*mult, h);
        g.fillRect(-w*mult, 0, w*mult, h);
    }

    /**
     * Render the background of this graphpane
     * @param g The graphics object to use
     */
    private void renderBackground(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        Font smallFont = new Font("Sans-Serif", Font.PLAIN, 10);

        Graphics2D g2d0 = (Graphics2D)g;

            // Paint a gradient from top to bottom
            GradientPaint gp0 = new GradientPaint(
                0, 0, BrowserDefaults.colorGraphPaneBackgroundTop,
                0, this.getHeight(), BrowserDefaults.colorGraphPaneBackgroundBottom );

            g2d0.setPaint( gp0 );
            g2d0.fillRect( 0, 0, this.getWidth(), this.getHeight() );

        if (this.isXGridOn) {

            int numseparators = (int) Math.ceil(Math.log(xMax-xMin));

            if (numseparators != 0) {
                int width = this.getWidth();
                double separation = width / numseparators;


                g2.setColor(BrowserDefaults.colorAxisGrid);
                for (int i = 0; i <= numseparators; i++) {
                    g2.drawLine((int)Math.ceil(i*separation)+1, this.getHeight(), (int) Math.ceil(i*separation)+1, 0);
                }
            }
        }

        if (this.isYGridOn) {

            int numseparators = (int) Math.ceil(Math.log(yMax-yMin));

            if (numseparators != 0) {
                int height = this.getHeight();
                double separation = height / numseparators;

                g2.setColor(BrowserDefaults.colorAxisGrid);
                for (int i = 0; i <= numseparators; i++) {
                    g2.drawLine(0, (int)Math.ceil(i*separation)+1, this.getWidth(), (int) Math.ceil(i*separation)+1);
                }

            }
            // draw max Y plot value
            /*g2.setColor(BrowserDefaults.colorAccent);
            String maxPlotString = "ymax=" + Integer.toString(yMax);
            g2.setFont(smallFont);
            Rectangle2D stringRect = smallFont.getStringBounds(maxPlotString, g2.getFontRenderContext());
            g2.drawString(maxPlotString, (int)(getWidth()-stringRect.getWidth()-5), (int)(stringRect.getHeight() + 5));*/

        }
    }

    public List<TrackRenderer> getTrackRenderers() {
        return this.trackRenderers;
    }

    /**
     * FRAME DIMENSIONS AND X AND Y AXIS
     */

    /**
     * Set the graph units for the horizontal axis
     *
     * @param r an X range
     */
    public void setXRange(Range r) {
        if (r == null) {
            return;
        }

        //Savant.log("Setting x range to " + r);

        this.xMin = r.getFrom();
        this.xMax = r.getTo();
        setUnitWidth();
    }

    /**
     * Set the graph units for the vertical axis
     *
     * @param r a Y range
     */
    public void setYRange(Range r) {

        if (r == null) {
            return;
        }
        if (this.isOrdinal) {
            return;
        }

        //Savant.log("Setting y range to " + r);

        this.yMin = r.getFrom();
        this.yMax = r.getTo();
        setUnitHeight();
    }

    /**
     * Set the pane's vertical coordinate system to be 0-1
     *
     * @param b true for ordinal, false otherwise.
     */
    public void setIsOrdinal(boolean b) {
        this.isOrdinal = b;
        if (this.isOrdinal) {
            // don't call setYRange, because it's just going to return without doing anything
            this.yMin = 0;
            this.yMax = 1;
            setUnitHeight();
        }
    }

    /**
     *
     * @return  the number of pixels equal to one graph unit of width.
     */
    public double getUnitWidth() {
        return this.unitWidth;
    }

    /**
     * Transform a graph width into a pixel width
     *
     * @param len width in graph units
     * @return corresponding number of pixels
     */
    public double getWidth(int len) {
        return this.unitWidth * len;
    }

    /**
     *
     * @return the number of pixels equal to one graph unit of height.
     */
    public double getUnitHeight() {
        return this.unitHeight;
    }

    /**
     * Transform a graph height into a pixel height
     *
     * @param len height in graph units
     * @return corresponding number of pixels
     */
    public double getHeight(int len) {
        return this.unitHeight * len;
    }

    // TODO: why does one of these take an int and the other a double?
    /**
     * Transform a horizontal position in terms of graph units into a drawing coordinate
     *
     * @param pos position in graph coordinates
     * @return a corresponding drawing coordinate
     */
    public double transformXPos(int pos) {
        pos = pos - this.xMin;
        return pos * getUnitWidth();
    }

    /**
     * Transform a vertical position in terms of graph units into a drawing coordinate
     *
     * @param pos position in graph coordinates
     * @return a corresponding drawing coordinate
     */
    public double transformYPos(double pos) {
        pos = pos - this.yMin;
        return this.getHeight() - (pos * getUnitHeight());
    }

    /**
     * Set the number of pixels equal to one graph unit of width.
     */
    public void setUnitWidth() {
        Dimension d = this.getSize();
        unitWidth = (double) d.width / (xMax - xMin + 1);
    }

    /**
     * Set the number of pixels equal to one graph unit of height.
     */
    public void setUnitHeight() {
        Dimension d = this.getSize();
        unitHeight = (double) d.height / (yMax - yMin);
    }

    public boolean isOrdinal() {
        return this.isOrdinal;
    }


    /**
     * MOUSE EVENT LISTENER
     */

    /**
     * {@inheritDoc}
     */
    public void mouseWheelMoved(MouseWheelEvent e) {

       int notches = e.getWheelRotation();

       if (mac && e.isMetaDown() || e.isControlDown()) {
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
       this.resetFrameLayers();
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


    /**
     * {@inheritDoc}
     */
    public void mouseClicked( final MouseEvent event ) {

        if (event.getClickCount() == 2) {
            RangeController.getInstance().zoomInOnMouse();
            return;
        }

        setMouseModifier(event);

        this.resetFrameLayers();

        /*if (mac && event.isControlDown() || this.isRightClick()) {
            menu.show(event.getComponent(), event.getX(), event.getY());
        }*/

    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed( final MouseEvent event ) {

        setMouseModifier(event);
        
        this.requestFocus();
        
        int x1 = event.getX();
        if (x1 < 0) { x1 = 0; }
        if (x1 > this.getWidth()) { x1 = this.getWidth(); }
        this.y1 = event.getY();

        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setMouseClickPosition(MiscUtils.transformPixelToPosition(x1, this.getWidth(), this.getPositionalRange()));
        this.resetFrameLayers();
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased( final MouseEvent event ) {

        GraphPaneController gpc = GraphPaneController.getInstance();
        
        int x2 = event.getX();
        if (x2 < 0) { x2 = 0; }
        if (x2 > this.getWidth()) { x2 = this.getWidth(); }
        this.y2 = event.getY();

        //if (gpc.isSelecting()) {
         //   this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        //} else {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        //}

        int x1 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getFrom(), this.getWidth(), this.getPositionalRange());

        if (gpc.isPanning()) {

            RangeController rc = RangeController.getInstance();
            Range r = rc.getRange();
            int shiftVal = (int) (Math.round((x1-x2) / this.getUnitWidth()));

            Range newr = new Range(r.getFrom()+shiftVal,r.getTo()+shiftVal);
            rc.setRange(newr);

        } else if (gpc.isZooming()) {

            RangeController rc = RangeController.getInstance();
            Range r;
            if (this.isLocked()) {
                r = this.lockedRange;
            } else {
                r = rc.getRange();
            }
            int newMin = (int) Math.round(Math.min(x1, x2) / this.getUnitWidth());
            // some weirdness here, but it's to get around an off by one
            int newMax = (int) Math.max(Math.round(Math.max(x1, x2) / this.getUnitWidth())-1, newMin);
            Range newr = new Range(r.getFrom()+newMin,r.getFrom()+newMax);

            rc.setRange(newr);
        } else if (gpc.isSelecting()) {
            selectElementsInRectangle(new Rectangle2D.Double(this.x, this.y, this.w, this.h));
        }

        this.isDragging = false;
        setMouseModifier(event);

        gpc.setMouseReleasePosition(MiscUtils.transformPixelToPosition(x2, this.getWidth(), this.getPositionalRange()));
        this.resetFrameLayers();
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered( final MouseEvent event ) {
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMouseModifier(event);
       // this.resetFrameLayers();
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited( final MouseEvent event ) {
        setMouseModifier(event);
        //this.resetFrameLayers();
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged( final MouseEvent event ) {

        setMouseModifier(event);

        GraphPaneController gpc = GraphPaneController.getInstance();

        int x2 = event.getX();
        if (x2 < 0) { x2 = 0; }
        if (x2 > this.getWidth()) { x2 = this.getWidth(); }
        this.y2 = event.getY();

        this.isDragging = true;

        if (gpc.isPanning()) {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else if (gpc.isZooming() || gpc.isSelecting()) {
            this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }

        gpc.setMouseReleasePosition(MiscUtils.transformPixelToPosition(x2, this.getWidth(), this.getPositionalRange()));
        this.resetFrameLayers();
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved( final MouseEvent event ) {

        // update the GraphPaneController's record of the mouse position
        GraphPaneController.getInstance().setMouseXPosition(MiscUtils.transformPixelToPosition(event.getX(), this.getWidth(), this.getPositionalRange()));
        if (this.isOrdinal()) {
            GraphPaneController.getInstance().setMouseYPosition(-1);
        } else {
            GraphPaneController.getInstance().setMouseYPosition(MiscUtils.transformPixelToPosition(this.getHeight() - event.getY(), this.getHeight(), new Range(this.yMin, this.yMax)));
        }
        GraphPaneController.getInstance().setSpotlightSize(this.getPositionalRange().getLength());
    }

    /**
     * TALK TO GRAPHPANE CONTROLLER
     */

    private void tellModifiersToGraphPaneController() {
        GraphPaneController gpc = GraphPaneController.getInstance();
        setZooming(gpc);
        setPanning(gpc);
        setSelecting(gpc);
    }

    private void setZooming(GraphPaneController gpc) {
        gpc.setZooming(this.isDragging &&  ((isLeftClick() && isZoomModifierPressed()) || (isRightClick() && isZoomModifierPressed())));
    }

    private void setSelecting(GraphPaneController gpc) {
        gpc.setSelecting(this.isDragging &&  ((isLeftClick() && isSelectModifierPressed()) || (isRightClick() && isSelectModifierPressed())));
    }

    private void setPanning(GraphPaneController gpc) {
        gpc.setPanning(this.isDragging && isNoKeyModifierPressed());
    }

    /**
     * TRACK LOCKING
     */

    /**
     *
     * @return true if the track is locked, false o/w
     */
    public boolean isLocked() {
        return this.isLocked;
    }

    public void lock() {
        setIsLocked(true);
    }
    public void unLock() {
        setIsLocked(false);
    }
    
    public void switchLocked() {
        setIsLocked(!this.isLocked);
    }

    public void setIsLocked(boolean b) {
        this.isLocked = b;
        if (b) {
            RangeController rc = RangeController.getInstance();
            this.lockedRange = rc.getRange();
        } else {
            this.lockedRange = null;
        }
        this.repaint();
    }

    /**
     * RANGE
     * @return
     */

    public Range getPositionalRange() {
        return new Range(this.xMin, this.xMax);
    }


    /**
     * MODE SWITCHING
     */

    private void switchMode(final ViewTrack track, final Mode mode) {

        DrawModeController.getInstance().switchMode(track, mode);
        
//        try {
//            // TODO: this needs to get done in a separate thread and then schedule the repaint for later
//            track.prepareForRendering(RangeController.getInstance().getRange());
//            repaint();
//        } catch (Throwable e) {
//            log.error("Unexpected exception while preparing to render track " + e.getMessage());
//        }

    }

    public void getBAMParams(BAMViewTrack bamViewTrack) {
        // capture parameters needed to adjust display
        ViewTrack.captureBAMDisplayParameters(bamViewTrack);
        try {
            // TODO: this needs to get done in a separate thread and then schedule the repaint for later
            bamViewTrack.prepareForRendering(RangeController.getInstance().getRange());
            repaint();
        } catch (Throwable e) {
            log.error("Unexpected exception while preparing to render track " + e.getMessage());
        }


    }

    private void selectElementsInRectangle(Rectangle2D r) {
        System.out.println("Should select " + r);
        // TODO: paste code in here
    }

    private void resetFrameLayers(){
        for(int i = 0; i < this.tracks.size(); i++){
            this.tracks.get(i).getFrame().resetLayers();
        }
    }

}
