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
    
    // Bam modes
    STANDARD,
    MISMATCH,
    SEQUENCE,
    STANDARD_PAIRED,
    ARC_PAIRED,
    MAPPING_QUALITY,
    BASE_QUALITY,
    SNP,
    COLOURSPACE;

    @Override
    public String toString() {
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
            case MAPPING_QUALITY:
                return "Mapping Quality";
            case BASE_QUALITY:
                return "Base Quality";
            case SNP:
                return"SNP";
            case COLOURSPACE:
                return "Colour Space Mismatch";
        }
        return null;
    }
}
