/*
 * BEDTrackRenderer.java
 * Created on Feb 19, 2010
 *
 *
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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.data.types.BEDIntervalRecord;
import savant.data.types.Block;
import savant.data.types.Interval;
import savant.data.types.IntervalRecord;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.file.DataFormat;
import savant.util.AxisRange;
import savant.util.ColorScheme;
import savant.util.DrawingInstruction;
import savant.util.IntervalPacker;
import savant.util.Range;
import savant.util.Resolution;
import savant.util.Strand;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;


/**
 * Renderer for BED gene tracks.
 *
 * @author vwilliams
 */
public class BEDTrackRenderer extends TrackRenderer {

    private static final Log LOG = LogFactory.getLog(BAMTrackRenderer.class);

    private static final Font TINY_FONT = new Font("Sans-Serif", Font.PLAIN, 8);
    private static final Font VERY_SMALL_FONT = new Font("Sans-Serif", Font.PLAIN, 10);
    private static final Font SMALL_FONT = new Font("Sans-Serif", Font.PLAIN, 12);
    private static final Font LARGE_FONT = new Font("Sans-Serif", Font.PLAIN, 18);

    public static final String STANDARD_MODE = "Standard";
    public static final String SQUISH_MODE = "Squish";

    String drawMode;
    Resolution resolution;

    public BEDTrackRenderer() {
        super(DataFormat.INTERVAL_BED);
    }
    
    @Override
    public void render(Graphics g, GraphPane gp) throws RenderingException {

        Graphics2D g2 = (Graphics2D)g;
        gp.setIsOrdinal(true);
        this.clearShapes();

        Boolean refexists = (Boolean)instructions.get(DrawingInstruction.REFERENCE_EXISTS);
        if (!refexists) {
            throw new RenderingException("No data for reference");
        }

        drawMode = (String)instructions.get(DrawingInstruction.MODE);
        resolution = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        String modeName = drawMode;
        if (modeName.equals(this.STANDARD_MODE)) {

            renderPackMode(g2, gp, resolution);
        } else if (modeName.equals(this.SQUISH_MODE)) {

            renderSquishMode(g2, gp, resolution);
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution resolution) throws RenderingException {

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor("Line");

        IntervalPacker packer = new IntervalPacker(data);
        // TODO: when it becomes possible, choose an appropriate number for breathing room parameter
//        Map<Integer, ArrayList<IntervalRecord>> intervals = packer.pack(10);
        ArrayList<List<IntervalRecord>> intervals = packer.pack(10);

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

        // display only a message if intervals will not be visible at this resolution
        if (gp.getUnitHeight() < 1) {
            throw new RenderingException("Too many intervals to display\nIncrease vertical pane size");
        }

        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

//            ArrayList<IntervalRecord> intervalsThisLevel = intervals.get(level);
            List<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                Interval interval = intervalRecord.getInterval();
                BEDIntervalRecord bedRecord = (BEDIntervalRecord) intervalRecord;

                renderGene(g2, gp, cs, bedRecord, interval, level);

            }

        }
    }

    private void renderGene(Graphics2D g2, GraphPane gp, ColorScheme cs, BEDIntervalRecord bedRecord, Interval interval, int level) {

        // for the chevrons
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double unitWidth = gp.getUnitWidth();
        double unitHeight = gp.getUnitHeight();

        // chose the color for the strand
        Color fillColor;
        if (bedRecord.getStrand() == Strand.FORWARD) {
            fillColor = cs.getColor("Forward Strand");
        }
        else {
            fillColor = cs.getColor("Reverse Strand");
        }
        Color lineColor = cs.getColor("Line");

        int startXPos = (int)gp.transformXPos(interval.getStart());

        boolean isInsertion = false;

        //If length is 0, draw insertion rhombus.  It is drawn here so that the
        //name can be drawn on top of it
        if(interval.getLength() == 0){

            g2.setColor(Color.white);
            int xCoordinate = (int)gp.transformXPos(interval.getStart());
            int yCoordinate = (int)(gp.transformYPos(0)-((level + 1)*unitHeight)) + 1;
            if((int)unitWidth/3 < 4 || (int)(unitHeight/2) < 6){
                yCoordinate = yCoordinate - 1;
                int lineWidth = Math.max((int)(unitWidth * (2.0/3.0)), 1);
                int xCoordinate1 = (int)(xCoordinate - Math.floor(lineWidth/2));
                int xCoordinate2 = (int)(xCoordinate - Math.floor(lineWidth/2)) + lineWidth - 1;
                if(xCoordinate1 == xCoordinate2) xCoordinate2++;
                int[] xPoints = {xCoordinate1, xCoordinate2, xCoordinate2, xCoordinate1};
                int[] yPoints = {yCoordinate, yCoordinate, yCoordinate+(int)unitHeight, yCoordinate+(int)unitHeight};
                g2.fillPolygon(xPoints, yPoints, 4);
            } else {
                int[] xPoints = {xCoordinate, xCoordinate+(int)(unitWidth/3), xCoordinate, xCoordinate-(int)(unitWidth/3)};
                int[] yPoints = {yCoordinate, yCoordinate+(int)(unitHeight/2), yCoordinate+(int)unitHeight-1, yCoordinate+(int)(unitHeight/2)};
                g2.fillPolygon(xPoints, yPoints, 4);
                if((int)unitWidth/3 >= 7 && (int)(unitHeight/2) >= 5){
                    g2.setColor(Color.BLACK);
                    g2.drawLine(xCoordinate, (int)yCoordinate, xCoordinate, (int)(yCoordinate+unitHeight)-1);
                }
            }

            isInsertion = true;
        }

        // draw the gene name, if possible
        String geneName = bedRecord.getName();
        boolean drawName = (geneName != null);
        if (drawName) {
            if (fontFits(geneName, LARGE_FONT, unitHeight, g2)) {
                g2.setFont(LARGE_FONT);
            }
            else if (fontFits(geneName, SMALL_FONT, unitHeight, g2)) {
                g2.setFont(SMALL_FONT);
            }
            else if (fontFits(geneName, VERY_SMALL_FONT, unitHeight, g2)) {
                g2.setFont(VERY_SMALL_FONT);
            }
            else if (fontFits(geneName, TINY_FONT, unitHeight, g2)) {
                g2.setFont(TINY_FONT);
            }
            else {
                drawName = false;
            }
        }

        if (drawName) {
            Rectangle2D nameRect = g2.getFont().getStringBounds(geneName, g2.getFontRenderContext());
            double topMargin = (unitHeight - nameRect.getHeight());
            g2.setColor(lineColor);
            g2.drawString(geneName, (int)(startXPos - nameRect.getWidth() - 5), (int)(gp.transformYPos(level) - topMargin));
        }

        if(isInsertion) return;

        // draw a line in the middle, the full length of the interval
        int yPos = (int) (gp.transformYPos(level)-unitHeight/2);
        g2.setColor(lineColor);
        g2.drawLine(startXPos, yPos, startXPos + (int)gp.getWidth(interval.getLength()), yPos);

        Area area = new Area(new Line2D.Double(startXPos, yPos, startXPos + (int)gp.getWidth(interval.getLength()), yPos));

        // for each block, draw a rectangle
        List<Block> blocks = bedRecord.getBlocks();
        double chevronIntervalStart = gp.transformXPos(interval.getStart());

        for (Block block : blocks) {

            chevronIntervalStart = Math.max(chevronIntervalStart, 0);

            double x = gp.transformXPos(interval.getStart() + block.getPosition());
            double y = gp.transformYPos(level)-unitHeight;

            double chevronIntervalEnd = x;

            chevronIntervalEnd = Math.min(chevronIntervalEnd, gp.getWidth());

            if (chevronIntervalEnd >= 0 && chevronIntervalStart <= gp.getWidth()) {
                drawChevrons(g2, chevronIntervalStart, chevronIntervalEnd,  yPos, unitHeight, lineColor, bedRecord.getStrand(), area);
            }

            double w = gp.getWidth(block.getSize());
            double h = unitHeight;

            Rectangle2D.Double blockRect = new Rectangle2D.Double(x,y,w,h);
            g2.setColor(fillColor);
            g2.fill(blockRect);
            if (h > 4 && w > 4) {
                g2.setColor(lineColor);
                g2.draw(blockRect);
            }
            area.add(new Area(blockRect));

            chevronIntervalStart = x + w;
        }

        this.recordToShapeMap.put(bedRecord, area);
    }

    private void drawChevrons(Graphics2D g2, double start, double end, double y, double height, Color color, Strand strand, Area area) {


        final int SCALE_FACTOR = 40;
        final int interval = ((int)height/SCALE_FACTOR+1) * SCALE_FACTOR;
        g2.setColor(color);
        // start the drawing on the next multiple of interval
        int startPos;
        if (start != interval) {
            startPos = (int)start + interval - ((int)start % interval);
        }
        else {
            startPos = interval;
        }

        //System.out.println("Start " + start);
        //System.out.println("End " + end);

        for (int i=startPos; i<end; i+=interval) {

            Polygon arrow = new Polygon();
            int arrowWidth = (int)(height/4);
            if ((end - start) > arrowWidth) {
                if (strand == Strand.FORWARD) {
                    if ((i-arrowWidth) > start) {
                        if (height > SCALE_FACTOR) {
                            g2.drawLine(i, (int)y, i-arrowWidth, (int)(y+arrowWidth));
                            g2.drawLine(i,(int)y, i-arrowWidth, (int)(y-arrowWidth));
                        }
                        else {
                            arrow.addPoint(i, (int)y);
                            arrow.addPoint(i-arrowWidth, (int)(y+arrowWidth));
                            arrow.addPoint(i-arrowWidth, (int)(y-arrowWidth));
                            g2.fill(arrow);
                        }
                    }
                }
                else {
                    if ((i+arrowWidth) < end) {
                        if (height > SCALE_FACTOR) {
                            g2.drawLine(i, (int)y, i+arrowWidth, (int)(y+arrowWidth));
                            g2.drawLine(i,(int)y, i+arrowWidth, (int)(y-arrowWidth));
                        }
                        else {
                            arrow.addPoint(i, (int)y);
                            arrow.addPoint(i+arrowWidth, (int)(y+arrowWidth));
                            arrow.addPoint(i+arrowWidth, (int)(y-arrowWidth));
                            g2.fill(arrow);
                        }
                    }
                }
                if(area != null)area.add(new Area(arrow));
            }

        }
    }

    private void renderSquishMode(Graphics2D g2, GraphPane gp, Resolution resolution) throws RenderingException {

        // ranges, width, and height
        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        gp.setIsOrdinal(false);
        gp.setXRange(axisRange.getXRange());
        // y range is set where levels are sorted out, after block merging pass

        if (resolution == Resolution.VERY_HIGH || resolution == Resolution.HIGH) {


            // colours
            ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
            Color fillColor = cs.getColor("Forward Strand");
            Color lineColor = cs.getColor("Line");

            // antialising, for the chevrons
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // first pass through the data, merging Blocks
            List<Interval> posStrandBlocks = new ArrayList<Interval>();
            List<Interval> negStrandBlocks = new ArrayList<Interval>();
            List<Interval> noStrandBlocks = new ArrayList<Interval>();
            for (Record record: data) {
                BEDIntervalRecord bedRecord = (BEDIntervalRecord)record;
                Strand strand =  bedRecord.getStrand();

                if (strand == Strand.FORWARD) {
                    mergeBlocks(posStrandBlocks, bedRecord);
                } else if (strand == Strand.REVERSE) {
                    mergeBlocks(negStrandBlocks, bedRecord);
                } else if (strand == null) {
                    mergeBlocks(noStrandBlocks, bedRecord);
                }

            }

            // allocate levels to strands, using 1, 2, or 3 depending on which strands are present in the data
            int posStrandLevel = -1;
            int negStrandLevel = -1;
            int noStrandLevel = -1;
            int signedStrandCount = posStrandBlocks.size() + negStrandBlocks.size();
            int noStrandCount = noStrandBlocks.size();
            if (signedStrandCount > 0 && noStrandCount > 0) {
                // assign three levels
                posStrandLevel = 0;
                negStrandLevel = 1;
                noStrandLevel = 2;
                gp.setYRange(new Range(0,3));
            }
            else if (signedStrandCount > 0 && noStrandCount == 0) {
                posStrandLevel = 0;
                negStrandLevel = 1;
                gp.setYRange(new Range(0,2));
            }
            else if (signedStrandCount == 0 && noStrandCount > 0) {
                noStrandLevel = 0;
                gp.setIsOrdinal(true);
            }
            double unitHeight = gp.getUnitHeight();
            // display only a message if intervals will not be visible at this resolution
            if (unitHeight < 1) {
                throw new RenderingException("Increase vertical pane size");
            }            

            for (Record record: data) {

                BEDIntervalRecord bedRecord = (BEDIntervalRecord)record;

                // we'll display different strands at different y positions
                Strand strand =  bedRecord.getStrand();
                int level;
                if (strand == Strand.FORWARD) level = posStrandLevel;
                else if (strand == Strand.REVERSE) level = negStrandLevel;
                else level = noStrandLevel;

                Interval interval = bedRecord.getInterval();

                int startXPos = (int)gp.transformXPos(interval.getStart());

                //If length is 0, draw insertion rhombus.
                if(interval.getLength() == 0){

                    drawInsertion(interval, level, gp, g2);
                    // draw nothing else for this interval
                    continue;
                }

                // draw a line in the middle, the full length of the interval
                int yPos = (int) (gp.transformYPos(level)-unitHeight/2);
                g2.setColor(lineColor);
                int lineWidth = (int)gp.getWidth(interval.getLength());
                if (lineWidth > 4) {
                    g2.drawLine(startXPos, yPos, startXPos + lineWidth, yPos);
                    drawChevrons(g2, startXPos, startXPos + lineWidth,  yPos, unitHeight, lineColor, strand, null);
                }

            }

            // Now draw all blocks
            drawBlocks(posStrandBlocks, posStrandLevel, gp, fillColor, lineColor, g2);
            drawBlocks(negStrandBlocks, negStrandLevel, gp, fillColor, lineColor, g2);
            drawBlocks(noStrandBlocks, noStrandLevel, gp, fillColor, lineColor, g2);
        } else {
            throw new RenderingException("Zoom in to see genes/intervals");
        }
    }

    private boolean fontFits(String string, Font font, double height, Graphics2D g2) {

        Rectangle2D charRect = font.getStringBounds(string, g2.getFontRenderContext());
        if (charRect.getHeight() > height) return false;
        else return true;
    }

    @Override
    public boolean isOrdinal() {
        return true;
    }

    @Override
    public Range getDefaultYRange() {
        return new Range(0,1);
    }

    private void mergeBlocks(List<Interval> intervals, BEDIntervalRecord bedRecord) {

        List<Block> blocks = bedRecord.getBlocks();
        Interval gene = bedRecord.getInterval();
        if (intervals.isEmpty()) {
            for (Block block: blocks) {
                long blockStart = gene.getStart() + block.getPosition();
                Interval blockInterval = Interval.valueOf(blockStart, blockStart+block.getSize());
                intervals.add(blockInterval);
            }
        }
        else {

            for (Block block: blocks) {
                // merging only works on intervals, so convert block to interval
                long blockStart = gene.getStart() + block.getPosition();
                Interval blockInterval = Interval.valueOf(blockStart, blockStart+block.getSize());
                ListIterator<Interval> intervalIt = intervals.listIterator();
                boolean merged = false;
                while (intervalIt.hasNext() && !merged) {
                    Interval interval = intervalIt.next();
                    if (blockInterval.intersects(interval)) {
                        intervalIt.set(blockInterval.merge(interval));
                        merged = true;
                    }
                }
                if (!merged) {
                    intervals.add(blockInterval);
                }
            }
        }
    }

    private void drawBlocks(List<Interval> blocks, int level, GraphPane gp, Color fillColor, Color lineColor, Graphics2D g2) {

        if (blocks.isEmpty()) return;

        double x, y, w, h;
        for (Interval block: blocks) {
            if (block.getLength() == 0) continue;
            x = gp.transformXPos(block.getStart());
            y = gp.transformYPos(level)-gp.getUnitHeight();
            w = gp.getWidth(block.getLength());
            h = gp.getUnitHeight();

                Rectangle2D.Double blockRect = new Rectangle2D.Double(x,y,w,h);

                g2.setColor(fillColor);
                g2.fill(blockRect);
                if (h > 4 && w > 4) {
                    g2.setColor(lineColor);
                    g2.draw(blockRect);
                }
        }

    }

    private void drawInsertion(Interval interval, int level, GraphPane gp, Graphics2D g2) {

        double unitHeight = gp.getUnitHeight();
        double unitWidth = gp.getUnitWidth();

        g2.setColor(Color.white);
        int xCoordinate = (int)gp.transformXPos(interval.getStart());
        int yCoordinate = (int)(gp.transformYPos(0)-((level + 1)*unitHeight)) + 1;
        if((int)unitWidth/3 < 4 || (int)(unitHeight/2) < 6){
            yCoordinate = yCoordinate - 1;
            int lineWidth = Math.max((int)(unitWidth * (2.0/3.0)), 1);
            int xCoordinate1 = (int)(xCoordinate - Math.floor(lineWidth/2));
            int xCoordinate2 = (int)(xCoordinate - Math.floor(lineWidth/2)) + lineWidth - 1;
            if(xCoordinate1 == xCoordinate2) xCoordinate2++;
            int[] xPoints = {xCoordinate1, xCoordinate2, xCoordinate2, xCoordinate1};
            int[] yPoints = {yCoordinate, yCoordinate, yCoordinate+(int)unitHeight, yCoordinate+(int)unitHeight};
            g2.fillPolygon(xPoints, yPoints, 4);
        } else {
            int[] xPoints = {xCoordinate, xCoordinate+(int)(unitWidth/3), xCoordinate, xCoordinate-(int)(unitWidth/3)};
            int[] yPoints = {yCoordinate, yCoordinate+(int)(unitHeight/2), yCoordinate+(int)unitHeight-1, yCoordinate+(int)(unitHeight/2)};
            g2.fillPolygon(xPoints, yPoints, 4);
            if((int)unitWidth/3 >= 7 && (int)(unitHeight/2) >= 5){
                g2.setColor(Color.BLACK);
                g2.drawLine(xCoordinate, (int)yCoordinate, xCoordinate, (int)(yCoordinate+unitHeight)-1);
            }
        }

    }

    @Override
    public List<String> getRenderingModes() {
        List<String> modes = new ArrayList<String>();
        modes.add(STANDARD_MODE);
        modes.add(SQUISH_MODE);
        return modes;
    }

    @Override
    public String getDefaultRenderingMode() {
        return STANDARD_MODE;
    }
}
