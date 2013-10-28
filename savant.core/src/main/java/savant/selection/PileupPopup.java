/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.selection;

import javax.swing.JLabel;

import savant.api.data.Strand;
import savant.data.types.PileupRecord;
import savant.util.DrawingMode;


/**
 * Panel which displays limited information about the SNP under the mouse.
 *
 * @author tarkvara
 */
public class PileupPopup extends PopupPanel {

    PileupPopup() {
        start = end = -1;
    }

    @Override
    protected void initInfo() {
        PileupRecord rec = (PileupRecord)record;
        add(new JLabel("Position: " + rec.getPosition()));
        if (mode == DrawingMode.STRAND_SNP) {
            add(new JLabel("Strand +"));
            addStrand(rec, Strand.FORWARD, "   ");
            add(new JLabel("Strand -"));
            addStrand(rec, Strand.REVERSE, "   ");
        } else {
            addStrand(rec, null, "");
        }
    }

    /**
     * Suppress initStandardButtons because SNP tracks don't want to be selectable or bookmarkable.
     */
    @Override
    protected void initStandardButtons() {
    }

    private void addStrand(PileupRecord rec, Strand strand, String indent) {
        int[] coverage = rec.getCoverage(strand);
        double[] percentage = rec.getPercentage(strand);
        double[] quality = rec.getAverageQuality(strand);
        for (int i = 0; i < coverage.length; i++) {
            if (coverage[i] > 0) {
                add(new JLabel(String.format("%s%s: %d (%.1f%%) (avg. BQ %.1f)", indent, PileupRecord.NUCLEOTIDE_NAMES[i], coverage[i], percentage[i], quality[i])));
            }
        }
    }
}
