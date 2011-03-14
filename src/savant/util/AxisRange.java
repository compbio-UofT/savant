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

package savant.util;

/**
 * Immutable class to represent two coordinate axes.
 *
 * @author mfiume
 */
public class AxisRange {

    private final long xMin;
    private final long xMax;
    private final long yMin;
    private final long yMax;

    AxisRange(long xMin, long xMax, long yMin, long yMax)
    {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public static AxisRange initWithMinMax(long xMin, long xMax, long yMin, long yMax) {
        return new AxisRange(xMin, xMax, yMin, yMax);
    }

    public static AxisRange initWithRanges(Range xRange, Range yRange) {
        return new AxisRange (xRange.getFrom(), xRange.getTo(), yRange.getFrom(), yRange.getTo());
    }

    public long getXMin() { return this.xMin; }
    public long getXMax() { return this.xMax; }
    public long getYMin() { return this.yMin; }
    public long getYMax() { return this.yMax; }

    public Range getXRange() {
        return new Range(xMin, xMax);
    }

    public Range getYRange() {
        return new Range(yMin, yMax);
    }

}
