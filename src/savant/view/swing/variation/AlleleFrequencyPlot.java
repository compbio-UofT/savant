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

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.settings.ColourSettings;
import savant.util.ColourAccumulator;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.MiscUtils;
import savant.util.Pileup;


/**
 * Simple graph of allele frequency for each point.
 *
 * @author tarkvara
 */
public class AlleleFrequencyPlot extends JPanel implements VariationPanel {
    private static final int NUM_AXIS_STEPS = 10;

    VariationController controller;
    double unitHeight;

    AlleleFrequencyPlot(VariationController vc) {
        controller = vc;
        VariantPopper popper = new VariantPopper(this);
        addMouseListener(popper);
        addMouseMotionListener(popper);
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Paint a gradient from top to bottom
        int h = getHeight();
        int w = getWidth();
        GradientPaint gp0 = new GradientPaint(0, 0, ColourSettings.getColor(ColourKey.GRAPH_PANE_BACKGROUND_TOP), 0, h, ColourSettings.getColor(ColourKey.GRAPH_PANE_BACKGROUND_BOTTOM));
        g2.setPaint(gp0);
        g2.fillRect(0, 0, w, h);

        // Calculate the number of cases and controls.  Since each has two alleles (in theory), we multiply by 2.
        int numControls = 0;
        int numCases = controller.getParticipantCount() * 2;
        for (String p: controller.participantNames) {
            if (controller.controls.contains(p)) {
                numControls += 2;
                numCases -= 2;
            }
        }

        List<VariantRecord> data = controller.getData();
        if (data != null && !data.isEmpty()) {
            unitHeight = (double)h / data.size();
            
            List<Pileup> controlPileups = new ArrayList<Pileup>(data.size());
            List<Pileup> casePileups = new ArrayList<Pileup>(data.size());
            for (int i = 0; i < data.size(); i++) {
                VariantRecord varRec = data.get(i);
                Pileup controlPile = new Pileup(varRec.getPosition());
                Pileup casePile = new Pileup(varRec.getPosition());
                controlPileups.add(controlPile);
                casePileups.add(casePile);
                
                for (int j = 0; j < varRec.getParticipantCount(); j++) {
                    VariantType[] jVariants = varRec.getVariantsForParticipant(j);
                    Pileup pile = casePile;
                    if (numControls > 0 && controller.controls.contains(controller.participantNames.get(j))) {
                        pile = controlPile;
                    }
                    if (jVariants.length == 1) {
                        pile.pileOn(jVariants[0], 1.0, null);
                        pile.pileOn(jVariants[0], 1.0, null);
                    } else {
                        pile.pileOn(jVariants[0], 1.0, null);
                        pile.pileOn(jVariants[1], 1.0, null);
                    }
                }
            }
            
            // This call sets the clip so that axes and data don't overwrite our numbers.
            drawAxes(g2, numControls > 0);

            ColourScheme scheme = new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.INSERTED_BASE, ColourKey.DELETED_BASE);
            ColourAccumulator accumulator = new ColourAccumulator(scheme);

            double y = 0.0;
            double x = 0.0;
            
            if (numControls > 0) {
                x = getWidth() * 0.5;
                for (Pileup p : controlPileups) {
                    VariantType snpNuc;
                    double unitWidth = w * 0.5 / numControls;
                    while ((snpNuc = p.getLargestVariantType(VariantType.NONE)) != null) {
                        double barWidth = p.getCoverage(snpNuc, null) * unitWidth;
                        Rectangle2D rect = new Rectangle2D.Double(x - barWidth, y, barWidth, unitHeight);
                        accumulator.addShape(scheme.getVariantColor(snpNuc), rect);
                        p.clearVariantType(snpNuc);
                    }
                    y += unitHeight;
                }
                y = 0.0;
            }

            for (Pileup p : casePileups) {
                VariantType snpNuc;
                double unitWidth = (double)w / numCases;
                if (numControls > 0) {
                    unitWidth *= 0.5;
                }
                while ((snpNuc = p.getLargestVariantType(VariantType.NONE)) != null) {
                    Rectangle2D rect = new Rectangle2D.Double(x, y, p.getCoverage(snpNuc, null) * unitWidth, unitHeight);
                    accumulator.addShape(scheme.getVariantColor(snpNuc), rect);
                    p.clearVariantType(snpNuc);
                }
                y += unitHeight;
            }

            accumulator.fill(g2);
            g2.setClip(null);
            
            if (numControls > 0) {
                g2.setColor(ColourSettings.getColor(ColourKey.AXIS_GRID));
                g2.draw(new Line2D.Double(w * 0.5, 0.0, w * 0.5, h));
            }
        }
    }

    private void drawAxes(Graphics2D g2, boolean hasControls) {
        // We don't want the axes stomping on our labels, so make sure the clip excludes them.
        int h = getHeight();
        int w = getWidth();
        Area clipArea = new Area(new Rectangle(0, 0, w, h));
        g2.setColor(ColourSettings.getColor(ColourKey.AXIS_GRID));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9));
        FontMetrics fm = g2.getFontMetrics();

        double verticalAxis = 0.0;
        int numSteps = NUM_AXIS_STEPS;
        if (hasControls) {
            verticalAxis = getWidth() * 0.5;
            numSteps /= 2;
        }
        double step = w / (double)NUM_AXIS_STEPS;
        
        double x = step;
        for (int i = 1; i < numSteps; i++) {
            String s = String.format("%.1f", i / (double)numSteps);
            Rectangle2D labelRect = fm.getStringBounds(s, g2);
            labelRect = new Rectangle2D.Double(verticalAxis + x - labelRect.getWidth() * 0.5 - 1.0, 1.0, labelRect.getWidth() + 2.0, labelRect.getHeight() + 2.0);
            MiscUtils.drawMessage(g2, s, labelRect);
            clipArea.subtract(new Area(labelRect));
            if (hasControls) {
                labelRect = new Rectangle2D.Double(verticalAxis - x - labelRect.getWidth() * 0.5, 1.0, labelRect.getWidth(), labelRect.getHeight());
                MiscUtils.drawMessage(g2, s, labelRect);
                clipArea.subtract(new Area(labelRect));
            }
            g2.setClip(clipArea);
            g2.draw(new Line2D.Double(verticalAxis + x, 0, verticalAxis + x, h));
            if (hasControls) {
                g2.draw(new Line2D.Double(verticalAxis - x, 0, verticalAxis - x, h));
            }
            x += step;
        }
    }

    /**
     * Allele plot has only variant record, and not participants.
     */
    @Override
    public Record pointToRecord(Point pt) {
        return pointToVariantRecord(pt);
    }

    @Override
    public VariantRecord pointToVariantRecord(Point pt) {
        int logicalY = (int)(pt.y / unitHeight);
        List<VariantRecord> data = controller.getData();
        if (data != null && logicalY >= 0 && logicalY < data.size()) {
            return data.get(logicalY);
        }
        return null;
    }
}
