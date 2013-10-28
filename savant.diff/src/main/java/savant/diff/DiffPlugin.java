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
package savant.diff;

import java.net.URI;
import java.net.URISyntaxException;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.util.DialogUtils;
import savant.api.util.TrackUtils;
import savant.plugin.SavantDataSourcePlugin;


/**
 * Sample DataSource plugin created as an example for the Savant 1.6.0 SDK.
 *
 * @author tarkvara
 */
public class DiffPlugin extends SavantDataSourcePlugin {

    @Override
    public void init() {
    }

    @Override
    public String getTitle() {
        return "Diff Plugin";
    }

    /**
     * Invoked when the user wants to define a fresh Diff track from two existing tracks.
     */
    @Override
    public DataSourceAdapter getDataSource() throws URISyntaxException {
        TrackAdapter[] availableTracks = TrackUtils.getTracks(DataFormat.CONTINUOUS);
        if (availableTracks.length >= 2) {
            SourceDialog dlg = new SourceDialog(availableTracks);
            dlg.setVisible(true);
            if (dlg.result != null) {
                return new DiffDataSource(dlg.result);
            }
        } else {
            DialogUtils.displayMessage("Sorry", "The Diff plugin requires at least two continuous tracks to serve as sources.");
        }
        return null;
    }

    /**
     * Invoked when Savant is constructing a Diff track from an existing URI (e.g. when
     * opening a project which has an existing diff:// track).
     */
    @Override
    public DataSourceAdapter getDataSource(URI uri) {
        try {
            if (uri.getScheme().equals("diff")) {
                return new DiffDataSource(uri);
            }
        } catch (URISyntaxException ignored) {
        }
        return null;
    }
}
