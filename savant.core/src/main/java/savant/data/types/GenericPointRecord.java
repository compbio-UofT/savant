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

import savant.api.data.PointRecord;


/**
 * Immutable Record class to hold a generic point (point + optional description)
 * 
 * @author vwilliams
 */
public class GenericPointRecord implements PointRecord {

    private final String reference;
    private final int point;
    private final String description;

    protected GenericPointRecord(String reference, int point, String description) {
        if (reference == null) throw new IllegalArgumentException("reference must not be null");
        this.reference = reference;
        this.point = point;
        this.description = description;
    }

    public static GenericPointRecord valueOf(String reference, int point, String description) {
        return new GenericPointRecord(reference, point, description);
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public int getPosition() {
        return point;
    }

    @Override
    public String getName() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericPointRecord that = (GenericPointRecord) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (point != that.point) return false;
        if (!reference.equals(that.reference)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = reference.hashCode();
        result = 31 * result + point;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GenericPointRecord");
        sb.append("{reference='").append(reference).append('\'');
        sb.append(", point=").append(point);
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(Object o) {
        GenericPointRecord that = (GenericPointRecord) o;

        //compare ref
        if (!this.reference.equals(that.getReference())) {
            String a1 = reference;
            String a2 = that.getReference();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++) {
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }

        //compare position
        if (point == that.getPosition()) {
            return 0;
        } else if(point < that.getPosition()) {
            return -1;
        } else {
            return 1;
        }
    }
}
