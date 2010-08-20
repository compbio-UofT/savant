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
 * GenericIntervalRecord.java
 * Created on Jan 8, 2010
 */

package savant.data.types;

/**
 * Immutable class to represent an interval + a description.
 * 
 * @author vwilliams
 */
public class GenericIntervalRecord implements IntervalRecord, Comparable {

    private final Interval interval;
    private final String description;

    GenericIntervalRecord(Interval interval, String description) {
        if (interval == null) throw new IllegalArgumentException("Invalid argument. Interval must not be null");
        this.interval = interval;
        this.description = description;
    }


    public static GenericIntervalRecord valueOf(Interval interval, String description) {
        return new GenericIntervalRecord(interval, description);
    }

    public Interval getInterval() {
        return this.interval;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericIntervalRecord that = (GenericIntervalRecord) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (!interval.equals(that.interval)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = interval.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GenericIntervalRecord");
        sb.append("{interval=").append(interval);
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public int compareTo(Object o) {

        GenericIntervalRecord other = (GenericIntervalRecord) o;

        //compare point
        int a = this.getInterval().getStart();
        int b = other.getInterval().getStart();

        if(a == b){
            String a1 = this.getDescription();
            String a2 = other.getDescription();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
            return 0;
        } else if(a < b) return -1;
        else return 1;
    }
}
