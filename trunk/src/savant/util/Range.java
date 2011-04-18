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
package savant.util;

import java.io.Serializable;
import savant.api.adapter.RangeAdapter;

/**
 * Utility class for storing any type of range.  For generality, the values
 * are all stored as longs.
 *
 * @author mfiume
 */
public final class Range implements RangeAdapter, Serializable
{
    private long from;
    private long to;

    public Range(int from, int to)
    {
        this((long) from, (long) to);
    }

    public Range(long from, long to)
    {
        this.from = from;
        this.to = to;
    }

    @Override
    public long getFrom() { return this.from; }
    @Override
    public long getTo() { return this.to; }

    @Override
    public long getLength() { return getTo() - getFrom() + 1; }

    /**
     * Like getFrom(), but used when we know that the value must fall within
     * the range of an int.
     *
     * @return the start of the range
     */
    public int getFromAsInt() {
        assert Math.abs(from) <= Integer.MAX_VALUE;
        return (int)from;
    }

    /**
     * Like getTo(), but used when we know that the value must fall within
     * the range of an int.
     *
     * @return the end of the range
     */
    public int getToAsInt() {
        assert Math.abs(to) <= Integer.MAX_VALUE;
        return (int)to;
    }

    /**
     * Like getLength(), but used when we know that the value must fall within
     * the range of an int.
     *
     * @return the length of the range
     */
    public int getLengthAsInt() {
        assert Math.abs(to - from + 1) <= Integer.MAX_VALUE;
        return (int)(to - from + 1);
    }

    @Override
    public String toString()
    {
        return MiscUtils.numToString(from) + " - " + MiscUtils.numToString(to);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        if (from != range.from) return false;
        if (to != range.to) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) from;
        result = (int) (31 * result + to);
        return result;
    }

}
