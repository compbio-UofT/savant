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

import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Utility functions for dealing with Sam records.
 *
 * @author mfiume
 */
public class SAMReadUtils {
    private static final Log LOG = LogFactory.getLog(SAMReadUtils.class);

    public enum PairedSequencingProtocol {
        UNKNOWN, UNPAIRED, MATEPAIR, PAIREDEND;
    }

    public enum PairMappingType { NORMAL, INVERTED_MATE, INVERTED_READ, EVERTED, UNKNOWN };

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
            LOG.warn("Encountered unknown pair possibility:\n"
                    + "prot=" + p
                    + "first=" + r.getFirstOfPairFlag()
                    + " neg=" + r.getReadNegativeStrandFlag()
                    + " mneg=" + r.getMateNegativeStrandFlag()
                    + " ffwd=" + ffwd + " sfwd=" + sfwd + " rev=" + rev + ": ");
            return PairMappingType.UNKNOWN;
        }
    }
}
