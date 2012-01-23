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

import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

import savant.api.data.Strand;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.settings.ColourSettings;
import savant.util.ColourAccumulator;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.Pileup;
import savant.view.tracks.VariantTrackRenderer;


/**
 * Simple graph of allele frequency for each point.
 *
 * @author tarkvara
 */
public class AlleleFrequencyPlot extends JPanel {
    VariationSheet owner;
    double unitHeight;
    double unitWidth;

    AlleleFrequencyPlot(VariationSheet v) {
        owner = v;
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

        List<VariantRecord> data = owner.getData();
        if (data != null && !data.isEmpty()) {
            int participantCount = owner.getParticipantCount();
            unitHeight = (double)h / data.size();
            unitWidth = (double)w / (participantCount * 2.0);
            
            List<Pileup> pileups = new ArrayList<Pileup>(data.size());
            for (int i = 0; i < data.size(); i++) {
                VariantRecord varRec = data.get(i);
                Pileup pile = new Pileup(varRec.getPosition());
                pileups.add(pile);
                
                for (int j = 0; j < varRec.getParticipantCount(); j++) {
                    VariantType[] jVariants = varRec.getVariantsForParticipant(j);
                    if (jVariants.length == 1) {
                        pile.pileOn(jVariants[0], 1.0, null);
                        pile.pileOn(jVariants[0], 1.0, null);
                    } else {
                        pile.pileOn(jVariants[0], 1.0, null);
                        pile.pileOn(jVariants[1], 1.0, null);
                    }
                }
            }

            ColourAccumulator accumulator = new ColourAccumulator(new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.INSERTED_BASE, ColourKey.DELETED_BASE));

            double y = 0.0;
            double x = 0.0;

            for (Pileup p : pileups) {
                VariantType snpNuc;
                while ((snpNuc = p.getLargestVariantType(VariantType.NONE)) != null) {
                    Rectangle2D rect = new Rectangle2D.Double(x, y, p.getCoverage(snpNuc, null) * unitWidth, unitHeight);
                    VariantTrackRenderer.accumulateShape(snpNuc, accumulator, rect);
                    p.clearVariantType(snpNuc);
                }
                y += unitHeight;
            }

            accumulator.fill(g2);
        }
    }
}
