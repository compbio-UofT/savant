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
package savant.view.swing;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

import savant.api.adapter.ModeAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.controller.SelectionController;
import savant.controller.DataSourceController;
import savant.controller.TrackController;
import savant.data.types.Genome;
import savant.data.sources.*;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.DataFormat;
import savant.util.ColorScheme;
import savant.util.MiscUtils;
import savant.util.Mode;
import savant.util.Range;
import savant.util.Resolution;
import savant.view.dialog.BAMParametersDialog;
import savant.view.swing.interval.BAMTrack;
import savant.view.swing.sequence.SequenceTrack;
import savant.view.swing.util.DialogUtils;

/**
 * Class to handle the preparation for rendering of a track. Handles colour schemes and
 * drawing instructions, getting and filtering of data, setting of vertical axis, etc. The
 * ranges associated with various resolutions are also handled here, and the drawing modes
 * are defined.
 *
 * @author mfiume
 */
public abstract class Track implements TrackAdapter {

    private final String name;
    private ColorScheme colorScheme;
    private List<Record> dataInRange;
    private List<ModeAdapter> drawModes;
    private ModeAdapter drawMode;
    protected final TrackRenderer renderer;
    private final DataSource dataSource;
    private ColorSchemeDialog colorDialog = new ColorSchemeDialog();
    private IntervalDialog intervalDialog = new IntervalDialog();
    // FIXME:
    private Frame frame;
    private static BAMParametersDialog paramDialog = new BAMParametersDialog(Savant.getInstance(), true);

    // TODO: put all of this in a TrackFactory class
    // TODO: inform the user when there is a problem
    

    public static Genome createGenome(Track sequenceTrack) {

        if (sequenceTrack == null) {
            return null;
        }

        if (!(sequenceTrack instanceof SequenceTrack)) {
            DialogUtils.displayMessage("Sorry", "Could not load this track as genome.");
            TrackController.getInstance().removeUnframedTrack(sequenceTrack);
            return null;
        }

        // determine default track name from filename
        String genomePath = sequenceTrack.getName();
        int lastSlashIndex = genomePath.lastIndexOf("/");
        String name = genomePath.substring(lastSlashIndex + 1, genomePath.length());

        return new Genome(name, (SequenceTrack) sequenceTrack);
    }

    /**
     * Constructor
     * @param source track data source; name, type, and will be derived from this
     */
    protected Track(DataSource dataSource, TrackRenderer renderer) throws SavantTrackCreationCancelledException {

        drawModes = new ArrayList<ModeAdapter>();

        this.dataSource = dataSource;
        this.renderer = renderer;

        String n = getUniqueName(dataSource.getName());

        if (n == null) {
            throw new SavantTrackCreationCancelledException();
        }

        name = n;

        renderer.setTrackName(name);
    }

    private String getUniqueName(String name) {
        String result = name;
        while (TrackController.getInstance().containsTrack(result)) {
            result = DialogUtils.displayInputMessage(
                    "A track with that name already exists. Please enter a new name:",
                    result);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    public void notifyControllerOfCreation() {
        TrackController tc = TrackController.getInstance();
        tc.addTrack(this);
        if (dataSource != null) {
            DataSourceController.getInstance().addDataSource(dataSource);
        }
    }

    /**
     * Get the type of file this view track represents
     *
     * @return  FileFormat
     *
    @Override
    public DataFormat getDataFormat() {
        return dataType;
    }
     */

    /**
     * Get the current colour scheme.
     *
     * @return ColorScheme
     */
    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    /**
     * Set individual colour.
     *
     * @param name color name
     * @param color new color
     */
    public void setColor(String name, Color color) {
        colorScheme.addColorSetting(name, color);
    }

    /**
     * Get the name of this track. Usually constructed from the file name.
     *
     * @return track name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the data currently being displayed (or ready to be displayed)
     *
     * @return List of data objects
     */
    @Override
    public List<Record> getDataInRange() {
        return dataInRange;
    }

    @Override
    public List<Record> getSelectedDataInRange() {
        return SelectionController.getInstance().getSelections(getName());
    }

    /*
    public DrawingInstructions getDrawingInstructions() {
    return this.DRAWING_INSTRUCTIONS;
    }
     */
    /**
     * Get current draw mode
     *
     * @return draw mode as Mode
     */
    @Override
    public ModeAdapter getDrawMode() {
        return drawMode;
    }

    /**
     * Get all valid draw modes for this track.
     *
     * @return List of draw Modes
     */
    @Override
    public List<ModeAdapter> getDrawModes() {
        return drawModes;
    }

    /**
     * Set colour scheme.
     *
     * @param cs new colour scheme
     */
    public void setColorScheme(ColorScheme cs) {
        this.colorScheme = cs;
    }

    /**
     * Reset colour scheme.
     *
     */
    public abstract void resetColorScheme();

    /*
    public void setDrawingInstructions(DrawingInstructions di) {
    this.DRAWING_INSTRUCTIONS = di;
    }
     */
    /**
     * Set the current draw mode.
     *
     * @param mode
     */
    @Override
    public void setDrawMode(ModeAdapter mode) {
        drawMode = mode;
    }

    /**
     * Set the current draw mode by its name
     *
     * @param modename
     */
    @Override
    public void setDrawMode(String modename) {
        for (ModeAdapter m : drawModes) {
            if (m.getName().equals(modename)) {
                setDrawMode(m);
                break;
            }
        }
    }

    /**
     * Set the list of valid draw modes
     *
     * @param modes
     */
    public final void setDrawModes(List<ModeAdapter> modes) {
        this.drawModes = modes;
    }

    /**
     * Get the record (data) track associated with this view track (if any.)
     *
     * @return Record Track or null (in the case of a genome.)
     */
    @Override
    public DataSource getDataSource() {
        return this.dataSource;
    }

    /**
     * Convenience method to get a track's data format.
     * 
     * @return the DataFormat of the track's DataSource
     */
    public DataFormat getDataFormat() {
        return dataSource.getDataFormat();
    }

    /**
     * Utility method to determine whether this track has data for the given reference.
     *
     * @return
     */
    public boolean containsReference(String ref) {
        return dataSource.getReferenceNames().contains(ref) || dataSource.getReferenceNames().contains(MiscUtils.homogenizeSequence(ref));
    }

    /**
     * Get the JPanel for the layer to draw on top of the track.
     *
     * @return component to draw onto
     */
    @Override
    public JPanel getLayerCanvas() {
        return frame.getLayerCanvas();
    }

    // FIXME:
    public Frame getFrame() {
        return frame;
    }

    // FIXME:
    public void setFrame(Frame frame) {
        this.frame = frame;
        colorDialog.setFrame(frame);
        intervalDialog.setFrame(frame);
    }

    /**
     * Retrieve the renderer associated with this track.
     *
     * @return the track's renderer
     */
    public TrackRenderer getRenderer() {
        return renderer;
    }

    /**
     * Prepare this view track to render the given range.
     *
     * @param range
     * @throws IOException
     */
    public abstract void prepareForRendering(String reference, Range range) throws IOException;

    /**
     * Method which plugins can use to force the Track to repaint itself.
     */
    @Override
    public void repaint() {
        frame.getGraphPane().repaint();
    }

    @Override
    public boolean isSelectionAllowed() {
        return renderer.selectionAllowed();
    }

    /**
     * Retrieve data from the underlying data track at the current resolution and save it.
     *
     * @param range The range within which to retrieve objects
     * @return a list of data objects in the given range
     * @throws Exception
     */
    public List<Record> retrieveAndSaveData(String reference, Range range) throws IOException {
        Resolution resolution = getResolution(range);

        /*
        // Get current time
        long start = System.currentTimeMillis();
         */

        dataInRange = retrieveData(reference, range, resolution);

        /*
        // Get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis()-start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis/1000F;

        System.out.println("\tData retrieval for " + this.getName() + " took " + elapsedTimeSec + " seconds");
         */

        return dataInRange;
    }

    /**
     * Store null to dataInRange.
     *
     * @throws Exception
     */
    public void saveNullData() {
        this.dataInRange = null;
    }

    /**
     * Retrieve data from the underlying data track.
     *
     * @param range The range within which to retrieve objects
     * @param resolution The resolution at which to get data
     * @return a List of data objects from the given range and resolution
     * @throws Exception
     */
    public abstract List<Record> retrieveData(String reference, RangeAdapter range, Resolution resolution) throws IOException;

    /**
     * Get the default draw mode.
     *
     * @return  the default draw mode
     */
    @Override
    public Mode getDefaultDrawMode() {
        return null;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static void captureBAMDisplayParameters(BAMTrack track) {
        paramDialog.setVisible(true);
        if (paramDialog.isAccepted()) {
            track.setArcSizeVisibilityThreshold(paramDialog.getArcLengthThreshold());
            track.setDiscordantMin(paramDialog.getDiscordantMin());
            track.setDiscordantMax(paramDialog.getDiscordantMax());
        }

    }

    public void captureColorParameters() {
        colorDialog.update(this);
        colorDialog.setVisible(true);
    }

    public void captureIntervalParameters() {
        intervalDialog.update(this);
        intervalDialog.setVisible(true);
    }
}
