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
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.data.event.DataRetrievalEvent;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Genome;
import savant.data.types.Interval;
import savant.data.types.IntervalRecord;
import savant.data.types.Record;
import savant.data.types.Strand;
import savant.exception.RenderingException;
import savant.settings.BrowserSettings;
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
    private static final Log LOG = LogFactory.getLog(BAMTrackRenderer.class);

    private static final Font SMALL_FONT = new Font("Sans-Serif", Font.PLAIN, 10);
    private static final Stroke ONE_STROKE = new BasicStroke(1.0f);
    private static final Stroke TWO_STROKE = new BasicStroke(2.0f);

    private byte[] refSeq = null;
    private DrawingMode lastMode;
    private Resolution lastResolution;

    // The number of standard deviations from the mean an arclength has to be before it's
    // considered discordant
    private static int DISCORDANT_STD_DEV = 3;

    private char getColor(char fromstate, char tostate) {
         switch(fromstate) {
             case 'A':
                 switch(tostate) {
                     case 'A':
                         return '0';
                     case 'C':
                         return '1';
                     case 'T':
                         return '3';
                     case 'G':
                         return '2';
                 }
             case 'C':
                 switch(tostate) {
                     case 'A':
                         return '1';
                     case 'C':
                         return '0';
                     case 'T':
                         return '2';
                     case 'G':
                         return '3';
                 }
             case 'G':
                 switch(tostate) {
                     case 'A':
                         return '2';
                     case 'C':
                         return '3';
                     case 'T':
                         return '1';
                     case 'G':
                         return '0';
                 }
             case 'T':
                 switch(tostate) {
                     case 'A':
                         return '3';
                     case 'C':
                         return '2';
                     case 'T':
                         return '0';
                     case 'G':
                         return '1';
                 }
         }

         //LOG.error("BMATrackRenderer.getColor : impossible state");
         return '4';
     }

    private String translateColorSpaceToLetterSpace(String colors) {
        char[] lettarr = new char[colors.length()-1];

        char currentState = colors.charAt(0);
        for (int i = 1; i < colors.length(); i++) {
            currentState = translateColor(currentState,colors.charAt(i));
            lettarr[i-1] = currentState;
        }

        return new String(lettarr);
    }

    private char translateColor(char letterState, char nextColor) {
        switch(letterState) {
            case 'A':
                switch(nextColor) {
                    case '0':
                        return 'A';
                    case '1':
                        return 'C';
                    case '2':
                        return 'G';
                    case '3':
                        return 'T';
                }
            case 'T':
                switch(nextColor) {
                    case '0':
                        return 'T';
                    case '1':
                        return 'G';
                    case '2':
                        return 'C';
                    case '3':
                        return 'A';
                }
            case 'C':
                switch(nextColor) {
                    case '0':
                        return 'C';
                    case '1':
                        return 'A';
                    case '2':
                        return 'T';
                    case '3':
                        return 'G';
                }
            case 'G':
                switch(nextColor) {
                    case '0':
                        return 'G';
                    case '1':
                        return 'T';
                    case '2':
                        return 'A';
                    case '3':
                        return 'C';
                }
            default:
                LOG.error("Invalid letter state: " + letterState);
                return 'N';
        }
    }

    private String complement(String str) {
        byte[] result = new byte[str.length()];
        for (int i = 0; i < str.length(); i++) {
            result[i] = complement((byte)str.charAt(i));
        }
        return new String(result);
    }

    private byte complement(byte c) {
        switch(c) {
            case 'A':
                return 'T';
            case 'T':
                return 'A';
            case 'C':
                return 'G';
            case 'G':
                return 'C';
            default:
                LOG.error("BAMTrackRenderer.complement : unsupported character " + c);
                return 'N';
        }
    }

    private boolean fontFits(Font f, double width, double height, Graphics2D g2) {
        String baseChar = "G";
        Rectangle2D charRect = f.getStringBounds(baseChar, g2.getFontRenderContext());
        if (charRect.getWidth() > width || charRect.getHeight() > height) return false;
        else return true;
    }

    public BAMTrackRenderer() {
    }

    @Override
    public void dataRetrievalCompleted(DataRetrievalEvent evt) {
        if ((DrawingMode)instructions.get(DrawingInstruction.MODE) == DrawingMode.ARC_PAIRED) {
            int maxDataValue = BAMTrack.getArcYMax(evt.getData());
            Range range = (Range)instructions.get(DrawingInstruction.RANGE);
            addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(0,(int)Math.round(maxDataValue+maxDataValue*0.1))));
        }
        super.dataRetrievalCompleted(evt);
    }

    @Override
    public void render(Graphics2D g2, GraphPane gp) throws RenderingException {

        DrawingMode oldMode = lastMode;
        lastMode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        Resolution r = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        if (r == Resolution.HIGH) {
            if (lastMode == DrawingMode.MISMATCH || lastMode == DrawingMode.SNP || lastMode == DrawingMode.STRAND_SNP || lastMode == DrawingMode.COLOURSPACE || lastMode == DrawingMode.SEQUENCE || lastMode == DrawingMode.BASE_QUALITY || lastMode == DrawingMode.STANDARD_PAIRED) {
                // fetch reference sequence for comparison with cigar string
                Genome genome = GenomeController.getInstance().getGenome();
                if(genome.isSequenceSet()){
                    AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
                    Range range = axisRange.getXRange();
                    try {
                        refSeq = genome.getSequence(LocationController.getInstance().getReferenceName(), range);
                    } catch (IOException e) {
                        throw new RenderingException(e.getMessage(), 2);
                    }
                }
            }
        }
        if (lastMode != DrawingMode.ARC_PAIRED) {
            // For non-arc modes, we want to establish our interval height when we switch from coverage to non-coverage.
            if (lastResolution != r || oldMode == DrawingMode.ARC_PAIRED) {
                if (r == Resolution.HIGH) {
                    // We're switching from coverage (or arc mode) to high resolution.  The initial interval height
                    // will be taken from the slider.
                    gp.getParentFrame().setHeightFromSlider();
                } else {
                    // We're switching to coverage.  Initially scaled by default.
                    gp.setScaledToFit(true);
                }
                lastResolution = r;                
            }
        } else {
            // Arc mode is always initially scaled to fit.
            if (oldMode != DrawingMode.ARC_PAIRED) {
                gp.setScaledToFit(true);
            }
        }

        // Put up an error message if we don't want to do any rendering.
        // The pre-check is here so that necessary calls to setScaledToFit() have a chance to happen.
        renderPreCheck();

        switch (lastMode) {
            case STANDARD:
            case MISMATCH:
            case SEQUENCE:
            case MAPPING_QUALITY:
            case BASE_QUALITY:
            case COLOURSPACE:
                if (r == Resolution.HIGH) {
                    renderPackMode(g2, gp, r);
                }
                break;
            case STANDARD_PAIRED:
                if (r == Resolution.HIGH) {
                    renderStandardPairedMode(g2, gp);
                }
                break;
            case ARC_PAIRED:
                renderArcPairedMode(g2, gp);
                break;
            case SNP:
                if (r == Resolution.HIGH) {
                    renderSNPMode(g2, gp, r);
                }
                break;
            case STRAND_SNP:
                if (r == Resolution.HIGH) {
                    renderStrandSNPMode(g2, gp, r);
                }
                break;
        }
        if (data.isEmpty()){
            throw new RenderingException("No data in range", 1);
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution r) throws RenderingException {

        if (lastMode == DrawingMode.MISMATCH) {
            Genome genome = GenomeController.getInstance().getGenome();
            if (!genome.isSequenceSet()) {
                throw new RenderingException("No reference sequence loaded. Switch to standard view", 2);
            }
        }

        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        Range range = axisRange.getXRange();

        //calculate breathing room parameter
        double arrowWidth = gp.getUnitHeight() * 0.25;
        double pixelsPerBase = Math.max(0.01, (double)gp.getWidth() / (double)range.getLength());
        int breathingRoom = (int)Math.ceil(2 * (arrowWidth / pixelsPerBase) + 2);

        // TODO: when it becomes possible, choose an appropriate number for breathing room parameter
        List<List<IntervalRecord>> intervals = new IntervalPacker(data).pack(breathingRoom);

        gp.setXRange(range);
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
        
        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor(ColourKey.INTERVAL_LINE);

        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

            List<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                Interval interval = intervalRecord.getInterval();

                BAMIntervalRecord bamRecord = (BAMIntervalRecord) intervalRecord;
                SAMRecord samRecord = bamRecord.getSamRecord();

                if (samRecord.getReadUnmappedFlag()) { // this read is unmapped, don't visualize it

                    this.recordToShapeMap.put(intervalRecord, null);

                    continue;
                }

                Color readColour = null;

                boolean strandFlag = samRecord.getReadNegativeStrandFlag();
                Strand strand = strandFlag ? Strand.REVERSE : Strand.FORWARD ;

                if (lastMode == DrawingMode.MAPPING_QUALITY) {
                    Color basecolor = cs.getColor(strand == Strand.FORWARD ? ColourKey.FORWARD_STRAND : ColourKey.REVERSE_STRAND);

                    int alpha = getConstrainedAlpha(samRecord.getMappingQuality());
                    readColour = new Color(basecolor.getRed(),basecolor.getGreen(),basecolor.getBlue(),alpha);
                } else if (lastMode == DrawingMode.BASE_QUALITY) {
                    readColour = new Color(0,0,0,0);
                } else {
                    g2.setColor(cs.getColor(strand == Strand.FORWARD ? ColourKey.FORWARD_STRAND : ColourKey.REVERSE_STRAND));
                }

                Color override = ((BAMIntervalRecord)intervalRecord).getColor();
                if (override != null){
                    readColour = override;
                }
                
                Polygon readshape = renderRead(g2, gp, samRecord, interval, level, range, readColour);

                this.recordToShapeMap.put(intervalRecord, readshape);

                if (lastMode == DrawingMode.BASE_QUALITY) {
                    renderBQMismatches(g2, gp, samRecord, level, refSeq, range);
                }

                if (lastMode == DrawingMode.MISMATCH) {
                    // visualize variations (indels and mismatches)
                    renderMismatches(g2, gp, samRecord, level, refSeq, range);
                } else if (lastMode == DrawingMode.SEQUENCE) {
                    renderLetters(g2, gp, samRecord, level, refSeq, range);
                } else if (lastMode == DrawingMode.COLOURSPACE) {
                    renderColorMismatches(g2, gp, samRecord, level, refSeq, range);
                }

                // draw outline, if there's room
                if (readshape.getBounds().getHeight() > 4) {
                    g2.setColor(linecolor);
                    g2.draw(readshape);
                }
            }
        }

    }

    public Polygon renderRead(Graphics2D g2, GraphPane gp, SAMRecord samRecord, Interval interval, int level, Range range, Color c) {
        return renderRead(g2, gp, samRecord, interval, level, range, c, -1);
    }
        
    public Polygon renderRead(Graphics2D g2, GraphPane gp, SAMRecord samRecord, Interval interval, int level, Range range, Color c, double forcedHeight) {

        double x=0;
        double y=0;
        double w=0;
        double h=0;

        double unitHeight;
        if(forcedHeight > 0){
            unitHeight = forcedHeight;
        } else {
            unitHeight = gp.getUnitHeight();
        }

        //unitHeight = intervalHeight;
        double arrowHeight = unitHeight/2;
        double arrowWidth = unitHeight/4;

        // cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo() + 1);

        boolean drawPoint = false;
        //y = gp.transformYPos(0) - (level + 1)*unitHeight;
        y = gp.transformYPos(0) - (level + 1)*unitHeight - gp.getOffset();
        
        w = gp.getWidth(interval.getLength());

        //if (w < 1) {
        //    return null; // don't draw intervals less than one pixel wide
        //}
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

    private void renderColorMismatches(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, byte[] refSeq, Range range) {


        // colours
        String colors = samRecord.getStringAttribute("CS");
        String readseq = samRecord.getReadString();
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);

        boolean revstrand = samRecord.getReadNegativeStrandFlag();

        if (samRecord.getReadNegativeStrandFlag()) {
            readseq = MiscUtils.reverseString(readseq);
        }

        String modreadseq;
        
        if (revstrand) {
            modreadseq = colors.charAt(0) + complement(readseq.trim());
        } else {
            modreadseq = colors.charAt(0) + readseq.trim();
        }


        LOG.debug(samRecord.getReadName());
        LOG.debug(colors);
        LOG.debug(modreadseq);
        LOG.debug(refSeq);


        if (colors == null || colors.equals("")) { return; }

        double unitHeight = gp.getUnitHeight();
        double unitWidth = gp.getUnitWidth();
        int offset = gp.getOffset();
        int xCoordinate;
        int yCoordinate;
        int cursor = 0;
        int deladvance = 0;
        int insadvance = 0;
        int clipOffset = 0;
        Cigar cigar = samRecord.getCigar();
        CigarOperator operator;
        int operatorLength;

        char currentState = colors.charAt(0);
        char nextState;

        colors = colors.substring(1);

        List<CigarElement> cigars = cigar.getCigarElements();
        List<CigarElement> cigsclone = new ArrayList<CigarElement>();
        cigsclone.addAll(cigars);
        if (revstrand) {
            Collections.reverse(cigsclone);
        }

        for (CigarElement cigarElement : cigsclone) {
            operatorLength = cigarElement.getLength();
            operator = cigarElement.getOperator();

            if (operator == CigarOperator.H) {
                clipOffset += operatorLength;
            }

            //System.out.println("OP=" + operator + "\tL=" + operatorLength + "\tRD=" + operator.consumesReadBases() + "\tRF=" + operator.consumesReferenceBases());
            if (operator.consumesReadBases()) {
                if (operator == CigarOperator.I) {

                    cursor += operatorLength;
                    insadvance += operatorLength;

                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.white);
                    if (revstrand) {
                        xCoordinate = (int)gp.transformXPos(samRecord.getAlignmentEnd() - cursor + operatorLength + 1);
                    } else {
                        xCoordinate = (int)gp.transformXPos(samRecord.getAlignmentStart() + cursor);
                    }
                    yCoordinate = (int)(gp.transformYPos(0)-((level + 1)*unitHeight)) + 1 - offset;
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
                            g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
                            g2.drawLine(xCoordinate, (int)yCoordinate, xCoordinate, (int)(yCoordinate+unitHeight)-1);
                        }
                    }

                    
                } else {

                    for (int i = cursor;(i < cursor + operatorLength); i++) {
                        int askForIndex = i - deladvance + insadvance;

                        if (revstrand) {
                            xCoordinate = (int) gp.transformXPos(samRecord.getAlignmentEnd() - i - deladvance + insadvance);
                        } else {
                            xCoordinate = (int) gp.transformXPos(samRecord.getAlignmentStart() + i + deladvance - insadvance);
                        }
                        yCoordinate = (int) (gp.transformYPos(0)-((level)*unitHeight) - offset);

                        if (askForIndex >= 0 && (
                                (revstrand && ((samRecord.getAlignmentEnd() - askForIndex + 2*insadvance ) >= range.getFrom() - 1))
                                || (!revstrand && ((askForIndex + samRecord.getAlignmentStart() - 2*insadvance) <= range.getTo() + 1))
                                )) {

                            if (getColor(modreadseq.charAt(i),modreadseq.charAt(i+1)) != colors.charAt(i + clipOffset)) {

                                Rectangle.Double rect = new Rectangle.Double(xCoordinate, yCoordinate - unitHeight, unitWidth, unitHeight);
                                Color fillcol = cs.getBaseColor(translateColor(modreadseq.charAt(i), colors.charAt(i + clipOffset)));
                                g2.setPaint(fillcol != null ? fillcol : cs.getColor(ColourKey.DELETED_BASE));
                                g2.fill(rect);

                                if (unitWidth > 10 && unitHeight > 10) {
                                    g2.setPaint(Color.black);
                                    if (revstrand) {
                                        g2.drawString("" + colors.charAt(i+clipOffset), xCoordinate + (int) unitWidth - 3, yCoordinate - (int) (unitHeight/2) + 3);
                                    } else {
                                        g2.drawString("" + colors.charAt(i+clipOffset), xCoordinate - 3, yCoordinate - (int) (unitHeight/2) + 4);
                                    }
                                }
                            }

                            if (unitWidth > 10 && unitHeight > 10) {
                                    g2.setPaint(Color.black);
                                    if (revstrand) {
                                        g2.drawString("" + colors.charAt(i), xCoordinate + (int) unitWidth - 3, yCoordinate - (int) (unitHeight/2) + 3);
                                    } else {
                                        g2.drawString("" + colors.charAt(i), xCoordinate - 3, yCoordinate - (int) (unitHeight/2) + 4);
                                    }
                                }
                        }
                    }
                    cursor += operatorLength;
                }
            } else if (operator.consumesReferenceBases()) {
                deladvance += operatorLength;
                if (operator == CigarOperator.D) {
                    if (revstrand) {
                        xCoordinate = (int) gp.transformXPos(samRecord.getAlignmentEnd() - cursor - operatorLength + 1);
                    } else {
                        xCoordinate = (int) gp.transformXPos(samRecord.getAlignmentStart() + cursor);
                    }
                        yCoordinate = (int) (gp.transformYPos(0)-((level)*unitHeight) - offset);
                        Rectangle.Double rect = new Rectangle.Double(xCoordinate, yCoordinate - unitHeight, unitWidth*operatorLength, unitHeight);
                        g2.setColor(Color.black);
                        g2.fill(rect);
                }
            }
        }
    }

    private ColourKey getSubPileColour(Nucleotide snpNuc, Nucleotide genomeNuc) {
        if (snpNuc.equals(genomeNuc)){
            return ColourKey.REVERSE_STRAND;
        } else {
            if (snpNuc.equals(Nucleotide.A)) {
                return ColourKey.A;
            } else if (snpNuc.equals(Nucleotide.C)) {
                return ColourKey.C;
            } else if (snpNuc.equals(Nucleotide.G)) {
                return ColourKey.G;
            } else if (snpNuc.equals(Nucleotide.T)) {
                return ColourKey.T;
            } else {
                return null;
            }
        }
    }

    private void renderLetters(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, byte[] refSeq, Range range) {

        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor(ColourKey.INTERVAL_LINE);

        double unitHeight = gp.getUnitHeight();
        double unitWidth = gp.getUnitWidth();
        int offset = gp.getOffset();

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

            if (operator == CigarOperator.D) {
                // Deletion
                double width = gp.getWidth(operatorLength);
                if (width < 1) width = 1;
                opRect = new Rectangle2D.Double(opStart, gp.transformYPos(0)-((level + 1)*unitHeight)-offset, Math.max(opWidth, 1), unitHeight);
                g2.setColor(Color.black);
                g2.fill(opRect);

            } else if (operator == CigarOperator.I) {
                // Insertion
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.white);
                double xCoord = gp.transformXPos(sequenceCursor);
                double yCoord = gp.transformYPos(0) - ((level + 1) * unitHeight) + 1 - offset;
                if ((int)unitWidth/3 < 4 || (int)(unitHeight/2) < 6){
                    yCoord--;
                    double lineWidth = Math.max((int)(unitWidth * (2.0/3.0)), 1);
                    double xCoord1 = opStart - Math.floor(lineWidth/2);
                    double xCoord2 = opStart - Math.floor(lineWidth/2) + lineWidth - 1;
                    if (xCoord1 == xCoord2) xCoord2++;
                    g2.fill(MiscUtils.createPolygon(xCoord1, yCoord, xCoord2, yCoord, xCoord2, yCoord + unitHeight, xCoord1, yCoord + unitHeight));
                } else {
                    g2.fill(MiscUtils.createPolygon(xCoord, yCoord, xCoord + unitWidth / 3, yCoord + unitWidth * 0.5, xCoord, yCoord + unitHeight - 1.0, xCoord - unitWidth / 3, yCoord + unitHeight * 0.5));
                    if (unitWidth >= 21.0 && unitHeight >= 10.0) {
                        g2.setColor(linecolor);
                        g2.draw(new Line2D.Double(xCoord, yCoord, xCoord, yCoord + unitHeight - 1.0));
                    }
                }
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            } else if (operator == CigarOperator.M) {
                // match or mismatch

                // some SAM files do not contain the read bases
                if (sequenceSaved) {
                    // determine if there's a mismatch
                    for (int i=0; i<operatorLength; i++) {
                        // indices into refSeq and readBases associated with this position in the cigar string
                        int readIndex = readCursor-alignmentStart+i;
                        int refIndex = sequenceCursor + i - range.getFrom();

                        if (refIndex < 0 || (refSeq != null && refIndex >= refSeq.length)) {
                            // Outside sequence and drawing range.
                            continue;
                        }
                        Color baseColor = cs.getBaseColor((char)readBases[readIndex]);
                        if (baseColor != null) {
                            double xCoordinate = gp.transformXPos(sequenceCursor+i);
                            opRect = new Rectangle2D.Double(xCoordinate, gp.transformYPos(0)-((level + 1)*unitHeight) - offset, unitWidth, unitHeight);
                            g2.setColor(baseColor);
                            g2.fill(opRect);
                        }
                    }
                }
            } else if (operator == CigarOperator.N) {
                // skipped
                // draw nothing
                opRect = new Rectangle2D.Double(opStart, gp.transformYPos(0)-((level+1)*unitHeight) - offset, opWidth, unitHeight);
                g2.setColor(Color.gray);
                g2.fill(opRect);

            } else if (operator == CigarOperator.P) {
                // padding
                // draw nothing
            } else if (operator == CigarOperator.H) {
                // hard clip
                // draw nothing
            } else if (operator == CigarOperator.S) {
                // soft clip
                // draw nothing
            }
            if (operator.consumesReadBases()) readCursor += operatorLength;
            if (operator.consumesReferenceBases()) sequenceCursor += operatorLength;
        }

    }
    
    public void renderMismatches(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, byte[] refSeq, Range range) {
        renderMismatches(g2, gp, samRecord, level, refSeq, range, -1);
    }

    public void renderMismatches(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, byte[] refSeq, Range range, double forcedHeight) {

        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor(ColourKey.INTERVAL_LINE);

        double unitHeight = gp.getUnitHeight();
        if(forcedHeight > 0){
            unitHeight = forcedHeight;
        }

        double unitWidth = gp.getUnitWidth();
        int offset = gp.getOffset();

        // cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo()) + unitWidth;

        // visualize variations (indels and mismatches)
        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();

        byte[] readBases = samRecord.getReadBases();
        byte[] baseQualities = samRecord.getBaseQualities();
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

            if (operator == CigarOperator.D) {
                // Deletion
                double width = gp.getWidth(operatorLength);
                if (width < 1) width = 1;
                opRect = new Rectangle2D.Double(
                        opStart,
                        gp.transformYPos(0)-((level + 1)*unitHeight)-offset,
                        Math.max(opWidth, 1),
                        unitHeight);
                g2.setColor(Color.black);
                g2.fill(opRect);
            } else if (operator == CigarOperator.I) {
                // Insertion
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
            } else if (operator == CigarOperator.M) {
                // Match or mismatch

                // Some SAM files do not contain the read bases.
                if (sequenceSaved) {
                    // determine if there's a mismatch
                    for (int i=0; i<operatorLength; i++) {
                        // indices into refSeq and readBases associated with this position in the cigar string
                        int readIndex = readCursor-alignmentStart+i;
                        int refIndex = sequenceCursor + i - range.getFrom();

                        if (refIndex < 0) continue;  // outside sequence and drawing range
                        if (refIndex > refSeq.length-1) continue;

                        if (refSeq[refIndex] != readBases[readIndex]) {

                            Color mismatchColor = cs.getBaseColor((char)readBases[readIndex]);

                            double xCoordinate = gp.transformXPos(sequenceCursor+i);
                            double width = gp.getUnitWidth();
                            if (width < 1) width = 1;
                            opRect = new Rectangle2D.Double(xCoordinate, gp.transformYPos(0)-((level + 1)*unitHeight) - offset, unitWidth, unitHeight);
                            if (mismatchColor != null) {
                                g2.setColor(mismatchColor);
                                g2.fill(opRect);
                            }

                            if (fontFits(BrowserSettings.getTrackFont(),unitWidth,unitHeight,g2)) {
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2.setColor(new Color(10,10,10));
                                g2.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
                                g2.drawBytes(readBases, readIndex, 1, (int) (xCoordinate + unitWidth/2 - g2.getFontMetrics().bytesWidth(readBases, readIndex, 1)/2), (int) ((gp.transformYPos(0)-((level)*unitHeight))-unitHeight/2+g2.getFontMetrics().getMaxAscent()/2));
                            }
                        }
                    }
                }
            } else if (operator == CigarOperator.N) {
                // Skipped.  Draw nothing
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


    private void renderBQMismatches(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, byte[] refSeq, Range range) {

        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor(ColourKey.INTERVAL_LINE);

        double unitHeight = gp.getUnitHeight();
        double unitWidth = gp.getUnitWidth();
        int offset = gp.getOffset();

        // cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo()) + unitWidth;

        // visualize variations (indels and mismatches)
        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();

        byte[] readBases = samRecord.getReadBases();
        byte[] baseQualities = samRecord.getBaseQualities();
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
                opRect = new Rectangle2D.Double( opStart, gp.transformYPos(0)-((level + 1)*unitHeight)-offset, Math.max(opWidth, 1), unitHeight);
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
                        int refIndex = sequenceCursor + i - range.getFrom();

                        if (refIndex >= 0 && (refSeq == null || refIndex < refSeq.length)) {

                            Color baseColor = null;
                            boolean isMismatch = refSeq != null && refSeq[refIndex] != readBases[readIndex];

                            if (isMismatch) {
                                baseColor = cs.getBaseColor((char)readBases[readIndex]);
                            } else {
                                baseColor = cs.getColor(samRecord.getReadNegativeStrandFlag() ? ColourKey.REVERSE_STRAND : ColourKey.FORWARD_STRAND);
                            }


                            byte q = baseQualities[readIndex];
                            int alpha = getConstrainedAlpha((int)Math.round((q * 0.025) * 255));
                            baseColor = new Color(baseColor.getRed(),baseColor.getGreen(),baseColor.getBlue(),alpha);

                            double xCoordinate = gp.transformXPos(sequenceCursor+i);
                            double width = gp.getUnitWidth();
                            if (width < 1) width = 1;
                            opRect = new Rectangle2D.Double(xCoordinate, gp.transformYPos(0)-((level + 1)*unitHeight) - offset, unitWidth, unitHeight);
                            g2.setColor(baseColor);
                            g2.fill(opRect);

                            if (isMismatch && fontFits(BrowserSettings.getTrackFont(),unitWidth,unitHeight,g2)) {
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2.setColor(new Color(10,10,10));
                                g2.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
                                g2.drawBytes(readBases, readIndex, 1, (int) (xCoordinate + unitWidth/2 - g2.getFontMetrics().bytesWidth(readBases, readIndex, 1)/2), (int) ((gp.transformYPos(0)-((level)*unitHeight))-unitHeight/2+g2.getFontMetrics().getMaxAscent()/2));
                            }
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
   
    private void renderArcPairedMode(Graphics2D g2, GraphPane gp) {

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LOG.debug("YMAX for ARC mode: " + ((AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE)).getYMax());
        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        double threshold = (Double) instructions.get(DrawingInstruction.ARC_MIN);
        int discordantMin = (Integer) instructions.get(DrawingInstruction.DISCORDANT_MIN);
        int discordantMax = (Integer) instructions.get(DrawingInstruction.DISCORDANT_MAX);
        //LOG.info("discordantMin=" + discordantMin + " discordantMax=" + discordantMax);

        // set up colors
        Color normalArcColor = cs.getColor(ColourKey.CONCORDANT_LENGTH);
        Color invertedReadColor = cs.getColor(ColourKey.ONE_READ_INVERTED);
        Color evertedPairColor = cs.getColor(ColourKey.EVERTED_PAIR);
        Color discordantLengthColor = cs.getColor(ColourKey.DISCORDANT_LENGTH);

        // set graph pane's range parameters
        gp.setXRange(axisRange.getXRange());
        gp.setYRange(axisRange.getYRange());

        // iterate through the data and draw
        LOG.info("BAMTrackRenderer.renderArcMatePairMode: " + data.size() + " records.");
        for (Record record: data) {
            BAMIntervalRecord bamRecord = (BAMIntervalRecord)record;
            SAMRecord samRecord = bamRecord.getSamRecord();
            SAMReadUtils.PairedSequencingProtocol prot = (SAMReadUtils.PairedSequencingProtocol) instructions.get(DrawingInstruction.PAIRED_PROTOCOL);
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
                if (!(mateAlignmentStart < LocationController.getInstance().getRangeStart())) {
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
                case INVERTED_MATE:
                    intervalStart = alignmentStart;
                    g2.setColor(invertedReadColor);
                    g2.setStroke(TWO_STROKE);
                    break;
                case EVERTED:
                    intervalStart = alignmentStart;
                    g2.setColor(evertedPairColor);
                    g2.setStroke(TWO_STROKE);
                    break;
                default:
                    // make sure arclength is over our threshold
                    /*if (threshold != 0.0d && threshold < 1.0d && arcLength < axisRange.getXRange().getLength()*threshold) {
                        continue;
                    }
                    else if (threshold > 1.0d && arcLength < threshold) {
                        continue;
                    }*/
                    
                    intervalStart = alignmentStart;

                    if (arcLength > discordantMax || arcLength < discordantMin) {
                        g2.setColor(discordantLengthColor);
                        g2.setStroke(TWO_STROKE);
                    }
                    else {
                        g2.setColor(normalArcColor);
                        g2.setStroke(ONE_STROKE);
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
            recordToShapeMap.put(record, arc);
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

        Genome genome = GenomeController.getInstance().getGenome();

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);

        List<Pileup> pileups = new ArrayList<Pileup>();

        // make the pileups
        int length = axisRange.getXMax() - axisRange.getXMin() + 1;
        assert Math.abs(axisRange.getXMin()) <= Integer.MAX_VALUE;
        int startPosition = (int)axisRange.getXMin();
        for (int j = 0; j < length; j++) {
            pileups.add(new Pileup(startPosition + j));
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

        double maxHeight = 0;
        for(Pileup p : pileups){
            double current = p.getTotalCoverage();
            if(current > maxHeight) maxHeight = current;
        }

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
            
            
            while ((genome.isSequenceSet() && (snpNuc = p.getLargestNucleotide(genomeNuc)) != null) || ((snpNuc = p.getLargestNucleotide()) != null)){
                double x = gp.transformXPos(p.getPosition());
                double coverage = p.getCoverage(snpNuc);

                Color subPileColor = cs.getColor(getSubPileColour(snpNuc, genomeNuc));

                int h = (int)(unitHeight * coverage);
                int y = bottom-h;

                g2.setColor(subPileColor);
                g2.fillRect((int)x, y, Math.max((int)Math.ceil(unitWidth), 1), h);

                bottom -= h;
                p.clearNucleotide(snpNuc);
            }
        }
    }
    
    private void renderStrandSNPMode(Graphics2D g2, GraphPane gp, Resolution r){

        Genome genome = GenomeController.getInstance().getGenome();

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);

        List<Pileup> pileups = new ArrayList<Pileup>();

        // make the pileups
        int length = axisRange.getXMax() - axisRange.getXMin() + 1;
        assert Math.abs(axisRange.getXMin()) <= Integer.MAX_VALUE;
        int startPosition = (int)axisRange.getXMin();
        for (int j = 0; j < length; j++) {
            pileups.add(new Pileup(startPosition + j));
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

        double maxHeight = 0;
        for(Pileup p : pileups){
            double current1 = p.getTotalStrandCoverage(true);
            double current2 = p.getTotalStrandCoverage(false);
            if(current1 > maxHeight) maxHeight = current1;
            if(current2 > maxHeight) maxHeight = current2;
        }

        gp.setXRange(axisRange.getXRange());
        gp.setYRange(new Range(0,(int)Math.rint((double)maxHeight/0.9)));

        double unitHeight = (Math.rint(gp.transformYPos(0) * 0.45)) / maxHeight;
        double unitWidth =  gp.getUnitWidth();

        for(Pileup p : pileups){

            Nucleotide snpNuc = null;
            int bottom = (int)gp.transformYPos(0) / 2;
            int top = (int)gp.transformYPos(0) / 2;

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


            while ((genome.isSequenceSet() && (snpNuc = p.getLargestNucleotide(genomeNuc)) != null) || ((snpNuc = p.getLargestNucleotide()) != null)){
            
                double x = gp.transformXPos(p.getPosition());
                double coverage1 = p.getStrandCoverage(snpNuc, true);
                double coverage2 = p.getStrandCoverage(snpNuc, false);

                Color subPileColor = cs.getColor(getSubPileColour(snpNuc, genomeNuc));

                if (coverage1 > 0){
                    int h = (int)(unitHeight * coverage1);
                    int y = top;

                    if (genome.isSequenceSet() && snpNuc.equals(genomeNuc)) {
                        g2.setColor(cs.getColor(ColourKey.REVERSE_STRAND));
                    } else {
                        g2.setColor(subPileColor);
                    }
                    g2.fillRect((int)x, y, Math.max((int)Math.ceil(unitWidth), 1), h);

                    top += h;
                }
                if (coverage2 > 0){
                    int h = (int)(unitHeight * coverage2);
                    int y = bottom-h;

                    if (genome.isSequenceSet() && snpNuc.equals(genomeNuc)) {
                        g2.setColor(cs.getColor(ColourKey.FORWARD_STRAND));
                    } else {
                        g2.setColor(subPileColor);
                    }
                    g2.fillRect((int)x, y, Math.max((int)Math.ceil(unitWidth), 1), h);

                    bottom -= h;
                }
                            
                p.clearNucleotide(snpNuc);
            }
        }
        
        g2.setColor(Color.BLACK);
        g2.drawLine(0, (int)gp.transformYPos(0) / 2, gp.getWidth(), (int)gp.transformYPos(0) / 2);
    }

    private void updatePileupsFromSAMRecord(List<Pileup> pileups, Genome genome, SAMRecord samRecord, int startPosition) throws IOException {
        
        boolean strand = samRecord.getReadNegativeStrandFlag();
        
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

                        int j = i + (int) (sequenceCursor - startPosition);
                        //for (int j = pileupcursor; j < operatorLength; j++) {
                        if (j >= 0 && j < pileups.size()) {
                            Pileup p = pileups.get(j);
                            p.pileOn(readN, strand);
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
    
    public void renderReadsFromArc(Graphics2D g2, GraphPane gp, BAMIntervalRecord rec1, BAMIntervalRecord rec2, Range range){
        
        ColourScheme cs = (ColourScheme) getInstruction(DrawingInstruction.COLOR_SCHEME);
        boolean strandFlag = rec1.getSamRecord().getReadNegativeStrandFlag();
        Strand strand = strandFlag ? Strand.REVERSE : Strand.FORWARD ;             
        Color c1 = null;
        Color c2 = null;
        if (strand == Strand.FORWARD) {
            c1 = cs.getColor(ColourKey.FORWARD_STRAND);
            c2 = cs.getColor(ColourKey.REVERSE_STRAND);
        } else {
            c1 = cs.getColor(ColourKey.REVERSE_STRAND);
            c2 = cs.getColor(ColourKey.FORWARD_STRAND);
        }
        
        int readHeight = gp.getParentFrame().getIntervalHeight();

        Polygon p1 = renderRead(g2, gp, rec1.getSamRecord(), rec1.getInterval(), 0, range, c1, readHeight);
        g2.setColor(Color.BLACK);
        g2.draw(p1);

        if(rec2 != null){
            Polygon p2 = renderRead(g2, gp, rec2.getSamRecord(), rec2.getInterval(), 0, range, c2, readHeight);
            g2.setColor(Color.BLACK);
            g2.draw(p2);
        }

        Genome genome = GenomeController.getInstance().getGenome();
        if(genome.isSequenceSet()){
            byte[] sequence = null;
            try {
                sequence = genome.getSequence(LocationController.getInstance().getReferenceName(), range);
            } catch (IOException e) {
                //do nothing
            }
            renderMismatches(g2, gp, rec1.getSamRecord(), 0, sequence, range, readHeight);
            if(rec2 != null)
                renderMismatches(g2, gp, rec2.getSamRecord(), 0, sequence, range, readHeight);
        }
        
    }

    private void renderStandardPairedMode(Graphics2D g2, GraphPane gp) throws RenderingException {

        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor(ColourKey.INTERVAL_LINE);
        Range range = axisRange.getXRange();

        gp.setXRange(axisRange.getXRange());

        Map<Integer, ArrayList<IntervalRecord>> mateQueue = new HashMap<Integer, ArrayList<IntervalRecord>>();
        ArrayList<ArrayList<Interval>> levels = new ArrayList<ArrayList<Interval>>();
        levels.add(new ArrayList<Interval>()); 
        ArrayList<DrawStore> savedDraws = new ArrayList<DrawStore>();

        for(int i = 0; i < data.size(); i++){

            IntervalRecord intervalRecord = (IntervalRecord)data.get(i);
            Interval interval = intervalRecord.getInterval();
            BAMIntervalRecord bamRecord = (BAMIntervalRecord) intervalRecord;
            SAMRecord samRecord = bamRecord.getSamRecord();
            SAMReadUtils.PairedSequencingProtocol prot = (SAMReadUtils.PairedSequencingProtocol) instructions.get(DrawingInstruction.PAIRED_PROTOCOL);
            SAMReadUtils.PairMappingType type = SAMReadUtils.getPairType(samRecord,prot);
            int arcLength = Math.abs(samRecord.getInferredInsertSize());

            //discard unmapped reads
            if (samRecord.getReadUnmappedFlag() || !samRecord.getReadPairedFlag() 
                    || samRecord.getMateUnmappedFlag() || type == null
                    || arcLength == 0) { // this read is unmapped, don't visualize it
                this.recordToShapeMap.put(intervalRecord, null);
                continue;
            }

            //if mate off screen to the right, draw immediately
            if(samRecord.getMateAlignmentStart() > range.getTo()){
                int level = computePiledIntervalLevel(levels, Interval.valueOf(interval.getStart(), range.getTo()));
                savedDraws.add(new DrawStore(intervalRecord, samRecord, level, Interval.valueOf(range.getTo(), range.getTo()), null));
                continue;
            }

            //check if mate has already been found
            IntervalRecord mate = popMate(mateQueue.get(samRecord.getMateAlignmentStart()), samRecord);
            if(mate != null){
                int level = computePiledIntervalLevel(levels, 
                        Interval.valueOf(Math.min(interval.getStart(), mate.getInterval().getStart()),
                        Math.max(interval.getEnd(), mate.getInterval().getEnd())));
                savedDraws.add(new DrawStore(intervalRecord, samRecord, level, null, null));
                savedDraws.add(new DrawStore(mate, ((BAMIntervalRecord)mate).getSamRecord(), level, interval, intervalRecord));


                continue;
            }

            //if mate not yet found, add to map queue
            if(mateQueue.get(interval.getStart()) == null){
                mateQueue.put(interval.getStart(), new ArrayList<IntervalRecord>());
            }
            mateQueue.get(interval.getStart()).add(intervalRecord);          
        }

        //if there are records remaining without a mate, they are probably off screen to the left
        Iterator<ArrayList<IntervalRecord>> it = mateQueue.values().iterator();
        while(it.hasNext()){
            ArrayList<IntervalRecord> list = it.next();
            for (IntervalRecord intervalRecord : list) {
                Interval interval = intervalRecord.getInterval();
                BAMIntervalRecord bamRecord = (BAMIntervalRecord) intervalRecord;
                SAMRecord samRecord = bamRecord.getSamRecord();
                int level = computePiledIntervalLevel(levels, Interval.valueOf(range.getFrom(), interval.getEnd()));
                savedDraws.add(new DrawStore(intervalRecord, samRecord, level, Interval.valueOf(range.getFrom(), range.getFrom()), null));
            }
        }

        //resize frame if necessary
        gp.setYRange(new Range(0,levels.size()+2));
        if (gp.needsToResize()) return;

        //now, draw everything
        for(DrawStore drawStore : savedDraws){
            drawPiledInterval(g2, gp, cs, range, linecolor, drawStore);
            renderMismatches(g2, gp, drawStore.sam, drawStore.level, refSeq, range);
            if(drawStore.mateInterval != null){
                connectPiledInterval(g2, gp, drawStore.interval, drawStore.mateInterval, drawStore.level, linecolor, drawStore.intervalRecord, drawStore.mateIntervalRecord);
            }
        }
    }

    /*
     * Find the mate for record with name readName in the list records.
     * If mate found, remove and return. Otherwise, return null.
     */
    private IntervalRecord popMate(ArrayList<IntervalRecord> records, SAMRecord samRecord){

        if(records == null) return null;
        for(int i = 0; i < records.size(); i++){
            if(MiscUtils.isMate(samRecord, ((BAMIntervalRecord)records.get(i)).getSamRecord())){
                IntervalRecord intervalRecord = records.get(i);
                records.remove(i);
                return intervalRecord;
            }
        }
        return null;
    }

    private void drawPiledInterval(Graphics2D g2, GraphPane gp, ColourScheme cs,
                                  Range range, Color linecolor, DrawStore drawStore){
        if(drawStore.sam.getReadNegativeStrandFlag()){
            g2.setColor(cs.getColor(ColourKey.REVERSE_STRAND));
        } else {
            g2.setColor(cs.getColor(ColourKey.FORWARD_STRAND));
        }
        
        Polygon readshape = renderRead(g2, gp, drawStore.sam, drawStore.interval, drawStore.level, range, null);

        this.recordToShapeMap.put(drawStore.intervalRecord, readshape);

        // draw outline, if there's room
        if (readshape.getBounds().getHeight() > 4) {
            g2.setColor(linecolor);
            g2.draw(readshape);
        }
    }

    /*
     * Connect intervals i1 and i2 with a dashed line to show mates. 
     */
    private void connectPiledInterval(Graphics2D g2, GraphPane gp, Interval i1, Interval i2, int level, Color linecolor, IntervalRecord ir1, IntervalRecord ir2){

        Interval mateInterval = computeMateInterval(i1, i2);

        Stroke currentStroke = g2.getStroke();
        //Stroke drawingStroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        Stroke drawingStroke = new BasicStroke(2);
        double yPos = gp.transformYPos(0) - (level + 1)*gp.getUnitHeight() + gp.getUnitHeight()/2.0 - gp.getOffset();
        Line2D line = new Line2D.Double(gp.transformXPos(mateInterval.getStart()), yPos, gp.transformXPos(mateInterval.getEnd()), yPos);
        g2.setStroke(drawingStroke);
        g2.setColor(linecolor);
        g2.draw(line);
        g2.setStroke(currentStroke);//reset stroke

        Rectangle2D bound = new Rectangle2D.Double(Math.min(gp.transformXPos(mateInterval.getStart()),
                gp.transformXPos(mateInterval.getEnd())),
                yPos - gp.getUnitHeight()/2.0, Math.abs(gp.transformXPos(mateInterval.getEnd())-gp.transformXPos(mateInterval.getStart())),
                gp.getUnitHeight());
        if(ir1 != null) this.artifactMap.put(ir1, bound);
        if(ir2 != null) this.artifactMap.put(ir2, bound);

    }

    private Interval computeMateInterval(Interval i1, Interval i2){

        int start;
        int end;
        if(i1.getEnd() < i2.getEnd()){
            start = i1.getEnd();
            end = i2.getStart();
        } else {
            start = i2.getEnd();
            end = i1.getStart();
        }

        return Interval.valueOf(start, end);

    }

    /*
     * Determine at what level a piled interval should be drawn.
     */
    private int computePiledIntervalLevel(ArrayList<ArrayList<Interval>> levels, Interval interval){
        ArrayList<Interval> level;
        for(int i = 0; i < levels.size(); i++){
            level = levels.get(i);
            boolean conflict = false;
            for(Interval current : level){
                if(current.intersects(interval)){
                    conflict = true;
                    break;
                }
            }
            if(!conflict){
                level.add(interval);
                return i;
            }
        }
        levels.add(new ArrayList<Interval>());
        levels.get(levels.size()-1).add(interval);
        return levels.size()-1;
    }
    
    /*
     * Store information for drawing a read so that it can be done later.
     * Used for piled interval mode
     */
    private class DrawStore extends JPanel{

        public IntervalRecord intervalRecord;
        public Interval interval;
        public SAMRecord sam;
        public int level;
        public Interval mateInterval; //only set if line connection generated from this read
        public IntervalRecord mateIntervalRecord; //as above + mate in view

        public DrawStore(IntervalRecord intervalRecord, SAMRecord samRecord, int level, Interval mateInterval, IntervalRecord mateIntervalRecord){
            this.intervalRecord = intervalRecord;
            this.interval = intervalRecord.getInterval();
            this.sam = samRecord;
            this.level = level;
            this.mateInterval = mateInterval;
            this.mateIntervalRecord = mateIntervalRecord;
        }
    }
    
    
    private void drawLegend(Graphics2D g2, String[] legendStrings, Color[] legendColors, int startx, int starty) {
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
        g2.setFont(SMALL_FONT);

        int x = startx;
        int y = starty;
        String legendString;
        for (int i=0; i<legendStrings.length; i++) {
            legendString = legendStrings[i];
            g2.setColor(legendColors[i]);
            g2.setStroke(TWO_STROKE );
            Rectangle2D stringRect = SMALL_FONT.getStringBounds(legendString, g2.getFontRenderContext());
            g2.drawLine(x-25, y-(int)stringRect.getHeight()/2, x-5, y-(int)stringRect.getHeight()/2);
            g2.setColor(cs.getColor(ColourKey.POINT_LINE));
            g2.setStroke(ONE_STROKE);
            g2.drawString(legendString, x, y);

            y += stringRect.getHeight()+2;

        }
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
        }

        @Override
        public void paint(Graphics g){

            if (isHidden) {
                drawHidden((Graphics2D)g);
            }else{
                drawLegend((Graphics2D)g, ColourKey.CONCORDANT_LENGTH, ColourKey.DISCORDANT_LENGTH, ColourKey.ONE_READ_INVERTED, ColourKey.EVERTED_PAIR);
            }
        }

        private void drawLegend(Graphics2D g2, ColourKey... keys){
            ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);

            g2.setFont(SMALL_FONT);

            GradientPaint gp = new GradientPaint(0, 0,
                Color.WHITE, 0, 60,
                new Color(230,230,230));

            g2.setPaint(gp);
            g2.fillRect(0, 0, 125, 60);


            g2.setColor(Color.BLACK);
            g2.draw(new Rectangle2D.Double(0, 0, 124, 60));

            int x = 30;
            int y = 15;

            for (ColourKey k: keys) {
                String legendString = k.getName();
                g2.setColor(cs.getColor(k));
                g2.setStroke(TWO_STROKE );
                Rectangle2D stringRect = SMALL_FONT.getStringBounds(legendString, g2.getFontRenderContext());
                g2.drawLine(x-25, y-(int)stringRect.getHeight()/2, x-5, y-(int)stringRect.getHeight()/2);
                g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
                g2.setStroke(ONE_STROKE);
                g2.drawString(legendString, x, y);

                y += stringRect.getHeight()+2;
            }
        }

        private void drawHidden(Graphics2D g2){

            g2.setFont(SMALL_FONT);

            GradientPaint gp = new GradientPaint(0, 0,
                Color.WHITE, 0, 22,
                new Color(230,230,230));

            g2.setPaint(gp);
            g2.fillRect(0, 0, 125, 60);

            g2.setColor(Color.darkGray);
            g2.draw(new Rectangle2D.Double(0, 0, 22, 22));

            int[] xp = {8,14,14};
            int[] yp = {11,5,17};
            g2.fillPolygon(new Polygon(xp,yp,3));
        }
    }
}
