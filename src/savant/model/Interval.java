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
package savant.model;

import savant.util.Range;

/**
 * Value class to represent an abstract interval.
 *
 * @author mfiume
 */
public class Interval {

    private int start;
    private int end;


    public Interval(int start, int end)
    {
        setStart(start);
        setEnd(end);
    }

    public void setStart(int start) {
        this.start = start;
    }
    public void setEnd(int end) {
        this.end = end;
    }

    public int getStart() { return this.start; }
    public int getEnd() { return this.end; }

    public int getLength() { return getEnd() - getStart() + 1;}


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
        int result = start;
        result = 31 * result + end;
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
