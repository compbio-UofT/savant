/*
 *    Copyright 2009-2011 University of Toronto
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

package savant.util;

/**
 * Immutable class to represent two coordinate axes.
 *
 * @author mfiume
 */
public class AxisRange {

    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;

    public AxisRange(int xMin, int xMax, int yMin, int yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public AxisRange(Range xRange, Range yRange) {
        this(xRange.getFrom(), xRange.getTo(), yRange.getFrom(), yRange.getTo());
    }

    public int getXMin() { return xMin; }
    public int getXMax() { return xMax; }
    public int getYMin() { return yMin; }
    public int getYMax() { return yMax; }

    public Range getXRange() {
        return new Range(xMin, xMax);
    }

    public Range getYRange() {
        return new Range(yMin, yMax);
    }
}
