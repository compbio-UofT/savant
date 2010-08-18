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
public class ContinuousRecord implements Record, Comparable {

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

    @Override
    public int compareTo(Object o) {
        ContinuousRecord that = (ContinuousRecord) o;

        //compare ref
        if(!this.getReference().equals(that.getReference())){
            String a1 = this.getReference();
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
