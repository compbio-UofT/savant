/*
 *    Copyright 2012 University of Toronto
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
