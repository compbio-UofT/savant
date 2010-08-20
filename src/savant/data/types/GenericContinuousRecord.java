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

    private final Continuous value;
    private final int position;

    GenericContinuousRecord(int position, Continuous value) {
        if (value == null) throw new IllegalArgumentException("Value may not be null.");
        this.position = position;
        this.value = value;
    }

    public static GenericContinuousRecord valueOf(int position, Continuous value) {
        return new GenericContinuousRecord(position, value);
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
        if (!value.equals(that.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + position;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GenericContinuousRecord");
        sb.append("{value=").append(value);
        sb.append(", position=").append(position);
        sb.append('}');
        return sb.toString();
    }

    public int compareTo(Object o) {
        GenericContinuousRecord that = (GenericContinuousRecord) o;

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
