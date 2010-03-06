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
 * Class to represent an interval + a description.
 * 
 * @author vwilliams
 */
public class GenericIntervalRecord extends IntervalRecord {

    private String description;
    private int descriptionLength;

    public GenericIntervalRecord(Interval interval, String description, int descriptionLength) {
        super(interval);
        this.description = description;
        this.descriptionLength = descriptionLength;
    }

    public GenericIntervalRecord(Interval interval, String description) {
        this(interval, description, description.length());
    }

    public String getDescription() {
        return description;
    }

    public int getDescriptionLength() {
        return this.descriptionLength;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GenericIntervalRecord that = (GenericIntervalRecord) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GenericIntervalRecord");
        sb.append("{description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
