/*
 *    Copyright 2010 University of Toronto
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

package savant.view.swing.point;

import savant.data.types.PointRecord;
import savant.util.DrawingInstructions;
import savant.util.Resolution;
import savant.util.ColorScheme;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;
import savant.view.swing.util.GlassMessagePane;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class PointTrackRenderer extends TrackRenderer {

    private static final int GLASS_PANE_WIDTH = 300;
    private static final String GLASS_PANE_MESSAGE = "Zoom in to see points";

    public PointTrackRenderer() { this(new DrawingInstructions()); }

    public PointTrackRenderer(
            DrawingInstructions drawingInstructions) {
        super(drawingInstructions);
    }

    @Override
    public void render(Graphics g, GraphPane gp) {

        Graphics2D g2 = (Graphics2D) g;
        gp.setIsOrdinal(true);
        this.clearShapes();

        double width = gp.getUnitWidth();

        DrawingInstructions di = this.getDrawingInstructions();

        Boolean refexists = (Boolean) di.getInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS);
        if (!refexists) {
            GlassMessagePane.draw(g2, gp, "no data for reference", GLASS_PANE_WIDTH);
            return;
        }

        Resolution r = (Resolution) di.getInstruction(DrawingInstructions.InstructionName.RESOLUTION.toString());

        List<Object> data = this.getData();

        // don't draw things which are too small to be seen: less than 1 pixel wide
        if (width < 1 || data == null) {
            // display informational glass pane
            GlassMessagePane.draw(g2, gp, GLASS_PANE_MESSAGE, GLASS_PANE_WIDTH);
            return;

        }
        int numdata = this.getData().size();


        if (r == Resolution.VERY_HIGH) {
            
            ColorScheme cs = (ColorScheme) di.getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
            Color bgcolor = cs.getColor("Background");
            Color linecolor = cs.getColor("Line");

            float pointiness = 0.1F;

            for (int i = 0; i < numdata; i++) {

                Polygon p = new Polygon();

                PointRecord record = (PointRecord)data.get(i);
                savant.data.types.Point sp = record.getPoint();

                Point2D.Double p1 = new Point2D.Double(gp.transformXPos(sp.getPosition()),0);
                Point2D.Double p2 = new Point2D.Double(gp.transformXPos(sp.getPosition()+1),0);
                Point2D.Double p3 = new Point2D.Double(gp.transformXPos(sp.getPosition()+1),gp.getHeight());
                Point2D.Double p4 = new Point2D.Double(gp.transformXPos(sp.getPosition()),gp.getHeight());

                p.addPoint((int)p1.x, (int)p1.y);
                p.addPoint((int)p2.x, (int)p2.y);
                p.addPoint((int)p3.x, (int)p3.y);
                p.addPoint((int)p4.x, (int)p4.y);

                g2.setColor(bgcolor);
                g2.fillPolygon(p);

                //add shape and info to list
                //this.dataShapes.add(p);
                this.recordToShapeMap.put(record, p);

                if (width > 5) {
                    g2.setColor(linecolor);
                    g2.drawPolygon(p);
                }
            }
        }
    }

    @Override
    public boolean isOrdinal() {
        return true;
    }

    @Override
    public Range getDefaultYRange() {
        return new Range(0,1);
    }
}
