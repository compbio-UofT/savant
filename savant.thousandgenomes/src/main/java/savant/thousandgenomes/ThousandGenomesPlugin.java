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
package savant.thousandgenomes;

import java.io.IOException;
import java.net.URL;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.SettingsUtils;
import savant.plugin.SavantPanelPlugin;


public class ThousandGenomesPlugin extends SavantPanelPlugin {
    private static final Log LOG = LogFactory.getLog(ThousandGenomesPlugin.class);

    FTPBrowser browser;

    @Override
    public void init(JPanel parent) {
        try {
            String root = SettingsUtils.getString(this, "Root");
            if (root == null) {
                root = "ftp://ftp-trace.ncbi.nih.gov/1000genomes/ftp/data";
            }
            browser = new FTPBrowser(new URL(root));
            parent.add(browser);
        } catch (IOException x) {
            parent.add(new JLabel("Unable to load 1000 genomes plugin: " + x.getMessage()));
        }
    }

    @Override
    public void shutDown() throws Exception {
        if (browser != null) {
            SettingsUtils.setString(this, "Root", browser.getRoot());
            SettingsUtils.store();
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
