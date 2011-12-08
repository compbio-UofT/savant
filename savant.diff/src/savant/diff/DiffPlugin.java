/*
 *    Copyright 2011 University of Toronto
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
