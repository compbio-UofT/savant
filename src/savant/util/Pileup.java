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

import savant.api.data.Strand;
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

    public void pileOn(VariantType n, double quality, Strand strand) {
        if (n != VariantType.NONE) {
            coverage.get(n).pileOn(quality, strand);
        }
    }

    /**
     * When rendering the SNP and Strand SNP modes, we start with the biggest variant
     * near the axis.
     */
    public VariantType getLargestVariantType(VariantType notThis) {
        VariantType[] values = { VariantType.SNP_A, VariantType.SNP_C, VariantType.SNP_G, VariantType.SNP_T, VariantType.DELETION, VariantType.INSERTION, VariantType.OTHER };

        VariantType snpNuc = null;

        for (VariantType n : values) {
            if (n != notThis) {
                if (getCoverage(n, null) > 0 && (snpNuc == null || getCoverage(n, null) > getCoverage(snpNuc, null))){
                    snpNuc = n;
                }
            }
        }

        return snpNuc;
    }

    public void clearVariantType(VariantType n) {
        coverage.put(n, new Coverage());
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

    public int getCoverage(VariantType n, Strand strand) {
        Coverage cov = coverage.get(n);
        if (strand == Strand.FORWARD) {
            return cov.forwardCount;
        } else if (strand == Strand.REVERSE) {
            return cov.reverseCount;
        } else {
            return cov.count;
        }
    }

    public int getTotalCoverage(Strand strand) {
        int total = 0;
        if (strand == Strand.FORWARD) {
            for (Coverage cov: coverage.values()) {
                total += cov.forwardCount;
            }
        } else if (strand == Strand.REVERSE) {
            for (Coverage cov: coverage.values()) {
                total += cov.reverseCount;
            }
        } else {
            for (Coverage cov: coverage.values()) {
                total += cov.count;
            }
        }
        return total;
    }

    public double getAverageQuality(VariantType n, Strand strand) {
        Coverage cov = coverage.get(n);
        if (strand == Strand.FORWARD) {
            return cov.forwardSum / cov.forwardCount;
        } else if (strand == Strand.REVERSE) {
            return cov.reverseSum / cov.reverseCount;
        } else {
            return cov.sum / cov.count;
        }
    }

    private class Coverage {
        int count = 0, forwardCount = 0, reverseCount = 0;
        double sum = 0.0, forwardSum = 0.0, reverseSum = 0.0;
        
        void pileOn(double quality, Strand strand) {
            count++;
            sum += quality;
            if (strand == Strand.FORWARD) {
                forwardCount++;
                forwardSum += quality;
            } else if (strand == Strand.REVERSE) {
                reverseCount++;
                reverseSum += quality;
            }
        }
    }
}
