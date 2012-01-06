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
import savant.util.Pileup;
import savant.util.Pileup.Nucleotide;

/**
 * Pseudo-record which stores pileup information.  We want this to look like a Record so
 * that we can use it as the source for a PileupPopup.
 *
 * @author tarkvara
 */
public class PileupRecord implements Record {
    /** Names to be displayed in popup.  Must be in same order as enum values in Pileup.Nucleotide. */
    public static final String[] NUCLEOTIDE_NAMES = { "A", "C", "G", "T", "Insertion", "Deletion", "Other" };

    int position;
    int coverage[][];
    double percentage[][];

    public PileupRecord(Pileup p, boolean stranded) {
        position = p.getPosition();
        if (stranded) {
            coverage = new int[2][7];
            percentage = new double[2][7];
            double denominator0 = p.getTotalStrandCoverage(true) * 0.01;
            double denominator1 = p.getTotalStrandCoverage(false) * 0.01;
            int i = 0;
            for (Nucleotide nuc: Nucleotide.values()) {
                coverage[0][i] = (int)p.getStrandCoverage(nuc, true);
                percentage[0][i] = coverage[0][i] / denominator0;
                coverage[1][i] = (int)p.getStrandCoverage(nuc, false);
                percentage[1][i] = coverage[1][i] / denominator1;
                i++;
            }
        } else {
            coverage = new int[1][7];
            percentage = new double[1][7];
            double denominator = p.getTotalCoverage() * 0.01;
            int i = 0;
            for (Nucleotide nuc: Nucleotide.values()) {
                coverage[0][i] = (int)p.getCoverage(nuc);
                percentage[0][i] = coverage[0][i] / denominator;
                i++;
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
     * @param strand 0=forward or unstranded; 1=reverse
     */
    public int[] getCoverage(int strand) {
        return coverage[strand];
    } 
    
    /**
     * Retrieve an array of 7 nucleotide coverage values for the given strand
     * @param strand 0=reverse or unstranded; 1=forward
     */
    public double[] getPercentage(int strand) {
        return percentage[strand];
    } 
}
