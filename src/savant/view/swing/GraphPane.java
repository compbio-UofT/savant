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

import savant.controller.FrameController;
import savant.controller.RangeController;
import savant.model.view.AxisRange;
import savant.model.view.DrawingInstructions;
import savant.model.view.Mode;
import savant.util.Range;
import savant.view.swing.util.GlassMessagePane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 *
 * @author mfiume
 */
public class GraphPane extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener, KeyListener {

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

    private
    // Popup menu
    JPopupMenu menu;

    // Locking
    private boolean isLocked = false;
    private Range lockedRange;

    /** Selection Variables */
    int x1, x2, y1, y2;
    int x, y, w, h;
    boolean isDragging = false;

    // let's behave nicely for the appropriate platform
    private static String os = System.getProperty("os.name").toLowerCase();
    private static boolean mac = os.contains("mac");

    public void mouseWheelMoved(MouseWheelEvent e) {

       int notches = e.getWheelRotation();

       if (mac && e.isMetaDown() || e.isControlDown()) {
           if (notches < 0) {
               RangeController rc = RangeController.getInstance();
               rc.zoomIn();
           } else {
               RangeController rc = RangeController.getInstance();
               rc.zoomOut();
           }
       }
       else {

           if (notches < 0) {
               RangeController rc = RangeController.getInstance();
               rc.shiftRangeLeft();
           } else {
               RangeController rc = RangeController.getInstance();
               rc.shiftRangeRight();
           }
       }
    }

    private void initContextualMenu() {
        menu = new JPopupMenu();

        /*
        JMenuItem undoMI = new JMenuItem("Undo Range Change");
        undoMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RangeController rc = RangeController.getInstance();
                rc.undoRangeChange();
            }
        });

        menu.add ( undoMI );
        
        JMenuItem redoMI = new JMenuItem("Redo Range Change");
        redoMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RangeController rc = RangeController.getInstance();
                rc.redoRangeChange();
            }
        });

        menu.add ( redoMI );

        JMenuItem addMI = new JMenuItem("Bookmark");
        addMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BookmarkController fc = BookmarkController.getInstance();
                fc.addCurrentRangeToBookmarks();
            }
        });

        menu.add ( addMI );

        menu.addSeparator();
         */

        JMenuItem lockMI = new JCheckBoxMenuItem("Lock");
        lockMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchLocked();
            }
        });
        
        menu.add ( lockMI );

        JMenuItem copyMI = new JMenuItem("Copy to clipboard");
        copyMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });

        menu.add ( copyMI );

        JMenuItem saveMI = new JMenuItem("Save image...");
        saveMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });

        menu.add ( saveMI );

        /*
        JMenuItem screenMI = new JMenuItem("Screen Capture");
        screenMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Savant.log("Taking screen...");
                ScreenShot.takeAndSave();
            }
        });

        menu.add ( screenMI );
         */

        //menu.add ( new JMenuItem ("Save As...") );

        menu.addSeparator();

        /*
        JMenuItem hideMI = new JMenuItem("Hide");

        hideMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hideFrame();
            }
        });
        menu.add(hideMI);

        JMenuItem closeMI = new JMenuItem("Close");

        closeMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeFrame();
            }
        });
        menu.add(closeMI);

        menu.addSeparator();
         */
    }

    boolean isLocked() {
        return this.isLocked;
    }

    private enum mouseModifier { NONE, LEFT, MIDDLE, RIGHT };
    private mouseModifier mouseMod = mouseModifier.LEFT;

    private enum keyModifier { DEFAULT, CTRL, SHIFT, META, ALT; };
    private keyModifier keyMod = keyModifier.DEFAULT;

    public GraphPane() {
        trackRenderers = new ArrayList<TrackRenderer>();
        tracks = new ArrayList<ViewTrack>();
        this.setDoubleBuffered(true);
        addMouseListener( this ); // listens for own mouse and
        addMouseMotionListener( this ); // mouse-motion events
        //addKeyListener( this );
        this.getInputMap().allKeys();
        addMouseWheelListener(this);

        initContextualMenu();
    }

    public void addTrackRenderer(TrackRenderer trackRenderer) {
        trackRenderers.add(trackRenderer);
        this.setIsOrdinal(trackRenderer.isOrdinal());
        setYRange(trackRenderer.getDefaultYRange());
    }

    public void addTrack(ViewTrack track) {
        tracks.add(track);
        JMenu trackMenu = new JMenu(track.getName());
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
        menu.add(trackMenu);
    }
    
    public void render(Graphics g) {
        render(g, new Range(xMin,xMax), null);
    }

    public void render(Graphics g, Range xRange) {
        render(g, xRange, null);
    }

    public void render(Graphics g, Range xRange, Range yRange) {

        if (isPanning() && !this.isLocked()) { g.translate(this.x2-this.x1, 0); }

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


        renderBackground(g);

        for (TrackRenderer tr : trackRenderers) {
            // change renderers' drawing instructions to reflect consolidated YRange
            tr.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(xRange, consolidatedYRange));
            tr.render(g, this);
        }

       
    }

    public void setXRange(Range r) {
        if (r == null) {
            return;
        }

        //Savant.log("Setting x range to " + r);

        this.xMin = r.getFrom();
        this.xMax = r.getTo();
        setUnitWidth();
    }

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

    public void setIsOrdinal(boolean b) {
        this.isOrdinal = b;
        if (this.isOrdinal) {
            // don't call setYRange, because it's just going to return without doing anything
            this.yMin = 0;
            this.yMax = 1;
            setUnitHeight();
        }
    }

    public double getUnitWidth() {
        return this.unitWidth;
    }

    public double getWidth(int len) {
        return this.unitWidth * len;
    }

    public double getUnitHeight() {
        return this.unitHeight;
    }

    public double getHeight(int len) {
        return this.unitHeight * len;
    }

    public double transformXPos(int pos) {
        pos = pos - this.xMin;
        return pos * getUnitWidth();
    }

    public double transformYPos(double pos) {
        pos = pos - this.yMin;
        //Savant.log("Transforming pos 1: height" + this.getHeight() + " subtract " +  (pos*getUnitHeight()));
        return this.getHeight() - (pos * getUnitHeight());
    }

    public void setUnitWidth() {
        Dimension d = this.getSize();
        unitWidth = (double) d.width / (xMax - xMin + 1);
    }

    public void setUnitHeight() {
        Dimension d = this.getSize();
        //Savant.log("Frame height: " + d.height);
        //Savant.log("Y Range " + (new Range(yMin, yMax)));
        //Savant.log("Unit Hieght set to: " + this.unitHeight);
        unitHeight = (double) d.height / (yMax - yMin);
    }

    public boolean isOrdinal() {
        return this.isOrdinal;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        render(g);

        if (this.x2 > this.x1) {
            //g.drawString( "Start : " + translatePixelToPosition(this.x1), this.x1 + 5, this.getHeight()/2 + 4 );
            //g.drawString( "End : " + translatePixelToPosition(this.x2), this.x2 + 5, this.getHeight()/2 + 4 );
        } else {
            //g.drawString( "End : " + translatePixelToPosition(this.x1), this.x1 + 5, this.getHeight()/2 + 4 );
            //g.drawString( "Start : " + translatePixelToPosition(this.x2), this.x2 + 5, this.getHeight()/2 + 4 );
        }
        //g.drawString( "length: " + this.w, this.x2 + 2, this.getHeight()/2 + 3);

        int width = this.x1 - this.x2;
        int height = this.getHeight();// this.y1 - this.y2;

        this.w = Math.max(2 ,Math.abs( width ));
        this.h = Math.abs( height );
        this.x = width < 0 ? this.x1
                : this.x2;
        this.y = 0; //height < 0 ? this.y1 : this.y2;


        if (this.isMovingPane()) {}
        else if (this.isPanning()) {}
        else if (this.isZooming()) {
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

            //g.setColor(new Color(255,200,200,255));
            //g.drawRect( this.x, this.y, this.w, this.h );

            g.setColor(BrowserDefaults.colorGraphPaneSelectionFill);
            g.fillRect(this.x, this.y, this.w, this.h);
        }
        //this.cords.setText( "w = " + this.w);


        if (this.isLocked()) {
            GlassMessagePane.draw((Graphics2D) g, this, "Locked", 300);
        }
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
            g2.setColor(BrowserDefaults.colorAccent);
            String maxPlotString = "ymax=" + Integer.toString(yMax);
            g2.setFont(smallFont);
            Rectangle2D stringRect = smallFont.getStringBounds(maxPlotString, g2.getFontRenderContext());
            g2.drawString(maxPlotString, (int)(getWidth()-stringRect.getWidth()-5), (int)(stringRect.getHeight() + 5));

        }
    }

    public List<TrackRenderer> getTrackRenderers() {
        return this.trackRenderers;
    }


    /** <SECTION> MOUSE EVENT LISTENER */

    // MouseListener event handlers // handle event when mouse released immediately after press
    public void mouseClicked( final MouseEvent event ) {

        setMouseModifier(event);

        //if (isMovingPane()) {
        //    Savant.log("MOVE");
            //this.getParent().dispatchEvent(event);
        //}
        //else
        if (this.isRightClick()) {
            Savant.log("MENU");
            menu.show(event.getComponent(), event.getX(), event.getY());
        }

        //this.mousePosition.setText( "Clicked at [" + event.getX() + ", " + event.getY() + "]" );
        Savant.log("Mouse clicked");
        //repaint();
    }

// handle event when mouse pressed
    public void mousePressed( final MouseEvent event ) {

        //Savant.log("Mouse pressed");

        setMouseModifier(event);
        
        this.requestFocus();

        /** Right clickt */
        if (isMovingPane()) {
            this.getParent().dispatchEvent(event);
            return;
        }
        
        this.x1 = event.getX();
        if (this.x1 < 0) { this.x1 = 0; }
        if (this.x1 > this.getWidth()) { this.x1 = this.getWidth(); }
        this.y1 = event.getY();

        //this.mousePosition.setText( "Pressed at [" + ( this.x1 ) + ", " + ( this.y1 ) + "]" );

        //this.recStart.setText( "Start:  [" + this.x1 + "]");

        //this.isNewRect = true;
        //this.isNewRect = false;
        repaint();
    }

// handle event when mouse released after dragging
    public void mouseReleased( final MouseEvent event ) {

        setMouseModifier(event);

        //Savant.log("Mouse released");

        if (isMovingPane()) {
            this.getParent().dispatchEvent(event);
            return;
        }

        this.x2 = event.getX();
        if (this.x2 < 0) { this.x2 = 0; }
        if (this.x2 > this.getWidth()) { this.x2 = this.getWidth(); }
        this.y2 = event.getY();
        //this.mousePosition.setText( "Released at [" + ( this.x2 ) + ", " + ( this.y2 ) + "]" );

        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        //this.recStop.setText( "End:  [" + this.x2 + "]" );

        if (this.isPanning()) {
            Savant.log("SHOULD PAN");

            RangeController rc = RangeController.getInstance();
            Range r = rc.getRange();
            int shiftVal = (int) (Math.round((this.x1-this.x2) / this.getUnitWidth()));

            Savant.log("X1: " + this.x1 + " X2: " + this.x2 + " Shift: " + shiftVal);

            Range newr = new Range(r.getFrom()+shiftVal,r.getTo()+shiftVal);
            Savant.log("Panning to " + newr);
            rc.setRange(newr);
            //return;
        } else if (this.isZooming()) {
            Savant.log("SHOULD ZOOM");

            RangeController rc = RangeController.getInstance();
            Range r;
            if (this.isLocked()) {
                r = this.lockedRange;
            } else {
                r = rc.getRange();
            }
            int newMin = (int) Math.round(Math.min(this.x1, this.x2) / this.getUnitWidth());
            // some weirdness here, but it's to get around an off by one
            int newMax = (int) Math.max(Math.round(Math.max(this.x1, this.x2) / this.getUnitWidth())-1, newMin);
            Range newr = new Range(r.getFrom()+newMin,r.getFrom()+newMax);
            Savant.log("Zooming to " + newr);
            rc.setRange(newr);
            //return;
        }

        this.isDragging = false;

        repaint();
    }

// handle event when mouse enters area
    public void mouseEntered( final MouseEvent event ) {
        
        setMouseModifier(event);
        
        // uncomment this line to ensure useer can use key and mouse panning
        //this.requestFocus();
    }

// handle event when mouse exits area
    public void mouseExited( final MouseEvent event ) {
        setMouseModifier(event);
        //this.mousePosition.setText( "Mouse outside window" );
        // repaint();
    }

// MouseMotionListener event handlers // handle event when user drags mouse with button pressed
    public void mouseDragged( final MouseEvent event ) {

        setMouseModifier(event);

        if (isMovingPane()) { 
            Savant.log("Moving");
            this.getParent().dispatchEvent(event);
            return;
        }

        this.x2 = event.getX();
        if (this.x2 < 0) { this.x2 = 0; }
        if (this.x2 > this.getWidth()) { this.x2 = this.getWidth(); }
        this.y2 = event.getY();

        if (this.isPanning() && !this.isLocked()) {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else if (this.isZooming()) {
            this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }

        //this.mousePosition.setText( "Length : [" + Math.abs(this.x2 - this.x1) + "]" ); // call repaint which calls paint repaint();

        this.isDragging = true;
        repaint();
    }

    // handle event when user moves mouse
    public void mouseMoved( final MouseEvent event ) {
        
    }

    public void keyTyped(KeyEvent e) {
        

    }

    public void keyPressed(KeyEvent e) {
        setKeyModifier(e);

        repaint();
    }

    public void keyReleased(KeyEvent e) {
        setKeyModifier(e);
        repaint();
    }

    /** Actions */
    private boolean isMovingPane() {
        return false;
        //return this.isDragging && ( ( isRightClick() && isNoKeyModifierPressed() ) || /** insert Mac specific combo here */ false );
    }

    private boolean isZooming() {
        return this.isDragging && isLeftClick() && isRightClick () || /** insert Mac specific combo here */ false;
    }

    private boolean isPanning() {
        return this.isDragging && isNoKeyModifierPressed();
        //return this.isDragging && isLeftClick() && isShiftKeyModifierPressed();
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

    /** Key modifiers */
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

    private void setMouseModifier(MouseEvent e) {
        
        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
            mouseMod = mouseModifier.LEFT;
        }
        else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
            mouseMod = mouseModifier.MIDDLE;
        }
        else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
            mouseMod = mouseModifier.RIGHT;
        } else {
            mouseMod = mouseModifier.NONE;
        }

        //Savant.log("Mouse modifier: " + mouseMod);
    }

    private void setKeyModifier(KeyEvent e) {

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
//    public void hideFrame() {
//        FrameController.getInstance().hideFrame(this);
//    }
//    public void closeFrame() {
//        FrameController.getInstance().closeFrame(this);
//    }
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

    private void switchMode(final ViewTrack track, final Mode mode) {
        track.setDrawMode(mode);
        try {
            // TODO: this needs to get done in a separate thread and then schedule the repaint for later
            track.prepareForRendering(RangeController.getInstance().getRange());
            repaint();
        } catch (Exception e) {
            log.error("Unexpected exception while preparing to render track " + e.getMessage());
        }

    }
}
