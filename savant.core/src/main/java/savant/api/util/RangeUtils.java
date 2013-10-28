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
package savant.api.util;

import savant.api.adapter.RangeAdapter;
import savant.util.Range;


/**
 * Utility methods for creating and manipulating RangeAdapter objects.
 *
 * @author tarkvara
 */
public class RangeUtils {
    /**
     * Construct
     * @param from start-point of the range
     * @param to end-point of the range
     * @return a newly-constructed <code>RangeAdapter</code>
     */
    public static RangeAdapter createRange(int from, int to) {
        return new Range(from, to);
    }

    /**
     * Determine whether one range completely contains a second one.
     *
     * @param r1 a big range
     * @param r2 a smaller range
     * @return true if <code>r1</code> contains <code>r2</code>
     */
    public static boolean contains(RangeAdapter r1, RangeAdapter r2) {
        return r1.getFrom() <= r2.getFrom() && r1.getTo() >= r2.getTo();
    }

    /**
     * Determine whether two ranges intersect.
     *
     * @param r1 a range
     * @param r2 another range
     * @return true if <code>r1</code> and <code>r2</code> intersect
     */
    public static boolean intersects(RangeAdapter r1, RangeAdapter r2) {
        return r1.getFrom() <= r2.getTo() && r1.getTo() >= r2.getFrom();
    }

    /**
     * Add two ranges together to create the smallest range which covers both of them.
     * If they intersect, this will be a real union.
     * @since Savant 1.5.1
     */
    public static RangeAdapter union(RangeAdapter r1, RangeAdapter r2) {
        return new Range(Math.min(r1.getFrom(), r2.getFrom()), Math.max(r1.getTo(), r2.getTo()));
    }

    /**
     * Subtract r2 from r1, producing an array of 0, 1, or 2 ranges.
     *
     * @param r1 a range
     * @param r2 another range
     * @return the difference between <code>r1</code> and <code>r2</code>.
     */
    public static RangeAdapter[] subtract(RangeAdapter r1, RangeAdapter r2) {
        if (r1.getTo() < r2.getFrom() || r1.getFrom() > r2.getTo()) {
            // No intersection, just return r1.
            return new RangeAdapter[] { r1 };
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
                    return new RangeAdapter[] { createRange(r1.getFrom(), r2.getFrom() - 1) };
                } else {
                    // r2 is a proper subset of r1.  Return a donut of two ranges.
                    return new RangeAdapter[] { createRange(r1.getFrom(), r2.getFrom() - 1), createRange(r2.getTo() + 1, r1.getTo()) };
                }
            } else if (r1.getTo() > r2.getTo()) {
                return new RangeAdapter[] { createRange(r2.getTo() + 1, r1.getTo()) };
            }
        }
        return new RangeAdapter[0];
    }
    
    /**
     * Add a reasonable margin around the given range for a mor attractive display.
     * Used for centring and highlighting bookmarks.
     * @param r the range before adjustment
     * @return a larger range.
     * @since 1.6.1
     */
    public static RangeAdapter addMargin(RangeAdapter r) {
        int buffer = Math.max(250, r.getLength() / 4);
        int newStart = Math.max(1, r.getFrom() - buffer);
        return new Range(newStart, r.getTo() + buffer);
    }
}
