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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import savant.data.types.PointRecord;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.file.DataFormat;

import savant.util.DrawingInstruction;
import savant.util.Resolution;
import savant.util.ColorScheme;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;

/**
 *
 * @author mfiume
 */
public class PointTrackRenderer extends TrackRenderer {

    public static final String STANDARD_MODE = "Standard";

    public PointTrackRenderer() {
        super(DataFormat.POINT_GENERIC);
    }

    @Override
    public void render(Graphics g, GraphPane gp) throws RenderingException {

        Graphics2D g2 = (Graphics2D) g;
        gp.setIsOrdinal(true);
        this.clearShapes();

        double width = gp.getUnitWidth();

        renderPreCheck();

        Resolution r = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        // don't draw things which are too small to be seen: less than 1 pixel wide
        if (width < 1 || data == null) {
            throw new RenderingException("Zoom in to see points");
        }


        if (r == Resolution.VERY_HIGH) {
            
            ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
            Color bgcolor = cs.getColor("Background");
            Color linecolor = cs.getColor("Line");

            float pointiness = 0.1F;

            for (Record record: data) {
                Polygon p = new Polygon();

                savant.data.types.Point sp = ((PointRecord)record).getPoint();

                Point2D.Double p1 = new Point2D.Double(gp.transformXPosExclusive(sp.getPosition()),0);
                Point2D.Double p2 = new Point2D.Double(gp.transformXPosExclusive(sp.getPosition()+1),0);
                Point2D.Double p3 = new Point2D.Double(gp.transformXPosExclusive(sp.getPosition()+1),gp.getHeight());
                Point2D.Double p4 = new Point2D.Double(gp.transformXPosExclusive(sp.getPosition()),gp.getHeight());

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
        if(data.isEmpty())throw new RenderingException("No data in range.");
    }

    @Override
    public boolean isOrdinal() {
        return true;
    }

    @Override
    public Range getDefaultYRange() {
        return new Range(0,1);
    }

    @Override
    public List<String> getRenderingModes() {
        List<String> modes = new ArrayList<String>();
        modes.add(STANDARD_MODE);
        return modes;
    }

    @Override
    public String getDefaultRenderingMode() {
        return STANDARD_MODE;
    }
}
