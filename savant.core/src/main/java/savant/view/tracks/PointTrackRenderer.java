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
package savant.view.tracks;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.PointRecord;
import savant.api.data.Record;
import savant.api.util.Resolution;
import savant.exception.RenderingException;
import savant.util.ColourKey;
import savant.util.DrawingInstruction;
import savant.util.ColourScheme;


/**
 *
 * @author mfiume
 */
public class PointTrackRenderer extends TrackRenderer {

    public PointTrackRenderer() {
    }

    @Override
    public void render(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        renderPreCheck();

        Resolution r = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        // don't draw things which are too small to be seen: less than 1/2 pixel wide
        double width = gp.getUnitWidth();
        if (width < 0.5 || data == null) {
            throw new RenderingException("Zoom in to see points", RenderingException.LOWEST_PRIORITY);
        }


        if (r == Resolution.HIGH) {
            
            ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
            Color bgcolor = cs.getColor(ColourKey.POINT_FILL);
            Color linecolor = cs.getColor(ColourKey.POINT_LINE);

            for (Record record: data) {
                Polygon p = new Polygon();

                int sp = ((PointRecord)record).getPosition();

                Point2D.Double p1 = new Point2D.Double(gp.transformXPos(sp),0);
                Point2D.Double p2 = new Point2D.Double(gp.transformXPos(sp + 1),0);
                Point2D.Double p3 = new Point2D.Double(gp.transformXPos(sp + 1),gp.getHeight());
                Point2D.Double p4 = new Point2D.Double(gp.transformXPos(sp),gp.getHeight());

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
        if (data.isEmpty()) {
            throw new RenderingException("No data in range", RenderingException.INFO_PRIORITY);
        }
    }
}
