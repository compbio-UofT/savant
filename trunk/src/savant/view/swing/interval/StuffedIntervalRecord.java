/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

    static List<List<IntervalRecord>> getOriginalIntervals(ArrayList<List<IntervalRecord>> pack) {

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
        this.stuffedRecord = new Interval(record.getInterval().getStart()-leftstuffing,record.getInterval().getEnd()+rightstuffing);
    }

    @Override
    public Interval getInterval() {
        return stuffedRecord;
    }

    @Override
    public String getReference() {
        return originalRecord.getReference();
    }

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
        long a = this.getInterval().getStart();
        long b = other.getInterval().getStart();

        long c = this.getInterval().getEnd();
        long d = other.getInterval().getEnd();

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
