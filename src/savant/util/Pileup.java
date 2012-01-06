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

package savant.util;

import java.util.EnumMap;
import java.util.Map;

/**
 * Keeps track of statistics when computing SNP and Strand SNP modes.
 *
 * @author mfiume
 */
public final class Pileup {

    private int position;

    private Map<Nucleotide, Coverage> coverage = new EnumMap<Nucleotide, Coverage>(Nucleotide.class);

    public Pileup(int position) {
        this.position = position;
        clearNucleotide(Nucleotide.A);
        clearNucleotide(Nucleotide.C);
        clearNucleotide(Nucleotide.G);
        clearNucleotide(Nucleotide.T);
        clearNucleotide(Nucleotide.DELETION);
        clearNucleotide(Nucleotide.INSERTION);
        clearNucleotide(Nucleotide.OTHER);
    }

    public int getPosition() {
        return position;
    }

    public void pileOn(Nucleotide n, double quality, boolean strand) {
        coverage.get(n).pileOn(quality, strand);
    }

    /**
     * When rendering the SNP and Strand SNP modes, we start with the biggest variant
     * near the axis.
     */
    public Nucleotide getLargestNucleotide(Nucleotide notThis) {
        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T, Nucleotide.DELETION, Nucleotide.INSERTION };

        Nucleotide snpNuc = null;

        for (Nucleotide n : nucs) {
            if(n == notThis) continue;
            if(this.getCoverage(n) > 0 && (snpNuc == null || this.getCoverage(n) > this.getCoverage(snpNuc))){
                snpNuc = n;
            }
        }

        return snpNuc;
    }

    public void clearNucleotide(Nucleotide n) {
        coverage.put(n, new Coverage());
    }

    public double getTotalCoverage() {
        double total = 0.0;
        for (Coverage cov: coverage.values()) {
            total += cov.total;
        }
        return total;
    }
    
    public double getTotalStrandCoverage(boolean reverse) {
        double total = 0.0;
        for (Coverage cov: coverage.values()) {
            total += cov.strand[reverse ? 1 : 0];
        }
        return total;
    }

    public static Nucleotide getNucleotide(char c) {
        switch (c) {
            case 'A':
            case 'a':
                return Nucleotide.A;
            case 'C':
            case 'c':
                return Nucleotide.C;
            case 'G':
            case 'g':
                return Nucleotide.G;
            case 'T':
            case 't':
                return Nucleotide.T;
            default:
                return Nucleotide.OTHER;
        }
    }

    public double getCoverage(Nucleotide n) {
        return coverage.get(n).total;
    }
    
        
    public double getStrandCoverage(Nucleotide n, boolean reverse) {
        return coverage.get(n).strand[reverse ? 1 : 0];
    }

    public double getCoverageProportion(Nucleotide n) {
        return getCoverage(n) / getTotalCoverage();
    }

    public enum Nucleotide { A, C, G, T, INSERTION, DELETION, OTHER; }

    private class Coverage {
        double total = 0.0;
        double[] strand = { 0.0, 0.0 };
        
        void pileOn(double value, boolean reverse) {
            total += value;
            strand[reverse ? 1 : 0] += value;
        }
    }
}
