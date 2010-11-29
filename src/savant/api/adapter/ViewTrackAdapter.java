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

package savant.api.adapter;

import java.net.URI;
import java.util.List;
import javax.swing.JPanel;
import savant.data.sources.DataSource;
import savant.data.types.Record;
import savant.file.DataFormat;
import savant.util.Resolution;

/**
 *
 * @author mfiume
 */
public interface ViewTrackAdapter {

    // constructors
    //public static List<ViewTrack> create(URI trackURI) throws IOException {
    //public static Genome createGenome(URI genomeURI) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
    //public ViewTrack(String name, FileFormat dataType, DataSource dataSource) {

    // questionable
    /*
    public ColorScheme getColorScheme();
    public void setColor(String name, Color color);
    public DrawingInstructions getDrawingInstructions() {
    public void setDrawingInstructions(DrawingInstructions di) {
    public abstract List<Object> retrieveData(String reference, Range range, Resolution resolution) throws Throwable;
    public void addTrackRenderer(TrackRenderer renderer) {
    public List<TrackRenderer> getTrackRenderers() {
    public void setColorScheme(ColorScheme cs);
    public abstract void resetColorScheme();
    public Frame getFrame();
     */

    /**
     * Get the data currently being displayed (or ready to be displayed)
     *
     * @return List of data records
     */
    public List<Record> getDataInRange();

    /**
     * Get the data currently being displayed (or ready to be displayed) and intersect
     * it with the selection.
     *
     * @return List of data records
     */
    public List<Record> getSelectedDataInRange();


    /**
     * Get the record (data) track associated with this view track (if any.)
     *
     * @return Record Track or null (in the case of a genome.)
     */
    public DataSource getDataSource();


    /**
     * Get the type of file this view track represents
     *
     * @return  data format
     */
    public DataFormat getDataType();


    /**
     * Get current draw mode
     *
     * @return draw mode
     */
    public ModeAdapter getDrawMode();

    /**
     * Set the current draw mode.
     *
     * @param mode
     */
    public void setDrawMode(ModeAdapter mode);


    /**
     * Set the current draw mode by its name
     *
     * @param mode name
     */
    public void setDrawMode(String modename);


    /**
     * Get all valid draw modes for this track.
     *
     * @return List of draw Modes
     */
    public List<ModeAdapter> getDrawModes();


    /**
     * Get the default draw mode.
     *
     * @return  the default draw mode
     */
    public ModeAdapter getDefaultDrawMode();


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
     * Get the URI corresponding to the track's DataSource.
     *
     * @return the URI for the track's data (possibly null)
     */
    public URI getURI();

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
