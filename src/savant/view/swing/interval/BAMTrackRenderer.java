/*
 * BAMTrackRenderer.java
 * Created on Feb 1, 2010
 *
 *
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JViewport;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.data.event.DataRetrievalEvent;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Genome;
import savant.data.types.Interval;
import savant.data.types.IntervalRecord;
import savant.data.types.ReadPairIntervalRecord;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.file.DataFormat;
import savant.settings.ColourSettings;
import savant.util.*;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;
import savant.view.swing.interval.Pileup.Nucleotide;

/**
 * Class to perform all the rendering of a BAM Track in all its modes.
 * 
 * @author vwilliams
 */
public class BAMTrackRenderer extends TrackRenderer {

    private static final int minTransparency = 20;
    private static final int maxTransparency = 255;

    /** MODE */
    public static final String STANDARD_MODE = "Standard";
    public static final String MISMATCH_MODE = "Mismatch";
    public static final String STANDARD_PAIRED_MODE = "Read Pair (Standard)";
    public static final String ARC_PAIRED_MODE = "Read Pair (Arc)";
    public static final String MAPPING_QUALITY_MODE = "Mapping Quality";
    public static final String BASE_QUALITY_MODE = "Base Quality";
    public static final String SNP_MODE = "SNP";

    private static Log LOG = LogFactory.getLog(BAMTrackRenderer.class);

    private static Font smallFont = new Font("Sans-Serif", Font.PLAIN, 10);
    private static Stroke oneStroke = new BasicStroke(1.0f);
    private static Stroke twoStroke= new BasicStroke(2.0f);

    //true -> dynamic, false -> fixed height
    //private boolean dynamicMode = true;
    //private int intervalHeight = 12;
    private int maximumHeight = 12;
    private int minimumHeight = 4;
    private boolean renderFixed = false;

    private byte[] refSeq = null;

    private int offset = 0;

    // The number of standard deviations from the mean an arclength has to be before it's
    // considered discordant
    private static int DISCORDANT_STD_DEV = 3;

    @Override
    public List<String> getRenderingModes() {
        List<String> modes = new ArrayList<String>();
        modes.add(STANDARD_MODE);
        modes.add(MISMATCH_MODE);
        //modes.add(STANDARD_PAIRED_MODE);
        modes.add(ARC_PAIRED_MODE);
        modes.add(MAPPING_QUALITY_MODE);
        modes.add(BASE_QUALITY_MODE);
        modes.add(SNP_MODE);
        return modes;
    }

    @Override
    public String getDefaultRenderingMode() {
        return MISMATCH_MODE;
    }

    public enum Strand { FORWARD, REVERSE };

    private String drawMode;

    public BAMTrackRenderer() {
        super(DataFormat.INTERVAL_BAM);
    }

    @Override
    public void dataRetrievalCompleted(DataRetrievalEvent evt) {
        String mode = (String)instructions.get(DrawingInstruction.MODE);
        if (mode.equals(ARC_PAIRED_MODE)){// && !instructions.containsKey(DrawingInstruction.ERROR)) {
            long maxDataValue = BAMTrack.getArcYMax(evt.getData());
            Range range = (Range)instructions.get(DrawingInstruction.RANGE);
            addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(0,(int)Math.round(maxDataValue+maxDataValue*0.1))));
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

        String errorMessage = (String)instructions.get(DrawingInstruction.ERROR);
        if (errorMessage != null){
            throw new RenderingException(errorMessage);
        }

        drawMode = (String)instructions.get(DrawingInstruction.MODE);
        Resolution r = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        String modeName = drawMode;

        if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {
            if(modeName.equals("Mismatch") || modeName.equals("SNP")){
                // fetch reference sequence for comparison with cigar string
                Genome genome = ReferenceController.getInstance().getGenome();
                if(genome.isSequenceSet()){
                   AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
                    Range range = axisRange.getXRange();
                    try {
                        refSeq = genome.getSequence(ReferenceController.getInstance().getReferenceName(), range);
                    } catch (IOException e) {
                        throw new RenderingException(e.getMessage());
                    }
                }
            }
        }

        if (modeName.equals(STANDARD_MODE) || modeName.equals(MISMATCH_MODE) || modeName.equals(MAPPING_QUALITY_MODE) || modeName.equals(BASE_QUALITY_MODE)) {
            if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {
                renderPackMode(g2, gp, r);
            } else {
                this.resizeFrame(gp);
            }
//            else {
//                renderCoverageMode(g2, gp);
//            }
        } else if (modeName.equals(STANDARD_PAIRED_MODE)) {
            if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {
                renderStandardMatepairMode(g2, gp, r);
            } else {
                this.resizeFrame(gp);
            }
        }
        else if (modeName.equals(ARC_PAIRED_MODE)) {
            renderArcMatepairMode(g2, gp);
        }
        else if (modeName.equals(SNP_MODE)){
            if (r == Resolution.VERY_HIGH || r == Resolution.HIGH) {
                renderSNPMode(g2, gp, r);
            } else {
                this.resizeFrame(gp);
            }
        }
    }

    private void renderStandardMatepairMode(Graphics2D g2, GraphPane gp, Resolution resolution) throws RenderingException {
        //set position offset for scrollpane
        this.offset = gp.getOffset();

        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        ColorScheme cs = (ColorScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor("Line");
        Range range = axisRange.getXRange();

        List mates = ReadPairIntervalRecord.getMatePairs(data);
        Collections.sort(mates);

        for (Object o : mates) {
            System.out.println(o);
        }

        System.out.println("Found " + mates.size() + " mates from " + data.size() + " alignments");

        IntervalPacker packer = new IntervalPacker(mates);
        // TODO: when it becomes possible, choose an appropriate number for breathing room parameter
//        Map<Integer, ArrayList<IntervalRecord>> intervals = packer.pack(10);
        ArrayList<List<IntervalRecord>> intervals = packer.pack(10);

        int levelnum = 0;
        int numpacked = 0;
        System.out.println("Pack:");
        for (List<IntervalRecord> list : intervals) {
            System.out.println(levelnum + ". " + list.size());
            levelnum++;
            numpacked+= list.size();
        }
        System.out.println("Num packed: " + numpacked);

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

        renderFixed = false;
        renderFixed = gp.getUnitHeight() < minimumHeight;
        if(!renderFixed){
            double unitHeight = (double) ((JViewport)gp.getParent().getParent()).getHeight() / (maxYRange);
            if(unitHeight < minimumHeight) renderFixed = true;
        }

        if(renderFixed){
            int currentHeight = gp.getHeight();
            int currentWidth = gp.getParentFrame().getFrameLandscape().getWidth();
            int currentHeight1 = ((JViewport)gp.getParent().getParent()).getHeight();
            int expectedHeight = Math.max((int)((intervals.size() * maximumHeight) / 0.9), currentHeight1);

            if(expectedHeight != currentHeight || currentWidth != gp.getWidth()){
//                gp.setBufferedImage(new BufferedImage(currentWidth, expectedHeight, BufferedImage.TYPE_INT_RGB));
                gp.newHeight = expectedHeight;
                gp.setPaneResize(true);
                return;
            }
            gp.setUnitHeight(maximumHeight);
            gp.setYRange(new Range(0,(int)Math.ceil(expectedHeight/maximumHeight)));
        } else if (gp.getSize() != ((JViewport)gp.getParent().getParent()).getSize()){
            this.resizeFrame(gp);
        }

        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

//            ArrayList<IntervalRecord> intervalsThisLevel = intervals.get(level);
            List<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                //Interval interval = intervalRecord.getInterval();

                ReadPairIntervalRecord mrecord = (ReadPairIntervalRecord) intervalRecord;

                if (mrecord.isSingleton()) {

                    SAMRecord samRecord = mrecord.getSingletonRecord();

                    if (samRecord.getReadUnmappedFlag()) { // this read is unmapped, don't visualize it

                        this.recordToShapeMap.put(intervalRecord, null);

                        continue;
                    }

                    Color readcolor = null;

                    boolean strandFlag = samRecord.getReadNegativeStrandFlag();
                    Strand strand = strandFlag ? Strand.REVERSE : Strand.FORWARD ;

                        if (strand == Strand.FORWARD) {
                            g2.setColor(cs.getColor("Forward Strand"));
                        }
                        else {
                            g2.setColor(cs.getColor("Reverse Strand"));
                        }


                    Polygon readshape = renderRead(g2, gp, cs, samRecord, new Interval(samRecord.getAlignmentStart(), samRecord.getAlignmentEnd()), level, range, readcolor);

                    this.recordToShapeMap.put(intervalRecord, readshape);

                    // draw outline, if there's room
                    if (readshape.getBounds().getHeight() > 4) {
                        g2.setColor(linecolor);
                        g2.draw(readshape);
                    }
                }
            }
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution r) throws RenderingException {

        //set position offset for scrollpane
        this.offset = gp.getOffset();

        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        ColorScheme cs = (ColorScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor("Line");
        Range range = axisRange.getXRange();

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
       
        if (drawMode.equals(MISMATCH_MODE)) {
            Genome genome = ReferenceController.getInstance().getGenome();
            if (!genome.isSequenceSet()) {
                throw new RenderingException("No reference sequence loaded. Switch to standard view");
            }
        }

        renderFixed = false;
        renderFixed = gp.getUnitHeight() < minimumHeight;
        if(!renderFixed){
            double unitHeight = (double) ((JViewport)gp.getParent().getParent()).getHeight() / (maxYRange);
            if(unitHeight < minimumHeight) renderFixed = true;
        }

        if(renderFixed){
            int currentHeight = gp.getHeight();
            int currentWidth = gp.getParentFrame().getFrameLandscape().getWidth();
            int currentHeight1 = ((JViewport)gp.getParent().getParent()).getHeight();
            int expectedHeight = Math.max((int)((intervals.size() * maximumHeight) / 0.9), currentHeight1);

            if(expectedHeight != currentHeight || currentWidth != gp.getWidth()){
//                gp.setBufferedImage(new BufferedImage(currentWidth, expectedHeight, BufferedImage.TYPE_INT_RGB));
                gp.newHeight = expectedHeight;
                gp.setPaneResize(true);
                return;
            }
            gp.setUnitHeight(maximumHeight);
            gp.setYRange(new Range(0,(int)Math.ceil(expectedHeight/maximumHeight)));
        } else if (gp.getSize() != ((JViewport)gp.getParent().getParent()).getSize()){
            this.resizeFrame(gp);
        }
        
        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

//            ArrayList<IntervalRecord> intervalsThisLevel = intervals.get(level);
            List<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                Interval interval = intervalRecord.getInterval();

                BAMIntervalRecord bamRecord = (BAMIntervalRecord) intervalRecord;
                SAMRecord samRecord = bamRecord.getSamRecord();

                if (samRecord.getReadUnmappedFlag()) { // this read is unmapped, don't visualize it

                    this.recordToShapeMap.put(intervalRecord, null);

                    continue;
                }

                Color readcolor = null;

                boolean strandFlag = samRecord.getReadNegativeStrandFlag();
                Strand strand = strandFlag ? Strand.REVERSE : Strand.FORWARD ;

                if (drawMode.equals(MAPPING_QUALITY_MODE)) {
                    Color basecolor = null;
                    if (strand == Strand.FORWARD) {
                        basecolor = cs.getColor("Forward Strand");
                    }
                    else {
                        basecolor = cs.getColor("Reverse Strand");
                    }

                    int alpha = samRecord.getMappingQuality();
                    alpha = alpha < minTransparency ? minTransparency : alpha;
                    alpha = alpha > maxTransparency ? maxTransparency : alpha;
                    
                    readcolor = new Color(basecolor.getRed(),basecolor.getGreen(),basecolor.getBlue(),alpha);
                } else if (drawMode.equals(BASE_QUALITY_MODE)) {
                    readcolor = new Color(0,0,0,0);
                } else {
                    
                    if (strand == Strand.FORWARD) {
                        g2.setColor(cs.getColor("Forward Strand"));
                    }
                    else {
                        g2.setColor(cs.getColor("Reverse Strand"));
                    }
                }

                Polygon readshape = renderRead(g2, gp, cs, samRecord, interval, level, range, readcolor);

                this.recordToShapeMap.put(intervalRecord, readshape);

                if (drawMode.equals(BASE_QUALITY_MODE)) {
                    Color col = null;
                    if (strand == Strand.FORWARD) {
                        col = cs.getColor("Forward Strand");
                    }
                    else {
                        col = cs.getColor("Reverse Strand");
                    }
                    renderBaseQualities(g2, gp, samRecord, level, range, col);
                }

                if (drawMode.equals(MISMATCH_MODE)) {
                    // visualize variations (indels and mismatches)
                    renderVariants(g2, gp, samRecord, level, refSeq, range);
                }


                // draw outline, if there's room
                if (readshape.getBounds().getHeight() > 4) {
                    g2.setColor(linecolor);
                    g2.draw(readshape);
                }
            }
        }

    }

    private Polygon renderRead(Graphics2D g2, GraphPane gp, ColorScheme cs, SAMRecord samRecord, Interval interval,
                                 int level, Range range, Color c) {

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
        //y = gp.transformYPos(0) - (level + 1)*unitHeight;
        y = gp.transformYPos(0) - (level + 1)*unitHeight - offset;
        
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
        g2.setColor(c);
        
        g2.fill(pointyBar);

        return pointyBar;

    }

    private void renderBaseQualities(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, Range range, Color baseColor) {
        
        double unitHeight;
        double unitWidth;
        unitHeight = gp.getUnitHeight();
        unitWidth = gp.getUnitWidth();
        
        // cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo()) + unitWidth;

        // visualize variations (indels and mismatches)
        int alignmentStart = samRecord.getAlignmentStart();
        //int alignmentEnd = samRecord.getAlignmentEnd();

        int sequenceCursor = alignmentStart;

        for (byte q : samRecord.getBaseQualities()) {
            int alpha = (int) q;
            alpha = (int) Math.round(((double) alpha/40)*255);
            alpha = alpha < minTransparency ? minTransparency : alpha;
            alpha = alpha > maxTransparency ? maxTransparency : alpha;

            int xCoordinate = (int)gp.transformXPos(sequenceCursor);
             Rectangle2D.Double opRect = new Rectangle2D.Double(xCoordinate,
                                    gp.transformYPos(0)-((level + 1)*unitHeight) - offset,
                                    unitWidth,
                                    unitHeight);
                            g2.setColor(new Color(baseColor.getRed(),baseColor.getGreen(),baseColor.getBlue(),alpha));
                            g2.fill(opRect);
            sequenceCursor++;
        }

    }

    private void renderVariants(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, byte[] refSeq, Range range) {

        ColorScheme cs = (ColorScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor("Line");


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
                opRect = new Rectangle2D.Double(
                        opStart,
                        gp.transformYPos(0)-((level + 1)*unitHeight)-offset,
                        Math.max(opWidth, 1),
                        unitHeight);
                g2.setColor(Color.black);
                g2.fill(opRect);
            }
            // insert
            else if (operator == CigarOperator.I) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.white);
                int xCoordinate = (int)gp.transformXPos(sequenceCursor);
                int yCoordinate = (int)(gp.transformYPos(0)-((level + 1)*unitHeight)) + 1 - offset;
                if((int)unitWidth/3 < 4 || (int)(unitHeight/2) < 6){
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
                        int refIndex = sequenceCursor + i - range.getFromAsInt();

                        if (refIndex < 0) continue;  // outside sequence and drawing range
                        if (refIndex > refSeq.length-1) continue;

                        if (refSeq[refIndex] != readBases[readIndex]) {
                            byte[] readBase = new byte[1];
                            readBase[0] = readBases[readIndex];
                            String base = new String(readBase);
                            Color mismatchColor = null;
                            if (base.equals("A")) {
                                mismatchColor = ColourSettings.getA();
                            }
                            else if (base.equals("C")) {
                                mismatchColor = ColourSettings.getC();
                            }
                            else if (base.equals("G")) {
                                mismatchColor = ColourSettings.getG();
                            }
                            else if (base.equals("T")) {
                                mismatchColor = ColourSettings.getT();
                            }
                            double xCoordinate = gp.transformXPos(sequenceCursor+i);
                            double width = gp.getUnitWidth();
                            if (width < 1) width = 1;
                            opRect = new Rectangle2D.Double(xCoordinate,
                                    gp.transformYPos(0)-((level + 1)*unitHeight) - offset,
                                    unitWidth,
                                    unitHeight);
                            g2.setColor(mismatchColor);
                            g2.fill(opRect);

                            /*
                            if (unitWidth > 10 && unitHeight > 10) {
                            g2.setColor(Color.BLACK);
                                FontMetrics fm   = g2.getFontMetrics(g2.getFont());
                                java.awt.geom.Rectangle2D rect = fm.getStringBounds(base, g2);
                                g2.drawString(base, (float) (xCoordinate + unitWidth/2 - rect.getWidth()/2),  (float) (gp.transformYPos(0)-((level)*unitHeight) - offset - unitHeight/2));
                            }
                             * 
                             */

                        }
                    }
                }
            }
            // skipped
            else if (operator == CigarOperator.N) {
                // draw nothing
                opRect = new Rectangle2D.Double(opStart,
                                                    gp.transformYPos(0)-((level+1)*unitHeight) - offset,
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

    private void renderArcMatepairMode(Graphics2D g2, GraphPane gp) {

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LOG.debug("YMAX for ARC mode: " + ((AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE)).getYMax());
        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        ColorScheme cs = (ColorScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        double threshold = (Double) instructions.get(DrawingInstruction.ARC_MIN);
        int discordantMin = (Integer) instructions.get(DrawingInstruction.DISCORDANT_MIN);
        int discordantMax = (Integer) instructions.get(DrawingInstruction.DISCORDANT_MAX);
        //LOG.info("discordantMin=" + discordantMin + " discordantMax=" + discordantMax);

        // set up colors
        Color normalArcColor = cs.getColor("Reverse Strand");
        Color invertedReadColor = cs.getColor("Inverted Read");
        Color invertedMateColor = cs.getColor("Inverted Mate");
        Color evertedPairColor = cs.getColor("Everted Pair");
        Color discordantLengthColor = cs.getColor("Discordant Length");

        resizeFrame(gp);

        // set graph pane's range parameters
        gp.setIsOrdinal(false);
        gp.setXRange(axisRange.getXRange());
        // Y range is given to us by BAMTrack for this mode
        gp.setYRange(axisRange.getYRange());

        this.clearShapes();

        // iterate through the data and draw
        for (Record record: data) {
            BAMIntervalRecord bamRecord = (BAMIntervalRecord)record;
            SAMRecord samRecord = bamRecord.getSamRecord();
            SAMReadUtils.PairedSequencingProtocol prot = (SAMReadUtils.PairedSequencingProtocol) instructions.get(DrawingInstruction.PAIREDPROTOCOL);
            SAMReadUtils.PairMappingType type = SAMReadUtils.getPairType(samRecord,prot);

            // skip reads with no mapped mate
            if (!samRecord.getReadPairedFlag() || samRecord.getMateUnmappedFlag() || type == null) continue;

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
                } else {
                    // switch the mate start/end for the read start/end to deal with reversed position
                    alignmentStart = mateAlignmentStart;
                    alignmentEnd = mateAlignmentStart + samRecord.getReadLength();
                }
            } else {
                alignmentStart = samRecord.getAlignmentStart();
                alignmentEnd = samRecord.getAlignmentEnd();

            }
            // at this point alignmentStart/End refers the the start end of the first occurrence in the pair


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

            Arc2D.Double arc = new Arc2D.Double(xOrigin, yOrigin, rectWidth, rectHeight, -180, -180, Arc2D.OPEN);
            g2.draw(arc);

            //this.dataShapes.set(i, arc);
            this.recordToShapeMap.put(record, arc);

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

        Genome genome = ReferenceController.getInstance().getGenome();

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor("Line");

        List<Pileup> pileups = new ArrayList<Pileup>();

        // make the pileups
        long length = axisRange.getXMax() - axisRange.getXMin() + 1;
        assert Math.abs(axisRange.getXMin()) <= Integer.MAX_VALUE;
        int startPosition = (int)axisRange.getXMin();
        for (long j = 0; j < length; j++) {
            pileups.add(new Pileup(startPosition + j));
            //pileups.add(new Pileup(trackName, startPosition + i, Pileup.getNucleotide(genome.getRecords(axisRange.getXRange()).charAt(i))));
        }

        // Go through the samrecords and edit the pileups
        for (Record record: data) {
            SAMRecord samRecord = ((BAMIntervalRecord)record).getSamRecord();
            try {
                updatePileupsFromSAMRecord(pileups, genome, samRecord, startPosition);
            } catch (IOException ex) {
                LOG.error("Unable to update pileups.", ex);
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
                byte[] readBase = new byte[1];
                assert Math.abs(p.getPosition() - startPosition) <= Integer.MAX_VALUE;
                readBase[0] = refSeq[(int)(p.getPosition() - startPosition)];
                genomeNucString = new String(readBase);
                //genomeNucString = genome.getRecords(ReferenceController.getInstance().getReferenceName(), new Range((int) p.getPosition(), (int) p.getPosition()));
                genomeNuc = Pileup.stringToNuc(genomeNucString);
                snpNuc = genomeNuc;
            }


            while((genome.isSequenceSet() && p.getCoverage(snpNuc) > 0) || (snpNuc = p.getLargestNucleotide()) != null){

                double x = gp.transformXPos(p.getPosition());
                double coverage = p.getCoverage(snpNuc);

                Color subPileColor = null;
                if(genome.isSequenceSet() && snpNuc.equals(genomeNuc)){
                    subPileColor = cs.getColor("Reverse Strand");
                } else {
                    if(snpNuc.equals(Nucleotide.A)) subPileColor = ColourSettings.getA();
                    else if (snpNuc.equals(Nucleotide.C)) subPileColor = ColourSettings.getC();
                    else if (snpNuc.equals(Nucleotide.G)) subPileColor = ColourSettings.getG();
                    else if (snpNuc.equals(Nucleotide.T)) subPileColor = ColourSettings.getT();
                    //FIXME: what do we do here?
                    else if (snpNuc.equals(Nucleotide.OTHER)) subPileColor = Color.BLACK;
                }

                int h = (int)(unitHeight * coverage);
                int y = bottom-h;

                g2.setColor(subPileColor);
                g2.fillRect((int)x, y, Math.max((int)Math.ceil(unitWidth), 1), h);

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
        //byte[] refSeq = genome.getRecords(new Range(alignmentStart, alignmentEnd)).getBytes();

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
            g2.setColor(ColourSettings.getPointLine());
            g2.setStroke(oneStroke);
            g2.drawString(legendString, x, y);

            y += stringRect.getHeight()+2;

        }
    }

    @Override
    public boolean hasHorizontalGrid() {
        String m = (String)instructions.get(DrawingInstruction.MODE);
        return m.equals(ARC_PAIRED_MODE);
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

            ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);

            // set up colors
            Color normalArcColor = cs.getColor("Reverse Strand");
            Color invertedReadColor = cs.getColor("Inverted Read");
            Color invertedMateColor = cs.getColor("Inverted Mate");
            Color evertedPairColor = cs.getColor("Everted Pair");
            Color discordantLengthColor = cs.getColor("Discordant Length");

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
                g2.setColor(ColourSettings.getPointLine());
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
