/*
 *    Copyright 2011 University of Toronto
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
package savant.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import savant.api.adapter.RangeAdapter;
import savant.util.Range;


/**
 * Utility methods for creating and manipulating RangeAdapter objects.
 *
 * @author tarkvara
 */
public class RangeUtils {
    public static RangeAdapter createRange(long from, long to) {
        return new Range(from, to);
    }

    /**
     * Determine whether one range completely contains a second one.
     *
     * @param r1 a big range
     * @param r2 a smaller range
     * @return true if r1 contains r2
     */
    public static boolean contains(RangeAdapter r1, RangeAdapter r2) {
        return r1.getFrom() <= r2.getFrom() && r1.getTo() >= r2.getTo();
    }

    public static boolean intersects(RangeAdapter r1, RangeAdapter r2) {
        return r1.getFrom() <= r2.getTo() && r1.getTo() >= r2.getFrom();
    }

    /**
     * Subtract r2 from r1, producing a list of 0, 1, or 2 ranges.
     */
    public static List<RangeAdapter> subtract(RangeAdapter r1, RangeAdapter r2) {
        if (r1.getTo() < r2.getFrom() || r1.getFrom() > r2.getTo()) {
            // No intersection, just return r1.
            return Arrays.asList(r1);
        } else {
            /*
             * |.. |     A
             *   |   |
             * 
             * |.. |     A
             *   | |
             * 
             * |.. ..|   B
             *   | |   
             * 
             *   | |     C=0
             * |     |
             * 
             *   |   |   C=0
             * |     |
             * 
             *   | ..|   D
             * |   |
             * 
             * |   |     C=0
             * |     |
             * 
             * | ..|     D
             * | |
             * 
             * |   |     C=0
             * |   |
             */
            if (r1.getFrom() < r2.getFrom()) {
                if (r1.getTo() <= r2.getTo()) {
                    // r1 overlaps with r2, but is offset to the left.
                    return Arrays.asList(createRange(r1.getFrom(), r2.getFrom() - 1));
                } else {
                    // r2 is a proper subset of r1.  Return a donut of two ranges.
                    return Arrays.asList(createRange(r1.getFrom(), r2.getFrom() - 1), createRange(r2.getTo() + 1, r1.getTo()));
                }
            } else if (r1.getTo() > r2.getTo()) {
                return Arrays.asList(createRange(r2.getTo() + 1, r1.getTo()));
            }
        }
        return new ArrayList<RangeAdapter>();
    }
}
