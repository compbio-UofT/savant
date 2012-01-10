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

package savant.util;

import java.util.EnumMap;
import java.util.Map;

import savant.api.data.VariantType;


/**
 * Keeps track of statistics when computing SNP and Strand SNP modes.
 *
 * @author mfiume
 */
public final class Pileup {

    private int position;

    private Map<VariantType, Coverage> coverage = new EnumMap<VariantType, Coverage>(VariantType.class);

    public Pileup(int position) {
        this.position = position;
        clearVariantType(VariantType.SNP_A);
        clearVariantType(VariantType.SNP_C);
        clearVariantType(VariantType.SNP_G);
        clearVariantType(VariantType.SNP_T);
        clearVariantType(VariantType.DELETION);
        clearVariantType(VariantType.INSERTION);
        clearVariantType(VariantType.OTHER);
    }

    public int getPosition() {
        return position;
    }

    public void pileOn(VariantType n, double quality, boolean strand) {
        coverage.get(n).pileOn(quality, strand);
    }

    /**
     * When rendering the SNP and Strand SNP modes, we start with the biggest variant
     * near the axis.
     */
    public VariantType getLargestVariantType(VariantType notThis) {
        VariantType[] nucs = { VariantType.SNP_A, VariantType.SNP_C, VariantType.SNP_G, VariantType.SNP_T, VariantType.DELETION, VariantType.INSERTION };

        VariantType snpNuc = null;

        for (VariantType n : nucs) {
            if(n == notThis) continue;
            if(this.getCoverage(n) > 0 && (snpNuc == null || this.getCoverage(n) > this.getCoverage(snpNuc))){
                snpNuc = n;
            }
        }

        return snpNuc;
    }

    public void clearVariantType(VariantType n) {
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

    public static VariantType getVariantType(char c) {
        switch (c) {
            case 'A':
            case 'a':
                return VariantType.SNP_A;
            case 'C':
            case 'c':
                return VariantType.SNP_C;
            case 'G':
            case 'g':
                return VariantType.SNP_G;
            case 'T':
            case 't':
                return VariantType.SNP_T;
            default:
                return VariantType.OTHER;
        }
    }

    public double getCoverage(VariantType n) {
        return coverage.get(n).total;
    }
    
        
    public double getStrandCoverage(VariantType n, boolean reverse) {
        return coverage.get(n).strand[reverse ? 1 : 0];
    }

    public double getCoverageProportion(VariantType n) {
        return getCoverage(n) / getTotalCoverage();
    }

    private class Coverage {
        double total = 0.0;
        double[] strand = { 0.0, 0.0 };
        
        void pileOn(double value, boolean reverse) {
            total += value;
            strand[reverse ? 1 : 0] += value;
        }
    }
}
