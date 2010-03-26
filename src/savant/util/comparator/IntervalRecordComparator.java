/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util.comparator;

import savant.model.IntervalRecord;
import savant.util.Range;

import java.util.Comparator;

/**
 *
 * @author mfiume
 */
public class IntervalRecordComparator implements Comparator{

    public int compare(Object o1, Object o2){

        Range r1 = ((IntervalRecord) o1).getInterval().getRange();
        Range r2 = ((IntervalRecord) o2).getInterval().getRange();

        if (r1.getFrom() < r2.getFrom()) {
            return -1;
        } else if (r1.getFrom() > r2.getFrom()) {
            return 1;
        } else {
            return 0;
        }
    }
}
