/*
 *    Copyright 2010 University of Toronto
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

    private long position;

    private String trackName;

    //private int totalquality = 0;
    private List<Double> coverageA = new ArrayList<Double>();
    private double totalCoverageA = 0;
    //private double a_quality = 0;
    private List<Double> coverageC = new ArrayList<Double>();
    private double totalCoverageC = 0;
    //private double c_quality = 0;
    private List<Double> coverageT = new ArrayList<Double>();
    private double totalCoverageT = 0;
    //private double t_quality = 0;
    private List<Double> coverageG = new ArrayList<Double>();
    private double totalCoverageG = 0;
    //private double g_quality = 0;
    private List<Double> coverageOther = new ArrayList<Double>();
    private double totalCoverageOther = 0;
    //private double other_quality = 0;

    public Pileup(String trackName, long position, Nucleotide n) {
        this.trackName = trackName;
        this.position = position;
        this.referenceNucleotide = n;
    }

    public Pileup(long position){
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
        hash = 89 * hash + (int) (this.position ^ (this.position >>> 32));
        hash = 89 * hash + (this.trackName != null ? this.trackName.hashCode() : 0);
        return hash;
    }

    public void pileOn(char c) { pileOn(c,1.0); }

    public void pileOn(Nucleotide n) { pileOn(n,1.0); }

    public void pileOn(char c, double quality) { pileOn(getNucleotide(c),quality); }

    public void pileOn(Nucleotide n, double quality) {
        double coverage = 1.0 * quality;

        //System.out.println("(P) " + n + " @ " + this.getPosition());

        switch(n) {
            case A:
                coverageA.add(coverage);
                totalCoverageA += coverage;
                break;
            case C:
                coverageC.add(coverage);
                totalCoverageC += coverage;
                break;
            case G:
                coverageA.add(coverage);
                totalCoverageG += coverage;
                break;
            case T:
                coverageT.add(coverage);
                totalCoverageT += coverage;
                break;
            default:
                coverageOther.add(coverage);
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
        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T };

        Nucleotide snpNuc = null;

        for (Nucleotide n : nucs) {
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
                break;
            case C:
                this.totalCoverageC = 0;
                break;
            case G:
                this.totalCoverageG = 0;
                break;
            case T:
                this.totalCoverageT = 0;
                break;
            case OTHER:
                this.totalCoverageOther = 0;
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

    public long getPosition() {
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
