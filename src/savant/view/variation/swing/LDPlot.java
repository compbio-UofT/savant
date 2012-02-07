/*
 *    Copyright 2012 University of Toronto
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
package savant.view.variation.swing;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.settings.ColourSettings;
import savant.util.ColourAccumulator;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.MiscUtils;
import savant.view.variation.LDRecord;
import savant.view.variation.VariationController;


/**
 * A Linkage Disequilibrium plot.
 *
 * @author tarkvara
 */
public class LDPlot extends VariationPlot {
    private static final double AXIS_WIDTH = 90.0;
    private static final double LEGEND_MARGIN = 15.0;
    private static final double LEGEND_HEIGHT = 180.0;
    private static final double LEGEND_WIDTH = 15.0;
    private static final float TICK_LENGTH = 3.0f;

    private static final Color[] HEATMAP_COLORS = { ColourSettings.getColor(ColourKey.HEATMAP_LOW), ColourSettings.getColor(ColourKey.HEATMAP_MEDIUM), ColourSettings.getColor(ColourKey.HEATMAP_HIGH) };

    private float[][] dPrimes;
    private float[][] rSquareds;
    private double x0, y0;
    private Area zones[];

    LDPlot(VariationController vc) {
        super(vc);
        
        VariantPopper popper = new VariantPopper(this);
        addMouseListener(popper);
        addMouseMotionListener(popper);
    }

    public void calculateLD() {
        if (dPrimes == null) {
            List<VariantRecord> data = controller.getData();
            if (data != null && data.size() > 0) {
                int participantCount = controller.getParticipantCount();
                dPrimes = new float[data.size()][data.size()];
                rSquareds = new float[data.size()][data.size()];
                for (int i = 0; i < data.size(); i++) {
                    VariantRecord recI = (VariantRecord)data.get(i);
                    VariantType varI = recI.getVariantType();
                    int n1 = 0;
                    for (int k = 0; k < participantCount; k++) {
                        if (recI.getVariantsForParticipant(k)[0] == varI) {
                            n1++;
                        }
                    }
                    double p1 = n1 / (double)participantCount;
                    double p2 = 1.0 - p1;

                    for (int j = i + 1; j < data.size(); j++) {
                        if (p1 > 0.0 && p1 < 1.0) {
                            VariantRecord recJ = (VariantRecord)data.get(j);
                            VariantType varJ = recJ.getVariantType();

                            n1 = 0;
                            int n11 = 0;
                            for (int k = 0; k < participantCount; k++) {
                                if (recJ.getVariantsForParticipant(k)[0] == varJ) {
                                    n1++;
                                    if (recI.getVariantsForParticipant(k)[0] == varI) {
                                        n11++;
                                    }
                                }
                            }
                            double q1 = n1 / (double)participantCount;
                            double q2 = 1.0 - q1;
                            if (q1 > 0.0 && q1 < 1.0) {
                                double x11 = n11 / (double)participantCount;
                                double d = x11 - p1 * q1;

                                // D'
                                double dMax = d < 0.0 ? -Math.min(p1 * q1, p2 * q2) : Math.min(p1 * q2, p2 * q1);
                                dPrimes[i][j] = (float)(d / dMax);
                                rSquareds[i][j] = (float)(d * d / (p1 * p2 * q1 * q2));
                            } else {
                                dPrimes[i][j] = Float.NaN;
                                rSquareds[i][j] = Float.NaN;
                            }
                        } else {
                            dPrimes[i][j] = Float.NaN;
                            rSquareds[i][j] = Float.NaN;
                        }
                    }
                }
            }
        }
    }
    
    void recalculate() {
        dPrimes = null;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        GradientPaint gp0 = new GradientPaint(0, 0, ColourSettings.getColor(ColourKey.GRAPH_PANE_BACKGROUND_TOP), 0, getHeight(), ColourSettings.getColor(ColourKey.GRAPH_PANE_BACKGROUND_BOTTOM));
        g2.setPaint(gp0);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        calculateLD();
        float[][] ldData = controller.isDPrimeSelected() ? dPrimes : rSquareds;
        
        if (ldData != null && ldData.length != 0) {
            int n = ldData.length;
            double h = getHeight() / n;
            double w = (getWidth() - AXIS_WIDTH) * 2.0 / n;
            unitHeight = Math.min(h, w);

            ColourAccumulator accumulator = new ColourAccumulator(null);
            Color transparent = new Color(0, 0, 0, 0);

            x0 = getWidth() - AXIS_WIDTH;
            y0 = (getHeight() - n * unitHeight) * 0.5;
            Path2D[] zonePaths = new Path2D[n];
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Shape diamond = getDiamond(i, j);
                    if (Float.isNaN(ldData[i][j])) {
                        accumulator.addShape(transparent, diamond);
                    } else {
                        accumulator.addShape(createBlend(ldData[i][j]), diamond);
                        addToZone(diamond, zonePaths, i);
                        addToZone(diamond, zonePaths, j);
                    }
                }
            }
            accumulator.fill(g2);
            if (unitHeight > 10.0) {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(0.5f));
                accumulator.draw(g2);
            }
            drawAxis(g2);
            drawLegend(g2);
            zones = new Area[n];
            for (int i = 0; i < n; i++) {
                if (zonePaths[i] != null) {
                    zones[i] = new Area(zonePaths[i]);
                } else {
                    zones[i] = new Area();
                }
            }
        }
    }

    private void addToZone(Shape diamond, Path2D[] zones, int z) {
        if (zones[z] == null) {
            zones[z] = new Path2D.Double();
        }
        zones[z].append(diamond.getPathIterator(null), false);
    }

    /**
     * Calculate the diamond at the intersection of variants i and j.
     */
    private Shape getDiamond(int i, int j) {
        double dj = 0.5 * (j - i - 1) * unitHeight;
        return MiscUtils.createPolygon(x0 - 0.5 * unitHeight - dj, y0 + (i + 0.5) * unitHeight + dj, x0 - dj, y0 + (i + 1) * unitHeight + dj, x0 - 0.5 * unitHeight - dj, y0 + (i + 1.5) * unitHeight + dj, x0 - unitHeight - dj, y0 + (i + 1) * unitHeight + dj);
    }

    private void drawAxis(Graphics2D g2) {

        List<VariantRecord> data = controller.getData();
        if (data != null) {
            ColourScheme cs = new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.INSERTED_BASE, ColourKey.DELETED_BASE);
            ColourAccumulator accumulator = new ColourAccumulator(cs);

            double y = y0;
            for (VariantRecord varRec: data) {

                Rectangle2D rect = new Rectangle2D.Double(getWidth() - AXIS_WIDTH, y, AXIS_WIDTH, unitHeight);
                switch (varRec.getVariantType()) {
                    case SNP_A:
                        accumulator.addBaseShape('A', rect);
                        break;
                    case SNP_C:
                        accumulator.addBaseShape('C', rect);
                        break;
                    case SNP_G:
                        accumulator.addBaseShape('G', rect);
                        break;
                    case SNP_T:
                        accumulator.addBaseShape('T', rect);
                        break;
                    case INSERTION:
                        accumulator.addShape(ColourKey.INSERTED_BASE, rect);
                        break;
                    case DELETION:
                        accumulator.addShape(ColourKey.DELETED_BASE, rect);
                        break;
                }
                y += unitHeight;
            }
            accumulator.fill(g2);
            
            if (unitHeight > 10.0) {
                y = y0;
                g2.setColor(ColourSettings.getColor(ColourKey.AXIS_GRID));
                Font tickFont = g2.getFont().deriveFont(Font.PLAIN, 9);
                g2.setFont(tickFont);
                FontMetrics fm = g2.getFontMetrics();
                float baselineOffset = fm.getAscent() - fm.getHeight() * 0.5f;

                for (VariantRecord varRec: data) {
                    String name = varRec.getName();
                    if (name == null || name.isEmpty()) {
                        name = varRec.getReference() + ":" + varRec.getPosition();
                    }
                    Rectangle2D labelRect = fm.getStringBounds(name, g2);
                    double baseline = y + unitHeight * 0.5 + baselineOffset;
                    g2.drawString(name, (float)(getWidth() - AXIS_WIDTH + (AXIS_WIDTH - labelRect.getWidth()) * 0.5), (float)baseline);
                    y += unitHeight;
                }
            }
        }
    }

    /**
     * Draw a legend in the lower right of the LD plot.
     * @param g2 
     */
    private void drawLegend(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Rectangle2D legendRect = new Rectangle2D.Double(LEGEND_MARGIN, getHeight() - LEGEND_HEIGHT - LEGEND_MARGIN, LEGEND_WIDTH, LEGEND_HEIGHT);

        // We support the use of a three-colour heat-map, so we have to draw the legend in two pieces.
        g2.setPaint(new GradientPaint(0.0f, (float)legendRect.getMinY(), createBlend(0.0), 0.0f, (float)legendRect.getCenterY(), createBlend(0.5)));
        g2.fill(new Rectangle2D.Double(legendRect.getX(), legendRect.getY(), legendRect.getWidth(), legendRect.getHeight() * 0.5));
        g2.setPaint(new GradientPaint(0.0f, (float)legendRect.getCenterY(), createBlend(0.5), 0.0f, (float)legendRect.getMaxY(), createBlend(1.0)));
        g2.fill(new Rectangle2D.Double(legendRect.getX(), legendRect.getCenterY(), legendRect.getWidth(), legendRect.getHeight() * 0.5));

        g2.setColor(ColourSettings.getColor(ColourKey.INTERVAL_LINE));
        g2.draw(legendRect);
        
        float x = (float)legendRect.getMaxX();
        drawTick(g2, x, (float)legendRect.getMinY(), 0.0);
        drawTick(g2, x, (float)legendRect.getCenterY(), 0.5);
        drawTick(g2, x, (float)legendRect.getMaxY(), 1.0);
    }

    private void drawTick(Graphics2D g2, float x, float y, double val) {
        g2.draw(new Line2D.Float(x, y, x + TICK_LENGTH, y));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(Double.toString(val), x + TICK_LENGTH + 1.0f, y + fm.getAscent() - fm.getHeight() * 0.5f);
    }

    private static Color createBlend(double val) {
        int i0 = 0;
        int i1 = 1;
        double w1 = val * 2.0;
        if (val > 0.5) {
            i0 = 1;
            i1 = 2;
            w1 -= 1.0;
        }
        double w0 = 1.0f - w1;
        return new Color((int)(HEATMAP_COLORS[i0].getRed() * w0 + HEATMAP_COLORS[i1].getRed() * w1), (int)(HEATMAP_COLORS[i0].getGreen() * w0 + HEATMAP_COLORS[i1].getGreen() * w1), (int)(HEATMAP_COLORS[i0].getBlue() * w0 + HEATMAP_COLORS[i1].getBlue() * w1));
    }

    @Override
    public VariantRecord pointToVariantRecord(Point pt) {
        if (pt.x >= getWidth() - AXIS_WIDTH) {
            int logicalY = (int)((pt.y - y0) / unitHeight);
            List<VariantRecord> data = controller.getData();
            if (data != null && logicalY >= 0 && logicalY < data.size()) {
                return data.get(logicalY);
            }
        }
        return null;
    }

    /**
     * Given a point on this panel, figure out which record it corresponds to.  We only
     * get a non-null VariantRecord when we're in the axis at right.  Otherwise we have
     * to depend on pointToRecord to give us an LDRecord.
     *
     * @param pt the point in which we're interested, in panel coordinates
     * @return a VariantRecord or an LDRecord, depending on where the mouse is
     */
    @Override
    public Record pointToRecord(Point pt) {
        Record result = pointToVariantRecord(pt);
        if (result == null) {
            // We may be somewhere in the diamonds.  Figure out which one.
            int i = -1;
            for (int j = 0; j < zones.length; j++) {
                if (zones[j].contains(pt)) {
                    if (i < 0) {
                        i = j;
                    } else {
                        List<VariantRecord> data = controller.getData();
                        result = new LDRecord(data.get(i), data.get(j), dPrimes[i][j], rSquareds[i][j]);
                        break;
                    }
                }
            }
        }
        return result;
    }
}
