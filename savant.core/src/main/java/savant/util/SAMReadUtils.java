/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
