/*
 *    Copyright 2011 University of Toronto
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

package savant.amino;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.TrackAdapter;
import savant.api.util.GenomeUtils;
import savant.api.util.NavigationUtils;
import savant.api.util.RangeUtils;
import savant.data.types.Block;
import savant.data.types.Record;
import savant.data.types.RichIntervalRecord;
import savant.data.types.Strand;

/**
 * Semi-transparent panel which is drawn over the layered-panel.
 *
 * @author tarkvara
 */
public class AminoCanvas extends JPanel {
    private static final Log LOG = LogFactory.getLog(AminoCanvas.class);

    /** The plugin we belong to. */
    AminoPlugin plugin;

    /** The track for which we are rendering amino acids. */
    TrackAdapter track;

    /** The sequence underlying the record being processed. */
    byte[] sequence;

    /** Start of the thick section of the record being processed. */
    int thickStart;

    public AminoCanvas(AminoPlugin p, TrackAdapter t) {
        plugin = p;
        track = t;
        setOpaque(false);
    }

    @Override
    public void paintComponent(Graphics g) {
        if (GenomeUtils.getGenome().isSequenceSet()) {

            double aminoWidth = track.transformXPos(3) - track.transformXPos(0);
            if (aminoWidth > 0.5) {

                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // We'll be drawing labels if a 'W' (tryptophan) will fit into the space of 3 bases.
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 8));
                boolean labelled = g2.getFontMetrics().charWidth('W') < aminoWidth;


                try {
                    List<Record> records = track.getDataInRange();
                    if (records != null) {
                        for (Record r: records) {
                            RichIntervalRecord rr = (RichIntervalRecord)r;
                            int recordStart = rr.getInterval().getStart();
                            thickStart = rr.getThickStart();
                            int thickEnd = rr.getThickEnd() + 1;
                            LOG.debug(rr.getAlternateName() + ": thickStart=" + thickStart + ", thickEnd=" + thickEnd);

                            if (thickEnd > thickStart) {
                                sequence = GenomeUtils.getGenome().getSequence(NavigationUtils.getCurrentReferenceName(), RangeUtils.createRange(thickStart, thickEnd));

                                int pos = thickStart;
                                int leftovers = -1;    // Left-overs from the previous block.
                                List<Block> blocks = rr.getBlocks();
                                if (blocks != null) {
                                    for (Block b: blocks) {

                                        if (pos + 3 <= thickEnd) {
                                            // Block positions are relative to the start of the record.
                                            int blockStart = b.getPosition() + recordStart;
                                            int blockEnd = b.getEnd() + recordStart;
                                            LOG.debug(rr.getAlternateName() + ": blockStart=" + blockStart + ", blockEnd=" + blockEnd);

                                            AminoAcid a;

                                            // If we have leftovers, take care of them first.
                                            switch (leftovers) {
                                                case -1:
                                                    // Fresh record with no leftovers.
                                                    break;
                                                case 0:
                                                    // No leftovers, so we can start immediately on the new block.
                                                    pos = blockStart;
                                                    if (pos < thickStart) {
                                                        pos = thickStart;
                                                    }
                                                    break;
                                                case 1:
                                                    // One base from previous block, two bases from current one.
                                                    LOG.debug(rr.getAlternateName() + ": handling leftover " + getBase(pos) + " at " + pos);
                                                    if (rr.getStrand() == Strand.FORWARD) {
                                                        a = AminoAcid.lookup(getBase(pos), getBase(blockStart), getBase(blockStart + 1));
                                                    } else {
                                                        a = AminoAcid.lookup(getComplement(blockStart + 1), getComplement(blockStart), getComplement(pos));
                                                    }
                                                    paintAminoAcid(g2, a, pos, 1, pos, labelled);
                                                    paintAminoAcid(g2, a, blockStart, 2, blockStart - 1, labelled);
                                                    pos = blockStart + 2;
                                                    break;
                                                case 2:
                                                    // Two bases from previous block, one base from current one.
                                                    LOG.debug(rr.getAlternateName() + ": handling leftover " + getBase(pos) + "," + getBase(pos + 1) + " at " + pos + "," + (pos + 1));
                                                    if (rr.getStrand() == Strand.FORWARD) {
                                                        a = AminoAcid.lookup(getBase(pos), getBase(pos + 1), getBase(blockStart));
                                                    } else {
                                                        a = AminoAcid.lookup(getComplement(blockStart), getComplement(pos + 1), getComplement(pos));
                                                    }
                                                    paintAminoAcid(g2, a, pos, 2, pos, labelled);
                                                    paintAminoAcid(g2, a, blockStart, 1, blockStart - 2, labelled);
                                                    pos = blockStart + 1;
                                                    break;
                                            }

                                            // Now, handle codons which are entirely contained within the block.
                                            while (pos + 3 <= blockEnd && pos + 3 <= thickEnd) {
                                                if (rr.getStrand() == Strand.FORWARD) {
                                                    a = AminoAcid.lookup(getBase(pos), getBase(pos + 1), getBase(pos + 2));
                                                } else {
                                                    a = AminoAcid.lookup(getComplement(pos + 2), getComplement(pos + 1), getComplement(pos));
                                                }
                                                paintAminoAcid(g2, a, pos, 3, pos, labelled);
                                                pos += 3;
                                            }
                                            leftovers = (blockEnd - pos) % 3;
                                            LOG.debug(rr.getAlternateName() + ": breaking out of loop: pos=" + pos + ", blockEnd=" + blockEnd + ", leftovers=" + leftovers);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException x) {
                    LOG.info("Unable to retrieve sequence.", x);
                }
            }
        }
    }

    private void paintAminoAcid(Graphics2D g2, AminoAcid a, int pos, int bases, int labelPos, boolean labelled) {
        if (a != null) {
            g2.setColor(new Color(a.color.getRed(), a.color.getGreen(), a.color.getBlue(), plugin.getAlpha()));
            double x0 = track.transformXPos(pos);
            double x1 = track.transformXPos(pos + bases);
            g2.fill(new Rectangle2D.Double(x0, 0.0, x1 - x0, getHeight()));
            if (labelled) {
                g2.setColor(a == AminoAcid.STOP ? Color.WHITE : Color.BLACK);
                double charWidth = g2.getFontMetrics().charWidth(a.code);
                g2.drawString(Character.toString(a.code), (float)(track.transformXPos(labelPos) + track.transformXPos(labelPos + 3) - charWidth) * 0.5F, getHeight() * 0.5F);
            }
        }
    }

    /**
     * Get the base at the given position within the sequence (relative to the start of the chromosome).
     */
    private char getBase(int pos) {
        return (char)sequence[pos - thickStart];
    }

    /**
     * Get the complement of the base at the given position within the sequence (relative to the start of the chromosome).
     */
    private char getComplement(int pos) {
        switch (getBase(pos)) {
            case 'T':
                return 'A';
            case 'A':
                return 'T';
            case 'C':
                return 'G';
            case 'G':
                return 'C';
            default:
                return 'N';
        }
    }
}
