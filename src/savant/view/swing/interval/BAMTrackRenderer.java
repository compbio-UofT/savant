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
 * BAMTrackRenderer.java
 * Created on Feb 1, 2010
 */

package savant.view.swing.interval;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.RangeController;
import savant.model.*;
import savant.model.data.interval.BAMIntervalTrack;
import savant.model.view.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.model.view.Mode;
import savant.util.IntervalPacker;
import savant.util.Range;
import savant.view.swing.BrowserDefaults;
import savant.view.swing.GraphPane;
import savant.view.swing.Savant;
import savant.view.swing.TrackRenderer;
import savant.view.swing.util.GlassMessagePane;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

/**
 * Class to perform all the rendering of a BAM Track in all its modes.
 * 
 * @author vwilliams
 */
public class BAMTrackRenderer extends TrackRenderer {
    
    private static Log log = LogFactory.getLog(BAMTrackRenderer.class);

    private static Font smallFont = new Font("Sans-Serif", Font.PLAIN, 10);
    private static Stroke oneStroke = new BasicStroke(1.0f);
    private static Stroke twoStroke= new BasicStroke(2.0f);

    // The number of standard deviations from the mean an arclength has to be before it's
    // considered discordant
    private static int DISCORDANT_STD_DEV = 3;

    public enum Strand { FORWARD, REVERSE };

    Mode drawMode;

    public BAMTrackRenderer() { this(new DrawingInstructions()); }

    public BAMTrackRenderer(
            DrawingInstructions drawingInstructions) {
        super(drawingInstructions);
    }
    
    @Override
    public void render(Graphics g, GraphPane gp) {

        Graphics2D g2 = (Graphics2D) g;

        if (this.getData() == null) {
            //GlassMessagePane.draw(g2, gp, "Too many intervals to display.", 500);
            return;
        }

        gp.setIsOrdinal(true);
        
        DrawingInstructions di = this.getDrawingInstructions();
        drawMode = (Mode) di.getInstruction(DrawingInstructions.InstructionName.MODE);
        Resolution r = (Resolution) di.getInstruction(DrawingInstructions.InstructionName.RESOLUTION.toString());

        String modeName = drawMode.getName();
        if (modeName.equals("STANDARD") || modeName.equals("VARIANTS")) {
            if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {
                renderPackMode(g2, gp, r);
            }
//            else {
//                renderCoverageMode(g2, gp);
//            }
        }
        else if (modeName.equals("MATE_PAIRS")) {
            renderArcMode(g2, gp);
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution r) {

        List<Object> data = this.getData();

        AxisRange axisRange = (AxisRange) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);
        ColorScheme cs = (ColorScheme) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
        Color linecolor = cs.getColor("LINE");

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



        if (drawMode.getName().equals("VARIANTS") && !Savant.getInstance().getGenome().isSequenceSet()) {
            GlassMessagePane.draw(g2, gp, "No reference sequence loaded. Switch to standard view", 500);
            return;
        }
        
        // display only a message if intervals will not be visible at this resolution
        if (gp.getUnitHeight() < 1) {
            GlassMessagePane.draw(g2, gp, "Too many intervals to display.", 500);
            return;
        }


        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

            ArrayList<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                Interval interval = intervalRecord.getInterval();

                BAMIntervalRecord bamRecord = (BAMIntervalRecord) intervalRecord;
                SAMRecord samRecord = bamRecord.getSamRecord();

                if (samRecord.getReadUnmappedFlag() == true) { // this read is unmapped, don't visualize it
                    continue;
                }

                Polygon strand = renderStrand(g2, gp, cs, samRecord, interval, level);

                if (drawMode.getName().equals("VARIANTS")) {
                    // visualize variations (indels and mismatches)
                    renderVariants(g2, gp, samRecord, level);
                }

                // draw outline, if there's room
                if (strand.getBounds().getHeight() > 4) {
                    g2.setColor(linecolor);
                    g2.draw(strand);
                }

            }
        }

    }

    private Polygon renderStrand(Graphics2D g2, GraphPane gp, ColorScheme cs, SAMRecord samRecord, Interval interval, int level) {

        Color forwardColor = cs.getColor("FORWARD_STRAND");
        Color reverseColor = cs.getColor("REVERSE_STRAND");

        double x=0;
        double y=0;
        double w=0;
        double h=0;

        double unitHeight;
        double unitWidth;
        unitHeight = gp.getUnitHeight();
        unitWidth = gp.getUnitWidth();
        double arrowHeight = unitHeight/2;
        double arrowWidth = unitHeight/4;

        boolean drawPoint = false;
        y = gp.transformYPos(level)-unitHeight;
        w = gp.getWidth(interval.getLength());

        if (w < 1) {
            return null; // don't draw intervals less than one pixel wide
        }
        if (w > arrowWidth) {
            drawPoint = true;
        }
        h = unitHeight;
        x = gp.transformXPos(interval.getStart());

        // find out which direction we're pointing
        boolean strandFlag = samRecord.getReadNegativeStrandFlag();
        Strand strand = strandFlag ? Strand.REVERSE : Strand.FORWARD ;

        Polygon pointyBar = new Polygon();
        pointyBar.addPoint((int)x, (int)y);
        pointyBar.addPoint((int)(x+w), (int)y);
        if (strand == Strand.FORWARD && drawPoint) {
            pointyBar.addPoint((int)(x+w+arrowWidth), (int)(y+arrowHeight));
        }
        pointyBar.addPoint((int)(x+w), (int)(y+h));
        pointyBar.addPoint((int)x, (int)(y+h));
        if (strand == Strand.REVERSE && drawPoint) {
            pointyBar.addPoint((int)(x-arrowWidth), (int)(y+arrowHeight));
        }
        if (strand == Strand.FORWARD) {
            g2.setColor(forwardColor);
        }
        else {
            g2.setColor(reverseColor);
        }
        g2.fill(pointyBar);

        return pointyBar;

    }

    private void renderVariants(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level) {

        ColorScheme cs = (ColorScheme) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
        Color linecolor = cs.getColor("LINE");

        double unitHeight;
        double unitWidth;
        unitHeight = gp.getUnitHeight();
        unitWidth = gp.getUnitWidth();

        // visualize variations (indels and mismatches)
        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();

        byte[] readBases = samRecord.getReadBases();
        boolean sequenceSaved = readBases.length > 0;

        Genome genome = Savant.getInstance().getGenome();
        try {
            byte[] refSeq = genome.getSequence(new Range(alignmentStart, alignmentEnd)).getBytes();

            Cigar cigar = samRecord.getCigar();
            int sequenceCursor = alignmentStart;
            int readCursor = alignmentStart;
            CigarOperator operator;
            int operatorLength;

            for (CigarElement cigarElement : cigar.getCigarElements()) {

                operatorLength = cigarElement.getLength();
                operator = cigarElement.getOperator();
                Rectangle2D.Double opRect = null;

                double opStart = gp.transformXPos(sequenceCursor);
                double opWidth = gp.getWidth(operatorLength);

                // delete
                if (operator == CigarOperator.D) {

                    opRect = new Rectangle2D.Double(
                            opStart,
                            gp.transformYPos(level)-unitHeight,
                            Math.max(opWidth, 1),
                            unitHeight);
                    g2.setColor(Color.black);
                    g2.fill(opRect);
                }
                // insert
                else if (operator == CigarOperator.I) {
//                    g2.setColor(Color.white);
//                    int xCoordinate = (int)gp.transformXPos(sequenceCursor);
//                    int yCoordinate = (int)(gp.transformYPos(level)-unitHeight);
//                    g2.drawLine(xCoordinate, (int)yCoordinate, xCoordinate, (int)(yCoordinate+unitHeight));
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.white);
                    int xCoordinate = (int)gp.transformXPos(sequenceCursor);
                    int yCoordinate = (int)(gp.transformYPos(level)-unitHeight) + 1;
                    if((int)unitWidth/3 < 4 || (int)(unitHeight/2) < 6){
                        yCoordinate = (int)(gp.transformYPos(level)-unitHeight);
                        int lineWidth = Math.max((int)(unitWidth * (2.0/3.0)), 1);
                        int xCoordinate1 = (int)(opStart - Math.floor(lineWidth/2));
                        int xCoordinate2 = (int)(opStart - Math.floor(lineWidth/2)) + lineWidth - 1;
                        if(xCoordinate1 == xCoordinate2) xCoordinate2++;
                        int[] xPoints = {xCoordinate1, xCoordinate2, xCoordinate2, xCoordinate1};
                        int[] yPoints = {yCoordinate, yCoordinate, yCoordinate+(int)unitHeight, yCoordinate+(int)unitHeight};
                        g2.fillPolygon(xPoints, yPoints, 4);
                    } else {
                        int[] xPoints = {xCoordinate, xCoordinate+(int)(unitWidth/3), xCoordinate, xCoordinate-(int)(unitWidth/3)};
                        int[] yPoints = {yCoordinate, yCoordinate+(int)(unitHeight/2), yCoordinate+(int)unitHeight-1, yCoordinate+(int)(unitHeight/2)};
                        g2.fillPolygon(xPoints, yPoints, 4);
                        if((int)unitWidth/3 >= 7 && (int)(unitHeight/2) >= 5){
                            g2.setColor(linecolor);
                            g2.drawLine(xCoordinate, (int)yCoordinate, xCoordinate, (int)(yCoordinate+unitHeight)-1);
                        }
                    }
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                }
                // match or mismatch
                else if (operator == CigarOperator.M) {

                    // some SAM files do not contain the read bases
                    if (sequenceSaved) {
                        // determine if there's a mismatch
                        for (int i=0; i<operatorLength; i++) {
                            int refIndex = sequenceCursor-alignmentStart+i;
                            int readIndex = readCursor-alignmentStart+i;
                            if (refSeq[refIndex] != readBases[readIndex]) {
                                byte[] readBase = new byte[1];
                                readBase[0] = readBases[readIndex];
                                String base = new String(readBase);
                                Color mismatchColor = null;
                                if (base.equals("A")) {
                                    mismatchColor = BrowserDefaults.A_COLOR;
                                }
                                else if (base.equals("C")) {
                                    mismatchColor = BrowserDefaults.C_COLOR;
                                }
                                else if (base.equals("G")) {
                                    mismatchColor = BrowserDefaults.G_COLOR;
                                }
                                else if (base.equals("T")) {
                                    mismatchColor = BrowserDefaults.T_COLOR;
                                }
                                double xCoordinate = gp.transformXPos(sequenceCursor+i);
                                double width = gp.getUnitWidth();
                                if (width < 1) width = 1;
                                opRect = new Rectangle2D.Double(xCoordinate,
                                        gp.transformYPos(level)-unitHeight,
                                        unitWidth,
                                        unitHeight);
                                g2.setColor(mismatchColor);
                                g2.fill(opRect);
                            }
                        }
                    }
                }
                // skipped
                else if (operator == CigarOperator.N) {
                    // draw nothing

                        opRect = new Rectangle2D.Double(opStart,
                                                        gp.transformYPos(level)-unitHeight,
                                                        opWidth,
                                                        unitHeight);
                        g2.setColor(Color.gray);
                        g2.fill(opRect);

                }
                // padding
                else if (operator == CigarOperator.P) {
                    // draw nothing

                }
                // hard clip
                else if (operator == CigarOperator.H) {
                    // draw nothing

                }
                // soft clip
                else if (operator == CigarOperator.S) {
                    // draw nothing

                }
                if (operator.consumesReadBases()) readCursor += operatorLength;
                if (operator.consumesReferenceBases()) sequenceCursor += operatorLength;
            }
        } catch (IOException e) {
            log.warn("Unable to read reference sequence");
            Savant.log("Unable to read reference sequence");
        }

    }

    private void renderArcMode(Graphics2D g2, GraphPane gp) {

        List<Object> data = this.getData();
        int numdata = this.getData().size();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AxisRange axisRange = (AxisRange) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);
        ColorScheme cs = (ColorScheme) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
        double threshold = (Double) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.ARC_MIN);
        int discordantMin = (Integer) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.DISCORDANT_MIN);
        int discordantMax = (Integer) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.DISCORDANT_MAX);
        Savant.log("discordantMin=" + discordantMin + " discordantMax=" + discordantMax);

        // set up colors
        Color normalArcColor = cs.getColor("REVERSE_STRAND");
        Color invertedReadColor = cs.getColor("INVERTED_READ");
        Color invertedMateColor = cs.getColor("INVERTED_MATE");
        Color evertedPairColor = cs.getColor("EVERTED_PAIR");
        Color discordantLengthColor = cs.getColor("DISCORDANT_LENGTH");

        // set graph pane's range parameters
        gp.setIsOrdinal(false);
        gp.setXRange(axisRange.getXRange());
        // Y range is given to us by BAMViewTrack for this mode
        gp.setYRange(axisRange.getYRange());

        // iterate through the data and draw
        for (int i = 0; i < numdata; i++) {

            BAMIntervalRecord record = (BAMIntervalRecord)data.get(i);
            SAMRecord samRecord = record.getSamRecord();

            // skip reads with no mapped mate
            if (!samRecord.getReadPairedFlag() || samRecord.getMateUnmappedFlag() || record.getType() == null) continue;

            int arcLength = Math.abs(samRecord.getInferredInsertSize());

            // skip reads with a zero insert length--probably mapping errors
            if (arcLength == 0) continue;

            int alignmentStart;
            int alignmentEnd;
            int mateAlignmentStart = samRecord.getMateAlignmentStart();
            if (samRecord.getAlignmentStart() > mateAlignmentStart) {
                if (!(mateAlignmentStart < RangeController.getInstance().getRangeStart())) {
                    // this is the second in the pair, and it doesn't span the beginning of the range, so don't draw anything
                    continue;
                }
                else {
                    // switch the mate start/end for the read start/end to deal with reversed position
                    alignmentStart = mateAlignmentStart;
                    alignmentEnd = mateAlignmentStart + samRecord.getReadLength();
                }
            }
            else {
                alignmentStart = samRecord.getAlignmentStart();
                alignmentEnd = samRecord.getAlignmentEnd();

            }
            // at this point alignmentStart/End refers the the start end of the first occurrence in the pair

            BAMIntervalRecord.PairType type = record.getType();


            int intervalStart;
            switch (type) {
                case INVERTED_READ:
                    intervalStart = alignmentStart;
                    g2.setColor(invertedReadColor);
                    g2.setStroke(twoStroke);
                    break;
                case INVERTED_MATE:
                    intervalStart = alignmentEnd;
                    g2.setColor(invertedMateColor);
                    g2.setStroke(twoStroke);
                    break;
                case EVERTED:
                    intervalStart = alignmentStart;
                    g2.setColor(evertedPairColor);
                    g2.setStroke(twoStroke);
                    break;
                default:
                    // make sure arclength is over our threshold
                    if (threshold != 0.0d && threshold < 1.0d && arcLength < axisRange.getXRange().getLength()*threshold) {
                        continue;
                    }
                    else if (threshold > 1.0d && arcLength < threshold) {
                        continue;
                    }
                    
                    intervalStart = alignmentEnd;

                    if (arcLength > discordantMax || arcLength < discordantMin) {
                        g2.setColor(discordantLengthColor);
                        g2.setStroke(twoStroke);
                    }
                    else {
                        g2.setColor(normalArcColor);
                        g2.setStroke(oneStroke);
                    }
                    break;
            }
            int arcHeight = arcLength;

            int rectWidth = (int)(gp.getWidth(arcLength));
            int rectHeight = (int)(gp.getHeight(arcHeight*2));

            int xOrigin = (int)(gp.transformXPos(intervalStart));
            int yOrigin = (int)(gp.transformYPos(arcHeight));

            g2.drawArc(xOrigin, yOrigin, rectWidth, rectHeight, -180, -180);

        }

        // draw legend
        /*String[] legendStrings = {"Discordant Length", "Inverted Read", "Inverted Mate", "Everted Pair"};
        Color[] legendColors = {discordantLengthColor, invertedReadColor, invertedMateColor, evertedPairColor};
        String sizingString = legendStrings[0];
        Rectangle2D stringRect = smallFont.getStringBounds(sizingString, g2.getFontRenderContext());

        drawLegend(g2, legendStrings, legendColors, (int)(gp.getWidth()-stringRect.getWidth()-5), (int)(2*stringRect.getHeight() + 5+2));
        */
    }

    private void drawLegend(Graphics2D g2, String[] legendStrings, Color[] legendColors, int startx, int starty) {

        g2.setFont(smallFont);

        int x = startx;
        int y = starty;
        String legendString;
        for (int i=0; i<legendStrings.length; i++) {
            legendString = legendStrings[i];
            g2.setColor(legendColors[i]);
            g2.setStroke(twoStroke);
            Rectangle2D stringRect = smallFont.getStringBounds(legendString, g2.getFontRenderContext());
            g2.drawLine(x-25, y-(int)stringRect.getHeight()/2, x-5, y-(int)stringRect.getHeight()/2);
            g2.setColor(BrowserDefaults.colorAccent);
            g2.setStroke(oneStroke);
            g2.drawString(legendString, x, y);

            y += stringRect.getHeight()+2;

        }
    }

    @Override
    public boolean hasHorizontalGrid() {
        Mode drawMode = (Mode)getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.MODE);
        String modeName = drawMode.getName();
        if (modeName == "MATE_PAIRS") {
            return true;
        }
        else {
            return false;
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

    //THIS IS A BIT MESSY
    @Override
    public JPanel arcLegendPaint(){
        JPanel panel = new LegendPanel();
        panel.setPreferredSize(new Dimension(125,61));
        return panel;
    }


    private class LegendPanel extends JPanel{
        private boolean isHidden = false;

        public LegendPanel(){
            this.setToolTipText("Hide Legend");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent me) {
                    changeMode();
                }
            });
        }

        private void changeMode(){
            isHidden = !isHidden;
            if(isHidden){
                this.setToolTipText("Show Legend");
                this.setPreferredSize(new Dimension(22,23));
            } else {
                this.setToolTipText("Hide Legend");
                this.setPreferredSize(new Dimension(125,61));
            }
            this.setVisible(false);
            this.paint(this.getGraphics());
            this.setVisible(true);

        }

        @Override
        public void paint(Graphics g){

            ColorScheme cs = (ColorScheme) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());

            // set up colors
            Color normalArcColor = cs.getColor("REVERSE_STRAND");
            Color invertedReadColor = cs.getColor("INVERTED_READ");
            Color invertedMateColor = cs.getColor("INVERTED_MATE");
            Color evertedPairColor = cs.getColor("EVERTED_PAIR");
            Color discordantLengthColor = cs.getColor("DISCORDANT_LENGTH");

            String[] legendStrings = {"Discordant Length", "Inverted Read", "Inverted Mate", "Everted Pair"};
            Color[] legendColors = {discordantLengthColor, invertedReadColor, invertedMateColor, evertedPairColor};
            String sizingString = legendStrings[0];

            if(isHidden){
                drawHidden(g);
            }else{
                drawLegend(g, legendStrings, legendColors);
            }
        }

        private void drawLegend(Graphics g, String[] legendStrings, Color[] legendColors){

            Graphics2D g2 = (Graphics2D) g;

            g2.setFont(smallFont);

            GradientPaint gp = new GradientPaint(0, 0,
                Color.WHITE, 0, 60,
                new Color(230,230,230));

            g2.setPaint(gp);
            g2.fillRect(0, 0, 125, 60);


            g2.setColor(Color.BLACK);
            g2.draw(new Rectangle2D.Double(0, 0, 125, 60));

            int x = 30;
            int y = 15;
            String legendString;
            for (int i=0; i<legendStrings.length; i++) {
                legendString = legendStrings[i];
                g2.setColor(legendColors[i]);
                g2.setStroke(twoStroke);
                Rectangle2D stringRect = smallFont.getStringBounds(legendString, g2.getFontRenderContext());
                g2.drawLine(x-25, y-(int)stringRect.getHeight()/2, x-5, y-(int)stringRect.getHeight()/2);
                g2.setColor(BrowserDefaults.colorAccent);
                g2.setStroke(oneStroke);
                g2.drawString(legendString, x, y);

                y += stringRect.getHeight()+2;
            }
        }

        private void drawHidden(Graphics g){

            Graphics2D g2 = (Graphics2D) g;

            g2.setFont(smallFont);

            GradientPaint gp = new GradientPaint(0, 0,
                Color.WHITE, 0, 22,
                new Color(230,230,230));

            g2.setPaint(gp);
            g2.fillRect(0, 0, 125, 60);

            g2.setColor(Color.BLACK);
            g2.draw(new Rectangle2D.Double(0, 0, 22, 22));

            int[] xp = {8,14,14};
            int[] yp = {11,5,17};
            g2.fillPolygon(new Polygon(xp,yp,3));
        }
    }
}
