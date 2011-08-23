/*
 *    Copyright 2010-2011 University of Toronto
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

import java.util.List;
import javax.swing.JPanel;

import savant.data.types.Record;
import savant.file.DataFormat;
import savant.plugin.SavantPanelPlugin;
import savant.util.AxisType;
import savant.util.DrawingMode;
import savant.util.Resolution;

/**
 * Public interface to Savant track objects.
 *
 * @author mfiume
 */
public interface TrackAdapter {

    /**
     * Get the data currently being displayed (or ready to be displayed)
     *
     * @return list of data records
     */
    public List<Record> getDataInRange();

    /**
     * Get the data currently being displayed (or ready to be displayed) and intersect
     * it with the selection.
     *
     * @return list of data records
     */
    public List<Record> getSelectedDataInRange();


    /**
     * Get the <code>DataSource</code> associated with this track.
     *
     * @return this track's <code>DataSource</code>
     */
    public DataSourceAdapter getDataSource();


    /**
     * Utility method to get a track's data format.  Equivalent to calling getDataSource().getDataFormat().
     *
     * @return this track's <code>DataFormat</code>
     * @since 1.6.0
     */
    public DataFormat getDataFormat();


    /**
     * Get current draw mode.
     *
     * @return draw mode
     */
    public DrawingMode getDrawingMode();

    /**
     * Set the current draw mode.
     *
     * @param mode
     */
    public void setDrawingMode(DrawingMode mode);

    /**
     * Get all valid draw modes for this track.
     *
     * @return List of draw Modes
     */
    public DrawingMode[] getValidDrawingModes();

    /**
     * Get the JPanel for the plugin to draw on top of the track.  This version of the
     * method will create a fresh canvas every time it is called.
     *
     * @return component to draw onto
     * @deprecated Renamed to <code>getLayerCanvas(SavantPanelPlugin)</code>.
     */
    public JPanel getLayerCanvas();

    /**
     * Get a JPanel for the given plugin to draw on top of the track.
     *
     * @param plugin the plugin which is requesting a canvas
     * @return component to draw onto
     * @since 1.6.0
     */
    public JPanel getLayerCanvas(SavantPanelPlugin plugin);

    /**
     * Get the name of this track. Usually constructed from the file name.
     *
     * @return track name
     */
    public String getName();


    /**
     * Get the resolution associated with the given range
     *
     * @param range
     * @return resolution appropriate to the range
     */
    public Resolution getResolution(RangeAdapter range);

    /**
     * Determine what kind of y-axis would be appropriate for this track at the given resolution.
     */
    public AxisType getYAxisType(Resolution r);

    /**
     * Convert a base-position into a pixel position along the x-axis.
     * @since 1.6.0
     */
    public double transformPos(int pos);

    /**
     * Convert pixel position along the x-axis into a base-position.
     * @since 1.6.0
     */
    public int transformPixel(double pixel);

    /**
     * Does this track allow selections?
     *
     * @return true if the user can select
     */
    public boolean isSelectionAllowed();


    /**
     * Repaint the track's contents.
     */
    public void repaint();
}
