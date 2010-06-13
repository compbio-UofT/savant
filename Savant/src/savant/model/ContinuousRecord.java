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
 * ContinuousRecord.java
 * Created on Jan 11, 2010
 */

package savant.model;

/**
 * Class to contain a value and the position at which that value obtains.
 * @author vwilliams
 */
public class ContinuousRecord implements Record {

    private String reference;
    private Continuous value;
    private int position;

    public ContinuousRecord(String reference, int position, Continuous value) {
        setReference(reference);
        setPosition(position);
        setValue(value);
    }

    public Continuous getValue() {
        return value;
    }

    public void setValue(Continuous value) {
        if (value == null) throw new IllegalArgumentException("Continuous value must not be null.");
        this.value = value;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
    
    public String getReference() {
        return this.reference;
    }


    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContinuousRecord that = (ContinuousRecord) o;

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
        sb.append("ContinuousRecord");
        sb.append("{value=").append(value);
        sb.append(", position=").append(position);
        sb.append('}');
        return sb.toString();
    }

}
