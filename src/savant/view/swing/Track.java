/*
 *    Copyright 2009-2011 University of Toronto
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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.TrackAdapter;
import savant.controller.SelectionController;
import savant.controller.DataSourceController;
import savant.controller.TrackController;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.DataFormat;
import savant.util.AxisType;
import savant.util.ColorScheme;
import savant.util.DrawingMode;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.util.Resolution;
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
    private static final Log LOG = LogFactory.getLog(Track.class);
    protected static final RenderingException ZOOM_MESSAGE = new RenderingException(MiscUtils.MAC ? "Zoom in to see data\nTo view data at this range, change Preferences > Track Resolutions" : "Zoom in to see data\nTo view data at this range, change Edit > Preferences > Track Resolutions", 0);

    private final String name;
    private ColorScheme colorScheme;
    private List<Record> dataInRange;
    private DrawingMode[] validDrawingModes;
    private DrawingMode drawingMode;
    protected final TrackRenderer renderer;
    private final DataSourceAdapter dataSource;

    private final List<DataRetrievalListener> listeners = new ArrayList<DataRetrievalListener>();

    // FIXME:
    private Frame frame;

    // TODO: put all of this in a TrackFactory class
    // TODO: inform the user when there is a problem


    /**
     * Constructor a new track with the given renderer.
     *
     * @param dataSource track data source; name, type, and will be derived from this
     * @param renderer the <code>TrackRenderer</code> to be used for this track
     */
    protected Track(DataSourceAdapter dataSource, TrackRenderer renderer) throws SavantTrackCreationCancelledException {

        validDrawingModes = new DrawingMode[0];

        this.dataSource = dataSource;
        this.renderer = renderer;

        String n = getUniqueName(dataSource.getName());

        if (n == null) {
            throw new SavantTrackCreationCancelledException();
        }

        name = n;

        renderer.setTrackName(name);
        addDataRetrievalListener(renderer);
    }

    @Override
    public String toString() {
        return name;
    }

    private String getUniqueName(String name) {
        String result = name;
        while (TrackController.getInstance().containsTrack(result)) {
            result = DialogUtils.displayInputMessage("Duplicate Track",
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

    /**
     * Get current draw mode
     *
     * @return draw mode as Mode
     */
    @Override
    public DrawingMode getDrawingMode() {
        return drawingMode;
    }

    /**
     * Get all valid draw modes for this track.
     *
     * @return List of draw Modes
     */
    @Override
    public DrawingMode[] getValidDrawingModes() {
        return validDrawingModes;
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

    /**
     * Set the current draw mode.
     *
     * @param mode
     */
    @Override
    public void setDrawingMode(DrawingMode mode) {
        drawingMode = mode;
    }

    /**
     * Set the list of valid draw modes
     *
     * @param modes
     */
    public final void setValidDrawingModes(DrawingMode[] modes) {
        validDrawingModes = modes;
    }

    /**
     * Get the record (data) track associated with this view track (if any.)
     *
     * @return Record Track or null (in the case of a genome.)
     */
    @Override
    public DataSourceAdapter getDataSource() {
        return dataSource;
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
     * @param ref the reference to be checked
     * @return true if the track has data for ref
     */
    public boolean containsReference(String ref) {
        return dataSource.getReferenceNames().contains(ref) || dataSource.getReferenceNames().contains(MiscUtils.homogenizeSequence(ref));
    }

    /**
     * Get the JPanel for the layer which plugins can use to draw on top of the track.
     *
     * @return component to draw onto or null if frame not initialized yet
     */
    @Override
    public JPanel getLayerCanvas() {
        return frame != null ? frame.getLayerCanvas() : null;
    }

    /**
     * For use by plugins.  Scale a pixel position along the x-axis into a base position.
     * @since 1.6.0
     */
    @Override
    public int transformPixel(double pix) {
        return frame.getGraphPane().transformXPixel(pix);
    }
    

    /**
     * For use by plugins.  Scale a position in bases into a pixel position along the x-axis.
     * @since 1.6.0
     */
    @Override
    public double transformPos(int pos) {
        return frame.getGraphPane().transformXPos(pos);
    }


    // FIXME:
    public Frame getFrame() {
        return frame;
    }

    // FIXME:
    public void setFrame(Frame frame) {
        this.frame = frame;
        addDataRetrievalListener(frame);
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
     * Prepare this track to render the given range.  Since the actual data-retrieval
     * is now done on a separate thread, preparing to render should not throw any
     * exceptions.
     *
     * @param reference the reference to be rendered
     * @param range the range to be rendered
     */
    public abstract void prepareForRendering(String reference, Range range);

    /**
     * Method which plugins can use to force the Track to repaint itself.
     */
    @Override
    public void repaint() {
        frame.getGraphPane().setRenderForced();
        frame.getGraphPane().repaint();
    }

    @Override
    public boolean isSelectionAllowed() {
        return renderer.selectionAllowed(false);
    }

    @Override
    public AxisType getYAxisType(Resolution r) {
        return AxisType.NONE;
    }

    /**
     * Request data from the underlying data track at the current resolution.  A new
     * thread will be started.
     *
     * @param reference The reference within which to retrieve objects
     * @param range The range within which to retrieve objects
     */
    public void requestData(final String reference, final Range range) {
        dataInRange = null;
        fireDataRetrievalStarted();
        Thread retriever = new Thread("DataRetriever") {
            @Override
            public void run() {
                try {
                    LOG.debug("Retrieving data for " + name + "(" + reference + ":" + range + ")");
                    dataInRange = retrieveData(reference, range, getResolution(range));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Retrieved " + (dataInRange != null ? Integer.toString(dataInRange.size()) : "no") + " records for " + name + "(" + reference + ":" + range + ")");
                    }
                    fireDataRetrievalCompleted();
                } catch (Exception x) {
                    LOG.error("Data retrieval failed.", x);
                    fireDataRetrievalFailed(x);
                }
            }
        };
        retriever.start();
        try {
            retriever.join(1000);
            if (retriever.isAlive()) {
                // Join timed out, but we are still waiting for data to arrive.
                LOG.trace("Join timed out, putting up progress-bar.");
            }
        } catch (InterruptedException ix) {
            LOG.error("DataRetriever interrupted during join.", ix);
        }
    }

    public final void addDataRetrievalListener(DataRetrievalListener l) {
        synchronized (listeners){
            listeners.add(l);
        }
    }

    public void removeDataRetrievalListener(DataRetrievalListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
     * Fires a DataSource started event.  This is called from the AWT-EventQueue
     * thread so it doesn't need to use invokeLater.
     */
    private void fireDataRetrievalStarted() {
        synchronized (listeners) {
            for (DataRetrievalListener l: listeners) {
                l.dataRetrievalStarted(new DataRetrievalEvent());
            }
        }
    }

    /**
     * Fires a DataSource successful completion event.  It will be posted to the
     * AWT event-queue thread, so that UI code can function properly.
     */
    private void fireDataRetrievalCompleted() {
        MiscUtils.invokeLaterIfNecessary(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (DataRetrievalListener l: listeners) {
                        l.dataRetrievalCompleted(new DataRetrievalEvent(dataInRange));
                    }
                }
            }
        });

    }

    /**
     * Fires a DataSource error event.  It will be posted to the AWT event-queue
     * thread, so that UI code can function properly.
     */
    private void fireDataRetrievalFailed(final Exception x) {
        MiscUtils.invokeLaterIfNecessary(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (DataRetrievalListener l: listeners) {
                        l.dataRetrievalFailed(new DataRetrievalEvent(x));
                    }
                }
            }
        });
    }

    /**
     * Store null to dataInRange.  This implicitly means that data-retrieval is considered
     * to have completed without error.
     *
     * @throws Exception
     */
    public void saveNullData() {
        dataInRange = null;
        fireDataRetrievalCompleted();
    }

    /**
     * Retrieve data from the underlying data source.  The default behaviour is just
     * to call getRecords on the track's data source.
     *
     * @param range The range within which to retrieve objects
     * @param resolution The resolution at which to get data
     * @return a List of data objects from the given range and resolution
     * @throws Exception
     */
    protected synchronized List<Record> retrieveData(String reference, Range range, Resolution resolution) throws Exception {
        return getDataSource().getRecords(reference, range, resolution);
    }
}
