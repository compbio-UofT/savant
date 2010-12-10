/*
 * DataSourceController.java
 * Created on Mar 11, 2010
 *
 *
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

package savant.controller;

import java.util.ArrayList;
import java.util.List;

import savant.controller.event.DataSourceListChangedEvent;
import savant.controller.event.DataSourceListChangedListener;
import savant.data.sources.DataSource;

/**
 * Singleton controller class to manage data tracks.
 */
public class DataSourceController {

    /**
     * Singleton instance. Use getInstance() to get a reference.
     */
    private static DataSourceController instance;

    /**
     * List of currently managed data sources
     */
    private List<DataSource> sources;

    private List<DataSourceListChangedListener> listeners;

    /**
     * Constructor. Private access, use getInstance() instead.
     */
    private DataSourceController() {
        sources = new ArrayList<DataSource>();
        listeners = new ArrayList<DataSourceListChangedListener>();
    }

    public static synchronized DataSourceController getInstance() {
        if (instance == null) {
            instance = new DataSourceController();
        }
        return instance;
    }

    public void addDataSource(DataSource source) {
        sources.add(source);
        fireTracksChangedEvent();
    }

    public void removeDataSource(DataSource dataSource) {
        sources.remove(dataSource);
        fireTracksChangedEvent();
    }

    public List<DataSource> getDataSources() {
        return sources;
    }

    public DataSource getDataSource(int index) {
        return sources.get(index);
    }
    
    public synchronized void addTrackListChangedListener(DataSourceListChangedListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeTrackListChangedListener(DataSourceListChangedListener listener) {
        listeners.remove(listener);
    }

    private void fireTracksChangedEvent() {
        DataSourceListChangedEvent evt = new DataSourceListChangedEvent(this, this.sources);
        for (DataSourceListChangedListener listener : listeners) {
            listener.trackListChangeReceived(evt);
        }
    }
}
