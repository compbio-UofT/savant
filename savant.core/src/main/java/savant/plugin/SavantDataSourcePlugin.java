/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.plugin;

import java.net.URI;

import savant.api.adapter.DataSourceAdapter;
import savant.plugin.SavantPlugin;


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
