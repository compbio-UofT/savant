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
/*
 * BAMIntervalRecord.java
 * Created on Jan 28, 2010
 */

package savant.data.types;

import savant.api.data.Interval;
import savant.api.data.IntervalRecord;
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

    private final Interval interval;
    private final SAMRecord samRecord;
    private Color overrideColor = null;

    /**
     * Constructor. Clients should use static factory method valueOf() instead.
     *
     * @param samRecord samRecord the SAMRecord associated with the read; may not be null
     */
    protected BAMIntervalRecord(SAMRecord samRecord) {

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

    public SAMRecord getSAMRecord() {
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

        SAMRecord otherSam = ((BAMIntervalRecord) o).getSAMRecord();
        SAMRecord thisSam = this.getSAMRecord();

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

