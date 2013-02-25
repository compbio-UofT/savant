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

package savant.data.types;

import savant.api.data.Record;
import savant.api.data.Strand;
import savant.api.data.VariantType;
import savant.util.Pileup;

/**
 * Pseudo-record which stores pileup information.  We want this to look like a Record so
 * that we can use it as the source for a PileupPopup.
 *
 * @author tarkvara
 */
public class PileupRecord implements Record {
    /** Names to be displayed in popup.  Must be in same order as enum values in VariantType. */
    public static final String[] NUCLEOTIDE_NAMES = { "A", "C", "G", "T", "Deletion", "Insertion", "Other" };

    int position;
    int coverage[][];
    double percentage[][];
    double quality[][];

    public PileupRecord(Pileup p, boolean stranded) {
        position = p.getPosition();
        if (stranded) {
            coverage = new int[2][7];
            percentage = new double[2][7];
            quality = new double[2][7];
            double denominator0 = p.getTotalCoverage(Strand.FORWARD) * 0.01;
            double denominator1 = p.getTotalCoverage(Strand.REVERSE) * 0.01;
            int i = 0;
            for (VariantType nuc: VariantType.values()) {
                if (nuc != VariantType.NONE) {
                    coverage[0][i] = p.getCoverage(nuc, Strand.FORWARD);
                    percentage[0][i] = coverage[0][i] / denominator0;
                    quality[0][i] = p.getAverageQuality(nuc, Strand.FORWARD);
                    coverage[1][i] = p.getCoverage(nuc, Strand.REVERSE);
                    percentage[1][i] = coverage[1][i] / denominator1;
                    quality[1][i] = p.getAverageQuality(nuc, Strand.REVERSE);
                    i++;
                }
            }
        } else {
            coverage = new int[1][7];
            percentage = new double[1][7];
            quality = new double[1][7];
            double denominator = p.getTotalCoverage(null) * 0.01;
            int i = 0;
            for (VariantType nuc: VariantType.values()) {
                if (nuc != VariantType.NONE) {
                    coverage[0][i] = p.getCoverage(nuc, null);
                    percentage[0][i] = coverage[0][i] / denominator;
                    quality[0][i] = p.getAverageQuality(nuc, null);
                    i++;
                }
            }
        }
    }

    @Override
    public int compareTo(Object o) {
        PileupRecord that = (PileupRecord) o;

        // References are equal; compare pposition
        if (getPosition() == that.getPosition()){
            return 0;
        } else if (getPosition() < that.getPosition()){
            return -1;
        } else {
            return 1;
        }

    }
    
    /**
     * PileupRecords are transient things which only ever exist for the current reference.
     */
    @Override
    public String getReference() {
        return null;
    }

    public int getPosition() {
        return position;
    }
    
    /**
     * Retrieve an array of 7 nucleotide coverage values for the given strand
     * @param strand FORWARD, REVERSE, or null
     */
    public int[] getCoverage(Strand strand) {
        return coverage[strand == Strand.REVERSE ? 1 : 0];
    } 
    
    /**
     * Retrieve an array of 7 nucleotide coverage values for the given strand
     * @param strand FORWARD, REVERSE, or null
     */
    public double[] getPercentage(Strand strand) {
        return percentage[strand == Strand.REVERSE ? 1 : 0];
    } 
    
    /**
     * Retrieve an array of 7 quality values for the given strand
     * @param strand FORWARD, REVERSE, or null
     */
    public double[] getAverageQuality(Strand strand) {
        return quality[strand == Strand.REVERSE ? 1 : 0];
    } 
}
