/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.format;

import savant.util.Range;

import java.util.Comparator;

/**
 *
 * @author mfiume
 */
public class LineRangeComparator implements Comparator{

    public int compare(Object o1, Object o2){

        Range r1 = ((LinePlusRange) o1).range;
        Range r2 = ((LinePlusRange) o2).range;

        if (r1.getFrom() < r2.getFrom()) {
            return -1;
        } else if (r1.getFrom() > r2.getFrom()) {
            return 1;
        } else {
            return 0;
        }
    }
}
