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
import savant.data.sources.DataSource;
import savant.data.types.Record;
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
    public DataSource getDataSource();


    /**
     * Get current draw mode.
     *
     * @return draw mode
     */
    public String getDrawMode();

    /**
     * Set the current draw mode.
     *
     * @param mode
     */
    public void setDrawMode(String mode);


    /**
     * Get all valid draw modes for this track.
     *
     * @return List of draw Modes
     */
    public List<String> getDrawModes();


    /**
     * Get the JPanel for the layer to draw on top of the track.
     *
     * @return component to draw onto
     */
    public JPanel getLayerCanvas();


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
