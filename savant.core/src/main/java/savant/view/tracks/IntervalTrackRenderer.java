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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.event.DataRetrievalEvent;
import savant.api.data.Interval;
import savant.api.data.IntervalRecord;
import savant.api.data.Record;
import savant.api.util.Resolution;
import savant.exception.RenderingException;
import savant.util.*;


/**
 *
 * @author mfiume
 */
public class IntervalTrackRenderer extends TrackRenderer {

    public IntervalTrackRenderer() {
    }

    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        switch (evt.getType()) {
            case COMPLETED:
                DrawingMode mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
                if (mode == DrawingMode.ARC) {
                    int maxDataValue = IntervalTrack.getMaxValue(evt.getData());
                    Range range = (Range)instructions.get(DrawingInstruction.RANGE);
                    addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(range, new Range(0,(int)Math.round(Math.log(maxDataValue)))));
                }
                break;
        }
        super.handleEvent(evt);
    }

    @Override
    public void render(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        renderPreCheck();

        DrawingMode mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        Resolution r = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        if (mode == DrawingMode.SQUISH) {
            renderSquishMode(g2, gp, r);
        } else if (mode == DrawingMode.ARC) {
            renderArcMode(g2, gp, r);
        } else if (mode == DrawingMode.PACK) {
            renderPackMode(g2, gp, r);
        }
        if (data.isEmpty()) {
            throw new RenderingException("No data in range", RenderingException.INFO_PRIORITY);
        }
    }

    private void renderSquishMode(Graphics2D g2, GraphPaneAdapter gp, Resolution r) throws RenderingException {

        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        Color bgcolor = cs.getColor(ColourKey.TRANSLUCENT_GRAPH);
        Color linecolor = cs.getColor(ColourKey.INTERVAL_LINE);

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        
        if (r == Resolution.HIGH) {

            gp.setXRange(axisRange.getXRange());
            gp.setYRange(axisRange.getYRange());
 
            double unitWidth = gp.getUnitWidth();
            double unitHeight = gp.getUnitHeight();
            double y = gp.transformYPos(0.0);

            for (Record record: data) {
                Interval inter = ((IntervalRecord)record).getInterval();


                double x = gp.transformXPos(inter.getStart());
                double w = unitWidth*inter.getLength();
                double h = unitHeight;

                Rectangle2D.Double rect = new Rectangle2D.Double(x, y - unitHeight, w, unitHeight);

                g2.setColor(bgcolor);
                g2.fill(rect);

                if (w > 5) {
                    g2.setColor(linecolor);
                    g2.draw(rect);
                }
            }
        } else {
            throw new RenderingException("Zoom in to see intervals", RenderingException.LOWEST_PRIORITY);
        }
    }

    private void renderArcMode(Graphics2D g2, GraphPaneAdapter gp, Resolution r) {


        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        Color bgcolor = cs.getColor(ColourKey.OPAQUE_GRAPH);

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);

        if (r == Resolution.HIGH) {

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            gp.setXRange(axisRange.getXRange());
            gp.setYRange(axisRange.getYRange());

            for (Record record: data) {
                Interval inter = ((IntervalRecord)record).getInterval();

                int arcLength = inter.getLength();
                double arcHeight = Math.log((double)arcLength);

                double rectWidth = arcLength * gp.getUnitWidth();
                double rectHeight = arcHeight * 2 * gp.getUnitHeight();

                double xOrigin = gp.transformXPos(inter.getStart());
                double yOrigin = gp.transformYPos(arcHeight);

                g2.setColor(bgcolor);
                g2.draw(new Arc2D.Double(xOrigin, yOrigin, rectWidth, rectHeight, -180, -180, Arc2D.OPEN));
            }
        }

    }

    private void renderPackMode(Graphics2D g2, GraphPaneAdapter gp, Resolution r) throws RenderingException {
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);

        if (r == Resolution.HIGH) {

            IntervalPacker packer = new IntervalPacker(data);
            List<List<IntervalRecord>> intervals = packer.pack(2);

            gp.setXRange(axisRange.getXRange());
            int maxYRange;
            int numIntervals = intervals.size();
            // Set the Y range to the closest value of 10, 20, 50, 100, n*100
            if (numIntervals <= 10) maxYRange = 10;
            else if (numIntervals <= 20) maxYRange = 20;
            else if (numIntervals <=50) maxYRange = 50;
            else if (numIntervals <= 100) maxYRange = 100;
            else maxYRange = numIntervals;
            gp.setYRange(new Range(0,maxYRange));

            //resize frame if necessary
            if (gp.needsToResize()) return;

            double unitHeight = gp.getUnitHeight();
            double unitWidth = gp.getUnitWidth();
            int offset = gp.getOffset();
            Color textColor = cs.getColor(ColourKey.INTERVAL_TEXT);
            Color bgColor = cs.getColor(ColourKey.OPAQUE_GRAPH);
            Color lineColor = cs.getColor(ColourKey.INTERVAL_LINE);

            // scan the map of intervals and draw the intervals for each level
            for (int k=0; k<intervals.size(); k++) {

                List<IntervalRecord> intervalsThisLevel = intervals.get(k);

                for (IntervalRecord intervalRecord : intervalsThisLevel) {
                    Interval interval = intervalRecord.getInterval();
                    double x = gp.transformXPos(interval.getStart());
                    //double y = gp.transformYPos(k)-unitHeight;
                    double y = gp.getHeight() - unitHeight * (k + 1) - offset;
                    double w = interval.getLength() * unitWidth;
                    //if (w < 1) continue; // don't draw intervals less than one pixel wide
                    double h = unitHeight;

                    Rectangle2D.Double intervalRect = new Rectangle2D.Double(x, y, w, h);

                    g2.setColor(bgColor);
                    g2.fill(intervalRect);
                    recordToShapeMap.put(intervalRecord, intervalRect);

                    if (w > 5) {
                        g2.setColor(lineColor);
                        g2.draw(intervalRect);
                    }

                    String geneName = intervalRecord.getName();
                    if (geneName != null) {
                        g2.setColor(textColor);
                        FontMetrics fm = g2.getFontMetrics();
                        drawFeatureLabel(g2, geneName, x, y + unitHeight * 0.5 + (fm.getHeight() - fm.getDescent()) * 0.5);
                    }
                }
            }
        }

    }
}
