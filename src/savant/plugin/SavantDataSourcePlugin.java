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

package savant.plugin;

import java.net.URI;

import savant.api.adapter.DataSourceAdapter;


/**
 * Base class to be used for any DataSource plugins.  The canonical example is our
 * own SavantFileRepositoryDataSourcePlugin.
 *
 * @author mfiume
 */
public abstract class SavantDataSourcePlugin extends SavantPlugin {

    /**
     * This method is called once during application life cycle to allow a third-party
     * plugin to initialise itself.
     */
    public abstract void init();

    public abstract DataSourceAdapter getDataSource() throws Exception;

    /**
     * Give the data source plugin an opportunity to handle URIs which aren't recognised
     * by Savant.
     *
     * @param uri the URI to be tested
     * @return a DataSource created from the given URI; if it's not a URI this plugin can handle, null is a perfectly good answer.
     */
    public DataSourceAdapter getDataSource(URI uri) {
        return null;
    }
}
