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

import savant.api.data.Interval;
import savant.api.data.IntervalRecord;

/**
 * Immutable class to represent an interval + a description.
 * 
 * @author vwilliams
 */
public class GenericIntervalRecord implements IntervalRecord {

    /**
     * Column names shared by our basic interval data-sources.
     */
    public static final String[] COLUMN_NAMES = new String[] { "Reference", "From", "To", "Description" };

    private final String reference;
    private final Interval interval;
    private final String name;

    protected GenericIntervalRecord(String reference, Interval interval, String name) {
        if (reference == null) throw new IllegalArgumentException("reference must not be null");
        if (interval == null) throw new IllegalArgumentException("Invalid argument. Interval must not be null");
        this.reference = reference;
        this.interval = interval;
        this.name = name;
    }


    public static GenericIntervalRecord valueOf(String reference, Interval interval, String description) {
        return new GenericIntervalRecord(reference, interval, description);
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public Interval getInterval() {
        return this.interval;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericIntervalRecord that = (GenericIntervalRecord) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (!interval.equals(that.interval)) return false;
        if (!reference.equals(that.reference)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = reference.hashCode();
        result = 31 * result + interval.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GenericIntervalRecord");
        sb.append("{reference='").append(reference).append('\'');
        sb.append(", interval=").append(interval);
        sb.append(", description='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(Object o) {

        GenericIntervalRecord other = (GenericIntervalRecord) o;

        //compare ref
        if (!this.reference.equals(other.getReference())){
            String a1 = this.reference;
            String a2 = other.getReference();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }
        //compare interval
        int a = this.getInterval().getStart();
        int b = other.getInterval().getStart();

        if (a == b){
            return name.compareTo(other.name);
        } else if(a < b) {
            return -1;
        } else {
            return 1;
        }
    }
}
