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

/*
 * BAMIntervalRecord.java
 * Created on Jan 28, 2010
 */

package savant.model;

import net.sf.samtools.SAMRecord;

/**
 * Class to represent an inverval from a BAM file. Wraps a SAMRecord. Almost, but not quite immutable.
 * The SAMRecord's internals are mutable and since it is not easy to make a defensive copy of it,
 * be careful not to modify the SAMRecord after construction of a BAMIntervalRecord.
 * 
 * @see net.sf.samtools.SAMRecord
 * @author vwilliams
 */
public class BAMIntervalRecord extends IntervalRecord {

    public enum PairType { NORMAL, INVERTED_MATE, INVERTED_READ, EVERTED };

    private final SAMRecord samRecord;
    private final PairType type;

    /**
     * Constructor. Clients should use static factory method valueOf() instead.
     *
     * @param samRecord samRecord the SAMRecord associated with the read; may not be null
     * @param type type the pair type; may be null if read is unpaired or mate is unmapped
     */
    protected BAMIntervalRecord(SAMRecord samRecord, PairType type) {
        super(Interval.valueOf(samRecord.getAlignmentStart(), samRecord.getAlignmentEnd()+1));
        this.samRecord = samRecord;
        this.type = type;

    }

    /**
     * Static factory method to construct a BAMIntervalRecord
     * 
     * @param samRecord the SAMRecord associated with the read; may not be null
     * @param type the pair type; may be null if read is unpaired or mate is unmapped
     * @return a newly constructed BAMIntervalRecord
     */
    public static BAMIntervalRecord valueOf(SAMRecord samRecord, PairType type) {
        return new BAMIntervalRecord(samRecord, type);
    }

    public SAMRecord getSamRecord() {
        return samRecord;
    }

    public PairType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BAMIntervalRecord that = (BAMIntervalRecord) o;

        if (!samRecord.equals(that.samRecord)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + samRecord.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BAMIntervalRecord");
        sb.append("{samRecord=").append(samRecord);
        sb.append('}');
        return sb.toString();
    }
}
