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
 * Class to represent an inverval from a BAM file. Wraps a SAMRecord.
 * 
 * @see net.sf.samtools.SAMRecord
 * @author vwilliams
 */
public class BAMIntervalRecord extends IntervalRecord {

    public enum PairType { NORMAL, INVERTED_MATE, INVERTED_READ, EVERTED };

    private SAMRecord samRecord;
    private SAMRecord mateRecord;
    PairType type;

    public BAMIntervalRecord(SAMRecord samRecord) {

        super(new Interval(samRecord.getAlignmentStart(), samRecord.getAlignmentEnd()));
        this.samRecord = samRecord;

    }

    @Override
    public void setInterval(Interval interval) {
        
        super.setInterval(interval);
    }

    public SAMRecord getSamRecord() {
        return samRecord;
    }

    public void setSamRecord(SAMRecord samRecord) {
        this.samRecord = samRecord;
    }

    public SAMRecord getMateRecord() {
        return mateRecord;
    }

    public void setMateRecord(SAMRecord mateRecord) {
        this.mateRecord = mateRecord;
    }

    public PairType getType() {
        return type;
    }

    public void setType(PairType type) {
        this.type = type;
    }

}
