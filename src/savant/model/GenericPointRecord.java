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
 * GenericPointRecord.java
 * Created on Jan 8, 2010
 */

package savant.model;

/**
 * Immutable Record class to hold a generic point (point + optional description)
 * 
 * @author vwilliams
 */
public class GenericPointRecord extends PointRecord {

    private final String description;

    protected GenericPointRecord(Point point, String description) {
        super(point);
        this.description = description;
    }

    public static GenericPointRecord valueOf(Point point, String description) {
        return new GenericPointRecord(point, description);    
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericPointRecord that = (GenericPointRecord) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return description != null ? description.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GenericPointRecord");
        sb.append("{description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
