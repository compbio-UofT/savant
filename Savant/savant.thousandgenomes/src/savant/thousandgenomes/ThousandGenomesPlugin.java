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

package savant.thousandgenomes;

import java.io.IOException;
import java.net.URL;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.plugin.PluginAdapter;
import savant.plugin.SavantPanelPlugin;

public class ThousandGenomesPlugin extends SavantPanelPlugin {
    private static final Log LOG = LogFactory.getLog(ThousandGenomesPlugin.class);

    FTPBrowser browser;

    @Override
    public void init(JPanel parent, PluginAdapter adapter) {
        try {
            browser = new FTPBrowser(new URL("ftp://ftp-trace.ncbi.nih.gov/1000genomes/ftp/data"));
            parent.add(browser);
        } catch (IOException x) {
            parent.add(new JLabel("Unable to load 1000 genomes plugin: " + x.getMessage()));
        }
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
        if (browser != null) {
            browser.closeConnection();
        }
    }

    /**
     * @return title to be displayed in panel
     */
    @Override
    public String getTitle() {
        return "1000 Genomes";
    }
}
