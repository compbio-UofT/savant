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
package savant.data.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.util.SAMReadUtils;
import savant.util.SAMReadUtils.PairedSequencingProtocol;

/**
 *
 * @author mfiume
 */
public class ReadPairIntervalRecord implements IntervalRecord {
    private static final Log LOG = LogFactory.getLog(ReadPairIntervalRecord.class);

    public static List<ReadPairIntervalRecord> getMatePairs(List<Record> data) {

        List<ReadPairIntervalRecord> result = new ArrayList<ReadPairIntervalRecord>();

        Map<String, List<SAMRecord>> unpaired = new HashMap<String, List<SAMRecord>>();
        String qname;
        int removeindex;
        for (int recordnum = 0; recordnum < data.size(); recordnum++) {
            SAMRecord s = ((BAMIntervalRecord) data.get(recordnum)).getSamRecord();
            qname = s.getReadName();
            List<SAMRecord> unpairedlist;
            removeindex = -1;

            LOG.info(qname);

            if (unpaired.containsKey(qname)) {
                unpairedlist = unpaired.get(qname);
                for (int j = 0; j < unpairedlist.size(); j++) {
                    if (isPair(unpairedlist.get(j), s)) {
                        result.add(new ReadPairIntervalRecord(unpairedlist.get(j), s));
                        removeindex = j;
                        break;
                    }
                }
            } else {
                unpairedlist = new ArrayList<SAMRecord>();
            }
            if (removeindex == -1) {
                unpairedlist.add(s);
            } else {
                unpairedlist.remove(removeindex);
            }
            unpaired.put(qname, unpairedlist);
        }

        return result;
    }

    private static boolean isPair(SAMRecord one, SAMRecord two) {
        LOG.info("Checking pair: " + one.getReadName() + " and " + two.getReadName());

        if (!one.getReadPairedFlag() || !two.getReadPairedFlag()) {
            return false;
        }
        if (!one.getReadName().equals(two.getReadName())) {
            return false;
        }
        if (one.getFirstOfPairFlag() == two.getFirstOfPairFlag()) {
            return false;
        }
        if (one.getMateAlignmentStart() != two.getAlignmentStart()) {
            return false;
        }

        LOG.info("*** YES ***");

        return true;
    }
    private SAMRecord first;
    private SAMRecord second;

    private ReadPairIntervalRecord(SAMRecord one, SAMRecord two) {
        if (one.getFirstOfPairFlag()) {
            first = one;
            second = two;
        } else {
            first = two;
            second = one;
        }
    }

    private ReadPairIntervalRecord(SAMRecord s) {
        if (!s.getReadPairedFlag() || s.getFirstOfPairFlag()) {
            first = s;
        } else {
            second = s;
        }
    }

    public SAMRecord getSingletonRecord() {
        if (!isSingleton()) {
            return null;
        } else {
            if (first != null) {
                return first;
            } else if (second != null) {
                return second;

                // this should never happen, but needed to keep
                // compiler happy
            } else {
                return null;
            }
        }
    }

    @Override
    public Interval getInterval() {

        // read has no mate in range
        if (this.isSingleton()) {
            SAMRecord s = this.getSingletonRecord();
            // mate exists
            if (s.getReadPairedFlag()) {
                if (s.getMateReferenceIndex() != s.getReferenceIndex()) {
                    LOG.info("Mate maps to different chr, unhandled");
                }
                return new Interval(s.getAlignmentStart(), s.getMateAlignmentStart());
            } // not mated
            else {
                return new Interval(s.getAlignmentStart(), s.getAlignmentEnd());
            }

            // we found the mate
        } else {
            return new Interval(this.first.getAlignmentStart(), this.second.getAlignmentEnd());
        }
    }

    /**
     * Not relevant for this class.
     * @return null
     */
    @Override
    public String getName() {
        return null;
    }

    public SAMRecord getFirst() {
        return this.first;
    }

    public SAMRecord getSecond() {
        return this.second;
    }

    public boolean isSingleton() {
        return this.first == null || this.second == null;
    }

    @Override
    public String getReference() {
        if (this.isSingleton()) {
            return this.getSingletonRecord().getReferenceName();
        } else {
            return this.first.getReferenceName();
        }
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof ReadPairIntervalRecord) {

            ReadPairIntervalRecord other = (ReadPairIntervalRecord) o;

            if (other.getInterval().getStart() < this.getInterval().getStart()) {
                return -1;
            } else if (other.getInterval().getEnd() > this.getInterval().getEnd()) {
                return 1;
            } else {
                return 0;
            }

        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "[ReadPair "
                + "firstofpair={" + first.getFirstOfPairFlag() + "} "
                + "firststart={" + first.getAlignmentStart() + "} "
                + "secondofpair={" + !second.getFirstOfPairFlag() + "} "
                + "secondstart={" + second.getAlignmentStart() + "} "
                + "backwards={" + (first.getAlignmentStart() > second.getAlignmentStart()) + "} "
                + "singleton={" + this.isSingleton() + "} "
                + "mptype={" + SAMReadUtils.getPairType(first, PairedSequencingProtocol.MATEPAIR) + "} "
                + "petype={" + SAMReadUtils.getPairType(first, PairedSequencingProtocol.PAIREDEND) + "}";
    }
}
