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
public class GenericPointRecord extends PointRecord implements Comparable  {

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
        if(this.getPoint().getPosition() != that.getPoint().getPosition()) return false;

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

    @Override
    public int compareTo(Object o) {
        GenericPointRecord that = (GenericPointRecord) o;

        //compare ref
        if (!this.getPoint().getReference().equals(that.getPoint().getReference())){
            String a1 = this.getPoint().getReference();
            String a2 = that.getPoint().getReference();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }

        //compare position
        if (this.getPoint().getPosition() == that.getPoint().getPosition()){
            return 0;
        } else if(this.getPoint().getPosition() < that.getPoint().getPosition()){
            return -1;
        } else {
            return 1;
        }
    }
}
