/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.snp;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mfiume
 */
class Pileup {

    private Nucleotide referenceNucleotide;
    private Nucleotide snpNucleotide;
    private double snpNucleotideConfidence;
    private double snpVariabilityConfidence;
    private boolean isHet;

    private int position;

    private String viewTrackName;

    private List<Double> coverageA = new ArrayList<Double>();
    private double totalCoverageA = 0;
    private double totalQualityA = 0;
    private List<Double> coverageC = new ArrayList<Double>();
    private double totalCoverageC = 0;
    private double totalQualityC = 0;
    private List<Double> coverageT = new ArrayList<Double>();
    private double totalCoverageT = 0;
    private double totalQualityT = 0;
    private List<Double> coverageG = new ArrayList<Double>();
    private double totalCoverageG = 0;
    private double totalQualityG = 0;
    private List<Double> coverageOther = new ArrayList<Double>();
    private double totalCoverageOther = 0;
    private double totalQualityOther = 0;
    private Map<Nucleotide, List<Double>> coverageAll;

    private double snpPrior = 0;
    private int pseudoCount = 5;
    private double baseline_rate = 0.01;

    public Pileup(String viewTrackName, int position, Nucleotide n) {
        this.viewTrackName = viewTrackName;
        this.position = position;
        this.referenceNucleotide = n;

        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T, Nucleotide.OTHER };
        coverageAll = new HashMap<Nucleotide, List<Double>>();
        for (Nucleotide let : nucs) {
            this.coverageAll.put(let, new ArrayList<Double>());
        }
    }

    public Pileup(String viewTrackName, int position, Nucleotide n, double baseline_rate) {
        this.viewTrackName = viewTrackName;
        this.position = position;
        this.referenceNucleotide = n;
        this.baseline_rate = baseline_rate;

        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T, Nucleotide.OTHER };
        coverageAll = new HashMap<Nucleotide, List<Double>>();
        for (Nucleotide let : nucs) {
            this.coverageAll.put(let, new ArrayList<Double>());
        }
    }

    public Pileup(String viewTrackName, int position, Nucleotide n, double baseline_rate, int pseudoCount) {
        this.viewTrackName = viewTrackName;
        this.position = position;
        this.referenceNucleotide = n;
        this.baseline_rate = baseline_rate;
        this.pseudoCount = pseudoCount;

        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T, Nucleotide.OTHER };
        coverageAll = new HashMap<Nucleotide, List<Double>>();
        for (Nucleotide let : nucs) {
            this.coverageAll.put(let, new ArrayList<Double>());
        }
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

    public void pileOn(byte c) { pileOn(c,0.0); }

    public void pileOn(Nucleotide n) { pileOn(n,0.0); }

    public void pileOn(byte c, double quality) { pileOn(getNucleotide(c),quality); }

    public void pileOn(Nucleotide n, double quality) {
        double coverage = 1.0;

//        System.out.println("(P) " + n + " @ " + this.getPosition());

        switch(n) {
            case A:
                coverageAll.get(n).add(quality);
                totalCoverageA += coverage;
                totalQualityA += quality;
                break;
            case C:
                coverageAll.get(n).add(quality);
                totalCoverageC += coverage;
                totalQualityC += quality;
                break;
            case G:
                coverageAll.get(n).add(quality);
                totalCoverageG += coverage;
                totalQualityG += quality;
                break;
            case T:
                 coverageAll.get(n).add(quality);
                totalCoverageT += coverage;
                totalQualityT += quality;
                break;
            default:
                coverageAll.get(Nucleotide.OTHER).add(quality);
                totalCoverageOther += coverage;
                totalQualityOther += quality;
                break;
        }

        
    }

    /*
    public double getConfidence(Nucleotide n) {
        return ((double) this.getAverageQuality(n)) / this.getTotalQuality();
    }
     */

    public Nucleotide getSNPNucleotide() {
        if (snpNucleotide != null) {
            return snpNucleotide;
        }
        else  throw new RuntimeException("getSNPNucleotide w/o prior and not computed!!!");
    }

    public boolean isHeterozygous() {
        return this.isHet;
    }

    private static double logGamma(double x) {
      double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
      double ser = 1.0 + 76.18009173    / (x + 0)   - 86.50532033    / (x + 1)
                       + 24.01409822    / (x + 2)   -  1.231739516   / (x + 3)
                       +  0.00120858003 / (x + 4)   -  0.00000536382 / (x + 5);
      return Math.log10(Math.exp(tmp + Math.log(ser * Math.sqrt(2 * Math.PI))));
    }
    
    private static double logBinomial (int x, int y) {

        double res = logNchooseK(x,y) + y * Math.log10(0.5);
       // System.out.println("logbinomial = " + res);
        return res;

    }

    private static double logMultinomial (int[] counts, double[] probs) {
        int total = 0;
        double res = 0;
        for (int i = 0; i < counts.length; i++) {
//            System.out.println("lm: "+counts[i]+ " " + probs[i]);
            total += counts[i];
            res -= logGamma(counts[i]+1);
//            System.out.println("res prenow " + res);

            res += (counts[i] * Math.log10(probs[i]));
//            System.out.println("res now " + res);
        }
        res += logGamma(total+1);
//        System.out.println("res finally " + res);
        return res;
    }

    private static double logNchooseK (int x, int y) {

        double res = logGamma(y+1) - logGamma(x+1) - logGamma(y-x+1);
       // System.out.println("longnchooske = " + res);
        return res;

    }

    private double learnErrRate() {
        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T };
        double pseudoRate = this.baseline_rate;
        double errrate = 0;
        double cov = 0;
        double eprob;

        for (Nucleotide o : nucs) {
            for (Double d : this.coverageAll.get(o)) {
                    //reverse the quality value since called correctly
                eprob = Math.pow(10, -d/10);
                errrate += eprob;
                cov += 1;
            }
        }
        errrate += (pseudoCount*pseudoRate);
        cov += pseudoCount;
        return errrate/cov;
    }

    public Nucleotide getSNPNucleotide(double snpPrior) {

        if (snpNucleotide != null && !snpNucleotide.equals(this.referenceNucleotide) && snpPrior == this.snpPrior) {
                return snpNucleotide;
        }
        if (snpNucleotide != null && snpNucleotide.equals(this.referenceNucleotide) && snpPrior == this.snpPrior) {
                return null;
        }

        this.snpPrior = snpPrior;

        Nucleotide[] nucs = { Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T };
        int[] counts = new int[nucs.length];
        double[] probs = new double[nucs.length];

        Nucleotide snpNuc = null;
        double totProb = 0;
        double maxProb = -1;
        double errRate = learnErrRate();
        boolean ih = false;


        for (Nucleotide n : nucs) {
            double thisProb = 1.0;
            double binProb = 0;
            double binProb2 = 0;
            double thisProb2;
            double Qval;

            if (n != this.referenceNucleotide)
                thisProb *= (snpPrior/3);
            else
                thisProb *= 1-snpPrior;
            thisProb = Math.log10(thisProb);
            thisProb2 = thisProb;

            if (n != this.referenceNucleotide) {
                for (Nucleotide o : nucs) {
                    for (Double d : this.coverageAll.get(o)) {
                        if (o == n || o == this.referenceNucleotide) {
                          //System.out.println("cons: " + n.toString() + " ref: " + this.referenceNucleotide.toString() + " nuc: " + o.toString() + " qval " + d);
                            //reverse the quality value since called correctly
                            Qval = Math.pow(10, -d/10);
                            Qval = 1-Qval;
                            Qval = Math.log10(Qval);
                        }
                        else {
                            Qval = d;
                       }
                       thisProb += Qval/-10;

                    }
                }
            //het
                for (int i = 0; i < nucs.length; i++) {
                   counts[i] = this.coverageAll.get(nucs[i]).size();
                   if (this.referenceNucleotide == nucs[i] || n == nucs[i]) {
                      probs[i] = (1-errRate)/2.0;
                   }
                   else {
                       probs[i] = (errRate/2.0);
                   }
                }
                binProb = logMultinomial(counts, probs);

             //   binProb = logBinomial(this.coverageAll.get(n).size(), this.coverageAll.get(n).size()
             //               + this.coverageAll.get(this.referenceNucleotide).size());



                thisProb += binProb;
   
            }
            for (Nucleotide o : nucs) {
                for (Double d : this.coverageAll.get(o)) {
                    if (o == n) {
                      //  System.out.println("cons: " + n.toString() + " nuc: " + o.toString() + " qval " + d);
                        //reverse the quality value since called correctly
                        Qval = Math.pow(10, -d/10);
                        Qval = 1-Qval;
                        Qval = Math.log10(Qval);
                    }
                    else {
                        Qval = d;
                    }
                    thisProb2 += Qval/-10;
                }
            }
            for (int i = 0; i < nucs.length; i++) {
                counts[i] = this.coverageAll.get(nucs[i]).size();
                if (n == nucs[i]) {
                   probs[i] = (1-errRate);
                }
                else {
                   probs[i] = (errRate/3);
                }
            }
            binProb2 = logMultinomial(counts, probs);

            thisProb2 += binProb2;



            if (n == this.referenceNucleotide) {
                thisProb = 0;
                snpVariabilityConfidence = 1 - Math.pow(10, thisProb2);
            }
            else   {
                thisProb = Math.pow(10, thisProb);
            }
            thisProb2 = Math.pow(10, thisProb2);
            if (thisProb2 + thisProb >= maxProb) {
                maxProb = thisProb2 + thisProb;
                snpNuc = n;
                ih = (thisProb > thisProb2);
            }

//            System.out.println("nuc: " + n.toString() + " prob " + thisProb + " (multininProb = " + Math.pow(10,binProb) +
//                    ") prob2 " + thisProb2 + " (multininProb = " + Math.pow(10,binProb2) +  ") ref " + this.referenceNucleotide.toString());
            totProb = totProb + thisProb + thisProb2;
        }

        snpNucleotide = snpNuc;
        snpNucleotideConfidence = maxProb/totProb;
//        System.out.println("Confidence set to " + snpNucleotideConfidence);
        isHet = ih;
        if (!snpNucleotide.equals(this.referenceNucleotide))
            return snpNucleotide;
        else
            return null;
    }

    public double getSNPNucleotideConfidence(double snpPrior) {
        if (snpNucleotide == null || snpPrior != this.snpPrior)
               getSNPNucleotide(snpPrior);
        return snpNucleotideConfidence;  //or NucleotideConfidence
    }

    public double getTotalCoverage() {
        double totalcoverage = 0;
        totalcoverage += this.getCoverage(Nucleotide.A);
        totalcoverage += this.getCoverage(Nucleotide.C);
        totalcoverage += this.getCoverage(Nucleotide.G);
        totalcoverage += this.getCoverage(Nucleotide.T);
        return totalcoverage;
    }

    public static Nucleotide getNucleotide(byte c) {
        switch (Character.toUpperCase(c)) {
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
