/*
 *    Copyright 2009-2010 University of Toronto
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

/**
 * Immutable class to represent a single position of interest.
 * 
 * @author mfiume
 */
public class Point {

    private final String reference;
    private final int position;

    private Point(String reference, int position) {

        if (reference == null) throw new IllegalArgumentException("Invalid argument. Reference may not be null.");
        if (position < 0) throw new IllegalArgumentException("Invalid argument. Points must be >= 0.");

        this.reference = reference;
        this.position = position;
    }

    public static Point valueOf(String reference, int position) {
        return new Point(reference, position);
    }

    public int getPosition() {
        return this.position;
    }
    
    public String getReference() {
        return this.reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (position != point.position) return false;
        if (!reference.equals(point.reference)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = reference.hashCode();
        result = 31 * result + position;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Point");
        sb.append("{reference='").append(reference).append('\'');
        sb.append(", position=").append(position);
        sb.append('}');
        return sb.toString();
    }
}
