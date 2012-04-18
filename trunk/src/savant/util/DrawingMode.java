/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

/**
 *
 * @author tarkvara
 */
public enum DrawingMode {
    // Interval modes
    SQUISH,
    PACK,
    ARC,
    
    // BAM modes
    STANDARD,
    MISMATCH,
    SEQUENCE,
    STANDARD_PAIRED,
    ARC_PAIRED,
    SNP,
    STRAND_SNP,
    
    // Variant modes
    MATRIX,
    FREQUENCY;

    public String getDescription() {
        switch (this) {
            case SQUISH:
                return "Squish";
            case PACK:
                return "Pack";
            case ARC:
                return "Arc";
            case STANDARD:
                return "Standard";
            case MISMATCH:
                return "Mismatch";
            case SEQUENCE:
                return "Read Sequence";
            case STANDARD_PAIRED:
                return "Read Pair (Standard)";
            case ARC_PAIRED:
                return "Read Pair (Arc)";
            case SNP:
                return"SNP";
            case STRAND_SNP:
                return "Strand SNP";
            case MATRIX:
                return "Participants";
            case FREQUENCY:
                return "Frequency";
        }
        return null;
    }
}
