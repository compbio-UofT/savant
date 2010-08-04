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
        if (modeName == "STANDARD") {
            renderPackMode(g2, gp, resolution);
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

        // draw the gene name, if possible
        String geneName = bedRecord.getName();
        boolean drawName = true;
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
        if (drawName) {
            Rectangle2D nameRect = g2.getFont().getStringBounds(geneName, g2.getFontRenderContext());
            double topMargin = (unitHeight - nameRect.getHeight());
            g2.setColor(lineColor);
            g2.drawString(geneName, (int)(startXPos - nameRect.getWidth() - 5), (int)(gp.transformYPos(level) - topMargin));
        }

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

        final double interval = 40;
        g2.setColor(color);
        for (double i=start+interval/2; i<end-interval/2; i+=interval) {
            Polygon arrow = new Polygon();
            if (strand == Strand.FORWARD) {
                //g2.drawLine((int)i, (int)y, (int)(i-height/4), (int)(y+height/4));
                //g2.drawLine((int)i,(int)y, (int)(i-height/4), (int)(y-height/4));
                arrow.addPoint((int)i, (int)y);
                arrow.addPoint((int)(i-height/4), (int)(y+height/4));
                arrow.addPoint((int)(i-height/4), (int)(y-height/4));
            }
            else {
                //g2.drawLine((int)i, (int)y, (int)(i+height/4), (int)(y+height/4));
                //g2.drawLine((int)i,(int)y, (int)(i+height/4), (int)(y-height/4));
                arrow.addPoint((int)i, (int)y);
                arrow.addPoint((int)(i+height/4), (int)(y+height/4));
                arrow.addPoint((int)(i+height/4), (int)(y-height/4));
            }
            g2.fill(arrow);
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
}
