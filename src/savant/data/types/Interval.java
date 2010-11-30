/*
 *    Copyright 2009-2010 University of Toronto
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

import savant.util.Range;

/**
 * Immutable value class to represent an abstract interval.
 *
 * @author mfiume, vwilliams
 */
public final class Interval {

    private final long start;
    private final long end;


    public Interval(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public static Interval valueOf(long start, long end) {
        return new Interval(start, end);
    }

    public long getStart() { return this.start; }
    public long getEnd() { return this.end; }
    public long getLength() { return end - start;}

    public boolean intersects(Interval interval) {
        return this.start < interval.getEnd()-1 && interval.getStart() < this.end-1;
    }

    public Interval merge(Interval interval) {
        if (interval.intersects(this)) {
            return Interval.valueOf(this.start < interval.getStart() ? this.start : interval.getStart(),
                                    this.end > interval.getEnd() ? this.end : interval.getEnd());
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

    public Range getRange() {
        return new Range(this.start, this.end);
    }
}
