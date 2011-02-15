/*
 *    Copyright 2009-2010 University of Toronto
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

import savant.controller.RangeController;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.RangeChangedListener;
import savant.controller.event.RangeSelectionChangedEvent;
import savant.controller.event.RangeSelectionChangedListener;
import savant.util.MiscUtils;
import savant.util.Range;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImageOp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class RangeSelectionPanel extends JPanel implements MouseListener, MouseMotionListener, RangeChangedListener {

    private boolean isMouseInside = false;
    private boolean isDragging = false;
    private boolean isActive = false;
    private long minimum = 0;
    private long maximum = 100;
    private static final long serialVersionUID = 1L;
    private final JLabel mousePosition;
    int x1, x2, y1, y2;
    int x, y, w, h;
    int x_notdragging;
    private final JLabel recStart;
    private final JLabel recStop;
    private final JLabel cords; // set up GUI and register mouse event handlers
    boolean isNewRect = true;
    /** Range Selection Changed Listeners */
    private List rangeSelectionChangedListeners = new ArrayList();
    private boolean rangeChangedExternally;

    public RangeSelectionPanel() {

        this.mousePosition = new JLabel();
        this.mousePosition.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(this.mousePosition, BorderLayout.CENTER);

        this.recStart = new JLabel();
        this.add(this.recStart, BorderLayout.WEST);

        this.recStop = new JLabel();
        this.add(this.recStop, BorderLayout.EAST);

        this.cords = new JLabel();
        this.add(this.cords, BorderLayout.NORTH);

        this.setToolTipText("Click and drag to select a subregion of the reference");

        addMouseListener(this); // listens for own mouse and
        addMouseMotionListener(this); // mouse-motion events

        setVisible(true);
    }

// MouseListener event handlers // handle event when mouse released immediately after press
    public void mouseClicked(final MouseEvent event) {
        //this.mousePos.setText( "Clicked at [" + event.getX() + ", " + event.getY() + "]" );
        //repaint();
    }

// handle event when mouse pressed
    public void mousePressed(final MouseEvent event) {

        this.x1 = event.getX();
        if (this.x1 < 1) {
            this.x1 = 1;
        }
        if (this.x1 > this.getWidth()) {
            this.x1 = this.getWidth();
        }
        this.y1 = event.getY();

        //this.mousePos.setText( "Pressed at [" + ( this.x1 ) + ", " + ( this.y1 ) + "]" );

        //this.recStart.setText( "Start:  [" + this.x1 + "]");

        this.isNewRect = true;
        this.rangeChangedExternally = false;

        //repaint();
    }

// handle event when mouse released after dragging
    @Override
    public void mouseReleased(final MouseEvent event) {

        isDragging = false;

        this.x2 = event.getX();
        if (this.x2 < 1) {
            this.x2 = 1;
        }
        if (this.x2 > this.getWidth()) {
            this.x2 = this.getWidth();
        }
        this.y2 = event.getY();
        //this.mousePos.setText( "Released at [" + ( this.x2 ) + ", " + ( this.y2 ) + "]" );

        //this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        //this.recStop.setText( "End:  [" + this.x2 + "]" );

        repaint();

        int st = this.x1;
        int end = this.x2;
        if (this.x1 > this.x2) {
            st = this.x2;
            end = this.x1;
        }

        long startRange, endRange;
        
        if (st < 2) {
            startRange = RangeController.getInstance().getMaxRangeStart();
        } else {
             startRange = translatePixelToPosition(st);
        }

        if (end > this.getWidth()-2) {
            endRange = RangeController.getInstance().getMaxRangeEnd();
        } else {
            endRange = translatePixelToPosition(end);
        }
        
        fireRangeSelectionChangedEvent(new Range(startRange, endRange));
    }

// handle event when mouse enters area
    public void mouseEntered(final MouseEvent event) {
         this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
         this.isMouseInside = true;
        //this.mousePos.setText( "Mouse entered at [" + event.getX() + ", " + event.getY() + "]" );
        //repaint();
    }

// handle event when mouse exits area
    public void mouseExited(final MouseEvent event) {
         this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
         this.isMouseInside = false;
         repaint();
        //this.mousePos.setText( "Mouse outside window" );
        //repaint();
    }

// MouseMotionListener event handlers // handle event when user drags mouse with button pressed
    public void mouseDragged(final MouseEvent event) {

        if (!this.isActive()) {
            return ;
        }

        isDragging = true;

        this.x2 = event.getX();
        if (this.x2 < 1) {
            this.x2 = 1;
        }
        if (this.x2 > this.getWidth()) {
            this.x2 = this.getWidth();
        }
        this.y2 = event.getY();

        //this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

        //this.mousePos.setText( "Length : [" + Math.abs(this.x2 - this.x1) + "]" ); // call repaint which calls paint repaint();

        this.isNewRect = false;
        repaint();
    }

 private AlphaComposite makeComposite(float alpha) {
    int type = AlphaComposite.SRC_OVER;
    return(AlphaComposite.getInstance(type, alpha));
 }

// handle event when user moves mouse
    public void mouseMoved(final MouseEvent event) {
        //this.mousePos.setText( "At [" + event.getX() + "]" );

        this.x_notdragging = event.getX();
        repaint();
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (!this.isActive()) { return ; }

        //Savant.log("Painting component");

        int wid = getWidth();
        int hei = getHeight();

        Image image_bar_unselected_glossy = null;
        Image image_bar_selected_glossy = null;
        Image image_left_cap = null;
        Image image_right_cap = null;
        Image image_dnabg = null;

        try {
            image_bar_unselected_glossy = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/bar_unselected_glossy.PNG"));
            image_bar_selected_glossy = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/bar_selected_glossy.png"));
            image_left_cap = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/round_cap_left_bordered.png"));
            image_right_cap = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/round_cap_right_bordered.png"));
            image_dnabg = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/dnabg.png"));
        } catch (IOException e) {}

        
        int dnawidth = 40;//image_dnabg.getWidth(this);
        int repeatdnatimes = this.getWidth()/dnawidth + 1;
        for (int i = 0; i < repeatdnatimes; i++) {
            g.drawImage(image_dnabg, i*dnawidth,0,dnawidth,this.getHeight(),this);
        }

        // draw background
        Graphics2D g2d = (Graphics2D) g;
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(makeComposite(0.75F));
        g.drawImage(image_bar_unselected_glossy, 0,0,this.getWidth(),this.getHeight(),this);
        g2d.setComposite(originalComposite);
        
        int width = this.x1 - this.x2;
        int height = this.getHeight();// this.y1 - this.y2;

        this.w = Math.max(2, Math.abs(width));
        this.h = Math.abs(height);
        this.x = width < 0 ? this.x1 : this.x2;
        this.y = 0; //height < 0 ? this.y1 : this.y2;

        // draw lines on top and bottom
        g.setColor(new Color(100, 100, 100));
        g.drawLine(0, 0, wid, 0);
        g.drawLine(0, hei-1, wid, hei-1);

        if (this.isDragging) {

            // draw selected region
            g2d.setComposite(makeComposite(0.5F));
            g.drawImage(image_bar_selected_glossy, this.x,this.y,this.w,this.h,this);
            g2d.setComposite(originalComposite);

            g.setColor(new Color(100, 100, 100));
            g.drawRect(
                    this.x,
                    this.y,
                    this.w,
                    this.h);
            
        } else {
            RangeController rc = RangeController.getInstance();
            long startrange = rc.getRangeStart();
            long endrange = rc.getRangeEnd();
            int startx = MiscUtils.transformPositionToPixel(startrange, this.getWidth(), rc.getMaxRange());
            int endx = MiscUtils.transformPositionToPixel(endrange, this.getWidth(), rc.getMaxRange());
            int widpixels = Math.max(5, endx-startx);

            //Graphics2D g2d = (Graphics2D) g;
            //Composite originalComposite = g2d.getComposite();
            g2d.setComposite(makeComposite(0.5F));
            g.drawImage(image_bar_selected_glossy, startx,this.y,widpixels,this.h,this);
            g2d.setComposite(originalComposite);

            g.setColor(new Color(100, 100, 100));
            g.drawRect(
                    startx,
                    this.y,
                    widpixels,
                    this.h);
        }

        int capwidth = 20;
        g.drawImage(image_left_cap, 0,0,capwidth,23,this);
        g.drawImage(image_right_cap, this.getWidth()-capwidth,0,capwidth,23,this);

        int numlines = 4;
        double space = ((double) this.getWidth()) / numlines;

        g.setColor(new Color(150,150,150,100));
        for (int i = 1; i <= numlines; i++) {
            g.drawLine((int) Math.round(i * space), 0, (int) Math.round(i * space), this.getHeight());
        }

        if (isDragging) {
            g.setColor(Color.black);

            int fromX = this.x1 > this.x2 ? this.x2 : this.x1;
            int toX = this.x1 > this.x2 ? this.x1 : this.x2;
            String from, to;
            int startFrom, startTo;
            int ypos = this.getHeight() / 2 + 4;

            if (this.rangeChangedExternally) {
                Range r = RangeController.getInstance().getRange();
                from = MiscUtils.posToShortString(r.getFrom());
                to = MiscUtils.posToShortString(r.getTo());
            } else {
                from = MiscUtils.posToShortString(MiscUtils.transformPixelToPosition(fromX, this.getWidth(), RangeController.getInstance().getMaxRange()));
                to = MiscUtils.posToShortString(MiscUtils.transformPixelToPosition(toX, this.getWidth(), RangeController.getInstance().getMaxRange()));
            }

            FontMetrics metrics = g.getFontMetrics(g.getFont());
            // get the advance of my text in this font and render context
            int fromWidth = metrics.stringWidth(from);
            int toWidth = metrics.stringWidth(to);

            startFrom = fromX - 7 - fromWidth;
            startTo = toX + 7;

            if (startFrom + fromWidth + 12 < startTo) {
                if (startFrom > 0) {
                    g.drawString(from, startFrom, ypos);
                }
                if (startTo + toWidth < this.getWidth()) {
                    g.drawString(to, startTo, ypos);
                }
            }
        } else if (this.isMouseInside) {
            g.setColor(Color.black);
            String msg = "click+drag to change range";

            FontMetrics metrics = g.getFontMetrics(g.getFont());

            long genomepos = this.translatePixelToPosition(this.x_notdragging);

            String mousePos = MiscUtils.posToShortString(genomepos);
            g.drawString(
                    mousePos,
                    x_notdragging-metrics.stringWidth(mousePos)-12,
                    this.getHeight() / 2 + 4);

            //g.drawString(msg, x_notdragging+12, this.getHeight() / 2 + 4);
        }
    }

    public void setMaximum(long max) {
        this.maximum = max - 1;
    }

    public void setRange(long lower, long upper) {
        setRange(new Range(lower, upper));
    }

    public void setRange(Range r) {
        this.x1 = translatePositionToPixel(r.getFrom());
        this.x2 = translatePositionToPixel(r.getTo());

        //this.isNewRect = false;
        repaint();
    }

    public long getLowerPosition() {
        return translatePixelToPosition(this.x1);
    }

    public long getUpperPosition() {
        return translatePixelToPosition(this.x2);
    }

    private long translatePixelToPosition(int pixel) {
        return (long) ((double) pixel * this.maximum / (double) this.getWidth());
    }

    private int translatePositionToPixel(long position) {
        return (int) (((double) position * this.getWidth() - 4) / this.maximum) + 1;
    }

    public void rangeChangeReceived(RangeChangedEvent event) {
        this.rangeChangedExternally = true;
        this.setRange(event.range());
    }

    public synchronized void addRangeChangedListener(RangeSelectionChangedListener l) {
        rangeSelectionChangedListeners.add(l);
    }

    public synchronized void removeRangeChangedListener(RangeSelectionChangedListener l) {
        rangeSelectionChangedListeners.remove(l);
    }

    /**
     * Fire the RangeSelectionChangedEvent
     */
    private synchronized void fireRangeSelectionChangedEvent(Range r) {
        RangeSelectionChangedEvent evt = new RangeSelectionChangedEvent(this, r);
        Iterator listeners = this.rangeSelectionChangedListeners.iterator();
        while (listeners.hasNext()) {
            ((RangeSelectionChangedListener) listeners.next()).rangeSelectionChangeReceived(evt);
        }
    }

    void setActive(boolean b) {
        this.isActive = b;
    }

    private boolean isActive() {
        return this.isActive;
    }

} // end class MouseTracker

