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
package savant.settings;

import java.awt.GridBagConstraints;
import java.io.IOException;
import javax.swing.JCheckBox;

/**
 *
 * @author mfiume
 */
public class GeneralSettingsSection extends Section {
    JCheckBox checkversion_cb;
    JCheckBox collectrstats_cb;
    private JCheckBox startpage_cb;

    @Override
    public String getTitle() {
        return "General Settings";
    }

    @Override
    public void lazyInitialize() {
        add(SettingsDialog.getHeader(getTitle()), getFullRowConstraints());

        checkversion_cb = new JCheckBox("Check version on startup");
        checkversion_cb.setSelected(BrowserSettings.getCheckVersionOnStartup());
        checkversion_cb.addActionListener(enablingActionListener);
        add(checkversion_cb, getFullRowConstraints());

        startpage_cb = new JCheckBox("Show Start Page");
        startpage_cb.setSelected(BrowserSettings.getShowStartPage());
        startpage_cb.addActionListener(enablingActionListener);
        add(startpage_cb, getFullRowConstraints());

        collectrstats_cb = new JCheckBox("Collect anonymous statistics about usage");
        collectrstats_cb.setSelected(BrowserSettings.getCollectAnonymousUsage());
        collectrstats_cb.addActionListener(enablingActionListener);
        GridBagConstraints gbc = getFullRowConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 1.0;
        add(collectrstats_cb, gbc);
    }

    @Override
    public void applyChanges() {
        // Only save anything if this panel has gone through lazy initialization.
        if (checkversion_cb != null) {
            BrowserSettings.setCheckVersionOnStartup(this.checkversion_cb.isSelected());
            BrowserSettings.setShowStartPage(this.startpage_cb.isSelected());
            BrowserSettings.setCollectAnonymousUsage(this.collectrstats_cb.isSelected());
            
            try {
                PersistentSettings.getInstance().store();
            } catch (IOException iox) {
                LOG.error("Unable to save general settings.", iox);
            }
        }
    }
}
