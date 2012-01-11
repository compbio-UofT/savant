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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JPanel;

import savant.api.data.VariantRecord;
import savant.api.util.RangeUtils;
import savant.controller.GraphPaneController;
import savant.controller.LocationController;
import savant.settings.BrowserSettings;
import savant.settings.ColourSettings;
import savant.util.ColourAccumulator;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.Range;


/**
 *
 * @author tarkvara
 */
public class VariantMap extends JPanel {
    
    private VariationPanel owner;
    
    private double unitHeight;
    private double unitWidth;

    VariantMap(VariationPanel owner) {
        this.owner = owner;
        setFont(BrowserSettings.getTrackFont());

        MouseAdapter listener = new MouseAdapter() {
            /**
             * Override mouseMoved because the x-position we report is actually derived from
             * our y-position.
             */
            @Override
            public void mouseMoved(MouseEvent event) {
                VariantRecord rec = pointToRecord(event.getPoint().y);
                if (rec != null) {
                    GraphPaneController.getInstance().setMouseXPosition(rec.getInterval().getStart());
                } else {
                    GraphPaneController.getInstance().setMouseXPosition(-1);
                }
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                VariantRecord rec = pointToRecord(event.getPoint().y);
                if (rec != null) {
                    LocationController.getInstance().setLocation((Range)RangeUtils.addMargin(new Range(rec.getInterval().getStart(), rec.getInterval().getEnd())));
                }
            }
        };
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.clearRect(0, 0, getWidth(), getHeight());
        List<VariantRecord> data = owner.getData();
        if (data == null || data.isEmpty()) {
            Font font = g2.getFont().deriveFont(Font.PLAIN, 36);
            g2.setColor(ColourSettings.getColor(ColourKey.GRAPH_PANE_MESSAGE));
            FontMetrics metrics = g2.getFontMetrics();
            String message = "No data in range";
            Rectangle2D stringBounds = font.getStringBounds(message, g2.getFontRenderContext());

            int x = (getWidth() - (int)stringBounds.getWidth()) / 2;
            int y = (getHeight() / 2) + ((metrics.getAscent()- metrics.getDescent()) / 2);

            g2.setFont(font);
            g2.drawString(message, x,y);
        } else {
            int participantCount = owner.getParticipantCount();
            unitHeight = getHeight() / data.size();
            unitWidth = getWidth() / participantCount;

            ColourScheme cs = new ColourScheme(ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.INSERTED_BASE, ColourKey.DELETED_BASE);
            ColourAccumulator accumulator = new ColourAccumulator(cs);
            

            double y = 0.0;
            for (VariantRecord varRec: data) {

                double x = 0.0;
                for (int j = 0; j < participantCount; j++) {
                    Rectangle2D rect = new Rectangle2D.Double(x, y, unitWidth, unitHeight);
                    switch (varRec.getVariantForParticipant(j)) {
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
                    x += unitWidth;
                }
                y += unitHeight;
            }
            accumulator.render(g2);
        }
    }

    /**
     * Given a point on this panel, figure out which record it corresponds to.
     * @param y y-coordinate of the point we're interested in
     * @return 
     */
    private VariantRecord pointToRecord(double y) {
        int logicalY = (int)(y / unitHeight);
        List<VariantRecord> data = owner.getData();
        if (data != null && logicalY >= 0 && logicalY < data.size()) {
            return (VariantRecord)data.get(logicalY);
        }
        return null;
    }
}
