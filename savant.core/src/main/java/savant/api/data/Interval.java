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
package savant.api.data;

import savant.api.adapter.RangeAdapter;


/**
 * Immutable value class to represent an abstract interval.  Between versions 1.2
 * and 1.4.2 of Savant, intervals were half-open, excluding the end-point.  In version
 * 1.4.3, we reverted to the old semantics, where Intervals were always closed.
 *
 * @author mfiume, vwilliams
 */
public final class Interval implements Comparable<Interval> {

    private final int start;
    private final int end;


    /**
     * An interval
     * VERY IMPORTANT: The coordinates for intervals must be 1-based!
     * @param start
     * @param end
     */
    private Interval(int start, int end) {
        this.start = start;
        this.end = end;
    }

    /**
     * An interval
     * VERY IMPORTANT: The coordinates for intervals must be 1-based!
     * @param start
     * @param end
     * @return an interval
     */
    public static Interval valueOf(int start, int end) {
        return new Interval(start, end);
    }

    public int getStart() { return start; }
    public int getEnd() { return end; }
    public int getLength() { return end - start + 1;}

    // TODO: Make sure that this is actually the correct calculation when packing intervals.
    public boolean intersects(Interval i2) {
        return start < i2.end && i2.start < end;
    }


    // TODO: Make sure this is actually the correct calculation when merging blocks.
    public boolean intersectsOrAbuts(Interval i2) {
        return start < i2.end - 1 && i2.start < end - 1;
    }

    public Interval merge(Interval i2) {
        if (intersectsOrAbuts(i2)) {
            return Interval.valueOf(Math.min(start, i2.start), Math.max(end, i2.end));
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Interval interval = (Interval) o;

        if (end != interval.end) return false;
        if (start != interval.start) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int)start;
        result = 31 * result + (int)end;
        return result;
    }

    @Override
    public String toString() {
        return getStart() + " - " + getEnd();

    }

    @Override
    public int compareTo(Interval that) {
        if (start < that.start) {
            return -1;
        } else if (start > that.start) {
            return 1;
        }
        return end - that.end;
    }

    /**
     * Calculate the intersection between this Interval and a Range.
     *
     * @param r the range to be compared against
     * @return true if this Interval intersects with r
     */
    public boolean intersectsRange(RangeAdapter r) {
        return start <= r.getTo() && end >= r.getFrom();
    }
}
