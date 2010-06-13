/*
 *    Copyright 2010 University of Toronto
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

/*
 * IntervalRecord.java
 * Created on Jan 8, 2010
 */

package savant.model;

/**
 * TODO:
 * @author vwilliams
 */
public class IntervalRecord {

    private Interval interval;

    public IntervalRecord(Interval interval) {
        setInterval(interval);
    }

    public Interval getInterval() { return this.interval; }
    public void setInterval(Interval interval) {
        if (interval == null) throw new IllegalArgumentException("Interval must not be null.");
        this.interval = interval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntervalRecord that = (IntervalRecord) o;

        if (!interval.equals(that.interval)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return interval.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("IntervalRecord");
        sb.append("{interval=").append(interval);
        sb.append('}');
        return sb.toString();
    }
}
