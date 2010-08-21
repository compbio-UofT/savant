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

    private final int position;

    private Point(int position) {

        if (position < 0) throw new IllegalArgumentException("Invalid argument. Points must be >= 0.");

        this.position = position;
    }

    public static Point valueOf(int position) {
        return new Point(position);
    }

    public int getPosition() {
        return this.position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (position != point.position) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return position;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Point");
        sb.append("{position=").append(position);
        sb.append('}');
        return sb.toString();
    }
}
