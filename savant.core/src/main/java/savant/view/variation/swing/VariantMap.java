/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.view.variation.swing;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.controller.LocationController;
import savant.settings.BrowserSettings;
import savant.settings.ColourSettings;
import savant.util.ColourAccumulator;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.Range;
import savant.view.tracks.VariantTrackRenderer;
import savant.view.variation.ParticipantRecord;
import savant.view.variation.VariationController;


/**
 * Map-like overview of all variant tracks.
 *
 * @author tarkvara
 */
public class VariantMap extends VariationPlot {
    private static final Log LOG = LogFactory.getLog(VariantMap.class);

    VariantMap(VariationController vc) {
        super(vc);
        setFont(BrowserSettings.getTrackFont());

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
        
        List<VariantRecord> data = controller.getData();
        if (data != null && !data.isEmpty()) {
            double boxTop = Double.NaN;
            double boxBottom = Double.NaN;
            Range browseRange = LocationController.getInstance().getRange();

            int participantCount = controller.getParticipantCount();
            unitHeight = (double)h / data.size();
            unitWidth = (double)w / participantCount;

            boolean gappable = unitHeight > GAP_HEIGHT * 2.0;

            ColourScheme cs = new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.INSERTED_BASE, ColourKey.DELETED_BASE, ColourKey.N);
            ColourAccumulator accumulator = new ColourAccumulator(cs);


            double y = 0.0;
            double topGap = 0.0;
            for (int i = 0; i < data.size(); i++) {
                VariantRecord varRec = data.get(i);

                double bottomGap = 0.0;
                if (gappable && i + 1 < data.size()) {
                    VariantRecord nextRec = data.get(i + 1);
                    if (nextRec.getPosition() - varRec.getPosition() > 1) {
                        bottomGap = GAP_HEIGHT * 0.5;
                    }
                }

                if (Double.isNaN(boxTop) && browseRange.getFrom() <= varRec.getPosition() && browseRange.getTo() >= varRec.getPosition()) {
                    boxTop = y + topGap;
                    boxBottom = boxTop + unitHeight;
                } else if (varRec.getPosition() <= browseRange.getTo()) {
                    boxBottom = y + unitHeight - bottomGap;
                }
                double x = 0.0;
                for (int j = 0; j < participantCount; j++) {
                    VariantTrackRenderer.accumulateZygoteShapes(varRec.getVariantsForParticipant(j), accumulator, new Rectangle2D.Double(x, y + topGap, unitWidth, unitHeight - topGap - bottomGap));
                    x += unitWidth;
                }
                topGap = bottomGap;
                y += unitHeight;
            }
            accumulator.fill(g2);

            if (gappable) {
                drawGapSizes(g2);
            } else {
                // Not enough room to draw gaps, so just label the axes.
                labelVerticalAxis(g2);
            }
        
            g2.setClip(null);
            g2.setColor(Color.BLUE);
            g2.draw(new Rectangle2D.Double(0.0, boxTop, w - 1.0, boxBottom - boxTop - 1.0));
        }
    }

    /**
     * Given a point on this panel, figure out which participant it corresponds to.
     * @param pt the point we're interested in
     * @return ParticipantRecord corresponding to the mouse location
     */
    @Override
    public Record pointToRecord(Point pt) {
        VariantRecord varRec = pointToVariantRecord(pt);
        if (varRec != null) {
            int logicalX = (int)(pt.x / unitWidth);
            return new ParticipantRecord(varRec, logicalX, controller.getParticipants()[logicalX]);
        }
        // Return null for the blank spaces between participants.
        return null;
    }
}
