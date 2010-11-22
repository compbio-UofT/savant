/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.snp;

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

    private String viewTrackName;

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

    public Pileup(String viewTrackName, long position, Nucleotide n) {
        this.viewTrackName = viewTrackName;
        this.position = position;
        this.referenceNucleotide = n;
    }

    public String getTrackName() {
        return this.viewTrackName;
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
        hash = 89 * hash + (this.viewTrackName != null ? this.viewTrackName.hashCode() : 0);
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
            if ((snpNuc == null && n != this.referenceNucleotide) || (n != this.referenceNucleotide &&
                    this.getCoverageProportion(n) > this.getCoverageProportion(snpNuc) )) {
                snpNuc = n;
            }
        }

        snpNucleotide = snpNuc;
        return snpNucleotide;
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
