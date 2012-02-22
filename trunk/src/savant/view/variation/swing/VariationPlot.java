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
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JPanel;

import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.settings.ColourSettings;
import savant.util.ColourKey;
import savant.util.MiscUtils;
import savant.view.variation.VariationController;


/**
 * Shared functionality of all our variation-related plots which allows us to display popups appropriately.
 * 
 * @author tarkvara
 */
public abstract class VariationPlot extends JPanel {

    /** Height in pixels of gap between blocks (when we have enough space to draw gap sizes). */
    protected static final double GAP_HEIGHT = 9.0;

    protected final VariationController controller;
    protected double unitHeight;
    protected double unitWidth;

    VariationPlot(VariationController vc) {
        controller = vc;
    }

    /**
     * Returns either a synthesised ParticipantRecord or the VariantRecord associated with this point.
     * Default (used by frequency and LD plots) is to return variants only, and not participants.
     */
    public Record pointToRecord(Point pt) {
        return pointToVariantRecord(pt);
    }

    /**
     * Given a point on this panel, figure out which record it corresponds to.
     * @param pt the point we're interested in
     */
    public VariantRecord pointToVariantRecord(Point pt) {
        int logicalY = (int)(pt.y / unitHeight);
        List<VariantRecord> data = controller.getData();
        if (data != null && logicalY >= 0 && logicalY < data.size()) {
            return data.get(logicalY);
        }
        return null;
    }

    /**
     * At low resolutions we draw the axis lines to indicate the non-linearity.
     */
    protected void labelVerticalAxis(Graphics2D g2) {
        List<VariantRecord> data = controller.getData();

        if (data.size() > 0) {
            int[] ticks = MiscUtils.getTickPositions(controller.getVisibleRange());

            Color gridColor = ColourSettings.getColor(ColourKey.AXIS_GRID);

            // Smallish font for tick labels.
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9));
            g2.setColor(gridColor);
            FontMetrics fm = g2.getFontMetrics();

            // We don't want the axes stomping on our labels, so make sure the clip excludes them.
            Area clipArea = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
            double[] ys = new double[ticks.length];

            int index = 0;
            float labelX = 0.0F;    // Labels may get stacked up.
            for (int i = 0; i < ticks.length && index < data.size(); i++) {
                int t = ticks[i];
                while (index < data.size() && t > data.get(index).getPosition()) {
                    index++;
                    labelX = 0.0F;
                }
                double y = index * unitHeight;
                ys[i] = y;

                String s = Integer.toString(t);
                Rectangle2D labelRect = fm.getStringBounds(s, g2);
                double baseline = y + fm.getAscent() - fm.getHeight() * 0.5;
                g2.drawString(s, labelX + 4.0F, (float)baseline);
                clipArea.subtract(new Area(new Rectangle2D.Double(labelX + 3.0, baseline - labelRect.getHeight() - 1.0, labelRect.getWidth() + 2.0, labelRect.getHeight() + 2.0)));
                labelX += labelRect.getWidth() + 2.0F;
            }
            g2.setClip(clipArea);
            for (int i = 0; i < ticks.length; i++) {
                double y = ys[i];
                g2.draw(new Line2D.Double(0.0, y, getWidth(), y));
            }
           g2.setClip(null);
        }
    }
    
    /**
     * If we have enough room, we draw the gap sizes between the variants.  Currently
     * only used for the VariantMap.  We tried it on the AlleleFrequencyPlot, but it looked stupid.
     */
    protected Area drawGapSizes(Graphics2D g2) {
        Area result = new Area();
        List<VariantRecord> data = controller.getData();
        if (data.size() > 1) {
            Color gridColor = ColourSettings.getColor(ColourKey.AXIS_GRID);

            // Tiny font for gap labels.
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 8));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(gridColor);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double y = unitHeight;
            double w = getWidth();
            for (int i = 1; i < data.size(); i++) {
                int gapSize = data.get(i).getPosition() - data.get(i - 1).getPosition() - 1;
                if (gapSize > 0) {
                    String s = gapSize > 1 ? String.format("%d bases", gapSize) : "1 base";

                    Rectangle2D labelRect = fm.getStringBounds(s, g2);
                    double baseline = y + fm.getAscent() - fm.getHeight() * 0.5;
                    g2.drawString(s, (float)((w - labelRect.getWidth()) * 0.5), (float)baseline);
                    g2.draw(new Line2D.Double(0.0, y - GAP_HEIGHT * 0.5, w, y - GAP_HEIGHT * 0.5));
                    g2.draw(new Line2D.Double(0.0, y + GAP_HEIGHT * 0.5, w, y + GAP_HEIGHT * 0.5));
                    result.add(new Area(new Rectangle2D.Double(0.0, y - GAP_HEIGHT * 0.5, w, GAP_HEIGHT)));
                }
                y += unitHeight;
            }
        }
        return result;
    }
}
