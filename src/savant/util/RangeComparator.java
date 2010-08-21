/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

import savant.util.Range;

import java.util.Comparator;

/**
 *
 * @author mfiume
 */
public class RangeComparator implements Comparator{
    public int compare(Object o1, Object o2){

        Range r1 = (Range) o1;
        Range r2 = (Range) o2;

        if (r1.getFrom() < r2.getFrom()) {
            return -1;
        } else if (r1.getFrom() > r2.getFrom()) {
            return 1;
        } else {
            return 0;
        }
    }
}