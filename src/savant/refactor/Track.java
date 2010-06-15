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

import java.util.List;

/**
 * Class to represent a particular data view. It may use many DataSources and many Renderers for each.
 *
 * To create a concreate Track, use {@TrackFactory}
 *
 * @see TrackFactory
 * @author vwilliams
 */
public abstract class Track {

    private VizPane vizPane;
    private TrackConfigurator trackConfigurator;
    private Grapher grapher;
    private List<DataSource> dataSources;
}
