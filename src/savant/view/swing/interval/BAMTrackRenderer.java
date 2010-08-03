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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JViewport;
import savant.controller.ReferenceController;
import savant.view.swing.interval.Pileup.Nucleotide;

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

    //true -> dynamic, false -> fixed height
    //private boolean dynamicMode = true;
    //private int intervalHeight = 12;
    private int maximumHeight = 12;
    private int minimumHeight = 4;
    private boolean renderFixed = false;

    // The number of standard deviations from the mean an arclength has to be before it's
    // considered discordant
    private static int DISCORDANT_STD_DEV = 3;

    public enum Strand { FORWARD, REVERSE };

    private Mode drawMode;

    public BAMTrackRenderer() { this(new DrawingInstructions()); }

    public BAMTrackRenderer(
            DrawingInstructions drawingInstructions) {
        super(drawingInstructions);
    }
    
    @Override
    public void render(Graphics g, GraphPane gp) {

        Graphics2D g2 = (Graphics2D) g;

        gp.setIsOrdinal(true);
        
        DrawingInstructions di = this.getDrawingInstructions();

        Boolean refexists = (Boolean) di.getInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS);

        if (!refexists) {
            resizeFrame(gp);
            GlassMessagePane.draw(g2, gp, "no data for reference", 500);
            return;
        // Don't display sequence if data is too high resolution to see.
        } 

        drawMode = (Mode) di.getInstruction(DrawingInstructions.InstructionName.MODE);
        Resolution r = (Resolution) di.getInstruction(DrawingInstructions.InstructionName.RESOLUTION.toString());

        String modeName = drawMode.getName();
        if (modeName.equals("STANDARD") || modeName.equals("VARIANTS")) {
            if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {
                renderPackMode(g2, gp, r);
            } else {
                this.resizeFrame(gp);
            }
//            else {
//                renderCoverageMode(g2, gp);
//            }
        }
        else if (modeName.equals("MATE_PAIRS")) {
            renderArcMode(g2, gp);
        }
        else if (modeName.equals("SNP")){
            renderSNPMode(g2, gp, r);
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution r) {

        List<Object> data = this.getData();

        AxisRange axisRange = (AxisRange) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);
        ColorScheme cs = (ColorScheme) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
        Color linecolor = cs.getColor("LINE");
        Range range = axisRange.getXRange();

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

        byte[] refSeq=null;
        if (drawMode.getName().equals("VARIANTS")) {
            Genome genome = ReferenceController.getInstance().getGenome();
            if (!genome.isSequenceSet()) {
                this.resizeFrame(gp);
                GlassMessagePane.draw(g2, gp, "No reference sequence loaded. Switch to standard view", 500);
                return;
            }

            // fetch reference sequence for comparison with cigar string
            try {
                refSeq = genome.getSequence(ReferenceController.getInstance().getReferenceName(), range).getBytes();
            } catch (IOException e) {
                // FIXME: this exception should be propagated to someone who can alert the user
                log.warn("Unable to read reference sequence");
                return;
            }
        }
        
        // display only a message if intervals will not be visible at this resolution
        /*if (gp.getUnitHeight() < 1) {
            GlassMessagePane.draw(g2, gp, "Too many intervals to display.", 500);
            return;
        }*/

        renderFixed = false;
        renderFixed = gp.getUnitHeight() < minimumHeight;
        //if(dynamicMode && !renderFixed){
        if(!renderFixed){
            //double unitHeight = (double) ((JViewport)gp.getParent()).getHeight() / (maxYRange);
            double unitHeight = (double) ((JViewport)gp.getParent().getParent()).getHeight() / (maxYRange);
            if(unitHeight < minimumHeight) renderFixed = true;
        }

        //if(!dynamicMode || renderFixed){
        if(renderFixed){

            int currentHeight = gp.getHeight();
            int currentWidth = gp.getParentFrame().getFrameLandscape().getWidth()-2;
            //int currentHeight1 = ((JViewport)gp.getParent()).getHeight();
            int currentHeight1 = ((JViewport)gp.getParent().getParent()).getHeight();
            int expectedHeight = Math.max((int)((intervals.size() * maximumHeight) / 0.9), currentHeight1);

            if(expectedHeight != currentHeight || currentWidth != gp.getWidth()){
                gp.bf = new BufferedImage(currentWidth, expectedHeight, BufferedImage.TYPE_INT_RGB);
                gp.newHeight = expectedHeight;
                gp.setPaneResize(true);
                g2 = gp.bf.createGraphics();
                gp.renderBackground(g2);
                return;
            }
            gp.setUnitHeight(maximumHeight);
            gp.setYRange(new Range(0,(int)Math.ceil(expectedHeight/maximumHeight)));
        //} else if (gp.getSize() != ((JViewport)gp.getParent()).getSize()){
        } else if (gp.getSize() != ((JViewport)gp.getParent().getParent()).getSize()){
            this.resizeFrame(gp);
        }

        
        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

            ArrayList<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                Interval interval = intervalRecord.getInterval();

                BAMIntervalRecord bamRecord = (BAMIntervalRecord) intervalRecord;
                SAMRecord samRecord = bamRecord.getSamRecord();

                if (samRecord.getReadUnmappedFlag()) { // this read is unmapped, don't visualize it
                    continue;
                }

                Polygon strand = renderStrand(g2, gp, cs, samRecord, interval, level, range);

                if (drawMode.getName().equals("VARIANTS")) {
                    // visualize variations (indels and mismatches)
                    renderVariants(g2, gp, samRecord, level, refSeq, range);
                }

                // draw outline, if there's room
                if (strand.getBounds().getHeight() > 4) {
                    g2.setColor(linecolor);
                    g2.draw(strand);
                }

            }
        }

    }

    private Polygon renderStrand(Graphics2D g2, GraphPane gp, ColorScheme cs, SAMRecord samRecord, Interval interval,
                                 int level, Range range) {

        Color forwardColor = cs.getColor("FORWARD_STRAND");
        Color reverseColor = cs.getColor("REVERSE_STRAND");

        double x=0;
        double y=0;
        double w=0;
        double h=0;


        double unitHeight = gp.getUnitHeight();
        double unitWidth = gp.getUnitWidth();

        //unitHeight = intervalHeight;
        double arrowHeight = unitHeight/2;
        double arrowWidth = unitHeight/4;

        // cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo()) + unitWidth;

        boolean drawPoint = false;
        //y = gp.transformYPos(level)-unitHeight;
        y = gp.transformYPos(0) - (level + 1)*unitHeight;
        
        w = gp.getWidth(interval.getLength());

        if (w < 1) {
            return null; // don't draw intervals less than one pixel wide
        }
        if (w > arrowWidth) {
            drawPoint = true;
        }
        h = unitHeight;
        x = gp.transformXPos(interval.getStart());

        // cut off x and w so no drawing happens off-screen
//        double x2 = Math.min(rightMostX, x+w);
//        x = Math.max(leftMostX, x);
//        w = x2 - x;
        boolean cutoffLeft = false;
        boolean cutoffRight = false;
        double x2;
        if (rightMostX < x+w) {
            x2 = rightMostX;
            cutoffRight = true;
        }
        else {
            x2 = x+w;
        }
        if (leftMostX > x) {
            x = leftMostX;
            cutoffLeft = true;
        }
        w = x2 - x;

        // find out which direction we're pointing
        boolean strandFlag = samRecord.getReadNegativeStrandFlag();
        Strand strand = strandFlag ? Strand.REVERSE : Strand.FORWARD ;

        Polygon pointyBar = new Polygon();
        pointyBar.addPoint((int)x, (int)y);
        pointyBar.addPoint((int)(x+w), (int)y);
        if (strand == Strand.FORWARD && drawPoint && !cutoffRight) {
            pointyBar.addPoint((int)(x+w+arrowWidth), (int)(y+arrowHeight));
        }
        pointyBar.addPoint((int)(x+w), (int)(y+h));
        pointyBar.addPoint((int)x, (int)(y+h));
        if (strand == Strand.REVERSE && drawPoint && !cutoffLeft) {
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

    private void renderVariants(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, byte[] refSeq, Range range) {

        DrawingInstructions di = getDrawingInstructions();
        ColorScheme cs = (ColorScheme) di.getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
        Color linecolor = cs.getColor("LINE");


        double unitHeight;
        double unitWidth;
        unitHeight = gp.getUnitHeight();
        unitWidth = gp.getUnitWidth();
        //unitHeight = intervalHeight;

        // cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo()) + unitWidth;

        // visualize variations (indels and mismatches)
        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();

        byte[] readBases = samRecord.getReadBases();
        boolean sequenceSaved = readBases.length > 0;
        Cigar cigar = samRecord.getCigar();

        // absolute positions in the reference sequence and the read bases, set after each cigar operator is processed
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

            // cut off start and width so no drawing happens off-screen, must be done in the order w, then x, since w depends on first value of x
            double x2 = Math.min(rightMostX, opStart+opWidth);
            opStart = Math.max(leftMostX, opStart);
            opWidth = x2 - opStart;

            // delete
            if (operator == CigarOperator.D) {

                double width = gp.getWidth(operatorLength);
                if (width < 1) width = 1;
//                opRect = new Rectangle2D.Double(
//                        opStart,
//                        gp.transformYPos(level)-unitHeight,
//                        Math.max(opWidth, 1),
//                        unitHeight);
                opRect = new Rectangle2D.Double(
                        opStart,
                        gp.transformYPos(0)-((level + 1)*unitHeight),
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
                //int yCoordinate = (int)(gp.transformYPos(level)-unitHeight) + 1;
                int yCoordinate = (int)(gp.transformYPos(0)-((level + 1)*unitHeight)) + 1;
                if((int)unitWidth/3 < 4 || (int)(unitHeight/2) < 6){
                    //yCoordinate = (int)(gp.transformYPos(level)-unitHeight);
                    yCoordinate = yCoordinate - 1;
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
                        // indices into refSeq and readBases associated with this position in the cigar string
                        int readIndex = readCursor-alignmentStart+i;
                        int refIndex = sequenceCursor+i-range.getFrom();

                        if (refIndex < 0) continue;  // outside sequence and drawing range
                        if (refIndex > refSeq.length-1) continue;

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
//                            opRect = new Rectangle2D.Double(xCoordinate,
//                                    gp.transformYPos(level)-unitHeight,
//                                    unitWidth,
//                                    unitHeight);
                            opRect = new Rectangle2D.Double(xCoordinate,
                                    gp.transformYPos(0)-((level + 1)*unitHeight),
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

//                opRect = new Rectangle2D.Double(opStart,
//                                                    gp.transformYPos(level)-unitHeight,
//                                                    opWidth,
//                                                    unitHeight);
                opRect = new Rectangle2D.Double(opStart,
                                                    gp.transformYPos(0)-((level+1)*unitHeight),
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

        resizeFrame(gp);

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

    private void renderSNPMode(Graphics2D g2, GraphPane gp, Resolution r){

        List<Object> data = this.getData();
        Genome genome = ReferenceController.getInstance().getGenome();

        AxisRange axisRange = (AxisRange) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);
        ColorScheme cs = (ColorScheme) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
        Color linecolor = cs.getColor("LINE");

        List<Pileup> pileups = new ArrayList<Pileup>();

        // make the pileups
        int length = axisRange.getXMax() - axisRange.getXMin() + 1;
        int startPosition = axisRange.getXMin();
        for (int j = 0; j < length; j++) {
            pileups.add(new Pileup(startPosition + j));
            //pileups.add(new Pileup(viewTrackName, startPosition + i, Pileup.getNucleotide(genome.getSequence(axisRange.getXRange()).charAt(i))));
        }

        // go through the samrecords and edit the pileups
        for (int i = 0; i < data.size(); i++) {
            BAMIntervalRecord bir = (BAMIntervalRecord) data.get(i);
            SAMRecord sr = bir.getSamRecord();
            try {
                updatePileupsFromSAMRecord(pileups, genome, sr, startPosition);
            } catch (IOException ex) {
                Logger.getLogger(BAMTrackRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        resizeFrame(gp);

        double maxHeight = 0;
        for(Pileup p : pileups){
            double current = p.getTotalCoverage();
            if(current > maxHeight) maxHeight = current;
        }

        gp.setIsOrdinal(false);
        gp.setXRange(axisRange.getXRange());
        gp.setYRange(new Range(0,(int)Math.rint((double)maxHeight/0.9)));

        double unitHeight = (Math.rint(gp.transformYPos(0) * 0.9)) / maxHeight;
        double unitWidth =  gp.getUnitWidth();

        for(Pileup p : pileups){

            Nucleotide snpNuc = null;
            int bottom = (int)gp.transformYPos(0);

            Nucleotide genomeNuc = null;
            if(genome.isSequenceSet()){
                String genomeNucString = "A";
                try {      
                    genomeNucString = genome.getSequence(ReferenceController.getInstance().getReferenceName(), new Range((int) p.getPosition(), (int) p.getPosition()));
                } catch (IOException ex) {
                    Logger.getLogger(BAMTrackRenderer.class.getName()).log(Level.SEVERE, null, ex);
                }
                genomeNuc = Pileup.stringToNuc(genomeNucString);
                snpNuc = genomeNuc;
            }


            while((genome.isSequenceSet() && p.getCoverage(snpNuc) > 0) || (snpNuc = p.getLargestNucleotide()) != null){

                double x = gp.transformXPos((int)p.getPosition());
                double coverage = p.getCoverage(snpNuc);

                Color subPileColor = null;
                if(genome.isSequenceSet() && snpNuc.equals(genomeNuc)){
                    subPileColor = cs.getColor("REVERSE_STRAND");
                } else {
                    if(snpNuc.equals(Nucleotide.A)) subPileColor = BrowserDefaults.A_COLOR;
                    else if (snpNuc.equals(Nucleotide.C)) subPileColor = BrowserDefaults.C_COLOR;
                    else if (snpNuc.equals(Nucleotide.G)) subPileColor = BrowserDefaults.G_COLOR;
                    else if (snpNuc.equals(Nucleotide.T)) subPileColor = BrowserDefaults.T_COLOR;
                    //FIXME: what do we do here?
                    else if (snpNuc.equals(Nucleotide.OTHER)) subPileColor = Color.BLACK;
                }

                int h = (int)(unitHeight * coverage);
                int y = bottom-h;

                g2.setColor(subPileColor);
                g2.fillRect((int)x, y, Math.max((int)Math.rint(unitWidth), 1), h);

                bottom -= h;
                p.clearNucleotide(snpNuc);
            }
        }
    }

    private void updatePileupsFromSAMRecord(List<Pileup> pileups, Genome genome, SAMRecord samRecord, long startPosition) throws IOException {

        // the start and end of the alignment
        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();

        // the read sequence
        byte[] readBases = samRecord.getReadBases();
        boolean sequenceSaved = readBases.length > 0; // true iff read sequence is set

        // return if no bases (can't be used for SNP calling)
        if (!sequenceSaved) {
            return;
        }

        // the reference sequence
        //byte[] refSeq = genome.getSequence(new Range(alignmentStart, alignmentEnd)).getBytes();

        // get the cigar object for this alignment
        Cigar cigar = samRecord.getCigar();

        // set cursors for the reference and read
        int sequenceCursor = alignmentStart;
        int readCursor = alignmentStart;

        int pileupcursor = (int) (alignmentStart - startPosition);

        // cigar variables
        CigarOperator operator;
        int operatorLength;

        // consider each cigar element
        for (CigarElement cigarElement : cigar.getCigarElements()) {

            operatorLength = cigarElement.getLength();
            operator = cigarElement.getOperator();

            // delete
            if (operator == CigarOperator.D) {
                // [ DRAW ]
            } // insert
            else if (operator == CigarOperator.I) {
                // [ DRAW ]
            } // match **or mismatch**
            else if (operator == CigarOperator.M) {

                // some SAM files do not contain the read bases
                if (sequenceSaved) {
                    // determine if there's a mismatch
                    for (int i = 0; i < operatorLength; i++) {
                        int refIndex = sequenceCursor - alignmentStart + i;
                        int readIndex = readCursor - alignmentStart + i;

                        byte[] readBase = new byte[1];
                        readBase[0] = readBases[readIndex];

                        Nucleotide readN = Pileup.getNucleotide((new String(readBase)).charAt(0));

                        int j = i + (int) (alignmentStart - startPosition);
                        //for (int j = pileupcursor; j < operatorLength; j++) {
                        if (j >= 0 && j < pileups.size()) {
                            Pileup p = pileups.get(j);
                            p.pileOn(readN);
//                            /System.out.println("(P) " + readN + "\t@\t" + p.getPosition());
                        }
                        //}
                    }
                }
            } // skipped
            else if (operator == CigarOperator.N) {
                // draw nothing
            } // padding
            else if (operator == CigarOperator.P) {
                // draw nothing
            } // hard clip
            else if (operator == CigarOperator.H) {
                // draw nothing
            } // soft clip
            else if (operator == CigarOperator.S) {
                // draw nothing
            }


            if (operator.consumesReadBases()) {
                readCursor += operatorLength;
            }
            if (operator.consumesReferenceBases()) {
                sequenceCursor += operatorLength;
                pileupcursor += operatorLength;
            }
        }
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

    /*@Override
    public void setIntervalMode(String mode){
        if(mode.equals("dynamic")){
            this.dynamicMode = true;
        } else {
            this.dynamicMode = false;
        }
       // this.resizeRequired = true;
    }*/

    public int getMaximumHeight(){
        return this.maximumHeight;
    }

    public int getMinimumHeight(){
        return this.minimumHeight;
    }

    public void setMaximumHeight(int h){
        this.maximumHeight = h;
    }

    public void setMinimumHeight(int h){
        this.minimumHeight = h;
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
