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
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

import net.sf.samtools.util.BlockCompressedInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.TrackAdapter;
import savant.controller.SelectionController;
import savant.controller.DataSourceController;
import savant.controller.TrackController;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.sources.*;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.DataFormat;
import savant.util.Bookmark;
import savant.util.ColorScheme;
import savant.util.NetworkUtils;
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

    private final String name;
    private ColorScheme colorScheme;
    private List<Record> dataInRange;
    private List<String> drawModes;
    private String drawMode;
    protected final TrackRenderer renderer;
    private final DataSource dataSource;

    /**
     * Dictionary which keeps track of gene names and other searchable items for this track.
     * Note that regardless of their original case, all keys are stored as lower-case.
     */
    private Map<String, Bookmark> dictionary = new HashMap<String, Bookmark>();

    private final List<DataRetrievalListener> listeners = new ArrayList<DataRetrievalListener>();

    // FIXME:
    private Frame frame;

    // TODO: put all of this in a TrackFactory class
    // TODO: inform the user when there is a problem


    /**
     * Constructor
     * @param source track data source; name, type, and will be derived from this
     */
    protected Track(DataSource dataSource, TrackRenderer renderer) throws SavantTrackCreationCancelledException {

        drawModes = new ArrayList<String>();

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
    public String getDrawMode() {
        return drawMode;
    }

    /**
     * Get all valid draw modes for this track.
     *
     * @return List of draw Modes
     */
    @Override
    public List<String> getDrawModes() {
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
    public void setDrawMode(String mode) {
        drawMode = mode;
    }

    /**
     * Set the list of valid draw modes
     *
     * @param modes
     */
    public final void setDrawModes(List<String> modes) {
        drawModes = modes;
    }

    /**
     * Get the record (data) track associated with this view track (if any.)
     *
     * @return Record Track or null (in the case of a genome.)
     */
    @Override
    public DataSource getDataSource() {
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
     * Load the dictionary from the given URI.
     */
    public void loadDictionary(URI uri) throws IOException {
        dictionary = new HashMap<String, Bookmark>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new BlockCompressedInputStream(NetworkUtils.getSeekableStreamForURI(uri))));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] entry = line.split("\\t");
                dictionary.put(entry[0].toLowerCase(), new Bookmark(entry[1]));
            }
        } catch (ParseException x) {
            throw new IOException(x);
        }
        reader.close();
    }

    /**
     * Look in our dictionary for the given key.  If we find it, set the reference and range appropriately.
     */
    public Bookmark lookup(String key) {
        return dictionary.get(key.toLowerCase());
    }

    /**
     * Utility method to determine whether this track has data for the given reference.
     *
     * @return
     */
    public boolean containsReference(String ref) {
        return true;//dataSource.getReferenceNames().contains(ref) || dataSource.getReferenceNames().contains(MiscUtils.homogenizeSequence(ref));
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
        boolean b = frame.getGraphPane().isRenderForced();
        frame.getGraphPane().setRenderForced();
        frame.getGraphPane().repaint();
        //frame.getGraphPane().setRenderForced(b);
    }

    @Override
    public boolean isSelectionAllowed() {
        return renderer.selectionAllowed(false);
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
                    LOG.debug("Retrieved " + dataInRange.size() + " records for " + name + "(" + reference + ":" + range + ")");
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
        for (DataRetrievalListener l: listeners) {
            l.dataRetrievalStarted(new DataRetrievalEvent());
        }
    }

    /**
     * Fires a DataSource successful completion event.  It will be posted to the
     * AWT event-queue thread, so that UI code can function properly.
     */
    private void fireDataRetrievalCompleted() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (DataRetrievalListener l: listeners) {
                    l.dataRetrievalCompleted(new DataRetrievalEvent(dataInRange));
                }
            }
        });

    }

    /**
     * Fires a DataSource error event.  It will be posted to the AWT event-queue
     * thread, so that UI code can function properly.
     */
    private void fireDataRetrievalFailed(final Exception x) {
        EventQueue.invokeLater(new Runnable() {
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

    public void captureColorParameters() {
        ColorSchemeDialog dlg = new ColorSchemeDialog(this);
        dlg.setLocationRelativeTo(frame);
        dlg.setVisible(true);
    }

    public void captureIntervalParameters() {
        IntervalDialog dlg = new IntervalDialog(this);
        dlg.setLocationRelativeTo(frame);
        dlg.setVisible(true);
    }
}
