/*
 * GenericIntervalRecord.java
 * Created on Jan 8, 2010
 *
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
package savant.data.types;

import java.util.ArrayList;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Immutable class to represent one line of a BED file and all the fields therein.
 * 
 * @author mfiume, vwilliams
 */
public final class TabixIntervalRecord implements IntervalRecord {

    private final Interval interval;
    private final String chrom;
    private final List<String> otherFields;

    public TabixIntervalRecord(String s) {
        this(s,0,1,2);
    }

    /**
     * Constructor. Clients should use static factory method valueOf() instead.
     */
    public TabixIntervalRecord(String s, int chrIndex, int startIndex, int endIndex) {
        StringTokenizer st = new StringTokenizer(s);
        int numTokens = st.countTokens();
        
        String token = null;
        String chr = null;
        long start = 0;
        long end = 0;
        otherFields = new ArrayList<String>();

        for (int i = 0; i < numTokens; i++) {
            
            token = st.nextToken();
            
            if (i == chrIndex) {
                chr = token;
            }
            else if (i == startIndex) {
                 start = Long.parseLong(token);
            }
            else if (i == endIndex) {
                end = Long.parseLong(token);
            } else {
                otherFields.add(token);
            }
        }

        this.chrom = chr;
        this.interval = new Interval(start, end);
    }

    /**
     * Static factory method to construct a TabixIntervalRecord
     */
    public static TabixIntervalRecord valueOf(String s, int chrIndex, int startIndex, int endIndex) {
        return new TabixIntervalRecord(s, chrIndex, startIndex, endIndex);
    }

    @Override
    public Interval getInterval() {
        return this.interval;
    }

    public List<String> getOtherValues() {
        return this.otherFields;
    }

    public String getChrom() {
        return chrom;
    }

    @Override
    public String getReference() {
        return getChrom();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TabixIntervalRecord that = (TabixIntervalRecord) o;

        if (!chrom.equals(that.chrom)) return false;
        if (!interval.equals(that.interval)) return false;
        if (that.otherFields.size() != this.otherFields.size()) return false;
        for (int i = 0; i < this.otherFields.size(); i++) {
            if (!this.otherFields.get(i).equals(that.otherFields.get(i))) return false;
        }

        return true;
    }

    @Override
    public int compareTo(Object o) {

        TabixIntervalRecord other = (TabixIntervalRecord) o;

        //compare ref
        if (!this.getChrom().equals(other.getChrom())){
            String a1 = this.getChrom();
            String a2 = other.getChrom();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }

        //compare point
        long a = this.getInterval().getStart();
        long b = other.getInterval().getStart();
        if(a == b){
            return 0;
        } else if(a < b) return -1;
        else return 1;
    }
}
