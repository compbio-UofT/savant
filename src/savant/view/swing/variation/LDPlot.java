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
package savant.view.swing.variation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.util.ColourAccumulator;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.MiscUtils;


/**
 * A Linkage Disequilibrium plot.
 *
 * @author tarkvara
 */
public class LDPlot extends JPanel {
    private static final Log LOG = LogFactory.getLog(LDPlot.class);
    private static final int AXIS_WIDTH = 24;
    private static final float[][] HEATMAP_COLORS = {{ 0.000f, 0.000f, 1.000f }, { 0.750f, 0.500f, 0.750f }, { 1.000f, 0.000f, 0.000f }};
//{{ 0.996f, 0.878f, 0.824f }, { 0.988f, 0.573f, 0.447f }, { 0.871f, 0.176f, 0.149f }};
//        { 0.996f, 0.910f, 0.784f }, { 0.992f, 0.733f, 0.518f }, { 0.890f, 0.290f, 0.200f }};

    private float[][] ldData;
    private double x0, y0;
    private double unitHeight;

    VariationSheet owner;

    LDPlot(VariationSheet p) {
        this.owner = p;
        MouseAdapter listener = new MouseAdapter() {
            /**
             * We're only interested in mouse-moves over near the right edge.
             */
            @Override
            public void mouseMoved(MouseEvent event) {
                VariantRecord rec = null;
                if (event.getX() >= getWidth() - AXIS_WIDTH) {
                    rec = pointToRecord(event.getPoint().y);
                }
                owner.updateStatusBar(rec);
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getX() >= getWidth() - AXIS_WIDTH) {
                    owner.navigateToRecord(pointToRecord(event.getPoint().y));
                }
            }
        };
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    public void calculateLD() {
        if (ldData == null) {
            List<VariantRecord> data = owner.getData();
            if (data != null && data.size() > 0) {
                int participantCount = owner.getParticipantCount();

                ldData = new float[data.size()][data.size()];
                for (int i = 0; i < data.size(); i++) {
                    VariantRecord recI = (VariantRecord)data.get(i);
                    VariantType varI = recI.getVariantType();
                    int n1 = 0;
                    for (int k = 0; k < participantCount; k++) {
                        if (recI.getVariantForParticipant(k) == varI) {
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
                                if (recJ.getVariantForParticipant(k) == varJ) {
                                    n1++;
                                    if (recI.getVariantForParticipant(k) == varI) {
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
                                float dPrime = (float)(d / dMax);

                                // r-squared
                                float rSquared = (float)(d * d / (p1 * p2 * q1 * q2));
    //                            System.out.println("D[" + i + "(" + varI + ")][" + j + "(" + varJ + ")]=" + d + "\tx11=" + x11 + "\tp1=" + p1 + "\tq1=" + q1 + "\tD'=" + dPrime + "\tr²=" + rSquared);
                                ldData[i][j] = dPrime;
                            } else {
                                ldData[i][j] = Float.NaN;
                            }
                        } else {
                            ldData[i][j] = Float.NaN;
                        }
                    }
                }
            }
        }
    }
    
    void forceRedraw() {
        ldData = null;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.clearRect(0, 0, getWidth(), getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        calculateLD();
        
        if (ldData == null || ldData.length == 0) {
            owner.drawNoDataMessage(g2, getSize());
        } else {
            int n = ldData.length;
            double h = getHeight() / n;
            double w = (getWidth() - AXIS_WIDTH) * 2.0 / n;
            unitHeight = Math.min(h, w);

            ColourAccumulator accumulator = new ColourAccumulator(null);

            x0 = getWidth() - AXIS_WIDTH;
            y0 = (getHeight() - n * unitHeight) * 0.5;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Shape diamond = getDiamond(i, j);
                    if (Float.isNaN(ldData[i][j])) {
                        accumulator.addShape(g2.getBackground(), diamond);
                    } else {
                        accumulator.addShape(createBlend(ldData[i][j]), diamond);
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
        }
    }

    /**
     * Calculate the diamond at the intersection of variants i and j.
     */
    private Shape getDiamond(int i, int j) {
        double dj = 0.5 * (j - i - 1) * unitHeight;
        return MiscUtils.createPolygon(x0 - 0.5 * unitHeight - dj, y0 + (i + 0.5) * unitHeight + dj, x0 - dj, y0 + (i + 1) * unitHeight + dj, x0 - 0.5 * unitHeight - dj, y0 + (i + 1.5) * unitHeight + dj, x0 - unitHeight - dj, y0 + (i + 1) * unitHeight + dj);
    }

    private void drawAxis(Graphics2D g2) {

        List<VariantRecord> data = owner.getData();
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
        }
    }

    private static Color createBlend(float val) {
        int i0 = 0;
        int i1 = 1;
        float w1 = val * 2.0f;
        if (val > 0.5f) {
            i0 = 1;
            i1 = 2;
            w1 -= 1.0f;
        }
        float w0 = 1.0f - w1;
        return new Color(HEATMAP_COLORS[i0][0] * w0 + HEATMAP_COLORS[i1][0] * w1, HEATMAP_COLORS[i0][1] * w0 + HEATMAP_COLORS[i1][1] * w1, HEATMAP_COLORS[i0][2] * w0 + HEATMAP_COLORS[i1][2] * w1);
    }

    /**
     * Given a point on this panel, figure out which record it corresponds to.
     * @param y y-coordinate of the point we're interested in
     * @return 
     */
    private VariantRecord pointToRecord(double y) {
        int logicalY = (int)((y - y0) / unitHeight);
        List<VariantRecord> data = owner.getData();
        if (data != null && logicalY >= 0 && logicalY < data.size()) {
            return (VariantRecord)data.get(logicalY);
        }
        return null;
    }
}
