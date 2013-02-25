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
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import savant.api.event.LocationChangedEvent;
import savant.api.util.Listener;
import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.data.types.Genome;
import savant.data.types.Genome.Cytoband;
import savant.util.Range;
import savant.util.MiscUtils;

/**
 * Component which lets user specify the current range.
 *
 * @author mfiume, tarkvara
 */
public class RangeSelectionPanel extends JPanel implements Listener<LocationChangedEvent> {

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
    private final JLabel coords;
    boolean isNewRect = true;
    private boolean rangeChangedExternally;
    private LocationController locationController = LocationController.getInstance();


    public RangeSelectionPanel() {

        mousePosition = new JLabel();
        mousePosition.setHorizontalAlignment(SwingConstants.CENTER);
        add(mousePosition, BorderLayout.CENTER);

        recStart = new JLabel();
        add(recStart, BorderLayout.WEST);

        recStop = new JLabel();
        add(recStop, BorderLayout.EAST);

        coords = new JLabel();
        add(coords, BorderLayout.NORTH);

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

        try {
            Cytoband[] bands = null;
            Genome genome = GenomeController.getInstance().getGenome();
            if (genome != null) {
                bands = genome.getCytobands(locationController.getReferenceName());
            }

            Image barUnselectedGlossyImage = ImageIO.read(getClass().getResource("/savant/images/bar_unselected_glossy.PNG"));
            Image leftCapImage = ImageIO.read(getClass().getResource("/savant/images/round_cap_left_bordered.png"));
            Image rightCapImage = ImageIO.read(getClass().getResource("/savant/images/round_cap_right_bordered.png"));

            int width = getWidth();
            int height = getHeight();

            // draw background
            Graphics2D g2d = (Graphics2D) g;
            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(COMPOSITE_75);
            g.drawImage(barUnselectedGlossyImage, 0, 0, width, height, this);
            g2d.setComposite(originalComposite);

            // Lines on top.
            g.setColor(LINE_COLOUR);
            g.drawLine(0, 0, width, 0);

            // At early points in the GUI initialisation, the range has not yet been set.
            if (locationController.getRange() == null) {
                g.drawLine(0, height - 1, width, height - 1);
                return;
            }

            String bandPos = "";

            if (bands != null) {
                int centromereStart = -1;
                g2d.setComposite(COMPOSITE_50);
                for (Cytoband b: bands) {
                    int bandX = MiscUtils.transformPositionToPixel(b.start, width, locationController.getMaxRange());
                    int bandWidth = MiscUtils.transformPositionToPixel(b.end, getWidth(), locationController.getMaxRange()) - bandX;
                    if (isMouseInside && x_notdragging >= bandX && x_notdragging <= bandX + bandWidth) {
                        bandPos = " (" + b.name + ")";
                    }
                    g.setColor(b.getColor());
                    g.fillRect(bandX, 1, bandWidth, height - 2);

                    if (b.isCentromere()) {
                        if (centromereStart >= 0) {
                            int mid = height / 2;
                            g2d.setComposite(originalComposite);
                            Polygon bowtie = new Polygon(new int[] { centromereStart, bandX, centromereStart, bandX + bandWidth, bandX, bandX + bandWidth },
                                                         new int[] { 0,               mid,   height,           height,             mid,   0 }, 6);
                            g.setColor(getBackground());
                            g.fillPolygon(bowtie);
                            g.setColor(LINE_COLOUR);
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g.drawLine(centromereStart - 1, 0, bandX, mid);
                            g.drawLine(centromereStart - 1, height, bandX, mid);
                            g.drawLine(bandX, mid, bandX + bandWidth, 0);
                            g.drawLine(bandX, mid, bandX + bandWidth, height);
                            g2d.setComposite(COMPOSITE_50);
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
                        } else {
                            centromereStart = bandX;
                        }
                    }
                }
                g2d.setComposite(originalComposite);
            }

            int selStart, selEnd;
            int startRange = locationController.getRangeStart();
            int endRange = locationController.getRangeEnd();
            if (isDragging) {
                selStart = x1 < x2 ? x1 : x2;
                selEnd = selStart + Math.max(2, Math.abs(x1 - x2));
            } else {
                selStart = MiscUtils.transformPositionToPixel(startRange, width, locationController.getMaxRange());
                selEnd = MiscUtils.transformPositionToPixel(endRange, width, locationController.getMaxRange());
                if (selEnd < selStart + 5) {
                    selEnd = selStart + 5;  // Make the selection big enough to be visible.
                }
            }

            // The shade corresponds to the shade of blue at the top of bar_selected_glossy.png.
            g.setColor(new Color(167, 201, 236));
            g2d.setComposite(COMPOSITE_75);
            g.fillRect(selStart, 0, selEnd - selStart, height);
            g2d.setComposite(originalComposite);

            // Draw the bottom dividing line along the whole genome, leaving a hole for the selected area.
            g.setColor(LINE_COLOUR);
            g.drawLine(0, height - 1, selStart, height - 1);
            g.drawLine(selStart, height - 1, selStart, 0);
            g.drawLine(selStart, 0, selEnd, 0);
            g.drawLine(selEnd, 0, selEnd, height - 1);
            g.drawLine(selEnd, height - 1, width, height - 1);

            g.drawImage(leftCapImage, 0, 0, Ruler.CAP_WIDTH, Ruler.CAP_HEIGHT, this);
            g.drawImage(rightCapImage, width - Ruler.CAP_WIDTH, 0, Ruler.CAP_WIDTH, Ruler.CAP_HEIGHT,this);

            if (isDragging) {
                g.setColor(Color.black);

                int fromX = x1 > x2 ? x2 : x1;
                int toX = x1 > x2 ? x1 : x2;
                String from, to;
                int startFrom, startTo;
                int ypos = this.getHeight() / 2 + 4;

                if (rangeChangedExternally) {
                    from = MiscUtils.posToShortString(startRange);
                    to = MiscUtils.posToShortString(endRange);
                } else {
                    from = MiscUtils.posToShortString(MiscUtils.transformPixelToPosition(fromX, width, locationController.getMaxRange()));
                    to = MiscUtils.posToShortString(MiscUtils.transformPixelToPosition(toX, width, locationController.getMaxRange()));
                }

                FontMetrics metrics = g.getFontMetrics(g.getFont());

                // Get the advance of my text in this font and render context
                int fromWidth = metrics.stringWidth(from);
                int toWidth = metrics.stringWidth(to);

                startFrom = fromX - 7 - fromWidth;
                startTo = toX + 7;

                if (startFrom + fromWidth + 12 < startTo) {
                    if (startFrom > 0) {
                        g.drawString(from, startFrom, ypos);
                    }
                    if (startTo + toWidth < width) {
                        g.drawString(to, startTo, ypos);
                    }
                }
            } else if (isMouseInside) {
                g.setColor(Color.black);

                FontMetrics metrics = g.getFontMetrics(g.getFont());

                int genomepos = translatePixelToPosition(x_notdragging);

                String mousePos = MiscUtils.posToShortString(genomepos) + bandPos;

                g.drawString(mousePos, x_notdragging - metrics.stringWidth(mousePos) - 12, height / 2 + 4);
            }
        } catch (IOException ignored) {
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
    public void handleEvent(LocationChangedEvent event) {
        if (event.isNewReference()) {
            Genome loadedGenome = GenomeController.getInstance().getGenome();
            setMaximum(loadedGenome.getLength());
        }
        rangeChangedExternally = true;
        setRange((Range)event.getRange());
    }
}

