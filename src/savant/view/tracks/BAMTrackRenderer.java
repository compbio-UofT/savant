/*
 *    Copyright 2010-2012 University of Toronto
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

package savant.view.tracks;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.Interval;
import savant.api.data.IntervalRecord;
import savant.api.data.Record;
import savant.api.data.Strand;
import savant.api.data.VariantType;
import savant.api.event.DataRetrievalEvent;
import savant.api.util.Resolution;
import savant.controller.GenomeController;
import savant.controller.LocationController;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Genome;
import savant.data.types.PileupRecord;
import savant.exception.RenderingException;
import savant.util.*;


/**
 * Class to perform all the rendering of a BAM Track in all its modes.
 *
 * @author vwilliams
 */
public class BAMTrackRenderer extends TrackRenderer {
    private static final Log LOG = LogFactory.getLog(BAMTrackRenderer.class);

    private static final Font MISMATCH_FONT = LEGEND_FONT.deriveFont(8.0f);

    private byte[] refSeq = null;
    private DrawingMode lastMode;
    private Resolution lastResolution;
    private double arrowWidth;          // Width (in pixels) of pointy bit

    public BAMTrackRenderer() {
    }

    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        switch (evt.getType()) {
            case COMPLETED:
                if ((DrawingMode)instructions.get(DrawingInstruction.MODE) == DrawingMode.ARC_PAIRED) {
                    int maxDataValue = BAMTrack.getArcYMax(evt.getData());
                    Range range = (Range)instructions.get(DrawingInstruction.RANGE);
                    addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(range, new Range(0,(int)Math.round(maxDataValue+maxDataValue*0.1))));
                }
                break;
        }
        super.handleEvent(evt);
    }

    @Override
    public void render(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        DrawingMode oldMode = lastMode;
        lastMode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        Resolution res = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        if (res == Resolution.HIGH) {
            if (lastMode != DrawingMode.STANDARD && lastMode != DrawingMode.SEQUENCE) {
                // fetch reference sequence for comparison with cigar string
                Genome genome = GenomeController.getInstance().getGenome();
                if (genome.isSequenceSet()) {
                    AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
                    String ref = LocationController.getInstance().getReferenceName();
                    Range r = axisRange.getXRange();
                    try {
                        refSeq = genome.getSequence(ref, r);
                    } catch (Exception e) {
                        throw new RenderingException(e.getMessage(), RenderingException.ERROR_PRIORITY);
                    }
                    if (refSeq == null) {
                        throw new RenderingException("No sequence data for " + ref + "\nSwitch to standard display mode", RenderingException.WARNING_PRIORITY);
                    }
                }
            }
        }
        if (lastMode != DrawingMode.ARC_PAIRED && lastMode != DrawingMode.SNP && lastMode != DrawingMode.STRAND_SNP) {
            // For non-arc modes, we want to establish our interval height when we switch from coverage to non-coverage.
            if (lastResolution != res || oldMode == DrawingMode.ARC_PAIRED || oldMode == DrawingMode.SNP || oldMode == DrawingMode.STRAND_SNP) {
                if (res == Resolution.HIGH) {
                    // We're switching from coverage (or arc mode) to high resolution.  The initial interval height
                    // will be taken from the slider.
                    gp.getParentFrame().setHeightFromSlider();
                } else {
                    // We're switching to coverage.  Initially scaled by default.
                    gp.setScaledToFit(true);
                }
                lastResolution = res;
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
                if (res == Resolution.HIGH) {
                    renderPackMode(g2, gp, res);
                }
                break;
            case STANDARD_PAIRED:
                if (res == Resolution.HIGH) {
                    renderStandardPairedMode(g2, gp);
                }
                break;
            case ARC_PAIRED:
                renderArcPairedMode(g2, gp);
                break;
            case SNP:
                if (res == Resolution.HIGH) {
                    renderSNPMode(g2, gp, res);
                }
                break;
            case STRAND_SNP:
                if (res == Resolution.HIGH) {
                    renderStrandSNPMode(g2, gp, res);
                }
                break;
        }
        if (data.isEmpty()) {
            throw new RenderingException("No data in range", RenderingException.INFO_PRIORITY);
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPaneAdapter gp, Resolution r) throws RenderingException {

        if (lastMode == DrawingMode.MISMATCH) {
            Genome genome = GenomeController.getInstance().getGenome();
            if (!genome.isSequenceSet()) {
                throw new RenderingException("No reference sequence loaded\nSwitch to standard display mode", RenderingException.WARNING_PRIORITY);
            }
        }

        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        Range range = axisRange.getXRange();

        //calculate breathing room parameter
        if (!gp.isScaledToFit()) {
            arrowWidth = gp.getUnitHeight() * 0.25;
        }
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

                if (bamRecord.getSAMRecord().getReadUnmappedFlag()) {
                    // this read is unmapped, don't visualize it
                    recordToShapeMap.put(intervalRecord, null);
                } else {
                    Shape readshape = renderRead(g2, gp, bamRecord, level, range, gp.getUnitHeight());
                    recordToShapeMap.put(intervalRecord, readshape);
                }
            }
        }

    }

    private Shape renderRead(Graphics2D g2, GraphPaneAdapter gp, BAMIntervalRecord rec, int level, Range range, double readHeight) {

        SAMRecord samRec = rec.getSAMRecord();
        boolean reverseStrand = samRec.getReadNegativeStrandFlag();

        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOUR_SCHEME);
        Color readColor = cs.getColor(reverseStrand ? ColourKey.REVERSE_STRAND : ColourKey.FORWARD_STRAND);

        Color override = rec.getColor();
        if (override != null) {
            readColor = override;
        }
        if ((Boolean)instructions.get(DrawingInstruction.MAPPING_QUALITY)) {
            int alpha = getConstrainedAlpha(samRec.getMappingQuality());
            readColor = new Color(readColor.getRed(), readColor.getGreen(), readColor.getBlue(), alpha);
        }

        // cutoffs to determine when not to draw
        double leftMostX = gp.transformXPos(range.getFrom());
        double rightMostX = gp.transformXPos(range.getTo() + 1);

        //y = gp.transformYPos(0) - (level + 1)*unitHeight;
        double x = gp.transformXPos(rec.getInterval().getStart());
        double y = gp.transformYPos(0) - (level + 1)* readHeight - gp.getOffset();
        double w = rec.getInterval().getLength() * gp.getUnitWidth();


        // cut off x and w so no drawing happens off-screen
        double x2;
        if (rightMostX < x+w) {
            x2 = rightMostX;
        } else {
            x2 = x+w;
        }
        if (leftMostX > x) {
            x = leftMostX;
        }
        w = x2 - x;

        Shape pointyBar = getPointyBar(reverseStrand, x, y, w, readHeight);

        // Only fill in the read if we're not going to slam bases on top of it.
        boolean baseQuality = (Boolean)instructions.get(DrawingInstruction.BASE_QUALITY);
        if (lastMode != DrawingMode.SEQUENCE && !baseQuality) {
            g2.setColor(readColor);
            g2.fill(pointyBar);
        }

        // Render individual bases/mismatches as appropriate for the current mode.
        if (lastMode != DrawingMode.STANDARD || baseQuality) {
            renderBases(g2, gp, samRec, level, refSeq, range, readHeight);
        }

        // Draw outline, if there's room
        if (pointyBar.getBounds().getHeight() >= 4.0) {
            g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
            g2.draw(pointyBar);
        }
        return pointyBar;

    }

    private Shape getPointyBar(boolean reverseStrand, double x, double y, double w, double h) {
        double arrowHeight = h * 0.5;
        if (w > arrowWidth) {
            if (reverseStrand) {
                return MiscUtils.createPolygon(x, y, x + w, y, x + w, y + h, x, y + h, x - arrowWidth, y + arrowHeight);
            } else {
                return MiscUtils.createPolygon(x, y, x + w, y, x + w + arrowWidth, y + arrowHeight, x + w, y + h, x, y + h);
            }
        }
        return MiscUtils.createPolygon(x, y, x + w, y, x + w, y + h, x, y + h);
    }

    private ColourKey getSubPileColour(VariantType snpNuc, VariantType genomeNuc) {
        if (snpNuc == genomeNuc || snpNuc == VariantType.INSERTION) {
            // We return REVERSE_STRAND here because that's the colour we want when we're
            // doing the non-stranded SNP mode.
            return ColourKey.REVERSE_STRAND;
        } else {
            switch (snpNuc) {
                case SNP_A:
                    return ColourKey.A;
                case SNP_C:
                    return ColourKey.C;
                case SNP_G:
                    return ColourKey.G;
                case SNP_T:
                    return ColourKey.T;
                case DELETION:
                    return ColourKey.DELETED_BASE;
                case OTHER:
                    return ColourKey.N;
                default:
                    return null;
            }
        }
    }

    /**
     * Render the individual bases on top of the read.  Depending on the drawing mode
     * this can be either bases read or mismatches.
     */
    private void renderBases(Graphics2D g2, GraphPaneAdapter gp, SAMRecord samRecord, int level, byte[] refSeq, Range range, double unitHeight) {

        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOUR_SCHEME);

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
        List<Rectangle2D> insertions = new ArrayList<Rectangle2D>();

        FontMetrics fm = g2.getFontMetrics(MISMATCH_FONT);
        Rectangle2D charRect = fm.getStringBounds("G", g2);
        boolean fontFits = charRect.getWidth() <= unitWidth && charRect.getHeight() <= unitHeight;
        if (fontFits) {
            g2.setFont(MISMATCH_FONT);
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (CigarElement cigarElement : cigar.getCigarElements()) {

            int operatorLength = cigarElement.getLength();
            CigarOperator operator = cigarElement.getOperator();
            Rectangle2D.Double opRect = null;

            double opStart = gp.transformXPos(sequenceCursor);
            double opWidth = operatorLength * unitWidth;

            // Cut off start and width so no drawing happens off-screen, must be done in the order w, then x, since w depends on first value of x
            double x2 = Math.min(rightMostX, opStart + opWidth);
            opStart = Math.max(leftMostX, opStart);
            opWidth = x2 - opStart;

            switch (operator) {
                case D: // Deletion
                    if (opWidth > 0.0) {
                        renderDeletion(g2, gp, opStart, level, operatorLength, unitHeight);
                    }
                    break;

                case I: // Insertion
                    insertions.add(new Rectangle2D.Double(gp.transformXPos(sequenceCursor), gp.transformYPos(0) - ((level + 1) * unitHeight) - gp.getOffset(), unitWidth, unitHeight));
                    break;

                case M: // Match or mismatch
                    // some SAM files do not contain the read bases
                    if (sequenceSaved) {
                        for (int i=0; i < operatorLength; i++) {
                            // indices into refSeq and readBases associated with this position in the cigar string
                            int readIndex = readCursor - alignmentStart + i;
                            int refIndex = sequenceCursor + i - range.getFrom();
                            boolean mismatched = false;
                            if (refIndex >= 0 && refSeq != null && refIndex < refSeq.length) {
                                mismatched = refSeq[refIndex] != readBases[readIndex];
                            }

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
                                double top = gp.transformYPos(0) - ((level + 1) * unitHeight) - offset;
                                if (col != null) {
                                    opRect = new Rectangle2D.Double(xCoordinate, top, unitWidth, unitHeight);
                                    g2.setColor(col);
                                    g2.fill(opRect);
                                }
                                if (lastMode != DrawingMode.SEQUENCE && mismatched && fontFits) {
                                    // If it's a real mismatch, we want to draw the base letter (space permitting).
                                    g2.setColor(new Color(10, 10, 10));
                                    String s = new String(readBases, readIndex, 1);
                                    charRect = fm.getStringBounds(s, g2);
                                    g2.drawString(s, (float)(xCoordinate + (unitWidth - charRect.getWidth()) * 0.5), (float)(top + fm.getAscent() + (unitHeight - charRect.getHeight()) * 0.5));
                                }
                            }
                        }
                    }
                    break;

                case N: // Skipped
                    opRect = new Rectangle2D.Double(opStart, gp.transformYPos(0)-((level+1)*unitHeight) - offset, opWidth, unitHeight);
                    g2.setColor(cs.getColor(ColourKey.SKIPPED));
                    g2.fill(opRect);
                    break;

                default:    // P - passing, H - hard clip, or S - soft clip
                    break;
            }
            if (operator.consumesReadBases()) readCursor += operatorLength;
            if (operator.consumesReferenceBases()) sequenceCursor += operatorLength;
        }
        for (Rectangle2D ins: insertions) {
            drawInsertion(g2, ins.getX(), ins.getY(), ins.getWidth(), ins.getHeight());
        }
    }

    /**
     * Render the black rectangle which indicates a deletion.
     */
    private void renderDeletion(Graphics2D g2, GraphPaneAdapter gp, double opStart, int level, int operatorLength, double unitHeight) {
        double width = operatorLength * gp.getUnitWidth();
        if (width < 1.0) {
            width = 1.0;
        }
        Rectangle2D opRect = new Rectangle2D.Double(opStart, gp.transformYPos(0) - (level + 1) * unitHeight - gp.getOffset(), width, unitHeight);
        g2.setColor(Color.BLACK);
        g2.fill(opRect);
    }

    private Color makeTransparent(Color c) {
        return new Color(c.getRed(),c.getGreen(),c.getBlue(),90);
    }

    private void renderArcPairedMode(Graphics2D g2, GraphPaneAdapter gp) {

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LOG.debug("YMAX for ARC mode: " + ((AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE)).getYMax());
        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOUR_SCHEME);
        int discordantMin = (Integer) instructions.get(DrawingInstruction.DISCORDANT_MIN);
        int discordantMax = (Integer) instructions.get(DrawingInstruction.DISCORDANT_MAX);
        //LOG.info("discordantMin=" + discordantMin + " discordantMax=" + discordantMax);

        // set up colors
        Color normalArcColor = makeTransparent(cs.getColor(ColourKey.CONCORDANT_LENGTH));
        Color invertedReadColor = makeTransparent(cs.getColor(ColourKey.ONE_READ_INVERTED));
        Color evertedPairColor = makeTransparent(cs.getColor(ColourKey.EVERTED_PAIR));
        Color discordantLengthColor = makeTransparent(cs.getColor(ColourKey.DISCORDANT_LENGTH));
        Color unmappedMateColor = makeTransparent(cs.getColor(ColourKey.UNMAPPED_MATE));

        // set graph pane's range parameters
        gp.setXRange(axisRange.getXRange());
        gp.setYRange(axisRange.getYRange());

        // iterate through the data and draw
        LOG.debug("BAMTrackRenderer.renderArcMatePairMode: " + data.size() + " records.");
        for (Record record: data) {
            BAMIntervalRecord bamRecord = (BAMIntervalRecord)record;
            SAMRecord samRecord = bamRecord.getSAMRecord();
            SAMReadUtils.PairedSequencingProtocol prot = (SAMReadUtils.PairedSequencingProtocol) instructions.get(DrawingInstruction.PAIRED_PROTOCOL);
            SAMReadUtils.PairMappingType type = SAMReadUtils.getPairType(samRecord,prot);

            if (samRecord.getReadPairedFlag() && type != null) {
                if (samRecord.getMateUnmappedFlag()) {
                    // Read with unmapped mate.
                    int alignmentStart = samRecord.getAlignmentStart();
                    double x = gp.transformXPos(alignmentStart);
                    double radius = 4.0;
                    double top = gp.transformYPos(axisRange.getYRange().getTo()) + radius;
                    g2.setColor(unmappedMateColor);
                    g2.setStroke(ONE_STROKE);
                    Path2D flower = new Path2D.Double();
                    flower.moveTo(x, gp.transformYPos(0.0));
                    flower.lineTo(x, top + radius);
                    flower.append(new Ellipse2D.Double(x - radius, top - radius, radius * 2.0, radius * 2.0), false);
                    g2.draw(flower);
                    recordToShapeMap.put(record, flower);
                } else {
                    // Paired read with normal mate.

                    int arcLength = Math.abs(samRecord.getInferredInsertSize());

                    // skip reads with a zero insert length--probably mapping errors
                    if (arcLength == 0) continue;

                    int alignmentStart;
                    int mateAlignmentStart = samRecord.getMateAlignmentStart();
                    if (samRecord.getAlignmentStart() > mateAlignmentStart) {
                        if (!(mateAlignmentStart < LocationController.getInstance().getRangeStart())) {
                            // this is the second in the pair, and it doesn't span the beginning of the range, so don't draw anything
                            continue;
                        } else {
                            // switch the mate start/end for the read start/end to deal with reversed position
                            alignmentStart = mateAlignmentStart;
                        }
                    } else {
                        alignmentStart = samRecord.getAlignmentStart();
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

                    double rectWidth = arcLength * gp.getUnitWidth();
                    double rectHeight = arcHeight * 2 * gp.getUnitHeight();

                    double xOrigin = gp.transformXPos(intervalStart);
                    double yOrigin = gp.transformYPos(arcHeight);

                    Arc2D arc = new Arc2D.Double(xOrigin, yOrigin, rectWidth, rectHeight, -180, -180, Arc2D.OPEN);
                    g2.draw(arc);
                    recordToShapeMap.put(record, arc);
                }
            }
        }
    }

    private void renderSNPMode(Graphics2D g2, GraphPaneAdapter gp, Resolution r) throws RenderingException {

        Genome genome = GenomeController.getInstance().getGenome();
        if (!genome.isSequenceSet()) {
            throw new RenderingException("No reference sequence loaded\nSwitch to standard display mode", RenderingException.WARNING_PRIORITY);
        }

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);

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
            SAMRecord samRecord = ((BAMIntervalRecord)record).getSAMRecord();
            updatePileupsFromSAMRecord(pileups, samRecord, startPosition);
        }

        double maxHeight = 0;
        for (Pileup p : pileups) {
            int current = p.getTotalCoverage(null);
            if (current > maxHeight) maxHeight = current;
        }

        gp.setXRange(axisRange.getXRange());
        gp.setYRange(new Range(0,(int)Math.rint((double)maxHeight/0.9)));

        double unitHeight = (Math.rint(gp.transformYPos(0) * 0.9)) / maxHeight;
        double unitWidth =  gp.getUnitWidth();

        ColourAccumulator accumulator = new ColourAccumulator(cs);
        List<Rectangle2D> insertions = new ArrayList<Rectangle2D>();

        for (Pileup p : pileups) {
            int totalCoverage = p.getTotalCoverage(null);
            if (totalCoverage > 0) {
                double bottom = gp.transformYPos(0);
                double x = gp.transformXPos(p.getPosition());
                double h = unitHeight * totalCoverage;

                VariantType genomeNuc = Pileup.getVariantType((char)refSeq[p.getPosition() - startPosition]);
                VariantType snpNuc = genomeNuc;
                if (totalCoverage > p.getCoverage(genomeNuc, null)) {
                    // Only record a shape if we have at least some mismatches.
                    recordToShapeMap.put(new PileupRecord(p, false), new Rectangle2D.Double(x, bottom - h, unitWidth, h));
                }

                while ((genome.isSequenceSet() && (snpNuc = p.getLargestVariantType(genomeNuc)) != null) || ((snpNuc = p.getLargestVariantType(VariantType.OTHER)) != null)) {
                    h = unitHeight * p.getCoverage(snpNuc, null);
                    Rectangle2D rect = new Rectangle2D.Double(x, bottom - h, unitWidth, h);
                    accumulator.addShape(getSubPileColour(snpNuc, genomeNuc), rect);
                    if (snpNuc == VariantType.INSERTION) {
                        insertions.add(rect);
                    } else {
                        bottom -= h;
                    }
                    p.clearVariantType(snpNuc);
                }
            }
        }

        accumulator.fill(g2);
        for (Rectangle2D ins: insertions) {
            drawInsertion(g2, ins.getX(), ins.getY(), ins.getWidth(), ins.getHeight());
        }
    }

    private void renderStrandSNPMode(Graphics2D g2, GraphPaneAdapter gp, Resolution r) throws RenderingException {

        Genome genome = GenomeController.getInstance().getGenome();
        if (!genome.isSequenceSet()) {
            throw new RenderingException("No reference sequence loaded\nSwitch to standard display mode", RenderingException.WARNING_PRIORITY);
        }


        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        int xMin = axisRange.getXMin();
        int xMax = axisRange.getXMax();
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);

        List<Pileup> pileups = new ArrayList<Pileup>();

        // make the pileups
        for (int j = xMin; j <= xMax; j++) {
            pileups.add(new Pileup(j));
        }

        // Go through the samrecords and edit the pileups
        for (Record record: data) {
            SAMRecord samRecord = ((BAMIntervalRecord)record).getSAMRecord();
            updatePileupsFromSAMRecord(pileups, samRecord, xMin);
        }

        double maxHeight = 0.0;
        for (Pileup p : pileups) {
            int current1 = p.getTotalCoverage(Strand.REVERSE);
            int current2 = p.getTotalCoverage(Strand.FORWARD);
            if (current1 > maxHeight) maxHeight = current1;
            if (current2 > maxHeight) maxHeight = current2;
        }
        int yMax = (int)Math.ceil(maxHeight / 0.9);
        gp.setYRange(new Range(-yMax, yMax));
        instructions.put(DrawingInstruction.AXIS_RANGE, new AxisRange(xMin, xMax, -yMax, yMax));

        ColourAccumulator accumulator = new ColourAccumulator(cs);
        List<Rectangle2D> insertions = new ArrayList<Rectangle2D>();

        double unitHeight = gp.getUnitHeight();
        double unitWidth =  gp.getUnitWidth();
        double axis = gp.transformYPos(0.0);

        for (Pileup p : pileups) {

            if (p.getTotalCoverage(null) > 0) {
                VariantType snpNuc = null;
                double bottom = axis;
                double top = axis;
                double x = gp.transformXPos(p.getPosition());
                double h = unitHeight * p.getTotalCoverage(null);

                VariantType genomeNuc = null;
                if (genome.isSequenceSet()) {
                    genomeNuc = Pileup.getVariantType((char)refSeq[p.getPosition() - xMin]);
                    snpNuc = genomeNuc;
                }

                if (p.getTotalCoverage(null) > p.getCoverage(genomeNuc, null)) {
                    // Only record a shape if we have at least some mismatches.
                    recordToShapeMap.put(new PileupRecord(p, true), new Rectangle2D.Double(x, bottom - unitHeight * p.getTotalCoverage(Strand.FORWARD), unitWidth, h));
                }

                while ((genome.isSequenceSet() && (snpNuc = p.getLargestVariantType(genomeNuc)) != null) || ((snpNuc = p.getLargestVariantType(null)) != null)) {

                    int forwardCoverage = p.getCoverage(snpNuc, Strand.FORWARD);
                    int reverseCoverage = p.getCoverage(snpNuc, Strand.REVERSE);

                    ColourKey col = getSubPileColour(snpNuc, genomeNuc);
                    if (forwardCoverage > 0) {
                        h = unitHeight * forwardCoverage;
                        Rectangle2D rect = new Rectangle2D.Double(x, bottom - h, unitWidth, h);
                        accumulator.addShape(col == ColourKey.REVERSE_STRAND ? ColourKey.FORWARD_STRAND : col, rect);
                        if (snpNuc == VariantType.INSERTION) {
                            insertions.add(rect);
                        } else {
                            bottom -= h;
                        }
                    }
                    if (reverseCoverage > 0) {
                        h = unitHeight * reverseCoverage;
                        Rectangle2D rect = new Rectangle2D.Double(x, top, unitWidth, h);
                        accumulator.addShape(col, rect);
                        if (snpNuc == VariantType.INSERTION) {
                            insertions.add(rect);
                        } else {
                            top += h;
                        }
                    }

                    p.clearVariantType(snpNuc);
                }
            }
        }

        accumulator.fill(g2);
        for (Rectangle2D ins: insertions) {
            drawInsertion(g2, ins.getX(), ins.getY(), ins.getWidth(), ins.getHeight());
        }

        g2.setColor(Color.BLACK);
        g2.draw(new Line2D.Double(0, axis, gp.getWidth(), axis));
    }

    private void updatePileupsFromSAMRecord(List<Pileup> pileups, SAMRecord samRecord, int startPosition) {

        // the read sequence
        byte[] readBases = samRecord.getReadBases();

        // Return if no bases (can't be used for SNP calling)
        if (readBases.length == 0) {
            return;
        }

        Strand strand = samRecord.getReadNegativeStrandFlag() ? Strand.REVERSE : Strand.FORWARD;

        int alignmentStart = samRecord.getAlignmentStart();

        // the reference sequence
        //byte[] refSeq = genome.getRecords(new Range(alignmentStart, alignmentEnd)).getBytes();

        // get the cigar object for this alignment
        Cigar cigar = samRecord.getCigar();
        byte[] baseQualities = samRecord.getBaseQualities();

        // set cursors for the reference and read
        int sequenceCursor = alignmentStart;
        int readCursor = alignmentStart;

        // consider each cigar element
        for (CigarElement cigarElement : cigar.getCigarElements()) {

            int operatorLength = cigarElement.getLength();
            CigarOperator operator = cigarElement.getOperator();

            switch (operator) {
                case D:
                    // Deletion
                    for (int i = 0; i < operatorLength; i++) {
                        int j = i + sequenceCursor - startPosition;
                        if (j >= 0 && j < pileups.size()) {
                            Pileup p = pileups.get(j);
                            p.pileOn(VariantType.DELETION, baseQualities[readCursor - alignmentStart + i], strand);
                        }
                    }
                    break;
                case I:
                    // Insertion
                    int insPos = sequenceCursor - startPosition;
                    if (insPos >= 0 && insPos < pileups.size()) {
                        Pileup p = pileups.get(insPos);
                        p.pileOn(VariantType.INSERTION, readCursor - alignmentStart, strand);
                    }
                    break;
                case M:
                    // Match or mismatch; we're only interested in mismatches.
                    for (int i = 0; i < operatorLength; i++) {
                        int readIndex = readCursor - alignmentStart + i;

                        VariantType readN = Pileup.getVariantType((char)readBases[readIndex]);

                        int j = i + sequenceCursor - startPosition;
                        if (j >= 0 && j < pileups.size()) {
                            Pileup p = pileups.get(j);
                            p.pileOn(readN, baseQualities[readIndex], strand);
                        }
                    }
                    break;
                case N: // Skipped
                case P: // Padding
                case H: // Hard clip
                case S: // Soft clip
                    break;
            }

            if (operator.consumesReadBases()) {
                readCursor += operatorLength;
            }
            if (operator.consumesReferenceBases()) {
                sequenceCursor += operatorLength;
            }
        }
    }

    public void renderReadsFromArc(Graphics2D g2, GraphPaneAdapter gp, BAMIntervalRecord rec1, BAMIntervalRecord rec2, Range range) {

        int readHeight = gp.getParentFrame().getIntervalHeight();

        renderRead(g2, gp, rec1, 0, range, readHeight);

        if (rec2 != null) {
            renderRead(g2, gp, rec2, 0, range, readHeight);
        }
    }

    private void renderStandardPairedMode(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        AxisRange axisRange = (AxisRange) instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme) instructions.get(DrawingInstruction.COLOUR_SCHEME);
        Color linecolor = cs.getColor(ColourKey.INTERVAL_LINE);
        Range range = axisRange.getXRange();
        int effectiveEnd = range.getTo() + 1;
        int effectiveStart = range.getFrom() - 1;

        gp.setXRange(axisRange.getXRange());

        Map<Integer, ArrayList<BAMIntervalRecord>> mateQueue = new HashMap<Integer, ArrayList<BAMIntervalRecord>>();
        ArrayList<ArrayList<Interval>> levels = new ArrayList<ArrayList<Interval>>();
        levels.add(new ArrayList<Interval>());
        ArrayList<DrawStore> savedDraws = new ArrayList<DrawStore>();

        for (int i = 0; i < data.size(); i++) {

            BAMIntervalRecord bamRecord = (BAMIntervalRecord)data.get(i);
            Interval interval = bamRecord.getInterval();
            SAMRecord samRecord = bamRecord.getSAMRecord();
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
        for (DrawStore drawStore : savedDraws) {
            Shape readshape = renderRead(g2, gp, drawStore.intervalRecord, drawStore.level, range, gp.getUnitHeight());
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
        for (int i = 0; i < records.size(); i++) {
            SAMRecord samRecord2 = ((BAMIntervalRecord)records.get(i)).getSAMRecord();
            if (MiscUtils.isMate(samRecord, samRecord2, false)) {
                BAMIntervalRecord intervalRecord = records.get(i);
                records.remove(i);
                return intervalRecord;
            }
        }
        return null;
    }

    /**
     * Connect intervals i1 and i2 with a dashed line to show mates.
     */
    private void connectPiledInterval(Graphics2D g2, GraphPaneAdapter gp, Interval i1, Interval i2, int level, Color linecolor, IntervalRecord ir1, IntervalRecord ir2) {
        Interval mateInterval = computeMateInterval(i1, i2);

        Stroke currentStroke = g2.getStroke();
        //Stroke drawingStroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        Stroke drawingStroke = new BasicStroke(2);
        double yPos = gp.transformYPos(0) - (level + 1)*gp.getUnitHeight() + gp.getUnitHeight()/2.0 - gp.getOffset();

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

    /**
     * Determine at what level a piled interval should be drawn.
     */
    private int computePiledIntervalLevel(ArrayList<ArrayList<Interval>> levels, Interval interval) {
        ArrayList<Interval> level;
        for (int i = 0; i < levels.size(); i++) {
            level = levels.get(i);
            boolean conflict = false;
            for (Interval current : level) {
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

    /**
     * Store information for drawing a read so that it can be done later.
     * Used for piled interval mode
     */
    private class DrawStore {

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

    @Override
    public Dimension getLegendSize(DrawingMode mode) {
        switch (mode) {
            case STANDARD:
            case SEQUENCE:
                return new Dimension(168, LEGEND_LINE_HEIGHT * 2 + 6);
            case MISMATCH:
            case STANDARD_PAIRED:
                return new Dimension(168, LEGEND_LINE_HEIGHT * 4 + 6);
            case ARC_PAIRED:
                // Five lines, but not as wide as the others.
                return new Dimension(125, LEGEND_LINE_HEIGHT * 5 + 6);
            case SNP:
                return new Dimension(168, LEGEND_LINE_HEIGHT * 2 + 6);
            case STRAND_SNP:
                // A little wider because "Forward Strand" and "Reverse Strand" are on the same line.
                return new Dimension(180, LEGEND_LINE_HEIGHT * 3 + 6);
            default:
                return null;
        }
    }

    @Override
    public void drawLegend(Graphics2D g2, DrawingMode mode) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = 6, y = 17;
        switch (mode) {
            case STANDARD:
                drawStrandLegends(g2, x, y);
                break;
            case MISMATCH:
            case STANDARD_PAIRED:
                drawStrandLegends(g2, x, y);
                y += LEGEND_LINE_HEIGHT * 2;
                drawBaseLegendExtended(g2, x, y, ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.SKIPPED);
                break;
            case SEQUENCE:
                drawBaseLegendExtended(g2, x, y, ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.SKIPPED);
                break;
            case ARC_PAIRED:
                drawSimpleLegend(g2, 30, 15, ColourKey.CONCORDANT_LENGTH, ColourKey.DISCORDANT_LENGTH, ColourKey.ONE_READ_INVERTED, ColourKey.EVERTED_PAIR, ColourKey.UNMAPPED_MATE);
                break;
            case SNP:
                drawBaseLegendExtended(g2, x, y, ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T);
                break;
            case STRAND_SNP:
                drawBaseLegendExtended(g2, x, y, ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T);
                y += LEGEND_LINE_HEIGHT * 2;
                drawBaseLegend(g2, x, y, ColourKey.FORWARD_STRAND);
                x += 90;
                drawBaseLegend(g2, x, y, ColourKey.REVERSE_STRAND);
                break;
            default:
                break;
        }
    }

    /**
     * Draw two read-shapes: one for forward and one for reverse.
     */
    private void drawStrandLegends(Graphics2D g2, int x, int y) {
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        Shape pointyBar = getPointyBar(false, x, y - 12, 36.0, 12.0);
        g2.setColor(cs.getColor(ColourKey.FORWARD_STRAND));
        g2.fill(pointyBar);
        g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
        g2.draw(pointyBar);

        pointyBar = getPointyBar(true, x, y - 12 + LEGEND_LINE_HEIGHT, 36.0, 12.0);
        g2.setColor(cs.getColor(ColourKey.REVERSE_STRAND));
        g2.fill(pointyBar);
        g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
        g2.draw(pointyBar);

        g2.setColor(Color.BLACK);
        g2.setFont(LEGEND_FONT);
        g2.drawString(ColourKey.FORWARD_STRAND.getName(), x + 45, y);
        g2.drawString(ColourKey.REVERSE_STRAND.getName(), x + 45, y + LEGEND_LINE_HEIGHT);
    }

    /**
     * Draw the legend for bases, but also the entries for insertions and deletions.
     */
    private void drawBaseLegendExtended(Graphics2D g2, int x, int y, ColourKey... keys) {
        drawBaseLegend(g2, x, y, keys);

        y += LEGEND_LINE_HEIGHT;
        g2.setColor(Color.BLACK);
        g2.fillRect(x, y - SWATCH_SIZE.height + 2, SWATCH_SIZE.width, SWATCH_SIZE.height);
        g2.setColor(Color.BLACK);
        g2.drawString("Deletion", x + SWATCH_SIZE.width + 3, y);
        x += 66;
        Shape s = drawInsertion(g2, x, y - SWATCH_SIZE.height + 2, 12.0, SWATCH_SIZE.height);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(0.25f));
        g2.draw(s);
        g2.drawString("Insertion", x + 12, y);
    }
}
