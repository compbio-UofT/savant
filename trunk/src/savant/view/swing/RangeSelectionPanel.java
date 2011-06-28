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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import savant.controller.LocationController;
import savant.controller.event.GenomeChangedEvent;
import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangedListener;

import savant.data.types.Genome;
import savant.data.types.Genome.Cytoband;
import savant.util.Range;
import savant.util.MiscUtils;

/**
 * Component which lets user specify the current range.
 *
 * @author mfiume, tarkvara
 */
public class RangeSelectionPanel extends JPanel implements LocationChangedListener {

    private static final AlphaComposite COMPOSITE_50 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50F);
    private static final AlphaComposite COMPOSITE_75 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75F);
    static final Color LINE_COLOUR = new Color(100, 100, 100);

    private boolean isMouseInside = false;
    private boolean isDragging = false;
    private int maximum = 100;
    private final JLabel mousePosition;
    int x1, x2;
    int x_notdragging;
    private final JLabel recStart;
    private final JLabel recStop;
    private final JLabel cords; // set up GUI and register mouse event handlers
    boolean isNewRect = true;
    private boolean rangeChangedExternally;
    private LocationController locationController = LocationController.getInstance();


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

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                x1 = event.getX();
                if (x1 < 1) {
                    x1 = 1;
                }
                if (x1 > getWidth()) {
                    x1 = getWidth();
                }
                isNewRect = true;
                rangeChangedExternally = false;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                boolean wasDragging = isDragging;
                isDragging = false;
                x2 = event.getX();
                if (x2 < 1) {
                    x2 = 1;
                } else if (x2 > getWidth()) {
                    x2 = getWidth();
                }
                repaint();

                int st = x1;
                int end = x2;
                if (x1 > x2) {
                    st = x2;
                    end = x1;
                }

                int startRange = Math.max(translatePixelToPosition(st), locationController.getMaxRangeStart());
                int endRange = wasDragging ? translatePixelToPosition(end) : startRange + locationController.getRange().getLength() - 1;
                endRange = Math.min(endRange, locationController.getMaxRangeEnd());
                locationController.setLocation(startRange, endRange);
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                 setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                 isMouseInside = true;
            }

            @Override
            public void mouseExited(MouseEvent event) {
                 setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                 isMouseInside = false;
                 repaint();
            }
        });
        addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent event) {
                isDragging = true;

                x2 = event.getX();
                if (x2 < 1) {
                    x2 = 1;
                } else if (x2 > getWidth()) {
                    x2 = getWidth();
                }

                isNewRect = false;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                x_notdragging = event.getX();
                repaint();
            }

        });

        setVisible(true);
    }


    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        int wid = getWidth();
        int hei = getHeight();

        Cytoband[] bands = null;
        Genome genome = locationController.getGenome();
        if (genome != null) {
            bands = genome.getCytobands(locationController.getReferenceName());
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

        int startrange = locationController.getRangeStart();
        int endrange = locationController.getRangeEnd();

        // Lines on top and bottom
        g.setColor(LINE_COLOUR);
        g.drawLine(0, 0, wid, 0);
        g.drawLine(0, hei - 1, wid, hei - 1);

        if (bands != null) {
            int centromereStart = -1;
            g2d.setComposite(COMPOSITE_50);
            for (Cytoband b: bands) {
                int bandX = MiscUtils.transformPositionToPixel(b.start, getWidth(), locationController.getMaxRange());
                int bandWidth = MiscUtils.transformPositionToPixel(b.end, getWidth(), locationController.getMaxRange()) - bandX;

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
            int startx = MiscUtils.transformPositionToPixel(startrange, this.getWidth(), locationController.getMaxRange());
            int endx = MiscUtils.transformPositionToPixel(endrange, this.getWidth(), locationController.getMaxRange());
            int widpixels = Math.max(5, endx-startx);

            //Graphics2D g2d = (Graphics2D) g;
            //Composite originalComposite = g2d.getComposite();
            g2d.setComposite(COMPOSITE_75);
            g.drawImage(image_bar_selected_glossy, startx, y,widpixels, h,this);
            g2d.setComposite(originalComposite);

            g.setColor(new Color(100, 100, 100));
            g.drawRect( startx, y, widpixels, h);
        }

        g.drawImage(image_left_cap, 0, 0, Ruler.CAP_WIDTH, Ruler.CAP_HEIGHT, this);
        g.drawImage(image_right_cap, getWidth() - Ruler.CAP_WIDTH, 0, Ruler.CAP_WIDTH, Ruler.CAP_HEIGHT,this);
        g.setColor(LINE_COLOUR);
        g.drawLine(0, hei - 1, Ruler.CAP_WIDTH + 1, hei - 1);

        if (isDragging) {
            g.setColor(Color.black);

            int fromX = this.x1 > this.x2 ? this.x2 : this.x1;
            int toX = this.x1 > this.x2 ? this.x1 : this.x2;
            String from, to;
            int startFrom, startTo;
            int ypos = this.getHeight() / 2 + 4;

            if (this.rangeChangedExternally) {
                Range r = locationController.getRange();
                from = MiscUtils.posToShortString(r.getFrom());
                to = MiscUtils.posToShortString(r.getTo());
            } else {
                from = MiscUtils.posToShortString(MiscUtils.transformPixelToPosition(fromX, this.getWidth(), locationController.getMaxRange()));
                to = MiscUtils.posToShortString(MiscUtils.transformPixelToPosition(toX, this.getWidth(), locationController.getMaxRange()));
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

        getParent().repaint();
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
    public void genomeChanged(GenomeChangedEvent event) {}

    @Override
    public void locationChanged(LocationChangedEvent event) {
        this.rangeChangedExternally = true;
        this.setRange(event.getRange());
    }
}

