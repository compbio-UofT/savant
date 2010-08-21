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
 * GenericContinuousRecord.java
 * Created on Aug 18, 2010
 */

package savant.data.types;

/**
 * Immutable class to contain a value and the position at which that value obtains.
 */
public class GenericContinuousRecord implements ContinuousRecord, Comparable {

    private final String reference;
    private final Continuous value;
    private final int position;

    GenericContinuousRecord(String reference, int position, Continuous value) {
        if (reference == null) throw new IllegalArgumentException("reference must not be null");
        if (value == null) throw new IllegalArgumentException("Value may not be null.");
        this.reference = reference;
        this.position = position;
        this.value = value;
    }

    public static GenericContinuousRecord valueOf(String reference, int position, Continuous value) {
        return new GenericContinuousRecord(reference, position, value);
    }

    public String getReference() {
        return reference;
    }

    public Continuous getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericContinuousRecord that = (GenericContinuousRecord) o;

        if (position != that.position) return false;
        if (!reference.equals(that.reference)) return false;
        if (!value.equals(that.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = reference.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + position;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GenericContinuousRecord");
        sb.append("{reference='").append(reference).append('\'');
        sb.append(", value=").append(value);
        sb.append(", position=").append(position);
        sb.append('}');
        return sb.toString();
    }

    public int compareTo(Object o) {
        GenericContinuousRecord that = (GenericContinuousRecord) o;

        //compare ref
        if (!this.reference.equals(that.getReference())){
            String a1 = this.reference;
            String a2 = that.getReference();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }

        //compare position
        if(this.getPosition() == that.getPosition()){
            return 0;
        } else if (this.getPosition() < that.getPosition()){
            return -1;
        } else {
            return 1;
        }

    }
}
