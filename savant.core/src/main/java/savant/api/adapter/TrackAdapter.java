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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JPanel;

import savant.api.data.Record;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;
import savant.plugin.SavantPanelPlugin;
import savant.util.AxisType;
import savant.util.DrawingMode;

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
     * Determine what kind of x-axis would be appropriate for this track at the given resolution.
     * All normal tracks should return AxisType.INTEGER to get the default display.
     * @since 2.0.0
     */
    public AxisType getXAxisType(Resolution r);

    /**
     * Determine what kind of y-axis would be appropriate for this track at the given resolution.
     */
    public AxisType getYAxisType(Resolution r);

    /**
     * Convert pixel position along the x-axis into a base-position.
     *
     * @param pix drawing position in pixels
     * @return the corresponding logical position
     * @since 1.6.0
     */
    public int transformXPixel(double pix);

    /**
     * Transform a horizontal position in terms of bases into a drawing coordinate.
     * @param pos position in bases
     * @return the corresponding drawing coordinate
     * @since 1.6.0
     */
    public double transformXPos(int pos);

    /**
     * Transform a vertical position in terms of pixels into graph units.
     *
     * @param pix position in pixel coordinates
     * @return corresponding graph coordinate
     * @since 1.6.0
     */
    public double transformYPixel(double pix);

    /**
     * Transform a vertical position in terms of graph units into a pixel position.
     *
     * @param pos position in graph coordinates
     * @return the corresponding drawing coordinate
     * @since 1.6.0
     */
    public double transformYPos(double pos);

    /**
     * Given a record, determine the bounds which would be used for displaying that record.
     * @param rec the record whose bounds we're interested in
     * @return the record's bounds in pixels, relative to the track's bounds (or <code>null</code> if record not found in track)
     */
    public Rectangle getRecordBounds(Record rec);

    /**
     * Given a location within a track window, determine the record which lies at that location.
     * If multiple records overlap at the given position, only the first one will be returned.
     * @param pt the point we're interested in
     * @return the record at that position, or <code>null</code> if no record is there
     */
    public Record getRecordAtPos(Point pt);

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
