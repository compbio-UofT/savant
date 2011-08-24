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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import savant.data.types.PointRecord;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.util.DrawingInstruction;
import savant.util.Resolution;
import savant.util.ColorScheme;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;

/**
 *
 * @author mfiume
 */
public class PointTrackRenderer extends TrackRenderer {

    public PointTrackRenderer() {
    }

    @Override
    public void render(Graphics2D g2, GraphPane gp) throws RenderingException {

        renderPreCheck();

        Resolution r = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        // don't draw things which are too small to be seen: less than 1/2 pixel wide
        double width = gp.getUnitWidth();
        if (width < 0.5 || data == null) {
            throw new RenderingException("Zoom in to see points", 0);
        }


        if (r == Resolution.HIGH) {
            
            ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
            Color bgcolor = cs.getColor("Background");
            Color linecolor = cs.getColor("Line");

            for (Record record: data) {
                Polygon p = new Polygon();

                savant.data.types.Point sp = ((PointRecord)record).getPoint();

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
        if(data.isEmpty()){
            throw new RenderingException("No data in range", 1);
        }
    }
}
