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

    public boolean isScaledToFit();

    public void setScaledToFit(boolean b);
}
