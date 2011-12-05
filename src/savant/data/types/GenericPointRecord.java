/*
 *    Copyright 2010-2011 University of Toronto
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
    public int getPoint() {
        return point;
    }

    @Override
    public String getDescription() {
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
        if (point == that.getPoint()) {
            return 0;
        } else if(point < that.getPoint()) {
            return -1;
        } else {
            return 1;
        }
    }
}
