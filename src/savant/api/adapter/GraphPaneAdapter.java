/*
 *    Copyright 2010-2012 University of Toronto
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

package savant.api.adapter;

import savant.util.Range;


/**
 * Interface which defines the types of objects which a track is rendered upon.  In 
 * the current implementation, this is a JPanel living inside a JIDE DockableFrame.
 *
 * This interface is intended for internal use only.
 *
 * @author tarkvara
 * @since 2.0.0
 */
public interface GraphPaneAdapter extends PopupHostingAdapter {

    /**
     * Convert an X position in pixels into a base position.
     * @param pix on-screen horizontal pixel position
     * @return the corresponding base within the current chromosome
     */
    public int transformXPixel(double pix);

    /**
     * Convert an Y position in pixels into a logical vertical position.
     * @param pix vertical pixel position
     * @return the corresponding logical vertical position
     */
    public double transformYPixel(double pix);

    /**
     * Convert an X position in bases into a pixel position.
     * @param pos base position within the current chromosome
     * @return the horizontal pixel position
     */
    public double transformXPos(int pos);

    /**
     * Convert a logical Y position into a vertical position in pixels.
     * @param pos logical vertical position
     * @return the corresponding vertical pixel position
     */
    public double transformYPos(double pos);

    public void setRenderForced();

    /**
     * Repaint the contents of this graph pane.  Typically falls back to JComponent.repaint().
     */
    public void repaint();

    /**
     * Get the width in pixels of this graph pane.  Typically falls back to JComponent.getWidth().
     */
    public int getWidth();

    /**
     * Get the height in pixels of this graph pane.  Typically falls back to JComponent.getHeight().
     */
    public int getHeight();

    public int getOffset();

    /**
     * Get the width in pixels of a base.
     * @return  the number of pixels equal to one graph unit of width (i.e. one base).
     */
    public double getUnitWidth();

    /**
     * Get the height of the graph pane's logical unit.
     * @return  the number of pixels equal to one graph unit of height
     */
    public double getUnitHeight();

    /**
     * Set the height of the graph pane's logical unit.
     * @param h the number of pixels equal to one graph unit of height
     */
    public void setUnitHeight(double h);

    public void setXRange(Range range);

    public Range getYRange();

    public void setYRange(Range range);

    public boolean needsToResize();

    public FrameAdapter getParentFrame();

    public void setScaledToFit(boolean b);
}
