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

/**
 * TODO:
 * @author mfiume
 */
public class Range
{
    private int from;
    private int to;

    public Range(int from, int to)
    {
        this.from = from;
        this.to = to;
    }

    public int getFrom() { return this.from; }
    public int getTo() { return this.to; }

    public int getLength() { return getTo() - getFrom() + 1; }

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
