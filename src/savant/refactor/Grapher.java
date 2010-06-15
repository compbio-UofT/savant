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
 * Grapher.java
 * Created on Jun 15, 2010
 */

package savant.refactor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.util.Range;

import javax.swing.*;
import java.awt.*;

/**
 * Helper class to manage the details of rendering a graph within a {@Track}
 *
 * @see Track
 * @author vwilliams
 */
public class Grapher {

    private static Log log = LogFactory.getLog(Grapher.class);

    /** min / max axis values */
    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;
    private double unitWidth;
    private double unitHeight;
    private int width;
    private int height;

    private boolean isOrdinal = false;

    /**
     * Constructor. Must be provided with the width and height of the visual component graphing will take place in.
     * @param width
     * @param height
     */
    public Grapher(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Set the graph units for the horizontal axis
     *
     * @param r an X range
     */
    public void setXRange(Range r) {
        if (r == null) {
            return;
        }

        //Savant.log("Setting x range to " + r);

        this.xMin = r.getFrom();
        this.xMax = r.getTo();
        setUnitWidth();
    }

    /**
     * Set the graph units for the vertical axis
     *
     * @param r a Y range
     */
    public void setYRange(Range r) {

        if (r == null) {
            return;
        }
        if (this.isOrdinal) {
            return;
        }

        //Savant.log("Setting y range to " + r);

        this.yMin = r.getFrom();
        this.yMax = r.getTo();
        setUnitHeight();
    }

    /**
     * Set the pane's vertical coordinate system to be 0-1
     *
     * @param b true for ordinal, false otherwise.
     */
    public void setOrdinal(boolean b) {
        this.isOrdinal = b;
        if (this.isOrdinal) {
            // don't call setYRange, because it's just going to return without doing anything
            this.yMin = 0;
            this.yMax = 1;
            setUnitHeight();
        }
    }

    /**
     *
     * @return  the number of pixels equal to one graph unit of width.
     */
    public double getUnitWidth() {
        return this.unitWidth;
    }

    /**
     * Set the number of pixels equal to one graph unit of width.
     */
    public void setUnitWidth() {
        unitWidth = (double) this.width / (xMax - xMin + 1);
    }

    /**
     * @return the number of pixels equal to one graph unit of height.
     */
    public double getUnitHeight() {
        return this.unitHeight;
    }

    /**
     * Set the number of pixels equal to one graph unit of height.
     */
    public void setUnitHeight() {
        unitHeight = (double) this.height / (yMax - yMin);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return true if Y Range is 0-1, false o/w
     */
    public boolean isOrdinal() {
        return this.isOrdinal;
    }

    /**
     * @return the range in the X direction spanned by this Grapher
     */
    public Range getRange() {
        return new Range(this.xMin, this.xMax);
    }
    
    /**
     * Transform a graph height into a pixel height
     *
     * @param len height in graph units
     * @return corresponding number of pixels
     */
    public double transformHeight(int len) {
        return this.unitHeight * len;
    }

    /**
     * Transform a graph width into a pixel width
     *
     * @param len width in graph units
     * @return corresponding number of pixels
     */
    public double transformWidth(int len) {
        return this.unitWidth * len;
    }

    /**
     * Transform a horizontal position in terms of graph units into a drawing coordinate
     *
     * @param pos position in graph coordinates
     * @return a corresponding drawing coordinate
     */
    public double transformXPos(int pos) {
        pos = pos - this.xMin;
        return pos * getUnitWidth();
    }

    /**
     * Transform a vertical position in terms of graph units into a drawing coordinate
     *
     * @param pos position in graph coordinates
     * @return a corresponding drawing coordinate
     */
    public double transformYPos(int pos) {
        pos = pos - this.yMin;
        return getHeight() - (pos * getUnitHeight());
    }

}
