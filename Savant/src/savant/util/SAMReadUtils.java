/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

import net.sf.samtools.SAMRecord;
import savant.data.types.ReadPairIntervalRecord;

/**
 *
 * @author mfiume
 */
public class SAMReadUtils {

    public enum PairedSequencingProtocol {
        UNKNOWN, UNPAIRED, MATEPAIR, PAIREDEND;
    }

    public enum PairMappingType { NORMAL, INVERTED_MATE, INVERTED_READ, EVERTED, UNKNOWN };

    public static PairMappingType getPairType(ReadPairIntervalRecord r) {
        return PairMappingType.NORMAL;
    }

    public static SAMReadUtils.PairMappingType getPairType(SAMRecord r, PairedSequencingProtocol p) {

        if (!r.getReadPairedFlag() || r.getMateUnmappedFlag()) {
            return PairMappingType.NORMAL;
        }

        if (p == PairedSequencingProtocol.UNKNOWN) {
            p = PairedSequencingProtocol.MATEPAIR;
        }

        boolean ffwd;
        boolean sfwd;
        boolean rev;
        boolean mp = p == PairedSequencingProtocol.MATEPAIR; // if matepair, otherwise paired end

        // this read is first of pair
        if (r.getFirstOfPairFlag()) {
            ffwd = !r.getReadNegativeStrandFlag();
            sfwd = !r.getMateNegativeStrandFlag();
            rev = r.getAlignmentStart() > r.getMateAlignmentStart();

            // this read is second of pair
        } else {
            ffwd = !r.getMateNegativeStrandFlag();
            sfwd = !r.getReadNegativeStrandFlag();
            rev = r.getMateAlignmentStart() > r.getAlignmentStart();
        }

        /* VERY USEFUL FOR DEBUGGING, please don't delete!
        System.out.print("prot=" + p + " first=" + r.getFirstOfPairFlag()
        + " neg=" + r.getReadNegativeStrandFlag()
        + " mneg=" + r.getMateNegativeStrandFlag()
        + " pos=" + r.getAlignmentStart()
        + " rpos=" + r.getMateAlignmentStart()
        + " ffwd=" + ffwd
        + " sfwd=" + sfwd
        + " rev=" + rev +  ": ");
         */

        /*
         * PAIRED END: sequenced from SAME strand
         * MATEPAIR: sequenced from OPPOSITE strands
         */


        if (sfwd && !ffwd && rev
                || ffwd && !sfwd && !rev) {
            if (mp) {
                return PairMappingType.NORMAL;
            } else {
                return PairMappingType.INVERTED_MATE;
            }
        } else if (sfwd && ffwd && rev
                || !ffwd && !sfwd && !rev) {
            if (mp) {
                return PairMappingType.INVERTED_READ;
            } else {
                return PairMappingType.EVERTED;
            }
        } else if (!sfwd && !ffwd && rev
                || ffwd && sfwd && !rev) {
            if (mp) {
                return PairMappingType.INVERTED_MATE;
            } else {
                return PairMappingType.NORMAL;
            }
        } else if (!sfwd && ffwd && rev
                || !ffwd && sfwd && !rev) {
            if (mp) {
                return PairMappingType.EVERTED;
            } else {
                return PairMappingType.INVERTED_READ;
            }

            // other
        } else {
            System.err.println("Encountered unknown pair possibility:\n"
                    + "prot=" + p
                    + "first=" + r.getFirstOfPairFlag()
                    + " neg=" + r.getReadNegativeStrandFlag()
                    + " mneg=" + r.getMateNegativeStrandFlag()
                    + " ffwd=" + ffwd + " sfwd=" + sfwd + " rev=" + rev + ": ");
            return PairMappingType.UNKNOWN;
        }
    }
}
