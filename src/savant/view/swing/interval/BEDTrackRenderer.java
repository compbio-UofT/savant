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

/*
 * BEDTrackRenderer.java
 * Created on Feb 19, 2010
 */

package savant.view.swing.interval;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.model.BEDIntervalRecord;
import savant.model.Interval;
import savant.model.IntervalRecord;
import savant.model.Resolution;
import savant.model.view.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.model.view.Mode;
import savant.util.Block;
import savant.util.IntervalPacker;
import savant.util.Range;
import savant.util.Strand;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;
import savant.view.swing.util.GlassMessagePane;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Renderer for BED gene tracks.
 *
 * @author vwilliams
 */
public class BEDTrackRenderer extends TrackRenderer {

    private static Log log = LogFactory.getLog(BAMTrackRenderer.class);

    private static final Font TINY_FONT = new Font("Sans-Serif", Font.PLAIN, 8);
    private static final Font VERY_SMALL_FONT = new Font("Sans-Serif", Font.PLAIN, 10);
    private static final Font SMALL_FONT = new Font("Sans-Serif", Font.PLAIN, 12);
    private static final Font LARGE_FONT = new Font("Sans-Serif", Font.PLAIN, 18);

    Mode drawMode;
    DrawingInstructions drawingInstructions;
    Resolution resolution;

    public BEDTrackRenderer() {
        this(new DrawingInstructions());
    }

    public BEDTrackRenderer(DrawingInstructions drawingInstructions) {
        super(drawingInstructions);
    }
    
    @Override
    public void render(Graphics g, GraphPane gp) {
        Graphics2D g2 = (Graphics2D) g;
        gp.setIsOrdinal(true);

        drawingInstructions = this.getDrawingInstructions();

        Boolean refexists = (Boolean) drawingInstructions.getInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS);
        if (!refexists) {
            GlassMessagePane.draw(g2, gp, "no data for reference", 500);
            return;
        }


        drawMode = (Mode) drawingInstructions.getInstruction(DrawingInstructions.InstructionName.MODE);
        resolution = (Resolution) drawingInstructions.getInstruction(DrawingInstructions.InstructionName.RESOLUTION.toString());

        String modeName = drawMode.getName();
        if (modeName.equals("STANDARD")) {
            renderPackMode(g2, gp, resolution);
        }
        else if (modeName.equals("SQUISH")) {
            renderSquishMode(g2, gp, resolution);
        }

    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution resolution) {

        java.util.List<Object> data = this.getData();

        AxisRange axisRange = (AxisRange) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);
        ColorScheme cs = (ColorScheme) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
        Color linecolor = cs.getColor("Line");

        IntervalPacker packer = new IntervalPacker(data);
        // TODO: when it becomes possible, choose an appropriate number for breathing room parameter
        Map<Integer, ArrayList<IntervalRecord>> intervals = packer.pack(10);

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
            GlassMessagePane.draw(g2, gp, "Too many intervals to display.\nIncrease vertical pane size", 300);
            return;
        }

        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

            ArrayList<IntervalRecord> intervalsThisLevel = intervals.get(level);

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

        // for each block, draw a rectangle
        List<Block> blocks = bedRecord.getBlocks();
        double chevronIntervalStart = gp.transformXPos(interval.getStart());
        for (Block block : blocks) {

            double x = gp.transformXPos(interval.getStart() + block.position);
            double y = gp.transformYPos(level)-unitHeight;

            double chevronIntervalEnd = x;
            // draw chevrons in interval
            drawChevrons(g2, chevronIntervalStart, chevronIntervalEnd,  yPos, unitHeight, lineColor, bedRecord.getStrand());

            double w = gp.getWidth(block.size);
            double h = unitHeight;

            Rectangle2D.Double blockRect = new Rectangle2D.Double(x,y,w,h);
            g2.setColor(fillColor);
            g2.fill(blockRect);
            if (h > 4 && w > 4) {
                g2.setColor(lineColor);
                g2.draw(blockRect);
            }

            chevronIntervalStart = x + w;

        }

    }

    private void drawChevrons(Graphics2D g2, double start, double end, double y, double height, Color color, Strand strand) {

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
        for (int i=startPos; i<end; i+=interval) {
            Polygon arrow = new Polygon();
            if (strand == Strand.FORWARD) {
                if (height > SCALE_FACTOR) {
                    g2.drawLine((int)i, (int)y, (int)(i-height/4), (int)(y+height/4));
                    g2.drawLine((int)i,(int)y, (int)(i-height/4), (int)(y-height/4));
                }
                else {
                    arrow.addPoint((int)i, (int)y);
                    arrow.addPoint((int)(i-height/4), (int)(y+height/4));
                    arrow.addPoint((int)(i-height/4), (int)(y-height/4));
                    g2.fill(arrow);
                }
            }
            else {
                if (height > SCALE_FACTOR) {
                    g2.drawLine((int)i, (int)y, (int)(i+height/4), (int)(y+height/4));
                    g2.drawLine((int)i,(int)y, (int)(i+height/4), (int)(y-height/4));
                }
                else {
                    arrow.addPoint((int)i, (int)y);
                    arrow.addPoint((int)(i+height/4), (int)(y+height/4));
                    arrow.addPoint((int)(i+height/4), (int)(y-height/4));
                    g2.fill(arrow);
                }
            }

        }
    }

    private void renderSquishMode(Graphics2D g2, GraphPane gp, Resolution resolution) {

        if (resolution == Resolution.VERY_HIGH || resolution == Resolution.HIGH) {

            // ranges, width, and height
            AxisRange axisRange = (AxisRange) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);
            gp.setIsOrdinal(false);
            gp.setXRange(axisRange.getXRange());
            // y range is set where levels are sorted out, after block merging pass

            // colours
            ColorScheme cs = (ColorScheme) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
            Color fillColor = cs.getColor("Forward Strand");
            Color lineColor = cs.getColor("Line");

            // antialising, for the chevrons
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // data
            java.util.List<Object> data = this.getData();
            int numdata = this.getData().size();

            // first pass through the data, merging Blocks
            List<Interval> posStrandBlocks = new ArrayList<Interval>();
            List<Interval> negStrandBlocks = new ArrayList<Interval>();
            List<Interval> noStrandBlocks = new ArrayList<Interval>();
            for (int i = 0; i < numdata; i++) {
                BEDIntervalRecord bedRecord = (BEDIntervalRecord) data.get(i);
                Interval interval = bedRecord.getInterval();
                Strand strand =  bedRecord.getStrand();

                if (strand == Strand.FORWARD) {
                    mergeBlocks(posStrandBlocks, bedRecord);
                }
                else if (strand == Strand.REVERSE) {
                    mergeBlocks(negStrandBlocks, bedRecord);
                }
                else if (strand == null) {
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
                GlassMessagePane.draw(g2, gp, "Increase vertical pane size", 300);
                return;
            }            

            for (int i = 0; i < numdata; i++) {

                BEDIntervalRecord bedRecord = (BEDIntervalRecord) data.get(i);

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
                g2.drawLine(startXPos, yPos, startXPos + (int)gp.getWidth(interval.getLength()), yPos);
                drawChevrons(g2, startXPos, startXPos + (int)gp.getWidth(interval.getLength()),  yPos, unitHeight, lineColor, strand);

            }

            // Now draw all blocks
            drawBlocks(posStrandBlocks, posStrandLevel, gp, fillColor, lineColor, g2);
            drawBlocks(negStrandBlocks, negStrandLevel, gp, fillColor, lineColor, g2);
            drawBlocks(noStrandBlocks, noStrandLevel, gp, fillColor, lineColor, g2);
        }
        else {
            GlassMessagePane.draw(g2, gp, "Zoom in to see genes/intervals", 300);
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
                int blockStart = gene.getStart() + block.position;
                Interval blockInterval = Interval.valueOf(blockStart, blockStart+block.size+1);
                intervals.add(blockInterval);
            }
        }
        else {
            ListIterator<Interval> intervalIt = intervals.listIterator();
            while (intervalIt.hasNext()) {
                Interval interval = intervalIt.next();
                for (Block block: blocks) {
                    // merging only works on intervals, so convert block to interval
                    int blockStart = gene.getStart() + block.position;
                    Interval blockInterval = Interval.valueOf(blockStart, blockStart+block.size+1);
                    if (blockInterval.intersects(interval)) {
                        intervalIt.set(blockInterval.merge(interval));
                    }
                }
            }
        }
    }

    private void drawBlocks(List<Interval> blocks, int level, GraphPane gp, Color fillColor, Color lineColor, Graphics2D g2) {

        if (blocks.isEmpty()) return;

        double x, y, w, h;
        for (Interval block: blocks) {
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
}
