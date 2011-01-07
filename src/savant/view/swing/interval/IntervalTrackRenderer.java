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

package savant.view.swing.interval;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import savant.data.event.DataRetrievalEvent;

import savant.data.types.Interval;
import savant.data.types.IntervalRecord;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.file.DataFormat;
import savant.util.*;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;


/**
 *
 * @author mfiume
 */
public class IntervalTrackRenderer extends TrackRenderer {

    public static final String SQUISH_MODE = "Squish";
    public static final String PACK_MODE = "Pack";
    public static final String ARC_MODE = "Arc";

    public IntervalTrackRenderer() {
        super(DataFormat.INTERVAL_GENERIC);
    }

    @Override
    public void dataRetrievalCompleted(DataRetrievalEvent evt) {
        String mode = (String)instructions.get(DrawingInstruction.MODE);
        if (mode.equals(ARC_MODE)) {
            int maxDataValue = IntervalTrack.getMaxValue(data);
            Range range = (Range)instructions.get(DrawingInstruction.RANGE);
            addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(0,(int)Math.round(Math.log(maxDataValue)))));
        }
        super.dataRetrievalCompleted(evt);
    }

    @Override
    public void render(Graphics g, GraphPane gp) throws RenderingException {

        Graphics2D g2 = (Graphics2D) g;
        gp.setIsOrdinal(true);
        this.clearShapes();

        Boolean refexists = (Boolean)instructions.get(DrawingInstruction.REFERENCE_EXISTS);
        if (!refexists) {
            throw new RenderingException("No data for reference");
        }

        String drawMode = (String)instructions.get(DrawingInstruction.MODE);
        Resolution r = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        if (drawMode.equals(SQUISH_MODE)) {
            renderSquishMode(g2, gp, r);
        }
        else if (drawMode.equals(this.ARC_MODE)) {
            renderArcMode(g2, gp, r);
        }
        else if (drawMode.equals(this.PACK_MODE)) {
            renderPackMode(g2, gp, r);
        }
    }

    private void renderSquishMode(Graphics2D g2, GraphPane gp, Resolution r) throws RenderingException {

        ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color bgcolor = cs.getColor("Translucent Graph");
        Color linecolor = cs.getColor("Line");

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        
        if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {

            gp.setIsOrdinal(true);
            gp.setXRange(axisRange.getXRange());
            gp.setYRange(axisRange.getYRange());
 
            double unitWidth;
            double unitHeight;
            unitWidth = gp.getUnitWidth();
            unitHeight = gp.getUnitHeight();

            for (Record record: data) {
                Interval inter = ((IntervalRecord)record).getInterval();


                double x = gp.transformXPos(inter.getStart());
                double y = 0;
                double w = unitWidth*inter.getLength();
                double h = unitHeight;

                Rectangle2D.Double rect = new Rectangle2D.Double(x, y, w, h);

                g2.setColor(bgcolor);
                g2.fill(rect);

                if (w > 5) {
                    g2.setColor(linecolor);
                    g2.draw(rect);
                }
            }
        } else {
            throw new RenderingException("Zoom in to see intervals");
        }
    }

    private void renderArcMode(Graphics2D g2, GraphPane gp, Resolution r) {


        ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color bgcolor = cs.getColor("Opaque Graph");

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);

        if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            gp.setIsOrdinal(false);
            gp.setXRange(axisRange.getXRange());
            gp.setYRange(axisRange.getYRange());

            for (Record record: data) {
                Interval inter = ((IntervalRecord)record).getInterval();

                long arcLength = inter.getLength();
                int arcHeight = (int)(Math.log((double)arcLength));

                int rectWidth = (int)(gp.getWidth(arcLength));
                int rectHeight = (int)(gp.getHeight(arcHeight)*2);

                int xOrigin = (int)(gp.transformXPos(inter.getStart()));
                int yOrigin = (int)(gp.transformYPos(arcHeight));

                g2.setColor(bgcolor);
                g2.drawArc(xOrigin, yOrigin, rectWidth, rectHeight, -180, -180);
            }
        }

    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution r) throws RenderingException {

        ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color bgcolor = cs.getColor("Opaque Graph");
        Color linecolor = cs.getColor("Line");

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);

        if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {

            IntervalPacker packer = new IntervalPacker(data);
//            Map<Integer, ArrayList<IntervalRecord>> intervals = packer.pack(2);
            ArrayList<List<IntervalRecord>> intervals = packer.pack(2);

            gp.setIsOrdinal(false);
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

            double unitHeight;
            unitHeight = gp.getUnitHeight();

            // display only a message if intervals will not be visible at this resolution
            if (unitHeight < 1) {
                throw new RenderingException("Increase height of window");
            }

            // scan the map of intervals and draw the intervals for each level
            for (int k=0; k<intervals.size(); k++) {

//                ArrayList<IntervalRecord> intervalsThisLevel = intervals.get(k);
                List<IntervalRecord> intervalsThisLevel = intervals.get(k);

                for (IntervalRecord intervalRecord : intervalsThisLevel) {
                    Interval interval = intervalRecord.getInterval();
                    double x = gp.transformXPos(interval.getStart());
                    double y = gp.transformYPos(k)-unitHeight;
                    double w = gp.getWidth(interval.getLength());
                    if (w < 1) continue; // don't draw intervals less than one pixel wide
                    double h = unitHeight;

                    Rectangle2D.Double intervalRect = new Rectangle2D.Double(x, y, w, h);

                    g2.setColor(bgcolor);
                    g2.fill(intervalRect);
                    this.recordToShapeMap.put(intervalRecord, intervalRect);

                    if (w > 5) {
                        g2.setColor(linecolor);
                        g2.draw(intervalRect);
                    }
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

    @Override
    public boolean hasHorizontalGrid() {
        String drawMode = (String)instructions.get(DrawingInstruction.MODE);
        return drawMode.equals(this.ARC_MODE);
    }

    @Override
    public List<String> getRenderingModes() {
        List<String> modes = new ArrayList<String>();
        modes.add(PACK_MODE);
        modes.add(SQUISH_MODE);
        modes.add(ARC_MODE);
        return modes;
    }

    @Override
    public String getDefaultRenderingMode() {
        return PACK_MODE;
    }
}
