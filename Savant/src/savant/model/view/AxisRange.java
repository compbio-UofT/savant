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

package savant.model.view;

import savant.util.Range;

/**
 *
 * @author mfiume
 */
public class AxisRange {

    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;

    private boolean isXMinSet = false;
    private boolean isXMaxSet = false;
    private boolean isYMinSet = false;
    private boolean isYMaxSet = false;

    public AxisRange() { }

    public AxisRange(Range xRange) { this(xRange.getFrom(), xRange.getTo()); }

    public AxisRange(int xMin, int xMax)
    {
        setYMin(xMin);
        setYMax(xMax);
    }

    public AxisRange(Range xRange, Range yRange) { this(xRange.getFrom(), xRange.getTo(), yRange.getFrom(), yRange.getTo()); }

    public AxisRange(int xMin, int xMax, int yMin, int yMax)
    {
        setXMin(xMin);
        setXMax(xMax);
        setYMin(yMin);
        setYMax(yMax);
    }

    public void setXMin(int xMin) { this.xMin = xMin; isXMinSet = true; }
    public void setXMax(int xMax) { this.xMax = xMax; isXMaxSet = true; }
    public void setYMin(int yMin) { this.yMin = yMin; isYMinSet = true; }
    public void setYMax(int yMax) { this.yMax = yMax; isYMaxSet = true; }

    public void setXRange(Range xRange)
    {
        setXMin(xRange.getFrom());
        setXMax(xRange.getTo());
    }

    public void setYRange(Range yRange)
    {
        setYMin(yRange.getFrom());
        setYMax(yRange.getTo());
    }

    public int getXMin() { return this.xMin; }
    public int getXMax() { return this.xMax; }
    public int getYMin() { return this.yMin; }
    public int getYMax() { return this.yMax; }

    public Range getXRange()
    {
        return new Range(getXMin(), getXMax());
    }

    public Range getYRange()
    {
        return new Range(getYMin(), getYMax());
    }

    public boolean isXRangeSet() { return isXMinSet() && isXMaxSet(); }
    public boolean isYRangeSet() { return isYMinSet() && isYMinSet(); }

    public boolean isXMinSet() { return isXMinSet; }
    public boolean isXMaxSet() { return isXMaxSet; }
    public boolean isYMinSet() { return isYMinSet; }
    public boolean isYMaxSet() { return isYMaxSet; }
}
