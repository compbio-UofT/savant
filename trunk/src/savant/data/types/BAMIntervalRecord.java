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

package savant.data.types;

import java.awt.Color;
import net.sf.samtools.SAMRecord;

/**
 * Class to represent an interval from a BAM file. Wraps a SAMRecord. Almost, but not quite immutable.
 * The SAMRecord's internals are mutable and since it is too expensive to make a defensive copy of it,
 * be careful not to modify the SAMRecord after construction of a BAMIntervalRecord.
 * 
 * @see net.sf.samtools.SAMRecord
 * @author vwilliams
 */
public class BAMIntervalRecord implements IntervalRecord {

    //public enum PairType { NORMAL, INVERTED_MATE, INVERTED_READ, EVERTED };

    private final Interval interval;
    private final SAMRecord samRecord;
    //private final PairType type;
    private Color overrideColor = null;

    /**
     * Constructor. Clients should use static factory method valueOf() instead.
     *
     * @param samRecord samRecord the SAMRecord associated with the read; may not be null
     * @param type type the pair type; may be null if read is unpaired or mate is unmapped
     */
    BAMIntervalRecord(SAMRecord samRecord) {

        if (samRecord == null) throw new IllegalArgumentException("samRecord must not be null");

        this.interval = Interval.valueOf(samRecord.getAlignmentStart(), samRecord.getAlignmentEnd());
        this.samRecord = samRecord;
    }

    /**
     * Static factory method to construct a BAMIntervalRecord
     * 
     * @param samRecord the SAMRecord associated with the read; may not be null
     * @return a newly constructed BAMIntervalRecord
     */
    public static BAMIntervalRecord valueOf(SAMRecord samRecord) {
        return new BAMIntervalRecord(samRecord);
    }

    @Override
    public String getReference() {
        return samRecord.getReferenceName();
    }

    @Override
    public Interval getInterval() {
        return interval;
    }

    @Override
    public String getName() {
        return samRecord.getReadName();
    }

    public SAMRecord getSamRecord() {
        return samRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BAMIntervalRecord that = (BAMIntervalRecord) o;

        if (!interval.equals(that.interval)) return false;
        if (!samRecord.equals(that.samRecord)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = interval.hashCode();
        result = 31 * result + samRecord.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BAMIntervalRecord");
        sb.append("{interval=").append(interval);
        sb.append(", samRecord=").append(samRecord);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(Object o) {

        SAMRecord otherSam = ((BAMIntervalRecord) o).getSamRecord();
        SAMRecord thisSam = this.getSamRecord();

        //compare ref
        if (!thisSam.getReferenceName().equals(otherSam.getReferenceName())){
            String a1 = thisSam.getReferenceName();
            String a2 = otherSam.getReferenceName();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }

        //compare position
        int a = thisSam.getAlignmentStart();
        int b = otherSam.getAlignmentStart();

        if (a == b){
            String a1 = thisSam.getReadName();
            String a2 = otherSam.getReadName();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;

            if(thisSam.getReadNegativeStrandFlag() == otherSam.getReadNegativeStrandFlag()) return 0;
            if(thisSam.getReadNegativeStrandFlag()) return 1;
            else return -1;


        } else if(a < b) return -1;
          else return 1;
    }

    public Color getColor(){
        return overrideColor;
    }

    public void setColor(Color color){
        this.overrideColor = color;
    }

}

