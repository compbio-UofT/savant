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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mfiume
 */
class Pileup {

    private Nucleotide referenceNucleotide;
    private Nucleotide snpNucleotide;

    private int position;

    private String trackName;

    //private int totalquality = 0;
    //private List<Double> coverageA = new ArrayList<Double>();
    private List<Double> coverageA1 = new ArrayList<Double>();
    private List<Double> coverageA2 = new ArrayList<Double>();
    private double totalCoverageA = 0;
    private double totalCoverageA1 = 0;
    private double totalCoverageA2 = 0;
    
    //private double a_quality = 0;
    //private List<Double> coverageC = new ArrayList<Double>();
    private List<Double> coverageC1 = new ArrayList<Double>();
    private List<Double> coverageC2 = new ArrayList<Double>();
    private double totalCoverageC = 0;
    private double totalCoverageC1 = 0;
    private double totalCoverageC2 = 0;
    //private double c_quality = 0;
    //private List<Double> coverageT = new ArrayList<Double>();
    private List<Double> coverageT1 = new ArrayList<Double>();
    private List<Double> coverageT2 = new ArrayList<Double>();
    private double totalCoverageT = 0;
    private double totalCoverageT1 = 0;
    private double totalCoverageT2 = 0;
    //private double t_quality = 0;
    //private List<Double> coverageG = new ArrayList<Double>();
    private List<Double> coverageG1 = new ArrayList<Double>();
    private List<Double> coverageG2 = new ArrayList<Double>();
    private double totalCoverageG = 0;
    private double totalCoverageG1 = 0;
    private double totalCoverageG2 = 0;
    //private double g_quality = 0;
    //private List<Double> coverageOther = new ArrayList<Double>();
    private List<Double> coverageOther1 = new ArrayList<Double>();
    private List<Double> coverageOther2 = new ArrayList<Double>();
    private double totalCoverageOther = 0;
    private double totalCoverageOther1 = 0;
    private double totalCoverageOther2 = 0;
    //private double other_quality = 0;

    public Pileup(String trackName, int position, Nucleotide n) {
        this.trackName = trackName;
        this.position = position;
        this.referenceNucleotide = n;
    }

    public Pileup(int position){
        this.trackName = null;
        this.position = position;
        this.referenceNucleotide = null;
    }

    public String getTrackName() {
        return this.trackName;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) { return true; }
        if (!(o instanceof Pileup)) { return false; }

        Pileup p = (Pileup) o;
        if (p.getPosition() == this.getPosition() &&
                p.getTrackName().equals(this.getTrackName()) &&
                p.getReferenceNucleotide() == this.getReferenceNucleotide() &&
                p.getSNPNucleotide() == this.getSNPNucleotide()) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.referenceNucleotide.hashCode();
        hash = 89 * hash + this.snpNucleotide.hashCode();
        hash = 89 * hash + this.position;
        hash = 89 * hash + (this.trackName != null ? this.trackName.hashCode() : 0);
        return hash;
    }

    public void pileOn(char c) { pileOn(c,1.0); }

    public void pileOn(Nucleotide n) { pileOn(n,1.0,true); }

    public void pileOn(char c, double quality) { pileOn(getNucleotide(c),quality, true); }

    public void pileOn(Nucleotide n, boolean strand) { pileOn(n, 1.0, strand); }
    
    public void pileOn(Nucleotide n, double quality, boolean strand) {
        double coverage = 1.0 * quality;

        //System.out.println("(P) " + n + " @ " + this.getPosition());

        switch(n) {
            case A:
                //coverageA.add(coverage);
                if(strand){
                    coverageA1.add(coverage);
                    totalCoverageA1 += coverage;
                } else {
                    coverageA2.add(coverage);
                    totalCoverageA2 += coverage;
                }
                totalCoverageA += coverage;
                break;
            case C:
                //coverageC.add(coverage);
                if(strand){
                    coverageC1.add(coverage);
                    totalCoverageC1 += coverage;
                } else {
                    coverageC2.add(coverage);
                    totalCoverageC2 += coverage;
                }
                totalCoverageC += coverage;
                break;
            case G:
                //coverageA.add(coverage);
                if(strand){
                    coverageG1.add(coverage);
                    totalCoverageG1 += coverage;
                } else {
                    coverageG2.add(coverage);
                    totalCoverageG2 += coverage;
                }
                totalCoverageG += coverage;
                break;
            case T:
                //coverageT.add(coverage);
                if(strand){
                    coverageT1.add(coverage);
                    totalCoverageT1 += coverage;
                } else {
                    coverageT2.add(coverage);
                    totalCoverageT2 += coverage;
                }
                totalCoverageT += coverage;
                break;
            default:
                //coverageOther.add(coverage);
                if(strand){
                    coverageOther1.add(coverage);
                    totalCoverageOther1 += coverage;
                } else {
                    coverageOther2.add(coverage);
                    totalCoverageOther2 += coverage;
                }
                totalCoverageOther += coverage;
                break;
        }


    }

    /*
    public double getConfidence(Nucleotide n) {
        return ((double) this.getAverageQuality(n)) / this.getTotalQuality();
    }
     */

    public Nucleotide getSNPNucleotide() {

        if (snpNucleotide != null) { return snpNucleotide; }

        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T };

        Nucleotide snpNuc = null;

        for (Nucleotide n : nucs) {
            if ((snpNuc == null && n != this.referenceNucleotide) || 
                    (n != this.referenceNucleotide && this.getCoverageProportion(n) > this.getCoverageProportion(snpNuc) )) {
                snpNuc = n;
            }
        }

        snpNucleotide = snpNuc;
        return snpNucleotide;
    }

    public Nucleotide getLargestNucleotide() {
        return getLargestNucleotide(Nucleotide.OTHER);
    }
    
    public Nucleotide getLargestNucleotide(Nucleotide notThis) {
        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T };

        Nucleotide snpNuc = null;

        for (Nucleotide n : nucs) {
            if(n == notThis) continue;
            if(this.getCoverage(n) > 0 && (snpNuc == null || this.getCoverage(n) > this.getCoverage(snpNuc))){
                snpNuc = n;
            }
        }

        return snpNuc;
    }

    public static Nucleotide stringToNuc(String s){
        if(s.equals("A")) return Nucleotide.A;
        else if(s.equals("C")) return Nucleotide.C;
        else if(s.equals("T")) return Nucleotide.T;
        else if(s.equals("G")) return Nucleotide.G;
        else return Nucleotide.OTHER;
    }

    public void clearNucleotide(Nucleotide n){
        switch(n) {
            case A:
                this.totalCoverageA = 0;
                this.totalCoverageA1 = 0;
                this.totalCoverageA2 =0;
                break;
            case C:
                this.totalCoverageC = 0;
                this.totalCoverageC1 = 0;
                this.totalCoverageC2 = 0;
                break;
            case G:
                this.totalCoverageG = 0;
                this.totalCoverageG1 = 0;
                this.totalCoverageG2 = 0;
                break;
            case T:
                this.totalCoverageT = 0;
                this.totalCoverageT1 = 0;
                this.totalCoverageT2 = 0;
                break;
            case OTHER:
                this.totalCoverageOther = 0;
                this.totalCoverageOther1 = 0;
                this.totalCoverageOther2 = 0;
                break;
            default:
                break;
        }
    }

    public double getSNPNucleotideConfidence() {
        return getCoverageProportion(getSNPNucleotide());
    }

    public double getTotalCoverage() {
        double totalcoverage = 0;
        totalcoverage += this.getCoverage(Nucleotide.A);
        totalcoverage += this.getCoverage(Nucleotide.C);
        totalcoverage += this.getCoverage(Nucleotide.G);
        totalcoverage += this.getCoverage(Nucleotide.T);
        return totalcoverage;
    }
    
    public double getTotalStrandCoverage(boolean strand) {
        double totalcoverage = 0;
        totalcoverage += this.getStrandCoverage(Nucleotide.A, strand);
        totalcoverage += this.getStrandCoverage(Nucleotide.C, strand);
        totalcoverage += this.getStrandCoverage(Nucleotide.G, strand);
        totalcoverage += this.getStrandCoverage(Nucleotide.T, strand);
        return totalcoverage;
    }

    /*
    public double getTotalQuality() {
        double totalquality = 0;
        totalquality += this.a_quality;
        totalquality += this.totalCoverageC;
        totalquality += this.totalCoverageT;
        totalquality += this.totalCoverageG;
        return totalquality;
    }
     */

    public static Nucleotide getNucleotide(char c) {
        c = ("" + c).toUpperCase().charAt(0);
        switch(c) {
            case 'A':
                return Nucleotide.A;
            case 'C':
                return Nucleotide.C;
            case 'G':
                return Nucleotide.G;
            case 'T':
                return Nucleotide.T;
            default:
                return Nucleotide.OTHER;
        }
    }

    public int getPosition() {
        return this.position;
    }

    public enum Nucleotide { A, C, G, T, OTHER; }

    public double getCoverage(Nucleotide n) {
        switch(n) {
            case A:
               return this.totalCoverageA;
            case C:
                return this.totalCoverageC;
            case G:
                return this.totalCoverageG;
            case T:
                return this.totalCoverageT;
            case OTHER:
                return this.totalCoverageOther;
            default:
                return -1;
        }
    }
    
        
    public double getStrandCoverage(Nucleotide n, boolean strand){
        switch(n) {
            case A:
                if(strand) return this.totalCoverageA1;
                else return this.totalCoverageA2;
            case C:
                if(strand) return this.totalCoverageC1;
                else return this.totalCoverageC2;
            case G:
                if(strand) return this.totalCoverageG1;
                else return this.totalCoverageG2;
            case T:
                if(strand) return this.totalCoverageT1;
                else return this.totalCoverageT2;
            case OTHER:
                if(strand) return this.totalCoverageOther1;
                else return this.totalCoverageOther2;
            default:
                return -1;
        }
    }

    /*
    public double getQuality(Nucleotide n) {
        switch(n) {
            case A:
               return this.a_quality;
            case C:
                return this.c_quality;
            case G:
                return this.g_quality;
            case T:
                return this.t_quality;
            case OTHER:
                return this.other_quality;
            default:
                return -1;
        }
    }
     */

    /*
    public double getAverageQuality(Nucleotide n) {
        return ((double) this.getQuality(n)) / this.getTotalQuality();
    }
     */

    public double getCoverageProportion(Nucleotide n) {
        return ((double) this.getCoverage(n)) / this.getTotalCoverage();
    }

    public Nucleotide getReferenceNucleotide() {
        return this.referenceNucleotide;
    }

    public void setReferenceNucleotide(Nucleotide n) {
        this.referenceNucleotide = n;
    }
}
