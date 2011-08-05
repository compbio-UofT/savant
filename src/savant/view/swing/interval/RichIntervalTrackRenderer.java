/*
 *    Copyright 2010-2011 University of Toronto
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

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.data.types.*;
import savant.exception.RenderingException;
import savant.settings.BrowserSettings;
import savant.util.*;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;


/**
 * Renderer for interval tracks which have extra information (and possibly blocks)
 *
 * @author vwilliams, tarkvara
 */
public class RichIntervalTrackRenderer extends TrackRenderer {

    private static final Log LOG = LogFactory.getLog(RichIntervalTrackRenderer.class);

    private DrawingMode mode;
    Resolution resolution;

    public RichIntervalTrackRenderer() {
    }
    
    @Override
    public void render(Graphics2D g2, GraphPane gp) throws RenderingException {

        renderPreCheck(gp);

        mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        resolution = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        if (mode == DrawingMode.STANDARD) {
            renderPackMode(g2, gp, resolution);
        } else if (mode == DrawingMode.SQUISH) {
            renderSquishMode(g2, gp, resolution);
        }

        if(data.isEmpty()){
            throw new RenderingException("No data in range", 1);
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution resolution) throws RenderingException {

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);

        double unitWidth = gp.getUnitWidth();

        FontMetrics fm = g2.getFontMetrics();
        List<Record> stuffedRecords = new ArrayList<Record>();
        for (Record r : data) {
            RichIntervalRecord ir = (RichIntervalRecord) r;
            int padamount = fm.stringWidth(ir.getName()) + 5;
            stuffedRecords.add(new StuffedIntervalRecord(ir,(int)(padamount/unitWidth),(int)(0*unitWidth)));
        }

        IntervalPacker packer = new IntervalPacker(stuffedRecords);
        // TODO: when it becomes possible, choose an appropriate number for breathing room parameter
//        Map<Integer, ArrayList<IntervalRecord>> intervals = packer.pack(10);
        List<List<IntervalRecord>> intervals = StuffedIntervalRecord.getOriginalIntervals(packer.pack(2));

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

        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

            List<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                Interval interval = intervalRecord.getInterval();
                RichIntervalRecord bedRecord = (RichIntervalRecord) intervalRecord;

                renderGene(g2, gp, cs, bedRecord, interval, level);

            }

        }
    }

    private void renderGene(Graphics2D g2, GraphPane gp, ColorScheme cs, RichIntervalRecord rec, Interval interval, int level) {

        // for the chevrons
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double unitWidth = gp.getUnitWidth();
        double unitHeight = gp.getUnitHeight();
        int offset = gp.getOffset();

        g2.setFont(BrowserSettings.getTrackFont());

        // chose the color for the strand
        Color fillColor;
        if ((Boolean)instructions.get(DrawingInstruction.ITEMRGB) && rec.getItemRGB() != null && !rec.getItemRGB().isNull()){
            //if an RGB value was supplied, use it
            fillColor = rec.getItemRGB().createColor();
        } else {
            //otherwise use the default color value
            fillColor = cs.getColor(rec.getStrand() == Strand.FORWARD ? "Forward Strand" : "Reverse Strand");
        }

        // Set alpha if score is enabled
        if ((Boolean)instructions.get(DrawingInstruction.SCORE) && !Float.isNaN(rec.getScore())) {
            fillColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), getConstrainedAlpha((int)(rec.getScore() * 0.255)));
        }

        Color lineColor = cs.getColor("Line");
        Color textColor = cs.getColor("Text");

        double startXPos = gp.transformXPos(interval.getStart());

        boolean isInsertion = false;

        // If length is 0, draw insertion rhombus.  It is drawn here so that the name can be drawn on top of it.
        if (interval.getLength() == 0){
            Shape rhombus = drawInsertion(interval, level, gp, g2);
            isInsertion = true;

            startXPos -= unitWidth * 0.5;   // So the name won't stomp on the leftward-pointing arrow of the insertion.
            recordToShapeMap.put(rec, rhombus);
        }

        // Draw the gene name, if possible.
        String geneName = rec.getName();
        if ((Boolean)instructions.get(DrawingInstruction.ALTERNATE_NAME)) {
            geneName = rec.getAlternateName();
        }

        int thickStart = rec.getThickStart();
        int thickEnd = rec.getThickEnd() + 1;
        double thickStartX = gp.transformXPos(thickStart);
        double thickEndX = gp.transformXPos(thickEnd);
        if (!isInsertion) {

            double yPos = gp.getHeight() - (unitHeight * level) - (unitHeight / 2) - offset;

            Area area = new Area();

            // for each block, draw a rectangle
            List<Block> blocks = rec.getBlocks();
            double chevronIntervalStart = gp.transformXPos(interval.getStart());

            if (blocks == null) {
                // When blocks are null, we fake it out by drawing a single block with the full interval of the feature.
                blocks = new ArrayList<Block>();
                blocks.add(Block.valueOf(0, interval.getLength()));
            }

            for (Block block : blocks) {

                chevronIntervalStart = Math.max(chevronIntervalStart, 0);

                int blockStart = interval.getStart() + block.getPosition();
                int blockEnd = blockStart + block.getSize();
                double x = gp.transformXPos(blockStart);
                double y = gp.getHeight() - (unitHeight * (level + 1)) - offset;

                double chevronIntervalEnd = x;

                chevronIntervalEnd = Math.min(chevronIntervalEnd, gp.getWidth());

                if (chevronIntervalStart < chevronIntervalEnd && chevronIntervalEnd >= 0 && chevronIntervalStart <= gp.getWidth()) {
                    g2.setColor(lineColor);
                    g2.draw(new Line2D.Double(chevronIntervalStart, yPos, chevronIntervalEnd, yPos));
                    drawChevrons(g2, chevronIntervalStart, chevronIntervalEnd,  yPos, unitHeight, lineColor, rec.getStrand(), area);
                }

                double w = gp.getWidth(block.getSize());
                double h = unitHeight;

                Shape blockShape;
                if (blockStart >= thickStart) {
                    if (blockEnd <= thickEnd) {
                        // Simplest case.  Entire block is thick (includes case where thickness is not relevant).
                        blockShape = new Rectangle2D.Double(x, y, w, h);
                    } else if (blockStart >= thickEnd) {
                        // Also simple.  Entire block is thin.
                        blockShape = new Rectangle2D.Double(x, y + h * 0.25, w, h * 0.5);
                    } else {
                        // Block starts thick but ends thin.
                        Path2D.Double blockPath = new Path2D.Double();
                        blockPath.moveTo(x, y);
                        blockPath.lineTo(thickEndX, y);
                        blockPath.lineTo(thickEndX, y + h * 0.25);
                        blockPath.lineTo(x + w, y + h * 0.25);
                        blockPath.lineTo(x + w, y + h * 0.75);
                        blockPath.lineTo(thickEndX, y + h * 0.75);
                        blockPath.lineTo(thickEndX, y + h);
                        blockPath.lineTo(x, y + h);
                        blockPath.closePath();
                        blockShape = blockPath;
                    }
                } else {
                    if (blockEnd <= thickStart) {
                        // Entire block is thin.
                        blockShape = new Rectangle2D.Double(x, y + h * 0.25, w, h * 0.5);
                    } else if (blockEnd <= thickEnd) {
                        // Block starts thin but ends thick.
                        Path2D.Double blockPath = new Path2D.Double();
                        blockPath.moveTo(x, y + h * 0.25);
                        blockPath.lineTo(thickStartX, y + h * 0.25);
                        blockPath.lineTo(thickStartX, y);
                        blockPath.lineTo(x + w, y);
                        blockPath.lineTo(x + w, y + h);
                        blockPath.lineTo(thickStartX, y + h);
                        blockPath.lineTo(thickStartX, y + h * 0.75);
                        blockPath.lineTo(x, y + h * 0.75);
                        blockPath.closePath();
                        blockShape = blockPath;
                    } else {
                        // Worst case.  Block starts thin, goes thick in the middle and ends thin.
                        Path2D.Double blockPath = new Path2D.Double();
                        blockPath.moveTo(x, y + h * 0.25);
                        blockPath.lineTo(thickStartX, y + h * 0.25);
                        blockPath.lineTo(thickStartX, y);
                        blockPath.lineTo(thickEndX, y);
                        blockPath.lineTo(thickEndX, y + h * 0.25);
                        blockPath.lineTo(x + w, y + h * 0.25);
                        blockPath.lineTo(x + w, y + h * 0.75);
                        blockPath.lineTo(thickEndX, y + h * 0.75);
                        blockPath.lineTo(thickEndX, y + h);
                        blockPath.lineTo(thickStartX, y + h);
                        blockPath.lineTo(thickStartX, y + h * 0.75);
                        blockPath.lineTo(x, y + h * 0.75);
                        blockPath.closePath();
                        blockShape = blockPath;
                    }
                }
                g2.setColor(fillColor);
                g2.fill(blockShape);
                if (h > 4 && w > 4) {
                    g2.setColor(lineColor);
                    g2.draw(blockShape);
                }
                area.add(new Area(blockShape));

                chevronIntervalStart = x + w;
            }
            recordToShapeMap.put(rec, area);
        }

        if (geneName != null) {
            FontMetrics fm = g2.getFontMetrics();
            double stringstartx = startXPos - fm.stringWidth(geneName) - 5;

            g2.setColor(textColor);

            double mid = gp.getHeight() - (level * unitHeight) - unitHeight * 0.5 + (fm.getHeight() - fm.getDescent()) * 0.5;
            if (stringstartx <= 0) {
                Rectangle2D r = fm.getStringBounds(geneName, g2);

                int b = 2;
                g2.setColor(new Color(255,255,255,200));
                g2.fill(new RoundRectangle2D.Double(3.0, mid - (fm.getHeight() - fm.getDescent()) - b - offset, r.getWidth() + 2 * b, r.getHeight() + 2 * b, 8.0, 8.0));
                g2.setColor(textColor);
                g2.drawString(geneName, 5.0F, (float)mid - offset);
            } else {
                g2.setColor(textColor);
                g2.drawString(geneName, (float)stringstartx, (float)mid - offset);
            }
        }
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

        for (double i=startPos; i<end; i+=interval) {

            int arrowWidth = (int)(height/4);
            if ((end - start) > arrowWidth) {
                Path2D.Double arrow = null;
                if (strand == Strand.FORWARD) {
                    if ((i-arrowWidth) > start) {
                        if (height > SCALE_FACTOR) {
                            g2.draw(new Line2D.Double(i, y, i - arrowWidth, y + arrowWidth + 1.0));
                            g2.draw(new Line2D.Double(i, y, i - arrowWidth, y - arrowWidth));
                        }
                        else {
                            arrow = new Path2D.Double();
                            arrow.moveTo(i, y);
                            arrow.lineTo(i - arrowWidth, y + arrowWidth + 1.0);
                            arrow.lineTo(i - arrowWidth, y - arrowWidth);
                        }
                    }
                } else {
                    if ((i+arrowWidth) < end) {
                        if (height > SCALE_FACTOR) {
                            g2.draw(new Line2D.Double(i, y, i + arrowWidth, y + arrowWidth + 1.0));
                            g2.draw(new Line2D.Double(i, y, i + arrowWidth, y - arrowWidth));
                        }
                        else {
                            arrow = new Path2D.Double();
                            arrow.moveTo(i, y);
                            arrow.lineTo(i + arrowWidth, y + arrowWidth + 1.0);
                            arrow.lineTo(i + arrowWidth, y - arrowWidth);
                        }
                    }
                }
                if (arrow != null) {
                    arrow.closePath();
                    g2.fill(new Area(arrow));
                    if (area != null) {
                        area.add(new Area(arrow));
                    }
                }
            }
        }
    }

    private void renderSquishMode(Graphics2D g2, GraphPane gp, Resolution resolution) throws RenderingException {

        // ranges, width, and height
        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);

        gp.setXRange(axisRange.getXRange());
        // y range is set where levels are sorted out, after block merging pass

        if (resolution == Resolution.HIGH) {


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
                RichIntervalRecord bedRecord = (RichIntervalRecord)record;
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
            } else if (signedStrandCount > 0 && noStrandCount == 0) {
                posStrandLevel = 0;
                negStrandLevel = 1;
                gp.setYRange(new Range(0,2));
            } else if (signedStrandCount == 0 && noStrandCount > 0) {
                noStrandLevel = 0;
            }
            double unitHeight = gp.getUnitHeight();
            // display only a message if intervals will not be visible at this resolution
            if (unitHeight < 1) {
                throw new RenderingException("Increase vertical pane size", 0);
            }            

            for (Record record: data) {

                RichIntervalRecord bedRecord = (RichIntervalRecord)record;

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
                double yPos = gp.transformYPos(level) - unitHeight * 0.5;
                g2.setColor(lineColor);
                int lineWidth = (int)gp.getWidth(interval.getLength());
                if (lineWidth > 4) {
                    g2.draw(new Line2D.Double(startXPos, yPos, startXPos + lineWidth, yPos));
                    drawChevrons(g2, startXPos, startXPos + lineWidth,  yPos, unitHeight, lineColor, strand, null);
                }

            }

            // Now draw all blocks
            drawBlocks(posStrandBlocks, posStrandLevel, gp, fillColor, lineColor, g2);
            drawBlocks(negStrandBlocks, negStrandLevel, gp, fillColor, lineColor, g2);
            drawBlocks(noStrandBlocks, noStrandLevel, gp, fillColor, lineColor, g2);
        } else {
            throw new RenderingException("Zoom in to see genes/intervals", 0);
        }
    }

    private void mergeBlocks(List<Interval> intervals, RichIntervalRecord bedRecord) {

        List<Block> blocks = bedRecord.getBlocks();
        if(blocks == null){
            // When blocks are null, we fake it out by drawing a single block with the full interval of the feature.
            // This occurs in snp tracks
            blocks = new ArrayList<Block>();
            blocks.add(Block.valueOf(0, bedRecord.getInterval().getLength()-1));
        }
        Interval gene = bedRecord.getInterval();
        if (intervals.isEmpty()) {
            for (Block block: blocks) {
                int blockStart = gene.getStart() + block.getPosition();
                Interval blockInterval = Interval.valueOf(blockStart, blockStart+block.getSize());
                intervals.add(blockInterval);
            }
        } else {
            for (Block block: blocks) {
                // merging only works on intervals, so convert block to interval
                int blockStart = gene.getStart() + block.getPosition();
                Interval blockInterval = Interval.valueOf(blockStart, blockStart+block.getSize());
                ListIterator<Interval> intervalIt = intervals.listIterator();
                boolean merged = false;
                while (intervalIt.hasNext() && !merged) {
                    Interval interval = intervalIt.next();
                    if (blockInterval.intersectsOrAbuts(interval)) {
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

        if (blocks == null || blocks.isEmpty()) return;

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

    private Shape drawInsertion(Interval interval, int level, GraphPane gp, Graphics2D g2) {

        double unitHeight = gp.getUnitHeight();
        double unitWidth = gp.getUnitWidth();

        g2.setColor(Color.white);
        double xCoordinate = gp.transformXPos(interval.getStart());
        double yCoordinate = gp.transformYPos(0) - ((level + 1) * unitHeight) + 1.0;
        double w = unitWidth * 0.5;

        Path2D.Double rhombus = new Path2D.Double();
        rhombus.moveTo(xCoordinate, yCoordinate);
        rhombus.lineTo(xCoordinate + w, yCoordinate + unitHeight * 0.5);
        rhombus.lineTo(xCoordinate, yCoordinate + unitHeight);
        rhombus.lineTo(xCoordinate - w, yCoordinate + unitHeight * 0.5);
        rhombus.closePath();
        g2.fill(rhombus);

        if (unitWidth > 2.0) {
            g2.setColor(Color.black);
            g2.draw(new Line2D.Double(xCoordinate, yCoordinate, xCoordinate, yCoordinate + unitHeight));
        }

        return rhombus;
    }

    @Override
    public DrawingMode[] getDrawingModes() {
        return new DrawingMode[] { DrawingMode.STANDARD, DrawingMode.SQUISH };
    }

    @Override
    public DrawingMode getDefaultDrawingMode() {
        return DrawingMode.STANDARD;
    }
}
