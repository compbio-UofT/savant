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
 * PointRecord.java
 * Created on Jan 8, 2010
 */

package savant.model;

/**
 * Immutable class implementing a Record which contains a Point
 *
 * @author vwilliams
 */
public class PointRecord implements Record {

    private final Point point;

    protected PointRecord(Point point) {
        if (point == null) throw new IllegalArgumentException("Invalid argument. Point may not be null.");
        this.point = point;
    }

    public static PointRecord valueOf(Point point) {
        return new PointRecord(point);
    }

    public Point getPoint() {
        return point;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PointRecord pointRecord = (PointRecord) o;

        if (!point.equals(pointRecord.point)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return point.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PointRecord");
        sb.append("{point=").append(point);
        sb.append('}');
        return sb.toString();
    }
}
