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
package savant.util;

import java.io.Serializable;
import savant.api.adapter.RangeAdapter;


/**
 * Utility class for storing any type of range.
 *
 * @author mfiume
 */
public final class Range implements RangeAdapter, Serializable {
    static final long serialVersionUID = 2690664822084781217L;

    private long from;
    private long to;

    public Range(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public int getFrom() { return (int)from; }

    @Override
    public int getTo() { return (int)to; }

    @Override
    public int getLength() { return (int)(to - from) + 1; }

    @Override
    @Deprecated
    public int getFromAsInt() { return (int)from; }

    @Override
    @Deprecated
    public int getToAsInt() { return (int)to; }

    @Override
    @Deprecated
    public int getLengthAsInt() { return (int)(to - from) + 1; }

    @Override
    public String toString() {
        return MiscUtils.numToString(from) + "-" + MiscUtils.numToString(to);
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
        int result = (int)from;
        result = 31 * result + (int)to;
        return result;
    }
}
