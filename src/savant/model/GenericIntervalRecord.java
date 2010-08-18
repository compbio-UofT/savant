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

package savant.model;

/**
 * Immutable class to represent an interval + a description.
 * 
 * @author vwilliams
 */
public class GenericIntervalRecord extends IntervalRecord implements Comparable {

    private final String reference;
    private final String description;

    protected GenericIntervalRecord(String reference, Interval interval, String description) {
        super(interval);
        if (reference == null) throw new IllegalArgumentException("Invalid argument. Reference may not be null.");
        this.reference = reference;
        this.description = description;
    }


    public static GenericIntervalRecord valueOf(String reference, Interval interval, String description) {
        return new GenericIntervalRecord(reference, interval, description);
    }


    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GenericIntervalRecord that = (GenericIntervalRecord) o;

        if (!reference.equals(that.reference)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + reference.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GenericIntervalRecord");
        sb.append("{reference='").append(reference).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(Object o) {

        GenericIntervalRecord other = (GenericIntervalRecord) o;

        //compare ref
        if (!this.getReference().equals(other.getReference())){
            String a1 = this.getReference();
            String a2 = other.getReference();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }

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
