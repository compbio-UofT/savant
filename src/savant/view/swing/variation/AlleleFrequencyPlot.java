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
import java.awt.geom.GeneralPath;
import java.util.List;
import javax.swing.JPanel;
import savant.api.data.VariantRecord;
import savant.settings.ColourSettings;
import savant.util.ColourKey;

/**
 * Simple graph of allele frequency for each point.
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
            unitWidth = (double)w / participantCount;
            
            double y = 0.0;
            GeneralPath path = new GeneralPath();
            path.moveTo(0.0, y);
            for (VariantRecord varRec: data) {
                
                y += unitHeight;
            }
        }
    }
    
    private double[] calculateFrequencies(VariantRecord varRec) {
        int total;
        double[] alleleTotals = new double[varRec.getAltAlleles().length + 1];
        for (int i = 0; i < varRec.getParticipantCount(); i++) {
            int[] alleles = varRec.getAllelesForParticipant(i);
            if (alleles.length == 1) {
                alleleTotals[alleles[0]]++;
            } else {
                alleleTotals[alleles[0]] += 0.5;
                alleleTotals[alleles[1]] += 0.5;
            }
        }
        return alleleTotals;
    }
}
