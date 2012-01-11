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
package savant.view.tracks;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.List;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.FrameAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.data.Record;
import savant.api.event.DataRetrievalEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Resolution;
import savant.controller.TrackController;
import savant.exception.RenderingException;
import savant.exception.SavantTrackCreationCancelledException;
import savant.plugin.SavantPanelPlugin;
import savant.selection.SelectionController;
import savant.util.*;

/**
 * Class to handle the preparation for rendering of a track. Handles colour schemes and
 * drawing instructions, getting and filtering of data, setting of vertical axis, etc. The
 * ranges associated with various resolutions are also handled here, and the drawing modes
 * are defined.
 *
 * @author mfiume
 */
public abstract class Track extends Controller<DataRetrievalEvent> implements TrackAdapter {
    private static final Log LOG = LogFactory.getLog(Track.class);
    protected static final RenderingException ZOOM_MESSAGE = new RenderingException(MiscUtils.MAC ? "Zoom in to see data\nTo view data at this range, change Preferences > Track Resolutions" : "Zoom in to see data\nTo view data at this range, change Edit > Preferences > Track Resolutions", RenderingException.LOWEST_PRIORITY);

    private final String name;
    private ColourScheme colourScheme;
    private List<Record> dataInRange;
    protected DrawingMode drawingMode = DrawingMode.STANDARD;
    protected final TrackRenderer renderer;
    private final DataSourceAdapter dataSource;
    private DataRetriever retriever;
    protected RecordFilterAdapter filter;

    /**
     * In practice this will be a JIDE DockableFrame, but we could conceivably have a
     * stub implementation for headless or web-based operation.
     */
    private FrameAdapter frame;

    /**
     * Constructor a new track with the given renderer.
     *
     * @param dataSource track data source; name, type, and will be derived from this
     * @param renderer the <code>TrackRenderer</code> to be used for this track
     */
    protected Track(DataSourceAdapter dataSource, TrackRenderer renderer) throws SavantTrackCreationCancelledException {

        this.dataSource = dataSource;
        this.renderer = renderer;

        String n = getUniqueName(dataSource.getName());

        if (n == null) {
            throw new SavantTrackCreationCancelledException();
        }

        name = n;

        renderer.setTrackName(name);
        addListener(renderer);
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

    /**
     * Get the current colour scheme.
     *
     * @return ColourScheme
     */
    public ColourScheme getColourScheme() {
        if (colourScheme == null) {
            colourScheme = getDefaultColourScheme();
        }
        return colourScheme;
    }

    /**
     * Set individual colour.
     *
     * @param key one of Savant's standard colour keys
     * @param color new color
     */
    public void setColor(ColourKey key, Color color) {
        getColourScheme().setColor(key, color);
    }

    public abstract ColourScheme getDefaultColourScheme();

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
        return new DrawingMode[] { DrawingMode.STANDARD };
    }

    /**
     * Set the current draw mode.
     *
     * @param mode
     */
    @Override
    public void setDrawingMode(DrawingMode mode) {
        drawingMode = mode;
        frame.drawModeChanged(this);
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
    @Override
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
     * Retrieve a JPanel for the layer which plugins can use to draw on top of the track, creating one if necessary.
     *
     * @return component to draw onto (guaranteed to be non-null if called after <code>TrackEvent.OPENED<code> notification has been received)
     * @since 1.6.0
     */
    @Override
    public JPanel getLayerCanvas(SavantPanelPlugin plugin) {
        return frame.getLayerCanvas(plugin, true);
    }

    /**
     * Create a JPanel for the layer which a plugin can use to draw on top of the track.
     *
     * @return component to draw onto or null if frame not initialized yet
     * @deprecated Renamed to <code>createLayerCanvas()</code>.
     */
    @Override
    public JPanel getLayerCanvas() {
        return getLayerCanvas(null);
    }

    /**
     * For use by plugins.  Scale a pixel position along the x-axis into a base position.
     * @since 1.6.0
     */
    @Override
    public int transformXPixel(double pix) {
        return frame.getGraphPane().transformXPixel(pix);
    }
    

    /**
     * For use by plugins.  Scale a position in bases into a pixel position along the x-axis.
     * @since 1.6.0
     */
    @Override
    public double transformXPos(int pos) {
        return frame.getGraphPane().transformXPos(pos);
    }

    /**
     * For use by plugins.  Scale a pixel position along the y-axis into a logical vertical position.
     * @since 1.6.0
     */
    @Override
    public double transformYPixel(double pix) {
        return frame.getGraphPane().transformYPixel(pix);
    }


    /**
     * For use by plugins.  Scale a logical vertical position into a pixel position along the y-axis.
     * @since 1.6.0
     */
    @Override
    public double transformYPos(double pos) {
        return frame.getGraphPane().transformYPos(pos);
    }

    /**
     * Given a record, determine the bounds which would be used for displaying that record.
     * @param rec the record whose bounds we're interested in
     * @return the record's bounds in pixels, relative to the track's bounds (or null
     */
    @Override
    public Rectangle getRecordBounds(Record rec) {
        Shape s = renderer.recordToShapeMap.get(rec);
        if (s != null) {
            return s.getBounds();
        }
        return null;
    }

    /**
     * Given a location within a track window, determine the record which lies at that location.
     * If multiple records overlap at the given position, only the first one will be returned.
     * @param pt the point we're interested in
     * @return the record at that position, or <code>null</code> if no record is there
     */
    @Override
    public Record getRecordAtPos(Point pt) {
        for (Record r: renderer.recordToShapeMap.keySet()) {
            Shape s = renderer.recordToShapeMap.get(r);
            if (s.contains(new Point2D.Double(pt.x, pt.y))) {
                return r;
            }
        }
        return null;
    }

    public FrameAdapter getFrame() {
        return frame;
    }

    public void setFrame(FrameAdapter frame) {
        this.frame = frame;
        addListener(frame);
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

    /**
     * All ordinary tracks have integer markings along their x axes.
     * @param res the resolution to be considered (ignored)
     * @return <code>AxisType.INTEGER</code>
     */
    @Override
    public AxisType getXAxisType(Resolution res) {
        return AxisType.INTEGER;
    }

    
    /**
     * A number of common track types (Sequence, Point, RichInterval) have no y-axis,
     * so they all share this implementation.
     * @param res the resolution to be considered (ignored)
     * @return <code>AxisType.NONE</code>
     */
    @Override
    public AxisType getYAxisType(Resolution res) {
        return AxisType.NONE;
    }

    /**
     * Request data from the underlying data track at the current resolution.  A new
     * thread will be started.
     *
     * @param reference The reference within which to retrieve objects
     * @param range The range within which to retrieve objects
     */
    public void requestData(String reference, Range range) {
        if (retriever != null) {
            if (retriever.reference.equals(reference) && retriever.range.equals(range)) {
                LOG.debug("Nothing to request, already busy retrieving " + reference + ":" + range);
                return;
            } else {
                LOG.debug("You're wasting your time on " + reference + ":" + range);
            }
        }
        dataInRange = null;
        fireEvent(new DataRetrievalEvent(this));
        retriever = new DataRetriever(reference, range, filter);
        retriever.start();
        try {
            if (retriever != null) {
                retriever.join(1000);
                if (retriever != null && retriever.isAlive()) {
                    // Join timed out, but we are still waiting for data to arrive.
                    LOG.trace("Join timed out, putting up progress-bar.");
                }
            }
        } catch (InterruptedException ix) {
            LOG.error("DataRetriever interrupted during join.", ix);
            retriever = null;
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
                fireEvent(new DataRetrievalEvent(Track.this, dataInRange));
            }
        });
    }

    /**
     * Fires a DataSource error event.  It will be posted to the AWT event-queue
     * thread, so that UI code can function properly.
     */
    private void fireDataRetrievalFailed(final Throwable x) {
        MiscUtils.invokeLaterIfNecessary(new Runnable() {
            @Override
            public void run() {
                fireEvent(new DataRetrievalEvent(Track.this, x));
            }
        });
    }

    /**
     * Cancel an in-progress request to retrieve data.
     */
    public void cancelDataRequest() {
        if (retriever != null) {
            retriever.interrupt();
            fireDataRetrievalFailed(new Exception("Data retrieval cancelled"));
        }
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
    protected synchronized List<Record> retrieveData(String reference, Range range, Resolution resolution, RecordFilterAdapter filter) throws Exception {
        return getDataSource().getRecords(reference, range, resolution, filter);
    }

    private class DataRetriever extends Thread {
        String reference;
        Range range;
        RecordFilterAdapter filter;
 
        DataRetriever(String ref, Range r, RecordFilterAdapter filt) {
            super("DataRetriever-" + ref + ":" + r);
            reference = ref;
            range = r;
            filter = filt;
        }

        @Override
        public void run() {
            try {
                LOG.debug("Retrieving data for " + name + "(" + reference + ":" + range + ")");
                dataInRange = retrieveData(reference, range, getResolution(range), filter);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Retrieved " + (dataInRange != null ? Integer.toString(dataInRange.size()) : "no") + " records for " + name + "(" + reference + ":" + range + ")");
                }
                fireDataRetrievalCompleted();
            } catch (Throwable x) {
                if (NetworkUtils.isStreamCached(dataSource.getURI())) {
                    LOG.info("Cached read failed for " + getName() + " with " + MiscUtils.getMessage(x) + "; deleting cache file and retrying.");
                    try {
                        RemoteFileCache.removeCacheEntry(dataSource.getURI().toString());
                        dataInRange = retrieveData(reference, range, getResolution(range), filter);
                        fireDataRetrievalCompleted();
                    } catch (Throwable x2) {
                        LOG.error("Data retrieval failed twice.", x2);
                        fireDataRetrievalFailed(x2);
                    }   
                } else {
                    LOG.error("Data retrieval failed.", x);
                    fireDataRetrievalFailed(x);
                }
            }
            retriever = null;
        }
    }
}
