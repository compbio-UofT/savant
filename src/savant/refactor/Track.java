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

/*
 * Track.java
 * Created on Jun 15, 2010
 */

/**
 * @author vwilliams
 */
package savant.refactor;

import savant.model.Resolution;
import savant.model.view.ColorScheme;
import savant.model.view.Mode;
import savant.util.Range;

import java.util.*;

/**
 * Class to represent a particular data view. It may use many DataSources and many Renderers for each.
 *
 * To create a concreate Track, use {@TrackFactory}
 *
 * @see TrackFactory
 * @author vwilliams
 */
public abstract class Track {

    private TrackConfigurator trackConfigurator;
    private Grapher grapher;
    private List<DataSource> dataSources = new ArrayList<DataSource>();

    private Map<DataSource, List<Renderer>> renderMap = new HashMap<DataSource, List<Renderer>>();

    private boolean locked;
    private Range lockedRange;
    private boolean panning;
    private boolean zooming;
    private boolean dragging;

    private ColorScheme colorScheme;

    private String name;

    // currently loaded data
    private RefSeq loadedSequence;
    private Range loadedRange;
    private Resolution loadedResolution;
    private Map<DataSource, List> dataMap;

    /**
     * Add a new data source to the track
     * @param dataSource the source to add
     */
    public void addDataSource(DataSource dataSource) {
        dataSources.add(dataSource);
    }

    /**
     * Add a data source and associated renderer to the track
     * @param dataSource the DataSource
     * @param renderer the Renderer
     */
    public void addDataSource(DataSource dataSource, Renderer renderer) {
        dataSources.add(dataSource);
        List<Renderer> renderers;
        if (renderMap.containsKey(dataSource)) {
            renderers = renderMap.get(dataSource);
        }
        else {
            renderers = new ArrayList<Renderer>();
            renderMap.put(dataSource, renderers);
        }
        renderers.add(renderer);

    }

    /**
     * Add a data source and a list of associated renderers to the track
     * @param dataSource the DataSource
     * @param rendererList a List of Renderers
     */
    public void addDataSource(DataSource dataSource, List<Renderer> rendererList) {
        dataSources.add(dataSource);
        List<Renderer> renderers;
        if (renderMap.containsKey(dataSource)) {
            renderers = renderMap.get(dataSource);
        }
        else {
            renderers = new ArrayList<Renderer>();
            renderMap.put(dataSource, renderers);
        }
        renderers.addAll(rendererList);

    }

    /**
     * Add a renderer to the list of renderers associated with an existing DataSource
     * @param dataSource DataSource already associated with the Track
     * @param renderer an additional Renderer
     */
    public void addRenderer(DataSource dataSource, Renderer renderer) {

        if (dataSources.contains(dataSource)) {
            List<Renderer> renderers;
            if (renderMap.containsKey(dataSource)) {
                renderers = renderMap.get(dataSource);
            }
            else {
                renderers = new ArrayList<Renderer>();
                renderMap.put(dataSource, renderers);
            }
            renderers.add(renderer);
        }
    }

    /**
     * Get all Renderers for a given DataSource
     * @param dataSource the DataSource to return renderers for
     * @return List of Renderers for the given DataSource; an empty list if no Renderers available
     */
    public List<Renderer> getRenderers(DataSource dataSource) {
        if (renderMap.containsKey(dataSource))
            return renderMap.get(dataSource);
        else {
            List<Renderer> l = Collections.EMPTY_LIST;
            return l;
        }           
    }

    /**
     * Get all Renderers associated with this Track, regardless of DataSource
     * @return a (possibly) empty List of renderers
     */
    public List<Renderer> getRenderers() {
        List<Renderer> results = new ArrayList<Renderer>();
        for (DataSource source: renderMap.keySet()) {
            List<Renderer> renderersForSource = renderMap.get(source);
            if (renderersForSource != null) {
                results.addAll(renderersForSource);
            }
        }
        return results;
    }

    /**
     * Render all data sources associated with this track
     * @param canvas some drawing surface, e.g awt.Graphics
     * @param sequence the sequence to use
     * @param range the range to draw
     * @param resolution the resolution at which to draw
     */
    public abstract void drawDataSources(Object canvas, RefSeq sequence, Range range, Resolution resolution);

    /**
     * Render a legend for this track
     * @param canvas  some drawing surface, e.g awt.Graphics
     */
    public abstract void drawLegend(Object canvas);

    /**
     * Render a background for this track
     *
     * @param canvas some drawing surface, e.g awt.Graphics
     * @param sequence the sequence to use
     * @param range the range to draw
     * @param resolution the resolution at which to draw
     */
    public abstract void drawBackground(Object canvas, RefSeq sequence, Range range, Resolution resolution);

    /**
     * Adjust to the fact that some renderer has changed its mode, e.g. change the mode of another renderer.
     * @param dataSource the DataSource whose Renderer has changed mode
     * @param renderer the Renderer whose Mode has changed
     * @param mode the new Mode
     */
    public void switchMode(DataSource dataSource, Renderer renderer, Mode mode) {
        // default does nothing, subclasses may override
    }

    /**
     * Load a data set in preparation for drawing.
     *
     * @param sequence the sequence to use
     * @param range the range to fetch
     * @param resolution the resolution at which to get the data
     */
    public void preloadData(RefSeq sequence, Range range, Resolution resolution) {

        if (!sequence.equals(this.loadedSequence) || !range.equals(this.loadedRange) || !resolution.equals(this.loadedResolution)) {
            for (DataSource source: dataSources) {
                dataMap.put(source, source.getRange(sequence, range, resolution));
            }
        }
    }

    public Range getLockedRange() {
        return this.lockedRange;    
    }

    public TrackConfigurator getTrackConfigurator() {
        return trackConfigurator;
    }

    public void setTrackConfigurator(TrackConfigurator trackConfigurator) {
        this.trackConfigurator = trackConfigurator;
    }

    public Grapher getGrapher() {
        return grapher;
    }

    public void setGrapher(Grapher grapher) {
        this.grapher = grapher;
    }

    public List<DataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isPanning() {
        return panning;
    }

    public void setPanning(boolean panning) {
        this.panning = panning;
    }

    public boolean isZooming() {
        return zooming;
    }

    public void setZooming(boolean zooming) {
        this.zooming = zooming;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
