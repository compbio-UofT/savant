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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import savant.controller.RangeController;
import savant.controller.event.GraphPaneChangeEvent;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import savant.controller.GraphPaneController;
import savant.controller.event.GraphPaneChangeListener;

/**
 *
 * @author mfiume
 */
public class MiniRangeSelectionPanel extends JPanel implements MouseListener, MouseMotionListener, RangeChangedListener, GraphPaneChangeListener {

    private boolean isDragging = false;
    private boolean isActive = false;
    private long minimum = 0;
    private long maximum = 100;
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

    public static MiniRangeSelectionPanel instance;

    public MiniRangeSelectionPanel() {

        this.mousePosition = new JLabel();
        this.mousePosition.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(this.mousePosition, BorderLayout.CENTER);

        this.recStart = new JLabel();
        this.add(this.recStart, BorderLayout.WEST);

        this.recStop = new JLabel();
        this.add(this.recStop, BorderLayout.EAST);

        this.cords = new JLabel();
        this.add(this.cords, BorderLayout.NORTH);

        this.setPreferredSize(new Dimension(10000, 23));
        this.setMaximumSize(new Dimension(10000, 23));

        addMouseListener(this); // listens for own mouse and
        addMouseMotionListener(this); // mouse-motion events
        GraphPaneController.getInstance().addGraphPaneChangedListener(this);

        //Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        //this.setBorder(loweredetched);

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
            this.x1 = (int)this.minimum;
        }
        if (this.x1 > this.maximum) {
            this.x1 = (int)this.maximum;
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

        this.isDragging = false;

        this.x2 = event.getX();
        if (this.x2 < this.minimum) {
            this.x2 = (int)this.minimum;
        }
        if (this.x2 > this.maximum) {
            this.x2 = (int)this.maximum;
        }
        this.y2 = event.getY();

        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        repaint();
    }

// handle event when mouse enters area
    public void mouseEntered(final MouseEvent event) {
    }

// handle event when mouse exits area
    public void mouseExited(final MouseEvent event) {
    }

// MouseMotionListener event handlers // handle event when user drags mouse with button pressed
    public void mouseDragged(final MouseEvent event) {

        if (!this.isActive()) { return ; }

        this.isDragging = true;

        this.x2 = event.getX();
        // TODO: This looks fishy to me:  x is a pixel value, while minimum/maximum are genome positions.
        if (this.x2 < this.minimum) {
            this.x2 = (int)this.minimum;
        }
        if (this.x2 > this.maximum) {
            this.x2 = (int)this.maximum;
        }
        this.y2 = event.getY();

        //this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

        //this.mousePosition.setText( "Length : [" + Math.abs(this.x2 - this.x1) + "]" ); // call repaint which calls paint repaint();

        this.isNewRect = false;
        repaint();
    }

// handle event when user moves mouse
    public void mouseMoved(final MouseEvent event) {
        //this.mousePosition.setText( "At [" + event.getX() + "]" );
        //repaint();
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (!this.isActive()) { return ; }

        GraphPaneController gpc = GraphPaneController.getInstance();
        if (gpc.isPanning()) {
            long fromx = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getFrom(), this.getWidth(), RangeController.getInstance().getRange());
            long tox = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getTo(), this.getWidth(), RangeController.getInstance().getRange());

            double shiftamount = tox-fromx;
            g.translate((int) shiftamount, 0);
            /*
            // shifting left
            if (shiftamount < 0) {
                shiftamount = -shiftamount;
                long maxlefttranslation = (RangeController.getInstance().getRangeStart()-1)*MiscUtils.transformPositionToPixel(RangeController.getInstance().getRangeStart()+1, this.getWidth(), RangeController.getInstance().getRange());
                shiftamount = Math.min(maxlefttranslation,shiftamount);
                g.translate((int) shiftamount, 0);
                //System.out.println("LEFT: " + " m=" + maxlefttranslation + " s=" + shiftamount);
            // shifting right
            } else {
                long maxrighttranslation = (RangeController.getInstance().getMaxRangeEnd() - RangeController.getInstance().getRangeEnd())*MiscUtils.transformPositionToPixel(RangeController.getInstance().getRangeStart()+1, this.getWidth(), RangeController.getInstance().getRange());
                shiftamount = Math.min(maxrighttranslation,shiftamount);
                g.translate((int) -shiftamount, 0);
                //System.out.println("RIGHT: " + " m=" + maxrighttranslation + " s=" + shiftamount);
            }
             *
             */
        }

        renderBackground(g);
    }

    /**
     * Render the background of this graphpane
     * @param g The graphics object to use
     */
    private void renderBackground(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        Font smallFont = new Font("Sans-Serif", Font.PLAIN, 10);

        try {

            Image image = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/bar_selected_glossy.png"));
            Composite originalComposite = ((Graphics2D) g).getComposite();
            ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85F));
            g.drawImage(image, 0-this.getWidth(),0,this.getWidth()*3,this.getHeight(),this);
            ((Graphics2D) g).setComposite(originalComposite);
        } catch (Exception e) {
            System.err.println("Error drawing image background");
        }

        int numseparators = (int) Math.max(Math.ceil(Math.log(this.maximum-this.minimum)),2);
        long genomicSeparation = (this.maximum-this.minimum)/Math.max(1, numseparators);

        if (numseparators != 0) {
            int width = this.getWidth();
            double barseparation = width / numseparators;

            int minstringseparation = 200;
            int skipstring = (int) Math.round(minstringseparation / barseparation);

            int startbarsfrom = MiscUtils.transformPositionToPixel(
                    (long) (Math.floor((RangeController.getInstance().getRange().getFrom()/Math.max(1, genomicSeparation)))*genomicSeparation),
                    width, (RangeController.getInstance()).getRange());

            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i <= numseparators*3; i++) {
                g2.setColor(new Color(50,50,50,50)); //BrowserDefaults.colorAxisGrid);
                int xOne = startbarsfrom + (int)Math.ceil(i*barseparation)+1 - this.getWidth();
                int xTwo = xOne;
                int yOne = this.getHeight();
                int yTwo = 0;
                g2.drawLine(xOne,yOne,xTwo,yTwo);

                if (skipstring != 0) {
                    if (i % skipstring == 0) {
                        g2.setColor(Color.black);
                        long a = MiscUtils.transformPixelToPosition(xOne, width, (RangeController.getInstance()).getRange());
                        if (a >= 1 && a <= (RangeController.getInstance()).getMaxRangeEnd()) {
                            String numstr = MiscUtils.posToShortString(genomicSeparation,a);
                            g2.setColor(Color.black);
                             g2.drawString(numstr, (float) (xOne + 3), (float) ((this.getHeight()*0.5)+3));
                        }
                    }
                } else {
                    long a = MiscUtils.transformPixelToPosition(xOne, width, (RangeController.getInstance()).getRange());
                    String numstr = MiscUtils.numToString(a);
                    g2.setColor(Color.black);
                    g2.drawString(numstr, (float) (xOne + 3), (float) ((this.getHeight()*0.5)+3));
                }
            }

            if (RangeController.getInstance().getRange().getLength() >= RangeController.getInstance().getRangeStart()) {
                try {
                    Image image_left_cap = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/round_cap_left_bordered.png"));
                    int capwidth = 20;
                    int pos = MiscUtils.transformPositionToPixel(1, this.getWidth(), RangeController.getInstance().getRange());
                    g.drawImage(image_left_cap, pos,0,capwidth,23,this);
                    g.setColor(Savant.getInstance().getBackground());
                    g.fillRect(pos,0, -this.getWidth(), this.getHeight());
                } catch (IOException ex) {
                    Logger.getLogger(MiniRangeSelectionPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (RangeController.getInstance().getRange().getLength() >= RangeController.getInstance().getMaxRangeEnd() - RangeController.getInstance().getRangeEnd()) {
                try {
                    Image image_right_cap = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/round_cap_right_bordered.png"));
                    int capwidth = 20;
                    int pos = MiscUtils.transformPositionToPixel(RangeController.getInstance().getMaxRangeEnd(), this.getWidth(), RangeController.getInstance().getRange());
                    g.drawImage(image_right_cap, pos-capwidth,0,capwidth,23,this);
                    g.setColor(Savant.getInstance().getBackground());
                    g.fillRect(pos,0, this.getWidth(), this.getHeight());
                    
                } catch (IOException ex) {
                    Logger.getLogger(MiniRangeSelectionPanel.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
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

    public void setRulerRange(Range r) {
        this.minimum = r.getFrom();
        this.maximum = r.getTo();

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
        return this.minimum + (long) (((double) pixel * (this.maximum-this.minimum)) / (double) this.getWidth());
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

    @Override
    public void graphpaneChangeReceived(GraphPaneChangeEvent event) {
        GraphPaneController gpc = GraphPaneController.getInstance();
        if (gpc.isPanning()) {
            this.repaint();
        }
    }
} // end class MouseTracker

