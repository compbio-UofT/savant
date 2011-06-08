/*
 *    Copyright 2009-2011 University of Toronto
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.RangeChangedListener;
import savant.controller.event.RangeSelectionChangedEvent;
import savant.controller.event.RangeSelectionChangedListener;
import savant.data.types.Genome;
import savant.data.types.Genome.Cytoband;
import savant.util.Range;
import savant.util.MiscUtils;

/**
 *
 * @author mfiume
 */
public class RangeSelectionPanel extends JPanel implements MouseListener, MouseMotionListener, RangeChangedListener {

    private static final AlphaComposite COMPOSITE_50 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50F);
    private static final AlphaComposite COMPOSITE_75 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75F);
    private static final Color LINE_COLOUR = new Color(100, 100, 100);

    private boolean isMouseInside = false;
    private boolean isDragging = false;
    private boolean isActive = false;
    private int minimum = 0;
    private int maximum = 100;
    private final JLabel mousePosition;
    int x1, x2, y1, y2;
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
    @Override
    public void mouseClicked(final MouseEvent event) {
        //this.mousePos.setText( "Clicked at [" + event.getX() + ", " + event.getY() + "]" );
        //repaint();
    }

// handle event when mouse pressed
    @Override
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

        int startRange, endRange;
        
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
    @Override
    public void mouseEntered(final MouseEvent event) {
         this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
         this.isMouseInside = true;
        //this.mousePos.setText( "Mouse entered at [" + event.getX() + ", " + event.getY() + "]" );
        //repaint();
    }

// handle event when mouse exits area
    @Override
    public void mouseExited(final MouseEvent event) {
         this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
         this.isMouseInside = false;
         repaint();
        //this.mousePos.setText( "Mouse outside window" );
        //repaint();
    }

// MouseMotionListener event handlers // handle event when user drags mouse with button pressed
    @Override
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

// handle event when user moves mouse
    @Override
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

        ReferenceController refCon = ReferenceController.getInstance();
        Cytoband[] bands = null;
        Genome genome = refCon.getGenome();
        if (genome != null) {
            bands = genome.getCytobands(refCon.getReferenceName());
        }

        Image image_bar_unselected_glossy = null;
        Image image_bar_selected_glossy = null;
        Image image_left_cap = null;
        Image image_right_cap = null;

        try {
            image_bar_unselected_glossy = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/bar_unselected_glossy.PNG"));
            image_bar_selected_glossy = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/bar_selected_glossy.png"));
            image_left_cap = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/round_cap_left_bordered.png"));
            image_right_cap = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/round_cap_right_bordered.png"));
        } catch (IOException e) {}

        
        // draw background
        Graphics2D g2d = (Graphics2D) g;
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(COMPOSITE_75);
        g.drawImage(image_bar_unselected_glossy, 0,0,this.getWidth(),this.getHeight(),this);
        g2d.setComposite(originalComposite);
        
        int width = this.x1 - this.x2;
        int height = this.getHeight();// this.y1 - this.y2;

        int w = Math.max(2, Math.abs(width));
        int h = Math.abs(height);
        int x = width < 0 ? this.x1 : this.x2;
        int y = 0;

        RangeController rc = RangeController.getInstance();
        int startrange = rc.getRangeStart();
        int endrange = rc.getRangeEnd();

        // Lines on top and bottom
        g.setColor(LINE_COLOUR);
        g.drawLine(0, 0, wid, 0);
        g.drawLine(0, hei-1, wid, hei-1);

        if (bands != null) {
            int centromereStart = -1;
            g2d.setComposite(COMPOSITE_50);
            for (Cytoband b: bands) {
                int bandX = MiscUtils.transformPositionToPixel(b.start, getWidth(), rc.getMaxRange());
                int bandWidth = MiscUtils.transformPositionToPixel(b.end, getWidth(), rc.getMaxRange()) - bandX;

                g.setColor(b.getColor());
                g.fillRect(bandX, y + 1, bandWidth, h - 2);
                
                if (b.isCentromere()) {
                    if (centromereStart >= 0) {
                        int mid = y + h / 2;
                        g2d.setComposite(originalComposite);
                        Polygon bowtie = new Polygon(new int[] { centromereStart, bandX, centromereStart, bandX + bandWidth, bandX, bandX + bandWidth },
                                                     new int[] { y,               mid,   y + h,           y + h,             mid,   y }, 6);
                        g.setColor(getBackground());
                        g.fillPolygon(bowtie);
                        g.setColor(LINE_COLOUR);
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.drawLine(centromereStart - 1, y, bandX, mid);
                        g.drawLine(centromereStart - 1, y + h, bandX, mid);
                        g.drawLine(bandX, mid, bandX + bandWidth, y);
                        g.drawLine(bandX, mid, bandX + bandWidth, y + h);
                        g2d.setComposite(COMPOSITE_50);
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
                    } else {
                        centromereStart = bandX;
                    }
                }
            }
            g2d.setComposite(originalComposite);
        }

        if (isDragging) {
            // draw selected region
            g2d.setComposite(COMPOSITE_50);
            g.drawImage(image_bar_selected_glossy, x, y, w, h,this);
            g2d.setComposite(originalComposite);

            g.setColor(LINE_COLOUR);
            g.drawRect(x, y, w, h);
            
        } else {
            int startx = MiscUtils.transformPositionToPixel(startrange, this.getWidth(), rc.getMaxRange());
            int endx = MiscUtils.transformPositionToPixel(endrange, this.getWidth(), rc.getMaxRange());
            int widpixels = Math.max(5, endx-startx);

            //Graphics2D g2d = (Graphics2D) g;
            //Composite originalComposite = g2d.getComposite();
            g2d.setComposite(COMPOSITE_75);
            g.drawImage(image_bar_selected_glossy, startx, y,widpixels, h,this);
            g2d.setComposite(originalComposite);

            g.setColor(new Color(100, 100, 100));
            g.drawRect( startx, y, widpixels, h);
        }

        int capwidth = 20;
        g.drawImage(image_left_cap, 0,0,capwidth,23,this);
        g.drawImage(image_right_cap, this.getWidth()-capwidth,0,capwidth,23,this);

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

            int genomepos = translatePixelToPosition(x_notdragging);

            String mousePos = MiscUtils.posToShortString(genomepos);
            g.drawString(
                    mousePos,
                    x_notdragging-metrics.stringWidth(mousePos)-12,
                    this.getHeight() / 2 + 4);

            //g.drawString(msg, x_notdragging+12, this.getHeight() / 2 + 4);
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
        return (int)((double)pixel * maximum / (double)getWidth());
    }

    private int translatePositionToPixel(int position) {
        return (int)(((double) position * getWidth() - 4) / maximum) + 1;
    }

    @Override
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
}

