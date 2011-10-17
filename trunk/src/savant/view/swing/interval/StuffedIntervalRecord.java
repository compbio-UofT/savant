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

package savant.view.swing.interval;

import java.util.ArrayList;
import java.util.List;
import savant.data.types.Interval;
import savant.data.types.IntervalRecord;

/**
 *
 * @author mfiume
 */
public class StuffedIntervalRecord implements IntervalRecord {

    static List<List<IntervalRecord>> getOriginalIntervals(List<List<IntervalRecord>> pack) {

        List<List<IntervalRecord>> result = new ArrayList<List<IntervalRecord>>();

        for (List<IntervalRecord> list : pack) {

            List<IntervalRecord> subresult = new ArrayList<IntervalRecord>();

            for (IntervalRecord r : list) {

                StuffedIntervalRecord sir = (StuffedIntervalRecord) r;
                subresult.add(sir.getOriginalInterval());

            }

            result.add(subresult);
        }

        return result;
    }

    Interval stuffedRecord;
    IntervalRecord originalRecord;
    int leftstuffing;
    int rightstuffing;

    public StuffedIntervalRecord(IntervalRecord record, int leftstuffing, int rightstuffing) {
        this.originalRecord = record;
        this.leftstuffing = leftstuffing;
        this.rightstuffing = rightstuffing;
        this.stuffedRecord = new Interval(Math.max(0,record.getInterval().getStart()-leftstuffing), record.getInterval().getEnd()+rightstuffing);
    }

    @Override
    public String getReference() {
        return originalRecord.getReference();
    }

    @Override
    public Interval getInterval() {
        return stuffedRecord;
    }

    @Override
    public String getName() {
        return originalRecord.getName();
    }

    @Override
    public int compareTo(Object o) {

        StuffedIntervalRecord other = (StuffedIntervalRecord) o;

        //compare ref
        if (!this.getReference().equals(other.getReference())){
            String a1 = this.getReference();
            String a2 = other.getReference();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }
        //compare interval
        int a = this.getInterval().getStart();
        int b = other.getInterval().getStart();

        int c = this.getInterval().getEnd();
        int d = other.getInterval().getEnd();

        if(a == b){
            if (d < c) { return -1; }
            else if (d > c) { return 1; } // longer intervals reported first
            else {return 0; }
        } 
        else if(a < b) { return -1; }
        else { return 1; }
    }

    private IntervalRecord getOriginalInterval() {
        return this.originalRecord;
    }

}
