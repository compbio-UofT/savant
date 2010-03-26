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
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;
import savant.controller.event.rangeselection.RangeSelectionChangedEvent;
import savant.controller.event.rangeselection.RangeSelectionChangedListener;
import savant.util.MiscUtils;
import savant.util.Range;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class RangeSelectionPanel extends JPanel implements MouseListener, MouseMotionListener, RangeChangedListener {

    private int minimum = 0;
    private int maximum = 100;
    private static final long serialVersionUID = 1L;
    private final JLabel mousePosition;
    int x1, x2, y1, y2;
    int x, y, w, h;
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

        addMouseListener(this); // listens for own mouse and
        addMouseMotionListener(this); // mouse-motion events

        setVisible(true);
    }

// MouseListener event handlers // handle event when mouse released immediately after press
    public void mouseClicked(final MouseEvent event) {
        //this.mousePosition.setText( "Clicked at [" + event.getX() + ", " + event.getY() + "]" );

        //repaint();
    }

// handle event when mouse pressed
    public void mousePressed(final MouseEvent event) {

        this.x1 = event.getX();
        if (this.x1 < this.minimum) {
            this.x1 = this.minimum;
        }
        if (this.x1 > this.maximum) {
            this.x1 = this.maximum;
        }
        this.y1 = event.getY();

        //this.mousePosition.setText( "Pressed at [" + ( this.x1 ) + ", " + ( this.y1 ) + "]" );

        //this.recStart.setText( "Start:  [" + this.x1 + "]");

        this.isNewRect = true;
        this.rangeChangedExternally = false;

        //repaint();
    }

// handle event when mouse released after dragging
    public void mouseReleased(final MouseEvent event) {
        this.x2 = event.getX();
        if (this.x2 < this.minimum) {
            this.x2 = this.minimum;
        }
        if (this.x2 > this.maximum) {
            this.x2 = this.maximum;
        }
        this.y2 = event.getY();
        //this.mousePosition.setText( "Released at [" + ( this.x2 ) + ", " + ( this.y2 ) + "]" );

        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        //this.recStop.setText( "End:  [" + this.x2 + "]" );

        repaint();

        int st = this.x1; int end = this.x2;
        if (this.x1 > this.x2) {
            st = this.x2;
            end = this.x1;
        }
        fireRangeSelectionChangedEvent(new Range(translatePixelToPosition(st), translatePixelToPosition(end)));
    }

// handle event when mouse enters area
    public void mouseEntered(final MouseEvent event) {
        //this.mousePosition.setText( "Mouse entered at [" + event.getX() + ", " + event.getY() + "]" );
        //repaint();
    }

// handle event when mouse exits area
    public void mouseExited(final MouseEvent event) {
        //this.mousePosition.setText( "Mouse outside window" );
        //repaint();
    }

// MouseMotionListener event handlers // handle event when user drags mouse with button pressed
    public void mouseDragged(final MouseEvent event) {
        this.x2 = event.getX();
        if (this.x2 < this.minimum) {
            this.x2 = this.minimum;
        }
        if (this.x2 > this.maximum) {
            this.x2 = this.maximum;
        }
        this.y2 = event.getY();

        this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

        //this.mousePosition.setText( "Length : [" + Math.abs(this.x2 - this.x1) + "]" ); // call repaint which calls paint repaint();

        this.isNewRect = false;
        repaint();
    }

// handle event when user moves mouse
    public void mouseMoved(final MouseEvent event) {
        //this.mousePosition.setText( "At [" + event.getX() + "]" );
        repaint();
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        Savant.log("Painting component");

        int wid = getWidth( );
        int hei = getHeight( );

        Graphics2D g2d0 = (Graphics2D)g;

            // Paint a gradient from top to bottom
            GradientPaint gp0 = new GradientPaint(
                0, 0, new Color(240, 240,240),
                0, h, new Color(220,220,220) );

            g2d0.setPaint( gp0 );
            g2d0.fillRect( 0, 0, wid, hei );

        

        int width = this.x1 - this.x2;
        int height = this.getHeight() - 4;// this.y1 - this.y2;

        this.w = Math.max(2, Math.abs(width));
        this.h = Math.abs(height);
        this.x = width < 0 ? this.x1 : this.x2;
        this.y = 1; //height < 0 ? this.y1 : this.y2;

            g.setColor(new Color(100, 100, 100, 100));
            g.drawRect(this.x, this.y, this.w, this.h);

            Graphics2D g2d = (Graphics2D)g;

            // Paint a gradient from top to bottom
            GradientPaint gp = new GradientPaint(
                0, 0, new Color(95,161,241),
                0, h, new Color(75,144,228) );

            g2d.setPaint( gp );
            g2d.fillRect( this.x, this.y, this.w, this.h );

            g.setColor(Color.black);

            int fromX = this.x1 > this.x2 ? this.x2 : this.x1;
            int toX = this.x1 > this.x2 ? this.x1 : this.x2;
            String from,to;
            int startFrom, startTo;
            int ypos = this.getHeight()/2+4;

            if (this.rangeChangedExternally) {
                Range r = RangeController.getInstance().getRange();
                from = MiscUtils.intToString(r.getFrom());
                to = MiscUtils.intToString(r.getTo());
            } else {
                from = MiscUtils.intToString(translatePixelToPosition(fromX)+1);
                to = MiscUtils.intToString(translatePixelToPosition(toX)+1);
            }

            FontMetrics metrics = g.getFontMetrics(g.getFont());
            // get the advance of my text in this font and render context
            int fromWidth = metrics.stringWidth(from);
            int toWidth = metrics.stringWidth(to);

            startFrom = fromX - 5 - fromWidth;
            startTo = toX + 5;

            if (startFrom + fromWidth + 5 < startTo) {
                if (startFrom > 0) { g.drawString( from, startFrom, ypos ); }
                if (startTo + toWidth < this.getWidth()) { g.drawString( to, startTo, ypos ); }
            }
    }

    public void setMaximum(int max) {
        this.maximum = max - 1;
    }

    public void setRange(int lower, int upper) {
        setRange(new Range(lower, upper));
    }

    public void setRange(Range r) {
        this.x1 = translatePositionToPixel(r.getFrom());
        this.x2 = translatePositionToPixel(r.getTo());

        //this.isNewRect = false;
        repaint();
    }

    public int getLowerPosition() {
        return translatePixelToPosition(this.x1);
    }

    public int getUpperPosition() {
        return translatePixelToPosition(this.x2);
    }

    private int translatePixelToPosition(int pixel) {
        return (int) ((double) pixel * this.maximum / (double) this.getWidth());
    }

    private int translatePositionToPixel(int position) {
        return (int) (((double) position * this.getWidth()-4) / this.maximum) + 1;
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
} // end class MouseTracker

