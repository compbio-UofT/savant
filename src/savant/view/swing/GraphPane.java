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

import savant.selection.PopupThread;
import savant.selection.PopupPanel;
import com.jidesoft.popup.JidePopup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.DrawModeController;
import savant.controller.RangeController;
import savant.controller.event.GraphPaneChangeEvent;
import savant.util.*;
import savant.util.DrawingInstructions;
import savant.view.swing.interval.BAMViewTrack;
import savant.view.swing.util.GlassMessagePane;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import savant.api.adapter.ModeAdapter;
import savant.controller.GraphPaneController;
import savant.controller.ReferenceController;
import savant.controller.event.GraphPaneChangeListener;
import savant.data.types.GenericContinuousRecord;
import savant.data.types.Record;
import savant.settings.ColourSettings;

/**
 *
 * @author mfiume
 */
public class GraphPane extends JPanel implements KeyListener, MouseWheelListener, MouseListener, MouseMotionListener, GraphPaneChangeListener {

    /**
     * VARIABLES
     */

    private static Log log = LogFactory.getLog(GraphPane.class);

    private List<TrackRenderer> trackRenderers;
    private List<ViewTrack> tracks;
    private Frame parentFrame;

    private int mouse_x = 0;
    private int mouse_y = 0;

    /** min / max axis values */
    private long xMin;
    private long xMax;
    private long yMin;
    private long yMax;
    private double unitWidth;
    private double unitHeight;

    private boolean isOrdinal = false;
    private boolean isYGridOn = true;
    private boolean isXGridOn = true;
    private boolean mouseInside = false;

    // Locking
    private boolean isLocked = false;
    private Range lockedRange;

    /** Selection Variables */
    private int y1, y2;
    private int x, y, w, h;
    private boolean isDragging = false;

    //scrolling...
    private BufferedImage bufferedImage;
    private Range prevRange = null;
    private ModeAdapter prevDrawMode = null;
    private Dimension prevSize = null;
    private String prevRef = null;
    public boolean paneResize = false;
    public int newHeight;
    private int oldWidth = -1;
    private int oldHeight = -1;
    private int newScroll = 0;
    private boolean renderRequired = false;
    //private int buffTop = -1;
    //private int buffBottom = -1;
    private int posOffset = 0;
    private boolean forcedHeight = false;

    //dragging
    private int startX;
    private int startY;
    private long baseX;
    private int initialScroll;
    private boolean panVert = false;

    //popup
    public Thread popupThread;
    public JidePopup jp = new JidePopup();
    private Record currentOverRecord = null;
    private Shape currentOverShape = null;
    private boolean popupVisible = false;
    private JPanel popPanel;
    private boolean mouseWheel = false; //used by popupThread

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        //System.out.println("Key pressed");
        this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void keyReleased(KeyEvent e) {
        //System.out.println("Key pressed");
        resetCursor();
    }

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
    public GraphPane(Frame parent) {
        this.parentFrame = parent;

        trackRenderers = new ArrayList<TrackRenderer>();
        tracks = new ArrayList<ViewTrack>();
        this.setDoubleBuffered(true);
        addMouseListener( this ); // listens for own mouse and
        addMouseMotionListener( this ); // mouse-motion events
        //addKeyListener( this );
        this.getInputMap().allKeys();
        addMouseWheelListener(this);

        popupThread = new Thread(new PopupThread(this));
        popupThread.start();

        //initContextualMenu();

        ((GraphPaneController) GraphPaneController.getInstance()).addBookmarksChangedListener(this);
    }

    /**
     * GRAPHPANE CHANGE LISTENER
     */

    public void graphpaneChangeReceived(GraphPaneChangeEvent event) {
        GraphPaneController gpc = GraphPaneController.getInstance();
        this.resetFrameLayers();
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
    public Dimension render(Graphics g) {
        return render(g, new Range(xMin,xMax), null);
    }

    public Dimension render(Graphics g, Range xRange) {
        return render(g, xRange, null);
    }

    public Dimension render(Graphics g, Range xRange, Range yRange) {

        double oldUnitHeight = unitHeight;
        long oldYMax = yMax;

        GraphPaneController gpc = GraphPaneController.getInstance();
        int x1 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getFrom(), this.getWidth(), new Range(this.xMin, this.xMax));
        int x2 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getTo(), this.getWidth(), new Range(this.xMin, this.xMax));

        if (gpc.isPanning() && !this.isLocked()) { g.translate(x2-x1, 0); }

        long minYRange = Long.MAX_VALUE;
        long maxYRange = Long.MIN_VALUE;
        isYGridOn = false;
        for (TrackRenderer tr: trackRenderers) {

            // ask renderers for extra info on range; consolidate to maximum Y range
            AxisRange axisRange = (AxisRange)tr.getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);

            long axisYMin = axisRange.getYMin();
            long axisYMax = axisRange.getYMax();
            if (axisYMin < minYRange) minYRange = axisYMin;
            if (axisYMax > maxYRange) maxYRange = axisYMax;

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

        Graphics2D g3;
        ModeAdapter currentMode = this.parentFrame.getTracks().get(0).getDrawMode();
        //BufferedImage bf1;

        //boolean sameRange = (prevRange != null && RangeController.getInstance().getRange().equals(prevRange));
        boolean sameRange = (prevRange != null && xRange.equals(prevRange));
        boolean sameMode = ((currentMode == null && prevDrawMode == null) ||
                (prevDrawMode != null && currentMode.equals(prevDrawMode)));
        boolean sameSize = (prevSize != null && this.getSize().equals(prevSize) && 
                this.parentFrame.getFrameLandscape().getWidth() == oldWidth &&
                this.getParentFrame().getFrameLandscape().getHeight() == oldHeight);
        boolean sameRef = prevRef != null && ReferenceController.getInstance().getReferenceName().equals(prevRef);

        boolean withinScrollBounds = this.bufferedImage != null &&
                (((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().getValue() >= this.getOffset()) &&
                (((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().getValue() < this.getOffset() + this.parentFrame.scrollPane.getViewport().getHeight() * 2);

        //bufferedImage stores the current graphic for future use. If nothing
        //has changed in the track since the last render, bufferedImage will
        //be used to redraw the current view. This method allows for fast repaints
        //on tracks where nothing has changed (panning, selection, plumbline,...)

        //if nothing has changed draw buffered image
        if(sameRange && sameMode && sameSize && sameRef && !this.renderRequired && withinScrollBounds){

            g.drawImage(bufferedImage, 0, this.getOffset(), this);
            
            if(this.currentOverShape != null){
                //temporarily shift the origin
                ((Graphics2D)g).translate(0, this.getOffset());
                if(currentMode != null && currentMode.getName().equals("MATE_PAIRS")){
                    g.setColor(Color.red);
                    ((Graphics2D)g).draw(currentOverShape);
                } else {
                    //g.setColor(Color.red);
                    g.setColor(new Color(255,0,0,200));
                    ((Graphics2D) g).fill(currentOverShape);
                    if(currentOverShape.getBounds() != null &&
                            currentOverShape.getBounds().getWidth() > 5 &&
                            currentOverShape.getBounds().getHeight() > 3){
                        g.setColor(Color.BLACK);
                        ((Graphics2D)g).draw(currentOverShape);
                    }
                }
                //shift origin back
                ((Graphics2D)g).translate(0, -1 * this.getOffset());
            }
            renderCurrentSelected(g);

            //force unitHeight from last render
            unitHeight = oldUnitHeight;
            yMax = oldYMax;

            this.parentFrame.commandBar.repaint();

            return this.getSize();


        //otherwise prepare for new render
        } else {

            renderRequired = false;
            this.bufferedImage = new BufferedImage(this.getWidth(), Math.min(this.getHeight(), this.parentFrame.scrollPane.getViewport().getHeight()*3), BufferedImage.TYPE_INT_RGB);
            if(this.forcedHeight){
                this.bufferedImage = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
            }
            if(this.bufferedImage.getHeight() == this.getHeight()){
                setOffset(0);
            } else {
                setOffset(((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().getValue() - this.parentFrame.scrollPane.getViewport().getHeight());
            }
            
            g3 = this.bufferedImage.createGraphics();
            prevRange = RangeController.getInstance().getRange();
            prevSize = this.getSize();
            prevDrawMode = this.parentFrame.getTracks().get(0).getDrawMode();
            prevRef = ReferenceController.getInstance().getReferenceName();
        }

        renderBackground(g3);

        /*
        // Get current time
        long start = System.currentTimeMillis();
         */

        for (TrackRenderer tr : trackRenderers) {


            // change renderers' drawing instructions to reflect consolidated YRange
            tr.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(xRange, consolidatedYRange));

            //System.out.println("\tRendering : " + tr.getClass());
            tr.render(g3, this);
            //System.out.println("\tDone");
        }

        drawMaxYPlotValue(g3);
        renderSides(g3);

        //if a change has occured that affects scrollbar...
        if(this.paneResize){

            paneResize = false;

            //get old scroll position
            int oldScroll = ((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().getValue();
            int oldViewHeight = ((JViewport)this.getParent().getParent()).getHeight();
            int oldBottomHeight = oldHeight - oldScroll - oldViewHeight;

            //change size of current frame
            Frame frame = this.getParentFrame();
            frame.getFrameLandscape().setPreferredSize(new Dimension(this.getWidth(), newHeight));
            this.setPreferredSize(new Dimension(frame.getFrameLandscape().getWidth()-2, newHeight));
            frame.getFrameLandscape().setSize(new Dimension(frame.getFrameLandscape().getWidth(), newHeight));
            this.setSize(new Dimension(frame.getFrameLandscape().getWidth(), newHeight));
            this.revalidate();

            //scroll so that bottom matches previous view
            newScroll = newHeight - oldViewHeight - oldBottomHeight;

            return new Dimension(new Dimension(frame.getFrameLandscape().getWidth()-2, newHeight));

        } else {

            if(newScroll != -1){
                ((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().setValue(newScroll);
                newScroll = -1;
            }
        }

        oldWidth = this.getParentFrame().getFrameLandscape().getWidth();
        oldHeight = this.getParentFrame().getFrameLandscape().getHeight();

        /*
        // Get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis()-start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis/1000F;

        System.out.println("\tRendering of " + tracks.get(0).getName() + " took " + elapsedTimeSec + " seconds");
         */

        //bufferedImage = bf1;
        g.drawImage(bufferedImage, 0, this.getOffset(), this);
        renderCurrentSelected(g);
        //drawMaxYPlotValue(g3);
        this.parentFrame.commandBar.repaint();

        return this.getSize();

    }

    private void renderCurrentSelected(Graphics g){
        //temporarily shift the origin
        ((Graphics2D)g).translate(0, this.getOffset());
        TrackRenderer tr = null;
        for(int i = 0; i < this.trackRenderers.size(); i++){
            tr = this.trackRenderers.get(i);
            if(tr.hasMappedValues()){
                break;
            }
        }
        if(!tr.hasMappedValues())return;
        List<Shape> currentSelected = tr.getCurrentSelectedShapes(this);
        if(!currentSelected.isEmpty()){
            Graphics2D g2 = (Graphics2D) g;
            boolean arcMode = false;
            if(this.parentFrame.getTracks().get(0).getDrawMode() != null){
                arcMode = this.parentFrame.getTracks().get(0).getDrawMode().getName().equals("MATE_PAIRS");
            }
            for(int i = 0; i < currentSelected.size(); i++){
                Shape selectedShape = currentSelected.get(i);
                if(arcMode){
                    g2.setColor(Color.GREEN);
                    g2.draw(selectedShape);
                } else {
                    //g2.setColor(Color.GREEN);
                    g2.setColor(new Color(0,255,0,150));
                    g2.fill(selectedShape);
                    if(selectedShape.getBounds().getWidth() > 5){
                        g2.setColor(Color.BLACK);
                        g2.draw(selectedShape);
                    }
                }
            }
        }
        //shift the origin back
        ((Graphics2D)g).translate(0, -1 * this.getOffset());
    }

    /*
     * Call before a repaint to override bufferedImage repainting
     */
    public void setRenderRequired(){
        this.renderRequired = true;
    }

    //Force the bufferedImage to contain entire height at current range
    //Make sure you unforce immediately after!
    public void forceFullRender(){
        this.forcedHeight = true;
    }

    public void unforceFullRender(){
        this.forcedHeight = false;
    }

    private void setOffset(int offset){
        this.posOffset = offset;
    }

    public int getOffset(){
        return this.posOffset;
    }

    private void drawMaxYPlotValue(Graphics g){
        if (this.isYGridOn && this.getOffset() == 0) {
            Graphics2D g2 = (Graphics2D) g;
            Font smallFont = new Font("Sans-Serif", Font.PLAIN, 10);
            g2.setColor(ColourSettings.getPointLine());
            String maxPlotString = "ymax=" + yMax;
            g2.setFont(smallFont);
            Rectangle2D stringRect = smallFont.getStringBounds(maxPlotString, g2.getFontRenderContext());
            g2.drawString(maxPlotString, (int)(getWidth()-stringRect.getWidth()-20), (int)(stringRect.getHeight() + 5));
        }
    }

    public void setPaneResize(boolean resized){
        this.paneResize = resized;
    }

    int rendercount = 0;
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        render(g);

        GraphPaneController gpc = GraphPaneController.getInstance();

        /* AIMING ADJUSTMENTS */
        if (gpc.isAiming() && mouseInside) {
            g.setColor(Color.BLACK);
            Font thickfont = new Font("Arial", Font.BOLD, 15);
            g.setFont(thickfont);
            long genome_x = gpc.getMouseXPosition();
            long genome_y = gpc.getMouseYPosition();
            String target = "";
            target += "X: " + MiscUtils.numToString(genome_x);
            target += (genome_y == -1) ? "" : " Y: " + MiscUtils.numToString(genome_y);

            g.drawLine(mouse_x, 0, mouse_x, this.getHeight());
            if (genome_y != -1) g.drawLine(0, mouse_y, this.getWidth(), mouse_y);
            g.drawString(target,
                    mouse_x + 5,
                    mouse_y - 5);
        }

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
              this.x, this.y-10, this.w, this.h+10);
            g2d.setColor (Color.gray);
            g2d.setStroke (new BasicStroke(
              1f,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND,
              3f,
              new float[] {4f},
              4f));
            g2d.draw(rectangle);

            if (gpc.isZooming()) {
                g.setColor(ColourSettings.getGraphPaneZoomFill());
            } else if (gpc.isSelecting()) {
                g.setColor(ColourSettings.getGraphPaneSelectionFill());
            }
            g.fillRect(this.x, this.y, this.w, this.h);
        }

        /** PLUMBING ADJUSTMENTS */
        if (gpc.isPlumbing()) {
            g.setColor(Color.BLACK);
            int spos = MiscUtils.transformPositionToPixel(GraphPaneController.getInstance().getMouseXPosition(), this.getWidth(), this.getHorizontalPositionalRange());
            g.drawLine(spos, 0, spos, this.getHeight());
            int rpos = MiscUtils.transformPositionToPixel(GraphPaneController.getInstance().getMouseXPosition()+1, this.getWidth(), this.getHorizontalPositionalRange());
            g.drawLine(rpos, 0, rpos, this.getHeight());
        }

        /** SPOTLIGHT */
        if (gpc.isSpotlight() && !gpc.isZooming()) {

            long center = gpc.getMouseXPosition();
            long left = center - gpc.getSpotlightSize()/2;
            long right = left + gpc.getSpotlightSize();
            
            g.setColor(new Color(0,0,0,200));

            int xt = MiscUtils.transformPositionToPixel(left, this.getWidth(), this.getHorizontalPositionalRange());
            int yt = MiscUtils.transformPositionToPixel(right, this.getWidth(), this.getHorizontalPositionalRange());

            // draw left of spotlight
            if (left >= this.getHorizontalPositionalRange().getFrom()) {
                g.fillRect(0, 0, MiscUtils.transformPositionToPixel(left, this.getWidth(), this.getHorizontalPositionalRange()), this.getHeight());
            }
            // draw right of spotlight
            if (right <= this.getHorizontalPositionalRange().getTo()) {
                int pix = MiscUtils.transformPositionToPixel(right, this.getWidth(), this.getHorizontalPositionalRange());
                g.fillRect(pix, 0, this.getWidth()-pix, this.getHeight());
            }
        }
        
        if (this.isLocked()) {
            GlassMessagePane.draw((Graphics2D) g, this, "Locked", 300);
        }

        GraphPaneController.getInstance().delistRenderingGraphpane(this);
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
    public void renderBackground(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        Font smallFont = new Font("Sans-Serif", Font.PLAIN, 10);

        Graphics2D g2d0 = (Graphics2D)g;

            // Paint a gradient from top to bottom
            GradientPaint gp0 = new GradientPaint(
                0, 0, ColourSettings.getGraphPaneBackgroundTop(),
                0, this.getHeight(), ColourSettings.getGraphPaneBackgroundBottom());

            g2d0.setPaint( gp0 );
            g2d0.fillRect( 0, 0, this.getWidth(), this.getHeight() );

        if (this.isXGridOn) {

            int numseparators = (int) Math.ceil(Math.log(xMax-xMin));

            if (numseparators != 0) {
                int width = this.getWidth();
                double separation = width / numseparators;


                g2.setColor(ColourSettings.getAxisGrid());
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

                g2.setColor(ColourSettings.getAxisGrid());
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
    public double getWidth(long len) {
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
    public double transformXPos(long pos) {
        pos -= xMin;
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

    /**
     * Set the number of pixels equal to one graph unit of height.
     */
    public void setUnitHeight(int height) {
        unitHeight = height;
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

        this.setMouseWheel(true);
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

        this.trySelect(event.getPoint());

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

        baseX = MiscUtils.transformPixelToPosition(x1, this.getWidth(), this.getHorizontalPositionalRange());
        //initialScroll = ((JScrollPane)this.getParent().getParent()).getVerticalScrollBar().getValue();
        initialScroll = ((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().getValue();
        
        Point l = event.getLocationOnScreen();
        startX = l.x;
        startY = l.y;

        //startX = event.getX();
        //startY = event.getY();

        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setMouseClickPosition(MiscUtils.transformPixelToPosition(x1, this.getWidth(), this.getHorizontalPositionalRange()));
        this.resetFrameLayers();
    }

    public void resetCursor() {
        //GraphPaneController gpc = GraphPaneController.getInstance();
        //if (gpc.isAiming()) {
        //    this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        //} else {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        //}
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

        resetCursor();

        int x1 = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getFrom(), this.getWidth(), this.getHorizontalPositionalRange());

        if (gpc.isPanning()) {
            
            if(!panVert){
                RangeController rc = RangeController.getInstance();
                Range r = rc.getRange();
                int shiftVal = (int) (Math.round((x1-x2) / this.getUnitWidth()));

                Range newr = new Range(r.getFrom()+shiftVal,r.getTo()+shiftVal);
                rc.setRange(newr);
            }

            this.getParentFrame().commandBar.setVisible(true);

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
            TrackRenderer tr = null;
            for(int i = 0; i < this.trackRenderers.size(); i++){
                tr = this.trackRenderers.get(i);
                if(tr.hasMappedValues()){
                    break;
                }
            }
            if(tr.hasMappedValues()){
                boolean repaintNeeded = tr.rectangleSelect(new Rectangle2D.Double(this.x, this.y, this.w, this.h));
                if(repaintNeeded) this.repaint();
            }
        }

        this.parentFrame.tempShowCommands();
        this.isDragging = false;
        setMouseModifier(event);

        gpc.setMouseReleasePosition(MiscUtils.transformPixelToPosition(x2, this.getWidth(), this.getHorizontalPositionalRange()));
        this.resetFrameLayers();
    }



    /**
     * {@inheritDoc}
     */
    public void mouseEntered( final MouseEvent event ) {
        resetCursor();
        mouseInside = true;
        setMouseModifier(event);
        hidePopup();
       // this.resetFrameLayers();
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited( final MouseEvent event ) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        setMouseModifier(event);

        mouseInside = false;
        GraphPaneController.getInstance().setMouseXPosition(-1);
        GraphPaneController.getInstance().setMouseYPosition(-1);
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

        this.getParentFrame().commandBar.setVisible(false);

        //check if scrollbar is present (only vertical pan if present)
        //boolean scroll = ((JScrollPane)this.getParent().getParent()).getVerticalScrollBar().isVisible();
        boolean scroll = ((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().isVisible();

        if(scroll){

            //get new points
            Point l = event.getLocationOnScreen();
            int currX = l.x;
            int currY = l.y;

            //magnitude
            int magX = Math.abs(currX - startX);
            int magY = Math.abs(currY - startY);

            if(magX >= magY){
                //pan horizontally, reset vertical pan
                panVert = false;
                gpc.setMouseReleasePosition(MiscUtils.transformPixelToPosition(x2, this.getWidth(), this.getHorizontalPositionalRange()));
                //((JScrollPane)this.getParent().getParent()).getVerticalScrollBar().setValue(initialScroll);
                ((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().setValue(initialScroll);
            } else {
                //pan vertically, reset horizontal pan
                panVert = true;
                gpc.setMouseReleasePosition(baseX);
                //((JScrollPane)this.getParent().getParent()).getVerticalScrollBar().setValue(initialScroll - (currY - startY));
                ((JScrollPane)this.getParent().getParent().getParent()).getVerticalScrollBar().setValue(initialScroll - (currY - startY));
            }
        } else {
            //pan horizontally
            panVert = false;
            gpc.setMouseReleasePosition(MiscUtils.transformPixelToPosition(x2, this.getWidth(), this.getHorizontalPositionalRange()));
        }
        
        this.resetFrameLayers();
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved( final MouseEvent event ) {

        mouse_x = event.getX();
        mouse_y = event.getY();

        // update the GraphPaneController's record of the mouse position
        GraphPaneController.getInstance().setMouseXPosition(MiscUtils.transformPixelToPosition(event.getX(), this.getWidth(), this.getHorizontalPositionalRange()));
        if (this.isOrdinal()) {
            GraphPaneController.getInstance().setMouseYPosition(-1);
        } else {
            GraphPaneController.getInstance().setMouseYPosition(MiscUtils.transformPixelToPosition(this.getHeight() - event.getY(), this.getHeight(), new Range(this.yMin, this.yMax)));
        }
        GraphPaneController.getInstance().setSpotlightSize(this.getHorizontalPositionalRange().getLength());
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

    public Range getHorizontalPositionalRange() {
        return new Range(this.xMin, this.xMax);
    }

    public Range getVerticalPositionalRange() {
        return new Range(this.yMin, this.yMax);
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
            bamViewTrack.prepareForRendering(ReferenceController.getInstance().getReferenceName() , RangeController.getInstance().getRange());
            repaint();
        } catch (Throwable e) {
            log.error("Unexpected exception while preparing to render track " + e.getMessage());
        }


    }

    private void resetFrameLayers(){
        for(int i = 0; i < this.tracks.size(); i++){
            this.tracks.get(i).getFrame().resetLayers();
        }
    }

    public Frame getParentFrame(){
        return this.parentFrame;
    }

    public void setIsYGridOn(boolean value){
        this.isYGridOn = value;
    }

    public void setBufferedImage(BufferedImage bi){
        this.bufferedImage = bi;
    }

    //POPUP
    public void tryPopup(Point p){

        Point p_offset = new Point(p.x, p.y - this.getOffset());

        //get shape
        int trackNum = 0;
        Map<Record, Shape> map = null;
        for(int i = 0; i < this.trackRenderers.size(); i++){
            map = this.trackRenderers.get(i).searchPoint(p_offset);
            if(map!=null){
                trackNum = i;
                break;
            }
        }
        if(map==null){
            currentOverShape = null;
            currentOverRecord = null;
            return;
        }

        /** XXX: This line is here to get around what looks like a bug in the 1.6.0_20 JVM for Snow Leopard
         * which causes the mouseExited events not to be triggered sometimes. We hide the popup before
         * showing another. This needs to be done exactly here: after we know we have a new popup to show
         * and before we set currentOverRecord. Otherwise, it won't work.
         */
        hidePopup();

        currentOverRecord = (Record)map.keySet().toArray()[0];
        currentOverShape = map.get(currentOverRecord);
        if(currentOverRecord.getClass().equals(GenericContinuousRecord.class)){
            currentOverShape = TrackRenderer.continuousRecordToEllipse(this, currentOverRecord);
        }

        this.createJidePopup();
        PopupPanel pp = PopupPanel.create(this, this.tracks.get(0).getDrawMode(), this.tracks.get(trackNum).getDataSource().getDataFormat(), currentOverRecord);
        if(pp != null){
            popPanel.add(pp, BorderLayout.CENTER);
            Point p1 = (Point)p.clone();
            SwingUtilities.convertPointToScreen(p1, this);
            this.jp.showPopup(p1.x -2, p1.y -2);
            popupVisible = true;
        }
        this.repaint();
        

    }

    public void hidePopup(){
        if(this.popupVisible){
            popupVisible = false;
            jp.hidePopupImmediately();
            currentOverShape = null;
            currentOverRecord = null;
            this.repaint();
        }
    }

    public void trySelect(Point p){

        Point p_offset = new Point(p.x, p.y - this.getOffset());

        int trackNum = 0;
        Map<Record, Shape> map = null;
        for(int i = 0; i < this.trackRenderers.size(); i++){
            map = this.trackRenderers.get(i).searchPoint(p_offset);
            if(map!=null){
                trackNum = i;
                break;
            }
        }
        if(map==null){
            return;
        }

        Record o = (Record)map.keySet().toArray()[0];
        this.trackRenderers.get(trackNum).addToSelected(o);
        
        this.repaint();

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

        jp.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
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

    /*
     * Set whether the mouseWheel recently used. 
     * This is used by PopupThread to determine whether a popup should appear. 
     */
    public void setMouseWheel(boolean value){
        this.mouseWheel = value;
    }

    public boolean getMouseWheel(){
        return this.mouseWheel;
    }

}
