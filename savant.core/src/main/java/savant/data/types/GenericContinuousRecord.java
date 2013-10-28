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
package savant.data.types;

import savant.api.data.ContinuousRecord;

/**
 * Immutable class to contain a value and the position at which that value obtains.
 */
public class GenericContinuousRecord implements ContinuousRecord {

    /**
     * Column names shared by our various continuous data-sources.
     */
    public static final String[] COLUMN_NAMES = new String[] { "Reference", "Position", "Value" };

    private final String reference;
    private final float value;
    private final int position;

    protected GenericContinuousRecord(String reference, int position, float value) {
        if (reference == null) throw new IllegalArgumentException("Reference may not be null.");
        this.reference = reference;
        this.position = position;
        this.value = value;
    }

    public static GenericContinuousRecord valueOf(String reference, int position, float value) {
        return new GenericContinuousRecord(reference, position, value);
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public float getValue() {
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
        if (value != that.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = reference.hashCode();
        result = 31 * result + Float.floatToIntBits(value);
        result = 31 * result + (int)position;
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

    @Override
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
