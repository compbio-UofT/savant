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
    private static final Font MISMATCH_FONT = new Font("Sans-Serif", Font.PLAIN, 12);
    private static final Stroke ONE_STROKE = new BasicStroke(1.0f);
    private static final Stroke TWO_STROKE = new BasicStroke(2.0f);

    private byte[] refSeq = null;
    private DrawingMode lastMode;
    private Resolution lastResolution;

    private static boolean fontFits(Font f, double width, double height, Graphics2D g2) {
        String baseChar = "G";
        Rectangle2D charRect = f.getStringBounds(baseChar, g2.getFontRenderContext());
        if (charRect.getWidth() > width || charRect.getHeight() > height) return false;
        else return true;
    }

    public BAMTrackRenderer() {
    }

    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        switch (evt.getType()) {
            case COMPLETED:
                if ((DrawingMode)instructions.get(DrawingInstruction.MODE) == DrawingMode.ARC_PAIRED) {
                    int maxDataValue = BAMTrack.getArcYMax(evt.getData());
                    Range range = (Range)instructions.get(DrawingInstruction.RANGE);
                    addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(0,(int)Math.round(maxDataValue+maxDataValue*0.1))));
                }
                break;
        }
        super.handleEvent(evt);
    }

    @Override
    public void render(Graphics2D g2, GraphPane gp) throws RenderingException {

        DrawingMode oldMode = lastMode;
        lastMode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        Resolution r = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        if (r == Resolution.HIGH) {
            if (lastMode == DrawingMode.MISMATCH || lastMode == DrawingMode.SNP || lastMode == DrawingMode.STRAND_SNP || lastMode == DrawingMode.SEQUENCE || lastMode == DrawingMode.STANDARD_PAIRED) {
                // fetch reference sequence for comparison with cigar string
                Genome genome = GenomeController.getInstance().getGenome();
                if (genome.isSequenceSet()) {
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
        if (lastMode != DrawingMode.ARC_PAIRED && lastMode != DrawingMode.SNP && lastMode != DrawingMode.STRAND_SNP) {
            // For non-arc modes, we want to establish our interval height when we switch from coverage to non-coverage.
            if (lastResolution != r || oldMode == DrawingMode.ARC_PAIRED || oldMode == DrawingMode.SNP || oldMode == DrawingMode.STRAND_SNP) {
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
            // Arc mode and the SNP modes are always initially scaled to fit.
            if (oldMode != DrawingMode.ARC_PAIRED && oldMode != DrawingMode.SNP && oldMode != DrawingMode.STRAND_SNP) {
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
        if (data.isEmpty()) {
            throw new RenderingException("No data in range", 1);
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPane gp, Resolution r) throws RenderingException {

        if (lastMode == DrawingMode.MISMATCH) {
            Genome genome = GenomeController.getInstance().getGenome();
            if (!genome.isSequenceSet()) {
                throw new RenderingException("No reference sequence loaded\nSwitch to standard display mode", 2);
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
        
        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

            List<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                BAMIntervalRecord bamRecord = (BAMIntervalRecord) intervalRecord;

                if (bamRecord.getSamRecord().getReadUnmappedFlag()) {
                    // this read is unmapped, don't visualize it
                    recordToShapeMap.put(intervalRecord, null);
                } else {
                    Polygon readshape = renderRead(g2, gp, bamRecord, level, range, gp.getUnitHeight());
                    recordToShapeMap.put(intervalRecord, readshape);
                }
            }
        }

    }

    public Polygon renderRead(Graphics2D g2, GraphPane gp, BAMIntervalRecord rec, int level, Range range, double unitHeight) {

        SAMRecord samRec = rec.getSamRecord();
        boolean reverseStrand = samRec.getReadNegativeStrandFlag();

        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color readColor = cs.getColor(reverseStrand ? ColourKey.REVERSE_STRAND : ColourKey.FORWARD_STRAND);

        Color override = rec.getColor();
        if (override != null) {
            readColor = override;
        }
        if ((Boolean)instructions.get(DrawingInstruction.MAPPING_QUALITY)) {
            int alpha = getConstrainedAlpha(samRec.getMappingQuality());
            readColor = new Color(readColor.getRed(), readColor.getGreen(), readColor.getBlue(), alpha);
        }

        double x=0;
        double y=0;
        double w=0;
        double h=0;

        double arrowHeight = unitHeight/2;
        double arrowWidth = unitHeight/4;

        // cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo() + 1);

        boolean drawPoint = false;
        //y = gp.transformYPos(0) - (level + 1)*unitHeight;
        y = gp.transformYPos(0) - (level + 1)*unitHeight - gp.getOffset();
        
        w = gp.getWidth(rec.getInterval().getLength());

        if (w > arrowWidth) {
            drawPoint = true;
        }
        h = unitHeight;
        x = gp.transformXPos(rec.getInterval().getStart());

        // cut off x and w so no drawing happens off-screen
        boolean cutoffLeft = false;
        boolean cutoffRight = false;
        double x2;
        if (rightMostX < x+w) {
            x2 = rightMostX;
            cutoffRight = true;
        } else {
            x2 = x+w;
        }
        if (leftMostX > x) {
            x = leftMostX;
            cutoffLeft = true;
        }
        w = x2 - x;

        // find out which direction we're pointing
        Polygon pointyBar = new Polygon();
        pointyBar.addPoint((int)x, (int)y);
        pointyBar.addPoint((int)(x+w), (int)y);
        if (!reverseStrand && drawPoint && !cutoffRight) {
            pointyBar.addPoint((int)(x+w+arrowWidth), (int)(y+arrowHeight));
        }
        pointyBar.addPoint((int)(x+w), (int)(y+h));
        pointyBar.addPoint((int)x, (int)(y+h));
        if (reverseStrand && drawPoint && !cutoffLeft) {
            pointyBar.addPoint((int)(x-arrowWidth), (int)(y+arrowHeight));
        }
        
        // Only fill in the read if we're not going to slam bases on top of it.
        boolean baseQuality = (Boolean)instructions.get(DrawingInstruction.BASE_QUALITY);
        if (lastMode != DrawingMode.SEQUENCE && !baseQuality) {
            g2.setColor(readColor);
            g2.fill(pointyBar);
        }

        // Render individual bases/mismatches as appropriate for the current mode.
        if (lastMode != DrawingMode.STANDARD || baseQuality) {
            renderBases(g2, gp, samRec, level, refSeq, range, unitHeight);
        }

        // Draw outline, if there's room
        if (pointyBar.getBounds().getHeight() > 4) {
            g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
            g2.draw(pointyBar);
        }
        return pointyBar;

    }

    private ColourKey getSubPileColour(Nucleotide snpNuc, Nucleotide genomeNuc) {
        if (snpNuc.equals(genomeNuc)) {
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

    /**
     * Render the individual bases on top of the read.  Depending on the drawing mode
     * this can be either bases read or mismatches.
     */
    private void renderBases(Graphics2D g2, GraphPane gp, SAMRecord samRecord, int level, byte[] refSeq, Range range, double unitHeight) {

        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        
        boolean baseQualityEnabled = (Boolean)instructions.get(DrawingInstruction.BASE_QUALITY);
        boolean drawingAllBases = lastMode == DrawingMode.SEQUENCE || baseQualityEnabled;
        
        double unitWidth = gp.getUnitWidth();
        int offset = gp.getOffset();

        // Cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo()) + unitWidth;

        int alignmentStart = samRecord.getAlignmentStart();

        byte[] readBases = samRecord.getReadBases();
        byte[] baseQualities = samRecord.getBaseQualities();
        boolean sequenceSaved = readBases.length > 0;
        Cigar cigar = samRecord.getCigar();

        // Absolute positions in the reference sequence and the read bases, set after each cigar operator is processed
        int sequenceCursor = alignmentStart;
        int readCursor = alignmentStart;

        for (CigarElement cigarElement : cigar.getCigarElements()) {

            int operatorLength = cigarElement.getLength();
            CigarOperator operator = cigarElement.getOperator();
            Rectangle2D.Double opRect = null;

            double opStart = gp.transformXPos(sequenceCursor);
            double opWidth = gp.getWidth(operatorLength);

            // Cut off start and width so no drawing happens off-screen, must be done in the order w, then x, since w depends on first value of x
            double x2 = Math.min(rightMostX, opStart + opWidth);
            opStart = Math.max(leftMostX, opStart);
            opWidth = x2 - opStart;

            switch (operator) {
                case D: // Deletion
                    renderDeletion(g2, gp, opStart, level, operatorLength, unitHeight);
                    break;
                
                case I: // Insertion
                    renderInsertion(g2, gp, sequenceCursor, level, unitHeight);
                    break;
                    
                case M: // Match or mismatch
                    // some SAM files do not contain the read bases
                    if (sequenceSaved) {
                        for (int i=0; i < operatorLength; i++) {
                            // indices into refSeq and readBases associated with this position in the cigar string
                            int readIndex = readCursor-alignmentStart+i;
                            int refIndex = sequenceCursor + i - range.getFrom();
                            if (refIndex >= 0 && (refSeq == null || refIndex < refSeq.length)) {
                                
                                boolean mismatched = refSeq[refIndex] != readBases[readIndex];
                                
                                if (mismatched || drawingAllBases) {
                                    Color col;
                                    if ((mismatched && lastMode != DrawingMode.STANDARD) || lastMode == DrawingMode.SEQUENCE) {
                                        col = cs.getBaseColor((char)readBases[readIndex]);
                                    } else {
                                        col = cs.getColor(samRecord.getReadNegativeStrandFlag() ? ColourKey.REVERSE_STRAND : ColourKey.FORWARD_STRAND);
                                    }

                                    if (baseQualityEnabled && col != null) {
                                        col = new Color(col.getRed(), col.getGreen(), col.getBlue(), getConstrainedAlpha((int)Math.round((baseQualities[readIndex] * 0.025) * 255)));
                                    }

                                    double xCoordinate = gp.transformXPos(sequenceCursor + i);
                                    if (col != null) {
                                        opRect = new Rectangle2D.Double(xCoordinate, gp.transformYPos(0)-((level + 1)*unitHeight) - offset, unitWidth, unitHeight);
                                        g2.setColor(col);
                                        g2.fill(opRect);
                                    }
                                    if (mismatched && fontFits(BrowserSettings.getTrackFont(),unitWidth,unitHeight,g2)) {
                                        // If it's a real mismatch, we want to draw the base letter (space permitting).
                                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                        g2.setColor(new Color(10, 10, 10));
                                        g2.setFont(MISMATCH_FONT);
                                        g2.drawBytes(readBases, readIndex, 1, (int) (xCoordinate + unitWidth/2 - g2.getFontMetrics().bytesWidth(readBases, readIndex, 1)/2), (int) ((gp.transformYPos(0)-((level)*unitHeight))-unitHeight/2+g2.getFontMetrics().getMaxAscent()/2));
                                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                                    }
                                }
                            }
                        }
                    }
                    break;
               
                case N: // Skipped
                    opRect = new Rectangle2D.Double(opStart, gp.transformYPos(0)-((level+1)*unitHeight) - offset, opWidth, unitHeight);
                    g2.setColor(Color.gray);
                    g2.fill(opRect);
                    break;

                default:    // P - passing, H - hard clip, or S - soft clip
                    break;
            }
            if (operator.consumesReadBases()) readCursor += operatorLength;
            if (operator.consumesReferenceBases()) sequenceCursor += operatorLength;
        }

    }
    
    /**
     * Render the black rectangle which indicates a deletion.
     */
    private void renderDeletion(Graphics2D g2, GraphPane gp, double opStart, int level, int operatorLength, double unitHeight) {
        double width = gp.getWidth(operatorLength);
        if (width < 1.0) {
            width = 1.0;
        }
        Rectangle2D opRect = new Rectangle2D.Double(opStart, gp.transformYPos(0) - (level + 1) * unitHeight - gp.getOffset(), width, unitHeight);
        g2.setColor(Color.BLACK);
        g2.fill(opRect);
    }

    private void renderInsertion(Graphics2D g2, GraphPane gp, int pos, int level, double unitHeight) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawInsertion(g2, gp, pos, level, unitHeight);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
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
        LOG.debug("BAMTrackRenderer.renderArcMatePairMode: " + data.size() + " records.");
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

    private void renderSNPMode(Graphics2D g2, GraphPane gp, Resolution r) {

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
        for(Pileup p : pileups) {
            double current = p.getTotalCoverage();
            if (current > maxHeight) maxHeight = current;
        }

        gp.setXRange(axisRange.getXRange());
        gp.setYRange(new Range(0,(int)Math.rint((double)maxHeight/0.9)));

        double unitHeight = (Math.rint(gp.transformYPos(0) * 0.9)) / maxHeight;
        double unitWidth =  gp.getUnitWidth();

        for(Pileup p : pileups) {

            Nucleotide snpNuc = null;
            int bottom = (int)gp.transformYPos(0);

            Nucleotide genomeNuc = null;
            if (genome.isSequenceSet()) {
                String genomeNucString = "A";
                byte[] readBase = new byte[1];
                assert Math.abs(p.getPosition() - startPosition) <= Integer.MAX_VALUE;
                readBase[0] = refSeq[(int)(p.getPosition() - startPosition)];
                genomeNucString = new String(readBase);
                //genomeNucString = genome.getRecords(ReferenceController.getInstance().getReferenceName(), new Range((int) p.getPosition(), (int) p.getPosition()));
                genomeNuc = Pileup.stringToNuc(genomeNucString);
                snpNuc = genomeNuc;
            }
            
            
            while ((genome.isSequenceSet() && (snpNuc = p.getLargestNucleotide(genomeNuc)) != null) || ((snpNuc = p.getLargestNucleotide()) != null)) {
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
    
    private void renderStrandSNPMode(Graphics2D g2, GraphPane gp, Resolution r) {

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
        for(Pileup p : pileups) {
            double current1 = p.getTotalStrandCoverage(true);
            double current2 = p.getTotalStrandCoverage(false);
            if (current1 > maxHeight) maxHeight = current1;
            if (current2 > maxHeight) maxHeight = current2;
        }

        gp.setXRange(axisRange.getXRange());
        gp.setYRange(new Range(0,(int)Math.rint((double)maxHeight/0.9)));

        double unitHeight = (Math.rint(gp.transformYPos(0) * 0.45)) / maxHeight;
        double unitWidth =  gp.getUnitWidth();

        for(Pileup p : pileups) {

            Nucleotide snpNuc = null;
            int bottom = (int)gp.transformYPos(0) / 2;
            int top = (int)gp.transformYPos(0) / 2;

            Nucleotide genomeNuc = null;
            if (genome.isSequenceSet()) {
                String genomeNucString = "A";
                byte[] readBase = new byte[1];
                assert Math.abs(p.getPosition() - startPosition) <= Integer.MAX_VALUE;
                readBase[0] = refSeq[(int)(p.getPosition() - startPosition)];
                genomeNucString = new String(readBase);
                //genomeNucString = genome.getRecords(ReferenceController.getInstance().getReferenceName(), new Range((int) p.getPosition(), (int) p.getPosition()));
                genomeNuc = Pileup.stringToNuc(genomeNucString);
                snpNuc = genomeNuc;
            }


            while ((genome.isSequenceSet() && (snpNuc = p.getLargestNucleotide(genomeNuc)) != null) || ((snpNuc = p.getLargestNucleotide()) != null)) {
            
                double x = gp.transformXPos(p.getPosition());
                double coverage1 = p.getStrandCoverage(snpNuc, true);
                double coverage2 = p.getStrandCoverage(snpNuc, false);

                Color subPileColor = cs.getColor(getSubPileColour(snpNuc, genomeNuc));

                if (coverage1 > 0) {
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
                if (coverage2 > 0) {
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
            }
        }
    }
    
    public void renderReadsFromArc(Graphics2D g2, GraphPane gp, BAMIntervalRecord rec1, BAMIntervalRecord rec2, Range range) {
        
        int readHeight = gp.getParentFrame().getIntervalHeight();

        renderRead(g2, gp, rec1, 0, range, readHeight);

        if (rec2 != null) {
            renderRead(g2, gp, rec2, 0, range, readHeight);
        }
    }

    private void renderStandardPairedMode(Graphics2D g2, GraphPane gp) throws RenderingException {

        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color linecolor = cs.getColor(ColourKey.INTERVAL_LINE);
        Range range = axisRange.getXRange();
        int effectiveEnd = range.getTo() + 1;
        int effectiveStart = range.getFrom() - 1;

        gp.setXRange(axisRange.getXRange());

        Map<Integer, ArrayList<BAMIntervalRecord>> mateQueue = new HashMap<Integer, ArrayList<BAMIntervalRecord>>();
        ArrayList<ArrayList<Interval>> levels = new ArrayList<ArrayList<Interval>>();
        levels.add(new ArrayList<Interval>()); 
        ArrayList<DrawStore> savedDraws = new ArrayList<DrawStore>();

        for(int i = 0; i < data.size(); i++) {

            BAMIntervalRecord bamRecord = (BAMIntervalRecord)data.get(i);
            Interval interval = bamRecord.getInterval();
            SAMRecord samRecord = bamRecord.getSamRecord();
            SAMReadUtils.PairedSequencingProtocol prot = (SAMReadUtils.PairedSequencingProtocol) instructions.get(DrawingInstruction.PAIRED_PROTOCOL);
            SAMReadUtils.PairMappingType type = SAMReadUtils.getPairType(samRecord,prot);
            int arcLength = Math.abs(samRecord.getInferredInsertSize());

            //discard unmapped reads
            if (samRecord.getReadUnmappedFlag() || !samRecord.getReadPairedFlag() 
                    || samRecord.getMateUnmappedFlag() || type == null
                    || arcLength == 0) { // this read is unmapped, don't visualize it
                recordToShapeMap.put(bamRecord, null);
                continue;
            }

            //if mate off screen to the right, draw immediately
            if (samRecord.getMateAlignmentStart() > range.getTo()) {
                int level = computePiledIntervalLevel(levels, Interval.valueOf(interval.getStart(), effectiveEnd));
                savedDraws.add(new DrawStore(bamRecord, level, Interval.valueOf(effectiveEnd + 1, Integer.MAX_VALUE), null));
                continue;
            }

            //check if mate has already been found
            BAMIntervalRecord mate = popMate(mateQueue.get(samRecord.getMateAlignmentStart()), samRecord);
            if (mate != null) {
                int level = computePiledIntervalLevel(levels, 
                        Interval.valueOf(Math.min(interval.getStart(), mate.getInterval().getStart()),
                        Math.max(interval.getEnd(), mate.getInterval().getEnd())));
                savedDraws.add(new DrawStore(bamRecord, level, null, null));
                savedDraws.add(new DrawStore(mate, level, interval, bamRecord));


                continue;
            }

            //if mate not yet found, add to map queue
            if (mateQueue.get(interval.getStart()) == null) {
                mateQueue.put(interval.getStart(), new ArrayList<BAMIntervalRecord>());
            }
            mateQueue.get(interval.getStart()).add(bamRecord);          
        }

        //if there are records remaining without a mate, they are probably off screen to the left
        Iterator<ArrayList<BAMIntervalRecord>> it = mateQueue.values().iterator();
        while(it.hasNext()) {
            ArrayList<BAMIntervalRecord> list = it.next();
            for (BAMIntervalRecord bamRecord : list) {
                Interval interval = bamRecord.getInterval();
                int level = computePiledIntervalLevel(levels, Interval.valueOf(effectiveStart, interval.getEnd()));
                savedDraws.add(new DrawStore(bamRecord, level, Interval.valueOf(0, effectiveStart - 1), null));
            }
        }

        //resize frame if necessary
        gp.setYRange(new Range(0,levels.size()+2));
        if (gp.needsToResize()) return;

        //now, draw everything
        for(DrawStore drawStore : savedDraws) {
            Polygon readshape = renderRead(g2, gp, drawStore.intervalRecord, drawStore.level, range, gp.getUnitHeight());
            recordToShapeMap.put(drawStore.intervalRecord, readshape);
            if (drawStore.mateInterval != null) {
                connectPiledInterval(g2, gp, drawStore.intervalRecord.getInterval(), drawStore.mateInterval, drawStore.level, linecolor, drawStore.intervalRecord, drawStore.mateIntervalRecord);
            }
        }
    }

    /**
     * Find the mate for record with name readName in the list records.
     * If mate found, remove and return. Otherwise, return null.
     */
    private BAMIntervalRecord popMate(ArrayList<BAMIntervalRecord> records, SAMRecord samRecord) {

        if (records == null) return null;
        for(int i = 0; i < records.size(); i++) {
            SAMRecord samRecord2 = ((BAMIntervalRecord)records.get(i)).getSamRecord();
            if (MiscUtils.isMate(samRecord, samRecord2, false)) {
                BAMIntervalRecord intervalRecord = records.get(i);
                records.remove(i);
                return intervalRecord;
            }
        }
        return null;
    }
    /*
     * Connect intervals i1 and i2 with a dashed line to show mates. 
     */
    private void connectPiledInterval(Graphics2D g2, GraphPane gp, Interval i1, Interval i2, int level, Color linecolor, IntervalRecord ir1, IntervalRecord ir2) {
        Interval mateInterval = computeMateInterval(i1, i2);

        Stroke currentStroke = g2.getStroke();
        //Stroke drawingStroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        Stroke drawingStroke = new BasicStroke(2);
        double yPos = gp.transformYPos(0) - (level + 1)*gp.getUnitHeight() + gp.getUnitHeight()/2.0 - gp.getOffset();

        double arrowWidth = gp.getUnitHeight() * 0.25;
        Line2D line = new Line2D.Double(gp.transformXPos(mateInterval.getStart()) + arrowWidth, yPos, gp.transformXPos(mateInterval.getEnd()) - arrowWidth, yPos);
        g2.setStroke(drawingStroke);
        g2.setColor(linecolor);
        g2.draw(line);
        g2.setStroke(currentStroke);//reset stroke

        Rectangle2D bound = new Rectangle2D.Double(Math.min(gp.transformXPos(mateInterval.getStart()),
                gp.transformXPos(mateInterval.getEnd())),
                yPos - gp.getUnitHeight()/2.0, Math.abs(gp.transformXPos(mateInterval.getEnd())-gp.transformXPos(mateInterval.getStart())),
                gp.getUnitHeight());
        if (ir1 != null) artifactMap.put(ir1, bound);
        if (ir2 != null) artifactMap.put(ir2, bound);

    }

    private Interval computeMateInterval(Interval i1, Interval i2) {

        int start;
        int end;
        if (i1.getEnd() < i2.getEnd()) {
            start = i1.getEnd();
            end = i2.getStart();
        } else {
            start = i2.getEnd();
            end = i1.getStart();
        }

        return Interval.valueOf(start + 1, end);

    }

    /*
     * Determine at what level a piled interval should be drawn.
     */
    private int computePiledIntervalLevel(ArrayList<ArrayList<Interval>> levels, Interval interval) {
        ArrayList<Interval> level;
        for(int i = 0; i < levels.size(); i++) {
            level = levels.get(i);
            boolean conflict = false;
            for(Interval current : level) {
                if (current.intersects(interval)) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
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

        public BAMIntervalRecord intervalRecord;
        public int level;
        public Interval mateInterval; //only set if line connection generated from this read
        public IntervalRecord mateIntervalRecord; //as above + mate in view

        public DrawStore(BAMIntervalRecord intervalRecord, int level, Interval mateInterval, IntervalRecord mateIntervalRecord) {
            this.intervalRecord = intervalRecord;
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
    public JPanel arcLegendPaint() {
        JPanel panel = new LegendPanel();
        panel.setPreferredSize(new Dimension(125,61));
        return panel;
    }

    private class LegendPanel extends JPanel{
        private boolean isHidden = false;

        public LegendPanel() {
        }

        @Override
        public void paint(Graphics g) {

            if (isHidden) {
                drawHidden((Graphics2D)g);
            }else{
                drawLegend((Graphics2D)g, ColourKey.CONCORDANT_LENGTH, ColourKey.DISCORDANT_LENGTH, ColourKey.ONE_READ_INVERTED, ColourKey.EVERTED_PAIR);
            }
        }

        private void drawLegend(Graphics2D g2, ColourKey... keys) {
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

        private void drawHidden(Graphics2D g2) {

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
