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
package savant.agp;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.plugin.SavantPanelPlugin;


/**
 * Plugin to provide HTTP browsing for the Autism Genome Project.
 *
 * @author mfiume
 */
public class AGP extends SavantPanelPlugin {
    private static final Log LOG = LogFactory.getLog(AGP.class);

    HTTPBrowser browser;

    @Override
    public void init(JPanel parent) {
        try {
            parent.setLayout(new BorderLayout());
            browser = new HTTPBrowser(new URL("http://compbio.cs.utoronto.ca/savant/data/asdexome/"));
            parent.add(browser, BorderLayout.CENTER);
        } catch (IOException x) {
            parent.add(new JLabel("Unable to load AGP plugin: " + x.getMessage()));
        }
    }

    @Override
    public void shutDown() throws Exception {
        if (browser != null) {
            browser.closeConnection();
        }
    }

    /**
     * @return title to be displayed in panel
     */
    @Override
    public String getTitle() {
        return "Autism Genome Project";
    }
}
